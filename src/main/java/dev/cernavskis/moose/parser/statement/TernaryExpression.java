package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Expression;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record TernaryExpression(DebugInfo debugInfo, Statement condition, Statement trueValue, Statement falseValue) implements Expression {
}
