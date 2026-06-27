//! `Signature` (JVMS §4.7.9): the generic-type metadata erased from the plain
//! descriptor. The attribute body is a `u2` index into the constant pool (a
//! Utf8 holding a signature in the grammar of §4.7.9.1). This module parses that
//! grammar and renders it back to javap's Java-source-like text — e.g.
//! `Ljava/util/List<Ljava/lang/String;>;` → `java.util.List<java.lang.String>`.
//!
//! Per the project's decision, parsing and rendering live together here: the
//! recursive-descent parser emits the rendered text directly as it consumes the
//! signature string (there is no separate AST — no other module needs it).

/// The constant-pool index in a `Signature` attribute body (its single `u2`),
/// used for the `Signature: #N` line. None if the body is too short.
pub fn index(bytes: &[u8]) -> Option<u16> {
    if bytes.len() < 2 {
        return None;
    }
    Some(((bytes[0] as u16) << 8) | bytes[1] as u16)
}

/// Renders a field type signature, e.g. `[TT;` → `T[]`. (Field types render the
/// same in both modes, so the `verbose` flag is irrelevant here.)
pub fn field_type(sig: &str) -> String {
    Parser::new(sig, true).type_signature()
}

/// Renders a method declaration (without access modifiers), e.g.
/// `<V:Ljava/lang/Object;>(TV;TU;)TV;` with name `pick`
/// → `<V extends java.lang.Object> V pick(V, U)`. In non-verbose mode an
/// `extends java.lang.Object` bound is elided, matching javap's brief output.
pub fn method_decl(sig: &str, name: &str, verbose: bool, varargs: bool) -> String {
    let mut p = Parser::new(sig, verbose);
    let tparams = p.opt_type_params();
    p.expect(b'(');
    let mut args = Vec::new();
    while p.peek() != b')' && p.has_more() {
        args.push(p.type_signature());
    }
    p.expect(b')');
    let ret = p.type_signature(); // Result: a JavaTypeSignature or `V` → "void"
    // A varargs method's last parameter renders `Type[]` as `Type...`.
    if varargs {
        if let Some(last) = args.last_mut() {
            if let Some(base) = last.strip_suffix("[]") {
                *last = format!("{base}...");
            }
        }
    }
    let prefix = if tparams.is_empty() { String::new() } else { format!("{tparams} ") };
    format!("{prefix}{ret} {name}({})", args.join(", "))
}

/// Like [`method_decl`] but for a constructor: renders `[<TypeParams> ]Class(args)`
/// from the signature (no return type), using the given class name.
pub fn constructor_decl(sig: &str, class: &str, verbose: bool, varargs: bool) -> String {
    let mut p = Parser::new(sig, verbose);
    let tparams = p.opt_type_params();
    p.expect(b'(');
    let mut args = Vec::new();
    while p.peek() != b')' && p.has_more() {
        args.push(p.type_signature());
    }
    p.expect(b')');
    if varargs {
        if let Some(last) = args.last_mut() {
            if let Some(base) = last.strip_suffix("[]") {
                *last = format!("{base}...");
            }
        }
    }
    let prefix = if tparams.is_empty() { String::new() } else { format!("{tparams} ") };
    format!("{prefix}{class}({})", args.join(", "))
}

/// The throws types from a method signature's `^…` parts (type variables or
/// generic classes), rendered Java-style. Empty when the signature has none —
/// in which case the `throws` clause comes from the `Exceptions` attribute.
pub fn method_throws(sig: &str) -> Vec<String> {
    let mut p = Parser::new(sig, true);
    p.opt_type_params();
    p.expect(b'(');
    while p.peek() != b')' && p.has_more() {
        p.type_signature();
    }
    p.expect(b')');
    p.type_signature(); // result
    let mut throws = Vec::new();
    while p.peek() == b'^' {
        p.next();
        throws.push(p.reference_type());
    }
    throws
}

/// Renders the part of a class declaration that comes *after* the class name:
/// the type parameters and the `extends`/`implements` clauses. An interface
/// lists its parents with `extends` and omits the implicit `Object` superclass.
pub fn class_clause(sig: &str, is_interface: bool, verbose: bool) -> String {
    let mut p = Parser::new(sig, verbose);
    let mut out = p.opt_type_params();
    let superclass = p.reference_type(); // SuperclassSignature
    let mut interfaces = Vec::new();
    while p.has_more() {
        interfaces.push(p.reference_type());
    }
    // Unlike the descriptor path (bare `,`), javap's signature renderer joins
    // the parent list with `, ` (comma + space).
    if is_interface {
        // An interface's superclass is always Object (not shown); its declared
        // parents are the superinterfaces, joined under `extends`.
        if !interfaces.is_empty() {
            out.push_str(&format!(" extends {}", interfaces.join(", ")));
        }
    } else {
        // Brief mode elides an `extends java.lang.Object` superclass; `-v` keeps it.
        if verbose || superclass != "java.lang.Object" {
            out.push_str(&format!(" extends {superclass}"));
        }
        if !interfaces.is_empty() {
            out.push_str(&format!(" implements {}", interfaces.join(", ")));
        }
    }
    out
}

