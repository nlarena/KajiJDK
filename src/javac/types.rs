//! El **álgebra de tipos**: las operaciones sobre [`RType`] que definen el sistema de tipos de
//! Java, separadas de la fase que las usa. Es el `Types.java` de javac.
//!
//! Acá viven las preguntas que se hacen la pasada 2 ([`attribute`](super::attribute)) y la
//! inferencia: *¿`S` es subtipo de `T`?*, *¿qué queda de `List<String>` al borrar los tipos?*,
//! *¿qué es `E` cuando miro `ArrayList<String>` como `List<E>`?*.
//!
//! Las tres piezas centrales:
//!
//! - **Erasure** ([`erasure`], JLS §4.6) — lo que sobrevive en el bytecode: `List<String>` → `List`.
//! - **Sustitución** ([`substitute`]) — cambiar parámetros de tipo por argumentos; es lo que hace
//!   que `ArrayList<String> <: List<String>` sea demostrable.
//! - **Subtipado** ([`is_subtype`], §4.10.2) con **containment** de wildcards (§4.5.1).

use std::collections::HashMap;

use super::ast::PrimType;
use super::symbol::{RType, RTypeArg, SymbolId, SymbolKind, SymbolTable};

/// Un mapa de sustitución: parámetro de tipo (`SymbolId` de un `TypeVar`) → argumento.
pub type Subst = HashMap<SymbolId, RType>;

// ---- erasure (JLS §4.6) ----

/// La **erasure** de un tipo: lo que queda al borrar los genéricos, o sea lo que ve la JVM.
///
/// - `List<String>` → `List`; `T` → la erasure de su **primera cota** (o `Object` si no tiene);
/// - `T[]` → `|T|[]`; los primitivos y `void` no cambian.
pub fn erasure(table: &SymbolTable, ty: &RType) -> RType {
    match ty {
        RType::Parameterized { base, .. } => RType::Class(*base),
        RType::Array(elem) => RType::Array(Box::new(erasure(table, elem))),
        RType::TypeVar(id) => match first_bound(table, *id) {
            Some(b) => erasure(table, &b),
            // Sin cota declarada la erasure es `Object` (si no lo tenemos cargado, se queda).
            None => match table.external("Object") {
                Some(obj) => RType::Class(obj),
                None => RType::Unresolved,
            },
        },
        RType::Prim(_) | RType::Void | RType::Class(_) | RType::Unresolved => ty.clone(),
    }
}

/// La primera cota **resuelta** de un parámetro de tipo, si declaró alguna.
fn first_bound(table: &SymbolTable, tv: SymbolId) -> Option<RType> {
    match table.resolved(tv) {
        Some(super::symbol::Resolved::TypeVar { bounds }) => bounds.first().cloned(),
        _ => None,
    }
}

// ---- sustitución ----

/// Reemplaza en `ty` cada parámetro de tipo por su argumento según `subst`. Los que no estén en
/// el mapa quedan intactos (siguen siendo variables).
pub fn substitute(ty: &RType, subst: &Subst) -> RType {
    match ty {
        RType::TypeVar(id) => subst.get(id).cloned().unwrap_or_else(|| ty.clone()),
        RType::Array(elem) => RType::Array(Box::new(substitute(elem, subst))),
        RType::Parameterized { base, args } => RType::Parameterized {
            base: *base,
            args: args.iter().map(|a| substitute_arg(a, subst)).collect(),
        },
        RType::Prim(_) | RType::Void | RType::Class(_) | RType::Unresolved => ty.clone(),
    }
}

fn substitute_arg(arg: &RTypeArg, subst: &Subst) -> RTypeArg {
    match arg {
        RTypeArg::Type(t) => RTypeArg::Type(substitute(t, subst)),
        RTypeArg::Extends(t) => RTypeArg::Extends(Box::new(substitute(t, subst))),
        RTypeArg::Super(t) => RTypeArg::Super(Box::new(substitute(t, subst))),
        RTypeArg::Wildcard => RTypeArg::Wildcard,
    }
}

