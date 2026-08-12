# holdon.md — Hitos de la JVM que se nos pasaron (auditoría vs JVMS/JSR)

> Auditoría 2026-08-12: gaps del runtime frente a la JVM Specification (JSR 924/202,
> hoy el JVMS de Java SE) y los JSR estructurales. Cada ítem dice cómo se verificó:
> **[empírico]** = probe ejecutado hoy contra la VM; **[inspección]** = leído en el código.
> No incluye lo ya documentado como pendiente en `roadmap.md` (volumen j.u.c, `final`/`VarHandle`,
> `ConstantValue`, `IllegalAccessError`, identidad `(nombre,loader)`, bug residual os-parallel).

## 🔴 Rotos o ausentes con impacto real (ordenados por prioridad sugerida)

### 1. Default methods de interfaces — JSR 335 (Java 8) · ~~ROTO~~ → **✅ ARREGLADO** (2026-08-12)
Estaba roto: `interface I { default int f() {...} }` + `new Impl().f()` → `NoSuchMethodError`,
porque `MetaspaceService::build_vtable` heredaba **solo de la superclase** — nunca fusionaba los
defaults de las interfaces implementadas.
Arreglo aplicado: la vtable ahora fusiona los defaults de las superinterfaces (BFS desde las
directas — la interface más específica se visita primero y su default shadowa al heredado; la
clase siempre gana; un método abstracto, sin `Code`, nunca toma slot). El diamante sin override
(ICCE, §5.4.6) queda fuera: javac no compila esa forma. Cubre `invokeinterface` e `invokevirtual`.
Test en la suite: `DefProbe` → 115 (herencia + shadowing + override de clase + static de
interface), `green≡os-gil≡os` + 10× os-parallel. Los static de interface ya funcionaban.

### 2. Excepción no atrapada → `panic!` del VM — JVMS §2.10 · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
Estaba roto: en `athrow.rs`, si el unwinding vaciaba el stack, la VM hacía
`panic!("uncaught exception")` — una excepción sin catch tumbaba la VM entera.
Arreglo aplicado: la VM ahora imprime a la consola el formato exacto del JDK real
(`Exception in thread "Thread-1" java.lang.RuntimeException: uncaught in worker` + la traza
`\tat ...` capturada en el throw) y termina **solo ese thread** (`Step::Return(None)` = el
return final del thread: los joiners despiertan y `main` sigue; si es `main`, termina el
programa). Implementación en `athrow.rs` (`report_uncaught` + `current_thread_name`).
Test en la suite: `UncTest` → 42 (worker lanza sin catch, main hace join y comprueba
TERMINATED), `green≡os-gil≡os` + 10× os-parallel. (La API `Thread.setUncaughtExceptionHandler`
queda pendiente; el default de imprimir-y-morir es lo que exige la spec.)

### 3. `ArrayStoreException` — JVMS §6.5 `aastore` · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
`aastore` ahora valida asignabilidad (clase runtime del valor vs tipo de elemento, `is_subtype`);
null siempre válido, stores primitivos intactos. Test `AsTest` → 42, oráculo completo.
**Bonus — el chequeo expuso 2 bugs latentes, arreglados:** los **mirrors de clases array se
alocaban en Eden** (el minor GC los movía/recolectaba → índice y headers colgados sobre un slot
reutilizado por otro mirror → ASE espurio en stores válidos); ahora **Old-pinned** como todo
mirror. Y `anewarray` nombraba arrays anidados `[L[J;` en vez de `[[J` — corregido en intérprete
(write+read path) y verificador. Regresión: `nested_array_stores_survive_minor_gc` → 64.

### 4. `StackOverflowError` — JVMS §6.3 · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
`MAX_FRAMES = 2000` chequeado en `push_frame_locked` (embudo de los 5 invokes), antes de adquirir
el monitor (no filtra locks de métodos `synchronized`); `call_java` cubierto vía
`pending_exception`. Test `SoTest` → 42, oráculo completo; la recursión legítima sigue pasando.

