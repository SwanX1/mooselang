package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.parser.Statement;

public record ArrayAccessStatement(DebugInfo debugInfo, Statement parent, Statement index) implements QualifiedNameStatement {
}
