use crate::util::DebugInfo;
use crate::lexer::TokenType;

#[derive(Debug, Clone)]
pub enum Statement {
    Block(BlockStatement),
    Declaration(DeclarationStatement),
    Assignment(AssignmentStatement),
    If(IfStatement),
    While(WhileStatement),
    DoWhile(DoWhileStatement),
    For(ForStatement),
    Loop(LoopStatement),
    Break(BreakStatement),
    Continue(ContinueStatement),
    FunctionCall(FunctionCallStatement),
    Binary(BinaryExpression),
    Unary(UnaryExpression),
    Ternary(TernaryExpression),
    Variable(VariableStatement),
    Number(NumberStatement),
    String(StringStatement),
    Array(ArrayStatement),
    PropertyAccess(PropertyAccessStatement),
    ArrayAccess(ArrayAccessStatement),
    LiterallyDontCare(LiterallyDontCareStatement),
}

impl Statement {
    pub fn debug_info(&self) -> &DebugInfo {
        match self {
            Statement::Block(s) => &s.debug_info,
            Statement::Declaration(s) => &s.debug_info,
            Statement::Assignment(s) => &s.debug_info,
            Statement::If(s) => &s.debug_info,
            Statement::While(s) => &s.debug_info,
            Statement::DoWhile(s) => &s.debug_info,
            Statement::For(s) => &s.debug_info,
            Statement::Loop(s) => &s.debug_info,
            Statement::Break(s) => &s.debug_info,
            Statement::Continue(s) => &s.debug_info,
            Statement::FunctionCall(s) => &s.debug_info,
            Statement::Binary(s) => &s.debug_info,
            Statement::Unary(s) => &s.debug_info,
            Statement::Ternary(s) => &s.debug_info,
            Statement::Variable(s) => &s.debug_info,
            Statement::Number(s) => &s.debug_info,
            Statement::String(s) => &s.debug_info,
            Statement::Array(s) => &s.debug_info,
            Statement::PropertyAccess(s) => &s.debug_info,
            Statement::ArrayAccess(s) => &s.debug_info,
            Statement::LiterallyDontCare(s) => &s.debug_info,
        }
    }
}

#[derive(Debug, Clone)]
pub struct BlockStatement {
    pub debug_info: DebugInfo,
    pub statements: Vec<Statement>,
}

#[derive(Debug, Clone)]
pub struct DeclarationStatement {
    pub debug_info: DebugInfo,
    pub name: String,
    pub value: Option<Box<Statement>>,
    pub var_type: String,
    pub is_const: bool,
}

#[derive(Debug, Clone)]
pub struct AssignmentStatement {
    pub debug_info: DebugInfo,
    pub qualified_name: Box<Statement>,
    pub value: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct IfStatement {
    pub debug_info: DebugInfo,
    pub condition: Box<Statement>,
    pub then_branch: Box<Statement>,
    pub else_branch: Option<Box<Statement>>,
}

#[derive(Debug, Clone)]
pub struct WhileStatement {
    pub debug_info: DebugInfo,
    pub condition: Box<Statement>,
    pub body: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct DoWhileStatement {
    pub debug_info: DebugInfo,
    pub condition: Box<Statement>,
    pub body: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct ForStatement {
    pub debug_info: DebugInfo,
    pub initializer: Option<Box<Statement>>,
    pub condition: Option<Box<Statement>>,
    pub increment: Option<Box<Statement>>,
    pub body: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct LoopStatement {
    pub debug_info: DebugInfo,
    pub body: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct BreakStatement {
    pub debug_info: DebugInfo,
    pub label: Option<String>,
}

#[derive(Debug, Clone)]
pub struct ContinueStatement {
    pub debug_info: DebugInfo,
    pub label: Option<String>,
}

#[derive(Debug, Clone)]
pub struct FunctionCallStatement {
    pub debug_info: DebugInfo,
    pub callable: Box<Statement>,
    pub arguments: Vec<Statement>,
}

#[derive(Debug, Clone)]
pub struct BinaryExpression {
    pub debug_info: DebugInfo,
    pub left: Box<Statement>,
    pub operator: String,
    pub right: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct UnaryExpression {
    pub debug_info: DebugInfo,
    pub value: Box<Statement>,
    pub operator: TokenType,
}

#[derive(Debug, Clone)]
pub struct TernaryExpression {
    pub debug_info: DebugInfo,
    pub condition: Box<Statement>,
    pub true_value: Box<Statement>,
    pub false_value: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct VariableStatement {
    pub debug_info: DebugInfo,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct NumberStatement {
    pub debug_info: DebugInfo,
    pub value: String,
}

#[derive(Debug, Clone)]
pub struct StringStatement {
    pub debug_info: DebugInfo,
    pub value: String,
}

#[derive(Debug, Clone)]
pub struct ArrayStatement {
    pub debug_info: DebugInfo,
    pub elements: Vec<Statement>,
}

#[derive(Debug, Clone)]
pub struct PropertyAccessStatement {
    pub debug_info: DebugInfo,
    pub parent: Box<Statement>,
    pub property: String,
}

#[derive(Debug, Clone)]
pub struct ArrayAccessStatement {
    pub debug_info: DebugInfo,
    pub parent: Box<Statement>,
    pub index: Box<Statement>,
}

#[derive(Debug, Clone)]
pub struct LiterallyDontCareStatement {
    pub debug_info: DebugInfo,
    pub code: String,
}
