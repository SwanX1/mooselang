package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.parser.Statement;

public record StringStatement(DebugInfo debugInfo, String value) implements Statement {
}
