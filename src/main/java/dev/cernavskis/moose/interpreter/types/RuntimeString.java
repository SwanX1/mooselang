package dev.cernavskis.moose.interpreter.types;

public class RuntimeString extends RuntimeType<String> {
  public RuntimeString(String value) {
    super(value);
  }

  @Override
  public String getTypeName() {
    return "string";
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    return switch (operation) {
      case "+" -> new RuntimeString(getValue() + other.getValue());
      case "==" -> new RuntimeBoolean(getValue().equals(other.getValue()));
      default -> throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
    };
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
  }

  public static RuntimeString of(Object o) {
    return new RuntimeString(o.toString());
  }
}
