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
pub mod transtypes;
pub mod sema;
pub mod codegen;
pub mod class_writer;

/// Un error de compilación con posición en el fuente (línea/columna, 1-based). Lo comparten
/// el lexer, el parser y la fase semántica.
///
/// `notes` son sub-líneas explicativas al estilo de `javac` (`symbol:`/`location:`, candidatos de
/// sobrecarga descartados, *did-you-mean*), que el render imprime **indentadas** bajo el mensaje.
/// Vacío por defecto: el contenido rico lo aporta cada sitio de error que lo amerite.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Error {
    pub message: String,
    pub line: u32,
    pub col: u32,
    pub notes: Vec<String>,
}

impl Error {
    /// Un error sin notas en `(línea, columna)`.
    pub fn new(message: impl Into<String>, line: u32, col: u32) -> Error {
        Error { message: message.into(), line, col, notes: Vec::new() }
    }

    /// El mismo error con sus sub-líneas explicativas (estilo `javac`).
    pub fn with_notes(mut self, notes: Vec<String>) -> Error {
        self.notes = notes;
        self
    }

    /// Renderiza el diagnóstico al estilo `javac`: `archivo:línea: error: mensaje`, seguido de la
    /// **línea del fuente** y una línea con el **caret** (`^`) bajo la columna, más las `notes`
    /// indentadas. La columna se transmite con el caret (no en el encabezado), igual que `javac`.
    /// El caret preserva los tabs del prefijo para alinear con la línea impresa tal cual.
    pub fn render(&self, source: &str, filename: &str) -> String {
        let mut out = format!("{filename}:{}: error: {}\n", self.line, self.message);
        if let Some(text) = source.lines().nth(self.line.saturating_sub(1) as usize) {
            out.push_str(text);
            out.push('\n');
            let indent: String = text
                .chars()
                .take(self.col.saturating_sub(1) as usize)
                .map(|c| if c == '\t' { '\t' } else { ' ' })
                .collect();
            out.push_str(&indent);
            out.push_str("^\n");
        }
        for note in &self.notes {
            for l in note.lines() {
                out.push_str("  ");
                out.push_str(l);
                out.push('\n');
            }
        }
        out
    }
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}:{}: error: {}", self.line, self.col, self.message)
    }
}

/// Renderiza **todos** los diagnósticos al estilo `javac` (cada uno con su snippet + caret) y cierra
/// con el resumen `N error[s]`. Es lo que imprime el CLI en `--check`/`--emit`. Devuelve `""` si no
/// hay errores (sin resumen), para que el llamador decida el mensaje de éxito.
pub fn render_diagnostics(errors: &[Error], source: &str, filename: &str) -> String {
    if errors.is_empty() {
        return String::new();
    }
    let mut out = String::new();
    for e in errors {
        out.push_str(&e.render(source, filename));
    }
    let n = errors.len();
    out.push_str(&format!("{n} error{}\n", if n == 1 { "" } else { "s" }));
    out
}

/// Alias de resultado del pipeline: `T` o un [`Error`] con posición.
pub type Result<T> = std::result::Result<T, Error>;

/// Front-end del pipeline: fuente `.java` (texto) → AST ([`ast::CompilationUnit`]),
/// corriendo el lexer (B0) y el parser (B1). Falla con un [`Error`] posicionado.
pub fn parse(source: &str) -> Result<ast::CompilationUnit> {
    let tokens = lexer::tokenize(source)?;
    let (unit, errors) = parser::parse(tokens);
    // El parser ahora **recupera** y acumula varios errores sintácticos; esta API de una sola unidad
    // devuelve el primero. El modo `--check` los lista todos (vía [`analyze`]).
    if let Some(first) = errors.into_iter().next() {
        return Err(first);
    }
    Ok(unit)
}

/// Chequeo semántico: corre el parser, la **pasada 1** (Enter) y la **pasada 2** (Attribute),
/// devolviendo **todos** los errores acumulados (vacío si el programa es correcto).
pub fn check(source: &str) -> Result<Vec<Error>> {
    check_cp(source, &[])
}

/// [`check`] con un **classpath extra** de directorios de `.class` (finding #7).
pub fn check_cp(source: &str, extra_classpath: &[std::path::PathBuf]) -> Result<Vec<Error>> {
    let (_unit, _table, errors) = analyze_cp(source, extra_classpath)?;
    Ok(errors)
}

/// Corre el front-end completo y devuelve las tres salidas: el AST **decorado** por la pasada 2,
/// la tabla de símbolos de la pasada 1, y los errores. Es lo que consumirá el codegen (B3).
pub fn analyze(source: &str) -> Result<(ast::CompilationUnit, symbol::SymbolTable, Vec<Error>)> {
    analyze_cp(source, &[])
}

