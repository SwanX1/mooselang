package dev.cernavskis.moose.interpreter.types;

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
    int otherValue = (Integer) other.getValue();
    return switch (operation) {
      case "+" -> new RuntimeInteger(getValue() + otherValue);
      case "-" -> new RuntimeInteger(getValue() - otherValue);
      case "*" -> new RuntimeInteger(getValue() * otherValue);
      case "/" -> new RuntimeInteger(getValue() / otherValue);
      case "%" -> new RuntimeInteger(getValue() % otherValue);
      case "**" -> new RuntimeInteger((int) Math.pow(getValue(), otherValue));
      case "==" -> new RuntimeBoolean(getValue() == otherValue);
      case "!=" -> new RuntimeBoolean(getValue() != otherValue);
      case ">=" -> new RuntimeBoolean(getValue() >= otherValue);
      case "<=" -> new RuntimeBoolean(getValue() <= otherValue);
      case ">" -> new RuntimeBoolean(getValue() > otherValue);
      case "<" -> new RuntimeBoolean(getValue() < otherValue);
      case "|" -> new RuntimeInteger(getValue() | otherValue);
      case "&" -> new RuntimeInteger(getValue() & otherValue);
      case "^" -> new RuntimeInteger(getValue() ^ otherValue);
      case ">>" -> new RuntimeInteger(getValue() >> otherValue);
      case "<<" -> new RuntimeInteger(getValue() << otherValue);
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
