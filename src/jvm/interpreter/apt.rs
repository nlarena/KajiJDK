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

use crate::javac::ast::CompilationUnit;
use crate::javac::symbol::{SymbolId, SymbolTable};

use super::bytecode_interpreter::objects_operations::{self, field_offset};
use super::bytecode_interpreter::{class_operations, JVM};
use super::frame::{Frame, Value};
use super::heap::HeapService;
use super::metaspace::{MethodId, MetaspaceService};
use super::natives::{drain_filer, install_filer};

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
    /// Los fuentes que los procesadores **generaron** vía el `Filer`, en orden de creación: cada uno
    /// `(nombre pedido a `createSourceFile`, texto que el procesador escribió en su `StringWriter`)`.
    pub generated_sources: Vec<(String, String)>,
    /// Los `.class` **compilados** a partir de esos fuentes generados —`(nombre interno, bytes)`—:
    /// una unidad generada puede producir varias clases (anidadas, sintéticas). Es lo que deja
    /// "disponible" lo generado: el llamador puede escribirlos a un classpath y correrlos.
    pub generated_classes: Vec<(String, Vec<u8>)>,
}

/// Tope de rondas normales, para que un procesador que genera fuente en cada ronda (un bug, o una
/// dependencia cíclica de generación) no cuelgue el compilador. `javac` real tiene el mismo riesgo;
/// un límite alto no molesta a un uso legítimo (que converge en unas pocas rondas) y corta el bucle.
const MAX_ROUNDS: usize = 100;

/// El recurso de `META-INF/services` que nombra los procesadores de un processorpath (JSR 269 §2).
const PROCESSOR_SERVICE: &str = "META-INF/services/javax.annotation.processing.Processor";

