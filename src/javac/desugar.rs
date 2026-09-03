//! La pasada **Desugar** (B4): baja el **azúcar sintáctico** a construcciones más simples, para
//! que el codegen tenga menos casos que emitir. Es el `Lower.java` de javac.
//!
//! Transforma el AST **decorado in situ** (necesita los tipos de la pasada 2, p. ej. para saber
//! si el iterable de un `for-each` es un array o un `Iterable`). Cada reescritura produce nodos
//! **frescos** (sin decorar); en el pipeline real, el codegen los consume tras re-atribuir.
//!
//! ## Qué baja
//!
//! Azúcar de **sentencias**:
//! - **`for-each`** (JLS §14.14.2) → un `for` indexado sobre un array, o un `while` sobre un
//!   `Iterator` (con el cast que inserta la *erasure*).
//! - **`try`-with-resources** (§14.20.3) → un `try/finally` que llama a `close()`.
//! - **Inicializadores de instancia** `{ … }` (§8.6/§12.5) → se copian al frente del cuerpo de cada
//!   constructor. Uno que delegue en `this(...)` **no** los recibe: los corre aquel.
//! - **`assert`** (§14.10) → `if (!$assertionsDisabled && !cond) throw new AssertionError(msg)`, más
//!   el campo sintético `static final boolean $assertionsDisabled` y el `<clinit>` que lo calcula de
//!   `C.class.desiredAssertionStatus()`. El *guard* es lo que hace que una aserción **no cueste nada**
//!   cuando están deshabilitadas (el default): al ser un `static final`, el JIT lo pliega.
//! - **`switch`-expresión** (§15.28) en posición de **cola** (`T v = switch…`, `return switch…`,
//!   `x = switch…`, `yield switch…`) → una `switch`-**sentencia** cuyos brazos **asignan** el valor a
//!   la variable (con un temporal cuando hace falta). Los brazos con **bloque** o de **dos puntos**
//!   con `yield` —aun adentro de un bucle— se bajan con un `break` **etiquetado** sobre el switch
//!   generado. Quedan como expresión para el codegen el exhaustivo **sin `default`** y las
//!   **embebidas** (`foo(switch…)`, condición de bucle), donde hoistear rompería la reevaluación.
//! - **`switch` sobre `String`** (§14.11) → **dos** switches sobre `int`: uno mapea `s.hashCode()` a
//!   un índice sintético (con `equals()` para desambiguar colisiones de hash), el otro replica los
//!   brazos indexados por ese entero — preservando *fall-through* y `default`. Un `case null` se
//!   rutea **antes** de tocar el hash; sin él, un selector nulo revienta en `hashCode()`, que es
//!   exactamente la semántica de Java.
//! - **`switch` sobre `enum`** (§14.11) → un switch sobre el `$SwitchMap`: un `int[]` sintético
//!   indexado por `ordinal()`, alojado en una clase anidada **`C$1`** y poblado en un `<clinit>` con
//!   un `try/catch NoSuchFieldError` por constante (**fiel a javac**: así sobrevive a que el enum se
//!   recompile con las constantes reordenadas).
//! - **`switch` con *patterns*** (§14.11.1) → una cadena de `instanceof` en un bloque
//!   **etiquetado**. Cada brazo es un `if` **suelto** (no `else if`) para que una **guarda que falla**
//!   caiga al `case` siguiente; el binding se materializa con un cast, se sale con `break` etiquetado
//!   —omitido si el brazo ya retorna, para no generar código inalcanzable— y un selector `null` sin
//!   `case null` lanza **NPE**. Vale también como **expresión**: la switch-expresión se convierte
//!   primero en sentencia que asigna, y esta pasada la baja después. La **deconstrucción de records**
//!   (`case Point(int x, int y)`) extrae cada componente por su *accessor* (`$r.x()`) y es
//!   **recursiva**: un componente puede volver a deconstruir.
//!
//! **Inicializadores de campo** (§8.6/§12.4.2): `int v = 7;` no es algo que el emisor pueda emitir
//! donde está —un campo se declara en la sección de campos del `.class` y nada más—, así que se
//! convierten en sentencias, unificadas **en orden de fuente** con los bloques `{ }` / `static { }`:
//! las de instancia al frente de cada constructor, las estáticas al `<clinit>`.
//!
//! Azúcar de **expresiones**:
//! - **Concatenación de `String`** (§15.18.1) `a + b` → `new StringBuilder().append(a).append(b).toString()`.
//! - **Asignación compuesta** (§15.26.2) `x op= y` → `x = (T)(x op y)`.
//! - **Incremento/decremento** en descarte (§15.14.2/§15.15.2) `x++`/`--x` como sentencia → `x += 1`
//!   (donde el valor no se usa, pre y post coinciden; se reduce a la asignación compuesta).
//! - **Varargs** en el call site (§15.12.4.2) `f(1, 2, 3)` → `f(new int[]{1, 2, 3})`.
//!
//! Y una reescritura que no es azúcar sino **preparación para el emisor**:
//! - **`synchronized (e)`** (§14.19) → `{ Object $lock = e; synchronized ($lock) { … } }`. El monitor
//!   se suelta en **dos** lugares del bytecode (la salida normal y el handler que re-lanza), y las dos
//!   necesitan la *misma* referencia: reevaluar `e` tomaría y soltaría objetos distintos. Copiarla acá
//!   —antes de la re-atribución, que le asigna su slot— le evita al emisor tener que inventarse un
//!   temporal sin saber qué slots usa el resto del método.
//!
//! ## Qué falta
//!
//! Además de bajar azúcar, esta pasada **materializa** los miembros implícitos de un `record`
//! (§8.10.3): un campo `private final` por componente, el **constructor canónico** y un *accessor*
//! por componente — respetando lo que la persona haya declarado a mano. Sus **símbolos** ya venían de
//! `enter` (por eso `p.x()` resolvía), pero sin cuerpo en el AST el `.class` los referenciaba sin
//! declararlos, y la deconstrucción de patterns llamaba a un método inexistente.
//!
//! Y los miembros implícitos de un **`enum`** (§8.9.3): las constantes como campos, el `$VALUES`
//! que las junta, el constructor privado `(String, int)` —lo único que puede darle a `java.lang.Enum`
//! su nombre y su ordinal— y `values()`/`valueOf()`. `values()` es `return (E[]) $VALUES.clone();`
//! **igual que javac** (el emisor y la VM saben `array.clone()`, §10.7). Queda **un** desvío
//! deliberado, no semántico: `valueOf` compara contra los nombres literales acá adentro en vez de
//! delegar en `Enum.valueOf(Class, String)`, que va por reflexión sobre el `$VALUES` de otra clase.
//!
//! Lo que queda:
//! - Un `enum` cuyas constantes lleven **argumentos** (`ROJO("rojo")`) o que declare su propio
//!   constructor: javac reescribe la firma insertándole `(String, int)` adelante, y eso pide
//!   actualizar también el símbolo que `enter` ya registró, no solo el AST.
//! - El `equals`/`hashCode`/`toString` de un `record` — van por `invokedynamic` + `ObjectMethods`.
//! - En la deconstrucción, un componente con un tipo **más angosto** que el declarado todavía no se
//!   re-testea con `instanceof` (se bindea directo, que cubre el caso habitual).
//! - Las **clases internas** (captura de `this$0`, métodos `access$`) — no es azúcar pendiente sino
//!   una feature de lenguaje que el front-end todavía no soporta.
//!
//! Ojo con una asimetría: varias de estas reescrituras producen construcciones que el **codegen
//! todavía no emite** (`instanceof` de los patterns, `new int[]` de los varargs, el indexado del
//! `$SwitchMap`). Desde que el emisor tiene barrera, eso **falla con un error** en vez de salir como
//! un `.class` roto — pero el desugar va por delante del back-end.
//!
//! **No** son de esta pasada: el **boxing/unboxing** y los casts de *erasure* son dirigidos por tipo
//! (el `TransTypes` de javac); las **lambdas**/*method refs* van por `invokedynamic` en la suya; y la
//! síntesis de los miembros de `enum`/`record` (`values()`, accessors, `equals`/`hashCode`) es
//! síntesis, no reescritura. Que la concatenación baje a `StringBuilder` y no a `invokedynamic` es
//! una **decisión**, no una carencia.
//!
//! Tampoco son de acá el `++`/`--` **en posición de valor** ni la asignación compuesta sobre un
//! *lvalue* con **efectos** (`a[i()] += 1`): son juego de pila y los resuelve el codegen (el primero
//! ya lo hace con `iinc`). Y el *inlining* del **`finally`** —duplicarlo en la salida normal y en un
//! handler *catch-all*, porque la v69 ya no acepta `jsr`/`ret`— resultó ser también del **codegen**,
//! donde ya está: no es una reescritura de AST.

use std::collections::{BTreeMap, HashMap, HashSet};

use super::ast::{
    AssignOp, Binding, BinOp, BootstrapArg, Block, CaseLabel, CatchClause, ClassDecl,
    CompilationUnit, Expr, ExprKind, FieldDecl, IndyCall, LambdaBody, Member, MethodDecl,
    MethodRefQualifier, Modifier, Param, Pattern, Pos, PrimType, Stmt, StmtKind, SwitchBody,
    SwitchCase, Type, TypeKind, TypeParam, UnOp,
};
use super::attribute::{candidates, constructors, functional_sam};
use super::codegen::{concat_needs_value_of, internal_name, rtype_desc};
use super::symbol::{
    ParamSig, RType, Resolved, ScopeId, Symbol, SymbolId, SymbolKind, SymbolTable,
};
use super::types;

/// El *reference kind* de un `MethodHandle` (§5.4.3.5). En una lambda la implementación es
/// `invokestatic` cuando no captura `this` e `invokespecial` (método privado de instancia) cuando sí;
/// un *method ref* usa además `invokevirtual`/`invokeinterface` (instancia) o `newInvokeSpecial`
/// (constructor).
const REF_GET_FIELD: u8 = 1;
const REF_INVOKE_VIRTUAL: u8 = 5;
const REF_INVOKE_STATIC: u8 = 6;
const REF_INVOKE_SPECIAL: u8 = 7;
const REF_NEW_INVOKE_SPECIAL: u8 = 8;
const REF_INVOKE_INTERFACE: u8 = 9;

/// El descriptor del *bootstrap method* `LambdaMetafactory.metafactory` (§ de
/// `java.lang.invoke.LambdaMetafactory`): recibe el `Lookup`, el nombre, el tipo del call site y los
/// tres argumentos estáticos, y devuelve un `CallSite`.
const METAFACTORY_DESC: &str = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

/// El descriptor de `ObjectMethods.bootstrap` (§ de `java.lang.runtime.ObjectMethods`): tras los tres
/// argumentos que aporta la VM (`Lookup`, nombre, tipo), toma la `Class` del record, la cadena de
/// nombres de componentes y un `MethodHandle` *getter* por componente (varargs), y devuelve un `Object`.
const OBJECT_METHODS_DESC: &str = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;";

/// El descriptor de `StringConcatFactory.makeConcatWithConstants` (§ de
/// `java.lang.invoke.StringConcatFactory`): tras los tres argumentos que aporta la VM (`Lookup`,
/// nombre, tipo del call site), toma la *receta* (`String`) y los argumentos constantes (varargs
/// `Object[]`), y devuelve un `CallSite`.
const STRING_CONCAT_DESC: &str = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;";

/// El marcador de un **argumento ordinario** (dinámico) en la receta de `makeConcatWithConstants`
/// (`TAG_ARG` de `StringConcatFactory`). Cada `` consume un operando empujado, en orden.
const CONCAT_TAG_ARG: char = '\u{0001}';
/// El marcador de una **constante** aportada como argumento estático del bootstrap (`TAG_CONST`).
/// Aquí no lo emitimos —las constantes se embeben literalmente en la receta— pero un literal que lo
/// contenga debe tratarse como dinámico para no corromper la receta.
const CONCAT_TAG_CONST: char = '\u{0002}';

/// Baja el azúcar de `unit`, transformándolo en el lugar. Necesita la tabla para reconocer
/// tipos (p. ej. si un `+` es concatenación de `String`).
pub fn desugar(unit: &mut CompilationUnit, table: &mut SymbolTable) {
    // El **paquete** es el prefijo del FQN, igual que en `enter` (§7.1): sin él, la búsqueda
    // `table.class(fqn)` de la síntesis de miembros (enum/record) fallaba para un tipo en un paquete
    // nombrado —`qualify` solo agrega las clases envolventes, no el paquete—, y el `enum`/`record`
    // salía **degenerado** (sin constantes, `values()`, `valueOf()`, ni el ctor propio).
    let base = unit.package.as_deref().unwrap_or("").to_string();
    let mut enums = HashMap::new();
    collect_enums(table, &unit.types, &base, &mut enums);
    let mut records = HashMap::new();
    collect_records(table, &unit.types, &base, &mut records);
    let unit_name = unit.types.first().map(|c| c.name.clone()).unwrap_or_else(|| "Switch".into());
    let top_scope = top_level_scope(table, &unit.types);
    // Los valores de las `static final int` con inicializador **constante** (§15.29), para plegar una
    // referencia a una de ellas usada como etiqueta de `case`.
    let mut consts = HashMap::new();
    collect_constants(table, &unit.types, &base, &mut consts);
    let mut d = Desugarer {
        counter: 0,
        table,
        enums,
        records,
        consts,
        holder: None,
        unit_name,
        top_scope,
        needs_assert_guard: false,
        lambda_methods: Vec::new(),
        cur_class: None,
        cur_method: String::new(),
        lambda_ordinal: 0,
        cur_method_sig: None,
        cur_method_type_params: Vec::new(),
        has_this: false,
        enclosing_type: None,
        cur_fqn: String::new(),
        lifted_locals: Vec::new(),
        captured_locals: HashMap::new(),
        local_new_args: HashMap::new(),
        local_uses_this: HashSet::new(),
        enclosing_store: false,
    };
    for ty in &mut unit.types {
        d.class(ty, &base);
    }
    // La clase sintética `C$1` (si algún `switch` sobre `enum` la creó) se agrega como tipo del final.
    if let Some(h) = d.holder.take() {
        unit.types.push(holder_class(h));
    }
}

struct Desugarer<'a> {
    /// Contador para nombres de variables sintéticas (`$a1`, `$it2`…), únicos en la unidad.
    counter: u32,
    /// **Mutable**: el desugar sintetiza símbolos (el campo `$SwitchMap` de un `switch` sobre `enum`),
    /// que las pasadas posteriores tienen que ver al re-atribuir.
    table: &'a mut SymbolTable,
    /// `SymbolId` de cada `enum` → nombres de sus constantes **en orden** (para mapear a su ordinal).
    enums: HashMap<SymbolId, Vec<String>>,
    /// `SymbolId` de cada `record` → nombres de sus componentes **en orden**, que son los de sus
    /// *accessors* (los necesita la deconstrucción de un pattern).
    records: HashMap<SymbolId, Vec<String>>,
    /// `SymbolId` de cada campo `static final int` con inicializador constante → su valor plegado
    /// (§15.29), para resolver una referencia a una constante usada como etiqueta de `case`.
    consts: HashMap<SymbolId, i32>,
    /// La clase sintética que aloja los `$SwitchMap$X` (una por unidad), creada perezosamente.
    holder: Option<Holder>,
    /// Nombre base de la clase sintética (`<primerTipo>$1`, estilo `C$1` de javac).
    unit_name: String,
    /// El scope donde viven los tipos **top-level** (el del paquete): ahí registramos la clase
    /// sintética para que resuelva por nombre igual que los demás tipos.
    top_scope: ScopeId,
    /// Si la clase que se está recorriendo bajó algún `assert` y necesita su `$assertionsDisabled`.
    needs_assert_guard: bool,
    /// Los métodos sintéticos `lambda$…` que produce el bajado de lambdas de la clase en curso. Se
    /// acumulan mientras se recorren sus miembros (no se puede mutar `class.members` en pleno
    /// recorrido) y se agregan al terminar.
    lambda_methods: Vec<Member>,
    /// La clase cuyos miembros se recorren: dueña de los métodos sintéticos y el tipo de su `this`.
    cur_class: Option<SymbolId>,
    /// Nombre del método/inicializador envolvente, para bautizar `lambda$<envolvente>$<n>`.
    cur_method: String,
    /// El `<n>` de `lambda$<envolvente>$<n>`: un ordinal que javac **reinicia por método envolvente**
    /// (la primera lambda de cada método es `$0`), no un contador de clase. Se pone a cero al entrar a
    /// cada método/inicializador y se salva/restaura alrededor de un tipo anidado (una clase local
    /// tiene su propia numeración por método).
    lambda_ordinal: u32,
    /// `(nombre, descriptor)` del método/constructor envolvente, o `None` si el punto actual está en
    /// un **inicializador** (o fuera de un método). Lo lee `lift_local_class` para el atributo
    /// `EnclosingMethod` (§4.7.7) de la local/anónima que levanta.
    cur_method_sig: Option<(String, String)>,
    /// Los parámetros de tipo del método envolvente. El método sintético de una lambda los
    /// hereda: sin ellos, un parámetro suyo tipado `T` no resuelve al emitir el descriptor y cae
    /// a `Object`, que no es la *erasure* de una `T` con cota (#282).
    cur_method_type_params: Vec<TypeParam>,
    /// Si en el punto actual hay `this` disponible (falso en `static`): una lambda solo puede
    /// **capturar** `this` —y su implementación ser un método de instancia— cuando lo hay.
    has_this: bool,
    /// La clase que encierra a la que se recorre, **si esta es una interna de instancia** (inner
    /// class no-estática): el tipo de su campo sintético `this$0` y por donde resuelven sus miembros
    /// capturados. `None` en top-level, anidadas estáticas, interfaces/enum/record.
    enclosing_type: Option<SymbolId>,
    /// El FQN de la clase en curso (`Outer` / `Outer.Inner`), para levantar una clase **local** a
    /// tipo anidado del enclosing (§14.3).
    cur_fqn: String,
    /// Las clases **locales** ya levantadas de la clase en curso: se agregan a sus miembros al
    /// terminar el recorrido (como los métodos `lambda$…`).
    lifted_locals: Vec<Member>,
    /// Los **locales capturados** por la clase local que se está procesando (`nombre → tipo`): dentro
    /// de su cuerpo, un `Name` a uno de ellos se reescribe a `this.val$nombre`.
    captured_locals: HashMap<String, RType>,
    /// Por cada clase local, los nombres de los locales que captura **en orden**: el `new L(...)` de
    /// su sitio de uso los empuja como argumentos de cabecera.
    local_new_args: HashMap<String, Vec<String>>,
    /// Las clases locales que **además** capturan la instancia envolvente (`this$0`): su `new L(...)`
    /// empuja `this` de cabecera (antes de los `val$`), y su ctor lo lleva de primer parámetro.
    local_uses_this: HashSet<String>,
    /// Si la clase interna/local/anónima que se va a recorrer **almacena** la instancia envolvente en
    /// un campo `this$0` (porque realmente la usa). Lo fija cada sitio que fija `enclosing_type`, y lo
    /// lee `capture_enclosing_instance`: el **parámetro** de captura va siempre (en contexto de
    /// instancia), pero el **campo** solo si se usa —javac lo omite y solo hace el `requireNonNull`—.
    enclosing_store: bool,
}

/// El scope que contiene los tipos top-level (el del paquete) — el `enclosing` del scope de miembros
/// de cualquier tipo top-level. Necesario para registrar la clase sintética en el lugar correcto.
/// La interfaz que **declara** el SAM. No siempre es la que la lambda instancia: `BinaryOperator<T>`
/// hereda su `apply` de `BiFunction`, y es contra *esa* que hay que sustituir.
fn sam_owner(table: &SymbolTable, sam: SymbolId) -> SymbolId {
    table.symbol(sam).owner.unwrap_or(sam)
}

fn top_level_scope(table: &SymbolTable, types: &[ClassDecl]) -> ScopeId {
    types
        .first()
        .and_then(|c| table.class(&c.name))
        .and_then(|cid| match &table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => table.scope(*members).enclosing,
            _ => None,
        })
        .unwrap_or(table.global)
}

/// La clase anidada **sintética** (estilo `C$1` de javac) que aloja los arrays `$SwitchMap$Enum` y su
/// `<clinit>` de población. Se construye perezosamente al ver el primer `switch` sobre `enum`.
struct Holder {
    cid: SymbolId,
    scope: ScopeId,
    name: String,
    members: Vec<Member>,
    /// `SymbolId` del enum → nombre del campo `$SwitchMap$Enum` ya creado (idempotencia).
    maps: HashMap<SymbolId, String>,
}

/// Recolecta los `enum` (por `SymbolId`) con sus constantes en orden de declaración, recorriendo los
/// tipos y sus anidados. Es lo que deja mapear `case ROJO` → ordinal de `ROJO`.
fn collect_enums(
    table: &SymbolTable,
    types: &[ClassDecl],
    enclosing: &str,
    out: &mut HashMap<SymbolId, Vec<String>>,
) {
    for c in types {
        let fqn = qualify(enclosing, &c.name);
        if c.kind == TypeKind::Enum {
            if let Some(id) = table.class(&fqn) {
                out.insert(id, c.enum_constants.iter().map(|e| e.name.clone()).collect());
            }
        }
        for m in &c.members {
            if let Member::Type(nested) = m {
                collect_enums(table, std::slice::from_ref(nested), &fqn, out);
            }
        }
    }
}

/// Recolecta el valor de cada campo `static final int` con inicializador **constante** (§15.29).
/// Itera a **punto fijo**: una constante puede referirse a otra ya definida (`B = A + 1`), así que se
/// repite mientras se pliegue alguna nueva.
fn collect_constants(table: &SymbolTable, types: &[ClassDecl], base: &str, out: &mut HashMap<SymbolId, i32>) {
    let mut fields: Vec<(SymbolId, ScopeId, &Expr)> = Vec::new();
    collect_const_fields(table, types, base, &mut fields);
    loop {
        let mut changed = false;
        for &(fid, scope, init) in &fields {
            if out.contains_key(&fid) {
                continue;
            }
            if let Some(v) = fold_const_int(table, scope, out, init) {
                out.insert(fid, v);
                changed = true;
            }
        }
        if !changed {
            break;
        }
    }
}

/// Junta `(símbolo del campo, scope de su clase, inicializador)` de cada `static final` de tipo
/// entero, recursivo por los tipos anidados.
fn collect_const_fields<'a>(
    table: &SymbolTable,
    types: &'a [ClassDecl],
    enclosing: &str,
    out: &mut Vec<(SymbolId, ScopeId, &'a Expr)>,
) {
    for class in types {
        let fqn = qualify(enclosing, &class.name);
        if let Some(cid) = table.class(&fqn) {
            let scope = member_scope_id(table, cid);
            for m in &class.members {
                match m {
                    Member::Field(f)
                        if f.modifiers.contains(&Modifier::Static)
                            && f.modifiers.contains(&Modifier::Final)
                            && matches!(
                                f.ty,
                                Type::Prim(
                                    PrimType::Int | PrimType::Char | PrimType::Short | PrimType::Byte
                                )
                            ) =>
                    {
                        if let Some(init) = &f.init {
                            if let Some(fid) = field_symbol(table, scope, &f.name) {
                                out.push((fid, scope, init));
                            }
                        }
                    }
                    Member::Type(nested) => {
                        collect_const_fields(table, std::slice::from_ref(nested), &fqn, out)
                    }
                    _ => {}
                }
            }
        }
    }
}

/// Evalúa una **expresión constante entera** (§15.28/§15.29): literales, `char`, unarios (`-`/`+`/`~`),
/// *cast*, aritmética/bit/desplazamiento entre constantes, y una **referencia a una `static final
/// int`** (`MAX` o `C.MAX`) ya plegada en `consts`. `None` si no es constante. Aritmética `i32` con
/// *wrapping*, como Java.
fn fold_const_int(
    table: &SymbolTable,
    scope: ScopeId,
    consts: &HashMap<SymbolId, i32>,
    e: &Expr,
) -> Option<i32> {
    match &e.kind {
        ExprKind::IntLit(n) => i32::try_from(*n).ok(),
        ExprKind::CharLit(c) => Some(*c as i32),
        ExprKind::Unary { op, expr, .. } => {
            let v = fold_const_int(table, scope, consts, expr)?;
            match op {
                UnOp::Neg => Some(v.wrapping_neg()),
                UnOp::Plus => Some(v),
                UnOp::BitNot => Some(!v),
                _ => None,
            }
        }
        ExprKind::Cast { expr, .. } => fold_const_int(table, scope, consts, expr),
        ExprKind::Binary { op, lhs, rhs } => {
            let a = fold_const_int(table, scope, consts, lhs)?;
            let b = fold_const_int(table, scope, consts, rhs)?;
            binary_const(*op, a, b)
        }
        // Referencia **sin cualificar** a una constante de esta clase (o de un supertipo por scope).
        ExprKind::Name(n) => consts.get(&field_symbol(table, scope, n)?).copied(),
        // `C.MAX`: se resuelve `C` a su clase y se busca `MAX` en su scope de miembros.
        ExprKind::Field { expr, name } => {
            let ExprKind::Name(cn) = &expr.kind else { return None };
            let cid = table.resolve_type(scope, cn)?;
            let fid = field_symbol(table, member_scope_id(table, cid), name)?;
            consts.get(&fid).copied()
        }
        _ => None,
    }
}

fn binary_const(op: BinOp, a: i32, b: i32) -> Option<i32> {
    use BinOp::*;
    Some(match op {
        Add => a.wrapping_add(b),
        Sub => a.wrapping_sub(b),
        Mul => a.wrapping_mul(b),
        Div if b != 0 => a.wrapping_div(b),
        Rem if b != 0 => a.wrapping_rem(b),
        BitAnd => a & b,
        BitOr => a | b,
        BitXor => a ^ b,
        Shl => a.wrapping_shl((b & 31) as u32),
        Shr => a.wrapping_shr((b & 31) as u32),
        UShr => ((a as u32).wrapping_shr((b & 31) as u32)) as i32,
        _ => return None,
    })
}

/// El scope de miembros de una clase por su `SymbolId` (versión libre de `member_scope_of`).
fn member_scope_id(table: &SymbolTable, cid: SymbolId) -> ScopeId {
    match &table.symbol(cid).kind {
        SymbolKind::Class { members, .. } => *members,
        _ => 0,
    }
}

/// El `SymbolId` del **campo** `name` en `scope` (salta un método/tipo homónimo).
fn field_symbol(table: &SymbolTable, scope: ScopeId, name: &str) -> Option<SymbolId> {
    table
        .scope(scope)
        .get(name)
        .iter()
        .copied()
        .find(|&id| matches!(table.symbol(id).kind, SymbolKind::Field { .. }))
}

/// Recolecta los `record` (por `SymbolId`) con los nombres de sus componentes en orden — que son los
/// de sus *accessors*. Es lo que deja bajar `case Point(int x, int y)` a `$r.x()` / `$r.y()`.
fn collect_records(
    table: &SymbolTable,
    types: &[ClassDecl],
    enclosing: &str,
    out: &mut HashMap<SymbolId, Vec<String>>,
) {
    for c in types {
        let fqn = qualify(enclosing, &c.name);
        if c.kind == TypeKind::Record {
            if let Some(id) = table.class(&fqn) {
                out.insert(id, c.components.iter().map(|p| p.name.clone()).collect());
            }
        }
        for m in &c.members {
            if let Member::Type(nested) = m {
                collect_records(table, std::slice::from_ref(nested), &fqn, out);
            }
        }
    }
}

/// Los miembros **implícitos** de un `record` (§8.10.3): un campo `private final` por componente, el
/// **constructor canónico** que los asigna, y un *accessor* por componente. Se saltea cualquiera que
/// la persona haya declarado a mano — la declaración explícita gana (§8.10.4).
///
/// El `equals`/`hashCode`/`toString` no se generan: van por `invokedynamic` + `ObjectMethods`, que es
/// otra pasada.
fn record_members(class: &ClassDecl) -> Vec<Member> {
    let declared: Vec<&str> = class
        .members
        .iter()
        .filter_map(|m| match m {
            Member::Method(me) if !me.is_constructor && me.params.is_empty() => Some(me.name.as_str()),
            _ => None,
        })
        .collect();
    let has_ctor = class.members.iter().any(|m| matches!(m, Member::Method(me) if me.is_constructor));

    let mut out = Vec::new();
    for c in &class.components {
        out.push(Member::Field(FieldDecl {
            doc: None,
            annotations: Vec::new(),
            type_annos: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Private, Modifier::Final],
            ty: c.ty.clone(),
            name: c.name.clone(),
            init: None,
        }));
    }
    // `Point(int x, int y) { this.x = x; this.y = y; }`
    if !has_ctor {
        let body = Block(
            class
                .components
                .iter()
                .map(|c| {
                    let field = ex(ExprKind::Field {
                        expr: Box::new(ex(ExprKind::This)),
                        name: c.name.clone(),
                    });
                    st(StmtKind::Expr(assign_expr(field, name(&c.name))))
                })
                .collect(),
        );
        out.push(Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Public],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: Type::Void,
            name: class.name.clone(),
            params: class.components.clone(),
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(body),
            is_constructor: true,
        }));
    }
    // `public int x() { return this.x; }`
    for c in &class.components {
        if declared.contains(&c.name.as_str()) {
            continue;
        }
        let body = Block(vec![st(StmtKind::Return(Some(ex(ExprKind::Field {
            expr: Box::new(ex(ExprKind::This)),
            name: c.name.clone(),
        }))))]);
        out.push(Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Public],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: c.ty.clone(),
            name: c.name.clone(),
            params: Vec::new(),
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(body),
            is_constructor: false,
        }));
    }
    out
}

