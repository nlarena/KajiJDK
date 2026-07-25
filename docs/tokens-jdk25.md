# Tokens de `javac` (JDK 25) — referencia para el lexer

Listado **autoritativo** de los tokens que reconoce el lexer de `javac`: el enum
`TokenKind` de `com.sun.tools.javac.parser.Tokens`, **115 constantes**.

- **Fuente:** `.jdk25_tmp/jdk-25.0.3+9/lib/src.zip` → `jdk.compiler/com/sun/tools/javac/parser/Tokens.java`
- **Target del proyecto:** JDK 25 / class-file **v69**. Oráculo de differential testing: el `javac`/`javap` de `.jdk25_tmp/jdk-25.0.3+9/bin/`.
- Cada token lleva un `Tag`: `NUMERIC` (literal numérico) · `STRING` (literal de string) · `NAMED` (puede aparecer donde se espera un nombre) · `DEFAULT` (puntuación y keywords comunes).

---

## 1. Especiales / sintéticos (4)

| TokenKind | Rol | Tag |
|---|---|---|
| `EOF` | fin de entrada | DEFAULT |
| `ERROR` | símbolo inválido (recuperación de errores) | DEFAULT |
| `IDENTIFIER` | identificador | NAMED |
| `CUSTOM` | reservado para extensiones internas | DEFAULT |

## 2. Palabras reservadas (50)

Todas con su lexema literal. **`const` y `goto` son reservadas pero sin uso** — existen
solo para dar un error claro si aparecen. Las marcadas `NAMED` son los keywords que pueden
figurar donde se espera un nombre (los tipos primitivos, `this`/`super`, `assert`, `enum`).

| TokenKind | Lexema | Tag | | TokenKind | Lexema | Tag |
|---|---|---|---|---|---|---|
| `ABSTRACT` | `abstract` | DEFAULT | | `INTERFACE` | `interface` | DEFAULT |
| `ASSERT` | `assert` | NAMED | | `LONG` | `long` | NAMED |
| `BOOLEAN` | `boolean` | NAMED | | `NATIVE` | `native` | DEFAULT |
| `BREAK` | `break` | DEFAULT | | `NEW` | `new` | DEFAULT |
| `BYTE` | `byte` | NAMED | | `PACKAGE` | `package` | DEFAULT |
| `CASE` | `case` | DEFAULT | | `PRIVATE` | `private` | DEFAULT |
| `CATCH` | `catch` | DEFAULT | | `PROTECTED` | `protected` | DEFAULT |
| `CHAR` | `char` | NAMED | | `PUBLIC` | `public` | DEFAULT |
| `CLASS` | `class` | DEFAULT | | `RETURN` | `return` | DEFAULT |
| `CONST` | `const` (sin uso) | DEFAULT | | `SHORT` | `short` | NAMED |
| `CONTINUE` | `continue` | DEFAULT | | `STATIC` | `static` | DEFAULT |
| `DEFAULT` | `default` | DEFAULT | | `STRICTFP` | `strictfp` | DEFAULT |
| `DO` | `do` | DEFAULT | | `SUPER` | `super` | NAMED |
| `DOUBLE` | `double` | NAMED | | `SWITCH` | `switch` | DEFAULT |
| `ELSE` | `else` | DEFAULT | | `SYNCHRONIZED` | `synchronized` | DEFAULT |
| `ENUM` | `enum` | NAMED | | `THIS` | `this` | NAMED |
| `EXTENDS` | `extends` | DEFAULT | | `THROW` | `throw` | DEFAULT |
| `FINAL` | `final` | DEFAULT | | `THROWS` | `throws` | DEFAULT |
| `FINALLY` | `finally` | DEFAULT | | `TRANSIENT` | `transient` | DEFAULT |
| `FLOAT` | `float` | NAMED | | `TRY` | `try` | DEFAULT |
| `FOR` | `for` | DEFAULT | | `VOID` | `void` | NAMED |
| `GOTO` | `goto` (sin uso) | DEFAULT | | `VOLATILE` | `volatile` | DEFAULT |
| `IF` | `if` | DEFAULT | | `WHILE` | `while` | DEFAULT |
| `IMPLEMENTS` | `implements` | DEFAULT | | | | |
| `IMPORT` | `import` | DEFAULT | | | | |
| `INSTANCEOF` | `instanceof` | DEFAULT | | | | |
| `INT` | `int` | NAMED | | | | |

## 3. Literales (7)