/// Corre el bucle de annotation processing sobre `processors` (nombres internos, p. ej.
/// `com/foo/MyProcessor`), con la VM booteada de `boot` (loader bootstrap: KajiLibrary + `boot/`)
/// y `app` (loader de aplicación: el `-processorpath` y el `-cp`).
///
/// **Descubrimiento.** Si `processors` viene vacío, se descubren por el recurso
/// `META-INF/services/javax.annotation.processing.Processor` de cada directorio de `app` (el
/// `-processorpath`), como manda JSR 269. Si viene con nombres, se usan tal cual (el `-processor
/// <FQN>` explícito de `javac`, que gana sobre el descubrimiento).
///
/// **El bucle (con re-entrada del `Filer`).** Cada procesador se instancia una vez y recibe su
/// `init(env)`, con un `ProcessingEnvironment` cuyo `getFiler()` entrega un `KajiFiler`. Luego el
/// bucle de rondas: antes de cada ronda se **arma** el `Filer` del hilo; tras `process()` se **drena**
/// y, por cada fuente que un procesador fabricó, se recupera su texto (reentrante, vía
/// `read_generated_text`). Los fuentes generados se **parsean y entran** en una `SymbolTable`
/// acumulada (multi-unidad), se **compilan** a `.class` (disponibles para el llamador y para la VM en
/// las rondas siguientes) y disparan **otra** ronda normal. El bucle termina cuando una ronda no
/// genera nada; entonces corre la **ronda final** (`processingOver == true`).
pub fn run_processors(processors: &[String], boot: Vec<PathBuf>, app: Vec<PathBuf>) -> AptOutcome {
    let mut outcome = AptOutcome::default();

    // Descubrimiento por servicios si no hubo `-processor` explícito. `app` es el processorpath.
    let discovered;
    let processors: &[String] = if processors.is_empty() {
        discovered = discover_processors(&app);
        &discovered
    } else {
        processors
    };
    let Some(first) = processors.first() else {
        return outcome; // ni explícitos ni descubiertos: nada que correr
    };

    // Directorio de salida de lo generado: los `.class` que se compilen de los fuentes que fabrique
    // el `Filer` se escriben acá, y se **antepone** al loader de aplicación para que la VM los cargue
    // en las rondas siguientes (y el compilador los resuelva al generar código que dependa de ellos).
    let gen_dir = std::env::temp_dir()
        .join(format!("apt_gen_{}_{}", std::process::id(), gen_seq()));
    let _ = std::fs::create_dir_all(&gen_dir);
    // Classpath para (re)compilar y (re)entrar lo generado: primero lo ya generado, luego el boot
    // (KajiLibrary) y el app originales, así un fuente generado puede referenciar a otro anterior o a
    // la biblioteca. (`compile_cp`/`enter_multi` ya anteponen esto al classpath por defecto.)
    let mut lib_dirs: Vec<PathBuf> = vec![gen_dir.clone()];
    lib_dirs.extend(boot.iter().cloned());
    lib_dirs.extend(app.iter().cloned());
    // El loader de aplicación de la VM ve `gen_dir` primero (para cargar lo generado por nombre).
    let mut app_with_gen = vec![gen_dir.clone()];
    app_with_gen.extend(app.iter().cloned());

    // La VM necesita un frame de arranque aunque nunca lo ejecute (`call_java` empuja *por encima*
    // de él y desenrolla de vuelta). Sirve cualquier método con cuerpo; usamos el `<init>` del
    // primer processor, que javac siempre genera. Resolverlo también valida el descubrimiento: si
    // la clase no está en el classpath, fallamos acá con un error claro.
    let mut metaspace = MetaspaceService::new(boot, app_with_gen);
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
        // 2. Reificar un `ProcessingEnvironment` (con `getFiler()` → `KajiFiler`) y correr `init(env)`
        //    (heredado de `AbstractProcessor`, así que se despacha virtual para el override real).
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
    // Unidades **acumuladas**: cada fuente generado se parsea y se suma acá, y la tabla se reconstruye
    // multi-unidad para que la ronda siguiente lo vea reificado (fase 3). Empieza vacío (el round loop
    // del MVP no recibe unidades de entrada; sólo lo que los procesadores generan).
    let mut units: Vec<CompilationUnit> = Vec::new();
    let mut normal_rounds = 0usize;
    loop {
        let generated = run_round(&mut jvm, &instances, false, &mut outcome);
        if outcome.error.is_some() {
            outcome.console = console(&mut jvm);
            let _ = std::fs::remove_dir_all(&gen_dir);
            return outcome;
        }
        // Sin fuentes nuevos, el bucle de rondas normales terminó: cae a la ronda final.
        if generated.is_empty() {
            break;
        }
        // Re-entrada: parsear+entrar+compilar cada fuente generado.
        for (name, text) in generated {
            outcome.generated_sources.push((name.clone(), text.clone()));
            match crate::javac::parse(&text) {
                Ok(unit) => units.push(unit),
                Err(err) => {
                    outcome.error = Some(format!("fuente generado '{name}' no parsea: {err}"));
                    break;
                }
            }
            match crate::javac::compile_cp(&text, &lib_dirs) {
                Ok(classes) => {
                    for (internal, bytes) in classes {
                        let path = gen_dir.join(format!("{internal}.class"));
                        if let Some(parent) = path.parent() {
                            let _ = std::fs::create_dir_all(parent);
                        }
                        let _ = std::fs::write(&path, &bytes);
                        outcome.generated_classes.push((internal, bytes));
                    }
                }
                Err(err) => {
                    outcome.error = Some(format!("fuente generado '{name}' no compila: {err}"));
                    break;
                }
            }
        }
        if outcome.error.is_some() {
            outcome.console = console(&mut jvm);
            let _ = std::fs::remove_dir_all(&gen_dir);
            return outcome;
        }
        // Reconstruir la tabla acumulada (multi-unidad) y atarla al VM: la reificación de la próxima
        // ronda (fase 3) verá los tipos recién generados junto a los previos.
        let (table, _errs) = crate::javac::enter::enter_multi(&units, &lib_dirs);
        jvm.set_apt(AptContext::new(Arc::new(table)));

        normal_rounds += 1;
        if normal_rounds >= MAX_ROUNDS {
            outcome.error = Some(format!("APT: el bucle de rondas superó {MAX_ROUNDS} rondas"));
            outcome.console = console(&mut jvm);
            let _ = std::fs::remove_dir_all(&gen_dir);
            return outcome;
        }
    }
    // Ronda final (`processingOver == true`): un procesador no debería generar acá, así que lo que
    // drene se descarta (ya no hay ronda que lo procese).
    run_round(&mut jvm, &instances, true, &mut outcome);

    outcome.console = console(&mut jvm);
    outcome.error = outcome.error.take().or_else(|| take_error(&mut jvm));
    let _ = std::fs::remove_dir_all(&gen_dir);
    outcome
}