/// ¿El constructor arranca delegando en otro (`this(...)`)? En ese caso los inicializadores de
/// instancia no se le copian: los corre el constructor al que delega.
/// Los **inicializadores de campo** se vuelven sentencias, unificados con los bloques de
/// inicialización (§8.6/§12.4.2).
///
/// `int v = 7;` no es algo que el emisor pueda emitir donde está: un campo se declara en la sección
/// de campos del `.class` y **nada más**. Su valor lo pone el constructor (o el `<clinit>` si es
/// `static`), y tiene que correr **en orden de fuente**, intercalado con los bloques `{ }` /
/// `static { }` — de ahí que se junten los dos en un solo recorrido y no por separado.
fn hoist_initializers(class: &mut ClassDecl, consts: &super::codegen::ConstFieldMap) {
    let mut statics: Vec<Stmt> = Vec::new();
    let mut instances: Vec<Stmt> = Vec::new();
    for member in &mut class.members {
        match member {
            Member::Field(f) => {
                // **Finding #124**: acá se miraban los modificadores **declarados**, y en una interfaz
                // vienen vacíos — JLS §9.3 da `public static final` por implícitos. El campo se tomaba
                // entonces por uno **de instancia**, se iba a `instances`, y más abajo eso sintetizaba
                // un **constructor sobre la interfaz**: un `<init>()V` ilegal, emitido además como
                // `default`, que arranca con `aload_0; invokespecial Object.<init>` sobre un `this` que
                // no puede existir, y que nadie llama — así que el campo quedaba sin asignar.
                //
                // Con los modificadores implícitos, un campo de interfaz es estático y su inicializador
                // se va al `<clinit>`, que es donde manda JVMS §2.9.2.
                let mods = super::enter::implicit_field_mods(class.kind, &f.modifiers);
                let is_static = mods.contains(&Modifier::Static);
                let is_final = mods.contains(&Modifier::Final);
                // Un `static final` con inicializador de expresión constante (§15.29) **no** se baja al
                // `<clinit>`: su valor va al atributo `ConstantValue` del campo, que la JVM asigna al
                // preparar la clase. Se deja `f.init` en su lugar para que el codegen lo lea y emita.
                if is_static
                    && is_final
                    && f.init.as_ref().is_some_and(|init| {
                        super::codegen::const_field_value(&f.ty, init, consts).is_some()
                    })
                {
                    continue;
                }
                let Some(init) = f.init.take() else { continue };
                // Un campo de instancia se escribe por `this`; uno estático, por su nombre.
                let target = if is_static {
                    name(&f.name)
                } else {
                    ex(ExprKind::Field {
                        expr: Box::new(ex(ExprKind::This)),
                        name: f.name.clone(),
                    })
                };
                let stmt = st(StmtKind::Expr(assign_expr(target, init)));
                if is_static {
                    statics.push(stmt);
                } else {
                    instances.push(stmt);
                }
            }
            Member::StaticInit(b) => statics.append(&mut b.0),
            Member::InstanceInit(b) => instances.append(&mut b.0),
            _ => {}
        }
    }
    class.members.retain(|m| !matches!(m, Member::StaticInit(_) | Member::InstanceInit(_)));
    if !statics.is_empty() {
        class.members.push(Member::StaticInit(Block(statics)));
    }
    if !instances.is_empty() {
        // Sin ningún constructor donde meterlos, hace falta uno: el por defecto que el emisor
        // sintetizaría es `super(); return`, y ahí los inicializadores se perderían.
        //
        // **Nunca en una interfaz** (finding #124): no puede declarar `<init>`, y con los
        // modificadores implícitos aplicados arriba ya no debería llegar nada a `instances` desde una
        // — todos sus campos son estáticos. La guarda deja el invariante escrito en vez de confiado.
        let es_interfaz = matches!(class.kind, TypeKind::Interface | TypeKind::Annotation);
        if !es_interfaz
            && !class.members.iter().any(|m| matches!(m, Member::Method(me) if me.is_constructor))
        {
            class.members.push(Member::Method(MethodDecl {
                doc: None,
                annotations: Vec::new(),
                pos: Pos::default(),
                modifiers: vec![Modifier::Public],
                type_params: Vec::new(),
                return_annos: Vec::new(),
                return_type: Type::Void,
                name: class.name.clone(),
                params: Vec::new(),
                throws: Vec::new(),
                throws_annos: Vec::new(),
                body: Some(Block(Vec::new())),
                is_constructor: true,
            }));
        }
        class.members.push(Member::InstanceInit(Block(instances)));
    }
}

fn delegates_to_this(body: &Block) -> bool {
    matches!(
        body.0.first().map(|s| &s.kind),
        Some(StmtKind::Expr(e)) if matches!(&e.kind, ExprKind::Call { name, .. } if name == "this")
    )
}

/// ¿El cuerpo arranca con un `super(...)` **escrito**? (finding #342)
///
/// Importa para saber **dónde** meter los inicializadores de instancia: van después de esa llamada,
/// no antes. Ponerlos antes los dejaba primeros en el cuerpo, el emisor ya no veía un `super()` al
/// frente y le anteponía el implícito — con lo que la superclase se inicializaba **dos veces**, la
/// primera por su constructor sin argumentos, que puede no existir o hacer algo muy distinto.
fn delegates_to_super(body: &Block) -> bool {
    matches!(
        body.0.first().map(|s| &s.kind),
        Some(StmtKind::Expr(e)) if matches!(&e.kind, ExprKind::Call { name, .. } if name == "super")
    )
}

fn qualify(enclosing: &str, name: &str) -> String {
    if enclosing.is_empty() { name.to_string() } else { format!("{enclosing}.{name}") }
}

/// Arma la `ClassDecl` de la clase sintética a partir de los miembros acumulados.
fn holder_class(h: Holder) -> ClassDecl {
    ClassDecl {
        doc: None,
        annotations: Vec::new(),
        pos: Pos::default(),
        modifiers: Vec::new(),
        kind: TypeKind::Class,
        name: h.name,
        type_params: Vec::new(),
        components: Vec::new(),
        extends: None,
        extends_annos: Vec::new(),
        implements: Vec::new(),
        implements_annos: Vec::new(),
        permits: Vec::new(),
        enum_constants: Vec::new(),
        members: h.members,
        annotation_defaults: Vec::new(),
    }
}

/// El cuerpo del `<clinit>` que puebla un `$SwitchMap$Enum`:
/// `$SwitchMap$Enum = new int[Enum.values().length]; try { $SwitchMap$Enum[Enum.C.ordinal()] = k; } catch (NoSuchFieldError $e) {} …`
/// El `try/catch` por constante es la robustez de §14.11 ante recompilación separada del enum: si una
/// constante desapareció, `E.C.ordinal()` lanza `NoSuchFieldError` y se ignora esa entrada.
fn switchmap_population(field: &str, enum_name: &str, consts: &[String]) -> Block {
    let mut stmts = Vec::new();
    // $SwitchMap$Enum = new int[Enum.values().length];
    let values = call(name(enum_name), "values", vec![]);
    let len = ex(ExprKind::Field { expr: Box::new(values), name: "length".into() });
    let new_arr = ex(ExprKind::NewArray { elem: Type::Prim(PrimType::Int), dims: vec![Some(len)], init: None });
    stmts.push(st(StmtKind::Expr(assign_expr(name(field), new_arr))));
    // Una entrada por constante, con su índice denso `ordinal + 1`.
    for (o, c) in consts.iter().enumerate() {
        let const_access = ex(ExprKind::Field { expr: Box::new(name(enum_name)), name: c.clone() });
        let ordinal = call(const_access, "ordinal", vec![]);
        let index = ex(ExprKind::Index { array: Box::new(name(field)), index: Box::new(ordinal) });
        let assign = st(StmtKind::Expr(assign_expr(index, ex(ExprKind::IntLit(o as i64 + 1)))));
        stmts.push(st(StmtKind::Try {
            resources: Vec::new(),
            body: Block(vec![assign]),
            catches: vec![CatchClause {
                types: vec![Type::Class("NoSuchFieldError".into())],
                name: "$e".into(),
                body: Block(Vec::new()),
                slot: None,
                is_final: false,
            }],
            finally: None,
        }));
    }
    Block(stmts)
}

// ---- constructores de nodos frescos ----

fn st(kind: StmtKind) -> Stmt {
    Stmt::new(Pos::default(), kind)
}
fn boxst(kind: StmtKind) -> Box<Stmt> {
    Box::new(st(kind))
}
fn ex(kind: ExprKind) -> Expr {
    Expr::new(Pos::default(), kind)
}
fn name(n: &str) -> Expr {
    ex(ExprKind::Name(n.to_string()))
}
fn call(target: Expr, method: &str, args: Vec<Expr>) -> Expr {
    ex(ExprKind::Call { target: Some(Box::new(target)), name: method.to_string(), args, type_args: Vec::new() })
}
fn local(ty: Type, name: String, init: Expr) -> Stmt {
    st(StmtKind::LocalVar { ty, name, init: Some(init), is_final: false, type_annos: Vec::new() })
}

/// El **wrapper** de un primitivo y su método de **desboxeo** (§5.1.8): `int`→`(Integer, intValue)`.
/// Lo usa el `for-each` sobre un `Iterable` cuando la variable es primitiva, para castear al wrapper y
/// desboxear igual que javac.
fn wrapper_and_value(p: PrimType) -> (&'static str, &'static str) {
    match p {
        PrimType::Boolean => ("Boolean", "booleanValue"),
        PrimType::Byte => ("Byte", "byteValue"),
        PrimType::Char => ("Character", "charValue"),
        PrimType::Short => ("Short", "shortValue"),
        PrimType::Int => ("Integer", "intValue"),
        PrimType::Long => ("Long", "longValue"),
        PrimType::Float => ("Float", "floatValue"),
        PrimType::Double => ("Double", "doubleValue"),
    }
}

