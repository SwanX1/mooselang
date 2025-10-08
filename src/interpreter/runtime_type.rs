use std::fmt;
use std::rc::Rc;
use std::cell::RefCell;

// Runtime type system for MooseLang
#[derive(Clone)]
pub enum RuntimeType {
    Void,
    Boolean(bool),
    Integer(i64),
    Float(f64),
    String(String),
    Array(Rc<RefCell<Vec<RuntimeType>>>, String), // elements, inner_type
    Function(RuntimeFunction),
    Pointer(Rc<RefCell<RuntimeType>>),  // Direct pointer to value
    VariablePointer(String),  // Pointer to a variable by name
}

impl RuntimeType {
    pub fn type_name(&self) -> &str {
        match self {
            RuntimeType::Void => "void",
            RuntimeType::Boolean(_) => "bool",
            RuntimeType::Integer(_) => "int",
            RuntimeType::Float(_) => "float",
            RuntimeType::String(_) => "string",
            RuntimeType::Array(_, _) => "array",
            RuntimeType::Function(_) => "func",
            RuntimeType::Pointer(_) => "pointer",
            RuntimeType::VariablePointer(_) => "pointer",
        }
    }

    pub fn perform_binary_operation(&self, op: &str, other: &RuntimeType) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        // Handle array indexing
        if op == "[" {
            match self {
                RuntimeType::Array(arr, _) => {
                    if let RuntimeType::Integer(index) = other {
                        let idx = *index as usize;
                        let borrowed = arr.borrow();
                        if idx >= borrowed.len() {
                            return Err(format!("Index out of bounds: {}", index).into());
                        }
                        return Ok(borrowed[idx].clone());
                    } else {
                        return Err(format!("Cannot index array with {}", other.type_name()).into());
                    }
                }
                _ => return Err(format!("Cannot index non-array type {}", self.type_name()).into()),
            }
        }
        