/// Recursive-descent cursor over a signature string (grammar §4.7.9.1).
struct Parser<'a> {
    b: &'a [u8],
    i: usize,
    /// `-v` keeps `extends java.lang.Object`; brief mode elides it.
    verbose: bool,
}

impl<'a> Parser<'a> {
    fn new(s: &'a str, verbose: bool) -> Self {
        Parser { b: s.as_bytes(), i: 0, verbose }
    }

    fn peek(&self) -> u8 {
        *self.b.get(self.i).unwrap_or(&0)
    }

    fn next(&mut self) -> u8 {
        let c = self.peek();
        self.i += 1;
        c
    }

    fn expect(&mut self, c: u8) {
        if self.peek() == c {
            self.i += 1;
        }
    }

    fn has_more(&self) -> bool {
        self.i < self.b.len()
    }

    /// JavaTypeSignature: ReferenceTypeSignature | BaseType (and `V` for Result).
    fn type_signature(&mut self) -> String {
        match self.peek() {
            b'L' | b'T' | b'[' => self.reference_type(),
            _ => self.base_type(),
        }
    }

    fn base_type(&mut self) -> String {
        match self.next() {
            b'B' => "byte",
            b'C' => "char",
            b'D' => "double",
            b'F' => "float",
            b'I' => "int",
            b'J' => "long",
            b'S' => "short",
            b'Z' => "boolean",
            b'V' => "void",
            _ => "?",
        }
        .to_string()
    }

    /// ReferenceTypeSignature: ClassTypeSignature | TypeVariableSignature | ArrayTypeSignature.
    fn reference_type(&mut self) -> String {
        match self.peek() {
            b'L' => self.class_type(),
            b'T' => self.type_var(),
            b'[' => {
                self.next();
                format!("{}[]", self.type_signature())
            }
            _ => self.base_type(),
        }
    }

    /// TypeVariableSignature: `T` Identifier `;` → just the identifier.
    fn type_var(&mut self) -> String {
        self.expect(b'T');
        let mut name = String::new();
        while self.peek() != b';' && self.has_more() {
            name.push(self.next() as char);
        }
        self.expect(b';');
        name
    }

    /// ClassTypeSignature: `L` [pkg`/`] SimpleClass {`.`SimpleClass} `;`.
    fn class_type(&mut self) -> String {
        self.expect(b'L');
        let mut out = self.class_name_part();
        if self.peek() == b'<' {
            out.push_str(&self.type_args());
        }
        while self.peek() == b'.' {
            self.next();
            out.push('.');
            out.push_str(&self.class_name_part());
            if self.peek() == b'<' {
                out.push_str(&self.type_args());
            }
        }
        self.expect(b';');
        out
    }

    /// Identifiers (with `/` separators rendered as `.`) up to `<`, `.` or `;`.
    fn class_name_part(&mut self) -> String {
        let mut s = String::new();
        loop {
            match self.peek() {
                b'<' | b'.' | b';' | 0 => break,
                b'/' => {
                    self.next();
                    s.push('.');
                }
                c => {
                    self.next();
                    s.push(c as char);
                }
            }
        }
        s
    }

    /// TypeArguments: `<` TypeArgument+ `>` → `<a, b, c>`.
    fn type_args(&mut self) -> String {
        self.expect(b'<');
        let mut args = Vec::new();
        while self.peek() != b'>' && self.has_more() {
            args.push(self.type_arg());
        }
        self.expect(b'>');
        format!("<{}>", args.join(", "))
    }

    /// TypeArgument: [`+`|`-`] ReferenceTypeSignature | `*`.
    fn type_arg(&mut self) -> String {
        match self.peek() {
            b'*' => {
                self.next();
                "?".to_string()
            }
            b'+' => {
                self.next();
                format!("? extends {}", self.reference_type())
            }
            b'-' => {
                self.next();
                format!("? super {}", self.reference_type())
            }
            _ => self.reference_type(),
        }
    }

    /// TypeParameters: `<` TypeParameter+ `>` → `<X extends …, Y extends …>`.
    /// Returns `""` when there are no type parameters.
    fn opt_type_params(&mut self) -> String {
        if self.peek() != b'<' {
            return String::new();
        }
        self.next();
        let mut params = Vec::new();
        while self.peek() != b'>' && self.has_more() {
            params.push(self.type_param());
        }
        self.expect(b'>');
        format!("<{}>", params.join(", "))
    }

    /// TypeParameter: Identifier ClassBound {InterfaceBound}, each bound `:`Ref.
    /// The class bound may be empty (`::`); multiple bounds join with ` & `.
    fn type_param(&mut self) -> String {
        let mut name = String::new();
        while self.peek() != b':' && self.has_more() {
            name.push(self.next() as char);
        }
        let mut bounds = Vec::new();
        self.expect(b':'); // class bound marker
        if self.peek() != b':' && self.peek() != b'>' {
            bounds.push(self.reference_type());
        }
        while self.peek() == b':' {
            self.next();
            bounds.push(self.reference_type());
        }
        // Brief mode drops a lone `extends java.lang.Object` bound; `-v` keeps it.
        let only_object = bounds.len() == 1 && bounds[0] == "java.lang.Object";
        if bounds.is_empty() || (!self.verbose && only_object) {
            name
        } else {
            format!("{name} extends {}", bounds.join(" & "))
        }
    }
}
