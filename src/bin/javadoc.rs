//! CLI de `javadoc` — el generador de documentación (javadoc, **etapas 3 y 4**).
//!
//! Corre el front-end del compilador propio (parse → enter → attribute) sobre uno o varios `.java`,
//! recolecta la **superficie pública/protegida** (tipos y sus miembros filtrados por modificadores),
//! empareja cada `doc` con el parser de la etapa 2 ([`jvm::javac::doc::parse_doc`]) y emite una
//! página HTML por clase más un `index.html`.
//!
//! Uso:
//!
//! ```text
//! javadoc [-d <outdir>] [-cp <dirs>] <A.java> [B.java …]
//! ```
//!
//! - `-d <outdir>`  directorio de salida (por defecto el actual).
//! - `-cp <dirs>`   directorios de `.class` antepuestos al classpath (para resolver tipos externos).
//!
//! Es el hito **mínimo end-to-end**: no arma un índice de miembros global, ni frames, ni herencia de
//! doc; documenta lo declarado en los archivos de entrada. Los `{@link}`/`@see` a un tipo **del
//! propio conjunto** se vuelven `<a href>`; los que no resuelven quedan como texto.

use std::collections::BTreeSet;
use std::env;
use std::fs;
use std::path::PathBuf;
use std::process;

use jvm::javac::ast::{ClassDecl, FieldDecl, MethodDecl, Member, Modifier, PrimType, Type, TypeArg, TypeKind};
use jvm::javac::doc::{self, BlockTag, DocComment, Inline};

fn main() {
    let raw: Vec<String> = env::args().skip(1).collect();
    let mut out_dir = PathBuf::from(".");
    let mut extra_classpath: Vec<PathBuf> = Vec::new();
    let mut inputs: Vec<String> = Vec::new();

    let mut it = raw.into_iter();
    while let Some(a) = it.next() {
        match a.as_str() {
            "-d" => {
                if let Some(v) = it.next() {
                    out_dir = PathBuf::from(v);
                } else {
                    eprintln!("javadoc: -d requiere un directorio");
                    process::exit(2);
                }
            }
            "-cp" | "-classpath" | "--classpath" => {
                if let Some(v) = it.next() {
                    extra_classpath.extend(env::split_paths(&v));
                } else {
                    eprintln!("javadoc: -cp requiere una lista de directorios");
                    process::exit(2);
                }
            }
            _ => inputs.push(a),
        }
    }

    if inputs.is_empty() {
        eprintln!("uso: javadoc [-d <outdir>] [-cp <dirs>] <A.java> [B.java …]");
        process::exit(2);
    }

    // Front-end por archivo: se recolectan los tipos documentables (public/protected), aplanando los
    // anidados. Los errores semánticos se avisan pero no frenan la generación (javadoc documenta lo
    // que puede).
    let mut docs: Vec<DocType> = Vec::new();
    for input in &inputs {
        let source = match fs::read_to_string(input) {
            Ok(s) => s,
            Err(err) => {
                eprintln!("javadoc: no se pudo leer {input}: {err}");
                process::exit(1);
            }
        };
        match jvm::javac::analyze_cp(&source, &extra_classpath) {
            Ok((unit, _table, errors)) => {
                for e in &errors {
                    eprintln!("javadoc: {input}: {}", e);
                }
                for ty in &unit.types {
                    collect_type(ty, &mut docs);
                }
            }
            Err(err) => {
                eprintln!("javadoc: {input}: {err}");
                // Un error de lexer/parse impide extraer el AST de este archivo; se sigue con los demás.
            }
        }
    }

    if docs.is_empty() {
        eprintln!("javadoc: no se encontraron tipos public/protected para documentar");
        process::exit(1);
    }

    if let Err(err) = fs::create_dir_all(&out_dir) {
        eprintln!("javadoc: no se pudo crear {}: {err}", out_dir.display());
        process::exit(1);
    }

    // El conjunto de nombres simples documentados: base para resolver `{@link}`/`@see`.
    let type_set: BTreeSet<String> = docs.iter().map(|d| d.decl.name.clone()).collect();

    for d in &docs {
        let html = render_class_page(d, &type_set);
        let path = out_dir.join(format!("{}.html", d.decl.name));
        if let Err(err) = fs::write(&path, html) {
            eprintln!("javadoc: no se pudo escribir {}: {err}", path.display());
            process::exit(1);
        }
        println!("javadoc: escrito {}", path.display());
    }

    let index = render_index(&docs);
    let index_path = out_dir.join("index.html");
    if let Err(err) = fs::write(&index_path, index) {
        eprintln!("javadoc: no se pudo escribir {}: {err}", index_path.display());
        process::exit(1);
    }
    println!("javadoc: escrito {}", index_path.display());
}

