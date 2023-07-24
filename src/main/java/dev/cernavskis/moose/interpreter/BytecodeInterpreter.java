package dev.cernavskis.moose.interpreter;

import dev.cernavskis.moose.interpreter.types.RuntimeFunction;
import dev.cernavskis.moose.interpreter.types.RuntimePointer;
import dev.cernavskis.moose.interpreter.types.RuntimeType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BytecodeInterpreter {
  private final String[] bytecode;
  private final List<RuntimeType<?>> memory = new LinkedList<>();
  private final Map<String, RuntimeType<?>> variables = new HashMap<>();
  private final Map<String, Integer> labels = new HashMap<>();
  private RuntimeType<?> register1 = null;
  private RuntimeType<?> register2 = null;
  private RuntimeType<?> buffer = null;

  public BytecodeInterpreter(String bytecode) {
    this.bytecode = bytecode.split("\n");
  }

  public void executeAll() {
    for (int i = 0; i < bytecode.length; i++) {
      String line = bytecode[i];
      if (line.startsWith("label ")) {
        String[] labelInstruction = line.split(" ");
        if (labelInstruction.length != 2) {
          throw new RuntimeException("Invalid label instruction: " + line);
        }
        labels.put(labelInstruction[1], i);
      }
    }
    int i = 0;
    while (i < bytecode.length) {
      try {
        String instruction = bytecode[i];
        if (instruction.startsWith("label ") || instruction.startsWith(";")) {
          i++;
          continue;
        }
        i = executeInstruction(instruction, i);
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Error on bytecode line " + (i + 1) + ": " + e.getMessage());
        break;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public int executeInstruction(String line, int index) throws InterpreterException {
    String[] lineParts = line.split(" ", 2);
    String instruction = lineParts[0];
    String[] args;
    if (lineParts.length > 1) {
      args = lineParts[1].split(" ", 2);
    } else {
      args = new String[0];
    }

    switch (instruction) {
      case "setb":
        String type = args[0];
        String value = args[1];

        Object convertedValue = RuntimeType.getStringConverter(type).apply(value);
        Function<Object, RuntimeType<?>> constructor = RuntimeType.getTypeConstructor(type);
        buffer = constructor.apply(convertedValue);
        break;
      case "getp":
        String name = args[0];
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        if (name.equals("@")) {
          buffer = new RuntimePointer<>(buffer);
        } else {
          buffer = buffer.getProperty(name);
        }
        break;
      case "setp1":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        if (!(buffer instanceof RuntimePointer)) {
          throw new InterpreterException("Buffer is not a pointer");
        }
        if (register1 == null) {
          throw new InterpreterException("Register 1 is empty");
        }
        if (((RuntimePointer<?>) buffer).getValue().getTypeName() != register1.getTypeName()) {
          throw new InterpreterException("Cannot assign " + register1.getTypeName() + " to " + ((RuntimePointer<?>) buffer).getValue().getTypeName());
        }
        ((RuntimePointer<Object>) buffer).getValue().setValue(register1.getValue());
        break;
      case "setp2":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        if (!(buffer instanceof RuntimePointer)) {
          throw new InterpreterException("Buffer is not a pointer");
        }
        if (register2 == null) {
          throw new InterpreterException("Register 1 is empty");
        }
        if (((RuntimePointer<?>) buffer).getValue().getTypeName() != register2.getTypeName()) {
          throw new InterpreterException("Cannot assign " + register2.getTypeName() + " to " + ((RuntimePointer<?>) buffer).getValue().getTypeName());
        }
        ((RuntimePointer<Object>) buffer).getValue().setValue(register2.getValue());
        break;
      case "setr1":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        register1 = buffer;
        break;
      case "setr2":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        register2 = buffer;
        break;
      case "getr1":
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        buffer = register1;
        break;
      case "getr2":
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        buffer = register2;
        break;
      case "clearr1":
        if (register1 == null) {
          throw new InterpreterException("Register 1 is empty");
        }
        register1 = null;
        break;
      case "clearr2":
        if (register2 == null) {
          throw new InterpreterException("Register 2 is empty");
        }
        register2 = null;
        break;
      case "clearb":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        buffer = null;
        break;
      case "crsetv":
        String varName0 = args[0];
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        if (variables.containsKey(varName0)) {
          throw new InterpreterException("Variable " + varName0 + " already exists");
        }
        variables.put(varName0, buffer);
        break;
      case "createv":
        String varName = args[0];
        if (variables.containsKey(varName)) {
          throw new InterpreterException("Variable already exists: " + varName);
        }
        String vtype = args[0];
        String vname = args[1];
        RuntimeType<?> def = RuntimeType.getDefaultOf(vtype);
        variables.put(vname, def);
        break;
      case "setv":
        String varName1 = args[0];
        if (!variables.containsKey(varName1)) {
          throw new InterpreterException("Variable does not exist: " + varName1);
        }
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        if (!variables.get(varName1).getTypeName().equals(buffer.getTypeName())) {
          throw new InterpreterException("Cannot assign " + buffer.getTypeName() + " to " + variables.get(varName1).getTypeName());
        }
        ((RuntimeType<Object>) variables.get(varName1)).setValue(buffer.getValue());
        break;
      case "setc":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        buffer.setConstant(true);
        break;
      case "loadv":
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        String varName3 = args[0];
        if (!variables.containsKey(varName3)) {
          throw new InterpreterException("Variable does not exist: " + varName3);
        }
        buffer = variables.get(varName3);
        break;
      case "clearv":
        String varName4 = args[0];
        if (!variables.containsKey(varName4)) {
          throw new InterpreterException("Variable does not exist: " + varName4);
        }
        variables.remove(varName4);
        break;
      case "pushm":
        if (buffer == null) {
          throw new InterpreterException("Buffer is empty");
        }
        memory.add(buffer);
        break;
      case "popm":
        if (memory.isEmpty()) {
          throw new InterpreterException("Memory is empty");
        }
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        buffer = memory.remove(memory.size() - 1);
        break;
      case "call":
        String callableName = args[0];
        if (!variables.containsKey(callableName)) {
          throw new InterpreterException("Callable does not exist: " + callableName);
        }
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        int argCount = Integer.parseInt(args[1]);
        if (memory.size() < argCount) {
          throw new InterpreterException("Not enough arguments on memory");
        }
        RuntimeFunction callable = (RuntimeFunction) variables.get(callableName);
        RuntimeType<?>[] argsArray = new RuntimeType<?>[argCount];
        for (int i = argCount - 1; i >= 0; i--) {
          argsArray[i] = memory.remove(memory.size() - 1);
        }
        buffer = callable.call(argsArray);
        break;
      case "op":
        String op = args[0];
        if (buffer != null) {
          throw new InterpreterException("Buffer is not empty");
        }
        if (register1 == null) {
          throw new InterpreterException("Register 1 is empty");
        }
        if (op.equals("!") || op.equals("~")) {
          buffer = register1.performUnaryOperation(op);
        } else {
          if (register2 == null) {
            throw new InterpreterException("Register 2 is empty");
          }
          buffer = register1.performBinaryOperation(op, register2);
        }
        break;
      case "jmp":
      case "jmpz":
      case "jpnz":
        String label = args[0];
        if (!labels.containsKey(label)) {
          throw new InterpreterException("Label does not exist: " + label);
        }
        if (instruction.equals("jmpz") || instruction.equals("jpnz")) {
          if (buffer == null) {
            throw new InterpreterException("Buffer is empty");
          }
          if (!buffer.getTypeName().equals("bool")) {
            throw new InterpreterException("Buffer is not a boolean");
          }
          if (instruction.equals("jmpz") && (boolean) buffer.getValue()) {
            break;
          }
          if (instruction.equals("jpnz") && !(boolean) buffer.getValue()) {
            break;
          }
        }
        return labels.get(label);
      default:
        throw new InterpreterException("Unknown instruction: " + instruction);
    }

    return index + 1;
  }

  public void setVariable(String name, RuntimeType<?> value) {
    variables.put(name, value);
  }
}
