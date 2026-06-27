# TODO

Pendientes y mejoras conocidas. Lo marcado `[x]` es registro de lo ya hecho.

## Estado general

- ✅ **Sistema de tipos completo** — `int`/`long`/`double`/`float` ejecutados y
  verificados (cómputo, conversiones, comparaciones, división con excepción,
  categoría-2 en params/campos/estáticos/arrays/frames, lattice de referencias).
- ✅ **GC generacional funcional** — young (Eden + 2 survivors) con colector por
  **copia** (minor: evacúa/promueve, write barrier + remembered set para old→young) y
  Old con **mark-sweep-compact**; marcado transitivo correcto, política de
  fragmentación, disparadores sobre safepoint (minor por Eden lleno; major/full por
  occupancy/explicit).
- ✅ **Verificador JVMS-estricto** — gate estructural (§4.9: targets en inicio de
  instrucción, no fall-off, `jsr`/`ret`, tabla de excepciones) antes del type-checking;
  tipos de locales al leer + cota de `max_stack`; reglas de objetos sin inicializar
  (`UninitializedThis`, `<init>` una sola vez, no usar medio construidos); handlers de
  excepción (`catch_type ⊑ Throwable`, `athrow`/`areturn` tipados); acceso/linkage
  (`<init>`/`<clinit>`, `count` de `invokeinterface`, regla `protected`); y verifica
  **con o sin StackMapTable** (inferencia por punto fijo como fallback). Detalle abajo.
- ⬜ **Lo que queda** es sobre todo *profesionalización del GC* (generacional,
  concurrente, referencias débiles, finalizers, class unloading) y pulir el disparador
  predictivo. El verificador además **cruza la StackMapTable contra la inferencia**
  (`cross_check_stackmap`: tras el chequeo lineal, corre el punto fijo y exige que cada
  frame declarado no contradiga lo inferido — misma altura de pila, slots compatibles)
  y modela `tableswitch`/`lookupswitch`.

## Verificador — endurecimiento hacia JVMS-estricto

- [x] **#2 Chequeo estricto de locales + `max_stack`**: `iload`/`lload`/`dload`/`fload`
  verifican que el local tiene el tipo correcto (`load_local`); `aload` que es una
  referencia (`is_reference`); y la profundidad de la pila (en slots) nunca excede
  `max_stack`. Verificado: los demos siguen pasando (sin falsos positivos) + test
  unitario que rechaza un `iload` sobre un local de referencia.
- [x] **#3 Reglas de objetos sin inicializar**: en un constructor `this` arranca como
  `UninitializedThis` (salvo `Object.<init>`, la raíz) hasta que su `super(…)`/`this(…)`
  lo inicializa. `use_reference` exige una referencia **inicializada** en todo *uso*
  real (receptor de `getfield`/`putfield`/`invoke*`, operandos de `acmp`/`instanceof`/
  `checkcast`/array) — un `aload`/`astore`/`dup` sí puede mover un sin-inicializar.
  `invokespecial <init>` solo apunta a un objeto sin inicializar y **exactamente una
  vez** (tras inicializarlo, una segunda llamada saca un `Reference` y se rechaza). Y
  `assert_no_uninitialized` corta dos casos: un sin-inicializar vivo en un *backward
  branch* (no sobrevive a un loop) y un `this` sin inicializar al `return` del
  constructor. Verificado: `Init.<init>` (super + `putfield this`) y `Garbage.<init>`
  verifican, `Init.run → 21` corre; tests unitarios de `use_reference` y
  `assert_no_uninitialized`; sin regresión en los `run` de los demos.
- [x] **#4 Handlers de excepción**: `verify_exception_table` valida cada fila de la
  tabla — `catch_type ⊑ Throwable` (y `catch_type == 0` = `finally` = cualquier
  `Throwable`), y que el `handler_pc` entre con **un solo** operando (la excepción)
  asignable al frame del StackMapTable. `athrow` exige `Throwable`; `areturn` (que
  *faltaba* en verificador **e** intérprete) chequea contra el tipo de retorno del
  método y se ejecuta vía `ireturn` (genérico). Verificado: `Exc` (throw explícito +
  catch por supertipo, dos handlers tipados, `areturn` de `String`) verifica **y**
  corre (`run → 31`); test unitario que rechaza un `catch_type` no-`Throwable` y un
  handler con la pila mal formada; `Arith.divZero` sin regresión.
