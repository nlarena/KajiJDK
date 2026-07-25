//! La **inferencia de tipos** (JLS Cap. 18) — el `Infer.java` de javac.
//!
//! Cuando llamás a un método genérico sin escribir los argumentos de tipo (`id("hola")` con
//! `<T> T id(T x)`), hay que **deducirlos**. El capítulo 18 lo plantea como un problema de
//! restricciones, en cuatro pasos:
//!
//! 1. **Variables de inferencia** (§18.1) — una `α` por cada parámetro de tipo del método.
//!    Acá una `α` *es* el símbolo del parámetro de tipo: [`RType::TypeVar`] apuntando a él.
//! 2. **Reducción** (§18.2) — cada *constraint formula* `‹argumento → parámetro›` se destila en
//!    **bounds** sobre las `α` (`String <: α`, `α = String`, …).
//! 3. **Incorporación** (§18.3) — de pares de bounds salen bounds nuevos.
//! 4. **Resolución** (§18.4) — instanciar cada `α`: con sus cotas inferiores va el `lub`; si solo
//!    tiene superiores, el `glb`; si no tiene ninguna, su propia cota declarada.
//!
//! ## Alcance
//!
//! Cubre las dos fuentes de restricciones:
//!
//! - Los **argumentos** de la llamada (§18.5.1 aplicabilidad y §18.5.2 tipo de la invocación).
//! - El ***target type***: el tipo que el contexto espera del resultado. Es lo único que puede
//!   instanciar una variable que **no aparece** en los argumentos — `Caja<String> c = fabricar();`
//!   o el diamante `new Caja<>()` (§15.9.3, vía [`infer_diamond`]). Requiere que la atribución baje
//!   un tipo esperado: el modo *checking* del chequeo bidireccional, que `attribute` aporta con
//!   `attrib_expr_to`.
//!
//! Los contextos que hoy aportan target son los tres de asignación (§5.2): el inicializador de una
//! variable, el `return` y el lado derecho de un `=`. **Falta** el de **argumento de llamada**
//! (`f(fabricar())`, donde el target es el tipo del parámetro): eso pide el algoritmo de dos fases
//! de javac —resolver la sobrecarga y recién entonces re-atribuir los argumentos *poly* con el
//! parámetro elegido—, y hoy los argumentos se atribuyen una sola vez, antes de elegir. Tampoco
//! hay *poly expressions* propiamente dichas (§18.5.2.1): el parser no tiene lambdas.

use std::collections::HashMap;

use super::symbol::{RType, RTypeArg, Resolved, SymbolId, SymbolKind, SymbolTable};
use super::types::{self, Subst};

/// Los parámetros de tipo **del método** (`<T> T id(T x)` → `[T]`). Son las variables a inferir;
/// no confundir con los de la clase, que ya vienen instanciados por el receptor.
pub fn method_type_params(table: &SymbolTable, m: SymbolId) -> Vec<SymbolId> {
    table
        .members_of(m)
        .into_iter()
        .filter(|&id| matches!(table.symbol(id).kind, SymbolKind::TypeVar { .. }))
        .collect()
}

/// El conjunto de **bounds** sobre las variables de inferencia (§18.1.3).
struct BoundSet {
    /// Las variables a inferir.
    vars: Vec<SymbolId>,
    /// `T <: α` — cotas **inferiores**: la α tiene que poder contener a `T`.
    lower: Vec<(SymbolId, RType)>,
    /// `α <: T` — cotas **superiores**.
    upper: Vec<(SymbolId, RType)>,
    /// `α = T` — igualdades (las que produce el containment de un argumento concreto).
    eq: Vec<(SymbolId, RType)>,
}

impl BoundSet {
    fn new(vars: Vec<SymbolId>) -> Self {
        BoundSet { vars, lower: Vec::new(), upper: Vec::new(), eq: Vec::new() }
    }

    fn is_var(&self, id: SymbolId) -> bool {
        self.vars.contains(&id)
    }

