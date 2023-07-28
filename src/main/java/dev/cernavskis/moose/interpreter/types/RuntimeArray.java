package dev.cernavskis.moose.interpreter.types;

import dev.cernavskis.moose.interpreter.InterpreterException;

import java.util.List;

public class RuntimeArray<T> extends RuntimeType<List<RuntimeType<T>>> {
  private String typeName;

  public RuntimeArray(List<RuntimeType<T>> value, String typeName) {
    super(value);
    this.typeName = typeName;
  }

  @Override
  public String getTypeName() {
    return typeName + "[]";
  }

  @Override
  public RuntimePointer<?> getProperty(String name) throws InterpreterException {
    if (name.equals("length")) {
      return new RuntimePointer<>(new RuntimeInteger(getValue().size()));
    }
    return super.getProperty(name);
  }

  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    if (operation.equals("[")) {
      if (!(other instanceof RuntimeInteger)) {
        throw new IllegalArgumentException("Cannot index array with " + other.getTypeName());
      }
      int index = ((RuntimeInteger) other).getValue();
      if (index < 0 || index >= getValue().size()) {
        throw new IllegalArgumentException("Index out of bounds");
      }
      return getValue().get(index);
    }
    throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform operation " + operation + " on " + getTypeName());
  }

  @SuppressWarnings("unchecked")
  public void setIndex(int i, RuntimeType<?> element) {
    if (typeName != null) {
      if (!typeName.equals(element.getTypeName())) {
        throw new IllegalArgumentException("Cannot set index " + i + " of " + getTypeName() + " to " + element.getTypeName());
      }
    } else {
      typeName = element.getTypeName();
    }

    getValue().set(i, (RuntimeType<T>) element);
  }

  public int getSize() {
    return getValue().size();
  }

  public void setSize(int size) {
    getValue().clear();
    for (int i = 0; i < size; i++) {
      getValue().add(null);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (int i = 0; i < getValue().size(); i++) {
      if (i != 0) {
        builder.append(", ");
      }
      builder.append(getValue().get(i));
    }
    builder.append("]");
    return builder.toString();
  }
}
