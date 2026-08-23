//! **Annotation processing** (APT, JSR 269) — dos piezas del plan que viven juntas acá porque
//! ambas necesitan correr código Java *desde afuera* (las primitivas `pub(super)` de
//! [`Exec`](super::bytecode_interpreter)), y por eso este módulo está dentro de `interpreter` y no
//! en `javac`:
//!
//! * **Fase 2 — el *round loop*** ([`run_processors`]): descubre un `Processor` por FQN, lo
//!   instancia en la VM, corre `init(env)` y el bucle de **rondas** (una normal + la ronda final
//!   con `processingOver == true`). Es el hito mínimo: todavía no reifica elementos, así que el
//!   `Set` de anotaciones y las raíces son vacíos y un processor sólo puede *imprimir* (vía el
//!   native `AptTrace.trace`, porque `System.out.println` aún no compila en este javac).
//! * **Fase 3 — la reificación del modelo** ([`AptContext`]): convierte un
//!   [`Symbol`](crate::javac::symbol::Symbol) de la tabla del compilador en un **objeto del heap**
//!   (`jdk/internal/apt/SymElement`) que un procesador puede tocar por `invokevirtual`. Funciona
//!   porque `javac` y esta JVM viven en el **mismo proceso** (un crate): la [`SymbolTable`] es una
//!   arena append-only indexada por `SymbolId` estable y de solo-lectura, así que un `SymElement`
//!   guarda un `int sym` y sus `native` releen la tabla viva (ver `natives::dispatch`). Capa 1:
//!   sólo identidad + nombre (`getSimpleName`); enums/listas/re-entrada son capas siguientes.
//!
//! **Por qué la tabla va por `Arc` y no `Rc`.** [`SharedVm`](super::bytecode_interpreter) debe ser
//! `Send + Sync` (el driver `os`/`os-gil` la comparte tras un `Arc<RwLock<…>>` y spawnea hilos).
//! `Rc` no es `Send`; para que `Arc<SymbolTable>` sea `Sync` los contadores internos de la tabla
//! pasaron de `Cell<u32>` a `AtomicU32` (semánticamente idéntico). Así el llamador conserva su copia
//! y no hacen falta candados ni `unsafe`.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;

use crate::javac::symbol::{SymbolId, SymbolTable};

use super::bytecode_interpreter::objects_operations::{self, field_offset};
use super::bytecode_interpreter::{class_operations, JVM};
use super::frame::{Frame, Value};
use super::heap::HeapService;
use super::metaspace::{MethodId, MetaspaceService};

// ============================================================================================
//  Fase 2 — el round loop
// ============================================================================================

/// Clases de soporte (KajiLibrary) que el driver reifica, y las firmas del contrato `Processor`.
const PROC_ENV_IMPL: &str = "javax/annotation/processing/ProcessingEnvironmentImpl";
const ROUND_ENV_IMPL: &str = "javax/annotation/processing/RoundEnvironmentImpl";
const HASH_SET: &str = "java/util/HashSet";
const INIT_DESC: &str = "(Ljavax/annotation/processing/ProcessingEnvironment;)V";
const PROCESS_DESC: &str = "(Ljava/util/Set;Ljavax/annotation/processing/RoundEnvironment;)Z";

/// El resultado de correr el bucle de annotation processing.
#[derive(Debug, Default)]
pub struct AptOutcome {
    /// Cuántas veces corrió `process()` en total (una por processor y por ronda, incluida la final).
    pub process_calls: usize,
    /// Cuántas rondas se corrieron (rondas normales + la ronda final).
    pub rounds: usize,
    /// Lo que los processors imprimieron durante el bucle (vía el native `AptTrace.trace`, ya que
    /// `System.out.println` todavía no compila en este javac).
    pub console: String,
    /// El nombre interno de la clase de la excepción que abortó el bucle, si alguna escapó.
    pub error: Option<String>,
}

