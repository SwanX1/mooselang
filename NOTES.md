# Development of the moose programming language

Table of contents:
 - [Initial Comments](#06092021-1648-utc--initial-comments)
   - [Initial thoughts](#initial-thoughts)
   - [Initial syntax](#initial-syntax)
 - [Chained comparison operators](#06092021-1130-utc--chained-comparison-operators)
 - [Template literals](#06092021-1645-utc--template-literals)
 - [Special characters/tokens](#07092021-1906-utc--special-characterstokens)
 - [I forgor 💀](#10122021-0635-utc--i-forgor-)

### 06/09/2021 16:48 UTC ● Initial comments.

#### Initial thoughts:
2 days ago I decided to create a programming language. It would need to fulfill the following criteria:
 - Typed/static.
 - Interpreted.
 - Easy to implement.
 - Easy to learn.
 - Can easily be sandboxed.
 - Interpreter can be extended with built-in global functions/variables, but only very basic functions would be provided by default.
 - Can be AOT compiled into bytecode represented by JSON (not really "byte"code is it?).

#### Initial syntax
##### Code sample from: 04/09/2021 18:03 UTC

Complex hello world and printing the value of `5 + 2`:

    export decl func main = (string[] args) void {
      print(getGreeting());
      decl const int a = 2;
      decl int b;
      b = 5;
      print(a + b);
      return 0;
    }

    decl const string DEFAULT_GREETING_NAME = "World";

    decl func getGreeting = () string {
      return getGreeting(DEFAULT_GREETING_NAME);
    }

    decl func getGreeting = (string name) string {
      return "Hello " + name + "!";
    }

`decl` is used as a variable declaration keyword.<br>
`export` is used to export declared variables into an "outer scope", so to say, so that other files can import them.<br>
The keyword `import` (syntax currently unknown) would be used for this.<br>
Syntax of `import` would be specified later.

After initially discussing my decision to create a functional typed interpreted language with my friend Moose (that's a nickname, of course), on a whim I decided to name the language "moose".

The extension would be `.moose` or `.mse`, both would be supported.<br>
The MIME type would be `text/moose`, but not `test/mse`. This type is not official and shouldn't be expected to be supported by anyone.

After showing my friend a similar code sample to the above provided one, I realized that the `main` function should return an int as languages that do this use it as an exit code.

I also decided to remove the `decl` keyword after this comment from him:
> I'm finding the use of decl everywhere to be a little strange, I think I'd get annoyed with that but it's not world ending<br>
> it looks a bit like rust right now so you aren't too avant garde with it

Another thing that's present in the code sample above, there's function overloading, however with how I've designed the language, that wouldn't work because functions are variables.

Revised code sample:

    export func main = (string[] args) int {
      print(getGreeting());
      const int a = 2;
      int b;
      b = 5;
      print(a + b);
      return 0;
    }

    const string DEFAULT_GREETING_NAME = "World";

    func getGreeting = (string name?) string {
      if (name == null || name == DEFAULT_GREETING_NAME) {
        return getGreeting(DEFAULT_GREETING_NAME);
      } else {
        return "Hello " + name + "!";
      }
    }

### 06/09/2021 11:30 UTC ● Chained comparison operators

I've now realized that most languages don't have support for chained comparison operators.

    // this works in moose
    if (1 < x <= 10) {
      // code
    }

    // this also works
    if (1 < x && x <= 10) {
      // code
    }

    // something like this could be possible
    if (a < b < c < d) {
      // code
    }

### 06/09/2021 16:45 UTC ● Template literals

I now realize that template literal support will eventually be required.
Normal strings are written with `"`, template literals strings are written with `'`:

    string world = "Planet";
    print("Hello {world}!"); // expected output: Hello {world}!
    print('Hello {world}!'); // expected output: Hello Planet!
    print('Hello \{world\}!'); // expected output: Hello {world}!
    print('Hello \{world}!'); // expected output: Hello {world}!
    print('Hello {world\}!'); // syntax error: Unterminated template literal

`'Hello {world}!'` would be interpreted as `"Hello " + world + "!"`

### 07/09/2021 19:06 UTC ● Special characters/tokens

Documenting this so I don't forget it later on:

    "(", ")", "{", "}", "[", "]",     // Brackets
    "<<", ">>",                       // Bitshift operators
    "==", "!=", "<=", ">=", "<", ">", // Equality operators
    "!", "&&", "||", "^^",            // Boolean operators
    "++", "--", "=", ".",             // Miscellaneous operators
    "%", "-", "+", "/", "**", "*",    // Math operators
    "^", "&", "~", "|",               // Bitwise operators

### 10/12/2021 06:35 UTC ● I forgor 💀

I literally forgot about this for like 3 months, my bad g's.

I had already started writing the lexer when I last worked on this (3 months ago), but now I don't understand any of the code I wrote, so I'm rewriting everything except the tokenizer, that already caused me too much pain.

I'm thinking of the possibility that a virtual machine could be used, similar to Java. I'm also considering the idea of Java where a LOT of utilities are coded for you.

I still want pointers tho...