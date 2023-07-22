package dev.cernavskis.moose.interpreter.types;

public class RuntimeVoid extends RuntimeType<Void> {
  public static final RuntimeVoid SINGLETON = new RuntimeVoid();
  private RuntimeVoid() {
    super(null);
  }

  @Override
  public String getTypeName() {
    return "void";
  }

  @Override
  public Void getValue() {
    throw new IllegalStateException("Cannot get value of void");
  }

  @Override
  public void setValue(Void value) {
    throw new IllegalStateException("Cannot set value of void");
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
  public boolean isAssignable() {
    return false;
  }
}
