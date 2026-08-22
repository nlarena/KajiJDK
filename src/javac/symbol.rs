//! Las **estructuras de la tabla de símbolos** — el producto de la pasada 1.
//!
//! Sigue la disciplina de **arenas + IDs** (como el `metaspace` de la JVM referencia por
//! `MethodId`/Class ID): los símbolos viven en un `Vec<Symbol>` y se referencian por
//! [`SymbolId`]; los *scopes* en otro `Vec<Scope>` por [`ScopeId`]. Así los ciclos del grafo
//! semántico (una clase apunta a sus miembros, un campo apunta a su clase) se vuelven
//! índices y el borrow checker no molesta.
//!
//! La **tabla de símbolos es un árbol de scopes enlazados que persiste** — cada scope guarda
//! un enlace a su contenedor (`enclosing`); resolver un nombre es seguir esa cadena hacia
//! afuera. No es una pila que se destruye al salir (eso es el modelo de un lenguaje de una
//! sola pasada).

use std::collections::HashMap;

use super::ast::{Modifier, PrimType, Type, TypeArg, TypeKind};

pub type SymbolId = usize;
pub type ScopeId = usize;

/// Un tipo **resuelto**: las referencias a clase/parámetro de tipo apuntan a su [`SymbolId`]
/// (a diferencia del [`Type`] sintáctico, que lleva el nombre sin resolver). Es lo que
/// persiste la pasada 1 para que la pasada 2 no vuelva a resolver nombres.
#[derive(Debug, Clone, PartialEq)]
pub enum RType {
    Prim(PrimType),
    Void,
    /// Un tipo referencia **sin argumentos**: no genérico (`String`), o el uso *raw* de uno
    /// genérico (`List`). Es también la forma que deja la *erasure* (§4.6).
    Class(SymbolId),
    /// Un tipo **parametrizado** resuelto: `List<String>` → base = el símbolo de `List`.
    /// `args` vacío es el **diamante** (`new ArrayList<>()`): pide inferir, no es raw.
    Parameterized { base: SymbolId, args: Vec<RTypeArg> },
    Array(Box<RType>),
    TypeVar(SymbolId),
    /// Una **variable de captura** (§5.1.10): la variable fresca que la *capture conversion*
    /// introduce por un *wildcard*. Lleva sus cotas **inline** (no hace falta un símbolo en la
    /// tabla): `upper` es su cota superior (la del parámetro, o `glb` con la del `? extends`), y
    /// `lower` la inferior de un `? super` (`None` si no la hay). `id` la hace **distinta** de otra
    /// captura con las mismas cotas (la frescura del §5.1.10).
    Capture { id: u32, upper: Box<RType>, lower: Option<Box<RType>> },
    /// Un **tipo intersección** (§4.9): `A & B & …`, con ≥2 miembros (tipos referencia). Lo produce
    /// el `lub` (§4.10.4) cuando dos tipos comparten varios supertipos incomparables (p. ej.
    /// `Integer`/`Long` → `Number & Comparable`). Su *erasure* es la del **primer** miembro (§4.6),
    /// que por convención es la clase (no una interfaz).
    Intersection(Vec<RType>),
    /// Una **variable de inferencia** (§18.1): la variable *fresca* que la inferencia de tipos
    /// introduce por cada parámetro de tipo de una invocación de método genérico. Distinta del
    /// [`RType::TypeVar`] (el parámetro declarado): dos invocaciones —o una anidada dentro de otra del
    /// **mismo** método (`id(id(x))`)— usan `id` frescos, así sus constraints no se pisan. **No escapa
    /// del módulo `infer`**: la inferencia la resuelve a un tipo concreto antes de devolver, así que el
    /// resto del compilador nunca la ve.
    InferVar(u32),
    /// El nombre no resolvió (ya reportado como error en la validación).
    Unresolved,
}

/// Un argumento de tipo resuelto (JLS §4.5.1): un tipo, o un *wildcard* con su cota.
#[derive(Debug, Clone, PartialEq)]
pub enum RTypeArg {
    Type(RType),
    /// `?` — equivale a `? extends Object`.
    Wildcard,
    /// `? extends T` — cota superior.
    Extends(Box<RType>),
    /// `? super T` — cota inferior.
    Super(Box<RType>),
}

