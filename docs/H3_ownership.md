# H3 — Diseño de *ownership* para sacar el GIL

> Documento de diseño previo al código. Fija **qué estado del `JVM` es compartido y cuál
> por-hilo**, en qué orden se toman los locks, en qué orden se sueltan los opcodes, y dónde
> está la línea del JMM. No cubre el punto 4 (JMM): eso es H4, y arranca recién con 1d hecho.

## 0. Objetivo y no-objetivo

- **Objetivo (punto 1):** que dos hilos de SO puedan ejecutar opcodes **sin** el `Arc<Mutex<JVM>>`
  global, reemplazándolo por (a) estado por-hilo que corre lock-free y (b) estado compartido
  detrás de locks finos.
- **No-objetivo acá:** la visibilidad de memoria del *programa Java* (`volatile`/fences). Bajo
  el modelo fino, los **bytes de los objetos** en el heap NO los protege ningún lock de la VM;
  las carreras sobre campos son problema del programa (se resuelven en H4). La VM solo
  sincroniza la **metadata** del allocator y las estructuras internas, no el contenido de los objetos.

## 1. Inventario de campos del `JVM` (struct en `bytecode_interpreter.rs:225`)

| Campo | Categoría | Destino de sync |
|-------|-----------|-----------------|
| `frames: Vec<Frame>` | **por-hilo** (pila del hilo activo) | mueve al contexto por-hilo — lock-free |
| `current: usize` | **por-hilo** (índice del que corre) | ⚠️ deja de ser global — cada hilo conoce su `idx` |
| `threads: Vec<GreenThread>` | **compartido** (registro) + slots casi-privados | `Mutex` sobre el registro; los campos privados del slot los toca solo su dueño |
| `metaspace` | **compartido**, read-mostly | `RwLock` + lock de init por clase (protocolo JLS) |
| `heap` | **compartido** | allocator: `Mutex` sobre cursores/free-list + **TLABs**; bytes de objetos: sin lock (línea JMM) |
| `monitors: HashMap` | **compartido**, muy mutado | `Mutex` primero; thin-locks/CAS por objeto después (se cruza con H5) |
| `lambdas: HashMap` | **compartido**, write-once por clave | `RwLock` / mapa concurrente |
| `condy`, `condy_in_progress` | **compartido**, resolución write-once | `Mutex` + protocolo "primero gana, el resto espera" |
| `console: String` | **compartido** | `Mutex<String>` (o buffers por-hilo que se mergean) |
| `next_thread_id`, `java_thread_counter` | **compartido**, contadores | atómicos |
| `gc_requested`, `halt` | **compartido**, flags | `AtomicBool` |
| `steps` | **compartido**, reloj lógico | atómico **relajado/aproximado** — solo alimenta heurísticas de GC (ver §3) |
| `last_gc_step`, `last_gc_used` | **compartido**, baselines de GC | escritos solo en STW → viven bajo el lock de GC |
| `gc_policy`, `mode` | **inmutables** tras el arranque | se comparten sin lock |

## 2. Las tres decisiones duras

Todo lo demás es mecánico una vez resueltas estas:

- **`current` → índice por-hilo.** Hoy `self.current` se lee en todos lados (`self.threads[self.current]`,
  `parked`, `run_one`). En paralelo "el hilo que corre" no es global: cada hilo de SO opera sobre
  **su** `idx`. La migración es enhebrar el `idx` explícitamente en `run_one` y los handlers en vez
  de leer `self.current`. Es lo más invasivo de 1a, y se hace **bajo el GIL** (sigue habiendo un solo
  hilo, comportamiento idéntico) para dejar el plumbing listo antes de 1d.
- **`frames` → contexto por-hilo.** Ya está casi: la pila viva se intercambia con `threads[idx].frames`
  vía `activate`/`deactivate`/`parked`. Formalizar que la pila **es** del hilo y no un campo global
  prestado. Es el compañero natural de la decisión de `current`.
- **`steps` (reloj lógico) en paralelo.** Un contador global incrementado por-opcode es un punto de
  contención y semánticamente raro ("¿los steps de quién?"). Decisión: `steps` pasa a atómico
  **relajado**, solo para las heurísticas de disparo del GC (aproximado alcanza). Los *timed waits*
  ya usan **wall-clock** en modo OS, así que no dependen de `steps`.

