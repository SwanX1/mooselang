import { SyntaxError } from "./MooseError";
import { getLexers, OperatorLexer } from "./OperatorLexer";
import { AssignmentElement, DeclarationElement, FunctionArgument, FunctionDeclarationElement, InvokeElement, LiteralElement, ReturningSyntaxElement, RootElement, SyntaxElement } from "./ParseTreeTypes";
import { PrimitiveElement } from "./Tokenizer";

export class Lexer {
  private tokens: PrimitiveElement[];
  private tokenPointer: number = 0;
  private script: string;
  private file?: string;
  private operatorLexers: readonly OperatorLexer[] = getLexers(this.getLexerInterface());

  public constructor(tokens: PrimitiveElement[], script: string, file?: string) {
    this.tokens = tokens;
    this.script = script;
    this.file = file;
  }

  private getCurrentPosition(): { line: number; column: number; } {
    return this.tokens[this.tokenPointer]?.position;
  }

  private nextPosition(i = 1): { line: number; column: number; } {
    return this.tokens[this.tokenPointer + i]?.position;
  }

  private getCurrentToken(): string {
    return this.tokens[this.tokenPointer]?.value;
  }

  private nextToken(i = 1): string {
    return this.tokens[this.tokenPointer + i]?.value;
  }

  private throwErrorOnCurrentToken(message: string): never {
    const position = this.getCurrentPosition();
    throw new SyntaxError(message + " at {position}", this.script, position.line, position.column);
  }

  public parse(force = false): RootElement {
    const elements: SyntaxElement[] = [];

    while (this.tokenPointer < this.tokens.length) {
      try {
        const element = this.parseElement();
        elements.push(element);
      } catch (err) {
        if (err instanceof SyntaxError) {
          if (force) {
            if (process.argv.slice(2).includes("--debug")) {
              console.error(err.stack.replace("Error", err.constructor.name));
            } else {
              console.error(err.constructor.name + ":" + err.message);
            }
          } else {
            throw err;
          }
        } else {
          throw err;
        }
      }
      this.tokenPointer++;
    }

    return {
      file: this.file,
      elements,
    };
  }

  private parseElement(): SyntaxElement | undefined {
    if (!this.getCurrentToken() || this.getCurrentToken() === ";") return undefined;
    
    const operatorLexer = this.operatorLexers.find(lexer => lexer.isValidPosition());
    if (operatorLexer) {
      const [element, i] = operatorLexer.parseElement();
      this.tokenPointer += i;
      return element;
    }

    if (this.getCurrentToken() === "return") {
      return this.parseReturnElement();
    }

    if (this.nextToken() === "(") {
      return this.parseReturningElement();
    }

    if (
      this.nextToken(2) === "=" ||
      this.getCurrentToken() === "export" ||
      this.getCurrentToken() === "const" ||
      (
        this.nextToken(this.getType(false)[1] + 1) === ";" &&
        this.nextToken(this.getType(false)[1] - 1) !== "="
      )
    ) {
      return this.parseDeclaration();
    } else if (this.getCurrentToken() === "func") {
      return this.parseFunctionDeclaration();
    } else if (this.nextToken() === "=") {
      return this.parseAssignment();
    }
    return this.getCurrentToken() as any as undefined;
  }

  private parseAssignment(): AssignmentElement {
    const position = this.getCurrentPosition();
    let name: string;
    let value: ReturningSyntaxElement;

    return {
      element: "assignment",
      name,
      position,
      value,
    };
  }

  private parseReturnElement(): InvokeElement {
    const position = this.getCurrentPosition();
    this.tokenPointer++;
    const returnElement = this.parseReturningElement();

    return {
      element: "invoke",
      name: "return",
      internal: true,
      arguments: [returnElement],
      position,
    }
  }

