package dev.cernavskis.moose.interpreter.types;

public class RuntimePointer<T> extends RuntimeType<RuntimeType<T>> {
  public RuntimePointer(RuntimeType<T> value) {
    super(value);
  }

  @Override
  public String getTypeName() {
    return "*" + getValue().getTypeName();
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    while (other instanceof RuntimePointer) {
      other = ((RuntimePointer<?>) other).getValue();
    }
    return getValue().performBinaryOperation(operation, other);
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    return getValue().performUnaryOperation(operation);
  }
}
