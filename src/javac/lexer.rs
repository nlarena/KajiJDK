//! El **lexer** (scanner): recorre el texto `.java` carácter a carácter y lo convierte en
//! una secuencia de [`Token`]s, descartando espacios y comentarios y clasificando cada
//! lexema en su [`TokenKind`]. Aplica *maximal munch* (toma el operador más largo posible:
//! `>>>=` antes que `>>>`, `>>`, `>`).
//!
//! Hito **B0**. Cubre el subconjunto clásico: identificadores, las 50 keywords (+
//! `true`/`false`/`null`/`_`), literales `int`/`long`/`float`/`double`/`char`/`String`
//! (incl. hex/bin/octal y `_` en números, text blocks), y todos los separadores/operadores.

use super::token::{Token, TokenKind};
use super::Error;

/// Tokeniza `source` completo, terminando siempre con un token [`TokenKind::Eof`]. Falla con
/// un [`Error`] posicionado ante un carácter inesperado o un literal sin cerrar.
pub fn tokenize(source: &str) -> Result<Vec<Token>, Error> {
    Lexer::new(source).run()
}

struct Lexer {
    chars: Vec<char>,
    pos: usize,
    line: u32,
    col: u32,
}

impl Lexer {
    fn new(source: &str) -> Self {
        let mut chars: Vec<char> = source.chars().collect();
        // Un BOM inicial (U+FEFF) no es parte del programa — javac lo ignora.
        if chars.first() == Some(&'\u{feff}') {
            chars.remove(0);
        }
        Lexer { chars, pos: 0, line: 1, col: 1 }
    }

    fn peek(&self) -> Option<char> {
        self.chars.get(self.pos).copied()
    }

    fn peek_at(&self, offset: usize) -> Option<char> {
        self.chars.get(self.pos + offset).copied()
    }

    /// Consume el carácter actual, avanzando la posición y el contador de línea/columna.
    fn bump(&mut self) -> Option<char> {
        let c = self.peek()?;
        self.pos += 1;
        if c == '\n' {
            self.line += 1;
            self.col = 1;
        } else {
            self.col += 1;
        }
        Some(c)
    }

    fn error(&self, message: impl Into<String>) -> Error {
        Error::new(message, self.line, self.col)
    }

    fn run(mut self) -> Result<Vec<Token>, Error> {
        let mut tokens = Vec::new();
        loop {
            self.skip_trivia()?;
            let (line, col) = (self.line, self.col);
            let Some(c) = self.peek() else {
                tokens.push(Token { kind: TokenKind::Eof, text: String::new(), line, col });
                return Ok(tokens);
            };
            let (kind, text) = if is_ident_start(c) {
                self.scan_ident_or_keyword()
            } else if c.is_ascii_digit() || (c == '.' && self.peek_at(1).is_some_and(|d| d.is_ascii_digit())) {
                self.scan_number()?
            } else if c == '"' {
                self.scan_string()?
            } else if c == '\'' {
                self.scan_char()?
            } else {
                self.scan_operator()?
            };
            tokens.push(Token { kind, text, line, col });
        }
    }

    /// Salta espacios en blanco y comentarios (`//` de línea y `/* */` de bloque).
    fn skip_trivia(&mut self) -> Result<(), Error> {
        loop {
            match self.peek() {
                Some(c) if c.is_whitespace() => {
                    self.bump();
                }
                Some('/') if self.peek_at(1) == Some('/') => {
                    while let Some(c) = self.peek() {
                        if c == '\n' {
                            break;
                        }
                        self.bump();
                    }
                }
                Some('/') if self.peek_at(1) == Some('*') => {
                    self.bump();
                    self.bump();
                    loop {
                        match self.peek() {
                            None => return Err(self.error("comentario de bloque sin cerrar")),
                            Some('*') if self.peek_at(1) == Some('/') => {
                                self.bump();
                                self.bump();
                                break;
                            }
                            _ => {
                                self.bump();
                            }
                        }
                    }
                }
                _ => return Ok(()),
            }
        }
    }

