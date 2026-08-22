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

### 9. Anotaciones en runtime — JSR 175 · ~~PARCIAL~~ → **✅ ACOTADO Y CERRADO** (2026-08-12)
`Class.isAnnotationPresent(Class)` nativo: mirror del receptor → `RuntimeVisibleAnnotations` →
comparación de descriptores contra el mirror del argumento. Reusa el parser que ya existía
(nueva vista `type_descriptors` junto a los renderers de javap) + `ClassFile::
runtime_visible_annotation_types()`. Hubo que sumar `java/lang/annotation/Annotation` al boot
classpath: sin ella el `.class` de una anotación **ni carga** (la nombra en su tabla de interfaces).
Test `AnTest` → 42, con scoring que un nativo trabado no puede fingir (siempre-false da 22,
siempre-true da 10; incluye un mirror primitivo). **Fuera de alcance, deliberado:**
`getAnnotation()` devolvería un *objeto* anotación, que exige sintetizar una clase proxy por
`@interface` (lo que el JDK hace con `AnnotationInvocationHandler` + dynamic proxies) — sin
proxies ni modelo de `Method` no hay nada que devolver; tampoco anotaciones de campos/métodos,
`@Inherited`, ni `RuntimeInvisibleAnnotations` (que es justo la regla de `RetentionPolicy.CLASS`).
No se inventó API no estándar como sustituto.

### 10. `java.lang.ref` — Soft/Phantom · ~~PARCIAL~~ → **✅ ARREGLADO** (2026-08-12)
`PhantomReference`: `get()` sobrescrito a `null` siempre, pero el campo `referent` heredado sigue
apuntando al objeto — así el GC detecta la muerte y encola igual, mientras el override impide
resucitar nada (como el JDK). `SoftReference`: la política vive en el **GC**, no en Java — un
`SoftPolicy::{Retain, Clear}` la concentra en un punto. Regla: el referent se traza como arista
**fuerte** en toda colecta mayor **salvo** cuando la colecta la pidió el heap por presión
(ocupancia / out-of-space / tasa de alocación), donde se traza débil y muere como un weak. Todo lo
demás (limpiar, encolar, liberar) se deriva de los bits de marca. Verificado en ambas direcciones:
por defecto `RfTest` → 42; forzando presión (`JVM_GC_CAPACITY=64 JVM_GC_OCCUPANCY=0.1`) → 36,
exactamente los 6 puntos del chequeo `sr.get() != null`. **`Cleaner` fuera de alcance.**
`finalize()` **no se implementa por decisión** (deprecado-for-removal en el JDK real).

### 11. Ciclo de vida de la VM — `System.exit`/`Runtime` · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
`System.exit(int)` interceptado en `invokestatic.rs` (tras el pop de argumentos, antes del bridge
de nativos) por una razón estructural: `natives::dispatch` devuelve `Option<Value>` —un valor para
apilar—, así que solo puede *continuar* la ejecución; terminar exige responder con un `Step`.
`vm_exit(status)` limpia la pila de frames **sin** `pop_frame` (nada de liberar monitores ni
desenrollar: justamente el apagado ordenado que `exit` no debe hacer), despierta a los threads
parkeados y propaga el status como resultado del programa desde cualquier thread (3 sitios del
driver: green, os-gil, os-parallel). `Runtime` con `getRuntime()`/`exit`/`availableProcessors()`.
Test `ExTest` → 42, contrastado con el `java` real del JDK 25 (mismo class file → exit code 42, sin
imprimir: ni el `finally` ni el `return` posterior corren). **Shutdown hooks fuera de alcance:** un
hook es un `Thread` que la VM debería *correr* justo en el camino que tiene que dejar de ejecutar
bytecode.

### 12. Periféricos de `Thread` restantes · ~~AUSENTE~~ → **✅ ARREGLADO** (2026-08-12)
**`UncaughtExceptionHandler` se invoca de verdad en Java** (no queda solo almacenado): interface
anidada + handler por-thread + default estático, y `athrow.rs::dispatch_uncaught` lo llama con
`call_virtual`. El detalle fino: el lookup **aloca** (el `Thread` de `main` se fabrica al vuelo), y
con la pila ya vacía no quedaría ninguna raíz de GC sosteniendo la excepción — así que el dispatch
corre **antes** de popear el último frame, con la excepción parkeada en su operand stack (el mismo
truco de `capture_backtrace`). Un handler que lanza queda contenido por el `exception_floor` ya
existente: se descarta y se imprime el reporte por defecto de la excepción **original** — no hay
recursión posible. Además `sleep(long,int)` (con el redondeo del JDK), `onSpinWait()` (no-op
documentado) y `ThreadGroup` (registro **enteramente en Java**, cero bookkeeping en el VM;
aproximación documentada: los miembros no se desenlazan, así que `activeCount()` cuenta los vivos —
lo que coincide con el contrato de "estimate" del JDK). Test `UhTest` → 42, contrastado con el JDK
real (que también da 42).

### 13. Regla fina de init de interfaces — JVMS §5.5 · ~~SIN VERIFICAR~~ → **✅ BUG ENCONTRADO Y ARREGLADO** (2026-08-12)
Había un desvío real: `ensure_initialized` recorría solo la cadena de superclases, así que el
`<clinit>` de una interface **con default methods nunca corría** — algo que recién importa ahora
que los defaults funcionan (#1). Medido con probes de contadores y campos no-constantes (un
`static final int X = 5` lo inlinea javac y no dispara init): fallaban el caso directo y el
**indirecto**; los otros ya eran correctos. Fix: `default_method_superinterfaces()` hace BFS sobre
superinterfaces directas e indirectas quedándose con las que declaran un método no-static/
no-private **con `Code`** (el mismo test de "default" que usa `build_vtable`), y **devuelve vacío
si la clase es ella misma una interface** — así "inicializar una interface nunca inicializa sus
superinterfaces" queda codificado en el tipo de retorno, no en un `if` suelto. Test `IiTest` → 42
(4 casos + 2 controles).

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
9. ~~**#9 anotaciones runtime**~~ ✅ hecho (2026-08-12, `AnTest` → 42; `getAnnotation` fuera de alcance).
10. ~~**#10 Soft/Phantom refs**~~ ✅ hecho (2026-08-12, `RfTest` → 42; `Cleaner` fuera de alcance).
11. ~~**#11 `System.exit`/`Runtime`**~~ ✅ hecho (2026-08-12, `ExTest` → 42; shutdown hooks fuera de alcance).
12. ~~**#12 periféricos de `Thread`**~~ ✅ hecho (2026-08-12, `UhTest` → 42; handler invocado en Java de verdad).
13. ~~**#13 init de interfaces §5.5**~~ ✅ hecho (2026-08-12, `IiTest` → 42; era un bug real).

**Auditoría A7 CERRADA: 13/13.** Lo único que queda son las piezas marcadas *fuera de alcance* con
su razón (`getAnnotation` sin dynamic proxies, `Cleaner`, shutdown hooks, `finalize()` por decisión)
y lo que depende de reflexión completa → Fase C. Suite: **820 passed, 0 failed**.
