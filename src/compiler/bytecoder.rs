use crate::parser::{Statement, *};
use crate::lexer::TokenType;
use std::collections::HashSet;

struct StatementBytecode {
    code: String,
    should_clear_buffer: bool,
}

impl StatementBytecode {
    fn new(code: String, should_clear_buffer: bool) -> Self {
        StatementBytecode { code, should_clear_buffer }
    }
}

impl std::fmt::Display for StatementBytecode {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.code)
    }
}

pub struct State {
    used_variables: HashSet<i32>,
    last_label: i32,
    pub last_continue_label: Option<String>,
    pub last_break_label: Option<String>,
}

impl State {
    fn new() -> Self {
        State {
            used_variables: HashSet::new(),
            last_label: 0,
            last_continue_label: None,
            last_break_label: None,
        }
    }

    fn get_label(&mut self) -> i32 {
        let label = self.last_label;
        self.last_label += 1;
        label
    }

    fn get_temp_variable(&mut self) -> i32 {
        let mut i = 0;
        while self.used_variables.contains(&i) {
            i += 1;
        }
        self.used_variables.insert(i);
        i
    }

    fn free_variable(&mut self, i: i32) {
        self.used_variables.remove(&i);
    }
}

pub struct Bytecoder;

impl Bytecoder {
    pub fn compile(statement: &Statement) -> Result<String, Box<dyn std::error::Error>> {
        let mut state = State::new();
        Ok(Self::compile_statement(statement, &mut state)?.to_string())
    }

