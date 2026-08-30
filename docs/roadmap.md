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
| JIT (bytecode → nativo) | **Sí** — hecho (Fase F: primer tier x86-64 corriendo, ~116×; censo ~57% de los métodos) |
| Biblioteca estándar Java (`java.base` + extras) | **Sí** — amplia (945 clases: colecciones, `java.time`, `java.util.zip`, `java.math`, `java.text`, `java.lang.constant`, jakarta) |
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
- **JIT:** era el sueño lejano; **hoy hecho** (Fase F, ver abajo).

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
   └── K · Fuzzer diferencial: no es un eslabón sino un instrumento — verifica A,
           B y F a la vez, porque el oráculo es la propia VM contra sí misma
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
- [x] JIT (bytecode → código nativo) — **hecho en la Fase F** (emisor x86-64 propio, primer tier corriendo, ~116×; ver más abajo)
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

> **Al día — el compilador está construido:** B0 (lexer), B1 (parser, lenguaje completo),
> B2 (análisis semántico: pasadas 1 y 2 + chequeos), B3 (generación de bytecode con
> `StackMapTable`), B4 (compilador robusto) **y B5 (genéricos completos: erasure +
> bridge methods)** están **hechos** — el `javac` propio compila objetos/bucles/
> excepciones/genéricos y el `.class` corre en la JVM (`fib(10)=55`, `fact(5)=120`) y pasa
> el verificador JVMS-estricto. En curso **B6 (fidelidad de `javac`)**: esquinas de
> lenguaje, `-Xlint`, APT y el diferencial de emisión (ver abajo). El detalle por hito está
> en `Roadmap_JDK.pdf`. Las casillas de abajo son el plan original.

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

### Hito B5 · Cumbre del compilador 🟣 — ✅ núcleo cerrado
- [x] Genéricos completos (*type erasure*, *wildcards*, inferencia del Cap. 18) + **bridge methods**
- **✅ Éxito:** compila código genérico equivalente al de `javac`.

### Hito B6 · Fidelidad de `javac` 🟣 — 🚧 en progreso
- [x] **Esquinas de lenguaje (A/B/C/D)** — expresiones constantes a `ConstantValue` (§15.28/§15.29), *bridges* en interfaz (§9.4.1.3 + defaults de superinterfaz en la vtable), exhaustividad con record patterns anidados (JEP 440/441) y asignación definitiva de un `switch` exhaustivo sin `default` (§16.2.9)
- [x] **`-Xlint`** (§9.6.4.5) — pase de lint con **nueve categorías** (`empty`, `cast`, `fallthrough`, `deprecation`, `rawtypes`, `unchecked`, `finally`, `serial`, `this-escape`), severidad en el diagnóstico, `@SuppressWarnings` y render estilo `javac`
- [x] **APT — fase 1** — la API de `javax.annotation.processing` / `javax.lang.model`, con resolución de tipos anidados de clases externas (atributo `InnerClasses`, p. ej. `Diagnostic.Kind`)
- [x] **Diferencial de emisión** (`tools/emitdiff`) — compila cada `.java` con nuestro `javac` y con el real, desensambla ambos con `javap -p -c` y compara **normalizado**; cerró un lote de divergencias (ACC_PUBLIC del `default` de interfaz, `iinc`, comparaciones contra 0, literales negativos, `ACC_STATIC` de clase anidada, `Signature` de enum + `values()` vía `array.clone()`, switch-expr sobre `String`)
- [ ] **Falta:** el *round loop* de APT y `javadoc`
- **✅ Éxito (parcial):** el `.class` emitido y los diagnósticos son indistinguibles de los de `javac` para el núcleo del lenguaje.

---

## FASE C — Las bibliotecas (escritas en Java, compiladas por tu `javac`)

> **Al día — muy por delante del plan original:** la biblioteca son hoy **945 clases** e
> incluye, además del núcleo, `java.util` completo (colecciones + concurrentes), `java.io`
> con decoradores, `java.time` (value types + offsets + `DateTimeFormatter` + zonas + chrono),
> `java.util.zip` (DEFLATE + ZIP), `java.math`, `java.text`, `java.lang.constant`,
> `java.lang.invoke`, reflexión parcial y **jakarta.validation**. Las casillas de abajo son
> el plan original mínimo.

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
- [x] 🟣 **`jdb` (debugger)** — hecho (Fase I: JPDA completo — JVMTI/JDWP/JDI, validado contra el `jdb` real de Temurin)
- [x] 🟣 **`jlink`/`jimage`** — hecho (Fase J: escritor/lector de `jimage`, imagen ejecutable, `--compress`)
- [ ] 🟣 `javadoc`, `jmod`, `keytool`…

---

## FASE E — Cerrar el círculo 🟣

El momento épico: las tres piezas funcionando juntas.
- [ ] Compilar las bibliotecas (Fase C) con tu propio `javac` (Fase B)
- [ ] Ejecutar un programa real que use esas bibliotecas en tu propia JVM (Fase A)
- [ ] Conformance / compatibilidad (la cumbre lejana: comportamiento fiel a la spec)
- **✅ Éxito:** un `.java` que escribes → lo compila tu `javac` → usa tus bibliotecas → corre en tu JVM, sin tocar nada del JDK de Temurin.

---

## FASE I — El depurador (JPDA: JVMTI · JDWP · jdb) 🟣 — ✅ completo

> La VM propia se depura, y no solo desde adentro: el mismo `com.sun.jdi` que usa IntelliJ
> attachea sin saber que la VM es propia.
- [x] **JVMTI** (`jvmti.rs`) — back-end *push*: un `JvmtiAgent` registra callbacks (`breakpoint`/`single_step`/`method_entry`/`method_exit`/`exception`) que la VM dispara en medio de `step()`, con *fast-path* por `Capabilities` y sin `unsafe` (destructurando `self` en campos disjuntos)
- [x] **JDWP** (`jdwp.rs`) — codec puro (handshake, framing, serialización tipada big-endian), *command handlers* (`VirtualMachine`/`EventRequest`), y el bridge JVMTI↔JDWP que empuja eventos como *Composite packets* y aplica los `EventRequest` del cliente a la VM real
- [x] **JDI** (`jdi.rs`) — API cliente por *mirrors* tipados (`Vm`/`Location`/`Event`); binarios `jvm-jdwp` (servidor `dt_socket`), `jdi-attach` y `jdb`
- [x] **Fidelidad (I5)** — inspección de pila/variables con la VM parada, breakpoints por línea de fuente y field watchpoints; **validado contra el `jdb` real de Temurin** (`Breakpoint hit: Add.add(), line=3 bci=0`)
- **✅ Éxito:** sesión de `jdb` 100% limpia (`threads`/`classes`/`stop at`/`cont`/`where`/`locals`/`watch`) por protocolo, dos procesos, todo propio.

## FASE J — Imagen enlazada (`jlink` / `jimage`) 🟣 — ✅ cerrada

