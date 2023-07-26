package dev.cernavskis.moose.interpreter.types;

import dev.cernavskis.moose.lexer.TokenType;

public class RuntimeInteger extends RuntimeType<Integer> {
  public RuntimeInteger(Integer value) {
    super(value);
  }

  @Override
  public String getTypeName() {
    return "int";
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    if (!"int".equals(other.getTypeName())) {
      throw new IllegalArgumentException("Cannot perform binary operation on " + getTypeName() + " and " + other.getTypeName());
    }
    return switch (operation) {
      case "+" -> new RuntimeInteger(getValue() + (Integer) other.getValue());
      case "-" -> new RuntimeInteger(getValue() - (Integer) other.getValue());
      case "*" -> new RuntimeInteger(getValue() * (Integer) other.getValue());
      case "/" -> new RuntimeInteger(getValue() / (Integer) other.getValue());
      case "%" -> new RuntimeInteger(getValue() % (Integer) other.getValue());
      case "**" -> new RuntimeInteger((int) Math.pow(getValue(), (Integer) other.getValue()));
      case "==" -> new RuntimeBoolean(getValue() == (Integer) other.getValue());
      case "!=" -> new RuntimeBoolean(getValue() != (Integer) other.getValue());
      case ">=" -> new RuntimeBoolean(getValue() >= (Integer) other.getValue());
      case "<=" -> new RuntimeBoolean(getValue() <= (Integer) other.getValue());
      case ">" -> new RuntimeBoolean(getValue() > (Integer) other.getValue());
      case "<" -> new RuntimeBoolean(getValue() < (Integer) other.getValue());
      case "|" -> new RuntimeInteger(getValue() | (Integer) other.getValue());
      case "&" -> new RuntimeInteger(getValue() & (Integer) other.getValue());
      case "^" -> new RuntimeInteger(getValue() ^ (Integer) other.getValue());
      case ">>" -> new RuntimeInteger(getValue() >> (Integer) other.getValue());
      case "<<" -> new RuntimeInteger(getValue() << (Integer) other.getValue());
      default ->
        throw new IllegalArgumentException("Cannot perform binary operation " + operation + " on " + getTypeName());
    };
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    return switch (operation) {
      case "~" -> new RuntimeInteger(~getValue());
      default ->
        throw new IllegalArgumentException("Cannot perform unary operation " + operation + " on " + getTypeName());
    };
  }
  public static RuntimeInteger of(Object o) {
    if (o instanceof Integer) {
      return new RuntimeInteger((Integer) o);
    } else if (o instanceof Float) {
      return new RuntimeInteger(((Float) o).intValue());
    } else if (o instanceof Boolean) {
      return new RuntimeInteger(((Boolean) o) ? 1 : 0);
    } else {
      throw new IllegalArgumentException("Cannot convert " + o.getClass().getSimpleName() + " to Integer");
    }
  }
}