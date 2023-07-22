package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record PropertyAccessStatement(DebugInfo debugInfo, Statement parent, String property) implements QualifiedNameStatement {
}
