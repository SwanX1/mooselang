use crate::util::DebugInfo;
use super::TokenType;

#[derive(Debug, Clone)]
pub struct Token {
    pub token_type: TokenType,
    pub value: String,
    pub line: usize,
    pub column: usize,
    pub file: String,
}

impl Token {
    pub fn new(token_type: TokenType, value: String, line: usize, column: usize, file: String) -> Self {
        Token {
            token_type,
            value,
            line,
            column,
            file,
        }
    }

    pub fn debug_info(&self) -> DebugInfo {
        DebugInfo::new(self.line, self.column, self.file.clone(), Some(self.clone()))
    }
}

impl std::fmt::Display for Token {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let name = self.token_type.name();
        let spaces = " ".repeat(TokenType::LONGEST_TOKEN_NAME - name.len());
        write!(f, "{}{} {}", name, spaces, self.value)
    }
}