/// Corre el bucle de annotation processing sobre `processors` (nombres internos, p. ej.
/// `com/foo/MyProcessor`), con la VM booteada de `boot` (loader bootstrap: KajiLibrary + `boot/`)
/// y `app` (loader de aplicación: el `-processorpath` y el `-cp`).
///
/// Descubrimiento: en esta fase los processors se pasan **explícitos** (el `-processor <FQN>` de
/// `javac`), no por `META-INF/services`. Para cada uno: se lo instancia en la VM, se corre
/// `init(env)`, y luego el bucle de rondas. El MVP corre **una ronda normal** (nada se genera) y la
/// **ronda final** (`processingOver == true`).
pub fn run_processors(processors: &[String], boot: Vec<PathBuf>, app: Vec<PathBuf>) -> AptOutcome {
    let mut outcome = AptOutcome::default();
    let Some(first) = processors.first() else {
        return outcome; // sin processors no hay nada que correr
    };

    // La VM necesita un frame de arranque aunque nunca lo ejecute (`call_java` empuja *por encima*
    // de él y desenrolla de vuelta). Sirve cualquier método con cuerpo; usamos el `<init>` del
    // primer processor, que javac siempre genera. Resolverlo también valida el descubrimiento: si
    // la clase no está en el classpath, fallamos acá con un error claro.
    let mut metaspace = MetaspaceService::new(boot, app);
    let Some(entry) = metaspace.resolve_method(first, "<init>", "()V") else {
        outcome.error = Some(format!("processor no encontrado o sin <init>: {first}"));
        return outcome;
    };
    let max_locals = metaspace.max_locals(entry);
    let mut jvm = JVM::new(metaspace, Frame::new(entry, max_locals, Vec::new()));

    // --- Instanciación (una vez por processor) --------------------------------------------------
    // `[(fqn, receiver)]`: cada processor construido + inicializado, listo para recibir rondas.
    let mut instances: Vec<(String, usize)> = Vec::new();
    for fqn in processors {
        // 1. `new Processor()` + `<init>()V`.
        let Some(obj) = new_instance(&mut jvm, fqn, "()V", Vec::new()) else {
            outcome.error = take_error(&mut jvm).or(Some(format!("no se pudo instanciar {fqn}")));
            outcome.console = console(&mut jvm);
            return outcome;
        };
        // 2. Reificar un `ProcessingEnvironment` mínimo y correr `init(env)` (heredado de
        //    `AbstractProcessor`, así que se despacha virtual para encontrar el override real).
        let Some(env) = new_instance(&mut jvm, PROC_ENV_IMPL, "()V", Vec::new()) else {
            outcome.error = take_error(&mut jvm).or(Some("no se pudo reificar el env".into()));
            outcome.console = console(&mut jvm);
            return outcome;
        };
        jvm.exec().call_virtual(obj, "init", INIT_DESC, vec![Value::Reference(env)]);
        if let Some(err) = take_error(&mut jvm) {
            outcome.error = Some(err);
            outcome.console = console(&mut jvm);
            return outcome;
        }
        instances.push((fqn.clone(), obj));
    }

    // --- El bucle de rondas ---------------------------------------------------------------------
    // Ronda(s) normal(es): en el MVP nada se genera (el conjunto de anotaciones y las raíces son
    // vacíos), así que hay exactamente una. Luego la ronda final con `processingOver == true`.
    loop {
        run_round(&mut jvm, &instances, false, &mut outcome);
        if outcome.error.is_some() {
            outcome.console = console(&mut jvm);
            return outcome;
        }
        // Sin reificación de elementos ni `Filer`, ningún processor puede aún emitir un nuevo
        // fuente: no hay nuevas unidades → el bucle de rondas normales termina tras la primera.
        break;
    }
    run_round(&mut jvm, &instances, true, &mut outcome);

    outcome.console = console(&mut jvm);
    outcome.error = outcome.error.take().or_else(|| take_error(&mut jvm));
    outcome
}

/// Una ronda: construye un `RoundEnvironment` (con `processingOver = over`) y un conjunto de
/// anotaciones vacío, y llama `process(annotations, roundEnv)` en cada processor. Corta apenas
/// una llamada deja una excepción pendiente.
fn run_round(jvm: &mut JVM, instances: &[(String, usize)], over: bool, outcome: &mut AptOutcome) {
    // El `RoundEnvironment` y el `Set` de anotaciones se comparten entre todos los processors de la
    // ronda (mismo contrato de JSR 269): se construyen una vez por ronda.
    let flag = Value::Int(over as i32);
    let Some(round_env) = new_instance(jvm, ROUND_ENV_IMPL, "(Z)V", vec![flag]) else {
        outcome.error = take_error(jvm).or(Some("no se pudo reificar el roundEnv".into()));
        return;
    };
    let Some(annotations) = new_instance(jvm, HASH_SET, "()V", Vec::new()) else {
        outcome.error = take_error(jvm).or(Some("no se pudo crear el set de anotaciones".into()));
        return;
    };
    for (_fqn, obj) in instances {
        let args = vec![Value::Reference(annotations), Value::Reference(round_env)];
        jvm.exec().call_virtual(*obj, "process", PROCESS_DESC, args);
        outcome.process_calls += 1;
        if let Some(err) = take_error(jvm) {
            outcome.error = Some(err);
            return;
        }
    }
    outcome.rounds += 1;
}