/// Un tipo documentable ya seleccionado por la superficie pública/protegida.
struct DocType {
    decl: ClassDecl,
}

/// Recolecta `ty` y sus tipos anidados documentables (public/protected), aplanándolos en `out`.
fn collect_type(ty: &ClassDecl, out: &mut Vec<DocType>) {
    if !is_documented(&ty.modifiers) {
        return;
    }
    out.push(DocType { decl: ty.clone() });
    for m in &ty.members {
        if let Member::Type(nested) = m {
            collect_type(nested, out);
        }
    }
}

/// ¿La declaración es parte de la superficie documentada? javadoc por defecto muestra los miembros
/// `public` y `protected`. Un tipo/miembro sin modificador de acceso (package-private) o `private`
/// no se documenta.
fn is_documented(modifiers: &[Modifier]) -> bool {
    modifiers.contains(&Modifier::Public) || modifiers.contains(&Modifier::Protected)
}

// ─── Generación de HTML (etapa 4) ────────────────────────────────────────────────────────────────

fn render_class_page(d: &DocType, type_set: &BTreeSet<String>) -> String {
    let decl = &d.decl;
    let kind = kind_word(decl.kind);
    let mods = modifiers_str(&decl.modifiers);
    let title = format!("{kind} {}", decl.name);

    let mut body = String::new();
    body.push_str(&format!("<h1>{}</h1>\n", esc(&title)));
    let header_line = if mods.is_empty() {
        format!("{kind} {}", esc(&decl.name))
    } else {
        format!("{} {kind} {}", esc(&mods), esc(&decl.name))
    };
    body.push_str(&format!("<p class=\"decl\"><code>{header_line}</code></p>\n"));

    // Descripción de la clase.
    let cdoc = decl.doc.as_deref().map(doc::parse_doc).unwrap_or_default();
    if !cdoc.description.is_empty() {
        body.push_str(&format!("<div class=\"description\">{}</div>\n", render_inline(&cdoc.description, type_set)));
    }
    render_class_tags(&cdoc, type_set, &mut body);

    // Constantes de enum (implícitamente públicas).
    if decl.kind == TypeKind::Enum && !decl.enum_constants.is_empty() {
        body.push_str("<h2>Constantes de enumeración</h2>\n<dl>\n");
        for c in &decl.enum_constants {
            body.push_str(&format!("<dt><code>{}</code></dt>\n", esc(&c.name)));
            let cd = c.doc.as_deref().map(doc::parse_doc).unwrap_or_default();
            if !cd.description.is_empty() {
                body.push_str(&format!("<dd>{}</dd>\n", render_inline(&cd.description, type_set)));
            }
        }
        body.push_str("</dl>\n");
    }

    // Campos public/protected.
    let fields: Vec<&FieldDecl> = decl
        .members
        .iter()
        .filter_map(|m| match m {
            Member::Field(f) if is_documented(&f.modifiers) => Some(f),
            _ => None,
        })
        .collect();
    if !fields.is_empty() {
        body.push_str("<h2>Campos</h2>\n");
        for f in fields {
            render_field(f, type_set, &mut body);
        }
    }

    // Métodos y constructores public/protected.
    let methods: Vec<&MethodDecl> = decl
        .members
        .iter()
        .filter_map(|m| match m {
            Member::Method(mth) if is_documented(&mth.modifiers) => Some(mth),
            _ => None,
        })
        .collect();
    if !methods.is_empty() {
        body.push_str("<h2>Métodos y constructores</h2>\n");
        for m in methods {
            render_method(m, type_set, &mut body);
        }
    }

    page("es", &title, &body)
}

