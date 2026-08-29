//! Visualizador del [`ast`](super::ast): renderiza una [`CompilationUnit`] como un **árbol
//! ASCII** con conectores (`├─`/`└─`/`│`), al estilo del comando `tree` — mucho más legible
//! que el `{:#?}` de Rust para inspeccionar lo que produjo el parser.
//!
//! Auto-contenido: solo depende de `super::ast`.

use super::ast::*;

/// Renderiza la unidad de compilación como un árbol ASCII (con `\n` finales por línea).
pub fn tree(unit: &CompilationUnit) -> String {
    let root = cu_node(unit);
    let mut out = String::new();
    out.push_str(&root.label);
    out.push('\n');
    render(&root.children, "", &mut out);
    out
}

/// Un nodo del árbol de visualización: una etiqueta y sus hijos.
struct Node {
    label: String,
    children: Vec<Node>,
}

fn leaf(label: impl Into<String>) -> Node {
    Node { label: label.into(), children: Vec::new() }
}

fn branch(label: impl Into<String>, children: Vec<Node>) -> Node {
    Node { label: label.into(), children }
}

/// Agrupa `child` bajo una etiqueta de rol (p. ej. `cond`, `then`) para desambiguar.
fn role(name: &str, child: Node) -> Node {
    branch(name, vec![child])
}

/// `"Break"` o `"Break L"` según haya etiqueta.
fn label_suffix(kind: &str, label: &Option<String>) -> String {
    match label {
        Some(l) => format!("{kind} {l}"),
        None => kind.to_string(),
    }
}

/// Imprime los hijos con el prefijo de conectores acumulado.
fn render(children: &[Node], prefix: &str, out: &mut String) {
    let n = children.len();
    for (i, child) in children.iter().enumerate() {
        let last = i + 1 == n;
        out.push_str(prefix);
        out.push_str(if last { "└─ " } else { "├─ " });
        out.push_str(&child.label);
        out.push('\n');
        let child_prefix = format!("{prefix}{}", if last { "   " } else { "│  " });
        render(&child.children, &child_prefix, out);
    }
}

// ---- constructores de nodos por nivel del AST ----

fn cu_node(unit: &CompilationUnit) -> Node {
    let mut children = Vec::new();
    if let Some(pkg) = &unit.package {
        children.push(leaf(format!("package {pkg}")));
    }
    for imp in &unit.imports {
        let stat = if imp.is_static { "static " } else { "" };
        let star = if imp.wildcard { ".*" } else { "" };
        children.push(leaf(format!("import {stat}{}{star}", imp.path)));
    }
    for ty in &unit.types {
        children.push(class_node(ty));
    }
    branch("CompilationUnit", children)
}

fn class_node(class: &ClassDecl) -> Node {
    let kw = match class.kind {
        TypeKind::Class => "class",
        TypeKind::Interface => "interface",
        TypeKind::Enum => "enum",
        TypeKind::Record => "record",
        TypeKind::Annotation => "@interface",
    };
    let mut label = format!("{}{}{kw} {}", annos_str(&class.annotations), modifiers(&class.modifiers), class.name);
    if !class.type_params.is_empty() {
        let tps: Vec<String> = class.type_params.iter().map(type_param_str).collect();
        label.push_str(&format!("<{}>", tps.join(", ")));
    }
    if !class.components.is_empty() {
        let cs: Vec<_> =
            class.components.iter().map(|p| format!("{} {}", type_str(&p.ty), p.name)).collect();
        label.push_str(&format!("({})", cs.join(", ")));
    }
    if let Some(sup) = &class.extends {
        label.push_str(&format!(" extends {}", type_str(sup)));
    }
    if !class.implements.is_empty() {
        let names: Vec<_> = class.implements.iter().map(type_str).collect();
        label.push_str(&format!(" implements {}", names.join(", ")));
    }
    let mut children: Vec<Node> = Vec::new();
    for c in &class.enum_constants {
        if c.args.is_empty() {
            children.push(leaf(format!("const {}", c.name)));
        } else {
            children.push(branch(format!("const {}", c.name), c.args.iter().map(expr_node).collect()));
        }
    }
    children.extend(class.members.iter().map(member_node));
    branch(label, children)
}

