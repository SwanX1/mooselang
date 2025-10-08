use crate::lexer::{Token, TokenType};
use crate::util::DebugInfo;
use super::statement::*;

pub struct Parser {
    tokens: Vec<Token>,
    position: usize,
}

#[derive(Debug)]
pub struct ParsingException {
    message: String,
    debug_info: DebugInfo,
}

impl std::fmt::Display for ParsingException {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{} at {:?}", self.message, self.debug_info)
    }
}

impl std::error::Error for ParsingException {}

impl Parser {
    pub fn new(tokens: Vec<Token>) -> Self {
        Parser { tokens, position: 0 }
    }

    pub fn parse(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let mut statements = Vec::new();
        let debug_info = self.get_debug_info();
        
        while !self.match_type(TokenType::Eof) {
            match self.statement() {
                Ok(stmt) => statements.push(stmt),
                Err(e) => {
                    eprintln!("Got {:?}", self.peek_token());
                    eprintln!("Current elements: {} parsed", statements.len());
                    return Err(e);
                }
            }
        }
        
        Ok(Statement::Block(BlockStatement {
            debug_info,
            statements,
        }))
    }

    fn peek_token(&self) -> &Token {
        self.peek_token_offset(0)
    }

    fn peek_token_offset(&self, offset: usize) -> &Token {
        if self.position + offset < self.tokens.len() {
            &self.tokens[self.position + offset]
        } else {
            // Return a default EOF token
            &self.tokens[self.tokens.len() - 1]
        }
    }

    fn next_token(&mut self) -> Token {
        let token = self.peek_token().clone();
        self.position += 1;
        token
    }

    fn match_type(&self, token_type: TokenType) -> bool {
        self.peek_token().token_type == token_type
    }

    fn match_type_offset(&self, offset: usize, token_type: TokenType) -> bool {
        self.peek_token_offset(offset).token_type == token_type
    }

    fn consume(&mut self, token_type: TokenType) -> Result<Token, Box<dyn std::error::Error>> {
        if self.match_type(token_type) {
            Ok(self.next_token())
        } else {
            Err(Box::new(ParsingException {
                message: format!("Expected {:?} but got {:?}", token_type, self.peek_token().token_type),
                debug_info: self.get_debug_info(),
            }))
        }
    }

    fn get_debug_info(&self) -> DebugInfo {
        let token = self.peek_token();
        DebugInfo::new(token.line, token.column, token.file.clone(), Some(token.clone()))
    }

    fn statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        // NOTE: This is a stub implementation. The full parser requires implementing:
        // - All statement parsing methods (const_statement, let_statement, if_statement, etc.)
        // - Expression parsing with operator precedence
        // - Error recovery
        // - Complete AST construction
        
        // For now, return an error indicating incomplete implementation
        Err(Box::new(ParsingException {
            message: "Parser implementation incomplete - this is a stub".to_string(),
            debug_info: self.get_debug_info(),
        }))
    }

    // TODO: Implement these methods by translating from Java Parser.java:
    // - const_statement()
    // - let_statement()
    // - if_statement()
    // - for_statement()
    // - while_statement()
    // - do_while_statement()
    // - loop_statement()
    // - break_statement()
    // - continue_statement()
    // - assignment()
    // - ternary()
    // - logical_or()
    // - logical_and()
    // - bitwise_or()
    // - bitwise_xor()
    // - bitwise_and()
    // - equality()
    // - comparison()
    // - bitwise_shift()
    // - term()
    // - factor()
    // - exponent()
    // - unary()
    // - postfix()
    // - call()
    // - primary()
}
