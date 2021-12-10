export class InterpreterError extends Error {
  constructor(message: string, script: string, line: number, column: number) {
    let scriptLines = script.replace(/\x1b/g, " ").split('\n');
    let lines = [
      message.replace(/\{position\}/g, `line ${line}, column ${column}`)
    ];
    for (let i = -2; i < 2; i++) {
      if (i === -1) {
        const lineToColor = scriptLines[line + i];
        let coloredLine = [
          lineToColor.slice(0, column - 1),
          "\x1b[31m",
          lineToColor.slice(column - 1, lineToColor.indexOf(" ", column)),
          "\x1b[39m",
          lineToColor.slice(lineToColor.indexOf(" ", column), lineToColor.length)
        ].join("");
        lines.push(`\x1b[90m${(line + i + 1).toString().padStart((line + 3).toString().length, " ")} | \x1b[39m${coloredLine}`);
        lines.push(`\x1b[90m${"".padStart((line + 2).toString().length, " ")} | \x1b[33m${"^".padStart(column, " ")}\x1b[39m`);
      } else if (line + i > 0 && scriptLines.length >= line + i) {
        lines.push(`\x1b[90m${(line + i + 1).toString().padStart((line + 3).toString().length, " ")} | ${scriptLines[line + i]}\x1b[39m`);
      }
    }
    lines = lines.map(line => line.trimEnd().substring(0, 80));
    super(`\n${lines.join("\n")}`);
  }
}

export class ParseError extends InterpreterError { }
export class SyntaxError extends InterpreterError { }