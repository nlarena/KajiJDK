const fs = require('fs');

// =============================================================
//  Generador de informe PDF — Roadmap del JDK en Rust
//  Estilo basado en los informes de IAM (portada + TOC + tablas
//  + header/footer con banda de color). Render: Chrome --print-to-pdf.
// =============================================================

const FECHA = new Date().toLocaleDateString("es-AR", { day: "2-digit", month: "long", year: "numeric" });
const AUTOR = "Esteban Nicolás Larena";

// ---- inline markdown-ish: `code`, **bold**, *italic* ----
function esc(s) {
  if (s === undefined || s === null) return "";
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function inline(text) {
  let t = esc(text);
  t = t.replace(/`([^`]+)`/g, '<code>$1</code>');
  t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  t = t.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  return t;
}
// Badge de horizonte: B=Base, A=Avanzado, C=Cumbre
function badge(code) {
  const map = {
    B: ['Base', 'b-base'],
    A: ['Avanzado', 'b-adv'],
    C: ['Cumbre', 'b-sum'],
  };
  const [label, cls] = map[code];
  return `<span class="badge ${cls}">${label}</span>`;
}
function tableRows(rows) {
  return rows.map(r => '<tr>' + r.map(c => `<td>${inline(c)}</td>`).join('') + '</tr>').join('\n');
}
function table(headers, rows) {
  const head = '<tr>' + headers.map(h => `<th>${inline(h)}</th>`).join('') + '</tr>';
  return `<table><thead>${head}</thead><tbody>${tableRows(rows)}</tbody></table>`;
}
// Tabla de hitos: cada hito = { id, name, hz, exito, items:[{hz,txt}] }
function hito(h) {
  const items = h.items.map(it => `<li>${badge(it.hz)} ${inline(it.txt)}</li>`).join('\n');
  return `
  <div class="hito">
    <h3><span class="hito-id">${esc(h.id)}</span> ${esc(h.name)} ${badge(h.hz)}</h3>
    <ul class="hito-items">${items}</ul>
    <p class="exito"><strong>✓ Éxito:</strong> ${inline(h.exito)}</p>
  </div>`;
}

// =============================================================
//  CONTENIDO
// =============================================================

const TBL_PILARES = [
  ["**JVM** (el *runtime*)", "Carga y **ejecuta** bytecode. Parser de `.class` → intérprete → objetos → heap → GC.", "Rust"],
  ["**Compilador** (`javac`)", "Convierte `.java` en bytecode `.class`: lexer → AST → análisis semántico → emisión.", "Rust"],
  ["**Bibliotecas** (`java.base`)", "`Object`, `String`, `System`, colecciones… escritas *en Java*, compiladas por el compilador propio.", "Java"],
];

const FASE_A = [
  { id: "A0", name: "Parsear el .class (≡ javap)  ✅ núcleo logrado", hz: "B",
    exito: "**logrado** — el volcado coincide **byte a byte** con `javap -v` (y brief) sobre 12 fixtures.",
    items: [
      { hz: "B", txt: "Lector de bytes (cursor big-endian) — el `Reader` (`u1`/`u2`/`u4`)" },
      { hz: "B", txt: "Constant pool (17 clases de entrada; 1-indexed, Long/Double = 2 slots)" },
      { hz: "B", txt: "Header: magic, versiones, flags, this/super/interfaces" },
      { hz: "B", txt: "fields, methods, attributes: `Code`, `LineNumberTable`, `SourceFile`, **`StackMapTable` (los 7 frame types)**" },
      { hz: "B", txt: "Desensamblado de bytecode (tabla de opcodes completa) con comentarios `// …` resueltos" },
      { hz: "B", txt: "Volcado `javap` brief y `-v` byte-idéntico; flags de visibilidad (`-public`/`-protected`/`-package`/`-p`)" },
      { hz: "A", txt: "*Pendiente (no esencial):* `Signature`, `BootstrapMethods`, `InnerClasses`, `Exceptions`, `ConstantValue`, anotaciones, `Record`, `NestHost/Members`, `LocalVariableTable`" },
    ] },
  { id: "A1", name: "Intérprete mínimo  ✅ logrado", hz: "B",
    exito: "**logrado** — ejecutar un método que sume dos enteros (`Add.add`).",
    items: [
      { hz: "B", txt: "*Frame*: pila de operandos + variables locales" },
      { hz: "B", txt: "Contador de programa (PC) y *loop* de despacho de opcodes" },
      { hz: "B", txt: "Opcodes: `iconst`, `iload`, `istore`, `iadd`, `ireturn`" },
      { hz: "B", txt: "Parseo de descriptores de método (`(II)I`)" },
    ] },
  { id: "A2", name: "Control de flujo y métodos  ✅ logrado", hz: "B",
    exito: "**logrado** — corren `factorial` recursivo (120) y `fibonacci` (55).",
    items: [
      { hz: "B", txt: "Saltos: `if_icmpgt`, `goto`, comparaciones" },
      { hz: "B", txt: "*Method area* (metadatos de clases cargadas)" },
      { hz: "B", txt: "`invokestatic` + pila de frames (llamadas anidadas)" },
    ] },
  { id: "A3", name: "Objetos y heap  ✅ logrado", hz: "B",
    exito: "**logrado** — herencia (`Dog extends Animal`) y dispatch dinámico verificados: `a.sound()` sobre una referencia `Animal`/`Speaker` corre `Dog.sound`.",
    items: [
      { hz: "B", txt: "Heap + allocator simple; arena de bytes (`Vec<u8>`) + `malloc` bump" },
      { hz: "B", txt: "*Layout* de objetos (campos, con herencia) y de clases (con *vtable*)" },
      { hz: "B", txt: "`new`, `getfield`/`putfield`" },
      { hz: "B", txt: "`invokevirtual`/`invokespecial`/`invokeinterface` + dispatch dinámico (vtable + itable)" },
      { hz: "B", txt: "Arrays (objetos y primitivos int-category, con ancho fiel)" },
    ] },
  { id: "A4", name: "Robustez del runtime  ✅ logrado", hz: "B",
    exito: "**logrado** — un `try`/`catch` atrapa una `Boom` (con unwinding entre frames y `finally`), y `Base`/`Derived` inicializan en orden super-primero.",
    items: [
      { hz: "B", txt: "Excepciones: `athrow` + tablas de excepción + *stack unwinding*; `finally`; excepciones **implícitas** (NPE, índice, cast, tamaño negativo)" },
      { hz: "B", txt: "*Linking*: resolución bajo demanda + **verificador** (type-checking con *StackMapTable*); errores de linkage (`NoClassDefFound`/`NoSuchMethod`)" },
      { hz: "B", txt: "Inicialización de clase (`<clinit>`, perezosa, super-primero)" },
      { hz: "B", txt: "Jerarquía de class loaders (bootstrap/app + delegación)" },
    ] },
  { id: "A5", name: "Nativos + GC  ✅ logrado", hz: "A",
    exito: "**logrado** — `System.out.println` imprime de verdad y el GC recolecta (y **compacta**) basura.",
    items: [
      { hz: "A", txt: "Puente a métodos nativos (I/O real) — `System.out.println` imprime de verdad" },
      { hz: "A", txt: "*Intrinsics* — `getClass`, `hashCode`, `Math`, `arraycopy`, `String`/`Class`…" },
      { hz: "A", txt: "GC mark & sweep — y más: **compactante**, free list con *coalescing*, política de fragmentación, 4 disparadores sobre *safepoint*, y **marcado transitivo** correcto" },
    ] },
  { id: "A6", name: "Cumbre del runtime  🚧 en progreso", hz: "C",
    exito: "**parcial** — **verificador JVMS-estricto**, **GC generacional**, el **set de opcodes completo** (`invokedynamic` incluido) e **hilos de SO con paralelismo real ensanchado** (GIL removido; cómputo + lecturas + asignación lock-free en `JVM_THREADS=os`, con el `unsafe` Miri-verificado) **y el JMM relajado** (campos no-volatile lock-free con `Relaxed`, `volatile` con `Acquire`/`Release`, Eden atómico) logrados; falta locks por-estructura finos y/o el JIT.",
    items: [
      { hz: "C", txt: "**✅ Verificador de bytecode JVMS-estricto** — verifica **con o sin `StackMapTable`** (inferencia por punto fijo como fallback, y *cross-check* de la tabla contra la inferencia); gate **estructural** (§4.9: targets, no caerse del final, `jsr`/`ret`, tabla de excepciones); tipos de locales estrictos + cota de `max_stack`; reglas de **objetos sin inicializar** (`<init>` una sola vez); **handlers de excepción** tipados; y **acceso/linkage** (regla `protected`, `count` de `invokeinterface`)" },
      { hz: "C", txt: "**✅ Sistema de tipos completo** — `int`/`long`/`double`/`float` ejecutados *y* verificados: cómputo, conversiones, comparaciones, división con excepción, categoría-2 (params/campos/estáticos/arrays/frames), y el **lattice de referencias** (covarianza de arrays + `join`/LUB)" },
      { hz: "C", txt: "**✅ GC generacional** — young (Eden + 2 *survivors*) con colector por **copia** (minor: evacúa/promueve por edad, estilo Cheney con *forwarding*), **write barrier + remembered set** para las raíces `old→young`, y Old con **mark-sweep-compact**; *minor* disparado al llenarse Eden, *major*/full por occupancy o explícito" },
      { hz: "C", txt: "**✅ Referencias débiles** (`java.lang.ref`) — `WeakReference` + `ReferenceQueue`: el major trata `referent` como débil y, al morir el referente, lo limpia (`get()→null`) y **encola** la referencia. Base para `PhantomReference`/`Cleaner` (post-mortem)" },
      { hz: "C", txt: "**Hilos / concurrencia** (*en progreso*) — objetivo: **multithread profesional**, por fases. **✅ Fase 0 — green threads**: `java.lang.Thread` + `start`/`join`/`sleep`, scheduler cooperativo round-robin en el safepoint, varios call stacks por-thread, GC sobre las raíces de **todos** los threads, terminación; verificado (`Threads.run → 100`) y visible en el step-visualizer. **✅ Monitores**: `synchronized` de **bloques y métodos** (`ACC_SYNCHRONIZED`, sin opcode — el VM toma/suelta el monitor al entrar/salir del frame), reentrancia, señalización `wait`/`notify`/`notifyAll` sobre *wait-set*/*blocked-set*, `IllegalMonitorStateException`, `wait(timeout)` y monitor ***GC-safe*** (las claves se remapean por el *forward* del GC, así que se compacta con monitores tomados). **✅ Substrato de SO + GIL (E1+E2)**: `JVM_THREADS=os-gil` corre cada `Thread.start()` en un `std::thread` real tras un **GIL** (`Arc<Mutex<SharedVm>>`, un opcode por lock), con `park`/`unpark` reales y `sleep` en tiempo real — correcto pero **serializado** (referencia/oráculo). **✅ API de `Thread` (H1)**: `currentThread`/`yield`/nombre/id/`isAlive`, `getState()` con los seis estados de `Thread.State`, `new Thread(runnable)`, `start` dos veces → `IllegalThreadStateException`, e `interrupt()` que despierta `sleep`/`join`/`wait`. **✅ Sacar el GIL (E3/H3 — paralelismo real, subconjunto conservador)**: `JVM_THREADS=os` corre los opcodes **frame-local** (aritmética int/stack/ramas) **lock-free en paralelo** sobre un `RunningCtx` por-hilo con code cache; solo `SharedVm` se lockea; el GC que mueve objetos frena a todos con un **safepoint stop-the-world cooperativo**. El `Arc<Mutex<JVM>>` de toda-la-VM desapareció. **✅ Ensanchado**: (W1) frame-local completo (int/long/float/double, shifts, conversiones, refs, ramas); (W3) lecturas compartidas concurrentes bajo `RwLock` (`getfield`/`arraylength`/array-loads); (W2) **asignación lock-free** (`new`/`newarray`, Eden en un arena `UnsafeCell` **verificado con Miri**). **✅ JMM (H4 — modelo de memoria relajado)**: campos no-volatile **lock-free** (`Relaxed` atómico), `volatile` con `Acquire`/`Release` (`AtomicU64` sin *tearing* para `long`/`double`), Eden sobre un substrato atómico 8-alineado **Miri-verificado**; test de publicación `volatile` que coincide `green≡os-gil≡os`. **✅ CAS (H5)**: `compareAndSwap` intrínseco (`cas_u32`/`cas_u64` sobre el `AtomicRegion`) + `AtomicInteger`/`AtomicLong`/`AtomicReference` (la CAS de referencia pasa por el write barrier). **✅ Núcleo + primeras utilidades de `java.util.concurrent` (H6)**: **AQS** (`AbstractQueuedSynchronizer`) en modo **exclusivo y compartido**, con `ReentrantLock`/`Condition`, `ReentrantReadWriteLock`, `Semaphore`, `CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue`/`LinkedBlockingQueue`/`PriorityBlockingQueue`/`DelayQueue`, el **framework `Executor`** (`ThreadPoolExecutor` + `ScheduledThreadPoolExecutor` + `submit`/`Future`/`FutureTask`/`Callable`), un **`ConcurrentHashMap`** (encadenamiento + *lock striping*) y un **`CompletableFuture`** (`supplyAsync`/`thenApply`/`thenCompose`, completación excepcional con `exceptionally`, y `thenCombine` que fusiona dos futuros con un `BiFunction`) —todo en Java sobre AQS (o el monitor), validado `green≡os-gil≡os`; motivaron `Object.equals`, `java.util.function`/`Objects`, `Comparator`/`Comparable`/`Delayed` y `System.nanoTime`—. **✅ Periféricos de `Thread`**: **`ThreadLocal`** (aislamiento por-hilo real — lista de asociación colgada del `Thread` actual, keyed por identidad GC-safe; verificado con 4 hilos OS), **prioridad** (get/set + rango `[1,10]`) y **daemon** (atributo + regla de ciclo de vida). **⬜ Falta**: el **volumen** restante de j.u.c (deques, *resize*/lecturas lock-free del `ConcurrentHashMap`), locks por-estructura finos y `UncaughtExceptionHandler`; y un **bug residual** conocido — una referencia *stale* puede alcanzar un frame bajo **GC intenso concurrente** en `JVM_THREADS=os` (guard parcial landeado; próximo paso ThreadSanitizer; `green`/`os-gil`, el oráculo, **no** afectados). El paralelismo de cómputo del LLM vive en **kernels off-heap** (rayon/CUDA), no en el heap Java. *La hoja de ruta completa (JMM, CAS, `java.util.concurrent`) tiene su propia sección («Concurrencia»).*" },
      { hz: "C", txt: "**✅ Cobertura del set de opcodes — 199/199 alcanzables: completo** — cerrados `nop` (0x00), `goto_w` (0xc8), el prefijo **`wide`** (0xc4: índice de local de 16 bits, la única forma de direccionar los slots pasados el 255; `wide iinc` mide 6 bytes, el resto 4 — salió barato porque los handlers de locales ya reciben `slot: usize` y nunca se enteran del ancho, así que ensanchar es puro *decoding*) y **`multianewarray`** (0xc5: aloca recursivamente **sólo** los `dimensions` niveles indicados —`new int[3][]` deja los internos en `null`—, validando todos los conteos *antes* de alocar nada; los niveles superiores guardan referencias y sólo el más interno usa el ancho real del elemento, y las referencias hijo pasan por `store_reference` para que el *remembered set* vea los punteros `old→young`. Hubo que **modelarlo también en el verificador**, sin lo cual la clase corría con el verificador claudicando en silencio). Los 3 de `jsr`/`ret`/`jsr_w` quedan **excluidos por diseño** —**JVMS §4.9.1** los prohíbe en class files de versión 50.0+ (Java 6 en adelante)— con la postura **leer sí, ejecutar no**: el desensamblador los soporta completo (requisito de A0, que se mide contra `javap` sobre `java.base`) y el gate estructural del verificador los rechaza. Nota de diseño: `subrutinas-jsr-ret.md`. El número honesto es entonces **199 de 199 alcanzables: completo**" },
      { hz: "C", txt: "**✅ `invokedynamic`** (0xba) — **no era un opcode, era un subsistema**, y corre: **5 de las 6 fábricas** que emite `javac`. **`StringConcatFactory`** (todo `\"a\" + b` desde Java 9), **`SwitchBootstraps.typeSwitch`** (`switch` sobre patrones de tipo *y* de enum), **`ObjectMethods`** (`equals`/`hashCode`/`toString` de un `record`, los tres desde una sola entrada de `BootstrapMethods`, distinguidos por el nombre del call site), **`LambdaMetafactory.metafactory`** (lambdas y method references) y **`ConstantBootstraps.invoke`** (constantes dinámicas). La sexta, `altMetafactory`, necesita serialización y queda **fuera de alcance**. **`LambdaMetafactory` genera una clase real** al vuelo (con el escritor de `.class`): implementa la interfaz funcional, con campos para las capturas y un método SAM que reenvía al handle — igual que el JDK (que spinnea con ASM), ya sin el atajo del objeto sintético. Y con el **modelo de objetos de `java.lang.invoke`** cerrado (abajo), `ConstantBootstraps.invoke` **se movió a Java** (es sólo `handle.invokeWithArguments(args)`); los otros *bootstrap methods* siguen como intrínsecos, con `java.lang.constant` en Java. Ruta completa, correcciones y mediciones: **`invokedynamic-ruta.md`**" },
      { hz: "A", txt: "**✅ `ldc` de literales de clase** (`Foo.class`, `int[].class`) — empuja el mirror `Class<…>`, cacheado por Class ID (así `Foo.class == Foo.class`) y **sin inicializar** la clase: un literal no es *uso activo* (§5.5). Los de array reusan el mirror sintético que arma `anewarray`, ya que una clase de array no tiene `.class` que cargar" },
      { hz: "C", txt: "**✅ La VM puede invocar Java** (`call_java`) — empuja un frame propio y lo corre con un bucle de `run_one` anidado, devolviendo el resultado. Resultó ser el **caso general de lo que la inicialización de clases ya hacía**: `<clinit>` era la variante sin argumentos ni resultado. Importa porque **los intrínsecos dejan de ser terminales**: un nativo que sólo puede calcular y devolver tiene que reimplementar lo que necesite de la biblioteca. Con esto, `String.valueOf(Object)` llama al `toString()` del objeto, un `record` pregunta el `equals`/`hashCode` de sus componentes, y un condy ejecuta su bootstrap" },
      { hz: "C", txt: "**✅ Modelo de objetos de `java.lang.invoke`** (`MethodHandle`/`MethodType`/`Lookup`) — **cierra 0xba**. Las clases viven en **Java** (`bootstrap/java/lang/invoke/`), con `MethodHandle.invoke`/`invokeWithArguments` **nativos** — polimorfia de firma (§2.9.3), interceptados en `invokevirtual` *antes* de la resolución normal: el único intrínseco que de verdad corresponde. **El premio se cobró:** `ConstantBootstraps.invoke` es ahora **Java** (`return handle.invokeWithArguments(args)`), lo que exigió hacer el **cache de condy raíz de GC** (se remapea con el *forward* del colector). `ldc` de `MethodHandle`/`MethodType` —que `javac` **nunca** emite— se prueba con **class files hechos a mano por el escritor de `.class`** (su estreno). Kinds: static/virtual/special/**constructor** + **mirrors de primitivos** (`int.class` → `Integer.TYPE`, sin boxing). Y **`LambdaMetafactory` genera clases reales** al vuelo (vía el escritor de `.class`), implementando la interfaz funcional y reenviando al handle — como el JDK, ya sin el atajo del objeto sintético" },
      { hz: "C", txt: "JIT (bytecode → código nativo)" },
    ] },
  { id: "A7", name: "Conformidad JVMS  ✅🟡 auditoría 2026-08-12 — 8/8 accionables hechos", hz: "C",
    exito: "**los 8 hallazgos accionables ARREGLADOS** (2026-08-12) — auditoría del runtime contra la JVM Specification y los JSR estructurales (detalle: `holdon.md`): default methods, uncaught exceptions, ArrayStoreException, StackOverflowError, OutOfMemoryError, autoboxing, clone y Class.getName, cada uno validado por el oráculo (`green≡os-gil≡os`). De paso cayeron 2 bugs latentes de mirrors/naming de arrays y apareció un 2º reproducer del heisenbug os-parallel. Quedan los menores (#9-#13) según demanda.",
    items: [
      { hz: "C", txt: "**✅ Default methods de interfaces (JSR 335, Java 8)** — estaba **roto** (`NoSuchMethodError`: la vtable heredaba sólo de la superclase). Ahora fusiona los defaults de las superinterfaces (BFS desde las directas = regla *maximally-specific* para todo lo que emite javac; la clase siempre gana; un método abstracto —sin `Code`— nunca toma slot). Cubre `invokeinterface` **e** `invokevirtual`. Los static de interface ya funcionaban. Test → 115 (`green≡os-gil≡os`)" },
      { hz: "C", txt: "**✅ `Throwable` completo** — `getMessage`/`toString` (nativo: lee el nombre de clase runtime) + **stack traces**: la VM captura la traza (`\\tat pkg.Class.method`, innermost-first) en el *unwind* —punto único de faults implícitos y `throw`— y la guarda en el objeto (interning GC-safe); `printStackTrace()` la imprime. Constructores `(String)` en toda la jerarquía" },
      { hz: "C", txt: "**✅ `ExceptionInInitializerError`** (JVMS §5.5) — un `<clinit>` que lanza **propagaba… nada**: se tragaba el fallo y el `getstatic` devolvía 0. Ahora propaga al código que disparó la init (un *floor* de unwinding por `call_java` + `pending_exception`), envuelve si no es `Error`, y deja la clase **erroneous** → `NoClassDefFoundError` al reuso" },
      { hz: "C", txt: "**✅ Uncaught exception** (§2.10) — estaba **roto**: una excepción sin catch hacía `panic!` de **toda la VM**. Ahora imprime el formato exacto del JDK (`Exception in thread \"Thread-1\" java.lang.RuntimeException: …` + la traza capturada en el throw) y termina **solo ese thread** — los joiners despiertan, `main` sigue; si es `main`, termina el programa. Test → 42 (`green≡os-gil≡os`). Falta la API `setUncaughtExceptionHandler`" },
      { hz: "C", txt: "**✅ `ArrayStoreException`** (§6.5) — `aastore` hace el chequeo dinámico (clase runtime del valor vs tipo de elemento, `is_subtype` con covarianza; null siempre válido). Test → 42. **Bonus: 2 bugs latentes expuestos y arreglados** — los mirrors de clases array se alocaban en **Eden** (el minor GC los movía → índice/headers colgados → ASE espurio en stores válidos; ahora **Old-pinned** como todo mirror) y `anewarray` nombraba `[L[J;` en vez de `[[J` (intérprete + verificador). Regresión → 64" },
      { hz: "C", txt: "**✅ `StackOverflowError`** (§6.3) — `MAX_FRAMES = 2000` en `push_frame_locked` (el embudo de los 5 invokes), chequeado **antes** de adquirir el monitor (no filtra locks de `synchronized`); `call_java` cubierto vía `pending_exception`. Test → 42" },
      { hz: "C", txt: "**✅ `OutOfMemoryError`** (§6.3) — agotamiento **recuperable para las alocaciones de bytecode** (`new`/`newarray`/`anewarray`/`multianewarray` → `try_malloc` → throw); las internas del VM conservan el panic, documentado. Test → 42 (recursión rooteando 512 KiB/frame agota los 16 MiB reales)" },
      { hz: "C", txt: "**✅ Autoboxing / wrappers** (JLS §5.1.7) — los 8 wrappers reales: `Integer`/`Long`/`Short`/`Byte` con `valueOf` + **cache −128..127** (identidad `==` verificada: `100==100` true / `200==200` false), `Character` (0..127), `Boolean` (TRUE/FALSE canónicos), `Double`/`Float`. Test → 42. Colateral: destapó un **2º reproducer del heisenbug os-parallel** (single-thread ~50%, `Long.<clinit>` + Eden lleno → `ArithmeticException` espuria; `java/BxDbg{T,Y}.java`)" },
      { hz: "C", txt: "**✅ `Object.clone()` + `Cloneable`** (JLS §10.7) — copia shallow interceptada en `invokevirtual` e `invokespecial` (`super.clone()`), `Cloneable` → `CloneNotSupportedException`; **arrays incluidos** (`\"[I\".clone()` pre-vtable). Clon alocado en **Old** (una alocación Eden podría mover el fuente a mitad de copia); refs vía write barrier. Test → 42" },
      { hz: "C", txt: "**✅ `Class.getName()`/`getSimpleName()`** — nativos sobre el mirror, formato JDK completo (clases con puntos; arrays `[Ljava.lang.String;` / `String[]`/`int[]`). Test → 42 (incl. `String.class.getName()` vía ldc de Class)" },
      { hz: "B", txt: "**⬜ Menores**: `Soft`/`PhantomReference`/`Cleaner` (`finalize()` no se implementa por decisión), `System.exit`/`Runtime`, anotaciones runtime, `ThreadGroup`/`setUncaughtExceptionHandler`, regla fina de init de interfaces. Cubierto ya auditado: class file 199/199, verificador, JMM, indy 5/6, j.u.c núcleo, records/patterns. Fuera de alcance: módulos (JSR 376), NIO" },
    ] },
];

