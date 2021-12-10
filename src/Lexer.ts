import { RootElement, SyntaxElement } from "./ParseTreeTypes";
import { PrimitiveElement } from "./Tokenizer";

export class Lexer {
  private tokens: PrimitiveElement[];
  private tokenPointer: number = 0;
  private script: string;
  private file?: string;

  public constructor(tokens: PrimitiveElement[], script: string, file?: string) {
    this.tokens = tokens;
    this.script = script;
    this.file = file;
  }

  public parse(): RootElement {
    const elements: SyntaxElement[] = [];

    

    return {
      file: this.file,
      elements,
    };
  }
}