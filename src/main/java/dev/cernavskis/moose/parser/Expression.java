package dev.cernavskis.moose.parser;

import java.util.Locale;

public interface Expression extends Statement {
  default String statementType() {
    return this.getClass().getSimpleName().replace("Expression", "").toLowerCase(Locale.ROOT);
  }
}
