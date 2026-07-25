//! Los **tokens**: las unidades léxicas que produce el lexer (palabras clave,
//! identificadores, literales, símbolos/puntuación).
//!
//! [`TokenKind`] es un espejo **fiel** del enum `TokenKind` de javac
//! (`com.sun.tools.javac.parser.Tokens`, JDK 25 — 115 constantes), para que el
//! *differential testing* del lexer contra el javac real sea 1-a-1. Convención de nombres:
//! **variante Rust = nombre javac en CamelCase** (`MONKEYS_AT` → `MonkeysAt`,
//! `GTGTGTEQ` → `GtGtGtEq`), así el mapeo es mecánico y reversible.
//!
//! Referencia: `docs/tokens-jdk25.md` y el fuente
//! `.jdk25_tmp/jdk-25.0.3+9/lib/src.zip → …/parser/Tokens.java`.
//!
//! Nota: las keywords **contextuales** (`var`, `record`, `sealed`, `yield`, `permits`,
//! `module`…) **no** son tokens acá — javac las lexea como [`TokenKind::Identifier`] y las
//! desambigua el parser. Este enum solo tiene las 50 reservadas clásicas.
//!
//! Hito B0.

/// El *kind* de un token: qué clase de lexema es, sin su texto ni su posición (eso va en el
/// `Token` que se construya encima). Las 115 constantes de javac, en el mismo orden.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TokenKind {
    // --- Especiales / sintéticos ---
    Eof,        // token.end-of-input
    Error,      // símbolo inválido (recuperación de errores)
    Identifier, // token.identifier
    // (el resto de los especiales, Custom, va al final como en javac)

    // --- Palabras reservadas (50) --- `Const` y `Goto` son reservadas pero sin uso.
    Abstract,
    Assert,
    Boolean,
    Break,
    Byte,
    Case,
    Catch,
    Char,
    Class,
    Const,
    Continue,
    Default,
    Do,
    Double,
    Else,
    Enum,
    Extends,
    Final,
    Finally,
    Float,
    For,
    Goto,
    If,
    Implements,
    Import,
    Instanceof,
    Int,
    Interface,
    Long,
    Native,
    New,
    Package,
    Private,
    Protected,
    Public,
    Return,
    Short,
    Static,
    Strictfp,
    Super,
    Switch,
    Synchronized,
    This,
    Throw,
    Throws,
    Transient,
    Try,
    Void,
    Volatile,
    While,

    // --- Literales (7) --- llevan su valor en el `Token`, no un lexema fijo.
    IntLiteral,
    LongLiteral,
    FloatLiteral,
    DoubleLiteral,
    CharLiteral,
    StringLiteral,
    StringFragment, // trozo de string template / text block

    // --- Literales-palabra y `_` (4) --- tokens propios, no identificadores.
    True,
    False,
    Null,
    Underscore, // `_`, keyword desde Java 9

    // --- Separadores (13) ---
    Arrow,     // ->
    ColCol,    // ::   (javac COLCOL)
    LParen,    // (
    RParen,    // )
    LBrace,    // {
    RBrace,    // }
    LBracket,  // [
    RBracket,  // ]
    Semi,      // ;
    Comma,     // ,
    Dot,       // .
    Ellipsis,  // ...
    MonkeysAt, // @    (javac MONKEYS_AT)

    // --- Operadores (37) ---
    Eq,        // =
    Gt,        // >
    Lt,        // <
    Bang,      // !
    Tilde,     // ~
    Ques,      // ?
    Colon,     // :
    EqEq,      // ==
    LtEq,      // <=
    GtEq,      // >=
    BangEq,    // !=
    AmpAmp,    // &&
    BarBar,    // ||
    PlusPlus,  // ++
    SubSub,    // --
    Plus,      // +
    Sub,       // -
    Star,      // *
    Slash,     // /
    Amp,       // &
    Bar,       // |
    Caret,     // ^
    Percent,   // %
    LtLt,      // <<
    GtGt,      // >>
    GtGtGt,    // >>>
    PlusEq,    // +=
    SubEq,     // -=
    StarEq,    // *=
    SlashEq,   // /=
    AmpEq,     // &=
    BarEq,     // |=
    CaretEq,   // ^=
    PercentEq, // %=
    LtLtEq,    // <<=
    GtGtEq,    // >>=
    GtGtGtEq,  // >>>=

    /// Reservado para extensiones internas (javac `CUSTOM`).
    Custom,
}