/// La información **resuelta** de un símbolo, por variante.
#[derive(Debug, Clone)]
pub enum Resolved {
    /// Los supertipos **con sus argumentos** (`extends AbstractList<E>`): el subtipado genérico
    /// (§4.10.2) los necesita enteros para sustituir al subir por la jerarquía. Su *erasure*
    /// (solo el `SymbolId`) se obtiene con [`SymbolTable::super_class`]/[`SymbolTable::interfaces`].
    Class { super_type: Option<RType>, interface_types: Vec<RType>, permitted: Vec<RType> },
    Field(RType),
    /// Una firma de método. `varargs` marca la *aridad variable* (`int... xs`): el último
    /// parámetro es el array, y lo necesita la **fase 3** del overload resolution (JLS §15.12.2.4).
    Method { params: Vec<RType>, ret: RType, varargs: bool, throws: Vec<RType> },
    /// Las **cotas resueltas** de un parámetro de tipo (`<T extends Number>`). Vacío = `Object`.
    TypeVar { bounds: Vec<RType> },
}

/// La firma de un parámetro (su tipo declarado; el nombre y `varargs` son informativos).
#[derive(Debug, Clone)]
pub struct ParamSig {
    pub ty: Type,
    pub name: String,
    pub varargs: bool,
}

/// La variante de un símbolo. Los tipos que llevan (extends/return/…) son los **declarados**
/// (sintácticos): resolver los *nombres* de tipo a su [`SymbolId`] es trabajo de la pasada 2.
#[derive(Debug, Clone)]
pub enum SymbolKind {
    /// Un paquete: su scope contiene los tipos top-level que declara. El paquete **sin
    /// nombre** (sin `package`) también es un `Package` (con nombre `<unnamed>`).
    Package {
        members: ScopeId,
    },
    Class {
        kind: TypeKind,
        /// El *binary name* del tipo (`Outer$Inner` para anidados); lo necesita el codegen.
        binary: String,
        extends: Option<Type>,
        implements: Vec<Type>,
        /// La cláusula `permits` sintáctica de un tipo `sealed` (§8.1.6); vacía si no se escribió o
        /// el tipo no es `sealed`. La resuelve la pasada 2 a `Resolved::Class::permitted`.
        permits: Vec<Type>,
        /// El scope de miembros de esta clase.
        members: ScopeId,
    },
    Method {
        params: Vec<ParamSig>,
        return_type: Type,
        is_constructor: bool,
        /// Los tipos de la cláusula `throws` (sintácticos); su *erasure* la resuelve `Resolved`.
        throws: Vec<Type>,
    },
    Field {
        ty: Type,
    },
    /// Un parámetro de tipo genérico (`T`) — resoluble como nombre de tipo en su scope. Lleva
    /// sus **cotas** declaradas (`<T extends Number & Comparable<T>>`); vacío = cota implícita
    /// `Object`. Las necesitan el subtipado (§4.10.2) y la inferencia (Cap. 18).
    TypeVar { bounds: Vec<Type> },
}

/// Un símbolo: una entidad con nombre (clase, método, campo…) con su dueño y modificadores.
#[derive(Debug, Clone)]
pub struct Symbol {
    pub name: String,
    pub kind: SymbolKind,
    /// El símbolo que lo encierra léxicamente (la clase de un miembro), o `None` (top-level).
    pub owner: Option<SymbolId>,
    pub modifiers: Vec<Modifier>,
}

/// Un ámbito: una tabla `nombre → símbolos` (varios por nombre, para el *overloading*) con un
/// enlace al scope que lo encierra.
#[derive(Debug)]
pub struct Scope {
    pub enclosing: Option<ScopeId>,
    pub owner: Option<SymbolId>,
    entries: HashMap<String, Vec<SymbolId>>,
}

impl Scope {
    /// Los símbolos declarados **directamente** con ese nombre en este scope (sin subir).
    pub fn get(&self, name: &str) -> &[SymbolId] {
        self.entries.get(name).map(Vec::as_slice).unwrap_or(&[])
    }
}

