package dev.cernavskis.moose.compiler;

import dev.cernavskis.moose.lexer.TokenType;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.parser.statement.*;
import dev.cernavskis.moose.util.Nullable;

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

    @Nullable
    public String lastContinueLabel = null;
    @Nullable
    public String lastBreakLabel = null;

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
    result.append("@")
      .append(statement.debugInfo().line()).append(",")
      .append(statement.debugInfo().column()).append(",")
      .append(statement.debugInfo().file()).append("\n");
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
      int r1 = state.getTempVariable();
      result.append("crsetv $").append(r1).append("\n");
      result.append("clearb\n");
      result.append(compileStatement(binaryExpression.right(), state));
      result.append("setr2\nclearb\n");
      result.append("loadv $").append(r1).append("\n");
      result.append("setr1\nclearb\n");
      result.append("op ").append(binaryExpression.operator()).append("\n");
      result.append("clearr1\nclearr2\n");
      result.append("clearv $").append(r1).append("\n");
      state.freeVariable(r1);
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
    } else if (statement instanceof PropertyAccessStatement propAccess) {
      result.append(compileStatement(propAccess.parent(), state));
      result.append("getp ").append(propAccess.property()).append("\n");
      bufferFilled = true;
    } else if (statement instanceof ArrayAccessStatement arrayAccess) {
      result.append(compileStatement(arrayAccess.parent(), state));
      int parent = state.getTempVariable();
      result.append("crsetv $").append(parent).append("\n");
      result.append("clearb\n");
      result.append(compileStatement(arrayAccess.index(), state));
      result.append("setr2\n");
      result.append("clearb\n");
      result.append("loadv $").append(parent).append("\n");
      result.append("clearv $").append(parent).append("\n");
      result.append("setr1\n");
      result.append("clearb\n");
      state.freeVariable(parent);
      result.append("op [\n");
      result.append("clearr1\n");
    } else if (statement instanceof ArrayStatement array) {
      for (Statement value : array.elements()) {
        result.append(compileStatement(value, state));
        result.append("pushm\n");
        result.append("clearb\n");
      }
      result.append("setb array ").append(array.elements().size()).append("\n");
      result.append("apush ").append(array.elements().size()).append("\n");
    } else if (statement instanceof IfStatement ifStatement) {
      boolean hasElse = ifStatement.elseBranch() != null;
      int elseLabel = -1;
      if (hasElse) {
        elseLabel = state.getLabel();
      }
      int endLabel = state.getLabel();

      result.append(compileStatement(ifStatement.condition(), state));
      if (hasElse) {
        result.append("jmpz $").append(elseLabel).append("\n");
      } else {
        result.append("jmpz $").append(endLabel).append("\n");
      }
      result.append("clearb\n");
      result.append(compileStatement(ifStatement.thenBranch(), state));
      result.append("jmp $").append(endLabel).append("\n");
      if (hasElse) {
        result.append("label $").append(elseLabel).append("\n");
        result.append("clearb\n");
        result.append(compileStatement(ifStatement.elseBranch(), state));
      }
      result.append("label $").append(endLabel).append("\n");
      result.append("clearbe\n");
    } else if (statement instanceof ForStatement forStatement) {
      int startLabel = state.getLabel();
      int continueLabel = state.getLabel();
      int endLabel = state.getLabel();

      StringBuilder cleanup = new StringBuilder();

      if (forStatement.initializer() != null) {
        StatementBytecode initializer = compileStatement(forStatement.initializer(), state);
        if (forStatement.initializer() instanceof DeclarationStatement declarationStatement) {
          cleanup.append("clearv ").append(declarationStatement.name()).append("\n");
        }
        result.append(initializer);
        if (initializer.shouldClearBuffer()) {
          result.append("clearb\n");
        }
      }

      String previousContinue = state.lastContinueLabel;
      String previousEnd = state.lastBreakLabel;

      state.lastContinueLabel = "$" + continueLabel;
      state.lastBreakLabel = "$" + endLabel;

      result.append("label $").append(startLabel).append("\n");
      result.append(compileStatement(forStatement.condition(), state));
      result.append("jmpz $").append(endLabel).append("\n");
      result.append("clearb\n");
      result.append(compileStatement(forStatement.body(), state));
      if (forStatement.body() instanceof DeclarationStatement declarationStatement) {
        cleanup.append("clearv ").append(declarationStatement.name()).append("\n");
      }
      result.append("label $").append(continueLabel).append("\n");
      if (forStatement.increment() != null) {
        StatementBytecode increment = compileStatement(forStatement.increment(), state);
        result.append(increment);
        if (increment.shouldClearBuffer()) {
          result.append("clearb\n");
        }
      }
      result.append("jmp $").append(startLabel).append("\n");
      result.append("label $").append(endLabel).append("\n");
      result.append(cleanup);

      state.lastContinueLabel = previousContinue;
      state.lastBreakLabel = previousEnd;
    } else if (statement instanceof WhileStatement whileStatement) {
      int startLabel = state.getLabel();
      int endLabel = state.getLabel();

      String previousContinue = state.lastContinueLabel;
      String previousEnd = state.lastBreakLabel;

      state.lastContinueLabel = "$" + startLabel;
      state.lastBreakLabel = "$" + endLabel;

      result.append("label $").append(startLabel).append("\n");
      result.append(compileStatement(whileStatement.condition(), state));
      result.append("jmpz $").append(endLabel).append("\n");
      result.append("clearb\n");
      result.append(compileStatement(whileStatement.body(), state));
      if (whileStatement.body() instanceof DeclarationStatement declarationStatement) {
        result.append("clearv ").append(declarationStatement.name()).append("\n");
      }
      result.append("jmp $").append(startLabel).append("\n");
      result.append("label $").append(endLabel).append("\n");

      state.lastContinueLabel = previousContinue;
      state.lastBreakLabel = previousEnd;
    } else if (statement instanceof DoWhileStatement doWhileStatement) {
      int startLabel = state.getLabel();
      int noCheckStartLabel = state.getLabel();
      int endLabel = state.getLabel();

      String previousContinue = state.lastContinueLabel;
      String previousEnd = state.lastBreakLabel;

      state.lastContinueLabel = "$" + startLabel;
      state.lastBreakLabel = "$" + endLabel;
      
      result.append("jmp $").append(noCheckStartLabel).append("\n");
      result.append("label $").append(startLabel).append("\n");
      result.append("clearb\n");
      result.append("label $").append(noCheckStartLabel).append("\n");
      result.append(compileStatement(doWhileStatement.body(), state));
      if (doWhileStatement.body() instanceof DeclarationStatement declarationStatement) {
        result.append("clearv ").append(declarationStatement.name()).append("\n");
      }
      result.append(compileStatement(doWhileStatement.condition(), state));
      result.append("jpnz $").append(startLabel).append("\n");
      result.append("label $").append(endLabel).append("\n");

      state.lastContinueLabel = previousContinue;
      state.lastBreakLabel = previousEnd;
    } else if (statement instanceof LoopStatement loopStatement) {
      int startLabel = state.getLabel();
      int endLabel = state.getLabel();

      String previousContinue = state.lastContinueLabel;
      String previousEnd = state.lastBreakLabel;

      state.lastContinueLabel = "$" + startLabel;
      state.lastBreakLabel = "$" + endLabel;

      result.append("label $").append(startLabel).append("\n");
      result.append(compileStatement(loopStatement.body(), state));
      if (loopStatement.body() instanceof DeclarationStatement declarationStatement) {
        result.append("clearv ").append(declarationStatement.name()).append("\n");
      }
      result.append("jmp $").append(startLabel).append("\n");
      result.append("label $").append(endLabel).append("\n");

      state.lastContinueLabel = previousContinue;
      state.lastBreakLabel = previousEnd;
    } else if (statement instanceof BreakStatement breakStatement) {
      if (state.lastBreakLabel == null) {
        throw new CompilerException("Break statement outside of loop", breakStatement.debugInfo());
      }

      if (breakStatement.label() != null) {
        throw new CompilerException("Break statement labels are not supported yet", breakStatement.debugInfo());
        // result.append("jmp ").append(breakStatement.label()).append("\n");
      } else {
        result.append("jmp ").append(state.lastBreakLabel).append("\n");
      }
    } else if (statement instanceof ContinueStatement continueStatement) {
      if (state.lastContinueLabel == null) {
        throw new CompilerException("Continue statement outside of loop", continueStatement.debugInfo());
      }

      if (continueStatement.label() != null) {
        throw new CompilerException("Continue statement labels are not supported yet", continueStatement.debugInfo());
        // result.append("jmp ").append(continueStatement.label()).append("\n");
      } else {
        result.append("jmp ").append(state.lastContinueLabel).append("\n");
      }
    } else {
      throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
    }

    return new StatementBytecode(result.toString(), bufferFilled);
  }
}
