package dev.cernavskis.moose.compiler;

import dev.cernavskis.moose.util.DebugInfo;

public class CompilerException extends RuntimeException {
  public CompilerException(String message, DebugInfo debugInfo) {
    super(message + " at " + debugInfo.toString());
  }
}
