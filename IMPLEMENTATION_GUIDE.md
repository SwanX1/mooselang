# Implementation Guide for Rust Port

This document outlines what has been completed and what remains to be done for the complete Rust port of MooseLang.

## Completed Components

### ✅ Project Structure
- Cargo.toml with proper configuration
- Module organization matching Java package structure
- .gitignore updated for Rust artifacts

### ✅ Utility Module (`src/util/`)
- `DebugInfo` struct - Complete

### ✅ Lexer Module (`src/lexer/`)
- `TokenType` enum with all 60+ token types - Complete
- `Token` struct - Complete
- `Lexer` implementation - **COMPLETE AND FUNCTIONAL**
  - Tokenizes all operators, keywords, identifiers
  - String parsing with escape sequences
  - Number parsing (integers and floats)
  - Comment handling
  - ASM token support

### ✅ Parser Module (`src/parser/`)
- All statement type definitions - Complete
- `Statement` enum with 21 variants - Complete
- Parser struct with basic methods - **STUB ONLY**

### ⚠️ Compiler Module (`src/compiler/`)
- Module structure - Complete
- `Bytecoder` struct - **STUB ONLY**

### ⚠️ Interpreter Module (`src/interpreter/`)
- `RuntimeType` enum with basic structure - **PARTIAL**
- `RuntimeFunction` wrapper - **PARTIAL**
- `BytecodeInterpreter` struct - **STUB ONLY**

### ✅ Main Entry Point
- CLI argument parsing - Complete
- compile() and exec() functions - Complete (but depend on incomplete modules)

## Remaining Work

### 1. Parser Implementation (`src/parser/parser.rs`)

The parser is currently a stub. It needs implementation of all parsing methods from `Parser.java` (462 lines):

#### Statement Parsing Methods
- `const_statement()` - Parse const declarations
- `let_statement()` - Parse let declarations
- `if_statement()` - Parse if/else statements
- `for_statement()` - Parse for loops
- `while_statement()` - Parse while loops
- `do_while_statement()` - Parse do-while loops
- `loop_statement()` - Parse infinite loops
- `break_statement()` - Parse break statements
- `continue_statement()` - Parse continue statements

#### Expression Parsing Methods (with operator precedence)
- `assignment()` - Assignment expressions
- `ternary()` - Ternary conditional operator
- `logical_or()` - Logical OR expressions
- `logical_and()` - Logical AND expressions
- `bitwise_or()` - Bitwise OR expressions
- `bitwise_xor()` - Bitwise XOR expressions
- `bitwise_and()` - Bitwise AND expressions
- `equality()` - Equality comparisons
- `comparison()` - Relational comparisons
- `bitwise_shift()` - Bit shift operations
- `term()` - Addition and subtraction
- `factor()` - Multiplication and division
- `exponent()` - Exponentiation
- `unary()` - Unary operators
- `postfix()` - Postfix increment/decrement
- `call()` - Function calls
- `primary()` - Primary expressions (literals, variables, etc.)

**Files to reference:**
- `src/main/java/dev/cernavskis/moose/parser/Parser.java` (lines 1-462)

### 2. Compiler Implementation (`src/compiler/bytecoder.rs`)

Needs complete implementation of bytecode generation from `Bytecoder.java` (546 lines):

#### Core Components
- `State` struct with label and variable management
- `compile_statement()` method - Main compilation logic
- Statement-specific compilation handlers for all 21 statement types
- Expression compilation with proper register management
- Debug info generation (@line,column,file format)

#### Bytecode Instructions to Generate
All the bytecode instructions documented in the README:
- Buffer operations: setb, clearb
- Register operations: setr1, setr2, getr1, getr2, clearr1, clearr2
- Variable operations: createv, setv, crsetv, loadv, clearv, setc
- Property operations: getp, setp1, setp2
- Memory operations: pushm, popm
- Array operations: apush, apop, alen
- Arithmetic operations: op [operator]
- Control flow: jmp, jmpz, jpnz, label, call

