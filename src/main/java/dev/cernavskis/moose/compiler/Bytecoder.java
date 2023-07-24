package dev.cernavskis.moose.compiler;

import dev.cernavskis.moose.lexer.TokenType;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.parser.statement.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// setb [type] [value] - sets a constant value to the buffer, type is needed to parse it correctly from bytecode
// getp [name] - gets pointer to a property of the current buffer value
// setp1 - when the buffer is a pointer, sets the value of the pointer from register 1
// setp2 - when the buffer is a pointer, sets the value of the pointer from register 2
// setr1 - sets the value of register 1 from the buffer
// setr2 - sets the value of register 2 from the buffer
// getr1 - gets the value of register 1 to the buffer
// getr2 - gets the value of register 2 to the buffer
// clearr1 - clears the value of register 1
// clearr2 - clears the value of register 2
// clearb - clears the buffer
// createv [type] [name] - creates a variable with the given name and type
// setv [name] - sets variable to the buffer value
// crsetv [name] - creates a variable with the given name, its type is inferred from the buffer value
// setc - sets buffer value to be constant, variable cannot be changed again
// loadv [name] - sets buffer from the variable value
// clearv [name] - destroys a variable
// pushm - pushes a value from the buffer to memory
// popm - discards the last value from the memory
// op [operator] - performs an operation on the register 1 and register 2 values, sets the result to the buffer. if operator is unary, only register 1 is used
// call [name] [arg_amount] - calls a function
// jmp [label] - jumps to the label
// jmpz [label] - jumps to the label if the buffer is zero
// jpnz [label] - jumps to the label if the buffer is not zero
// label [name] - defines a label
public class Bytecoder {
  public static class State {
    private final Set<Integer> usedVariables = new HashSet<>();
    private int lastLabel = 0;

    public int getLabel() {
      return this.lastLabel++;
    }

    public int getTempVariable() {
      int i = 0;
      while (usedVariables.contains(i)) {
        i++;
      }
      usedVariables.add(i);
      return i;
    }

    public void freeVariable(int i) {
      usedVariables.remove(i);
    }
  }

  public static String compile(Statement statement) {
    return compileStatement(statement, new State()).toString();
  }

