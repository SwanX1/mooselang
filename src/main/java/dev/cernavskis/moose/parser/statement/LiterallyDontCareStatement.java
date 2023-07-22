package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record LiterallyDontCareStatement(DebugInfo debugInfo, String code) implements Statement {
}
