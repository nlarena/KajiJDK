# H4 — El modelo de memoria de Java (JMM), relajado y fiel

> Estado: **en curso**. La base (substrato atómico) está hecha y Miri-verificada;
> la integración al heap va por fases (abajo).

## 0. El hallazgo que reencuadra H4

Después de W1/W2/W3, **todo acceso a campos pasa por el `RwLock`** del `SharedVm`:
`getfield` bajo `.read()`, `putfield`/`iastore`/statics bajo `.write()`. Un `RwLock`
da *happens-before*: una escritura ordenada antes de una lectura es visible, y las
escrituras son mutuamente exclusivas. `long`/`double` se leen/escriben con
`read_u64`/`write_u64` **bajo el lock** → atómicos, sin *tearing*.

O sea: **las tres garantías del JMM —visibilidad, ordenamiento, no-tearing— ya se
cumplen**, porque la VM **sobre-sincroniza**. No hay data races sobre campos Java.
`volatile` ya está honrado (todos los campos son, de hecho, "volatile-strength").

Esto es lo contrario de lo que uno esperaría de "implementar el JMM": no falta
*agregar* sincronización, **sobra**. La corrección ya está.

## 1. La decisión (JVM profesional): relajar, no verificar

Una JVM profesional relaja los campos no-volatile **por rendimiento** (como HotSpot):
acceso plano, sin candado, y `volatile` es lo que repone las garantías donde hacen
falta. Ese es el JMM *relajado fiel*, y es el camino de H4.

En Rust "relajar" ≠ escribir bytes planos concurrentes — eso es **UB** (data race).
La forma *sound* y fiel es con **atómicos**:

| Campo | Acceso | Semántica JMM |
|-------|--------|---------------|
| **no-volatile** `getfield`/`putfield` | atómico **`Relaxed`** | lock-free, sin ordering (racy pero *sound*); el valor propaga por coherencia de cache → un spin-on-flag lo termina viendo |
| **`volatile`** read | atómico **`Acquire`** | ve todo lo escrito antes del release correspondiente |
| **`volatile`** write | atómico **`Release`** | publica todo lo previo |
| **`long`/`double`** | `AtomicU64` alineado | no-tearing (incluso volatile) |

Esto saca los campos del `RwLock` → **acceso a campos lock-free**, y recién ahí
`volatile` deja de ser inerte: es la diferencia entre `Relaxed` y `Acquire`/`Release`.

## 2. Las sutilezas reales (por eso va con Miri)

1. **Acceso atómico a bytes del heap.** `AtomicU32::from_ptr`/`AtomicU64::from_ptr`
   sobre el buffer. Derivar un `*mut` de `as_ptr()` **no** alcanza (provenance de
   solo-lectura → UB en Stacked Borrows); hace falta `UnsafeCell`. Y para no cruzar
   fronteras de provenance con un acceso multi-byte, la celda es de **8 bytes**
   (`UnsafeCell<u64>`): un `u32` a offset 4-alineado cae *dentro* de una celda, un
   `u64` es la celda entera. → `AtomicRegion` (§4).
2. **Alineación.** Campo en slot `N` → offset `HEADER_SIZE + N*4 = 8 + N*4`. Para
   `AtomicU64` hace falta offset 8-alineado → **slot par**. Un `long`/`double` en slot
   impar queda a 4 (misaligned → UB). Layout: padear category-2 a slot par.
3. **Mezcla atómico/no-atómico.** El GC (en *safepoint*, exclusivo) copia bytes
   no-atómicamente al evacuar/compactar. Eso es sound porque el safepoint da el
   happens-before entre el acceso atómico normal y el memcpy exclusivo. Miri lo verifica.
4. **`ACC_VOLATILE`.** Leer el flag del field (`MemberInfo::is_volatile`, 0x0040) para
   elegir `Relaxed` vs `Acquire`/`Release`.

## 3. Compatibilidad: ¿`Threads.java` sigue andando?

`Threads.java` spinnea sobre flags **no-volatile** (`while (aDone==0||bDone==0){}`).
En Java estricto eso es un data race (debería ser `volatile`), pero anda en la
práctica porque el hardware propaga la escritura. Con **`Relaxed`** (atómico) pasa
lo mismo: la store es siempre *eventualmente* visible por coherencia de cache, así
que el spin termina — y es *sound* (atómico, no data race). ✔

## 4. Fases

- **H4-a — Substrato atómico.** `src/jvm/interpreter/atomic_region.rs`:
  `AtomicRegion` (celdas `UnsafeCell<u64>`, 8-alineado) con `load/store_u32/u64`
  con ordering explícito. **Hecho. Miri-verificado** (publicación Acquire/Release,
  no-tearing u64, roundtrips).
- **H4-b — Reconocer `volatile`.** `MemberInfo::is_volatile()` (ACC_VOLATILE).
  **Hecho.** Falta cablearlo a la resolución de campos.
- **H4-c — Alinear `long`/`double` a 8.** **Hecho.** Una única primitiva `place_field`
  (redondea el slot de un category-2 a par → offset 8-alineado) sobre la que **foldean
  los 7 consumidores del layout**: offset/size de instancia (write + read), el trazado
  de referencias del GC (`instance_reference_slots`), y los estáticos (`static_slot`,
  `static_reference_slots`, sizing del mirror). La regla vive en un solo lugar, así que
  no pueden divergir. Tests: alineación de category-2 + fold con mezcla intercalada;
  suite completa (132) verde (oráculo `green≡os-gil≡os` + GC como red de seguridad).
- **H4-d — Heap sobre substrato atómico (Eden).** **Hecho.** `EdenArena` ahora se
  apoya en `AtomicRegion`: **todo** acceso a Eden es atómico (uniforme mutadores + GC →
  sin mezcla atómico/no-atómico), `alloc` padea el stride a 8 (objetos 8-alineados →
  `u32` fields 4-alineados, `long`/`double` en slot par 8-alineados). Accessors:
  `Relaxed` para el camino general (headers, no-volatile, `long[]` vía dos `u32`), y
  `*_ordered` (`Acquire`/`Release`, `AtomicU64` real para `long`/`double` volatile sin
  tearing) listos para H4-e. **Old sigue byte-a-byte bajo el `RwLock`** — el routing
  `in_eden` los separa; se ensancha después. Miri-verde (`eden_arena`, `atomic_region`)
  + suite completa (132).
- **H4-e — Relajar `getfield`/`putfield`.** `Relaxed` no-volatile / `Acquire`-`Release`
  volatile, fuera del `RwLock` (lock-free). *Pendiente.*
- **H4-f — Verificar.** Miri sobre el heap real concurrente; test de publicación en
  Java (`volatile` visible), no-tearing de `long`, y `Threads.java` sigue verde.
  *Pendiente.*

## 5. Diferencial como oráculo (se mantiene)

`green ≡ os-gil ≡ os` deben coincidir en resultado funcional. El JMM relajado no
cambia resultados *bien-sincronizados* (los que usan `volatile`/`join`/monitores);
sí habilita reordenamientos observables en programas con data races — que es
justamente lo que `volatile` tapa. La prueba de soundness del núcleo `unsafe` es
**Miri**, no el diferencial.
