//! El **AST** (árbol de sintaxis abstracta): los tipos que representan la estructura del
//! programa — unidad de compilación, clases, miembros, sentencias, expresiones — tal como
//! los produce el [`parser`](super::parser) y los recorren la fase semántica y el codegen.
//!
//! ## El árbol se **decora**
//!
//! Cada [`Expr`]/[`Stmt`] es un **struct** con dos mitades:
//!
//! - lo que escribe el **parser**: `kind` (la forma sintáctica) + `pos` (para ubicar errores),
//! - lo que rellena la **pasada 2** ([`attribute`](super::attribute)): el `ty` de cada expresión,
//!   su `binding` (a qué resolvió el nombre), y el `local` (slot + tipo) de las declaraciones.
//!
//! Es el modelo de javac, donde `JCTree` lleva sus campos `type`/`sym` y `Attr` los completa
//! **in situ**. Sin esa decoración el codegen no sabría qué `invoke*`/`get*` emitir ni con qué
//! descriptor (ver `docs/pasada2-attribute.md` §9–§10).

use super::symbol::{RType, SymbolId};

/// Una unidad de compilación: un archivo `.java` — `package` opcional, `import`s y las
/// declaraciones de tipo de nivel superior. Un `module-info.java` no lleva tipos: su declaración de
/// **módulo** (§7.7) va en `module`.
#[derive(Debug, Clone, PartialEq)]
pub struct CompilationUnit {
    pub package: Option<String>,
    pub imports: Vec<Import>,
    pub types: Vec<ClassDecl>,
    /// La declaración de módulo de un `module-info.java` (§7.7); `None` en una unidad ordinaria.
    pub module: Option<ModuleDecl>,
}

/// La declaración de un **módulo** (§7.7): `[open] module nombre.cualificado { directivas }`.
#[derive(Debug, Clone, PartialEq)]
pub struct ModuleDecl {
    pub pos: Pos,
    pub annotations: Vec<Annotation>,
    /// `open module` (§7.7): abre **todos** los paquetes a la reflexión en tiempo de ejecución.
    pub open: bool,
    /// El nombre cualificado del módulo (`com.example.foo`), con los puntos tal cual.
    pub name: String,
    pub directives: Vec<ModuleDirective>,
}

/// Una **directiva** del cuerpo de un módulo (§7.7.1–§7.7.4).
#[derive(Debug, Clone, PartialEq)]
pub enum ModuleDirective {
    /// `requires [transitive] [static] otro.modulo;` — dependencia (§7.7.1).
    Requires { transitive: bool, is_static: bool, name: String },
    /// `exports paquete [to m1, m2];` — hace público un paquete (§7.7.2). `to` vacío = a todos.
    Exports { package: String, to: Vec<String> },
    /// `opens paquete [to m1, m2];` — lo abre a la reflexión (§7.7.2).
    Opens { package: String, to: Vec<String> },
    /// `uses tipo.Servicio;` — declara consumir un servicio (§7.7.3).
    Uses { service: String },
    /// `provides tipo.Servicio with Impl1, Impl2;` — provee implementaciones (§7.7.4).
    Provides { service: String, with: Vec<String> },
}

#[derive(Debug, Clone, PartialEq)]
pub struct Import {
    pub path: String,
    pub is_static: bool,
    pub wildcard: bool,
}

/// Una posición en el fuente (línea/columna, 1-based). Se adjunta a las declaraciones para
/// que los errores de la fase semántica puedan ubicarse.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Pos {
    pub line: u32,
    pub col: u32,
}

/// La forma de una declaración de tipo.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TypeKind {
    Class,
    Interface,
    Enum,
    Record,
    /// Un tipo anotación (`@interface`). Reservado — el parser aún no lo produce.
    Annotation,
}

