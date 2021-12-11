/**
 * Tokens that are to be seperated from other keywords.
 * They cannot be used in keywords and must always be by themselves as a token.
 * The most common tokens must be last (i.e. `==` must be before `=`)
 */
export const TOKEN = Object.freeze({
  ARRAY_TYPE_INDICATOR: "[]",
  LEFT_PARENTHESES: "(",
  RIGHT_PARENTHESES: ")",
  LEFT_BRACE: "{",
  RIGHT_BRACE: "}",
  LEFT_BRACKET: "[",
  RIGHT_BRACKET: "]",
  BITSHIFT_LEFT: "<<",
  BITSHIFT_RIGHT: ">>",
  EQUAL: "==",
  NOT_EQUAL: "!=",
  LESS_THAN_OR_EQUAL: "<=",
  GREATER_THAN_OR_EQUAL: ">=",
  LESS_THAN: "<",
  GREATER_THAN: ">",
  BOOLEAN_NOT: "!",
  BOOLEAN_AND: "&&",
  BOOLEAN_OR: "||",
  BOOLEAN_XOR: "^^",
  INCREMENT: "++",
  DECREMENT: "--",
  ASSIGNMENT: "=",
  DOT: ".",
  MODULO: "%",
  MINUS: "-",
  PLUS: "+",
  DIVIDE: "/",
  POWER: "**",
  MULTIPLY: "*",
  BITWISE_XOR: "^",
  BITWISE_AND: "&",
  BITWISE_NOT: "~",
  BITWISE_OR: "|",
  SEMICOLON: ";",
  COMMA: ",",
  COLON: ":",
  QUOTE: "\"", // This is an exception in code, this token doesn't actually get seperated, it is just used to mark the start/end of a string
});

/**
 * Tokens that are to be seperated from other keywords.
 * They cannot be used in keywords and must always be by themselves as a token.
 * The most common tokens must be last (i.e. `==` must be before `=`)
 */
export const TOKEN_LIST = (() => {
  const tokens = [];
  for (const key in TOKEN) {
    if (TOKEN.hasOwnProperty(key)) {
      //@ts-expect-error Shut the fuck up typescript
      tokens.push(TOKEN[key]);
    }
  }
  return Object.freeze(tokens);
})() as readonly string[];

export const KEYWORD = Object.freeze({
  BREAK: "break",
  CONSTANT: "const",
  CONTINUE: "continue",
  ELSE: "else",
  EXPORT: "export",
  FALSE: "false",
  FOR: "for",
  FUNCTION: "func",
  IF: "if",
  RETURN: "return",
  TRUE: "true",
  WHILE: "while",
  // Reserved, unused keywords below
  ANY: "any",
  AS: "as",
  BOOLEAN: "boolean",
  CASE: "case",
  CATCH: "catch",
  CLASS: "class",
  DEFAULT: "default",
  DELETE: "delete",
  EXTENDS: "extends",
  FINALLY: "finally",
  FOR_EACH: "foreach",
  FROM: "from",
  IMPLEMENTS: "implements",
  IMPORT: "import",
  IN: "in",
  INFINITY: "Infinity",
  INTERFACE: "interface",
  LET: "let",
  NEVER: "never",
  NEW: "new",
  NOT_A_NUMBER: "NaN",
  NULL: "null",
  NUMBER: "number",
  OBJECT: "object",
  OF: "of",
  PRIVATE: "private",
  PROTECTED: "protected",
  PUBLIC: "public",
  STATIC: "static",
  STRING: "string",
  SWITCH: "switch",
  SYMBOL: "symbol",
  THIS: "this",
  THROW: "throw",
  TRY: "try",
  TYPE_OF: "typeof",
  UNDEFINED: "undefined",
  UNKNOWN: "unknown",
  VARIABLE: "var",
  VOID: "void",
  YIELD: "yield",
});

export const KEYWORD_LIST = (() => {
  const tokens = [];
  for (const key in TOKEN) {
    if (TOKEN.hasOwnProperty(key)) {
      //@ts-expect-error Shut the fuck up typescript
      tokens.push(TOKEN[key]);
    }
  }
  return Object.freeze(tokens);
})() as readonly string[];