const FASE_B = [
  { id: "B0", name: "Lexer  ✅ logrado", hz: "B",
    exito: "**logrado** — tokeniza el juego **completo** de Java (las 115 `TokenKind` de JDK 25).",
    items: [
      { hz: "B", txt: "Scanner con **maximal munch**; comentarios; literales `int`/`long`/`float`/`double`/`char`/`String` (hex/bin/octal, text blocks)" },
      { hz: "B", txt: "`TokenKind` espejo **fiel** del enum de javac (115 constantes; las keywords contextuales `var`/`record`/`sealed`… van como identificador)" },
    ] },
  { id: "B1", name: "Parser  ✅ logrado (lenguaje completo)", hz: "B",
    exito: "**logrado** — AST por *recursive descent* de clases/miembros, sentencias y expresiones con toda la precedencia; **genéricos**, **lambdas**, **referencias a método**, **clases anónimas y locales**, **type witness** y **anotaciones** incluidos.",
    items: [
      { hz: "B", txt: "Gramática → AST. **Auditado contra el §14**: están **todas** las sentencias —incluida la **clase local** (§14.3)— bloque, declaración local (múltiple, `final`, y con declarador al estilo C `int y[]`), `;`, etiquetada, expresión, `if`, `assert`, `switch` (dos puntos y flecha, con patterns y guardas), `while`, `do`, `for` (básico/multi-init/infinito) y `for-each`, `break`/`continue` **con y sin etiqueta**, `return`, `throw`, `synchronized`, `try`/`catch`/`finally` con multi-catch y recursos, y `yield`. Más los **inicializadores** `static { }` y `{ }`, el **inicializador de array** `{1,2,3}` (§10.6) y el `instanceof` con **pattern**" },
      { hz: "B", txt: "*Precedence climbing* + *backtracking* (declaración local vs. expresión, *cast* vs. paréntesis, **cierre de genéricos** — parte `>>`/`>>>` con *undo log*)" },
      { hz: "B", txt: "**Genéricos**: argumentos de tipo (`List<String>`), *wildcards* (`? extends`/`? super`), parámetros con cotas (`<T extends A & B>`), el **diamante** `<>`" },
      { hz: "B", txt: "**Visualizador de AST** (árbol ASCII) + volcado de tokens en el binario `javac`; y **`javac-step`**: visor **paso a paso de las pasadas** (tokens → AST → tabla → chequeo)" },
      { hz: "B", txt: "**✅ Formas de expresión modernas** — cerradas esta iteración, todas parseadas y guardadas fielmente en el AST (compilarlas es de B2/B3, y hasta entonces la barrera del emisor las corta): **lambdas** (§15.27, con el desambiguador `(`-cast-vs-paréntesis-vs-parámetros por *lookahead* de la flecha), **referencias a método** (§15.13, incl. `T[]::new` por los corchetes vacíos), **clases anónimas** (§15.9.5, cuerpo de clase con nombre vacío), **clases locales** (§14.3) y el **type witness** (`Collections.<String>emptyList()`) —que además **se aplica**: fija los parámetros de tipo del método en vez de inferirlos, así un override deliberado incompatible ahora falla—" },
      { hz: "B", txt: "**✅ Anotaciones** (§9.7) — antes se **descartaban** (`skip_annotation`); ahora `modifiers()` las lee junto a los modificadores y las **cuelga** de la declaración (clase, método, campo, parámetro). Se parsean enteras: marcador, valor único, pares con nombre, arreglos y anotaciones anidadas. Metadata, no bytecode: emitir el atributo `RuntimeVisibleAnnotations` es de B3. **Lección de la auditoría:** los inicializadores `static { }`/`{ }` también se *parseaban y se descartaban* —el código desaparecía sin que nada fallara—; hoy se conservan. *Parsear y tirar* es peor que no parsear, porque no deja rastro" },
      { hz: "A", txt: "*Bordes menores que quedan:* las anotaciones sobre un **parámetro de tipo** (`<@Foo T>`) o una constante de `enum` (posiciones sin dónde colgarlas aún), las **anotaciones de tipo/uso** (§9.7.4, `List<@NonNull String>`) y el **type witness de constructor** (`new <T>Foo()`, rarísimo)" },
    ] },
  { id: "B2", name: "Análisis semántico  ✅ pasadas 1 y 2 + chequeos", hz: "B",
    exito: "**logrado (núcleo)** — acepta un programa bien tipado y rechaza uno con error de tipos/nombres, con **overload resolution** y **genéricos** reales; semántica **completa** de JDK 25 como norte, citada a la JLS 25.",
    items: [
      { hz: "B", txt: "**✅ Pasada 1 — Enter/MemberEnter**: tabla de símbolos completa — paquetes, tipos **anidados**, `enum`/`record`/`@interface`, **genéricos con cotas**; **class finder real** (lee `.class` del classpath, incl. el atributo **`Signature`** → jerarquía genérica del JDK); resolución + validación con errores **posicionados**; síntesis implícita; y el **grafo tipado persistido** como salida" },
      { hz: "B", txt: "**✅ Pasada 2 — Attribute**: resuelve nombres + tipa **los cuerpos**, **decorando el AST in situ** (tipo por nodo, *binding* local/campo/método, slot de cada local con categoría-2, **variables de patrón** de un `case T v` con su slot, y el **constructor resuelto** de cada `new` para que el codegen sepa qué descriptor emitir) — la salida que consume el codegen" },
      { hz: "A", txt: "**✅ Overload resolution** en **3 fases** (JLS §15.12.2: estricta → *boxing* → *varargs*), con *most specific* y detección de ambigüedad" },
      { hz: "A", txt: "**✅ Genéricos** (etapas A·B·C): subtipado con *containment* de wildcards (invariancia), *erasure*, sustitución del receptor (`List<String>.get` ⇒ `String`), `lub`/`glb`, e **inferencia** de los argumentos de tipo de una llamada (Cap. 18: reducción → incorporación → resolución)" },
      { hz: "A", txt: "**✅ Errores a nivel de expresión** — cada error lleva la línea:col del **nodo**, no del método (`int b = a + noSuchVar;` apunta a `noSuchVar`, no a la sentencia); documentado en `docs/pasada2-attribute.md` y `docs/semantica-jdk25.md`. **✅ Indulgencia con externos acotada** — antes, un miembro no hallado en un tipo del classpath se aceptaba en silencio. La raíz eran dos: no se cargaba la **cadena de supertipos** (un método heredado, `s.hashCode()` de `Object`, podía no aparecer), y los nombres del `.class` venían con puntos donde el *finder* esperaba barras (la superclase no cargaba). Cerrados los dos: se cargan los supertipos **transitivamente**, y la indulgencia quedó **acotada** — un **nombre inexistente** (`s.noExiste()`, `ArrayList.noExiste()`) ahora se reporta si la jerarquía está **completa**; sigue indulgente solo la **sobrecarga que no matchea** (nuestra resolución no cubre toda firma del JDK) y las jerarquías **incompletas** (classpath del JDK ausente → red de seguridad). Con esto **B2 no tiene deudas de rigor conocidas**" },
      { hz: "A", txt: "**✅ Chequeos semánticos** (esta iteración, en la pasada **Check** y en Attribute): **control de acceso** (§6.6 — `private`/`protected`/paquete sobre cada uso), **excepciones chequeadas** (§11.2 — toda checked que se lance tiene que estar capturada o en el `throws`; requirió **retener la cláusula `throws`**, que el parser descartaba, y guardarla en la tabla), **`this`/`super` en contexto estático** (§8.4.3.2), **reasignar un campo `final`** fuera de constructor/inicializador (§8.3.1.2) y el **acceso a tipo anidado cualificado** (`Outer.Inner.v()`, §6.5.5 — el único que *rechazaba código válido*). El chequeo de acceso y el de excepciones corren sobre el árbol **fuente**, antes del desugar, para no tropezar con el código sintético (el constructor del `enum` llama a `Enum.<init>`, que es `protected`)" },
    ] },
  { id: "B3", name: "Generación de bytecode  ✅ núcleo completo (con StackMapTable)", hz: "B",
    exito: "**logrado** — el `javac` propio compila objetos, bucles y excepciones, y el `.class` **corre en tu JVM** (`fib(10)=55`, `fact(5)=120`, `new M(10); m.add(5); m.get()` → 15) **y pasa el verificador JVMS-estricto**, que hace *cross-check* de nuestra `StackMapTable` contra su propia inferencia.",
    items: [
      { hz: "B", txt: "**✅ Pipeline completo en `compile()`** — las cinco pasadas en el orden de javac: `Enter → Attribute → Flow → Desugar → (re-Attribute) → Codegen`. **Flow va antes que Desugar** (sus errores apuntan al código fuente, no al bajado) y se **re-atribuye después** (las reescrituras dejan nodos sin decorar y el emisor necesita tipo/binding/slot). Un programa con errores semánticos **no emite** `.class`" },
      { hz: "B", txt: "**✅ Constant pool** con deduplicación + escritor de `.class` (v69) propio: `Utf8`/`Class`/`NameAndType`/`Methodref`/`Fieldref`/`Integer`/**`String`**/**`Long`**/**`Double`**/**`Float`**, con el hueco que exige la JVMS tras cada `Long`/`Double` (ocupan **dos** entradas); sección de **campos** y atributos `Code`/`SourceFile`" },
      { hz: "B", txt: "**✅ Tipos anchos y `String`** — `lconst`/`dconst`/`fconst`, `ldc2_w` para constantes de categoría 2, `ldc` para `String`, y la **promoción numérica binaria** (§5.6.2) con los 12 opcodes `x2y` (`longA + 1` inserta el `i2l`), con los desplazamientos como excepción (§15.19)" },
      { hz: "B", txt: "**✅ Objetos** — `new`+`dup`+`invokespecial <init>`, `getfield`/`putfield`, `getstatic`/`putstatic`, `invokevirtual` con receptor, `checkcast` y los estrechamientos `i2b`/`i2c`/`i2s`; **asignación** a local y a campo; y el `super()` implícito de todo constructor (§8.8.7), sin el cual el `this` queda sin inicializar y el verificador rechaza el `putfield`" },
      { hz: "B", txt: "**✅ Flujo de control** — etiquetas con parcheo de saltos; `if`/`else`, `while`, `do-while`, `for`, `break`, `continue`; comparaciones por categoría (`if_icmp*`, `if_acmp*`, `lcmp`/`fcmpl`/`dcmpl` + `if*`) y `&&`/`||` compilados como **saltos** (cortocircuito real, no un booleano calculado)" },
      { hz: "B", txt: "**✅ Excepciones** — `try`/`catch` con su tabla, `throw`, y el `finally` **duplicado** en la salida normal y en un handler *catch-all* que re-lanza (§14.20.2): la v69 ya no acepta `jsr`/`ret`, así que duplicar es la única vía. (Esto reubicó el *inlining* del `finally`, que teníamos mal anotado como tarea del **desugar**: es del emisor)" },
      { hz: "A", txt: "**✅ `StackMapTable`** (§4.7.4) — el frame de cada destino de salto, como el **merge** de los estados que llegan ahí (un slot que no coincide en todos los caminos pasa a `Top`). Siempre `full_frame`: legal en cualquier posición y evita las formas comprimidas. Los *handlers* llevan `stack = [E]` y hay que **forzarles** el frame, porque no los alcanza ningún salto. La pila de operandos se lleva **tipada**, no como simple altura: es lo que el frame tiene que declarar, y sin eso un destino alcanzado con algo ya empujado (`f(a, b > 0 ? 1 : 2)`) declaraba la pila vacía. Eso incluye el tipo **`uninitialized`** (tag 8), que identifica un objeto recién creado por el **offset de su `new`**; corrido su `<init>`, todas sus apariciones —pila y locales— pasan al tipo definitivo (§4.10.2.4). Validada con el *cross-check* del verificador propio" },
      { hz: "B", txt: "**✅ Barrera de seguridad** — `generate()` devuelve `Result`: una construcción no soportada **falla con posición** en vez de emitir un `.class` a medias. Destapó dos agujeros graves que el propio desugar venía alimentando en silencio —**`instanceof`** (que produce todo *pattern switch*) y la **creación de arrays** (que produce todo *varargs*)—, que a partir de ahí se cerraron. Si un `for-each`/`assert`/`yield` llega al emisor, el mensaje dice que es un **bug del desugar**, no una limitación" },
      { hz: "B", txt: "**✅ Arrays e `instanceof`** — `newarray`/`anewarray`, `arraylength`, las familias `Xaload`/`Xastore` (donde `byte`/`char`/`short` llevan **su propio** opcode aunque compartan la categoría `int`), inicializadores (`new int[]{4,5,6}`) y escritura de elementos. Con esto **compilan y corren** las llamadas **varargs** y los ***pattern switch***, que el desugar ya venía generando" },
      { hz: "B", txt: "**✅ Ternario, `switch`, saltos etiquetados y `synchronized`** — el `? :` con sus dos ramas **promovidas al tipo del todo** (§15.25); el `switch` como `tableswitch` o `lookupswitch` según la **densidad** de las etiquetas (la heurística de javac), con el relleno de alineación y los desplazamientos de 4 bytes, preservando el *fall-through* de la forma de dos puntos y cortándolo en la de flecha; `break`/`continue` **con y sin etiqueta** sobre una pila de sentencias «de las que se puede salir» —un `switch` interpuesto captura un `break` pero **no** un `continue` (§14.16)— más el **bloque etiquetado**; y `synchronized` con `monitorexit` en **las dos** salidas, la normal y un handler *catch-all* que re-lanza, con el monitor copiado a un local por el desugar para no reevaluarlo. De yapa, el **`switch` sobre `String`** pasó a compilar de punta a punta: el desugar ya lo bajaba a dos switches sobre `int`, pero ninguno de los dos se podía emitir" },
      { hz: "B", txt: "**✅ Emisión multi-clase** — `generate()` emite **un `.class` por tipo** (top-level y anidados, recursivo), cada uno con **su** nombre (`Multi`, `Multi$Nested`, `Otra`, el `E$1` del `switch`-enum). Era la última fuga silenciosa: antes se escribía **una sola clase** con el nombre del archivo, y un segundo tipo se perdía sin aviso. De paso, el **`switch` sobre `enum` ahora corre** de punta a punta (su `$SwitchMap` vive en `E$1`, que recién ahora llega al disco)" },
      { hz: "A", txt: "**⬜ Falta** — `invokedynamic` (lambdas, referencias a método, el `toString`/`equals`/`hashCode` de un `record`), las clases internas/anónimas/locales (clase sintética + captura), el atributo `RuntimeVisibleAnnotations` (el parser ya retiene las anotaciones) y el **plegado de constantes** para una `static final int` como etiqueta de `case`" },
    ] },
  { id: "B4", name: "Compilador robusto  ✅ núcleo cerrado", hz: "A",
    exito: "**logrado** — compila programas con sobrecarga, herencia y flujo no trivial. Los siete frentes están cubiertos; quedan **colas menores** documentadas en cada ítem (`@Override` sin override y `throws` en el chequeo, el *target type* en posición de argumento, el `enum` con constantes parametrizadas) — ninguna bloquea el criterio.",
    items: [
      { hz: "A", txt: "**✅ *Overload resolution*** (hecho en B2) **y chequeo de *override*** — pasada **Check** propia (el `Check.java` de javac: bien-formación de las *declaraciones*, no de los usos). Cubre las cuatro reglas de §8.4.8 sobre firmas *override-equivalentes* (mismo nombre y mismos parámetros **borrados**, §8.4.2): no sobrescribir un `final`, no cruzar la frontera `static`/instancia, no **reducir** la visibilidad, y **retorno covariante** —idéntico para primitivos, subtipo para referencias—. Más §8.1.1.1: una clase concreta tiene que implementar todo lo `abstract` que hereda, con las jerarquías que tocan un tipo **externo exentas** (de esos solo conocemos los miembros que aparecieron en alguna firma, así que una ausencia no prueba nada). Falta `@Override` sin override —el parser **saltea** las anotaciones, no llegan al AST— y el `throws` más ancho (`MethodDecl` no lleva la cláusula)" },
      { hz: "A", txt: "**✅ Inferencia** desde los argumentos **y desde el *target type*** — la atribución pasó a tener modo *checking* (`attrib_expr_to`), no solo síntesis: el tipo que el contexto espera baja a la expresión y es lo único que puede instanciar una variable que **no aparece** en los argumentos (`Caja<String> c = fabricar();`). Vale también para el **diamante** (§15.9.3), que antes pasaba por indulgente —`new Caja<>()` daba un parametrizado con cero argumentos, que se compara con cualquier cosa sin comparar nada— y ahora infiere de verdad, del target y del constructor. La **incorporación** (§18.3) arbitra entre las dos fuentes: un target que contradice a los argumentos se descarta, para que el error lo reporte el chequeo de asignación y no una inferencia inventada. Contextos cubiertos: los tres de asignación (§5.2 — inicializador, `return`, `=`). Falta el de **argumento de llamada**, que pide el algoritmo de dos fases (resolver la sobrecarga y recién entonces re-atribuir los argumentos *poly*)" },
      { hz: "A", txt: "**✅ Análisis de flujo (Flow)** — **asignación definitiva** de locales (Cap. 16, con los flujos *when-true/when-false* de `&&`/`||`/`!`), las reglas de variables **`final`** (conjunto *definitely unassigned*: no reasignar, asignación única) y **alcanzabilidad** (§14.21: sentencias inalcanzables), con `break`/`continue` **etiquetados** (§14.15/§14.16: la pila de contextos lleva la etiqueta a la que salta cada `break`)" },
      { hz: "A", txt: "**✅ Desugar** — sentencias (`for-each` → `for`/`while`+`Iterator`, `try`-recursos → `try/finally`+`close()` con la **excepción del `close()` suprimida** vía `addSuppressed` (§14.20.3.1), `assert` → `if(!$assertionsDisabled && !cond) throw` con su campo sintético y el `<clinit>` que lo calcula de `C.class.desiredAssertionStatus()` (§14.10), **`++`/`--` en descarte** → `x += 1` (§15.14.2), **switch-expresión** en cola → switch-sentencia que asigna (§15.28, con `yield` desde bloques/bucles vía `break` **etiquetado** sobre el switch generado), **`switch` sobre `String`** → dos switches sobre `int` con `hashCode`+`equals` (§14.11), **`switch` sobre `enum`** → switch sobre un `$SwitchMap` sintético (`int[]` indexado por `ordinal()`, alojado en una clase anidada **`C$1`** y poblado en un `<clinit>` con `try/catch NoSuchFieldError`, **fiel a javac** §14.11), **`switch` con *patterns*** (incl. **deconstrucción de records**, que extrae cada componente por su *accessor* y es recursiva) → cadena de `instanceof` en un bloque **etiquetado** (§14.11.1: cada brazo es un `if` **suelto** —no `else if`— para que una **guarda que falla** caiga al `case` siguiente; el binding se materializa con un cast, y un selector `null` sin `case null` lanza **NPE**)) y expresiones (concat de `String` → `StringBuilder`, `x op= y` → `x = (T)(x op y)`, **varargs** → array en el call site). Habilitado por una **síntesis a nivel clase** nueva: `Member::StaticInit` (los bloques `static { }` ya no se descartan), tabla de símbolos **mutable** en el desugar, el `enum extends Enum` implícito y el **binding de variables de patrón** en Attribute/Flow. Además **materializa los miembros implícitos de un `record`** (§8.10.3: campos, constructor canónico y *accessors*, respetando lo declarado a mano) — sus símbolos ya venían de `enter`, pero sin cuerpo en el AST el `.class` los referenciaba **sin declararlos**, y la deconstrucción llamaba a un método inexistente. El *inlining* del `finally` resultó ser del **codegen**, no de acá. Queda como asimetría que varias de estas reescrituras producen construcciones que el emisor **todavía no soporta** (`instanceof`, `new int[]`): desde que hay barrera, eso **falla** en vez de salir roto. Aparte (no son desugar): el boxing/erasure es *TransTypes* (dirigido por tipo) y las lambdas/`invokedynamic` van en su propia pasada" },
      { hz: "A", txt: "**✅ Miembros implícitos del `enum`** (§8.9.3) — constantes como campos, `$VALUES`, el constructor privado `(String, int)` y `values()`/`valueOf()`. Destrabarlo pidió cerrar antes la **invocación explícita de constructor** (`super(...)`/`this(...)`, §8.8.7.1), que **no existía**: el parser la producía, la atribución la rechazaba, y sin ella no se puede heredar de ninguna clase con constructor parametrizado. Dos desvíos deliberados de javac, ninguno semántico: `values()` copia con un **bucle** en vez de `$VALUES.clone()` (lo que importa de `clone()` es devolver un array fresco), y `valueOf` compara contra los nombres literales en vez de delegar en `Enum.valueOf(Class, String)`, que va por reflexión. Se sumó **`java.lang.Enum`** a `bootstrap/`+`boot/`: sin ella el `.class` emitido no se podía ni verificar ni correr. Falta el `enum` con constantes **con argumentos** o constructor propio (javac le inserta `(String, int)` a la firma, lo que obliga a reescribir el símbolo y no solo el AST)" },
      { hz: "A", txt: "**✅ Inicializadores de campo** (§8.6/§12.4.2) — `int v = 7;` se vuelve una sentencia, unificada **en orden de fuente** con los bloques `{ }`/`static { }`: las de instancia al frente de cada constructor, las estáticas al `<clinit>`. Hasta ahora **no llegaban al `.class`**: un campo con inicializador compilaba a un campo en cero, y el emisor ni siquiera generaba `<clinit>` —así que el `$VALUES` de un `enum`, el `$SwitchMap` de su `switch` y el guard del `assert` quedaban declarados y vacíos—" },
      { hz: "A", txt: "**✅ Generación de `StackMapTable`** — hecha en B3 (ver ahí): la pila de operandos se lleva **tipada**, no como altura, y el tipo `uninitialized` (tag 8) identifica cada objeto por el offset de su `new`" },
    ] },
  { id: "B5", name: "Cumbre del compilador  🚧 parcial", hz: "C",
    exito: "compila código genérico equivalente al de `javac`.",
    items: [
      { hz: "C", txt: "**✅ Genéricos** — *type erasure*, *wildcards* (containment), subtipado e inferencia por argumentos ya funcionan" },
      { hz: "C", txt: "**✅ Inferencia por *target type*** — hecha en B4 (modo *checking* en la atribución; el diamante incluido). Falta: *capture conversion* (§5.1.10), las *poly expressions* propiamente dichas (lambdas/*method refs* — el parser aún no las tiene) y el target type en posición de **argumento de llamada** (pide el algoritmo de dos fases)" },
    ] },
];