/// [`analyze`] con un **classpath extra** de directorios de `.class` (finding #7): se antepone al
/// classpath por defecto, así los tipos ahí (KajiLibrary ya compilado) sombrean al JDK.
pub fn analyze_cp(
    source: &str,
    extra_classpath: &[std::path::PathBuf],
) -> Result<(ast::CompilationUnit, symbol::SymbolTable, Vec<Error>)> {
    let tokens = lexer::tokenize(source)?;
    let (mut unit, parse_errors) = parser::parse(tokens);
    let (mut table, sem_errors) = enter::enter_cp(&unit, extra_classpath);
    // Los errores **sintácticos** (ya recuperados) van primero; luego los semánticos, en orden de pasada.
    let mut errors = parse_errors;
    errors.extend(sem_errors);
    // **Clases locales** (§14.3): registro + renombrado a único (`1L`, `2L`…) + reescritura de
    // referencias, con scope léxico por bloque, para que dos homónimas no colisionen.
    enter::register_local_classes(&mut unit, &mut table, &mut errors);
    // **Anónimas** → clase local sintética (§15.9.5): entre Enter y Attribute, con los tipos ya
    // resueltos (para decidir extends/implements).
    enter::hoist_anonymous(&mut unit, &mut table, &mut errors);
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
    compile_cp(source, &[])
}

/// [`compile`] con un **classpath extra** de directorios de `.class` (finding #7): se antepone al
/// classpath por defecto, así los tipos ahí (KajiLibrary ya compilado) sombrean al JDK.
pub fn compile_cp(
    source: &str,
    extra_classpath: &[std::path::PathBuf],
) -> Result<Vec<(String, Vec<u8>)>> {
    let tokens = lexer::tokenize(source)?;
    let (mut unit, parse_errors) = parser::parse(tokens);
    let (mut table, sem_errors) = enter::enter_cp(&unit, extra_classpath);
    let mut errors = parse_errors;
    errors.extend(sem_errors);
    enter::register_local_classes(&mut unit, &mut table, &mut errors);
    enter::hoist_anonymous(&mut unit, &mut table, &mut errors);
    errors.extend(attribute::attribute(&mut unit, &table));
    errors.extend(check::check(&unit, &table));
    errors.extend(flow::flow(&unit));
    if let Some(first) = errors.into_iter().next() {
        return Err(first); // no se emite bytecode para un programa con errores
    }
    // **TransTypes** (§5.1.7/§5.1.8): inserta las conversiones de boxing/unboxing dirigidas por tipo
    // (`Integer.valueOf`/`x.intValue`). Va después de Flow (que analiza el árbol fuente) y antes del
    // desugar; sus inserciones las materializa la re-atribución de abajo.
    transtypes::trans_types(&mut unit, &table);
    desugar::desugar(&mut unit, &mut table);
    // Re-decorar: el desugar dejó nodos nuevos sin `ty`/`binding`/`slot`. Un error acá **no** es del
    // usuario (su código ya pasó): sería un bug de una reescritura, y emitir igual daría basura.
    let lowered = attribute::attribute(&mut unit, &table);
    if let Some(first) = lowered.into_iter().next() {
        return Err(first);
    }
    codegen::generate(&unit, &table)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn render_shows_the_source_line_and_a_caret_under_the_column() {
        let src = "class C {\n    int n = s;\n}\n";
        let err = Error::new("incompatible types", 2, 13);
        let out = err.render(src, "C.java");
        assert_eq!(out, "C.java:2: error: incompatible types\n    int n = s;\n            ^\n");
    }

    #[test]
    fn the_caret_preserves_tabs_in_the_prefix() {
        // Un prefijo con tab debe copiarse como tab en la línea del caret, para que alinee con la
        // línea del fuente impresa tal cual (no importa el ancho de tab del terminal).
        let src = "\tint x = y;\n";
        let err = Error::new("cannot find symbol", 1, 10);
        let out = err.render(src, "T.java");
        assert_eq!(out, "T.java:1: error: cannot find symbol\n\tint x = y;\n\t        ^\n");
    }

    #[test]
    fn notes_are_indented_under_the_message() {
        let err = Error::new("cannot find symbol", 3, 5)
            .with_notes(vec!["symbol:   method fro()".into(), "location: variable s".into()]);
        let out = err.render("x\ny\n  s.fro();\n", "C.java");
        assert!(out.contains("\n  symbol:   method fro()\n  location: variable s\n"), "{out}");
    }

    #[test]
    fn the_summary_pluralizes_and_counts() {
        let errs = vec![Error::new("a", 1, 1), Error::new("b", 2, 1)];
        let out = render_diagnostics(&errs, "x\ny\n", "C.java");
        assert!(out.ends_with("2 errors\n"), "{out}");

        let one = render_diagnostics(&errs[..1], "x\ny\n", "C.java");
        assert!(one.ends_with("1 error\n"), "{one}");

        assert_eq!(render_diagnostics(&[], "x\n", "C.java"), "");
    }

    #[test]
    fn a_position_past_the_end_of_source_still_renders_the_header() {
        // Robustez: una línea inexistente (p.ej. un error en EOF) no debe panicquear; sale solo el
        // encabezado, sin snippet.
        let err = Error::new("reached end of file while parsing", 99, 1);
        let out = err.render("class C {}\n", "C.java");
        assert_eq!(out, "C.java:99: error: reached end of file while parsing\n");
    }
}