- [x] **#5 Restricciones estructurales (§4.9)**: `structural_check` corre **antes** del
  type-checking (así el data-flow confía en el CFG). Rechaza: código vacío o con la
  última instrucción truncada más allá de `code_length`; ejecución que **se cae del
  final** (la última instrucción debe transferir control); **subrutinas** `jsr`/`jsr_w`/
  `ret` (ilegales bajo el verificador por tipos, §4.10.1); cualquier **target** de
  branch/switch que no caiga en inicio de instrucción (incluye `tableswitch`/
  `lookupswitch` con su padding); y rangos de la **tabla de excepciones** vacíos, fuera
  de orden, o con start/end/handler fuera de un inicio de instrucción. Verificado: los
  demos reales pasan (loop con back-edge, try/catch, objetos); tests negativos sintéticos
  (vacío, fall-off, truncado, goto a media instrucción, `jsr`, rango malo) y un unit del
  parser de targets de `tableswitch`. Sin falsos positivos (16/16 demos).
- [x] **#7 Inferencia sin StackMapTable** (verificador legacy, JVMS §4.10.2): se
  extrajo la transición por-opcode a una función `transfer` compartida, y se montaron
  dos drivers sobre ella — el lineal con `StackMapTable` (el de siempre) y uno nuevo de
  **punto fijo por worklist** (`verify_by_inference`) que infiere el estado en cada
  punto fusionando (`join_states`, LUB del lattice) todos los caminos que llegan, con
  *seeding* de los handlers de excepción (`[tipo]` en la pila) y el corte de
  sin-inicializar en back-edges. Termina porque los estados solo suben por el lattice
  (altura finita). Se usa automáticamente cuando no hay tabla. Verificado forzando la
  inferencia sobre bytecode ramificado real (ignorando su tabla): `Recursion.fact`
  (recursión+branch), `Arith.loop`/`Cmp.sumWhile` (loops con back-edge, uno con
  acumulador categoría-2), `Zoo.run` (merges de referencias + llamadas virtuales),
  `Arith.divZero` (handler) y `Add.add` (lineal); + test unitario de `join_states`
  (merge `Dog ⊔ Animal = Animal`, rechazo por pila de distinta altura). Sin regresión:
  el driver lineal sigue dando los mismos resultados en todos los demos.
- [x] **#8 Acceso/linkage** (§4.9.1, §4.10.1.8): `<init>`/`<clinit>` no pueden ser
  target de `invokevirtual`/`invokestatic`/`invokeinterface` (y `<clinit>` tampoco de
  `invokespecial`; `<init>` sí); el `count` de `invokeinterface` debe igualar los slots
  de argumentos (1 receptor + ancho de cada param, `long`/`double` = 2) y el 4º byte ser
  0; y la **regla `protected`**: acceder (`getfield`/`putfield`/`invokevirtual`) a un
  miembro `protected` declarado en una **superclase de otro paquete** exige que el
  receptor sea del tipo actual o un subtipo (`resolve_member` camina la jerarquía hasta
  la clase declarante; `same_package` compara el prefijo del nombre binario). Verificado:
  demo cross-package real `Sub extends pkg.Base` lee `x` protegido sobre `this` y
  verifica; `LocalVars.g` (invokeinterface real) pasa el chequeo de `count`; tests del
  predicado (rechazo sobre un receptor `pkg.Base`, no-restricción en mismo paquete) y de
  `count`/nombres especiales. Sin falsos positivos (15/15 demos).

## GC

Estado: tiene las tres fases (mark · sweep · compact), la política de
fragmentación y los cuatro disparadores (OutOfSpace · Occupancy · AllocationRate ·
Explicit) sobre un safepoint. Lo que falta, por orden de impacto:

### Correctitud

- [x] **`reference_slots` (marcado transitivo).** ✅ Implementado en
  `src/jvm/interpreter/gc.rs`: instancia (campos no estáticos, super-primero), array
  de referencias (elementos) y mirror `Class<…>` (estáticos). Alimenta tanto el
  marcado (seguir targets) como la reescritura de la compactación (reubicar punteros).
  Verificado: `Trans` (campo), `Arr` (array), `Stat` (estático) y `CompactRef`
  (reescritura al mover). **El GC ya es correcto para grafos con referencias entre
  objetos** — se levantó el gate del disparo automático.

### Raíces (completitud)

- [ ] **Auditar las fuentes de raíces.** Hoy: pila+locales de frames ✓, mirrors ✓.
  Faltaría cubrir, si/cuando aparezcan: la tabla de **strings internados** (si se
  agrega dedup de `String`), la **excepción en vuelo** durante el unwinding, y
  referencias retenidas por **nativos** (hoy ninguna las retiene entre llamadas).

