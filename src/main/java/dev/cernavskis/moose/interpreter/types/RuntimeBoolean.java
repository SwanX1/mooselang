package dev.cernavskis.moose.interpreter.types;

public class RuntimeBoolean extends RuntimeType<Boolean> {
  public RuntimeBoolean(Boolean value) {
    super(value);
  }

  @Override
  public String getTypeName() {
    return "bool";
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    if (!"bool".equals(other.getTypeName())) {
      throw new IllegalArgumentException("Cannot perform binary operation on " + getTypeName() + " and " + other.getTypeName());
    }
    return switch (operation) {
      case "&&" -> new RuntimeBoolean(getValue() && (Boolean) other.getValue());
      case "||" -> new RuntimeBoolean(getValue() || (Boolean) other.getValue());
      case "==" -> new RuntimeBoolean(getValue() == (Boolean) other.getValue());
      case "!=" -> new RuntimeBoolean(getValue() != (Boolean) other.getValue());
      default ->
        throw new IllegalArgumentException("Cannot perform binary operation " + operation + " on " + getTypeName());
    };
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    return switch (operation) {
      case "!" -> new RuntimeBoolean(!getValue());
      default ->
        throw new IllegalArgumentException("Cannot perform unary operation " + operation + " on " + getTypeName());
    };
  }

  public static RuntimeBoolean of(Object o) {
    if (o instanceof Boolean) {
      return new RuntimeBoolean((boolean) o);
    } else if (o instanceof Integer) {
      return new RuntimeBoolean((int) o != 0);
    } else if (o instanceof Float) {
      return new RuntimeBoolean((float) o != 0.0);
    } else {
      throw new IllegalArgumentException("Cannot cast " + o + " to boolean");
    }
  }
}