// ---- Componentes del compilador: las pasadas y las estructuras intermedias ----
const TBL_PASADAS = [
  ["**Lexer**", "texto `.java` → tokens", "B0", "✅"],
  ["**Parser**", "tokens → **AST** (cada nodo con su posición de fuente)", "B1", "✅"],
  ["**1 · Enter**", "declaraciones → **tabla + grafo tipado** (class finder real, resolución)", "B2", "✅"],
  ["**2 · Attribute**", "resuelve nombres + tipa + **decora** el AST (overload, genéricos, inferencia)", "B2", "✅"],
  ["**Check**", "bien-formación de **declaraciones** (override §8.4.8, abstractos §8.1.1.1) + **usos** (acceso §6.6, excepciones chequeadas §11.2)", "B4", "✅"],
  ["**3 · Flow**", "asignación definitiva (+ `final`) + alcanzabilidad", "B4", "✅"],
  ["**4 · Desugar**", "baja azúcar (sentencias, concat, compuesta, varargs, `++`/`--`, switch-expr→sentencia, `switch` sobre `String`, `enum` (con `$SwitchMap` sintético) **y *patterns*** (cadena de `instanceof`), guard del `assert`, records, **miembros del `enum`**, **inicializadores de campo**, supresión en try-recursos); falta *TransTypes*/indy", "B4", "✅"],
  ["**5 · Codegen**", "AST decorado → **bytecode** → `.class` (v69): objetos, tipos anchos, flujo de control completo (`switch`, ternario, saltos etiquetados), excepciones, `synchronized` y **StackMapTable**; falta `invokedynamic`", "B3", "🚧"],
];

