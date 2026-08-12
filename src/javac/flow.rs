//! La pasada **Flow** (B4): análisis de **flujo de datos** sobre el AST ya decorado. Cubre las
//! reglas del **Cap. 16** de la JLS (asignación definitiva de variables) y la **§14.21**
//! (alcanzabilidad), entrelazadas porque comparten el recorrido.
//!
//! ## Las dos propiedades del Cap. 16
//!
//! Por cada punto del programa se arrastran **dos** conjuntos de slots (§16.1):
//!
//! - **DA** — *definitely assigned*: en **todos** los caminos la variable ya fue asignada. Una
//!   lectura de un local que no está en DA es error (*"puede no haber sido inicializada"*).
//! - **DU** — *definitely unassigned*: en **todos** los caminos la variable **no** fue asignada
//!   todavía. Asignar a una variable `final` solo es legal si está en DU (si no, ya podría estar
//!   asignada → error).
//!
//! No son complementarias: una variable puede no estar en **ninguno** de los dos (el estado
//! *ambiguo* — asignada en algunos caminos y en otros no). Por eso DU se lleva **aparte**, no se
//! deduce de DA. En este módulo ambos viven en un [`Flow`] que se threadea igual; solo difieren
//! en cómo los tocan la **declaración** y la **asignación**.
//!
//! ## Alcanzabilidad (§14.21)
//!
//! Una sentencia que no puede ejecutarse (tras `return`/`break`, el cuerpo de un `while(false)`,
//! código después de un bucle infinito…) es error. El estado es `Option<Flow>`: `None` = punto
//! inalcanzable.
//!
//! No usa la tabla de símbolos: se apoya solo en la decoración de la pasada 2 (cada `Name` local
//! trae su `Binding::Local { slot }`, cada declaración su slot). Una variable se identifica por su
//! **slot**; el recorrido sigue el anidamiento léxico y cada declaración re-inicializa su slot,
//! así que los slots reusados entre bloques hermanos no se confunden.
//!
//! Cola larga: la asignación definitiva en **inicializadores de campo** y bloques `static`; el
//! refinamiento por condiciones constantes en `if` (§16 lo permite; acá se sigue §14.21); y la
//! detección de reasignación de una `final` **dentro de un bucle** (`while(c) x = 1;`) — se es
//! **indulgente** ahí a propósito, para no rechazar código válido como `while(c){ x=1; break; }`.

use std::collections::HashSet;

use super::ast::{
    AssignOp, Binding, Block, CaseLabel, CatchClause, ClassDecl, CompilationUnit, Expr, ExprKind,
    Member, MethodDecl, Modifier, PrimType, Pos, Stmt, StmtKind, SwitchBody, Type, UnOp,
};
use super::Error;

type Set = HashSet<u16>;

/// El estado de flujo de un punto: los slots **definitivamente asignados** (`da`) y los
/// **definitivamente sin asignar** (`du`). Un slot puede no estar en ninguno (ambiguo).
#[derive(Clone)]
struct Flow {
    da: Set,
    du: Set,
}

/// `Some(flow)` si el punto es **alcanzable**; `None` si el control no puede llegar.
type State = Option<Flow>;

/// El resultado de una expresión **booleana** (§16.1): el flujo si evalúa a `true` y si evalúa a
/// `false`. Para una no booleana, ambos coinciden con el "después".
struct Cond {
    t: Flow,
    f: Flow,
}

pub fn flow(unit: &CompilationUnit) -> Vec<Error> {
    let mut a = Analyzer { errors: Vec::new(), breaks: Vec::new(), finals: Set::new() };
    for ty in &unit.types {
        a.class(ty);
    }
    a.errors
}

struct Analyzer {
    errors: Vec<Error>,
    /// Pila de contextos que capturan un `break` (bucles, `switch` y sentencias etiquetadas). Cada
    /// contexto lleva la etiqueta que lo nombra (`None` = destino de un `break` **sin** etiqueta).
    breaks: Vec<BreakCtx>,
    /// Slots que son variables `final` **en alcance** — se actualiza en cada declaración.
    finals: Set,
}

/// Un contexto de captura de `break` en la pila: la etiqueta que lo nombra (si alguna) y los estados
/// de flujo que llegan por cada `break` que le apunta.
struct BreakCtx {
    label: Option<String>,
    states: Vec<Flow>,
}

