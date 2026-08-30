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
//! Los contextos que aportan target son los tres de asignación (§5.2, inicializador/`return`/`=`) y el
//! de **argumento de llamada** (`f(fabricar())`), este último por el algoritmo de **dos fases** de
//! javac (§15.12.2.6): se resuelve la sobrecarga y recién entonces se re-atribuyen los argumentos
//! *poly* (lambdas, *method refs*, diamante) con el tipo del parámetro elegido como target.
//!
//! La **incorporación** (§18.3) y la **resolución** (§18.4) son **transitivas**: se siembran las
//! cotas declaradas de cada variable (`<C extends List<T>>` ⇒ `C <: List<T>`), se cierra el conjunto
//! de bounds hasta punto fijo cruzando `S <: α` con `α <: U`, y las variables se instancian en orden
//! de dependencia sustituyendo lo ya resuelto — así una variable **sin cota directa** se deduce
//! fluyendo por la cota de otra (`first(anArrayListOfString)` con `<T, C extends List<T>> T first(C)`
//! infiere `T = String`). El containment de `? super X` también reduce (§18.2.3: acota `X` por
//! arriba), y la inferencia **detecta el `false`** de §18 (§18.5.1): igualdades incompatibles sobre
//! una misma variable, y un argumento **fuera de la cota declarada** (`m("x")` sobre
//! `<T extends Number>`, §4.5.1). [`infer_call_checked`] lo señala para que la atribución reporte el
//! error en un método propio. Las contradicciones cota-inferior/superior **derivadas del target**
//! (`Integer n = id("hola")`) se dejan **a propósito** al chequeo de asignación, que da mejor mensaje.

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

/// El conjunto de **bounds** sobre las **variables de inferencia** (§18.1). Cada variable es un `id`
/// **fresco** ([`RType::InferVar`]), no el símbolo del parámetro de tipo: así dos invocaciones —o una
/// anidada dentro de otra del mismo método— no se pisan (§18.1). Como la variable fresca no lleva sus
/// cotas declaradas, se guarda `origin` (variable → parámetro de tipo) para consultarlas, y `fresh`
/// (parámetro de tipo → su variable) para sustituirlas cuando una cota nombra a un hermano.
struct BoundSet {
    /// Las variables a inferir (ids frescos).
    vars: Vec<u32>,
    /// Variable fresca → el parámetro de tipo del que salió (para sus cotas declaradas, §18.1.3).
    origin: HashMap<u32, SymbolId>,
    /// Parámetro de tipo → su variable fresca: para reescribir las cotas declaradas (`<C extends
    /// List<T>>` ⇒ la cota de `C` menciona la variable de `T`, no el `T` declarado).
    fresh: Subst,
    /// `T <: α` — cotas **inferiores**: la α tiene que poder contener a `T`.
    lower: Vec<(u32, RType)>,
    /// `α <: T` — cotas **superiores**.
    upper: Vec<(u32, RType)>,
    /// `α = T` — igualdades (las que produce el containment de un argumento concreto).
    eq: Vec<(u32, RType)>,
    /// Una restricción que se redujo a **false** de entrada, sin pasar por las cotas.
    ///
    /// No todo lo insatisfacible aparece como dos cotas que se contradicen: `int[] → α[]` es falso
    /// por lo que **es**, no por lo que implica. Marcarlo acá es la forma de que
    /// [`BoundSet::hard_false`] lo vea (finding #290).
    falso: bool,
}

impl BoundSet {
    fn new(vars: Vec<u32>, origin: HashMap<u32, SymbolId>, fresh: Subst) -> Self {
        BoundSet {
            vars,
            origin,
            fresh,
            lower: Vec::new(),
            upper: Vec::new(),
            eq: Vec::new(),
            falso: false,
        }
    }

    fn is_var(&self, id: u32) -> bool {
        self.vars.contains(&id)
    }

    /// Las cotas **declaradas** de una variable fresca, con los hermanos ya reescritos a sus variables
    /// (`<C extends List<T>>` → la cota de `C` es `List<InferVar_T>`).
    fn declared_bounds(&self, table: &SymbolTable, v: u32) -> Vec<RType> {
        match self.origin.get(&v) {
            Some(&orig) => types::bounds_of(table, orig)
                .iter()
                .map(|b| types::substitute(b, &self.fresh))
                .collect(),
            None => Vec::new(),
        }
    }