/// Una declaración de tipo (`class`/`interface`/`enum`/`record`; sin genéricos por ahora).
#[derive(Debug, Clone, PartialEq)]
pub struct ClassDecl {
    pub pos: Pos,
    pub annotations: Vec<Annotation>,
    pub modifiers: Vec<Modifier>,
    pub kind: TypeKind,
    pub name: String,
    /// Parámetros de tipo genéricos (`class Box<T, U extends Number>`), con sus cotas.
    pub type_params: Vec<TypeParam>,
    /// Componentes de un `record` (`record P(int x, int y)`); vacío para los demás.
    pub components: Vec<Param>,
    pub extends: Option<Type>,
    pub implements: Vec<Type>,
    /// Anotaciones de tipo sobre el `extends` (`class C extends @A Base`), con su `type_path`. Target
    /// `0x10`, `supertype_index = 0xFFFF`. Vacío si no hay `extends` o no lleva anotaciones.
    pub extends_annos: Vec<TypeUseAnnot>,
    /// Anotaciones de tipo sobre cada interfaz de `implements` (`implements @A I1, @B I2`), **en
    /// paralelo** a `implements`. Target `0x10`, `supertype_index` = índice en la lista. Cada sublista
    /// puede estar vacía.
    pub implements_annos: Vec<Vec<TypeUseAnnot>>,
    /// La cláusula `permits` de un tipo `sealed` (§8.1.6): los subtipos autorizados. Vacío si no se
    /// escribió (implícita: los subtipos declarados en la misma unidad) o si el tipo no es `sealed`.
    pub permits: Vec<Type>,
    /// Constantes de un `enum`; vacío para los demás.
    pub enum_constants: Vec<EnumConstant>,
    pub members: Vec<Member>,
    /// Los valores **por defecto** de los elementos de un `@interface` (`String value() default "x";`),
    /// como pares `(nombre del elemento, valor)`. Vacío salvo en un tipo de anotación con defaults. Se
    /// retienen para el atributo `AnnotationDefault` (§4.7.22) del método del elemento.
    pub annotation_defaults: Vec<(String, AnnotationValue)>,
}