/// Los parámetros de tipo declarados por una clase, **en orden de declaración** — que es el de
/// sus `SymbolId` (se crean al entrar la clase), y el orden en que `members_of` los devuelve.
pub fn type_params_of(table: &SymbolTable, class: SymbolId) -> Vec<SymbolId> {
    table
        .members_of(class)
        .into_iter()
        .filter(|&id| matches!(table.symbol(id).kind, SymbolKind::TypeVar { .. }))
        .collect()
}

/// El mapa `parámetro → argumento` de un tipo parametrizado. Vacío si es crudo, si es el
/// diamante, o si la aridad no coincide (código mal formado: no inventamos sustituciones).
pub fn subst_of(table: &SymbolTable, ty: &RType) -> Subst {
    let RType::Parameterized { base, args } = ty else { return Subst::new() };
    let params = type_params_of(table, *base);
    if params.len() != args.len() {
        return Subst::new();
    }
    params
        .iter()
        .zip(args)
        .map(|(&p, a)| (p, arg_as_type(a)))
        .collect()
}

/// La sustitución que hace falta para mirar `ty` **como** su supertipo `owner`.
///
/// Es lo que convierte la firma declarada de un miembro heredado en la que ve el que llama: para
/// `xs.get(0)` con `xs: ArrayList<String>` y `get` declarado en `List<E>`, busca `List<String>`
/// entre los supertipos de `ArrayList<String>` y devuelve `{E → String}`. Sin esto, `get`
/// devolvería `E` en crudo.
pub fn subst_for(table: &SymbolTable, ty: &RType, owner: SymbolId) -> Subst {
    supertypes_of(table, ty)
        .into_iter()
        .find(|s| erased_id(s) == Some(owner))
        .map(|s| subst_of(table, &s))
        .unwrap_or_default()
}

/// El tipo que aporta un argumento para sustituir. Un wildcard no es un tipo: se usa su cota
/// (aproximación — el *capture conversion* real (§5.1.10) es cola larga).
fn arg_as_type(arg: &RTypeArg) -> RType {
    match arg {
        RTypeArg::Type(t) => t.clone(),
        RTypeArg::Extends(t) => (**t).clone(),
        RTypeArg::Super(_) | RTypeArg::Wildcard => RType::Unresolved,
    }
}

// ---- supertipos ----

/// Los supertipos **directos** de un tipo, ya sustituidos: mirar `ArrayList<String>` da
/// `AbstractList<String>` y `List<String>`, no `AbstractList<E>`.
pub fn direct_supertypes(table: &SymbolTable, ty: &RType) -> Vec<RType> {
    let Some(base) = erased_id(ty) else { return Vec::new() };
    let subst = subst_of(table, ty);
    let mut out = Vec::new();
    if let Some(s) = table.super_type(base) {
        out.push(substitute(s, &subst));
    }
    for i in table.interface_types(base) {
        out.push(substitute(i, &subst));
    }
    out
}

/// El `SymbolId` de un tipo referencia, sea crudo o parametrizado (su erasure).
pub fn erased_id(ty: &RType) -> Option<SymbolId> {
    match ty {
        RType::Class(id) | RType::Parameterized { base: id, .. } => Some(*id),
        _ => None,
    }
}

// ---- subtipado (JLS §4.10.2) ----