fn render_class_tags(cdoc: &DocComment, type_set: &BTreeSet<String>, body: &mut String) {
    let mut extras = String::new();
    for tag in &cdoc.tags {
        match tag {
            BlockTag::Since { text } => {
                extras.push_str(&format!("<dt>Desde:</dt><dd>{}</dd>\n", esc(text)));
            }
            BlockTag::Deprecated { desc } => {
                extras.push_str(&format!("<dt>Obsoleto:</dt><dd>{}</dd>\n", render_inline(desc, type_set)));
            }
            BlockTag::See { reference } => {
                extras.push_str(&format!("<dt>Ver también:</dt><dd>{}</dd>\n", render_reference(reference, type_set)));
            }
            _ => {}
        }
    }
    if !extras.is_empty() {
        body.push_str(&format!("<dl class=\"tags\">\n{extras}</dl>\n"));
    }
}

fn render_field(f: &FieldDecl, type_set: &BTreeSet<String>, body: &mut String) {
    let sig = format!("{} {} {}", modifiers_str(&f.modifiers), type_str(&f.ty), f.name);
    body.push_str(&format!("<h3><code>{}</code></h3>\n", esc(sig.trim())));
    let fdoc = f.doc.as_deref().map(doc::parse_doc).unwrap_or_default();
    if !fdoc.description.is_empty() {
        body.push_str(&format!("<div class=\"description\">{}</div>\n", render_inline(&fdoc.description, type_set)));
    }
}

fn render_method(m: &MethodDecl, type_set: &BTreeSet<String>, body: &mut String) {
    let params: Vec<String> = m
        .params
        .iter()
        .map(|p| {
            let ty = if p.varargs { format!("{}...", type_str(strip_array(&p.ty))) } else { type_str(&p.ty) };
            format!("{} {}", ty, p.name)
        })
        .collect();
    let sig = if m.is_constructor {
        format!("{} {}({})", modifiers_str(&m.modifiers), m.name, params.join(", "))
    } else {
        format!(
            "{} {} {}({})",
            modifiers_str(&m.modifiers),
            type_str(&m.return_type),
            m.name,
            params.join(", ")
        )
    };
    body.push_str(&format!("<h3><code>{}</code></h3>\n", esc(sig.trim())));

    let mdoc = m.doc.as_deref().map(doc::parse_doc).unwrap_or_default();
    if !mdoc.description.is_empty() {
        body.push_str(&format!("<div class=\"description\">{}</div>\n", render_inline(&mdoc.description, type_set)));
    }

    // Tags de parámetros / retorno / excepciones.
    let mut params_html = String::new();
    let mut return_html = String::new();
    let mut throws_html = String::new();
    for tag in &mdoc.tags {
        match tag {
            BlockTag::Param { name, desc } => {
                params_html.push_str(&format!(
                    "<dd><code>{}</code> — {}</dd>\n",
                    esc(name),
                    render_inline(desc, type_set)
                ));
            }
            BlockTag::Return { desc } => {
                return_html.push_str(&format!("<dd>{}</dd>\n", render_inline(desc, type_set)));
            }
            BlockTag::Throws { exception, desc } => {
                throws_html.push_str(&format!(
                    "<dd>{} — {}</dd>\n",
                    render_type_name(exception, type_set),
                    render_inline(desc, type_set)
                ));
            }
            _ => {}
        }
    }
    if !params_html.is_empty() {
        body.push_str(&format!("<dl class=\"tags\"><dt>Parámetros:</dt>\n{params_html}</dl>\n"));
    }
    if !return_html.is_empty() {
        body.push_str(&format!("<dl class=\"tags\"><dt>Devuelve:</dt>\n{return_html}</dl>\n"));
    }
    if !throws_html.is_empty() {
        body.push_str(&format!("<dl class=\"tags\"><dt>Lanza:</dt>\n{throws_html}</dl>\n"));
    }
}