/// Aloca una instancia de `class` (carga + prepara + `<clinit>`) y corre su constructor
/// `<init> ctor_desc` con `ctor_args` (sin el `this`, que este helper antepone). Devuelve la
/// referencia, o `None` si la clase no se pudo inicializar o el constructor no existe.
fn new_instance(jvm: &mut JVM, class: &str, ctor_desc: &str, ctor_args: Vec<Value>) -> Option<usize> {
    let obj = jvm.exec().apt_new_instance(class)?;
    let ctor = jvm.exec().apt_resolve(class, "<init>", ctor_desc)?;
    // Anchos de slot: el receptor primero (1), luego cada parámetro. En el MVP los constructores
    // de soporte sólo toman category-1 (`boolean`), así que cada arg mide un slot.
    let mut operands = vec![Value::Reference(obj)];
    let widths: Vec<usize> = std::iter::once(1).chain(ctor_args.iter().map(|_| 1)).collect();
    operands.extend(ctor_args);
    call(jvm, ctor, operands, &widths);
    Some(obj)
}

/// `call_java` a través de una vista `Exec` fresca (todo el estado vive en el `JVM`).
fn call(jvm: &mut JVM, method: MethodId, args: Vec<Value>, widths: &[usize]) -> Option<Value> {
    jvm.exec().call_java(method, args, widths)
}

/// Retira la excepción pendiente (si la hay) y devuelve el nombre interno de su clase.
fn take_error(jvm: &mut JVM) -> Option<String> {
    jvm.exec().apt_take_pending()
}

/// Lo impreso por `System.out` hasta ahora.
fn console(jvm: &mut JVM) -> String {
    jvm.exec().console().to_string()
}

// ============================================================================================
//  Fase 3 — la reificación del modelo de elementos
// ============================================================================================

/// La clase de la biblioteca que **reifica** un `Symbol` en el heap: un `int sym` (el
/// `SymbolId`) más los `native` de nombre. Vive en `KajiLibrary/jdk/internal/apt/`.
pub const SYM_ELEMENT: &str = "jdk/internal/apt/SymElement";

/// El estado del modelo APT durante una corrida: la tabla del compilador (compartida por `Arc`)
/// y la **caché de identidad** `SymbolId → offset del SymElement`. La caché es lo que hace que
/// dos pedidos del mismo símbolo devuelvan el **mismo** objeto (como `getClass()` devuelve el
/// mismo mirror), condición para que `==` entre elementos tenga sentido en las capas siguientes.
pub struct AptContext {
    table: Arc<SymbolTable>,
    reified: HashMap<SymbolId, usize>,
}

impl AptContext {
    /// Comparte la tabla por `Arc` (ver la nota del módulo sobre `Send + Sync`); el llamador
    /// conserva su propia copia del `Arc`.
    pub fn new(table: Arc<SymbolTable>) -> Self {
        AptContext { table, reified: HashMap::new() }
    }

    /// La tabla del compilador que este contexto reifica (solo-lectura), para que los `native`
    /// releen `Symbol.name`, `members_of`, etc.
    pub fn table(&self) -> &SymbolTable {
        &self.table
    }

