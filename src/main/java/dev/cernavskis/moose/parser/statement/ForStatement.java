package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.util.Nullable;

public record ForStatement(DebugInfo debugInfo, @Nullable Statement initializer, @Nullable Statement condition, @Nullable Statement increment, Statement body) implements Statement {
}
