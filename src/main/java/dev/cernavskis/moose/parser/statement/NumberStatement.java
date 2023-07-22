package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.parser.Statement;

public record NumberStatement(DebugInfo debugInfo, String value) implements Statement {
}
