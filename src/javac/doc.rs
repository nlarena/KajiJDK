//! **javadoc, etapa 2** — el *parser* de la sintaxis de los doc comments (`/** … */`).
//!
//! La etapa 1 (en el [`lexer`](super::lexer)) ya retiene el **texto crudo** del comentario —sin los
//! delimitadores `/**`…`*/`, pero **con** el margen ` * ` de cada línea— y lo cuelga del campo `doc`
//! de la declaración (`ClassDecl`/`MethodDecl`/`FieldDecl`/`EnumConstant`). Este módulo lo convierte
//! en un modelo estructurado, el [`DocComment`], que el binario `javadoc` (etapas 3–4) renderiza a
//! HTML.
//!
//! El trabajo es el de `com.sun.tools.javac.parser.DocCommentParser`, reducido a lo esencial:
//!
//! 1. **desmarginar** — quitar el ` * ` líder de cada línea (el margen no es contenido);
//! 2. separar la **descripción** de los **block tags** (`@param`, `@return`, …);
//! 3. parsear cada block tag a su variante de [`BlockTag`];
//! 4. parsear los **inline tags** (`{@code}`, `{@link}`, `{@literal}`) dentro de descripción y de
//!    cada texto de tag, a una secuencia de [`Inline`].
//!
//! No se intenta validar (que un `@param` nombre exista, que un `{@link}` resuelva): eso es del
//! generador, que tiene a mano el conjunto de tipos. Aquí solo se *estructura* el texto.

/// Un doc comment ya estructurado: la descripción principal seguida de sus block tags, en el orden
/// en que aparecieron.
#[derive(Debug, Clone, PartialEq, Default)]
pub struct DocComment {
    /// La descripción principal (lo que va antes del primer block tag), con sus inline tags ya
    /// parseados. Vacía si el comentario arranca directo con un block tag.
    pub description: Vec<Inline>,
    /// Los block tags (`@param`, `@return`, …), en orden.
    pub tags: Vec<BlockTag>,
}

/// Un **block tag**: `@nombre …`, que en javadoc debe abrir una línea (o, como concesión al estilo
/// de una sola línea `/** … @param … */`, ir precedido de espacio). Se modelan los de la etapa 2;
/// cualquier otro cae en [`BlockTag::Other`] para no perderlo.
#[derive(Debug, Clone, PartialEq)]
pub enum BlockTag {
    /// `@param nombre descripción`.
    Param { name: String, desc: Vec<Inline> },
    /// `@return descripción`.
    Return { desc: Vec<Inline> },
    /// `@throws Tipo descripción` (y su sinónimo `@exception`).
    Throws { exception: String, desc: Vec<Inline> },
    /// `@see referencia` — la referencia se guarda cruda (un `Tipo`, `Tipo#miembro`, texto, …).
    See { reference: String },
    /// `@deprecated descripción`.
    Deprecated { desc: Vec<Inline> },
    /// `@since texto`.
    Since { text: String },
    /// Cualquier otro tag (`@author`, `@version`, …): nombre + texto crudo con inline tags.
    Other { name: String, desc: Vec<Inline> },
}

/// Un fragmento de contenido *inline*: texto plano o un inline tag ya reconocido.
#[derive(Debug, Clone, PartialEq)]
pub enum Inline {
    /// Texto corrido (todavía sin escapar para HTML — eso lo hace el generador).
    Text(String),
    /// `{@code texto}` o `{@literal texto}`: el texto va literal (el generador lo escapa; el `@code`
    /// además lo envuelve en `<code>`). Se unifican porque su único efecto es "no interpretar".
    Code { text: String, monospace: bool },
    /// `{@link destino etiqueta}` o `{@linkplain …}`: una referencia a un tipo/miembro y una
    /// etiqueta opcional. El generador decide si resuelve a `<a href>` o cae a texto.
    Link { target: String, label: Option<String>, plain: bool },
}

