package dev.cernavskis.moose.compiler;

public record StatementBytecode(String code, boolean shouldClearBuffer) {
  @Override
  public String toString() {
    return code;
  }
}
