package dev.cernavskis.moose.parser;

import dev.cernavskis.moose.util.DebugInfo;

public class ParsingException extends RuntimeException {
  public ParsingException(String message, DebugInfo debugInfo) {
    super(message + " at " + debugInfo.toString());
  }

  public ParsingException(String message) {
    super(message);
  }
}