  private parseDeclaration(): DeclarationElement {
    const position = this.getCurrentPosition();
    let exported = false;
    let constant = false;
    let type: string;
    let name: string;
    let value: ReturningSyntaxElement;

    for (let i = 0; i < 3; i++) {
      if (this.nextToken(i) === "export") {
        if (!exported) {
          exported = true;
        } else {
          this.tokenPointer += i;
          this.throwErrorOnCurrentToken("Unexpected keyword");
        }
      } else if (this.nextToken(i) === "const") {
        if (!constant) {
          constant = true;
        } else {
          this.tokenPointer += i;
          this.throwErrorOnCurrentToken("Unexpected keyword");
        }
      } else if (this.nextToken(i) === "func") {
        if (constant) {
          this.tokenPointer += i;
          this.throwErrorOnCurrentToken("Functions cannot be declared as constant");
        }
        return this.parseFunctionDeclaration();
      } else {
        this.tokenPointer += i;
        break;
      }
    }

    {
      const [_type, i] = this.getType();
      this.tokenPointer += i;
      type = _type;
    }

    name = this.getCurrentToken();
    {
      const isNameValid = this.isValidIdentifier(name);
      if (typeof isNameValid === "string") {
        this.throwErrorOnCurrentToken(isNameValid);
      }
    }
    this.tokenPointer++;

    if (this.getCurrentToken() !== ";") {
      if (this.getCurrentToken() === "=") {
        this.tokenPointer++;
      } else {
        this.throwErrorOnCurrentToken("Expected ';' or '=', got '" + this.getCurrentToken() + "'");
      }
    }

    value = this.parseReturningElement();
    this.tokenPointer++;


    return {
      element: "declaration",
      constant,
      exported,
      name,
      position,
      type,
      value,
    };
  }

  private parseFunctionDeclaration(): FunctionDeclarationElement {
    let exported = false;
    let name: string;
    let parameters: FunctionArgument[] = [];
    let returnType: string;
    const functionPosition = this.getCurrentPosition();

    if (this.getCurrentToken() === "export") {
      exported = true;
      this.tokenPointer++;
    }
    if (this.getCurrentToken() === "const") {
      this.throwErrorOnCurrentToken("Functions cannot be declared as constant");
    }
    if (this.getCurrentToken() !== "func") {
      this.throwErrorOnCurrentToken("Expected keyword 'func'");
    }
    this.tokenPointer++;

    name = this.getCurrentToken();
    {
      const isNameValid = this.isValidIdentifier(name);
      if (typeof isNameValid === "string") {
        this.throwErrorOnCurrentToken(isNameValid);
      }
    }
    this.tokenPointer++;

    if (this.getCurrentToken() !== "(") {
      this.throwErrorOnCurrentToken("Expected '('");
    }
    this.tokenPointer++;

    while (this.getCurrentToken() !== ")") {
      const position = this.tokens[this.tokenPointer]?.position;

      let parameterType: string;
      {
        const [type, i] = this.getType();
        this.tokenPointer += i;
        parameterType = type;
      }

      const parameterName: string = this.getCurrentToken();
      {
        const isNameValid = this.isValidIdentifier(parameterName);
        if (typeof isNameValid === "string") {
          this.throwErrorOnCurrentToken(isNameValid);
        }
      }
      this.tokenPointer++;

      parameters.push({
        name: parameterName,
        type: parameterType,
        position,
      });

      if (this.getCurrentToken() === ",") {
        continue;
      } else if (this.getCurrentToken() !== ")") {
        this.throwErrorOnCurrentToken("Expected ',' or ')'");
      }
    }
    this.tokenPointer++;

    {
      const [type, i] = this.getType();
      this.tokenPointer += i;
      returnType = type;
    }

    if (this.getCurrentToken() !== "{") {
      this.throwErrorOnCurrentToken("Expected '{'");
    }

    this.tokenPointer++;

    const elements: SyntaxElement[] = [];
    while (this.getCurrentToken() !== "}") {
      const element = this.parseElement();
      elements.push(element);
      this.tokenPointer++;
    }

    return {
      element: "declaration",
      name,
      constant: true,
      exported: exported,
      type: "function",
      value: {
        position: functionPosition,
        returnType,
        arguments: parameters,
        elements,
      },
      position: functionPosition,
    };
  }