    /// **Reducción** (§18.2.1/§18.2.2) de `‹s → t›`: qué bounds impone que el argumento de tipo
    /// `s` tenga que ser compatible con el parámetro `t`.
    fn reduce(&mut self, table: &SymbolTable, s: &RType, t: &RType) {
        match t {
            // ‹S → α› ⇒ `S <: α`. El argumento se **boxea**: una α nunca puede ser un primitivo
            // (§4.5.1), así que de `id(1)` sale `T = Integer`.
            RType::TypeVar(v) if self.is_var(*v) => {
                self.lower.push((*v, types::boxed(table, s)));
            }
            // ‹S[] → α[]› ⇒ ‹S → α›.
            RType::Array(te) => {
                if let RType::Array(se) = s {
                    self.reduce(table, se, te);
                }
            }
            // ‹S → List<α>› ⇒ mirar `S` **como** `List<…>` y equiparar argumento a argumento.
            RType::Parameterized { base, args: targs } => {
                let seen = types::supertypes_of(table, s)
                    .into_iter()
                    .find(|x| types::erased_id(x) == Some(*base));
                if let Some(RType::Parameterized { args: sargs, .. }) = seen {
                    for (sa, ta) in sargs.iter().zip(targs) {
                        self.reduce_arg(table, sa, ta);
                    }
                }
            }
            // Un tipo concreto no aporta bounds: que el argumento encaje lo chequea la
            // aplicabilidad, no la inferencia.
            _ => {}
        }
    }

    /// Reducción de un **argumento de tipo** (§18.2.3, containment `‹S <= T›`).
    fn reduce_arg(&mut self, table: &SymbolTable, s: &RTypeArg, t: &RTypeArg) {
        match (s, t) {
            (RTypeArg::Type(st), RTypeArg::Type(tt)) => {
                // Un argumento **concreto** contra una α: containment con un tipo, o sea
                // **igualdad** — `List<String>` contra `List<α>` fuerza `α = String`, no `α :> String`.
                if let RType::TypeVar(v) = tt {
                    if self.is_var(*v) {
                        self.eq.push((*v, st.clone()));
                        return;
                    }
                }
                self.reduce(table, st, tt);
            }
            // `X` o `? extends X` contra `? extends α` ⇒ `X <: α`.
            (RTypeArg::Type(st), RTypeArg::Extends(tb)) => self.reduce(table, st, tb),
            (RTypeArg::Extends(st), RTypeArg::Extends(tb)) => self.reduce(table, st, tb),
            _ => {}
        }
    }

    /// Reducción de la restricción que impone el ***target type*** (§18.5.2): el tipo de retorno
    /// del método —donde viven las α— tiene que ser compatible con el tipo que el **contexto**
    /// espera. Es el espejo de [`reduce`](Self::reduce): allá las variables están del lado del
    /// parámetro, acá del lado de la fuente.
    fn reduce_target(&mut self, table: &SymbolTable, ret: &RType, target: &RType) {
        match ret {
            // ‹α → T› ⇒ `α <: T`. Sin otra cota, la resolución la instancia justo en `T`: es lo
            // que hace que `String s = pick();` infiera `T = String`.
            RType::TypeVar(v) if self.is_var(*v) => self.upper.push((*v, target.clone())),
            RType::Array(re) => {
                if let RType::Array(te) = target {
                    self.reduce_target(table, re, te);
                }
            }
            // ‹Caja<α> → Caja<String>› ⇒ `α = String`: los genéricos son **invariantes**, así que
            // el argumento del target fija la variable. El retorno se mira **como** el tipo del
            // target, para que `ArrayList<α>` contra un `List<String>` también funcione.
            RType::Parameterized { .. } => {
                let (Some(tbase), RType::Parameterized { args: targs, .. }) =
                    (types::erased_id(target), target)
                else {
                    return;
                };
                let seen = types::supertypes_of(table, ret)
                    .into_iter()
                    .find(|x| types::erased_id(x) == Some(tbase));
                if let Some(RType::Parameterized { args: rargs, .. }) = seen {
                    for (ra, ta) in rargs.iter().zip(targs) {
                        if let (RTypeArg::Type(RType::TypeVar(v)), RTypeArg::Type(tt)) = (ra, ta) {
                            if self.is_var(*v) {
                                self.eq.push((*v, tt.clone()));
                            }
                        }
                    }
                }
            }
            _ => {}
        }
    }

