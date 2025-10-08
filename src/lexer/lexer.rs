use super::{Token, TokenType};

pub struct Lexer {
    input: String,
    position: usize,
    line: usize,
    column: usize,
    file: String,
}

impl Lexer {
    pub fn new(input: &str, file: &str) -> Self {
        Lexer {
            input: input.to_string(),
            position: 0,
            line: 1,
            column: 1,
            file: file.to_string(),
        }
    }

    pub fn get_all_tokens(&mut self) -> Vec<Token> {
        let mut tokens = Vec::new();
        loop {
            let token = self.next_token();
            let is_eof = token.token_type == TokenType::Eof;
            tokens.push(token);
            if is_eof {
                break;
            }
        }
        tokens
    }

    fn next_token(&mut self) -> Token {
        while self.position < self.input.len() {
            let current_char = self.current_char();

            // Skip whitespace
            if current_char == ' ' || current_char == '\n' || current_char == '\r' || current_char == '\t' {
                self.incr_pos();
                continue;
            }

            // Skip comments
            if current_char == '/' && self.peek_char() == Some('/') {
                self.incr_pos();
                self.incr_pos();
                while self.position < self.input.len() && self.current_char() != '\n' {
                    self.incr_pos();
                }
                continue;
            }

            // Single character tokens
            if current_char == '[' {
                self.incr_pos();
                return self.new_token(TokenType::ArrayLeft, "[");
            }
            if current_char == ']' {
                self.incr_pos();
                return self.new_token(TokenType::ArrayRight, "]");
            }
            if current_char == '(' {
                self.incr_pos();
                return self.new_token(TokenType::ParenLeft, "(");
            }
            if current_char == ')' {
                self.incr_pos();
                return self.new_token(TokenType::ParenRight, ")");
            }
            if current_char == '{' {
                self.incr_pos();
                return self.new_token(TokenType::BlockLeft, "{");
            }
            if current_char == '}' {
                self.incr_pos();
                return self.new_token(TokenType::BlockRight, "}");
            }
            if current_char == ',' {
                self.incr_pos();
                return self.new_token(TokenType::Comma, ",");
            }
            if current_char == ';' {
                self.incr_pos();
                return self.new_token(TokenType::Semicolon, ";");
            }

            // ASM token
            if current_char == '$' {
                self.incr_pos();
                let mut asm = String::new();
                while self.position < self.input.len() && self.current_char() != '$' {
                    asm.push(self.current_char());
                    self.incr_pos();
                }
                self.incr_pos();
                return self.new_token(TokenType::Asm, &asm);
            }

            // Operators
            if self.is_operator_char(current_char) {
                let operator = self.get_operator();
                if self.is_valid_operator(&operator) {
                    let token_type = match operator.as_str() {
                        "=" => TokenType::Assignment,
                        "+" => TokenType::Addition,
                        "-" => TokenType::Subtraction,
                        "*" => TokenType::Multiplication,
                        "/" => TokenType::Division,
                        "+=" => TokenType::AdditionAssignment,
                        "-=" => TokenType::SubtractionAssignment,
                        "*=" => TokenType::MultiplicationAssignment,
                        "/=" => TokenType::DivisionAssignment,
                        "%" => TokenType::Modulo,
                        "%=" => TokenType::ModuloAssignment,
                        "++" => TokenType::PreIncrement,
                        "--" => TokenType::PreDecrement,
                        "**" => TokenType::Exponentiation,
                        "**=" => TokenType::ExponentiationAssignment,
                        "==" => TokenType::Eq,
                        "!=" => TokenType::Neq,
                        ">=" => TokenType::Gte,
                        "<=" => TokenType::Lte,
                        ">" => TokenType::Gt,
                        "<" => TokenType::Lt,
                        "||" => TokenType::LogicalOr,
                        "&&" => TokenType::LogicalAnd,
                        "!" => TokenType::LogicalNot,
                        "." => TokenType::Dot,
                        "?" => TokenType::Ternary,
                        ":" => TokenType::Colon,
                        "~" => TokenType::BitNot,
                        "|" => TokenType::BitOr,
                        "&" => TokenType::BitAnd,
                        "^" => TokenType::BitXor,
                        "~=" => TokenType::BitNotAssignment,
                        "|=" => TokenType::BitOrAssignment,
                        "&=" => TokenType::BitAndAssignment,
                        "^=" => TokenType::BitXorAssignment,
                        ">>" => TokenType::BitRshift,
                        "<<" => TokenType::BitLshift,
                        ">>=" => TokenType::BitRshiftAssignment,
                        "<<=" => TokenType::BitLshiftAssignment,
                        _ => panic!("Invalid operator: {}", operator),
                    };
                    return self.new_token(token_type, &operator);
                } else {
                    panic!("Invalid operator: {}", operator);
                }
            }

            // Strings
            if current_char == '"' {
                let value = self.get_string();
                return self.new_token(TokenType::String, &value);
            }

            // Identifiers and keywords
            if self.is_identifier(current_char) {
                let value = self.get_identifier();
                let token_type = match value.as_str() {
                    "func" => TokenType::Func,
                    "let" => TokenType::Let,
                    "const" => TokenType::Const,
                    "if" => TokenType::If,
                    "else" => TokenType::Else,
                    "for" => TokenType::For,
                    "foreach" => TokenType::Foreach,
                    "do" => TokenType::Do,
                    "while" => TokenType::While,
                    "loop" => TokenType::Loop,
                    "return" => TokenType::Return,
                    "break" => TokenType::Break,
                    "continue" => TokenType::Continue,
                    "import" => TokenType::Import,
                    "export" => TokenType::Export,
                    _ => TokenType::Identifier,
                };
                return self.new_token(token_type, &value);
            }

            // Constants (numbers)
            if self.is_constant(current_char) {
                let value = self.get_constant();
                return self.new_token(TokenType::Constant, &value);
            }

            self.incr_pos();
        }
        
        self.new_token(TokenType::Eof, "")
    }