impl Desugarer<'_> {
    fn fresh(&mut self, tag: &str) -> String {
        self.counter += 1;
        format!("${tag}{}", self.counter)
    }

    fn class(&mut self, class: &mut ClassDecl, enclosing: &str) {
        let fqn = qualify(enclosing, &class.name);
        // Primero de todo: los inicializadores de campo pasan a ser sentencias. Después el recorrido
        // les baja el azúcar que tengan adentro como a cualquier otra.
        hoist_initializers(class, self.table.const_fields());
        // El `assert` se baja dentro de un método, pero su *guard* es un campo de **esta** clase: se
        // anota acá y se agrega al final, cuando ya se recorrieron todos los miembros.
        let outer = std::mem::take(&mut self.needs_assert_guard);
        // El contexto de la clase en curso se **salva**: un tipo anidado recursa por acá y no debe
        // pisar ni el dueño de los métodos sintéticos ni el colector a medio llenar del padre.
        let saved_class = self.cur_class;
        let saved_fqn = std::mem::replace(&mut self.cur_fqn, fqn.clone());
        let saved_lambdas = std::mem::take(&mut self.lambda_methods);
        let saved_locals = std::mem::take(&mut self.lifted_locals);
        // `has_this` se fija por miembro dentro del bucle, pero también hay que **restaurarlo** al
        // salir: una clase local se procesa (vía `lift_local_class` → `class()`) en medio del método
        // que la declara, y su método de instancia dejaría `has_this=true` filtrado al resto de ese
        // método —una segunda local en un método **estático** creería capturar `this$0`—.
        let saved_has_this = self.has_this;
        // El método envolvente también se salva/restaura: una local se procesa (vía `class()`) en
        // medio del método que la declara, y sus propios métodos pisarían este contexto.
        let saved_method_sig = self.cur_method_sig.take();
        // La numeración de lambdas es por método: se salva la del método envolvente (una clase local
        // se procesa en medio de él) y se restaura al salir, para que sus lambdas posteriores sigan.
        let saved_lambda_ordinal = std::mem::take(&mut self.lambda_ordinal);
        let saved_method_tps = std::mem::take(&mut self.cur_method_type_params);
        self.cur_class = self.table.class(&fqn);
        // Una interna de **instancia**: se le sintetiza el campo `this$0` y se le inyecta a los
        // constructores el parámetro/asignación de la instancia envolvente, **antes** de recorrer sus
        // cuerpos (que reescriben los accesos capturados).
        if let (Some(cid), Some(outer)) = (self.cur_class, self.enclosing_type) {
            self.capture_enclosing_instance(class, cid, outer);
        }
        // Se toma antes del bucle porque adentro `class` está prestado mutable.
        let enclosing_kind = class.kind;
        for member in &mut class.members {
            match member {
                Member::Method(m) => {
                    self.cur_method = m.name.clone();
                    self.lambda_ordinal = 0; // la primera lambda de cada método es `$0`
                    self.cur_method_type_params = m.type_params.clone();
                    // El método/constructor envolvente, para el `EnclosingMethod` de una local que
                    // declare en su cuerpo: su nombre (`<init>` para un ctor) y descriptor emitido.
                    self.cur_method_sig = self.cur_class.map(|cid| {
                        let scope = self.member_scope_of(cid);
                        let name = if m.is_constructor { "<init>".to_string() } else { m.name.clone() };
                        (name, super::codegen::method_descriptor(self.table, scope, m))
                    });
                    // Un constructor tiene `this`; un método, según su `static`.
                    self.has_this = m.is_constructor || !m.modifiers.contains(&Modifier::Static);
                    if let Some(body) = &mut m.body {
                        self.block(body);
                    }
                }
                Member::Type(nested) => {
                    // El enclosing de una interna de **instancia** es la clase actual; para una
                    // anidada estática (o interface/enum/record) no hay captura.
                    //
                    // Y tampoco la hay si la **envolvente** es una interfaz: un tipo miembro de una
                    // interfaz es implícitamente `static` (§9.5) aunque no lo diga, así que no
                    // existe instancia envolvente que capturar. Sin este segundo filtro, a los
                    // constructores de una `class` anidada en una `interface` se les inyectaba el
                    // parámetro de cabecera `Outer this$0` — y entonces `new Inner(x)` no encontraba
                    // ningún constructor aplicable, porque el único que había pedía dos argumentos
                    // (#295).
                    let saved = self.enclosing_type;
                    let saved_store = self.enclosing_store;
                    self.enclosing_type = (is_instance_inner(nested)
                        && enclosing_kind != TypeKind::Interface)
                        .then_some(self.cur_class)
                        .flatten();
                    // El campo `this$0` solo se materializa si la interna **usa** la instancia
                    // envolvente (§8.1.3); si no, javac igual pasa el parámetro (para el `requireNonNull`)
                    // pero omite el campo.
                    self.enclosing_store = self
                        .enclosing_type
                        .is_some_and(|enc| self.uses_enclosing_instance(nested, enc));
                    self.class(nested, &fqn);
                    self.enclosing_type = saved;
                    self.enclosing_store = saved_store;
                }
                Member::StaticInit(block) => {
                    self.cur_method = "static".to_string();
                    self.lambda_ordinal = 0;
                    self.cur_method_sig = None; // un inicializador no es un método (EnclosingMethod: 0)
                    self.has_this = false;
                    self.block(block);
                }
                Member::InstanceInit(block) => {
                    self.cur_method = "init".to_string();
                    self.lambda_ordinal = 0;
                    self.cur_method_sig = None;
                    self.has_this = true;
                    self.block(block);
                }
                Member::Field(_) => {}
            }
        }
        // Los métodos `lambda$…` y las clases **locales levantadas** de ESTA clase pasan a ser
        // miembros suyos; se restauran los colectores del padre para que siga acumulando los propios.
        let synth = std::mem::replace(&mut self.lambda_methods, saved_lambdas);
        class.members.extend(synth);
        let locals = std::mem::replace(&mut self.lifted_locals, saved_locals);
        class.members.extend(locals);
        self.cur_class = saved_class;
        self.cur_fqn = saved_fqn;
        self.has_this = saved_has_this;
        self.cur_method_sig = saved_method_sig;
        self.lambda_ordinal = saved_lambda_ordinal;
        self.cur_method_type_params = saved_method_tps;
        // Los inicializadores de **instancia** corren dentro de cada constructor, después del
        // `super()` (§8.6/§12.5): se copian al frente de su cuerpo. Un constructor que delega en
        // `this(...)` **no** los corre — ya los corrió aquel.
        let inits: Vec<Block> = class
            .members
            .iter()
            .filter_map(|m| match m {
                Member::InstanceInit(b) => Some(b.clone()),
                _ => None,
            })
            .collect();
        if !inits.is_empty() {
            for member in &mut class.members {
                let Member::Method(m) = member else { continue };
                if !m.is_constructor {
                    continue;
                }
                let Some(body) = &mut m.body else { continue };
                if delegates_to_this(body) {
                    continue;
                }
                // Van al frente, PERO detrás del `super(...)` explícito si lo hay (§12.5: el orden
                // es superclase, inicializadores, cuerpo). Meterlos delante del `super(...)` hacía
                // que el emisor no lo reconociera como primera sentencia y le antepusiera el
                // `super()` implícito, con lo que la superclase se construía dos veces --- una con
                // el constructor equivocado (finding #342).
                let corte = if delegates_to_super(body) { 1 } else { 0 };
                let mut merged: Vec<Stmt> = body.0.drain(..corte).collect();
                merged.extend(inits.iter().flat_map(|b| b.0.clone()));
                merged.append(&mut body.0);
                body.0 = merged;
            }
            class.members.retain(|m| !matches!(m, Member::InstanceInit(_)));
        }
        // Un `record` trae sus miembros **implícitos** (§8.10): los símbolos ya los registró `enter`
        // —por eso `p.x()` resuelve—, pero sin cuerpo en el AST el `.class` los referencia sin
        // declararlos. Acá se materializan.
        if class.kind == TypeKind::Record {
            let synth = record_members(class);
            class.members.extend(synth);
            // `equals`/`hashCode`/`toString` van por `invokedynamic` + `ObjectMethods` (§8.10.2): se
            // sintetizan aparte porque necesitan la tabla (los descriptores de cada componente).
            if let Some(cid) = self.table.class(&fqn) {
                let synth = self.record_object_methods(class, cid);
                class.members.extend(synth);
            }
        }
        // Un `enum` trae sus miembros **implícitos** (§8.9.3): las constantes como campos, el
        // `$VALUES` que las junta, el constructor que las nombra, y `values()`/`valueOf()`.
        if class.kind == TypeKind::Enum {
            if let Some(cid) = self.table.class(&fqn) {
                // Un `enum` con **constantes parametrizadas** (`ROJO("rojo")`) trae constructor(es)
                // propios: se les antepone `(String $name, int $ordinal)` + `super(...)` y cada
                // constante se construye con sus argumentos. Sin ctor propio va el sintético `(String,
                // int)` por defecto.
                let has_ctor = class
                    .members
                    .iter()
                    .any(|m| matches!(m, Member::Method(me) if me.is_constructor));
                if has_ctor {
                    self.rewrite_enum_ctors(class, cid);
                }
                let synth = self.enum_members(class, cid, has_ctor);
                // El `<clinit>` sintetizado va **adelante** del que escribió el usuario (#317).
                //
                // JLS §12.4.2 manda construir las constantes del `enum` **primero** y recién después
                // correr el resto del inicializador estático. Acá pasaba al revés y por una razón de
                // orden de pasadas: `hoist_initializers` corre al entrar a la clase y deja el
                // `StaticInit` del usuario en `members`; la síntesis del `enum` corre después y
                // **agregaba** el suyo detrás. El emisor concatena en orden de miembro, así que las
                // constantes se construían últimas.
                //
                // Lo que producía es de lo peor que puede pasar: nada falla. La clase carga, y un
                // `static final X ALIAS = A;` queda en `null` porque `A` todavía no existía cuando se
                // leyó. Salió en `AclEntryPermission`, donde tres constantes son alias de otras tres.
                let (clinit_enum, resto): (Vec<Member>, Vec<Member>) =
                    synth.into_iter().partition(|m| matches!(m, Member::StaticInit(_)));
                class.members.extend(resto);
                for m in clinit_enum {
                    let Member::StaticInit(bloque) = m else { continue };
                    // Se fusiona por delante del que ya haya, en vez de agregar un segundo bloque: el
                    // emisor los concatenaría igual, pero un solo `<clinit>` es lo que el `.class`
                    // tiene y dejarlo así hace que lo que se lee sea lo que se emite.
                    let existente = class
                        .members
                        .iter_mut()
                        .find_map(|m| match m {
                            Member::StaticInit(b) => Some(b),
                            _ => None,
                        });
                    match existente {
                        Some(usuario) => {
                            let mut juntos = bloque.0;
                            juntos.append(&mut usuario.0);
                            usuario.0 = juntos;
                        }
                        None => class.members.push(Member::StaticInit(bloque)),
                    }
                }
            }
        }
        if self.needs_assert_guard {
            if let Some(cid) = self.table.class(&fqn) {
                let name = class.name.clone();
                let (field, init) = self.assert_guard_members(cid, &name);
                class.members.push(field);
                class.members.push(init);
            }
        }
        self.needs_assert_guard = outer;
    }

    /// El campo sintético `$assertionsDisabled` y el `<clinit>` que lo calcula (§14.10): las
    /// aserciones están **deshabilitadas por defecto**, y `Class.desiredAssertionStatus()` es lo que
    /// dice si hay que evaluarlas. Registra el símbolo para que la re-atribución lo resuelva.
    /// Los miembros **implícitos** de un `enum` (§8.9.3). `enter` ya registró las constantes como
    /// campos `public static final` —por eso `Color.ROJO` resuelve—, pero sin nada que las
    /// construya el `.class` las declaraba y las dejaba en `null`.
    ///
    /// Se sintetiza lo mismo que javac:
    ///
    /// ```text
    /// public static final E A;              // una por constante
    /// private static final E[] $VALUES;
    /// static { A = new E("A", 0); …; $VALUES = new E[]{A, …}; }
    /// private E(String $name, int $ordinal) { super($name, $ordinal); }
    /// public static E[] values()          { …copia de $VALUES… }
    /// public static E   valueOf(String n) { …busca por nombre… }
    /// ```
    ///
    /// Dos desvíos deliberados de javac, los dos por la misma razón —no depender de reflexión ni de
    /// opcodes que no emitimos—, y ninguno cambia la semántica:
    ///
    /// - `values()` copia con un **bucle** en vez de `$VALUES.clone()`. Lo que importa de `clone()`
    ///   es que cada llamada devuelva un array **fresco**, para que nadie mute el estado del enum
    ///   desde afuera; el bucle da exactamente eso.
    /// - `valueOf` compara contra los nombres **literales** acá adentro, en vez de delegar en
    ///   `Enum.valueOf(Class, String)`: esa versión va por `Class.enumConstantDirectory()`, o sea
    ///   reflexión sobre el `$VALUES` de otra clase. **No es negociable** mientras no haya reflexión:
    ///   `java.lang.Enum` de KajiLibrary **no declara** ese método, a propósito, así que delegar deja
    ///   sin compilar a todo archivo que declare un `enum` (finding #250, 70 fuentes caídas).
    ///
    /// Los símbolos del constructor, de `values()` y de `valueOf()` se **registran** acá: `enter` no
    /// los vio, y la re-atribución los busca por la tabla, no por el AST.
    fn enum_members(&mut self, class: &ClassDecl, cid: SymbolId, has_user_ctor: bool) -> Vec<Member> {
        let ename = class.name.clone();
        let ety = Type::Class(ename.clone());
        let arr = Type::Array(Box::new(ety.clone()));
        let string = Type::Class("String".to_string());
        let int = Type::Prim(PrimType::Int);
        let scope = match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => self.top_scope,
        };
        let mut out = Vec::new();

        // 1. Un campo por constante. El símbolo ya existe; lo que falta es la **declaración**, sin
        //    la cual el `.class` referencia un campo que no declara.
        for c in &class.enum_constants {
            out.push(Member::Field(FieldDecl {
                doc: None,
                annotations: Vec::new(),
                type_annos: Vec::new(),
                pos: Pos::default(),
                modifiers: vec![Modifier::Public, Modifier::Static, Modifier::Final],
                ty: ety.clone(),
                name: c.name.clone(),
                init: None,
            }));
        }

        // 2. `$VALUES` sí es nuevo: hay que registrarlo además de declararlo.
        let values_field = "$VALUES";
        let fid = self.table.new_symbol(Symbol {
            name: values_field.to_string(),
            kind: SymbolKind::Field { ty: arr.clone() },
            owner: Some(cid),
            modifiers: vec![Modifier::Private, Modifier::Static, Modifier::Final],
        });
        self.table.define(scope, values_field, fid);
        self.table
            .set_resolved(fid, Resolved::Field(RType::Array(Box::new(RType::Class(cid)))));
        out.push(Member::Field(FieldDecl {
            doc: None,
            annotations: Vec::new(),
            type_annos: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Private, Modifier::Static, Modifier::Final],
            ty: arr.clone(),
            name: values_field.to_string(),
            init: None,
        }));

        // 3. El `<clinit>`: construir cada constante —de ahí salen su nombre y su ordinal— y
        //    juntarlas en `$VALUES`.
        let mut clinit = Vec::new();
        for (i, c) in class.enum_constants.iter().enumerate() {
            // `new E("NAME", ordinal, ...args)`: el nombre y el ordinal de cabecera (para `Enum`), y
            // detrás los **argumentos de la constante** (`ROJO("rojo")` → `new Color("ROJO", 0,
            // "rojo")`), que resuelven al ctor propio ya con `(String, int)` antepuesto.
            let mut args =
                vec![ex(ExprKind::StringLit(c.name.clone())), ex(ExprKind::IntLit(i as i64))];
            args.extend(c.args.iter().cloned());
            let made = ex(ExprKind::NewObject { ty: ety.clone(), args, body: None, outer: None });
            clinit.push(st(StmtKind::Expr(assign_expr(name(&c.name), made))));
        }
        // El array de constantes se factoriza en un método sintético `private static E[] $values()`,
        // igual que javac; el `<clinit>` lo invoca con `$VALUES = $values()` en vez de construir el
        // array inline. (`$values` lleva `ACC_SYNTHETIC`, que se lo pone el codegen por nombre.)
        let all = ex(ExprKind::NewArray {
            elem: ety.clone(),
            dims: vec![None],
            init: Some(class.enum_constants.iter().map(|c| name(&c.name)).collect()),
        });
        self.register_method(cid, scope, "$values", &[], &arr, false);
        out.push(Member::Method(MethodDecl {
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Private, Modifier::Static],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: arr.clone(),
            name: "$values".to_string(),
            params: Vec::new(),
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(Block(vec![st(StmtKind::Return(Some(all)))])),
            is_constructor: false,
            doc: None,
        }));
        let values_call = ex(ExprKind::Call {
            target: None,
            name: "$values".to_string(),
            args: Vec::new(),
            type_args: Vec::new(),
        });
        clinit.push(st(StmtKind::Expr(assign_expr(name(values_field), values_call))));
        out.push(Member::StaticInit(Block(clinit)));

        // 4. El constructor. Es lo único que puede darle a `java.lang.Enum` su nombre y su posición,
        //    y por eso la invocación explícita de `super(...)` era el bloqueante de todo esto. Solo se
        //    sintetiza el `(String, int)` por defecto **si el usuario no declaró uno**; si lo hizo,
        //    `rewrite_enum_ctors` ya le antepuso los mismos parámetros.
        if !has_user_ctor {
            let (pn, po) = ("$name".to_string(), "$ordinal".to_string());
            let super_call = ex(ExprKind::Call {
                target: None,
                name: "super".to_string(),
                args: vec![name(&pn), name(&po)],
                type_args: Vec::new(),
            });
            let ctor_params = vec![
                Param { annotations: Vec::new(), ty: string.clone(), name: pn, varargs: false, is_final: false, type_annos: Vec::new() },
                Param { annotations: Vec::new(), ty: int.clone(), name: po, varargs: false, is_final: false, type_annos: Vec::new() },
            ];
            self.register_method(cid, scope, &ename, &ctor_params, &Type::Void, true);
            out.push(Member::Method(MethodDecl {
                doc: None,
                annotations: Vec::new(),
                pos: Pos::default(),
                modifiers: vec![Modifier::Private],
                type_params: Vec::new(),
                return_annos: Vec::new(),
                return_type: Type::Void,
                name: ename.clone(),
                params: ctor_params,
                throws: Vec::new(),
                throws_annos: Vec::new(),
                body: Some(Block(vec![st(StmtKind::Expr(super_call))])),
                is_constructor: true,
            }));
        }

        // 5. `values()`: `return (E[]) $VALUES.clone();` — una copia **fresca** de `$VALUES` en cada
        //    llamada, igual que javac (`invokevirtual clone` + `checkcast`). El emisor sabe emitir
        //    `array.clone()` (§10.7); el `checkcast` lo pone él porque el `clone` heredado devuelve
        //    `Object`. Antes se copiaba con un bucle (para no depender de `array.clone()`); ya no hace
        //    falta y así el `.class` coincide con javac byte a byte.
        let body = Block(vec![st(StmtKind::Return(Some(ex(ExprKind::Call {
            target: Some(Box::new(name(values_field))),
            name: "clone".to_string(),
            args: Vec::new(),
            type_args: Vec::new(),
        }))))]);
        self.register_method(cid, scope, "values", &[], &arr, false);
        out.push(Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Public, Modifier::Static],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: arr,
            name: "values".to_string(),
            params: Vec::new(),
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(body),
            is_constructor: false,
        }));

        // 6. `valueOf(String)`: **autocontenido**, comparando contra los nombres literales acá
        //    adentro:
        //
        //        if ($n.equals("ROJO"))  return ROJO;
        //        if ($n.equals("VERDE")) return VERDE;
        //        throw new IllegalArgumentException($n);
        //
        //    Es un desvío deliberado de javac, que emite
        //    `return (E) Enum.valueOf(E.class, $n);`. **Finding #250 — regresión:** en algún momento
        //    se cambió a esa delegación "para coincidir byte a byte con javac", y eso rompió la
        //    compilación de **todo** archivo que declare un `enum`: `java.lang.Enum.valueOf(Class,
        //    String)` **no existe en KajiLibrary, y su ausencia es intencional** — la versión real va
        //    por `Class.enumConstantDirectory()`, o sea reflexión sobre el `$VALUES` de otra clase,
        //    que no tenemos. El costo medido de la regresión fueron **70 de 941 fuentes sin `.class`**.
        //
        //    Ojo con el precedente de al lado: `values()` sí se pudo alinear con javac (usa
        //    `$VALUES.clone()`, que el emisor y la VM soportan). La diferencia es que ahí el opcode
        //    existe; acá la dependencia es la reflexión, y por eso este desvío se queda.
        //
        //    Dos detalles que **no** hay que "mejorar" — los dos se probaron y rompen:
        //    - El receptor es el **literal** (`"ROJO".equals($n)`), no el argumento. Con `$n` de
        //      receptor un nombre `null` daría `NullPointerException`, que es lo que manda §8.9.3,
        //      pero acá no hay con que distinguirlo del caso "no matchea", y la forma con literal es
        //      la que estaba probada.
        //    - El `IllegalArgumentException` va **sin argumentos**. Pasarle `$n` hace que el
        //      verificador estricto rechace el método (`<init> receiver … is not an uninitialized
        //      object`) cuando el tipo no resuelve en el classpath del que compila — que es
        //      justamente el caso de los tests con classpath minimo.
        let arg = "$n".to_string();
        let mut vo_stmts: Vec<Stmt> = Vec::new();
        for c in &class.enum_constants {
            let cmp = call(ex(ExprKind::StringLit(c.name.clone())), "equals", vec![name(&arg)]);
            vo_stmts.push(st(StmtKind::If {
                cond: cmp,
                then: boxst(StmtKind::Return(Some(name(&c.name)))),
                els: None,
            }));
        }
        // Nombre desconocido: el mismo error que manda §8.9.3.
        vo_stmts.push(st(StmtKind::Throw(ex(ExprKind::NewObject {
            ty: Type::Class("IllegalArgumentException".to_string()),
            args: Vec::new(),
            body: None,
            outer: None,
        }))));
        let vo_body = Block(vo_stmts);
        let vo_params = vec![Param { annotations: Vec::new(), ty: string, name: arg, varargs: false, is_final: false, type_annos: Vec::new() }];
        self.register_method(cid, scope, "valueOf", &vo_params, &ety, false);
        out.push(Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Public, Modifier::Static],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: ety,
            name: "valueOf".to_string(),
            params: vo_params,
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(vo_body),
            is_constructor: false,
        }));

        out
    }

    /// Registra el símbolo de un método sintetizado, ya **resuelto**.
    fn register_method(
        &mut self,
        cid: SymbolId,
        scope: ScopeId,
        name: &str,
        params: &[Param],
        ret: &Type,
        is_constructor: bool,
    ) {
        let psig: Vec<ParamSig> = params
            .iter()
            .map(|p| ParamSig { ty: p.ty.clone(), name: p.name.clone(), varargs: false })
            .collect();
        let modifiers = if is_constructor {
            vec![Modifier::Private]
        } else {
            vec![Modifier::Public, Modifier::Static]
        };
        let mid = self.table.new_symbol(Symbol {
            name: name.to_string(),
            kind: SymbolKind::Method { params: psig, return_type: ret.clone(), is_constructor, throws: Vec::new() },
            owner: Some(cid),
            modifiers,
        });
        self.table.define(scope, name, mid);
        let rparams: Vec<RType> = params.iter().map(|p| self.rtype(cid, &p.ty)).collect();
        let rret = if is_constructor { RType::Void } else { self.rtype(cid, ret) };
        self.table.set_resolved(mid, Resolved::Method { params: rparams, ret: rret, varargs: false, throws: Vec::new() });
    }

    /// Reescribe los constructores **propios** de un `enum` con constantes parametrizadas (§8.9.2): les
    /// **antepone** `(String $name, int $ordinal)` —los que `java.lang.Enum` necesita— tanto en el AST
    /// (para el descriptor y las referencias del cuerpo) como en el **`Resolved`** de su símbolo (la
    /// re-atribución no re-resuelve firmas, y sin eso `new E("A", 0, …)` no encontraría el ctor). En el
    /// cuerpo inyecta `super($name, $ordinal)` de cabecera —un ctor de `enum` no puede llamar `super`
    /// explícito—, o, si delega en `this(...)`, le propaga los dos de cabecera.
    fn rewrite_enum_ctors(&mut self, class: &mut ClassDecl, cid: SymbolId) {
        let string = Type::Class("String".to_string());
        let int = Type::Prim(PrimType::Int);
        let sr = self.rtype(cid, &string);
        // Símbolo: anteponer `[String, int]` al `Resolved` de cada ctor (se extrae y se re-escribe
        // fuera del préstamo inmutable).
        for mid in constructors(self.table, cid) {
            let updated = match self.table.resolved(mid) {
                Some(Resolved::Method { params, ret, varargs, throws }) => {
                    let mut np = vec![sr.clone(), RType::Prim(PrimType::Int)];
                    np.extend(params.iter().cloned());
                    Some(Resolved::Method {
                        params: np,
                        ret: ret.clone(),
                        varargs: *varargs,
                        throws: throws.clone(),
                    })
                }
                _ => None,
            };
            if let Some(r) = updated {
                self.table.set_resolved(mid, r);
            }
        }
        // AST: anteponer los parámetros e inyectar/propagar el `super`/`this`.
        for m in class.members.iter_mut() {
            let Member::Method(me) = m else { continue };
            if !me.is_constructor {
                continue;
            }
            me.params.insert(0, synth_param(int.clone(), "$ordinal".to_string()));
            me.params.insert(0, synth_param(string.clone(), "$name".to_string()));
            let Some(body) = &mut me.body else { continue };
            let delegates = matches!(
                body.0.first().map(|s| &s.kind),
                Some(StmtKind::Expr(e))
                    if matches!(&e.kind, ExprKind::Call { target: None, name, .. } if name == "this")
            );
            if delegates {
                // `this(args)` → `this($name, $ordinal, args)`: el ctor delegado recibe la cabecera.
                if let Some(StmtKind::Expr(e)) = body.0.first_mut().map(|s| &mut s.kind) {
                    if let ExprKind::Call { args, .. } = &mut e.kind {
                        args.insert(0, name("$ordinal"));
                        args.insert(0, name("$name"));
                    }
                }
            } else {
                let super_call = ex(ExprKind::Call {
                    target: None,
                    name: "super".to_string(),
                    args: vec![name("$name"), name("$ordinal")],
                    type_args: Vec::new(),
                });
                body.0.insert(0, st(StmtKind::Expr(super_call)));
            }
        }
    }

    /// El [`RType`] de los pocos tipos que esta pasada sintetiza: el propio enum, su array, `String`,
    /// `int` y `void`. No pretende ser un resolvedor general — ese vive en `enter`/`attribute`.
    fn rtype(&self, cid: SymbolId, ty: &Type) -> RType {
        match ty {
            Type::Void => RType::Void,
            Type::Prim(p) => RType::Prim(*p),
            Type::Array(inner) => RType::Array(Box::new(self.rtype(cid, inner))),
            Type::Class(n) if *n == self.table.symbol(cid).name => RType::Class(cid),
            Type::Class(n) => self.table.external(n).map_or(RType::Unresolved, RType::Class),
            _ => RType::Unresolved,
        }
    }

    fn assert_guard_members(&mut self, cid: SymbolId, class_name: &str) -> (Member, Member) {
        let field = "$assertionsDisabled";
        let scope = match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => self.top_scope,
        };
        let fid = self.table.new_symbol(Symbol {
            name: field.to_string(),
            kind: SymbolKind::Field { ty: Type::Prim(PrimType::Boolean) },
            owner: Some(cid),
            modifiers: vec![Modifier::Static, Modifier::Final],
        });
        self.table.define(scope, field, fid);
        self.table.set_resolved(fid, Resolved::Field(RType::Prim(PrimType::Boolean)));

        let decl = Member::Field(FieldDecl {
            doc: None,
            annotations: Vec::new(),
            type_annos: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Static, Modifier::Final],
            ty: Type::Prim(PrimType::Boolean),
            name: field.to_string(),
            init: None,
        });
        // `$assertionsDisabled = !C.class.desiredAssertionStatus();`
        let status = call(
            ex(ExprKind::ClassLit(Type::Class(class_name.to_string()))),
            "desiredAssertionStatus",
            vec![],
        );
        let negated = ex(ExprKind::Unary { op: UnOp::Not, expr: Box::new(status), prefix: true });
        let init = Member::StaticInit(Block(vec![st(StmtKind::Expr(assign_expr(name(field), negated)))]));
        (decl, init)
    }

    fn block(&mut self, block: &mut Block) {
        let mut out = Vec::with_capacity(block.0.len());
        for mut s in std::mem::take(&mut block.0) {
            // `T v = switch(...){ flechas }` → `T v;` + `switch(...){ ... v = ... }`: así el codegen
            // solo ve la forma **sentencia** del switch, nunca una switch-expresión en un `init`.
            // Una declaración de local es siempre hija directa de un bloque, así que este es el único
            // lugar donde hace falta partir una sentencia en dos (las demás posiciones se auto-contienen).
            // Una switch-expresión de **pila** (selector `int`, brazos que dejan el valor en la pila)
            // sobrevive entera al codegen: el `init` la consume con un `store`, sin temporal.
            if matches!(&s.kind, StmtKind::LocalVar { init: Some(e), .. } if is_lowerable_switch_expr(e) && !self.is_stack_switch_expr(e)) {
                let StmtKind::LocalVar { ty, name: var, init, .. } =
                    std::mem::replace(&mut s.kind, StmtKind::Empty)
                else {
                    unreachable!()
                };
                out.push(st(StmtKind::LocalVar { ty, name: var.clone(), init: None, is_final: false, type_annos: Vec::new() }));
                s.kind = self.switch_to_stmt(init.unwrap(), name(&var));
            }
            self.stmt(&mut s); // baja el switch-sentencia resultante (y su azúcar interna)
            out.push(s);
        }
        block.0 = out;
    }

    fn stmt(&mut self, s: &mut Stmt) {
        // 1. Bajar primero las sentencias **y expresiones** anidadas (bottom-up), así el azúcar
        //    interno ya está resuelto cuando reescribimos este nodo.
        match &mut s.kind {
            StmtKind::LocalVar { init, .. } => {
                if let Some(e) = init {
                    self.expr(e);
                }
            }
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.expr(e);
                }
            }
            StmtKind::Throw(e) | StmtKind::Yield(e) => self.expr(e),
            StmtKind::Expr(e) => self.discard_expr(e), // posición de descarte: `x++` → `x += 1`
            StmtKind::Block(b) => self.block(b),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock);
                self.block(body);
            }
            StmtKind::If { cond, then, els } => {
                self.expr(cond);
                self.stmt(then);
                if let Some(e) = els {
                    self.stmt(e);
                }
            }
            StmtKind::While { cond, body } => {
                self.expr(cond);
                self.stmt(body);
            }
            StmtKind::Do { body, cond } => {
                self.stmt(body);
                self.expr(cond);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i);
                }
                if let Some(c) = cond {
                    self.expr(c);
                }
                update.iter_mut().for_each(|u| self.discard_expr(u)); // el `i++` del for descarta
                self.stmt(body);
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.expr(iterable);
                self.stmt(body);
            }
            StmtKind::Assert { cond, message } => {
                self.expr(cond);
                if let Some(m) = message {
                    self.expr(m);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector);
                for c in cases {
                    for l in c.labels.iter_mut() {
                        self.fold_case_label(l);
                    }
                    if let Some(g) = &mut c.guard {
                        self.expr(g);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(s) => self.stmt(s),
                        SwitchBody::Colon(ss) => ss.iter_mut().for_each(|s| self.stmt(s)),
                    }
                }
            }
            StmtKind::Try { resources, body, catches, finally } => {
                resources.iter_mut().for_each(|r| self.stmt(r));
                self.block(body);
                catches.iter_mut().for_each(|c| self.block(&mut c.body));
                if let Some(f) = finally {
                    self.block(f);
                }
            }
            StmtKind::Labeled { body, .. } => self.stmt(body),
            StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Empty => {}
            // Una **clase local** (§14.3) se **levanta** a tipo anidado del enclosing y se captura sus
            // locales (`val$`); la sentencia queda vacía. El `new L(...)` de su sitio de uso —que
            // aparece después en el mismo cuerpo— se reescribe con los locales capturados de cabecera.
            StmtKind::LocalClass(_) => {
                let old = std::mem::replace(&mut s.kind, StmtKind::Empty);
                let StmtKind::LocalClass(lc) = old else { return };
                if let Some(member) = self.lift_local_class(lc) {
                    self.lifted_locals.push(member);
                }
            }
        }

        // 2. Reescribir este nodo si es azúcar.
        let sugar = match &s.kind {
            StmtKind::ForEach { .. } => Sugar::ForEach,
            StmtKind::Assert { .. } => Sugar::Assert,
            StmtKind::Try { resources, .. } if !resources.is_empty() => Sugar::TryResources,
            _ => Sugar::None,
        };
        match sugar {
            Sugar::ForEach => self.lower_for_each(s),
            Sugar::Assert => self.lower_assert(s),
            Sugar::TryResources => self.lower_try_resources(s),
            Sugar::None => {}
        }

        // Una switch-expresión en posición de cola (`return`/`yield`/`x = switch…`) → switch-sentencia.
        // (El caso `T v = switch…` lo maneja `block`, que puede partir la sentencia en dos.)
        self.lower_switch_stmt(s);
        // Un `switch` (ya sentencia) sobre `String` → dos switches sobre `int`.
        self.lower_string_switch(s);
        // Un `switch` (ya sentencia) sobre `enum` → switch sobre el `$SwitchMap` (clase `C$1`).
        self.lower_enum_switch(s);
        // Un `switch` con *type patterns* → cadena de `instanceof` en un bloque etiquetado.
        self.lower_pattern_switch(s);
        // `synchronized (e)`: la copia del monitor a un local sintético (§14.19) y su `monitorexit`
        // en cada salida la hace el emisor (`codegen::sync_stmt`), espejando a javac (`dup`/`astore`
        // en vez de una sentencia `Object $lock = e;`), así que no hay azúcar que bajar acá.
    }

    /// Punto de entrada del switch sobre `String`: opera sobre el `switch`-sentencia, ya venga
    /// directo o **envuelto en un `Labeled`** (cuando la switch-expresión que lo originó tenía brazos
    /// que yieldan y necesitó una etiqueta). El `break $sw` de esos brazos sigue apuntando al
    /// `Labeled`, que ahora envuelve los dos switches sobre `int`.
    fn lower_string_switch(&mut self, s: &mut Stmt) {
        let direct = matches!(s.kind, StmtKind::Switch { .. });
        let in_label =
            matches!(&s.kind, StmtKind::Labeled { body, .. } if matches!(body.kind, StmtKind::Switch { .. }));
        if direct {
            self.string_switch_here(s);
        } else if in_label {
            if let StmtKind::Labeled { body, .. } = &mut s.kind {
                self.string_switch_here(body);
            }
        } else {
            // `return`/`yield switch (s) { … }` de cola: se baja dejando el valor en la pila.
            self.string_switch_expr_tail(s);
        }
    }

    /// `switch (s) { case "a": … }` sobre un `String` → **dos** switches sobre `int` (JLS §14.11):
    /// el primero mapea `s.hashCode()` a un índice sintético vía `equals()` (con una cadena
    /// `if/else` para las colisiones de hash), el segundo replica los brazos originales indexados por
    /// ese entero. Preserva el *fall-through* y el `default` porque el segundo switch es un **espejo**
    /// del original (solo cambian las etiquetas `"str"` por el índice). Un `case null` o un selector
    /// no-`String` caen fuera y se dejan para otra pasada / el codegen.
    fn string_switch_here(&mut self, s: &mut Stmt) {
        let StmtKind::Switch { selector, cases } = &s.kind else { return };
        if !self.is_string_switch(selector, cases) {
            return;
        }
        let StmtKind::Switch { selector, cases } = std::mem::replace(&mut s.kind, StmtKind::Empty)
        else {
            unreachable!()
        };
        let (decl_s, decl_i, dispatch, ivar, level2) = self.string_switch_parts(selector, cases);
        // { String $s = sel; int $i = -1; <ruteo>; switch ($i){nivel 2} }
        s.kind = StmtKind::Block(Block(vec![
            decl_s,
            decl_i,
            dispatch,
            st(StmtKind::Switch { selector: name(&ivar), cases: level2 }),
        ]));
    }

    /// Piezas comunes de un `switch` sobre `String`: declara `$s`/`$i`, arma el **nivel 1** (dispatch
    /// por `hashCode`, cadena de `equals` por grupo de hash y el ruteo de `null`) y el **nivel 2**
    /// (espejo del original, indexado por el entero sintético). Devuelve
    /// `(decl $s, decl $i, dispatch, nombre de $i, casos del nivel 2)`; el llamador decide si el
    /// nivel 2 es una **sentencia** (`switch` sobre `String`) o el valor de un `return`/`yield`
    /// (switch-**expresión** de cola) — así ambas formas comparten exactamente los mismos slots.
    fn string_switch_parts(
        &mut self,
        selector: Expr,
        cases: Vec<SwitchCase>,
    ) -> (Stmt, Stmt, Stmt, String, Vec<SwitchCase>) {
        let svar = self.fresh("s");
        let ivar = self.fresh("i");

        // Nivel 2 (espejo del original) + recolección de `hash → [(string, índice)]` (ordenado por
        // hash con `BTreeMap`, como el `lookupswitch` que exige claves crecientes).
        let mut groups: BTreeMap<i32, Vec<(String, i64)>> = BTreeMap::new();
        let mut level2 = Vec::with_capacity(cases.len());
        let mut idx: i64 = 0;
        let mut null_idx: Option<i64> = None;
        for c in cases {
            if c.is_default {
                level2.push(SwitchCase { labels: vec![], is_default: true, guard: None, body: c.body });
                continue;
            }
            let my = idx;
            idx += 1;
            for label in &c.labels {
                match label {
                    CaseLabel::Constant(e) => {
                        if let ExprKind::StringLit(str) = &e.kind {
                            groups.entry(java_string_hash(str)).or_default().push((str.clone(), my));
                        }
                    }
                    // `case null` no participa del hash: se rutea antes (ver el ensamblado).
                    CaseLabel::Null => null_idx = Some(my),
                    CaseLabel::Pattern { .. } => {}
                }
            }
            level2.push(SwitchCase {
                labels: vec![CaseLabel::Constant(ex(ExprKind::IntLit(my)))],
                is_default: false,
                guard: None,
                body: c.body,
            });
        }

        // Nivel 1: `switch (s.hashCode()) { case <hash> -> { if (s.equals("x")) $i = k; else if … } }`.
        let mut level1 = Vec::with_capacity(groups.len());
        for (hash, entries) in groups {
            let mut chain: Option<Box<Stmt>> = None; // se arma de atrás hacia adelante
            for (str, k) in entries.into_iter().rev() {
                let assign = st(StmtKind::Expr(assign_expr(name(&ivar), ex(ExprKind::IntLit(k)))));
                let cond = call(name(&svar), "equals", vec![ex(ExprKind::StringLit(str))]);
                chain = Some(Box::new(st(StmtKind::If {
                    cond,
                    then: Box::new(st(StmtKind::Block(Block(vec![assign])))),
                    els: chain,
                })));
            }
            let body = chain.map(|b| *b).unwrap_or_else(|| st(StmtKind::Empty));
            level1.push(SwitchCase {
                labels: vec![CaseLabel::Constant(ex(ExprKind::IntLit(hash as i64)))],
                is_default: false,
                guard: None,
                body: SwitchBody::Arrow(Box::new(st(StmtKind::Block(Block(vec![body]))))),
            });
        }

        // El nivel 1, precedido del ruteo de `null` si hay un `case null`. **Sin** él, un selector
        // nulo revienta en `hashCode()` — que es exactamente la semántica de Java.
        let level1_switch =
            st(StmtKind::Switch { selector: call(name(&svar), "hashCode", vec![]), cases: level1 });
        let dispatch = match null_idx {
            Some(i) => st(StmtKind::If {
                cond: eq_null(name(&svar)),
                then: Box::new(st(StmtKind::Expr(assign_expr(
                    name(&ivar),
                    ex(ExprKind::IntLit(i)),
                )))),
                els: Some(Box::new(level1_switch)),
            }),
            None => level1_switch,
        };

        let decl_s = local(Type::Class("String".into()), svar.clone(), selector);
        let decl_i = local(Type::Prim(PrimType::Int), ivar.clone(), ex(ExprKind::IntLit(-1)));
        (decl_s, decl_i, dispatch, ivar, level2)
    }

    /// `return switch (s) { case "a" -> 1; … }` / `yield switch (s) { … }` sobre un `String`: baja la
    /// switch-**expresión** de cola dejando el **nivel 2 como switch-expresión sobre `int`** — que el
    /// codegen consume con el valor en la pila (`ireturn`/`yield` directo), **sin** temporal de
    /// resultado. Esto reproduce a javac byte a byte: `$s`/`$i` toman los slots inmediatamente
    /// después de los parámetros (no hay un slot de resultado que se cuele antes). Solo aplica si los
    /// brazos son de **flecha-valor** (o `throw`) y hay `default`; el resto sigue el camino con
    /// temporal de [`lower_switch_stmt`]/[`hoist_switch`].
    fn string_switch_expr_tail(&mut self, s: &mut Stmt) {
        let applies = match &s.kind {
            StmtKind::Return(Some(e)) | StmtKind::Yield(e) => self.is_tail_string_switch_expr(e),
            _ => false,
        };
        if !applies {
            return;
        }
        let (e, is_return) = match std::mem::replace(&mut s.kind, StmtKind::Empty) {
            StmtKind::Return(Some(e)) => (e, true),
            StmtKind::Yield(e) => (e, false),
            _ => unreachable!(),
        };
        let ety = e.ty.clone();
        let ExprKind::Switch { selector, cases } = e.kind else { unreachable!() };
        let (decl_s, decl_i, dispatch, ivar, level2) = self.string_switch_parts(*selector, cases);
        // La switch-expresión de cola sobre `$i` (`int`): deja **el valor en la pila** — el codegen la
        // toma por los caminos normales (`return`→pila→`ireturn`), como cualquier switch-expr entera.
        let mut sel = name(&ivar);
        sel.ty = Some(RType::Prim(PrimType::Int));
        let mut tail_expr = ex(ExprKind::Switch { selector: Box::new(sel), cases: level2 });
        tail_expr.ty = ety; // el tipo del switch original (para la categoría/`vtype` del resultado)
        let tail = if is_return {
            st(StmtKind::Return(Some(tail_expr)))
        } else {
            st(StmtKind::Yield(tail_expr))
        };
        s.kind = StmtKind::Block(Block(vec![decl_s, decl_i, dispatch, tail]));
    }

    /// ¿`e` es una switch-**expresión** sobre `String` bajable **en la pila** (con `default` y todos
    /// los brazos de flecha-valor o `throw`)? Es el subconjunto que [`string_switch_expr_tail`] emite
    /// byte a byte como javac; el resto usa el camino con temporal.
    fn is_tail_string_switch_expr(&self, e: &Expr) -> bool {
        let ExprKind::Switch { selector, cases } = &e.kind else { return false };
        self.is_string_switch(selector, cases)
            && cases.iter().any(|c| c.is_default)
            && cases.iter().all(|c| is_arrow_value_or_throw(&c.body))
    }

    /// ¿El selector es un `String` y todas las etiquetas son constantes `String` (o `default`)? Solo
    /// entonces bajamos: `case null`, *patterns* o guardas caen fuera.
    fn is_string_switch(&self, selector: &Expr, cases: &[SwitchCase]) -> bool {
        self.is_string(&selector.ty)
            && cases.iter().all(|c| {
                c.is_default
                    || (c.guard.is_none()
                        && !c.labels.is_empty()
                        && c.labels.iter().all(|l| {
                            matches!(l, CaseLabel::Null)
                                || matches!(l, CaseLabel::Constant(e) if matches!(e.kind, ExprKind::StringLit(_)))
                        }))
            })
    }

    /// `switch (color) { case ROJO: … }` sobre un `enum` → `switch ($SwitchMap$Color[color.ordinal()])`
    /// (JLS §14.11, el `$SwitchMap` **fiel a javac**): un array `int[]` sintético, alojado en la clase
    /// anidada `C$1`, indexa `ordinal → índice denso` y se puebla en un `<clinit>` con `try/catch`
    /// `NoSuchFieldError` por constante (robusto ante recompilación separada del enum). Como el switch
    /// sobre `String`, opera directo o dentro de un `Labeled` (brazos que yieldan).
    fn lower_enum_switch(&mut self, s: &mut Stmt) {
        let direct = matches!(s.kind, StmtKind::Switch { .. });
        let in_label =
            matches!(&s.kind, StmtKind::Labeled { body, .. } if matches!(body.kind, StmtKind::Switch { .. }));
        if direct {
            self.enum_switch_here(s);
        } else if in_label {
            if let StmtKind::Labeled { body, .. } = &mut s.kind {
                self.enum_switch_here(body);
            }
        }
    }

    fn enum_switch_here(&mut self, s: &mut Stmt) {
        let StmtKind::Switch { selector, cases } = &s.kind else { return };
        if !self.is_enum_switch(selector, cases) {
            return;
        }
        let Some(RType::Class(enum_id)) = selector.ty.clone() else { return };
        let enum_name = self.table.symbol(enum_id).name.clone();
        let consts = self.enums.get(&enum_id).cloned().unwrap_or_default();
        let field = self.ensure_switchmap(enum_id, &enum_name, &consts);
        let holder_name = self.holder.as_ref().unwrap().name.clone();

        let StmtKind::Switch { selector, cases } = std::mem::replace(&mut s.kind, StmtKind::Empty) else {
            unreachable!()
        };
        // Nuevo selector: `Holder.$SwitchMap$Enum[selector.ordinal()]`.
        let map = ex(ExprKind::Field { expr: Box::new(name(&holder_name)), name: field });
        let ordinal = call(selector, "ordinal", vec![]);
        let new_selector = ex(ExprKind::Index { array: Box::new(map), index: Box::new(ordinal) });
        // Cada `case CONST` → `case <ordinal(CONST) + 1>`; el `default` queda igual.
        let cases = cases
            .into_iter()
            .map(|c| {
                if c.is_default {
                    return c;
                }
                let SwitchCase { labels, is_default, guard, body } = c;
                let labels = labels
                    .into_iter()
                    .map(|l| match &l {
                        CaseLabel::Constant(e) => match &e.kind {
                            ExprKind::Name(n) => match consts.iter().position(|x| x == n) {
                                Some(o) => CaseLabel::Constant(ex(ExprKind::IntLit(o as i64 + 1))),
                                None => l,
                            },
                            _ => l,
                        },
                        _ => l,
                    })
                    .collect();
                SwitchCase { labels, is_default, guard, body }
            })
            .collect();
        s.kind = StmtKind::Switch { selector: new_selector, cases };
    }

    /// `switch (o) { case T v when g -> b; case null -> n; default -> d }` → una cadena de
    /// `instanceof` dentro de un bloque **etiquetado** (§14.11.1):
    ///
    /// ```text
    /// $m: { T0 $t = o;
    ///       if ($t == null) { n; break $m; }            // o `throw new NPE()` si no hay `case null`
    ///       if ($t instanceof T) { T v = (T) $t; if (g) { b; break $m; } }
    ///       …
    ///       d }
    /// ```
    ///
    /// Cada brazo es un `if` **suelto** (no `else if`): así una **guarda que falla** cae al `case`
    /// siguiente, como manda la semántica. El `break` **etiquetado** es lo que sale del switch entero
    /// desde adentro del `if` anidado. Como el `instanceof` del AST no lleva binding, la variable de
    /// patrón se materializa con un *cast* (`T v = (T) $t`).
    fn lower_pattern_switch(&mut self, s: &mut Stmt) {
        let direct = matches!(s.kind, StmtKind::Switch { .. });
        let in_label =
            matches!(&s.kind, StmtKind::Labeled { body, .. } if matches!(body.kind, StmtKind::Switch { .. }));
        if direct {
            self.pattern_switch_here(s);
        } else if in_label {
            if let StmtKind::Labeled { body, .. } = &mut s.kind {
                self.pattern_switch_here(body);
            }
        }
    }

    fn pattern_switch_here(&mut self, s: &mut Stmt) {
        let StmtKind::Switch { cases, .. } = &s.kind else { return };
        if !is_pattern_switch(cases) {
            return;
        }
        let StmtKind::Switch { selector, cases } = std::mem::replace(&mut s.kind, StmtKind::Empty) else {
            unreachable!()
        };
        let tvar = self.fresh("t");
        let label = self.fresh("m");

        // Se reparten los brazos: el de `null`, el `default`, y los patterns (en orden).
        let mut null_body: Option<Stmt> = None;
        let mut default_body: Option<Stmt> = None;
        let mut arms: Vec<(Pattern, Option<Expr>, Stmt)> = Vec::new();
        for c in cases {
            let SwitchCase { labels, is_default, guard, body } = c;
            let SwitchBody::Arrow(b) = body else { continue };
            let body = *b;
            if is_default {
                default_body = Some(body.clone());
            }
            for l in &labels {
                match l {
                    CaseLabel::Null => null_body = Some(body.clone()),
                    CaseLabel::Pattern(p) => arms.push((p.clone(), guard.clone(), body.clone())),
                    CaseLabel::Constant(_) => {}
                }
            }
        }

        let t_ty = match &selector.ty {
            Some(rt) => rtype_to_type(self.table, rt),
            None => Type::Var,
        };
        let mut stmts = vec![local(t_ty, tvar.clone(), selector)];

        // `null`: su brazo si lo hay; si no, NPE (un pattern switch sobre `null` sin `case null` la lanza).
        let on_null = match null_body {
            Some(n) => with_break(n, &label),
            None => st(StmtKind::Throw(ex(ExprKind::NewObject {
                ty: Type::Class("NullPointerException".into()),
                args: Vec::new(),
                body: None,
                outer: None,
            }))),
        };
        stmts.push(st(StmtKind::If { cond: eq_null(name(&tvar)), then: Box::new(on_null), els: None }));

        // Un `if (instanceof)` por pattern, con los bindings adentro (y la guarda, si la hay).
        for (pat, guard, body) in arms {
            let run = with_break(body, &label);
            let inner = match guard {
                Some(g) => st(StmtKind::If { cond: g, then: Box::new(run), els: None }),
                None => run,
            };
            let arm = self.match_pattern(&pat, name(&tvar), inner);
            stmts.push(arm);
        }
        if let Some(d) = default_body {
            stmts.push(d);
        }
        s.kind = StmtKind::Labeled { label, body: Box::new(st(StmtKind::Block(Block(stmts)))) };
    }

    /// Emite el chequeo y los *bindings* de un patrón sobre `src`, con `inner` adentro de todo.
    ///
    /// - **Type pattern** `T v` → `if (src instanceof T) { T v = (T) src; inner }`.
    /// - **Record pattern** `R(p1, p2)` → el mismo `instanceof`, más un temporal con el récord ya
    ///   casteado del que se extrae cada componente por su ***accessor*** (`$r.x()`), y sobre ese
    ///   valor se aplica recursivamente el patrón del componente.
    ///
    /// Los componentes que son *type patterns* se bindean **sin** `instanceof`: cubren el caso de que
    /// el patrón repita el tipo declarado del componente, que es el habitual. Un componente con un
    /// tipo **más angosto** todavía no se chequea.
    fn match_pattern(&mut self, pat: &Pattern, src: Expr, inner: Stmt) -> Stmt {
        match pat {
            Pattern::Type { ty, name: var, .. } => {
                let bind = local(
                    ty.clone(),
                    var.clone(),
                    ex(ExprKind::Cast { ty: ty.clone(), expr: Box::new(src.clone()) }),
                );
                st(StmtKind::If {
                    cond: ex(ExprKind::InstanceOf { expr: Box::new(src), ty: ty.clone(), binding: None, slot: None }),
                    then: Box::new(st(StmtKind::Block(Block(vec![bind, inner])))),
                    els: None,
                })
            }
            Pattern::Record { ty, components } => {
                let rvar = self.fresh("r");
                let accessors = self.record_components(ty);
                // Se arma de adentro hacia afuera: el último componente envuelve a `inner`.
                let mut body = inner;
                for (i, c) in components.iter().enumerate().rev() {
                    let Some(acc) = accessors.get(i) else { continue };
                    let getter = call(name(&rvar), acc, vec![]);
                    body = match c {
                        // Componente simple: se bindea directo, sin volver a testear el tipo.
                        Pattern::Type { ty: cty, name: cvar, .. } => st(StmtKind::Block(Block(vec![
                            local(
                                cty.clone(),
                                cvar.clone(),
                                ex(ExprKind::Cast { ty: cty.clone(), expr: Box::new(getter) }),
                            ),
                            body,
                        ]))),
                        // Componente que vuelve a deconstruir: recursión (y sí testea el tipo).
                        nested => self.match_pattern(nested, getter, body),
                    };
                }
                let bind = local(
                    ty.clone(),
                    rvar,
                    ex(ExprKind::Cast { ty: ty.clone(), expr: Box::new(src.clone()) }),
                );
                st(StmtKind::If {
                    cond: ex(ExprKind::InstanceOf { expr: Box::new(src), ty: ty.clone(), binding: None, slot: None }),
                    then: Box::new(st(StmtKind::Block(Block(vec![bind, body])))),
                    els: None,
                })
            }
        }
    }

    /// Los nombres de los componentes de un `record`, **en orden** — que son también los de sus
    /// *accessors*. Vacío si el tipo no es un record conocido de esta unidad.
    fn record_components(&self, ty: &Type) -> Vec<String> {
        let (Type::Class(n) | Type::Parameterized { base: n, .. }) = ty else { return Vec::new() };
        self.table
            .class(n)
            .and_then(|id| self.records.get(&id))
            .cloned()
            .unwrap_or_default()
    }

    /// ¿El selector es un `enum` (que conocemos) y todas las etiquetas son constantes suyas (o
    /// `default`), sin guardas? Los *patterns*/`case null` caen fuera.
    fn is_enum_switch(&self, selector: &Expr, cases: &[SwitchCase]) -> bool {
        let Some(RType::Class(id)) = &selector.ty else { return false };
        let Some(consts) = self.enums.get(id) else { return false };
        cases.iter().all(|c| {
            c.is_default
                || (c.guard.is_none()
                    && !c.labels.is_empty()
                    && c.labels.iter().all(|l| {
                        matches!(l, CaseLabel::Constant(e)
                            if matches!(&e.kind, ExprKind::Name(n) if consts.contains(n)))
                    }))
        })
    }

    /// Crea (una vez) la clase sintética `C$1` que aloja los `$SwitchMap$X`, registrando su símbolo,
    /// su scope y su nombre en la tabla — para que la re-atribución resuelva `C$1.$SwitchMap$X`.
    fn ensure_holder(&mut self) {
        if self.holder.is_some() {
            return;
        }
        let name = format!("{}$1", self.unit_name);
        let top = self.top_scope;
        let scope = self.table.new_scope(Some(top), None);
        let cid = self.table.new_symbol(Symbol {
            name: name.clone(),
            kind: SymbolKind::Class {
                kind: TypeKind::Class,
                binary: name.clone(),
                extends: None,
                implements: Vec::new(),
                permits: Vec::new(),
                members: scope,
            },
            owner: None,
            modifiers: Vec::new(),
        });
        self.table.set_scope_owner(scope, cid);
        self.table.register_class(&name, cid);
        self.table.define(top, &name, cid);
        // La `Resolved::Class` (aunque sin super/interfaces) para que sea una clase "completa".
        self.table.set_resolved(cid, Resolved::Class { super_type: None, interface_types: Vec::new(), permitted: Vec::new() });
        self.holder = Some(Holder { cid, scope, name, members: Vec::new(), maps: HashMap::new() });
    }

    /// Asegura (idempotente por enum) el campo `$SwitchMap$Enum` en la clase sintética: registra su
    /// símbolo, agrega el `FieldDecl` y el `<clinit>` de población. Devuelve el nombre del campo.
    fn ensure_switchmap(&mut self, enum_id: SymbolId, enum_name: &str, consts: &[String]) -> String {
        self.ensure_holder();
        if let Some(f) = self.holder.as_ref().unwrap().maps.get(&enum_id) {
            return f.clone();
        }
        let field = format!("$SwitchMap${enum_name}");
        let (cid, scope) = {
            let h = self.holder.as_ref().unwrap();
            (h.cid, h.scope)
        };
        let int_arr = Type::Array(Box::new(Type::Prim(PrimType::Int)));
        let fid = self.table.new_symbol(Symbol {
            name: field.clone(),
            kind: SymbolKind::Field { ty: int_arr.clone() },
            owner: Some(cid),
            modifiers: vec![Modifier::Static, Modifier::Final],
        });
        self.table.define(scope, &field, fid);
        // Su tipo resuelto `int[]` — si no, `resolve_name` lo encuentra pero lo descarta.
        self.table.set_resolved(fid, Resolved::Field(RType::Array(Box::new(RType::Prim(PrimType::Int)))));
        let field_decl = Member::Field(FieldDecl {
            doc: None,
            annotations: Vec::new(),
            type_annos: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Static, Modifier::Final],
            ty: int_arr,
            name: field.clone(),
            init: None, // el `<clinit>` lo asigna
        });
        let static_init = Member::StaticInit(switchmap_population(&field, enum_name, consts));
        let h = self.holder.as_mut().unwrap();
        h.members.push(field_decl);
        h.members.push(static_init);
        h.maps.insert(enum_id, field.clone());
        field
    }

    /// ¿`e` es la switch-**expresión** que javac deja con el valor **en la pila**? Ese es el modelo
    /// que emite [`Codegen::switch_expr`]: cada brazo `bipush v; goto join`, y el brazo físicamente
    /// último **cae** al join sin `goto`; el contexto consume el valor (`ireturn`/`istore`). Cuando
    /// esto vale, **no** hay que bajar la expresión a un temporal — el codegen la toma por los
    /// caminos normales. Exige (espejo del predicado `simple` de `switch_expr`, más las condiciones
    /// de un dispatch entero simple): selector `int` (no `String`/`enum`), `default` presente, cada
    /// `case` default o (sin guarda, con etiquetas **constantes enteras**) y **todos** los brazos de
    /// flecha con expresión o `throw`.
    fn is_stack_switch_expr(&self, e: &Expr) -> bool {
        let ExprKind::Switch { selector, cases } = &e.kind else { return false };
        // Solo selector entero (int/short/byte/char): `String`/`enum` los baja su propia pasada.
        if !matches!(
            selector.ty,
            Some(RType::Prim(PrimType::Int | PrimType::Short | PrimType::Byte | PrimType::Char))
        ) {
            return false;
        }
        if !cases.iter().any(|c| c.is_default) {
            return false;
        }
        cases.iter().all(|c| {
            (c.is_default
                || (c.guard.is_none()
                    && !c.labels.is_empty()
                    && c.labels.iter().all(is_int_const_label)))
                && is_arrow_value_or_throw(&c.body)
        })
    }

    /// Baja una switch-**expresión** que aparece como el valor directo de un `return`, un `yield` o
    /// una asignación simple (`x = switch…`). Las dos primeras necesitan un temporal `$s` (más un
    /// bloque que lo declara, asigna y usa); la asignación reescribe el switch para que sus brazos
    /// escriban `x` directamente. Ver [`is_lowerable_switch_expr`] para el subconjunto que bajamos.
    fn lower_switch_stmt(&mut self, s: &mut Stmt) {
        // Una switch-expresión de **pila** (selector `int`, cada brazo deja el valor en la pila y el
        // último cae al join sin `goto`) NO se baja a un temporal: el codegen la consume por los
        // caminos normales — `return`→valor en pila→`ireturn`, `x = switch…`→`store`.
        let ok = match &s.kind {
            StmtKind::Return(Some(e)) | StmtKind::Yield(e) => {
                is_lowerable_switch_expr(e)
                    && e.ty.is_some()
                    && !self.is_stack_switch_expr(e)
                    // Un `return switch(String)` de flecha-valor lo baja `string_switch_expr_tail`
                    // (valor en la pila, sin temporal). No lo hoisteamos a un temporal acá.
                    && !self.is_tail_string_switch_expr(e)
            }
            StmtKind::Expr(e) => {
                is_assign_switch(e)
                    && !matches!(&e.kind, ExprKind::Assign { value, .. } if self.is_stack_switch_expr(value))
            }
            _ => false,
        };
        if !ok {
            return;
        }
        s.kind = match std::mem::replace(&mut s.kind, StmtKind::Empty) {
            StmtKind::Return(e) => {
                let (decl, sw, tmp) = self.hoist_switch(e.unwrap());
                StmtKind::Block(Block(vec![decl, sw, st(StmtKind::Return(Some(name(&tmp))))]))
            }
            StmtKind::Yield(e) => {
                let (decl, sw, tmp) = self.hoist_switch(e);
                StmtKind::Block(Block(vec![decl, sw, st(StmtKind::Yield(name(&tmp)))]))
            }
            StmtKind::Expr(e) => {
                let ExprKind::Assign { target, value, .. } = e.kind else { unreachable!() };
                self.switch_to_stmt(*value, *target)
            }
            _ => unreachable!(),
        };
    }

    /// Declara un temporal `$s` del tipo del switch y arma la `switch`-sentencia que lo asigna en
    /// cada brazo. Devuelve `(decl, switch, nombre)` para intercalarlos antes del uso del temporal.
    fn hoist_switch(&mut self, e: Expr) -> (Stmt, Stmt, String) {
        let tmp = self.fresh("s");
        let ty = rtype_to_type(self.table, e.ty.as_ref().unwrap());
        let decl = st(StmtKind::LocalVar { ty, name: tmp.clone(), init: None, is_final: false, type_annos: Vec::new() });
        let mut sw = st(self.switch_to_stmt(e, name(&tmp)));
        // El switch-**sentencia** generado puede ser sobre `String`/`enum`/*patterns*: hay que bajarlo
        // también. El llamador (`lower_switch_stmt`) ya no lo alcanza porque `sw` queda **dentro de un
        // `Block`**, y los lowerings de `stmt` solo miran el nodo de tope — sin esto, un
        // `return switch (s) { … }` sobre un `String` llegaba **crudo** al emisor (que no lo soporta).
        self.lower_string_switch(&mut sw);
        self.lower_enum_switch(&mut sw);
        self.lower_pattern_switch(&mut sw);
        (decl, sw, tmp)
    }

    /// Convierte una switch-**expresión** en la sentencia que **asigna** su valor a `lvalue`. Los
    /// brazos de **flecha con expresión** (`case X -> v`) se vuelven `case X -> lvalue = v` (la flecha
    /// ya rompe). Los brazos con **bloque** o de **dos puntos** pueden tener `yield` en cualquier
    /// profundidad (dentro de un `if`, un bucle, otro `switch`): cada `yield e` se reescribe a
    /// `{ lvalue = e; break $sw; }` y **todo** el switch se envuelve en una sentencia etiquetada
    /// `$sw:` — el `break` etiquetado es lo que permite saltar afuera desde adentro de un bucle
    /// anidado (un `break` pelado solo saldría del bucle). Si ningún brazo yielda, no hace falta la
    /// etiqueta.
    fn switch_to_stmt(&mut self, e: Expr, lvalue: Expr) -> StmtKind {
        let ExprKind::Switch { selector, cases } = e.kind else { unreachable!() };
        let needs_label = cases.iter().any(|c| !is_arrow_value_or_throw(&c.body));
        let label = needs_label.then(|| self.fresh("sw"));
        let cases = cases
            .into_iter()
            .map(|c| {
                let SwitchCase { labels, is_default, guard, body } = c;
                let body = match body {
                    SwitchBody::Arrow(b) => {
                        SwitchBody::Arrow(Box::new(self.arm_body(*b, &lvalue, label.as_deref())))
                    }
                    SwitchBody::Colon(ss) => SwitchBody::Colon(
                        ss.into_iter()
                            .map(|mut s| {
                                if let Some(l) = &label {
                                    self.rewrite_yields(&mut s, &lvalue, l);
                                }
                                s
                            })
                            .collect(),
                    ),
                };
                SwitchCase { labels, is_default, guard, body }
            })
            .collect();
        let switch = StmtKind::Switch { selector: *selector, cases };
        match label {
            Some(l) => StmtKind::Labeled { label: l, body: Box::new(st(switch)) },
            None => switch,
        }
    }

    /// El cuerpo de un brazo de flecha, reescrito para producir el valor en `lvalue`. Una expresión
    /// suelta es el *yield implícito* de la flecha → asignación (sin `break`: la flecha ya rompe). Un
    /// `throw` queda igual. Un bloque puede llevar `yield` adentro → se reescriben con `break $label`.
    fn arm_body(&self, mut s: Stmt, lvalue: &Expr, label: Option<&str>) -> Stmt {
        if matches!(s.kind, StmtKind::Expr(_)) {
            let StmtKind::Expr(v) = std::mem::replace(&mut s.kind, StmtKind::Empty) else { unreachable!() };
            s.kind = StmtKind::Expr(assign_expr(lvalue.clone(), v));
        } else if !matches!(s.kind, StmtKind::Throw(_)) {
            if let Some(l) = label {
                self.rewrite_yields(&mut s, lvalue, l);
            }
        }
        s
    }

    /// Reescribe cada `yield e` de una sentencia (recursivamente) a `{ lvalue = e; break label; }`.
    /// **No** desciende a switch-**expresiones** anidadas (ya bajadas); un `yield` dentro de un
    /// `switch`-**sentencia** anidado sí es nuestro (la palabra apunta a la switch-expresión más
    /// cercana), así que se recorre.
    fn rewrite_yields(&self, s: &mut Stmt, lvalue: &Expr, label: &str) {
        match &mut s.kind {
            StmtKind::Yield(_) => {
                let StmtKind::Yield(e) = std::mem::replace(&mut s.kind, StmtKind::Empty) else { unreachable!() };
                s.kind = StmtKind::Block(Block(vec![
                    st(StmtKind::Expr(assign_expr(lvalue.clone(), e))),
                    st(StmtKind::Break(Some(label.to_string()))),
                ]));
            }
            StmtKind::Block(b) => b.0.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label)),
            StmtKind::If { then, els, .. } => {
                self.rewrite_yields(then, lvalue, label);
                if let Some(e) = els {
                    self.rewrite_yields(e, lvalue, label);
                }
            }
            StmtKind::While { body, .. }
            | StmtKind::Do { body, .. }
            | StmtKind::For { body, .. }
            | StmtKind::ForEach { body, .. }
            | StmtKind::Labeled { body, .. } => self.rewrite_yields(body, lvalue, label),
            StmtKind::Synchronized { body, .. } => {
                body.0.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label))
            }
            StmtKind::Try { resources, body, catches, finally } => {
                resources.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label));
                body.0.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label));
                for c in catches {
                    c.body.0.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label));
                }
                if let Some(f) = finally {
                    f.0.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label));
                }
            }
            StmtKind::Switch { cases, .. } => {
                for c in cases {
                    match &mut c.body {
                        SwitchBody::Arrow(s) => self.rewrite_yields(s, lvalue, label),
                        SwitchBody::Colon(ss) => {
                            ss.iter_mut().for_each(|s| self.rewrite_yields(s, lvalue, label))
                        }
                    }
                }
            }
            _ => {}
        }
    }

    /// `for (T x : it) body` → un `for` indexado (array) o un `while` sobre un `Iterator`.
    fn lower_for_each(&mut self, s: &mut Stmt) {
        let old = std::mem::replace(&mut s.kind, StmtKind::Empty);
        let StmtKind::ForEach { ty, name: var, iterable, body, .. } = old else { return };
        let is_array = matches!(iterable.ty, Some(RType::Array(_)));
        let body = *body;

        s.kind = if is_array {
            // { var $a = it; int $len = $a.length; for (int $i = 0; $i < $len; $i = $i + 1) { T x = $a[$i]; body } }
            // El `$a.length` se **cachea** en un local (como javac): se calcula una sola vez, no en cada
            // vuelta de la condición. Sin esto el bucle recomputaba `arraylength` por iteración.
            let a = self.fresh("a");
            let len = self.fresh("len");
            let i = self.fresh("i");
            let a_decl = local(Type::Var, a.clone(), iterable);
            let len_decl = st(StmtKind::LocalVar {
                ty: Type::Prim(PrimType::Int),
                name: len.clone(),
                init: Some(ex(ExprKind::Field {
                    expr: Box::new(name(&a)),
                    name: "length".into(),
                })),
                is_final: false,
                type_annos: Vec::new(),
            });
            let i_decl = boxst(StmtKind::LocalVar {
                ty: Type::Prim(PrimType::Int),
                name: i.clone(),
                init: Some(ex(ExprKind::IntLit(0))),
                is_final: false,
                type_annos: Vec::new(),
            });
            let cond = ex(ExprKind::Binary {
                op: BinOp::Lt,
                lhs: Box::new(name(&i)),
                rhs: Box::new(name(&len)),
            });
            let update = ex(ExprKind::Assign {
                op: AssignOp::Assign,
                target: Box::new(name(&i)),
                value: Box::new(ex(ExprKind::Binary {
                    op: BinOp::Add,
                    lhs: Box::new(name(&i)),
                    rhs: Box::new(ex(ExprKind::IntLit(1))),
                })),
            });
            let elem = local(
                ty,
                var,
                ex(ExprKind::Index { array: Box::new(name(&a)), index: Box::new(name(&i)) }),
            );
            let for_loop = st(StmtKind::For {
                init: Some(i_decl),
                cond: Some(cond),
                update: vec![update],
                body: boxst(StmtKind::Block(Block(vec![elem, body]))),
            });
            StmtKind::Block(Block(vec![a_decl, len_decl, for_loop]))
        } else {
            // { var $it = it.iterator(); while ($it.hasNext()) { T x = (T) $it.next(); body } }
            let it = self.fresh("it");
            let it_decl = local(Type::Var, it.clone(), call(iterable, "iterator", vec![]));
            let cond = call(name(&it), "hasNext", vec![]);
            let next = call(name(&it), "next", vec![]);
            // El valor del elemento: `Iterator.next()` da `Object` (borrado), así que se castea al tipo
            // del elemento. Si la variable del `for-each` es **primitiva** (`for (int x : List<Integer>)`),
            // javac castea al **wrapper** (checkcast Integer) y **desboxea** (`intValue()`): se emite
            // `((Integer) $it.next()).intValue()`. Para un tipo referencia basta el `(T) $it.next()`.
            let init = match &ty {
                Type::Prim(p) => {
                    let (wrapper, value_method) = wrapper_and_value(*p);
                    let cast = ex(ExprKind::Cast {
                        ty: Type::Class(wrapper.into()),
                        expr: Box::new(next),
                    });
                    call(cast, value_method, vec![])
                }
                _ => ex(ExprKind::Cast { ty: ty.clone(), expr: Box::new(next) }),
            };
            let elem = local(ty.clone(), var, init);
            let while_loop = st(StmtKind::While {
                cond,
                body: boxst(StmtKind::Block(Block(vec![elem, body]))),
            });
            StmtKind::Block(Block(vec![it_decl, while_loop]))
        };
    }

    /// `assert cond [: msg];` → `if (!$assertionsDisabled && !cond) throw new AssertionError([msg]);`
    ///
    /// El *guard* es lo que hace que las aserciones **no cuesten nada** cuando están deshabilitadas
    /// (que es el default, §14.10): al ser un `static final` que el `<clinit>` fija, el JIT lo pliega
    /// y el `if` entero desaparece. El campo lo agrega [`Desugarer::class`] al terminar la clase.
    fn lower_assert(&mut self, s: &mut Stmt) {
        let old = std::mem::replace(&mut s.kind, StmtKind::Empty);
        let StmtKind::Assert { cond, message } = old else { return };
        let args = message.into_iter().collect();
        let throw = boxst(StmtKind::Throw(ex(ExprKind::NewObject {
            ty: Type::Class("AssertionError".into()),
            args,
            body: None,
            outer: None,
        })));
        let not_cond = ex(ExprKind::Unary { op: UnOp::Not, expr: Box::new(cond), prefix: true });
        let enabled = ex(ExprKind::Unary {
            op: UnOp::Not,
            expr: Box::new(name("$assertionsDisabled")),
            prefix: true,
        });
        // El `&&` cortocircuita: con las aserciones apagadas, `cond` ni se evalúa.
        let guarded = ex(ExprKind::Binary {
            op: BinOp::And,
            lhs: Box::new(enabled),
            rhs: Box::new(not_cond),
        });
        self.needs_assert_guard = true;
        s.kind = StmtKind::If { cond: guarded, then: throw, els: None };
    }

    /// `try (R r = init; …) body [catch…] [finally]` → cada recurso se envuelve con la traducción
    /// del JLS §14.20.3.1 (ver [`wrap_resource`]), anidando de adentro hacia afuera, y los
    /// `catch`/`finally` originales quedan alrededor.
    fn lower_try_resources(&mut self, s: &mut Stmt) {
        let old = std::mem::replace(&mut s.kind, StmtKind::Empty);
        let StmtKind::Try { resources, body, catches, finally } = old else { return };

        // El cuerpo protegido, envolviendo cada recurso del último al primero.
        let mut managed = StmtKind::Block(body);
        for res in resources.into_iter().rev() {
            managed = self.wrap_resource(res, st(managed));
        }
        s.kind = StmtKind::Try {
            resources: vec![],
            body: Block(vec![st(managed)]),
            catches,
            finally,
        };
    }

    /// Envuelve un recurso con la traducción de §14.20.3.1, que **preserva la excepción primaria**:
    /// si el cuerpo lanza `e1` y `close()` lanza `e2`, propaga `e1` con `e2` **suprimida** (en vez de
    /// dejar que el `close` del `finally` tape a `e1`). Produce
    ///
    /// ```text
    /// { R r = init;
    ///   Throwable $p = null;
    ///   try { inner }
    ///   catch (Throwable $t) { $p = $t; throw $t; }
    ///   finally {
    ///     if (r != null) {
    ///       if ($p != null) { try { r.close(); } catch (Throwable $x) { $p.addSuppressed($x); } }
    ///       else            { r.close(); }
    ///     }
    ///   } }
    /// ```
    fn wrap_resource(&mut self, res: Stmt, inner: Stmt) -> StmtKind {
        let rname = match &res.kind {
            StmtKind::LocalVar { name, .. } => name.clone(),
            StmtKind::Expr(e) => match &e.kind {
                ExprKind::Name(n) => n.clone(),
                _ => return StmtKind::Block(Block(vec![res, inner])), // recurso no-nombrable: sin close
            },
            _ => return StmtKind::Block(Block(vec![res, inner])),
        };
        let primary = self.fresh("p");
        let caught = self.fresh("t");
        let supp = self.fresh("x");
        let throwable = || Type::Class("Throwable".into());
        let close = || st(StmtKind::Expr(call(name(&rname), "close", vec![])));

        // Throwable $p = null;  — guarda la excepción primaria (la del cuerpo).
        let primary_decl = local(throwable(), primary.clone(), ex(ExprKind::Null));

        // catch (Throwable $t) { $p = $t; throw $t; }  — la registra y la relanza intacta.
        let record_and_rethrow = CatchClause {
            types: vec![throwable()],
            name: caught.clone(),
            body: Block(vec![
                st(StmtKind::Expr(assign_expr(name(&primary), name(&caught)))),
                st(StmtKind::Throw(name(&caught))),
            ]),
            slot: None,
            is_final: false,
        };

        // try { r.close(); } catch (Throwable $x) { $p.addSuppressed($x); }
        let close_suppressing = st(StmtKind::Try {
            resources: vec![],
            body: Block(vec![close()]),
            catches: vec![CatchClause {
                types: vec![throwable()],
                name: supp.clone(),
                body: Block(vec![st(StmtKind::Expr(call(name(&primary), "addSuppressed", vec![name(&supp)])))]),
                slot: None,
                is_final: false,
            }],
            finally: None,
        });

        // if ($p != null) { close-suprimiendo } else { r.close(); }
        let close_dispatch = st(StmtKind::If {
            cond: ne_null(name(&primary)),
            then: Box::new(st(StmtKind::Block(Block(vec![close_suppressing])))),
            els: Some(Box::new(st(StmtKind::Block(Block(vec![close()]))))),
        });

        // if (r != null) { close_dispatch }  — un recurso null no se cierra.
        let null_guard = st(StmtKind::If {
            cond: ne_null(name(&rname)),
            then: Box::new(st(StmtKind::Block(Block(vec![close_dispatch])))),
            els: None,
        });

        let managed = st(StmtKind::Try {
            resources: vec![],
            body: Block(vec![inner]),
            catches: vec![record_and_rethrow],
            finally: Some(Block(vec![null_guard])),
        });
        StmtKind::Block(Block(vec![res, primary_decl, managed]))
    }

    // ---- azúcar de expresiones ----

    /// Baja una expresión en **posición de descarte** (una sentencia-expresión, o el `update` de un
    /// `for`): ahí `x++`/`++x` (y `--`) son exactamente `x += 1`/`x -= 1` — como el valor no se usa,
    /// pre y post coinciden. Se reescribe a la asignación compuesta y [`lower_compound`] hace el
    /// resto (incluido el cast de reducción de §15.26.2, y el reparto puro/impuro del *lvalue*). En
    /// posición de **valor** (`y = x++`), el `++`/`--` **no** se toca: lo resuelve el codegen
    /// (`iinc` para un local, *read-modify-write* con `dup` para un campo/elemento).
    fn discard_expr(&mut self, e: &mut Expr) {
        if let ExprKind::Unary { op: UnOp::Inc | UnOp::Dec, .. } = &e.kind {
            let old = std::mem::replace(&mut e.kind, ExprKind::Null);
            let ExprKind::Unary { op, expr: operand, .. } = old else { return };
            let assign_op = if op == UnOp::Inc { AssignOp::Add } else { AssignOp::Sub };
            e.kind = ExprKind::Assign {
                op: assign_op,
                target: operand,
                value: Box::new(ex(ExprKind::IntLit(1))),
            };
        }
        self.expr(e);
    }

    fn expr(&mut self, e: &mut Expr) {
        // La concatenación se aplana **antes** de descender: si bajáramos primero los hijos, un
        // `a + b` interno ya sería un StringBuilder y no se podría encadenar con el de afuera.
        if self.is_string_concat(e) {
            self.lower_concat(e);
            return;
        }
        // Una **lambda** con target funcional resuelto se baja a un método sintético + un nodo
        // `Indy` (LambdaToMethod). Sin target resuelto queda como estaba y la corta la barrera del
        // emisor. Sus capturas (nodos del método envolvente) se dejan sin tocar: el `invokedynamic`
        // las empuja tal cual.
        if matches!(e.kind, ExprKind::Lambda { .. }) {
            self.lower_lambda(e);
            return;
        }
        // Una **referencia a método** con target funcional resuelto se baja a un nodo `Indy` cuyo
        // `MethodHandle` apunta al método/constructor **real** —sin sintetizar nada—. Las formas que
        // no sabemos bajar (array `int[]::new`, `super::m`) quedan como `MethodRef` y las corta el
        // emisor.
        if matches!(e.kind, ExprKind::MethodRef { .. }) {
            self.lower_method_ref(e);
            return;
        }
        // Dentro de una interna de instancia, un acceso sin cualificar a un miembro de la clase
        // envolvente (`f`, `m()`) se reescribe a `this$0.f` / `this$0.m()` **antes** de descender.
        self.rewrite_enclosing_access(e);
        // Dentro de una clase local, un `Name` a un local capturado → `this.val$nombre`.
        self.rewrite_captured_local(e);
        match &mut e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs);
                self.expr(rhs);
            }
            ExprKind::Unary { expr, .. } => self.expr(expr),
            ExprKind::Assign { target, value, .. } => {
                self.expr(target);
                self.expr(value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond);
                self.expr(then);
                self.expr(els);
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.expr(t);
                }
                args.iter_mut().for_each(|a| self.expr(a));
            }
            ExprKind::Field { expr, .. } => self.expr(expr),
            ExprKind::Index { array, index } => {
                self.expr(array);
                self.expr(index);
            }
            ExprKind::Cast { expr, .. } | ExprKind::InstanceOf { expr, .. } => self.expr(expr),
            ExprKind::NewObject { args, outer, .. } => {
                if let Some(o) = outer {
                    self.expr(o);
                }
                args.iter_mut().for_each(|a| self.expr(a));
            }
            ExprKind::NewArray { dims, init, .. } => {
                dims.iter_mut().flatten().for_each(|d| self.expr(d));
                if let Some(es) = init {
                    es.iter_mut().for_each(|e| self.expr(e));
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.expr(selector);
                for c in cases {
                    if let Some(g) = &mut c.guard {
                        self.expr(g);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(s) => self.stmt(s),
                        SwitchBody::Colon(ss) => ss.iter_mut().for_each(|s| self.stmt(s)),
                    }
                }
            }
            _ => {}
        }
        // Asignación compuesta, varargs, y el `this` de un `new Inner()`: se reescriben **después**
        // de bajar los hijos.
        match &e.kind {
            ExprKind::Assign { op, .. } if *op != AssignOp::Assign => self.lower_compound(e),
            ExprKind::Call { .. } => self.lower_varargs(e),
            ExprKind::QualifiedThis(_) => self.lower_qualified_this(e),
            ExprKind::NewObject { body: None, .. } => {
                // Los `val$` primero; el `this` de `this$0` se antepone después, quedando de cabecera
                // `new L(this, val$…, args)`.
                self.add_local_captures_arg(e);
                self.add_outer_arg(e);
                // Y el varargs **al final**, no antes: los dos pasos de arriba anteponen argumentos
                // sintéticos, y el símbolo del constructor de una interna ya lleva esos parámetros
                // en su firma resuelta. Empaquetar primero contaría los fijos contra una lista de
                // argumentos a la que todavía le faltan los sintéticos y partiría en el lugar
                // equivocado (finding #328).
                self.lower_varargs(e);
            }
            _ => {}
        }
    }

    /// `f(1, 2, 3)` con `f(int... xs)` → `f(new int[]{1, 2, 3})`. No toca la llamada si ya se pasó
    /// un array directamente (`f(arr)`).
    ///
    /// Vale igual para un `new X(1, 2, 3)` contra `X(int... xs)`: un constructor varargs se invoca
    /// con las mismas reglas que un método (§15.9.3 delega en §15.12.2), y hasta #327 esta bajada
    /// solo miraba `Call` — el `new` se emitía con los argumentos sueltos contra un descriptor que
    /// esperaba el array.
    fn lower_varargs(&mut self, e: &mut Expr) {
        let Some(Binding::Method(m)) = e.binding else { return };
        // Un método **polimórfico de firma** se declara `(Object...)` y no se llama como un varargs
        // (JLS §15.12.3): sus argumentos van al sitio con su tipo estático, sin array y sin boxear.
        // Empaquetarlos acá era lo que hacía que `vh.set(9)` emitiera
        // `set:([Ljava/lang/Object;)V` en vez de `set:(I)V`, un sitio que la JVM real rechaza con
        // `WrongMethodTypeException`.
        if super::codegen::signature_polymorphic(self.table, m) {
            return;
        }
        let Some(Resolved::Method { params, varargs: true, .. }) = self.table.resolved(m) else {
            return;
        };
        let params = params.clone(); // suelta el préstamo de la tabla
        let fixed = params.len().saturating_sub(1);
        let args = match &mut e.kind {
            ExprKind::Call { args, .. } => args,
            ExprKind::NewObject { args, .. } => args,
            _ => return,
        };
        if args.len() < fixed {
            return; // llamada mal formada: no la tocamos
        }
        // ¿Se pasó el array directamente? (aridad exacta y el último arg encaja en el array).
        if args.len() == params.len() {
            if let (Some(last), Some(vararg)) = (args.last().and_then(|a| a.ty.as_ref()), params.last()) {
                if varargs_passthrough(self.table, last, vararg) {
                    return;
                }
            }
        }
        let RType::Array(elem) = &params[fixed] else { return };
        let elem = elem.as_ref().clone();
        let trailing = args.split_off(fixed);
        // Para un varargs **genérico** (`<T> f(T...)`), el elemento del array sintético es el tipo
        // **inferido** de `T`, no su borrado a `Object`: javac emite `new String[]{…}` para
        // `f("a","b")`. Se aproxima con el *lub* de los tipos de los argumentos del *spread* (para
        // argumentos homogéneos, ese lub es su propio tipo). Un varargs no genérico (`int...`,
        // `String...`) conserva su elemento declarado.
        let elem_ty = if matches!(elem, RType::TypeVar(_)) {
            let arg_tys: Vec<RType> = trailing.iter().filter_map(|a| a.ty.clone()).collect();
            if !arg_tys.is_empty() && arg_tys.len() == trailing.len() {
                rtype_to_type(self.table, &types::lub(self.table, &arg_tys))
            } else {
                rtype_to_type(self.table, &elem)
            }
        } else {
            rtype_to_type(self.table, &elem)
        };
        args.push(ex(ExprKind::NewArray { elem: elem_ty, dims: vec![None], init: Some(trailing) }));
    }

    fn is_string_concat(&self, e: &Expr) -> bool {
        matches!(e.kind, ExprKind::Binary { op: BinOp::Add, .. }) && self.is_string(&e.ty)
    }

    fn is_string(&self, ty: &Option<RType>) -> bool {
        matches!(ty, Some(RType::Class(id)) if self.table.symbol(*id).name == "String")
    }

    /// `a + b + c` (concatenación) → un `invokedynamic makeConcatWithConstants`, como javac 9+
    /// (*StringConcat*). Se aplana la cadena de `+` y cada operando se clasifica:
    ///
    /// - **Dinámico** (no constante): se marca con `` en la receta, se **empuja** en el call site
    ///   (pasa a `captures`) y su tipo se suma a los parámetros del descriptor.
    /// - **Constante literal** (String/char/bool/int/long): su texto se **embebe** directamente en la
    ///   receta (javac lo pasaría como argumento estático, pero embeberlo produce el mismo resultado y
    ///   evita entradas en el pool). Float/double quedan dinámicos por ahora.
    ///
    /// Si **todos** los operandos son constantes, no hay call site: se pliega a un solo `StringLit`
    /// (javac emite un `ldc`, no un indy vacío).
    fn lower_concat(&mut self, e: &mut Expr) {
        let ty = e.ty.clone();
        let old = std::mem::replace(&mut e.kind, ExprKind::Null);
        let mut operands = Vec::new();
        self.flatten_concat(Expr { kind: old, pos: e.pos, ty, binding: None, type_annos: Vec::new() }, &mut operands);
        // Bajar cada operando (puede tener su propia azúcar anidada) antes de clasificarlo.
        operands.iter_mut().for_each(|op| self.expr(op));

        let mut recipe = String::new();
        let mut dyn_descs = String::new();
        let mut captures: Vec<Expr> = Vec::new();
        for op in operands {
            match concat_const(&op.kind) {
                // Constante: se embebe su texto en la receta. Un literal que ya contenga un marcador
                // de la receta se degrada a dinámico para no corromperla.
                Some(text) if !text.contains([CONCAT_TAG_ARG, CONCAT_TAG_CONST]) => {
                    recipe.push_str(&text);
                }
                _ => {
                    recipe.push(CONCAT_TAG_ARG);
                    // #282: una referencia que no es ya un `String` viaja al call site **convertida**
                    // — el codegen le mete el `String.valueOf(Object)` justo despues de empujarla,
                    // con este mismo predicado. Es lo que hace javac, y es lo que el bootstrap
                    // espera: `StringConcatFactory` no llama a `toString()` por su cuenta.
                    if concat_needs_value_of(self.table, &op.ty) {
                        dyn_descs.push_str("Ljava/lang/String;");
                    } else {
                        dyn_descs.push_str(
                            &op.ty.as_ref().map_or_else(|| "Ljava/lang/Object;".to_string(), |t| rtype_desc(self.table, t)),
                        );
                    }
                    captures.push(op);
                }
            }
        }

        // Todos constantes: sin call site, un literal plegado (`"a" + "b"` → `ldc "ab"`).
        if captures.is_empty() {
            e.kind = ExprKind::StringLit(recipe);
            return;
        }

        let info = IndyCall {
            name: "makeConcatWithConstants".to_string(),
            descriptor: format!("({dyn_descs})Ljava/lang/String;"),
            bootstrap_owner: "java/lang/invoke/StringConcatFactory".to_string(),
            bootstrap_name: "makeConcatWithConstants".to_string(),
            bootstrap_desc: STRING_CONCAT_DESC.to_string(),
            bootstrap_args: vec![BootstrapArg::Str(recipe)],
        };
        e.kind = ExprKind::Indy { info: Box::new(info), captures };
        // `e.ty` sigue siendo `String`: no se toca.
    }

    /// Aplana una cadena de `+` de tipo `String` en sus operandos hoja (izquierda a derecha). Se
    /// detiene en el primer nodo que no sea un `+` de `String` (`1 + 2 + "x"` → `[1+2, "x"]`).
    fn flatten_concat(&self, e: Expr, out: &mut Vec<Expr>) {
        if self.is_string_concat(&e) {
            if let ExprKind::Binary { lhs, rhs, .. } = e.kind {
                self.flatten_concat(*lhs, out);
                self.flatten_concat(*rhs, out);
                return;
            }
        }
        out.push(e);
    }

    /// `x op= y` → `x = (T)(x op y)`, duplicando el destino — solo si es **re-evaluable** sin
    /// efectos ([`is_pure`]). El cast reproduce la reducción implícita (`byte b; b += 1;` es
    /// `b = (byte)(b + 1)`); para `String` (concat) no hace falta.
    fn lower_compound(&mut self, e: &mut Expr) {
        let old = std::mem::replace(&mut e.kind, ExprKind::Null);
        let ExprKind::Assign { op, target, value } = old else { return };
        if !is_pure(&target) {
            e.kind = ExprKind::Assign { op, target, value }; // destino con efectos: al codegen
            return;
        }
        let read = target.clone();
        let mut combined = ex(ExprKind::Binary { op: compound_binop(op), lhs: read, rhs: value });
        // El tipo de `E1 op= E2` es el de `E1`; hay que decorarlo para que un `s += x` se reconozca
        // como concatenación y también se baje.
        combined.ty = target.ty.clone();
        self.expr(&mut combined);
        let value = match &target.ty {
            Some(RType::Prim(p)) => {
                Box::new(ex(ExprKind::Cast { ty: Type::Prim(*p), expr: Box::new(combined) }))
            }
            _ => Box::new(combined),
        };
        e.kind = ExprKind::Assign { op: AssignOp::Assign, target, value };
    }

    /// Baja una **lambda** (§15.27) a un método sintético `lambda$…` + un nodo [`ExprKind::Indy`]
    /// (LambdaToMethod, el `LambdaToMethod.java` de javac):
    ///
    /// - **Captura**: los locales del método envolvente que el cuerpo referencia se vuelven los
    ///   parámetros de cabecera del método sintético; si usa `this` (o un miembro de instancia), el
    ///   método es de **instancia** y el receptor viaja como primer argumento del call site.
    /// - **Implementación**: el cuerpo de la lambda pasa a ser el del método (una lambda de
    ///   expresión se envuelve en `return e;`, salvo SAM `void`). La re-atribución le asigna slots en
    ///   su propio frame, resolviendo por nombre las capturas (ahora parámetros) y `this`.
    /// - **Call site**: el `invokedynamic` empuja las capturas; su *bootstrap* es
    ///   `LambdaMetafactory.metafactory`, con los `MethodType` borrado/instanciado y el `MethodHandle`
    ///   de la implementación como argumentos estáticos (§4.7.23).
    /// Reemplaza por su *erasure* las variables de tipo que **no se pueden nombrar acá**: las que
    /// no son ni del método envolvente ni de la clase envolvente.
    ///
    /// Es el arreglo del #306. La sustitución del SAM resuelve las variables de la **interfaz**,
    /// no las que entran por el **tipo esperado**. Un
    /// `<X extends Throwable> T orElseThrow(Supplier<? extends X>)` al que se le pasa una lambda
    /// deja el retorno instanciado en `X`, y ese `X` es de un método de **otra clase**: acá no hay
    /// declaración a la que apunte. El método sintético salía declarando un retorno `X` que no
    /// resuelve y el generador cortaba con "no puede resolver el tipo `X`" — un programa Java
    /// correcto que no compilaba.
    ///
    /// Nameable es exactamente lo que ese método sintético puede escribir en su firma: los `<T>` del
    /// método que lo contiene —que se le copian al `type_params` (#282)— y los de la clase.
    ///
    /// Borrar no es una salida por la tangente: el descriptor usa la *erasure* igual (§4.6), así que
    /// el bytecode sale idéntico. Lo único que se pierde es precisión en el `Signature` de un método
    /// privado sintético, que no lo lee nadie. Por lo mismo es inocuo que una variable de una clase
    /// **envolvente** (el `T` del outer, usable desde una inner) se borre también, aunque sí sea
    /// nombrable: de más, y sin consecuencia.
    fn erase_unnameable(&self, t: &RType) -> RType {
        match t {
            RType::TypeVar(id) => {
                let del_metodo = self
                    .cur_method_type_params
                    .iter()
                    .any(|p| p.name == self.table.symbol(*id).name);
                let de_la_clase = self.table.symbol(*id).owner == self.cur_class;
                if del_metodo || de_la_clase {
                    t.clone()
                } else {
                    types::erasure(self.table, t)
                }
            }
            RType::Array(e) => RType::Array(Box::new(self.erase_unnameable(e))),
            _ => t.clone(),
        }
    }

    fn lower_lambda(&mut self, e: &mut Expr) {
        // Sin target funcional resuelto no hay contra qué bajar: queda como `Lambda` y la corta el
        // emisor.
        let Some(iface) = e.ty.clone() else { return };
        let Some(iface_id) = types::erased_id(&iface) else { return };
        let Some(sam) = functional_sam(self.table, iface_id) else { return };
        let Some(Resolved::Method { params: sam_params, ret: sam_ret, .. }) =
            self.table.resolved(sam)
        else {
            return;
        };
        let (sam_params, sam_ret) = (sam_params.clone(), sam_ret.clone());
        let sam_name = self.table.symbol(sam).name.clone();
        // Los tipos **instanciados** del SAM (los que la lambda ve de verdad): se sustituyen los
        // parámetros de tipo de la interfaz (`Function<Integer,Integer>` ⇒ `(Integer)Integer`).
        // Va por `subst_for` y no por `subst_of` porque el SAM puede estar declarado en una
        // **superinterfaz**: el de `BinaryOperator<T>` es el `apply` de `BiFunction<T,T,T>`, y sus
        // tipos hablan del `T`/`U`/`R` de *BiFunction*. Con la sustitución de la interfaz sola esas
        // variables sobrevivían sin sustituir, y el método sintético quedaba declarando un retorno
        // `R` que no es ningún tipo — invisible porque la erasure lo borra a `Object` igual (#208).
        let subst = types::subst_for(self.table, &iface, sam_owner(self.table, sam));
        let inst_params: Vec<RType> =
            sam_params.iter().map(|p| types::substitute(p, &subst)).collect();
        let inst_ret = types::substitute(&sam_ret, &subst);
        // Y se borran las variables de tipo que el sintético no puede nombrar (#306).
        let inst_params: Vec<RType> =
            inst_params.iter().map(|t| self.erase_unnameable(t)).collect();
        let inst_ret = self.erase_unnameable(&inst_ret);

        let ExprKind::Lambda { params, body } = std::mem::replace(&mut e.kind, ExprKind::Null) else {
            return;
        };
        let mut body = *body;
        // Bajar el azúcar del **cuerpo** antes de extraerlo (concatenación, lambdas anidadas…).
        //
        // El cuerpo-expresión de una lambda con SAM `void` es una **posición de descarte**: su valor
        // no se usa, igual que en una sentencia-expresión. Por eso va por `discard_expr`, que es
        // donde `x++` se reescribe a `x += 1` y `lower_compound` reparte el *lvalue*.
        //
        // Con `expr` a secas —lo que hacía— el `++` llegaba crudo al generador, y para un **elemento
        // de arreglo** se emitía algo que no incrementaba nada: `() -> c[1]++` era un no-op, mientras
        // `() -> { c[1]++; }` andaba. Una diferencia entre dos formas que el JLS declara
        // equivalentes, sin error ni aviso — el peor tipo de bug (#308).
        match &mut body {
            LambdaBody::Expr(be) if matches!(inst_ret, RType::Void) => self.discard_expr(be),
            LambdaBody::Expr(be) => self.expr(be),
            LambdaBody::Block(b) => self.block(b),
        }

        // --- análisis de capturas ---
        let mut cap = Captures {
            table: self.table,
            declared: params.iter().map(|p| p.name.clone()).collect(),
            free: Vec::new(),
            uses_this: false,
        };
        cap.scan_body(&body);
        let free = cap.free;
        // Solo se captura `this` si además existe (jamás en un contexto estático).
        let uses_this = cap.uses_this && self.has_this;

        let owner = self.cur_class;
        let owner_internal = owner.map(|c| internal_name(self.table, c)).unwrap_or_default();

        // --- método sintético ---
        let impl_name = format!("lambda${}${}", self.cur_method, self.lambda_ordinal);
        self.lambda_ordinal += 1;
        // Parámetros: primero las capturas (por su tipo resuelto), después los de la lambda con su
        // tipo **instanciado** (el `x` de `x -> …` deja de ser inferido).
        let mut method_params: Vec<Param> = free
            .iter()
            .map(|(n, rt)| synth_param(rtype_to_type(self.table, rt), n.clone()))
            .collect();
        for (p, it) in params.iter().zip(&inst_params) {
            method_params.push(synth_param(rtype_to_type(self.table, it), p.name.clone()));
        }
        let return_type = rtype_to_type(self.table, &inst_ret);
        // El cuerpo: una lambda de expresión rinde su valor (`return e;`), salvo SAM `void`.
        let method_body = match body {
            LambdaBody::Block(b) => b,
            LambdaBody::Expr(be) => {
                let stmt = if matches!(inst_ret, RType::Void) {
                    st(StmtKind::Expr(*be))
                } else {
                    st(StmtKind::Return(Some(*be)))
                };
                Block(vec![stmt])
            }
        };
        let mut modifiers = vec![Modifier::Private];
        if !uses_this {
            modifiers.push(Modifier::Static);
        }
        self.lambda_methods.push(Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            return_annos: Vec::new(),
            pos: e.pos,
            modifiers,
            // Los del método envolvente. Un parámetro de la lambda tipado con una variable de
            // tipo de ese método —`(T a, T b) -> …` dentro de `<T extends Comparable<…>> …`—
            // solo se puede *borrar* bien si `T` sigue siendo nombrable acá: sin esto el emisor
            // no la resolvía y escribía `Object`, mientras el `MethodHandle` del pool escribía
            // la cota. Los dos descriptores del MISMO método dejaban de coincidir y el call site
            // moría con `NoSuchMethodError` (#282).
            type_params: self.cur_method_type_params.clone(),
            return_type,
            name: impl_name.clone(),
            params: method_params,
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(method_body),
            is_constructor: false,
        }));

        // --- descriptores (cadenas listas para el pool) y bootstrap ---
        let free_descs: String = free.iter().map(|(_, rt)| rtype_desc(self.table, rt)).collect();
        let inst_param_descs: String =
            inst_params.iter().map(|t| rtype_desc(self.table, t)).collect();
        let sam_param_descs: String =
            sam_params.iter().map(|t| rtype_desc(self.table, t)).collect();
        let inst_ret_desc = rtype_desc(self.table, &inst_ret);
        let iface_desc = format!("L{iface_internal};", iface_internal = internal_name(self.table, iface_id));

        // Implementación: `(capturas ++ paramsInstanciados) retInstanciado` — SIN el receptor (un
        // método de instancia no lo lleva en su descriptor; el `MethodHandle` se lo suma solo).
        let impl_desc = format!("({free_descs}{inst_param_descs}){inst_ret_desc}");
        // Call site: `([receptor] ++ capturas) InterfazFuncional`.
        let receiver = if uses_this { format!("L{owner_internal};") } else { String::new() };
        let site_desc = format!("({receiver}{free_descs}){iface_desc}");
        // `MethodType` borrado (arg 1) e instanciado (arg 3) del SAM.
        let sam_desc =
            format!("({sam_param_descs}){ret}", ret = rtype_desc(self.table, &sam_ret));
        let inst_desc = format!("({inst_param_descs}){inst_ret_desc}");

        let info = metafactory_indy(
            sam_name,
            site_desc,
            sam_desc,
            inst_desc,
            if uses_this { REF_INVOKE_SPECIAL } else { REF_INVOKE_STATIC },
            owner_internal,
            impl_name,
            impl_desc,
        );

        // --- argumentos de captura, en el orden del descriptor del call site ---
        let mut captures: Vec<Expr> = Vec::new();
        if uses_this {
            let mut this = ex(ExprKind::This);
            this.ty = owner.map(RType::Class);
            captures.push(this);
        }
        for (n, rt) in &free {
            let mut nm = name(n);
            nm.ty = Some(rt.clone());
            captures.push(nm);
        }

        e.kind = ExprKind::Indy { info: Box::new(info), captures };
        // `e.ty` sigue siendo la interfaz funcional: no se toca.
    }

    /// Baja una **referencia a método** (§15.13) a un nodo [`ExprKind::Indy`]. A diferencia de la
    /// lambda, **no** se sintetiza nada: el `MethodHandle` apunta al método o constructor **real**.
    /// Cuatro formas (§15.13.1), que se distinguen mirando si el *qualifier* es un **tipo** o un
    /// **valor** (su binding) y si el método es `static`:
    ///
    /// - `C::sm` estático → `REF_invokeStatic`, sin captura.
    /// - `C::im` de instancia (no ligado) → `REF_invokeVirtual`/`Interface`; el **receptor es el
    ///   primer parámetro del SAM**, sin captura.
    /// - `expr::im` (ligado) → ídem, pero el receptor es el **valor** del qualifier, que se **captura**
    ///   (se evalúa una sola vez, §15.13.3).
    /// - `C::new` → `REF_newInvokeSpecial`, sin captura.
    ///
    /// Las formas con qualifier de **tipo sintáctico** (`int[]::new`) y `super::m` no se bajan: quedan
    /// como `MethodRef` y las corta la barrera del emisor.
    fn lower_method_ref(&mut self, e: &mut Expr) {
        let Some(iface) = e.ty.clone() else { return };
        let Some(iface_id) = types::erased_id(&iface) else { return };
        let Some(sam) = functional_sam(self.table, iface_id) else { return };
        let Some(Resolved::Method { params: sam_params, ret: sam_ret, .. }) = self.table.resolved(sam)
        else {
            return;
        };
        let (sam_params, sam_ret) = (sam_params.clone(), sam_ret.clone());
        let sam_name = self.table.symbol(sam).name.clone();
        let sam_arity = sam_params.len();
        // Igual que en la lambda: el SAM puede venir de una superinterfaz (ver `lower_lambda`).
        let subst = types::subst_for(self.table, &iface, sam_owner(self.table, sam));
        let inst_params: Vec<RType> =
            sam_params.iter().map(|p| types::substitute(p, &subst)).collect();
        let inst_ret = types::substitute(&sam_ret, &subst);
        // Y se borran las variables de tipo que el sintético no puede nombrar (#306).
        let inst_params: Vec<RType> =
            inst_params.iter().map(|t| self.erase_unnameable(t)).collect();
        let inst_ret = self.erase_unnameable(&inst_ret);

        // Inspeccionar el qualifier **sin** moverlo (si no se puede bajar, se deja el `MethodRef`).
        let ExprKind::MethodRef { qualifier, name, .. } = &e.kind else { return };
        let name = name.clone();
        let MethodRefQualifier::Expr(qe) = qualifier.as_ref() else { return };
        let Some(qual_ty) = qe.ty.clone() else { return };
        let Some(qual_id) = types::erased_id(&qual_ty) else { return };
        // `Binding::Class` ⇒ el qualifier nombra un **tipo** (`String::…`); si no, es un **valor**.
        let is_type = matches!(qe.binding, Some(Binding::Class(_)));
        let iface_owner = matches!(
            &self.table.symbol(qual_id).kind,
            SymbolKind::Class { kind: TypeKind::Interface, .. }
        );

        // Elegir el objetivo y su forma según aridad y `static`.
        let (target, variant) = if name == "new" {
            match constructors(self.table, qual_id)
                .into_iter()
                .find(|&m| self.arity_of(m) == Some(sam_arity))
            {
                Some(m) => (m, MRefKind::Ctor),
                None => return,
            }
        } else {
            let mut chosen = None;
            for m in candidates(self.table, qual_id, &name) {
                let stat = self.is_static(m);
                let ar = self.arity_of(m);
                let v = if !is_type {
                    // Valor: instancia ligada, aridad igual a la del SAM.
                    (!stat && ar == Some(sam_arity)).then_some(MRefKind::BoundInstance)
                } else if stat && ar == Some(sam_arity) {
                    Some(MRefKind::Static)
                } else if !stat && sam_arity >= 1 && ar == Some(sam_arity - 1) {
                    // Tipo + método de instancia: no ligado, el receptor es el 1.er parámetro del SAM.
                    Some(MRefKind::UnboundInstance)
                } else {
                    None
                };
                if let Some(v) = v {
                    chosen = Some((m, v));
                    break;
                }
            }
            match chosen {
                Some(c) => c,
                None => return,
            }
        };

        let Some(Resolved::Method { params: tparams, ret: tret, .. }) = self.table.resolved(target)
        else {
            return;
        };
        let (tparams, tret) = (tparams.clone(), tret.clone());
        let owner_internal = internal_name(self.table, qual_id);
        let tparam_descs: String = tparams.iter().map(|p| rtype_desc(self.table, p)).collect();
        let tret_desc = rtype_desc(self.table, &tret);

        let (impl_kind, impl_name, impl_desc) = match variant {
            MRefKind::Ctor => {
                (REF_NEW_INVOKE_SPECIAL, "<init>".to_string(), format!("({tparam_descs})V"))
            }
            MRefKind::Static => {
                (REF_INVOKE_STATIC, name.clone(), format!("({tparam_descs}){tret_desc}"))
            }
            MRefKind::BoundInstance | MRefKind::UnboundInstance => {
                let k = if iface_owner { REF_INVOKE_INTERFACE } else { REF_INVOKE_VIRTUAL };
                (k, name.clone(), format!("({tparam_descs}){tret_desc}"))
            }
        };

        // Captura y descriptor del call site: solo la forma **ligada** captura (su receptor).
        let iface_desc = format!("L{};", internal_name(self.table, iface_id));
        let (captures, site_params) = match variant {
            MRefKind::BoundInstance => (vec![(**qe).clone()], format!("L{owner_internal};")),
            _ => (Vec::new(), String::new()),
        };
        let site_desc = format!("({site_params}){iface_desc}");

        let sam_param_descs: String =
            sam_params.iter().map(|t| rtype_desc(self.table, t)).collect();
        let sam_desc = format!("({sam_param_descs}){}", rtype_desc(self.table, &sam_ret));
        let inst_param_descs: String =
            inst_params.iter().map(|t| rtype_desc(self.table, t)).collect();
        let inst_desc = format!("({inst_param_descs}){}", rtype_desc(self.table, &inst_ret));

        let info =
            metafactory_indy(sam_name, site_desc, sam_desc, inst_desc, impl_kind, owner_internal, impl_name, impl_desc);
        e.kind = ExprKind::Indy { info: Box::new(info), captures };
    }

    /// La aridad (cantidad de parámetros) de un método resuelto, o `None` si no lo está.
    fn arity_of(&self, m: SymbolId) -> Option<usize> {
        match self.table.resolved(m) {
            Some(Resolved::Method { params, .. }) => Some(params.len()),
            _ => None,
        }
    }

    fn is_static(&self, m: SymbolId) -> bool {
        self.table.symbol(m).modifiers.contains(&Modifier::Static)
    }

    /// Los tres métodos de `Object` de un `record` (§8.10.2), cada uno un **único** `invokedynamic` a
    /// `ObjectMethods.bootstrap`. Ese bootstrap toma la `Class` del record, la cadena de nombres de
    /// componentes (`;`-separada) y un `MethodHandle` `REF_getField` por componente, y devuelve el
    /// `CallSite` con la igualdad/hash/formato **estructural**. Se saltea el que la persona haya
    /// declarado a mano (§8.10.4).
    fn record_object_methods(&mut self, class: &ClassDecl, cid: SymbolId) -> Vec<Member> {
        let rec = internal_name(self.table, cid);
        // Argumentos estáticos compartidos por los tres: la `Class` del record, los nombres, y un
        // getter (`REF_getField` al campo privado) por componente.
        let names = class.components.iter().map(|c| c.name.clone()).collect::<Vec<_>>().join(";");
        let mut args = vec![BootstrapArg::Class(rec.clone()), BootstrapArg::Str(names)];
        for c in &class.components {
            args.push(BootstrapArg::MethodHandle {
                kind: REF_GET_FIELD,
                owner: rec.clone(),
                name: c.name.clone(),
                desc: self.component_desc(cid, &c.name),
            });
        }

        let declared = |n: &str, arity: usize| {
            class.members.iter().any(|m| {
                matches!(m, Member::Method(me) if !me.is_constructor && me.name == n && me.params.len() == arity)
            })
        };
        let boolean = RType::Prim(PrimType::Boolean);
        let int = RType::Prim(PrimType::Int);
        let string = self.table.external("String").map_or(RType::Unresolved, RType::Class);

        let mut out = Vec::new();
        if !declared("equals", 1) {
            out.push(self.record_method(
                "equals",
                &args,
                Type::Prim(PrimType::Boolean),
                vec![synth_param(Type::Class("Object".to_string()), "o".to_string())],
                format!("(L{rec};Ljava/lang/Object;)Z"),
                vec![ex(ExprKind::This), name("o")],
                boolean,
            ));
        }
        if !declared("hashCode", 0) {
            out.push(self.record_method(
                "hashCode",
                &args,
                Type::Prim(PrimType::Int),
                Vec::new(),
                format!("(L{rec};)I"),
                vec![ex(ExprKind::This)],
                int,
            ));
        }
        if !declared("toString", 0) {
            out.push(self.record_method(
                "toString",
                &args,
                Type::Class("String".to_string()),
                Vec::new(),
                format!("(L{rec};)Ljava/lang/String;"),
                vec![ex(ExprKind::This)],
                string,
            ));
        }
        out
    }

    /// Un método de `Object` de un `record`: `public final <ret> <name>(<params>) { return <indy>; }`,
    /// donde `<indy>` es el `invokedynamic` a `ObjectMethods.bootstrap`. `captures` es lo que el
    /// emisor empuja (siempre `this` primero); `indy_ty` fija la categoría del `return` del emisor.
    #[allow(clippy::too_many_arguments)]
    fn record_method(
        &self,
        name: &str,
        args: &[BootstrapArg],
        ret: Type,
        params: Vec<Param>,
        descriptor: String,
        captures: Vec<Expr>,
        indy_ty: RType,
    ) -> Member {
        let info = IndyCall {
            name: name.to_string(),
            descriptor,
            bootstrap_owner: "java/lang/runtime/ObjectMethods".to_string(),
            bootstrap_name: "bootstrap".to_string(),
            bootstrap_desc: OBJECT_METHODS_DESC.to_string(),
            bootstrap_args: args.to_vec(),
        };
        let mut indy = ex(ExprKind::Indy { info: Box::new(info), captures });
        indy.ty = Some(indy_ty);
        let body = Block(vec![st(StmtKind::Return(Some(indy)))]);
        Member::Method(MethodDecl {
            doc: None,
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Public, Modifier::Final],
            type_params: Vec::new(),
            return_annos: Vec::new(),
            return_type: ret,
            name: name.to_string(),
            params,
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(body),
            is_constructor: false,
        })
    }

    /// El descriptor de campo del componente `name` — vía su *accessor* (ya registrado por `enter`),
    /// cuyo retorno es el tipo del componente.
    fn component_desc(&self, cid: SymbolId, name: &str) -> String {
        let rt = candidates(self.table, cid, name)
            .into_iter()
            .find(|&m| self.arity_of(m) == Some(0))
            .and_then(|m| match self.table.resolved(m) {
                Some(Resolved::Method { ret, .. }) => Some(ret.clone()),
                _ => None,
            })
            .unwrap_or(RType::Unresolved);
        rtype_desc(self.table, &rt)
    }

    /// Sintetiza la captura de la instancia envolvente de una interna de instancia `cid` cuyo
    /// enclosing es `outer` (§8.1.3). En **todo** constructor va el parámetro de cabecera `Outer this$0`
    /// (javac siempre lo pasa en contexto de instancia, para el `requireNonNull` de §15.9.5). El campo
    /// `final Outer this$0` y su asignación `this.this$0 = this$0` solo se sintetizan si la clase
    /// **usa** la instancia envolvente (`self.enclosing_store`): si no, javac omite el campo y solo hace
    /// `Objects.requireNonNull(this$0)` (aquí, para no depender de resolver `java.util.Objects`, se
    /// omite también ese chequeo — la instancia es no-nula en el `new` no cualificado). Si no había
    /// constructor, sintetiza el por defecto. Actualiza el **símbolo** de cada constructor para que
    /// `new Inner(outer)` resuelva en la re-atribución.
    fn capture_enclosing_instance(&mut self, class: &mut ClassDecl, cid: SymbolId, outer: SymbolId) {
        let scope = self.member_scope_of(cid);
        let outer_ty = Type::Class(self.table.symbol(outer).name.clone());
        let store = self.enclosing_store;
        // Campo `final Outer this$0;` — símbolo + declaración. Solo si la clase usa el enclosing.
        if store {
            let fid = self.table.new_symbol(Symbol {
                name: "this$0".to_string(),
                kind: SymbolKind::Field { ty: outer_ty.clone() },
                owner: Some(cid),
                modifiers: vec![Modifier::Final],
            });
            self.table.define(scope, "this$0", fid);
            self.table.set_resolved(fid, Resolved::Field(RType::Class(outer)));
            class.members.insert(
                0,
                Member::Field(FieldDecl {
                    doc: None,
                    annotations: Vec::new(),
                    type_annos: Vec::new(),
                    pos: Pos::default(),
                    modifiers: vec![Modifier::Final],
                    ty: outer_ty.clone(),
                    name: "this$0".to_string(),
                    init: None,
                }),
            );
        }

        // A cada constructor: el parámetro de cabecera y —solo si se usa— la asignación de `this$0`.
        let mut had_ctor = false;
        for m in class.members.iter_mut() {
            let Member::Method(me) = m else { continue };
            if !me.is_constructor {
                continue;
            }
            had_ctor = true;
            me.params.insert(0, synth_param(outer_ty.clone(), "this$0".to_string()));
            if store {
                if let Some(body) = &mut me.body {
                    let at = usize::from(starts_with_ctor_call(body));
                    body.0.insert(at, this0_assign());
                }
            }
        }

        if had_ctor {
            // Al símbolo de cada constructor se le antepone `Outer` a sus parámetros resueltos. Se
            // **fija** aunque no exista `Resolved` aún (caso de una anónima, registrada tras la
            // resolución de Enter): si no, `new $1(this)` no resolvería el ctor y caería al `()V`.
            for ctor in constructors(self.table, cid) {
                let (existing, ret, varargs, throws) = match self.table.resolved(ctor) {
                    Some(Resolved::Method { params, ret, varargs, throws }) => {
                        (params.clone(), ret.clone(), *varargs, throws.clone())
                    }
                    _ => (Vec::new(), RType::Void, false, Vec::new()),
                };
                let mut params = vec![RType::Class(outer)];
                params.extend(existing);
                self.table.set_resolved(ctor, Resolved::Method { params, ret, varargs, throws });
            }
        } else {
            // Constructor por defecto: `public Inner(Outer this$0) { this.this$0 = this$0; }` (el
            // emisor le antepone el `super()` implícito). Si la clase no usa el enclosing, el cuerpo
            // queda vacío: se recibe el parámetro pero no se guarda.
            let body = if store { vec![this0_assign()] } else { Vec::new() };
            class.members.push(Member::Method(MethodDecl {
                doc: None,
                annotations: Vec::new(),
                pos: Pos::default(),
                modifiers: vec![Modifier::Public],
                type_params: Vec::new(),
                return_annos: Vec::new(),
                return_type: Type::Void,
                name: class.name.clone(),
                params: vec![synth_param(outer_ty.clone(), "this$0".to_string())],
                throws: Vec::new(),
                throws_annos: Vec::new(),
                body: Some(Block(body)),
                is_constructor: true,
            }));
            let mid = self.table.new_symbol(Symbol {
                name: class.name.clone(),
                kind: SymbolKind::Method {
                    params: vec![ParamSig {
                        ty: outer_ty,
                        name: "this$0".to_string(),
                        varargs: false,
                    }],
                    return_type: Type::Void,
                    is_constructor: true,
                    throws: Vec::new(),
                },
                owner: Some(cid),
                modifiers: vec![Modifier::Public],
            });
            self.table.define(scope, &class.name, mid);
            self.table.set_resolved(
                mid,
                Resolved::Method {
                    params: vec![RType::Class(outer)],
                    ret: RType::Void,
                    varargs: false,
                    throws: Vec::new(),
                },
            );
        }
    }

    /// Reescribe un acceso **sin cualificar** a un miembro de instancia de una clase envolvente a un
    /// acceso vía `this$0` (dentro de una interna de instancia, §8.1.3): `f` → `this.this$0.f`,
    /// `m(args)` → `this.this$0.m(args)`. **Multinivel** (§8.1.3): si el miembro es de un enclosing más
    /// lejano, encadena tantos `this$0` como niveles (`this.this$0.this$0.f`). La re-atribución los
    /// vuelve a resolver ya cualificados.
    fn rewrite_enclosing_access(&mut self, e: &mut Expr) {
        if self.enclosing_type.is_none() {
            return;
        }
        // Cadena de tipos envolventes por `this$0`: nivel 1 = enclosing directo, nivel 2 = el suyo, …
        let chain = self.enclosing_chain();
        enum Act {
            Field(String, usize),
            Receiver(usize),
        }
        let act = match (&e.kind, &e.binding) {
            (ExprKind::Name(n), Some(Binding::Field(fid))) if !self.is_static_sym(*fid) => {
                match self.enclosing_level(&chain, *fid) {
                    Some(k) => Act::Field(n.clone(), k),
                    None => return,
                }
            }
            (ExprKind::Call { target: None, .. }, Some(Binding::Method(mid)))
                if !self.is_static_sym(*mid) =>
            {
                match self.enclosing_level(&chain, *mid) {
                    Some(k) => Act::Receiver(k),
                    None => return,
                }
            }
            _ => return,
        };
        match act {
            Act::Field(n, k) => {
                e.kind = ExprKind::Field { expr: Box::new(this0_chain(k)), name: n };
            }
            Act::Receiver(k) => {
                if let ExprKind::Call { target, .. } = &mut e.kind {
                    *target = Some(Box::new(this0_chain(k)));
                }
            }
        }
        e.binding = None; // la re-atribución la resuelve de nuevo, ya cualificada
    }

    /// `new Inner(args)` sobre una interna de **instancia** cuyo enclosing es la clase actual →
    /// `new Inner(this, args)`. Solo en contexto de instancia (hay `this`); el `outer.new Inner()`
    /// cualificado y el anidamiento multinivel quedan pendientes.
    fn add_outer_arg(&mut self, e: &mut Expr) {
        // `outer.new Inner(...)` **cualificado** (§15.9.2): la instancia envolvente la designa el
        // calificador, no `this`. Se antepone tal cual —vale incluso en contexto estático, donde no
        // hay `this`—, siempre que `Inner` sea una interna de instancia.
        if matches!(&e.kind, ExprKind::NewObject { outer: Some(_), .. }) {
            let Some(target) = e.ty.as_ref().and_then(types::erased_id) else { return };
            if !self.is_instance_inner_id(target) {
                return;
            }
            if let ExprKind::NewObject { args, outer, .. } = &mut e.kind {
                let q = outer.take().expect("outer es Some por el guard");
                args.insert(0, *q);
            }
            return;
        }
        if !self.has_this {
            return;
        }
        let Some(target) = e.ty.as_ref().and_then(types::erased_id) else { return };
        let tname = self.table.symbol(target).name.clone();
        // Una clase **local** que NO captura la instancia envolvente maneja su captura aparte (`val$`):
        // no lleva `this$0`. La que sí la captura pasa por acá igual que una interna.
        if self.local_new_args.contains_key(&tname) && !self.local_uses_this.contains(&tname) {
            return;
        }
        if !self.is_instance_inner_id(target) || self.table.symbol(target).owner != self.cur_class {
            return;
        }
        if let ExprKind::NewObject { args, .. } = &mut e.kind {
            args.insert(0, ex(ExprKind::This));
        }
    }

    /// `Outer.this` (§15.8.4) → la cadena de `this$0` que va del `this` actual a la instancia
    /// envolvente pedida. Si el calificador **es** la clase actual, es `this` a secas; si es la
    /// envolvente directa, `this.this$0`; y para niveles más profundos, encadena `this$0` subiendo
    /// por la cadena de internas de instancia (`this.this$0.this$0…`). La re-atribución resuelve
    /// cada acceso ya cualificado.
    fn lower_qualified_this(&mut self, e: &mut Expr) {
        let ExprKind::QualifiedThis(Type::Class(name)) = &e.kind else { return };
        // El nombre pedido es simple (`Outer`); se compara contra los nombres simples de la cadena.
        let target = name.rsplit('.').next().unwrap_or(name).to_string();
        let Some(mut cur) = self.cur_class else { return };
        // El calificador ya es la clase actual → `this`.
        if self.table.symbol(cur).name == target {
            e.kind = ExprKind::This;
            e.binding = None;
            return;
        }
        // Sube por la cadena de owners (internas de instancia) acumulando accesos `this$0`.
        let mut acc = ex(ExprKind::This);
        while let Some(owner) = self.table.symbol(cur).owner {
            acc = ex(ExprKind::Field { expr: Box::new(acc), name: "this$0".to_string() });
            if self.table.symbol(owner).name == target {
                e.kind = acc.kind;
                e.binding = None;
                return;
            }
            cur = owner;
        }
        // No apareció en la cadena: se deja como estaba y la barrera del emisor lo corta.
    }

    /// Reescribe, dentro del cuerpo de una clase local, un `Name` a un local **capturado** del método
    /// envolvente en un acceso al campo sintético `this.val$nombre` (§14.3). La re-atribución lo
    /// resuelve ya cualificado.
    fn rewrite_captured_local(&mut self, e: &mut Expr) {
        if self.captured_locals.is_empty() {
            return;
        }
        let ExprKind::Name(n) = &e.kind else { return };
        if !matches!(e.binding, Some(Binding::Local { .. })) || !self.captured_locals.contains_key(n) {
            return;
        }
        let field = format!("val${n}");
        e.kind = ExprKind::Field { expr: Box::new(ex(ExprKind::This)), name: field };
        e.binding = None;
    }

    /// `new L(args)` de una clase **local** → `new L(val$capturas…, args)`: empuja los locales que la
    /// clase captura, en orden, tal como están en scope en el sitio de uso.
    fn add_local_captures_arg(&mut self, e: &mut Expr) {
        let Some(target) = e.ty.as_ref().and_then(types::erased_id) else { return };
        let tname = self.table.symbol(target).name.clone();
        let Some(caps) = self.local_new_args.get(&tname).cloned() else { return };
        if let ExprKind::NewObject { args, .. } = &mut e.kind {
            let mut prefixed: Vec<Expr> = caps.iter().map(|n| name(n)).collect();
            prefixed.append(args);
            *args = prefixed;
        }
    }

    /// Levanta una clase **local** (§14.3) a tipo anidado del enclosing, capturando sus locales
    /// effectively-final en campos `val$` y —si se declara en un contexto de instancia— la instancia
    /// envolvente en `this$0` (reusando el molde de las internas). Devuelve el `Member::Type` a
    /// agregar a los miembros del enclosing.
    fn lift_local_class(&mut self, mut lc: ClassDecl) -> Option<Member> {
        let enclosing = self.cur_class?;
        let scope = self.member_scope_of(enclosing);
        let lc_cid = self.table.resolve_type(scope, &lc.name)?;
        // El método envolvente de esta local/anónima, para su atributo `EnclosingMethod` (§4.7.7).
        // Si se declaró en un inicializador (`cur_method_sig == None`), no se registra: `method_index`
        // queda en 0, pero el `class_index` lo pone el emisor igual (la clase envolvente).
        if let Some((name, desc)) = self.cur_method_sig.clone() {
            self.table.set_enclosing_method(lc_cid, name, desc);
        }
        // El **parámetro** de la instancia envolvente va siempre que el método envolvente tenga
        // instancia (§15.9.5): javac lo pasa y lo `requireNonNull`ea aunque la clase no lo use. El
        // **campo** `this$0`, en cambio, solo si el cuerpo realmente usa un miembro del enclosing (o
        // `Outer.this`): esa decisión la lleva `enclosing_store` a `capture_enclosing_instance`.
        let uses_this = self.has_this;
        let stores_this = uses_this && self.uses_enclosing_instance(&lc, enclosing);

        // Captura de locales: se reusa el análisis de variables libres de las lambdas, ignorando su
        // `uses_this` (pensado para lambdas —sin `this` propio—; una local sí lo tiene).
        let mut cap = Captures {
            table: self.table,
            declared: HashSet::new(),
            free: Vec::new(),
            uses_this: false,
        };
        for m in &lc.members {
            if let Member::Method(me) = m {
                cap.declared = me.params.iter().map(|p| p.name.clone()).collect();
                if let Some(body) = &me.body {
                    cap.block(body);
                }
            }
        }
        let free = cap.free;

        // Primero los `val$` (parámetros de cola); el `this$0` lo antepone `capture_enclosing_instance`
        // dentro de `class()` —cuando `enclosing_type` está seteado—, quedando el ctor `[this$0, val$…]`.
        self.add_val_captures(&mut lc, lc_cid, &free);

        self.captured_locals = free.iter().cloned().collect();
        let fqn = self.cur_fqn.clone();
        let saved_encl = self.enclosing_type;
        let saved_store = self.enclosing_store;
        self.enclosing_type = if uses_this { Some(enclosing) } else { None };
        self.enclosing_store = stores_this;
        self.class(&mut lc, &fqn);
        self.enclosing_type = saved_encl;
        self.enclosing_store = saved_store;
        self.captured_locals.clear();

        self.local_new_args.insert(lc.name.clone(), free.iter().map(|(n, _)| n.clone()).collect());
        if uses_this {
            self.local_uses_this.insert(lc.name.clone());
        }
        Some(Member::Type(lc))
    }

    /// Sintetiza en la clase local los campos `final T val$x` por cada local capturado y, en su
    /// constructor (explícito o sintético), los parámetros de cabecera con sus asignaciones. Actualiza
    /// el símbolo de cada constructor anteponiéndoles los tipos, para que `new L(val$…)` resuelva.
    fn add_val_captures(&mut self, lc: &mut ClassDecl, lc_cid: SymbolId, free: &[(String, RType)]) {
        if free.is_empty() {
            return;
        }
        let scope = self.member_scope_of(lc_cid);
        let mut params: Vec<Param> = Vec::new();
        let mut assigns: Vec<Stmt> = Vec::new();
        let mut rparams: Vec<RType> = Vec::new();
        for (n, rt) in free {
            let field = format!("val${n}");
            let ty = rtype_to_type(self.table, rt);
            let fid = self.table.new_symbol(Symbol {
                name: field.clone(),
                kind: SymbolKind::Field { ty: ty.clone() },
                owner: Some(lc_cid),
                modifiers: vec![Modifier::Final],
            });
            self.table.define(scope, &field, fid);
            self.table.set_resolved(fid, Resolved::Field(rt.clone()));
            lc.members.insert(
                0,
                Member::Field(FieldDecl {
                    doc: None,
                    annotations: Vec::new(),
                    type_annos: Vec::new(),
                    pos: Pos::default(),
                    modifiers: vec![Modifier::Final],
                    ty: ty.clone(),
                    name: field.clone(),
                    init: None,
                }),
            );
            params.push(synth_param(ty, field.clone()));
            assigns.push(val_assign(&field));
            rparams.push(rt.clone());
        }

        // AST: al constructor explícito se le anteponen params + asignaciones; si no hay, se sintetiza.
        let had_ctor = lc.members.iter().any(|m| matches!(m, Member::Method(me) if me.is_constructor));
        if had_ctor {
            for m in lc.members.iter_mut() {
                let Member::Method(me) = m else { continue };
                if !me.is_constructor {
                    continue;
                }
                for (i, p) in params.iter().enumerate() {
                    me.params.insert(i, p.clone());
                }
                if let Some(body) = &mut me.body {
                    let at = usize::from(starts_with_ctor_call(body));
                    for (i, a) in assigns.iter().enumerate() {
                        body.0.insert(at + i, a.clone());
                    }
                }
            }
        } else {
            lc.members.push(Member::Method(MethodDecl {
                doc: None,
                annotations: Vec::new(),
                pos: Pos::default(),
                modifiers: vec![Modifier::Public],
                type_params: Vec::new(),
                return_annos: Vec::new(),
                return_type: Type::Void,
                name: lc.name.clone(),
                params: params.clone(),
                throws: Vec::new(),
                throws_annos: Vec::new(),
                body: Some(Block(assigns.clone())),
                is_constructor: true,
            }));
        }
        // Símbolo: a cada constructor (incluido el default que `enter` sintetizó) se le anteponen los
        // tipos capturados a sus parámetros resueltos. El ctor de una **anónima** puede no tener
        // `Resolved` aún (se registra tras la resolución de Enter), así que se **fija** —no solo se
        // actualiza—: sin esto, `new $1(cap)` no resolvía el ctor `(cap)V` y caía al `()V` por
        // defecto, empujando un argumento que el descriptor no consumía (el verificador lo rechazaba).
        for ctor in constructors(self.table, lc_cid) {
            let (existing, ret, varargs, throws) = match self.table.resolved(ctor) {
                Some(Resolved::Method { params, ret, varargs, throws }) => {
                    (params.clone(), ret.clone(), *varargs, throws.clone())
                }
                _ => (Vec::new(), RType::Void, false, Vec::new()),
            };
            let mut np = rparams.clone();
            np.extend(existing);
            self.table.set_resolved(ctor, Resolved::Method { params: np, ret, varargs, throws });
        }
    }

    fn member_scope_of(&self, cid: SymbolId) -> ScopeId {
        match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => self.top_scope,
        }
    }

    /// Pliega una etiqueta de `case` a un literal entero si es una **expresión constante** (§15.28):
    /// una `static final int` (`case MAX:`), aritmética entre constantes (`case 1 + 2:`), etc. Así el
    /// emisor —que solo entiende literales— la acepta. Las que no plegan quedan igual.
    fn fold_case_label(&self, l: &mut CaseLabel) {
        let CaseLabel::Constant(e) = l else { return };
        if matches!(e.kind, ExprKind::IntLit(_)) {
            return;
        }
        let scope = self.cur_class.map_or(self.top_scope, |c| self.member_scope_of(c));
        if let Some(v) = fold_const_int(self.table, scope, &self.consts, e) {
            *e = ex(ExprKind::IntLit(v as i64));
        }
    }
    fn member_owner_is(&self, sym: SymbolId, class: SymbolId) -> bool {
        self.table.symbol(sym).owner == Some(class)
    }
    /// La cadena de tipos envolventes alcanzables por `this$0` desde la clase en curso: `[enclosing
    /// directo, su enclosing, …]`. Se sube mientras cada eslabón sea una **interna de instancia** (la
    /// única que lleva su propio `this$0` al siguiente).
    fn enclosing_chain(&self) -> Vec<SymbolId> {
        let mut chain = Vec::new();
        let mut cur = self.enclosing_type;
        while let Some(c) = cur {
            chain.push(c);
            cur = if self.is_instance_inner_id(c) { self.table.symbol(c).owner } else { None };
        }
        chain
    }
    /// ¿La clase interna/local/anónima `class` **usa** la instancia envolvente (`enclosing`)? (§8.1.3)
    /// javac solo materializa el campo `this$0` cuando el cuerpo referencia un miembro **de instancia**
    /// del enclosing (o de un enclosing más lejano) sin cualificar, o usa `Outer.this` — exactamente lo
    /// que `rewrite_enclosing_access`/`lower_qualified_this` rutean por `this$0`. Se recorre el cuerpo
    /// entero, **incluidas las clases anidadas**: si una nieta usa el enclosing, la del medio también
    /// tiene que capturarlo para pasárselo (transitividad de §8.1.3). El conjunto son los tipos de la
    /// cadena de `this$0` alcanzable desde `class` (el enclosing directo y sus enclosings de instancia).
    fn uses_enclosing_instance(&self, class: &ClassDecl, enclosing: SymbolId) -> bool {
        let mut set: HashSet<SymbolId> = HashSet::new();
        let mut names: HashSet<String> = HashSet::new();
        let mut cur = Some(enclosing);
        while let Some(c) = cur {
            set.insert(c);
            names.insert(self.table.symbol(c).name.clone());
            cur = if self.is_instance_inner_id(c) { self.table.symbol(c).owner } else { None };
        }
        let mut scan = EnclosingUseScan { table: self.table, set: &set, names: &names, used: false };
        scan.members(&class.members);
        scan.used
    }
    /// El **número de saltos `this$0`** (1-based) hasta el enclosing de la cadena que declara `member`,
    /// o `None` si no pertenece a ninguno.
    fn enclosing_level(&self, chain: &[SymbolId], member: SymbolId) -> Option<usize> {
        chain.iter().position(|&c| self.member_owner_is(member, c)).map(|i| i + 1)
    }
    fn is_static_sym(&self, sym: SymbolId) -> bool {
        self.table.symbol(sym).modifiers.contains(&Modifier::Static)
    }
    /// ¿`id` es una interna de **instancia** (una `class` no-estática miembro de otra clase)?
    fn is_instance_inner_id(&self, id: SymbolId) -> bool {
        let s = self.table.symbol(id);
        // Una **anónima** (`$1`) o una **local** (`1L`) no son tipos *miembro*: se crean en el
        // cuerpo de un método y, si ese método tiene `this`, capturan. Se reconocen por el nombre
        // sintético que les puso `hoist_anonymous`/`register_local_classes` — ningún identificador
        // del fuente empieza con `$` ni con un dígito.
        let sintetica =
            s.name.starts_with('$') || s.name.starts_with(|c: char| c.is_ascii_digit());
        // Un tipo **miembro** de una interfaz es implícitamente `static` (§9.5): no hay instancia
        // envolvente que capturar, porque una interfaz no tiene instancias (#295). La anónima de un
        // método `default` queda afuera de la regla justamente porque no es un tipo miembro — y
        // ponerla adentro dejaba a los `$1` de `Spliterator` y `PrimitiveIterator` sin su parámetro
        // de cabecera.
        let dueña_captura = matches!(
            s.owner.map(|o| &self.table.symbol(o).kind),
            Some(SymbolKind::Class { kind, .. }) if sintetica || *kind != TypeKind::Interface
        );
        matches!(s.kind, SymbolKind::Class { kind: TypeKind::Class, .. })
            && !s.modifiers.contains(&Modifier::Static)
            && dueña_captura
    }
}