> Empaquetar el runtime + la biblioteca en una imagen ejecutable, como el `jlink` del JDK.
- [x] **`jimage`** (`jimage.rs`, `src/jvm/jimage.rs`) — escritor y lector del contenedor (`info`/`list`/`extract`), con `module-info` de cada módulo aislado
- [x] **`jlink`** (`jlink.rs`, `src/jvm/modules.rs`) — grafo de módulos JPMS (accesibilidad, `--add-modules`, `--describe-module`), pipeline de plugins (`strip-debug`, `add-options`, `launcher`) y `--compress` con `inflate` propio (`inflate.rs`) que hace **booteable** la imagen
- [x] **Accesibilidad JPMS** enforced en el intérprete (`check_access` en los `invoke*`/field, §5.4.4)
- **✅ Éxito:** `run-headless --boot <imagen>` corre desde la imagen enlazada; el `java.base` propio vive en `KajiLibrary/module-info.java`.

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
- [ ] **Deuda: re-medir tras el ensanchado (2026-08-21)** — no se mide nada desde los tipos anchos, así que el costo del inline caching, de los `long` y de la alocación de arrays es **desconocido**, y una regresión pasaría inadvertida: se estuvo agregando cobertura a ciegas. Peor, el instrumento se degradó solo: `BmArray` y `BmField` **dejaron de ser controles de efecto-cero** al volverse compilables, así que la tabla quedó casi sin brazos de referencia. Hay que **rediseñar el set de workloads** antes de que los números vuelvan a significar algo
- **✅ Éxito:** dos cuellos "obvios" resultaron falsos y la medición los desmintió — el GC parecía culpable de que `BmField` fuera 8.6× el piso (era ≤5%) y el scan O(#clases) de `class_name_at_mirror` parecía la gran palanca de `BmVirtual` (valía ~3%: hay **5** mirrors, no cientos)

### Hito F0 · Quickening 🔵 — ✅ hecho (2026-08-13)
- [x] Resolución del constant pool **una sola vez por call site**, cacheada por `(MethodId, pc)` en una celda `AtomicU64` por byte de código (sin hash ni strings). Se eligió **tabla lateral** en vez de reescribir el opcode: el costo medido no estaba en el despacho sino en el trabajo redundante, y mutar bytecode compartido habría tocado el barrio del heisenbug de `os-parallel`
- [x] **Campos** (`field_sites`) — `field_offset` **reconstruía el layout entero de la clase** en cada acceso: **−86%**. Cerró además un peligro real: `layout_fields_ref` truncaba en silencio ante una superclase no cargada devolviendo offsets demasiado chicos; sin cache era una escalación inofensiva, **con** cache habría inmortalizado un offset incorrecto
- [x] **Llamadas** (`call_sites`) — callee resuelto, `vtable_slot` estático, anchos de slots, y la cadena de ~7 comparaciones de string reemplazada por un enum `Intrinsic` en el `MethodBody` **del callee** (por vtable el sitio no puede saber la respuesta: depende del receptor): `invokestatic` **−40%**, `invokevirtual` **−45%**
- [x] **Pool de frames** — reusar `Frame`s y la capacidad de sus `Vec`: **−11/12%** más en llamadas (~72 ns/llamada). Blindado contra el riesgo de GC con una sola puerta de entrada (`scrub` incondicional), `debug_assert!` en cada reuso con la suite corrida en **modo debug**, y verificación de que el pool no está en ningún camino de raíces
- **✅ Éxito superado:** el criterio era "no re-resolver en cada vuelta"; la medición mostró que el mismo patrón —*un cache keyeado por `&str` alcanzado alocando un `String`, recomputado por ejecución*— estaba en **tres** opcodes, y que esas alocaciones eran **impuesto del borrow checker**, no necesidad semántica

### Hito F1 · Superinstrucciones / fusión 🔵 — ⏭️ salteado a propósito
- **Decisión, no omisión:** fusionar `iload,iload,iadd` atacaría el piso de despacho (~28-30 ns/opcode) con una mejora estimada de 10-20%, **por debajo del ruido de layout de esta máquina (±3-12%)** — no podríamos *demostrar* que funciona. El esfuerzo se redirigió a F3, donde el efecto es de dos órdenes de magnitud. Queda disponible si el piso de despacho vuelve a ser el cuello con mejor instrumentación

### Hito F2 · Inline caching + stack caching 🟣 — ✅ hecho (2026-08-21)
> **Corrección:** este hito es de la **caja de herramientas del intérprete**, no del compilador — la Fase F arranca diciendo que *«un optimizador no necesita JIT»*. Las dos ideas ya existen **dentro del JIT** (guarda de clase exacta + registros para la pila de operandos), pero eso no es lo mismo: en el intérprete, `invokevirtual` **registra** el receptor observado y aun así vuelve a hacer `vtable_method_at_mirror` en cada llamada, y la pila de operandos sigue siendo un `Vec<Value>` con `push`/`pop` reales. Lo hecho en el JIT queda documentado abajo; lo del intérprete es lo que falta.
- [x] **En el JIT: inline cache monomórfico** (receptor → método) — **guarda de clase exacta + cuerpo inlineado + deopt en miss**. La clase se elige por **observación del propio intérprete**: `invokevirtual`/`invokeinterface` ya cargaban el header del receptor para despachar, así que registrarlo cuesta **un store `Relaxed` en un camino que ya tenía el valor en la mano**, y al llegar al umbral de 32 invocaciones hay 32 observaciones gratis. La alternativa (adivinar el tipo estático) es peor: una apuesta sobre una clase que el sitio puede no ver nunca, y errarla cuesta un deopt **por llamada** en vez de nada
- [x] **En el JIT: stack caching** — asignación de registros a la pila de operandos, dentro de un bloque básico y derramando en los bordes; `JVM_JIT_REGS=0` la desactiva, lo que hace que las dos ramas sean **el mismo binario** y el ruido de layout quede estructuralmente ausente de la comparación
- [x] **En el intérprete: inline cache** — la celda que solo alimentaba al compilador pasó a guardar `[mirror | método]` en **una sola palabra atómica**, y en un hit se saltea el lookup de vtable entero. En `invokeinterface` el premio es mayor: evita un **escaneo lineal de la tabla del receptor comparando dos `String` por entrada**. La palabra única es requisito de **corrección**, no de layout: dos celdas `Relaxed` paralelas se pueden leer desgarradas, y un torn read de este cache no es una llamada lenta sino **una llamada al método equivocado**. **No necesita invalidación**, y el argumento es más fuerte que «el lookup está bien»: *el cache no puede discrepar del lookup que reemplaza* — la clave (offset de mirror) es estable porque los mirrors están pinneados, y el valor (`vtables[clase][slot]`) porque `vtables` es **insert-only** y `MethodId` indexa un vector append-only
- [x] **Medición, aislando el ruido** — los controles se movieron **−8% por puro relayout**, así que en vez de pelear contra eso se midió la razón **`BmMono`/`BmVirtual` dentro del mismo binario** (mismo programa opcode por opcode, mismas direcciones, difiriendo solo en si el receptor es monomórfico): **1.0037 → 0.9385**, con los rangos sin solaparse. Antes de F2 un sitio monomórfico costaba **lo mismo** que uno megamórfico; ahora cuesta **6.5% menos**, y el megamórfico no se degrada. El hit ahorra ~50-65 ns por llamada, que es justo lo que cuestan los tres probes de hash que evita (dos de ellos hasheando el *nombre* de la clase)
- [x] **El sabotaje que reveló un agujero** — de cinco, uno **no lo atrapó nadie**: un guard que compara solo los **16 bits bajos** del mirror pasaba la suite entera en verde. Que dos clases coincidan en media palabra es suerte, y un guard de clave parcial es silencioso hasta el día que no lo es; van asertos que fijan la comparación de los 32 bits completos en ambas direcciones. Queda además un test permanente que **falla si un workload de control empieza a despachar** — exactamente la deuda que hizo que `BmArray`/`BmField` dejaran de ser controles sin que nadie se enterara
- [x] **En el intérprete: stack caching — ⏭️ evaluado y descartado, con datos.** Segunda vez que un hito de esta fase se cierra decidiendo *no* hacerlo (la primera fue F1). El costo: 193 sitios de `push`, 64 de `pop`, 9 escapes a `operands_mut()` (la familia `dup`/`swap`, que inserta en posiciones que un tope-en-registro no alcanza) y **tres recorridos de raíces del GC** que leen la pila — cada uno necesitaría flush/reload. El beneficio: techo **~5%**, porque antes de que corra cualquier handler `run_one` ya paga el safepoint, dos indexados con bounds-check al scheduler, `sync_code_cache` y varios `frames.last_mut()`; sobre un instrumento cuyos controles se mueven −8% por relayout, sería inmedible. **Dónde sí está la plata, medido:** `BmInvoke` cuesta **51.9 ns/opcode** contra los **36.6** de `BmLoop` — con el call site ya cacheado por F0, lo que queda es **armado y desarmado de frames**, no `push`/`pop`

### Hito F3 · JIT (bytecode → nativo) 🟣 — ✅🟡 primer tier corriendo (2026-08-13)
- [x] **Emisor x86-64 propio** (`src/burst/x64.rs`), **sin dependencias** — las funciones de Windows declaradas a mano con `extern "system"`, igual que en su día se escribió el cursor de bytes en vez de usar `nom`. ALU, `idiv`, `jcc` con labels y parcheo, ABI **Microsoft x64**. Verificado **ejecutando**: bucles, factorial, y código generado **llamando a código generado** (que cubre de una las tres reglas del ABI que solo muerden en un `call`)
- [x] **Memoria W^X como garantía del tipo** (`exec_mem.rs`): `CodeBuf` → `.make_executable()` **consume `self`** → `ExecMem`. Tras la transición no queda ningún handle escribible — el tipo lo hace imposible, no es convención
- [x] **Compilador** (`compile.rs`) del subconjunto **frame-local de enteros** (el mismo que ya corría lock-free en W1: sin heap, sin llamadas, sin referencias ⇒ **función pura de sus locales**). La profundidad de pila se recalcula **del CFG**, sin confiar en el `StackMapTable`: un desacuerdo entre ramas se rechaza en vez de generar código sobre una suposición
- [x] **Las tres trampas de semántica**, con prueba: *normalización* (`movsxd` tras la aritmética pero **no** tras `iand/ior/ixor`, con la demostración de por qué), *shifts* (Java enmascara a 5 bits, x86 a 6 en ops de 64; `iushr` es lógico sobre 32), y *división* (`INT_MIN/-1` trunca por JLS §15.17.2 en vez de fallar con `#DE`). Los 11 operadores binarios contra un modelo en Rust sobre **361 pares de borde**
- [x] **Deopt por pureza**: sin efectos secundarios, ante lo que el nativo no pueda manejar (divisor cero) se abandona y **el intérprete ejecuta el método desde cero** — indistinguible. Evita emitir manejo de excepciones en nativo
- [x] **OSR + poll de safepoint**, ambos en back-edges con **pila vacía** ⇒ el estado a transferir es solo *locales + pc*. Sin OSR un bucle largo entrado una sola vez nunca calentaba (el caso de `BmLoop`). El poll **sale hacia el intérprete** en vez de manejar el GC desde el nativo, y usa una palabra estable propia: el `gc_pending` del driver es un `Arc` construido fresco en cada corrida (e inexistente en green), así que hornear su dirección habría sido corrupción silenciosa
- **✅ Éxito (primer tier):** `BmLoop` **475 ms → 4.1 ms = 116×** (re-medido tras ensanchar el subconjunto: **125×**), y el número que lo explica no es el tiempo sino los opcodes interpretados: **17.325.011 → 620**. Controles planos. El *differential testing* no hubo que construirlo: con el JIT activo en green/os-gil y apagado en os-parallel, el oráculo `green≡os-gil≡os` **es** la comparación JIT-vs-intérprete sobre el corpus entero (y la suite pasa igual con `JVM_JIT=0`)
- [x] **Subconjunto ensanchado** — prefijo `wide` (un `+= 256` ya no descalifica), los siete opcodes de pila que faltaban (en su mayoría **no emiten código**: la pila se simula en compilación, son permutaciones de slots), `tableswitch`/`lookupswitch` (con el padding de alineación a 4 bytes, el *default* como target real, y los arms hacia atrás registrados como cabeceras de bucle para OSR) y **`getstatic` de `int`**. Guarda notable: `wide iinc` mide **6 bytes**, y un decodificador que lo tratara como 4 se re-sincronizaría sobre el byte bajo del delta —que en `0x0102` es `0x02`, un `iconst_m1` decodificable, o sea corrupción que sigue "ejecutando"—, así que hay un test clavado
- [x] **Dos rechazos razonados**, que valen tanto como lo agregado. **`putstatic`**: la regla propuesta era sólida, pero aceptarla convertiría el invariante *"el código compilado no escribe nada observable"* en *"…salvo cuando no puede deoptar"*, y cada guarda futura tendría que re-derivar su interacción con esa excepción (y bloquea solo 6 de 682 métodos). **`long`**: no es esfuerzo sino **límite estructural** — `lreturn` no entra en el protocolo de retorno (`RAX = (status<<32) | value` deja 32 bits), así que exige mover la frontera y con ella el marshalling, el write-back de OSR y cada `unsafe` que la cruza
- [x] **El techo, medido** (`subset_census`): **77 de 710 métodos** compilan, 31% del techo de 246 (`ireturn` es la única salida). Bloqueantes: **`aload_0` 354** (todo método de instancia: `this` es referencia), `new` 96, `invokestatic` 41, statics no-`int` 26, `ldc2_w` 18. Los 18 métodos `void` bloqueados **no valen nada**: un método puro y frame-local que devuelve `void` es literalmente un no-op. Conclusión: ensanchar más *opcodes* rinde poco — **lo que sigue son referencias y llamadas**
- [x] **Referencias (2026-08-14)** — `aload_0` **desapareció** de los bloqueantes. Entraron `aconst_null`, `aload`/`astore` (+`wide`), `ifnull`/`ifnonnull`, `if_acmp*`, `getfield` de `int`, `arraylength`, `iaload` y **`areturn`** (que subió el techo del censo de 246 a **299**, porque `ireturn` dejó de ser la única salida). **No hicieron falta stack maps para el colector**: como el nativo no aloca, corre solo en green/os-gil y en los polls *sale*, ningún GC puede observar un frame compilado — el object graph está congelado mientras hay nativo en la pila. Bastó un **mapa de tipos** (int/ref por local y posición de pila) por interpretación abstracta sobre el mismo punto fijo que ya calculaba la profundidad, leído solo en la frontera de marshalling. **Censo: 77 → 96 métodos.** `BmVirtual` dejó de ser control (sus overrides son `aload_0; getfield; ireturn`), aunque gana solo 1.15×: cuerpos diminutos, domina el cruce de frontera
- [x] **Dos hallazgos que el diseño no anticipaba** — (1) **el heap son DOS buffers**: Eden vive en un `AtomicRegion` aparte y survivors/Old en `memory`, así que `base + offset` es una función de dos ramas; asumir un solo buffer habría hecho que todo acceso a un objeto en Eden leyera memoria ajena. (2) **Los exception handlers son aristas invisibles al recorrido hacia adelante**, así que el intérprete puede llegar a un loop header habiendo ejecutado código que el mapa nunca analizó (alcanzable: un deopt lo manda a re-ejecutar y esa pasada puede lanzar y ser atrapada) → un método con tabla de excepciones compila pero **sin entradas por OSR ni sitios de poll**. Rechazados a propósito: campos `volatile` (una lectura volátil *es* un `mov` en x86-64, pero apoyarse en el modelo de memoria de una arquitectura dentro de código generado merece su propio paso)
- [x] **El test del peor modo de falla, verificado como detector** — devolver una referencia como `Value::Int` en el write-back haría que el GC tratara un entero como puntero, y no fallaría donde está el bug. Se **rompió el código a propósito** para confirmar que el test falla, y recién después se restauró. Los tests machine-level usan un `FakeHeap` con **dos** buffers, porque uno solo dejaría pasar a un compilador que ignorara la partición de Eden
- [x] **Deopt de verdad (2026-08-14)** — dejó de ser *por reinicio* (válido solo mientras todo fuera puro) y pasa a **reconstruir el estado del intérprete en un pc**: locales, pila de operandos y posición. La pieza que faltaba ya existía: el mapa de tipos sabe en cada pc cuántos operandos hay y de qué tipo. Con eso entraron las **escrituras** (`putfield`, `iastore`, `putstatic` de `int`). Polls de safepoint y deopts **se unificaron** en una sola tabla de `resume_sites`: un header de bucle resultó ser un sitio de resume con pila vacía. **La regla que lo hace correcto**: toda guarda se emite **antes** del primer efecto observable de su instrucción y nada después del efecto puede deoptar ⇒ el pc devuelto siempre nombra una instrucción cuyo efecto no se aplicó, y el par «intento nativo + resume interpretado» escribe exactamente una vez. Validado rompiéndolo: reponer el viejo reinicio da 832497 en vez de 832289, 208 escrituras duplicadas
- [x] **Alocación `new` sin romper el invariante (2026-08-15)** — se emite **solo el camino rápido** (bump de Eden con `lock xadd` sobre la misma palabra que usa el intérprete, así una reserva nativa nunca se solapa con una suya) y se **deopta** cuando no entra: si el bump entró, no hubo colección. El bookkeeping del GC —el índice de objetos jóvenes, que es un `Mutex` inalcanzable desde nativo— resultó **diferible**: se drena en un único lugar de toda la VM (la entrada al GC), así que la ventana entre bump y registro no contiene ninguna colección *por construcción*. Hallazgo de paso: el cursor de Eden era un atómico **inline** en una estructura que se devuelve por valor — hornear su dirección habría sido hornear una dirección que se mueve
- [x] **Llamadas por inlining (2026-08-15)** — último bloqueante grande (`invokespecial` refusaba 379). Se eligió inlining sobre llamada nativa (sin ABI ni unwind info); el costo es el deopt, resuelto con **virtual frames**. La clave: **el intérprete ya tenía la convención necesaria** — en un invoke el pc del caller queda apuntando al invoke y es el `return` del callee el que lo avanza, así que un deopt desde código inlineado se reconstruye como «caller en el pc del invoke con los argumentos ya popeados + callee en el pc del deopt» y lo termina la maquinaria existente sin casos especiales. Recursión cortada por **identidad de método**, no por profundidad (atrapa `f→g→f`); un callee con rama hacia atrás se rechaza de plano, porque solo los headers de la raíz tienen poll y un bucle inlineado sería código del que no se puede salir
- [x] **Merge del mapa de tipos (2026-08-15)** — un slot que llegaba `Int` por una arista y `Reference` por otra (la forma corriente de javac cuando está *muerto* ahí) descalificaba el método entero. El retículo ganó un elemento de **conflicto**, y lo que prueba que el slot está muerto **no es una suposición sobre javac sino el código**: toda lectura pide `Int` o `Reference`, así que un slot en conflicto falla toda lectura — el rechazo *es* la prueba. En un resume no se escribe de vuelta: el frame conserva su `Value` anterior, seguro para el GC porque no puede producir ninguna de las dos corrupciones (offset marcado `Int`, int marcado `Reference`); costo documentado: una referencia vieja puede quedar viva de más, fuga menor y nunca corrupción. Donde no podía *probar* la deadness, declinó asumirla: un cuerpo con exception handlers no recibe conflictos, porque las aristas hacia un handler son las que el recorrido no sigue
- [x] **`BmField` compila: 98.7 ms → 1.8 ms = 55×** — el patrón `new X(…)` en bucle con campos, código Java corriente, y el último de los cinco workloads que el JIT nunca pudo tocar (`BmArray` pasó a control pinneado para conservar dos brazos de efecto-cero). En el mismo paso, el riesgo anotado por el paso anterior —un `resume_state` devolviendo `None` tras correr nativo **reiniciaría el método en silencio**, re-aplicando escrituras— pasó de mitigado con `debug_assert` a **estructuralmente imposible**: una compilación con sitios de resume no reconstruibles **no se instala**
- [x] **Censo: 21% → ~57%** (152 → **444 de 785 métodos**), rechazos por `invokespecial` de **379 a 3**. La disciplina de *romper el código a propósito para verificar que el test detecta* se volvió rutina: en el inlining se sabotearon cinco cosas distintas y **el primer juego de tests no atrapaba ninguna** — dos tests nuevos y uno reformado existen por eso
- [x] **Ensanchado del subconjunto (2026-08-21)** — cinco frentes integrados: **tipos anchos** (`long` completo, `float`/`double`; obligó a rediseñar el protocolo de retorno, que gastaba la mitad alta de RAX en el status), **arrays** (`newarray`/`anewarray` en el fast path de Eden, con el tope inline acotando el tramo de ceros que **no puede poll-ear**), **referencias de lectura** (`getfield`/`getstatic` de ref, `volatile`, `checkcast`/`instanceof` por clase exacta o deopt), **llamadas** (inline caching + polls en bucles inlineados) y **control excepcional** (`athrow`/monitores como deopt, donde lo que carga el peso es el `decode`: ambos son `Flow::Return`, así que el escaneo se detiene y nada posterior a un `monitorenter` corre nativo). Censo: **11% → 68%** (697 de 1029 métodos)
- [x] **Una brecha de cobertura ajena al JIT, encontrada por auditoría** — `gc::compact` **no se ejecutaba ni una sola vez en toda la suite** (0 invocaciones en 1074 tests), así que el pinning de los mirrors —el hecho del que dependen **todos** los inmediatos horneados: `checkcast`, `instanceof`, `ldc Foo.class` y la dirección de `getstatic`— no tenía ninguna prueba: borrar el conjunto `pinned` entero era invisible. Va el test que faltaba, verificado en ambas direcciones
- [ ] **Write barrier del GC** — el bloque más grande y más concentrado que queda: **~76 métodos** (`ReferenceWrite` 60 + `aastore` 11 + `aaload` 5), y creciendo más rápido que el corpus porque el código de biblioteca real guarda objetos en campos. La forma probablemente ya existe en el árbol: el **log diferido de alocaciones** (anotar en un array plano y replayar al volver, válido porque la ventana no contiene ningún GC *por construcción*) es exactamente la misma figura que necesita un barrier diferido. **Costo a aceptar con los ojos abiertos:** hoy el `DANGLING` intermitente (1 de 10 corridas del verificador) se atribuye al heisenbug de `os-parallel` *precisamente porque* el código compilado no puede escribir un puntero; con esto esa atribución se vuelve más difícil
- [ ] **Accesos de array por ancho** (~20: `aastore` 11, `lastore` 4, `baload`/`caload` 4, `dastore`/`fastore` 2) — mecánico: la tabla de anchos ya existe (la construyó la alocación de arrays); falta que el emisor sepa codificar loads/stores de 8 y 16 bits, con sign-extend para `byte`/`short` y zero-extend para `char`. `aastore` depende del write barrier
- [ ] **Llamadas reales (compilado → compilado)** — hoy el JIT **solo sabe inlinear**, y lo irreducible es la recursión real (`InlineCycle` 11) más `TooBig`/`InlineDepth`. Exige la ABI (ya probada) y, sobre todo, **deopt a través de un frame nativo**, que es la parte difícil. El más caro y el que menos rinde por esfuerzo: va último
- [ ] **Cola larga de análisis** (~22: `WrongType` 10, `TypeMismatch` 3, `StackUnderflow` 3, `Unrebuildable` 1, y las **conversiones saturantes** `d2i`/`d2l`/`f2i` 5, que la JLS §5.1.3 hace NaN-aware mientras `cvtt*2si` devuelve el "integer indefinite" para los tres casos)
- [x] **`ldc` de String — hecho (2026-08-29)** — el compilador lo rechazaba por una carencia **de la VM, no del JIT**, y FZ-008 la cerró: un literal es ahora un objeto permanente en una dirección estable (una instancia por literal, en Old, raíz de GC y pinneado en `compact`), que es exactamente lo que un inmediato necesita.
  - **Un resolver más en el `Environment`, de sólo lectura**, con el mismo argumento escrito que `class_mirror`: internear un literal que el intérprete nunca alcanzó **aloca**, y una compilación que puede alocar es una que puede recolectar. Un literal que no está en el pool rechaza el método — gratis, porque un método llega a este tier sólo después de correr, y un literal en un camino que corrió ya está en el pool
  - **El mapa de tipos no hizo falta tocarlo**: ya caía en `Reference` para todo lo que no fuera `int` ni `float`, que es lo que impide que un deopt derrame un offset en un slot marcado `Int` y el colector pase de largo por un objeto vivo
  - **Medido**: el censo del subconjunto pasa de **1012 a 1026 métodos** (53% → 54% del techo) y la razón «String» **desaparece del cuadro de rechazos**. La cifra que decía este ítem —46 métodos, hoy 167 en este corpus— era la de los que *mencionaban* esa razón, no la de los que se destraban: la mayoría tenía además otro bloqueo. Los que mandan ahora son la familia de los `invoke` (271 + 187 + 79 + 58 + 23) y **`ReferenceWrite` con 86**, que es la barrera de escritura
  - **Sabotaje, y lo que enseñó**: el primero —correr todos los offsets 8 bytes— **pasó**, porque un programa Java sólo observa la *relación* de identidad y no la dirección. Colapsar dos literales en un offset sí falla. Queda anotado que la diferencia es inalcanzable y no meramente no probada: el resolver lee el mismo mapa que escribe `strings::intern`
  - **Y un hecho del arnés que conviene saber**: los tests del JIT bootean desde `boot/` y no desde `KajiLibrary`, así que corren **otro** `java.lang.String`. La primera versión de la fixture tocaba `new String(…)` y daba distinto que la misma fixture por `run-headless`; quedó reducida a lo que este cambio prueba
- [ ] **`invokedynamic`** (25) — **estructuralmente cerrado hoy, y no por falta de opcodes**: en esta VM un indy **no es una llamada**. El intérprete re-lee el constant pool y **re-corre el bootstrap en cada ejecución**; solo memoiza la clase spun y las constantes condy, así que no hay ningún `MethodId` ligado que darle al JIT ni nada que una guarda de clase pueda proteger. Necesita dos pasos previos del lado del intérprete: darle un call site ligado que cachear, y expresar `metafactory` como el par `new` + `invokespecial` que ya es
- [ ] **`os-parallel`** — el JIT sigue apagado ahí a propósito: es el único modo con el heisenbug abierto y no se le agrega una variable a un problema con ~17 sesiones de acotamiento. Además, el argumento que autoriza emitir accesos `volatile` como `mov` planos es **una propiedad del sustrato** (green tiene un solo hilo, os-gil tiene el GIL tomado) — habilitarlo ahí lo invalida y hay que revisar cada acceso (marcador `VOLATILE-REVISIT-OS-PARALLEL` en el código)

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

## FASE K — El fuzzer diferencial (verificación)

> Va aparte de A, B y F porque **verifica a las tres**, y no cabe dentro de ninguna.
>
> Lo caro de un fuzzer nunca es generar entradas: es **saber si la respuesta está mal**. Un programa
> generado devuelve `847`; ¿está bien? Nadie calculó el valor esperado, así que no hay contra qué
> comparar. Ése es el *problema del oráculo*, y es lo que frena a la mayoría de los proyectos.
> Este ya lo tenía resuelto tres veces sin darse cuenta: `JVM_JIT=0` contra el JIT, `green ≡ os-gil ≡
> os`, y esta VM contra el `java` real. Ninguno necesita conocer la respuesta correcta — solo que dos
> caminos lleguen al mismo lugar. Los workloads de `java/` son los casos que a alguien **se le
> ocurrieron**; esto explora los que no.

### Hito K0 · El lazo y los cuatro contratos 🔵 — ✅ hecho (2026-08-28)
- [x] `src/fuzz.rs`: generar → correr por dos caminos → oráculo → reducir. Cuatro verbos, y cada nivel más profundo es un refinamiento de uno de ellos
- [x] **Un `Program` es una estructura de datos, no texto** — no es preferencia de estilo: el reductor tiene que *manipular* un programa que falla (borrar una sentencia, encoger una constante), y todo corte sobre texto produce algo que ya no compila. Reducir sobre un árbol queda válido por construcción, y reducir es lo que convierte «el fuzzer encontró algo raro» en un caso que un humano va a leer
- [x] **El predicado es del lazo, no del reductor** — `campaign` le pasa un cierre armado con **el mismo** runner y oráculo que acaban de discrepar. Un reductor con su propia noción de «sigue fallando» puede minimizar un programa hasta uno que falla por *otro motivo*: la forma clásica en que un shrinker convierte un bug real en uno ficticio
- [x] **El tercer veredicto: `Unusable`** — el nivel 0 tenía dos (coinciden o no). Un programa que no compila, o que no termina en *ninguno* de los dos lados, no dice nada sobre la VM: es una promesa rota del generador. Contarlo aparte es lo que permite leer «0 divergencias» junto a «y además corrió algo»

### Hito K1 · Ejecutor, oráculo, generador y reductor 🔵 — ✅ hecho (2026-08-28)
- [x] **Ejecutor fuera de proceso**, aunque llamar al intérprete in-process sería más rápido: para un fuzzer **un crash y un cuelgue son hallazgos, no fallas de la herramienta**. In-process, un panic de la VM se lleva puesta la campaña y la semilla; un bucle infinito generado la congela para siempre (un hilo de Rust no se mata desde afuera); y `JVM_JIT`/`JVM_THREADS` se leen del ambiente, que es por proceso. Un hijo resuelve los tres: un panic es un exit status, un cuelgue es un `kill`, y el ambiente es por hijo
- [x] **Cuatro propiedades por construcción**, porque ninguna se puede chequear después sin tirar el programa: **tipado correcto** (un `CompileError` es un bug del generador, no de la VM), **determinista** (por *ausencia*: el AST no tiene ningún nodo que produzca un valor irreproducible, así que no es una regla que alguien tenga que recordar), **terminante** (cota literal en el `for`, contador legible pero **no asignable**, grafo de llamadas DAG, más un **presupuesto de costo** que divide al entrar a un bucle) y **total** (el `run()` se atrapa a sí mismo y codifica qué excepción cazó en el `int` que devuelve)
- [x] **Sesgo hacia los bordes** — las constantes no salen de un `i32` uniforme sino de un pool cargado con los valores donde vivieron los bugs que este proyecto encontró a mano: `Long.MIN_VALUE / -1`, corrimientos de 31/32/33/63/64, `-1 >>> 1`. Un `i32` uniforme pegaría en `32` una vez cada cuatro mil millones
- [x] **Reductor estructural** hasta un mínimo **local** — con una limitación anotada en vez de escondida: un shrinker greedy no puede bajar por un bug con forma de módulo
- [x] **El orden del reductor cuenta las referencias (2026-08-29)** — una hoja que **nombra** algo (`o.v()`, `o.a`, una variable) tenía el mismo peso que un literal, así que reemplazarla por `0` no era un paso y el reductor no podía aceptarlo. Mientras la referencia queda en pie la declaración que lee tampoco es borrable: **una sola llamada clava el objeto, su argumento de constructor y toda su jerarquía en el hallazgo, para siempre**. El peso pasa a ser `(nodos, masa de literales, referencias)`, que sigue siendo un orden bien fundado

### Hito K2 · Ensanchado de la gramática 🟣 — ✅ 5 de 5 etapas
- [x] **Etapa 1 · `float`/`double`** — tres esquinas justifican tenerlos: **NaN en comparaciones** (`fcmpl` y `fcmpg` difieren *solo* ahí, lo que hace que confundirlos sea invisible en toda otra entrada y fatal en ésa), las **conversiones estrechantes** (`f2i`/`d2i`/`d2l`, saturantes y NaN-aware por JLS §5.1.3, donde el `cvtt*2si` de x86 contesta el «integer indefinite» — el JIT **rechaza el método entero** antes que hacerlas mal, así que el intérprete es la única implementación que corre y nadie la había contrastado contra un JDK real) y **`frem`/`drem`**, que el JIT compila a un deopt incondicional a propósito
- [x] **El canal de resultado, que resultó ser un hallazgo** — el ejecutor observa **un** `int`, y la forma obvia de convertir un `double` es la conversión que está bajo prueba. `Double.doubleToLongBits` parecía la salida: está **declarado nativo y no registrado en `natives.rs`**, así que el `javac` de referencia lo acepta y **solo esta VM falla en runtime** — el envoltorio total lo habría tragado como `OTHER` y el oráculo habría reportado divergencia en cada semilla. La campaña habría sido 100% falsa. Se usa un clasificador **solo por comparaciones** (NaN por `d != d`, los dos ceros separados por `1.0/d`, escalera de 14 sondas en los bordes de saturación), con su limitación —es grueso entre sondas— escrita
- [x] **Etapa 2 · arrays** — `newarray`, `iaload`/`iastore`, `arraylength`, índices fuera de rango y longitudes negativas. Lo valioso no son las lecturas: **un acceso a array no lanza en código compilado, deopta** — el JIT emite una guarda de cota que sale de nativo y devuelve el pc al intérprete, que levanta la excepción que el compilado declinó
- [x] **Etapa 3 · objetos, campos y llamadas virtuales (2026-08-28)** — `new`, `getfield`, `putfield` e `invokevirtual` sobre una jerarquía fija de cuatro clases que el programa lleva al lado. La jerarquía es constante y el **uso** es lo generado, porque la dimensión que importa no es la forma del grafo de clases sino **cuántas clases receptoras llegan a un sitio y en qué orden**. Una de las cuatro (`S2`) **no sobrescribe nada**: es la que falla contra una vtable armada copiando solo lo que una clase *declara*, y la que las otras dos tapan
- [x] **La sonda de la caché de inline va plantada, no sorteada** — un sitio polimórfico no es un programa polimórfico: hace falta un único sitio de bytecode cuyo receptor cambie entre ejecuciones de *ese mismo* sitio, o sea una reasignación dentro de un bucle sobre un nombre declarado afuera, y sortear esa forma pasaría unas pocas veces cada mil semillas. Va a la cabeza del método de entrada —el único que `run()` llama 40 veces y por lo tanto el único que seguro cruza el umbral de 32— y **lo que computa se pliega al resultado**: sin eso las llamadas correrían, la guarda fallaría, deoptaría, y devolvería un número que no podría diferir
- [x] **Medida, y dio lo contrario de FZ-005:** sin objetos **121** métodos compilados y **18** deopts; con todo, **401** y **802**. Las filas que difieren *solo* en la sonda dan 62 contra 802, así que ese salto es la caché fallando — el camino crítico de F2, que hasta ahora no ejercitaba nada generado. De paso desmintió un pronóstico propio: se esperaba que un campo `long` sacara del JIT a todo método con un `new` (el `invokespecial` del constructor se inlinea en su llamador) y el efecto predicho era un derrumbe hacia 121; midió 401, así que `wide_fields` pasó a **on**
- [x] **Etapa 4a · estrechamiento (`i2b`/`i2s`/`i2c`) (2026-08-29)** — sin tipos angostos nuevos: un estrechamiento es un **viaje de ida y vuelta** `int → truncar → extender → int`, así que la semántica interesante entra (truncado, y `char` extendiendo con cero donde `byte` y `short` llevan el signo) y el contexto de tipos **no** se multiplica, que era el costo anotado. Perilla propia `narrowing_share`, porque tira para lados opuestos en los dos pairings: contra el JDK real es el punto —`conversion_operations` nunca se había diffeado— y contra el JIT es **veneno**, porque `0x91`–`0x93` no están en el scan de `burst::compile` y el método se rechaza entero. **80 seeds, 80 usables, 0 divergencias**
- [x] **Etapa 4b · `switch` (2026-08-29)** — la otra mitad, y al revés: `tableswitch`/`lookupswitch` **sí** están en el subconjunto del JIT, así que suma cobertura en vez de restarla, y por eso va **encendida por defecto** (`switch_share: 12`) mientras las otras dos perillas del hito arrancan en cero. Tres propiedades fijadas por test, y ninguna de ellas es "aparece": etiquetas **densas y dispersas** — que es lo que decide si `javac` emite `tableswitch` o `lookupswitch`, dos opcodes con decodificadores distintos —, **fall-through** — lo único acá que los opcodes *no* codifican: la tabla de saltos no lo dice, lo arma el compilador — y que los brazos se **alcancen**, para lo cual el selector va enmascarado: uno arbitrario no pegaría nunca una etiqueta y el `switch` se reduciría a "evaluar y caer en `default`". Campañas: intérprete≡JIT **120/120, 0 divergencias**, y VM≡JDK real **120/120, 0 divergencias**
- [x] **Etapa 5 · `while`/`break`/`continue` y `throw` explícito (2026-08-29)** — el argumento de terminación se construye en vez de perderse: `while (g++ < K && cond)` con `K` literal, la guarda evaluada **primero** y `g` no asignable en el cuerpo, que son las dos mismas reglas del `for` contado. Un terminador va siempre último y nunca en el cuerpo de un método, y un predicado `completes_abruptly` corta el bloque en la primera sentencia que se va. **4 pareos × 300 semillas, 100% usables, 0 divergencias**
- [x] **Strings (2026-08-29)** — interning de literales, plegado de una concatenación constante y `equals` por contenido. Es la etapa que encontró **FZ-008**
- [x] **`invokeinterface` (2026-08-29)** — resuelve por **itable**, no vtable, y basta declarar el local con el tipo de la interfaz: el opcode lo elige `javac`. **40 sitios en 21/60 semillas**, y **0** al sabotear la declaración. No cuesta cobertura del JIT: 67/80 semillas sin interfaces, 71/80 con ellas
- [x] **Hilos (2026-08-29)** — K workers computando cada uno un `int` en su propio slot, joineados y reducidos en orden de índice, más el oráculo que corre el **mismo** programa N veces en `os-parallel` y lo compara contra sí mismo. **4 pareos × 300 semillas** y **2400 ejecuciones contra sí mismo**, 0 divergencias. Rígido como es para ser determinista, sólo detecta carreras que **rompan** el determinismo

### Hito K3 · Registro de hallazgos 🔵 — ✅ vivo (`docs/fuzzer_findings.md`)
> La regla de oro: **un hallazgo sin caso mínimo reproducible no está terminado** — el programa de 200 líneas que salió del generador no sirve, hace falta lo que queda después del reductor.
- [x] **FZ-003** — el `javac` del `PATH` puede ser **el nuestro**: este repo compila un binario llamado `javac` y `cargo test` pone el directorio de build en el `PATH` del hijo. Una campaña que confiara en el `PATH` compilaría sus programas con el compilador bajo prueba, que es justo la herramienta cuyos bugs se supone que está contrastando contra una referencia
- [x] **FZ-004** — el brazo «JIT» corría el intérprete el **88%** de las veces. `JitCache::THRESHOLD` es 32: un programa cuyo `run()` llama al cuerpo una vez no cruza ningún umbral, y entonces `JVM_JIT=0` y `JVM_JIT` sin setear son **el mismo motor**. Con `warmup: 1`, **7 de 60** programas entraban a nativo, y un reporte limpio no significaba casi nada
- [x] **FZ-005** — los programas con arrays **morían antes de que el JIT los mirara**: los índices se generaban sin saber a qué array indexaban, así que lanzaban en la iteración 1 del calentamiento. 46% de las semillas moría así y la cobertura se partió al medio (73/80 → 40/80). La firma que lo distingue de un rechazo es `rejected: 0`. Es FZ-004 con otro sombrero, y las dos veces apareció **midiendo, no razonando**
- [x] **Disciplina de sabotaje** — romper el código a propósito para verificar que el test se da cuenta. En la etapa 3 los siete sabotajes fueron detectados; el primer clasificador del script daba los siete como «no compila» porque `cargo test` imprime `error: test failed` cuando un test **falla** — el script estaba roto, no los tests
- [x] **FZ-001 — arreglado (2026-08-29)** — la VM siempre supo qué excepción mató al hilo, pero el reporte iba a un buffer de consola que `run-headless` nunca volcaba. Ahora nombra la clase en stderr y sale con **1**, como `java`, y los dos lados del oráculo se leen con la misma función
- [x] **FZ-006 — arreglado (2026-08-29)** — el reductor no podía reemplazar por una constante una hoja que **nombra** algo, así que no podía soltar la declaración que esa hoja lee: una sola llamada clavaba el objeto y toda su jerarquía en el hallazgo, para siempre
- [x] **FZ-007 — arreglado (2026-08-29)** — `cargo test --lib` no reconstruye binarios, así que el test de extremo a extremo ejecutaba lo que hubiera quedado en `target/release`. Los dos sabotajes de FZ-001 pasaron en verde hasta que se puso un `cargo build` en el medio. Va guarda, no nota: la nota ya estaba, y lo que no se notaba era incumplirla
- [ ] **FZ-002 — abierto, no reproducido (2026-08-29)** — el reproductor del hallazgo da **60 de 60 en verde** con el verificador de heap puesto, y los programas concurrentes dan **0 divergencias en 4000 corridas** con **528 colecciones por corrida de media**. Cuatro mil corridas sin reproducir no prueban que no exista: prueban que no es alcanzable por esta forma en esta máquina. **No se le baja la severidad por no aparecer**
- [x] **FZ-008 — arreglado (2026-08-29)** — los literales de `String` no se interneaban: `"a" == "a"` daba `false` acá y `true` en un JDK real, contra la **JLS §3.10.5**. Es el **primer bug de conformidad de la VM que encontró el fuzzer**, y el reductor lo dejó en dos líneas. Destraba además `ldc` de String en el JIT

**Estado (2026-08-29):** 300 semillas × 4 pareos (intérprete/JIT, JIT/JDK real, JIT/os-gil, JIT/os-parallel): **100% usables, 0 divergencias**; más 300 semillas en proceso con **256 entrando a código nativo**. Ningún bug de conformidad encontrado todavía — y lo que ese cero mide es lo que la gramática alcanza hoy, no la VM.

**De los siete hallazgos, cinco son de la herramienta y no de la VM** (FZ-003 a FZ-007). Todos tienen la misma forma —parecen estar probando algo y no lo están— y todos dan verde. Es la razón por la que cada perilla de la gramática se cierra con un censo que demuestre que el constructo **llega**, y no solo con un reporte limpio.


### Hito K4 · Lo que la gramática no alcanzaba 🟣 — ✅ 3 de 3 (2026-08-29)

> Las tres entraron enteras en `src/fuzz/`: una arista del heap que ninguna otra construcción
> puede crear, una pérdida de cobertura que el reporte no mostraba, y un par de opcodes que
> estaban dentro del subconjunto del JIT y no se emitían.
>
> **Las tres se cerraron midiendo, y las tres desmintieron algo que estaba escrito**: que los
> campos de referencia estaban bloqueados (lo estaban para *un* pareo), que un receptor `null`
> gratis era un bonus (costaba 41 de 80 semillas), y que el envoltorio total estaba bien donde
> estaba (una línea lo movía de 58-75 a 78-80 de 80 entrando a código nativo).

- [x] **Campos de referencia (2026-08-29)** — un campo de tipo referencia en la jerarquía, su `putfield` y la lectura encadenada. Es **lo único de la gramática que construye una arista de un objeto del heap a otro**, o sea lo que ejercita la barrera de escritura y el remembered set. Perilla propia y apagada por defecto, medido: contra el JIT los rechazos por método suben de **116 a 243**
- [x] **Que las semillas dejen de morir temprano (2026-08-29)** — el envoltorio total atrapaba **alrededor** del bucle de calentamiento, así que una excepción en la iteración 1 terminaba el programa y el JIT nunca cruzaba su umbral. Atrapando **por iteración**, las semillas que entran a código nativo pasan de **58-75 a 78-80 de 80** en las once configuraciones del censo, y las muertas en marcador de 5-41 a **0 en todas**. El número queda en el reporte y la campaña larga puede fallar por él
- [x] **`instanceof` y casts (2026-08-29)** — la misma pregunta hecha de las dos formas en que la hace la JVM: la que nunca falla y la que sí. Están **dentro** del subconjunto del JIT, así que no cuestan cobertura (79/80 semillas, 398 métodos compilados). Hacen alcanzable el marcador `CLASS_CAST`, que estaba escrito desde el principio y ningún nodo podía producir


### Hito K5 · Los constructos que la gramática todavía no emite 🟣

> Estaban anotados en la tabla de alcance del generador como decisiones tomadas. No lo son: son
> cosas sin hacer, cada una con lo que costaría.

- [ ] **Arrays multidimensionales** — `multianewarray` está fuera del subconjunto del JIT, así que pagaría cobertura como los estrechamientos. **Agregar**: la dimensión en `Stmt::NewArray`, el índice anidado, y perilla propia apagada por defecto. **Listo cuando** las campañas contra el JDK real los emiten y el censo mide qué cuestan
- [ ] **Arrays `null`** — hoy el receptor nulo se alcanza sólo por un objeto. Necesita que una variable de array pueda ser `null`, o sea un tipo referencia en `Ty`, que es lo que los objetos evitaron para que la identidad no fuera observable. **Listo cuando** un `arraylength` sobre `null` aparece en las campañas sin que `Ty` gane un caso que rompa la propiedad 2
- [ ] **Payloads de NaN** — el clasificador colapsa todo NaN a un código, así que hoy la esquina que la JLS deja definida por implementación es invisible. **Agregar**: un canal que distinga los payloads y una lista de divergencias legítimas para lo que sea genuinamente libre. **Listo cuando** un NaN con payload llega al `int` observado y el oráculo sabe cuál diferencia aceptar
- [ ] **Locales `char`/`byte`/`short`/`boolean`** — las conversiones ya se generan; lo que falta son los *locales*, que multiplican el contexto de tipos. **Listo cuando** el generador los declara y asigna sin que suba la tasa de programas que `javac` rechaza
- [ ] **`do`/`while` y saltos etiquetados** — una etiqueta deja salir de un bucle del que se escribió el argumento de terminación, así que hace falta rehacer ese argumento antes que el opcode. **Listo cuando** el presupuesto de costo acota un bucle del que se puede salir por una etiqueta
- [ ] **Recursión** — hoy imposible por construcción: el grafo de llamadas es un DAG, que es la mitad de la propiedad 3. **Agregar**: una cota de profundidad que el generador pueda demostrar, no suponer. **Listo cuando** un programa recursivo termina por una razón escrita y no por suerte
- [ ] **Carreras que un programa determinista no puede tener** — `Stmt::Fork` es rígido a propósito para que el resultado sea fijo, así que sólo expone carreras que rompan el determinismo. **Agregar**: un modo cuyo oráculo no sea la igualdad sino un conjunto de resultados admisibles. **Listo cuando** un entrelazado legal con más de una respuesta correcta no se reporta como divergencia

### Hito K6 · Los límites de forma del generador 🟣

> No son decisiones de alcance sino atajos que tomé para que cada etapa entrara: cada uno se puede
> levantar sin tocar nada más.

- [ ] **Más de un sitio paralelo por programa** — la clase worker lleva los cuerpos en un `switch` sobre `k`, así que un segundo `Fork` necesitaría una segunda clase. **Listo cuando** dos sitios coexisten y el censo los ve a los dos
- [ ] **Que un worker pueda llamar y usar strings** — hoy `Scope::foreign` se lo prohíbe, porque los helpers del programa (`m0`, `fcls`, `ssame`) se emiten sin calificar y no resuelven desde otra clase. **Agregar**: calificarlos con el nombre de la clase. **Listo cuando** un cuerpo de worker contiene una llamada y las campañas siguen en 100% usables
- [ ] **Una jerarquía de objetos generada, no fija** — hoy son cuatro clases y una interfaz siempre iguales; lo generado es el *uso*. **Listo cuando** la profundidad y el ancho del grafo de clases salen de la semilla y el censo de sitios por vtable e itable no cae
- [ ] **El cast que lee el campo ancho** — `((…S1) o).b` está prohibido por el checker porque el local que declara es `int`. **Listo cuando** el `long` a través de un cast se genera y el censo del JIT mide qué cuesta
- [ ] **Que las perillas de objetos manden sobre su propio techo** — la sonda de tipo llega a 27/200 y el store de referencia a 41/200 con las perillas altas, porque el techo lo pone `object_share` y no ellas. **Listo cuando** subir una perilla mueve su número de forma proporcional

### Hito K7 · El instrumento 🔵

> Lo que le falta al fuzzer como herramienta, aparte de gramática.

- [ ] **Una corrida larga** — todas las campañas se cerraron con 200-300 semillas. Eso es horas de máquina, no código, y es lo que más rinde ahora. **Listo cuando** hay una corrida de miles de semillas por pareo con su resultado anotado en `docs/fuzzer_findings/`
- [ ] **Una medida de cobertura que no lea 0 por construcción** — desde que el envoltorio atrapa por iteración, `marked` sólo detecta el caso degenerado, porque un programa que lanza siempre devuelve `31·…·marca` y no la marca pelada. **Agregar**: un canal que reporte cuántas iteraciones lanzaron. **Listo cuando** el número distingue "lanzó una vez" de "lanzó siempre"
- [ ] **Un reductor que baje por un valle** — es greedy, así que llega a un mínimo **local**, y bajo el oráculo de repetición su predicado además es inestable. **Listo cuando** una forma que hoy resiste la reducción se minimiza, con el caso anotado
- [ ] **Que los tests del JIT booteen como la VM** — el arnés de `jit_tests` arranca desde `boot/` y no desde `KajiLibrary`, o sea que corre otro `java.lang.String` que el que corre `run-headless`. **Listo cuando** las dos rutas cargan la misma biblioteca y una fixture que toca `String` da lo mismo por las dos

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
