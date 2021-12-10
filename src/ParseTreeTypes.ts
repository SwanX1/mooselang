//#region Typescript definitions for the parse tree
export interface RootElement {
  file?: string;
  elements: SyntaxElement[];
}

export type SyntaxElement = DeclarationElement | AssignmentElement | InvokeElement | ReferenceElement | LiteralElement | StatementElement;
export type ReturningSyntaxElement = InvokeElement | ReferenceElement | LiteralElement;

export interface AbstractSyntaxElement {
  position: {
    line: number;
    column: number;
  }
}

//#region Declaration
export type DeclarationElement = FunctionDeclarationElement | ConstantDeclarationElement | DefaultDeclarationElement;

export interface AbstractDeclarationElement extends AbstractSyntaxElement {
  element: "declaration";
  name: string;
  constant: boolean;
  exported: boolean;
  type: string;
}

export interface FunctionDeclarationElement extends ConstantDeclarationElement {
  name: "function";
  value: FunctionElement;
}

export interface ConstantDeclarationElement extends AbstractDeclarationElement {
  constant: true;
  value: any;
}

export interface DefaultDeclarationElement extends AbstractDeclarationElement {
  constant: false;
  value?: any;
}
//#endregion

//#region Function
export interface FunctionElement extends AbstractSyntaxElement {
  returnType: string;
  arguments: FunctionArgument[];
  elements: SyntaxElement[];
}

export interface FunctionArgument extends AbstractSyntaxElement {
  name: string;
  type: string;
}
//#endregion

export interface AssignmentElement extends AbstractSyntaxElement {
  element: "assignment";
  name: string;
  value: ReturningSyntaxElement;
}

export interface InvokeElement extends AbstractSyntaxElement {
  element: "invoke";
  internal: boolean;
  name: string;
  arguments: ReturningSyntaxElement[];
}

export interface ReferenceElement extends AbstractSyntaxElement {
  element: "reference";
  value: string;
}

export interface LiteralElement extends AbstractSyntaxElement {
  element: "literal";
  type: string;
  value: ReturningSyntaxElement | any;
}

//#region Statement
export type StatementElement = IfStatementElement | ElseStatementElement | ForStatementElement /*| ForEachStatementElement*/ | WhileStatementElement;

export interface AbstractStatementElement extends AbstractSyntaxElement {
  element: "statement";
  type: string;
  value: {
    elements: SyntaxElement[];
  }
}

export interface IfStatementElement extends AbstractStatementElement {
  type: "if";
  value: {
    condition: ReturningSyntaxElement;
    elements: SyntaxElement[];
  }
}

export interface ElseStatementElement extends AbstractStatementElement {
  type: "else";
  value: {
    elements: SyntaxElement[];
  }
}

export interface ForStatementElement extends AbstractStatementElement {
  type: "for";
  value: {
    expression: {
      initial: SyntaxElement;
      condition: ReturningSyntaxElement;
      repeated: SyntaxElement;
    };
    elements: SyntaxElement[];
  }
}

// export interface ForEachStatementElement extends AbstractStatementElement {
//   type: "foreach";
//   value: {
//     expression: ReturningSyntaxElement;
//     elements: SyntaxElement[];
//   }
// }

export interface WhileStatementElement extends AbstractStatementElement {
  type: "while";
  value: {
    condition: ReturningSyntaxElement;
    elements: SyntaxElement[];
  }
}
//#endregion
//#endregion