    fn scan_ident_or_keyword(&mut self) -> (TokenKind, String) {
        let mut text = String::new();
        while let Some(c) = self.peek() {
            if is_ident_part(c) {
                text.push(c);
                self.bump();
            } else {
                break;
            }
        }
        let kind = TokenKind::keyword(&text).unwrap_or(TokenKind::Identifier);
        (kind, text)
    }

    /// Números: decimal/hex (`0x`)/binario (`0b`)/octal, con `_` separadores, punto decimal,
    /// exponente (`e`/`E`) y sufijos (`l`/`L`, `f`/`F`, `d`/`D`). El *kind* se decide por el
    /// sufijo o por tener parte fraccionaria/exponente.
    fn scan_number(&mut self) -> Result<(TokenKind, String), Error> {
        let mut text = String::new();
        let mut is_floating = false;

        let radix_hex = self.peek() == Some('0')
            && matches!(self.peek_at(1), Some('x') | Some('X'));
        let radix_bin = self.peek() == Some('0')
            && matches!(self.peek_at(1), Some('b') | Some('B'));

        if radix_hex || radix_bin {
            text.push(self.bump().unwrap()); // 0
            text.push(self.bump().unwrap()); // x / b
            while let Some(c) = self.peek() {
                if c.is_ascii_hexdigit() || c == '_' {
                    text.push(c);
                    self.bump();
                } else {
                    break;
                }
            }
        } else {
            while let Some(c) = self.peek() {
                if c.is_ascii_digit() || c == '_' {
                    text.push(c);
                    self.bump();
                } else {
                    break;
                }
            }
            // Parte fraccionaria: `.` seguido de dígitos (o el `.` inicial de `.5`).
            if self.peek() == Some('.') && self.peek_at(1).is_some_and(|d| d.is_ascii_digit() || d == '_')
                || self.peek() == Some('.') && text.is_empty()
            {
                is_floating = true;
                text.push(self.bump().unwrap()); // .
                while let Some(c) = self.peek() {
                    if c.is_ascii_digit() || c == '_' {
                        text.push(c);
                        self.bump();
                    } else {
                        break;
                    }
                }
            } else if self.peek() == Some('.') && text.chars().all(|c| c.is_ascii_digit() || c == '_') {
                // Caso `5.` (fracción vacía).
                is_floating = true;
                text.push(self.bump().unwrap());
            }
            // Exponente.
            if matches!(self.peek(), Some('e') | Some('E')) {
                is_floating = true;
                text.push(self.bump().unwrap());
                if matches!(self.peek(), Some('+') | Some('-')) {
                    text.push(self.bump().unwrap());
                }
                while let Some(c) = self.peek() {
                    if c.is_ascii_digit() || c == '_' {
                        text.push(c);
                        self.bump();
                    } else {
                        break;
                    }
                }
            }
        }

        // Sufijo.
        let kind = match self.peek() {
            Some('l') | Some('L') => {
                text.push(self.bump().unwrap());
                TokenKind::LongLiteral
            }
            Some('f') | Some('F') => {
                text.push(self.bump().unwrap());
                TokenKind::FloatLiteral
            }
            Some('d') | Some('D') => {
                text.push(self.bump().unwrap());
                TokenKind::DoubleLiteral
            }
            _ if is_floating => TokenKind::DoubleLiteral,
            _ => TokenKind::IntLiteral,
        };
        Ok((kind, text))
    }