  private static StatementBytecode compileStatement(Statement statement, State state) {
    StringBuilder result = new StringBuilder();
    boolean bufferFilled = false;

    if (statement instanceof BlockStatement block) {
      List<String> cleanup = new ArrayList<>();
      for (Statement child : block.statements()) {
        StatementBytecode childResult = compileStatement(child, state);
        result.append(childResult.code());
        if (childResult.shouldClearBuffer()) {
          result.append("clearb\n");
        }

        if (child instanceof DeclarationStatement declaration) {
          cleanup.add(declaration.name());
        }
      }
      for (String name : cleanup) {
        result.append("clearv ").append(name).append("\n");
      }
    } else if (statement instanceof DeclarationStatement declaration) {
      result.append("createv ").append(declaration.type()).append(" ").append(declaration.name()).append("\n");
      Statement value = declaration.value();
      if (value != null) {
        String valueResult = compileStatement(value, state).code();
        result.append(valueResult);
        if (declaration.isConst()) {
          result.append("setc\n");
        }
        result.append("setv ").append(declaration.name()).append("\n");
        result.append("clearb\n");
      }
    } else if (statement instanceof FunctionCallStatement functionCall) {
      List<Integer> args = new ArrayList<>(functionCall.arguments().size());

      for (Statement argument : functionCall.arguments()) {
        String argumentResult = compileStatement(argument, state).code();

        result.append(argumentResult);
        int tempVariable = state.getTempVariable();
        args.add(tempVariable);
        result.append("crsetv $" + tempVariable + "\n");
        result.append("clearb\n");
      }

      String callableResult = compileStatement(functionCall.callable(), state).code();
      result.append(callableResult);
      int tempCallable = state.getTempVariable();
      result.append("crsetv $").append(tempCallable).append("\n");
      result.append("clearb\n");

      for (int tempArgument : args) {
        result.append("loadv $").append(tempArgument).append("\n");
        result.append("clearv $").append(tempArgument).append("\n");
        state.freeVariable(tempArgument);
        result.append("pushm\n");
        result.append("clearb\n");
      }

      result.append("call $").append(tempCallable).append(" ").append(functionCall.arguments().size()).append("\n");
      result.append("clearv $").append(tempCallable).append("\n");
      state.freeVariable(tempCallable);
      bufferFilled = true;
    } else if (statement instanceof StringStatement string) {
      StringBuilder value = new StringBuilder();
      for (char c : string.value().toCharArray()) {
        value.append(switch (c) {
          case '\n' -> "\\n";
          case '\t' -> "\\t";
          case '\"' -> "\\\"";
          case '\\' -> "\\\\";
          default -> c;
        });
      }
      result.append("setb string ").append(value).append("\n");
      bufferFilled = true;
    } else if (statement instanceof NumberStatement number) {
      boolean isFloat = number.value().contains(".");
      String value;
      result.append("setb ");
      if (isFloat) {
        value = String.valueOf(Float.parseFloat(number.value()));
        result.append("float ");
      } else {
        value = number.value();
        int radix = 10;
        if (value.startsWith("0x")) {
          radix = 16;
          value = value.substring(2);
        } else if (value.startsWith("0b")) {
          radix = 2;
          value = value.substring(2);
        }
        value = String.valueOf(Integer.parseInt(value, radix));
        result.append("int ");
      }
      result.append(value).append("\n");
      bufferFilled = true;
    } else if (statement instanceof VariableStatement variable) {
      result.append("loadv ").append(variable.value()).append("\n");
      bufferFilled = true;
    } else if (statement instanceof AssignmentStatement assignment) {
      int tempVariable = state.getTempVariable();
      String assignableResult = compileStatement(assignment.qualifiedName(), state).code();
      String valueResult = compileStatement(assignment.value(), state).code();
      result.append(assignableResult);
      result.append("getp @\n");
      result.append("crsetv $").append(tempVariable).append("\n");
      result.append("clearb\n");
      result.append(valueResult);
      result.append("setr1\n");
      result.append("clearb\n");
      result.append("loadv $").append(tempVariable).append("\n");
      result.append("setp1\n");
      result.append("clearv $").append(tempVariable).append("\n");
      result.append("clearr1\n");
      state.freeVariable(tempVariable);
      bufferFilled = true;
    } else if (statement instanceof BinaryExpression binaryExpression) {
      result.append(compileStatement(binaryExpression.left(), state));
      result.append("setr1\nclearb\n");
      result.append(compileStatement(binaryExpression.right(), state));
      result.append("setr2\nclearb\n");
      result.append("op ").append(binaryExpression.operator()).append("\n");
      result.append("clearr1\nclearr2\n");
      bufferFilled = true;
    } else if (statement instanceof UnaryExpression unaryExpression) {
      int temp = state.getTempVariable();
      switch (unaryExpression.operator()) {
//        case PREINCREMENT:
//          result.append(compileStatement(
//            new BinaryExpression(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              "+",
//              new NumberStatement(unaryExpression.debugInfo(), "1")
//            ), state
//          ));
//
//          result.append("crsetv $").append(temp).append("\n");
//          result.append("clearb\n");
//
//          result.append(compileStatement(
//            new AssignmentStatement(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              new VariableStatement(unaryExpression.debugInfo(), "$" + temp)
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append("loadv $").append(temp).append("\n");
//          result.append("clearv $").append(temp).append("\n");
//          break;
//        case PREDECREMENT:
//          result.append(compileStatement(
//            new BinaryExpression(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              "-",
//              new NumberStatement(unaryExpression.debugInfo(), "1")
//            ), state
//          ));
//
//          result.append("crsetv $").append(temp).append("\n");
//          result.append("clearb\n");
//
//          result.append(compileStatement(
//            new AssignmentStatement(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              new VariableStatement(unaryExpression.debugInfo(), "$" + temp)
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append("loadv $").append(temp).append("\n");
//          result.append("clearv $").append(temp).append("\n");
//          break;
//        case POSTINCREMENT:
//          result.append(compileStatement(unaryExpression.value(), state));
//          result.append("crsetv $").append(temp).append("\n");
//          result.append("clearb\n");
//          result.append(compileStatement(
//            new BinaryExpression(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              "+",
//              new NumberStatement(unaryExpression.debugInfo(), "1")
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append(compileStatement(
//            new AssignmentStatement(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              new VariableStatement(unaryExpression.debugInfo(), "$" + temp)
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append("loadv $").append(temp).append("\n");
//          result.append("clearv $").append(temp).append("\n");
//          break;
//        case POSTDECREMENT:
//          result.append(compileStatement(unaryExpression.value(), state));
//          result.append("crsetv $").append(temp).append("\n");
//          result.append("clearb\n");
//          result.append(compileStatement(
//            new BinaryExpression(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              "-",
//              new NumberStatement(unaryExpression.debugInfo(), "1")
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append(compileStatement(
//            new AssignmentStatement(
//              unaryExpression.debugInfo(),
//              unaryExpression.value(),
//              new VariableStatement(unaryExpression.debugInfo(), "$" + temp)
//            ), state
//          ));
//          result.append("clearb\n");
//
//          result.append("loadv $").append(temp).append("\n");
//          result.append("clearv $").append(temp).append("\n");
//          break;
        case PREINCREMENT:
        case PREDECREMENT:
        case POSTDECREMENT:
        case POSTINCREMENT:
          throw new UnsupportedOperationException(unaryExpression.operator() + " iz not wowking yet 3: *sad uwu*");
        case LOGICAL_NOT:
        case BIT_NOT:
          char operator;
          if (unaryExpression.operator() == TokenType.BIT_NOT) {
            operator = '~';
          } else {
            operator = '!';
          }
          result.append(compileStatement(unaryExpression.value(), state));
          result.append("setr1\n");
          result.append("clearb\n");
          result.append("op ").append(operator).append("\n");
          result.append("clearr1\n");
          break;
        default:
          throw new RuntimeException("Unknown unary operator: " + unaryExpression.operator());
      }
      state.freeVariable(temp);
      bufferFilled = true;
    } else if (statement instanceof TernaryExpression ternary) {
      int labelFalse = state.getLabel();
      int labelEnd = state.getLabel();
      result.append(compileStatement(ternary.condition(), state));
      result.append("jmpz $").append(labelFalse).append("\n");

      result.append("clearb\n");
      result.append(compileStatement(ternary.trueValue(), state));
      result.append("jmp $").append(labelEnd).append("\n");

      result.append("label $").append(labelFalse).append("\n");
      result.append("clearb\n");
      result.append(compileStatement(ternary.falseValue(), state));
      result.append("label $").append(labelEnd).append("\n");
      bufferFilled = true;
    } else if (statement instanceof LiterallyDontCareStatement asm) {
      result.append(asm.code()).append("\n");
    } else {
      throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
    }

    return new StatementBytecode(result.toString(), bufferFilled);
  }
}
