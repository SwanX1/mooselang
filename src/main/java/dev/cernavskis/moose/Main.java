package dev.cernavskis.moose;

import dev.cernavskis.moose.compiler.Bytecoder;
import dev.cernavskis.moose.interpreter.BytecodeInterpreter;
import dev.cernavskis.moose.interpreter.types.RuntimeFunction;
import dev.cernavskis.moose.lexer.Token;
import dev.cernavskis.moose.parser.statement.BlockStatement;
import dev.cernavskis.moose.parser.Parser;
import dev.cernavskis.moose.lexer.Lexer;

import java.io.*;
import java.util.List;

public class Main {
  private static void tryWrite(String filename, String content) {
    try {
      File file = new File(filename);
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      FileWriter writer = new FileWriter(file);
      writer.write(content);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      String compiled = compile(args[0]);
      tryWrite("out.mses", compiled);
      exec(compiled);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void exec(String bytecode) throws Exception {
    BytecodeInterpreter interpreter = new BytecodeInterpreter(bytecode);
    interpreter.setVariable("print", new RuntimeFunction((args) -> {
      for (int i = 0; i < args.length; i++) {
        System.out.print(args[i].toString());
        if (i != args.length - 1) {
          System.out.print(" ");
        }
      }
      System.out.println();
      return null;
    }));

    long start = System.nanoTime();
    interpreter.executeAll();
    long end = System.nanoTime();

    System.out.println("Execution took " + ((float)(end - start)) / 1000000 + "ms");
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

    long start;
    long lexTime;
    long parseTime;
    long compileTime;

    start = System.nanoTime();
    Lexer lexer = new Lexer(content.toString(), inFilename);
    List<Token> tokens = lexer.getAllTokens();
    lexTime = System.nanoTime() - start;

    start = System.nanoTime();
    Parser parser = new Parser(tokens);
    BlockStatement statement = parser.parse();
    parseTime = System.nanoTime() - start;

    start = System.nanoTime();
    String bytecode = Bytecoder.compile(statement);
    compileTime = System.nanoTime() - start;

    System.out.println("Lexed in " + ((float)lexTime) / 1000000 + "ms");
    System.out.println("Parsed in " + ((float)parseTime) / 1000000 + "ms");
    System.out.println("Compiled in " + ((float)compileTime) / 1000000 + "ms");
    System.out.println("Everything took " + ((float)(lexTime + parseTime + compileTime)) / 1000000 + "ms");
    System.out.println("");

    return bytecode;
  }
}