    /// String `"..."` (con escapes) o text block `"""..."""`. `text` guarda el lexema crudo
    /// **incluidas** las comillas y los escapes — el parser lo decodifica.
    fn scan_string(&mut self) -> Result<(TokenKind, String), Error> {
        let mut text = String::new();
        // ¿text block?  `"""`
        if self.peek() == Some('"') && self.peek_at(1) == Some('"') && self.peek_at(2) == Some('"') {
            for _ in 0..3 {
                text.push(self.bump().unwrap());
            }
            loop {
                match self.peek() {
                    None => return Err(self.error("text block sin cerrar")),
                    Some('"') if self.peek_at(1) == Some('"') && self.peek_at(2) == Some('"') => {
                        for _ in 0..3 {
                            text.push(self.bump().unwrap());
                        }
                        return Ok((TokenKind::StringLiteral, text));
                    }
                    Some('\\') => {
                        text.push(self.bump().unwrap());
                        if let Some(c) = self.bump() {
                            text.push(c);
                        }
                    }
                    _ => text.push(self.bump().unwrap()),
                }
            }
        }
        // String normal.
        text.push(self.bump().unwrap()); // "
        loop {
            match self.peek() {
                None | Some('\n') => return Err(self.error("string literal sin cerrar")),
                Some('"') => {
                    text.push(self.bump().unwrap());
                    return Ok((TokenKind::StringLiteral, text));
                }
                Some('\\') => {
                    text.push(self.bump().unwrap());
                    if let Some(c) = self.bump() {
                        text.push(c);
                    }
                }
                _ => text.push(self.bump().unwrap()),
            }
        }
    }

    /// Char `'a'` (con escapes). `text` incluye las comillas.
    fn scan_char(&mut self) -> Result<(TokenKind, String), Error> {
        let mut text = String::new();
        text.push(self.bump().unwrap()); // '
        loop {
            match self.peek() {
                None | Some('\n') => return Err(self.error("char literal sin cerrar")),
                Some('\'') => {
                    text.push(self.bump().unwrap());
                    return Ok((TokenKind::CharLiteral, text));
                }
                Some('\\') => {
                    text.push(self.bump().unwrap());
                    if let Some(c) = self.bump() {
                        text.push(c);
                    }
                }
                _ => text.push(self.bump().unwrap()),
            }
        }
    }

    /// Separadores y operadores, con *maximal munch*.
    fn scan_operator(&mut self) -> Result<(TokenKind, String), Error> {
        use TokenKind::*;
        let c = self.bump().unwrap();
        // Devuelve `kind` consumiendo `extra` caracteres adicionales del que ya tomamos.
        let take = |lexer: &mut Lexer, extra: usize, kind: TokenKind| {
            let mut text = String::from(c);
            for _ in 0..extra {
                if let Some(x) = lexer.bump() {
                    text.push(x);
                }
            }
            (kind, text)
        };
        let n1 = self.peek();
        let n2 = self.peek_at(1);
        let n3 = self.peek_at(2);
        let out = match c {
            '(' => take(self, 0, LParen),
            ')' => take(self, 0, RParen),
            '{' => take(self, 0, LBrace),
            '}' => take(self, 0, RBrace),
            '[' => take(self, 0, LBracket),
            ']' => take(self, 0, RBracket),
            ';' => take(self, 0, Semi),
            ',' => take(self, 0, Comma),
            '@' => take(self, 0, MonkeysAt),
            '~' => take(self, 0, Tilde),
            '?' => take(self, 0, Ques),
            '.' if n1 == Some('.') && n2 == Some('.') => take(self, 2, Ellipsis),
            '.' => take(self, 0, Dot),
            ':' if n1 == Some(':') => take(self, 1, ColCol),
            ':' => take(self, 0, Colon),
            '=' if n1 == Some('=') => take(self, 1, EqEq),
            '=' => take(self, 0, Eq),
            '!' if n1 == Some('=') => take(self, 1, BangEq),
            '!' => take(self, 0, Bang),
            '+' if n1 == Some('+') => take(self, 1, PlusPlus),
            '+' if n1 == Some('=') => take(self, 1, PlusEq),
            '+' => take(self, 0, Plus),
            '-' if n1 == Some('-') => take(self, 1, SubSub),
            '-' if n1 == Some('=') => take(self, 1, SubEq),
            '-' if n1 == Some('>') => take(self, 1, Arrow),
            '-' => take(self, 0, Sub),
            '*' if n1 == Some('=') => take(self, 1, StarEq),
            '*' => take(self, 0, Star),
            '/' if n1 == Some('=') => take(self, 1, SlashEq),
            '/' => take(self, 0, Slash),
            '%' if n1 == Some('=') => take(self, 1, PercentEq),
            '%' => take(self, 0, Percent),
            '^' if n1 == Some('=') => take(self, 1, CaretEq),
            '^' => take(self, 0, Caret),
            '&' if n1 == Some('&') => take(self, 1, AmpAmp),
            '&' if n1 == Some('=') => take(self, 1, AmpEq),
            '&' => take(self, 0, Amp),
            '|' if n1 == Some('|') => take(self, 1, BarBar),
            '|' if n1 == Some('=') => take(self, 1, BarEq),
            '|' => take(self, 0, Bar),
            '<' if n1 == Some('<') && n2 == Some('=') => take(self, 2, LtLtEq),
            '<' if n1 == Some('<') => take(self, 1, LtLt),
            '<' if n1 == Some('=') => take(self, 1, LtEq),
            '<' => take(self, 0, Lt),
            '>' if n1 == Some('>') && n2 == Some('>') && n3 == Some('=') => take(self, 3, GtGtGtEq),
            '>' if n1 == Some('>') && n2 == Some('>') => take(self, 2, GtGtGt),
            '>' if n1 == Some('>') && n2 == Some('=') => take(self, 2, GtGtEq),
            '>' if n1 == Some('>') => take(self, 1, GtGt),
            '>' if n1 == Some('=') => take(self, 1, GtEq),
            '>' => take(self, 0, Gt),
            other => return Err(self.error(format!("carácter inesperado {other:?}"))),
        };
        Ok(out)
    }
}