const TBL_ESTRUCTURAS = [
  ["Tabla de símbolos", "catálogo de todo lo declarado, con sus tipos", "persistente"],
  ["Arena de tipos (*interned*)", "tipos resueltos, comparables por `TypeId` en O(1)", "persistente"],
  ["Interner de nombres", "identificadores → `NameId` (comparación e identidad baratas)", "persistente"],
  ["**Log de errores**", "acumula errores/warnings — **no aborta** al primero", "persistente"],
  ["Decoración del AST", "cada nodo lleva su `tipo` y su `binding` **in situ** (salida de Attribute, estilo `JCTree.type`/`sym`)", "persistente"],
  ["Álgebra de tipos (`types`)", "subtipado, erasure, sustitución, `lub`/`glb` — el `Types` de javac", "persistente"],
  ["Motor de inferencia (`infer`)", "variables de inferencia + *bounds* + resolución (Cap. 18)", "transitorio"],
  ["Tabla de constantes", "valores de expresiones constantes (JLS §15.29)", "persistente"],
  ["*Class finder*", "carga los **tipos externos** (`.class` del classpath)", "persistente"],
  ["**Env** (contexto)", "*scope* / clase / método / tipo esperado actuales", "transitorio"],
  ["Estado de *Flow*", "*bitsets* de asignación definitiva y alcanzabilidad", "transitorio"],
  ["Estado de inferencia", "variables de inferencia + restricciones (genéricos/lambdas)", "transitorio"],
];

const PASADAS_ASCII = [
  "  .java ─[lexer]─▶ tokens ─[parser]─▶ AST",
  "                                      │   (un solo árbol, reusado por las 5 pasadas)",
  "                                      ├─ 1 Enter      ─▶ tabla de símbolos",
  "                                      ├─ 2 Attribute  ─▶ decora el AST (tipos + bindings)",
  "                                      ├─ 3 Flow       ─▶ asignación definitiva + alcanzabilidad",
  "                                      ├─ 4 Desugar    ─▶ baja la azúcar sintáctica",
  "                                      └─ 5 Codegen    ─▶ bytecode ─[write]─▶ .class",
].join("\n");

const FASE_C = [
  { id: "C0", name: "Núcleo de java.lang", hz: "B",
    exito: "un programa que usa `String` y `System.out` corre en tu JVM.",
    items: [{ hz: "B", txt: "`Object`, `String`, `System`, wrappers, `Math`, `StringBuilder`" }] },
  { id: "C1", name: "Excepciones", hz: "B",
    exito: "lanzar y atrapar excepciones de la biblioteca propia.",
    items: [{ hz: "B", txt: "Jerarquía `Throwable`/`Exception`/`RuntimeException`" }] },
  { id: "C2", name: "Colecciones e IO", hz: "A",
    exito: "un programa que usa `ArrayList`/`HashMap` corre.",
    items: [
      { hz: "A", txt: "`java.util`: `List`, `ArrayList`, `Map`, `HashMap`" },
      { hz: "A", txt: "`java.io`: `InputStream`/`OutputStream`/`PrintStream`" },
    ] },
  { id: "C3", name: "Cumbre de la biblioteca", hz: "C",
    exito: "cubrir lo necesario de `java.base` para programas reales.",
    items: [{ hz: "C", txt: "Resto de `java.base` (net, nio, time, reflexión completa…)" }] },
];

