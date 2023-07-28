package dev.cernavskis.moose.interpreter.types;

import dev.cernavskis.moose.interpreter.InterpreterException;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Represents a Moose runtime type and value.
 * @param <T> The Java type of the value.
 */
public abstract class RuntimeType<T> {
  private T value;
  private boolean isConstant = false;

  public RuntimeType(T value) {
    this.value = value;
  }

  /**
   * Returns the name of the type. This is what is used in the Moose language.
   */
  public abstract String getTypeName();

  /**
   * If the type is constant
   */
  public void setConstant(boolean constant) {
    isConstant = constant;
  }

  public RuntimePointer<?> getProperty(String name) throws InterpreterException {
    throw new InterpreterException("Cannot get property " + name + " of " + getTypeName());
  }

  /**
   * Returns the Java value of the type.
   */
  public T getValue() {
    return value;
  }

  /**
   * Sets the Java value of the type.
   */
  public void setValue(T value) {
    if (isConstant) {
      throw new UnsupportedOperationException("Cannot set value of constant");
    }
    this.value = value;
  }

  /**
   * Performs a binary operation on this value and another.
   * @return The result of the operation.
   */
  public abstract RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException;

  /**
   * Performs a unary operation on this value.
   * @return The result of the operation.
   */
  public abstract RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException;

  /**
   * If the value is callable, calls it with the given arguments.
   * @return The result of the call.
   */
  public RuntimeType<?> call(RuntimeType<?>[] args) throws IllegalArgumentException {
    throw new IllegalArgumentException(getTypeName() + " is not callable");
  }

  /**
   * Checks if this type is an assignable type.
   * For example, void is not, and you cannot use it as a variable type.
   * @return true if this type is a value-bearing type, false otherwise
   */
  public boolean isAssignable() {
    return true;
  }

  public RuntimePointer<T> getPointer() {
    return new RuntimePointer<>(this);
  }

  @Override
  public String toString() {
    return getValue().toString();
  }

  /**
   * Returns the constructor for this type
   */
  public static Function<Object, RuntimeType<?>> getTypeConstructor(String type) {
    return switch (type) {
      case "bool" -> RuntimeBoolean::of;
      case "int" -> RuntimeInteger::of;
      case "float" -> RuntimeFloat::of;
      case "string" -> RuntimeString::of;
      case "void" -> (o) -> RuntimeVoid.SINGLETON;
      case "func" -> RuntimeFunction::of;
      case "array" -> (o) -> new RuntimeArray<>(new ArrayList<>(), null);
      default -> throw new IllegalArgumentException("Unknown type " + type);
    };
  }

  /**
   * Returns string converter for type. Used for executing bytecode.
   */
  public static Function<String, Object> getStringConverter(String type) {
    return switch (type) {
      case "bool" -> Boolean::valueOf;
      case "int" -> Integer::valueOf;
      case "float" -> Float::valueOf;
      case "string" -> (s) -> {
        StringBuilder valueBuilder = new StringBuilder();
        boolean escape = false;
        for (char c : s.toCharArray()) {
          if (escape) {
            switch (c) {
              case 'n':
                valueBuilder.append('\n');
                break;
              case 't':
                valueBuilder.append('\t');
                break;
              case '\\':
                valueBuilder.append('\\');
                break;
              case '"':
                valueBuilder.append('"');
                break;
              default:
                throw new IllegalArgumentException("Unknown escape sequence: \\" + c);
            }
            escape = false;
          } else {
            if (c == '\\') {
              escape = true;
            } else {
              valueBuilder.append(c);
            }
          }
        }
        return valueBuilder.toString();
      };
      case "array" -> (s) -> new ArrayList<>(Integer.parseInt(s));
      case "void", "func" -> throw new IllegalArgumentException(type + "cannot be converted from string");
      default -> throw new IllegalArgumentException("Unknown type " + type);
    };
  }

  /**
   * Returns the default value for the given Java type.
   */
  @SuppressWarnings("unchecked")
  public static RuntimeType<?> getDefaultOf(String type) {
    return switch (type) {
      case "bool" -> new RuntimeBoolean(false);
      case "int" -> new RuntimeInteger(0);
      case "float" -> new RuntimeFloat(0.0F);
      case "string" -> new RuntimeString("");
      case "void" -> RuntimeVoid.SINGLETON;
      case "func" -> new RuntimeFunction((args) -> null);
      default -> {
        if (type.endsWith("[]")) {
          String innerType = type.substring(0, type.length() - 2);
          yield new RuntimeArray<>(new ArrayList<>(), innerType);
        } else {
          throw new IllegalArgumentException("Unknown type " + type);
        }
      }
    };
  }
}
