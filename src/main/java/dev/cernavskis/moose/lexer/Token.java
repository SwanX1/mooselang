package dev.cernavskis.moose.lexer;

import dev.cernavskis.moose.util.DebugInfo;

public final class Token {
  private final TokenType type;
  private final String value;
  private final int line;
  private final int column;
  private final String file;


  public Token(TokenType type, String value, int line, int column, String file) {
    this.type = type;
    this.value = value;
    this.line = line;
    this.column = column;
    this.file = file;
  }

  @Override
  public String toString() {
    return String.format("%s" + " ".repeat(TokenType.LONGEST_TOKEN_NAME - type.name().length()) + " %s", type.name(), value);
  }

  public TokenType type() {
    return type;
  }

  public String value() {
    return value;
  }

  public DebugInfo debugInfo() {
    return new DebugInfo(line, column, file, this);
  }
}
