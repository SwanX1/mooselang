package dev.cernavskis.moose;

import dev.cernavskis.moose.compiler.Bytecoder;
import dev.cernavskis.moose.interpreter.BytecodeInterpreter;
import dev.cernavskis.moose.interpreter.types.RuntimeFunction;
import dev.cernavskis.moose.lexer.Token;
import dev.cernavskis.moose.parser.Statement;
import dev.cernavskis.moose.parser.statement.BlockStatement;
import dev.cernavskis.moose.parser.Parser;
import dev.cernavskis.moose.lexer.Lexer;

import java.io.*;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    try {
      String compiled = compile(args[0]);
//      File out = new File("out.mseb");
//      if (out.exists()) {
//        out.delete();
//      }
//      out.createNewFile();
//      FileWriter writer = new FileWriter(out);
//      writer.write(compiled);
//      writer.close();
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

//    for (Statement s : statement.statements()) {
//      System.out.println(s);
//    }
    start = System.nanoTime();
    String bytecode = Bytecoder.compile(statement);
    compileTime = System.nanoTime() - start;

//    System.out.println("Lexed in " + ((float)lexTime) / 1000000 + "ms");
//    System.out.println("Parsed in " + ((float)parseTime) / 1000000 + "ms");
//    System.out.println("Compiled in " + ((float)compileTime) / 1000000 + "ms");
//    System.out.println("Everything took " + ((float)(lexTime + parseTime + compileTime)) / 1000000 + "ms");

    return bytecode;
  }
}