## 3. Asignación de locks y **orden de adquisición** (anti-deadlock)

Regla dura: **un único orden total** de locks; siempre se toman en ese orden, nunca al revés.

```
metaspace  <  heap  <  monitors  <  registro(threads)  <  console  <  gc
```

- Un handler que necesite dos locks los toma en ese orden y los suelta en el inverso.
- El GC (último) puede tomar todo lo de arriba durante el STW; nadie toma el de GC y después otro.
- Los contadores atómicos y los flags no participan del orden (no bloquean).
- Objetivo: mantener las secciones críticas **chicas** — tomar, mutar, soltar; nunca bloquear
  (`park`/`sleep`) con un lock tomado (ya es la invariante del `OsTick` actual).

## 4. La línea del JMM (qué NO protege la VM)

- **Sí** protege: cursores del heap, free-list, tabla de monitores, metaspace, registro de hilos,
  contadores. Corromper esto es un bug de la VM.
- **No** protege: los bytes de un objeto (`getfield`/`putfield`). Dos hilos que pisan el mismo campo
  sin `synchronized`/`volatile` es una carrera **del programa Java** — comportamiento indefinido por el
  JMM, igual que en la JVM real. La VM solo garantiza que el *allocator* no se corrompa, no que las
  lecturas vean la última escritura. Esa garantía es H4.

## 5. Orden de liberación del GIL (§ opcodes, en 1d)

De más seguro a menos:

1. **Frame-local puros** — aritmética, `load`/`store`, `dup`, `goto`, comparaciones, ramas. Tocan
   solo el contexto por-hilo. Corren sin ningún lock global apenas `current`/`frames` son por-hilo.
2. **Lecturas de metaspace** — resolución de call/field ya cacheada → `RwLock` en lectura.
3. **Acceso a campos de objeto** (`getfield`/`putfield`, arrays) — sin lock de VM (línea JMM §4);
   solo la *dirección* tiene que ser estable (la da el safepoint del GC, §7).
4. **Asignación** (`new`/`newarray`) — TLAB lock-free; lock solo en refill.
5. **Monitores** (`monitorenter`/`exit`, `wait`/`notify`) — lock de monitores / CAS.
6. **Init de clase, condy, spawn de hilos** — los protocolos con lock dedicado.

## 6. Safepoint / stop-the-world (el "punto 2", que aparece en 1d)

Hoy el STW **es el GIL**: llegar a `safepoint()` (`run_one:1172`) con el lock tomado ya implica mundo
detenido. Al soltar el GIL hay que reconstruirlo explícito:

- Flag global `AtomicBool gc_pending`.
- **Poll** en los back-edges y en el slow-path de asignación: si `gc_pending`, el hilo se estaciona
  en el safepoint y avisa (check-in).
- El iniciador espera a que **todos los `Runnable` hayan hecho check-in** (los `Blocked`/`Waiting`/
  `TimedWaiting` **ya** están seguros — H1 los dejó identificables vía `block()`), corre el GC, libera.
- La carrera de mover objetos con hilos corriendo se elimina porque ninguno mantiene punteros crudos
  a través de un safepoint: las raíces se enumeran por-hilo (`roots()`/`parked`, ya existe).

## 7. Secuencia de rebanadas y qué valida cada una

Cada una mantiene `os_parallel_matches_oracle` verde (green ≡ os-gil ≡ os).

- **1a — Formalizar el borde. ✅ hecho.** Resultó ser documentación: bajo el GIL el borde a nivel
  código *es* el split de la struct (abajo), y los accesores pelean con el borrow checker sin reducir
  riesgo. El borde quedó documentado en la struct.
- **Split estructural. ✅ hecho.** `frames`+`current` → tipo **`RunningCtx`** (contexto por-hilo);
  todo el estado compartido → tipo **`SharedVm`**. Hoy `JVM { shared: SharedVm, running: RunningCtx }`,
  todavía entero tras el `Arc<Mutex<JVM>>`. Comportamiento idéntico, oráculo verde. Es el boundary de
  tipos que las rebanadas siguientes necesitan.