const FASE_F = [
  { id: "F0", name: "Quickening", hz: "A",
    exito: "un método que invoca en bucle no re-resuelve el constant pool en cada vuelta.",
    items: [{ hz: "A", txt: "Resolver refs del constant pool **una sola vez** y reescribir el opcode a su variante resuelta (como las JVM reales)" }] },
  { id: "F1", name: "Superinstrucciones / fusión", hz: "A",
    exito: "menos overhead de despacho fusionando secuencias calientes.",
    items: [{ hz: "A", txt: "Fusionar `iload, iload, iadd` en un solo handler (primo del *operator fusion* de los compiladores ML, p. ej. FlashAttention)" }] },
  { id: "F2", name: "Inline caching + stack caching", hz: "C",
    exito: "dispatch dinámico acelerado en llamadas repetidas.",
    items: [
      { hz: "C", txt: "Inline cache en sitios de llamada (recordar receptor → método)" },
      { hz: "C", txt: "Tope de la pila de operandos en registro/variable" },
    ] },
  { id: "F3", name: "JIT (bytecode → nativo)", hz: "C",
    exito: "compilar métodos calientes a nativo. La cumbre lejana — **meta real** del track (apuesta del dueño), no descartada; el differential testing la mantiene honesta.",
    items: [{ hz: "C", txt: "Compilación de métodos calientes con *register allocation*, etc." }] },
];

const FASE_G = [
  { id: "G0", name: "plain_data plano en el heap", hz: "C",
    exito: "un value type sin header conviviendo con los objetos normales.",
    items: [
      { hz: "C", txt: "Tipo sin header: `[field0 | field1]` vs objeto normal `[class_ptr | fields]`" },
      { hz: "C", txt: "El compilador (Fase B) lo marca; la VM lo guarda inline" },
    ] },
  { id: "G1", name: "Arrays flatteados + semántica de valor", hz: "C",
    exito: "comparar (medible) memoria/layout de `plain_data[]` vs un array de objetos.",
    items: [
      { hz: "C", txt: "`plain_data[]` contiguo: sin los N punteros, ni los N headers, ni N alocaciones" },
      { hz: "C", txt: "Igualdad por valor (comparar bytes), sin `null`, sin GC para ellos" },
    ] },
  { id: "G2", name: "Escape analysis lite", hz: "C",
    exito: "aplanar en *load time* cuando se prueba que un objeto no escapa.",
    items: [{ hz: "C", txt: "Versión declarativa/estática (el automático completo es del JIT, Hito F3)" }] },
];

const TBL_HERRAMIENTAS = [
  ["`javap` (desensamblador)", "Se logra con el Hito A0", "Base"],
  ["`java` (lanzador de la JVM)", "Se logra durante la Fase A", "Base"],
  ["`javac` (compilador)", "Es toda la Fase B", "Base"],
  ["`jar` (empaquetador de `.class`)", "Tras la Fase B", "Avanzado"],
  ["`jdb`, `javadoc`, `jlink`, `keytool`", "Largo plazo", "Cumbre"],
];

// ---- Concurrencia: el "rascacielos" (lo que falta para multithread pro) ----
const TBL_CONCURRENCIA = [
  ["6 · `java.util.concurrent`", "✅🟡 núcleo + utils (H6)", "**Hecho:** **AQS** (`AbstractQueuedSynchronizer`) en **modo exclusivo y compartido** —base de casi todo—, `ReentrantLock` + `Condition`, `ReentrantReadWriteLock`, `Semaphore`, `CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue`/`LinkedBlockingQueue`/`PriorityBlockingQueue`/`DelayQueue` (una/dos locks / min-heap / delay), el **framework `Executor`** (`ThreadPoolExecutor` + `submit`/`Future`/`FutureTask`/`Callable`, y un `ScheduledThreadPoolExecutor` con min-heap por deadline), un **`ConcurrentHashMap`** (encadenamiento + *lock striping*) y un **`CompletableFuture`** (`supplyAsync`/`thenApply`/`thenCompose`/`get`, `exceptionally` y `thenCombine`), todo **en Java** sobre AQS (o el monitor) y validado por el oráculo (`green≡os-gil≡os`). Motivaron agregar `Object.equals`, `java.util.function`/`Objects`, `Comparator`/`Comparable`/`Delayed` y el intrínseco `System.nanoTime`. **Falta (volumen):** deques, *resize*/lecturas lock-free del `ConcurrentHashMap`."],
  ["5 · Atómicos / CAS", "✅ **hecho (H5)**", "**Hecho:** `compareAndSwap` (la instrucción atómica raíz: `cas_u32`/`cas_u64` sobre el `AtomicRegion`, `AcqRel`) y `AtomicInteger`/`AtomicLong`/`AtomicReference` —la CAS de referencia pasa por el write barrier—. Verificado: `AtomicLong` 64-bit + `AtomicReference` identity-CAS → 202."],
  ["4 · Modelo de Memoria (JMM)", "✅🟡 relajado", "**Hecho (H4):** campos no-volatile **lock-free** (`Relaxed`), `volatile` = `Acquire`/`Release`, no-*tearing* de `long`/`double` (`AtomicU64`), Eden atómico **Miri-verificado**; el cache de condy es raíz de GC. *happens-before* vía los átomicos + el `RwLock`. Falta: semántica de `final`, `VarHandle`/CAS."],
  ["3 · API de `Thread`", "✅ **hecho (H1)**", "**Hecho:** `start`/`run`/`join`/`sleep`, `currentThread`, `yield`, nombre/id, `isAlive`, `getState()` (los seis estados; `ThreadStatus` pasó a cinco + `NEW` derivado, con `sleep_until`/`joining_on` plegados dentro), `Thread(Runnable)`, `start` dos veces → `IllegalThreadStateException`, e `interrupt`/`InterruptedException` que despierta `sleep`/`join`/`wait` (re-adquiere el monitor antes de lanzar en el `wait`; la carrera notify/interrupt la resuelve el GIL). **Periféricos hechos:** `ThreadLocal` (aislamiento por-hilo verificado), prioridad (rango `[1,10]`), daemon (regla de ciclo de vida). **Falta:** `UncaughtExceptionHandler`."],
  ["2 · Monitor intrínseco", "🟢 casi", "**Hecho:** `monitorenter`/`exit`, `synchronized` (bloques **y** métodos), reentrancia, `wait`/`notify`/`notifyAll`, `IllegalMonitorStateException`, `wait(timeout)` y monitor ***GC-safe*** (las claves se remapean por el *forward* del GC en minor/compact, así que se puede compactar con monitores tomados). **Falta:** interrupción del `wait` (llega con H1), biased/thin locks, detección de deadlock."],
  ["1 · Substrato de ejecución", "✅🟡 gran avance", "**Hecho (E1+E2):** green threads (default y visor) **y** hilos de SO reales (`std::thread` por `Thread.start()`); `park`/`unpark` reales. **Hecho (E3 — GIL removido):** en `JVM_THREADS=os` los opcodes **frame-local** (aritmética int/stack/ramas) corren **lock-free en paralelo** sobre un `RunningCtx` por-hilo con code cache; solo `SharedVm` se lockea; GC con **safepoint stop-the-world cooperativo**. El `Arc<Mutex<JVM>>` de toda-la-VM desapareció; `os-gil` queda como oráculo. **Ensanchado:** frame-local completo (W1), lecturas concurrentes bajo `RwLock` (W3), y **asignación lock-free** con Eden en un arena `UnsafeCell` **Miri-verificado** (W2). **JMM (H4) hecho:** modelo relajado — campos lock-free `Relaxed`, `volatile` `Acquire`/`Release`, Eden atómico. **Falta:** locks por-estructura finos, preempción."],
];

// Rascacielos en ASCII, con padding calculado en JS para que el borde derecho cierre.
const TOWER_W = 78;
const cBar = (l, r) => l + "─".repeat(TOWER_W - 2) + r;
function cRow(text) {
  const inner = TOWER_W - 4;
  const t = text.length > inner ? text.slice(0, inner) : text;
  return "│ " + t + " ".repeat(inner - t.length) + " │";
}
const TOWER_ASCII = [
  "        RASCACIELOS DE LA CONCURRENCIA   ·   construir hacia arriba ↑",
  "",
  cBar("┌", "┐"),
  cRow("[6]  java.util.concurrent   ·  núcleo + utils · falta vol.  HECHO"),
  cRow("     [x] AQS (excl + shared) · ReentrantLock/RWLock · Condition"),
  cRow("     [x] Semaphore · CountDownLatch · CyclicBarrier · ArrayBlockingQueue"),
  cRow("     [x] ThreadPoolExecutor · ScheduledThreadPoolExecutor · Future"),
  cRow("     [x] ConcurrentHashMap · Linked/Priority/DelayQueue · Completabl"),
  cRow("     [ ] deques · resize/lecturas lock-free CHM · locks finos"),
  cBar("├", "┤"),
  cRow("[5]  Atómicos / CAS                   ·  raíz lock-free        HECHO"),
  cRow("     [x] compareAndSwap · AtomicInteger / AtomicLong / Reference"),
  cBar("├", "┤"),
  cRow("[4]  Modelo de Memoria (JMM)          ·  relajado (H4)       ✅🟡"),
  cRow("     [x] volatile=Acq/Rel · no-volatile Relaxed · no-tearing · Eden atómico"),
  cBar("├", "┤"),
  cRow("[3]  API de Thread                    ·  control de hilos    HECHO"),
  cRow("     [x] start · run · join · sleep · currentThread · yield"),
  cRow("     [x] getState (6 estados) · isAlive · name/id · start x2"),
  cRow("     [x] interrupt despierta sleep/join/wait · daemon/prio/TLocal"),
  cBar("├", "┤"),
  cRow("[2]  Monitor intrínseco               ·  monitor completo    HECHO"),
  cRow("     [x] monitorenter/exit · synchronized (bloques Y métodos)"),
  cRow("     [x] reentrancia · wait / notify / notifyAll · IMSE"),
  cRow("     [x] wait(timeout) · GC-safe · interrupt-of-wait"),
  cRow("     [ ] biased/thin locks · deadlock detect"),
  cBar("├", "┤"),
  cRow("[1]  Substrato de ejecución           ·  paralelismo real   ✅🟡"),
  cRow("     [x] green threads (cooperativo, default + visor)"),
  cRow("     [x] hilos de SO reales · park/unpark · os-gil (referencia)"),
  cRow("     [x] GIL removido: frame-local + lecturas + alloc lock-free"),
  cRow("     [x] W2 alloc: Eden en arena UnsafeCell (Miri-verificado)"),
  cRow("     [x] JMM (H4) relajado · [ ] locks por-estructura finos"),
  cBar("└", "┘"),
  "                     ▲ todo descansa en la planta 1 ▲",
].join("\n");

const ROUTE_ASCII = [
  "  ✔ HECHO             ✔ HECHO             ✔ HECHO             ✔ HECHO (subconj.)",
  " cerrar monitor      OS threads + GIL    API de Thread       sacar el GIL",
  " IMSE · wait(t)  ──► park/unpark     ──► interrupt       ──► frame-local sin lock",
  " · GC-safe           (os-gil = oráculo)  getState            → JMM·CAS·AQS ✔ · ACÁ: volumen j.u.c",
].join("\n");

// =============================================================
//  HTML
// =============================================================

const BRAND = "#B7410E";       // rust orange
const BRAND_DK = "#7A2D0A";    // rust oscuro
const INK = "#23201E";         // casi negro cálido