/// Una ronda: **arma** el `Filer` del hilo, construye un `RoundEnvironment` (con `processingOver =
/// over`) y un conjunto de anotaciones vacío, llama `process(annotations, roundEnv)` en cada
/// processor, y al terminar **drena** el `Filer` y recupera el texto de cada fuente registrado
/// (`(nombre, texto)`, en orden de creación). Corta apenas una llamada deja una excepción pendiente.
fn run_round(
    jvm: &mut JVM,
    instances: &[(String, usize)],
    over: bool,
    outcome: &mut AptOutcome,
) -> Vec<(String, String)> {
    // Armar el canal del `Filer` para esta ronda: lo que los procesadores registren queda acá hasta
    // que lo drenemos al final. (Cada ronda parte de cero — un fuente generado se procesa una sola vez.)
    install_filer();

    // El `RoundEnvironment` y el `Set` de anotaciones se comparten entre todos los processors de la
    // ronda (mismo contrato de JSR 269): se construyen una vez por ronda.
    let flag = Value::Int(over as i32);
    let Some(round_env) = new_instance(jvm, ROUND_ENV_IMPL, "(Z)V", vec![flag]) else {
        outcome.error = take_error(jvm).or(Some("no se pudo reificar el roundEnv".into()));
        drain_filer();
        return Vec::new();
    };
    let Some(annotations) = new_instance(jvm, HASH_SET, "()V", Vec::new()) else {
        outcome.error = take_error(jvm).or(Some("no se pudo crear el set de anotaciones".into()));
        drain_filer();
        return Vec::new();
    };
    for (_fqn, obj) in instances {
        let args = vec![Value::Reference(annotations), Value::Reference(round_env)];
        jvm.exec().call_virtual(*obj, "process", PROCESS_DESC, args);
        outcome.process_calls += 1;
        if let Some(err) = take_error(jvm) {
            outcome.error = Some(err);
            drain_filer();
            return Vec::new();
        }
    }
    outcome.rounds += 1;

    // Drenar lo que el `Filer` registró y recuperar su texto (reentrante, sobre el mismo heap).
    drain_filer()
        .into_iter()
        .map(|(name, writer_ref)| (name, jvm.read_generated_text(writer_ref as usize)))
        .collect()
}

/// Descubre procesadores por `META-INF/services/javax.annotation.processing.Processor` en cada
/// directorio de `processorpath`, en orden. Devuelve **nombres internos** (con `/`), sin duplicados,
/// ignorando líneas en blanco y comentarios (`#…`) como manda el formato de `ServiceLoader`.
fn discover_processors(processorpath: &[PathBuf]) -> Vec<String> {
    let mut names: Vec<String> = Vec::new();
    for dir in processorpath {
        let Ok(text) = std::fs::read_to_string(dir.join(PROCESSOR_SERVICE)) else {
            continue;
        };
        for line in text.lines() {
            // El comentario `#` va hasta fin de línea; luego se recorta el espacio.
            let entry = line.split('#').next().unwrap_or("").trim();
            if entry.is_empty() {
                continue;
            }
            let internal = entry.replace('.', "/");
            if !names.contains(&internal) {
                names.push(internal);
            }
        }
    }
    names
}