### Tipos de referencia y ciclo de vida (lo "profesional")

- [~] **Referencias débiles/blandas/fantasma** + `ReferenceQueue` (`java.lang.ref`).
  - [x] **`WeakReference` + `ReferenceQueue`**: clases en `boot/java/lang/ref/`
    (compiladas con `--patch-module java.base=bootstrap`). El **major** trata `referent`
    como débil (`strong_reference_slots` lo excluye del mark), y `process_weak_references`
    limpia el referente muerto (`get()→null`) y **encola** la Reference en su `queue`
    (resolviendo offsets de campos con `field_byte_offset`). Simplificación: el **minor**
    trata `referent` como fuerte (spec-compliant; las débiles se procesan en el major).
    De paso se agregaron `ifnull`/`ifnonnull` (faltaban en verificador e intérprete).
    Verificado: demo `Weak` (referente sin raíz fuerte → tras `System.gc()`, `get()==null`
    y `q.poll()==wr`) → `run → 11`; test de integración. Sin regresión.
  - [ ] **`SoftReference`** (limpiar solo bajo presión de memoria) y **`PhantomReference`**.
- [ ] **Finalizers / `Cleaner`** (post-mortem de objetos) — `Cleaner` sobre
  `PhantomReference` + un `Runnable` de limpieza corrido en el safepoint (siguiente fase).
- [ ] **Class unloading**: descargar clases y sus mirrors sin uso. Hoy los mirrors
  están *pinned* y nunca se liberan ni se mueven (la memoria de clase no se recupera).

### Ingeniería / performance

- [ ] **Generacional** (en curso, diseño de regiones físicas + copia). Layout:
  `[ null | Eden | S0 | S1 | Old (crece) ]`. Por fases:
  - [x] **Fase 1 — Regiones**: `Gen{Young,Old}`/`Region{Eden,S0,S1,Old}` + `age` por
    `Allocation`; tamaños de Eden/survivor configurables (`JVM_GC_EDEN_SIZE`,
    `JVM_GC_SURVIVOR_SIZE`); `old_start`/`gen_of`/`region_of` clasifican por dirección.
    No-destructivo (el allocator y el GC actual siguen igual; los objetos llevan ya su
    generación). Test de clasificación; sin regresión.
  - [x] **Fase 2 — Minor GC (copia)**: allocator con cursores por región (Eden bombea y
    se **resetea** por colección; survivors con to/from + swap; Old crece con su free
    list). `minor()` (gc.rs) evacúa los vivos de Eden+from-survivor → to-survivor (o
    **promueve** a Old por edad ≥ `tenure`, configurable `JVM_GC_TENURE`), vía forwarding
    Cheney: raíces de frames, raíces **old→young** (escaneo de todo Old por ahora) y
    scan transitivo, reescribiendo todas las refs. Mirrors se alojan en Old
    (`malloc_old`, pinned). El major (`compact`/`sweep`) quedó **scoped a Old** (young es
    del minor). Disparo automático al llenarse Eden en el safepoint (el copying es
    correcto sobre cualquier estado). Verificado: demo `Genny` (200 objetos efímeros que
    desbordan Eden ~13 veces + un `keep` long-lived con puntero old→young) corre a
    **20106** preservando sobrevivientes y referencias; test de integración + tests del
    heap reescritos para el modelo generacional.
  - [x] **Fase 3 — Write barrier + remembered set**: `putfield`/`aastore` que crean un
    puntero **old→young** llaman `Heap::record_reference_store`, que registra el holder
    en el `remembered: HashSet` (las raíces que el barrier ve). El minor ya **no escanea
    todo Old**: sus raíces old→young son los mirrors (siempre, sus estáticos) + el
    remembered set; y lo reconstruye al final (un holder queda solo si sigue apuntando a
    un survivor). Tras un **major** (que mueve/libera Old) se recomputa con
    `rebuild_remembered` (O(Old), fuera del hot path). Verificado: demo `Barrier`
    (un objeto tenured apunta a un young alcanzable **solo** por ese puntero; sobrevive
    los minors → `run → 99`) + test de integración; `Genny` sin regresión.
  - [x] **Fase 4 — Major scoped a Old + triggers + demo**: `compact`/`sweep` reubican/
    liberan **solo Old** (young es del minor); el major reescribe refs a Old movido desde
    *cualquier* objeto (young u old). `collect()` (disparo explícito / auto) es una
    colección **completa**: minor (young) + major (Old). El minor se dispara solo al
    llenarse Eden en el safepoint. Test de scoping (`major_sweep_reclaims_old_garbage_but_leaves_young`).
    Demos: `Genny` (minor + promoción) y `Barrier` (old→young). *(Pendiente opcional: pintar
    las regiones en el visualizador.)*
