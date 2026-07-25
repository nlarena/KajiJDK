//! `javac` — el compilador: texto `.java` → bytecode `.class`.
//!
//! Módulo **auto-contenido**: aunque vive dentro del crate `jvm`, no depende de
//! `crate::jvm` — su único contrato con el mundo es el formato `.class` (congelado). Eso lo
//! deja trasplantable tal cual (copiar `src/javac.rs` + `src/javac/` + una línea `pub mod
//! javac;`). El bytecode que emite se valida corriéndolo en la JVM propia (o en el `java`
//! real como segundo oráculo).
//!
//! El pipeline es la cadena clásica de un compilador, un módulo por etapa:
//!
//! ```text
//! .java → [lexer] tokens → [parser] AST → [sema] AST tipado → [codegen] bytecode
//!                                                                      → [class_writer] .class
//! ```

pub mod token;
pub mod lexer;
pub mod ast;
pub mod ast_view;
pub mod parser;
pub mod symbol;
pub mod types;
pub mod infer;
pub mod classfile;
pub mod enter;
pub mod attribute;
pub mod check;
pub mod flow;
pub mod desugar;
pub mod sema;
pub mod codegen;
pub mod class_writer;

/// Un error de compilación con posición en el fuente (línea/columna, 1-based). Lo comparten
/// el lexer y el parser; más adelante lo usará también la fase semántica.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Error {
    pub message: String,
    pub line: u32,
    pub col: u32,
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}:{}: error: {}", self.line, self.col, self.message)
    }
}

/// Alias de resultado del pipeline: `T` o un [`Error`] con posición.
pub type Result<T> = std::result::Result<T, Error>;

/// Front-end del pipeline: fuente `.java` (texto) → AST ([`ast::CompilationUnit`]),
/// corriendo el lexer (B0) y el parser (B1). Falla con un [`Error`] posicionado.
pub fn parse(source: &str) -> Result<ast::CompilationUnit> {
    let tokens = lexer::tokenize(source)?;
    parser::parse(tokens)
}

/// Chequeo semántico: corre el parser, la **pasada 1** (Enter) y la **pasada 2** (Attribute),
/// devolviendo **todos** los errores acumulados (vacío si el programa es correcto).
pub fn check(source: &str) -> Result<Vec<Error>> {
    let (_unit, _table, errors) = analyze(source)?;
    Ok(errors)
}

/// Corre el front-end completo y devuelve las tres salidas: el AST **decorado** por la pasada 2,
/// la tabla de símbolos de la pasada 1, y los errores. Es lo que consumirá el codegen (B3).
pub fn analyze(source: &str) -> Result<(ast::CompilationUnit, symbol::SymbolTable, Vec<Error>)> {
    let mut unit = parse(source)?;
    let (table, mut errors) = enter::enter(&unit);
    errors.extend(attribute::attribute(&mut unit, &table));
    // **Check**: bien-formación de las declaraciones (override, abstractos sin implementar).
    errors.extend(check::check(&unit, &table));
    // **Flow** (B4): asignación definitiva + alcanzabilidad, sobre el AST ya decorado.
    errors.extend(flow::flow(&unit));
    Ok((unit, table, errors))
}

/// Punto de entrada del pipeline **completo**: fuente `.java` (texto) → los bytes de un `.class`.
/// Corre las cinco pasadas en el orden de javac:
///
/// ```text
/// lexer → parser → 1 Enter → 2 Attribute → Check → 3 Flow → 4 Desugar → (re-Attribute) → 5 Codegen
/// ```
///
/// Dos detalles del orden que importan:
///
/// - **Flow va antes que Desugar**: analiza el árbol *fuente*, así sus errores (variable sin
///   inicializar, sentencia inalcanzable) apuntan al código que escribió la persona, no al bajado.
/// - **Se re-atribuye después de Desugar**: las reescrituras producen nodos **frescos, sin decorar**,
///   y el codegen necesita el tipo/binding/slot de cada nodo para elegir el opcode. Sin este paso
///   emitiría sobre un árbol a medio decorar.
///
/// Si el programa tiene errores **semánticos** no se emite nada: devuelve el primero. (El modo
/// `--check` del binario los lista todos.)
///
/// Devuelve **un `.class` por tipo** — `(nombre interno, bytes)` —: una unidad puede declarar varias
/// clases, y las anidadas/sintéticas son clases aparte (§7.6).
pub fn compile(source: &str) -> Result<Vec<(String, Vec<u8>)>> {
    let mut unit = parse(source)?;
    let (mut table, mut errors) = enter::enter(&unit);
    errors.extend(attribute::attribute(&mut unit, &table));
    errors.extend(check::check(&unit, &table));
    errors.extend(flow::flow(&unit));
    if let Some(first) = errors.into_iter().next() {
        return Err(first); // no se emite bytecode para un programa con errores
    }
    desugar::desugar(&mut unit, &mut table);
    // Re-decorar: el desugar dejó nodos nuevos sin `ty`/`binding`/`slot`. Un error acá **no** es del
    // usuario (su código ya pasó): sería un bug de una reescritura, y emitir igual daría basura.
    let lowered = attribute::attribute(&mut unit, &table);
    if let Some(first) = lowered.into_iter().next() {
        return Err(first);
    }
    codegen::generate(&unit, &table)
}