| TokenKind | Qué | Tag |
|---|---|---|
| `INTLITERAL` | entero (`42`, `0x1F`, `0b101`, `07`) | NUMERIC |
| `LONGLITERAL` | long (`42L`) | NUMERIC |
| `FLOATLITERAL` | float (`3.14f`) | NUMERIC |
| `DOUBLELITERAL` | double (`3.14`, `1e9`) | NUMERIC |
| `CHARLITERAL` | char (`'a'`) | NUMERIC |
| `STRINGLITERAL` | string (`"..."`, text block `"""..."""`) | STRING |
| `STRINGFRAGMENT` | trozo de string template / text block | STRING |

## 4. Literales-palabra y `_` (4)

Son **tokens propios**, no identificadores — el lexer los separa.

| TokenKind | Lexema | Tag |
|---|---|---|
| `TRUE` | `true` | NAMED |
| `FALSE` | `false` | NAMED |
| `NULL` | `null` | NAMED |
| `UNDERSCORE` | `_` (keyword desde Java 9) | NAMED |

## 5. Separadores (13)

| TokenKind | Lexema | | TokenKind | Lexema |
|---|---|---|---|---|
| `ARROW` | `->` | | `RBRACKET` | `]` |
| `COLCOL` | `::` | | `SEMI` | `;` |
| `LPAREN` | `(` | | `COMMA` | `,` |
| `RPAREN` | `)` | | `DOT` | `.` |
| `LBRACE` | `{` | | `ELLIPSIS` | `...` |
| `RBRACE` | `}` | | `MONKEYS_AT` | `@` |
| `LBRACKET` | `[` | | | |

## 6. Operadores (37)

| TokenKind | Lexema | | TokenKind | Lexema | | TokenKind | Lexema |
|---|---|---|---|---|---|---|---|
| `EQ` | `=` | | `PLUSPLUS` | `++` | | `LTLT` | `<<` |
| `GT` | `>` | | `SUBSUB` | `--` | | `GTGT` | `>>` |
| `LT` | `<` | | `PLUS` | `+` | | `GTGTGT` | `>>>` |
| `BANG` | `!` | | `SUB` | `-` | | `PLUSEQ` | `+=` |
| `TILDE` | `~` | | `STAR` | `*` | | `SUBEQ` | `-=` |
| `QUES` | `?` | | `SLASH` | `/` | | `STAREQ` | `*=` |
| `COLON` | `:` | | `AMP` | `&` | | `SLASHEQ` | `/=` |
| `EQEQ` | `==` | | `BAR` | `\|` | | `AMPEQ` | `&=` |
| `LTEQ` | `<=` | | `CARET` | `^` | | `BAREQ` | `\|=` |
| `GTEQ` | `>=` | | `PERCENT` | `%` | | `CARETEQ` | `^=` |
| `BANGEQ` | `!=` | | | | | `PERCENTEQ` | `%=` |
| `AMPAMP` | `&&` | | | | | `LTLTEQ` | `<<=` |
| `BARBAR` | `\|\|` | | | | | `GTGTEQ` | `>>=` |
| | | | | | | `GTGTGTEQ` | `>>>=` |

---

## Notas de diseño para el lexer

1. **Las keywords contextuales NO son tokens.** `var`, `yield`, `record`, `sealed`,
   `permits`, `non-sealed`, `module`, `requires`, `open`, `exports`, `provides`, `uses`,
   `to`, `with`, `transitive`, `when`… **no** están en `TokenKind`. javac las lexea como
   `IDENTIFIER` y las reconoce **el parser por contexto**. El lexer solo necesita las 50
   reservadas clásicas.
2. **`true`/`false`/`null` y `_` son tokens propios**, no identificadores.
3. **`const` y `goto`** se tokenizan (para el error), pero la gramática nunca los acepta.
4. **Máximo mordisco (maximal munch):** ante `>>>=` el lexer toma el operador más largo
   posible; `>>` vs `>` se decide igual. (Ojo con genéricos: `List<List<T>>` — javac
   parte los `>>` en el *parser*, no en el lexer.)
5. **Display / `toString()`** (útil si armás un oráculo de tokens para diffear): los
   literales se muestran como `token.integer`, `token.string`, etc.; `IDENTIFIER` →
   `token.identifier`; `EOF` → `token.end-of-input`; los separadores `. , ; ( ) [ ] { }`
   se muestran entre comillas (`'('`); el resto muestra su lexema.

## Subconjunto mínimo para B0 (`Add.java`)

Para arrancar el lexer con `Add.java` alcanza con: `IDENTIFIER`, `INTLITERAL`, las
keywords `public`/`static`/`int`/`void`/`class`/`return`, los separadores
`( ) { } ; , .`, y los operadores `= + -`. Más `EOF`. El resto se agrega al crecer.