/// La tabla de símbolos completa: la arena de símbolos, la de scopes, y el índice global de
/// clases por nombre cualificado. Es el "catálogo" que escribe la pasada 1 y lee la pasada 2.
pub struct SymbolTable {
    symbols: Vec<Symbol>,
    scopes: Vec<Scope>,
    /// Nombre cualificado → símbolo de clase. Punto de entrada para resolver tipos y para el
    /// *lazy completion* (aquí entrarían también las clases externas de los `.class`).
    classes: HashMap<String, SymbolId>,
    /// Nombre de paquete (`""` = paquete sin nombre) → su `Package`.
    packages: HashMap<String, SymbolId>,
    /// Tipos **externos** modelados (el *class finder*): nombre simple → símbolo sintético.
    /// No cuelgan de ningún paquete, así que no aparecen en el volcado.
    externals: HashMap<String, SymbolId>,
    /// Posición de fuente de cada símbolo declarado (para errores de fases que trabajan sobre
    /// la tabla, como la detección de ciclos).
    positions: HashMap<SymbolId, (u32, u32)>,
    /// `import static` de la unidad — **salida** de la pasada 1 para la pasada 2 (resolución
    /// de miembros): `miembro → tipo dueño` (single) y los tipos on-demand.
    pub static_single: HashMap<String, String>,
    pub static_on_demand: Vec<String>,
    /// Tipos **resueltos** por símbolo (grafo de herencia + firmas) — salida para la pasada 2.
    resolved_map: HashMap<SymbolId, Resolved>,
    /// Contador de **variables de captura** (§5.1.10): da un `id` fresco por *wildcard* capturado.
    /// Es mutabilidad **interior** (`Cell`) para poder crear capturas con la tabla compartida `&self`
    /// —Attribute la tiene inmutable—, sin conflictos de *borrow* en medio del tipado.
    capture_counter: std::cell::Cell<u32>,
    /// Contador de **variables de inferencia** (§18.1): da un `id` fresco por parámetro de tipo de cada
    /// invocación genérica. Mutabilidad **interior** por la misma razón que `capture_counter`.
    infer_counter: std::cell::Cell<u32>,
    /// El **método envolvente** de una clase local/anónima (para el atributo `EnclosingMethod`,
    /// §4.7.7): `class_id → (nombre, descriptor)` del método/constructor que la declara. Ausente si
    /// se declaró en un inicializador (ahí `method_index` es 0). Lo puebla el desugar.
    enclosing_methods: HashMap<SymbolId, (String, String)>,
    /// Valores de **campos constantes** (`static final` numéricos/`String` de la unidad, §15.28):
    /// `SymbolId del campo → su valor plegado`. Lo puebla la pasada de plegado tras el atributado
    /// (fixpoint sobre las referencias entre `final`), y lo leen el desugar y el codegen para
    /// **inlinear** las referencias (`static final int B = A * 2;` → `B = 20` en `ConstantValue`).
    const_fields: HashMap<SymbolId, super::codegen::ConstVal>,
    /// El scope **raíz**: contiene los paquetes; cada paquete tiene su propio scope, y el de
    /// cada clase se enlaza al de su paquete.
    pub global: ScopeId,
}

/// El `SymbolId` de un supertipo, sea crudo (`Class`) o parametrizado — o sea, su *erasure*.
fn erased_id(t: &RType) -> Option<SymbolId> {
    match t {
        RType::Class(id) | RType::Parameterized { base: id, .. } => Some(*id),
        RType::Capture { upper, .. } => erased_id(upper),
        RType::Intersection(ms) => ms.first().and_then(erased_id),
        _ => None,
    }
}

impl SymbolTable {
    pub fn new() -> Self {
        let mut table = SymbolTable {
            symbols: Vec::new(),
            scopes: Vec::new(),
            classes: HashMap::new(),
            packages: HashMap::new(),
            externals: HashMap::new(),
            positions: HashMap::new(),
            static_single: HashMap::new(),
            static_on_demand: Vec::new(),
            resolved_map: HashMap::new(),
            capture_counter: std::cell::Cell::new(0),
            infer_counter: std::cell::Cell::new(0),
            enclosing_methods: HashMap::new(),
            const_fields: HashMap::new(),
            global: 0,
        };
        table.global = table.new_scope(None, None);
        table
    }

    /// Un `id` fresco para una nueva variable de captura (§5.1.10). Solo necesita `&self` (mutabilidad
    /// interior): así se puede capturar en medio del tipado, con la tabla compartida.
    pub fn fresh_capture_id(&self) -> u32 {
        let n = self.capture_counter.get();
        self.capture_counter.set(n + 1);
        n
    }

    /// Un `id` fresco para una nueva **variable de inferencia** (§18.1). Como [`Self::fresh_capture_id`],
    /// solo necesita `&self` (mutabilidad interior): la inferencia corre con la tabla compartida.
    pub fn fresh_infer_id(&self) -> u32 {
        let n = self.infer_counter.get();
        self.infer_counter.set(n + 1);
        n
    }

    /// Registra el **método envolvente** de una clase local/anónima (para `EnclosingMethod`, §4.7.7).
    pub fn set_enclosing_method(&mut self, class_id: SymbolId, name: String, descriptor: String) {
        self.enclosing_methods.insert(class_id, (name, descriptor));
    }

