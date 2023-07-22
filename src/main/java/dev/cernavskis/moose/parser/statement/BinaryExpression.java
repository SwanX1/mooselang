package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Expression;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record BinaryExpression(DebugInfo debugInfo, Statement left, String operator,
                               Statement right) implements Expression {
}