impl TokenKind {
    /// El lexema **fijo** de una keyword, separador u operador (lo que se escribe tal cual
    /// en el fuente). `None` para los que no tienen texto fijo: `Eof`/`Error`/`Custom`, el
    /// `Identifier` y los literales (que llevan su valor en el `Token`).
    pub fn lexeme(self) -> Option<&'static str> {
        use TokenKind::*;
        let s = match self {
            // Palabras reservadas
            Abstract => "abstract",
            Assert => "assert",
            Boolean => "boolean",
            Break => "break",
            Byte => "byte",
            Case => "case",
            Catch => "catch",
            Char => "char",
            Class => "class",
            Const => "const",
            Continue => "continue",
            Default => "default",
            Do => "do",
            Double => "double",
            Else => "else",
            Enum => "enum",
            Extends => "extends",
            Final => "final",
            Finally => "finally",
            Float => "float",
            For => "for",
            Goto => "goto",
            If => "if",
            Implements => "implements",
            Import => "import",
            Instanceof => "instanceof",
            Int => "int",
            Interface => "interface",
            Long => "long",
            Native => "native",
            New => "new",
            Package => "package",
            Private => "private",
            Protected => "protected",
            Public => "public",
            Return => "return",
            Short => "short",
            Static => "static",
            Strictfp => "strictfp",
            Super => "super",
            Switch => "switch",
            Synchronized => "synchronized",
            This => "this",
            Throw => "throw",
            Throws => "throws",
            Transient => "transient",
            Try => "try",
            Void => "void",
            Volatile => "volatile",
            While => "while",
            // Literales-palabra
            True => "true",
            False => "false",
            Null => "null",
            Underscore => "_",
            // Separadores
            Arrow => "->",
            ColCol => "::",
            LParen => "(",
            RParen => ")",
            LBrace => "{",
            RBrace => "}",
            LBracket => "[",
            RBracket => "]",
            Semi => ";",
            Comma => ",",
            Dot => ".",
            Ellipsis => "...",
            MonkeysAt => "@",
            // Operadores
            Eq => "=",
            Gt => ">",
            Lt => "<",
            Bang => "!",
            Tilde => "~",
            Ques => "?",
            Colon => ":",
            EqEq => "==",
            LtEq => "<=",
            GtEq => ">=",
            BangEq => "!=",
            AmpAmp => "&&",
            BarBar => "||",
            PlusPlus => "++",
            SubSub => "--",
            Plus => "+",
            Sub => "-",
            Star => "*",
            Slash => "/",
            Amp => "&",
            Bar => "|",
            Caret => "^",
            Percent => "%",
            LtLt => "<<",
            GtGt => ">>",
            GtGtGt => ">>>",
            PlusEq => "+=",
            SubEq => "-=",
            StarEq => "*=",
            SlashEq => "/=",
            AmpEq => "&=",
            BarEq => "|=",
            CaretEq => "^=",
            PercentEq => "%=",
            LtLtEq => "<<=",
            GtGtEq => ">>=",
            GtGtGtEq => ">>>=",
            // Sin lexema fijo: fin de entrada, error, identificador, literales, custom.
            Eof | Error | Identifier | IntLiteral | LongLiteral | FloatLiteral
            | DoubleLiteral | CharLiteral | StringLiteral | StringFragment | Custom => {
                return None;
            }
        };
        Some(s)
    }

    /// La keyword (o literal-palabra) cuyo lexema es `text`, para que el lexer clasifique un
    /// identificador reconocido. `None` si `text` es un identificador común. Cubre las 50
    /// reservadas + `true`/`false`/`null` + `_`.
    pub fn keyword(text: &str) -> Option<TokenKind> {
        use TokenKind::*;
        Some(match text {
            "abstract" => Abstract,
            "assert" => Assert,
            "boolean" => Boolean,
            "break" => Break,
            "byte" => Byte,
            "case" => Case,
            "catch" => Catch,
            "char" => Char,
            "class" => Class,
            "const" => Const,
            "continue" => Continue,
            "default" => Default,
            "do" => Do,
            "double" => Double,
            "else" => Else,
            "enum" => Enum,
            "extends" => Extends,
            "final" => Final,
            "finally" => Finally,
            "float" => Float,
            "for" => For,
            "goto" => Goto,
            "if" => If,
            "implements" => Implements,
            "import" => Import,
            "instanceof" => Instanceof,
            "int" => Int,
            "interface" => Interface,
            "long" => Long,
            "native" => Native,
            "new" => New,
            "package" => Package,
            "private" => Private,
            "protected" => Protected,
            "public" => Public,
            "return" => Return,
            "short" => Short,
            "static" => Static,
            "strictfp" => Strictfp,
            "super" => Super,
            "switch" => Switch,
            "synchronized" => Synchronized,
            "this" => This,
            "throw" => Throw,
            "throws" => Throws,
            "transient" => Transient,
            "try" => Try,
            "void" => Void,
            "volatile" => Volatile,
            "while" => While,
            "true" => True,
            "false" => False,
            "null" => Null,
            "_" => Underscore,
            _ => return None,
        })
    }
}

/// Un token producido por el lexer: su [`TokenKind`], el texto tal como apareció en el
/// fuente (el lexema — relevante para identificadores y literales) y su posición de inicio
/// (línea/columna, 1-based) para los mensajes de error.
#[derive(Debug, Clone, PartialEq)]
pub struct Token {
    pub kind: TokenKind,
    pub text: String,
    pub line: u32,
    pub col: u32,
}