/// Un contador de proceso para que dos corridas de `run_processors` (p. ej. tests en paralelo) usen
/// directorios de generación distintos y no se pisen los `.class`.
fn gen_seq() -> usize {
    use std::sync::atomic::{AtomicUsize, Ordering};
    static SEQ: AtomicUsize = AtomicUsize::new(0);
    SEQ.fetch_add(1, Ordering::Relaxed)
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

/// El enum `javax.lang.model.element.ElementKind` — el que `SymElement.getKind()` (capa 4)
/// devuelve por constante estática, tras correr su `<clinit>`.
pub const ELEMENT_KIND: &str = "javax/lang/model/element/ElementKind";

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

        // Reificar Foo y pedirle su nombre simple por invokevirtual (→ el native). Capa 2: el
        // nombre viaja como un `Name` (un `SymName`), no como un `String`; su texto se lee con
        // `toString()` (parte del contrato `CharSequence` de `Name`).
        let element = jvm.exec().apt_element_for(foo);
        let name = jvm
            .exec()
            .call_virtual(element, "getSimpleName", "()Ljavax/lang/model/element/Name;", Vec::new())
            .expect("getSimpleName devuelve un valor");
        let Value::Reference(name_obj) = name else {
            panic!("getSimpleName devuelve una referencia a Name, no {name:?}");
        };
        assert_eq!(name_text(&mut jvm, name_obj), "Foo");

        // La caché de identidad: reificar Foo otra vez da el **mismo** objeto.
        assert_eq!(jvm.exec().apt_element_for(foo), element, "identidad estable");

        // Y el VM no consumió la tabla: el llamador conserva su `Arc` (sigue consultable).
        assert_eq!(table.symbol(foo).name, "Foo", "el llamador conserva la tabla");
    }

    /// Compila con el javac del proyecto cada fuente `source` (contra `cp`) y escribe sus `.class`
    /// bajo `dir`, por nombre interno (con subdirectorios de paquete), para que un classpath los
    /// resuelva. Helper de los tests de re-entrada.
    fn compile_into(dir: &std::path::Path, source: &str, cp: &[PathBuf]) {
        let classes = crate::javac::compile_cp(source, cp).expect("la fuente debe compilar");
        for (internal, bytes) in classes {
            let path = dir.join(format!("{internal}.class"));
            std::fs::create_dir_all(path.parent().unwrap()).unwrap();
            std::fs::write(path, bytes).unwrap();
        }
    }

    /// **El cable completo de fase 4 → fase 2**: un procesador usa su `Filer` para fabricar
    /// `FooGreeting` con un `public static int value() { return 42; }`; el round loop **drena** ese
    /// fuente, lo **re-parsea/entra/compila**, y el `.class` generado queda disponible y corre dando
    /// 42. Ejercita `createSourceFile → texto → re-entrada → enter → codegen`, más el criterio de
    /// terminación (una ronda que no genera nada → ronda final) y el conteo de rondas resultante.
    #[test]
    fn a_processor_generates_a_source_that_is_recompiled_and_runs() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;

        let kaji = PathBuf::from("KajiLibrary");
        let n = gen_seq();
        let base = std::env::temp_dir().join(format!("apt_reentry_{}_{n}", std::process::id()));
        let lib = base.join("lib"); // shadow de boot: Filer real + ProcessingEnvironmentImpl
        let proc = base.join("proc"); // processorpath: el procesador
        std::fs::create_dir_all(&lib).unwrap();
        std::fs::create_dir_all(&proc).unwrap();

        // 1) Las clases de soporte del `Filer`, compiladas con el javac del proyecto (bytecode que
        //    esta VM corre). Van a `lib`, que se antepone a `boot` para tapar la
        //    `ProcessingEnvironmentImpl` de KajiLibrary (cuya `getFiler()` ahora entrega un KajiFiler).
        //    En orden de dependencia: KajiSourceFile ← KajiFiler ← ProcessingEnvironmentImpl.
        let lib_cp = [lib.clone(), kaji.clone()];
        for name in [
            "javax/annotation/processing/KajiSourceFile",
            "javax/annotation/processing/KajiFiler",
            "javax/annotation/processing/ProcessingEnvironmentImpl",
        ] {
            let src = std::fs::read_to_string(kaji.join(format!("{name}.java"))).unwrap();
            compile_into(&lib, &src, &lib_cp);
        }

        // 2) El procesador: en la **primera** ronda normal usa el Filer para crear `FooGreeting`, y no
        //    vuelve a generar (guarda un flag). Captura el `env` en su propio campo (evita depender de
        //    resolver el campo heredado de AbstractProcessor). Sin `System.out` (no compila acá).
        let processor = r#"
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.Set;
@SupportedAnnotationTypes("*")
public class GenProcessor extends AbstractProcessor {
    private ProcessingEnvironment env;
    private boolean done;
    public void init(ProcessingEnvironment e) { this.env = e; }
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver() && !this.done) {
            this.done = true;
            Filer f = this.env.getFiler();
            // `process` no puede declarar `throws IOException`: la firma de `Processor` no la tiene y
            // un override no puede ensanchar las chequeadas (JLS 8.4.8.3). Se atrapa, que es lo que
            // hace cualquier procesador real.
            try {
                JavaFileObject jfo = f.createSourceFile("FooGreeting");
                StringWriter w = (StringWriter) jfo.openWriter();
                w.write("public class FooGreeting { public static int value() { return 42; } }");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
"#;
        compile_into(&proc, processor, &[kaji.clone()]);

        // 3) Correr el round loop. boot = [lib (shadow), KajiLibrary, boot]; app = [processorpath].
        let boot = vec![lib.clone(), kaji.clone(), PathBuf::from("boot")];
        let app = vec![proc.clone()];
        let outcome = run_processors(&["GenProcessor".to_string()], boot, app);

        assert!(outcome.error.is_none(), "el bucle no debería fallar: {:?}", outcome.error);
        // Tres rondas: normal-genera, normal-vacía (el flag cortó la generación), y la final.
        assert_eq!(outcome.rounds, 3, "generar, converger y la final: {}", outcome.rounds);
        assert_eq!(outcome.process_calls, 3, "una llamada a process por ronda");
        // El Filer entregó el fuente de vuelta a Rust.
        assert_eq!(outcome.generated_sources.len(), 1, "un solo fuente generado");
        assert_eq!(outcome.generated_sources[0].0, "FooGreeting");
        assert!(
            outcome.generated_sources[0].1.contains("static int value"),
            "el texto recuperado: {:?}",
            outcome.generated_sources[0].1
        );
        // Y se **recompiló**: el `.class` de FooGreeting quedó disponible.
        let foo = outcome
            .generated_classes
            .iter()
            .find(|(name, _)| name == "FooGreeting")
            .expect("FooGreeting.class generado");

        // 4) El `.class` generado corre y `value()` da 42 — la re-entrada llegó hasta el codegen.
        let out = base.join("out");
        std::fs::create_dir_all(&out).unwrap();
        std::fs::write(out.join("FooGreeting.class"), &foo.1).unwrap();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![out.clone()]);
        let class = ClassFile::from_path(out.join("FooGreeting.class").to_str().unwrap())
            .expect("el .class generado parsea");
        let name = class.class_name(class.this_class).unwrap().to_string();
        ms.add(name.clone(), class);
        let value = ms.resolve_method(&name, "value", "()I").expect("FooGreeting.value");
        let max_locals = ms.max_locals(value);
        let result = execute(ms, Frame::new(value, max_locals, Vec::new()));

        std::fs::remove_dir_all(&base).ok();
        assert_eq!(result, Some(Value::Int(42)), "FooGreeting.value() debería dar 42");
    }

    /// Descubrimiento por `META-INF/services`: sin `-processor` explícito, el round loop lee el
    /// recurso del processorpath y corre lo que nombra. Reusa el `HelloProcessor` (sólo imprime), así
    /// el foco es el **descubrimiento**, no la generación.
    #[test]
    fn processors_are_discovered_via_meta_inf_services() {
        let source = r#"
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.AptTrace;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;
@SupportedAnnotationTypes("*")
public class SvcProcessor extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        AptTrace.trace("SvcProcessor ran");
        return false;
    }
}
"#;
        let kaji = PathBuf::from("KajiLibrary");
        let n = gen_seq();
        let dir = std::env::temp_dir().join(format!("apt_svc_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        compile_into(&dir, source, &[kaji.clone()]);
        // El archivo de servicios: FQN en notación con puntos, con un comentario y una línea en blanco
        // para ejercitar el parseo tolerante.
        let svc = dir.join("META-INF/services/javax.annotation.processing.Processor");
        std::fs::create_dir_all(svc.parent().unwrap()).unwrap();
        std::fs::write(&svc, "# procesadores\nSvcProcessor\n\n").unwrap();

        // Sin `-processor` explícito: `run_processors` los descubre del processorpath (`app`).
        let boot = vec![kaji.clone(), PathBuf::from("boot")];
        let outcome = run_processors(&[], boot, vec![dir.clone()]);

        std::fs::remove_dir_all(&dir).ok();
        assert!(outcome.error.is_none(), "no debería fallar: {:?}", outcome.error);
        assert_eq!(outcome.rounds, 2, "normal + final (no genera nada)");
        assert!(outcome.console.contains("SvcProcessor ran"), "corrió: {:?}", outcome.console);
    }

    /// [`enter_multi`] entra **dos** unidades en **una** tabla y resuelve una referencia **cruzada**
    /// entre ellas (una clase de la unidad B extiende una de la A). Es el front-end multi-unidad que
    /// la re-entrada del round loop necesita.
    #[test]
    fn enter_multi_resolves_a_cross_unit_reference() {
        use crate::javac::symbol::Resolved;
        let a = crate::javac::parse("public class Base { int a() { return 1; } }").expect("A parsea");
        let b = crate::javac::parse("public class Sub extends Base {}").expect("B parsea");
        let (table, errors) = crate::javac::enter::enter_multi(&[a, b], &[]);
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let base = table.class("Base").expect("símbolo de Base en la tabla acumulada");
        let sub = table.class("Sub").expect("símbolo de Sub en la tabla acumulada");
        // El `extends Base` de Sub resolvió al símbolo de Base de la **otra** unidad.
        match table.resolved(sub) {
            Some(Resolved::Class { super_type: Some(crate::javac::symbol::RType::Class(c)), .. }) => {
                assert_eq!(*c, base, "Sub extends Base resuelto entre unidades");
            }
            other => panic!("Sub debería extender Base: {other:?}"),
        }
    }

    // ---- ayudantes de las capas 2-5 ------------------------------------------------------------

    /// Una JVM sobre KajiLibrary con la tabla `table` atada como contexto APT, parada en un método
    /// cualquiera (no corre `main`): las llamadas virtuales del test manejan native/intrínsecos sin
    /// necesitar un frame propio. Comparte la tabla por `Arc` (no la consume).
    fn jvm_with_apt(table: &Arc<SymbolTable>) -> JVM {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("KajiLibrary")], vec![]);
        let park = metaspace
            .resolve_method("java/lang/Object", "hashCode", "()I")
            .expect("un método donde estacionar el frame de entrada");
        let max_locals = metaspace.max_locals(park);
        let mut jvm = JVM::new(metaspace, Frame::new(park, max_locals, Vec::new()));
        jvm.set_apt(AptContext::new(Arc::clone(table)));
        jvm
    }

    /// El texto de un `Name` (`SymName`) del heap: su `toString()` (contrato `CharSequence`).
    fn name_text(jvm: &mut JVM, name_obj: usize) -> String {
        let text = jvm
            .exec()
            .call_virtual(name_obj, "toString", "()Ljava/lang/String;", Vec::new())
            .expect("Name.toString devuelve un String");
        match text {
            Value::Reference(s) => strings::read(jvm.exec().heap(), s),
            other => panic!("toString devuelve una referencia, no {other:?}"),
        }
    }

    /// El nombre simple de un `SymElement` del heap (llama a `getSimpleName().toString()`).
    fn simple_name(jvm: &mut JVM, element: usize) -> String {
        let name = jvm
            .exec()
            .call_virtual(element, "getSimpleName", "()Ljavax/lang/model/element/Name;", Vec::new())
            .expect("getSimpleName");
        match name {
            Value::Reference(obj) => name_text(jvm, obj),
            other => panic!("getSimpleName no devolvió una referencia: {other:?}"),
        }
    }

    /// El nombre de la constante `ElementKind` que `getKind()` devuelve para `element` (llama a
    /// `getKind()` —intrínseco— y luego `Enum.name()` sobre la constante).
    fn kind_name(jvm: &mut JVM, element: usize) -> String {
        let kind = jvm
            .exec()
            .call_virtual(element, "getKind", "()Ljavax/lang/model/element/ElementKind;", Vec::new())
            .expect("getKind devuelve una constante");
        let Value::Reference(constant) = kind else {
            panic!("getKind no devolvió una referencia: {kind:?}");
        };
        let named = jvm
            .exec()
            .call_virtual(constant, "name", "()Ljava/lang/String;", Vec::new())
            .expect("Enum.name");
        match named {
            Value::Reference(s) => strings::read(jvm.exec().heap(), s),
            other => panic!("name() no devolvió una referencia: {other:?}"),
        }
    }

    /// **Capa 3** — `getQualifiedName()` de un tipo **anidado** transforma el `$` del binary name
    /// en `.`: `class Outer { class Inner {} }` → `Inner.getQualifiedName() == "Outer.Inner"`,
    /// mientras que su nombre simple sigue siendo `"Inner"`.
    #[test]
    fn get_qualified_name_of_a_nested_type_dots_the_binary() {
        let (_unit, table, errors) =
            crate::javac::analyze("class Outer { class Inner {} }").expect("analyze");
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let table = Arc::new(table);
        let inner = table.class("Outer.Inner").expect("símbolo de Outer.Inner");
        let mut jvm = jvm_with_apt(&table);

        let element = jvm.exec().apt_element_for(inner);
        let qualified = jvm
            .exec()
            .call_virtual(element, "getQualifiedName", "()Ljavax/lang/model/element/Name;", Vec::new())
            .expect("getQualifiedName");
        let Value::Reference(qname) = qualified else {
            panic!("getQualifiedName no devolvió una referencia: {qualified:?}");
        };
        assert_eq!(name_text(&mut jvm, qname), "Outer.Inner", "FQN con el `$` vuelto `.`");
        assert_eq!(simple_name(&mut jvm, element), "Inner", "el nombre simple no cambia");
    }

    /// **Capa 5** — `getEnclosedElements()` de una clase con miembros devuelve una `List` con un
    /// elemento por miembro (`members_of`); cada uno reificado con su propio nombre. Se verifica que
    /// la lista contenga el campo `x` y el método `m`.
    #[test]
    fn get_enclosed_elements_lists_the_members() {
        let (_unit, table, errors) =
            crate::javac::analyze("class Foo { int x; void m() {} }").expect("analyze");
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let table = Arc::new(table);
        let foo = table.class("Foo").expect("símbolo de Foo");
        let mut jvm = jvm_with_apt(&table);

        let element = jvm.exec().apt_element_for(foo);
        let list = jvm
            .exec()
            .call_virtual(element, "getEnclosedElements", "()Ljava/util/List;", Vec::new())
            .expect("getEnclosedElements");
        let Value::Reference(list) = list else {
            panic!("getEnclosedElements no devolvió una referencia: {list:?}");
        };

        // Recorrer la `List` (size/get) y juntar los nombres simples de sus elementos.
        let size = match jvm.exec().call_virtual(list, "size", "()I", Vec::new()) {
            Some(Value::Int(n)) => n,
            other => panic!("List.size no devolvió un int: {other:?}"),
        };
        let mut names = Vec::new();
        for i in 0..size {
            let child = jvm
                .exec()
                .call_virtual(list, "get", "(I)Ljava/lang/Object;", vec![Value::Int(i)])
                .expect("List.get");
            let Value::Reference(child) = child else { panic!("get no devolvió una referencia") };
            names.push(simple_name(&mut jvm, child));
        }
        assert!(names.iter().any(|n| n == "x"), "debería listar el campo x: {names:?}");
        assert!(names.iter().any(|n| n == "m"), "debería listar el método m: {names:?}");
    }

    /// **Capa 4** — `getKind()` mapea `SymbolKind`/`TypeKind` a la constante de `ElementKind`
    /// correcta, corriendo antes el `<clinit>` del enum. Se prueban una clase, una interfaz y un
    /// enum en una misma unidad.
    #[test]
    fn get_kind_maps_class_interface_and_enum() {
        let source = "class Foo {} interface Bar {} enum Color { RED }";
        let (_unit, table, errors) = crate::javac::analyze(source).expect("analyze");
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let table = Arc::new(table);
        let (foo, bar, color) = (
            table.class("Foo").expect("Foo"),
            table.class("Bar").expect("Bar"),
            table.class("Color").expect("Color"),
        );
        let mut jvm = jvm_with_apt(&table);

        let foo_el = jvm.exec().apt_element_for(foo);
        assert_eq!(kind_name(&mut jvm, foo_el), "CLASS");
        let bar_el = jvm.exec().apt_element_for(bar);
        assert_eq!(kind_name(&mut jvm, bar_el), "INTERFACE");
        let color_el = jvm.exec().apt_element_for(color);
        assert_eq!(kind_name(&mut jvm, color_el), "ENUM");
    }

    /// **Capa 3** — `getEnclosingElement()` reifica el `Symbol.owner` con la **caché de identidad**:
    /// dos miembros de la misma clase devuelven el **mismo** objeto para su elemento envolvente, y
    /// ese objeto es el `SymElement` de la clase (`==` a `element_for` de la clase).
    #[test]
    fn get_enclosing_element_shares_identity() {
        let (_unit, table, errors) =
            crate::javac::analyze("class Foo { int a; int b; }").expect("analyze");
        assert!(
            errors.iter().all(|e| e.severity == crate::javac::Severity::Warning),
            "sin errores: {errors:?}"
        );
        let table = Arc::new(table);
        let foo = table.class("Foo").expect("símbolo de Foo");
        // Los símbolos de los campos a y b, hijos de Foo.
        let field = |name: &str| {
            table
                .members_of(foo)
                .into_iter()
                .find(|&id| table.symbol(id).name == name)
                .unwrap_or_else(|| panic!("campo {name} no encontrado"))
        };
        let (a, b) = (field("a"), field("b"));
        let mut jvm = jvm_with_apt(&table);

        let a_el = jvm.exec().apt_element_for(a);
        let b_el = jvm.exec().apt_element_for(b);
        let enclosing = |jvm: &mut JVM, el: usize| {
            match jvm
                .exec()
                .call_virtual(el, "getEnclosingElement", "()Ljavax/lang/model/element/Element;", Vec::new())
                .expect("getEnclosingElement")
            {
                Value::Reference(owner) => owner,
                other => panic!("getEnclosingElement no devolvió una referencia: {other:?}"),
            }
        };
        let owner_a = enclosing(&mut jvm, a_el);
        let owner_b = enclosing(&mut jvm, b_el);
        assert_eq!(owner_a, owner_b, "mismo dueño → mismo objeto (identidad)");

        // Y ese dueño es exactamente el `SymElement` de Foo (misma caché).
        let foo_el = jvm.exec().apt_element_for(foo);
        assert_eq!(owner_a, foo_el, "el envolvente es el elemento de la clase");
        assert_eq!(simple_name(&mut jvm, owner_a), "Foo");
    }
}