### 5. Autoboxing / wrappers — JLS §5.1.7 (JSR 201) · ~~STUB~~ → **✅ ARREGLADO** (2026-08-12)
Los 8 wrappers reales en bootstrap: `Integer`/`Long`/`Short`/`Byte` con `valueOf` + cache
−128..127 (identidad `==` verificada: `100==100` true / `200==200` false), `Character` (0..127),
`Boolean` (TRUE/FALSE canónicos), `Double`/`Float` sin cache; `xxxValue`/`equals`/`hashCode`.
Test `BxTest` → 42, oráculo completo. **Hallazgo colateral:** el test destapó un **segundo
reproducer del heisenbug os-parallel** — single-thread, ~50%/corrida: `Long.<clinit>` con Eden
casi lleno → `ArithmeticException` espuria sin división, que `catch (ArithmeticException)` NO
atrapa pero `catch (RuntimeException)` sí (corrupción de estado de control, no de valores;
`JVM_GC_VERIFY` no dispara). Reproducers conservados: `java/BxDbgT.java` / `java/BxDbgY.java` —
anotado en la memoria del bug como punto de partida del próximo arco.

### 6. `Object.clone()` + `Cloneable` — JLS §10.7 · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
Copia shallow interceptada en `invokevirtual` (post-resolución) e `invokespecial`
(`super.clone()`), chequeo de `Cloneable` → `CloneNotSupportedException`; **arrays incluidos**
(`"[I".clone()` pre-vtable). Clon alocado en **Old** (una alocación Eden podría disparar un minor
que mueva el fuente a mitad de copia); referencias vía write barrier. Test `CnTest` → 42 (4
caminos), oráculo completo.

### 7. `OutOfMemoryError` — JVMS §6.3 · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
El agotamiento es **recuperable para las alocaciones de bytecode** (`new`/`newarray`/`anewarray`/
`multianewarray` → `try_malloc` → throw); las internas del VM (intern, mirrors, promociones del
GC) conservan el panic, documentado. Test `OmTest` → 42 (recursión rooteando 512 KiB/frame agota
los 16 MiB reales), oráculo completo.

## 🟡 Ausentes de menor urgencia / superficie de API

### 8. Reflexión mínima — `Class.getName()` · ~~AUSENTE~~ → **✅ ARREGLADO parcial** (2026-08-12)
`getName()`/`getSimpleName()` nativos sobre el mirror, formato JDK completo (clases con puntos,
arrays `[Ljava.lang.String;` / `String[]`/`int[]` con dimensiones). Test `GnTest` → 42 (incl.
`String.class.getName()` vía ldc de Class), oráculo completo.
`forName`/`newInstance`/`java.lang.reflect.*` siguen pendientes (otra liga — Fase C).