        match (self, other) {
            (RuntimeType::Integer(a), RuntimeType::Integer(b)) => {
                Ok(match op {
                    "+" => RuntimeType::Integer(a + b),
                    "-" => RuntimeType::Integer(a - b),
                    "*" => RuntimeType::Integer(a * b),
                    "/" => RuntimeType::Integer(a / b),
                    "%" => RuntimeType::Integer(a % b),
                    "**" => RuntimeType::Integer(a.pow(*b as u32)),
                    "==" => RuntimeType::Boolean(a == b),
                    "!=" => RuntimeType::Boolean(a != b),
                    "<" => RuntimeType::Boolean(a < b),
                    ">" => RuntimeType::Boolean(a > b),
                    "<=" => RuntimeType::Boolean(a <= b),
                    ">=" => RuntimeType::Boolean(a >= b),
                    "&" => RuntimeType::Integer(a & b),
                    "|" => RuntimeType::Integer(a | b),
                    "^" => RuntimeType::Integer(a ^ b),
                    "<<" => RuntimeType::Integer(a << b),
                    ">>" => RuntimeType::Integer(a >> b),
                    _ => return Err(format!("Unknown operator: {}", op).into()),
                })
            }
            (RuntimeType::Float(a), RuntimeType::Float(b)) => {
                Ok(match op {
                    "+" => RuntimeType::Float(a + b),
                    "-" => RuntimeType::Float(a - b),
                    "*" => RuntimeType::Float(a * b),
                    "/" => RuntimeType::Float(a / b),
                    "%" => RuntimeType::Float(a % b),
                    "**" => RuntimeType::Float(a.powf(*b)),
                    "==" => RuntimeType::Boolean((a - b).abs() < f64::EPSILON),
                    "!=" => RuntimeType::Boolean((a - b).abs() >= f64::EPSILON),
                    "<" => RuntimeType::Boolean(a < b),
                    ">" => RuntimeType::Boolean(a > b),
                    "<=" => RuntimeType::Boolean(a <= b),
                    ">=" => RuntimeType::Boolean(a >= b),
                    _ => return Err(format!("Unknown operator: {}", op).into()),
                })
            }
            (RuntimeType::Boolean(a), RuntimeType::Boolean(b)) => {
                Ok(match op {
                    "&&" => RuntimeType::Boolean(*a && *b),
                    "||" => RuntimeType::Boolean(*a || *b),
                    "==" => RuntimeType::Boolean(a == b),
                    "!=" => RuntimeType::Boolean(a != b),
                    _ => return Err(format!("Unknown operator: {}", op).into()),
                })
            }
            (RuntimeType::String(a), RuntimeType::String(b)) => {
                Ok(match op {
                    "+" => RuntimeType::String(format!("{}{}", a, b)),
                    "==" => RuntimeType::Boolean(a == b),
                    "!=" => RuntimeType::Boolean(a != b),
                    _ => return Err(format!("Unknown operator: {}", op).into()),
                })
            }
            _ => Err(format!("Type mismatch in binary operation: {} {} {}", self.type_name(), op, other.type_name()).into()),
        }
    }

    pub fn perform_unary_operation(&self, op: &str) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        match self {
            RuntimeType::Boolean(b) => {
                Ok(match op {
                    "!" => RuntimeType::Boolean(!b),
                    _ => return Err(format!("Unknown unary operator: {}", op).into()),
                })
            }
            RuntimeType::Integer(i) => {
                Ok(match op {
                    "~" => RuntimeType::Integer(!i),
                    "!" => RuntimeType::Boolean(*i == 0),
                    _ => return Err(format!("Unknown unary operator: {}", op).into()),
                })
            }
            _ => Err(format!("Type mismatch in unary operation: {} {}", op, self.type_name()).into()),
        }
    }

    pub fn get_property(&self, name: &str) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        match self {
            RuntimeType::Array(arr, _) => {
                if name == "length" {
                    Ok(RuntimeType::Integer(arr.borrow().len() as i64))
                } else {
                    Err(format!("Unknown property: {}", name).into())
                }
            }
            RuntimeType::String(s) => {
                if name == "length" {
                    Ok(RuntimeType::Integer(s.len() as i64))
                } else {
                    Err(format!("Unknown property: {}", name).into())
                }
            }
            _ => Err(format!("Type {} has no properties", self.type_name()).into()),
        }
    }

    pub fn from_string(type_str: &str, value_str: &str) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        Ok(match type_str {
            "int" => RuntimeType::Integer(value_str.parse()?),
            "float" => RuntimeType::Float(value_str.parse()?),
            "string" => RuntimeType::String(value_str.to_string()),
            "bool" => RuntimeType::Boolean(value_str == "true"),
            "void" => RuntimeType::Void,
            "array" => RuntimeType::Array(Rc::new(RefCell::new(Vec::new())), "".to_string()),
            _ => return Err(format!("Unknown type: {}", type_str).into()),
        })
    }

    pub fn default_value(type_str: &str) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        Ok(match type_str {
            "int" => RuntimeType::Integer(0),
            "float" => RuntimeType::Float(0.0),
            "string" => RuntimeType::String(String::new()),
            "bool" => RuntimeType::Boolean(false),
            "void" => RuntimeType::Void,
            _ => {
                if type_str.ends_with("[]") {
                    let inner_type = &type_str[..type_str.len() - 2];
                    RuntimeType::Array(Rc::new(RefCell::new(Vec::new())), inner_type.to_string())
                } else {
                    return Err(format!("Unknown type: {}", type_str).into());
                }
            }
        })
    }
}

impl fmt::Display for RuntimeType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            RuntimeType::Void => write!(f, "void"),
            RuntimeType::Boolean(b) => write!(f, "{}", if *b { "true" } else { "false" }),
            RuntimeType::Integer(i) => write!(f, "{}", i),
            RuntimeType::Float(fl) => write!(f, "{}", fl),
            RuntimeType::String(s) => write!(f, "{}", s),
            RuntimeType::Array(elements, _) => {
                write!(f, "[")?;
                let borrowed = elements.borrow();
                for (i, elem) in borrowed.iter().enumerate() {
                    if i > 0 { write!(f, ", ")?; }
                    write!(f, "{}", elem)?;
                }
                write!(f, "]")
            }
            RuntimeType::Function(_) => write!(f, "<function>"),
            RuntimeType::Pointer(p) => write!(f, "*{}", p.borrow()),
            RuntimeType::VariablePointer(name) => write!(f, "*{}", name),
        }
    }
}

// Function wrapper
#[derive(Clone)]
pub struct RuntimeFunction {
    func: Rc<dyn Fn(&[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>>>,
}

impl RuntimeFunction {
    pub fn new<F>(f: F) -> Self
    where
        F: Fn(&[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>> + 'static,
    {
        RuntimeFunction {
            func: Rc::new(f),
        }
    }

    pub fn call(&self, args: &[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        (self.func)(args)
    }
}
