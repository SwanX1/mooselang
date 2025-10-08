#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TokenType {
    ParenLeft, ParenRight,
    BlockLeft, BlockRight,
    ArrayLeft, ArrayRight,
    String, Constant, Identifier,
    Comma, Colon, Semicolon,

    // Keywords
    Func,
    Let, Const,
    If, Else,
    For, Foreach,
    While, Do, Loop,
    Return, Break, Continue,
    Import, Export,

    // Operators
    Assignment,           // =
    Addition,             // +
    Subtraction,          // -
    Multiplication,       // *
    Division,             // /
    AdditionAssignment,   // +=
    SubtractionAssignment,// -=
    MultiplicationAssignment, // *=
    DivisionAssignment,   // /=
    Modulo,               // %
    ModuloAssignment,     // %=
    PreIncrement,         // ++
    PreDecrement,         // --
    PostIncrement,        // ++, used by parser
    PostDecrement,        // --, used by parser
    Exponentiation,       // **
    ExponentiationAssignment, // **=
    Eq,                   // ==
    Neq,                  // !=
    Gte,                  // >=
    Lte,                  // <=
    Gt,                   // >
    Lt,                   // <
    LogicalOr,            // ||
    LogicalAnd,           // &&
    LogicalNot,           // !
    Dot,                  // .
    Ternary,              // ?
    BitNot,               // ~
    BitOr,                // |
    BitAnd,               // &
    BitXor,               // ^
    BitNotAssignment,     // ~=
    BitOrAssignment,      // |=
    BitAndAssignment,     // &=
    BitXorAssignment,     // ^=
    BitRshift,            // >>
    BitLshift,            // <<
    BitRshiftAssignment,  // >>=
    BitLshiftAssignment,  // <<=
    Eof,
    Asm,
}

impl TokenType {
    pub fn name(&self) -> &'static str {
        match self {
            TokenType::ParenLeft => "PAREN_LEFT",
            TokenType::ParenRight => "PAREN_RIGHT",
            TokenType::BlockLeft => "BLOCK_LEFT",
            TokenType::BlockRight => "BLOCK_RIGHT",
            TokenType::ArrayLeft => "ARRAY_LEFT",
            TokenType::ArrayRight => "ARRAY_RIGHT",
            TokenType::String => "STRING",
            TokenType::Constant => "CONSTANT",
            TokenType::Identifier => "IDENTIFIER",
            TokenType::Comma => "COMMA",
            TokenType::Colon => "COLON",
            TokenType::Semicolon => "SEMICOLON",
            TokenType::Func => "FUNC",
            TokenType::Let => "LET",
            TokenType::Const => "CONST",
            TokenType::If => "IF",
            TokenType::Else => "ELSE",
            TokenType::For => "FOR",
            TokenType::Foreach => "FOREACH",
            TokenType::While => "WHILE",
            TokenType::Do => "DO",
            TokenType::Loop => "LOOP",
            TokenType::Return => "RETURN",
            TokenType::Break => "BREAK",
            TokenType::Continue => "CONTINUE",
            TokenType::Import => "IMPORT",
            TokenType::Export => "EXPORT",
            TokenType::Assignment => "ASSIGNMENT",
            TokenType::Addition => "ADDITION",
            TokenType::Subtraction => "SUBTRACTION",
            TokenType::Multiplication => "MULTIPLICATION",
            TokenType::Division => "DIVISION",
            TokenType::AdditionAssignment => "ADDITION_ASSIGNMENT",
            TokenType::SubtractionAssignment => "SUBTRACTION_ASSIGNMENT",
            TokenType::MultiplicationAssignment => "MULTIPLICATION_ASSIGNMENT",
            TokenType::DivisionAssignment => "DIVISION_ASSIGNMENT",
            TokenType::Modulo => "MODULO",
            TokenType::ModuloAssignment => "MODULO_ASSIGNMENT",
            TokenType::PreIncrement => "PREINCREMENT",
            TokenType::PreDecrement => "PREDECREMENT",
            TokenType::PostIncrement => "POSTINCREMENT",
            TokenType::PostDecrement => "POSTDECREMENT",
            TokenType::Exponentiation => "EXPONENTIATION",
            TokenType::ExponentiationAssignment => "EXPONENTIATION_ASSIGNMENT",
            TokenType::Eq => "EQ",
            TokenType::Neq => "NEQ",
            TokenType::Gte => "GTE",
            TokenType::Lte => "LTE",
            TokenType::Gt => "GT",
            TokenType::Lt => "LT",
            TokenType::LogicalOr => "LOGICAL_OR",
            TokenType::LogicalAnd => "LOGICAL_AND",
            TokenType::LogicalNot => "LOGICAL_NOT",
            TokenType::Dot => "DOT",
            TokenType::Ternary => "TERNARY",
            TokenType::BitNot => "BIT_NOT",
            TokenType::BitOr => "BIT_OR",
            TokenType::BitAnd => "BIT_AND",
            TokenType::BitXor => "BIT_XOR",
            TokenType::BitNotAssignment => "BIT_NOT_ASSIGNMENT",
            TokenType::BitOrAssignment => "BIT_OR_ASSIGNMENT",
            TokenType::BitAndAssignment => "BIT_AND_ASSIGNMENT",
            TokenType::BitXorAssignment => "BIT_XOR_ASSIGNMENT",
            TokenType::BitRshift => "BIT_RSHIFT",
            TokenType::BitLshift => "BIT_LSHIFT",
            TokenType::BitRshiftAssignment => "BIT_RSHIFT_ASSIGNMENT",
            TokenType::BitLshiftAssignment => "BIT_LSHIFT_ASSIGNMENT",
            TokenType::Eof => "EOF",
            TokenType::Asm => "ASM",
        }
    }

    pub const LONGEST_TOKEN_NAME: usize = 29; // "MULTIPLICATION_ASSIGNMENT"

    pub fn is_binary_operator(&self) -> bool {
        matches!(self,
            TokenType::Assignment |
            TokenType::Addition |
            TokenType::Subtraction |
            TokenType::Multiplication |
            TokenType::Division |
            TokenType::AdditionAssignment |
            TokenType::SubtractionAssignment |
            TokenType::MultiplicationAssignment |
            TokenType::DivisionAssignment |
            TokenType::Modulo |
            TokenType::ModuloAssignment |
            TokenType::Exponentiation |
            TokenType::ExponentiationAssignment |
            TokenType::Eq |
            TokenType::Neq |
            TokenType::Gte |
            TokenType::Lte |
            TokenType::Gt |
            TokenType::Lt |
            TokenType::LogicalOr |
            TokenType::LogicalAnd |
            TokenType::BitOr |
            TokenType::BitAnd |
            TokenType::BitXor |
            TokenType::BitNotAssignment |
            TokenType::BitOrAssignment |
            TokenType::BitAndAssignment |
            TokenType::BitXorAssignment |
            TokenType::BitRshift |
            TokenType::BitLshift |
            TokenType::BitRshiftAssignment |
            TokenType::BitLshiftAssignment
        )
    }
}