fn member_node(member: &Member) -> Node {
    match member {
        Member::Field(f) => {
            let label = format!("field {}{}{}: {}", annos_str(&f.annotations), modifiers(&f.modifiers), f.name, type_str(&f.ty));
            match &f.init {
                Some(init) => branch(label, vec![role("init", expr_node(init))]),
                None => leaf(label),
            }
        }
        Member::Method(m) => {
            let params: Vec<_> = m
                .params
                .iter()
                .map(|p| format!("{}{} {}", type_str(&p.ty), if p.varargs { "..." } else { "" }, p.name))
                .collect();
            let tps = if m.type_params.is_empty() {
                String::new()
            } else {
                let ps: Vec<String> = m.type_params.iter().map(type_param_str).collect();
                format!("<{}> ", ps.join(", "))
            };
            let an = annos_str(&m.annotations);
            let label = if m.is_constructor {
                format!("{an}{}{tps}constructor {}({})", modifiers(&m.modifiers), m.name, params.join(", "))
            } else {
                format!(
                    "{an}{}{tps}method {}({}): {}",
                    modifiers(&m.modifiers),
                    m.name,
                    params.join(", "),
                    type_str(&m.return_type)
                )
            };
            match &m.body {
                Some(block) => branch(label, vec![block_node(block)]),
                None => branch(label, vec![leaf("(sin cuerpo)")]),
            }
        }
        Member::Type(nested) => class_node(nested),
        Member::StaticInit(block) => branch("static init", vec![block_node(block)]),
        Member::InstanceInit(block) => branch("instance init", vec![block_node(block)]),
    }
}

fn block_node(block: &Block) -> Node {
    branch("Block", block.0.iter().map(stmt_node).collect())
}

fn stmt_node(stmt: &Stmt) -> Node {
    match &stmt.kind {
        StmtKind::LocalVar { ty, name, init, is_final, .. } => {
            let kw = if *is_final { "final " } else { "" };
            let label = format!("LocalVar {kw}{name}: {}", type_str(ty));
            match init {
                Some(e) => branch(label, vec![role("init", expr_node(e))]),
                None => leaf(label),
            }
        }
        StmtKind::Return(e) => match e {
            Some(e) => branch("Return", vec![expr_node(e)]),
            None => leaf("Return"),
        },
        StmtKind::Expr(e) => branch("ExprStmt", vec![expr_node(e)]),
        StmtKind::If { cond, then, els } => {
            let mut ch = vec![role("cond", expr_node(cond)), role("then", stmt_node(then))];
            if let Some(e) = els {
                ch.push(role("else", stmt_node(e)));
            }
            branch("If", ch)
        }
        StmtKind::While { cond, body } => {
            branch("While", vec![role("cond", expr_node(cond)), role("body", stmt_node(body))])
        }
        StmtKind::For { init, cond, update, body } => {
            let mut ch = Vec::new();
            if let Some(i) = init {
                ch.push(role("init", stmt_node(i)));
            }
            if let Some(c) = cond {
                ch.push(role("cond", expr_node(c)));
            }
            if !update.is_empty() {
                ch.push(branch("update", update.iter().map(expr_node).collect()));
            }
            ch.push(role("body", stmt_node(body)));
            branch("For", ch)
        }
        StmtKind::ForEach { ty, name, iterable, body, is_final } => branch(
            format!("ForEach {}{name}: {}", if *is_final { "final " } else { "" }, type_str(ty)),
            vec![role("in", expr_node(iterable)), role("body", stmt_node(body))],
        ),
        StmtKind::Block(b) => block_node(b),
        StmtKind::Synchronized { lock, body } => {
            branch("Synchronized", vec![role("lock", expr_node(lock)), role("body", block_node(body))])
        }
        StmtKind::Do { body, cond } => {
            branch("Do", vec![role("body", stmt_node(body)), role("while", expr_node(cond))])
        }
        StmtKind::Assert { cond, message } => {
            let mut ch = vec![role("cond", expr_node(cond))];
            if let Some(m) = message {
                ch.push(role("message", expr_node(m)));
            }
            branch("Assert", ch)
        }
        StmtKind::Try { resources, body, catches, finally } => {
            let mut ch = Vec::new();
            if !resources.is_empty() {
                ch.push(branch("resources", resources.iter().map(stmt_node).collect()));
            }
            ch.push(role("body", block_node(body)));
            for c in catches {
                let types: Vec<_> = c.types.iter().map(type_str).collect();
                ch.push(branch(format!("catch {} {}", types.join(" | "), c.name), vec![block_node(&c.body)]));
            }
            if let Some(f) = finally {
                ch.push(role("finally", block_node(f)));
            }
            branch("Try", ch)
        }
        StmtKind::Switch { selector, cases } => {
            let mut ch = vec![role("selector", expr_node(selector))];
            ch.extend(cases.iter().map(case_node));
            branch("Switch", ch)
        }
        StmtKind::Yield(e) => branch("Yield", vec![expr_node(e)]),
        StmtKind::Break(label) => leaf(label_suffix("Break", label)),
        StmtKind::Continue(label) => leaf(label_suffix("Continue", label)),
        StmtKind::Labeled { label, body } => branch(format!("Labeled {label}"), vec![stmt_node(body)]),
        StmtKind::Throw(e) => branch("Throw", vec![expr_node(e)]),
        StmtKind::Empty => leaf("Empty"),
        StmtKind::LocalClass(c) => class_node(c),
    }
}

