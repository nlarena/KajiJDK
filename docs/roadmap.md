# JVM en Rust — Roadmap y plan de aprendizaje

> Proyecto personal para **medir y exigir habilidades** implementando una JVM
> (máquina virtual de Java) desde cero en Rust.
> Objetivo: no competir con HotSpot, sino **cargar y ejecutar bytecode real**, subiendo
> por niveles donde cada uno es un checkpoint medible.
> Documento creado el 2026-06-01.

---

## Modo de trabajo

- El código lo escribo **yo** (esa es la prueba). Claude actúa como **revisor /
  desatascador**: al llegar a un hito o al trabarme, lo revisamos juntos (diseño, idioms
  de Rust, bugs sutiles).
- Lenguaje elegido: **Rust**, justamente porque el *ownership* obliga a decidir
  explícitamente quién es dueño del constant pool, los frames y el heap.

---

## Alcance

| Objetivo | ¿Entra? |
|---|---|
| Parsear `.class` y ejecutar bytecode | **Sí** (el corazón del proyecto) |
| Objetos, herencia, dispatch dinámico | Sí (Nivel 3) |
| Garbage collector simple (mark & sweep) | Aspiracional (Nivel 4) |
| JIT (bytecode → nativo) | **No** — territorio PhD, sueño lejano |
| Biblioteca estándar Java completa | **No** — se stubea lo mínimo |
| Certificación TCK | **No** — inviable para una persona |

---

## La escalera de hitos

### Nivel 0 — Parsear el `.class` (reimplementar `javap`)
El `.class` es un binario **big-endian** muy bien especificado.
1. Leer el archivo a `Vec<u8>`.
2. `Reader` con índice que avanza, exponiendo `u1/u2/u4`
   (`u16::from_be_bytes`, `u32::from_be_bytes`).
3. Validar magic `0xCAFEBABE`, leer `minor`/`major`.
4. Modelar el **constant pool** como `enum` (`Utf8`, `Class{name_index}`,
   `Methodref{...}`, `Integer(i32)`, …).
5. Volcar todo a texto.

**Criterio de éxito:** el volcado coincide con `javap -v` sobre el mismo `.class`.

> Trampas clásicas del constant pool: es **1-indexed**, y las entradas `Long`/`Double`
> **ocupan dos slots**.

### Nivel 1 — Intérprete mínimo
Frame de ejecución (operand stack + variables locales) y un puñado de opcodes:
`iconst`, `iload`, `istore`, `iadd`, `return`.
**Éxito:** ejecutar un método que sume dos enteros.

### Nivel 2 — Control de flujo y métodos
Saltos (`if_icmpgt`, `goto`), `invokestatic`, pila de frames.
**Éxito:** correr un **factorial recursivo** o **fibonacci**.

### Nivel 3 — Objetos y heap
`new`, `getfield`/`putfield`, `invokevirtual`, dispatch dinámico, heap propio.
**Éxito:** crear objetos, llamar métodos de instancia, herencia simple.

### Nivel 4 — Lo difícil de verdad
- **Native methods / bootstrap:** enganchar lo mínimo de `java.lang`/`java.io`
  (p. ej. para `System.out.println`).
- **Garbage collector:** empezar con "no liberar nada" → mark & sweep simple.
- **JIT:** fuera de alcance por ahora.

### Cómo se mide
- Nivel 2 en solitario → sólido en sistemas/bajo nivel.
- Nivel 3 limpio y extensible → muy buen diseño de software.
- Nivel 4 con GC propio → territorio de poca gente.

---

## Estructura del proyecto (binario Cargo)

```
jvm/
├─ Cargo.toml
└─ src/
   ├─ main.rs                # CLI: recibe un .class y lo ejecuta
   ├─ classfile/
   │   ├─ mod.rs             # struct ClassFile + parser
   │   ├─ constant_pool.rs   # enum ConstantPoolEntry
   │   └─ reader.rs          # cursor sobre &[u8], lectura big-endian
   ├─ interpreter/
   │   ├─ frame.rs           # operand stack + locals
   │   └─ engine.rs          # loop de opcodes
   └─ runtime/
       └─ heap.rs            # Nivel 3+
```

---

## Gotchas de Rust específicos

- **No empezar con `nom` ni `byteorder`** — escribir el cursor a mano la primera vez; es
  donde se aprende. Refactorizar después.
- El constant pool con referencias cruzadas pelea con el *borrow checker*: lo más simple
  al inicio es resolver índices **bajo demanda** (guardar `u16`, resolver al usar), no
  punteros entre entradas.
- Para el heap del Nivel 3, lo pragmático en Rust suele ser **arena + índices**
  (`Vec<Object>` y `usize` como "puntero"), evitando `Rc<RefCell<...>>` por todos lados.

---

## Generar clases de prueba

Escribir un `Add.java` mínimo y compilarlo con cualquier JDK ya instalado:
```
javac Add.java
```
y usar el `.class` resultante como entrada de la JVM.

---

## Recursos

- **The Java Virtual Machine Specification** — capítulos 4 ("The class File Format") y
  6 ("The JVM Instruction Set"). Es *la* referencia.
- Libros/repos educativos tipo *"Build Your Own JVM"* en Rust/Go/C como mapa de ruta.

---

## Próximo paso

Levantar el proyecto Cargo y atacar el **Nivel 0** (parser + volcado estilo `javap`).
Opcional: arrancar de un **esqueleto vacío** (módulos con `todo!()`) o desde cero.

---

# Roadmap del JDK completo (alcance ampliado 2026-06-01)

> El alcance creció: ya no es "solo la JVM" sino el **JDK completo** = JVM + compilador
> + bibliotecas. Decisión: el **compilador se escribe en Rust** (rompe el bootstrap).
> Orden de construcción: la **JVM va primero** (todo lo demás es inerte sin un motor que
> lo ejecute). Nada queda descartado; solo hay horizontes distintos.

**Leyenda de horizonte:**
- 🟢 **Base** — el núcleo, por aquí empezamos
- 🔵 **Avanzado** — más duro, segunda pasada
- 🟣 **Cumbre** — lo más difícil, pero vamos a llegar

**Cómo leer este roadmap:** es una **ruta ordenada por hitos**. Cada hito tiene un
*criterio de éxito medible* y no se considera cerrado hasta cumplirlo. Las casillas
`- [ ]` son las piezas concretas de ese hito; márcalas a medida que avanzas. El orden
respeta las dependencias: no se puede el hito N sin el N-1.