**Files to reference:**
- `src/main/java/dev/cernavskis/moose/compiler/Bytecoder.java` (lines 1-546)
- `src/main/java/dev/cernavskis/moose/compiler/StatementBytecode.java`
- `src/main/java/dev/cernavskis/moose/compiler/CompilerException.java`

### 3. Interpreter Runtime Types (`src/interpreter/runtime_type.rs`)

Needs complete implementation of all runtime types from the Java implementation:

#### Type System
- RuntimeVoid - Singleton void type
- RuntimeBoolean - Boolean type with operations
- RuntimeInteger - Integer type with operations
- RuntimeFloat - Float type with operations
- RuntimeString - String type with operations
- RuntimeArray - Array type with generic elements
- RuntimeFunction - Function type (partially done)
- RuntimePointer - Pointer/reference type

#### Operations to Implement
- Binary operations for each type (+, -, *, /, %, **, ==, !=, <, >, <=, >=, ||, &&, |, &, ^, <<, >>)
- Unary operations (!, ~, ++, --)
- Type conversions and coercion
- Property access (for arrays: length)
- Array indexing

**Files to reference:**
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeType.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeBoolean.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeInteger.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeFloat.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeString.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeArray.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeFunction.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeVoid.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimePointer.java`
- `src/main/java/dev/cernavskis/moose/interpreter/types/RuntimeCallable.java`

### 4. Bytecode Interpreter (`src/interpreter/interpreter.rs`)

Needs complete implementation from `BytecodeInterpreter.java` (342 lines):

#### Execution Logic
- Two-pass execution (label collection, then execution)
- Instruction parsing and dispatch
- All bytecode instruction implementations (30+ instructions)
- Error handling and reporting with debug info
- Register and buffer management
- Memory stack operations
- Variable storage and retrieval
- Function calling mechanism

**Files to reference:**
- `src/main/java/dev/cernavskis/moose/interpreter/BytecodeInterpreter.java` (lines 1-342)
- `src/main/java/dev/cernavskis/moose/interpreter/InterpreterException.java`

## Testing Strategy

Once implementation is complete:

1. **Unit Tests**: Create tests for each module
   - Lexer: Test tokenization of various inputs
   - Parser: Test parsing of all statement types
   - Compiler: Test bytecode generation
   - Interpreter: Test bytecode execution

2. **Integration Test**: Run `example.mse`
   ```bash
   cargo run -- example.mse
   ```
   Expected output should match Java implementation

3. **Bytecode Compatibility**: Ensure generated bytecode is identical to Java version
   ```bash
   # Java version
   ./gradlew run --args="example.mse"
   cp out.mses out-java.mses
   
   # Rust version
   cargo run -- example.mse
   cp out.mses out-rust.mses
   
   # Compare
   diff out-java.mses out-rust.mses
   ```

## Implementation Priority

Recommended order of implementation:

1. **Parser** - Critical path, enables testing of compilation
2. **Runtime Types** - Needed for interpreter
3. **Interpreter** - Enables execution and testing
4. **Compiler** - Completes the toolchain

## Estimated Effort

Based on the Java implementation:
- Parser: ~500-600 lines of Rust code
- Compiler: ~600-700 lines of Rust code  
- Runtime Types: ~400-500 lines of Rust code
- Interpreter: ~400-500 lines of Rust code
- **Total**: ~1900-2300 lines of Rust code

## Current Status

**Lines completed**: ~750 (lexer, types, stubs)
**Lines remaining**: ~1900-2300
**Overall progress**: ~25% complete

## Notes on Rust-Specific Considerations

1. **Error Handling**: Convert Java exceptions to `Result<T, E>` types
2. **Ownership**: Use `Rc<RefCell<T>>` for shared mutable state where needed
3. **Pattern Matching**: Leverage Rust's enums and pattern matching instead of Java's instanceof
4. **Lifetimes**: May need explicit lifetimes in some parser methods
5. **Traits**: Consider implementing Display, Debug, etc. for better ergonomics