/// La forma de una referencia a método (§15.13.1), que fija el *reference kind* y si hay captura.
enum MRefKind {
    /// `C::sm` — método estático.
    Static,
    /// `expr::im` — método de instancia sobre un **valor** (el receptor se captura).
    BoundInstance,
    /// `C::im` — método de instancia sobre un **tipo** (el receptor es el 1.er parámetro del SAM).
    UnboundInstance,
    /// `C::new` — constructor.
    Ctor,
}

/// Un `IndyCall` cuyo *bootstrap* es `LambdaMetafactory.metafactory`: lo comparten la lambda y el
/// *method ref*, que solo difieren en el `MethodHandle` de la implementación. Los argumentos
/// estáticos son el `MethodType` **borrado** del SAM, ese `MethodHandle`, y el `MethodType`
/// **instanciado** (§ de `LambdaMetafactory`).
#[allow(clippy::too_many_arguments)]
fn metafactory_indy(
    name: String,
    descriptor: String,
    sam_desc: String,
    inst_desc: String,
    impl_kind: u8,
    impl_owner: String,
    impl_name: String,
    impl_desc: String,
) -> IndyCall {
    IndyCall {
        name,
        descriptor,
        bootstrap_owner: "java/lang/invoke/LambdaMetafactory".to_string(),
        bootstrap_name: "metafactory".to_string(),
        bootstrap_desc: METAFACTORY_DESC.to_string(),
        bootstrap_args: vec![
            BootstrapArg::MethodType(sam_desc),
            BootstrapArg::MethodHandle {
                kind: impl_kind,
                owner: impl_owner,
                name: impl_name,
                desc: impl_desc,
            },
            BootstrapArg::MethodType(inst_desc),
        ],
    }
}

