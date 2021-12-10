import Tokenizer from "./Tokenizer";
import * as fs from "fs-extra";
import path from "path";
import { Lexer } from "./Lexer";

async function main() {
  const script = (await fs.readFile(path.join(__dirname, "../test.moose"))).toString();
  const tokenizer = new Tokenizer(script, "test.moose");
  const { tokens } = tokenizer.parse();
  const lexer = new Lexer(tokens, script, "test.moose");
  const elementTree = lexer.parse();
  fs.writeFile("test.moose_tokenized.json", JSON.stringify(tokens, (key, value) => {
    return key === "position" ? `${value.line},${value.column}` : value;
  }, 2));
  fs.writeFile("test.moose_lexed.json", JSON.stringify(elementTree.elements, (key, value) => {
    return key === "position" ? `${value.line},${value.column}` : value;
  }, 2));
}

main();