fn render_index(docs: &[DocType]) -> String {
    let mut body = String::from("<h1>Documentación</h1>\n<ul class=\"index\">\n");
    for d in docs {
        body.push_str(&format!(
            "<li><a href=\"{name}.html\">{kind} {name}</a></li>\n",
            name = esc(&d.decl.name),
            kind = kind_word(d.decl.kind),
        ));
    }
    body.push_str("</ul>\n");
    page("es", "Índice", &body)
}

// ─── Render de contenido inline (etapa 4) ────────────────────────────────────────────────────────

/// Renderiza una secuencia de [`Inline`] a HTML: texto escapado, `{@code}` → `<code>`, `{@link}` →
/// `<a href>` si resuelve a un tipo del conjunto, o texto si no.
fn render_inline(segments: &[Inline], type_set: &BTreeSet<String>) -> String {
    let mut out = String::new();
    for seg in segments {
        match seg {
            Inline::Text(t) => out.push_str(&esc(t)),
            Inline::Code { text, monospace } => {
                if *monospace {
                    out.push_str(&format!("<code>{}</code>", esc(text)));
                } else {
                    out.push_str(&esc(text));
                }
            }
            Inline::Link { target, label, plain } => {
                let shown = label.clone().unwrap_or_else(|| target.clone());
                let inner = link_href(target, type_set)
                    .map(|href| format!("<a href=\"{}\">{}</a>", esc(&href), esc(&shown)))
                    .unwrap_or_else(|| esc(&shown));
                if *plain {
                    out.push_str(&inner);
                } else {
                    out.push_str(&format!("<code>{inner}</code>"));
                }
            }
        }
    }
    out
}

/// Renderiza una referencia `@see`: `Tipo`/`Tipo#miembro` → enlace si resuelve, si no texto.
fn render_reference(reference: &str, type_set: &BTreeSet<String>) -> String {
    match link_href(reference, type_set) {
        Some(href) => format!("<a href=\"{}\">{}</a>", esc(&href), esc(reference)),
        None => esc(reference),
    }
}

/// Renderiza un nombre de tipo (p. ej. de una excepción `@throws`): enlace si es un tipo del
/// conjunto, si no texto en `<code>`.
fn render_type_name(name: &str, type_set: &BTreeSet<String>) -> String {
    if type_set.contains(name) {
        format!("<a href=\"{name}.html\"><code>{}</code></a>", esc(name))
    } else {
        format!("<code>{}</code>", esc(name))
    }
}

/// El `href` de un destino `Tipo` / `Tipo#miembro`, o `None` si el tipo no está en el conjunto
/// documentado. Un destino que empieza con `#` (miembro de la misma clase) no se resuelve aquí.
fn link_href(target: &str, type_set: &BTreeSet<String>) -> Option<String> {
    let type_part = target.split('#').next().unwrap_or(target).trim();
    if type_part.is_empty() {
        return None;
    }
    // Nombre simple (último segmento tras un `.` calificador).
    let simple = type_part.rsplit('.').next().unwrap_or(type_part);
    if type_set.contains(simple) {
        Some(format!("{simple}.html"))
    } else {
        None
    }
}

// ─── Utilidades de formato ───────────────────────────────────────────────────────────────────────