    /// El método envolvente registrado (`(nombre, descriptor)`), o `None` si la clase no está dentro
    /// de un método (o no es local/anónima).
    pub fn enclosing_method(&self, class_id: SymbolId) -> Option<&(String, String)> {
        self.enclosing_methods.get(&class_id)
    }

    pub fn symbol_count(&self) -> usize {
        self.symbols.len()
    }

    /// Guarda el mapa de **valores de campos constantes** (ver [`Self::const_fields`]). Lo produce el
    /// plegado de constantes (fixpoint) tras el atributado, antes del desugar.
    pub(crate) fn set_const_fields(&mut self, map: HashMap<SymbolId, super::codegen::ConstVal>) {
        self.const_fields = map;
    }

    /// El mapa de **campos constantes** de la unidad (`SymbolId del campo → valor plegado`), que el
    /// desugar y el codegen consultan para inlinear referencias entre `final`. Vacío hasta el plegado.
    pub(crate) fn const_fields(&self) -> &HashMap<SymbolId, super::codegen::ConstVal> {
        &self.const_fields
    }

    /// Guarda la info resuelta de un símbolo (grafo/firmas), salida para la pasada 2.
    pub fn set_resolved(&mut self, id: SymbolId, r: Resolved) {
        self.resolved_map.insert(id, r);
    }

    pub fn resolved(&self, id: SymbolId) -> Option<&Resolved> {
        self.resolved_map.get(&id)
    }

    /// La **superclase** de una clase, ya borrada (`SymbolId`) — lo que necesita el lookup de
    /// miembros y la detección de ciclos. Para el subtipado genérico usar [`Self::super_type`].
    pub fn super_class(&self, cid: SymbolId) -> Option<SymbolId> {
        self.super_type(cid).and_then(erased_id)
    }

    /// Las **interfaces** de un tipo, ya borradas (`SymbolId`s).
    pub fn interfaces(&self, cid: SymbolId) -> Vec<SymbolId> {
        self.interface_types(cid).iter().filter_map(erased_id).collect()
    }

    /// El supertipo **con sus argumentos de tipo** (`AbstractList<E>`), para el subtipado genérico.
    pub fn super_type(&self, cid: SymbolId) -> Option<&RType> {
        match self.resolved_map.get(&cid) {
            Some(Resolved::Class { super_type, .. }) => super_type.as_ref(),
            _ => None,
        }
    }

    /// Las interfaces **con sus argumentos de tipo** (`Collection<E>`).
    pub fn interface_types(&self, cid: SymbolId) -> &[RType] {
        match self.resolved_map.get(&cid) {
            Some(Resolved::Class { interface_types, .. }) => interface_types,
            _ => &[],
        }
    }

    /// Los subtipos **autorizados** de un tipo `sealed` (§8.1.6), con sus argumentos de tipo — ya
    /// resueltos. Vacío si el tipo no es `sealed` o no declaró `permits` (implícita).
    pub fn permitted(&self, cid: SymbolId) -> &[RType] {
        match self.resolved_map.get(&cid) {
            Some(Resolved::Class { permitted, .. }) => permitted,
            _ => &[],
        }
    }

    /// ¿El tipo es `sealed`? (Lo lleva el modificador; el `permits` resuelto vive en el grafo.)
    pub fn is_sealed(&self, cid: SymbolId) -> bool {
        self.symbol(cid).modifiers.contains(&Modifier::Sealed)
    }

    /// Rellena los subtipos autorizados de un tipo `sealed` (para el `permits` **implícito**, §8.1.6).
    /// No hace nada si el símbolo aún no tiene su `Resolved::Class`.
    pub fn set_permitted(&mut self, cid: SymbolId, permitted: Vec<RType>) {
        if let Some(Resolved::Class { permitted: p, .. }) = self.resolved_map.get_mut(&cid) {
            *p = permitted;
        }
    }

    /// Registra la posición de fuente de un símbolo.
    pub fn set_pos(&mut self, id: SymbolId, line: u32, col: u32) {
        self.positions.insert(id, (line, col));
    }

    /// La posición de fuente de un símbolo, o `(0, 0)` si no se registró.
    pub fn pos_of(&self, id: SymbolId) -> (u32, u32) {
        self.positions.get(&id).copied().unwrap_or((0, 0))
    }

