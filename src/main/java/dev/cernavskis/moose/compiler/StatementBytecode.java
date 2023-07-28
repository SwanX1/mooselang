package dev.cernavskis.moose.compiler;

import java.util.List;

public record StatementBytecode(String code, boolean shouldClearBuffer) {
  @Override
  public String toString() {
    return code;
  }
}