/// ¿`s <: t`? Subtipado **probado**, sin indulgencias.
///
/// Sube por los supertipos de `s` sustituyendo los argumentos, hasta encontrar uno con la misma
/// base que `t`; ahí compara los argumentos por **containment** (§4.5.1). Un tipo crudo o el
/// diamante contra un parametrizado se aceptan (unchecked, §4.8).
pub fn is_subtype(table: &SymbolTable, s: &RType, t: &RType) -> bool {
    if s == t {
        return true;
    }
    match (s, t) {
        (RType::Prim(a), RType::Prim(b)) => widens_to(*a, *b),
        // Arrays: covariantes en el tipo elemento (§4.10.3).
        (RType::Array(a), RType::Array(b)) => is_subtype(table, a, b),
        // Una variable de tipo es subtipo de sus cotas.
        (RType::TypeVar(v), _) => bounds_of(table, *v).iter().any(|b| is_subtype(table, b, t)),
        _ => {
            let (Some(sb), Some(tb)) = (erased_id(s), erased_id(t)) else { return false };
            if object_id(table) == Some(tb) {
                return true; // todo tipo referencia es subtipo de Object
            }
            if sb == tb {
                return same_base_ok(table, s, t);
            }
            // Buscar `t` entre los supertipos, sustituyendo al subir.
            direct_supertypes(table, s).iter().any(|sup| is_subtype(table, sup, t))
        }
    }
}

/// Con la **misma base**, `s <: t` sii cada argumento de `t` *contiene* al de `s` (§4.5.1).
/// Si alguno de los dos es crudo o diamante, no hay nada que comparar (§4.8, unchecked).
fn same_base_ok(table: &SymbolTable, s: &RType, t: &RType) -> bool {
    match (s, t) {
        (RType::Parameterized { args: sa, .. }, RType::Parameterized { args: ta, .. }) => {
            if sa.is_empty() || ta.is_empty() || sa.len() != ta.len() {
                return true; // diamante o aridad rota: no bloqueamos
            }
            sa.iter().zip(ta).all(|(a, b)| contains(table, b, a))
        }
        // Uno de los dos es crudo: conversión unchecked (§4.8) — se acepta.
        _ => true,
    }
}

/// **Containment** (§4.5.1): ¿el argumento `t` "contiene" a `s` (`s <= t`)?
///
/// - `?` y `? extends Object` contienen a todo;
/// - `? extends T` contiene a `S` si `S <: T`;
/// - `? super T` contiene a `S` si `T <: S`;
/// - un argumento **concreto** solo se contiene a sí mismo — por eso `List<String>` **no** es
///   subtipo de `List<Object>` (la trampa clásica de los genéricos).
fn contains(table: &SymbolTable, t: &RTypeArg, s: &RTypeArg) -> bool {
    match t {
        RTypeArg::Wildcard => true,
        RTypeArg::Extends(tb) => match s {
            RTypeArg::Type(st) => is_subtype(table, st, tb),
            RTypeArg::Extends(sb) => is_subtype(table, sb, tb),
            _ => false,
        },
        RTypeArg::Super(tb) => match s {
            RTypeArg::Type(st) => is_subtype(table, tb, st),
            RTypeArg::Super(sb) => is_subtype(table, tb, sb),
            _ => false,
        },
        RTypeArg::Type(tt) => match s {
            RTypeArg::Type(st) => st == tt,
            _ => false,
        },
    }
}

/// Las cotas resueltas de un parámetro de tipo; `Object` si no declaró ninguna.
pub fn bounds_of(table: &SymbolTable, tv: SymbolId) -> Vec<RType> {
    match table.resolved(tv) {
        Some(super::symbol::Resolved::TypeVar { bounds }) if !bounds.is_empty() => bounds.clone(),
        _ => match table.external("Object") {
            Some(obj) => vec![RType::Class(obj)],
            None => Vec::new(),
        },
    }
}

fn object_id(table: &SymbolTable) -> Option<SymbolId> {
    table.external("Object")
}

/// El *widening* primitivo (§5.1.2), que es el subtipado entre primitivos (§4.10.1).
pub fn widens_to(from: PrimType, to: PrimType) -> bool {
    if from == to {
        return true;
    }
    if matches!(from, PrimType::Boolean) || matches!(to, PrimType::Boolean) {
        return false;
    }
    rank(from) <= rank(to)
}

// ---- boxing (JLS §5.1.7 / §5.1.8) ----

