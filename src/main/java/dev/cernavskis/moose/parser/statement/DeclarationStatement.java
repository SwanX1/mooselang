package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

public record DeclarationStatement(String name, Statement value, String type, boolean isConst,
                                   DebugInfo debugInfo) implements Statement {
}
