package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.util.DebugInfo;

public record VariableStatement(DebugInfo debugInfo, String value) implements QualifiedNameStatement {
}