```
A · JVM ──→ B · Compilador ──→ C · Bibliotecas ──→ E · Cerrar el círculo
(motor)     (.java → .class)   (en Java)           (todo junto)
   └── D · Herramientas se va completando en el camino (javap, java, jar...)
```

---

## FASE A — La JVM (el motor que ejecuta bytecode)

### Hito A0 · Parsear el `.class` (≡ `javap`) 🟢 — ✅ núcleo logrado
Leer el binario y volcarlo a texto. No se ejecuta nada todavía, solo se *entiende*.
- [x] Lector de bytes (cursor big-endian) — el `Reader` (`u1`/`u2`/`u4`)
- [x] Constant pool (las 17 clases de entrada; ojo 1-indexed y Long/Double = 2 slots)
- [x] Header: magic, versiones, flags, this/super/interfaces
- [x] Parseo de fields, methods, attributes
- [x] Atributos clave: `Code`, `LineNumberTable`, `SourceFile`, **`StackMapTable` (los 7 frame types)**
- [x] Desensamblado de bytecode (tabla de opcodes completa) con comentarios `// …` resueltos
- [x] Volcado estilo `javap`: **brief y `-v` byte-idénticos** (incl. cabecera Classfile/SHA-256)
- [x] Flags de visibilidad de CLI: `-public` / `-protected` / `-package` / `-p` / `-private`
- **✅ Éxito alcanzado:** el volcado coincide **byte a byte** con `javap -v` (y `javap` brief) sobre 12 fixtures.

**Pendiente (atributos no esenciales — el `.class` se parsea entero, pero estos aún se muestran crudos o se omiten):**

| Aparece cuando… | Atributo |
|---|---|
| genéricos | `Signature` (reescribe la línea de declaración → requiere un parser de la gramática de firmas genéricas) |
| lambdas / `invokedynamic` | `BootstrapMethods` |
| clases internas / anónimas | `InnerClasses`, `EnclosingMethod` |
| `throws` | `Exceptions` |
| `final int X = 5` | `ConstantValue` |
| anotaciones | `RuntimeVisible/InvisibleAnnotations`, … (`element_value` es recursivo) |
| records | `Record` |
| `sealed` | `PermittedSubclasses` |
| nests (Java 11+) | `NestHost`, `NestMembers` |
| debug (`javac -g`) | `LocalVariableTable`, `LocalVariableTypeTable` |

> También pendiente, pero **cosmético**: los flags de *contenido* de javap (`-c`, `-l`, `-s`),
> que exigen refactorizar la salida en secciones componibles. Ninguno de estos pendientes
> bloquea avanzar al intérprete.

### Hito A1 · Intérprete mínimo 🟢
El motor base: un frame y un puñado de opcodes aritméticos.
- [x] *Frame*: pila de operandos + variables locales
- [x] Contador de programa (PC) y *loop* de despacho de opcodes
- [x] Opcodes: `iconst`, `iload`, `istore`, `iadd`, `return`/`ireturn`
- [x] Parseo de descriptores de método (`(II)I`)
- **✅ Éxito:** ejecutar un método que sume dos enteros.

### Hito A2 · Control de flujo y métodos 🟢
- [x] Saltos: `if_icmpgt`, `goto`, comparaciones
- [x] *Method area* (metadatos de clases cargadas)
- [x] `invokestatic` + pila de frames (llamadas anidadas)
- **✅ Éxito:** correr un **factorial recursivo** o un **fibonacci**.

### Hito A3 · Objetos y heap 🟢
- [x] Heap + allocator simple ("no liberar nada"); arena de bytes (`Vec<u8>`) + `malloc` bump
- [x] Representación de objetos (*layout* de campos, con herencia) y de clases (con *vtable*)
- [x] `new`, `getfield`/`putfield`
- [x] `invokevirtual`/`invokespecial`/`invokeinterface` + *dispatch* dinámico (vtable + itable)
- [x] Arrays (objetos y primitivos int-category, con ancho fiel; `long`/`double` → pendiente)
- **✅ Éxito:** crear objetos, herencia simple, llamar métodos de instancia con dispatch dinámico.

### Hito A4 · Robustez del runtime 🟢
- [x] Excepciones: `athrow` + tablas de excepción + *stack unwinding*
- [x] *Linking*: resolución de símbolos bajo demanda (+ verificador de bytecode con *StackMapTable*)
- [x] Inicialización de clase (`<clinit>`, perezosa, super-first)
- [x] Jerarquía de class loaders (bootstrap/app + delegación)
- Robustez fina (conceptos de A4; varios ya cerrados):
  - [x] Excepciones **implícitas**: `NullPointerException` (campos + invokes + arrays), `ArrayIndexOutOfBoundsException`, `NegativeArraySizeException`, `ClassCastException`, `ArithmeticException` (división entera por cero) — la VM las sintetiza y lanza.
  - [x] `finally` (catch-all + re-throw del compilador — verificado, sin opcode nuevo)
  - [x] Errores de **linkage** como excepciones: `NoClassDefFoundError` / `NoSuchMethodError` en los invokes
  - [x] **`ExceptionInInitializerError`** (JVMS §5.5) — un `<clinit>` que lanza ahora **propaga** su fallo al código que disparó la init (antes se tragaba y el `getstatic` devolvía 0), lo **envuelve** si no es un `Error`, y deja la clase **`Erroneous`** → un segundo uso lanza `NoClassDefFoundError`. Mecánica: un *floor* de unwinding por `call_java` frena la excepción en el frame sintético del `<clinit>` (`pending_exception`), y los opcodes que disparan init (`new`/`getstatic`/`putstatic`/`invokestatic`/handle) la re-lanzan. Verificado `green≡os-gil≡os`
  - [x] **Stack trace + `getMessage`/`toString` en `Throwable`** — `message` + constructores `(String)` en toda la jerarquía; la VM captura una **traza** (`"\tat pkg.Class.method"`, innermost-first) en el `unwind` —punto único por el que pasan faults implícitos y `throw`— y la guarda en el objeto (interned de forma GC-safe); `printStackTrace()` la imprime. Verificado `green≡os-gil≡os`
  - [ ] `ConstantValue` (las constantes de compilación no disparan init) — **no testeable con javac**: javac *inlinea* toda constante compile-time (`static final` primitivo/String) en el sitio de uso, así que nunca se emite un `getstatic` que lo observe; requeriría class files a mano
  - [ ] Chequeos de **acceso** (`IllegalAccessError`) — **no producible con javac** (nunca compila un acceso ilegal); requeriría class files a mano
  - [ ] Identidad `(nombre, loader)` (re-key por loader definidor) — profundo, valor observable bajo con un solo class path