/// La clase *wrapper* de un primitivo (§5.1.7).
pub fn wrapper_of(p: PrimType) -> &'static str {
    match p {
        PrimType::Boolean => "Boolean",
        PrimType::Byte => "Byte",
        PrimType::Short => "Short",
        PrimType::Char => "Character",
        PrimType::Int => "Integer",
        PrimType::Long => "Long",
        PrimType::Float => "Float",
        PrimType::Double => "Double",
    }
}

/// El tipo **boxeado**: `int` → `Integer`. Los que no son primitivos quedan igual. Lo necesita la
/// inferencia: un argumento de tipo nunca puede ser un primitivo (§4.5.1), así que de `id(1)` se
/// deduce `T = Integer`, no `T = int`.
pub fn boxed(table: &SymbolTable, t: &RType) -> RType {
    match t {
        RType::Prim(p) => match table.external(wrapper_of(*p)) {
            Some(w) => RType::Class(w),
            None => t.clone(),
        },
        _ => t.clone(),
    }
}

/// El primitivo que envuelve una clase, si es un *wrapper* externo (§5.1.8).
pub fn unboxed(table: &SymbolTable, c: SymbolId) -> Option<PrimType> {
    if table.symbol(c).owner.is_some() {
        return None; // una clase del fuente llamada `Integer` no es `java.lang.Integer`
    }
    Some(match table.symbol(c).name.as_str() {
        "Boolean" => PrimType::Boolean,
        "Byte" => PrimType::Byte,
        "Short" => PrimType::Short,
        "Character" => PrimType::Char,
        "Integer" => PrimType::Int,
        "Long" => PrimType::Long,
        "Float" => PrimType::Float,
        "Double" => PrimType::Double,
        _ => return None,
    })
}

// ---- lub / glb ----

/// Todos los supertipos de `ty`, él incluido (cierre reflexivo-transitivo), ya sustituidos.
/// Acotado por un set de visitados: la jerarquía de Java es un DAG (interfaces múltiples) y sin
/// eso se recorrerían los rombos una y otra vez.
pub fn supertypes_of(table: &SymbolTable, ty: &RType) -> Vec<RType> {
    let mut out: Vec<RType> = Vec::new();
    let mut queue = vec![ty.clone()];
    while let Some(t) = queue.pop() {
        if out.contains(&t) {
            continue;
        }
        out.push(t.clone());
        queue.extend(direct_supertypes(table, &t));
    }
    out
}

/// El **lub** (*least upper bound*, §4.10.4): el tipo más específico que es supertipo de todos.
///
/// Lo usan el ternario (§15.25) y la resolución de la inferencia (§18.4). Esta es la versión
/// *práctica*: busca el supertipo común más profundo. La del JLS construye una intersección
/// (`lub(A,B) = X & Y`) y aplica *lub con recursión infinita acotada*; eso es cola larga.
pub fn lub(table: &SymbolTable, types: &[RType]) -> RType {
    let mut it = types.iter().filter(|t| !matches!(t, RType::Unresolved));
    let Some(first) = it.next() else { return RType::Unresolved };
    // Solo tipos referencia **resueltos**: un supertipo cuyo `.class` no cargamos queda como
    // `Unresolved`, y si entrara acá ningún candidato podría probar ser subtipo de *todos* —
    // el `lub` se desplomaría a `Object` aun para un único tipo.
    let mut candidates: Vec<RType> =
        supertypes_of(table, first).into_iter().filter(|c| erased_id(c).is_some()).collect();
    for t in it {
        // Quedarse solo con los supertipos que también lo son de `t`.
        candidates.retain(|c| is_subtype(table, t, c));
    }
    // El más específico de los que quedan: el que es subtipo de todos los demás.
    candidates
        .iter()
        .find(|c| candidates.iter().all(|o| is_subtype(table, c, o)))
        .cloned()
        .unwrap_or_else(|| match object_id(table) {
            Some(o) => RType::Class(o),
            None => RType::Unresolved,
        })
}

