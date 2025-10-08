use std::collections::HashMap;
use super::{RuntimeType, RuntimeFunction};
use std::rc::Rc;
use std::cell::RefCell;

pub struct BytecodeInterpreter {
    bytecode: Vec<String>,
    memory: Vec<RuntimeType>,
    variables: HashMap<String, RuntimeType>,
    labels: HashMap<String, usize>,
    register1: Option<RuntimeType>,
    register2: Option<RuntimeType>,
    buffer: Option<RuntimeType>,
    last_loaded_variable: Option<String>,  // Track which variable was just loaded
    line: usize,
    column: usize,
    file: String,
}

impl BytecodeInterpreter {
    pub fn new(bytecode: &str) -> Self {
        let bytecode: Vec<String> = bytecode.lines().map(|s| s.to_string()).collect();
        
        BytecodeInterpreter {
            bytecode,
            memory: Vec::new(),
            variables: HashMap::new(),
            labels: HashMap::new(),
            register1: None,
            register2: None,
            buffer: None,
            last_loaded_variable: None,
            line: 0,
            column: 0,
            file: "<unknown>".to_string(),
        }
    }

    pub fn set_variable(&mut self, name: String, value: RuntimeType) {
        self.variables.insert(name, value);
    }

    pub fn execute_all(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        // First pass: collect labels
        for (i, line) in self.bytecode.iter().enumerate() {
            if line.starts_with("label ") {
                let parts: Vec<&str> = line.split_whitespace().collect();
                if parts.len() != 2 {
                    return Err(format!("Invalid label instruction: {}", line).into());
                }
                self.labels.insert(parts[1].to_string(), i);
            }
        }

        // Second pass: execute instructions
        let mut i = 0;
        while i < self.bytecode.len() {
            let instruction = self.bytecode[i].clone();
            
            // Handle debug info
            if instruction.starts_with('@') {
                let parts: Vec<&str> = instruction.split(',').collect();
                if parts.len() != 3 {
                    return Err(format!("Invalid debug info: {}", instruction).into());
                }
                self.line = parts[0][1..].parse()?;
                self.column = parts[1].parse()?;
                self.file = parts[2].to_string();
                i += 1;
                continue;
            }

            // Skip labels and comments
            if instruction.starts_with("label ") || instruction.starts_with(";") {
                i += 1;
                continue;
            }

            match self.execute_instruction(&instruction, i) {
                Ok(new_i) => i = new_i,
                Err(e) => {
                    eprintln!("Error on line {}: {} (bytecode line {}, {} {}:{})", 
                              self.line, e, i + 1, self.file, self.line, self.column);
                    return Err(e);
                }
            }
        }

        Ok(())
    }

