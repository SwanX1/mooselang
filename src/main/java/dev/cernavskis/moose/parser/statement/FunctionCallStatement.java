package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.parser.Statement;

import java.util.List;

public record FunctionCallStatement(DebugInfo debugInfo, Statement callable,
                                    List<Statement> arguments) implements Statement {
}
