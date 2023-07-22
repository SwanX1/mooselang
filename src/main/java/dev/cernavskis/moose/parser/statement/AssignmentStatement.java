package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record AssignmentStatement(DebugInfo debugInfo, Statement qualifiedName, Statement value) implements Statement {
}