/// Parsea el texto crudo de un doc comment (tal como lo guarda el lexer) a un [`DocComment`].
pub fn parse_doc(raw: &str) -> DocComment {
    let body = demargin(raw);
    let (desc_text, tag_texts) = split_tags(&body);
    let description = parse_inline(desc_text.trim());
    let tags = tag_texts.iter().map(|t| parse_block_tag(t)).collect();
    DocComment { description, tags }
}

/// **Paso 1 — desmarginar.** Quita, línea por línea, el margen líder: espacios, un `*` opcional y un
/// único espacio tras él. Es lo que hace javadoc antes de mirar el contenido: el ` * ` de la columna
/// izquierda es adorno, no texto. La primera línea (que suele ir pegada a `/**`) también se recorta
/// de su espacio líder. Se conservan los saltos de línea (importan para separar block tags).
fn demargin(raw: &str) -> String {
    let mut out = String::new();
    for (i, line) in raw.lines().enumerate() {
        if i > 0 {
            out.push('\n');
        }
        let trimmed = line.trim_start();
        let stripped = match trimmed.strip_prefix('*') {
            // Tras el `*` del margen se come **un** espacio si lo hay (no más, para no tragarse la
            // indentación de un bloque `{@code}`).
            Some(rest) => rest.strip_prefix(' ').unwrap_or(rest),
            None => trimmed,
        };
        out.push_str(stripped);
    }
    out
}

/// **Paso 2 — separar descripción de block tags.** Devuelve `(descripción, [texto de cada tag])`.
///
/// Un block tag empieza en un `@` que abre línea o va precedido de espacio en blanco, y **no** está
/// precedido por `{` (eso sería un inline tag). El texto de cada tag corre hasta el próximo comienzo
/// de block tag. La descripción es todo lo anterior al primero.
fn split_tags(body: &str) -> (&str, Vec<&str>) {
    let bytes = body.as_bytes();
    // Índices (byte) donde arranca cada block tag.
    let mut starts = Vec::new();
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == b'@' {
            let prev = if i == 0 { None } else { Some(bytes[i - 1]) };
            let opens_line = matches!(prev, None | Some(b'\n'));
            let after_space = matches!(prev, Some(c) if c == b' ' || c == b'\t');
            let inline = matches!(prev, Some(b'{'));
            // El char siguiente debe iniciar un nombre de tag (letra), para no confundir un `@` suelto.
            let looks_like_tag = bytes.get(i + 1).is_some_and(|c| c.is_ascii_alphabetic());
            if (opens_line || after_space) && !inline && looks_like_tag {
                starts.push(i);
            }
        }
        i += 1;
    }
    if starts.is_empty() {
        return (body, Vec::new());
    }
    let desc = &body[..starts[0]];
    let mut tags = Vec::new();
    for (k, &s) in starts.iter().enumerate() {
        let end = starts.get(k + 1).copied().unwrap_or(bytes.len());
        tags.push(body[s..end].trim());
    }
    (desc, tags)
}

/// **Paso 3 — un block tag.** El fragmento entra como `@nombre resto…`. Se separa el nombre y, según
/// cuál sea, se parte el `resto` en los pedazos que ese tag define.
fn parse_block_tag(text: &str) -> BlockTag {
    // `text` empieza con `@`. Nombre = letras tras el `@`; el resto es el cuerpo.
    let after_at = &text[1..];
    let name_len = after_at.find(|c: char| !c.is_ascii_alphabetic()).unwrap_or(after_at.len());
    let name = &after_at[..name_len];
    let rest = after_at[name_len..].trim_start();
    match name {
        "param" => {
            let (arg, desc) = split_first_word(rest);
            BlockTag::Param { name: arg.to_string(), desc: parse_inline(desc.trim()) }
        }
        "throws" | "exception" => {
            let (exc, desc) = split_first_word(rest);
            BlockTag::Throws { exception: exc.to_string(), desc: parse_inline(desc.trim()) }
        }
        "return" => BlockTag::Return { desc: parse_inline(rest) },
        "see" => BlockTag::See { reference: rest.trim().to_string() },
        "since" => BlockTag::Since { text: rest.trim().to_string() },
        "deprecated" => BlockTag::Deprecated { desc: parse_inline(rest) },
        other => BlockTag::Other { name: other.to_string(), desc: parse_inline(rest) },
    }
}