- **1c — `SharedVm` por-lock + `RunningCtx` por-hilo. ⬜ el gran paso.** Sacar `running` del `JVM`
  compartido para que cada hilo de SO lo posea, poner `SharedVm` tras `Arc` con locks finos (orden §3),
  y cambiar el *receiver* de cada handler de `&mut self` a operar sobre `(&mut RunningCtx, &SharedVm)`.
  Todo-o-nada: no compila hasta completo. Se puede preservar el oráculo empezando con **un** `Mutex`
  sobre `SharedVm` antes de afinar los locks.
- **1b — TLABs.** Buffer de Eden por hilo. Ahora sí con beneficio (la asignación contiende). Va con/después de 1c.
- **1d — Soltar el lock.** Por grupos de opcodes (§5), ensanchando. Acá entra el safepoint real (§6).
  Primer punto con paralelismo medible.

Después de 1d: **punto 4 (H4, el JMM)**.

## 8. Decisiones ratificadas

1. **`RunningCtx` como tipo** (no un `running_idx` "global"). ✅ ya implementado como `struct RunningCtx`.
   En 1c pasa a ser propiedad de cada hilo de SO; el `idx` se enhebra donde haga falta (decisión-1).
2. **Orden de locks** `metaspace < heap < monitors < registro < console < gc` (§3). ✅ ratificado.
3. **Línea del JMM** (§4): la VM **no** protege los bytes de objetos; la visibilidad es problema del
   programa Java, resuelta en H4. ✅ ratificado.

## 10. Diseño de 1d — soltar el lock (ratificado)

Es una **sola pieza concurrente acoplada** (no hay brick verde intermedio) y el oráculo serializado
**no valida thread-safety** — la verificación es un stress test paralelo (señal, no prueba).

**Decisiones ratificadas:**
1. **Bytecode sin lock → code cache en `RunningCtx`.** Copia del slice de código del frame actual,
   refrescada en invoke/return (que ya son ops compartidas que lockean). El código de un método es inmutable.
2. **Subconjunto conservador para arrancar:** solo frame-local sin referencias — aritmética int/long,
   `load`/`store`, `iinc`, `goto`, ramas int. Ensanchar después (aload/astore/dup → luego).
3. **Poll de safepoint en back-edges + antes de lockear** (estándar HotSpot). Acota la latencia del GC
   sin costo por-opcode-recto.

**Mapa de integración (dónde toca el código):**
- `RunningCtx` += `code: Vec<u8>` (+ el `MethodId` cacheado para detectar cambio de frame).
- Fast-path: un loop nuevo sobre `RunningCtx` que replica el subconjunto conservador del `match opcode`
  de `run_one` (bc_interpreter.rs:1264), leyendo de `running.code`; retorna al ver un opcode fuera del set.
- `os_thread_loop`: corre el fast-path lock-free; toma `SharedVm` solo para el opcode compartido siguiente.
- Safepoint gate: `Arc<...>` con `requested: AtomicBool` + check-in (Mutex+Condvar). El que va a colectar
  lo pide, espera a que todos sincronicen su frame al slot y se estacionen, colecta, libera.
- Stress test: N hilos con loops de cómputo + asignación/GC, muchas corridas, resultado determinista + sin
  deadlock. **Señal, no prueba** de ausencia de races.

**Riesgo:** deadlock sutil en el handshake / race no cubierta por el oráculo. Se implementa como esfuerzo
enfocado y dedicado, no apurado.

## 9. Estado (actualizar cada checkpoint)

- ✅ Rename `os`→`os-gil`; clase `OsParallel` (`JVM_THREADS=os`) + test `os_parallel_matches_oracle`.
- ✅ Split estructural: `RunningCtx` + `SharedVm`. `JVM { shared, running }`, todavía tras un `Mutex`.
- ✅ **1c-i — receiver a la vista.** `JVM` quedó como **owner** (`{ shared, running }` + `new()` + `exec()`);
  todos los handlers/scheduler pasaron a `impl Exec<'_>` sobre `Exec<'a> { shared: &mut SharedVm, running:
  &mut RunningCtx }`. Green/os/visualizador/tests construyen la vista con `.exec()`. Cuerpos de handlers
  intactos. Todavía `Arc<Mutex<JVM>>` (running en el owner, serializado) — sin paralelismo aún, pero el
  receiver ya toma borrows separados. 121 tests verdes.
