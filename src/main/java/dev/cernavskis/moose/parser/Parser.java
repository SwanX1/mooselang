package dev.cernavskis.moose.parser;

import dev.cernavskis.moose.lexer.TokenType;
import dev.cernavskis.moose.parser.statement.*;
import dev.cernavskis.moose.util.DebugInfo;
import dev.cernavskis.moose.lexer.Token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Parser {
  private final List<Token> tokens;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public BlockStatement parse() {
    final List<Statement> elements = new LinkedList<>();
    while (!match(TokenType.EOF)) {
      try {
        elements.add(statement());
      } catch (ParsingException e) {
        System.err.println("Got " + peekToken());
        System.err.println("Current elements: " + elements);
        throw new RuntimeException(e);
      }
    }
    return new BlockStatement(getDebugInfo(), new ArrayList<>(elements));
  }

  public Token peekToken() {
    return peekToken(0);
  }

  public Token peekToken(int offset) {
    Token token;
    if (tokens.isEmpty()) {
      token = new Token(TokenType.EOF, "", 0, 0, "<unknown>");
    } else {
      token = tokens.get(offset);
    }
    return token;
  }

  public Token nextToken() {
    peekToken();
    return tokens.remove(0);
  }

  public boolean match(int offset, TokenType type) {
    Token token = peekToken(offset);
    return token.type() == type;
  }
  public boolean match(TokenType type) {
    return match(0, type);
  }

  public Token consume(TokenType type) {
    if (match(type)) {
      return nextToken();
    } else {
      throw new ParsingException("Expected " + type + " but got " + peekToken().type(), getDebugInfo());
    }
  }

  private Statement statement() {
    Token token = peekToken();
    if (token.type() == TokenType.EOF) {
      throw new ParsingException("Unexpected EOF", getDebugInfo());
    }


    // Take care of keywords first
    if (match(TokenType.CONST)) {
      return constStatement();
    } else if (match(TokenType.LET)) {
      return letStatement();
    }

    Statement statement = assignment();
    consume(TokenType.SEMICOLON);
    return statement;
  }

  private Statement expression() {
    return assignment();
  }

  private Statement assignment() {
    Statement result = assignmentStrict();
    if (result != null) {
      return result;
    }
    return ternary();
  }

  private Statement assignmentStrict() {
    if (match(0, TokenType.IDENTIFIER)) {
      if (match(1, TokenType.ASSIGNMENT)) {
        DebugInfo debugInfo = getDebugInfo();
        Statement name = qualifiedName();
        consume(TokenType.ASSIGNMENT);
        Statement value = expression();
        return new AssignmentStatement(debugInfo, name, value);
      }
    }
    return null;
  }

  private Statement qualifiedName() {
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected identifier");
    }
    return qualifiedName(new VariableStatement(getDebugInfo(), consume(TokenType.IDENTIFIER).value()));
  }
  private Statement qualifiedName(Statement parent) {
    Statement result = parent;
    DebugInfo debugInfo = getDebugInfo();
    while (match(TokenType.DOT) || match(TokenType.ARRAY_LEFT)) {
      if (match(TokenType.DOT)) {
        consume(TokenType.DOT);
        result = new PropertyAccessStatement(debugInfo, result, consume(TokenType.IDENTIFIER).value());
      } else if (match(TokenType.ARRAY_LEFT)) {
        consume(TokenType.ARRAY_LEFT);
        Statement index = expression();
        consume(TokenType.ARRAY_RIGHT);
        result = new ArrayAccessStatement(debugInfo, result, index);
      }
    }
    return result;
  }

  private Statement ternary() {
    Statement result = binary();
    if (match(TokenType.TERNARY)) {
      DebugInfo debugInfo = getDebugInfo();
      consume(TokenType.TERNARY);
      Statement trueValue = expression();
      consume(TokenType.COLON);
      Statement falseValue = expression();
      return new TernaryExpression(debugInfo, result, trueValue, falseValue);
    }
    return result;
  }

  private Statement binary() {
    Statement result = unary();
    Token token = peekToken();
    if (token.type().isBinaryOperator()) {
      DebugInfo debugInfo = getDebugInfo();
      consume(token.type());
      result = new BinaryExpression(debugInfo, result, token.value(), binary());
    }
    return result;
  }

  private Statement unary() {
    if (match(TokenType.PREINCREMENT)) {
      consume(TokenType.PREINCREMENT);
      return new UnaryExpression(primary(), TokenType.PREINCREMENT, getDebugInfo());
    } else if (match(TokenType.PREDECREMENT)) {
      consume(TokenType.PREDECREMENT);
      return new UnaryExpression(primary(), TokenType.PREDECREMENT, getDebugInfo());
    } else if (match(TokenType.BIT_NOT)) {
      consume(TokenType.BIT_NOT);
      return new UnaryExpression(primary(), TokenType.BIT_NOT, getDebugInfo());
    } else if (match(TokenType.LOGICAL_NOT)) {
      consume(TokenType.LOGICAL_NOT);
      return new UnaryExpression(primary(), TokenType.LOGICAL_NOT, getDebugInfo());
    }
    return primary();
  }

  private Statement primary() {
    if (match(TokenType.PAREN_LEFT)) {
      consume(TokenType.PAREN_LEFT);
      Statement result = expression();
      consume(TokenType.PAREN_RIGHT);
      return result;
    }

    return literal();
  }

  private Statement literal() {
    try {
      DebugInfo debugInfo = getDebugInfo();
      Statement name;
      try {
        name = qualifiedName();
      } catch (RuntimeException e) {
        // doesn't matter
        throw new ParsingException(e.getMessage());
      }
      if (match(TokenType.PAREN_LEFT)) {
        return functionChain(name);
      }
      if (match(TokenType.PREINCREMENT)) {
        consume(TokenType.PREINCREMENT);
        return new UnaryExpression(name, TokenType.POSTINCREMENT, debugInfo);
      } else if (match(TokenType.PREDECREMENT)) {
        consume(TokenType.PREDECREMENT);
        return new UnaryExpression(name, TokenType.POSTDECREMENT, debugInfo);
      }
      return name;
    } catch (ParsingException e) {
      return value();
    }
  }

  private Statement array() {
    DebugInfo debugInfo = getDebugInfo();
    consume(TokenType.ARRAY_LEFT);
    List<Statement> elements = new LinkedList<>();
    while (!match(TokenType.ARRAY_RIGHT)) {
      elements.add(expression());
      if (match(TokenType.COMMA)) {
        consume(TokenType.COMMA);
      }
    }
    consume(TokenType.ARRAY_RIGHT);
    return new ArrayStatement(debugInfo, elements);
  }

  private Statement value() {
    DebugInfo debugInfo = getDebugInfo();
    if (match(TokenType.STRING)) {
      Statement result = new StringStatement(debugInfo, consume(TokenType.STRING).value());
      while (match(TokenType.DOT) || match(TokenType.ARRAY_LEFT) || match(TokenType.PAREN_LEFT)) {
        if (match(TokenType.DOT)) {
          consume(TokenType.DOT);
          result = new PropertyAccessStatement(debugInfo, result, consume(TokenType.IDENTIFIER).value());
        } else if (match(TokenType.ARRAY_LEFT)) {
          consume(TokenType.ARRAY_LEFT);
          Statement index = expression();
          consume(TokenType.ARRAY_RIGHT);
          result = new ArrayAccessStatement(debugInfo, result, index);
        } else if (match(TokenType.PAREN_LEFT)) {
          result = functionChain(result);
        }
      }
      return result;
    } else if (match(TokenType.CONSTANT)) {
      return new NumberStatement(debugInfo, consume(TokenType.CONSTANT).value());
    } else if (match(TokenType.ARRAY_LEFT)) {
      return array();
    } else if (match(TokenType.ASM)) {
      return new LiterallyDontCareStatement(debugInfo, consume(TokenType.ASM).value());
    }

    throw new ParsingException("Unknown expression", getDebugInfo());
  }

  private Statement functionChain(Statement callable) {
    Statement result = function(callable);
    if (match(TokenType.PAREN_LEFT)) {
      return functionChain(result);
    }
    if (match(TokenType.DOT) || match(TokenType.ARRAY_LEFT)) {
      result = qualifiedName(result);
      if (match(TokenType.PAREN_LEFT)) {
        result = functionChain(result);
      }
    }

    return result;
  }

  private Statement function(Statement callable) {
    DebugInfo debugInfo = getDebugInfo();
    consume(TokenType.PAREN_LEFT);
    List<Statement> arguments = new LinkedList<>();
    while (!match(TokenType.PAREN_RIGHT)) {
      arguments.add(expression());
      if (match(TokenType.COMMA)) {
        consume(TokenType.COMMA);
      } else {
        break;
      }
    }
    consume(TokenType.PAREN_RIGHT);
    return new FunctionCallStatement(debugInfo, callable, new ArrayList<>(arguments));
  }

  private DeclarationStatement constStatement() {
    return declarationStatement(true);
  }

  private DeclarationStatement letStatement() {
    return declarationStatement(false);
  }

  private DeclarationStatement declarationStatement(boolean isConst) {
    DebugInfo debugInfo = getDebugInfo();
    if (isConst) {
      consume(TokenType.CONST);
    } else {
      consume(TokenType.LET);
    }
    String name = consume(TokenType.IDENTIFIER).value();
    consume(TokenType.COLON);
    String type = typeName();
    if (!match(TokenType.ASSIGNMENT)) {
      if (!isConst) {
        consume(TokenType.SEMICOLON);
        return new DeclarationStatement(name, null, type, false, debugInfo);
      } else {
        throw new ParsingException("Expected = but got " + peekToken().value(), getDebugInfo());
      }
    }
    consume(TokenType.ASSIGNMENT);
    Statement value = expression();
    consume(TokenType.SEMICOLON);
    return new DeclarationStatement(name, value, type, isConst, debugInfo);
  }

  private String typeName() {
    StringBuilder type = new StringBuilder();
    type.append(consume(TokenType.IDENTIFIER).value());

    while (match(TokenType.ARRAY_LEFT)) {
      consume(TokenType.ARRAY_LEFT);
      consume(TokenType.ARRAY_RIGHT);
      type.append("[]");
    }

    return type.toString();
  }

  public DebugInfo getDebugInfo() {
    return peekToken().debugInfo();
  }
}