impl BreakCtx {
    /// El contexto de un bucle/`switch`: destino de los `break` **sin** etiqueta (la etiqueta, si la
    /// sentencia la tiene, la aporta el contexto de la sentencia `Labeled` que la envuelve).
    fn unlabeled() -> Self {
        BreakCtx { label: None, states: Vec::new() }
    }
}

// ---- helpers de conjuntos y de flujo ----

fn intersect(a: &Set, b: &Set) -> Set {
    a.iter().filter(|x| b.contains(x)).copied().collect()
}
/// El *meet* de dos flujos (unión de caminos): **ambos** conjuntos intersecan — un slot es DA (o
/// DU) tras la unión solo si lo es por los dos caminos.
fn meet(a: &Flow, b: &Flow) -> Flow {
    Flow { da: intersect(&a.da, &b.da), du: intersect(&a.du, &b.du) }
}
fn both(f: Flow) -> Cond {
    Cond { t: f.clone(), f }
}
/// El *meet* de varios flujos, o `None` si no hay ninguno (ningún camino llega).
fn meet_all(flows: Vec<Flow>) -> State {
    let mut it = flows.into_iter();
    let first = it.next()?;
    Some(it.fold(first, |acc, f| meet(&acc, &f)))
}
/// Une dos caminos: alcanzable si **alguno** lo es.
fn merge(a: State, b: State) -> State {
    match (a, b) {
        (Some(x), Some(y)) => Some(meet(&x, &y)),
        (Some(x), None) | (None, Some(x)) => Some(x),
        (None, None) => None,
    }
}

fn const_bool(e: &Expr) -> Option<bool> {
    match &e.kind {
        ExprKind::BoolLit(b) => Some(*b),
        ExprKind::Unary { op: UnOp::Not, expr, .. } => const_bool(expr).map(|b| !b),
        _ => None,
    }
}

/// El slot de un local usado como **lectura** o **destino** (`Name` con binding a local), con su
/// nombre para el mensaje.
fn local_slot(e: &Expr) -> Option<(u16, &str)> {
    match (&e.kind, e.binding) {
        (ExprKind::Name(n), Some(Binding::Local { slot })) => Some((slot, n)),
        _ => None,
    }
}

/// Los slots de **todas** las variables de un patrón, bajando por la deconstrucción de un `record`.
fn pattern_slots(p: &super::ast::Pattern) -> Vec<u16> {
    match p {
        super::ast::Pattern::Type { slot, .. } => slot.iter().copied().collect(),
        super::ast::Pattern::Record { components, .. } => {
            components.iter().flat_map(pattern_slots).collect()
        }
    }
}

fn slot_width(ty: &Type) -> u16 {
    matches!(ty, Type::Prim(PrimType::Long | PrimType::Double)) as u16 + 1
}

impl Analyzer {
    fn error(&mut self, pos: Pos, message: impl Into<String>) {
        self.errors.push(Error::new(message, pos.line, pos.col));
    }
    fn uninit(&mut self, pos: Pos, name: &str) {
        self.error(pos, format!("la variable `{name}` puede no haber sido inicializada"));
    }

    fn class(&mut self, class: &ClassDecl) {
        for member in &class.members {
            match member {
                Member::Method(m) => self.method(m),
                Member::Type(nested) => self.class(nested),
                // Un inicializador estático fluye como un cuerpo `static` sin params (todo sin asignar).
                Member::StaticInit(b) => {
                    self.finals = Set::new();
                    self.block(Flow { da: Set::new(), du: Set::new() }, b);
                }
                // El de instancia corre dentro del constructor: `this` (slot 0) ya está asignado.
                Member::InstanceInit(b) => {
                    self.finals = Set::new();
                    let mut da = Set::new();
                    da.insert(0);
                    self.block(Flow { da, du: Set::new() }, b);
                }
                Member::Field(_) => {}
            }
        }
    }

    fn method(&mut self, m: &MethodDecl) {
        let Some(body) = &m.body else { return };
        // Estado inicial: `this` (slot 0, en no-`static`) y los parámetros están **asignados** —
        // o sea, en DA y **no** en DU. Los slots reinician por método (los números se repiten).
        self.finals = Set::new();
        let mut da = Set::new();
        let mut slot = 0u16;
        if !m.modifiers.contains(&Modifier::Static) {
            da.insert(0);
            slot = 1;
        }
        for p in &m.params {
            da.insert(slot);
            if p.is_final {
                self.finals.insert(slot);
            }
            slot += slot_width(&p.ty);
        }
        self.block(Flow { da, du: Set::new() }, body);
    }