    fn compile_statement(statement: &Statement, state: &mut State) -> Result<StatementBytecode, Box<dyn std::error::Error>> {
        let mut result = String::new();
        let debug_info = statement.debug_info();
        
        result.push_str(&format!("@{},{},{}\n", debug_info.line, debug_info.column, debug_info.file));
        
        let mut buffer_filled = false;

        match statement {
            Statement::Block(block) => {
                let mut cleanup = Vec::new();
                for child in &block.statements {
                    let child_result = Self::compile_statement(child, state)?;
                    result.push_str(&child_result.code);
                    if child_result.should_clear_buffer {
                        result.push_str("clearb\n");
                    }

                    if let Statement::Declaration(decl) = child {
                        cleanup.push(decl.name.clone());
                    }
                }
                for name in cleanup {
                    result.push_str(&format!("clearv {}\n", name));
                }
            }

            Statement::Declaration(decl) => {
                result.push_str(&format!("createv {} {}\n", decl.var_type, decl.name));

                if let Some(value) = &decl.value {
                    let value_result = Self::compile_statement(value, state)?;
                    result.push_str(&value_result.code);
                    if decl.is_const {
                        result.push_str("setc\n");
                    }
                    result.push_str(&format!("setv {}\n", decl.name));
                    result.push_str("clearb\n");
                }
            }

            Statement::FunctionCall(func_call) => {
                let mut args = Vec::new();

                for argument in &func_call.arguments {
                    let arg_result = Self::compile_statement(argument, state)?;
                    result.push_str(&arg_result.code);
                    let temp_var = state.get_temp_variable();
                    args.push(temp_var);
                    result.push_str(&format!("crsetv ${}\n", temp_var));
                    result.push_str("clearb\n");
                }

                let callable_result = Self::compile_statement(&func_call.callable, state)?;
                result.push_str(&callable_result.code);
                let temp_callable = state.get_temp_variable();
                result.push_str(&format!("crsetv ${}\n", temp_callable));
                result.push_str("clearb\n");

                for temp_arg in args.iter() {
                    result.push_str(&format!("loadv ${}\n", temp_arg));
                    result.push_str(&format!("clearv ${}\n", temp_arg));
                    state.free_variable(*temp_arg);
                    result.push_str("pushm\n");
                    result.push_str("clearb\n");
                }

                result.push_str(&format!("call ${} {}\n", temp_callable, func_call.arguments.len()));
                result.push_str(&format!("clearv ${}\n", temp_callable));
                state.free_variable(temp_callable);
                buffer_filled = true;
            }

            Statement::String(string_stmt) => {
                let mut value = String::new();
                for c in string_stmt.value.chars() {
                    match c {
                        '\n' => value.push_str("\\n"),
                        '\t' => value.push_str("\\t"),
                        '"' => value.push_str("\\\""),
                        '\\' => value.push_str("\\\\"),
                        _ => value.push(c),
                    }
                }
                result.push_str(&format!("setb string {}\n", value));
                buffer_filled = true;
            }

            Statement::Number(number) => {
                let is_float = number.value.contains('.');
                result.push_str("setb ");
                
                if is_float {
                    let value: f64 = number.value.parse()?;
                    result.push_str(&format!("float {}\n", value));
                } else {
                    let mut value = number.value.clone();
                    let radix = if value.starts_with("0x") {
                        value = value[2..].to_string();
                        16
                    } else if value.starts_with("0b") {
                        value = value[2..].to_string();
                        2
                    } else {
                        10
                    };
                    let parsed: i64 = i64::from_str_radix(&value, radix)?;
                    result.push_str(&format!("int {}\n", parsed));
                }
                buffer_filled = true;
            }

            Statement::Variable(var) => {
                result.push_str(&format!("loadv {}\n", var.name));
                buffer_filled = true;
            }

            Statement::Assignment(assignment) => {
                let temp_var = state.get_temp_variable();
                let assignable_result = Self::compile_statement(&assignment.qualified_name, state)?;
                let value_result = Self::compile_statement(&assignment.value, state)?;
                
                result.push_str(&assignable_result.code);
                result.push_str("getp @\n");
                result.push_str(&format!("crsetv ${}\n", temp_var));
                result.push_str("clearb\n");
                result.push_str(&value_result.code);
                result.push_str("setr1\n");
                result.push_str("clearb\n");
                result.push_str(&format!("loadv ${}\n", temp_var));
                result.push_str("setp1\n");
                result.push_str(&format!("clearv ${}\n", temp_var));
                result.push_str("clearr1\n");
                state.free_variable(temp_var);
                buffer_filled = true;
            }

            Statement::Binary(binary) => {
                let left_result = Self::compile_statement(&binary.left, state)?;
                result.push_str(&left_result.code);
                let r1 = state.get_temp_variable();
                result.push_str(&format!("crsetv ${}\n", r1));
                result.push_str("clearb\n");
                
                let right_result = Self::compile_statement(&binary.right, state)?;
                result.push_str(&right_result.code);
                result.push_str("setr2\nclearb\n");
                result.push_str(&format!("loadv ${}\n", r1));
                result.push_str("setr1\nclearb\n");
                result.push_str(&format!("op {}\n", binary.operator));
                result.push_str("clearr1\nclearr2\n");
                result.push_str(&format!("clearv ${}\n", r1));
                state.free_variable(r1);
                buffer_filled = true;
            }

            Statement::Unary(unary) => {
                let _temp = state.get_temp_variable();
                match unary.operator {
                    TokenType::LogicalNot | TokenType::BitNot => {
                        let value_result = Self::compile_statement(&unary.value, state)?;
                        result.push_str(&value_result.code);
                        result.push_str("setr1\nclearb\n");
                        let op = if unary.operator == TokenType::LogicalNot { "!" } else { "~" };
                        result.push_str(&format!("op {}\n", op));
                        result.push_str("clearr1\n");
                        state.free_variable(_temp);
                        buffer_filled = true;
                    }
                    _ => {
                        state.free_variable(_temp);
                        return Err("Unsupported unary operator".into());
                    }
                }
            }

            Statement::Ternary(ternary) => {
                let true_label = state.get_label();
                let false_label = state.get_label();
                let end_label = state.get_label();

                let cond_result = Self::compile_statement(&ternary.condition, state)?;
                result.push_str(&cond_result.code);
                result.push_str(&format!("jpnz ${ }\n", true_label));
                result.push_str("clearb\n");
                
                result.push_str(&format!("label ${}\n", false_label));
                let false_result = Self::compile_statement(&ternary.false_value, state)?;
                result.push_str(&false_result.code);
                result.push_str(&format!("jmp ${}\n", end_label));
                
                result.push_str(&format!("label ${}\n", true_label));
                let true_result = Self::compile_statement(&ternary.true_value, state)?;
                result.push_str(&true_result.code);
                
                result.push_str(&format!("label ${}\n", end_label));
                buffer_filled = true;
            }

            Statement::PropertyAccess(prop) => {
                let parent_result = Self::compile_statement(&prop.parent, state)?;
                result.push_str(&parent_result.code);
                result.push_str(&format!("getp {}\n", prop.property));
                buffer_filled = true;
            }

            Statement::ArrayAccess(arr) => {
                let parent_result = Self::compile_statement(&arr.parent, state)?;
                result.push_str(&parent_result.code);
                let parent_var = state.get_temp_variable();
                result.push_str(&format!("crsetv ${}\n", parent_var));
                result.push_str("clearb\n");
                
                let index_result = Self::compile_statement(&arr.index, state)?;
                result.push_str(&index_result.code);
                result.push_str("setr2\n");
                result.push_str("clearb\n");
                
                result.push_str(&format!("loadv ${}\n", parent_var));
                result.push_str(&format!("clearv ${}\n", parent_var));
                result.push_str("setr1\n");
                result.push_str("clearb\n");
                state.free_variable(parent_var);
                
                result.push_str("op [\n");
                result.push_str("clearr1\n");
                buffer_filled = true;
            }

            Statement::Array(arr) => {
                for element in &arr.elements {
                    let elem_result = Self::compile_statement(element, state)?;
                    result.push_str(&elem_result.code);
                    result.push_str("pushm\n");
                    result.push_str("clearb\n");
                }
                result.push_str(&format!("setb array {}\n", arr.elements.len()));
                result.push_str(&format!("apush {}\n", arr.elements.len()));
                buffer_filled = true;
            }

            Statement::If(if_stmt) => {
                let else_label = state.get_label();
                let end_label = state.get_label();

                let cond_result = Self::compile_statement(&if_stmt.condition, state)?;
                result.push_str(&cond_result.code);
                result.push_str(&format!("jmpz ${}\n", else_label));
                result.push_str("clearb\n");

                let then_result = Self::compile_statement(&if_stmt.then_branch, state)?;
                result.push_str(&then_result.code);
                if then_result.should_clear_buffer {
                    result.push_str("clearb\n");
                }

                if let Statement::Declaration(decl) = if_stmt.then_branch.as_ref() {
                    result.push_str(&format!("clearv {}\n", decl.name));
                }

                result.push_str(&format!("jmp ${}\n", end_label));
                result.push_str(&format!("label ${}\n", else_label));

                if let Some(else_branch) = &if_stmt.else_branch {
                    let else_result = Self::compile_statement(else_branch, state)?;
                    result.push_str(&else_result.code);
                    if else_result.should_clear_buffer {
                        result.push_str("clearb\n");
                    }

                    if let Statement::Declaration(decl) = else_branch.as_ref() {
                        result.push_str(&format!("clearv {}\n", decl.name));
                    }
                }

                result.push_str(&format!("label ${}\n", end_label));
            }

            Statement::While(while_stmt) => {
                let start_label = state.get_label();
                let end_label = state.get_label();

                let previous_continue = state.last_continue_label.clone();
                let previous_end = state.last_break_label.clone();

                state.last_continue_label = Some(format!("${}", start_label));
                state.last_break_label = Some(format!("${}", end_label));

                result.push_str(&format!("label ${}\n", start_label));
                let cond_result = Self::compile_statement(&while_stmt.condition, state)?;
                result.push_str(&cond_result.code);
                result.push_str(&format!("jmpz ${}\n", end_label));
                result.push_str("clearb\n");

                let body_result = Self::compile_statement(&while_stmt.body, state)?;
                result.push_str(&body_result.code);
                if let Statement::Declaration(decl) = while_stmt.body.as_ref() {
                    result.push_str(&format!("clearv {}\n", decl.name));
                }

                result.push_str(&format!("jmp ${}\n", start_label));
                result.push_str(&format!("label ${}\n", end_label));

                state.last_continue_label = previous_continue;
                state.last_break_label = previous_end;
            }

            Statement::DoWhile(do_while) => {
                let start_label = state.get_label();
                let no_check_start_label = state.get_label();
                let end_label = state.get_label();

                let previous_continue = state.last_continue_label.clone();
                let previous_end = state.last_break_label.clone();

                state.last_continue_label = Some(format!("${}", start_label));
                state.last_break_label = Some(format!("${}", end_label));

                result.push_str(&format!("label ${}\n", no_check_start_label));
                let body_result = Self::compile_statement(&do_while.body, state)?;
                result.push_str(&body_result.code);
                if let Statement::Declaration(decl) = do_while.body.as_ref() {
                    result.push_str(&format!("clearv {}\n", decl.name));
                }

                result.push_str(&format!("label ${}\n", start_label));
                let cond_result = Self::compile_statement(&do_while.condition, state)?;
                result.push_str(&cond_result.code);
                result.push_str(&format!("jpnz ${}\n", no_check_start_label));
                result.push_str("clearb\n");
                result.push_str(&format!("label ${}\n", end_label));

                state.last_continue_label = previous_continue;
                state.last_break_label = previous_end;
            }

            Statement::For(for_stmt) => {
                let start_label = state.get_label();
                let continue_label = state.get_label();
                let end_label = state.get_label();

                let previous_continue = state.last_continue_label.clone();
                let previous_end = state.last_break_label.clone();

                if let Some(initializer) = &for_stmt.initializer {
                    let init_result = Self::compile_statement(initializer, state)?;
                    result.push_str(&init_result.code);
                    if init_result.should_clear_buffer {
                        result.push_str("clearb\n");
                    }
                }

                state.last_continue_label = Some(format!("${}", continue_label));
                state.last_break_label = Some(format!("${}", end_label));

                result.push_str(&format!("label ${}\n", start_label));

                if let Some(condition) = &for_stmt.condition {
                    let cond_result = Self::compile_statement(condition, state)?;
                    result.push_str(&cond_result.code);
                    result.push_str(&format!("jmpz ${}\n", end_label));
                    result.push_str("clearb\n");
                }

                let body_result = Self::compile_statement(&for_stmt.body, state)?;
                result.push_str(&body_result.code);
                if let Statement::Declaration(decl) = for_stmt.body.as_ref() {
                    result.push_str(&format!("clearv {}\n", decl.name));
                }

                result.push_str(&format!("label ${}\n", continue_label));

                if let Some(increment) = &for_stmt.increment {
                    let inc_result = Self::compile_statement(increment, state)?;
                    result.push_str(&inc_result.code);
                    if inc_result.should_clear_buffer {
                        result.push_str("clearb\n");
                    }
                }

                result.push_str(&format!("jmp ${}\n", start_label));
                result.push_str(&format!("label ${}\n", end_label));

                if let Some(init) = &for_stmt.initializer {
                    if let Statement::Declaration(decl) = init.as_ref() {
                        result.push_str(&format!("clearv {}\n", decl.name));
                    }
                }

                state.last_continue_label = previous_continue;
                state.last_break_label = previous_end;
            }

            Statement::Loop(loop_stmt) => {
                let start_label = state.get_label();
                let end_label = state.get_label();

                let previous_continue = state.last_continue_label.clone();
                let previous_end = state.last_break_label.clone();

                state.last_continue_label = Some(format!("${}", start_label));
                state.last_break_label = Some(format!("${}", end_label));

                result.push_str(&format!("label ${}\n", start_label));
                let body_result = Self::compile_statement(&loop_stmt.body, state)?;
                result.push_str(&body_result.code);
                if let Statement::Declaration(decl) = loop_stmt.body.as_ref() {
                    result.push_str(&format!("clearv {}\n", decl.name));
                }
                result.push_str(&format!("jmp ${}\n", start_label));
                result.push_str(&format!("label ${}\n", end_label));

                state.last_continue_label = previous_continue;
                state.last_break_label = previous_end;
            }

            Statement::Break(break_stmt) => {
                if let Some(label) = state.last_break_label.as_ref() {
                    if break_stmt.label.is_some() {
                        return Err("Break statement labels are not supported yet".into());
                    }
                    result.push_str(&format!("jmp {}\n", label));
                } else {
                    return Err("Break statement outside of loop".into());
                }
            }

            Statement::Continue(continue_stmt) => {
                if let Some(label) = state.last_continue_label.as_ref() {
                    if continue_stmt.label.is_some() {
                        return Err("Continue statement labels are not supported yet".into());
                    }
                    result.push_str(&format!("jmp {}\n", label));
                } else {
                    return Err("Continue statement outside of loop".into());
                }
            }

            Statement::LiterallyDontCare(stmt) => {
                result.push_str(&format!("{}\n", stmt.code));
                buffer_filled = true;
            }
        }

        Ok(StatementBytecode::new(result, buffer_filled))
    }
}
