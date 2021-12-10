import { SyntaxError } from "./MooseError";
import { DeclarationElement, FunctionArgument, FunctionDeclarationElement, RootElement, SyntaxElement } from "./ParseTreeTypes";
import { PrimitiveElement } from "./Tokenizer";

export class Lexer {
  private static KEYWORDS: string[] = [
    "export", "const", "func", "return", "if", "else", "for", "while", "break", "continue",
    "case", "class", "default", "extends", "foreach", "implements", "in", "interface", // Reserved keywords
    "let", "new", "of", "private", "protected", "public", "static", "switch", "var"    // Reserved keywords
  ];
  private tokens: PrimitiveElement[];
  private tokenPointer: number = 0;
  private script: string;
  private file?: string;

  public constructor(tokens: PrimitiveElement[], script: string, file?: string) {
    this.tokens = tokens;
    this.script = script;
    this.file = file;
  }

  private getCurrentToken(): string {
    return this.tokens[this.tokenPointer]?.value;
  }

  private nextToken(i = 1): string {
    return this.tokens[this.tokenPointer + i]?.value;
  }

  private throwErrorOnCurrentToken(message: string): never {
    const { position } = this.tokens[this.tokenPointer];
    throw new SyntaxError(message, this.script, position.line, position.column);
  }

  public parse(): RootElement {
    const elements: SyntaxElement[] = [];

    while (this.tokenPointer < this.tokens.length) {
      const element = this.parseElement();
      elements.push(element);
      this.tokenPointer++;
    }

    return {
      file: this.file,
      elements,
    };
  }

  private parseElement(): SyntaxElement | undefined {
    if (!this.getCurrentToken()) return undefined;
    if (this.nextToken(2) === "=" || this.getCurrentToken() === "export" || this.getCurrentToken() === "const") {
      return this.parseDeclaration();
    } else if (this.nextToken() === "=") {
      // return this.parseAssignment();
    }
    return undefined;
  }

  private parseDeclaration(): DeclarationElement {
    let exported = false;
    let constant = false;
    let type: string;
    let name: string;
    let value: string;

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
        break;
      }
    }



    return {};
  }

  private parseFunctionDeclaration(): FunctionDeclarationElement {
    let exported = false;
    let name: string;
    let parameters: FunctionArgument[] = [];
    let returnType: string;
    const functionPosition = this.tokens[this.tokenPointer].position;

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
    this.tokenPointer++;

    if (this.getCurrentToken() !== "(") {
      this.throwErrorOnCurrentToken("Expected '('");
    }
    this.tokenPointer++;

    while (this.getCurrentToken() !== ")") {
      const position = this.tokens[this.tokenPointer]?.position;

      let parameterType: string = this.getCurrentToken();
      this.tokenPointer++;

      while (this.getCurrentToken() === "[]") {
        parameterType += "[]";
        this.tokenPointer++;
      }

      const parameterName: string = this.getCurrentToken();
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

    returnType = this.getCurrentToken();
    this.tokenPointer++;
    
    while (this.getCurrentToken() === "[]") {
      returnType += "[]";
      this.tokenPointer++;
    }

    if (this.getCurrentToken() !== "{") {
      this.throwErrorOnCurrentToken("Expected '{'");
    }

    while (this.getCurrentToken() !== "}") {
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
        elements: [],
      },
      position: functionPosition,
    };
  }
}