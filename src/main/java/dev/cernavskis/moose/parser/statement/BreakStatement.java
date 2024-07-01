package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.util.Nullable;

public record BreakStatement(DebugInfo debugInfo, @Nullable String label) implements Statement {
}