    /// **Incorporación** (§18.3), en su forma mínima: si una α quedó con una igualdad, esa manda,
    /// y las cotas inferiores que la contradigan se descartan (no las mezclamos en el `lub`).
    ///
    /// Con una excepción que importa desde que el *target type* también produce igualdades: una
    /// igualdad que **contradice** a los argumentos se tira. `<T> Caja<T> envolver(T x)` usado como
    /// `Caja<Object> c = envolver("s")` da `String <: α` y `α = Object`, que son compatibles y
    /// gana el target (§18.3 los incorpora juntos). Pero `Caja<String> c = envolver(new Object())`
    /// da `Object <: α` y `α = String`, que **no** lo son: ahí se descarta la igualdad, la
    /// inferencia queda en `Object` y el error lo reporta —bien puesto— el chequeo de asignación.
    fn incorporate(&mut self, table: &SymbolTable) {
        let lower = &self.lower;
        self.eq.retain(|(v, t)| lower.iter().all(|(x, l)| x != v || types::is_subtype(table, l, t)));
        let fixed: Vec<SymbolId> = self.eq.iter().map(|(v, _)| *v).collect();
        self.lower.retain(|(v, _)| !fixed.contains(v));
        self.upper.retain(|(v, _)| !fixed.contains(v));
    }

    /// **Resolución** (§18.4): instanciar cada variable.
    fn resolve(&self, table: &SymbolTable) -> Subst {
        let mut out: Subst = HashMap::new();
        for &v in &self.vars {
            let ty = if let Some((_, t)) = self.eq.iter().find(|(x, _)| *x == v) {
                t.clone() // una igualdad fija la variable
            } else {
                let lowers: Vec<RType> =
                    self.lower.iter().filter(|(x, _)| *x == v).map(|(_, t)| t.clone()).collect();
                if !lowers.is_empty() {
                    // Con cotas inferiores, la instanciación es su `lub` (§18.4).
                    types::lub(table, &lowers)
                } else {
                    let uppers: Vec<RType> =
                        self.upper.iter().filter(|(x, _)| *x == v).map(|(_, t)| t.clone()).collect();
                    if !uppers.is_empty() {
                        types::glb(table, &uppers)
                    } else {
                        // Sin restricciones: su cota declarada (`Object` si no declaró).
                        types::glb(table, &types::bounds_of(table, v))
                    }
                }
            };
            out.insert(v, ty);
        }
        out
    }
}

/// Infiere los argumentos de tipo de una llamada a `m` con esos `args`, sobre el receptor `recv`.
///
/// `target` es el ***target type***: el tipo que el contexto espera del resultado (§5), cuando lo
/// hay. Es lo único que puede resolver una variable que **no aparece en los argumentos** —
/// `Caja<String> c = fabricar();` no tiene de dónde sacar el `T` si no es de la izquierda.
///
/// Devuelve la sustitución `parámetro de tipo → tipo inferido`. Vacía si `m` no es genérico (no
/// hay nada que inferir). Es §18.5.2 (*invocation type inference*) en su forma practicable.
pub fn infer_call(
    table: &SymbolTable,
    recv: &RType,
    m: SymbolId,
    args: &[RType],
    target: Option<&RType>,
) -> Subst {
    let vars = method_type_params(table, m);
    if vars.is_empty() {
        return HashMap::new();
    }
    let Some(Resolved::Method { params, ret, .. }) = table.resolved(m) else {
        return HashMap::new();
    };

    // Los parámetros de tipo **de la clase** ya los fija el receptor (`List<String>` ⇒ `E=String`);
    // se sustituyen antes para que la inferencia solo vea las variables del método.
    let recv_subst = match table.symbol(m).owner {
        Some(owner) => types::subst_for(table, recv, owner),
        None => HashMap::new(),
    };

    let mut bounds = BoundSet::new(vars);
    for (p, a) in params.iter().zip(args) {
        let p = types::substitute(p, &recv_subst);
        bounds.reduce(table, a, &p);
    }
    if let Some(t) = target {
        let r = types::substitute(ret, &recv_subst);
        bounds.reduce_target(table, &r, t);
    }
    bounds.incorporate(table);
    bounds.resolve(table)
}