/// Una constante de `enum`: `NAME` o `NAME(args)`.
#[derive(Debug, Clone, PartialEq)]
pub struct EnumConstant {
    /// Anotaciones sobre la constante (`@Deprecated FOO`); se retienen aunque el emisor todavía no
    /// las escriba.
    pub annotations: Vec<Annotation>,
    pub name: String,
    pub args: Vec<Expr>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Modifier {
    Public,
    Private,
    Protected,
    Static,
    Final,
    Abstract,
    Native,
    Synchronized,
    Transient,
    Volatile,
    Strictfp,
    Default,
    /// `sealed` (§8.1.1.2 / §9.1.1.4): la clase/interfaz restringe sus subtipos a los de `permits`.
    Sealed,
    /// `non-sealed`: reabre a la extensión un subtipo de un tipo `sealed`.
    NonSealed,
}

/// Una **anotación** de uso (§9.7): `@Name`, `@Name(v)` o `@Name(a = 1, b = {…})`. Se retiene en el
/// AST —no se descarta— para que el chequeo de `@Override` y, más adelante, el emisor de atributos
/// `RuntimeVisibleAnnotations` tengan qué mirar.
#[derive(Debug, Clone, PartialEq)]
pub struct Annotation {
    pub name: String,
    pub args: Vec<AnnotationArg>,
}

/// Un par `elemento = valor` de una anotación. `name` es `None` en la forma de **valor único**
/// (`@SuppressWarnings("x")`, donde el elemento implícito es `value`).
#[derive(Debug, Clone, PartialEq)]
pub struct AnnotationArg {
    pub name: Option<String>,
    pub value: AnnotationValue,
}

/// El valor de un elemento de anotación (§9.7.1): una expresión constante (o literal de clase, o
/// constante de `enum`), un arreglo `{ … }`, o una anotación **anidada**.
#[derive(Debug, Clone, PartialEq)]
pub enum AnnotationValue {
    Expr(Box<Expr>),
    Array(Vec<AnnotationValue>),
    Nested(Box<Annotation>),
}

/// Un paso del `type_path` de una **type annotation** (§4.7.20.2): cómo llegar, desde el tipo raíz de
/// una posición, al componente anotado. Los `kind`s del JVMS: `0` array, `1` tipo anidado, `2` cota de
/// wildcard, `3` argumento de tipo (con su índice).
#[derive(Debug, Clone, PartialEq)]
pub enum TypePathStep {
    Array,
    Nested,
    WildcardBound,
    TypeArgument(u8),
}

/// Una anotación sobre un **uso de tipo** (§9.7.4), con el `type_path` desde el tipo raíz de su
/// posición (vacío = el tipo de arriba de todo). El **target** (campo/retorno/param/…) lo da el lugar
/// donde se guarda. Retenida por el parser para el atributo `RuntimeVisibleTypeAnnotations` (§4.7.20).
#[derive(Debug, Clone, PartialEq)]
pub struct TypeUseAnnot {
    pub path: Vec<TypePathStep>,
    pub annotation: Annotation,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Member {
    Field(FieldDecl),
    Method(MethodDecl),
    /// Un tipo **anidado** (clase o interfaz declarada dentro del cuerpo de otra).
    Type(ClassDecl),
    /// Un **inicializador estático** `static { … }` (§8.7) — el cuerpo del `<clinit>`. También lo
    /// sintetiza el desugar para poblar el `$SwitchMap` de un `switch` sobre `enum`.
    StaticInit(Block),
    /// Un **inicializador de instancia** `{ … }` (§8.6): corre al construir, dentro de cada
    /// constructor y después del `super()`. El desugar los copia al cuerpo de cada constructor.
    InstanceInit(Block),
}

#[derive(Debug, Clone, PartialEq)]
pub struct FieldDecl {
    pub pos: Pos,
    pub annotations: Vec<Annotation>,
    pub modifiers: Vec<Modifier>,
    pub ty: Type,
    pub name: String,
    pub init: Option<Expr>,
    /// Anotaciones sobre **usos de tipo anidados** dentro del tipo del campo (`List<@A String> f`),
    /// con su `type_path`. Las **líder** (`@A String f`) las rutea el emisor por `@Target`; estas son
    /// las que el parser recolecta al descender por el tipo. Target `0x13`. Vacío si no hay.
    pub type_annos: Vec<TypeUseAnnot>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct MethodDecl {
    pub pos: Pos,
    pub annotations: Vec<Annotation>,
    pub modifiers: Vec<Modifier>,
    /// Parámetros de tipo de un método genérico (`<T> T id(T x)`), con sus cotas.
    pub type_params: Vec<TypeParam>,
    /// Tipo de retorno; [`Type::Void`] para un constructor (ver `is_constructor`).
    pub return_type: Type,
    pub name: String,
    pub params: Vec<Param>,
    /// Los tipos de la cláusula `throws` (§8.4.6). Vacío si no la hay. Se retienen —ya no se
    /// descartan— para el chequeo de **excepciones chequeadas** (§11.2).
    pub throws: Vec<Type>,
    /// Cuerpo, o `None` para métodos `abstract`/`native` (declarados con `;`).
    pub body: Option<Block>,
    pub is_constructor: bool,
    /// Anotaciones sobre usos de tipo anidados en el **tipo de retorno** (`List<@A X> m()`), con su
    /// `type_path`. Target `0x14`. Vacío si no hay.
    pub return_annos: Vec<TypeUseAnnot>,
    /// Anotaciones de tipo sobre cada tipo de la cláusula `throws` (`throws @A E1, @B E2`), **en
    /// paralelo** a `throws`. Target `0x17` (con el índice en la cláusula). Cada sublista puede estar
    /// vacía.
    pub throws_annos: Vec<Vec<TypeUseAnnot>>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Param {
    pub annotations: Vec<Annotation>,
    pub ty: Type,
    pub name: String,
    pub varargs: bool,
    pub is_final: bool,
    /// Anotaciones sobre usos de tipo anidados en el tipo del **parámetro** (`m(List<@A X> p)`), con
    /// su `type_path`. Target `0x16` (con el índice del parámetro). Vacío si no hay.
    pub type_annos: Vec<TypeUseAnnot>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Type {
    Void,
    Prim(PrimType),
    /// Un tipo referencia **crudo** por nombre (posiblemente cualificado, `java.lang.String`).
    /// Sin argumentos de tipo: `String`, o el uso *raw* de un genérico (`List`).
    Class(String),
    /// Un tipo **parametrizado**: `List<String>`, `Map<K, V>` (JLS §4.5). `base` es el nombre
    /// del genérico y `args` sus argumentos (nunca vacío — si no hay, es un [`Type::Class`]).
    Parameterized { base: String, args: Vec<TypeArg> },
    /// Un array de su tipo elemento (`T[]`).
    Array(Box<Type>),
    /// `var` — tipo inferido en un `local` (contextual: el parser lo produce al ver `var`).
    Var,
}

/// Un **argumento de tipo** (JLS §4.5.1): un tipo concreto o un *wildcard*.
#[derive(Debug, Clone, PartialEq)]
pub enum TypeArg {
    /// `List<String>` — un argumento concreto.
    Type(Type),
    /// `List<?>` — wildcard sin cota.
    Wildcard,
    /// `List<? extends Number>` — wildcard con cota **superior**.
    Extends(Box<Type>),
    /// `List<? super Integer>` — wildcard con cota **inferior**.
    Super(Box<Type>),
}

/// Un **parámetro de tipo** de una clase o método genérico (JLS §4.4): `<T extends A & B>`.
#[derive(Debug, Clone, PartialEq)]
pub struct TypeParam {
    /// Anotaciones sobre el parámetro (§9.7.4: `<@Foo T>`); se retienen aunque el emisor todavía no
    /// las escriba.
    pub annotations: Vec<Annotation>,
    pub name: String,
    /// Las cotas (`extends A & B`); vacío si no se declararon (cota implícita `Object`).
    pub bounds: Vec<Type>,
    /// Anotaciones de tipo sobre cada cota (`<T extends @A A & @B B>`), **en paralelo** a `bounds`.
    /// Target `0x11` (parámetro de clase) / `0x12` (de método), con `{type_parameter_index,
    /// bound_index}`. Cada sublista puede estar vacía.
    pub bound_annos: Vec<Vec<TypeUseAnnot>>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PrimType {
    Int,
    Long,
    Short,
    Byte,
    Char,
    Boolean,
    Float,
    Double,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Block(pub Vec<Stmt>);

/// Un `catch` de un `try`. `types` lleva más de uno en un *multi-catch* (`A | B`).
#[derive(Debug, Clone, PartialEq)]
pub struct CatchClause {
    pub types: Vec<Type>,
    pub name: String,
    pub body: Block,
    /// Decoración (pasada 2): el **slot** de la variable del `catch` en la frame. La variable
    /// está **siempre** definitivamente asignada (la JVM la deposita al entrar al handler);
    /// [`super::flow`] necesita su slot para saberlo. `None` hasta que corre la atribución.
    pub slot: Option<u16>,
    /// Si la variable es `final` — declarada así, o **implícitamente** en un *multi-catch*
    /// (`catch (A | B e)`, JLS §14.20): no se puede reasignar.
    pub is_final: bool,
}

/// A qué resolvió un nombre/llamada — la **decoración de binding** que escribe la pasada 2.
/// Distingue los casos que el codegen necesita separar: un local sale de la *frame* por su slot
/// (`iload`), un campo del *constant pool* (`getfield`), etc.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Binding {
    /// Una variable local o parámetro, en su **slot** de la frame (JVMS §2.6.1).
    Local { slot: u16 },
    /// Un campo, con su símbolo en la tabla (→ `getfield`/`getstatic`).
    Field(SymbolId),
    /// Un método resuelto (→ `invokevirtual`/`invokestatic`/`invokespecial`).
    Method(SymbolId),
    /// Un nombre de **tipo** (el receptor de un acceso estático, `Tipo.x`).
    Class(SymbolId),
}

/// La decoración de una **declaración de variable local**: el slot que se le asignó en la frame
/// y su tipo ya resuelto (necesario cuando se declaró con `var`).
#[derive(Debug, Clone, PartialEq)]
pub struct LocalInfo {
    pub slot: u16,
    pub ty: RType,
}

/// Una sentencia: su forma sintáctica (`kind`), su posición, y la decoración que agrega la
/// pasada 2 (`local`, en las que declaran una variable).
#[derive(Debug, Clone, PartialEq)]
pub struct Stmt {
    pub kind: StmtKind,
    pub pos: Pos,
    /// Decoración (pasada 2): el slot y el tipo resuelto de la variable que declara esta
    /// sentencia (`LocalVar`/`ForEach`); `None` en las demás.
    pub local: Option<LocalInfo>,
}

impl Stmt {
    /// Una sentencia recién parseada: **sin decorar** (la pasada 2 rellena `local`).
    pub fn new(pos: Pos, kind: StmtKind) -> Self {
        Stmt { kind, pos, local: None }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum StmtKind {
    /// `[final] Type name [= init];`. `type_annos` son las anotaciones sobre **usos de tipo** en el
    /// tipo del local (`@A int x`, `List<@A X> xs`), con su `type_path`. El emisor las escribe como
    /// target `0x40` (LOCAL_VARIABLE) en el `RuntimeVisibleTypeAnnotations` del `Code`, con el **rango
    /// de vida** del local como `target_info`. Vacío en los locales sintéticos del desugar.
    LocalVar { ty: Type, name: String, init: Option<Expr>, is_final: bool, type_annos: Vec<TypeUseAnnot> },
    /// `return [expr];`
    Return(Option<Expr>),
    /// Una expresión usada como sentencia (llamada, asignación, `++`…).
    Expr(Expr),
    If { cond: Expr, then: Box<Stmt>, els: Option<Box<Stmt>> },
    While { cond: Expr, body: Box<Stmt> },
    /// `for (init; cond; update...) body`
    For { init: Option<Box<Stmt>>, cond: Option<Expr>, update: Vec<Expr>, body: Box<Stmt> },
    /// `for ([final] Type name : iterable) body`
    ForEach { ty: Type, name: String, iterable: Expr, body: Box<Stmt>, is_final: bool },
    Block(Block),
    /// `synchronized (lock) { ... }` — el cuerpo es siempre un bloque (JLS §14.19).
    Synchronized { lock: Expr, body: Block },
    /// `do body while (cond);`
    Do { body: Box<Stmt>, cond: Expr },
    /// `assert cond;` o `assert cond : message;`
    Assert { cond: Expr, message: Option<Expr> },
    /// `try (resources) { body } catch (...) {...} finally {...}`. `resources` son las
    /// declaraciones del *try-with-resources* (vacío si no hay); `finally` es opcional.
    Try { resources: Vec<Stmt>, body: Block, catches: Vec<CatchClause>, finally: Option<Block> },
    /// `switch (selector) { cases }` como **sentencia**.
    Switch { selector: Expr, cases: Vec<SwitchCase> },
    /// `yield expr;` — produce el valor de un brazo de *switch expression*.
    Yield(Expr),
    /// `break;` o `break label;` — la etiqueta opcional nombra la sentencia de la que sale (§14.15).
    Break(Option<String>),
    /// `continue;` o `continue label;` — la etiqueta opcional nombra el bucle que reitera (§14.16).
    Continue(Option<String>),
    /// Una sentencia **etiquetada** `label: body` (§14.7). El `break`/`continue` con esa etiqueta la
    /// referencian; una etiqueta puede ir sobre cualquier sentencia, pero `continue label` solo es
    /// válido si nombra un bucle.
    Labeled { label: String, body: Box<Stmt> },
    Throw(Expr),
    Empty,
    /// Una **clase local** (§14.3): una declaración de tipo dentro del cuerpo de un método. Se
    /// parsea con la misma maquinaria que una anidada; entrarla a la tabla y compilarla (una clase
    /// sintética, con captura del entorno) son de una fase posterior.
    LocalClass(ClassDecl),
}

/// Una expresión: su forma sintáctica (`kind`), su posición, y la **decoración** que rellena la
/// pasada 2 — su tipo (`ty`) y a qué resolvió (`binding`). Como el `JCTree.type`/`sym` de javac.
#[derive(Debug, Clone, PartialEq)]
pub struct Expr {
    pub kind: ExprKind,
    pub pos: Pos,
    /// Decoración (pasada 2): el **tipo** de la expresión. `None` hasta que corre la atribución.
    pub ty: Option<RType>,
    /// Decoración (pasada 2): a qué **resolvió** el nombre/llamada/campo. `None` si no aplica
    /// (un literal no vincula a nada).
    pub binding: Option<Binding>,
    /// Anotaciones de tipo sobre el tipo **escrito** en una expresión de posición-Code: el destino de
    /// un `(@A T) e` (target `0x47`), un `e instanceof @A T` (`0x43`), un `new @A T(...)` (`0x44`) o un
    /// `new @A T[]` (`0x44`), con su `type_path`. El emisor las escribe en el `RuntimeVisibleType-
    /// Annotations` **dentro del `Code`**, con el offset del bytecode como `target_info`. Vacío en toda
    /// otra expresión (y en las sintéticas del desugar).
    pub type_annos: Vec<TypeUseAnnot>,
}

impl Expr {
    /// Una expresión recién parseada: **sin decorar** (la pasada 2 rellena `ty`/`binding`).
    pub fn new(pos: Pos, kind: ExprKind) -> Self {
        Expr { kind, pos, ty: None, binding: None, type_annos: Vec::new() }
    }

    /// La misma expresión con sus anotaciones de tipo de posición-Code adjuntas (ver [`Expr::type_annos`]).
    pub fn with_type_annos(mut self, type_annos: Vec<TypeUseAnnot>) -> Self {
        self.type_annos = type_annos;
        self
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum ExprKind {
    IntLit(i64),
    LongLit(i64),
    FloatLit(f32),
    DoubleLit(f64),
    CharLit(char),
    StringLit(String),
    BoolLit(bool),
    Null,
    /// Un nombre suelto (variable, campo o tipo, según resuelva la semántica).
    Name(String),
    This,
    Super,
    Binary { op: BinOp, lhs: Box<Expr>, rhs: Box<Expr> },
    /// Unario prefijo o postfijo (`prefix` distingue `++x` de `x++`).
    Unary { op: UnOp, expr: Box<Expr>, prefix: bool },
    Assign { op: AssignOp, target: Box<Expr>, value: Box<Expr> },
    Ternary { cond: Box<Expr>, then: Box<Expr>, els: Box<Expr> },
    /// `target.name(args)` o, si `target` es `None`, `name(args)`. `type_args` son los argumentos
    /// de tipo **explícitos** de un *type witness* (`Collections.<String>emptyList()`, §15.12): casi
    /// siempre vacío, y cuando no, **fijan** los parámetros de tipo del método en vez de inferirlos.
    Call { target: Option<Box<Expr>>, name: String, args: Vec<Expr>, type_args: Vec<TypeArg> },
    /// `expr.name` (acceso a campo).
    Field { expr: Box<Expr>, name: String },
    /// `array[index]`.
    Index { array: Box<Expr>, index: Box<Expr> },
    Cast { ty: Type, expr: Box<Expr> },
    /// `e instanceof T` o, con *pattern* (§14.30.2), `e instanceof T v`: `binding` es el nombre de la
    /// variable y `slot` su decoración (pasada 2), como en un `case T v`.
    InstanceOf { expr: Box<Expr>, ty: Type, binding: Option<String>, slot: Option<u16> },
    /// Un **literal de clase**, `C.class` (§15.8.2): su valor es el `Class<C>` del tipo.
    ClassLit(Type),
    /// `Outer.this` (§15.8.4): la **instancia envolvente** cualificada de una clase interna — el
    /// `this` de la clase `Type` que encierra a la actual. El desugar la reescribe a `this.this$0`.
    QualifiedThis(Type),
    /// `new Type(args)`, o con `body` una **clase anónima** (§15.9.5): `new Type(args) { members }`.
    /// El `body` son los miembros del cuerpo `{ … }`; `None` en el caso corriente. Una clase anónima
    /// no puede declarar constructores (§15.9.5.1), así que sus miembros son campos, métodos e
    /// inicializadores — nunca un `<init>` propio. Sigue el patrón de la lambda: se parsea, pero
    /// **compilarla** (una clase sintética anidada) es de una fase posterior.
    /// `outer` es la **instancia envolvente** cualificada de un `outer.new Inner(args)` (§15.9.2):
    /// se pasa como `this$0` de la interna en vez del `this` actual. `None` en el `new` corriente.
    NewObject { ty: Type, args: Vec<Expr>, body: Option<Vec<Member>>, outer: Option<Box<Expr>> },
    /// `new Elem[len]` o `new Elem[]{...}`. `dims` lleva las longitudes dadas (una por `[]`,
    /// `None` si el corchete va vacío); `init` es el `{...}` opcional.
    NewArray { elem: Type, dims: Vec<Option<Expr>>, init: Option<Vec<Expr>> },
    /// `switch (selector) { cases }` como **expresión** (produce un valor, con `yield`).
    Switch { selector: Box<Expr>, cases: Vec<SwitchCase> },
    /// Una **expresión lambda** (§15.27): `params -> body`. Los parámetros reutilizan [`Param`];
    /// uno **de tipo inferido** (`x ->`, sin anotación) lleva [`Type::Var`] — la semántica lo
    /// resuelve desde el *target type*, igual que un `var`. Es una *poly expression*: no tiene tipo
    /// propio hasta que un contexto le da una *functional interface* (pendiente de B2/B3).
    Lambda { params: Vec<Param>, body: Box<LambdaBody> },
    /// Una **referencia a método** (§15.13): `qualifier :: name`. `name` es el método, o `"new"`
    /// para una referencia a **constructor** (`ArrayList::new`, `int[]::new`). `type_args` son los
    /// de un *type witness* (`C::<String>m`), casi siempre vacío. Como una lambda, es una *poly
    /// expression* que se resuelve contra su *functional interface*.
    MethodRef { qualifier: Box<MethodRefQualifier>, name: String, type_args: Vec<TypeArg> },
    /// Un *call site* de `invokedynamic` ya **bajado** por el desugar (LambdaToMethod): la forma en
    /// que una lambda —y más adelante un *method ref*— llega al codegen. El emisor empuja `captures`
    /// y emite el `invokedynamic` cuyo *bootstrap* es `LambdaMetafactory.metafactory`; el `info`
    /// lleva ya armados los descriptores y el *method handle* de la implementación (§15.27 → §4.10.1.9).
    /// No lo produce el parser: solo existe entre el desugar y el codegen.
    Indy { info: Box<IndyCall>, captures: Vec<Expr> },
    /// Un **nodo de error**: el placeholder de una expresión que no parseó (recuperación a nivel
    /// expresión). El parser ya reportó el error de sintaxis y sigue; la atribución lo tipa
    /// [`RType::Unresolved`] **sin** emitir un error nuevo, para no encadenar diagnósticos derivados
    /// de una parte ya rota. Nunca llega al codegen: la compilación aborta si hubo errores.
    Error,
}

/// Los datos ya resueltos de un *call site* de `invokedynamic` (§4.7.23 / §5.4.3.6), que el desugar
/// calcula una vez y el codegen serializa sin recomputar nada. Es **genérico**: sirve para el
/// `LambdaMetafactory` de una lambda/*method ref* y para el `ObjectMethods` de un `record`, que solo
/// difieren en el *bootstrap method* y sus argumentos estáticos. Todo son descriptores/nombres
/// **internos** listos para el constant pool.
#[derive(Debug, Clone, PartialEq)]
pub struct IndyCall {
    /// El *invoked name* del call site (el nombre del SAM, o `equals`/`hashCode`/`toString`).
    pub name: String,
    /// El descriptor del call site: `(parámetros)Retorno`. Sus parámetros son lo que el emisor
    /// **empuja** antes del `invokedynamic` (las capturas de una lambda, o `this`/args de un `record`).
    pub descriptor: String,
    /// El *bootstrap method* — siempre `REF_invokeStatic`: la clase dueña, el nombre y su descriptor.
    pub bootstrap_owner: String,
    pub bootstrap_name: String,
    pub bootstrap_desc: String,
    /// Los **argumentos estáticos** del bootstrap (§4.7.23), en orden. El emisor traduce cada uno a
    /// su entrada del constant pool.
    pub bootstrap_args: Vec<BootstrapArg>,
}

/// Un argumento estático de un *bootstrap method* (§4.7.23): una constante *cargable* del pool. Solo
/// las variantes que necesitan lambda/*method ref* (`MethodType`, `MethodHandle`) y `record`
/// (`Class`, `Str` y los `MethodHandle` de los *getters*).
#[derive(Debug, Clone, PartialEq)]
pub enum BootstrapArg {
    /// Un `MethodType`, por su descriptor de método.
    MethodType(String),
    /// Un `MethodHandle`: su *reference kind* (§5.4.3.5), la clase dueña, el nombre y el descriptor
    /// (de método para los *invoke*, de campo para `REF_getField` y compañía).
    MethodHandle { kind: u8, owner: String, name: String, desc: String },
    /// Un `Class`, por su nombre interno.
    Class(String),
    /// Una constante `String`.
    Str(String),
}

/// El lado izquierdo de una referencia a método. La distinción tipo/expresión la resuelve la
/// semántica (§15.13.1: `String::length` mira `String` como tipo, `s::length` como valor), así que
/// el parser guarda **la categoría sintáctica que vio**: una expresión (`obj::m`,
/// `System.out::println`) o un tipo (el array de `int[]::new`, que no es ninguna expresión).
#[derive(Debug, Clone, PartialEq)]
pub enum MethodRefQualifier {
    Expr(Box<Expr>),
    Type(Type),
}

/// El cuerpo de una lambda: una **expresión** (`x -> x + 1`, un *yield* implícito) o un **bloque**
/// (`x -> { … }`, con `return` explícito). Son las dos formas del §15.27.2.
#[derive(Debug, Clone, PartialEq)]
pub enum LambdaBody {
    Expr(Box<Expr>),
    Block(Block),
}

/// Un grupo de un `switch`: sus etiquetas, un `guard` opcional (`when`), y su cuerpo (forma
/// flecha `->` o dos puntos `:`).
#[derive(Debug, Clone, PartialEq)]
pub struct SwitchCase {
    pub labels: Vec<CaseLabel>,
    pub is_default: bool,
    pub guard: Option<Expr>,
    pub body: SwitchBody,
}

#[derive(Debug, Clone, PartialEq)]
pub enum CaseLabel {
    /// Una constante: `case 1`, `case FOO`.
    Constant(Expr),
    /// Un *pattern*: `case Integer i` o `case Point(int x, int y)` (ver [`Pattern`]).
    Pattern(Pattern),
    /// `case null`.
    Null,
}

/// Un patrón de `case` (§14.30). Es **recursivo**: la deconstrucción de un `record` lleva adentro
/// los patrones de sus componentes, que a su vez pueden deconstruir.
#[derive(Debug, Clone, PartialEq)]
pub enum Pattern {
    /// `Type name` — un *type pattern*. `slot` es la decoración (pasada 2) de la variable en la
    /// frame: la necesita [`super::flow`] para saber que está asignada en el brazo, como la del
    /// `catch`. `None` hasta que corre la atribución.
    Type { ty: Type, name: String, slot: Option<u16> },
    /// `Rec(p1, p2, …)` — deconstrucción de un `record`: cada componente trae su propio patrón.
    Record { ty: Type, components: Vec<Pattern> },
}

#[derive(Debug, Clone, PartialEq)]
pub enum SwitchBody {
    /// `case X -> stmt` (una sentencia; en una expresión, el valor va en una `Expr`/`yield`).
    Arrow(Box<Stmt>),
    /// `case X: stmts...` hasta la próxima etiqueta.
    Colon(Vec<Stmt>),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BinOp {
    Or, And, // || &&
    BitOr, BitXor, BitAnd, // | ^ &
    Eq, Ne, // == !=
    Lt, Gt, Le, Ge, // < > <= >=
    Shl, Shr, UShr, // << >> >>>
    Add, Sub, // + -
    Mul, Div, Rem, // * / %
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum UnOp {
    Plus, Neg, // + -
    Not, BitNot, // ! ~
    Inc, Dec, // ++ -- (pre o post según el flag `prefix` del `Unary`)
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AssignOp {
    Assign, // =
    Add, Sub, Mul, Div, Rem, // += -= *= /= %=
    And, Or, Xor, // &= |= ^=
    Shl, Shr, UShr, // <<= >>= >>>=
}