    /// El `SymElement` del heap que reifica `sym`, construyéndolo **una sola vez** (caché de
    /// identidad). Carga la clase, aloca la instancia y le escribe el `SymbolId` en el campo
    /// `sym`; los pedidos siguientes del mismo símbolo devuelven ese mismo offset.
    ///
    /// Alocado en **Old** (`allocate_old`): un elemento es un *handle* durable que el procesador
    /// puede sostener a través de otras alocaciones/GC —como un mirror `Class`—, así que no debe
    /// moverse. El campo `sym` es un `int` (no una referencia), así que no hace falta barrera de
    /// escritura.
    pub fn element_for(
        &mut self,
        sym: SymbolId,
        metaspace: &mut MetaspaceService,
        heap: &mut HeapService,
    ) -> usize {
        if let Some(&offset) = self.reified.get(&sym) {
            return offset;
        }
        class_operations::load_class(metaspace, heap, SYM_ELEMENT);
        let object = objects_operations::allocate_old(metaspace, heap, SYM_ELEMENT);
        let sym_offset = field_offset(metaspace, SYM_ELEMENT, "sym");
        heap.write_u32(object + sym_offset, sym as u32);
        self.reified.insert(sym, object);
        object
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::interpreter::strings;

    /// End-to-end de la fase 2: se **compila** un `HelloProcessor` con el propio javac del
    /// proyecto, se lo **descubre** por FQN, se lo **instancia** en la VM, se corre `init` y el
    /// **bucle de rondas** (normal + final). El processor sólo imprime el flag `processingOver`,
    /// así que verifica de punta a punta: descubrimiento → instanciación → init → process → bucle →
    /// ronda final. Estilo `run_int` de `gc.rs`, pero compilando el fuente en caliente.
    #[test]
    fn hello_processor_runs_a_normal_round_then_the_final_round() {
        // Un processor mínimo: extiende AbstractProcessor y en cada ronda "imprime" (vía el native
        // `AptTrace.trace`, porque `System.out.println` todavía no compila en este javac) si es la
        // ronda final o no. Sin concatenación de String (dos literales) para no depender de indy.
        let source = r#"
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.AptTrace;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;
@SupportedAnnotationTypes("*")
public class HelloProcessor extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            AptTrace.trace("HelloProcessor round over=true");
        } else {
            AptTrace.trace("HelloProcessor round over=false");
        }
        return false;
    }
}
"#;
        // Compilar con el javac del proyecto, resolviendo la API de APT contra KajiLibrary.
        let kaji = PathBuf::from("KajiLibrary");
        let classes = crate::javac::compile_cp(source, std::slice::from_ref(&kaji))
            .expect("HelloProcessor debería compilar");

        // Escribir los `.class` a un processorpath temporal, por nombre interno.
        let dir = std::env::temp_dir().join(format!("apt_test_{}", std::process::id()));
        std::fs::create_dir_all(&dir).expect("crear dir temporal");
        for (internal, bytes) in &classes {
            let path = dir.join(format!("{internal}.class"));
            if let Some(parent) = path.parent() {
                std::fs::create_dir_all(parent).ok();
            }
            std::fs::write(&path, bytes).expect("escribir .class");
        }

        // Correr el bucle: boot = [KajiLibrary, boot], app = [processorpath].
        let boot = vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")];
        let app = vec![dir.clone()];
        let outcome = run_processors(&["HelloProcessor".to_string()], boot, app);

        // Limpieza best-effort del processorpath temporal.
        std::fs::remove_dir_all(&dir).ok();

        assert!(outcome.error.is_none(), "el bucle no debería fallar: {:?}", outcome.error);
        // Dos rondas: una normal + la final. Un solo processor → dos llamadas a process.
        assert_eq!(outcome.rounds, 2, "una ronda normal + la final");
        assert_eq!(outcome.process_calls, 2, "process debería correr dos veces");
        // Y process realmente corrió en la VM, viendo el flag correcto en cada ronda.
        assert!(
            outcome.console.contains("HelloProcessor round over=false"),
            "ronda normal: {:?}",
            outcome.console
        );
        assert!(
            outcome.console.contains("HelloProcessor round over=true"),
            "ronda final: {:?}",
            outcome.console
        );
    }

    /// El "hola mundo" de la reificación (fase 3): compilar `class Foo {}`, reificar su
    /// `TypeElement`, y que `getSimpleName()` devuelva `"Foo"` — leyendo la tabla viva de `javac`
    /// desde un `native` de la JVM, en el mismo proceso.
    #[test]
    fn get_simple_name_reifies_a_root_type() {
        // Pasada 1 de javac: `class Foo {}` → la tabla de símbolos, compartida por `Arc`.
        let (_unit, table, errors) = crate::javac::analyze("class Foo {}").expect("analyze");
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let table = Arc::new(table);
        let foo = table.class("Foo").expect("símbolo de Foo");

        // Una JVM sobre KajiLibrary (donde vive SymElement, String, Object…). No corre `main`:
        // se para en un método cualquiera y `call_virtual` maneja el native sin frame.
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("KajiLibrary")], vec![]);
        let park = metaspace
            .resolve_method("java/lang/Object", "hashCode", "()I")
            .expect("un método donde estacionar el frame de entrada");
        let max_locals = metaspace.max_locals(park);
        let mut jvm = JVM::new(metaspace, Frame::new(park, max_locals, Vec::new()));

        // El cable APT: compartir la tabla con el VM por `Arc` (no la consume; ver nota del módulo).
        jvm.set_apt(AptContext::new(Arc::clone(&table)));

        // Reificar Foo y pedirle su nombre simple por invokevirtual (→ el native).
        let element = jvm.exec().apt_element_for(foo);
        let name_ref = jvm
            .exec()
            .call_virtual(element, "getSimpleName", "()Ljava/lang/String;", Vec::new())
            .expect("getSimpleName devuelve un valor");
        let Value::Reference(name) = name_ref else {
            panic!("getSimpleName devuelve una referencia a String, no {name_ref:?}");
        };
        assert_eq!(strings::read(jvm.exec().heap(), name), "Foo");

        // La caché de identidad: reificar Foo otra vez da el **mismo** objeto.
        assert_eq!(jvm.exec().apt_element_for(foo), element, "identidad estable");

        // Y el VM no consumió la tabla: el llamador conserva su `Arc` (sigue consultable).
        assert_eq!(table.symbol(foo).name, "Foo", "el llamador conserva la tabla");
    }
}