    /// Registra (o recupera) un tipo **externo** modelado por su nombre simple — el *class
    /// finder*. Crea un `ClassSymbol` sintético sin dueño (invisible al volcado).
    pub fn add_external(&mut self, simple: &str) -> SymbolId {
        if let Some(&id) = self.externals.get(simple) {
            return id;
        }
        let members = self.new_scope(None, None);
        let id = self.new_symbol(Symbol {
            name: simple.to_string(),
            kind: SymbolKind::Class {
                kind: TypeKind::Class,
                binary: format!("java.lang.{simple}"),
                extends: None,
                implements: Vec::new(),
                permits: Vec::new(),
                members,
            },
            owner: None,
            modifiers: Vec::new(),
        });
        self.set_scope_owner(members, id);
        self.externals.insert(simple.to_string(), id);
        id
    }

    /// El tipo externo modelado con ese nombre simple, si está registrado.
    pub fn external(&self, simple: &str) -> Option<SymbolId> {
        self.externals.get(simple).copied()
    }

    /// Registra un símbolo externo ya construido bajo su nombre simple (para el *class finder*
    /// real, que arma el símbolo con sus miembros leídos del `.class`).
    pub fn register_external(&mut self, simple: &str, id: SymbolId) {
        self.externals.insert(simple.to_string(), id);
    }

    /// El `Package` de nombre `name` (o el paquete sin nombre si `None`), creándolo la
    /// primera vez con su scope enlazado al raíz.
    pub fn get_or_create_package(&mut self, name: Option<&str>) -> SymbolId {
        let key = name.unwrap_or("");
        if let Some(&id) = self.packages.get(key) {
            return id;
        }
        let members = self.new_scope(Some(self.global), None);
        let display = name.unwrap_or("<unnamed>").to_string();
        let id = self.new_symbol(Symbol {
            name: display,
            kind: SymbolKind::Package { members },
            owner: None,
            modifiers: Vec::new(),
        });
        self.set_scope_owner(members, id);
        self.packages.insert(key.to_string(), id);
        id
    }

    /// El `Package` ya registrado con ese nombre (`""` = sin nombre), si existe.
    pub fn package(&self, name: &str) -> Option<SymbolId> {
        self.packages.get(name).copied()
    }

    pub fn new_symbol(&mut self, symbol: Symbol) -> SymbolId {
        self.symbols.push(symbol);
        self.symbols.len() - 1
    }

    pub fn new_scope(&mut self, enclosing: Option<ScopeId>, owner: Option<SymbolId>) -> ScopeId {
        self.scopes.push(Scope { enclosing, owner, entries: HashMap::new() });
        self.scopes.len() - 1
    }

    pub fn symbol(&self, id: SymbolId) -> &Symbol {
        &self.symbols[id]
    }

    pub fn scope(&self, id: ScopeId) -> &Scope {
        &self.scopes[id]
    }

    /// El scope **propio** de un símbolo: el que declara sus parámetros de tipo. Lo tienen las
    /// clases (su scope de miembros) y los métodos **genéricos**; los demás métodos no, y su
    /// firma resuelve en el de su clase.
    pub fn own_scope(&self, id: SymbolId) -> Option<ScopeId> {
        self.scopes.iter().position(|s| s.owner == Some(id))
    }

    pub fn set_scope_owner(&mut self, scope: ScopeId, owner: SymbolId) {
        self.scopes[scope].owner = Some(owner);
    }

    /// El símbolo de clase con nombre cualificado `fqn`, si está registrado.
    pub fn class(&self, fqn: &str) -> Option<SymbolId> {
        self.classes.get(fqn).copied()
    }

    pub fn register_class(&mut self, fqn: &str, id: SymbolId) {
        self.classes.insert(fqn.to_string(), id);
    }

    /// Registra `sym` bajo `name` en `scope` (acumula, no pisa — de ahí el overloading).
    pub fn define(&mut self, scope: ScopeId, name: &str, sym: SymbolId) {
        self.scopes[scope].entries.entry(name.to_string()).or_default().push(sym);
    }

    /// Resuelve `name` desde `scope` **subiendo por la cadena** de scopes contenedores;
    /// devuelve los símbolos del scope más cercano que lo tenga (vacío si no aparece).
    pub fn resolve(&self, scope: ScopeId, name: &str) -> &[SymbolId] {
        let mut current = Some(scope);
        while let Some(id) = current {
            let hit = self.scopes[id].get(name);
            if !hit.is_empty() {
                return hit;
            }
            current = self.scopes[id].enclosing;
        }
        &[]
    }