/// El **texto de una constante** para embeber en la receta de `makeConcatWithConstants`, si el
/// operando es un literal que se pliega en tiempo de compilación. Reconoce los literales que javac
/// trata como constantes de compilación de una concatenación (String/char/bool/int/long); los
/// `float`/`double` se dejan como argumentos dinámicos por ahora. Devuelve `None` para todo lo demás.
fn concat_const(kind: &ExprKind) -> Option<String> {
    match kind {
        ExprKind::StringLit(s) => Some(s.clone()),
        ExprKind::CharLit(c) => char::from_u32(u32::from(*c)).map(|ch| ch.to_string()),
        ExprKind::BoolLit(b) => Some(b.to_string()),
        ExprKind::IntLit(i) | ExprKind::LongLit(i) => Some(i.to_string()),
        _ => None,
    }
}

/// El acceso `this.this$0` — la instancia envolvente capturada de una interna de instancia.
fn this0_access() -> Expr {
    ex(ExprKind::Field { expr: Box::new(ex(ExprKind::This)), name: "this$0".to_string() })
}

/// `this.this$0.this$0…` con `hops` accesos encadenados — el camino a la instancia envolvente que está
/// `hops` niveles hacia afuera (§8.1.3). `this0_chain(1)` == [`this0_access`].
fn this0_chain(hops: usize) -> Expr {
    let mut e = ex(ExprKind::This);
    for _ in 0..hops {
        e = ex(ExprKind::Field { expr: Box::new(e), name: "this$0".to_string() });
    }
    e
}

