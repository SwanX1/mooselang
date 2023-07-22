package dev.cernavskis.moose;

import dev.cernavskis.moose.compiler.Bytecoder;
import dev.cernavskis.moose.interpreter.BytecodeInterpreter;
import dev.cernavskis.moose.interpreter.types.RuntimeFunction;
import dev.cernavskis.moose.parser.statement.BlockStatement;
import dev.cernavskis.moose.parser.Parser;
import dev.cernavskis.moose.lexer.Lexer;

import java.io.*;

public class Main {

  public static void main(String[] args) {
    try {
      exec(compile(args[0]));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void exec(String bytecode) throws Exception {
    BytecodeInterpreter interpreter = new BytecodeInterpreter(bytecode);
    interpreter.setVariable("print", new RuntimeFunction((args) -> {
      for (int i = 0; i < args.length; i++) {
        System.out.print(args[i].getValue());
        if (i != args.length - 1) {
          System.out.print(" ");
        }
      }
      System.out.println();
      return null;
    }));
    interpreter.executeAll();
  }

  public static String compile(String inFilename) throws Exception {
    StringBuilder content = new StringBuilder();

    File inFile = new File(inFilename);
    try (InputStream inStream = inFile.toURI().toURL().openStream()) {
      int c;
      while ((c = inStream.read()) != -1) {
        content.append((char) c);
      }
    }

    Lexer lexer = new Lexer(content.toString());
    Parser parser = new Parser(lexer);
    BlockStatement statement = parser.parse();
    String bytecode = Bytecoder.compile(statement);
    return bytecode;
  }
}