/// Parte `s` en (primera palabra, resto), donde la palabra termina en el primer espacio en blanco.
fn split_first_word(s: &str) -> (&str, &str) {
    match s.find(char::is_whitespace) {
        Some(idx) => (&s[..idx], &s[idx..]),
        None => (s, ""),
    }
}

/// **Paso 4 — inline tags.** Convierte un texto en una secuencia de [`Inline`], reconociendo
/// `{@code …}`, `{@literal …}`, `{@link …}` y `{@linkplain …}`. Las llaves internas se equilibran
/// para permitir contenido con `{`/`}`. Un `{` que no abre un inline tag conocido se trata como texto.
fn parse_inline(text: &str) -> Vec<Inline> {
    let mut out: Vec<Inline> = Vec::new();
    let mut buf = String::new();
    let bytes = text.as_bytes();
    let mut i = 0;
    while i < bytes.len() {
        // ¿Abre un inline tag conocido en `i`?  Forma: `{@nombre …}`.
        if bytes[i] == b'{' && bytes.get(i + 1) == Some(&b'@') {
            if let Some((tag, content, end)) = read_inline_tag(text, i) {
                match tag {
                    "code" | "literal" => {
                        flush(&mut out, &mut buf);
                        out.push(Inline::Code {
                            text: content.to_string(),
                            monospace: tag == "code",
                        });
                    }
                    "link" | "linkplain" => {
                        flush(&mut out, &mut buf);
                        let (target, label) = split_first_word(content.trim());
                        let label = label.trim();
                        out.push(Inline::Link {
                            target: target.to_string(),
                            label: (!label.is_empty()).then(|| label.to_string()),
                            plain: tag == "linkplain",
                        });
                    }
                    _ => {
                        // Inline tag desconocido: se deja el texto crudo (sin interpretar las llaves).
                        buf.push_str(&text[i..end]);
                    }
                }
                i = end;
                continue;
            }
        }
        // Char normal: se acumula (avanzando por caracteres UTF-8 completos).
        let ch_len = utf8_len(bytes[i]);
        buf.push_str(&text[i..i + ch_len]);
        i += ch_len;
    }
    flush(&mut out, &mut buf);
    out
}

/// Lee un inline tag que empieza en `start` (`text[start] == '{'`). Devuelve `(nombre, contenido,
/// fin)` donde `fin` es el índice **tras** la `}` de cierre, o `None` si no cierra o no tiene nombre.
fn read_inline_tag(text: &str, start: usize) -> Option<(&str, &str, usize)> {
    let bytes = text.as_bytes();
    // start apunta a `{`, start+1 a `@`. Nombre = letras tras el `@`.
    let name_start = start + 2;
    let after = &text[name_start..];
    let name_len = after.find(|c: char| !c.is_ascii_alphabetic()).unwrap_or(after.len());
    if name_len == 0 {
        return None;
    }
    let name = &text[name_start..name_start + name_len];
    // El contenido va tras el nombre (comiendo un espacio separador) hasta la `}` que equilibra.
    let mut j = name_start + name_len;
    // Un espacio de separación entre el nombre y el contenido, si lo hay.
    if bytes.get(j) == Some(&b' ') {
        j += 1;
    }
    let content_start = j;
    let mut depth = 1;
    while j < bytes.len() {
        match bytes[j] {
            b'{' => depth += 1,
            b'}' => {
                depth -= 1;
                if depth == 0 {
                    return Some((name, &text[content_start..j], j + 1));
                }
            }
            _ => {}
        }
        j += 1;
    }
    None // sin `}` de cierre → no era un inline tag bien formado
}

