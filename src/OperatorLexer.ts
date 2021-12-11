import { LexerInterface } from "./Lexer";
import { InvokeElement, ReturningSyntaxElement, SyntaxElement } from "./ParseTreeTypes";

export abstract class OperatorLexer<T extends SyntaxElement = SyntaxElement> {
  public constructor(protected lexer: LexerInterface) { }
  public abstract isValidPosition(): boolean;
  public abstract parseElement(): [T, number];
  public abstract returningElement(): this is OperatorLexer<ReturningSyntaxElement>;
}

export const getLexers: (lexer: LexerInterface) => readonly OperatorLexer[] = (lexer) => Object.freeze([
  new IncrementDecrementLexer(lexer),
]);

class IncrementDecrementLexer extends OperatorLexer<InvokeElement> {
  public returningElement(): true {
    return true;
  }

  public isValidPosition(): boolean {
    return (
      ( // ++variable;
        (
          this.lexer.getToken() === "++" ||
          this.lexer.getToken() === "--"
        ) &&
        this.lexer.isValidIdentifier(this.lexer.getToken(1)) &&
        this.lexer.getToken(2) === ";"
      ) ||
      ( // variable++;

        this.lexer.isValidIdentifier(this.lexer.getToken()) &&
        (
          this.lexer.getToken(1) === "++" ||
          this.lexer.getToken(1) === "--"
        ) &&
        this.lexer.getToken(2) === ";"
      )
    );
  }

  public parseElement(): [InvokeElement, number] {
    const tokens: [string, string] = [
      this.lexer.getToken(),
      this.lexer.getToken(1)
    ];

    let pre: boolean;
    let identifier: string;
    let action: "increment" | "decrement";

    switch (tokens[0]) {
      case "++":
        pre = true;
        identifier = tokens[1];
        action = "increment";
        break;
      case "--":
        pre = true;
        identifier = tokens[1];
        action = "decrement";
        break;
      default:
        pre = false;
        identifier = tokens[0];
        action = tokens[1] === "++" ? "increment" : "decrement";
        break;
    }

    return [
      {
        element: "invoke",
        internal: true,
        name: (pre ? "pre" : "post") + action,
        arguments: [
          {
            element: "reference",
            position: pre ? this.lexer.getPosition(1) : this.lexer.getPosition(),
            value: identifier,
          }
        ],
        position: pre ? this.lexer.getPosition() : this.lexer.getPosition(1),
      },
      3
    ];
  }
}
