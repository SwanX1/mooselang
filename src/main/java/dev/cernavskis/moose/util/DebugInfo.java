package dev.cernavskis.moose.util;

import dev.cernavskis.moose.lexer.Token;

public record DebugInfo(int line, int column, String file, Token token) {
  @Override
  public String toString() {
    return "DebugInfo{" +
      "line=" + line +
      ", column=" + column +
      ", file='" + file + '\'' +
      ", token=" + token +
      '}';
  }
}