- **✅ Éxito:** un `try`/`catch` atrapa una excepción y la inicialización de clases ocurre en el orden correcto.

### Hito A5 · Nativos + GC 🔵 — ✅ logrado
- [x] Puente a métodos nativos (lo mínimo para I/O real) — `System.out.println` imprime de verdad
- [x] *Intrinsics* (lo que Java no puede hacerse a sí mismo) — `getClass`, `hashCode`, `Math`, `arraycopy`, `String`/`Class`…
- [x] Garbage collector mark & sweep — y más: **compactante**, free list con *coalescing*, política de fragmentación, 4 disparadores sobre *safepoint*, y **marcado transitivo** correcto.
- **✅ Éxito alcanzado:** `System.out.println` imprime y el GC recolecta (y compacta) basura.

### Hito A6 · Cumbre del runtime 🟣 — 🚧 en progreso
- [x] **Verificador de bytecode** (seguridad de tipos antes de ejecutar) — cobertura completa del set de opcodes que ejecuta el intérprete (objetos, arrays, invokes, categoría-2, conversiones, comparaciones).
- [x] **Sistema de tipos completo** — `int`/`long`/`double`/`float` ejecutados *y* verificados: cómputo, conversiones, comparaciones, división con excepción, categoría-2 (params/campos/estáticos/arrays/frames), y el **lattice de referencias** (covarianza de arrays + `join`/LUB).
- [x] GC **compactante** (mover + reescribir punteros) — hecho ya en A5.
- [x] GC **generacional** (young Eden+survivors por copia / Old; write barrier + remembered set para raíces `old→young`)
- [x] **Referencias débiles** (`java.lang.ref`: `WeakReference` + `ReferenceQueue`)
- [x] **Hilos, monitores, `synchronized`** — green threads cooperativos (default + visor) **y** substrato **hilos de SO + GIL** (`JVM_THREADS=os-gil`, E1+E2): `std::thread` por `Thread.start()`, `park`/`unpark`, `wait`/`notify`/`join`, IMSE, `wait(timeout)` y **monitor GC-safe** (las claves se remapean por el *forward* del GC en minor/compact); GC seguro bajo el GIL
- [x] **API de `Thread` (H1)** — `currentThread`/`yield`/nombre/id/`isAlive`, `getState()` con los seis estados (`ThreadStatus` a cinco + `NEW` derivado, un punto único de bloqueo), `Thread(Runnable)` (una lambda lo satisface), `start` dos veces → `IllegalThreadStateException`, e `interrupt`/`InterruptedException` que despierta `sleep`/`join`/`wait` (re-adquiere el monitor antes de lanzar en el `wait`; la carrera notify/interrupt la resuelve el GIL). **Periféricos (H1 loose ends):** **`ThreadLocal`** con aislamiento por-hilo real (lista de asociación colgada del `Thread` actual, keyed por identidad `==` GC-safe; sin locks porque cada hilo sólo toca su propia lista) — verificado con 4 hilos OS —, **`priority`** (get/set + validación de rango `[1,10]`, hint advisory como en la spec) y **`daemon`** (get/set + regla de ciclo de vida; la VM termina cuando `main` retorna, así que es atributo, no driver de shutdown); motivó agregar `java.lang.IllegalArgumentException`
- [x] **Sacar el GIL (E3/H3) — paralelismo real, ensanchado (W1/W2/W3)** — `JVM_THREADS=os`: (W1) los opcodes **frame-local** (aritmética int/long/float/double, shifts, conversiones, refs, ramas) corren **lock-free en paralelo** sobre un `RunningCtx` por-hilo con code cache; (W3) las ops compartidas de **solo-lectura** (`getfield`/`arraylength`/array-loads) corren concurrentes bajo un **`RwLock`** (`.read()`); (W2) la **asignación** (`new`/`newarray`) es **lock-free**, con Eden en un arena `UnsafeCell` **Miri-verificado**. El GC que mueve objetos frena a todos con un **safepoint stop-the-world cooperativo** (`gc_pending` + `park`/`unpark`). El `Arc<Mutex<JVM>>` de toda-la-VM **desapareció** (`SharedVm` tras un `RwLock`); `os-gil` queda como referencia serializada + oráculo. Verificado por el oráculo (`os` ≡ `os-gil` ≡ green) + stress test + Miri (el `unsafe` es sound). **Falta:** locks por-estructura finos. Diseño: `H3_ownership.md`
- [x] **Modelo de memoria de Java (H4)** — **relajado y fiel**: campos no-volatile **lock-free** (`Relaxed`), `volatile` = `Acquire`/`Release`, no-*tearing* de `long`/`double` (`AtomicU64`), Eden sobre un substrato atómico (`AtomicRegion`) **Miri-verificado**; *happens-before* vía los átomicos + el `RwLock` + monitores + `start`/`join`. El cache de condy se hizo raíz de GC. Test de publicación `volatile` `green≡os-gil≡os`. Falta: semántica de `final`, `VarHandle`
- [x] **Atómicos / CAS (H5)** — `compareAndSwap` intrínseco (`cas_u32`/`cas_u64` sobre el `AtomicRegion`, `AcqRel`) + `AtomicInteger`/`AtomicLong`/`AtomicReference` (la CAS de referencia pasa por el write barrier). Verificado (`AtomicLong` 64-bit + `AtomicReference` identity-CAS → 202)
- [x] **`java.util.concurrent` — núcleo + utilidades (H6)** — **AQS** (`AbstractQueuedSynchronizer`) en modo **exclusivo y compartido** (cola Treiber + wake-all), con `ReentrantLock`/`Condition`, **`ReentrantReadWriteLock`** (state partido read/write), `Semaphore`, `CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue` (productor/consumidor acotado) el **framework `Executor`** (`Executor`/`ExecutorService` + un **`ThreadPoolExecutor`** de tamaño fijo con workers sobre la cola y shutdown por *poison-pill* + `submit`/`Future`/`FutureTask`/`Callable`) y un **`ConcurrentHashMap`** (encadenamiento separado + *lock striping* por franjas de `ReentrantLock`; usó agregar `Object.equals`) y un **`LinkedBlockingQueue`** (cola enlazada con el diseño clásico de **dos locks** —putLock en la cola, takeLock en la cabeza— para que productor y consumidor no contiendan) y un **`ScheduledThreadPoolExecutor`** (tareas con delay/periódicas sobre un min-heap por deadline + un worker con espera temporizada; usó agregar el intrínseco nativo `System.nanoTime`) y un **`CompletableFuture`** (resultado async con `complete`/`get`/`join`, encadenamiento `thenApply`/`thenRun`/**`thenCompose`** —este último aplana `CF<CF<U>>`—, completación **excepcional** (`completeExceptionally` + captura de `Throwable` en `supplyAsync`) con **`exceptionally`** (recuperación) y **`thenCombine`** (fusiona dos futuros con un `BiFunction`, gate `AtomicInteger`), y `supplyAsync`/`runAsync` sobre un `Executor`; usó agregar `java.util.function.Supplier`/`Function`/`BiFunction` y `java.util.Objects`) y un **`PriorityBlockingQueue`** (min-heap sobre un `ReentrantLock` + `notEmpty`, orden por `Comparator`/`Comparable`; usó agregar `java.util.Comparator` + `java.lang.Comparable`) y un **`DelayQueue`** (min-heap de elementos `Delayed`; `take` hace una espera temporizada hasta que expira la cabeza, con `System.nanoTime`), todo **en Java** sobre AQS/monitor y validado por el oráculo (`green≡os-gil≡os`). Falta el **volumen** restante: deques (`ArrayDeque`/`ConcurrentLinkedDeque`), *resize* del `ConcurrentHashMap` + lecturas lock-free y los locks por-estructura finos
- [ ] **Bug residual os-parallel** — bajo **GC intenso concurrente** (`JVM_THREADS=os`) una referencia *stale* puede alcanzar un frame (repro `gc_race_stress` → receptor corrupto); se acotó y arregló un mecanismo (guard `had_local_frames` en el safepoint), pero queda **un segundo mecanismo abierto** (data race de bajo nivel en la integración; todos los invariantes lógicos y el remap del GC son correctos). Próximo paso: **ThreadSanitizer**. `green`/`os-gil` (el oráculo) **no** afectados; `os` documentado como experimental
- [x] **Cobertura del set de opcodes — 199/199 alcanzables: completo** — cerrados `nop` (0x00), `goto_w` (0xc8), el prefijo `wide` (0xc4, índices de local de 16 bits: `wide iinc` mide 6 bytes, el resto 4), `multianewarray` (0xc5, alocación recursiva de sólo los `dimensions` niveles indicados, modelado **también en el verificador**) e `invokedynamic` (0xba). Los 3 de `jsr`/`ret`/`jsr_w` quedan **excluidos por diseño** (JVMS §4.9.1 los prohíbe en class files de versión 50.0+, o sea Java 6 en adelante), con la postura **leer sí, ejecutar no**: el desensamblador los soporta completo —requisito de A0— y el gate estructural del verificador los rechaza. Nota de diseño: `subrutinas-jsr-ret.md`.
- [x] **`invokedynamic`** (0xba) — **no era un opcode, era un subsistema**, y corre: **5 de las 6 fábricas** que emite `javac`. Concatenación de strings (`StringConcatFactory`), `switch` sobre patrones de tipo *y* de enum (`SwitchBootstraps.typeSwitch`), `equals`/`hashCode`/`toString` de records (`ObjectMethods`), lambdas y method references (`LambdaMetafactory.metafactory`), y constantes dinámicas (`ConstantBootstraps.invoke`). La sexta, `altMetafactory`, necesita serialización y queda fuera de alcance. Ruta, correcciones y mediciones: **`invokedynamic-ruta.md`**
- [x] **`ldc` de literales de clase** (`Foo.class`, `int[].class`) — empuja el mirror, cacheado por Class ID, y sin inicializar la clase (un literal no es *uso activo*, §5.5)
- [x] **La VM puede invocar Java** (`call_java`) — empuja un frame propio y lo corre con un bucle anidado, devolviendo el resultado. Era el caso general de lo que ya hacía `<clinit>`. **Los intrínsecos dejan de ser terminales**: es lo que permite que `String.valueOf(Object)` llame al `toString()` del objeto, que un record pregunte el `equals`/`hashCode` de sus componentes, y que un condy ejecute su bootstrap
- [x] **Modelo de objetos de `java.lang.invoke`** (`MethodHandle`/`MethodType`/`Lookup`) — **cierra 0xba**: vive en Java (`bootstrap/java/lang/invoke/`), con `MethodHandle.invoke`/`invokeWithArguments` **nativos** (polimorfia de firma, §2.9.3, interceptados en `invokevirtual` antes de la resolución normal). `ConstantBootstraps.invoke` es **ahora Java** (`handle.invokeWithArguments(args)`), lo que exigió el **cache de condy raíz de GC**; el `ldc` de `MethodHandle`/`MethodType` —que `javac` nunca emite— se probó con **class files hechos a mano por el escritor de `.class`**. Kinds static/virtual/special/constructor + mirrors de primitivos (`int.class` → `Integer.TYPE`), y `LambdaMetafactory` **genera clases reales** al vuelo. Detalle en `invokedynamic-ruta.md`
- [ ] JIT (bytecode → código nativo)
- **✅ Éxito (parcial):** verificación de tipos completa, GC generacional, set de opcodes completo (incluido `invokedynamic`), y **concurrencia con paralelismo real** (GIL removido, ensanchado W1/W2/W3) — con el **JMM relajado (H4)**, **CAS (H5)** y el **núcleo de `java.util.concurrent` (H6)** sobre AQS (`ReentrantLock`/`Semaphore`/`CountDownLatch`) ya hechos y validados por el oráculo. Falta el **volumen de j.u.c** (pools, colecciones), los locks finos, resolver el **bug residual del modo `os`** (ver arriba) y/o el JIT. *Detalle en los informes `Concurrencia_KajiJDK.pdf` e `invokedynamic-ruta.md`.*

### Hito A7 · Conformidad JVMS 🟣 — auditoría 2026-08-12 (detalle: `holdon.md`)

> Gaps del runtime encontrados auditando contra la JVM Specification y los JSR
> estructurales (JSR 335 default methods, JSR 201 autoboxing, JVMS §2.10/§6.3/§6.5).
> Ordenados por prioridad; todos testeables con javac salvo indicación. **Pre-JIT.**

- [x] **Default methods de interfaces (JSR 335, Java 8)** — estaba **ROTO** (`NoSuchMethodError`: `build_vtable` heredaba solo de la superclase). Arreglado: la vtable ahora fusiona los defaults de las superinterfaces en BFS desde las directas — una interface más específica se visita antes que las que extiende (regla *maximally-specific* para todo lo que emite javac; el diamante sin override no llega: javac no lo compila), la clase siempre gana, y un método abstracto (sin `Code`) nunca toma slot. Cubre `invokeinterface` **y** `invokevirtual` (receptor tipado por la clase). Los static de interface ya funcionaban. Test `DefProbe` → 115 (`green≡os-gil≡os`)
- [x] **Excepción no atrapada → `panic!` del VM (JVMS §2.10)** — estaba **ROTO** (una excepción sin catch hacía `panic!` de toda la VM). Arreglado: la VM imprime a la consola el formato exacto del JDK real (`Exception in thread "Thread-1" java.lang.RuntimeException: uncaught in worker` + la traza `\tat ...` capturada en el throw) y termina **solo ese thread** (`Step::Return(None)` = el return final del thread: los joiners despiertan, `main` sigue; si es `main`, termina el programa). Test `UncTest` → 42 (worker lanza sin catch, main hace join y comprueba TERMINATED), `green≡os-gil≡os` + 10× os-parallel. La API `Thread.setUncaughtExceptionHandler` queda pendiente
- [x] **`ArrayStoreException` (JVMS §6.5 `aastore`)** — `aastore` ahora hace el chequeo dinámico: clase runtime del valor vs tipo de elemento del array (`is_subtype`, covarianza incluida); null siempre válido, primitivos intactos. Test `AsTest` → 42 (`green≡os-gil≡os`). **Bonus — dos bugs latentes que el chequeo expuso, arreglados:** (1) los **mirrors sintéticos de clases array se alocaban en Eden** — el minor GC los movía/recolectaba dejando el índice del metaspace y los headers apuntando a un slot reutilizado por OTRO mirror (un store válido de `[J` en `[[J` lanzaba ASE espurio); ahora van a **Old** como todo mirror (invariante "Old-pinned"); (2) `anewarray` nombraba los arrays anidados `[L[J;` en vez de `[[J` — corregido en el intérprete (write+read path) y el verificador. Test de regresión `nested_array_stores_survive_minor_gc` → 64
- [x] **`StackOverflowError` (JVMS §6.3)** — límite `MAX_FRAMES = 2000` chequeado en `push_frame_locked` (el embudo de los 5 invokes), **antes** de adquirir el monitor (un overflow en un método `synchronized` no filtra el lock); `call_java` (frames sintéticos) cubierto vía `pending_exception`. Test `SoTest` → 42 (`green≡os-gil≡os`)
- [x] **Autoboxing / wrappers (JLS §5.1.7, JSR 201)** — los 8 wrappers reales en bootstrap: `Integer`/`Long`/`Short`/`Byte` con `valueOf` + cache −128..127 (identidad `==` para valores chicos, como exige la JLS — verificado: `100==100` true, `200==200` false), `Character` (cache 0..127), `Boolean` (TRUE/FALSE canónicos), `Double`/`Float` sin cache; `xxxValue`/`equals`/`hashCode`. Test `BxTest` → 42 (`green≡os-gil≡os`). El test destapó un **segundo reproducer del heisenbug os-parallel** (single-thread, ~50%: `Long.<clinit>` con Eden casi lleno → `ArithmeticException` espuria) — conservado en `java/BxDbg{T,Y}.java`, anotado en la memoria del bug
- [x] **`Object.clone()` + `Cloneable`** — copia shallow interceptada en `invokevirtual` (post-resolución) e `invokespecial` (`super.clone()`, el patrón estándar), con chequeo de `Cloneable` → `CloneNotSupportedException`; **arrays incluidos** (`"[I".clone()` interceptado pre-vtable — las clases array sintéticas no tienen). El clon se aloca en **Old** (una alocación Eden podría disparar un minor que mueva el fuente a mitad de copia); referencias vía write barrier. Test `CnTest` → 42 (los 4 caminos) (`green≡os-gil≡os`)
- [x] **`OutOfMemoryError` (JVMS §6.3)** — el agotamiento del heap es **recuperable para las alocaciones de bytecode** (`new`/`newarray`/`anewarray`/`multianewarray` → `try_malloc` → throw); las alocaciones internas del VM (intern, mirrors, promociones) conservan el panic, documentado. Test `OmTest` → 42 (recursión rooteando 512 KiB/frame agota los 16 MiB de verdad) (`green≡os-gil≡os`)
- [x] **Reflexión mínima: `Class.getName()`/`getSimpleName()`** — nativos sobre el mirror (`class_name_at_mirror`), formato JDK completo: `java.lang.String`, arrays `[Ljava.lang.String;` (getName) / `String[]`/`int[]` (getSimpleName, con dimensiones). Test `GnTest` → 42 (incl. `String.class.getName()` vía ldc de Class) (`green≡os-gil≡os`)
- [x] **Anotaciones en runtime (JSR 175)** — `Class.isAnnotationPresent(Class)` nativo sobre el `RuntimeVisibleAnnotations` ya parseado (hubo que sumar `java/lang/annotation/Annotation` al boot: sin ella el `.class` de una anotación ni carga). Test `AnTest` → 42. **Fuera de alcance:** `getAnnotation()` devolvería un *objeto* anotación → exige clases proxy por `@interface` (dynamic proxies + modelo de `Method`, que no existen); tampoco anotaciones de campos/métodos, `@Inherited` ni `RuntimeInvisibleAnnotations`
- [x] **`java.lang.ref`: `SoftReference` + `PhantomReference`** — phantom con `get()` → `null` siempre pero `referent` heredado intacto (el GC detecta la muerte y encola; el override impide resucitar). La política soft vive en el **GC** (`SoftPolicy::{Retain, Clear}`): el referent se traza **fuerte** salvo en una colecta pedida por **presión** de memoria (ocupancia/out-of-space/tasa de alocación), donde muere como un weak. Verificado en ambas direcciones (`RfTest` → 42 por defecto; → 36 forzando presión). **`Cleaner` fuera de alcance**; `finalize()` **no se implementa por decisión**
- [x] **Ciclo de vida: `System.exit(int)` + `Runtime`** — interceptado en `invokestatic` (el bridge de nativos devuelve `Option<Value>`, solo sabe *continuar*; terminar exige un `Step`); `vm_exit` descarta los frames **sin** liberar monitores ni desenrollar —el apagado ordenado que `exit` no debe hacer—, despierta a los parkeados y propaga el status desde cualquier thread. Test `ExTest` → 42, contrastado con el `java` real (exit code 42, sin correr el `finally`). **Shutdown hooks fuera de alcance** (un hook es un `Thread` que habría que *correr* en el camino que deja de ejecutar bytecode)
- [x] **Periféricos de `Thread`** — **`UncaughtExceptionHandler` invocado de verdad en Java** (`dispatch_uncaught` → `call_virtual`), corriendo **antes** de popear el último frame: el lookup aloca y esa es la única raíz de GC que sostiene la excepción; un handler que lanza queda contenido por el `exception_floor` y se imprime el reporte de la excepción original. Más `sleep(long,int)`, `onSpinWait()` y `ThreadGroup` (registro enteramente en Java, cero bookkeeping en el VM). Test `UhTest` → 42, igual que el JDK real
- [x] **Regla fina de init de interfaces (§5.5)** — **era un bug**: `ensure_initialized` solo recorría superclases, así que el `<clinit>` de una interface con default methods nunca corría (directo *ni* indirecto). `default_method_superinterfaces()` hace BFS quedándose con las que declaran un método con `Code`, y **devuelve vacío si la clase es una interface** — la regla "una interface no inicializa sus superinterfaces" queda en el tipo de retorno, no en un `if`. Test `IiTest` → 42
- **Cubierto (auditado ✅):** class file + 199/199 opcodes, verificador `StackMapTable`, JMM (JSR 133), `invokedynamic` 5/6 (JSR 292), j.u.c núcleo (JSR 166), records/pattern-switch. Módulos (JSR 376) y NIO (JSR 51/203): fuera de alcance declarado.

---

## FASE B — El compilador (`javac`, escrito en Rust)

> **Al día — el compilador está construido (fusionado desde el pendrive):** B0 (lexer),
> B1 (parser, lenguaje completo), B2 (análisis semántico: pasadas 1 y 2 + chequeos),
> B3 (generación de bytecode con `StackMapTable`) y B4 (compilador robusto) están
> **hechos** — el `javac` propio compila objetos/bucles/excepciones y el `.class` corre
> en la JVM (`fib(10)=55`, `fact(5)=120`) y pasa el verificador JVMS-estricto. Falta B5
> (cumbre: genéricos completos). El detalle por hito está en `Roadmap_JDK.pdf`. Las
> casillas de abajo son el plan original.

### Hito B0 · Lexer 🟢
- [ ] Scanner: texto `.java` → tokens (palabras clave, identificadores, literales, símbolos)
- **✅ Éxito:** tokeniza `Add.java` sin perder ni inventar tokens.

### Hito B1 · Parser 🟢
- [ ] Gramática → AST (clases, métodos, sentencias, expresiones)
- [ ] Tabla de símbolos / *scopes*
- **✅ Éxito:** produce un AST correcto de `Add.java`.

### Hito B2 · Análisis semántico 🟢
- [ ] Resolución de nombres (qué es cada identificador)
- [ ] *Type checking* (chequeo de tipos)
- **✅ Éxito:** acepta `Add.java` y rechaza un programa con error de tipos.

### Hito B3 · Generación de bytecode 🟢
- [ ] AST → bytecode
- [ ] Construcción del constant pool
- [ ] Escritor de `.class` (emisor del binario)
- **✅ Éxito:** tu `javac` compila `Add.java` y el `.class` resultante corre en **tu** JVM dando el mismo resultado que el de `javac` real.

### Hito B4 · Compilador robusto 🔵
- [ ] *Overload resolution* y chequeo de *override*
- [ ] Análisis de flujo (asignación definitiva, alcanzabilidad)
- [ ] Inferencia de tipos (`var`)
- [ ] Generación de `StackMapTable` (la exige el verificador moderno)
- **✅ Éxito:** compila programas con sobrecarga, herencia y flujo no trivial.

### Hito B5 · Cumbre del compilador 🟣
- [ ] Genéricos completos (*type erasure*, *wildcards*, inferencia)
- **✅ Éxito:** compila código genérico equivalente al de `javac`.

---

## FASE C — Las bibliotecas (escritas en Java, compiladas por tu `javac`)

### Hito C0 · Núcleo de `java.lang` 🟢
- [ ] `Object`, `String`, `System`, wrappers (`Integer`...), `Math`, `StringBuilder`
- **✅ Éxito:** un programa que usa `String` y `System.out` corre en tu JVM.

### Hito C1 · Excepciones 🟢
- [ ] Jerarquía `Throwable`/`Exception`/`RuntimeException`
- **✅ Éxito:** lanzar y atrapar excepciones de la biblioteca propia.

### Hito C2 · Colecciones e IO 🔵
- [ ] `java.util`: `List`, `ArrayList`, `Map`, `HashMap`
- [ ] `java.io`: `InputStream`/`OutputStream`/`PrintStream`
- **✅ Éxito:** un programa que usa `ArrayList`/`HashMap` corre.

### Hito C3 · Cumbre de la biblioteca 🟣
- [ ] Resto de `java.base` (net, nio, time, reflexión completa...)

---

## FASE D — Herramientas (la "DK" = *Development Kit*)

> Se completan en el camino, no en bloque.
- [ ] 🟢 `javap` (desensamblador) — se logra con el Hito A0
- [ ] 🟢 `java` (lanzador que arranca la JVM) — se logra durante la Fase A
- [ ] 🟢 `javac` (compilador) — es toda la Fase B
- [ ] 🔵 `jar` (empaquetador de `.class`)
- [ ] 🟣 `jdb` (debugger), `javadoc`, `jlink`/`jmod`, `keytool`...

---

## FASE E — Cerrar el círculo 🟣

El momento épico: las tres piezas funcionando juntas.
- [ ] Compilar las bibliotecas (Fase C) con tu propio `javac` (Fase B)
- [ ] Ejecutar un programa real que use esas bibliotecas en tu propia JVM (Fase A)
- [ ] Conformance / compatibilidad (la cumbre lejana: comportamiento fiel a la spec)
- **✅ Éxito:** un `.java` que escribes → lo compila tu `javac` → usa tus bibliotecas → corre en tu JVM, sin tocar nada del JDK de Temurin.

---

# Más allá del JDK — la JVM como plataforma (alcance ampliado 2026-06-01)

> La JVM no es un fin en sí mismo: es la **carrocería** de un proyecto más grande.
> Como construimos VM **+** compilador propios, controlamos el stack entero y
> podemos **inventar** más allá de lo que `javac`/HotSpot permiten. Estos dos
> tracks son extensiones aspiracionales apoyadas en las fases A–E.

## FASE F — `burst`: el optimizador (rendimiento)

> Módulo de optimización **opcional**, atornillado sobre el intérprete ingenuo.
> Arquitectura clave: el intérprete ingenuo es el **chasis y el oráculo de
> corrección**; `burst` transforma la representación antes/durante la ejecución y
> **debe** dar resultados idénticos. Se valida con **differential testing** (mismo
> programa por los dos caminos → assert de igualdad).
> Importante: un optimizador **no necesita JIT** — el intérprete tiene su propia
> caja de herramientas (típico 2–10× sin compilar a nativo).

### Hito F-bench · Infraestructura de medición 🔵 — ✅ hecha (2026-08-13)
- [x] `bench_baseline` (`#[ignore]`): 5 workloads en `java/Bm*.java` que aíslan una dimensión cada uno — `BmLoop` (aritmética frame-local = el piso), `BmArray`, `BmInvoke`, `BmVirtual`, `BmField`. Conteos de opcodes exactos y reproducibles → **ns/opcode** compara entre workloads. Valores esperados contrastados contra el `java` real del JDK 25, con un test no-ignorado que los verifica
- [x] **Protocolo de medición** (aprendido a los golpes): el ruido de *code layout* de esta máquina es **±3-12%, mayor que el efecto de casi cualquier cambio** — demostrado con un experimento nulo (agregar un campo `HashMap` **sin usar** movió un control +3.4%). Por eso: workloads de **control de efecto-cero** verificados *contando* la operación, **cuadrado latino** con binarios pre-compilados, medianas y mínimos
- **✅ Éxito:** dos cuellos "obvios" resultaron falsos y la medición los desmintió — el GC parecía culpable de que `BmField` fuera 8.6× el piso (era ≤5%) y el scan O(#clases) de `class_name_at_mirror` parecía la gran palanca de `BmVirtual` (valía ~3%: hay **5** mirrors, no cientos)

### Hito F0 · Quickening 🔵 — ✅ hecho (2026-08-13)
- [x] Resolución del constant pool **una sola vez por call site**, cacheada por `(MethodId, pc)` en una celda `AtomicU64` por byte de código (sin hash ni strings). Se eligió **tabla lateral** en vez de reescribir el opcode: el costo medido no estaba en el despacho sino en el trabajo redundante, y mutar bytecode compartido habría tocado el barrio del heisenbug de `os-parallel`
- [x] **Campos** (`field_sites`) — `field_offset` **reconstruía el layout entero de la clase** en cada acceso: **−86%**. Cerró además un peligro real: `layout_fields_ref` truncaba en silencio ante una superclase no cargada devolviendo offsets demasiado chicos; sin cache era una escalación inofensiva, **con** cache habría inmortalizado un offset incorrecto
- [x] **Llamadas** (`call_sites`) — callee resuelto, `vtable_slot` estático, anchos de slots, y la cadena de ~7 comparaciones de string reemplazada por un enum `Intrinsic` en el `MethodBody` **del callee** (por vtable el sitio no puede saber la respuesta: depende del receptor): `invokestatic` **−40%**, `invokevirtual` **−45%**
- [x] **Pool de frames** — reusar `Frame`s y la capacidad de sus `Vec`: **−11/12%** más en llamadas (~72 ns/llamada). Blindado contra el riesgo de GC con una sola puerta de entrada (`scrub` incondicional), `debug_assert!` en cada reuso con la suite corrida en **modo debug**, y verificación de que el pool no está en ningún camino de raíces
- **✅ Éxito superado:** el criterio era "no re-resolver en cada vuelta"; la medición mostró que el mismo patrón —*un cache keyeado por `&str` alcanzado alocando un `String`, recomputado por ejecución*— estaba en **tres** opcodes, y que esas alocaciones eran **impuesto del borrow checker**, no necesidad semántica

### Hito F1 · Superinstrucciones / fusión 🔵 — ⏭️ salteado a propósito
- **Decisión, no omisión:** fusionar `iload,iload,iadd` atacaría el piso de despacho (~28-30 ns/opcode) con una mejora estimada de 10-20%, **por debajo del ruido de layout de esta máquina (±3-12%)** — no podríamos *demostrar* que funciona. El esfuerzo se redirigió a F3, donde el efecto es de dos órdenes de magnitud. Queda disponible si el piso de despacho vuelve a ser el cuello con mejor instrumentación

### Hito F2 · Inline caching + stack caching 🟣
- [ ] **Inline cache monomórfico** (receptor → método). F0 ya dejó el lugar: cachea el `vtable_slot` del tipo estático y lo único que queda por llamada es `vtable_method(runtime_class, slot)`; `SiteKind::Vtable` está formado para alojarlo
- [ ] **Stack caching**: tope de la pila de operandos en registro (en el JIT, equivale a asignar registros a la pila de operandos — hoy cada op Java es load/op/store contra L1)

### Hito F3 · JIT (bytecode → nativo) 🟣 — ✅🟡 primer tier corriendo (2026-08-13)
- [x] **Emisor x86-64 propio** (`src/burst/x64.rs`), **sin dependencias** — las funciones de Windows declaradas a mano con `extern "system"`, igual que en su día se escribió el cursor de bytes en vez de usar `nom`. ALU, `idiv`, `jcc` con labels y parcheo, ABI **Microsoft x64**. Verificado **ejecutando**: bucles, factorial, y código generado **llamando a código generado** (que cubre de una las tres reglas del ABI que solo muerden en un `call`)
- [x] **Memoria W^X como garantía del tipo** (`exec_mem.rs`): `CodeBuf` → `.make_executable()` **consume `self`** → `ExecMem`. Tras la transición no queda ningún handle escribible — el tipo lo hace imposible, no es convención
- [x] **Compilador** (`compile.rs`) del subconjunto **frame-local de enteros** (el mismo que ya corría lock-free en W1: sin heap, sin llamadas, sin referencias ⇒ **función pura de sus locales**). La profundidad de pila se recalcula **del CFG**, sin confiar en el `StackMapTable`: un desacuerdo entre ramas se rechaza en vez de generar código sobre una suposición
- [x] **Las tres trampas de semántica**, con prueba: *normalización* (`movsxd` tras la aritmética pero **no** tras `iand/ior/ixor`, con la demostración de por qué), *shifts* (Java enmascara a 5 bits, x86 a 6 en ops de 64; `iushr` es lógico sobre 32), y *división* (`INT_MIN/-1` trunca por JLS §15.17.2 en vez de fallar con `#DE`). Los 11 operadores binarios contra un modelo en Rust sobre **361 pares de borde**
- [x] **Deopt por pureza**: sin efectos secundarios, ante lo que el nativo no pueda manejar (divisor cero) se abandona y **el intérprete ejecuta el método desde cero** — indistinguible. Evita emitir manejo de excepciones en nativo
- [x] **OSR + poll de safepoint**, ambos en back-edges con **pila vacía** ⇒ el estado a transferir es solo *locales + pc*. Sin OSR un bucle largo entrado una sola vez nunca calentaba (el caso de `BmLoop`). El poll **sale hacia el intérprete** en vez de manejar el GC desde el nativo, y usa una palabra estable propia: el `gc_pending` del driver es un `Arc` construido fresco en cada corrida (e inexistente en green), así que hornear su dirección habría sido corrupción silenciosa
- **✅ Éxito (primer tier):** `BmLoop` **475 ms → 4.1 ms = 116×**, y el número que lo explica no es el tiempo sino los opcodes interpretados: **17.325.011 → 620**. Controles planos. El *differential testing* no hubo que construirlo: con el JIT activo en green/os-gil y apagado en os-parallel, el oráculo `green≡os-gil≡os` **es** la comparación JIT-vs-intérprete sobre el corpus entero (y la suite pasa igual con `JVM_JIT=0`)
- [ ] **Pendiente, por impacto:** ensanchar el subconjunto (hoy un `+= 256` descalifica un método por `wide iinc`; después `getstatic`/`putstatic` de ints, y llamadas), **registros** para la pila de operandos, y habilitar el JIT en **`os-parallel`** — apagado a propósito: es el único modo con el heisenbug abierto y no se le agrega una variable a un problema con ~17 sesiones de acotamiento

## FASE G — `plain_data` / value types (modelo de datos)

> Los **"huérfanos de Object"**: tipos planos sin header, sin identidad, sin
> monitor, *flatteables* — los FrankenObjects que discutimos. Viable porque
> tenemos compilador propio (Fase B) que puede emitir el constructo y una VM que
> lo trata especial. Es, en chiquito, el principio de representación de un
> **tensor** (puente directo a la tesis del usuario sobre LLMs). Inspiración:
> *value classes* de Valhalla, `struct` de .NET.

### Hito G0 · `plain_data` plano en el heap 🟣
- [ ] Tipo sin header: `[ field0 | field1 ]` vs objeto normal `[ class_ptr | fields ]`.
- [ ] El compilador (Fase B) lo marca; la VM lo guarda inline.

### Hito G1 · Arrays flatteados + semántica de valor 🟣
- [ ] `plain_data[]` contiguo (sin los N punteros, ni los N headers, ni N alocaciones).
- [ ] Igualdad por valor (comparar bytes), sin `null`, sin GC para ellos.
- **✅ Éxito (medible):** comparar memoria/layout de `plain_data[]` vs array de objetos.

### Hito G2 · Escape analysis "lite" 🟣
- [ ] Decidir *flattening* en *load time* cuando se prueba que un objeto no escapa
      (versión declarativa/estática; el automático completo es del JIT, Hito F3).

---

## Estado de A0 — snapshot (2026-06-02)

> **Al día (2026-08-05):** este bloque es el snapshot de **A0** (sus "6 tests verdes"
> son de aquel momento). Desde entonces se
> completaron **A1–A5 y gran parte de A6**: intérprete, objetos/heap con dispatch
> dinámico, excepciones, class loaders, nativos+intrínsecos, **GC generacional** +
> referencias débiles, **verificador JVMS-estricto**, sistema de tipos completo, e
> **hilos + monitores**, con la concurrencia **ensanchada a paralelismo real** — GIL
> removido (W1/W2/W3), **JMM relajado (H4)**, **CAS (H5)** y el **núcleo de
> `java.util.concurrent` (H6)** sobre AQS (`ReentrantLock`/`Semaphore`/`CountDownLatch`),
> todo validado por el oráculo `green≡os-gil≡os` — más `wait(timeout)` y monitores
> GC-safe, y el **set de opcodes completo**: 199 de 199 alcanzables, con `invokedynamic`
> cubriendo 5 de las 6 fábricas que emite `javac` (concatenación, `switch` sobre patrones,
> records, lambdas y constantes dinámicas).
> El proyecto compila **sin warnings** y pasa el grueso de la suite (los ~17 rojos son del
> `javac`, en curso en otra PC).
> Detalle vigente en `Concurrencia_KajiJDK.pdf`, `Roadmap_JDK.pdf` e
> `invokedynamic-ruta.md`.
> **Siguiente: el volumen restante de `java.util.concurrent` (`BlockingQueue`,
> `ConcurrentHashMap`, `Executor`/pools) y los locks por-estructura finos; y resolver el
> bug residual del modo `os` (referencia *stale* bajo GC intenso — guard parcial landeado,
> próximo paso ThreadSanitizer). `green`/`os-gil` no afectados.**

**Fase A / Hito A0 — núcleo logrado.** Compila **sin warnings**, 6 tests verdes,
**12 fixtures byte-idénticos** a `javap`.
- ✅ `ClassReader` (cursor big-endian) y constant pool completo (17 tags + `Tombstone`),
  con árbol de referencias en `pretty_class_visualizer.rs`.
- ✅ Header: versiones, `access_flags` (+ métodos `is_*`), `this_class` / `super_class`
  con `class_name()` (índice → `Class` → `Utf8`). `from_path() -> Result<_, ParseError>`.
- ✅ `fields`/`methods`/`attributes`; atributo `Code` con **desensamblado completo**
  (tabla de opcodes + `tableswitch`/`lookupswitch`/`wide`) y comentarios `// …` resueltos.
- ✅ `LineNumberTable`, `SourceFile`, y **`StackMapTable` (los 7 frame types)** — cada
  frame en su propia clase bajo `parser/stack_map_table/`, con `verification_type_info`.
- ✅ `javap` **brief y `-v` byte-idénticos** (incl. cabecera Classfile / Last modified /
  SHA-256 / Compiled from).
- ✅ Flags de visibilidad (`-public` / `-protected` / `-package` / `-p` / `-private`).
- ✅ Renderers factorizados en `parser/printers/`: `verbose` (orquestación) + `file_header`
  + `pool_comments` + `member_dump` + `brief` + `dump_common` + `visibility`.
- Estructura real: `src/main.rs`, `src/javap.rs`, `src/jvm/{class_file.rs, opcode.rs,
  parser/{reader, constant_pool, member, attribute, code, stack_map_table/, printers/}}`,
  `src/pretty_class_visualizer.rs`. Fixture congelado `java/Sample.class`.

**Pendiente de A0:** atributos no esenciales (ver tabla en Hito A0) y flags de contenido
(`-c`/`-l`/`-s`). Ninguno bloquea avanzar.

*(Histórico: en su momento el siguiente paso fue arrancar el Nivel 1 / intérprete —
ya hecho. Ver la nota "Al día" arriba para el estado actual.)*