/// Infiere los argumentos de tipo de un **diamante** (`new Caja<>()`, §15.9.3).
///
/// Es el mismo problema que [`infer_call`] con las variables corridas de lugar: acá las α son los
/// parámetros de tipo de la **clase**, y el «tipo de retorno» contra el que se contrasta el target
/// es `Caja<α>` — la instancia que el `new` produce. Los argumentos del constructor también
/// aportan, igual que los de una llamada.
pub fn infer_diamond(
    table: &SymbolTable,
    cid: SymbolId,
    ctor: Option<SymbolId>,
    args: &[RType],
    target: Option<&RType>,
) -> Subst {
    let vars = types::type_params_of(table, cid);
    if vars.is_empty() {
        return HashMap::new();
    }
    let mut bounds = BoundSet::new(vars.clone());
    if let Some(c) = ctor {
        if let Some(Resolved::Method { params, .. }) = table.resolved(c) {
            for (p, a) in params.iter().zip(args) {
                bounds.reduce(table, a, p);
            }
        }
    }
    if let Some(t) = target {
        let made = RType::Parameterized {
            base: cid,
            args: vars.iter().map(|&v| RTypeArg::Type(RType::TypeVar(v))).collect(),
        };
        bounds.reduce_target(table, &made, t);
    }
    bounds.incorporate(table);
    bounds.resolve(table)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{enter::enter, lexer::tokenize, parser::parse};

    /// Corre la pasada 1 e infiere la llamada a `m` de la clase `C` con esos tipos de argumento,
    /// devolviendo el **tipo inferido** para su primer parámetro de tipo, como texto.
    fn infer_first(src: &str, method: &str, args_from: &[&str]) -> String {
        infer_first_to(src, method, args_from, None)
    }

    /// Ídem, pero con un ***target type*** — escrito, como los argumentos, con el nombre de un
    /// campo de `Args` del que se toma el tipo.
    fn infer_first_to(
        src: &str,
        method: &str,
        args_from: &[&str],
        target_from: Option<&str>,
    ) -> String {
        let unit = parse(tokenize(src).unwrap()).unwrap();
        let (t, errs) = enter(&unit);
        assert!(errs.is_empty(), "{errs:?}");
        let cid = t.class("C").expect("clase C");
        let m = t
            .members_of(cid)
            .into_iter()
            .find(|&x| t.symbol(x).name == method)
            .expect("el método existe");
        // Los argumentos se escriben como los tipos de los campos de `Args`.
        let aid = t.class("Args").expect("clase Args");
        let args: Vec<RType> = args_from.iter().map(|f| field_type(&t, aid, f)).collect();
        let target = target_from.map(|f| field_type(&t, aid, f));
        let subst = infer_call(&t, &RType::Class(cid), m, &args, target.as_ref());
        let vars = method_type_params(&t, m);
        let inferred = subst.get(&vars[0]).expect("se infirió el primer parámetro de tipo");
        name_of(&t, inferred)
    }

    /// El tipo de un campo de la clase `Args` — la forma de escribir un tipo cualquiera en estos
    /// tests sin tener que construirlo a mano.
    fn field_type(t: &SymbolTable, aid: SymbolId, name: &str) -> RType {
        let fid = t
            .members_of(aid)
            .into_iter()
            .find(|&x| t.symbol(x).name == name)
            .expect("el campo existe");
        match t.resolved(fid) {
            Some(Resolved::Field(ty)) => ty.clone(),
            _ => panic!("campo sin resolver"),
        }
    }

    fn name_of(t: &SymbolTable, ty: &RType) -> String {
        match ty {
            RType::Class(id) | RType::TypeVar(id) => t.symbol(*id).name.clone(),
            RType::Prim(p) => format!("{p:?}").to_lowercase(),
            RType::Parameterized { base, args } => {
                let a: Vec<String> = args
                    .iter()
                    .map(|x| match x {
                        RTypeArg::Type(t2) => name_of(t, t2),
                        _ => "?".to_string(),
                    })
                    .collect();
                format!("{}<{}>", t.symbol(*base).name, a.join(","))
            }
            RType::Array(e) => format!("{}[]", name_of(t, e)),
            other => format!("{other:?}"),
        }
    }

    #[test]
    fn infers_a_type_variable_from_the_argument() {
        // `<T> T id(T x)` con un `String` ⇒ `T = String`.
        let src = "class C { <T> T id(T x) { return x; } }
                   class Args { String s; }";
        assert_eq!(infer_first(src, "id", &["s"]), "String");
    }

    #[test]
    fn boxes_a_primitive_argument() {
        // Un argumento de tipo no puede ser primitivo (§4.5.1): de `id(1)` sale `Integer`.
        let src = "class C { <T> T id(T x) { return x; } }
                   class Args { int n; }";
        assert_eq!(infer_first(src, "id", &["n"]), "Integer");
    }

    #[test]
    fn infers_from_a_parameterized_argument() {
        // `<T> T first(Lst<T> xs)` con `Lst<String>` ⇒ `T = String` (containment ⇒ igualdad).
        let src = "class Lst<E> {}
                   class C { <T> T first(Lst<T> xs) { return null; } }
                   class Args { Lst<String> xs; }";
        assert_eq!(infer_first(src, "first", &["xs"]), "String");
    }

    #[test]
    fn infers_from_an_array_argument() {
        let src = "class C { <T> T head(T[] xs) { return null; } }
                   class Args { String[] xs; }";
        assert_eq!(infer_first(src, "head", &["xs"]), "String");
    }

    #[test]
    fn takes_the_lub_of_several_lower_bounds() {
        // `<T> T pick(T a, T b)` con `Dog` y `Cat` ⇒ `T = Animal` (§18.4: lub de las cotas).
        let src = "class Animal {} class Dog extends Animal {} class Cat extends Animal {}
                   class C { <T> T pick(T a, T b) { return a; } }
                   class Args { Dog d; Cat c; }";
        assert_eq!(infer_first(src, "pick", &["d", "c"]), "Animal");
    }

    #[test]
    fn falls_back_to_the_declared_bound_without_constraints() {
        // Sin argumentos que la restrinjan, `T` se instancia en su cota (§18.4).
        let src = "class C { <T extends Number> T zero() { return null; } }
                   class Args { int n; }";
        assert_eq!(infer_first(src, "zero", &[]), "Number");
    }

    #[test]
    fn an_unbounded_variable_falls_back_to_object() {
        let src = "class C { <T> T none() { return null; } }
                   class Args { int n; }";
        assert_eq!(infer_first(src, "none", &[]), "Object");
    }

    // ---- inferencia desde el *target type* (§18.5.2) ----

    #[test]
    fn the_target_type_instantiates_a_bare_variable() {
        // `<T> T pick()` no tiene argumentos de los que sacar `T`: sale del contexto.
        let src = "class C { <T> T pick() { return null; } }
                   class Args { String s; }";
        assert_eq!(infer_first_to(src, "pick", &[], Some("s")), "String");
    }

    #[test]
    fn the_target_type_instantiates_a_parameterized_return() {
        let src = "class Lst<E> {}
                   class C { <T> Lst<T> make() { return null; } }
                   class Args { Lst<String> xs; }";
        assert_eq!(infer_first_to(src, "make", &[], Some("xs")), "String");
    }

    #[test]
    fn the_target_type_is_seen_through_a_supertype() {
        // `SubLst<α>` contra un target `Lst<String>`: hay que mirar el retorno **como** un `Lst`.
        let src = "class Lst<E> {} class SubLst<E> extends Lst<E> {}
                   class C { <T> SubLst<T> make() { return null; } }
                   class Args { Lst<String> xs; }";
        assert_eq!(infer_first_to(src, "make", &[], Some("xs")), "String");
    }

    #[test]
    fn the_target_type_widens_what_the_argument_suggested() {
        // El argumento da `String <: α` y el target `α = Object`: son compatibles y gana el
        // target, que es lo que hace legal `Lst<Object> l = envolver("s")`.
        let src = "class Lst<E> {}
                   class C { <T> Lst<T> envolver(T x) { return null; } }
                   class Args { String s; Lst<Object> lo; }";
        assert_eq!(infer_first_to(src, "envolver", &["s"], Some("lo")), "Object");
    }

    #[test]
    fn a_target_that_contradicts_the_argument_is_dropped() {
        // `Object <: α` y `α = String` no son compatibles: se descarta la igualdad y la inferencia
        // queda en lo que dicen los argumentos, para que el error lo reporte —bien puesto— el
        // chequeo de asignación y no una inferencia inventada.
        let src = "class Lst<E> {}
                   class C { <T> Lst<T> envolver(T x) { return null; } }
                   class Args { Object o; Lst<String> ls; }";
        assert_eq!(infer_first_to(src, "envolver", &["o"], Some("ls")), "Object");
    }

    #[test]
    fn without_a_target_nothing_changes() {
        // El mismo caso sin contexto: la inferencia sigue siendo la de los argumentos.
        let src = "class Lst<E> {}
                   class C { <T> Lst<T> envolver(T x) { return null; } }
                   class Args { String s; }";
        assert_eq!(infer_first(src, "envolver", &["s"]), "String");
    }
}
