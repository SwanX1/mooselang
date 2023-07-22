package dev.cernavskis.moose.interpreter.types;

public class RuntimeFloat extends RuntimeType<Float> {

  public RuntimeFloat(Float value) {
    super(value);
  }

  @Override
  public String getTypeName() {
    return "float";
  }
  @Override
  public RuntimeType<?> performBinaryOperation(String operation, RuntimeType<?> other) throws IllegalArgumentException {
    if (!"float".equals(other.getTypeName())) {
      throw new IllegalArgumentException("Cannot perform binary operation on " + getTypeName() + " and " + other.getTypeName());
    }
    return switch (operation) {
      case "+" -> new RuntimeFloat(getValue() + (Float) other.getValue());
      case "-" -> new RuntimeFloat(getValue() - (Float) other.getValue());
      case "*" -> new RuntimeFloat(getValue() * (Float) other.getValue());
      case "/" -> new RuntimeFloat(getValue() / (Float) other.getValue());
      case "%" -> new RuntimeFloat(getValue() % (Float) other.getValue());
      case "**" -> new RuntimeFloat((float) Math.pow(getValue(), (Float) other.getValue()));
      case "==" -> new RuntimeBoolean(getValue() == (Float) other.getValue());
      case "!=" -> new RuntimeBoolean(getValue() != (Float) other.getValue());
      case ">=" -> new RuntimeBoolean(getValue() >= (Float) other.getValue());
      case "<=" -> new RuntimeBoolean(getValue() <= (Float) other.getValue());
      case ">" -> new RuntimeBoolean(getValue() > (Float) other.getValue());
      case "<" -> new RuntimeBoolean(getValue() < (Float) other.getValue());
      default ->
        throw new IllegalArgumentException("Cannot perform binary operation " + operation + " on " + getTypeName());
    };
  }

  @Override
  public RuntimeType<?> performUnaryOperation(String operation) throws IllegalArgumentException {
    throw new IllegalArgumentException("Cannot perform unary operation " + operation + " on " + getTypeName());
  }

  public static RuntimeFloat of(Object o) {
    if (o instanceof Float) {
      return new RuntimeFloat((float) o);
    } else if (o instanceof Integer) {
      return new RuntimeFloat((float) o);
    } else if (o instanceof Boolean) {
      return new RuntimeFloat((boolean) o ? 1.0f : 0.0f);
    } else {
      throw new IllegalArgumentException("Cannot cast " + o + " to float");
    }
  }
}