  private parseReturningElement(): ReturningSyntaxElement {
    const position = this.getCurrentPosition();
    
    const operatorLexer = (this.operatorLexers.filter(lexer => lexer.returningElement()) as OperatorLexer<ReturningSyntaxElement>[]).find(lexer => lexer.isValidPosition());
    if (operatorLexer) {
      const [element, i] = operatorLexer.parseElement();
      this.tokenPointer += i;
      return element;
    }

    if (this.nextToken() === "(") {
      const name = this.getCurrentToken();
      this.tokenPointer += 2;

      const parameters: ReturningSyntaxElement[] = [];

      while (this.getCurrentToken() !== ")") {
        parameters.push(this.parseReturningElement());
        this.tokenPointer++;

        if (this.getCurrentToken() === ",") {
          this.tokenPointer++;
          continue;
        } else if (this.getCurrentToken() !== ")") {
          this.throwErrorOnCurrentToken("Expected ',' or ')'");
        }
      }

      return {
        element: "invoke",
        internal: false,
        name,
        arguments: parameters,
        position,
      };
    } else if (this.getCurrentToken() === "(") {
      this.tokenPointer++;
      const element = this.parseReturningElement();
      this.tokenPointer++;
      return element;
    } else {
      try {
        return this.parseLiteralElement();
      } catch (error) {
        if (this.isValidIdentifier(this.getCurrentToken())) {
          return {
            element: "reference",
            value: this.getCurrentToken(),
            position,
          };
        } else {
          throw error;
        }
      }
    }
  }

  private parseLiteralElement(): LiteralElement {
    const position = this.getCurrentPosition();
    let type: string;
    let value: string;

    if (this.getCurrentToken()[0] === "\"") {
      type = "string";
      value = this.getCurrentToken().slice(1, -1);
    } else if (this.getCurrentToken() === "true") {
      type = "boolean";
      value = "true";
    } else if (this.getCurrentToken() === "false") {
      type = "boolean";
      value = "false";
    } else if (this.getCurrentToken() === "null") {
      type = "null";
      value = "null";
    } else if (/\d/.test(this.getCurrentToken()[0])) {
      value = this.getCurrentToken();
      if (/\d+(\.\d+)?[fF]/.test(value)) {
        type = "float";
      } else if (/\d+(\.\d+)?[dD]/.test(value)) {
        type = "double";
      } else if (/\d+/.test(value)) {
        type = "int";
      } else if (/0x[a-fA-F0-9]+/.test(value)) {
        type = "int";
        value = parseInt(value.slice(2), 16).toString();
      } else if (/0b[01]+/.test(value)) {
        type = "int";
        value = parseInt(value.slice(2), 2).toString();
      }
    } else {
      this.throwErrorOnCurrentToken("Could not determine literal type");
    }

    return {
      element: "literal",
      position,
      type,
      value,
    }
  }

  /**
   * @returns {[string, number]} [type, tokens consumed]
   * @throws {SyntaxError} if the type identifier is invalid
   */
  private getType(throwError: boolean = true): [string, number] {
    let type: string = "";
    let i = 0;
    do {
      const nextToken = this.nextToken(i);
      if (throwError && nextToken !== "[]") {
        const isTokenValid = this.isValidIdentifier(nextToken);
        if (typeof isTokenValid === "string") {
          this.throwErrorOnCurrentToken(isTokenValid);
        }
      }
      type += nextToken;
      i++;
    } while (this.nextToken(i) === "[]");

    return [type, i];
  }

  /**
   * @returns {true | string} true if the identifier is valid, otherwise the reason it is not.
   */
  private isValidIdentifier(identifier: string): true | string {
    if (identifier === "") {
      return "Empty identifier";
    } else if (!/^[a-zA-Z\$\#\@][a-zA-Z\$\#\@0-9_]*$/.test(identifier)) {
      return "Identifier must only contain alphanumeric characters and the following special characters: '$', '#', '@', '_'";
    } else if (/^\d$/.test(identifier[0])) {
      return "Identifier must not start with a number";
    } else {
      return true;
    }
  }

  private getLexerInterface(): LexerInterface {
    return {
      getToken: (i: number = 0) => this.nextToken(i),
      getPosition: (i: number = 0) => this.nextPosition(i),
      getSource: () => this.script,
      isValidIdentifier: (identifier: string) => this.isValidIdentifier(identifier) === true,
    };
  }
}

export interface LexerInterface {
  getToken(i?: number): string;
  getPosition(i?: number): { line: number, column: number };
  getSource(): string;
  isValidIdentifier(identifier: string): boolean;
}