    // ---- sentencias ----

    fn block(&mut self, entry: Flow, block: &Block) -> State {
        let mut state: State = Some(entry);
        for s in &block.0 {
            match state {
                Some(flow) => state = self.stmt(flow, s),
                None => {
                    self.error(s.pos, "sentencia inalcanzable");
                    return None;
                }
            }
        }
        state
    }

    fn stmt(&mut self, flow: Flow, s: &Stmt) -> State {
        match &s.kind {
            StmtKind::LocalVar { init, is_final, .. } => {
                let mut f = match init {
                    Some(e) => self.expr_after(&flow, e),
                    None => flow,
                };
                if let Some(l) = &s.local {
                    if init.is_some() {
                        f.da.insert(l.slot);
                        f.du.remove(&l.slot);
                    } else {
                        // En blanco: definitivamente **sin** asignar.
                        f.da.remove(&l.slot);
                        f.du.insert(l.slot);
                    }
                    if *is_final {
                        self.finals.insert(l.slot);
                    } else {
                        self.finals.remove(&l.slot);
                    }
                }
                Some(f)
            }
            StmtKind::Expr(e) => Some(self.expr_after(&flow, e)),
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.expr_after(&flow, e);
                }
                None
            }
            StmtKind::Throw(e) => {
                self.expr_after(&flow, e);
                None
            }
            StmtKind::Break(label) => {
                // Sin etiqueta va al contexto más interno; con etiqueta, al más cercano que la lleve
                // (que es el de la sentencia `Labeled`, no el del bucle/switch en sí).
                let target = match label {
                    Some(l) => self.breaks.iter_mut().rev().find(|c| c.label.as_deref() == Some(l.as_str())),
                    None => self.breaks.last_mut(),
                };
                if let Some(ctx) = target {
                    ctx.states.push(flow);
                }
                None
            }
            // `continue` (con o sin etiqueta) salta al encabezado del bucle: no aporta salida normal,
            // y con el modelo de 0 iteraciones no realimenta el estado — como antes, corta el camino.
            StmtKind::Continue(_) => None,
            StmtKind::Labeled { label, body } => {
                // La etiqueta puede ir sobre cualquier sentencia; un `break label` sale de ella.
                self.breaks.push(BreakCtx { label: Some(label.clone()), states: Vec::new() });
                let body_state = self.stmt(flow, body);
                let ctx = self.breaks.pop().unwrap();
                merge(body_state, meet_all(ctx.states))
            }
            StmtKind::Empty => Some(flow),
            // Declarar una clase local no ejecuta nada: completa normalmente y no toca el flujo. La
            // regla de captura *effectively final* de sus métodos es de cuando se compile.
            StmtKind::LocalClass(_) => Some(flow),
            StmtKind::Block(b) => self.block(flow, b),
            StmtKind::Synchronized { lock, body } => {
                let f = self.expr_after(&flow, lock);
                self.block(f, body)
            }
            StmtKind::Assert { cond, message } => {
                self.expr_cond(&flow, cond); // un `assert` puede estar deshabilitado: no asigna
                if let Some(m) = message {
                    self.expr_after(&flow, m);
                }
                Some(flow)
            }
            StmtKind::Yield(e) => {
                self.expr_after(&flow, e);
                None
            }
            StmtKind::If { cond, then, els } => {
                let c = self.expr_cond(&flow, cond);
                let then_state = self.stmt(c.t, then);
                let else_state = match els {
                    Some(e) => self.stmt(c.f, e),
                    None => Some(c.f),
                };
                merge(then_state, else_state)
            }
            StmtKind::While { cond, body } => self.while_loop(flow, cond, body),
            StmtKind::Do { body, cond } => self.do_loop(flow, body, cond),
            StmtKind::For { init, cond, update, body } => self.for_loop(flow, init, cond, update, body),
            StmtKind::ForEach { iterable, body, is_final, .. } => {
                self.for_each(flow, iterable, body, s, *is_final)
            }
            StmtKind::Switch { selector, cases } => self.switch(flow, selector, cases),
            StmtKind::Try { resources, body, catches, finally } => {
                self.try_stmt(flow, resources, body, catches, finally.as_ref())
            }
        }
    }

    fn while_loop(&mut self, flow: Flow, cond: &Expr, body: &Stmt) -> State {
        if const_bool(cond) == Some(false) {
            self.error(body.pos, "sentencia inalcanzable");
            return Some(flow);
        }
        let infinite = const_bool(cond) == Some(true);
        let c = self.expr_cond(&flow, cond);
        self.breaks.push(BreakCtx::unlabeled());
        // El cuerpo arranca con "when true"; no se realimentan sus asignaciones (modelo de 0
        // iteraciones para DA, e indulgente para las `final` dentro del bucle).
        self.stmt(c.t, body);
        let breaks = self.breaks.pop().unwrap().states;
        if infinite {
            meet_all(breaks) // sin salida normal: solo por `break`
        } else {
            Some(breaks.iter().fold(c.f, |acc, b| meet(&acc, b)))
        }
    }

    fn do_loop(&mut self, flow: Flow, body: &Stmt, cond: &Expr) -> State {
        self.breaks.push(BreakCtx::unlabeled());
        let body_state = self.stmt(flow, body); // corre al menos una vez
        let breaks = self.breaks.pop().unwrap().states;
        let normal = match body_state {
            // Como el cuerpo ya corrió, sus asignaciones cuentan (estado post-cuerpo, preciso).
            Some(bs) if const_bool(cond) != Some(true) => Some(self.expr_cond(&bs, cond).f),
            _ => None,
        };
        merge(normal, meet_all(breaks))
    }

    fn for_loop(
        &mut self,
        flow: Flow,
        init: &Option<Box<Stmt>>,
        cond: &Option<Expr>,
        update: &[Expr],
        body: &Stmt,
    ) -> State {
        let flow = match init {
            Some(i) => match self.stmt(flow, i) {
                Some(f) => f,
                None => return None,
            },
            None => flow,
        };
        if cond.as_ref().is_some_and(|c| const_bool(c) == Some(false)) {
            self.error(body.pos, "sentencia inalcanzable");
            return Some(flow);
        }
        let infinite = match cond {
            None => true,
            Some(c) => const_bool(c) == Some(true),
        };
        let c = match cond {
            Some(c) => self.expr_cond(&flow, c),
            None => both(flow.clone()),
        };
        self.breaks.push(BreakCtx::unlabeled());
        let body_state = self.stmt(c.t.clone(), body);
        let upd_flow = body_state.unwrap_or(c.t);
        for u in update {
            self.expr_after(&upd_flow, u);
        }
        let breaks = self.breaks.pop().unwrap().states;
        if infinite {
            meet_all(breaks)
        } else {
            Some(breaks.iter().fold(c.f, |acc, b| meet(&acc, b)))
        }
    }

    fn for_each(&mut self, flow: Flow, iterable: &Expr, body: &Stmt, stmt: &Stmt, is_final: bool) -> State {
        let f = self.expr_after(&flow, iterable);
        let mut body_in = f.clone();
        if let Some(l) = &stmt.local {
            body_in.da.insert(l.slot); // la variable del for-each está asignada en el cuerpo
            body_in.du.remove(&l.slot);
            if is_final {
                self.finals.insert(l.slot);
            } else {
                self.finals.remove(&l.slot);
            }
        }
        self.breaks.push(BreakCtx::unlabeled());
        self.stmt(body_in, body);
        let breaks = self.breaks.pop().unwrap().states;
        Some(breaks.iter().fold(f, |acc, b| meet(&acc, b))) // puede iterar 0 veces
    }

    fn switch(&mut self, flow: Flow, selector: &Expr, cases: &[super::ast::SwitchCase]) -> State {
        let f = self.expr_after(&flow, selector);
        self.breaks.push(BreakCtx::unlabeled());
        let mut fall: State = None; // estado que **cae** desde el case anterior
        let mut has_default = false;
        let mut ends: Vec<State> = Vec::new();
        for c in cases {
            if c.is_default {
                has_default = true;
            }
            // A un case se llega por salto directo (`f`) o por caída: la entrada es su meet.
            let mut entry = match &fall {
                Some(x) => meet(&f, x),
                None => f.clone(),
            };
            // Las **variables de patrón** del case están siempre asignadas dentro del brazo (la
            // vincula el propio match), igual que la variable de un `catch`. Valen en la guarda.
            for l in &c.labels {
                if let CaseLabel::Pattern(p) = l {
                    for s in pattern_slots(p) {
                        entry.da.insert(s);
                        entry.du.remove(&s);
                    }
                }
            }
            if let Some(g) = &c.guard {
                self.expr_cond(&entry, g);
            }
            let end = match &c.body {
                SwitchBody::Arrow(st) => self.stmt(entry, st),
                SwitchBody::Colon(sts) => {
                    let mut cur = Some(entry);
                    for st in sts {
                        match cur {
                            Some(cs) => cur = self.stmt(cs, st),
                            None => break,
                        }
                    }
                    cur
                }
            };
            fall = end.clone();
            ends.push(end);
        }
        let breaks = self.breaks.pop().unwrap().states;
        let mut paths: Vec<State> = breaks.into_iter().map(Some).collect();
        if let Some(last) = ends.last() {
            paths.push(last.clone());
        }
        if !has_default {
            paths.push(Some(f)); // el selector podría no matchear nada
        }
        paths.into_iter().fold(None, merge)
    }

    fn try_stmt(
        &mut self,
        flow: Flow,
        resources: &[Stmt],
        body: &Block,
        catches: &[CatchClause],
        finally: Option<&Block>,
    ) -> State {
        let mut res_flow = flow.clone();
        for r in resources {
            res_flow = self.stmt(res_flow, r).unwrap_or_else(|| flow.clone());
        }
        let body_state = self.block(res_flow, body);
        // Cada `catch` se entra desde el estado **previo al try** (la excepción puede saltar antes
        // de que el cuerpo asigne nada). La variable del catch está siempre asignada.
        let mut paths = vec![body_state];
        for c in catches {
            let mut cin = flow.clone();
            if let Some(slot) = c.slot {
                cin.da.insert(slot);
                cin.du.remove(&slot);
                if c.is_final {
                    self.finals.insert(slot);
                } else {
                    self.finals.remove(&slot);
                }
            }
            paths.push(self.block(cin, &c.body));
        }
        let try_state = paths.into_iter().fold(None, merge);
        match finally {
            None => try_state,
            Some(fin_block) => {
                // El `finally` corre en **todos** los caminos, incluida una excepción temprana:
                // se analiza desde el estado previo al try. Si no completa normalmente, domina.
                match self.block(flow.clone(), fin_block) {
                    None => None,
                    Some(fin) => {
                        // Lo que el finally asigna se suma a lo del try.
                        let assigned: Set = fin.da.difference(&flow.da).copied().collect();
                        try_state.map(|t| Flow {
                            da: t.da.union(&assigned).copied().collect(),
                            du: intersect(&t.du, &fin.du),
                        })
                    }
                }
            }
        }
    }

    // ---- expresiones ----

    fn expr_after(&mut self, flow: &Flow, e: &Expr) -> Flow {
        let c = self.expr_cond(flow, e);
        meet(&c.t, &c.f)
    }

    fn expr_cond(&mut self, flow: &Flow, e: &Expr) -> Cond {
        use super::ast::BinOp;
        match &e.kind {
            // --- flujos booleanos (§16.1) ---
            ExprKind::Binary { op: BinOp::And, lhs, rhs } => {
                let ca = self.expr_cond(flow, lhs);
                let cb = self.expr_cond(&ca.t, rhs); // el rhs solo si el lhs fue true
                Cond { t: cb.t, f: meet(&ca.f, &cb.f) }
            }
            ExprKind::Binary { op: BinOp::Or, lhs, rhs } => {
                let ca = self.expr_cond(flow, lhs);
                let cb = self.expr_cond(&ca.f, rhs); // el rhs solo si el lhs fue false
                Cond { t: meet(&ca.t, &cb.t), f: cb.f }
            }
            ExprKind::Unary { op: UnOp::Not, expr, .. } => {
                let c = self.expr_cond(flow, expr);
                Cond { t: c.f, f: c.t }
            }
            ExprKind::Ternary { cond, then, els } => {
                let cc = self.expr_cond(flow, cond);
                let ct = self.expr_cond(&cc.t, then);
                let ce = self.expr_cond(&cc.f, els);
                Cond { t: meet(&ct.t, &ce.t), f: meet(&ct.f, &ce.f) }
            }
            // --- asignación ---
            ExprKind::Assign { op, target, value } => match local_slot(target) {
                Some((slot, name)) => {
                    // `+=` también **lee** el destino (debe estar asignado).
                    if *op != AssignOp::Assign && !flow.da.contains(&slot) {
                        self.uninit(target.pos, name);
                    }
                    let mut f = self.expr_after(flow, value);
                    self.check_final_assign(slot, name, &f, target.pos);
                    f.da.insert(slot);
                    f.du.remove(&slot);
                    both(f)
                }
                None => {
                    let f1 = self.expr_after(flow, target);
                    both(self.expr_after(&f1, value))
                }
            },
            ExprKind::Unary { op: UnOp::Inc | UnOp::Dec, expr, .. } => match local_slot(expr) {
                Some((slot, name)) => {
                    if !flow.da.contains(&slot) {
                        self.uninit(expr.pos, name);
                    }
                    self.check_final_assign(slot, name, flow, expr.pos);
                    let mut f = flow.clone();
                    f.da.insert(slot);
                    f.du.remove(&slot);
                    both(f)
                }
                None => both(self.expr_after(flow, expr)),
            },
            // --- lectura de un local ---
            ExprKind::Name(name) => {
                if let Some(Binding::Local { slot }) = e.binding {
                    if !flow.da.contains(&slot) {
                        self.uninit(e.pos, name);
                    }
                }
                both(flow.clone())
            }
            // --- resto: hilar las subexpresiones de izquierda a derecha ---
            ExprKind::Binary { lhs, rhs, .. } => {
                let f1 = self.expr_after(flow, lhs);
                both(self.expr_after(&f1, rhs))
            }
            ExprKind::Unary { expr, .. } => both(self.expr_after(flow, expr)),
            ExprKind::Call { target, args, .. } => {
                let mut f = flow.clone();
                if let Some(t) = target {
                    f = self.expr_after(&f, t);
                }
                for a in args {
                    f = self.expr_after(&f, a);
                }
                both(f)
            }
            ExprKind::Field { expr, .. } => both(self.expr_after(flow, expr)),
            ExprKind::Index { array, index } => {
                let f1 = self.expr_after(flow, array);
                both(self.expr_after(&f1, index))
            }
            ExprKind::Cast { expr, .. } | ExprKind::InstanceOf { expr, .. } => {
                both(self.expr_after(flow, expr))
            }
            // Un literal de clase no lee ninguna variable: el flujo pasa igual.
            ExprKind::ClassLit(_) => both(flow.clone()),
            ExprKind::NewObject { args, .. } => {
                let mut f = flow.clone();
                for a in args {
                    f = self.expr_after(&f, a);
                }
                both(f)
            }
            ExprKind::NewArray { dims, init, .. } => {
                let mut f = flow.clone();
                for d in dims.iter().flatten() {
                    f = self.expr_after(&f, d);
                }
                if let Some(es) = init {
                    for e in es {
                        f = self.expr_after(&f, e);
                    }
                }
                both(f)
            }
            ExprKind::Switch { selector, cases } => {
                let f = self.expr_after(flow, selector);
                for c in cases {
                    if let Some(g) = &c.guard {
                        self.expr_cond(&f, g);
                    }
                    match &c.body {
                        SwitchBody::Arrow(st) => {
                            self.stmt(f.clone(), st);
                        }
                        SwitchBody::Colon(sts) => {
                            let mut cur = Some(f.clone());
                            for st in sts {
                                match cur {
                                    Some(cs) => cur = self.stmt(cs, st),
                                    None => break,
                                }
                            }
                        }
                    }
                }
                both(f)
            }
            ExprKind::IntLit(_)
            | ExprKind::LongLit(_)
            | ExprKind::FloatLit(_)
            | ExprKind::DoubleLit(_)
            | ExprKind::CharLit(_)
            | ExprKind::StringLit(_)
            | ExprKind::BoolLit(_)
            | ExprKind::Null
            | ExprKind::This
            | ExprKind::QualifiedThis(_)
            | ExprKind::Super
            // Un nodo de error no aporta al flujo: ya se reportó su error de sintaxis.
            | ExprKind::Error => both(flow.clone()),
            // El cuerpo de una **lambda** se ejecuta **diferido**, no acá: no aporta al flujo del
            // método que la construye (§16.1 no lo atraviesa). La regla que sí falta —que las
            // variables **capturadas** sean *effectively final*— es de cuando la lambda se compile
            // de verdad; el emisor la corta antes con su barrera.
            ExprKind::Lambda { .. } => both(flow.clone()),
            // Una referencia a método `obj::m` **sí** evalúa su qualifier (§15.13.3), así que en
            // rigor debería atravesar el flujo — pero attribute todavía no decora ese nodo, y como no
            // se compila (la barrera del emisor la corta), no se desciende, igual que la lambda. Se
            // completará cuando la referencia se resuelva de verdad.
            ExprKind::MethodRef { .. } => both(flow.clone()),
            // El desugar (posterior a esta pasada) es quien produce un `Indy`: acá nunca aparece.
            ExprKind::Indy { .. } => both(flow.clone()),
        }
    }

    /// Chequea una **asignación a un local `final`**: solo es legal si está *definitely
    /// unassigned* (§16.2). `after` es el flujo tras evaluar el valor.
    fn check_final_assign(&mut self, slot: u16, name: &str, after: &Flow, pos: Pos) {
        if !self.finals.contains(&slot) || after.du.contains(&slot) {
            return; // no es final, o está garantizadamente sin asignar → legal
        }
        if after.da.contains(&slot) {
            self.error(pos, format!("no se puede asignar a la variable `final` `{name}`: ya está asignada"));
        } else {
            self.error(pos, format!("la variable `final` `{name}` ya podría haber sido asignada"));
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{attribute::attribute, enter::enter, lexer::tokenize, parser::parse};

    fn flow_errs(src: &str) -> Vec<Error> {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (table, _e1) = enter(&unit);
        let _e2 = attribute(&mut unit, &table);
        flow(&unit)
    }

    fn in_method(body: &str) -> String {
        format!("class C {{ void m(int p) {{ {body} }} }}")
    }

    // ---- asignación definitiva (DA) ----

    #[test]
    fn reads_before_assignment_is_an_error() {
        let errs = flow_errs(&in_method("int x; int y = x;"));
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("inicializada"));
    }

    #[test]
    fn assign_then_read_is_fine() {
        assert!(flow_errs(&in_method("int x; x = 1; int y = x;")).is_empty());
    }

    #[test]
    fn parameters_are_assigned() {
        assert!(flow_errs(&in_method("int y = p;")).is_empty());
    }

    #[test]
    fn both_if_branches_assign_then_read_ok() {
        assert!(flow_errs(&in_method("int x; if (p > 0) x = 1; else x = 2; int y = x;")).is_empty());
    }

    #[test]
    fn only_one_if_branch_assigns_is_an_error() {
        assert_eq!(flow_errs(&in_method("int x; if (p > 0) x = 1; int y = x;")).len(), 1);
    }

    #[test]
    fn the_other_branch_returning_makes_it_assigned() {
        assert!(flow_errs(&in_method("int x; if (p > 0) x = 1; else return; int y = x;")).is_empty());
    }

    #[test]
    fn while_may_run_zero_times() {
        assert_eq!(flow_errs(&in_method("int x; while (p > 0) x = 1; int y = x;")).len(), 1);
    }

    #[test]
    fn labeled_break_paths_join_for_definite_assignment() {
        // `x` se asigna en el camino del `break L` (x=1) y en el de caída (x=2): DA tras `L`.
        assert!(flow_errs(
            &in_method("int x; L: { if (p > 0) { x = 1; break L; } x = 2; } int y = x;")
        )
        .is_empty());
    }

    #[test]
    fn labeled_break_skipping_the_assignment_is_not_definite() {
        // El `break L` salta el `x = 1`: en ese camino `x` queda sin asignar → lectura insegura.
        assert_eq!(
            flow_errs(&in_method("int x; L: { if (p > 0) break L; x = 1; } int y = x;")).len(),
            1
        );
    }

    #[test]
    fn labeled_break_from_a_nested_loop_stays_reachable() {
        // `break outer` sale de los dos bucles sin dejar código "inalcanzable" espurio.
        assert!(flow_errs(&in_method(
            "outer: while (p > 0) { while (p > 1) { break outer; } } int y = 1;"
        ))
        .is_empty());
    }

    #[test]
    fn a_class_literal_types_and_resolves_methods_on_it() {
        // Lo que destraba el guard `$assertionsDisabled`: `C.class` tipa como `Class`, y sobre él
        // resuelve un método real de `java.lang.Class`.
        let errs = flow_errs(
            "class C { boolean f() { return C.class.desiredAssertionStatus(); } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn pattern_variable_is_bound_in_arm_and_guard() {
        // `case String s when …` bindea `s` tanto en la guarda como en el cuerpo, y flow la ve
        // definitivamente asignada (la vincula el propio match).
        let errs = flow_errs(
            "class C { void m(Object o) { switch (o) { \
             case String s when s.length() > 0 -> { int n = s.length(); } \
             default -> {} } } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn static_init_block_is_flow_analyzed() {
        // Un `static { }` se analiza como un cuerpo estático: leer una local sin asignar es error.
        assert_eq!(flow_errs("class C { static { int x; int y = x; } }").len(), 1);
    }

    #[test]
    fn well_formed_static_init_block_is_ok() {
        assert!(flow_errs("class C { static { int x = 1; int y = x; } }").is_empty());
    }

    #[test]
    fn do_while_runs_at_least_once() {
        assert!(flow_errs(&in_method("int x; do { x = 1; } while (p > 0); int y = x;")).is_empty());
    }

    #[test]
    fn short_circuit_and_carries_assignment_when_true() {
        let errs = flow_errs(
            "class C { int f() { return 1; } void m(int p) { int x; if (p > 0 && (x = f()) > 0) { int y = x; } } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn compound_assignment_reads_the_target() {
        assert_eq!(flow_errs(&in_method("int x; x += 1;")).len(), 1);
    }

    #[test]
    fn catch_variable_is_assigned() {
        assert!(flow_errs(&in_method("try { } catch (RuntimeException e) { int y = e.hashCode(); }")).is_empty());
    }

    #[test]
    fn finally_assignment_counts() {
        assert!(flow_errs(&in_method("int x; try { } finally { x = 1; } int y = x;")).is_empty());
    }

    // ---- alcanzabilidad (§14.21) ----

    #[test]
    fn code_after_return_is_unreachable() {
        let errs = flow_errs(&in_method("return; int x = 1;"));
        assert_eq!(errs.len(), 1);
        assert!(errs[0].message.contains("inalcanzable"));
    }

    #[test]
    fn code_after_infinite_loop_is_unreachable() {
        assert_eq!(flow_errs(&in_method("while (true) { } int x = 1;")).len(), 1);
    }

    #[test]
    fn a_break_makes_the_code_after_the_loop_reachable() {
        assert!(flow_errs(&in_method("while (true) { break; } int x = 1;")).is_empty());
    }

    #[test]
    fn while_false_body_is_unreachable() {
        assert_eq!(flow_errs(&in_method("while (false) { int x = 1; }")).len(), 1);
    }

    #[test]
    fn if_false_is_not_flagged_unreachable() {
        assert!(flow_errs(&in_method("if (false) { int x = 1; }")).is_empty());
    }

    // ---- variables `final` (DU, §16.2) ----

    #[test]
    fn reassigning_an_initialized_final_is_an_error() {
        let errs = flow_errs(&in_method("final int x = 1; x = 2;"));
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("final"));
    }

    #[test]
    fn assigning_a_blank_final_twice_is_an_error() {
        let errs = flow_errs(&in_method("final int x; x = 1; x = 2;"));
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("final"));
    }

    #[test]
    fn assigning_a_blank_final_once_is_fine() {
        assert!(flow_errs(&in_method("final int x; x = 1; int y = x;")).is_empty());
    }

    #[test]
    fn a_blank_final_assigned_in_both_branches_is_fine() {
        // Cada rama la asigna **una** vez; después del `if` está asignada, sin doble asignación.
        assert!(flow_errs(&in_method("final int x; if (p > 0) x = 1; else x = 2; int y = x;")).is_empty());
    }

    #[test]
    fn a_final_possibly_already_assigned_is_an_error() {
        // Tras `if (c) x = 1;` la variable está en el estado **ambiguo**: `x = 2` podría ser doble.
        let errs = flow_errs(&in_method("final int x; if (p > 0) x = 1; x = 2;"));
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("podría"));
    }

    #[test]
    fn reassigning_a_final_parameter_is_an_error() {
        let errs = flow_errs("class C { void m(final int p) { p = 1; } }");
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("final"));
    }

    #[test]
    fn incrementing_a_final_is_an_error() {
        assert_eq!(flow_errs(&in_method("final int x = 1; x++;")).len(), 1);
    }

    #[test]
    fn a_multi_catch_variable_is_implicitly_final() {
        let errs = flow_errs(&in_method("try { } catch (RuntimeException | Error e) { e = null; }"));
        assert_eq!(errs.len(), 1, "la variable del multi-catch es final: {errs:?}");
    }

    #[test]
    fn a_non_final_local_can_be_reassigned() {
        assert!(flow_errs(&in_method("int x = 1; x = 2; x = 3;")).is_empty());
    }
}
