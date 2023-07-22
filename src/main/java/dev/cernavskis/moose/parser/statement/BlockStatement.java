package dev.cernavskis.moose.parser.statement;

import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.util.DebugInfo;

import java.util.List;

public record BlockStatement(DebugInfo debugInfo, List<Statement> statements) implements Statement { }
