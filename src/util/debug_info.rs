use crate::lexer::Token;

#[derive(Debug, Clone)]
pub struct DebugInfo {
    pub line: usize,
    pub column: usize,
    pub file: String,
    pub token: Option<Token>,
}

impl DebugInfo {
    pub fn new(line: usize, column: usize, file: String, token: Option<Token>) -> Self {
        DebugInfo { line, column, file, token }
    }
}