fn is_ident_start(c: char) -> bool {
    c == '_' || c == '$' || c.is_alphabetic()
}

fn is_ident_part(c: char) -> bool {
    c == '_' || c == '$' || c.is_alphanumeric()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn kinds(src: &str) -> Vec<TokenKind> {
        tokenize(src).unwrap().into_iter().map(|t| t.kind).collect()
    }

    #[test]
    fn maximal_munch_on_shift_assign() {
        use TokenKind::*;
        assert_eq!(kinds("a >>>= b"), vec![Identifier, GtGtGtEq, Identifier, Eof]);
        assert_eq!(kinds("a >> b"), vec![Identifier, GtGt, Identifier, Eof]);
        assert_eq!(kinds("a > b"), vec![Identifier, Gt, Identifier, Eof]);
    }

    #[test]
    fn keywords_vs_identifiers() {
        use TokenKind::*;
        // `var`/`record` son contextuales → Identifier; `int`/`return`/`true` sí son keywords.
        assert_eq!(kinds("var record int return true x"),
            vec![Identifier, Identifier, Int, Return, True, Identifier, Eof]);
    }

    #[test]
    fn literals_and_comments() {
        use TokenKind::*;
        let src = r#"
            // línea
            int x = 0xFF_00; /* bloque */ long y = 42L; double d = 3.14e2;
            char c = 'a'; String s = "hola\n";
        "#;
        let ks = kinds(src);
        assert!(ks.contains(&IntLiteral));
        assert!(ks.contains(&LongLiteral));
        assert!(ks.contains(&DoubleLiteral));
        assert!(ks.contains(&CharLiteral));
        assert!(ks.contains(&StringLiteral));
    }

    #[test]
    fn skips_leading_bom() {
        assert_eq!(kinds("\u{feff}class A {}")[0], TokenKind::Class);
    }

    #[test]
    fn tokenizes_add_java() {
        // El fixture clásico del roadmap: no debe fallar ni dejar nada raro.
        let src = "public class Add { public static int add(int a, int b) { return a + b; } }";
        let toks = tokenize(src).unwrap();
        assert_eq!(toks.last().unwrap().kind, TokenKind::Eof);
        use TokenKind::*;
        let ks: Vec<_> = toks.iter().map(|t| t.kind).collect();
        assert_eq!(&ks[0..6], &[Public, Class, Identifier, LBrace, Public, Static]);
    }
}