- [ ] **Concurrente / incremental / paralelo**: hoy es stop-the-world single-thread.
- [ ] **Free list mejor que first-fit O(n)**: segregada por tamaño / buddy allocator.
- [ ] **`mark`: el `HashSet seen` es redundante** con el bit de marca
  (`src/jvm/interpreter/gc.rs`). Usar solo el bit (`if is_marked { continue }`) y
  ahorrar la asignación del set por colección.
- [ ] **Política de expansión de heap / OOM real**: cuando el *live set* supera
  `capacity`, hoy no se agranda formalmente ni se lanza `OutOfMemoryError`.
- [ ] **Modo "verify heap"** post-GC (aserciones de consistencia para debug).

### Disparadores

- [ ] **Mejorar el predictivo (`AllocationRate`).** Extrapolación lineal naíf en
  `GcPolicy::auto_cause`: `rate = Δused / Δsteps` desde el último GC, proyectada
  `rate_horizon` opcodes. Dispara **en frío** (step 1, `last_gc_used = 0` → tasa
  enorme; se vio `[gc] AllocationRate` en `used 8B → 8B` con 0 vivos). Falta:
  - **warmup**: no predecir hasta tener una ventana mínima de muestras;
  - **suavizado**: media móvil / decaimiento en vez de la pendiente cruda;
  - **baseline** desde el piso del heap (`Heap::floor()`), no desde 0.

### Observabilidad

- [ ] **Estadísticas de GC**: nº de colecciones, bytes recuperados acumulados,
  "pausa" en opcodes — hoy solo está el log `[gc] …` ad-hoc en la consola.

## Sistema de tipos completos (verificador + intérprete) — ✅ COMPLETO

Los 4 tipos numéricos modelados y **ejecutados** (no solo `int`/referencia), con su
verificación de tipos. Registro de lo hecho, por orden cronológico:

- [x] **`long` — cómputo local.** `Value::Long`, opcodes `lconst`/`ldc2_w`/`lload`/
  `lstore`/`ladd`/`lsub`/`lmul`/`lreturn`, y el verificador con `VType::Long` +
  marcado de categoría-2 en locales. Corre y verifica (`Lng.run` → `12L`).
- [x] **`long`/`double` — params categoría-2**: `Frame::for_call` ubica los args con
  huecos (`Metaspace::param_slot_widths`), aplicado a los 4 invokes; el verificador
  arma los locales iniciales con la mitad alta. Verificado: `LAdd.add(long,long)`→`30L`
  (estático) y `Adder.plus` vía `invokevirtual`→`7L` (instancia).
- [x] **`long`/`double` en StackMapTable frames**: expansión categoría-2 en `decode`
  (`push_local`/`chop_one_local` agregan/quitan la mitad alta `Top` en Append/Full/
  Chop). Desbloqueó long/double con branches/loops. Verificado con `Cmp.sumWhile`
  (loop con `long` y back-edge) y `Cmp.longLess`/`dmax`.
- [x] **`long` — campos de instancia**: `Heap::read_u64`/`write_u64` + layout
  width-aware (`field_slots`, `instance_field_slots`, `field_offset`, y el caminado
  de campos del GC `reference_slots`). `getfield`/`putfield` manejan `long`.
  Verificado con `LongField.run` (`b.val=42L` + `b.tag` después → `42L`, sin pisarse).
- [x] **Estáticos y arrays de `long`/`double`/`float`**: estáticos width-aware en el
  mirror (`static_slot`, conteo en `load_class`, `getstatic`/`putstatic` tipados, y el
  GC `static_reference_slots`); arrays con `newarray` atypes 6/7/11 (elem 4/8/8) y los
  opcodes `laload`/`lastore`/`daload`/`dastore`/`faload`/`fastore`. Verificado
  (`Statics.run`→`5000000011.625d` con un int *después* de los cat-2;
  `Arrays2`: longArr→`10000000007L`, dblArr→`3.75d`, fltArr→`2f`).