fn expr_node(expr: &Expr) -> Node {
    match &expr.kind {
        ExprKind::Error => leaf("«error»"),
        ExprKind::IntLit(v) => leaf(format!("IntLit {v}")),
        ExprKind::LongLit(v) => leaf(format!("LongLit {v}")),
        ExprKind::FloatLit(v) => leaf(format!("FloatLit {v}")),
        ExprKind::DoubleLit(v) => leaf(format!("DoubleLit {v}")),
        ExprKind::CharLit(c) => leaf(match char::from_u32(u32::from(*c)) {
            Some(ch) => format!("CharLit {ch:?}"),
            // Un sustituto suelto no tiene `char` que lo muestre: va por su valor.
            None => format!("CharLit 0x{c:04x}"),
        }),
        ExprKind::StringLit(s) => leaf(format!("StringLit {s:?}")),
        ExprKind::BoolLit(b) => leaf(format!("BoolLit {b}")),
        ExprKind::Null => leaf("Null"),
        ExprKind::Name(n) => leaf(format!("Name {n}")),
        ExprKind::This => leaf("This"),
        ExprKind::QualifiedThis(ty) => leaf(format!("QualifiedThis {}.this", type_str(ty))),
        ExprKind::Super => leaf("Super"),
        ExprKind::Binary { op, lhs, rhs } => {
            branch(format!("Binary {}", bin_op(*op)), vec![expr_node(lhs), expr_node(rhs)])
        }
        ExprKind::Unary { op, expr, prefix } => {
            let fix = if *prefix { "prefijo" } else { "postfijo" };
            branch(format!("Unary {} ({fix})", un_op(*op)), vec![expr_node(expr)])
        }
        ExprKind::Assign { op, target, value } => {
            branch(format!("Assign {}", assign_op(*op)), vec![expr_node(target), expr_node(value)])
        }
        ExprKind::Ternary { cond, then, els } => branch(
            "Ternary",
            vec![role("cond", expr_node(cond)), role("then", expr_node(then)), role("else", expr_node(els))],
        ),
        ExprKind::Call { target, name, args, type_args } => {
            let mut ch = Vec::new();
            if let Some(t) = target {
                ch.push(role("target", expr_node(t)));
            }
            if !args.is_empty() {
                ch.push(branch("args", args.iter().map(expr_node).collect()));
            }
            branch(format!("Call {}{name}", type_witness_str(type_args)), ch)
        }
        ExprKind::Field { expr, name } => branch(format!("Field .{name}"), vec![expr_node(expr)]),
        ExprKind::Index { array, index } => {
            branch("Index", vec![role("array", expr_node(array)), role("index", expr_node(index))])
        }
        ExprKind::Cast { ty, expr } => branch(format!("Cast ({})", type_str(ty)), vec![expr_node(expr)]),
        ExprKind::InstanceOf { expr, ty, binding, .. } => {
            let b = binding.as_deref().map(|n| format!(" {n}")).unwrap_or_default();
            branch(format!("InstanceOf {}{b}", type_str(ty)), vec![expr_node(expr)])
        }
        ExprKind::ClassLit(ty) => leaf(format!("ClassLit {}.class", type_str(ty))),
        ExprKind::NewObject { ty, args, body, outer } => {
            let mut ch: Vec<Node> = Vec::new();
            if let Some(o) = outer {
                ch.push(role("outer", expr_node(o)));
            }
            ch.extend(args.iter().map(expr_node));
            if let Some(members) = body {
                ch.push(branch("body", members.iter().map(member_node).collect()));
            }
            let head = if body.is_some() { format!("new {} {{…}}", type_str(ty)) } else { format!("new {}", type_str(ty)) };
            branch(head, ch)
        }
        ExprKind::NewArray { elem, dims, init } => {
            let mut ch = Vec::new();
            for dim in dims {
                match dim {
                    Some(e) => ch.push(role("dim", expr_node(e))),
                    None => ch.push(leaf("dim []")),
                }
            }
            if let Some(elems) = init {
                ch.push(branch("init", elems.iter().map(expr_node).collect()));
            }
            branch(format!("new {}[]", type_str(elem)), ch)
        }
        ExprKind::Switch { selector, cases } => {
            let mut ch = vec![role("selector", expr_node(selector))];
            ch.extend(cases.iter().map(case_node));
            branch("Switch", ch)
        }
        ExprKind::Lambda { params, body } => {
            let mut ch: Vec<Node> = params
                .iter()
                .map(|p| leaf(format!("param {} {}", type_str(&p.ty), p.name)))
                .collect();
            match body.as_ref() {
                LambdaBody::Expr(e) => ch.push(role("body", expr_node(e))),
                LambdaBody::Block(b) => ch.push(branch("body", b.0.iter().map(stmt_node).collect())),
            }
            branch("Lambda", ch)
        }
        ExprKind::MethodRef { qualifier, name, type_args } => {
            let q = match qualifier.as_ref() {
                MethodRefQualifier::Expr(e) => role("qualifier", expr_node(e)),
                MethodRefQualifier::Type(t) => leaf(format!("qualifier {}", type_str(t))),
            };
            let witness = type_witness_str(type_args);
            branch(format!("MethodRef ::{witness}{name}"), vec![q])
        }
        ExprKind::Indy { info, captures } => {
            let mut ch = vec![leaf(format!("bsm {}#{}", info.bootstrap_owner, info.bootstrap_name))];
            ch.extend(captures.iter().map(|c| role("capture", expr_node(c))));
            branch(format!("Indy {}{}", info.name, info.descriptor), ch)
        }
    }
}