/// La sentencia `this.this$0 = this$0;` — asigna el parámetro de captura al campo sintético.
fn this0_assign() -> Stmt {
    st(StmtKind::Expr(assign_expr(this0_access(), name("this$0"))))
}

/// La sentencia `this.val$x = val$x;` — asigna el parámetro de captura de un local al campo `val$x`.
fn val_assign(field: &str) -> Stmt {
    let target = ex(ExprKind::Field { expr: Box::new(ex(ExprKind::This)), name: field.to_string() });
    st(StmtKind::Expr(assign_expr(target, name(field))))
}

/// ¿El único argumento de un varargs **ya es** el array, y por lo tanto se pasa tal cual (§15.12.4.2)?
///
/// El caso directo lo resuelve el subtipado: `Object[]` contra `Object...`, `String[]` contra
/// `String...`. Lo que faltaba es el **varargs genérico**: `String[]` contra `T...` no es un
/// subtipo de nada —`T` todavía no está instanciado—, así que el array se envolvía en otro y
/// `Arrays.asList(arr)` devolvía una lista de **un** elemento: el arreglo. Silencioso y muy caro
/// (#298).
///
/// La condición correcta es la de §4.5.1, la misma de #290: una variable de tipo solo liga tipos
/// **referencia**. Con `T...`:
///
/// - `String[]` → `T` liga `String` y el array pasa tal cual;
/// - `int[]` → `T` no puede ligar `int`, así que liga `int[]` y el array **se envuelve** —que es
///   justamente lo que hace el javac real, y por eso `Arrays.asList(new int[]{1,2})` da una lista
///   de un solo elemento.
fn varargs_passthrough(table: &SymbolTable, arg: &RType, vararg: &RType) -> bool {
    if types::is_subtype(table, arg, vararg) {
        return true;
    }
    match (arg, vararg) {
        (RType::Array(a), RType::Array(v)) => {
            matches!(**v, RType::TypeVar(_)) && !matches!(**a, RType::Prim(_))
        }
        _ => false,
    }
}

/// ¿Es `class` una **interna de instancia** (una `class` no-estática)? Las interfaces, `enum` y
/// `record` anidados son implícitamente estáticos (§8.1.3/§8.9/§8.10): no capturan el entorno.
fn is_instance_inner(class: &ClassDecl) -> bool {
    class.kind == TypeKind::Class && !class.modifiers.contains(&Modifier::Static)
}

/// ¿El cuerpo de un constructor arranca con un `super(...)`/`this(...)` explícito (§8.8.7.1)?
fn starts_with_ctor_call(body: &Block) -> bool {
    matches!(
        body.0.first().map(|s| &s.kind),
        Some(StmtKind::Expr(e))
            if matches!(&e.kind, ExprKind::Call { name, .. } if name == "super" || name == "this")
    )
}

/// Un parámetro sintético, sin anotaciones ni `final`/`varargs`.
fn synth_param(ty: Type, name: String) -> Param {
    Param { annotations: Vec::new(), ty, name, varargs: false, is_final: false, type_annos: Vec::new() }
}

/// El análisis de **variables libres** del cuerpo de una lambda (§15.27.2): qué locales del método
/// envolvente captura y si usa `this`. `declared` arranca con los parámetros de la lambda y va
/// juntando lo que el cuerpo declara (locales, bindings de patterns, variables de `catch`/`for-each`):
/// un uso cuyo nombre no esté ahí y resuelva a un local **externo** es una captura. Java prohíbe que
/// un local de la lambda sombree uno del entorno, así que el conjunto plano de declarados alcanza.
struct Captures<'a> {
    table: &'a SymbolTable,
    declared: HashSet<String>,
    /// Los locales capturados, en orden de primera aparición y sin repetir (son los parámetros de
    /// cabecera del método sintético).
    free: Vec<(String, RType)>,
    uses_this: bool,
}

impl Captures<'_> {
    fn scan_body(&mut self, body: &LambdaBody) {
        match body {
            LambdaBody::Expr(e) => self.expr(e),
            LambdaBody::Block(b) => self.block(b),
        }
    }

    fn block(&mut self, b: &Block) {
        b.0.iter().for_each(|s| self.stmt(s));
    }

    fn declare(&mut self, name: &str) {
        self.declared.insert(name.to_string());
    }

    fn stmt(&mut self, s: &Stmt) {
        match &s.kind {
            // El inicializador se escanea **antes** de declarar el nombre (no puede referirse a sí).
            StmtKind::LocalVar { init, name, .. } => {
                if let Some(e) = init {
                    self.expr(e);
                }
                self.declare(name);
            }
            StmtKind::ForEach { iterable, name, body, .. } => {
                self.expr(iterable);
                self.declare(name);
                self.stmt(body);
            }
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.expr(e);
                }
            }
            StmtKind::Yield(e) | StmtKind::Expr(e) | StmtKind::Throw(e) => self.expr(e),
            StmtKind::If { cond, then, els } => {
                self.expr(cond);
                self.stmt(then);
                if let Some(e) = els {
                    self.stmt(e);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.expr(cond);
                self.stmt(body);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i);
                }
                if let Some(c) = cond {
                    self.expr(c);
                }
                update.iter().for_each(|u| self.expr(u));
                self.stmt(body);
            }
            StmtKind::Block(b) => self.block(b),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock);
                self.block(body);
            }
            StmtKind::Assert { cond, message } => {
                self.expr(cond);
                if let Some(m) = message {
                    self.expr(m);
                }
            }
            StmtKind::Try { resources, body, catches, finally } => {
                resources.iter().for_each(|r| self.stmt(r));
                self.block(body);
                for c in catches {
                    self.declare(&c.name);
                    self.block(&c.body);
                }
                if let Some(f) = finally {
                    self.block(f);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector);
                self.cases(cases);
            }
            StmtKind::Labeled { body, .. } => self.stmt(body),
            StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Empty => {}
            // Una clase local no debería quedar dentro de una lambda que sepamos bajar; se ignora
            // su interior (su captura es otra feature).
            StmtKind::LocalClass(_) => {}
        }
    }

    fn cases(&mut self, cases: &[SwitchCase]) {
        for c in cases {
            for l in &c.labels {
                if let CaseLabel::Pattern(pattern) = l {
                    declare_pattern(pattern, &mut self.declared);
                }
            }
            if let Some(g) = &c.guard {
                self.expr(g);
            }
            match &c.body {
                SwitchBody::Arrow(s) => self.stmt(s),
                SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.stmt(s)),
            }
        }
    }

    fn expr(&mut self, e: &Expr) {
        match &e.kind {
            ExprKind::Name(n) => match &e.binding {
                Some(Binding::Local { .. }) => {
                    if !self.declared.contains(n) && !self.free.iter().any(|(x, _)| x == n) {
                        self.free.push((n.clone(), e.ty.clone().unwrap_or(RType::Unresolved)));
                    }
                }
                // Un campo **de instancia** referido sin cualificar es un `this.campo` implícito.
                Some(Binding::Field(id)) if !self.is_static(*id) => self.uses_this = true,
                _ => {}
            },
            ExprKind::This | ExprKind::Super => self.uses_this = true,
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs);
                self.expr(rhs);
            }
            ExprKind::Unary { expr, .. } => self.expr(expr),
            ExprKind::Assign { target, value, .. } => {
                self.expr(target);
                self.expr(value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond);
                self.expr(then);
                self.expr(els);
            }
            ExprKind::Call { target, args, .. } => {
                match target {
                    Some(t) => self.expr(t),
                    // Una llamada sin receptor a un método de instancia usa `this`.
                    None => {
                        if let Some(Binding::Method(id)) = &e.binding {
                            if !self.is_static(*id) {
                                self.uses_this = true;
                            }
                        }
                    }
                }
                args.iter().for_each(|a| self.expr(a));
            }
            ExprKind::Field { expr, .. } => self.expr(expr),
            ExprKind::Index { array, index } => {
                self.expr(array);
                self.expr(index);
            }
            ExprKind::Cast { expr, .. } => self.expr(expr),
            ExprKind::InstanceOf { expr, binding, .. } => {
                self.expr(expr);
                if let Some(b) = binding {
                    self.declare(b);
                }
            }
            ExprKind::NewObject { args, .. } => args.iter().for_each(|a| self.expr(a)),
            ExprKind::NewArray { dims, init, .. } => {
                dims.iter().flatten().for_each(|d| self.expr(d));
                if let Some(es) = init {
                    es.iter().for_each(|e| self.expr(e));
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.expr(selector);
                self.cases(cases);
            }
            // Una lambda anidada ya fue bajada a `Indy` antes que esta: sus capturas son nombres de
            // este entorno y hay que contarlas.
            ExprKind::Indy { captures, .. } => captures.iter().for_each(|c| self.expr(c)),
            // Un *method ref* todavía no se baja; su cualificador de expresión puede capturar.
            ExprKind::MethodRef { qualifier, .. } => {
                if let super::ast::MethodRefQualifier::Expr(q) = qualifier.as_ref() {
                    self.expr(q);
                }
            }
            _ => {}
        }
    }

    fn is_static(&self, id: SymbolId) -> bool {
        self.table.symbol(id).modifiers.contains(&Modifier::Static)
    }
}