### 9. Anotaciones en runtime — JSR 175 (Java 5) · **PARCIAL** [inspección]
`RuntimeVisibleAnnotations` se **parsea** (A0/javap la muestra) pero no hay API runtime
(`Class.getAnnotation(...)`). Sin reflexión (#8 versión completa) no tiene consumidor — anotar
como dependiente de Fase C.

### 10. `java.lang.ref` parcial — Soft/Phantom/Cleaner · **PARCIAL** [inspección]
`WeakReference` + `ReferenceQueue` hechos (A6). Faltan `SoftReference` (semántica de presión de
memoria — decisión de política de GC), `PhantomReference` y `Cleaner`. `finalize()` está
deprecado-for-removal en el JDK real: **razonable no implementarlo nunca** (documentar la decisión).

### 11. Ciclo de vida de la VM — `System.exit`/`Runtime` · **AUSENTE** [inspección]
No hay `System.exit(int)`, `Runtime.getRuntime()`, shutdown hooks. La VM termina cuando `main`
retorna. Para el modelo actual (tests `run() -> int`) alcanza; para el lanzador `java` real
(Fase D) hará falta `exit` como mínimo.

### 12. Periféricos de `Thread` restantes · **AUSENTE** [inspección]
`ThreadGroup`, `sleep(long,int)`, `onSpinWait()`, `setUncaughtExceptionHandler`. Todos de baja
prioridad; `daemon`/`priority`/`ThreadLocal` ya están (2026-08-12).

### 13. Regla fina de init de interfaces — JVMS §5.5 · **SIN VERIFICAR** [inspección]
La spec dice: inicializar una clase NO inicializa sus superinterfaces (salvo que declaren
defaults), y una interface solo se inicializa por el primer uso de *sus propios* statics.
Nuestro `ensure_initialized` recorre solo la cadena de superclases — probablemente correcto por
accidente para interfaces sin `<clinit>` encadenado, pero la regla de los defaults (cuando se
arregle #1) hay que modelarla. Verificar junto con #1.

## ✅ Cubierto (verificado, para contexto)

| JSR / área | Estado |
|---|---|
| JSR 924/202 — class file + set de opcodes | ✅ 199/199 alcanzables; `jsr/ret` excluidos por diseño (§4.9.1) |
| JSR 202 — verificador con `StackMapTable` | ✅ cobertura completa del set ejecutable |
| JSR 133 — JMM (Java 5) | ✅ relajado y fiel (volatile Acq/Rel, no-tearing); falta `final`/`VarHandle` (ya en roadmap) |
| JSR 292 — `invokedynamic`/`MethodHandle` (Java 7) | ✅ 5/6 fábricas; `altMetafactory` fuera de alcance (serialización) |
| JSR 335 — lambdas (Java 8) | ✅ `LambdaMetafactory` genera clases reales · ✅ default methods (ítem #1, arreglado 2026-08-12) |
| JSR 166 — j.u.c (Java 5+) | ✅ núcleo + utilidades (AQS, pools, colas, CHM, CF); volumen restante en roadmap |
| Sealed/records/pattern switch (Java 17+) | ✅ `ObjectMethods`, `typeSwitch` (enum y tipo) |
| Excepciones/`<clinit>`/`Throwable` | ✅ implícitas + `ExceptionInInitializerError` + stack traces (2026-08-12) |
| JSR 376 — módulos (Java 9) | ⛔ fuera de alcance declarado |
| JSR 51/203 — NIO | ⛔ Fase C (bibliotecas), no VM |

## Recomendación de orden (pre-JIT)

1. ~~**#1 default methods**~~ ✅ hecho (2026-08-12, `DefProbe` → 115).
2. ~~**#2 uncaught → stack trace + exit limpio**~~ ✅ hecho (2026-08-12, `UncTest` → 42).
3. ~~**#3 `ArrayStoreException`**~~ ✅ hecho (2026-08-12, `AsTest` → 42; + 2 bugs latentes de mirrors/naming arreglados).
4. ~~**#4 `StackOverflowError`**~~ ✅ hecho (2026-08-12, `SoTest` → 42).
5. ~~**#5 autoboxing/wrappers**~~ ✅ hecho (2026-08-12, `BxTest` → 42; + nuevo reproducer del heisenbug).
6. ~~**#6 `clone()`/`Cloneable`**~~ ✅ hecho (2026-08-12, `CnTest` → 42, arrays incluidos).
7. ~~**#7 `OutOfMemoryError`**~~ ✅ hecho (2026-08-12, `OmTest` → 42).
8. ~~**#8 `Class.getName()`**~~ ✅ hecho (2026-08-12, `GnTest` → 42; `forName`/reflect → Fase C).
9. El resto (#9–#13: anotaciones runtime, Soft/Phantom/Cleaner, `System.exit`/`Runtime`, periféricos de `Thread`, regla fina de init de interfaces) según demanda, o post-JIT.