    fn execute_instruction(&mut self, line: &str, index: usize) -> Result<usize, Box<dyn std::error::Error>> {
        let parts: Vec<&str> = line.splitn(2, ' ').collect();
        let instruction = parts[0];
        let args: Vec<&str> = if parts.len() > 1 {
            parts[1].splitn(2, ' ').collect()
        } else {
            Vec::new()
        };

        match instruction {
            "setb" => {
                if args.len() < 1 {
                    return Err("setb requires at least type".into());
                }
                let type_str = args[0];
                
                if type_str == "array" {
                    // Array creation with optional size
                    let size = if args.len() > 1 { args[1].parse().unwrap_or(0) } else { 0 };
                    self.buffer = Some(RuntimeType::Array(
                        Rc::new(RefCell::new(Vec::with_capacity(size))), 
                        "".to_string()
                    ));
                } else {
                    if args.len() < 2 {
                        return Err("setb requires type and value for non-array types".into());
                    }
                    let value_str = args[1];
                    self.buffer = Some(RuntimeType::from_string(type_str, value_str)?);
                }
            }

            "getp" => {
                let name = args[0];
                
                if name == "@" {
                    // Create a variable pointer if we just loaded a variable
                    if let Some(var_name) = &self.last_loaded_variable {
                        self.buffer = Some(RuntimeType::VariablePointer(var_name.clone()));
                        self.last_loaded_variable = None;
                    } else {
                        // Otherwise create a value pointer
                        let buffer_clone = self.buffer.as_ref().ok_or("Buffer is empty")?.clone();
                        self.buffer = Some(RuntimeType::Pointer(Rc::new(RefCell::new(buffer_clone))));
                    }
                } else {
                    // Property access
                    let prop_value = self.buffer.as_ref().ok_or("Buffer is empty")?.get_property(name)?;
                    self.buffer = Some(prop_value);
                    self.last_loaded_variable = None;
                }
            }
            
            "setp1" => {
                match &self.buffer {
                    Some(RuntimeType::Pointer(ptr)) => {
                        let value = self.register1.as_ref().ok_or("Register 1 is empty")?.clone();
                        *ptr.borrow_mut() = value;
                    }
                    Some(RuntimeType::VariablePointer(var_name)) => {
                        let value = self.register1.as_ref().ok_or("Register 1 is empty")?.clone();
                        self.variables.insert(var_name.clone(), value);
                    }
                    _ => return Err("Buffer is not a pointer".into()),
                }
            }

            "setp2" => {
                match &self.buffer {
                    Some(RuntimeType::Pointer(ptr)) => {
                        let value = self.register2.as_ref().ok_or("Register 2 is empty")?.clone();
                        *ptr.borrow_mut() = value;
                    }
                    Some(RuntimeType::VariablePointer(var_name)) => {
                        let value = self.register2.as_ref().ok_or("Register 2 is empty")?.clone();
                        self.variables.insert(var_name.clone(), value);
                    }
                    _ => return Err("Buffer is not a pointer".into()),
                }
            }

            "setr1" => {
                self.register1 = self.buffer.clone();
            }

            "setr2" => {
                self.register2 = self.buffer.clone();
            }

            "getr1" => {
                self.buffer = self.register1.clone();
            }

            "getr2" => {
                self.buffer = self.register2.clone();
            }

            "clearr1" => {
                self.register1 = None;
            }

            "clearr2" => {
                self.register2 = None;
            }

            "clearb" => {
                self.buffer = None;
            }

            "createv" => {
                if args.len() < 2 {
                    return Err("createv requires type and name".into());
                }
                let type_str = args[0];
                let name = args[1];
                self.variables.insert(name.to_string(), RuntimeType::default_value(type_str)?);
            }

            "setv" => {
                let name = args[0];
                let value = self.buffer.as_ref().ok_or("Buffer is empty")?.clone();
                self.variables.insert(name.to_string(), value);
            }

            "crsetv" => {
                let name = args[0];
                let value = self.buffer.as_ref().ok_or("Buffer is empty")?.clone();
                self.variables.insert(name.to_string(), value);
            }

            "setc" => {
                // In Java this marks the value as constant, but in our simplified version
                // we don't track this - it's enforced at compile time
            }

            "loadv" => {
                let name = args[0];
                let value = self.variables.get(name)
                    .ok_or(format!("Variable does not exist: {}", name))?
                    .clone();
                self.buffer = Some(value);
                self.last_loaded_variable = Some(name.to_string());
            }

            "clearv" => {
                let name = args[0];
                self.variables.remove(name);
            }

            "pushm" => {
                let value = self.buffer.as_ref().ok_or("Buffer is empty")?.clone();
                self.memory.push(value);
            }

            "popm" => {
                self.memory.pop().ok_or("Memory is empty")?;
            }

            "op" => {
                let operator = args[0];
                let r1 = self.register1.as_ref().ok_or("Register 1 is empty")?;
                
                let result = if operator == "!" || operator == "~" {
                    r1.perform_unary_operation(operator)?
                } else {
                    let r2 = self.register2.as_ref().ok_or("Register 2 is empty")?;
                    
                    // Unwrap pointers
                    let mut r2_val = r2.clone();
                    while let RuntimeType::Pointer(ptr) = r2_val {
                        r2_val = ptr.borrow().clone();
                    }
                    
                    r1.perform_binary_operation(operator, &r2_val)?
                };
                
                self.buffer = Some(result);
            }

            "call" => {
                if args.len() < 2 {
                    return Err("call requires variable name and arg count".into());
                }
                let var_name = args[0];
                let arg_count: usize = args[1].parse()?;
                
                let func = self.variables.get(var_name)
                    .ok_or(format!("Variable does not exist: {}", var_name))?;
                
                if let RuntimeType::Function(f) = func {
                    // Collect arguments from memory
                    if self.memory.len() < arg_count {
                        return Err(format!("Not enough arguments on stack: expected {}, got {}", 
                                         arg_count, self.memory.len()).into());
                    }
                    
                    let start = self.memory.len() - arg_count;
                    let args: Vec<RuntimeType> = self.memory.drain(start..).collect();
                    
                    self.buffer = Some(f.call(&args)?);
                } else {
                    return Err(format!("Variable {} is not a function", var_name).into());
                }
            }

            "jmp" | "jmpz" | "jpnz" => {
                let label = args[0];
                let target = *self.labels.get(label)
                    .ok_or(format!("Label does not exist: {}", label))?;
                
                if instruction == "jmpz" || instruction == "jpnz" {
                    let buffer = self.buffer.as_ref().ok_or("Buffer is empty")?;
                    let is_true = match buffer {
                        RuntimeType::Boolean(b) => *b,
                        _ => return Err("Buffer is not a boolean".into()),
                    };
                    
                    if (instruction == "jmpz" && !is_true) || (instruction == "jpnz" && is_true) {
                        return Ok(target);
                    }
                } else {
                    return Ok(target);
                }
            }

            "apush" => {
                let size: usize = args[0].parse()?;
                let buffer = self.buffer.as_mut().ok_or("Buffer is empty")?;
                
                if let RuntimeType::Array(arr, _) = buffer {
                    if self.memory.len() < size {
                        return Err(format!("Not enough arguments on memory: expected {}, got {}", 
                                         size, self.memory.len()).into());
                    }
                    
                    // Java: for (int i = size - 1; i >= 0; i--) {
                    //   arr.setIndex(i, memory.remove(memory.size() - 1));
                    // }
                    // This means last item in memory goes to last index in array
                    let mut arr_mut = arr.borrow_mut();
                    arr_mut.clear();
                    arr_mut.resize(size, RuntimeType::Void);
                    
                    for i in (0..size).rev() {
                        arr_mut[i] = self.memory.pop().ok_or("Memory is empty")?;
                    }
                } else {
                    return Err("Buffer is not an array".into());
                }
            }

            "apop" => {
                let buffer = self.buffer.as_mut().ok_or("Buffer is empty")?;
                
                if let RuntimeType::Array(arr, _) = buffer {
                    arr.borrow_mut().pop().ok_or("Array is empty")?;
                } else {
                    return Err("Buffer is not an array".into());
                }
            }

            "alen" => {
                let len = match self.buffer.as_ref().ok_or("Buffer is empty")? {
                    RuntimeType::Array(arr, _) => arr.borrow().len() as i64,
                    _ => return Err("Buffer is not an array".into()),
                };
                self.buffer = Some(RuntimeType::Integer(len));
            }

            _ => {
                if !instruction.is_empty() {
                    return Err(format!("Unknown instruction: {}", instruction).into());
                }
            }
        }

        Ok(index + 1)
    }
}
