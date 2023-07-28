package dev.cernavskis.moose.lexer;

import java.util.*;

// Implements iterator so that we can implement this as a stream.
// This is done to save memory, as well as it allows us to run this on a seperate thread.
public class Lexer {
  private final String input;
  private int position = 0;
  // These next three are used for debugging and error messages.
  private int line = 1;
  private int column = 1;
  private final String file;

  public Lexer(String input) {
    this(input, "<unknown>");
  }

  public Lexer(String input, String file) {
    this.input = input;
    this.file = file;
  }

  private boolean hadEOF = false;
  public List<Token> getAllTokens() {
    List<Token> tokens = new LinkedList<>();
    while (!hadEOF) {
      Token token = nextToken();
      tokens.add(token);
      hadEOF = token.type() == TokenType.EOF;
    }
    return new ArrayList<>(tokens);
  }

  // Okay, this is the real deal.
  private Token nextToken() {
    // Loop over everything while we still have input.
    while (position < input.length()) {
      char currentChar = input.charAt(position);

      // Skip whitespace.
      if (currentChar == ' ' || currentChar == '\n' || currentChar == '\r' || currentChar == '\t') {
        incrPos();
        continue;
      }

      // Skip comments.
      if (currentChar == '/' && input.charAt(position + 1) == '/') {
        incrPos();
        incrPos();
        while (position < input.length() && input.charAt(position) != '\n') {
          incrPos();
        }
        continue;
      }

      // Tokenize everything that is a single character
      if (currentChar == '[') {
        incrPos();
        return newToken(TokenType.ARRAY_LEFT, "[");
      }

      if (currentChar == ']') {
        incrPos();
        return newToken(TokenType.ARRAY_RIGHT, "]");
      }

      if (currentChar == '(') {
        incrPos();
        return newToken(TokenType.PAREN_LEFT, "(");
      }
      if (currentChar == ')') {
        incrPos();
        return newToken(TokenType.PAREN_RIGHT, ")");
      }
      if (currentChar == '{') {
        incrPos();
        return newToken(TokenType.BLOCK_LEFT, "{");
      }
      if (currentChar == '}') {
        incrPos();
        return newToken(TokenType.BLOCK_RIGHT, "}");
      }
      if (currentChar == ',') {
        incrPos();
        return newToken(TokenType.COMMA, ",");
      }

      if (currentChar == ';') {
        incrPos();
        return newToken(TokenType.SEMICOLON, ";");
      }

      if (currentChar == '$') {
        incrPos();
        StringBuilder asm = new StringBuilder();
        while (position < input.length() && input.charAt(position) != '$') {
          asm.append(input.charAt(position));
          incrPos();
        }
        incrPos();
        return newToken(TokenType.ASM, asm.toString());
      }

      // More complex tokens down below, see the functions for more info.
      if (isOperatorChar(currentChar)) {
        String operator = getOperator();
        if (isValidOperator(operator)) {
          TokenType type;
          switch (operator) {
            case "=" -> type = TokenType.ASSIGNMENT;
            case "+" -> type = TokenType.ADDITION;
            case "-" -> type = TokenType.SUBTRACTION;
            case "*" -> type = TokenType.MULTIPLICATION;
            case "/" -> type = TokenType.DIVISION;
            case "+=" -> type = TokenType.ADDITION_ASSIGNMENT;
            case "-=" -> type = TokenType.SUBTRACTION_ASSIGNMENT;
            case "*=" -> type = TokenType.MULTIPLICATION_ASSIGNMENT;
            case "/=" -> type = TokenType.DIVISION_ASSIGNMENT;
            case "%" -> type = TokenType.MODULO;
            case "%=" -> type = TokenType.MODULO_ASSIGNMENT;
            case "++" -> type = TokenType.PREINCREMENT;
            case "--" -> type = TokenType.PREDECREMENT;
            case "**" -> type = TokenType.EXPONENTIATION;
            case "**=" -> type = TokenType.EXPONENTIATION_ASSIGNMENT;
            case "==" -> type = TokenType.EQ;
            case "!=" -> type = TokenType.NEQ;
            case ">=" -> type = TokenType.GTE;
            case "<=" -> type = TokenType.LTE;
            case ">" -> type = TokenType.GT;
            case "<" -> type = TokenType.LT;
            case "||" -> type = TokenType.LOGICAL_OR;
            case "&&" -> type = TokenType.LOGICAL_AND;
            case "!" -> type = TokenType.LOGICAL_NOT;
            case "." -> type = TokenType.DOT;
            case "?" -> type = TokenType.TERNARY;
            case ":" -> type = TokenType.COLON;
            case "~" -> type = TokenType.BIT_NOT;
            case "|" -> type = TokenType.BIT_OR;
            case "&" -> type = TokenType.BIT_AND;
            case "^" -> type = TokenType.BIT_XOR;
            case "~=" -> type = TokenType.BIT_NOT_ASSIGNMENT;
            case "|=" -> type = TokenType.BIT_OR_ASSIGNMENT;
            case "&=" -> type = TokenType.BIT_AND_ASSIGNMENT;
            case "^=" -> type = TokenType.BIT_XOR_ASSIGNMENT;
            case ">>" -> type = TokenType.BIT_RSHIFT;
            case "<<" -> type = TokenType.BIT_LSHIFT;
            case ">>=" -> type = TokenType.BIT_RSHIFT_ASSIGNMENT;
            case "<<=" -> type = TokenType.BIT_LSHIFT_ASSIGNMENT;
            default -> throw new RuntimeException("Invalid operator: " + operator); // Unreachable
          }

          return newToken(type, operator);
        } else {
          throw new IllegalArgumentException("Invalid operator: " + operator);
        }
      }

      if (isString(currentChar)) {
        String value = getString();
        return newToken(TokenType.STRING, value);
      }


      if (isIdentifier(currentChar)) {
        String value = getIdentifier();
        return switch (value) {
          case "func" -> newToken(TokenType.FUNC, value);
          case "let" -> newToken(TokenType.LET, value);
          case "const" -> newToken(TokenType.CONST, value);
          case "if" -> newToken(TokenType.IF, value);
          case "else" -> newToken(TokenType.ELSE, value);
          case "for" -> newToken(TokenType.FOR, value);
          case "foreach" -> newToken(TokenType.FOREACH, value);
          case "do" -> newToken(TokenType.DO, value);
          case "while" -> newToken(TokenType.WHILE, value);
          case "loop" -> newToken(TokenType.LOOP, value);
          case "return" -> newToken(TokenType.RETURN, value);
          case "break" -> newToken(TokenType.BREAK, value);
          case "continue" -> newToken(TokenType.CONTINUE, value);
          case "import" -> newToken(TokenType.IMPORT, value);
          case "export" -> newToken(TokenType.EXPORT, value);
          default -> newToken(TokenType.IDENTIFIER, value);
        };
      }

      if (isConstant(currentChar)) {
        String value = getConstant();
        return newToken(TokenType.CONSTANT, value);
      }

      incrPos();
    }
    return newToken(TokenType.EOF, "");
  }

