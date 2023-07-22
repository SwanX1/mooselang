package dev.cernavskis.moose.lexer;

import java.util.Arrays;

public enum TokenType {
  PAREN_LEFT, PAREN_RIGHT,
  BLOCK_LEFT, BLOCK_RIGHT,
  ARRAY_LEFT, ARRAY_RIGHT,
  STRING, CONSTANT, IDENTIFIER,
  COMMA, COLON, SEMICOLON,

  // Keywords
  FUNC,
  LET, CONST,
  IF, ELSE,
  FOR, FOREACH,
  WHILE, LOOP,
  RETURN, BREAK, CONTINUE,
  IMPORT, EXPORT,

  // Operators
  ASSIGNMENT, // =
  ADDITION, // +
  SUBTRACTION, // -
  MULTIPLICATION, // *
  DIVISION, // /
  ADDITION_ASSIGNMENT, // +=
  SUBTRACTION_ASSIGNMENT, // -=
  MULTIPLICATION_ASSIGNMENT, // *=
  DIVISION_ASSIGNMENT, // /=
  MODULO, // %
  MODULO_ASSIGNMENT, // %=
  PREINCREMENT, // ++
  PREDECREMENT, // --
  POSTINCREMENT, // ++, used by parser, not lexer.
  POSTDECREMENT, // --, used by parser, not lexer.
  EXPONENTIATION, // **
  EXPONENTIATION_ASSIGNMENT, // **=
  EQ, // ==
  NEQ, // !=
  GTE, // >=
  LTE, // <=
  GT, // >
  LT, // <
  LOGICAL_OR, // ||
  LOGICAL_AND, // &&
  LOGICAL_NOT, // !
  DOT, // .
  TERNARY, // ?
  BIT_NOT, // ~
  BIT_OR, // |
  BIT_AND, // &
  BIT_XOR, // ^
  BIT_NOT_ASSIGNMENT, // ~=
  BIT_OR_ASSIGNMENT, // |=
  BIT_AND_ASSIGNMENT, // &=
  BIT_XOR_ASSIGNMENT, // ^=
  BIT_RSHIFT, // >>
  BIT_LSHIFT, // <<
  BIT_RSHIFT_ASSIGNMENT, // >>=
  BIT_LSHIFT_ASSIGNMENT, // <<=
  EOF, ASM;
  public static int LONGEST_TOKEN_NAME = Integer.MIN_VALUE;

  static {
    for (TokenType tokenType : TokenType.values()) {
      if (tokenType.name().length() > LONGEST_TOKEN_NAME) {
        LONGEST_TOKEN_NAME = tokenType.name().length();
      }
    }
  }

  public boolean isBinaryOperator() {
    return Arrays.asList(new TokenType[]{
      ASSIGNMENT, // =
      ADDITION, // +
      SUBTRACTION, // -
      MULTIPLICATION, // *
      DIVISION, // /
      ADDITION_ASSIGNMENT, // +=
      SUBTRACTION_ASSIGNMENT, // -=
      MULTIPLICATION_ASSIGNMENT, // *=
      DIVISION_ASSIGNMENT, // /=
      MODULO, // %
      MODULO_ASSIGNMENT, // %=
      EXPONENTIATION, // **
      EXPONENTIATION_ASSIGNMENT, // **=
      EQ, // ==
      NEQ, // !=
      GTE, // >=
      LTE, // <=
      GT, // >
      LT, // <
      LOGICAL_OR, // ||
      LOGICAL_AND, // &&
      BIT_OR, // |
      BIT_AND, // &
      BIT_XOR, // ^
      BIT_NOT_ASSIGNMENT, // ~=
      BIT_OR_ASSIGNMENT, // |=
      BIT_AND_ASSIGNMENT, // &=
      BIT_XOR_ASSIGNMENT, // ^=
      BIT_RSHIFT, // >>
      BIT_LSHIFT, // <<
      BIT_RSHIFT_ASSIGNMENT, // >>=
      BIT_LSHIFT_ASSIGNMENT, // <<=
    }).contains(this);
  }
}
