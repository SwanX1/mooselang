use std::fmt;

// Runtime type system for MooseLang
#[derive(Clone)]
pub enum RuntimeType {
    Void,
    Boolean(bool),
    Integer(i64),
    Float(f64),
    String(String),
    Array(Vec<RuntimeType>, String), // elements, inner_type
    Function(RuntimeFunction),
    Pointer(Box<RuntimeType>),
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
                for (i, elem) in elements.iter().enumerate() {
                    if i > 0 { write!(f, ", ")?; }
                    write!(f, "{}", elem)?;
                }
                write!(f, "]")
            }
            RuntimeType::Function(_) => write!(f, "<function>"),
            RuntimeType::Pointer(p) => write!(f, "*{}", p),
        }
    }
}

// Function wrapper
#[derive(Clone)]
pub struct RuntimeFunction {
    func: std::rc::Rc<dyn Fn(&[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>>>,
}

impl RuntimeFunction {
    pub fn new<F>(f: F) -> Self
    where
        F: Fn(&[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>> + 'static,
    {
        RuntimeFunction {
            func: std::rc::Rc::new(f),
        }
    }

    pub fn call(&self, args: &[RuntimeType]) -> Result<RuntimeType, Box<dyn std::error::Error>> {
        (self.func)(args)
    }
}

// TODO: Implement:
// - Type conversions and constructors
// - Binary and unary operations for each type
// - Property and array access
// - Type checking and validation
