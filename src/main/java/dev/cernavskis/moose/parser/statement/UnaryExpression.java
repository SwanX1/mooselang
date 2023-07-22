package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.lexer.TokenType;
import dev.cernavskis.moose.parser.Expression;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record UnaryExpression(Statement value, TokenType operator, DebugInfo debugInfo) implements Expression {
}