    /// Resuelve `name` a un símbolo de **tipo** (clase o parámetro de tipo), subiendo por la
    /// cadena de scopes y **salteando** los que no son tipos — clave porque un constructor se
    /// llama igual que su clase y la taparía en su propio scope.
    pub fn resolve_type(&self, scope: ScopeId, name: &str) -> Option<SymbolId> {
        let mut current = Some(scope);
        while let Some(id) = current {
            for &sid in self.scopes[id].get(name) {
                if matches!(self.symbols[sid].kind, SymbolKind::Class { .. } | SymbolKind::TypeVar { .. }) {
                    return Some(sid);
                }
            }
            current = self.scopes[id].enclosing;
        }
        None
    }

    /// Los símbolos declarados con dueño `owner`, en orden de declaración (para volcados).
    pub fn members_of(&self, owner: SymbolId) -> Vec<SymbolId> {
        (0..self.symbols.len()).filter(|&i| self.symbols[i].owner == Some(owner)).collect()
    }

    pub fn class_names(&self) -> Vec<&str> {
        self.classes.keys().map(String::as_str).collect()
    }

    /// Los símbolos de las clases del **fuente** (no incluye los tipos externos modelados).
    pub fn source_classes(&self) -> Vec<SymbolId> {
        self.classes.values().copied().collect()
    }

    /// El *nest host* de un tipo: su clase de nivel superior — se sube por la cadena de dueños
    /// mientras el dueño siga siendo una clase. Para un tipo top-level, es él mismo.
    pub fn nest_host(&self, id: SymbolId) -> SymbolId {
        let mut host = id;
        while let Some(owner) = self.symbol(host).owner {
            if matches!(self.symbol(owner).kind, SymbolKind::Class { .. }) {
                host = owner;
            } else {
                break;
            }
        }
        host
    }

    /// Vuelca la tabla como texto legible, agrupada por **paquete** → clases → miembros —
    /// para inspección (`javac --symbols`) y tests. Orden estable (por nombre).
    pub fn dump(&self) -> String {
        let mut pkg_keys: Vec<&String> = self.packages.keys().collect();
        pkg_keys.sort();
        let mut out = String::new();
        for key in pkg_keys {
            let pid = self.packages[key];
            out.push_str(&format!("package {}\n", self.symbol(pid).name));
            let mut classes = self.members_of(pid);
            classes.sort_by(|&a, &b| self.symbol(a).name.cmp(&self.symbol(b).name));
            for cid in classes {
                self.dump_class(cid, 1, &mut out);
            }
        }
        out
    }

    /// Vuelca la tabla como **grilla**: una fila por símbolo de la arena (su [`SymbolId`], kind,
    /// dueño, tipo/firma **resuelto** y posición). Es la vista "plana" — complementa a [`dump`],
    /// que muestra el anidamiento de scopes. Los tipos **externos** (cargados del classpath) van
    /// en una sección aparte al pie, para no ahogar los símbolos del fuente.
    pub fn dump_table(&self) -> String {
        let ext_roots: std::collections::HashSet<SymbolId> = self.externals.values().copied().collect();
        let headers = ["#", "SÍMBOLO", "KIND", "DUEÑO", "TIPO / FIRMA (resuelto)", "POS"];
        let mut rows: Vec<[String; 6]> = Vec::new();
        for id in 0..self.symbols.len() {
            if ext_roots.contains(&self.root_owner(id)) {
                continue; // los externos van al pie
            }
            let sym = &self.symbols[id];
            let owner = match sym.owner {
                Some(o) => format!("#{o} {}", self.symbols[o].name),
                None => "—".to_string(),
            };
            let (l, c) = self.pos_of(id);
            let pos = if l == 0 { "—".to_string() } else { format!("{l}:{c}") };
            rows.push([
                format!("#{id}"),
                sym.name.clone(),
                self.kind_label(sym).to_string(),
                owner,
                self.signature_str(id),
                pos,
            ]);
        }

        // Anchos por columna, del máximo entre el encabezado y las celdas.
        let mut w = [0usize; 6];
        for (i, h) in headers.iter().enumerate() {
            w[i] = h.chars().count();
        }
        for r in &rows {
            for (i, cell) in r.iter().enumerate() {
                w[i] = w[i].max(cell.chars().count());
            }
        }
        let mut out = String::new();
        let render = |out: &mut String, cells: &[String; 6]| {
            for (i, cell) in cells.iter().enumerate() {
                if i > 0 {
                    out.push_str("  ");
                }
                out.push_str(cell);
                out.push_str(&" ".repeat(w[i] - cell.chars().count()));
            }
            // Quitar el relleno sobrante de la última columna.
            while out.ends_with(' ') {
                out.pop();
            }
            out.push('\n');
        };
        let header_cells: [String; 6] = headers.map(String::from);
        render(&mut out, &header_cells);
        let total: usize = w.iter().sum::<usize>() + 2 * (w.len() - 1);
        out.push_str(&"─".repeat(total));
        out.push('\n');
        for r in &rows {
            render(&mut out, r);
        }

        // Externos cargados (nombre simple + conteo de miembros leídos del `.class`).
        let mut names: Vec<&String> = self.externals.keys().collect();
        names.sort();
        if !names.is_empty() {
            out.push_str("\nexternos cargados: ");
            let parts: Vec<String> = names
                .iter()
                .map(|n| {
                    let id = self.externals[*n];
                    let (f, m) = self.member_counts(id);
                    if f == 0 && m == 0 {
                        (*n).clone()
                    } else {
                        format!("{n} ({f}c/{m}m)")
                    }
                })
                .collect();
            out.push_str(&parts.join(", "));
            out.push('\n');
        }
        out
    }