- [x] **Conversiones numéricas** (`i2l`/`i2f`/`i2d`/`l2i`/`l2f`/`l2d`/`f2i`/`f2l`/
  `f2d`/`d2i`/`d2l`/`d2f`/`i2b`/`i2c`/`i2s`): módulo `conversion_operations` +
  transiciones en el verificador. El `as` de Rust satura en float→int igual que la
  JVM. Verificado (`Conv`: widen→`12L`, narrow→`44`, roundtrip→`42`, chars→`8928`).
- [x] **Comparaciones** `lcmp`/`fcmpl`/`fcmpg`/`dcmpl`/`dcmpg` (módulo
  `comparison_operations`) + los branches `iflt`/`ifge`/`ifgt`/`ifle` que faltaban en
  el intérprete. NaN: `fcmpl`/`dcmpl`→`-1`, `fcmpg`/`dcmpg`→`1`. Verificado (`Cmp`:
  `longLess`→`1`, `dmax`→`7.25d`, `nanLess`→`0`, `sumWhile`→`10L`).
- [x] **Resto de aritmética**: división/resto (`idiv`/`ldiv`/`fdiv`/`ddiv`, `irem`…),
  negación (`ineg`…`dneg`), shifts (`ishl`/`ishr`/`iushr` + variantes long), bitwise
  (`iand`/`ior`/`ixor` + long), `iinc`, y los `if_icmp*` que faltaban en el intérprete
  (solo tenía `if_icmpgt`). División entera por cero lanza `ArithmeticException`
  (clase nueva en `boot/`). Verificado (`Arith`: intMix→`8`, longMix→`3149L`,
  loop→`10`, divZero capturada→`-1`).
- [x] **`double`** (categoría-2, `f64`): `Value::Double` (se bajó `Eq` del `derive`
  de `Value` — nada lo usaba como clave), opcodes `dconst`/`ldc2_w`/`dload`/`dstore`/
  `dadd`/`dsub`/`dmul`/`dreturn`, campos `double` (8 bytes vía bits f64), y el
  verificador con `VType::Double`. Corre y verifica (`Doub`→`4d`, `DoubField`→`3.5d`).
- [x] **`float`** (categoría-1, `f32`): `Value::Float`, opcodes `fconst`/`ldc` (float)/
  `fload`/`fstore`/`fadd`/`fsub`/`fmul`/`freturn`, campos `float` (4 bytes vía bits
  f32, 1 slot → sin cambios de layout), params (categoría-1, sin hueco), y el
  verificador con `VType::Float`. Corre y verifica (`Flt`→`3.75f`, `FloatField`→`3.5f`,
  `FAdd`→`4f`).
- [x] **Lattice de referencias**: covarianza de arrays en `is_subtype` (`[X ⊑ [Y` ⟺
  `X ⊑ Y`, y `[… ⊑ Object`/`Cloneable`/`Serializable`); interfaces ya se caminaban; y
  `VType::join` (LUB) como operación de lattice para completitud (no se invoca con
  StackMapTable, pero la cierra). Verificado (`ArrCov`: `Dog[]`→`Animal[]` → `2`; tests
  unitarios de covarianza y join).

## Intérprete

- [x] **Manipulación de pila completa** (`pop`/`pop2`/`dup`/`dup_x1`/`dup_x2`/`dup2`/
  `dup2_x1`/`dup2_x2`/`swap`, 0x57–0x5f): módulo `stack_operations`, **category-aware**
  (toda la familia `dup` parametrizada por `(dup_slots, skip_slots)`; un `long`/`double`
  cuenta como 2 slots aunque sea 1 entrada). Espejado en el verificador. Verificado
  (`Stk`: withPop→`42`, chainAssign→`14`, compound→`15`, longCompound `dup2`+long→`101L`)
  + tests unitarios de las formas categoría-2. Cierra los `todo!` de expression
  statements (`foo();`, `new X();` descartados).
- [x] **`bipush`/`sipush`** (0x10/0x11): agregados (empujan un byte/short con signo
  como int). El verificador ya los manejaba; faltaban en el intérprete.
- [x] **`tableswitch`/`lookupswitch`** (0xaa/0xab): saca la clave int y salta al caso
  (`tableswitch` indexa el rango contiguo `[low, high]`; `lookupswitch` escanea los
  pares `match → offset` ordenados) o al `default` — con el padding de alineación a 4
  bytes. Modelado también en el verificador (`transfer`: saca un int, ramifica a
  `default` + casos, sin fall-through). Verificado: demo `Switch` (denso → `tableswitch`,
  disperso → `lookupswitch`) verifica por ambos drivers **y** corre (`run → 205`).
