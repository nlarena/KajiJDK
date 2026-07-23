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
  - [ ] Chequeos de **acceso** (`IllegalAccessError`) — sin demo trivial (javac no compila accesos ilegales)
  - [ ] Sutilezas de `<clinit>`: `ConstantValue` (las constantes de compilación no disparan init), `ExceptionInInitializerError`
  - [ ] Identidad `(nombre, loader)` (re-key por loader definidor)
  - [ ] Stack trace / `getMessage` en `Throwable`
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
- [x] **Hilos, monitores, `synchronized`** — green threads cooperativos (default + visor) **y** substrato **hilos de SO + GIL** (`JVM_THREADS=os`, E1+E2): `std::thread` por `Thread.start()`, `park`/`unpark`, `wait`/`notify`/`join`, IMSE, `wait(timeout)` y **monitor GC-safe** (las claves se remapean por el *forward* del GC en minor/compact); GC seguro bajo el GIL
- [x] **API de `Thread` (H1)** — `currentThread`/`yield`/nombre/id/`isAlive`, `getState()` con los seis estados (`ThreadStatus` a cinco + `NEW` derivado, un punto único de bloqueo), `Thread(Runnable)` (una lambda lo satisface), `start` dos veces → `IllegalThreadStateException`, e `interrupt`/`InterruptedException` que despierta `sleep`/`join`/`wait` (re-adquiere el monitor antes de lanzar en el `wait`; la carrera notify/interrupt la resuelve el GIL). Faltan periféricos (daemon, prioridad, `ThreadLocal`)
- [ ] **Sacar el GIL** → paralelismo real (locks finos + TLABs + handshake stop-the-world) — *E3, próximo*
- [ ] **Modelo de memoria de Java** (`volatile`, happens-before, fences) — recién útil con paralelismo real
- [x] **Cobertura del set de opcodes — 199/199 alcanzables: completo** — cerrados `nop` (0x00), `goto_w` (0xc8), el prefijo `wide` (0xc4, índices de local de 16 bits: `wide iinc` mide 6 bytes, el resto 4), `multianewarray` (0xc5, alocación recursiva de sólo los `dimensions` niveles indicados, modelado **también en el verificador**) e `invokedynamic` (0xba). Los 3 de `jsr`/`ret`/`jsr_w` quedan **excluidos por diseño** (JVMS §4.9.1 los prohíbe en class files de versión 50.0+, o sea Java 6 en adelante), con la postura **leer sí, ejecutar no**: el desensamblador los soporta completo —requisito de A0— y el gate estructural del verificador los rechaza. Nota de diseño: `subrutinas-jsr-ret.md`.
- [x] **`invokedynamic`** (0xba) — **no era un opcode, era un subsistema**, y corre: **5 de las 6 fábricas** que emite `javac`. Concatenación de strings (`StringConcatFactory`), `switch` sobre patrones de tipo *y* de enum (`SwitchBootstraps.typeSwitch`), `equals`/`hashCode`/`toString` de records (`ObjectMethods`), lambdas y method references (`LambdaMetafactory.metafactory`), y constantes dinámicas (`ConstantBootstraps.invoke`). La sexta, `altMetafactory`, necesita serialización y queda fuera de alcance. Ruta, correcciones y mediciones: **`invokedynamic-ruta.md`**
- [x] **`ldc` de literales de clase** (`Foo.class`, `int[].class`) — empuja el mirror, cacheado por Class ID, y sin inicializar la clase (un literal no es *uso activo*, §5.5)
- [x] **La VM puede invocar Java** (`call_java`) — empuja un frame propio y lo corre con un bucle anidado, devolviendo el resultado. Era el caso general de lo que ya hacía `<clinit>`. **Los intrínsecos dejan de ser terminales**: es lo que permite que `String.valueOf(Object)` llame al `toString()` del objeto, que un record pregunte el `equals`/`hashCode` de sus componentes, y que un condy ejecute su bootstrap
- [ ] **Modelo de objetos de `java.lang.invoke`** (`MethodHandle`/`MethodType`/`Lookup`) — desbloquea `ldc` de esas constantes y los bootstrap methods del usuario; **parte necesita el escritor de `.class` (B3) para tener con qué probarse**. Detalle en `TODO.md`
- [ ] JIT (bytecode → código nativo)
- **✅ Éxito (parcial):** verificación de tipos completa, GC generacional, set de opcodes completo (incluido `invokedynamic`), y concurrencia con hilos de SO reales serializados por un GIL; falta el paralelismo real (sacar el GIL), el JMM y/o el JIT. *Detalle en los informes `Concurrencia_KajiJDK.pdf` e `invokedynamic-ruta.md`.*

---

## FASE B — El compilador (`javac`, escrito en Rust)

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

### Hito F0 · Quickening 🔵
- [ ] Resolver refs del constant pool **una sola vez** y reescribir el opcode a su
      variante "resuelta" (como las JVM reales) → saltar la resolución repetida.
- **✅ Éxito:** un método que invoca en bucle no re-resuelve en cada vuelta.

### Hito F1 · Superinstrucciones / fusión 🔵
- [ ] Fusionar secuencias calientes (`iload, iload, iadd`) en un solo handler →
      menos overhead de despacho. (Primo del *operator fusion* de los compiladores
      de ML — p. ej. FlashAttention.)

### Hito F2 · Inline caching + stack caching 🟣
- [ ] Inline cache en sitios de llamada (recordar receptor → método) para acelerar
      el *dispatch* dinámico.
- [ ] Mantener el tope de la pila de operandos en registro/variable.

### Hito F3 · JIT (bytecode → nativo) 🟣
- [ ] Compilar métodos calientes a código nativo (register allocation, etc.).
- **Nota:** la cumbre lejana. El dueño del proyecto **apuesta a alcanzarla** y
      queda como **meta real** del track, no descartada. El differential testing
      la mantiene honesta igual que al resto de `burst`.

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

> **Al día (2026-07-21):** este bloque es el snapshot de **A0** (sus "6 tests verdes"
> son de aquel momento). Desde entonces se
> completaron **A1–A5 y gran parte de A6**: intérprete, objetos/heap con dispatch
> dinámico, excepciones, class loaders, nativos+intrínsecos, **GC generacional** +
> referencias débiles, **verificador JVMS-estricto**, sistema de tipos completo, e
> **hilos + monitores** con el substrato **OS-threads + GIL** (E1+E2), más
> `wait(timeout)` y monitores GC-safe, y el **set de opcodes completo**: 199 de 199
> alcanzables, con `invokedynamic` cubriendo 5 de las 6 fábricas que emite `javac`
> (concatenación, `switch` sobre patrones, records, lambdas y constantes dinámicas).
> El proyecto pasa **120 tests** sin warnings.
> Detalle vigente en `Concurrencia_KajiJDK.pdf`, `Roadmap_JDK.pdf` e
> `invokedynamic-ruta.md`.
> **Siguiente: E3 — sacar el GIL (paralelismo real). H1 (API de `Thread` + `interrupt`) cerrado.**

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
