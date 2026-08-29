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
pub mod doc;
pub mod parser;
pub mod symbol;
pub mod types;
pub mod infer;
pub mod classfile;
pub mod enter;
pub mod attribute;
pub mod check;
pub mod flow;
pub mod lint;
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
/// La **severidad** de un diagnóstico. Un `Error` corta la emisión; un `Warning` (`-Xlint`) se
/// reporta pero **no** impide compilar (§9.6.4.5: un aviso no es un error).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Severity {
    Error,
    Warning,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Error {
    pub message: String,
    pub line: u32,
    pub col: u32,
    pub notes: Vec<String>,
    /// Error (default) o warning. Solo los `Error` cortan la compilación; los `Warning` los produce
    /// el pase de *lint* y el CLI los imprime aparte.
    pub severity: Severity,
}

impl Error {
    /// Un error sin notas en `(línea, columna)`.
    pub fn new(message: impl Into<String>, line: u32, col: u32) -> Error {
        Error { message: message.into(), line, col, notes: Vec::new(), severity: Severity::Error }
    }

    /// Un **warning** (`-Xlint`) sin notas en `(línea, columna)`.
    pub fn warning(message: impl Into<String>, line: u32, col: u32) -> Error {
        Error { message: message.into(), line, col, notes: Vec::new(), severity: Severity::Warning }
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
        let kind = match self.severity {
            Severity::Error => "error",
            Severity::Warning => "warning",
        };
        let mut out = format!("{filename}:{}: {kind}: {}\n", self.line, self.message);
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
        let kind = match self.severity {
            Severity::Error => "error",
            Severity::Warning => "warning",
        };
        write!(f, "{}:{}: {kind}: {}", self.line, self.col, self.message)
    }
}

/// Renderiza **todos** los diagnósticos al estilo `javac` (cada uno con su snippet + caret) y cierra
/// con el resumen `N error[s]`. Es lo que imprime el CLI en `--check`/`--emit`. Devuelve `""` si no
/// hay errores (sin resumen), para que el llamador decida el mensaje de éxito.
pub fn render_diagnostics(diagnostics: &[Error], source: &str, filename: &str) -> String {
    if diagnostics.is_empty() {
        return String::new();
    }
    let mut out = String::new();
    for e in diagnostics {
        out.push_str(&e.render(source, filename));
    }
    // Resumen por severidad (`N error[s]` y/o `M warning[s]`), como `javac`.
    let errors = diagnostics.iter().filter(|d| d.severity == Severity::Error).count();
    let warnings = diagnostics.len() - errors;
    if errors > 0 {
        out.push_str(&format!("{errors} error{}\n", if errors == 1 { "" } else { "s" }));
    }
    if warnings > 0 {
        out.push_str(&format!("{warnings} warning{}\n", if warnings == 1 { "" } else { "s" }));
    }
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

/// Corre el pase de **lint** (`-Xlint`) sobre el fuente y devuelve los avisos (severidad `Warning`) de
/// las categorías de `set`. Se apoya en [`analyze`], así el AST llega **atribuido** y **antes** del
/// desugar (las posiciones caen en el fuente). No falla por errores semánticos: los avisos van igual.
pub fn lint_source(source: &str, set: &lint::LintSet) -> Result<Vec<Error>> {
    lint_cp(source, &[], set)
}

/// [`lint_source`] con un **classpath extra** de directorios de `.class`.
pub fn lint_cp(
    source: &str,
    extra_classpath: &[std::path::PathBuf],
    set: &lint::LintSet,
) -> Result<Vec<Error>> {
    let (unit, table, _errors) = analyze_cp(source, extra_classpath)?;
    Ok(lint::lint(&unit, &table, set))
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
    Ok(compile_units_cp(std::slice::from_ref(&source), extra_classpath)?.remove(0))
}

/// [`compile_cp`] sobre **varias** unidades de compilación en una sola invocación: todas comparten
/// una tabla de símbolos, así que cada una **ve los tipos de las otras** (#234).
///
/// Devuelve las clases **agrupadas por unidad**, en el mismo orden que `sources`, para que el
/// llamador pueda escribir cada `.class` al lado de *su* fuente.
///
/// Sin esto, dos clases que se referencian mutuamente pedían un bootstrap en tres pasos —compilar
/// una con el cuerpo talado, compilar la otra, recompilar la primera—, que fue lo que hubo que
/// hacer tres veces en `java.text` y una en `javax.lang.model.type`.
pub fn compile_units_cp(
    sources: &[&str],
    extra_classpath: &[std::path::PathBuf],
) -> Result<Vec<Vec<(String, Vec<u8>)>>> {
    let mut units = Vec::with_capacity(sources.len());
    let mut errors = Vec::new();
    for source in sources {
        let tokens = lexer::tokenize(source)?;
        let (unit, parse_errors) = parser::parse(tokens);
        errors.extend(parse_errors);
        units.push(unit);
    }
    // Pasada 1 **global**: Enter de todas las clases, después MemberEnter, después resolución.
    let (mut table, sem_errors) = enter::enter_cp_multi(&units, extra_classpath);
    errors.extend(sem_errors);
    for unit in &mut units {
        enter::register_local_classes(unit, &mut table, &mut errors);
        enter::hoist_anonymous(unit, &mut table, &mut errors);
    }
    for unit in &mut units {
        errors.extend(attribute::attribute(unit, &table));
    }
    for unit in &units {
        errors.extend(check::check(unit, &table));
        errors.extend(flow::flow(unit));
    }
    if let Some(first) = errors.into_iter().next() {
        return Err(first); // no se emite bytecode para un programa con errores
    }
    // Las constantes de **todas** las unidades se juntan antes del desugar: una puede leer el
    // `static final` de otra, y el plegado tiene que verlo (§15.29).
    let mut const_fields = std::collections::HashMap::new();
    for unit in &units {
        const_fields.extend(codegen::collect_const_fields(unit, &table));
    }
    table.set_const_fields(const_fields);
    let mut out = Vec::with_capacity(units.len());
    for unit in &mut units {
        transtypes::trans_types(unit, &table);
        desugar::desugar(unit, &mut table);
        let lowered = attribute::attribute(unit, &table);
        if let Some(first) = lowered.into_iter().next() {
            return Err(first);
        }
        out.push(codegen::generate(unit, &table)?);
    }
    Ok(out)
}

#[allow(dead_code)]
fn compile_cp_single(
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
    // Pliega los **campos constantes** de la unidad (`static final` numéricos, §15.29) a un mapa
    // `SymbolId → valor`, resolviendo referencias entre `final` por *fixpoint* (`B = A * 2`). El
    // desugar lo consulta para no bajar esas inits al `<clinit>`, y el codegen para emitir su
    // `ConstantValue`. Debe correr **después** del atributado (que resuelve los `Binding::Field`) y
    // **antes** del desugar.
    let const_fields = codegen::collect_const_fields(&unit, &table);
    table.set_const_fields(const_fields);
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