/// Vuelca el buffer de texto acumulado como un [`Inline::Text`] (si no está vacío).
fn flush(out: &mut Vec<Inline>, buf: &mut String) {
    if !buf.is_empty() {
        out.push(Inline::Text(std::mem::take(buf)));
    }
}

/// Largo en bytes del char UTF-8 cuyo primer byte es `b`.
fn utf8_len(b: u8) -> usize {
    if b < 0x80 {
        1
    } else if b >> 5 == 0b110 {
        2
    } else if b >> 4 == 0b1110 {
        3
    } else {
        4
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn demargin_strips_the_leading_star_and_one_space() {
        let raw = "\n * Suma dos enteros.\n * Segunda línea.\n ";
        assert_eq!(demargin(raw), "\nSuma dos enteros.\nSegunda línea.\n");
    }

    #[test]
    fn a_single_line_comment_splits_description_and_tags() {
        // El caso del enunciado: descripción y tags en la misma línea.
        let doc = parse_doc(" Suma. @param a el primero @return la suma ");
        assert_eq!(doc.description, vec![Inline::Text("Suma.".to_string())]);
        assert_eq!(
            doc.tags,
            vec![
                BlockTag::Param {
                    name: "a".to_string(),
                    desc: vec![Inline::Text("el primero".to_string())],
                },
                BlockTag::Return { desc: vec![Inline::Text("la suma".to_string())] },
            ]
        );
    }

    #[test]
    fn multiline_block_tags_are_separated_by_line_start() {
        let raw = "\n * Descripción.\n *\n * @param x la equis\n * @param y la ye\n * @return el resultado\n * @throws IllegalStateException si algo\n ";
        let doc = parse_doc(raw);
        assert_eq!(doc.tags.len(), 4);
        assert_eq!(
            doc.tags[0],
            BlockTag::Param {
                name: "x".to_string(),
                desc: vec![Inline::Text("la equis".to_string())],
            }
        );
        assert_eq!(
            doc.tags[3],
            BlockTag::Throws {
                exception: "IllegalStateException".to_string(),
                desc: vec![Inline::Text("si algo".to_string())],
            }
        );
    }

    #[test]
    fn inline_code_and_link_are_parsed() {
        let doc = parse_doc(" Usa {@code new Foo()} y ver {@link Foo#bar etiqueta}. ");
        assert_eq!(
            doc.description,
            vec![
                Inline::Text("Usa ".to_string()),
                Inline::Code { text: "new Foo()".to_string(), monospace: true },
                Inline::Text(" y ver ".to_string()),
                Inline::Link {
                    target: "Foo#bar".to_string(),
                    label: Some("etiqueta".to_string()),
                    plain: false,
                },
                Inline::Text(".".to_string()),
            ]
        );
    }

    #[test]
    fn link_without_label_keeps_only_target() {
        let doc = parse_doc(" Ver {@link Otra}. ");
        assert_eq!(
            doc.description[1],
            Inline::Link { target: "Otra".to_string(), label: None, plain: false }
        );
    }

    #[test]
    fn see_since_and_deprecated_are_recognized() {
        let doc = parse_doc(" X. @since 1.0 @deprecated usar Y @see Otra#m ");
        assert!(doc.tags.contains(&BlockTag::Since { text: "1.0".to_string() }));
        assert!(doc.tags.contains(&BlockTag::See { reference: "Otra#m".to_string() }));
        assert!(doc.tags.iter().any(|t| matches!(
            t,
            BlockTag::Deprecated { desc } if desc == &vec![Inline::Text("usar Y".to_string())]
        )));
    }

    #[test]
    fn an_unclosed_inline_tag_stays_as_text() {
        let doc = parse_doc(" roto {@code sin cierre ");
        // Sin `}` no es un inline tag: todo queda como texto.
        assert_eq!(doc.description, vec![Inline::Text("roto {@code sin cierre".to_string())]);
    }
}