/// Recorre el cuerpo de una clase interna/local/anónima buscando un **uso de la instancia envolvente**
/// (§8.1.3): un acceso sin cualificar a un miembro de instancia de un enclosing (`set`), o `Outer.this`
/// (`names`). Es el análisis que decide si materializar el campo `this$0`. Recorre también las clases
/// anidadas (transitividad): una nieta que use el enclosing obliga a la del medio a capturarlo.
struct EnclosingUseScan<'a> {
    table: &'a SymbolTable,
    /// Los tipos alcanzables por la cadena de `this$0` desde la clase analizada (enclosing directo y
    /// sus enclosings de instancia). Un miembro de instancia de uno de ellos, accedido sin cualificar,
    /// es un uso de la instancia envolvente.
    set: &'a HashSet<SymbolId>,
    /// Sus nombres simples, para reconocer `Outer.this`.
    names: &'a HashSet<String>,
    used: bool,
}

impl EnclosingUseScan<'_> {
    fn is_static(&self, id: SymbolId) -> bool {
        self.table.symbol(id).modifiers.contains(&Modifier::Static)
    }
    /// ¿El miembro `id` (campo o método) pertenece a un tipo del conjunto de enclosings?
    fn owner_in_set(&self, id: SymbolId) -> bool {
        self.table.symbol(id).owner.is_some_and(|o| self.set.contains(&o))
    }

    fn members(&mut self, members: &[Member]) {
        for m in members {
            match m {
                Member::Field(f) => {
                    if let Some(e) = &f.init {
                        self.expr(e);
                    }
                }
                Member::Method(me) => {
                    if let Some(b) = &me.body {
                        self.block(b);
                    }
                }
                Member::StaticInit(b) | Member::InstanceInit(b) => self.block(b),
                // Una clase anidada: sus accesos al enclosing común también cuentan (transitividad).
                Member::Type(nested) => self.members(&nested.members),
            }
        }
    }

    fn block(&mut self, b: &Block) {
        b.0.iter().for_each(|s| self.stmt(s));
    }

    fn stmt(&mut self, s: &Stmt) {
        if self.used {
            return;
        }
        match &s.kind {
            StmtKind::LocalVar { init, .. } => {
                if let Some(e) = init {
                    self.expr(e);
                }
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.expr(iterable);
                self.stmt(body);
            }
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.expr(e);
                }
            }
            StmtKind::Yield(e) | StmtKind::Expr(e) | StmtKind::Throw(e) => self.expr(e),
            StmtKind::If { cond, then, els } => {
                self.expr(cond);
                self.stmt(then);
                if let Some(e) = els {
                    self.stmt(e);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.expr(cond);
                self.stmt(body);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i);
                }
                if let Some(c) = cond {
                    self.expr(c);
                }
                update.iter().for_each(|u| self.expr(u));
                self.stmt(body);
            }
            StmtKind::Block(b) => self.block(b),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock);
                self.block(body);
            }
            StmtKind::Assert { cond, message } => {
                self.expr(cond);
                if let Some(m) = message {
                    self.expr(m);
                }
            }
            StmtKind::Try { resources, body, catches, finally } => {
                resources.iter().for_each(|r| self.stmt(r));
                self.block(body);
                catches.iter().for_each(|c| self.block(&c.body));
                if let Some(f) = finally {
                    self.block(f);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector);
                self.cases(cases);
            }
            StmtKind::Labeled { body, .. } => self.stmt(body),
            // Una clase local anidada: sus accesos al enclosing común cuentan (transitividad).
            StmtKind::LocalClass(lc) => self.members(&lc.members),
            StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Empty => {}
        }
    }

    fn cases(&mut self, cases: &[SwitchCase]) {
        for c in cases {
            if let Some(g) = &c.guard {
                self.expr(g);
            }
            match &c.body {
                SwitchBody::Arrow(s) => self.stmt(s),
                SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.stmt(s)),
            }
        }
    }

    fn expr(&mut self, e: &Expr) {
        if self.used {
            return;
        }
        match &e.kind {
            // Un nombre suelto ligado a un campo de instancia de un enclosing → `this$0.campo`.
            ExprKind::Name(_) => {
                if let Some(Binding::Field(id)) = &e.binding {
                    if !self.is_static(*id) && self.owner_in_set(*id) {
                        self.used = true;
                    }
                }
            }
            // Una llamada sin receptor a un método de instancia de un enclosing → `this$0.m(...)`.
            ExprKind::Call { target, args, .. } => {
                if target.is_none() {
                    if let Some(Binding::Method(id)) = &e.binding {
                        if !self.is_static(*id) && self.owner_in_set(*id) {
                            self.used = true;
                        }
                    }
                }
                if let Some(t) = target {
                    self.expr(t);
                }
                args.iter().for_each(|a| self.expr(a));
            }
            // `Outer.this` que nombra a un enclosing (no a la propia clase) usa la cadena de `this$0`.
            ExprKind::QualifiedThis(Type::Class(n)) => {
                let simple = n.rsplit('.').next().unwrap_or(n);
                if self.names.contains(simple) {
                    self.used = true;
                }
            }
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs);
                self.expr(rhs);
            }
            ExprKind::Unary { expr, .. } => self.expr(expr),
            ExprKind::Assign { target, value, .. } => {
                self.expr(target);
                self.expr(value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond);
                self.expr(then);
                self.expr(els);
            }
            ExprKind::Field { expr, .. } => self.expr(expr),
            ExprKind::Index { array, index } => {
                self.expr(array);
                self.expr(index);
            }
            ExprKind::Cast { expr, .. } => self.expr(expr),
            ExprKind::InstanceOf { expr, .. } => self.expr(expr),
            // Una clase anónima aún sin levantar (por si se analiza antes de `hoist_anonymous`): su
            // cuerpo puede usar el enclosing común.
            ExprKind::NewObject { args, outer, body, .. } => {
                if let Some(o) = outer {
                    self.expr(o);
                }
                args.iter().for_each(|a| self.expr(a));
                if let Some(members) = body {
                    self.members(members);
                }
            }
            ExprKind::NewArray { dims, init, .. } => {
                dims.iter().flatten().for_each(|d| self.expr(d));
                if let Some(es) = init {
                    es.iter().for_each(|e| self.expr(e));
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.expr(selector);
                self.cases(cases);
            }
            ExprKind::Lambda { body, .. } => match body.as_ref() {
                LambdaBody::Expr(e) => self.expr(e),
                LambdaBody::Block(b) => self.block(b),
            },
            ExprKind::Indy { captures, .. } => captures.iter().for_each(|c| self.expr(c)),
            ExprKind::MethodRef { qualifier, .. } => {
                if let super::ast::MethodRefQualifier::Expr(q) = qualifier.as_ref() {
                    self.expr(q);
                }
            }
            _ => {}
        }
    }
}

/// Junta en `out` los nombres que **liga** un pattern (§14.30.1): el binding de un `Type name` y,
/// recursivamente, los de una deconstrucción de record.
fn declare_pattern(p: &Pattern, out: &mut HashSet<String>) {
    match p {
        Pattern::Type { name, .. } => {
            out.insert(name.clone());
        }
        Pattern::Record { components, .. } => {
            components.iter().for_each(|c| declare_pattern(c, out));
        }
    }
}

/// ¿`e` es una switch-**expresión** que sabemos bajar a sentencia? Debe tener `default` (para que la
/// asignación definitiva vea que la variable siempre se escribe) y todas sus etiquetas ser
/// **constantes** sin guardas: los *patterns*, el `case null` y las guardas caen fuera (son de la
/// bajada de patterns). Los brazos pueden ser de flecha, de bloque o de dos puntos con `yield`
/// (ver [`Desugarer::switch_to_stmt`]).
fn is_lowerable_switch_expr(e: &Expr) -> bool {
    let ExprKind::Switch { cases, .. } = &e.kind else { return false };
    // Alcanza con que tenga `default`: así la asignación definitiva ve que la variable siempre se
    // escribe. Las etiquetas pueden ser constantes **o patterns** (con sus guardas): la
    // switch-sentencia resultante la baja después la pasada de patterns, que corre a continuación.
    cases.iter().any(|c| c.is_default)
}

/// `x = switch(…){…}`: una asignación **simple** cuyo valor es una switch-expresión bajable.
fn is_assign_switch(e: &Expr) -> bool {
    matches!(&e.kind, ExprKind::Assign { op: AssignOp::Assign, value, .. } if is_lowerable_switch_expr(value))
}

/// ¿El brazo produce su valor **sin** un `yield` que haya que reescribir? Una flecha con expresión
/// (`case X -> v`) o con `throw` no necesita la etiqueta; un bloque o la forma de dos puntos sí.
fn is_arrow_value_or_throw(body: &SwitchBody) -> bool {
    matches!(body, SwitchBody::Arrow(s) if matches!(s.kind, StmtKind::Expr(_) | StmtKind::Throw(_)))
}

/// ¿La etiqueta es una **constante entera** que el emisor de `switch` entiende directo (`case 1`,
/// `case 'a'`, con `-`/`+`/*cast* al frente)? Espejo de `const_int` del codegen: las que necesitan
/// plegado (`case MAX`, `case 1 + 2`) caen fuera y siguen el camino con temporal.
fn is_int_const_label(l: &CaseLabel) -> bool {
    fn is_int_const(e: &Expr) -> bool {
        match &e.kind {
            ExprKind::IntLit(_) | ExprKind::CharLit(_) => true,
            ExprKind::Unary { op: UnOp::Neg | UnOp::Plus, expr, .. } => is_int_const(expr),
            ExprKind::Cast { expr, .. } => is_int_const(expr),
            _ => false,
        }
    }
    matches!(l, CaseLabel::Constant(e) if is_int_const(e))
}

/// Una asignación simple fresca `target = value`.
fn assign_expr(target: Expr, value: Expr) -> Expr {
    ex(ExprKind::Assign { op: AssignOp::Assign, target: Box::new(target), value: Box::new(value) })
}

/// La comparación `e != null`.
fn ne_null(e: Expr) -> Expr {
    ex(ExprKind::Binary { op: BinOp::Ne, lhs: Box::new(e), rhs: Box::new(ex(ExprKind::Null)) })
}

/// La comparación `e == null`.
fn eq_null(e: Expr) -> Expr {
    ex(ExprKind::Binary { op: BinOp::Eq, lhs: Box::new(e), rhs: Box::new(ex(ExprKind::Null)) })
}

/// El cuerpo de un brazo seguido del `break` que sale del switch — **salvo** que el cuerpo ya
/// transfiera el control por su cuenta (`return`/`throw`/`break`), en cuyo caso el `break` sería
/// código **inalcanzable**.
fn with_break(body: Stmt, label: &str) -> Stmt {
    if completes_normally(&body) {
        st(StmtKind::Block(Block(vec![body, st(StmtKind::Break(Some(label.to_string())))])))
    } else {
        body
    }
}

/// ¿La sentencia puede **completar normalmente**? Chequeo sintáctico conservador (§14.21): alcanza
/// para no emitir un `break` detrás de un brazo que ya sale.
fn completes_normally(s: &Stmt) -> bool {
    match &s.kind {
        StmtKind::Return(_) | StmtKind::Throw(_) | StmtKind::Break(_) | StmtKind::Continue(_) => false,
        StmtKind::Block(b) => b.0.last().is_none_or(completes_normally),
        _ => true,
    }
}

/// ¿Es un `switch` con *type patterns*? Basta con que algún `case` traiga un pattern; exigimos que
/// todos los brazos sean de **flecha** (Java no deja caer en un `case` con pattern, así que la forma
/// de dos puntos no aporta *fall-through* acá).
fn is_pattern_switch(cases: &[SwitchCase]) -> bool {
    cases.iter().any(|c| c.labels.iter().any(|l| matches!(l, CaseLabel::Pattern { .. })))
        && cases.iter().all(|c| matches!(c.body, SwitchBody::Arrow(_)))
}

/// El `String.hashCode()` de Java, **calculado en tiempo de compilación** para emitirlo como la
/// etiqueta `int` del switch de nivel 1: `s[0]*31^(n-1) + … + s[n-1]`, sobre las unidades **UTF-16**
/// y con aritmética que **envuelve** en `i32` (idéntico al del JDK, JLS/`java.lang.String`).
fn java_string_hash(s: &str) -> i32 {
    let mut h: i32 = 0;
    for u in s.encode_utf16() {
        h = h.wrapping_mul(31).wrapping_add(u as i32);
    }
    h
}

/// ¿Se puede **re-evaluar** `e` sin efectos observables? Un nombre, un campo o un índice sobre
/// expresiones puras: releerlos da lo mismo y no dispara métodos.
fn is_pure(e: &Expr) -> bool {
    match &e.kind {
        ExprKind::Name(_)
        | ExprKind::This
        | ExprKind::Super
        | ExprKind::IntLit(_)
        | ExprKind::LongLit(_)
        | ExprKind::FloatLit(_)
        | ExprKind::DoubleLit(_)
        | ExprKind::CharLit(_)
        | ExprKind::StringLit(_)
        | ExprKind::BoolLit(_)
        | ExprKind::Null => true,
        ExprKind::Field { expr, .. } => is_pure(expr),
        ExprKind::Index { array, index } => is_pure(array) && is_pure(index),
        _ => false,
    }
}

/// Un [`RType`] resuelto de vuelta a un [`Type`] sintáctico (por nombre simple), para construir
/// nodos frescos como el array de un varargs. La *erasure* de un parametrizado es su base.
fn rtype_to_type(table: &SymbolTable, t: &RType) -> Type {
    match t {
        RType::Prim(p) => Type::Prim(*p),
        RType::Void => Type::Void,
        RType::Class(id) | RType::TypeVar(id) => Type::Class(table.symbol(*id).name.clone()),
        RType::Parameterized { base, .. } => Type::Class(table.symbol(*base).name.clone()),
        RType::Array(e) => Type::Array(Box::new(rtype_to_type(table, e))),
        // Una variable de captura se materializa por su cota superior (su *erasure*).
        RType::Capture { upper, .. } => rtype_to_type(table, upper),
        RType::Intersection(ms) => ms.first().map_or(Type::Var, |m| rtype_to_type(table, m)),
        RType::InferVar(_) => Type::Var,
        // El tipo nulo no se puede escribir, asi que no hay `Type` que lo nombre.
        RType::Null | RType::Unresolved => Type::Var,
    }
}

fn compound_binop(op: AssignOp) -> BinOp {
    match op {
        AssignOp::Add => BinOp::Add,
        AssignOp::Sub => BinOp::Sub,
        AssignOp::Mul => BinOp::Mul,
        AssignOp::Div => BinOp::Div,
        AssignOp::Rem => BinOp::Rem,
        AssignOp::And => BinOp::BitAnd,
        AssignOp::Or => BinOp::BitOr,
        AssignOp::Xor => BinOp::BitXor,
        AssignOp::Shl => BinOp::Shl,
        AssignOp::Shr => BinOp::Shr,
        AssignOp::UShr => BinOp::UShr,
        AssignOp::Assign => BinOp::Add, // no se llega (solo se llama con compuestas)
    }
}

enum Sugar {
    ForEach,
    Assert,
    TryResources,
    None,
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{attribute::attribute, enter::enter, flow::flow, lexer::tokenize, parser::parse};

    fn desugared(src: &str) -> CompilationUnit {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (mut table, _e1) = enter(&unit);
        attribute(&mut unit, &table); // decora (desugar necesita los tipos)
        desugar(&mut unit, &mut table);
        unit
    }

    /// Las sentencias del primer método de la primera clase.
    fn body_of(unit: &CompilationUnit) -> &[Stmt] {
        let Member::Method(m) = &unit.types[0].members.iter().find(|m| matches!(m, Member::Method(_))).unwrap()
        else {
            panic!()
        };
        &m.body.as_ref().unwrap().0
    }