    fn current_char(&self) -> char {
        self.input.chars().nth(self.position).unwrap()
    }

    fn peek_char(&self) -> Option<char> {
        self.input.chars().nth(self.position + 1)
    }

    fn is_operator_char(&self, c: char) -> bool {
        matches!(c, '~' | '|' | '&' | '^' | '+' | '-' | '*' | '/' | '=' | '!' | '>' | '<' | '%' | '?' | ':' | '.')
    }

    fn is_valid_operator(&self, operator: &str) -> bool {
        matches!(operator,
            "~" | "|" | "&" | "^" | "~=" | "|=" | "&=" | "^=" |
            ">>" | "<<" | ">>=" | "<<=" |
            "+" | "-" | "*" | "/" | "+=" | "-=" | "*=" | "/=" |
            "%" | "%=" | "++" | "--" | "**" | "**=" |
            ">" | "<" | "==" | "!=" | ">=" | "<=" |
            "||" | "&&" | "!" |
            "?" | ":" | "." | "="
        )
    }

    fn get_operator(&mut self) -> String {
        let mut builder = String::new();
        while self.position < self.input.len() && self.is_operator_char(self.current_char()) {
            builder.push(self.current_char());
            self.incr_pos();
        }
        builder
    }

    fn get_string(&mut self) -> String {
        let mut result = String::new();
        self.incr_pos(); // Skip opening quote
        
        while self.position < self.input.len() && self.current_char() != '"' {
            if self.current_char() == '\\' {
                self.incr_pos();
                if self.position < self.input.len() {
                    match self.current_char() {
                        'n' => result.push('\n'),
                        't' => result.push('\t'),
                        '"' => result.push('"'),
                        '\\' => result.push('\\'),
                        c => result.push(c),
                    }
                }
            } else {
                result.push(self.current_char());
            }
            self.incr_pos();
        }
        
        self.incr_pos(); // Skip closing quote
        result
    }

    fn is_identifier(&self, c: char) -> bool {
        c.is_alphabetic() || c == '_'
    }

    fn get_identifier(&mut self) -> String {
        let mut result = String::new();
        while self.position < self.input.len() {
            let c = self.current_char();
            if c.is_alphanumeric() || c == '_' {
                result.push(c);
                self.incr_pos();
            } else {
                break;
            }
        }
        result
    }

    fn is_constant(&self, c: char) -> bool {
        c.is_numeric() || (c == '-' && self.peek_char().map_or(false, |ch| ch.is_numeric()))
    }

    fn get_constant(&mut self) -> String {
        let mut result = String::new();
        while self.position < self.input.len() {
            let c = self.current_char();
            if c.is_numeric() || c == '.' {
                result.push(c);
                self.incr_pos();
            } else {
                break;
            }
        }
        result
    }

    fn new_token(&self, token_type: TokenType, value: &str) -> Token {
        Token::new(token_type, value.to_string(), self.line, self.column, self.file.clone())
    }

    fn incr_pos(&mut self) {
        if self.position < self.input.len() && self.current_char() == '\n' {
            self.line += 1;
            self.column = 0;
        } else {
            self.column += 1;
        }
        self.position += 1;
    }
}
