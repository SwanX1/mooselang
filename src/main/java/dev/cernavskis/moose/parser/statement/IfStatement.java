package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.util.Nullable;

public record IfStatement(DebugInfo debugInfo, Statement condition, Statement thenBranch, @Nullable Statement elseBranch) implements Statement {
}