fn case_node(c: &SwitchCase) -> Node {
    let head = if c.is_default && c.labels.is_empty() { "case (default)" } else { "case" };
    let mut ch: Vec<Node> = c.labels.iter().map(label_node).collect();
    if c.is_default && !c.labels.is_empty() {
        ch.push(leaf("label default"));
    }
    if let Some(g) = &c.guard {
        ch.push(role("when", expr_node(g)));
    }
    match &c.body {
        SwitchBody::Arrow(s) => ch.push(role("->", stmt_node(s))),
        SwitchBody::Colon(stmts) => ch.push(branch(":", stmts.iter().map(stmt_node).collect())),
    }
    branch(head, ch)
}

fn label_node(l: &CaseLabel) -> Node {
    match l {
        CaseLabel::Constant(e) => role("label", expr_node(e)),
        CaseLabel::Pattern(p) => role("label", pattern_node(p)),
        CaseLabel::Null => leaf("label null"),
    }
}

fn pattern_node(p: &Pattern) -> Node {
    match p {
        Pattern::Type { ty, name, .. } => leaf(format!("pattern {} {name}", type_str(ty))),
        Pattern::Record { ty, components } => branch(
            format!("pattern record {}", type_str(ty)),
            components.iter().map(pattern_node).collect(),
        ),
    }
}

// ---- helpers de string ----

fn modifiers(mods: &[Modifier]) -> String {
    if mods.is_empty() {
        return String::new();
    }
    mods.iter().map(|m| format!("{} ", modifier_str(*m))).collect()
}

