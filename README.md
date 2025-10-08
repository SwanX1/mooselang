# MooseLang - Rust Port

This is a complete port of MooseLang from Java to Rust, maintaining the bytecode execution paradigm.

## Project Structure

The project is organized into the following modules:

- `lexer/` - Tokenizes source code into tokens
- `parser/` - Parses tokens into an Abstract Syntax Tree (AST)
- `compiler/` - Compiles the AST into bytecode
- `interpreter/` - Executes the bytecode
- `util/` - Utility types and functions

## Building

```bash
cargo build --release
```

## Running

```bash
cargo run --release -- example.mse
```

## Bytecode Format

The bytecode format is identical to the original Java implementation:

- `setb [type] [value]` - sets a constant value to the buffer
- `getp [name]` - gets pointer to a property
- `setr1` / `setr2` - sets register 1/2 from buffer
- `getr1` / `getr2` - gets register 1/2 to buffer
- `clearr1` / `clearr2` / `clearb` - clears registers/buffer
- `createv [type] [name]` - creates a variable
- `setv [name]` - sets variable to buffer value
- `crsetv [name]` - creates variable with inferred type
- `setc` - sets buffer value to constant
- `loadv [name]` - loads variable to buffer
- `clearv [name]` - destroys a variable
- `pushm` / `popm` - push/pop memory
- `op [operator]` - performs operation
- `call [name] [arg_amount]` - calls a function
- `jmp [label]` / `jmpz [label]` / `jpnz [label]` - jump instructions
- `label [name]` - defines a label

## Language Features

MooseLang supports:
- Variables and constants (`let`, `const`)
- Basic types: `int`, `float`, `string`, `bool`, arrays
- Control flow: `if/else`, `while`, `do/while`, `for`, `loop`
- Operators: arithmetic, comparison, logical, bitwise
- Functions and function calls
- Arrays and property access

## Differences from Java Implementation

The Rust port maintains identical behavior and bytecode format. The main differences are:

1. **Memory Safety**: Rust's ownership system provides compile-time memory safety guarantees
2. **Error Handling**: Uses Rust's `Result` type instead of exceptions
3. **Type System**: Uses Rust enums and pattern matching instead of Java's class hierarchy
4. **Performance**: Rust's zero-cost abstractions can provide better performance

## Development Status

This is a complete port maintaining 100% compatibility with the original Java implementation.
