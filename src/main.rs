mod lexer;
mod parser;
mod compiler;
mod interpreter;
mod util;

use std::fs;
use std::time::Instant;
use std::env;

use lexer::Lexer;
use parser::Parser;
use compiler::Bytecoder;
use interpreter::{BytecodeInterpreter, RuntimeType, RuntimeFunction};

fn main() {
    let args: Vec<String> = env::args().collect();
    
    if args.len() < 2 {
        eprintln!("Usage: {} <input_file>", args[0]);
        std::process::exit(1);
    }
    
    match compile(&args[1]) {
        Ok(bytecode) => {
            if let Err(e) = fs::write("out.mses", &bytecode) {
                eprintln!("Failed to write bytecode: {}", e);
            }
            
            if let Err(e) = exec(&bytecode) {
                eprintln!("Execution error: {}", e);
                std::process::exit(1);
            }
        }
        Err(e) => {
            eprintln!("Compilation error: {}", e);
            std::process::exit(1);
        }
    }
}

fn compile(filename: &str) -> Result<String, Box<dyn std::error::Error>> {
    let content = fs::read_to_string(filename)?;
    
    let start = Instant::now();
    let mut lexer = Lexer::new(&content, filename);
    let tokens = lexer.get_all_tokens();
    let lex_time = start.elapsed();
    
    let start = Instant::now();
    let mut parser = Parser::new(tokens);
    let statement = parser.parse()?;
    let parse_time = start.elapsed();
    
    let start = Instant::now();
    let bytecode = Bytecoder::compile(&statement)?;
    let compile_time = start.elapsed();
    
    println!("Lexed in {:.6}ms", lex_time.as_secs_f64() * 1000.0);
    println!("Parsed in {:.6}ms", parse_time.as_secs_f64() * 1000.0);
    println!("Compiled in {:.6}ms", compile_time.as_secs_f64() * 1000.0);
    println!("Everything took {:.6}ms", 
             (lex_time + parse_time + compile_time).as_secs_f64() * 1000.0);
    println!();
    
    Ok(bytecode)
}

fn exec(bytecode: &str) -> Result<(), Box<dyn std::error::Error>> {
    let mut interpreter = BytecodeInterpreter::new(bytecode);
    
    // Add print function
    interpreter.set_variable(
        "print".to_string(),
        RuntimeType::Function(RuntimeFunction::new(|args| {
            for (i, arg) in args.iter().enumerate() {
                print!("{}", arg);
                if i != args.len() - 1 {
                    print!(" ");
                }
            }
            println!();
            Ok(RuntimeType::Void)
        }))
    );
    
    let start = Instant::now();
    interpreter.execute_all()?;
    let exec_time = start.elapsed();
    
    println!("Execution took {:.6}ms", exec_time.as_secs_f64() * 1000.0);
    
    Ok(())
}