fn kind_word(kind: TypeKind) -> &'static str {
    match kind {
        TypeKind::Class => "class",
        TypeKind::Interface => "interface",
        TypeKind::Enum => "enum",
        TypeKind::Record => "record",
        TypeKind::Annotation => "@interface",
    }
}

fn modifiers_str(mods: &[Modifier]) -> String {
    mods.iter().map(modifier_word).collect::<Vec<_>>().join(" ")
}

fn modifier_word(m: &Modifier) -> &'static str {
    match m {
        Modifier::Public => "public",
        Modifier::Private => "private",
        Modifier::Protected => "protected",
        Modifier::Static => "static",
        Modifier::Final => "final",
        Modifier::Abstract => "abstract",
        Modifier::Native => "native",
        Modifier::Synchronized => "synchronized",
        Modifier::Transient => "transient",
        Modifier::Volatile => "volatile",
        Modifier::Strictfp => "strictfp",
        Modifier::Default => "default",
        Modifier::Sealed => "sealed",
        Modifier::NonSealed => "non-sealed",
    }
}

/// Nombre legible de un [`Type`] (réplica mínima de la de `ast_view`, que es privada).
fn type_str(ty: &Type) -> String {
    match ty {
        Type::Void => "void".to_string(),
        Type::Prim(p) => prim_str(*p).to_string(),
        Type::Class(name) => name.clone(),
        Type::Parameterized { base, args } => {
            let a: Vec<String> = args.iter().map(type_arg_str).collect();
            format!("{base}<{}>", a.join(", "))
        }
        Type::Array(inner) => format!("{}[]", type_str(inner)),
        Type::Var => "var".to_string(),
    }
}

fn type_arg_str(arg: &TypeArg) -> String {
    match arg {
        TypeArg::Type(t) => type_str(t),
        TypeArg::Wildcard => "?".to_string(),
        TypeArg::Extends(t) => format!("? extends {}", type_str(t)),
        TypeArg::Super(t) => format!("? super {}", type_str(t)),
    }
}

/// El tipo elemento de un array de varargs (`T[]` → `T`); para no-array devuelve el tipo tal cual.
fn strip_array(ty: &Type) -> &Type {
    match ty {
        Type::Array(inner) => inner,
        other => other,
    }
}

fn prim_str(p: PrimType) -> &'static str {
    match p {
        PrimType::Int => "int",
        PrimType::Long => "long",
        PrimType::Short => "short",
        PrimType::Byte => "byte",
        PrimType::Char => "char",
        PrimType::Boolean => "boolean",
        PrimType::Float => "float",
        PrimType::Double => "double",
    }
}

/// Escapa un texto para HTML (`&`, `<`, `>`, `"`).
fn esc(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '&' => out.push_str("&amp;"),
            '<' => out.push_str("&lt;"),
            '>' => out.push_str("&gt;"),
            '"' => out.push_str("&quot;"),
            _ => out.push(c),
        }
    }
    out
}

/// Envuelve el cuerpo en un documento HTML mínimo con un poco de estilo.
fn page(lang: &str, title: &str, body: &str) -> String {
    format!(
        "<!DOCTYPE html>\n<html lang=\"{lang}\">\n<head>\n<meta charset=\"utf-8\">\n\
<title>{}</title>\n<style>\n\
body {{ font-family: system-ui, sans-serif; margin: 2rem auto; max-width: 52rem; line-height: 1.5; }}\n\
h1 {{ border-bottom: 2px solid #ccc; padding-bottom: .3rem; }}\n\
h3 {{ margin-top: 1.5rem; }}\n\
code {{ background: #f4f4f4; padding: .1rem .3rem; border-radius: 3px; }}\n\
.decl code {{ background: none; }}\n\
dl.tags {{ margin: .3rem 0 .3rem 1rem; }}\n\
dl.tags dt {{ font-weight: bold; }}\n\
.index {{ list-style: none; padding: 0; }}\n\
</style>\n</head>\n<body>\n{body}</body>\n</html>\n",
        esc(title)
    )
}
