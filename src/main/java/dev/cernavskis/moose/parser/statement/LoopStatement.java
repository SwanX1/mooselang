package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record LoopStatement(DebugInfo debugInfo, Statement body) implements Statement {
}