  // Is the current character a character used in operators?
  private boolean isOperatorChar(char currentChar) {
    return currentChar == '~' || currentChar == '|' || currentChar == '&' || currentChar == '^' ||
      currentChar == '+' || currentChar == '-' || currentChar == '*' || currentChar == '/' ||
      currentChar == '=' || currentChar == '!' || currentChar == '>' || currentChar == '<' ||
      currentChar == '%' || currentChar == '?' || currentChar == ':' || currentChar == '.';
  }

  // Validate that the operator actually exists.
  private boolean isValidOperator(String operator) {
    return Arrays.asList(new String[]{
      // Bitwise
      "~", "|", "&", "^", "~=", "|=", "&=", "^=",
      ">>", "<<", ">>=", "<<=",
      // Numerical
      "+", "-", "*", "/", "+=", "-=", "*=", "/=",
      "%", "%=", "++", "--", "**", "**=",
      // Comparison
      ">", "<", "==", "!=", ">=", "<=",
      // Boolean
      "||", "&&", "!",
      // Miscellaneous
      "?", ":", ".", "=",
    }).contains(operator);
  }

  // Loop over everything until we no longer have an operator character
  private String getOperator() {
    StringBuilder builder = new StringBuilder();
    while (position < input.length() && isOperatorChar(input.charAt(position))) {
      builder.append(input.charAt(position));
      incrPos();
    }
    return builder.toString();
  }

  // Ooooh do we have the start of a string?
  private boolean isString(char currentChar) {
    return currentChar == '"';
  }

  // Basic JSON-type string parsing.
  // TODO: uXXXX and xXX escaping support.
  private String getString() {
    StringBuilder sb = new StringBuilder();
    incrPos();
    while (position < input.length() && input.charAt(position) != '"') {
      if (input.charAt(position) == '\\') {
        incrPos();
        if (position < input.length()) {
          if (input.charAt(position) == 'n') {
            sb.append('\n');
          } else if (input.charAt(position) == 't') {
            sb.append('\t');
          } else if (input.charAt(position) == '"') {
            sb.append('"');
          } else if (input.charAt(position) == '\\') {
            sb.append('\\');
          } else {
            sb.append(input.charAt(position));
          }
        }
      } else {
        sb.append(input.charAt(position));
      }
      incrPos();
    }
    incrPos();
    return sb.toString();
  }

  // Is this the start of an identifier? Identifiers are alphanumeric, and can contain underscores. They must not start with a number, otherwise they are parsed as a constant.
  private boolean isIdentifier(char c) {
    return Character.isLetter(c) || c == '_';
  }

  // Here we check for a number in the input.
  private String getIdentifier() {
    StringBuilder sb = new StringBuilder();
    while (position < input.length() && (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
      sb.append(input.charAt(position));
      incrPos();
    }
    return sb.toString();
  }

  // Is this the start of a constant? Constants are just numbers. Constants can start with a number or a minus sign and a number after it.
  private boolean isConstant(char c) {
    return (Character.isDigit(c) || (c == '-' && Character.isDigit(input.charAt(position + 1))));
  }

  // Here we also check for a decimal point in the input.
  private String getConstant() {
    StringBuilder sb = new StringBuilder();
    while (position < input.length() && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
      sb.append(input.charAt(position));
      incrPos();
    }
    return sb.toString();
  }

  // Utility function to not need to write getDebugInfo every time.
  private Token newToken(TokenType type, String value) {
    return new Token(type, value, line, column, file);
  }

  // Analagous to position++, but also increments line and column correctly.
  private void incrPos() {
    if (input.charAt(position) == '\n') {
      line++;
      column = 0;
    } else {
      column++;
    }
    position++;
  }
}