    /// Las sentencias del método `name` (de cualquier clase de la unidad).
    fn body_named<'a>(unit: &'a CompilationUnit, name: &str) -> &'a [Stmt] {
        for ty in &unit.types {
            for mem in &ty.members {
                if let Member::Method(m) = mem {
                    if m.name == name {
                        return &m.body.as_ref().unwrap().0;
                    }
                }
            }
        }
        panic!("no se encontró el método `{name}`");
    }

    /// Verifica que el árbol **bajado** vuelva a tipar y fluir sin errores nuevos.
    fn preserves_semantics(src: &str) {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (mut table, e1) = enter(&unit);
        let e2 = attribute(&mut unit, &table);
        assert!(e1.is_empty() && e2.is_empty(), "el fuente ya tenía errores: {e1:?} {e2:?}");
        desugar(&mut unit, &mut table);
        let e3 = attribute(&mut unit, &table); // re-atribuir el árbol bajado
        let e4 = flow(&unit);
        assert!(e3.is_empty(), "desugar introdujo errores de tipo: {e3:?}");
        assert!(e4.is_empty(), "desugar introdujo errores de flujo: {e4:?}");
    }

    #[test]
    fn for_each_over_an_array_becomes_an_indexed_for() {
        let unit = desugared("class C { void m(int[] xs) { for (int x : xs) { int y = x; } } }");
        // Queda un Block { var $a = xs; int $len = $a.length; for(...) {...} } — el largo se **cachea**
        // en un temporal (como javac), así que el `for` es el tercer elemento del bloque.
        let StmtKind::Block(b) = &body_of(&unit)[0].kind else { panic!("{:?}", body_of(&unit)[0].kind) };
        assert!(matches!(b.0[0].kind, StmtKind::LocalVar { .. }), "el array se copia a un temporal");
        assert!(matches!(b.0[1].kind, StmtKind::LocalVar { .. }), "el largo se cachea en un temporal");
        assert!(matches!(b.0[2].kind, StmtKind::For { .. }), "y se vuelve un for indexado");
    }

    #[test]
    fn for_each_over_an_iterable_becomes_a_while_over_an_iterator() {
        let unit = desugared(
            "import java.util.List; class C { void m(List<String> xs) { for (String s : xs) { int n = s.length(); } } }",
        );
        let StmtKind::Block(b) = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(b.0[0].kind, StmtKind::LocalVar { .. }), "el iterator se declara antes");
        assert!(matches!(b.0[1].kind, StmtKind::While { .. }), "y se recorre con un while");
    }

    #[test]
    fn assert_becomes_an_if_throw() {
        let unit = desugared("class C { void m(boolean b) { assert b; } }");
        let StmtKind::If { then, .. } = &body_of(&unit)[0].kind else { panic!("{:?}", body_of(&unit)[0].kind) };
        assert!(matches!(then.kind, StmtKind::Throw(_)), "el brazo lanza un AssertionError");
    }

    #[test]
    fn instance_initializers_are_merged_into_constructors() {
        // §8.6/§12.5: corren dentro de cada constructor, **antes** de su cuerpo.
        let unit = desugared("class C { int x; { x = 1; } C() { x = x + 10; } }");
        let c = &unit.types[0];
        assert!(!c.members.iter().any(|m| matches!(m, Member::InstanceInit(_))), "el bloque se consumió");
        let Member::Method(ctor) = c.members.iter().find(|m| matches!(m, Member::Method(me) if me.is_constructor)).unwrap() else { panic!() };
        assert_eq!(ctor.body.as_ref().unwrap().0.len(), 2, "el init quedó al frente del cuerpo");
    }

    #[test]
    fn a_constructor_delegating_to_this_skips_the_initializers() {
        // Los corre el constructor al que delega; duplicarlos sería un bug.
        let unit = desugared("class C { int x; { x = 1; } C() { this(2); } C(int n) { x = n; } }");
        let Member::Method(ctor) = unit.types[0].members.iter().find(|m| matches!(m, Member::Method(me) if me.is_constructor && me.params.is_empty())).unwrap() else { panic!() };
        assert_eq!(ctor.body.as_ref().unwrap().0.len(), 1, "solo el `this(2)`");
    }

    #[test]
    fn assert_is_guarded_by_assertions_disabled() {
        let unit = desugared("class C { void m(boolean b) { assert b; } }");
        // La condición pasa a ser `!$assertionsDisabled && !b`.
        let StmtKind::If { cond, .. } = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(cond.kind, ExprKind::Binary { op: BinOp::And, .. }), "{:?}", cond.kind);
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains("$assertionsDisabled"), "usa el guard:\n{tree}");
        // Y la clase gana el campo sintético más el `<clinit>` que lo calcula.
        let c = &unit.types[0];
        assert!(c.members.iter().any(|m| matches!(m, Member::Field(f) if f.name == "$assertionsDisabled")));
        assert!(c.members.iter().any(|m| matches!(m, Member::StaticInit(_))));
        assert!(tree.contains("desiredAssertionStatus"), "lo calcula del status real:\n{tree}");
    }

    #[test]
    fn desugared_assert_guard_still_type_checks() {
        // Lo que valida de verdad: el campo sintético y el `C.class.desiredAssertionStatus()` del
        // `<clinit>` tienen que resolver al re-atribuir.
        preserves_semantics("class C { void m(int p) { assert p > 0 : \"positivo\"; } }");
    }

    #[test]
    fn a_class_without_asserts_gets_no_guard() {
        let unit = desugared("class C { void m(int p) { int x = p; } }");
        let c = &unit.types[0];
        assert!(!c.members.iter().any(|m| matches!(m, Member::Field(_))), "sin asserts, sin campo");
    }

    #[test]
    fn assert_with_message_passes_it_to_the_error() {
        let unit = desugared("class C { void m(boolean b) { assert b : \"nope\"; } }");
        let StmtKind::If { then, .. } = &body_of(&unit)[0].kind else { panic!() };
        let StmtKind::Throw(e) = &then.kind else { panic!() };
        let ExprKind::NewObject { args, .. } = &e.kind else { panic!() };
        assert_eq!(args.len(), 1, "el mensaje va como argumento del AssertionError");
    }

    #[test]
    fn try_with_resources_becomes_try_finally() {
        let unit = desugared(
            "class R implements AutoCloseable { public void close() {} }
             class C { void m() { try (R r = new R()) { int x = 1; } } }",
        );
        // El primer método es el de C (la segunda clase); busquemos su cuerpo.
        let Member::Method(m) = unit.types[1].members.iter().find(|m| matches!(m, Member::Method(_))).unwrap()
        else {
            panic!()
        };
        // Queda un try SIN recursos; su cuerpo es el bloque gestionado
        // { R r = ...; Throwable $p = null; try {} catch(Throwable){...} finally { ...close } }.
        let StmtKind::Try { resources, body, .. } = &m.body.as_ref().unwrap().0[0].kind else {
            panic!()
        };
        assert!(resources.is_empty(), "los recursos se bajaron");
        let StmtKind::Block(inner) = &body.0[0].kind else { panic!() };
        assert!(matches!(inner.0[0].kind, StmtKind::LocalVar { .. }), "declara el recurso");
        assert!(matches!(inner.0[1].kind, StmtKind::LocalVar { .. }), "y la excepción primaria `$p`");
        let StmtKind::Try { catches, finally: Some(_), .. } = &inner.0[2].kind else {
            panic!("try con catch+finally: {:?}", inner.0[2].kind)
        };
        assert_eq!(catches.len(), 1, "un catch (Throwable) que registra y relanza la primaria");
    }

    #[test]
    fn try_with_resources_suppresses_the_close_exception() {
        // §14.20.3.1: el `close()` debe cerrarse **suprimiendo** su excepción dentro de la primaria,
        // no tapándola. En el árbol bajado eso se ve como una llamada a `addSuppressed`.
        let unit = desugared(
            "class R implements AutoCloseable { public void close() {} }
             class C { void m() { try (R r = new R()) { int x = 1; } } }",
        );
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains("addSuppressed"), "el close suprime en vez de tapar:\n{tree}");
        assert!(tree.contains("close"), "y desde luego se cierra el recurso");
    }

    // ---- azúcar de expresiones ----

    #[test]
    fn string_concat_becomes_a_makeconcat_indy() {
        let unit = desugared("class C { String m(String a, int b) { return a + b; } }");
        let StmtKind::Return(Some(e)) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Indy { info, captures } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(info.name, "makeConcatWithConstants");
        assert_eq!(info.bootstrap_owner, "java/lang/invoke/StringConcatFactory");
        assert_eq!(info.bootstrap_name, "makeConcatWithConstants");
        assert_eq!(info.bootstrap_desc, STRING_CONCAT_DESC);
        // Dos operandos dinámicos: `String` e `int`.
        assert_eq!(info.descriptor, "(Ljava/lang/String;I)Ljava/lang/String;");
        assert_eq!(captures.len(), 2, "se empujan ambos operandos");
        // La receta son dos marcadores dinámicos, sin texto constante.
        assert_eq!(info.bootstrap_args, vec![BootstrapArg::Str("\u{1}\u{1}".to_string())]);
    }

    #[test]
    fn a_three_way_concat_captures_three_operands() {
        let unit = desugared("class C { String m(String a, int b, Object c) { return a + b + c; } }");
        let StmtKind::Return(Some(e)) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Indy { info, captures } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(captures.len(), 3, "un push por operando dinámico");
        // El operando `Object` figura en el descriptor como `String`, no como `Object`: el codegen
        // le mete un `String.valueOf(Object)` antes del `invokedynamic` (#282), así que lo que
        // llega al call site ya es un `String`. Esta afirmación decía `Ljava/lang/Object;` --de
        // antes de #282-- y contradecía al javac real, que para
        // `"a=" + o` emite `invokestatic String.valueOf` y un descriptor con `Ljava/lang/String;`.
        assert_eq!(info.descriptor, "(Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;");
        assert_eq!(info.bootstrap_args, vec![BootstrapArg::Str("\u{1}\u{1}\u{1}".to_string())]);
    }

    #[test]
    fn concat_recipe_and_descriptor_embed_the_literal() {
        // §StringConcat: `a + "=" + b` con `a:String`, `b:int` da la receta `"="` y el
        // descriptor `(Ljava/lang/String;I)…` — verificado contra javac real.
        let unit = desugared("class C { String m(String a, int b) { return a + \"=\" + b; } }");
        let StmtKind::Return(Some(e)) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Indy { info, captures } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(info.descriptor, "(Ljava/lang/String;I)Ljava/lang/String;");
        assert_eq!(captures.len(), 2, "el `=` es constante y no se empuja");
        assert_eq!(info.bootstrap_args, vec![BootstrapArg::Str("\u{1}=\u{1}".to_string())]);
    }

    #[test]
    fn concat_of_only_constants_folds_to_a_string_literal() {
        // `"a" + "b"` es una constante de compilación: javac emite un `ldc`, no un indy vacío.
        let unit = desugared("class C { String m() { return \"a\" + \"b\"; } }");
        let StmtKind::Return(Some(e)) = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(&e.kind, ExprKind::StringLit(s) if s == "ab"), "plegado a \"ab\": {:?}", e.kind);
    }

    #[test]
    fn compound_assignment_becomes_a_simple_assignment_with_cast() {
        let unit = desugared("class C { void m(int x) { x += 1; } }");
        let StmtKind::Expr(e) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Assign { op, value, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(*op, AssignOp::Assign, "ya no es compuesta");
        assert!(matches!(value.kind, ExprKind::Cast { .. }), "con el cast de reducción: {:?}", value.kind);
    }

    #[test]
    fn compound_assignment_on_a_complex_lvalue_is_left_alone() {
        // `a[f()] += 1` tiene efectos al evaluar el índice: no se baja (lo maneja el codegen).
        let unit = desugared("class C { int f() { return 0; } void m(int[] a) { a[f()] += 1; } }");
        let Member::Method(mth) = unit.types[0].members.iter().filter(|m| matches!(m, Member::Method(_))).nth(1).unwrap()
        else {
            panic!()
        };
        let StmtKind::Expr(e) = &mth.body.as_ref().unwrap().0[0].kind else { panic!() };
        assert!(matches!(e.kind, ExprKind::Assign { op: AssignOp::Add, .. }), "sigue compuesta: {:?}", e.kind);
    }

    #[test]
    fn increment_statement_becomes_a_compound_assignment() {
        // `x++;` (valor descartado) → `x = (int)(x + 1)`.
        let unit = desugared("class C { void m(int x) { x++; } }");
        let StmtKind::Expr(e) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Assign { op, value, target } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(*op, AssignOp::Assign, "ya no es un `++`, es una asignación simple");
        assert!(matches!(target.kind, ExprKind::Name(_)), "sobre el mismo local");
        assert!(matches!(value.kind, ExprKind::Cast { .. }), "con el cast de reducción: {:?}", value.kind);
    }

    #[test]
    fn prefix_decrement_statement_lowers_too() {
        // `--x;` es igual que `x--;` en descarte: pre/post no importan.
        let unit = desugared("class C { void m(int x) { --x; } }");
        let StmtKind::Expr(e) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Assign { op, value, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(*op, AssignOp::Assign);
        // El binario de adentro debe ser una **resta** (`x - 1`).
        let ExprKind::Cast { expr, .. } = &value.kind else { panic!() };
        assert!(matches!(expr.kind, ExprKind::Binary { op: BinOp::Sub, .. }), "resta: {:?}", expr.kind);
    }

    #[test]
    fn increment_in_a_for_update_is_lowered() {
        let unit = desugared("class C { void m(int n) { for (int i = 0; i < n; i++) { int y = i; } } }");
        let StmtKind::For { update, .. } = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(update[0].kind, ExprKind::Assign { op: AssignOp::Assign, .. }), "{:?}", update[0].kind);
    }

    #[test]
    fn increment_in_value_position_is_left_for_codegen() {
        // `y = x++` usa el valor: el `++` **no** se baja (lo maneja el codegen).
        let unit = desugared("class C { int m(int x) { int y = x++; return y; } }");
        let StmtKind::LocalVar { init: Some(init), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(init.kind, ExprKind::Unary { op: UnOp::Inc, .. }), "sigue siendo `++`: {:?}", init.kind);
    }

    #[test]
    fn varargs_call_wraps_trailing_args_in_an_array() {
        let unit = desugared("class C { void f(int... xs) {} void m() { f(1, 2, 3); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::Call { args, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(args.len(), 1, "los 3 args se envuelven en un solo array");
        let ExprKind::NewArray { init, .. } = &args[0].kind else { panic!("{:?}", args[0].kind) };
        assert_eq!(init.as_ref().unwrap().len(), 3, "con los 3 elementos");
    }

    #[test]
    fn varargs_with_fixed_params_wraps_only_the_tail() {
        let unit = desugared("class C { void f(String s, int... xs) {} void m() { f(\"a\", 1, 2); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::Call { args, .. } = &e.kind else { panic!() };
        assert_eq!(args.len(), 2, "el fijo `s` + el array de la cola");
        assert!(matches!(args[1].kind, ExprKind::NewArray { .. }));
    }

    #[test]
    fn varargs_constructor_wraps_trailing_args_too() {
        // Finding #328: la bajada solo miraba `Call`, así que un `new C(1, 2)` contra `C(int...)`
        // llegaba al emisor con los argumentos sueltos y se emitía un `invokespecial` contra un
        // descriptor `([I)V` que esperaba el array.
        let unit = desugared("class C { C(int... xs) {} void m() { new C(1, 2); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::NewObject { args, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(args.len(), 1, "los 2 args se envuelven en un solo array");
        let ExprKind::NewArray { init, .. } = &args[0].kind else { panic!("{:?}", args[0].kind) };
        assert_eq!(init.as_ref().unwrap().len(), 2, "con los 2 elementos");
    }

    #[test]
    fn varargs_constructor_with_fixed_params_wraps_only_the_tail() {
        let unit = desugared("class C { C(String s, int... xs) {} void m() { new C(\"a\", 1, 2); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::NewObject { args, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(args.len(), 2, "el fijo `s` + el array de la cola");
        assert!(matches!(args[1].kind, ExprKind::NewArray { .. }));
    }

    #[test]
    fn passing_an_array_directly_to_a_varargs_constructor_is_not_rewrapped() {
        let unit = desugared("class C { C(int... xs) {} void m(int[] a) { new C(a); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::NewObject { args, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert!(matches!(args[0].kind, ExprKind::Name(_)), "el array se pasa tal cual: {:?}", args[0].kind);
    }

    #[test]
    fn passing_an_array_directly_to_varargs_is_not_rewrapped() {
        let unit = desugared("class C { void f(int... xs) {} void m(int[] a) { f(a); } }");
        let StmtKind::Expr(e) = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::Call { args, .. } = &e.kind else { panic!() };
        assert!(matches!(args[0].kind, ExprKind::Name(_)), "el array se pasa tal cual: {:?}", args[0].kind);
    }

    #[test]
    fn string_compound_assignment_lowers_the_concat_too() {
        // `s += x` → `s = s + x` → un `makeConcatWithConstants` (sin cast: el destino es `String`).
        let unit = desugared("class C { void m(String s, int x) { s += x; } }");
        let StmtKind::Expr(e) = &body_of(&unit)[0].kind else { panic!() };
        let ExprKind::Assign { value, .. } = &e.kind else { panic!() };
        let ExprKind::Indy { info, .. } = &value.kind else { panic!("{:?}", value.kind) };
        assert_eq!(info.name, "makeConcatWithConstants");
        assert_eq!(info.descriptor, "(Ljava/lang/String;I)Ljava/lang/String;");
    }

    // ---- switch-expresión ----

    #[test]
    fn switch_expression_in_a_local_init_survives_for_codegen() {
        // `int r = switch(x){ case 1 -> 10; default -> 0; };` — switch de **pila**: NO se parte en
        // decl + switch-sentencia; sobrevive entera y el codegen la consume con un `store`.
        let unit = desugared("class C { int m(int x) { int r = switch (x) { case 1 -> 10; default -> 0; }; return r; } }");
        let body = body_of(&unit);
        // [0] `int r = switch…` intacta (con init), [1] el return.
        let StmtKind::LocalVar { init: Some(e), .. } = &body[0].kind else {
            panic!("esperaba LocalVar con init: {:?}", body[0].kind)
        };
        assert!(matches!(e.kind, ExprKind::Switch { .. }), "el init sigue siendo una switch-expresión: {:?}", e.kind);
    }

    #[test]
    fn switch_expression_in_a_return_survives_for_codegen() {
        // `return switch(x){...};` — switch de **pila**: NO usa temporal; el `return` la consume
        // dejando el valor en la pila y haciendo `ireturn`.
        let unit = desugared("class C { int m(int x) { return switch (x) { case 1 -> 10; default -> 0; }; } }");
        let StmtKind::Return(Some(e)) = &body_of(&unit)[0].kind else {
            panic!("esperaba Return con valor: {:?}", body_of(&unit)[0].kind)
        };
        assert!(matches!(e.kind, ExprKind::Switch { .. }), "el return sigue siendo una switch-expresión: {:?}", e.kind);
    }

    #[test]
    fn switch_expression_assigned_to_a_variable_survives_for_codegen() {
        // `r = switch(x){...};` — switch de **pila**: NO se baja a switch-sentencia; queda como la
        // asignación intacta y el codegen la consume con un `store`.
        let unit = desugared("class C { void m(int x) { int r = 0; r = switch (x) { case 1 -> 10; default -> 0; }; } }");
        let StmtKind::Expr(e) = &body_of(&unit)[1].kind else { panic!("{:?}", body_of(&unit)[1].kind) };
        let ExprKind::Assign { value, .. } = &e.kind else { panic!("esperaba una asignación: {:?}", e.kind) };
        assert!(matches!(value.kind, ExprKind::Switch { .. }), "el valor sigue siendo una switch-expresión: {:?}", value.kind);
    }

    #[test]
    fn switch_expression_with_a_yielding_block_arm_uses_a_labeled_break() {
        // `case 1 -> { yield 10; }` → el `yield` se vuelve `r = 10; break $sw;` y el switch se etiqueta.
        let unit = desugared("class C { int m(int x) { int r = switch (x) { case 1 -> { yield 10; } default -> 0; }; return r; } }");
        let body = body_of(&unit);
        // [0] int r;  [1] $sw: switch(...)  [2] return r
        let StmtKind::Labeled { label, body: sw } = &body[1].kind else { panic!("esperaba Labeled: {:?}", body[1].kind) };
        assert!(matches!(sw.kind, StmtKind::Switch { .. }), "la etiqueta envuelve el switch");
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains(&format!("Break {label}")), "el yield se baja con break etiquetado:\n{tree}");
    }

    #[test]
    fn yield_from_inside_a_loop_still_type_checks() {
        // El `yield` está dentro de un `for`: un `break` pelado saldría del `for`, no del switch.
        // El `break` etiquetado es lo que lo hace salir del switch — y el árbol re-tipa y re-fluye.
        preserves_semantics(
            "class C { int m(int x) { int r = switch (x) { \
             case 1 -> { for (int i = 0; i < 3; i++) { if (i == x) yield i; } yield -1; } \
             default -> 0; }; return r; } }",
        );
    }

    #[test]
    fn colon_form_switch_expression_still_type_checks() {
        preserves_semantics(
            "class C { int m(int x) { int r = switch (x) { case 1: yield 10; default: yield 0; }; return r; } }",
        );
    }

    #[test]
    fn java_string_hash_matches_the_jdk() {
        // Valores canónicos de `String.hashCode()` del JDK.
        assert_eq!(java_string_hash(""), 0);
        assert_eq!(java_string_hash("a"), 97);
        assert_eq!(java_string_hash("foo"), 101574);
        assert_eq!(java_string_hash("bar"), 97299);
        assert_eq!(java_string_hash("hello"), 99162322);
    }

    #[test]
    fn string_switch_becomes_two_int_switches() {
        let unit = desugared(
            "class C { int m(String s) { switch (s) { case \"a\" -> { return 1; } default -> { return 0; } } } }",
        );
        // { String $s = s; int $i = -1; switch($s.hashCode()){…}; switch($i){…} }
        let StmtKind::Block(b) = &body_of(&unit)[0].kind else { panic!("{:?}", body_of(&unit)[0].kind) };
        assert!(matches!(&b.0[0].kind, StmtKind::LocalVar { ty: Type::Class(t), .. } if t == "String"), "copia el selector a un String");
        assert!(matches!(&b.0[1].kind, StmtKind::LocalVar { ty: Type::Prim(PrimType::Int), .. }), "declara el índice int");
        // Nivel 1: switch sobre `s.hashCode()`.
        let StmtKind::Switch { selector, cases } = &b.0[2].kind else { panic!() };
        assert!(matches!(&selector.kind, ExprKind::Call { name, .. } if name == "hashCode"), "nivel 1 = hashCode");
        // La etiqueta del único case es el hash de "a" (97).
        let CaseLabel::Constant(e) = &cases[0].labels[0] else { panic!() };
        assert!(matches!(e.kind, ExprKind::IntLit(97)), "case <hash de \"a\">: {:?}", e.kind);
        // Nivel 2: switch sobre el índice.
        let StmtKind::Switch { selector, .. } = &b.0[3].kind else { panic!() };
        assert!(matches!(&selector.kind, ExprKind::Name(n) if n.starts_with("$i")), "nivel 2 = índice");
    }

    #[test]
    fn non_string_switch_is_left_alone() {
        // Un switch sobre `int` no es azúcar (mapea directo a tableswitch/lookupswitch: es codegen).
        let unit = desugared("class C { void m(int x) { switch (x) { case 1: break; default: break; } } }");
        assert!(matches!(body_of(&unit)[0].kind, StmtKind::Switch { .. }), "{:?}", body_of(&unit)[0].kind);
    }

    // ---- switch sobre enum ----

    const ENUM_SRC: &str = "enum Color { RED, GREEN, BLUE } \
        class C { int m(Color c) { switch (c) { case RED: return 1; case GREEN: return 2; default: return 0; } } }";

    #[test]
    fn enum_switch_uses_the_switchmap_indirection() {
        let unit = desugared(ENUM_SRC);
        // El selector ahora es `Holder.$SwitchMap$Color[c.ordinal()]`.
        let StmtKind::Switch { selector, cases } = &body_named(&unit, "m")[0].kind else { panic!() };
        let ExprKind::Index { array, index } = &selector.kind else { panic!("esperaba Index: {:?}", selector.kind) };
        assert!(matches!(&array.kind, ExprKind::Field { name, .. } if name.starts_with("$SwitchMap$")), "sobre el $SwitchMap: {:?}", array.kind);
        assert!(matches!(&index.kind, ExprKind::Call { name, .. } if name == "ordinal"), "por c.ordinal(): {:?}", index.kind);
        // `case RED` (ordinal 0) → `case 1`.
        let CaseLabel::Constant(e) = &cases[0].labels[0] else { panic!() };
        assert!(matches!(e.kind, ExprKind::IntLit(1)), "RED → 1: {:?}", e.kind);
    }

    #[test]
    fn enum_switch_synthesizes_the_holder_class() {
        let unit = desugared(ENUM_SRC);
        let holder = unit.types.iter().find(|t| t.name.ends_with("$1")).expect("clase sintética C$1");
        // Tiene un campo `$SwitchMap$Color` y un inicializador estático que lo puebla.
        assert!(holder.members.iter().any(|m| matches!(m, Member::Field(f) if f.name.starts_with("$SwitchMap$"))));
        assert!(holder.members.iter().any(|m| matches!(m, Member::StaticInit(_))), "con su <clinit>");
    }

    #[test]
    fn desugared_enum_switch_still_type_checks() {
        preserves_semantics(ENUM_SRC);
    }

    // ---- switch con patterns ----

    // Brazos que **completan normalmente** (asignan, no retornan): así se emite el `break` del switch.
    const PAT_SRC: &str = "class C { int m(Object o) { int r = 0; switch (o) { \
        case String s when s.length() > 0 -> { r = s.length(); } \
        case String s -> { r = -1; } \
        case Integer i -> { r = 7; } \
        default -> { r = 0; } } return r; } }";

    #[test]
    fn pattern_switch_becomes_a_labeled_instanceof_chain() {
        let unit = desugared(PAT_SRC);
        // [0] es `int r = 0;`; el switch bajado es [1].
        let StmtKind::Labeled { label, body } = &body_named(&unit, "m")[1].kind else {
            panic!("esperaba Labeled: {:?}", body_named(&unit, "m")[1].kind)
        };
        let StmtKind::Block(b) = &body.kind else { panic!() };
        // [0] el temporal del selector, [1] el chequeo de null, luego un `if` por pattern.
        assert!(matches!(b.0[0].kind, StmtKind::LocalVar { .. }), "copia el selector a un temporal");
        assert!(matches!(b.0[1].kind, StmtKind::If { .. }), "chequea null primero");
        let StmtKind::If { cond, then, .. } = &b.0[2].kind else { panic!("{:?}", b.0[2].kind) };
        assert!(matches!(cond.kind, ExprKind::InstanceOf { .. }), "el brazo testea instanceof");
        // El cuerpo del brazo materializa el binding con un cast y sale con `break <label>`.
        let StmtKind::Block(arm) = &then.kind else { panic!() };
        let StmtKind::LocalVar { init: Some(init), .. } = &arm.0[0].kind else { panic!() };
        assert!(matches!(init.kind, ExprKind::Cast { .. }), "binding por cast: {:?}", init.kind);
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains(&format!("Break {label}")), "sale con break etiquetado:\n{tree}");
    }

    #[test]
    fn pattern_switch_without_case_null_throws_npe() {
        // §14.11.1: un pattern switch sobre `null` sin `case null` lanza NPE (no cae al default).
        let unit = desugared(PAT_SRC);
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains("NullPointerException"), "null → NPE:\n{tree}");
    }

    // ---- miembros implícitos de un `record` ----

    #[test]
    fn a_record_gets_its_fields_constructor_and_accessors() {
        let unit = desugared("record Point(int x, int y) {}");
        let c = &unit.types[0];
        let fields: Vec<_> = c.members.iter().filter_map(|m| match m {
            Member::Field(f) => Some(f.name.as_str()),
            _ => None,
        }).collect();
        assert_eq!(fields, vec!["x", "y"], "un campo por componente");
        assert!(c.members.iter().any(|m| matches!(m, Member::Method(me) if me.is_constructor && me.params.len() == 2)), "el constructor canónico");
        let methods: Vec<_> = c.members.iter().filter_map(|m| match m {
            Member::Method(me) if !me.is_constructor => Some(me.name.as_str()),
            _ => None,
        }).collect();
        // Un accessor por componente, más los tres métodos de `Object` por `invokedynamic` (§8.10.2).
        assert_eq!(methods, vec!["x", "y", "equals", "hashCode", "toString"], "accessors + métodos de Object");
    }

    #[test]
    fn an_explicit_accessor_wins_over_the_synthesized_one() {
        // §8.10.4: lo que la persona declara gana.
        let unit = desugared("record Point(int x, int y) { public int x() { return -1; } }");
        let n = unit.types[0].members.iter().filter(|m| matches!(m, Member::Method(me) if me.name == "x")).count();
        assert_eq!(n, 1, "no se duplica el accessor declarado");
    }

    #[test]
    fn synthesized_record_members_type_check() {
        preserves_semantics("record Point(int x, int y) {}");
    }

    #[test]
    fn a_record_pattern_deconstructs_through_accessors() {
        let unit = desugared(
            "record Point(int x, int y) {} \
             class C { int m(Object o) { switch (o) { \
             case Point(int a, int b) -> { return a + b; } default -> { return 0; } } } }",
        );
        let tree = crate::javac::ast_view::tree(&unit);
        // Se testea el tipo y se extrae cada componente por su accessor.
        assert!(tree.contains("InstanceOf Point"), "testea el record:\n{tree}");
        assert!(tree.contains("Call x"), "extrae el primer componente con `x()`:\n{tree}");
        assert!(tree.contains("Call y"), "y el segundo con `y()`:\n{tree}");
    }

    #[test]
    fn desugared_record_pattern_still_type_checks() {
        preserves_semantics(
            "record Point(int x, int y) {} \
             class C { int m(Object o) { switch (o) { \
             case Point(int a, int b) -> { return a + b; } default -> { return 0; } } } }",
        );
    }

    #[test]
    fn a_nested_record_pattern_recurses() {
        // `Line(Point(…), Point p)`: el componente que vuelve a deconstruir sí re-testea el tipo.
        preserves_semantics(
            "record Point(int x, int y) {} record Line(Point a, Point b) {} \
             class C { int m(Object o) { switch (o) { \
             case Line(Point(int x1, int y1), Point p) -> { return x1 + y1 + p.x(); } \
             default -> { return 0; } } } }",
        );
    }

    #[test]
    fn a_record_pattern_with_a_guard_still_type_checks() {
        preserves_semantics(
            "record Point(int x, int y) {} \
             class C { int m(Object o) { switch (o) { \
             case Point(int a, int b) when a > b -> { return a; } \
             case Point(int a, int b) -> { return b; } default -> { return 0; } } } }",
        );
    }

    #[test]
    fn pattern_arm_that_returns_gets_no_redundant_break() {
        // Si el brazo ya sale por `return`, agregarle el `break` sería código **inalcanzable**.
        let unit = desugared(
            "class C { int m(Object o) { switch (o) { case String s -> { return 1; } default -> { return 0; } } } }",
        );
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(!tree.contains("Break"), "sin break redundante tras un return:\n{tree}");
    }

    #[test]
    fn desugared_pattern_switch_still_type_checks() {
        preserves_semantics(PAT_SRC);
    }

    #[test]
    fn a_pattern_switch_expression_lowers_too() {
        // La switch-expresión con patterns se vuelve sentencia que asigna, y **después** la pasada
        // de patterns la baja a la cadena de `instanceof`.
        let unit = desugared(
            "class C { int m(Object o) { int r = switch (o) { \
             case String s -> s.length(); case Integer i -> 1; default -> 0; }; return r; } }",
        );
        // [0] `int r;` · [1] el bloque etiquetado con la cadena · [2] `return r;`
        assert!(matches!(body_named(&unit, "m")[1].kind, StmtKind::Labeled { .. }), "{:?}", body_named(&unit, "m")[1].kind);
        let tree = crate::javac::ast_view::tree(&unit);
        assert!(tree.contains("InstanceOf"), "baja a instanceof:\n{tree}");
    }

    #[test]
    fn desugared_pattern_switch_expression_still_type_checks() {
        preserves_semantics(
            "class C { int m(Object o) { int r = switch (o) { \
             case String s -> s.length(); default -> 0; }; return r; } }",
        );
    }

    #[test]
    fn a_string_switch_with_case_null_routes_before_hashing() {
        // Sin `case null` el selector nulo revienta en `hashCode()`; con él, se rutea antes.
        let unit = desugared(
            "class C { int m(String s) { switch (s) { case \"a\": return 1; case null: return -1; default: return 0; } } }",
        );
        let StmtKind::Block(b) = &body_named(&unit, "m")[0].kind else { panic!() };
        // [2] pasa a ser un `if (s == null) … else switch(hashCode)`.
        let StmtKind::If { cond, els: Some(_), .. } = &b.0[2].kind else {
            panic!("esperaba el ruteo de null: {:?}", b.0[2].kind)
        };
        assert!(matches!(cond.kind, ExprKind::Binary { op: BinOp::Eq, .. }), "compara contra null");
    }

    #[test]
    fn desugared_string_switch_with_case_null_still_type_checks() {
        preserves_semantics(
            "class C { int m(String s) { switch (s) { case \"a\": return 1; case null: return -1; default: return 0; } } }",
        );
    }

    #[test]
    fn desugared_pattern_switch_with_returning_arms_still_type_checks() {
        preserves_semantics(
            "class C { int m(Object o) { switch (o) { \
             case String s when s.length() > 0 -> { return s.length(); } \
             case Integer i -> { return 7; } \
             default -> { return 0; } } } }",
        );
    }

    #[test]
    fn switch_expression_without_default_is_left_for_codegen() {
        // Sin `default`, la asignación definitiva no vería la variable escrita: se deja al codegen.
        let unit = desugared("class C { int m(int x) { int r = switch (x) { case 1 -> 10; case 2 -> 20; }; return r; } }");
        let StmtKind::LocalVar { init: Some(e), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(e.kind, ExprKind::Switch { .. }), "sin default: no se baja: {:?}", e.kind);
    }

    // ---- preservación de semántica ----

    #[test]
    fn desugared_array_for_each_still_type_checks() {
        preserves_semantics("class C { int m(int[] xs) { int s = 0; for (int x : xs) { s = s + x; } return s; } }");
    }

    #[test]
    fn desugared_assert_still_type_checks() {
        preserves_semantics("class C { void m(int p) { assert p > 0 : \"positivo\"; } }");
    }

    #[test]
    fn desugared_try_with_resources_still_type_checks() {
        preserves_semantics(
            "class R implements AutoCloseable { public void close() {} }
             class C { void m() { try (R r = new R()) { int x = 1; } } }",
        );
    }

    #[test]
    fn desugared_concat_still_type_checks() {
        preserves_semantics("class C { String m(String a, int b, Object c) { return a + b + c; } }");
    }

    #[test]
    fn desugared_compound_assignment_still_type_checks() {
        preserves_semantics("class C { int m(int x) { x += 5; x *= 2; return x; } }");
    }

    #[test]
    fn byte_compound_assignment_inserts_the_narrowing_cast() {
        // Sin el cast, `b = b + 1` sería `byte = int` → error. El cast lo evita.
        preserves_semantics("class C { byte m(byte b) { b += 1; return b; } }");
    }

    #[test]
    fn desugared_increment_still_type_checks() {
        preserves_semantics("class C { int m(int n) { int s = 0; for (int i = 0; i < n; i++) { s += i; } return s; } }");
    }

    #[test]
    fn desugared_byte_increment_inserts_the_narrowing_cast() {
        // `b++` sobre un `byte` necesita el mismo cast de reducción que `b += 1`.
        preserves_semantics("class C { byte m(byte b) { b++; --b; return b; } }");
    }

    #[test]
    fn desugared_varargs_still_type_checks() {
        preserves_semantics("class C { int f(int... xs) { return 0; } int m() { return f(1, 2, 3); } }");
    }

    #[test]
    fn desugared_switch_expression_local_still_type_checks() {
        preserves_semantics("class C { int m(int x) { int r = switch (x) { case 1 -> 10; case 2 -> 20; default -> 0; }; return r; } }");
    }

    #[test]
    fn desugared_switch_expression_return_still_type_checks() {
        preserves_semantics("class C { int m(int x) { return switch (x) { case 1 -> 10; default -> 0; }; } }");
    }

    #[test]
    fn desugared_string_switch_still_type_checks() {
        // Re-atribuir el árbol bajado ejercita que `hashCode()`/`equals()` resuelvan sobre `String`.
        preserves_semantics(
            "class C { int m(String s) { switch (s) { case \"a\": return 1; case \"b\": return 2; default: return 0; } } }",
        );
    }

    #[test]
    fn desugared_string_switch_with_hash_collision_still_type_checks() {
        // "Aa" y "BB" colisionan en hashCode() (2112): deben ir bajo el mismo case con if/else-equals.
        preserves_semantics(
            "class C { int m(String s) { switch (s) { case \"Aa\": return 1; case \"BB\": return 2; default: return 0; } } }",
        );
    }

    #[test]
    fn nested_for_each_is_fully_lowered() {
        // Un for-each dentro de otro: ambos deben quedar bajados (ningún ForEach residual).
        let unit = desugared("class C { void m(int[][] xss) { for (int[] xs : xss) { for (int x : xs) { int y = x; } } } }");
        assert!(!has_for_each(&unit.types[0]), "quedó un for-each sin bajar");
    }

    fn has_for_each(class: &ClassDecl) -> bool {
        fn in_stmt(s: &Stmt) -> bool {
            match &s.kind {
                StmtKind::ForEach { .. } => true,
                StmtKind::Block(b) => b.0.iter().any(in_stmt),
                StmtKind::If { then, els, .. } => in_stmt(then) || els.as_ref().is_some_and(|e| in_stmt(e)),
                StmtKind::While { body, .. } | StmtKind::Do { body, .. } => in_stmt(body),
                StmtKind::For { body, .. } => in_stmt(body),
                _ => false,
            }
        }
        class.members.iter().any(|m| match m {
            Member::Method(me) => me.body.as_ref().is_some_and(|b| b.0.iter().any(in_stmt)),
            _ => false,
        })
    }

    // ---- captura de la instancia envolvente (`this$0`): solo por uso (§8.1.3) ----

    /// Corre el front-end completo (incluido `register_local_classes`/`hoist_anonymous`, que bajan las
    /// clases locales y anónimas) y luego el desugar. Necesario para ejercitar `lift_local_class`.
    fn desugared_full(src: &str) -> CompilationUnit {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (mut table, _e1) = enter(&unit);
        let mut errs = Vec::new();
        crate::javac::enter::register_local_classes(&mut unit, &mut table, &mut errs);
        crate::javac::enter::hoist_anonymous(&mut unit, &mut table, &mut errs);
        attribute(&mut unit, &table);
        desugar(&mut unit, &mut table);
        unit
    }

    /// Como `preserves_semantics`, pero con el front-end completo (locales/anónimas).
    fn preserves_semantics_full(src: &str) {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (mut table, e1) = enter(&unit);
        let mut errs = Vec::new();
        crate::javac::enter::register_local_classes(&mut unit, &mut table, &mut errs);
        crate::javac::enter::hoist_anonymous(&mut unit, &mut table, &mut errs);
        let e2 = attribute(&mut unit, &table);
        assert!(e1.is_empty() && e2.is_empty() && errs.is_empty(), "el fuente ya tenía errores: {e1:?} {e2:?} {errs:?}");
        desugar(&mut unit, &mut table);
        let e3 = attribute(&mut unit, &table);
        let e4 = flow(&unit);
        assert!(e3.is_empty(), "desugar introdujo errores de tipo: {e3:?}");
        assert!(e4.is_empty(), "desugar introdujo errores de flujo: {e4:?}");
    }

    /// Todos los tipos anidados (recursivo) de la unidad — incluidas las locales/anónimas ya levantadas.
    fn all_nested(unit: &CompilationUnit) -> Vec<&ClassDecl> {
        fn rec<'a>(c: &'a ClassDecl, out: &mut Vec<&'a ClassDecl>) {
            for m in &c.members {
                if let Member::Type(n) = m {
                    out.push(n);
                    rec(n, out);
                }
            }
        }
        let mut out = Vec::new();
        for t in &unit.types {
            rec(t, &mut out);
        }
        out
    }

    /// El único tipo anidado que declara un método `run` — la clase interna bajo prueba.
    fn the_runner(unit: &CompilationUnit) -> &ClassDecl {
        let runners: Vec<&ClassDecl> = all_nested(unit)
            .into_iter()
            .filter(|c| c.members.iter().any(|m| matches!(m, Member::Method(me) if me.name == "run")))
            .collect();
        assert_eq!(runners.len(), 1, "se esperaba exactamente una clase con `run`");
        runners[0]
    }

    fn has_this0(c: &ClassDecl) -> bool {
        c.members.iter().any(|m| matches!(m, Member::Field(f) if f.name == "this$0"))
    }

    /// Cantidad de parámetros del (primer) constructor del AST, o `None` si la clase no declara uno
    /// (una anónima sin capturas ni instancia envolvente no gana constructor hasta el codegen).
    fn ctor_params(c: &ClassDecl) -> Option<usize> {
        c.members.iter().find_map(|m| match m {
            Member::Method(me) if me.is_constructor => Some(me.params.len()),
            _ => None,
        })
    }

    #[test]
    fn an_anonymous_class_that_ignores_the_enclosing_instance_drops_this0() {
        // El bug: capturaba `this$0` aunque `run()` no tocara nada del enclosing. javac no crea el campo
        // (§8.1.3), aunque igual pasa el parámetro Outer al ctor (para el `requireNonNull` de §15.9.5).
        let unit = desugared_full(
            "interface R { void run(); } \
             class C { int f; void m() { R r = new R() { public void run() { int x = 1; } }; } }",
        );
        let anon = the_runner(&unit);
        assert!(!has_this0(anon), "no debe capturar this$0 si no usa el enclosing:\n{}", crate::javac::ast_view::tree(&unit));
        assert_eq!(ctor_params(anon), Some(1), "el ctor igual recibe el parámetro Outer");
    }

    #[test]
    fn an_anonymous_class_that_reads_an_enclosing_field_keeps_this0() {
        let unit = desugared_full(
            "interface R { void run(); } \
             class C { int f; void m() { R r = new R() { public void run() { int x = f; } }; } }",
        );
        assert!(has_this0(the_runner(&unit)), "usa `f` del enclosing: captura this$0");
    }

    #[test]
    fn an_anonymous_class_that_calls_an_enclosing_method_keeps_this0() {
        let unit = desugared_full(
            "interface R { void run(); } \
             class C { void g() {} void m() { R r = new R() { public void run() { g(); } }; } }",
        );
        assert!(has_this0(the_runner(&unit)), "llama `g()` del enclosing: captura this$0");
    }

    #[test]
    fn an_anonymous_class_that_uses_outer_this_keeps_this0() {
        let unit = desugared_full(
            "interface R { void run(); } \
             class C { void m() { R r = new R() { public void run() { Object o = C.this; } }; } }",
        );
        assert!(has_this0(the_runner(&unit)), "usa `C.this`: captura this$0");
    }

    #[test]
    fn an_anonymous_class_in_a_static_method_takes_no_enclosing_param() {
        let unit = desugared_full(
            "interface R { void run(); } \
             class C { static void m() { R r = new R() { public void run() { int x = 1; } }; } }",
        );
        let anon = the_runner(&unit);
        assert!(!has_this0(anon), "sin instancia envolvente, sin this$0");
        // Sin capturas ni instancia envolvente, la anónima ni siquiera declara un ctor en el AST
        // (lo sintetiza el codegen); si lo declarara, no llevaría el parámetro Outer.
        assert_ne!(ctor_params(anon), Some(1), "ningún parámetro Outer");
    }

    #[test]
    fn a_local_class_that_ignores_the_enclosing_instance_drops_this0() {
        let unit = desugared_full(
            "class C { int f; void m() { class L { void run() { int x = 1; } } new L().run(); } }",
        );
        let l = the_runner(&unit);
        assert!(!has_this0(l), "una local que no usa el enclosing no captura this$0");
        assert_eq!(ctor_params(l), Some(1), "pero el ctor recibe el parámetro Outer (contexto de instancia)");
    }

    #[test]
    fn a_local_class_that_uses_the_enclosing_instance_keeps_this0() {
        let unit = desugared_full(
            "class C { int f; void m() { class L { void run() { int x = f; } } new L().run(); } }",
        );
        assert!(has_this0(the_runner(&unit)), "usa `f`: captura this$0");
    }

    #[test]
    fn a_member_inner_class_that_ignores_the_enclosing_instance_drops_this0() {
        let unit = desugared_full("class C { int f; class I { void run() { int x = 1; } } }");
        let i = the_runner(&unit);
        assert!(!has_this0(i), "una interna miembro que no usa el enclosing no captura this$0");
        assert_eq!(ctor_params(i), Some(1), "el ctor sí recibe Outer (la interna se crea con `outer.new I()`)");
    }

    #[test]
    fn a_member_inner_class_that_uses_the_enclosing_instance_keeps_this0() {
        let unit = desugared_full("class C { int f; class I { void run() { int x = f; } } }");
        assert!(has_this0(the_runner(&unit)), "usa `f`: captura this$0");
    }

    #[test]
    fn a_local_class_still_captures_effectively_final_locals() {
        // La captura de locales (`val$x`) es por uso y **no** debe romperse por el cambio en this$0.
        let unit = desugared_full(
            "class C { void m(int p) { class L { void run() { int x = p; } } new L().run(); } }",
        );
        let l = the_runner(&unit);
        assert!(l.members.iter().any(|m| matches!(m, Member::Field(f) if f.name == "val$p")), "captura val$p");
        assert!(!has_this0(l), "pero no this$0 (no usa el enclosing)");
    }

    #[test]
    fn a_nested_local_class_that_uses_the_outer_forces_the_middle_to_capture_it() {
        // Transitividad (§8.1.3): la local de en medio no usa `f` directamente, pero su local anidada
        // sí, así que la del medio tiene que capturar this$0 para pasárselo.
        let unit = desugared_full(
            "class C { int f; void m() { \
               class Mid { void run() { class Inner { void run() { int x = f; } } new Inner().run(); } } \
               new Mid().run(); } }",
        );
        let mid = all_nested(&unit)
            .into_iter()
            .find(|c| c.name.contains("Mid"))
            .expect("la local del medio");
        assert!(has_this0(mid), "la del medio captura this$0 para pasárselo a la anidada:\n{}", crate::javac::ast_view::tree(&unit));
    }

    #[test]
    fn the_this0_capture_by_use_still_type_checks() {
        preserves_semantics_full(
            "interface R { void run(); } \
             class C { int f; void g() {} \
               void a() { R r = new R() { public void run() { int x = 1; } }; r.run(); } \
               void b() { R r = new R() { public void run() { g(); int x = f; } }; r.run(); } \
               void c() { class L { void run() { int x = f; } } new L().run(); } \
               void d(int p) { class L2 { void run() { int x = p; } } new L2().run(); } }",
        );
    }
}