const html = `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Roadmap del JDK en Rust</title>
<style>
  * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  @page {
    size: A4;
    margin: 24mm 18mm 22mm 18mm;
    @top-left-corner { content: ""; background: ${BRAND_DK}; margin:0; padding:0; }
    @top-left {
      content: "JDK en Rust · Roadmap técnico";
      font-family: 'Segoe UI','Calibri',sans-serif; font-size: 9.5pt; font-weight: 600;
      color: #fff; background: ${BRAND_DK}; padding: 3.5mm 0 3.5mm 4mm; margin:0; vertical-align: middle;
    }
    @top-center { content: ""; background: ${BRAND_DK}; margin:0; padding:0; }
    @top-right {
      content: "Documento técnico";
      font-family: 'Segoe UI','Calibri',sans-serif; font-size: 9.5pt; font-style: italic;
      color: #F3D9C9; background: ${BRAND_DK}; padding: 3.5mm 4mm 3.5mm 0; margin:0; vertical-align: middle;
    }
    @top-right-corner { content: ""; background: ${BRAND_DK}; margin:0; padding:0; }
    @bottom-left { content: "${AUTOR}"; font-family:'Segoe UI',sans-serif; font-size: 8pt; color: #999; }
    @bottom-right { content: "Página " counter(page) " de " counter(pages); font-family:'Segoe UI',sans-serif; font-size: 8pt; color: #999; }
  }
  @page :first {
    margin: 0;
    @top-left-corner{content:"";background:transparent;} @top-left{content:"";background:transparent;}
    @top-center{content:"";background:transparent;} @top-right{content:"";background:transparent;}
    @top-right-corner{content:"";background:transparent;} @bottom-left{content:"";} @bottom-right{content:"";}
  }
  html, body { font-family:'Segoe UI','Calibri',sans-serif; font-size: 10.8pt; color: ${INK}; line-height: 1.45; margin:0; padding:0; }
  h1 { color: ${BRAND}; font-size: 21pt; margin: 1.3em 0 .45em; border-bottom: 2.5px solid ${BRAND}; padding-bottom:.18em; page-break-after: avoid; }
  h2 { color: ${BRAND_DK}; font-size: 14.5pt; margin: 1.1em 0 .35em; page-break-after: avoid; }
  h3 { color: ${INK}; font-size: 11.5pt; margin: .7em 0 .25em; page-break-after: avoid; }
  p { margin: .45em 0; }
  ul, ol { margin: .35em 0 .55em 1.15em; }
  li { margin: .14em 0; }
  code { font-family:'Consolas','Courier New',monospace; font-size: 9pt; background:#F3EEEA; padding: 1px 4px; border-radius: 3px; color:#7A2D0A; }
  pre { font-family:'Consolas','Courier New',monospace; font-size: 9pt; background:#FBF7F4; border-left: 3px solid ${BRAND}; padding:.7em 1em; margin:.6em 0; white-space: pre; overflow: hidden; page-break-inside: avoid; line-height: 1.3; }
  blockquote { margin:.6em 0; padding:.45em .9em; border-left: 4px solid ${BRAND}; background:#FBF4EF; color:#5b5550; font-style: italic; }
  table { border-collapse: collapse; width: 100%; margin:.5em 0 1em; page-break-inside: avoid; font-size: 9.8pt; }
  th { background:${INK}; color:#fff; padding:6px 9px; text-align:left; font-weight:600; border:1px solid ${INK}; border-bottom: 3px solid ${BRAND}; }
  td { padding:5px 9px; border:1px solid #E2DAD4; vertical-align: top; }
  tbody tr:nth-child(even) td { background:#FAF6F2; }

  .badge { display:inline-block; font-size:7.5pt; font-weight:700; padding:1px 7px; border-radius:10px; color:#fff; vertical-align: middle; letter-spacing:.3px; }
  .b-base { background:#2E7D32; }
  .b-adv  { background:#1565C0; }
  .b-sum  { background:#6A1B9A; }

  .hito { page-break-inside: avoid; margin: .2em 0 1em; padding-left:.2em; }
  .hito h3 { margin:.6em 0 .25em; font-size:11.5pt; color:${BRAND_DK}; }
  .hito-id { display:inline-block; background:${BRAND}; color:#fff; font-weight:700; font-size:9pt; padding:1px 7px; border-radius:4px; margin-right:4px; }
  .hito-items { list-style:none; margin:.2em 0 .3em; padding:0; }
  .hito-items li { margin:.2em 0; padding-left:.2em; }
  .exito { margin:.2em 0 .2em; font-size:10pt; color:#3a6b3a; background:#F1F7F1; border-radius:4px; padding:3px 9px; }
  .exito strong { color:#2E7D32; }

  .cover { height: 297mm; width: 210mm; margin:0; padding:0; display:flex; flex-direction:column; align-items:center; justify-content:center; page-break-after: always; position:relative; box-sizing:border-box; }
  .cover::before { content:""; position:absolute; top:0; left:0; right:0; height:20mm; background: linear-gradient(to right, ${BRAND_DK}, ${BRAND}); }
  .cover::after  { content:""; position:absolute; bottom:0; left:0; right:0; height:8mm; background: linear-gradient(to right, ${BRAND_DK}, ${BRAND}); }
  .cover-emblem { font-family:'Consolas',monospace; font-size:13pt; color:${BRAND}; letter-spacing:2px; margin-bottom:6mm; }
  .cover-sub { color:${BRAND_DK}; font-size:15pt; text-align:center; margin-bottom:10mm; letter-spacing:1px; }
  .cover-title { font-size:38pt; font-weight:800; text-align:center; color:${INK}; line-height:1.08; }
  .cover-tagline { color:${BRAND}; font-size:16pt; text-align:center; margin-top:7mm; font-style:italic; }
  .cover-rule { width:62mm; height:3px; background:${BRAND}; margin:7mm auto; }
  .cover-meta { position:absolute; bottom:18mm; left:0; right:0; text-align:center; color:#777; font-size:10pt; }
  .cover-meta strong { color:${BRAND_DK}; }

  .toc-list { list-style:none; padding:0; margin:.6em 0; }
  .toc-list li { display:flex; padding:4px 0; border-bottom:1px dotted #D8CFC8; font-size:10.5pt; }
  .toc-list li.l2 { padding-left:1.6em; color:#5b5550; }
  .toc-list .num { flex:1; }
  .toc-list .pg { color:${BRAND}; font-weight:600; }
  .legend { margin:.6em 0; font-size:10pt; }
  .legend .badge { margin-right:4px; }
  .section-break { page-break-before: always; }
</style>
</head>
<body>

<!-- Portada -->
<div class="cover">
  <div class="cover-emblem">&lt;/&gt; fn main()</div>
  <div class="cover-sub">Proyecto personal · Sistemas de bajo nivel</div>
  <div class="cover-rule"></div>
  <div class="cover-title">Construir un JDK<br>desde cero en Rust</div>
  <div class="cover-rule"></div>
  <div class="cover-tagline">Roadmap técnico por hitos</div>
  <div class="cover-meta">
    Autor: <strong>${AUTOR}</strong><br>
    Fecha: <strong>${FECHA}</strong>
  </div>
</div>

<!-- TOC -->
<div class="section-break">
  <h1>Contenido</h1>
  <ul class="toc-list">
    <li><span class="num">Introducción y alcance</span><span class="pg">3</span></li>
    <li class="l2"><span class="num">Los tres pilares</span><span class="pg">3</span></li>
    <li class="l2"><span class="num">El problema del bootstrap</span><span class="pg">3</span></li>
    <li class="l2"><span class="num">Orden de construcción</span><span class="pg">4</span></li>
    <li><span class="num">Fase A — La JVM (el runtime)</span><span class="pg">5</span></li>
    <li><span class="num">Concurrencia — ruta al multithread profesional</span><span class="pg">7</span></li>
    <li><span class="num">Fase B — El compilador (javac, en Rust)</span><span class="pg">9</span></li>
    <li><span class="num">Fase C — Las bibliotecas</span><span class="pg">10</span></li>
    <li><span class="num">Fase D — Herramientas (la &ldquo;DK&rdquo;)</span><span class="pg">11</span></li>
    <li><span class="num">Fase E — Cerrar el círculo</span><span class="pg">11</span></li>
    <li><span class="num">Más allá del JDK — la plataforma</span><span class="pg">13</span></li>
    <li class="l2"><span class="num">Fase F — burst (optimizador)</span><span class="pg">13</span></li>
    <li class="l2"><span class="num">Fase G — plain_data / value types</span><span class="pg">14</span></li>
    <li><span class="num">Estado actual</span><span class="pg">15</span></li>
  </ul>
</div>

<!-- Introducción -->
<div class="section-break">
<h1>Introducción y alcance</h1>
<p>Este documento traza la ruta para construir un <strong>JDK educativo desde cero en Rust</strong>: no para competir con HotSpot, sino para <strong>cargar y ejecutar bytecode real</strong> y, eventualmente, compilar y correr código Java escrito y compilado con herramientas propias. El objetivo es doble: aprender sistemas de bajo nivel a fondo y completar un reto de ingeniería de gran calibre.</p>
<p>Un JDK no es una sola pieza. Son <strong>tres pilares</strong> que se sostienen entre sí:</p>

<h2>Los tres pilares</h2>
${table(["Pilar", "Qué hace", "Lenguaje"], TBL_PILARES)}

<h2>El problema del bootstrap</h2>
<p>Las bibliotecas necesitan al <strong>compilador</strong> (para compilarse) y a la <strong>JVM</strong> (para ejecutarse). Si el compilador se escribiera en Java, se necesitaría a sí mismo para compilarse — el clásico problema del huevo y la gallina.</p>
<blockquote>Decisión de diseño: el compilador se escribe <strong>en Rust</strong>, igual que la JVM. Así Rust compila al compilador, el compilador compila las bibliotecas, y la JVM las ejecuta. El ciclo queda roto.</blockquote>

<h2>Orden de construcción</h2>
<p>La <strong>JVM va primero</strong>, sin excepción: tanto la salida del compilador como las bibliotecas son inertes sin un motor que las ejecute, y no se puede ni <em>probar</em> que el compilador genera bytecode correcto sin algo que lo corra.</p>
<pre>A · JVM ──→ B · Compilador ──→ C · Bibliotecas ──→ E · Cerrar el círculo
(motor)     (.java → .class)    (en Java)           (todo junto)
   └── D · Herramientas se completan en el camino (javap, java, jar…)</pre>
<p class="legend">
  <strong>Horizontes:</strong>
  ${badge('B')} el núcleo, por aquí empezamos &nbsp;
  ${badge('A')} más duro, segunda pasada &nbsp;
  ${badge('C')} lo más difícil, pero vamos a llegar
</p>
<blockquote>Cómo leer el roadmap: es una <strong>ruta ordenada por hitos</strong>. Cada hito tiene un criterio de éxito medible y no se cierra hasta cumplirlo. El orden respeta las dependencias: no se puede el hito N sin el N-1.</blockquote>
</div>

<!-- Fase A -->
<div class="section-break">
<h1>Fase A — La JVM (el motor que ejecuta bytecode)</h1>
${FASE_A.map(hito).join('\n')}
</div>

<!-- Concurrencia (deep-dive de A6) -->
<div class="section-break">
<h1>Concurrencia — ruta al multithread profesional</h1>
<p>La ejecución concurrente (Hito A6) merece su propio mapa, porque es donde el proyecto apunta a <strong>multithread profesional</strong>. Lo que hoy corre sobre <strong>green threads</strong> es la <em>planta baja</em> de un edificio cuya cumbre es <code>java.util.concurrent</code>. Cada planta se apoya en la de abajo: no hay <code>ReentrantLock</code> sin CAS, ni CAS útil sin un modelo de memoria, ni modelo de memoria que <em>importe</em> sin hilos reales que lo hagan necesario.</p>

<pre>${esc(TOWER_ASCII)}</pre>

<p class="legend"><strong>Estado:</strong> ✅ hecho &nbsp;&nbsp; 🟡 parcial &nbsp;&nbsp; ⬜ falta</p>
${table(["Planta", "Estado", "Qué falta para &ldquo;profesional&rdquo;"], TBL_CONCURRENCIA)}

<h2>Los dos cimientos reales</h2>
<p>De todo el edificio, <strong>dos primitivas</strong> sostienen el resto: <strong><code>park</code>/<code>unpark</code></strong> (planta 1) y <strong>CAS</strong> (planta 5). De esas dos se deduce <em>casi todo</em> lo de arriba — AQS, los locks, los atómicos, los pools. Por eso el salto grande <strong>no</strong> era terminar el monitor (planta 2), sino cambiar el <strong>substrato</strong> de green threads a hilos de SO — y ese salto <strong>ya está dado</strong> (E1+E2): <code>park</code>/<code>unpark</code> son reales y el monitor quedó cerrado salvo la interrupción del <code>wait</code>. Queda la segunda mitad: <strong>sacar el GIL</strong> (E3), y después CAS como cimiento de todo lo lock-free.</p>

<h2>Ruta crítica</h2>
<pre>${esc(ROUTE_ASCII)}</pre>
<p>El camino canónico — el mismo que recorrieron CPython y las JVM tempranas — es <strong>GIL primero</strong> (hilos de SO con un lock global que serializa el intérprete: simple y correcto, pero todavía sin paralelismo real) y después <strong>sacar el GIL</strong> (locks finos por estructura + TLABs + GC stop-the-world). Recién ahí <code>volatile</code> y los <em>fences</em> dejan de ser decorativos: con green threads no hay reordenamientos reales que tapar; con hilos de SO, omitirlos rompe el código de formas no-deterministas.</p>
<blockquote>Para el LLM, el paralelismo pesado <strong>no</strong> vive en este rascacielos: vive en <strong>kernels off-heap</strong> (rayon/CUDA). Esta torre es para la <strong>corrección</strong> de la concurrencia Java, no para el cómputo numérico — que sale del heap por completo.</blockquote>
</div>

<!-- Fase B -->
<div class="section-break">
<h1>Fase B — El compilador (javac, escrito en Rust)</h1>
<p>El front-end convierte texto en estructuras y la back-end las convierte en bytecode:</p>
<pre>.java → lexer (tokens) → parser (AST) → análisis semántico → generación de bytecode → .class</pre>
${FASE_B.map(hito).join('\n')}
</div>

<!-- Fase C -->
<div class="section-break">
<h1>Fase C — Las bibliotecas (en Java, compiladas por tu javac)</h1>
${FASE_C.map(hito).join('\n')}
</div>

<!-- Fase D y E -->
<div class="section-break">
<h1>Fase D — Herramientas (la &ldquo;DK&rdquo; = Development Kit)</h1>
<p>No se construyen en bloque, sino que se van completando como subproducto de las otras fases.</p>
${table(["Herramienta", "Cuándo se logra", "Horizonte"], TBL_HERRAMIENTAS)}

<h1>Fase E — Cerrar el círculo</h1>
<p>El momento épico: las tres piezas funcionando juntas.</p>
<ul>
  <li>${badge('C')} Compilar las bibliotecas (Fase C) con tu propio <code>javac</code> (Fase B)</li>
  <li>${badge('C')} Ejecutar un programa real que use esas bibliotecas en tu propia JVM (Fase A)</li>
  <li>${badge('C')} Conformance: comportamiento fiel a la especificación</li>
</ul>
<p class="exito"><strong>✓ Éxito:</strong> un <code>.java</code> que escribís → lo compila tu <code>javac</code> → usa tus bibliotecas → corre en tu JVM, sin tocar nada del JDK de Temurin.</p>
</div>

<!-- Más allá del JDK: la plataforma -->
<div class="section-break">
<h1>Más allá del JDK — la JVM como plataforma</h1>
<p>La JVM no es un fin en sí mismo: es la <strong>carrocería</strong> de un proyecto más grande. Como construimos VM <strong>+</strong> compilador propios, controlamos el stack entero y podemos <strong>inventar</strong> más allá de lo que <code>javac</code>/HotSpot permiten. Estos dos tracks son extensiones aspiracionales apoyadas en las fases A–E.</p>

<h1>Fase F — <code>burst</code>: el optimizador (rendimiento)</h1>
<blockquote>Módulo de optimización <strong>opcional</strong> atornillado sobre el intérprete ingenuo, que actúa de <strong>chasis y oráculo de corrección</strong>. <code>burst</code> debe dar resultados <strong>idénticos</strong> y se valida con <strong>differential testing</strong> (mismo programa por los dos caminos → assert de igualdad). Importante: un optimizador <strong>no necesita JIT</strong> — el intérprete tiene su propia caja de herramientas (típico 2–10× sin compilar a nativo).</blockquote>
${FASE_F.map(hito).join('\n')}
</div>

<!-- Fase G -->
<div class="section-break">
<h1>Fase G — <code>plain_data</code> / value types (modelo de datos)</h1>
<blockquote>Los <strong>&ldquo;huérfanos de Object&rdquo;</strong>: tipos planos sin header, sin identidad, sin monitor, <em>flatteables</em>. Viable porque tenemos compilador propio (Fase B) que puede emitir el constructo y una VM que lo trata especial. Es, en chiquito, el principio de representación de un <strong>tensor</strong> — puente directo a sistemas de ML. Inspiración: <em>value classes</em> de Valhalla, <code>struct</code> de .NET.</blockquote>
${FASE_G.map(hito).join('\n')}
</div>

<!-- Estado -->
<div class="section-break">
<h1>Estado actual</h1>
<p><strong>Fase A / Hitos A0–A5 logrados; A6 en progreso.</strong> El proyecto compila <strong>sin warnings</strong>, pasa <strong>142 tests</strong>, cubre <strong>el set de opcodes completo</strong> (199 de 199 alcanzables, <code>invokedynamic</code> incluido), produce <strong>fixtures byte-idénticos</strong> a <code>javap</code> y ya <strong>ejecuta bytecode real con objetos, GC generacional, hilos (green <em>y</em> de SO bajo un GIL), monitores y un sistema de tipos completo</strong>: aritmética, control de flujo, recursión, <code>switch</code> (<code>tableswitch</code>/<code>lookupswitch</code>), un <strong>heap</strong> con objetos/herencia/campos/arrays, <strong>dispatch dinámico</strong>, <strong>excepciones</strong>, <strong>inicialización de clases</strong>, <strong>class loaders</strong>, un <strong>recolector generacional</strong> (young por copia + Old mark·sweep·compact, con <strong>referencias débiles</strong> de <code>java.lang.ref</code>), <strong>green threads</strong> (scheduler cooperativo) con <strong>monitores</strong> (<code>synchronized</code> de bloques y métodos, <code>wait</code>/<code>notify</code>), y un <strong>verificador de bytecode JVMS-estricto</strong>.</p>
<ul>
  <li><strong>ClassReader</strong> (cursor big-endian) y <strong>constant pool</strong> completo (<code>ConstantPoolEntry</code>, 17 tags + <code>Tombstone</code>), con árbol de referencias en <code>pretty_class_visualizer.rs</code>.</li>
  <li><strong>Header</strong>: versiones, <code>access_flags</code> (+ métodos <code>is_*</code>), <code>this_class</code>/<code>super_class</code> con <code>class_name()</code>. <code>from_path() → Result</code> (sin panics).</li>
  <li><strong>Code</strong> con <strong>desensamblado completo</strong> (opcodes + <code>tableswitch</code>/<code>lookupswitch</code>/<code>wide</code>) y comentarios <code>// …</code> resueltos.</li>
  <li><strong><code>StackMapTable</code> (los 7 frame types)</strong>, cada frame en su clase bajo <code>parser/stack_map_table/</code>, con <code>verification_type_info</code>.</li>
  <li><code>javap</code> <strong>brief y <code>-v</code> byte-idénticos</strong> (incl. cabecera Classfile / Last modified / SHA-256). Flags de visibilidad (<code>-public</code>/<code>-protected</code>/<code>-package</code>/<code>-p</code>).</li>
  <li>Renderers factorizados en <code>parser/printers/</code>: <code>verbose</code> (orquestación) + <code>file_header</code> + <code>pool_comments</code> + <code>member_dump</code> + <code>brief</code> + <code>dump_common</code> + <code>visibility</code>.</li>
</ul>
<p><strong>Pendiente de A0</strong> (no esencial, no bloquea avanzar): atributos <code>Signature</code> (reescribe la declaración → parser de firmas genéricas), <code>BootstrapMethods</code>, <code>InnerClasses</code>/<code>EnclosingMethod</code>, <code>Exceptions</code>, <code>ConstantValue</code>, anotaciones, <code>Record</code>, <code>PermittedSubclasses</code>, <code>NestHost</code>/<code>NestMembers</code>, <code>LocalVariableTable</code>/<code>Type</code>; y flags de contenido (<code>-c</code>/<code>-l</code>/<code>-s</code>).</p>
<p><strong>A1–A2 — el intérprete.</strong> Loop de despacho sobre una <strong>pila de frames</strong>; cada frame referencia su bytecode por índice (<code>MethodId</code>) en el <em>Method Area</em> (<code>metaspace</code>, con resolución de llamadas por el código del constant pool). Opcodes: <code>iconst</code>/<code>iload</code>/<code>istore</code>, <code>iadd</code>/<code>isub</code>/<code>imul</code>, <code>goto</code>/<code>if_icmpgt</code>, <code>invokestatic</code>/<code>ireturn</code>. Corre <strong>factorial</strong> y <strong>fibonacci recursivos</strong>. Visualizador <code>jvm-step</code> paso a paso con vista de dos paneles de la call stack.</p>
<p><strong>A3 — objetos y heap.</strong> El <strong>heap</strong> es una arena de bytes (<code>Vec&lt;u8&gt;</code>) con <code>malloc</code> bump (sin liberar; el GC llega en A5). Los objetos se disponen como <code>[class_id | mark | campos]</code> con <em>layout</em> que respeta la herencia; cada clase recibe un <strong>Class ID</strong> (UUID) y un <em>mirror</em> en el heap para sus estáticos (estilo HotSpot). Opcodes: <code>new</code>, <code>dup</code>, <code>getfield</code>/<code>putfield</code>, <code>aload</code>/<code>astore</code>, los cuatro <code>invoke*</code> y la familia de arrays (<code>newarray</code>/<code>anewarray</code>, <code>arraylength</code>, <code>iaload</code>/<code>iastore</code>/<code>aaload</code>/<code>aastore</code> + byte/char/short). El <strong>dispatch dinámico</strong> usa <em>vtables</em> por clase (slot del tipo estático, método del tipo real) e <em>itable</em> para interfaces — verificado: <code>Animal a = new Dog(); a.sound()</code> → <code>Dog.sound</code>.</p>
<p><strong>A4 — robustez del runtime.</strong> <strong>Excepciones</strong>: <code>athrow</code> + tabla de excepciones + <em>stack unwinding</em> por la pila de frames, con <code>finally</code> (catch-all + re-throw) y las <strong>implícitas</strong> que la VM sintetiza (NPE, índice fuera de rango, cast inválido, tamaño negativo). <strong>Verificador</strong> de bytecode (type-checking en una pasada con el <code>StackMapTable</code>) que corre antes de ejecutar. <strong>Inicialización</strong> de clases <code>&lt;clinit&gt;</code> perezosa y super-primero. <strong>Class loaders</strong> bootstrap/app con delegación parent-first (las <code>java.lang.*</code> propias viven en <code>boot/</code>). Resolución que falla con <strong>errores de linkage</strong> (<code>NoClassDefFoundError</code>/<code>NoSuchMethodError</code>) en vez de paniquear. <strong>Robustez fina cerrada (2026-08):</strong> <code>Throwable</code> con <code>getMessage</code>/<code>toString</code> y <strong>stack traces</strong> capturados por la VM en el throw; <code>ExceptionInInitializerError</code> (JVMS §5.5: un <code>&lt;clinit&gt;</code> que lanza propaga, se envuelve, y deja la clase <em>erroneous</em> → <code>NoClassDefFoundError</code> al reuso); <strong>default methods</strong> de interfaces (JSR 335 — la vtable fusiona superinterfaces, regla <em>maximally-specific</em>). Pendiente fino: chequeos de acceso e identidad <code>(nombre, loader)</code> (no producibles con javac — ver <code>holdon.md</code>).</p>
<p><strong>A5 — nativos + GC.</strong> <strong>Puente nativo</strong> (<code>System.out.println</code> imprime de verdad) e <em>intrinsics</em> (<code>getClass</code>, <code>hashCode</code>, <code>Math</code>, <code>arraycopy</code>, <code>String</code>/<code>Class</code>). <strong>Recolector de basura</strong> que arrancó en mark &amp; sweep y creció a un colector con <strong>compactación</strong> (mover objetos + reescribir punteros), <strong>free list</strong> con <em>coalescing</em>, <strong>política de fragmentación</strong> configurable, <strong>disparadores</strong> (out-of-space / occupancy / allocation-rate / explícito) sobre un <em>safepoint</em>, y <strong>marcado transitivo</strong> correcto (sigue campos, arrays y estáticos).</p>
<p><strong>A6 (en progreso) — cumbre del runtime.</strong> <strong>Verificador de bytecode JVMS-estricto</strong>: además de la cobertura completa de opcodes (incluido <code>switch</code>), verifica <strong>con o sin <code>StackMapTable</code></strong> — inferencia por punto fijo como fallback, con <em>cross-check</em> de la tabla contra la inferencia — sobre un <strong>gate estructural</strong> (§4.9), con <strong>tipos de locales</strong> estrictos + <code>max_stack</code>, reglas de <strong>objetos sin inicializar</strong>, <strong>handlers de excepción</strong> tipados y <strong>acceso/linkage</strong> (regla <code>protected</code>, <code>count</code> de <code>invokeinterface</code>). <strong>Sistema de tipos completo</strong>: los cuatro tipos numéricos (<code>int</code>/<code>long</code>/<code>double</code>/<code>float</code>) <strong>ejecutados y verificados</strong> — cómputo, conversiones, comparaciones, división con <code>ArithmeticException</code>, <strong>categoría-2</strong> (params, campos, estáticos, arrays, frames del <code>StackMapTable</code>) y el <strong>lattice de referencias</strong> (covarianza de arrays + <em>join</em>/LUB). <strong>GC generacional</strong>: arena dividida en <code>Eden | S0 | S1 | Old</code>; los objetos nacen en Eden, un <strong>minor</strong> por <strong>copia</strong> evacúa los pocos sobrevivientes a un <em>survivor</em> (o los <strong>promueve</strong> a Old por edad) reescribiendo referencias con <em>forwarding</em>, un <strong>write barrier</strong> mantiene un <strong>remembered set</strong> de punteros <code>old→young</code> (las raíces que el minor escanea, sin recorrer todo Old), y el <strong>major</strong> hace mark-sweep-compact sobre Old. <strong>Referencias débiles</strong> (<code>java.lang.ref</code>): <code>WeakReference</code> + <code>ReferenceQueue</code> — el major trata el <code>referent</code> como débil y, al morir, lo limpia (<code>get()</code> → <code>null</code>) y <strong>encola</strong> la referencia; cimiento de <code>PhantomReference</code>/<code>Cleaner</code>.</p>
<p><strong>Cobertura del set de opcodes — 199/199 alcanzables.</strong> El intérprete despacha todos los opcodes del JVMS que un class file moderno puede contener. Los últimos en cerrarse fueron <code>nop</code> (0x00), <code>goto_w</code> (0xc8) — el <code>goto</code> de offset de 4 bytes, para targets a más de ±32 KB, implementado ensanchando <code>jump_to</code> hacia un <code>jump_to_wide</code> común en vez de duplicar la aritmética — y el prefijo <strong><code>wide</code></strong> (0xc4), que reejecuta el opcode siguiente con un índice de local de <strong>16 bits</strong>: la única manera de direccionar los slots pasados el 255 en un método con más de 256 locales (<code>wide iinc</code> mide 6 bytes, el resto 4). Ese último salió barato por una propiedad que el diseño ya tenía: los handlers de <code>variable_operations</code> reciben <code>slot: usize</code> y <strong>nunca se enteran del ancho</strong>, así que ensanchar es puro <em>decoding</em> y el módulo no cambió de comportamiento. Verificado por <em>differential testing</em>: <code>WideLocals.java</code> declara 300 locales para forzar a <code>javac</code> a emitir el prefijo (<code>istore_w 299</code>, <code>iinc_w 299, 35</code>, <code>iload_w 299</code>), y el mismo <code>.class</code> da <code>42</code> tanto en el <code>java</code> de JDK 25 como en nuestra VM. <strong>De los 4 restantes, 3 son una decisión y no deuda</strong>: <code>jsr</code>/<code>ret</code>/<code>jsr_w</code> están prohibidos por <strong>JVMS §4.9.1</strong> en class files de versión 50.0+ (Java 6 en adelante) — desde que existe el <code>StackMapTable</code> y el verificador por <em>type-checking</em>, las subrutinas quedaron fuera del modelo, y <code>javac</code> compila <code>finally</code> duplicando el bloque. La postura del proyecto es <strong>leer sí, ejecutar no</strong>: el desensamblador soporta <code>jsr</code>/<code>ret</code>/<code>jsr_w</code>/<code>ret_w</code> porque un <code>javap</code> debe renderizar <em>cualquier</em> class file legal (lo exige A0, medido sobre <code>java.base</code>), mientras el gate estructural del verificador los rechaza y el intérprete no los despacha. Curiosamente <em>ejecutarlos</em> sería lo más fácil que queda —unas 10 líneas—; el costo está en una variante <code>ReturnAddress</code> que toca todos los <code>match</code> sobre <code>Value</code> y, sobre todo, en verificar subrutinas polimórficas, la parte más difícil del type-checker del JVMS. Razonamiento completo en la nota de diseño <code>subrutinas-jsr-ret.md</code>. Contarlos como faltantes mezclaría una decisión con una tarea: el número honesto es <strong>199 de 199 alcanzables — completo</strong>.</p>
<p><strong><code>invokedynamic</code> (0xba) — el subsistema.</strong> El opcode solo no hace nada: no nombra ningún método, sino un <em>bootstrap</em> que <strong>produce</strong> el destino la primera vez. Esa indirección es lo que le permitió a Java agregar lambdas, concatenación de strings y records sin inventar un opcode por cada una — el lenguaje pone la política en el bootstrap y la VM queda fija. Corren <strong>5 de las 6 fábricas</strong> que emite <code>javac</code>: <code>StringConcatFactory</code>, <code>SwitchBootstraps.typeSwitch</code> (patrones de tipo y de enum), <code>ObjectMethods</code> (los tres métodos de un <code>record</code>, desde una sola entrada de <code>BootstrapMethods</code> y distinguidos por el <em>nombre</em> del call site), <code>LambdaMetafactory.metafactory</code> y <code>ConstantBootstraps.invoke</code>. La sexta, <code>altMetafactory</code>, necesita serialización (<code>java.io</code>) y queda fuera de alcance. Dos decisiones de diseño: los <em>bootstrap methods</em> se modelan como <strong>intrínsecos</strong> —el mismo criterio de <code>intrinsecos.md</code>—, con <code>java.lang.constant</code> escrito en <strong>Java</strong>; y <code>LambdaMetafactory</code> <strong>genera una clase real</strong> al vuelo (con el escritor de <code>.class</code>): implementa la interfaz funcional, con campos para las capturas y un método SAM que reenvía al handle — igual que el JDK, que la spinnea con ASM. El objeto lambda es entonces una instancia común, despachada por el itable normal, sin atajo. Ruta, correcciones y mediciones: <code>invokedynamic-ruta.md</code>.</p>
<p><strong>La VM puede invocar Java (<code>call_java</code>).</strong> Empuja un frame propio y lo corre con un bucle de <code>run_one</code> anidado, devolviendo el resultado. Resultó ser el <strong>caso general de lo que la inicialización de clases venía haciendo</strong>: <code>&lt;clinit&gt;</code> era la variante sin argumentos ni resultado, así que el mecanismo ya existía entero y sólo estaba especializado. Importa porque <strong>los intrínsecos dejan de ser terminales</strong>: un nativo que sólo puede calcular y devolver está obligado a reimplementar lo que necesite de la biblioteca — así fue como <code>String.valueOf</code> terminó a medias en Rust. Con esto, <code>String.valueOf(Object)</code> llama al <code>toString()</code> del objeto, un <code>record</code> pregunta el <code>equals</code>/<code>hashCode</code> de sus componentes en vez de comparar referencias, y una constante dinámica ejecuta su bootstrap. Eso <strong>cerró 0xba</strong>: el <strong>modelo de objetos de <code>java.lang.invoke</code></strong> (<code>MethodHandle</code>/<code>MethodType</code>/<code>Lookup</code>) vive en Java, con <code>MethodHandle.invoke</code>/<code>invokeWithArguments</code> nativos —polimorfia de firma, interceptados antes de la resolución—, el único intrínseco que corresponde. El premio se cobró: <code>ConstantBootstraps.invoke</code> es <strong>ahora Java</strong> (<code>handle.invokeWithArguments(args)</code>), lo que obligó a hacer el <strong>cache de condy raíz de GC</strong>. El <code>ldc</code> de <code>MethodHandle</code>/<code>MethodType</code> —que <code>javac</code> nunca emite— se probó con <strong>class files hechos a mano por el escritor de <code>.class</code></strong> (su estreno). Con mirrors de primitivos (<code>int.class</code> → <code>Integer.TYPE</code>) y los kinds static/virtual/special/constructor, y <code>LambdaMetafactory</code> generando clases reales, <code>invokedynamic</code> queda completo.</p>
<p><strong><code>multianewarray</code> (0xc5).</strong> El último en cerrarse, y el que más enseña sobre el modelo de datos: <strong>Java no tiene arrays rectangulares</strong>. <code>new int[2][3]</code> no es un bloque plano sino dos <code>[I</code> colgando de un <code>[[I</code>, cada nivel un objeto real — por eso las filas se reemplazan por separado y <code>a[0].length</code> no tiene por qué igualar <code>a[1].length</code>. La instrucción lleva el descriptor del array <em>mismo</em> (<code>[[I</code>, no el elemento como <code>anewarray</code>) más un contador de dimensiones, porque sólo se materializan <strong>esos</strong> niveles: <code>new int[3][]</code> deja los internos en <code>null</code>. Los conteos se validan <em>todos</em> antes de alocar nada, para que un largo negativo en una dimensión posterior no deje un array a medio construir; los niveles superiores guardan referencias y sólo el más interno usa el ancho real del elemento (una fila <code>byte[]</code> ocupa un byte por elemento, no cuatro); y las referencias hijo se escriben por <code>store_reference</code>, nunca por un <code>write_u32</code> crudo, porque son exactamente los punteros <code>old→young</code> que el <em>remembered set</em> debe registrar. <strong>Ejecutarlo era la mitad del trabajo:</strong> hubo que modelarlo en el <code>transfer</code> del verificador, ya que un opcode sin modelar produce un <code>VerifyError</code> marcado <code>unsupported</code> — que por diseño hace que el llamador avise y siga, o sea que la clase habría corrido con el verificador claudicando en silencio.</p>
<p><strong>Green threads (Fase 0).</strong> <code>java.lang.Thread</code> + <code>Thread.start()</code> nativo, un scheduler <strong>cooperativo</strong> round-robin que conmuta en el safepoint (entre opcodes), un call stack por-thread, y el GC tomando las raíces de <strong>todos</strong> los threads. Verificado: dos workers intercalándose con <code>main</code> (que los espera por spin-wait) dan <code>100</code>, y el step-visualizer muestra los cambios de contexto, el spawn y la terminación de cada thread. Es el modelo de los <em>virtual threads</em> de Java 21 (scheduling en espacio de usuario), con un solo carrier.</p>
<p><strong>Hilos de SO + GIL (E1+E2).</strong> El substrato es un <strong>parámetro de aplicación</strong> (<code>JVM_THREADS</code>, como los <code>JVM_GC_*</code>): en modo <code>os-gil</code> cada <code>Thread.start()</code> lanza un <code>std::thread</code> real y la VM entera vive tras un <strong>GIL</strong> (<code>Arc&lt;Mutex&lt;JVM&gt;&gt;</code>) que se toma y se suelta <strong>por opcode</strong>, en el mismo safepoint que ya usaba el GC. El bloqueo es <code>park</code>/<code>unpark</code> de verdad (centralizado en <code>make_runnable</code>) para monitores, <code>wait</code>/<code>notify</code> y <code>join</code>, y <code>Thread.sleep</code> pasa a tiempo real. El GC corre seguro porque el GIL <em>es</em> el stop-the-world: un solo hilo toca la VM a la vez, así que heap, monitores y metaspace quedan correctos sin sincronización extra. Es el camino canónico — el de CPython y las JVM tempranas —: <strong>correcto pero todavía serializado</strong>. Validado con 4 tests de modo OS que dan el mismo resultado que en green, usando el intérprete cooperativo como oráculo de corrección. <strong>El costo real de este salto no fue el JIT sino el refactor de <em>ownership</em></strong>, y lo abarató el seam: toda escritura de referencia ya pasaba por <code>HeapService.store_reference</code>.</p>
<p><strong>Monitores.</strong> Cada objeto tiene su monitor intrínseco (lock reentrante con dueño + <em>blocked-set</em> + <em>wait-set</em>). <code>synchronized</code> funciona en sus dos formas: <strong>bloques</strong> (opcodes <code>monitorenter</code>/<code>monitorexit</code>) y <strong>métodos</strong> (<code>ACC_SYNCHRONIZED</code>, <em>sin</em> opcode — el VM toma el monitor del receptor o del <code>Class</code> al empujar el frame y lo suelta al popearlo, por <code>return</code> <em>o</em> por <em>unwind</em> de excepción, vía un único punto de release imposible de saltear). La señalización <code>wait</code>/<code>notify</code>/<code>notifyAll</code> libera el monitor (guardando el conteo de reentrada), parquea al hilo en el <em>wait-set</em> y lo re-adquiere al despertar. Verificado con exclusión mutua real (dos hilos × 100 incrementos → 200, contra el control sin lock que pierde updates) y el <em>handshake</em> productor/consumidor (<code>wait</code> → <code>notify</code> → 42). Después se cerró el resto: <code>IllegalMonitorStateException</code> (gate <code>owns_monitor</code> sobre <code>monitorexit</code>/<code>wait</code>/<code>notify</code>, JVMS §6.5), <code>Object.wait(long)</code> con <strong>timeout</strong> (deadline sobre <code>sleep_until</code>; reloj de opcodes en green, tiempo real en OS — demo <code>WaitTimeout</code> → 7) y el <strong>monitor <em>GC-safe</em></strong>: los monitores se keyean por offset del heap, así que al mover objetos (minor y compact) las claves se remapean por el <em>forward</em> del GC, y los monitores de objetos colectados se descartan — ya se puede compactar con monitores tomados (demo <code>GcMonitor</code> → 5, y el test se cuelga si se quita el remapeo). <strong>Pendiente del monitor</strong>: la interrupción del <code>wait</code> (llega con H1), biased/thin locks y detección de deadlock.</p>
<p><strong>Arquitectura en capas (estilo <em>service</em>).</strong> El heap se encapsuló tras un <strong><code>HeapService</code></strong> que es su <strong>único dueño</strong>: toda escritura de referencia pasa por un portón (<code>store_reference</code>) que aplica el <strong>write barrier</strong> de forma no-bypasseable — el seam donde irá la sincronización cuando los threads pasen a ser de SO. El runtime es la <strong><code>JVM</code></strong> (composition root) sobre <code>HeapService</code> + <code>MetaspaceService</code> + el sistema de threads.</p>
<p class="exito"><strong>✓ Siguiente:</strong> con <strong>H4 (JMM relajado)</strong>, <strong>H5 (CAS — <code>compareAndSwap</code> + los <code>Atomic*</code>)</strong> y el <strong>núcleo + primeras utilidades de H6</strong> (<strong>AQS</strong> exclusivo+compartido → <code>ReentrantLock</code>/<code>Condition</code>, <code>ReentrantReadWriteLock</code>, <code>Semaphore</code>, <code>CountDownLatch</code>, <code>CyclicBarrier</code>, <code>ArrayBlockingQueue</code>/<code>LinkedBlockingQueue</code>/<code>PriorityBlockingQueue</code>/<code>DelayQueue</code>, el framework <code>Executor</code> (<code>ThreadPoolExecutor</code> + <code>ScheduledThreadPoolExecutor</code>), <code>ConcurrentHashMap</code> y <code>CompletableFuture</code>) ya hechos y validados por el oráculo, sigue el <strong>volumen restante de <code>java.util.concurrent</code></strong> (deques, <em>resize</em>/lecturas lock-free del <code>ConcurrentHashMap</code>) y los <strong>locks finos por estructura</strong> (ver la sección «Concurrencia»). Los green threads quedan como substrato elegible y <strong>oráculo determinista</strong> de corrección (<code>green≡os-gil≡os</code>) — clave para cazar el <strong>bug residual</strong> del modo paralelo (referencia <em>stale</em> bajo GC intenso; guard parcial landeado, próximo paso ThreadSanitizer). El paralelismo de cómputo del LLM vive en <strong>kernels off-heap</strong> (rayon/CUDA).</p>
</div>

</body>
</html>`;

// Se escribe junto al script, sea cual sea la máquina donde se clone el repo.
const outHtml = require("path").join(__dirname, "Roadmap_JDK.html");
fs.writeFileSync(outHtml, html, "utf8");
console.log("HTML OK ->", outHtml, "(" + html.length + " bytes)");
