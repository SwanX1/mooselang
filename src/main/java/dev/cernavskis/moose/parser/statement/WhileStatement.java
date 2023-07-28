package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record WhileStatement(DebugInfo debugInfo, Statement condition, Statement body) implements Statement {
}
