import { ParseError } from "./InterpreterError";

export interface PrimitiveElement {
  position: {
    line: number;
    column: number;
  };
  value: string;
}

export default class Tokenizer {
  /**
   * Tokens that are to be seperated from other keywords.
   * They cannot be used in keywords and must always be by themselves as a token.
   * The most common tokens must be last (i.e. `==` must be before `=`)
   */
  private static SPECIAL_TOKENS: string[] = [
    "[]",                             // Array type indicator
    "(", ")", "{", "}", "[", "]",     // Brackets
    "<<", ">>",                       // Bitshift operators
    "==", "!=", "<=", ">=", "<", ">", // Equality operators
    "!", "&&", "||", "^^",            // Boolean operators
    "++", "--", "=", ".",             // Miscellaneous operators
    "%", "-", "+", "/", "**", "*",    // Math operators
    "^", "&", "~", "|",               // Bitwise operators
    ";",                              // Statement terminator
  ];

  private parsed: boolean = false;
  private tokenized: boolean = false;
  private tokens: PrimitiveElement[] = [];
  private file?: string;
  private script: string;
  private pointer: number = 0;
  private line: number = 0;
  private column: number = 0;
  
  public constructor(script: string, file?: string) {
    this.script = script.replace(/\r/g, ""); // Convert to LF line endings
    this.file = file;
  }

  /**
   * Increments pointer, line, and column values.
   *
   * If the new position of the pointer is out of bounds that
   * means that the parsing is done and we set the pointer to
   * `-1` to signal that to the parser loop.
   */
  private incrementPointer(n = 1): void {
    if (n === 1) {
      if (this.pointer === -1) return;
      this.pointer++;
      if (typeof this.script[this.pointer] !== 'undefined') {
        if (this.script[this.pointer] === "\n") {
          this.line++;
          this.column = 0;
        } else {
          this.column++;
        }
      } else {
        this.pointer = -1;
      }
    } else {
      for (let i = 0; i < n; i++) {
        this.incrementPointer();
      }
    }
  }
  
  /**
   * Character at current pointer position
   */
  private get char(): string {
    return this.script[this.pointer];
  }

  /**
   * Non-modifying method for getting the next token from pointer
   * position of script. Pointer will be incremented if there's a
   * parse error to safely get line and column for error message.
   */
  private getNextElement(): { value: string; offset: number } {
    //! Only this method is used for getting tokens
    let offset = 0;
    let value = "";

    while (/(\s|\n)/.test(this.script[this.pointer + offset])) {
      offset++;
    }

    if (["\"", "\'"].includes(this.script[this.pointer + offset])) {
      value += this.script[this.pointer + offset];
      const quoteCharacter = this.script[this.pointer + offset];
      let i = 1;
      let escaped = false;
      quoteLoop: while (true) {
        if (typeof this.script[this.pointer + offset + i] === "undefined" || this.script[this.pointer + offset + i] === "\n") {
          this.incrementPointer(offset);
          throw new ParseError("Unterminated string literal at {position}", this.script, this.line, this.column);
        }
        value += this.script[this.pointer + offset + i];
        if (!escaped) {
          switch (this.script[this.pointer + offset + i]) {
            case quoteCharacter:
              break quoteLoop;
            case "\\":
              escaped = true;
          }
        } else {
          escaped = false;
        }
        i++;
      }
    }

    if (value === "") {
      const maxTokenSize = Tokenizer.SPECIAL_TOKENS.reduce((prev, cur) => Math.max(prev, cur.length), 0);
      for (let i = maxTokenSize; i > 0; i--) {
        const specialToken = Tokenizer.SPECIAL_TOKENS.find(e => e === this.script.slice(this.pointer + offset, this.pointer + offset + i));
        if (typeof specialToken !== "undefined") {
          value = specialToken;
          break;
        }
      }
    }

    if (value === "") {
      value = this.script.slice(
        this.pointer,
        Math.min(
          this.script.indexOf(" ", this.pointer),
          ...Tokenizer.SPECIAL_TOKENS
            .map(token => this.script.indexOf(token, this.pointer))
            .filter(tokenIndex => tokenIndex >= this.pointer)
        )
      );
      value = value.split("\n", 1)[0];
    }

    return {
      value,
      offset,
    };
  }

  public parse(): { file?: string; tokens: PrimitiveElement[] } {
    //! Parser loop
    while (!this.parsed) {
      tokenizerLoop: while (!this.tokenized) {
        if (/\s/.test(this.char)) {
          this.incrementPointer();
          /** See comment for {@link incrementPointer} */
          if (this.pointer === -1) {
            this.tokenized = true;
            break tokenizerLoop;
          }
        }
        if (this.char === "\n" || /\s/.test(this.char)) {
          continue tokenizerLoop;
        }

        const nextToken = this.getNextElement();
        this.incrementPointer(nextToken.offset);
        this.tokens.push({
          position: {
            line: this.line,
            column: this.column
          },
          value: nextToken.value
        });
        this.incrementPointer(nextToken.value.length);
        /** See comment for {@link incrementPointer} */
        if (this.pointer === -1) {
          this.tokenized = true;
          break tokenizerLoop;
        }
      }

      this.parsed = true;
    }

    return {
      file: this.file,
      tokens: this.tokens
    };
  }
}
