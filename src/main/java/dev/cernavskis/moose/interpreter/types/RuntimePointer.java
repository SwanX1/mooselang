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
    throw new IllegalArgumentException("Cannot perform binary operation " + operation + " on " + getTypeName());
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform unary operation " + operation + " on " + getTypeName());
  }
}