/// Las anotaciones de una declaración, como prefijo (`@Override @Deprecated `). Muestra los
/// argumentos de forma compacta (`@Foo(...)`) para no ensuciar el árbol.
fn annos_str(annos: &[Annotation]) -> String {
    annos
        .iter()
        .map(|a| {
            let args = if a.args.is_empty() { "" } else { "(...)" };
            format!("@{}{args} ", a.name)
        })
        .collect()
}

fn modifier_str(m: Modifier) -> &'static str {
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

fn type_str(ty: &Type) -> String {
    match ty {
        Type::Void => "void".to_string(),
        Type::Prim(p) => prim_str(*p).to_string(),
        Type::Class(name) => name.clone(),
        // Sin argumentos es el **diamante** `<>` (pide inferir), no un tipo crudo.
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

/// El *type witness* de una invocación/referencia (`<String>`), o vacío si no lo hay.
fn type_witness_str(args: &[TypeArg]) -> String {
    if args.is_empty() {
        return String::new();
    }
    let a: Vec<String> = args.iter().map(type_arg_str).collect();
    format!("<{}>", a.join(", "))
}

/// Un parámetro de tipo con sus cotas: `T`, `U extends Number & Comparable<U>`.
fn type_param_str(p: &TypeParam) -> String {
    if p.bounds.is_empty() {
        return p.name.clone();
    }
    let bs: Vec<String> = p.bounds.iter().map(type_str).collect();
    format!("{} extends {}", p.name, bs.join(" & "))
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

fn bin_op(op: BinOp) -> &'static str {
    match op {
        BinOp::Or => "||",
        BinOp::And => "&&",
        BinOp::BitOr => "|",
        BinOp::BitXor => "^",
        BinOp::BitAnd => "&",
        BinOp::Eq => "==",
        BinOp::Ne => "!=",
        BinOp::Lt => "<",
        BinOp::Gt => ">",
        BinOp::Le => "<=",
        BinOp::Ge => ">=",
        BinOp::Shl => "<<",
        BinOp::Shr => ">>",
        BinOp::UShr => ">>>",
        BinOp::Add => "+",
        BinOp::Sub => "-",
        BinOp::Mul => "*",
        BinOp::Div => "/",
        BinOp::Rem => "%",
    }
}

fn un_op(op: UnOp) -> &'static str {
    match op {
        UnOp::Plus => "+",
        UnOp::Neg => "-",
        UnOp::Not => "!",
        UnOp::BitNot => "~",
        UnOp::Inc => "++",
        UnOp::Dec => "--",
    }
}

fn assign_op(op: AssignOp) -> &'static str {
    match op {
        AssignOp::Assign => "=",
        AssignOp::Add => "+=",
        AssignOp::Sub => "-=",
        AssignOp::Mul => "*=",
        AssignOp::Div => "/=",
        AssignOp::Rem => "%=",
        AssignOp::And => "&=",
        AssignOp::Or => "|=",
        AssignOp::Xor => "^=",
        AssignOp::Shl => "<<=",
        AssignOp::Shr => ">>=",
        AssignOp::UShr => ">>>=",
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{lexer::tokenize, parser::parse};

    fn tree_of(src: &str) -> String {
        tree(&parse(tokenize(src).unwrap()).0)
    }

    #[test]
    fn renders_add_java_tree() {
        let src = "public class Add {\n\
            public static int add(int a, int b) { return a + b; }\n\
        }";
        let t = tree_of(src);
        assert!(t.starts_with("CompilationUnit\n"));
        assert!(t.contains("class Add"));
        assert!(t.contains("public static method add(int a, int b): int"));
        assert!(t.contains("Binary +"));
        assert!(t.contains("Name a"));
        // Conectores del árbol presentes.
        assert!(t.contains("└─ ") && t.contains("├─ "));
    }

    #[test]
    fn nests_control_flow_with_roles() {
        let src = "class T { void m() { if (x > 0) return; } }";
        let t = tree_of(src);
        assert!(t.contains("If"));
        assert!(t.contains("cond"));
        assert!(t.contains("then"));
    }
}
