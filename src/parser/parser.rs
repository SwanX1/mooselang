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
            // Return last token (EOF)
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
        let token = self.peek_token();
        if token.token_type == TokenType::Eof {
            return Err(Box::new(ParsingException {
                message: "Unexpected EOF".to_string(),
                debug_info: self.get_debug_info(),
            }));
        }

        // Handle keywords first
        if self.match_type(TokenType::Const) {
            return self.const_statement();
        } else if self.match_type(TokenType::Let) {
            return self.let_statement();
        } else if self.match_type(TokenType::If) {
            return self.if_statement();
        } else if self.match_type(TokenType::For) {
            return self.for_statement();
        } else if self.match_type(TokenType::While) {
            return self.while_statement();
        } else if self.match_type(TokenType::Do) {
            return self.do_while_statement();
        } else if self.match_type(TokenType::Loop) {
            return self.loop_statement();
        } else if self.match_type(TokenType::Break) {
            return self.break_statement();
        } else if self.match_type(TokenType::Continue) {
            return self.continue_statement();
        }

        let statement = self.assignment()?;
        self.consume(TokenType::Semicolon)?;
        Ok(statement)
    }

    fn break_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::Break)?;

        let label = if self.match_type(TokenType::Identifier) {
            Some(self.consume(TokenType::Identifier)?.value)
        } else {
            None
        };

        self.consume(TokenType::Semicolon)?;
        Ok(Statement::Break(BreakStatement { debug_info, label }))
    }

    fn continue_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::Continue)?;

        let label = if self.match_type(TokenType::Identifier) {
            Some(self.consume(TokenType::Identifier)?.value)
        } else {
            None
        };

        self.consume(TokenType::Semicolon)?;
        Ok(Statement::Continue(ContinueStatement { debug_info, label }))
    }

    fn for_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::For)?;
        self.consume(TokenType::ParenLeft)?;
        
        let initializer = if !self.match_type(TokenType::Semicolon) {
            Some(Box::new(self.statement()?))
        } else {
            None
        };
        
        let condition = if !self.match_type(TokenType::Semicolon) {
            Some(Box::new(self.expression()?))
        } else {
            None
        };
        self.consume(TokenType::Semicolon)?;
        
        let increment = if !self.match_type(TokenType::ParenRight) {
            Some(Box::new(self.expression()?))
        } else {
            None
        };
        self.consume(TokenType::ParenRight)?;
        
        let body = Box::new(self.block_or_statement()?);
        
        Ok(Statement::For(ForStatement {
            debug_info,
            initializer,
            condition,
            increment,
            body,
        }))
    }

    fn while_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::While)?;
        self.consume(TokenType::ParenLeft)?;
        let condition = Box::new(self.expression()?);
        self.consume(TokenType::ParenRight)?;
        let body = Box::new(self.block_or_statement()?);
        
        Ok(Statement::While(WhileStatement {
            debug_info,
            condition,
            body,
        }))
    }

    fn do_while_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::Do)?;
        let body = Box::new(self.block_or_statement()?);
        self.consume(TokenType::While)?;
        self.consume(TokenType::ParenLeft)?;
        let condition = Box::new(self.expression()?);
        self.consume(TokenType::ParenRight)?;
        self.consume(TokenType::Semicolon)?;
        
        Ok(Statement::DoWhile(DoWhileStatement {
            debug_info,
            condition,
            body,
        }))
    }

    fn loop_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::Loop)?;
        let body = Box::new(self.block_or_statement()?);
        
        Ok(Statement::Loop(LoopStatement { debug_info, body }))
    }

    fn if_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::If)?;
        self.consume(TokenType::ParenLeft)?;
        let condition = Box::new(self.expression()?);
        self.consume(TokenType::ParenRight)?;
        let then_branch = Box::new(self.block_or_statement()?);
        
        let else_branch = if self.match_type(TokenType::Else) {
            self.consume(TokenType::Else)?;
            Some(Box::new(self.block_or_statement()?))
        } else {
            None
        };
        
        Ok(Statement::If(IfStatement {
            debug_info,
            condition,
            then_branch,
            else_branch,
        }))
    }

    fn block_or_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        if self.match_type(TokenType::BlockLeft) {
            self.block()
        } else {
            self.statement()
        }
    }

    fn block(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::BlockLeft)?;
        let mut statements = Vec::new();
        
        while !self.match_type(TokenType::BlockRight) {
            statements.push(self.statement()?);
        }
        
        self.consume(TokenType::BlockRight)?;
        Ok(Statement::Block(BlockStatement {
            debug_info,
            statements,
        }))
    }

    fn expression(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        self.assignment()
    }

    fn assignment(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        if let Some(result) = self.assignment_strict()? {
            return Ok(result);
        }
        self.ternary()
    }

    fn assignment_strict(&mut self) -> Result<Option<Statement>, Box<dyn std::error::Error>> {
        if self.match_type_offset(0, TokenType::Identifier) && 
           self.match_type_offset(1, TokenType::Assignment) {
            let debug_info = self.get_debug_info();
            let name = Box::new(self.qualified_name()?);
            self.consume(TokenType::Assignment)?;
            let value = Box::new(self.expression()?);
            return Ok(Some(Statement::Assignment(AssignmentStatement {
                debug_info,
                qualified_name: name,
                value,
            })));
        }
        Ok(None)
    }

    fn qualified_name(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        if !self.match_type(TokenType::Identifier) {
            return Err(Box::new(ParsingException {
                message: "Expected identifier".to_string(),
                debug_info: self.get_debug_info(),
            }));
        }
        
        let debug_info = self.get_debug_info();
        let name = self.consume(TokenType::Identifier)?.value;
        let parent = Statement::Variable(VariableStatement { debug_info, name });
        
        self.qualified_name_tail(parent)
    }

    fn qualified_name_tail(&mut self, mut parent: Statement) -> Result<Statement, Box<dyn std::error::Error>> {
        while self.match_type(TokenType::Dot) || self.match_type(TokenType::ArrayLeft) {
            let debug_info = self.get_debug_info();
            
            if self.match_type(TokenType::Dot) {
                self.consume(TokenType::Dot)?;
                let property = self.consume(TokenType::Identifier)?.value;
                parent = Statement::PropertyAccess(PropertyAccessStatement {
                    debug_info,
                    parent: Box::new(parent),
                    property,
                });
            } else if self.match_type(TokenType::ArrayLeft) {
                self.consume(TokenType::ArrayLeft)?;
                let index = Box::new(self.expression()?);
                self.consume(TokenType::ArrayRight)?;
                parent = Statement::ArrayAccess(ArrayAccessStatement {
                    debug_info,
                    parent: Box::new(parent),
                    index,
                });
            }
        }
        
        Ok(parent)
    }

    fn ternary(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let result = self.binary()?;
        
        if self.match_type(TokenType::Ternary) {
            let debug_info = self.get_debug_info();
            self.consume(TokenType::Ternary)?;
            let true_value = Box::new(self.expression()?);
            self.consume(TokenType::Colon)?;
            let false_value = Box::new(self.expression()?);
            
            return Ok(Statement::Ternary(TernaryExpression {
                debug_info,
                condition: Box::new(result),
                true_value,
                false_value,
            }));
        }
        
        Ok(result)
    }

    fn binary(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let mut result = self.unary()?;
        let token = self.peek_token().clone();
        
        if token.token_type.is_binary_operator() {
            let debug_info = self.get_debug_info();
            self.consume(token.token_type)?;
            let right = Box::new(self.binary()?);
            result = Statement::Binary(BinaryExpression {
                debug_info,
                left: Box::new(result),
                operator: token.value,
                right,
            });
        }
        
        Ok(result)
    }

    fn unary(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        
        if self.match_type(TokenType::PreIncrement) {
            self.consume(TokenType::PreIncrement)?;
            return Ok(Statement::Unary(UnaryExpression {
                debug_info,
                value: Box::new(self.primary()?),
                operator: TokenType::PreIncrement,
            }));
        } else if self.match_type(TokenType::PreDecrement) {
            self.consume(TokenType::PreDecrement)?;
            return Ok(Statement::Unary(UnaryExpression {
                debug_info,
                value: Box::new(self.primary()?),
                operator: TokenType::PreDecrement,
            }));
        } else if self.match_type(TokenType::BitNot) {
            self.consume(TokenType::BitNot)?;
            return Ok(Statement::Unary(UnaryExpression {
                debug_info,
                value: Box::new(self.primary()?),
                operator: TokenType::BitNot,
            }));
        } else if self.match_type(TokenType::LogicalNot) {
            self.consume(TokenType::LogicalNot)?;
            return Ok(Statement::Unary(UnaryExpression {
                debug_info,
                value: Box::new(self.primary()?),
                operator: TokenType::LogicalNot,
            }));
        }
        
        self.primary()
    }

    fn primary(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        if self.match_type(TokenType::ParenLeft) {
            self.consume(TokenType::ParenLeft)?;
            let result = self.expression()?;
            self.consume(TokenType::ParenRight)?;
            return Ok(result);
        }
        
        self.literal()
    }

    fn literal(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        
        // Try to parse as qualified name (variable/property access)
        if self.match_type(TokenType::Identifier) {
            let name = self.qualified_name()?;
            
            if self.match_type(TokenType::ParenLeft) {
                return self.function_chain(name);
            }
            
            if self.match_type(TokenType::PreIncrement) {
                self.consume(TokenType::PreIncrement)?;
                return Ok(Statement::Unary(UnaryExpression {
                    debug_info,
                    value: Box::new(name),
                    operator: TokenType::PostIncrement,
                }));
            } else if self.match_type(TokenType::PreDecrement) {
                self.consume(TokenType::PreDecrement)?;
                return Ok(Statement::Unary(UnaryExpression {
                    debug_info,
                    value: Box::new(name),
                    operator: TokenType::PostDecrement,
                }));
            }
            
            return Ok(name);
        }
        
        self.value()
    }

    fn array(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::ArrayLeft)?;
        let mut elements = Vec::new();
        
        while !self.match_type(TokenType::ArrayRight) {
            elements.push(self.expression()?);
            if self.match_type(TokenType::Comma) {
                self.consume(TokenType::Comma)?;
            }
        }
        
        self.consume(TokenType::ArrayRight)?;
        Ok(Statement::Array(ArrayStatement {
            debug_info,
            elements,
        }))
    }

    fn value(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        
        if self.match_type(TokenType::String) {
            let mut result = Statement::String(StringStatement {
                debug_info: debug_info.clone(),
                value: self.consume(TokenType::String)?.value,
            });
            
            while self.match_type(TokenType::Dot) || 
                  self.match_type(TokenType::ArrayLeft) || 
                  self.match_type(TokenType::ParenLeft) {
                let debug_info = self.get_debug_info();
                
                if self.match_type(TokenType::Dot) {
                    self.consume(TokenType::Dot)?;
                    let property = self.consume(TokenType::Identifier)?.value;
                    result = Statement::PropertyAccess(PropertyAccessStatement {
                        debug_info,
                        parent: Box::new(result),
                        property,
                    });
                } else if self.match_type(TokenType::ArrayLeft) {
                    self.consume(TokenType::ArrayLeft)?;
                    let index = Box::new(self.expression()?);
                    self.consume(TokenType::ArrayRight)?;
                    result = Statement::ArrayAccess(ArrayAccessStatement {
                        debug_info,
                        parent: Box::new(result),
                        index,
                    });
                } else if self.match_type(TokenType::ParenLeft) {
                    result = self.function_chain(result)?;
                }
            }
            
            return Ok(result);
        } else if self.match_type(TokenType::Constant) {
            return Ok(Statement::Number(NumberStatement {
                debug_info,
                value: self.consume(TokenType::Constant)?.value,
            }));
        } else if self.match_type(TokenType::ArrayLeft) {
            return self.array();
        } else if self.match_type(TokenType::Asm) {
            return Ok(Statement::LiterallyDontCare(LiterallyDontCareStatement {
                debug_info,
                code: self.consume(TokenType::Asm)?.value,
            }));
        }
        
        Err(Box::new(ParsingException {
            message: "Unknown expression".to_string(),
            debug_info: self.get_debug_info(),
        }))
    }

    fn function_chain(&mut self, callable: Statement) -> Result<Statement, Box<dyn std::error::Error>> {
        let mut result = self.function(callable)?;
        
        if self.match_type(TokenType::ParenLeft) {
            return self.function_chain(result);
        }
        
        if self.match_type(TokenType::Dot) || self.match_type(TokenType::ArrayLeft) {
            result = self.qualified_name_tail(result)?;
            if self.match_type(TokenType::ParenLeft) {
                result = self.function_chain(result)?;
            }
        }
        
        Ok(result)
    }

    fn function(&mut self, callable: Statement) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        self.consume(TokenType::ParenLeft)?;
        let mut arguments = Vec::new();
        
        while !self.match_type(TokenType::ParenRight) {
            arguments.push(self.expression()?);
            if self.match_type(TokenType::Comma) {
                self.consume(TokenType::Comma)?;
            } else {
                break;
            }
        }
        
        self.consume(TokenType::ParenRight)?;
        Ok(Statement::FunctionCall(FunctionCallStatement {
            debug_info,
            callable: Box::new(callable),
            arguments,
        }))
    }

    fn const_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        self.declaration_statement(true)
    }

    fn let_statement(&mut self) -> Result<Statement, Box<dyn std::error::Error>> {
        self.declaration_statement(false)
    }

    fn declaration_statement(&mut self, is_const: bool) -> Result<Statement, Box<dyn std::error::Error>> {
        let debug_info = self.get_debug_info();
        
        if is_const {
            self.consume(TokenType::Const)?;
        } else {
            self.consume(TokenType::Let)?;
        }
        
        let name = self.consume(TokenType::Identifier)?.value;
        self.consume(TokenType::Colon)?;
        let var_type = self.type_name()?;
        
        if !self.match_type(TokenType::Assignment) {
            if !is_const {
                self.consume(TokenType::Semicolon)?;
                return Ok(Statement::Declaration(DeclarationStatement {
                    debug_info,
                    name,
                    value: None,
                    var_type,
                    is_const: false,
                }));
            } else {
                return Err(Box::new(ParsingException {
                    message: format!("Expected = but got {}", self.peek_token().value),
                    debug_info: self.get_debug_info(),
                }));
            }
        }
        
        self.consume(TokenType::Assignment)?;
        let value = Some(Box::new(self.expression()?));
        self.consume(TokenType::Semicolon)?;
        
        Ok(Statement::Declaration(DeclarationStatement {
            debug_info,
            name,
            value,
            var_type,
            is_const,
        }))
    }

    fn type_name(&mut self) -> Result<String, Box<dyn std::error::Error>> {
        let mut type_str = self.consume(TokenType::Identifier)?.value;
        
        while self.match_type(TokenType::ArrayLeft) {
            self.consume(TokenType::ArrayLeft)?;
            self.consume(TokenType::ArrayRight)?;
            type_str.push_str("[]");
        }
        
        Ok(type_str)
    }
}
