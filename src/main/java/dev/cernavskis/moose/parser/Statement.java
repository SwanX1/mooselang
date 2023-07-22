package dev.cernavskis.moose.parser;

import dev.cernavskis.moose.util.DebugInfo;

import java.util.Locale;

public interface Statement {
  default String statementType() {
    return this.getClass().getSimpleName().replace("Statement", "").toLowerCase(Locale.ROOT);
  }
  DebugInfo debugInfo();
}