    /// El símbolo raíz de la cadena de dueños (para distinguir fuente de externo).
    fn root_owner(&self, id: SymbolId) -> SymbolId {
        let mut cur = id;
        while let Some(o) = self.symbols[cur].owner {
            cur = o;
        }
        cur
    }

    /// La etiqueta de kind de un símbolo para la grilla.
    fn kind_label(&self, sym: &Symbol) -> &'static str {
        match &sym.kind {
            SymbolKind::Package { .. } => "package",
            SymbolKind::Class { kind, .. } => type_kw(*kind),
            SymbolKind::Method { is_constructor: true, .. } => "ctor",
            SymbolKind::Method { .. } => "method",
            SymbolKind::Field { .. } => "field",
            SymbolKind::TypeVar { .. } => "type-var",
        }
    }

    /// La firma **resuelta** de un símbolo como texto (lo que consume la pasada 2): la jerarquía
    /// de una clase, el tipo de un campo, `(params): ret` de un método. `—` si no aplica.
    fn signature_str(&self, id: SymbolId) -> String {
        match self.resolved(id) {
            Some(Resolved::Class { super_type, interface_types, permitted }) => {
                let mut s = String::new();
                if let Some(sc) = super_type {
                    s.push_str(&format!(": {}", self.rtype_str(sc)));
                }
                if !interface_types.is_empty() {
                    let names: Vec<_> = interface_types.iter().map(|i| self.rtype_str(i)).collect();
                    s.push_str(&format!(" impl {}", names.join(", ")));
                }
                if !permitted.is_empty() {
                    let names: Vec<_> = permitted.iter().map(|i| self.rtype_str(i)).collect();
                    s.push_str(&format!(" permits {}", names.join(", ")));
                }
                if s.is_empty() { "—".to_string() } else { s }
            }
            Some(Resolved::Field(t)) => self.rtype_str(t),
            Some(Resolved::Method { params, ret, varargs, .. }) => {
                let mut ps: Vec<_> = params.iter().map(|p| self.rtype_str(p)).collect();
                // Un varargs se muestra como lo escribió el fuente: `int...`, no `int[]`.
                if *varargs {
                    if let Some(last) = ps.last_mut() {
                        *last = format!("{}...", last.trim_end_matches("[]"));
                    }
                }
                format!("({}): {}", ps.join(", "), self.rtype_str(ret))
            }
            // Las cotas de un parámetro de tipo: `T extends Number & Comparable<T>`.
            Some(Resolved::TypeVar { bounds }) if !bounds.is_empty() => {
                let bs: Vec<String> = bounds.iter().map(|b| self.rtype_str(b)).collect();
                format!("extends {}", bs.join(" & "))
            }
            Some(Resolved::TypeVar { .. }) | None => "—".to_string(),
        }
    }

    /// Un [`RType`] como texto, resolviendo los `SymbolId` a nombres. Público para los
    /// **diagnósticos** (las notas `símbolo:`/`ubicación:` de un *cannot find symbol*).
    pub fn rtype_str(&self, rt: &RType) -> String {
        match rt {
            RType::Prim(p) => type_str(&Type::Prim(*p)),
            RType::Void => "void".to_string(),
            RType::Class(id) | RType::TypeVar(id) => self.symbols[*id].name.clone(),
            RType::Parameterized { base, args } => {
                let a: Vec<String> = args.iter().map(|x| self.rtype_arg_str(x)).collect();
                format!("{}<{}>", self.symbols[*base].name, a.join(", "))
            }
            RType::Array(inner) => format!("{}[]", self.rtype_str(inner)),
            RType::Capture { id, upper, .. } => format!("cap#{id} of {}", self.rtype_str(upper)),
            RType::InferVar(id) => format!("infer#{id}"),
            RType::Intersection(ms) => {
                ms.iter().map(|m| self.rtype_str(m)).collect::<Vec<_>>().join(" & ")
            }
            RType::Unresolved => "?".to_string(),
        }
    }

    fn rtype_arg_str(&self, arg: &RTypeArg) -> String {
        match arg {
            RTypeArg::Type(t) => self.rtype_str(t),
            RTypeArg::Wildcard => "?".to_string(),
            RTypeArg::Extends(t) => format!("? extends {}", self.rtype_str(t)),
            RTypeArg::Super(t) => format!("? super {}", self.rtype_str(t)),
        }
    }

    /// Cuenta (campos, métodos) declarados directamente en el scope de miembros de una clase.
    fn member_counts(&self, cid: SymbolId) -> (usize, usize) {
        let (mut fields, mut methods) = (0, 0);
        for mid in self.members_of(cid) {
            match self.symbols[mid].kind {
                SymbolKind::Field { .. } => fields += 1,
                SymbolKind::Method { .. } => methods += 1,
                _ => {}
            }
        }
        (fields, methods)
    }

    /// Vuelca una clase y sus miembros a profundidad `depth` (2 espacios por nivel),
    /// recurriendo en los **tipos anidados**.
    fn dump_class(&self, cid: SymbolId, depth: usize, out: &mut String) {
        let sym = self.symbol(cid);
        let SymbolKind::Class { kind, binary, extends, implements, .. } = &sym.kind else {
            return;
        };
        let pad = "  ".repeat(depth);
        let inner = "  ".repeat(depth + 1);
        let kw = type_kw(*kind);
        let mut line = format!("{pad}{}{kw} {}", mods(&sym.modifiers), sym.name);
        if binary.contains('$') {
            line.push_str(&format!("  [{binary}]"));
        }
        if let Some(e) = extends {
            line.push_str(&format!(" extends {}", type_str(e)));
        }
        if !implements.is_empty() {
            let names: Vec<_> = implements.iter().map(type_str).collect();
            line.push_str(&format!(" implements {}", names.join(", ")));
        }
        out.push_str(&line);
        out.push('\n');
        for mid in self.members_of(cid) {
            let m = self.symbol(mid);
            match &m.kind {
                SymbolKind::Field { ty } => {
                    out.push_str(&format!("{inner}field {}{}: {}\n", mods(&m.modifiers), m.name, type_str(ty)));
                }
                SymbolKind::Method { params, return_type, is_constructor, .. } => {
                    // Un varargs se muestra como se escribió (`int...`), aunque su tipo declarado
                    // sea el array (`int[]`, JLS §8.4.1).
                    let ps: Vec<String> = params
                        .iter()
                        .map(|p| {
                            let t = type_str(&p.ty);
                            if p.varargs { format!("{}...", t.trim_end_matches("[]")) } else { t }
                        })
                        .collect();
                    if *is_constructor {
                        out.push_str(&format!("{inner}ctor {}{}({})\n", mods(&m.modifiers), m.name, ps.join(", ")));
                    } else {
                        out.push_str(&format!(
                            "{inner}method {}{}({}): {}\n",
                            mods(&m.modifiers),
                            m.name,
                            ps.join(", "),
                            type_str(return_type)
                        ));
                    }
                }
                // Un tipo anidado: se dibuja recursivamente, un nivel más adentro.
                SymbolKind::Class { .. } => self.dump_class(mid, depth + 1, out),
                SymbolKind::Package { .. } | SymbolKind::TypeVar { .. } => {}
            }
        }
    }
}

impl Default for SymbolTable {
    fn default() -> Self {
        Self::new()
    }
}

// ---- helpers de string para el volcado ----

fn type_kw(kind: TypeKind) -> &'static str {
    match kind {
        TypeKind::Class => "class",
        TypeKind::Interface => "interface",
        TypeKind::Enum => "enum",
        TypeKind::Record => "record",
        TypeKind::Annotation => "@interface",
    }
}

fn mods(mods: &[Modifier]) -> String {
    mods.iter().map(|m| format!("{} ", modifier_str(*m))).collect()
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
    use super::ast::PrimType;
    match ty {
        Type::Void => "void".to_string(),
        Type::Prim(p) => match p {
            PrimType::Int => "int",
            PrimType::Long => "long",
            PrimType::Short => "short",
            PrimType::Byte => "byte",
            PrimType::Char => "char",
            PrimType::Boolean => "boolean",
            PrimType::Float => "float",
            PrimType::Double => "double",
        }
        .to_string(),
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