- ✅ **1c-ii — `running` thread-local, lock acotado a `SharedVm`.** Los 4 free-fns del driver OS
  (`run_os_threaded`/`os_thread_loop`/`os_block_tick`/`spawn_pending`) pasaron de `JVM`/`Arc<Mutex<JVM>>`
  a `SharedVm`/`Arc<Mutex<SharedVm>>`. Cada hilo de SO posee un `RunningCtx` local y lo aparea con el
  guard por opcode (`Exec { running: &mut local, shared: &mut guard }`). **El `Arc<Mutex<JVM>>` de
  toda-la-VM ya no existe** — solo se lockea `SharedVm`. Sin tocar handlers. 121 tests verdes (incluidos
  los de OS real + el oráculo `os_parallel`). Todavía **un lock por opcode → serializado**.
- ✅ **1d — soltar el lock. HECHO (subconjunto conservador).** El motor `os` corre opcodes
  **frame-local lock-free** en paralelo real; solo `SharedVm` se lockea. Piezas:
  - **Code cache** (`RunningCtx.code`/`code_method`, `sync_code_cache`): bytecode desde memoria thread-local.
  - **Fast-path** (`run_frame_local`): aritmética int / stack shuffles / ramas int corren sobre el
    `RunningCtx` sin lock; todo lo demás cae al `run_one` lockeado. Clasificación **conservadora** → un
    error solo puede quitar paralelismo, nunca correr un op compartido sin lock.
  - **Safepoint STW cooperativo** (`gc_pending` atómico + `park`/`unpark` + `coordinate_gc`): el GC que
    mueve objetos frena a todos (sync de frames al slot), colecta, remapea, despierta. `safepoint()`
    difiere al driver vía `gc_by_driver`.
  - **`os-gil` intacto** como referencia serializada y fallback.
  - **Verificación:** oráculo (`os` funcional ≡ `os-gil` ≡ green) + **stress test** `os_parallel_stress`
    (3 hilos, cómputo lock-free + GC handshake, 20 corridas). *Señal, no prueba* de ausencia de races.
  - ✅ **W1 — fast-path completo.** El subconjunto lock-free cubre **todos** los opcodes frame-local:
    int/long/float/double (arith, shifts, bitwise, negación, div/rem de float/double), **loads/stores**
    tipados (reusan `iload`/`istore`), constantes, **conversiones** (`i2l`..`i2s`), **referencias**
    (`aload`/`astore`/`aconst_null`) e `if_acmp`. Quedan afuera *a propósito*: `idiv`/`irem`/`ldiv`/`lrem`
    (tiran `ArithmeticException`), y array/field/cp ops (tocan heap/cp). Oráculo cruzado + stress.
  - 🟡 **W2 — TLABs (en curso).** Asignación lock-free; el peligro (`unsafe`) está concentrado en W2c.
    - ✅ **W2a — Eden estable.** El buffer del heap (`memory`) ahora **pre-reserva su capacidad** al máximo
      (`JVM_GC_MAX_HEAP`, default 16 MiB) → **nunca realoca** al crecer Old (garantía de `Vec`: no realoca
      con `len ≤ capacity`; `resize` panica "heap exhausted" si se excede, nunca realoca). Las direcciones
      de bytes son **estables de por vida** — la base que los raw pointers de W2c necesitan. Test dedicado:
      `heap_buffer_address_is_stable_across_old_growth`. Behavior-idéntico (oráculo verde).
    - ✅ **W2b — log de asignación por-hilo.** `HeapService` tiene `pending: Vec<Vec<Allocation>>`
      (indexado por slot de hilo) + `current_thread` (seteado por `activate` en cada context switch). Los
      `malloc` de Eden loguean en `pending[current_thread]` en vez del `objects` compartido; `commit_pending()`
      los vuelca a `objects` **en cada entrada de GC** (vía `parked`), así las tripas del colector no cambian.
      (Old sigue logueando directo a `objects`.) Behavior-idéntico (oráculo verde). Bajo `.write()` el índice
      compartido alcanza; W2c lo hace verdaderamente thread-local. Ajusté 2 tests que llamaban `sweep`/inspeccionaban
      `allocations()` sin pasar por el commit del GC.
    - 🟡 **W2c — asignación lock-free (`unsafe`).** El **corazón `unsafe` está hecho y verificado con Miri**:
      `EdenArena` (`src/jvm/interpreter/eden_arena.rs`) — un bump-allocator lock-free con los bytes en
      `UnsafeCell` (la única forma *sound* de escribir memoria compartida desde `&self`; cualquier `*mut`
      derivado de un `&`/`&mut` compartido es UB por aliasing). `fetch_add` reserva rangos **disjuntos**, el
      backing store nunca realoca (W2a) → las escrituras raw no tienen data races. Verificado: `cargo +nightly
      miri test eden_arena` — el test concurrente `concurrent_bump_is_race_free` (4 hilos asignando+escribiendo)
      pasa bajo Miri (cero data races / violaciones de aliasing). `unsafe impl Sync` justificado con la invariante.
      - ✅ **Integración de storage.** `HeapService` reemplazó el `eden_cursor` + la región Eden de `memory`
        por un campo `eden: EdenArena`. Un helper `in_eden(offset)` rutea **cada** accesor de bytes
        (`read/write_u8..u64`, `read_bytes`→`Vec`) al arena para offsets de Eden, a `memory` para el resto;
        `malloc` usa `eden.alloc()`, el minor evacúa desde el arena (`evacuate_block` cruza buffers byte a
        byte) y lo `reset`ea. Behavior-idéntico (126 tests verdes). Todavía **bajo el lock** → el `unsafe` es
        trivialmente sound (serializado).
      - ✅ **Flip a `.read()` — asignación concurrente.** `pending` pasó a `Vec<Mutex<Vec<Allocation>>>`
        (push lock-free por slot); `HeapService::alloc_object_lockfree(&self, size, class_id, idx)` bumpea
        Eden + escribe el header + loguea, todo por `&self`; `objects_operations::allocate_read` resuelve
        tamaño/class-id read-only (reusa `instance_field_slots_read` + `class_id_read`); y **`new` (0xbb)
        entró al read-path** de `run_read_shared` — alloc lock-free si la clase está `Done`, si no escala
        (el `<clinit>` es write). Verificado: oráculo cruzado + hammer, **y Miri** con el test integrado
        `concurrent_alloc_and_read_of_published_is_race_free` (alloc de bloques frescos concurrente con
        lecturas de objetos publicados — la forma de un `new` corriendo a la par de un `getfield`). Cero UB.
      - ⬜ Se ensancha más: `newarray`/`anewarray` (arrays) al read-path (mismo patrón).
  - ✅ **W3 — locks finos (vía `RwLock<SharedVm>`).** El path paralelo pasó de `Mutex` a `RwLock`
    (deadlock-free, un lock). Los opcodes compartidos **de solo-lectura** corren bajo `.read()`
    **concurrentes**; los que escriben, bajo `.write()`. Read-path: **`getfield`** (`getfield_read` +
    resolución read-only con `get()`), **`arraylength`** y **array-loads** (`iaload`..`saload`, vía
    `array_load_read`). Todos escalan a `.write()` con *pop-y-restaurar* si tocan una condición de throw
    (receiver/array null, índice fuera de rango, clase no cargada) — el stack queda intacto para que el
    write-path lance la excepción. Validado por oráculo cruzado + stress con **getfield/arraylength/iaload
    concurrentes** (~200k lecturas/corrida en 3 hilos). Se ensancha más con resolución de métodos
    (`invoke*` cache-hit). `os-gil` sigue en `Mutex`, intacto como oráculo.
    - ⬜ El **heap sigue siendo una estructura** bajo el `RwLock`, así que *escrituras* de campo/array y
      asignación aún serializan en `.write()` (eso lo rompería W2).
- ⬜ H4 (JMM — ya con paralelismo real que puede reordenar).