    /// **Reducción** (§18.2.1/§18.2.2) de `‹s → t›`: qué bounds impone que el argumento de tipo
    /// `s` tenga que ser compatible con el parámetro `t`.
    fn reduce(&mut self, table: &SymbolTable, s: &RType, t: &RType) {
        // Un argumento **sin resolver** (`?`) no aporta ninguna cota: es lenient, como en el resto del
        // compilador. Sin esto, el array de un varargs ya bajado por el desugar (`new T[]{…}` con `T`
        // todavía sin instanciar, o sea `?[]`) reducía `? <: T` y volvía la inferencia insatisfacible
        // en la **re-atribución** posterior al desugar —aunque la primera atribución ya había fijado `T`—.
        if matches!(s, RType::Unresolved) {
            return;
        }
        match t {
            // ‹S → α› ⇒ `S <: α`. El argumento se **boxea**: una α nunca puede ser un primitivo
            // (§4.5.1), así que de `id(1)` sale `T = Integer`.
            RType::InferVar(v) if self.is_var(*v) => {
                self.lower.push((*v, types::boxed(table, s)));
            }
            // ‹S[] → α[]› ⇒ ‹S → α›, **pero solo si `S` es un tipo referencia**.
            //
            // Con `S` primitivo la restricción es **falsa**, y hay que decirlo acá: si se dejara
            // recursar, el caso ‹S → α› de arriba **boxea**, y de `int[] → α[]` saldría
            // `T = Integer`. Eso está bien para un argumento suelto —`id(1)` sí da `T = Integer`—
            // y está mal dentro de un arreglo: `int[]` no es `Integer[]` ni ningún `T[]`, porque
            // una variable de tipo solo liga tipos referencia (§4.5.1).
            //
            // Lo que producía: `Arrays.copyOf(int[], int)` resolvía al genérico `<T> T[]
            // copyOf(T[], int)` en vez de al de `int[]`, emitía la llamada a la sobrecarga de
            // `Object[]` y le ponía un `checkcast [Ljava/lang/Integer;` encima. Compilaba, y
            // reventaba en el primer `aastore` sobre lo que en realidad era un `int[]`
            // (finding #290).
            RType::Array(te) => {
                if let RType::Array(se) = s {
                    let destino_es_var = matches!(**te, RType::InferVar(v) if self.is_var(v));
                    if destino_es_var && matches!(**se, RType::Prim(_)) {
                        self.falso = true;
                        return;
                    }
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

    /// Reducción de un **argumento de tipo** (§18.2.3, containment `‹S <= T›`): `s` es el argumento
    /// del tipo del **argumento** de la llamada, `t` el del **parámetro** (donde viven las α).
    fn reduce_arg(&mut self, table: &SymbolTable, s: &RTypeArg, t: &RTypeArg) {
        match (s, t) {
            (RTypeArg::Type(st), RTypeArg::Type(tt)) => {
                // Un argumento **concreto** contra una α: containment con un tipo, o sea
                // **igualdad** — `List<String>` contra `List<α>` fuerza `α = String`, no `α :> String`.
                if let RType::InferVar(v) = tt {
                    if self.is_var(*v) {
                        self.eq.push((*v, st.clone()));
                        return;
                    }
                }
                self.reduce(table, st, tt);
            }
            // `T = ? extends T'`: `X` o `? extends X` contra `? extends α` ⇒ `X <: α`.
            (RTypeArg::Type(st), RTypeArg::Extends(tb)) => self.reduce(table, st, tb),
            (RTypeArg::Extends(st), RTypeArg::Extends(tb)) => self.reduce(table, st, tb),
            // `?` (unbounded) contra `? extends α` ⇒ `Object <: α` (§18.2.3): el argumento no dice nada
            // más específico que `Object`.
            (RTypeArg::Wildcard, RTypeArg::Extends(tb)) => {
                if let Some(obj) = table.external("Object") {
                    self.reduce(table, &RType::Class(obj), tb);
                }
            }
            // `? super S'` contra `? extends α` ⇒ `Object = α` (§18.2.3): las dos cotas solo coinciden
            // en `Object`.
            (RTypeArg::Super(_), RTypeArg::Extends(tb)) => {
                if let (Some(obj), RType::InferVar(v)) = (table.external("Object"), &**tb) {
                    if self.is_var(*v) {
                        self.eq.push((*v, RType::Class(obj)));
                    }
                }
            }
            // `T = ? super T'` (§18.2.3): un tipo `A` o un `? super A` del lado del argumento reduce a
            // `T' <: A` — el `? super` **acota por arriba** a la variable de `T'` (`List<? super X>` con
            // un `List<Number>` da `X <: Number`). Con `? extends`/`?` del lado del argumento sería
            // `false`, que no modelamos: se deja caer (lo reporta la aplicabilidad).
            (RTypeArg::Type(sa), RTypeArg::Super(tb)) => self.reduce_target(table, tb, sa),
            (RTypeArg::Super(sa), RTypeArg::Super(tb)) => self.reduce_target(table, tb, sa),
            // Argumento **wildcard** contra un parámetro de tipo **invariante** `α` (§5.1.10, capture
            // conversion del argumento): pasar `Box<? extends X>` a `Box<α>` **captura** —el argumento
            // se convierte a `Box<CAP>` con `CAP <: X`, y la invariancia fuerza `α = CAP`—. Una `CAP`
            // fresca es un tipo **distinto** de todo tipo nombrable: si el *target* además fija `α = T`
            // concreto, son igualdades incompatibles y la llamada se rechaza (como javac). La cota de la
            // captura se toma **concreta** (la declarada de `X` cuando `X` es una variable de inferencia,
            // típicamente `Object`), para que la igualdad sea *proper* y el conflicto se detecte. Solo
            // alcanza a llamadas **anidadas**: los wildcards de un argumento normal ya se capturaron en el
            // sitio de uso (§5.1.10 en [`types::capture`]).
            (RTypeArg::Extends(inner), RTypeArg::Type(RType::InferVar(v))) if self.is_var(*v) => {
                let upper = self.capture_bound(table, inner);
                let cap = RType::Capture { id: table.fresh_capture_id(), upper: Box::new(upper), lower: None };
                self.eq.push((*v, cap));
            }
            (RTypeArg::Super(inner), RTypeArg::Type(RType::InferVar(v))) if self.is_var(*v) => {
                let obj = table.external("Object").map(RType::Class).unwrap_or(RType::Unresolved);
                let lower = self.capture_bound(table, inner);
                let cap = RType::Capture {
                    id: table.fresh_capture_id(),
                    upper: Box::new(obj),
                    lower: Some(Box::new(lower)),
                };
                self.eq.push((*v, cap));
            }
            _ => {}
        }
    }

    /// La cota **concreta** de un lado de wildcard para modelar una captura (§5.1.10): si es una
    /// variable de inferencia, su cota declarada (`glb` de las cotas, o `Object`); si ya es un tipo
    /// propio, él mismo. Concreta a propósito: una captura con cota que menciona una α no sería
    /// *proper* y [`satisfiable`](Self::satisfiable) la ignoraría.
    fn capture_bound(&self, table: &SymbolTable, inner: &RType) -> RType {
        match inner {
            RType::InferVar(id) if self.is_var(*id) => {
                let db = self.declared_bounds(table, *id);
                if db.is_empty() {
                    table.external("Object").map(RType::Class).unwrap_or(RType::Unresolved)
                } else {
                    types::glb(table, &db)
                }
            }
            other => other.clone(),
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
            RType::InferVar(v) if self.is_var(*v) => self.upper.push((*v, target.clone())),
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
                        self.reduce_target_arg(table, ra, ta);
                    }
                }
            }
            _ => {}
        }
    }

    /// Containment de un **argumento de tipo del retorno** contra el del *target* (§18.5.2.1 →
    /// §18.3.2, «bounds involving capture conversion»). El JLS, cuando el retorno `Rθ` es `G<A…>`
    /// con algún `Ai` wildcard, introduce variables de captura frescas `β` y el bound
    /// `G<β> = capture(G<A…>)`, reduce `‹G<β> → target›` y **incorpora** la captura. Acá se colapsa
    /// esa cadena a la cota directa sobre la variable que vive dentro del wildcard del retorno,
    /// porque `β` es puramente intermedia:
    ///
    /// - `G<α> → G<T>` (ambos **invariantes**): `α = T` — el caso de siempre.
    /// - `G<? extends inner> → G<? extends X>`: por containment `β <: X`, y la incorporación de la
    ///   captura (caso c, cota declarada `Object`) da `inner <: X`. Sin esto, un método que devuelve
    ///   `G<? extends T>` no infería nada de su wildcard y `T` caía a `Object` → rechazo espurio.
    /// - `G<? super inner> → G<? super X>`: por containment `X <: β`, y la captura (caso d) da
    ///   `X <: inner` — una **cota inferior** para la variable.
    ///
    /// Los cruces restantes (target invariante contra un wildcard del retorno, o `? super` contra
    /// `? extends`) son `false` en el JLS; no aportan cota y los rechaza —bien— el chequeo de
    /// asignación sobre el retorno ya resuelto.
    fn reduce_target_arg(&mut self, table: &SymbolTable, ra: &RTypeArg, ta: &RTypeArg) {
        match (ra, ta) {
            (RTypeArg::Type(RType::InferVar(v)), RTypeArg::Type(tt)) if self.is_var(*v) => {
                self.eq.push((*v, tt.clone()));
            }
            (RTypeArg::Extends(inner), RTypeArg::Extends(x)) => self.reduce_target(table, inner, x),
            (RTypeArg::Super(inner), RTypeArg::Super(x)) => self.reduce(table, x, inner),
            _ => {}
        }
    }

    /// Reduce una restricción **derivada** `s <: u` (de la incorporación): produce bounds del lado
    /// que tenga variables — si `u` las tiene, por [`reduce`](Self::reduce) (variables del lado del
    /// «parámetro»); si `s` las tiene, por [`reduce_target`](Self::reduce_target) (variables del lado
    /// del «sub»). Con ambos lados **propios** (sin variables) no aporta nada.
    fn reduce_subtype(&mut self, table: &SymbolTable, s: &RType, u: &RType) {
        self.reduce(table, s, u);
        self.reduce_target(table, s, u);
    }

    /// Descarta bounds **exactamente** duplicados: la incorporación reintroduce los mismos una y otra
    /// vez, y sin esto el punto fijo nunca se detectaría.
    fn dedup(&mut self) {
        for v in [&mut self.lower, &mut self.upper, &mut self.eq] {
            let mut out: Vec<(u32, RType)> = Vec::new();
            for b in v.drain(..) {
                if !out.contains(&b) {
                    out.push(b);
                }
            }
            *v = out;
        }
    }

    /// **Incorporación** (§18.3): cierre **transitivo** de los bounds hasta punto fijo, y después la
    /// poda de igualdades contradictorias.
    ///
    /// Primero se **siembran las cotas declaradas** de cada variable (§18.1.3): `<C extends List<T>>`
    /// aporta `C <: List<T>`, que es lo único que enlaza `C` con `T`. Después se cierran las reglas
    /// cruzadas: de `S <: α` y `α <: U` sale `S <: U` —que se **reduce**, y puede fijar otra variable:
    /// así `ArrayList<String> <: C <: List<T>` deduce `T = String`—, una igualdad se propaga a las
    /// cotas de su variable, y una cota `α <: β` entre dos variables encadena las cotas de una en la
    /// otra.
    ///
    /// Al final, la poda (desde que el *target type* también produce igualdades): una igualdad que
    /// **contradice** a una cota inferior se tira. `<T> Caja<T> envolver(T x)` usado como
    /// `Caja<Object> c = envolver("s")` da `String <: α` y `α = Object`, compatibles, y gana el
    /// target; pero `Caja<String> c = envolver(new Object())` da `Object <: α` y `α = String`, que
    /// **no** lo son: se descarta la igualdad, la inferencia queda en `Object` y el error lo reporta
    /// —bien puesto— el chequeo de asignación.
    fn incorporate(&mut self, table: &SymbolTable, prune: bool) {
        // §18.1.3 — sembrar las cotas declaradas de cada variable como `α <: B` (con los hermanos ya
        // reescritos a sus variables frescas).
        for v in self.vars.clone() {
            for b in self.declared_bounds(table, v) {
                self.upper.push((v, b));
            }
        }
        self.dedup();
        // §18.3 — cierre transitivo hasta punto fijo (con tope de seguridad).
        for _ in 0..64 {
            let before = self.lower.len() + self.upper.len() + self.eq.len();
            // `S <: α` & `α <: U`  ⟹  `S <: U`.  Con `S` un tipo **propio** (`ArrayList<String>`)
            // esto es lo que fija otra variable (`… <: C <: List<T>` ⟹ `T = String`). Pero si `S` es
            // **otra variable de inferencia pelada** (`β <: α <: U`), NO se le hereda `U` como cota
            // superior: una `β` así —típicamente la variable de una llamada anidada que aparece bajo un
            // wildcard del retorno (`Box<? extends β>`)— se resuelve por **sus propias** cotas, no por
            // el contexto externo que la usa (§18.4: la resolución de una variable sin cota inferior va
            // a su cota **declarada**; el target no la baja por debajo). La propagación var-var legítima
            // la hace el encadenamiento `α <: β` de más abajo, cuando hay un enlace explícito. Sin esto
            // `take(make())` con `make(): Box<? extends T>` aceptaba lo que javac rechaza (`T` caía a
            // `Number` heredado del target en vez de a `Object`).
            for (v, s) in self.lower.clone() {
                if matches!(&s, RType::InferVar(id) if self.is_var(*id)) {
                    continue;
                }
                for (v2, u) in self.upper.clone() {
                    if v == v2 {
                        self.reduce_subtype(table, &s, &u);
                    }
                }
            }
            // Una igualdad `α = S` se propaga a las cotas de `α`.
            for (v, seq) in self.eq.clone() {
                for (v2, u) in self.upper.clone() {
                    if v == v2 {
                        self.reduce_subtype(table, &seq, &u);
                    }
                }
                for (v2, l) in self.lower.clone() {
                    if v == v2 {
                        self.reduce_subtype(table, &l, &seq);
                    }
                }
            }
            // Dos igualdades sobre la **misma** variable igualan sus valores: `α = A` & `α = B` ⟹
            // `A = B`. Importa cuando un valor es **otra variable** (`T = β` de un argumento anidado y
            // `T = String` de otro ⟹ `β = String`): así se resuelve `pick(empty(), strs())` (§18.5.2.1).
            let eqs = self.eq.clone();
            for (i, (v, a)) in eqs.iter().enumerate() {
                for (v2, b) in eqs.iter().skip(i + 1) {
                    if v == v2 {
                        self.reduce_subtype(table, a, b);
                        self.reduce_subtype(table, b, a);
                    }
                }
            }
            // `α <: β` entre dos variables: encadenar. `S <: α` ⟹ `S <: β`; `β <: U` ⟹ `α <: U`.
            for (v, u) in self.upper.clone() {
                let RType::InferVar(b) = u else { continue };
                if !self.is_var(b) {
                    continue;
                }
                for (v2, s) in self.lower.clone() {
                    if v == v2 {
                        self.lower.push((b, s));
                    }
                }
                for (v2, u2) in self.upper.clone() {
                    if b == v2 {
                        self.upper.push((v, u2));
                    }
                }
            }
            self.dedup();
            if self.lower.len() + self.upper.len() + self.eq.len() == before {
                break;
            }
        }
        // Poda: una igualdad manda sobre las cotas inferiores que la contradicen (el *target-type*
        // gana sobre los argumentos, §18.3). Se **omite** en el modo aplicabilidad (§18.5.1): ahí la
        // contradicción **no** se descarta, se detecta como `false` (la llamada no es aplicable).
        if prune {
            let lower = &self.lower;
            self.eq
                .retain(|(v, t)| lower.iter().all(|(x, l)| x != v || types::is_subtype(table, l, t)));
            let fixed: Vec<u32> = self.eq.iter().map(|(v, _)| *v).collect();
            self.lower.retain(|(v, _)| !fixed.contains(v));
            self.upper.retain(|(v, _)| !fixed.contains(v));
        }
    }

    /// **Resolución** (§18.4): instanciar las variables **en orden de dependencia**. Una variable
    /// cuyas cotas mencionan otra **sin instanciar** espera; al instanciar una, su valor se
    /// **sustituye** en las cotas de las demás (así `C = ArrayList<String>` deja `C <: List<String>`
    /// y de ahí `T` ya no queda pendiente). Un ciclo (todas se mencionan) se fuerza junto.
    ///
    /// Cada variable se instancia con su igualdad si la tiene; si no, con el `lub` de sus cotas
    /// inferiores; si no, con el `glb` de las superiores (incluida la cota declarada, ya sembrada).
    fn resolve(&self, table: &SymbolTable) -> HashMap<u32, RType> {
        let mut inst: HashMap<u32, RType> = HashMap::new();
        let mut lower = self.lower.clone();
        let mut upper = self.upper.clone();
        let mut eq = self.eq.clone();
        let mut pending = self.vars.clone();
        while !pending.is_empty() {
            // Sustituir lo ya instanciado en todas las cotas.
            for (_, t) in lower.iter_mut().chain(upper.iter_mut()).chain(eq.iter_mut()) {
                *t = subst_infer(t, &inst);
            }
            // Variables «listas»: ninguna de sus cotas menciona otra variable pendiente.
            let touches = |bs: &[(u32, RType)], v: u32, pend: &[u32]| {
                bs.iter().any(|(x, t)| *x == v && mentions(t, pend))
            };
            let ready: Vec<u32> = pending
                .iter()
                .cloned()
                .filter(|&v| {
                    !(touches(&lower, v, &pending)
                        || touches(&upper, v, &pending)
                        || touches(&eq, v, &pending))
                })
                .collect();
            // Sin ninguna lista hay un ciclo entre variables: se fuerzan todas las pendientes.
            let batch = if ready.is_empty() { pending.clone() } else { ready };
            for &v in &batch {
                let ty = if let Some((_, t)) = eq.iter().find(|(x, _)| *x == v) {
                    t.clone()
                } else {
                    let lowers: Vec<RType> =
                        lower.iter().filter(|(x, _)| *x == v).map(|(_, t)| t.clone()).collect();
                    if !lowers.is_empty() {
                        types::lub(table, &lowers)
                    } else {
                        let uppers: Vec<RType> =
                            upper.iter().filter(|(x, _)| *x == v).map(|(_, t)| t.clone()).collect();
                        if !uppers.is_empty() {
                            types::glb(table, &uppers)
                        } else {
                            types::glb(table, &self.declared_bounds(table, v))
                        }
                    }
                };
                inst.insert(v, ty);
            }
            pending.retain(|v| !batch.contains(v));
        }
        inst
    }

    /// ¿El conjunto de bounds es **satisfacible** (§18.5.1)? Detecta el `false` de §18 en dos formas,
    /// siempre entre cotas **propias** (sin variables sin instanciar, para no confundir un enlace
    /// pendiente con una contradicción):
    ///
    /// 1. Dos **igualdades incompatibles** sobre la misma variable (`α = A` y `α = B`, `A ≠ B`).
    /// 2. Una cota **inferior** (o una igualdad) que **no cae bajo la cota declarada** del parámetro de
    ///    tipo: `S <: α` con `S ⊄ B` (`B` la cota de `<α extends B>`) es un argumento **fuera de la
    ///    cota** (§4.5.1/§18.3), que la aplicabilidad —indulgente con la variable— deja pasar.
    ///
    /// **No** se chequea contra las cotas superiores **derivadas del target/argumentos**: esa clase de
    /// contradicción (`Integer n = id("hola")`, donde el target impone `T <: Integer` y el argumento
    /// `String <: T`) la reporta el **chequeo de asignación**, con mejor mensaje — reportarla también
    /// acá la duplicaría.
    fn satisfiable(&self, table: &SymbolTable) -> bool {
        let proper = |t: &RType| !mentions(t, &self.vars);
        // (1) Igualdades incompatibles.
        for (i, (v, a)) in self.eq.iter().enumerate() {
            for (w, b) in self.eq.iter().skip(i + 1) {
                if v == w
                    && proper(a)
                    && proper(b)
                    && !(types::is_subtype(table, a, b) && types::is_subtype(table, b, a))
                {
                    return false;
                }
            }
        }
        // (2) Argumento fuera de la **cota declarada** de la variable.
        for &v in &self.vars {
            for bound in self.declared_bounds(table, v) {
                if !proper(&bound) {
                    continue;
                }
                let violates = |bs: &[(u32, RType)]| {
                    bs.iter()
                        .any(|(x, t)| *x == v && proper(t) && !types::is_subtype(table, t, &bound))
                };
                if violates(&self.lower) || violates(&self.eq) {
                    return false;
                }
            }
        }
        true
    }

    /// ¿El conjunto de bounds (que hay que incorporar **sin podar**) tiene un `false` **duro** de §18
    /// que hace la llamada **inaplicable** (§18.5.1)? Detecta las contradicciones **entre argumentos**:
    ///
    /// - dos **igualdades** incompatibles sobre la misma variable;
    /// - una igualdad `α = E` con una cota **inferior** `S <: α` que **no cae bajo** `E` — no existe
    ///   `T` que satisfaga los dos (`m(T, List<T>)` con `(List<String>, List<String>)`: el 2º arg fija
    ///   `T = String`, el 1º pide `List<String> <: T`, y `List<String> ⊄ String`).
    ///
    /// A diferencia de [`satisfiable`](Self::satisfiable), **no** mira la cota declarada (eso lo reporta
    /// el sitio de la llamada, no la aplicabilidad) ni el *target* (§18.5.1 usa solo los argumentos).
    fn hard_false(&self, table: &SymbolTable) -> bool {
        // Una restricción que ya se redujo a false no necesita que las cotas se contradigan.
        if self.falso {
            return true;
        }
        let proper = |t: &RType| !mentions(t, &self.vars);
        for (i, (v, a)) in self.eq.iter().enumerate() {
            for (w, b) in self.eq.iter().skip(i + 1) {
                if v == w
                    && proper(a)
                    && proper(b)
                    && !(types::is_subtype(table, a, b) && types::is_subtype(table, b, a))
                {
                    return true;
                }
            }
        }
        for (v, e) in &self.eq {
            if proper(e)
                && self
                    .lower
                    .iter()
                    .any(|(x, l)| x == v && proper(l) && !types::is_subtype(table, l, e))
            {
                return true;
            }
        }
        false
    }
}

/// ¿El tipo `ty` **menciona** alguna de las variables de `set` (directa o anidada)? Lo usa la
/// resolución para respetar el orden de dependencia entre variables de inferencia.
fn mentions(ty: &RType, set: &[u32]) -> bool {
    match ty {
        RType::InferVar(v) => set.contains(v),
        RType::Array(e) => mentions(e, set),
        RType::Parameterized { args, .. } => args.iter().any(|a| mentions_arg(a, set)),
        RType::Capture { upper, lower, .. } => {
            mentions(upper, set) || lower.as_deref().is_some_and(|l| mentions(l, set))
        }
        RType::Intersection(ms) => ms.iter().any(|m| mentions(m, set)),
        _ => false,
    }
}

fn mentions_arg(a: &RTypeArg, set: &[u32]) -> bool {
    match a {
        RTypeArg::Type(t) => mentions(t, set),
        RTypeArg::Extends(t) | RTypeArg::Super(t) => mentions(t, set),
        RTypeArg::Wildcard => false,
    }
}

/// Sustituye cada [`RType::InferVar`] por su valor en `inst` (los que no estén, quedan). Es el análogo
/// de [`types::substitute`] pero sobre variables de inferencia (por `id`), no sobre parámetros de tipo.
fn subst_infer(ty: &RType, inst: &HashMap<u32, RType>) -> RType {
    match ty {
        RType::InferVar(id) => inst.get(id).cloned().unwrap_or_else(|| ty.clone()),
        RType::Array(e) => RType::Array(Box::new(subst_infer(e, inst))),
        RType::Parameterized { base, args } => RType::Parameterized {
            base: *base,
            args: args.iter().map(|a| subst_infer_arg(a, inst)).collect(),
        },
        RType::Capture { id, upper, lower } => RType::Capture {
            id: *id,
            upper: Box::new(subst_infer(upper, inst)),
            lower: lower.as_ref().map(|l| Box::new(subst_infer(l, inst))),
        },
        RType::Intersection(ms) => RType::Intersection(ms.iter().map(|m| subst_infer(m, inst)).collect()),
        _ => ty.clone(),
    }
}

fn subst_infer_arg(a: &RTypeArg, inst: &HashMap<u32, RType>) -> RTypeArg {
    match a {
        RTypeArg::Type(t) => RTypeArg::Type(subst_infer(t, inst)),
        RTypeArg::Extends(t) => RTypeArg::Extends(Box::new(subst_infer(t, inst))),
        RTypeArg::Super(t) => RTypeArg::Super(Box::new(subst_infer(t, inst))),
        RTypeArg::Wildcard => RTypeArg::Wildcard,
    }
}

/// Reemplaza cualquier [`RType::InferVar`] que haya quedado sin resolver por `Object` (o `Unresolved`):
/// una variable de inferencia **no puede escapar** del módulo. Sólo pasa en un ciclo entre variables
/// que la resolución tuvo que forzar; el resultado igual no debe llevar la variable.
fn scrub_infer(table: &SymbolTable, ty: &RType) -> RType {
    let obj = table.external("Object").map_or(RType::Unresolved, RType::Class);
    fn go(ty: &RType, obj: &RType) -> RType {
        match ty {
            RType::InferVar(_) => obj.clone(),
            RType::Array(e) => RType::Array(Box::new(go(e, obj))),
            RType::Parameterized { base, args } => RType::Parameterized {
                base: *base,
                args: args
                    .iter()
                    .map(|a| match a {
                        RTypeArg::Type(t) => RTypeArg::Type(go(t, obj)),
                        RTypeArg::Extends(t) => RTypeArg::Extends(Box::new(go(t, obj))),
                        RTypeArg::Super(t) => RTypeArg::Super(Box::new(go(t, obj))),
                        RTypeArg::Wildcard => RTypeArg::Wildcard,
                    })
                    .collect(),
            },
            RType::Intersection(ms) => RType::Intersection(ms.iter().map(|m| go(m, obj)).collect()),
            other => other.clone(),
        }
    }
    go(ty, &obj)
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
    infer_call_checked(table, recv, m, args, target).0
}

/// Como [`infer_call`], pero además dice si el conjunto de constraints es **satisfacible** (§18.5.1):
/// el `bool` es `false` cuando hay un `false` de §18 —hoy, dos **igualdades incompatibles** sobre una
/// misma variable (`α = String` y `α = Integer`, de `m(List<String>, List<Integer>)` sobre
/// `<T> m(List<T>, List<T>)`)—, lo que hace la llamada un **error de inferencia**. La sustitución que
/// devuelve sigue siendo la de **mejor esfuerzo** (para no propagar `Unresolved`); es el `bool` el que
/// marca que hubo contradicción.
pub fn infer_call_checked(
    table: &SymbolTable,
    recv: &RType,
    m: SymbolId,
    args: &[RType],
    target: Option<&RType>,
) -> (Subst, bool) {
    let (subst, _, ok) = infer_call_nested(table, recv, m, args, &[], target);
    (subst, ok)
}

/// **§18.5.1 — aplicabilidad por inferencia.** ¿Existe alguna instanciación de las variables de tipo de
/// `m` que satisfaga los **argumentos**? Reduce los argumentos (sin el *target type*) y devuelve `false`
/// si el conjunto de bounds tiene un `false` **duro** ([`BoundSet::hard_false`]). Un método **no
/// genérico** siempre pasa (no hay nada que inferir). Es lo que hace que `m(T, List<T>)` **no** sea
/// aplicable a `(List<String>, List<String>)` —`T` no puede ser `String` y contener a `List<String>`—,
/// dejando la sobrecarga a otro candidato como en javac.
pub fn applicable_by_inference(
    table: &SymbolTable,
    recv: &RType,
    m: SymbolId,
    args: &[RType],
) -> bool {
    let vars_orig = method_type_params(table, m);
    if vars_orig.is_empty() {
        return true;
    }
    let Some(Resolved::Method { params, .. }) = table.resolved(m) else {
        return true;
    };
    let recv_subst = match table.symbol(m).owner {
        Some(owner) => types::subst_for(table, recv, owner),
        None => HashMap::new(),
    };
    let (fresh_ids, fresh, origin) = fresh_vars(table, &vars_orig);
    let mut bounds = BoundSet::new(fresh_ids, origin, fresh.clone());
    for (p, a) in params.iter().zip(args) {
        let p = types::substitute(&types::substitute(p, &recv_subst), &fresh);
        bounds.reduce(table, a, &p);
    }
    bounds.incorporate(table, false); // sin podar: la contradicción sobrevive para detectarla
    !bounds.hard_false(table)
}

/// Una llamada a método **genérico anidada** como argumento de otra (`pick(empty(), …)`): su
/// resolución se **difiere** para resolverse **junto** con la externa (§18.5.2.1). `arg_index` es su
/// posición en la llamada externa; `recv` el receptor de la anidada (para fijar sus parámetros de clase).
pub struct NestedCall {
    pub arg_index: usize,
    pub method: SymbolId,
    pub recv: RType,
}

/// El resultado por cada llamada anidada: la sustitución de **sus** parámetros de tipo, para re-tipar
/// su nodo una vez resuelto el sistema combinado.
pub struct NestedResult {
    pub arg_index: usize,
    pub subst: Subst,
}

/// [`infer_call_checked`] con **solving combinado** (§18.5.2.1): las llamadas genéricas **anidadas** en
/// `nested` no se resuelven por separado —lo que perdía su contexto (`empty()` daba `Box<Object>`)—;
/// sus variables de inferencia entran en el **mismo** conjunto de constraints que las de `m`, con el
/// **retorno** de cada una (con sus variables frescas) como tipo de ese argumento. Al resolver todo
/// junto, `pick(empty(), strs())` deduce `E = String` de que `strs()` es `Box<String>`.
///
/// Devuelve la sustitución de `m`, la de **cada** anidada (para re-tipar su nodo), y si es satisfacible.
pub fn infer_call_nested(
    table: &SymbolTable,
    recv: &RType,
    m: SymbolId,
    arg_types: &[RType],
    nested: &[NestedCall],
    target: Option<&RType>,
) -> (Subst, Vec<NestedResult>, bool) {
    let vars_orig = method_type_params(table, m);
    if vars_orig.is_empty() && nested.is_empty() {
        return (HashMap::new(), Vec::new(), true);
    }
    let Some(Resolved::Method { params, ret, varargs, .. }) = table.resolved(m) else {
        return (HashMap::new(), Vec::new(), true);
    };
    let recv_subst = match table.symbol(m).owner {
        Some(owner) => types::subst_for(table, recv, owner),
        None => HashMap::new(),
    };

    // Variables frescas de `m`, y los mapas **combinados** (se les suman los de cada anidada).
    let (m_ids, mut fresh, mut origin) = fresh_vars(table, &vars_orig);
    let mut all_ids = m_ids.clone();
    // Por cada anidada: sus variables frescas + su retorno reescrito con ellas.
    let mut nested_info: Vec<(usize, Vec<SymbolId>, Vec<u32>, RType)> = Vec::new();
    for nc in nested {
        let inner_params = method_type_params(table, nc.method);
        if inner_params.is_empty() {
            continue;
        }
        let (i_ids, i_fresh, i_origin) = fresh_vars(table, &inner_params);
        fresh.extend(i_fresh.iter().map(|(k, v)| (*k, v.clone())));
        origin.extend(i_origin);
        all_ids.extend(i_ids.iter().copied());
        // El retorno de la anidada con sus parámetros de clase fijados por su receptor, y sus
        // parámetros de tipo reemplazados por las variables frescas.
        let inner_ret = match table.resolved(nc.method) {
            Some(Resolved::Method { ret, .. }) => {
                let irecv = match table.symbol(nc.method).owner {
                    Some(o) => types::subst_for(table, &nc.recv, o),
                    None => HashMap::new(),
                };
                types::substitute(&types::substitute(&ret, &irecv), &i_fresh)
            }
            _ => RType::Unresolved,
        };
        nested_info.push((nc.arg_index, inner_params, i_ids, inner_ret));
    }

    let mut bounds = BoundSet::new(all_ids, origin, fresh.clone());
    // Índice del último parámetro (el de `...` en un método de aridad variable) y su tipo de
    // **elemento** (`T[]` → `T`): un argumento del *spread* restringe la variable de tipo contra el
    // elemento, no contra el array. Sin esto, `<T> count(T... xs)` llamado `count("a","b")` reducía
    // `String <: T[]` (insatisfacible) en vez de `String <: T` (⇒ `T = String`).
    let last = params.len().saturating_sub(1);
    for (i, a) in arg_types.iter().enumerate() {
        // Para los argumentos del *spread* (índice ≥ último parámetro) de un método varargs, se usa el
        // tipo de elemento —salvo que el argumento sea **él mismo** un array (invocación de aridad fija,
        // p. ej. pasar el `T[]` ya armado), donde se conserva el parámetro array.
        let p = if *varargs && i >= last && !matches!(a, RType::Array(_)) {
            match params.get(last) {
                Some(RType::Array(elem)) => Some(elem.as_ref()),
                other => other,
            }
        } else {
            params.get(i)
        };
        let Some(p) = p else { continue };
        let p = types::substitute(&types::substitute(p, &recv_subst), &fresh);
        // Si este argumento es una llamada anidada, se usa su **retorno** (con variables frescas), no
        // el tipo concreto que resolvió aislada.
        let arg_ty = nested_info
            .iter()
            .find(|(idx, ..)| *idx == i)
            .map(|(.., r)| r.clone())
            .unwrap_or_else(|| a.clone());
        bounds.reduce(table, &arg_ty, &p);
    }
    if let Some(t) = target {
        let r = types::substitute(&types::substitute(ret, &recv_subst), &fresh);
        bounds.reduce_target(table, &r, t);
    }
    bounds.incorporate(table, true);
    let ok = bounds.satisfiable(table);
    let resolved = bounds.resolve(table);

    let back = |params: &[SymbolId], ids: &[u32]| -> Subst {
        let mut s: Subst = HashMap::new();
        for (orig, fid) in params.iter().zip(ids) {
            if let Some(ty) = resolved.get(fid) {
                s.insert(*orig, scrub_infer(table, ty));
            }
        }
        s
    };
    let out = back(&vars_orig, &m_ids);
    let nres = nested_info
        .iter()
        .map(|(arg_index, inner_params, i_ids, _)| NestedResult {
            arg_index: *arg_index,
            subst: back(inner_params, i_ids),
        })
        .collect();
    (out, nres, ok)
}

/// Crea una **variable de inferencia fresca** (§18.1) por cada parámetro de tipo: devuelve sus `id`s,
/// el mapa `parámetro → InferVar` (para reescribir firmas/cotas) y el inverso `InferVar → parámetro`
/// (para las cotas declaradas).
fn fresh_vars(table: &SymbolTable, params: &[SymbolId]) -> (Vec<u32>, Subst, HashMap<u32, SymbolId>) {
    let mut ids = Vec::with_capacity(params.len());
    let mut fresh: Subst = HashMap::new();
    let mut origin: HashMap<u32, SymbolId> = HashMap::new();
    for &p in params {
        let id = table.fresh_infer_id();
        ids.push(id);
        fresh.insert(p, RType::InferVar(id));
        origin.insert(id, p);
    }
    (ids, fresh, origin)
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
    let vars_orig = types::type_params_of(table, cid);
    if vars_orig.is_empty() {
        return HashMap::new();
    }
    let (fresh_ids, fresh, origin) = fresh_vars(table, &vars_orig);
    let mut bounds = BoundSet::new(fresh_ids.clone(), origin, fresh.clone());
    if let Some(c) = ctor {
        if let Some(Resolved::Method { params, .. }) = table.resolved(c) {
            for (p, a) in params.iter().zip(args) {
                let p = types::substitute(p, &fresh);
                bounds.reduce(table, a, &p);
            }
        }
    }
    if let Some(t) = target {
        // La instancia que produce el `new`: `Caja<InferVar…>`.
        let made = RType::Parameterized {
            base: cid,
            args: fresh_ids.iter().map(|&id| RTypeArg::Type(RType::InferVar(id))).collect(),
        };
        bounds.reduce_target(table, &made, t);
    }
    bounds.incorporate(table, true);
    let resolved = bounds.resolve(table);
    let mut out: Subst = HashMap::new();
    for (orig, fid) in vars_orig.iter().zip(&fresh_ids) {
        if let Some(ty) = resolved.get(fid) {
            out.insert(*orig, scrub_infer(table, ty));
        }
    }
    out
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
        let unit = parse(tokenize(src).unwrap()).0;
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

    /// Corre la inferencia de la llamada a `m` y devuelve si el conjunto de constraints es
    /// **satisfacible** (§18.5.1) — el flag de [`infer_call_checked`].
    fn infer_ok(src: &str, method: &str, args_from: &[&str]) -> bool {
        let unit = parse(tokenize(src).unwrap()).0;
        let (t, errs) = enter(&unit);
        assert!(errs.is_empty(), "{errs:?}");
        let cid = t.class("C").expect("clase C");
        let m = t
            .members_of(cid)
            .into_iter()
            .find(|&x| t.symbol(x).name == method)
            .expect("el método existe");
        let aid = t.class("Args").expect("clase Args");
        let args: Vec<RType> = args_from.iter().map(|f| field_type(&t, aid, f)).collect();
        infer_call_checked(&t, &RType::Class(cid), m, &args, None).1
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
    fn infers_from_a_super_bounded_wildcard_parameter() {
        // `<T> T pick(Lst<? super T> l)` con un `Lst<Animal>`: el `? super T` **acota por arriba** a
        // `T` (`T <: Animal`, §18.2.3), y sin cota inferior la resolución instancia en esa cota → `T =
        // Animal`. Antes el caso `Super` de `reduce_arg` se caía y `T` quedaba en `Object`.
        let src = "class Lst<E> {} class Animal {}
                   class C { <T> T pick(Lst<T> l) { return null; }
                             <T> T sup(Lst<? super T> l) { return null; } }
                   class Args { Lst<Animal> l; }";
        assert_eq!(infer_first(src, "sup", &["l"]), "Animal");
    }

    #[test]
    fn infers_a_variable_through_another_variables_bound() {
        // `<T, C extends Lst<T>> T first(C c)` con `Sub` (un `Lst<String>`): `T` **no** tiene cota
        // directa —solo `C`—, y sale fluyendo por la cota `C extends Lst<T>` con `C = Sub` (§18.3/§18.4,
        // incorporación transitiva + resolución por dependencia). Antes caía a `Object`.
        let src = "class Lst<E> {} class Sub extends Lst<String> {}
                   class C { <T, L extends Lst<T>> T first(L c) { return null; } }
                   class Args { Sub s; }";
        assert_eq!(infer_first(src, "first", &["s"]), "String");
    }

    #[test]
    fn contradictory_equalities_are_unsatisfiable() {
        // `<T> void m(Lst<T> a, Lst<T> b)` con `Lst<Foo>` y `Lst<Bar>`: fuerza `T = Foo` **y**
        // `T = Bar`, incompatibles (§18 `false`) → la inferencia **no es satisfacible**. Antes se
        // tomaba una arbitraria y la llamada se aceptaba en silencio.
        let src = "class Lst<E> {} class Foo {} class Bar {}
                   class C { <T> void m(Lst<T> a, Lst<T> b) {} }
                   class Args { Lst<Foo> f; Lst<Bar> b; }";
        assert!(!infer_ok(src, "m", &["f", "b"]));
    }

    #[test]
    fn compatible_equalities_are_satisfiable() {
        // El mismo método con dos `Lst<Foo>`: `T = Foo` dos veces, **compatible**.
        let src = "class Lst<E> {} class Foo {}
                   class C { <T> void m(Lst<T> a, Lst<T> b) {} }
                   class Args { Lst<Foo> f; Lst<Foo> g; }";
        assert!(infer_ok(src, "m", &["f", "g"]));
    }

    #[test]
    fn an_argument_outside_the_declared_bound_is_unsatisfiable() {
        // `<T extends Num> T m(T x)` con un `Other` (que **no** es `Num`): `Other <: T <: Num` es
        // `false` (§18.3). La aplicabilidad, indulgente con `T`, lo dejaba pasar.
        let src = "class Num {} class Other {}
                   class C { <T extends Num> T m(T x) { return x; } }
                   class Args { Other o; }";
        assert!(!infer_ok(src, "m", &["o"]));
    }

    #[test]
    fn an_argument_within_the_declared_bound_is_satisfiable() {
        // Un subtipo de la cota (`SubNum <: Num`) sí encaja.
        let src = "class Num {} class SubNum extends Num {}
                   class C { <T extends Num> T m(T x) { return x; } }
                   class Args { SubNum s; }";
        assert!(infer_ok(src, "m", &["s"]));
    }

    #[test]
    fn nested_generic_call_is_solved_with_the_outer() {
        // `<T> Box<T> pick(Box<T> a, Box<T> b)` con `empty()` (`<E> Box<E>`) y un `Box<String>`: el
        // solving combinado (§18.5.2.1) deduce `E = String` de que el otro argumento es `Box<String>`,
        // y por lo tanto `T = String`. Aislada, `empty()` habría dado `Box<Object>`.
        let src = "class Box<E> {}
                   class C { <E> Box<E> empty() { return null; }
                             <T> Box<T> pick(Box<T> a, Box<T> b) { return null; } }
                   class Args { Box<String> bs; }";
        let unit = parse(tokenize(src).unwrap()).0;
        let (t, errs) = enter(&unit);
        assert!(errs.is_empty(), "{errs:?}");
        let cid = t.class("C").unwrap();
        let find =
            |n: &str| t.members_of(cid).into_iter().find(|&x| t.symbol(x).name == n).unwrap();
        let (pick, empty) = (find("pick"), find("empty"));
        let box_string = field_type(&t, t.class("Args").unwrap(), "bs");
        // arg0 = empty() (anidada, su tipo se ignora), arg1 = Box<String>.
        let arg_types = vec![RType::Unresolved, box_string];
        let nested = vec![NestedCall { arg_index: 0, method: empty, recv: RType::Class(cid) }];
        let (out, nres, ok) =
            infer_call_nested(&t, &RType::Class(cid), pick, &arg_types, &nested, None);
        assert!(ok, "satisfacible");
        let pick_t = method_type_params(&t, pick)[0];
        assert_eq!(name_of(&t, out.get(&pick_t).unwrap()), "String", "T de pick");
        let empty_e = method_type_params(&t, empty)[0];
        assert_eq!(nres.len(), 1);
        assert_eq!(name_of(&t, nres[0].subst.get(&empty_e).unwrap()), "String", "E de empty");
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