/// El **glb** (*greatest lower bound*, §5.1.10): el tipo más general que es subtipo de todos.
/// Con tipos no relacionados el JLS produce una intersección (`A & B`), que todavía no
/// modelamos: en ese caso devolvemos el primero, que es la aproximación que alcanza para las
/// cotas inferiores de la inferencia.
pub fn glb(table: &SymbolTable, types: &[RType]) -> RType {
    let mut best: Option<RType> = None;
    for t in types.iter().filter(|t| !matches!(t, RType::Unresolved)) {
        best = Some(match best {
            None => t.clone(),
            Some(b) if is_subtype(table, t, &b) => t.clone(),
            Some(b) => b,
        });
    }
    best.unwrap_or(RType::Unresolved)
}

/// El orden de ensanchamiento numérico. `char` y `short` comparten rango: ninguno ensancha al
/// otro (§5.1.2), y eso se chequea aparte.
pub fn rank(p: PrimType) -> u8 {
    match p {
        PrimType::Boolean => 0,
        PrimType::Byte => 1,
        PrimType::Short | PrimType::Char => 2,
        PrimType::Int => 3,
        PrimType::Long => 4,
        PrimType::Float => 5,
        PrimType::Double => 6,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{enter::enter, lexer::tokenize, parser::parse};

    fn table_of(src: &str) -> SymbolTable {
        let unit = parse(tokenize(src).unwrap()).unwrap();
        let (t, errs) = enter(&unit);
        assert!(errs.is_empty(), "la pasada 1 no debería fallar: {errs:?}");
        t
    }

    /// El tipo del campo `f` de la clase `Probe` — la forma cómoda de escribir un `RType`
    /// arbitrario en un test: se declara en Java y se lee ya resuelto.
    fn field_ty(table: &SymbolTable, class: &str, field: &str) -> RType {
        let cid = table.class(class).expect("clase del fuente");
        let fid = table
            .members_of(cid)
            .into_iter()
            .find(|&m| table.symbol(m).name == field)
            .expect("el campo existe");
        match table.resolved(fid) {
            Some(super::super::symbol::Resolved::Field(t)) => t.clone(),
            other => panic!("el campo no resolvió: {other:?}"),
        }
    }

    #[test]
    fn generics_are_invariant() {
        // La trampa clásica: `List<String>` **no** es subtipo de `List<Object>` (§4.5.1) —
        // un argumento concreto solo se contiene a sí mismo.
        let t = table_of(
            "class Lst<T> {}
             class Probe { Lst<String> s; Lst<Object> o; }",
        );
        let s = field_ty(&t, "Probe", "s");
        let o = field_ty(&t, "Probe", "o");
        assert!(is_subtype(&t, &s, &s), "reflexivo");
        assert!(!is_subtype(&t, &s, &o), "Lst<String> NO es subtipo de Lst<Object>");
        assert!(!is_subtype(&t, &o, &s), "ni al revés");
    }

    #[test]
    fn wildcards_are_covariant_with_extends() {
        // `Lst<String> <: Lst<? extends Object>`: el wildcard **contiene** al argumento concreto.
        let t = table_of(
            "class Lst<T> {}
             class Probe { Lst<String> s; Lst<? extends Object> up; Lst<?> any; }",
        );
        let s = field_ty(&t, "Probe", "s");
        assert!(is_subtype(&t, &s, &field_ty(&t, "Probe", "up")), "? extends Object contiene String");
        assert!(is_subtype(&t, &s, &field_ty(&t, "Probe", "any")), "? contiene cualquier cosa");
    }

    #[test]
    fn wildcards_are_contravariant_with_super() {
        // `Lst<Object> <: Lst<? super String>`, pero `Lst<String>` no lo es de `Lst<? super Object>`.
        let t = table_of(
            "class Lst<T> {}
             class Probe { Lst<Object> o; Lst<String> s; Lst<? super String> lo; Lst<? super Object> uo; }",
        );
        let o = field_ty(&t, "Probe", "o");
        let s = field_ty(&t, "Probe", "s");
        assert!(is_subtype(&t, &o, &field_ty(&t, "Probe", "lo")), "? super String contiene Object");
        assert!(!is_subtype(&t, &s, &field_ty(&t, "Probe", "uo")), "? super Object NO contiene String");
    }

    #[test]
    fn substitution_climbs_the_hierarchy() {
        // `StrBox <: Box<String>` exige **sustituir** `T:=String` en `extends Box<T>`.
        let t = table_of(
            "class Box<T> {}
             class StrBox extends Box<String> {}
             class Probe { StrBox sb; Box<String> bs; Box<Integer> bi; }",
        );
        let sb = field_ty(&t, "Probe", "sb");
        assert!(is_subtype(&t, &sb, &field_ty(&t, "Probe", "bs")), "StrBox <: Box<String>");
        assert!(!is_subtype(&t, &sb, &field_ty(&t, "Probe", "bi")), "StrBox NO es Box<Integer>");
    }

    #[test]
    fn erasure_drops_the_type_arguments() {
        let t = table_of(
            "class Lst<T> {}
             class Probe { Lst<String> s; }",
        );
        let s = field_ty(&t, "Probe", "s");
        let RType::Parameterized { base, .. } = &s else { panic!("{s:?}") };
        assert_eq!(erasure(&t, &s), RType::Class(*base), "|Lst<String>| = Lst");
    }

    #[test]
    fn erasure_of_a_type_variable_is_its_first_bound() {
        // `|T|` con `<T extends Number>` es `Number`; sin cota, `Object` (§4.6).
        let t = table_of("class C<T extends Number, U> { T a; U b; }");
        let a = field_ty(&t, "C", "a");
        let number = t.external("Number").expect("Number cargado");
        assert_eq!(erasure(&t, &a), RType::Class(number));

        let b = field_ty(&t, "C", "b");
        let object = t.external("Object").expect("Object cargado");
        assert_eq!(erasure(&t, &b), RType::Class(object));
    }

    #[test]
    fn lub_of_two_siblings_is_their_common_supertype() {
        let t = table_of(
            "class Animal {}
             class Dog extends Animal {}
             class Cat extends Animal {}
             class Probe { Dog d; Cat c; Animal a; }",
        );
        let d = field_ty(&t, "Probe", "d");
        let c = field_ty(&t, "Probe", "c");
        assert_eq!(lub(&t, &[d, c]), field_ty(&t, "Probe", "a"));
    }

    #[test]
    fn lub_of_unrelated_types_is_object() {
        let t = table_of("class A {} class B {} class Probe { A a; B b; }");
        let object = t.external("Object").expect("Object cargado");
        let (a, b) = (field_ty(&t, "Probe", "a"), field_ty(&t, "Probe", "b"));
        assert_eq!(lub(&t, &[a, b]), RType::Class(object));
    }

    #[test]
    fn lub_of_a_type_with_itself_is_itself() {
        let t = table_of("class A {} class Probe { A a; }");
        let a = field_ty(&t, "Probe", "a");
        assert_eq!(lub(&t, &[a.clone(), a.clone()]), a);
    }

    #[test]
    fn glb_picks_the_most_derived() {
        let t = table_of(
            "class Animal {}
             class Dog extends Animal {}
             class Probe { Dog d; Animal a; }",
        );
        let d = field_ty(&t, "Probe", "d");
        let a = field_ty(&t, "Probe", "a");
        assert_eq!(glb(&t, &[a, d.clone()]), d, "glb(Animal, Dog) = Dog");
    }

    #[test]
    fn a_type_variable_is_a_subtype_of_its_bound() {
        let t = table_of("class C<T extends Number> { T a; Number n; }");
        let a = field_ty(&t, "C", "a");
        let n = field_ty(&t, "C", "n");
        assert!(is_subtype(&t, &a, &n), "T <: Number por su cota");
        assert!(!is_subtype(&t, &n, &a), "pero Number no es T");
    }
}
