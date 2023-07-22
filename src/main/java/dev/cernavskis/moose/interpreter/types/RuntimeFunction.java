package dev.cernavskis.moose.interpreter.types;

import java.util.Objects;
import java.util.function.Function;

public class RuntimeFunction extends RuntimeType<RuntimeCallable> {
  public RuntimeFunction(RuntimeCallable value) {
    super(value);
  }

  public static RuntimeFunction of(Object o) {
    if (o instanceof Function) {
      return new RuntimeFunction((RuntimeCallable) o);
    } else {
      throw new IllegalArgumentException("Cannot create function from " + o);
    }
  }

  @Override
  public String getTypeName() {
    return "func";
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
  }

  @Override
  public RuntimeType<?> call(RuntimeType<?>[] args) throws IllegalArgumentException {
    RuntimeType<?> returnValue = getValue().call(args);
    return Objects.requireNonNullElse(returnValue, RuntimeVoid.SINGLETON);
  }
}
