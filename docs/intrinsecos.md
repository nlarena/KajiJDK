# Intrínsecos — lista de referencia (A5)

> Métodos que la VM **reconoce y resuelve por sí misma**. En HotSpot son
> optimizaciones del **JIT** (ej. `Math.sqrt` → instrucción del CPU); como nosotros
> **no tenemos JIT**, para nosotros un "intrínseco" = un método **native / interno de
> la VM** que despachamos en `interpreter::natives` (el mismo puente del `println`).
> Muchos de estos son `native` también en la JDK real.
>
> El criterio del roadmap — *"lo que Java no puede hacerse a sí mismo"* — apunta a la
> categoría **1** (introspección: leer el header, la identidad, la jerarquía).

**Estado:** ✅ hecho · 🟢 factible ya (ints/referencias) · ⏳ necesita prerrequisito.

## 1. Introspección / identidad — *lo que Java no puede leer de sí mismo*
La categoría más pura: necesitan internos de la VM que el bytecode no puede expresar.

| Intrínseco | Qué hace | Estado |
|---|---|---|
| `Object.getClass()` | lee el `class_id` del header → devuelve el mirror (`Class<…>`) | ✅ |
| `Object.hashCode()` (identidad) | el offset del objeto en el heap como identidad | ✅ |
| `System.identityHashCode(Object)` | idem, como estático | ✅ |
| `Object.equals(Object)` (default) | identidad de referencia (`==`) — *no es native: el bytecode default es `this == obj`* | 🟢 |
| `Class.getName()` | nombre binario de la clase | ⏳ (necesita `String`/conversión) |
| `Class.isInstance` / `isAssignableFrom` | chequeo de subtipo (reusa `is_subtype`) | ✅ (requirió que el mirror sea instancia de `java.lang.Class` + reservar la *null page*) |
| `Class.getSuperclass()` | la superclase como `Class<…>` | 🟢 (idem) |

## 2. Memoria / arrays — *operaciones a nivel VM*
| Intrínseco | Qué hace | Estado |
|---|---|---|
| `System.arraycopy(src,sp,dst,dp,len)` | copia masiva entre arrays (memcpy) | ✅ (asume elementos de 4 bytes: int/ref) |
| `Array.newInstance` | crear arrays por reflexión | 🟢 |
| `sun.misc.Unsafe.*` | acceso directo a memoria | 🟢 (pero peligroso/poco educativo) |

## 3. Matemática — *mapean a instrucciones del CPU*
| Intrínseco | Qué hace | Estado |
|---|---|---|
| `Math.abs/min/max` (int) | aritmética entera | ✅ |
| `Math.sqrt/sin/cos/pow` | flotante → instrucción del CPU | ⏳ (necesita `Float`/`Double`) |
| `Integer.bitCount` | `popcnt` | ✅ (`u32::count_ones`) |
| `Integer.numberOfLeadingZeros` | `lzcnt` | ✅ (`u32::leading_zeros`) |
| `Integer.reverse` / `reverseBytes` | bit/byte reversal | 🟢 |

## 4. Strings / conversión — *el camino al print de texto rico*
| Intrínseco | Qué hace | Estado |
|---|---|---|
| `Integer.toString(int)` / `String.valueOf(int)` | int → `String` | ⏳ (necesita armar el `String` desde dígitos) |
| `String.length()` | largo | ✅ (lee el `length` del header del String) |
| `String.charAt(int)` | un char | ✅ (matiz: nuestro String es UTF-8; OK para ASCII) |
| `String.equals` / `hashCode` | comparación / hash de contenido | ✅ |
| `StringBuilder.append` / `toString` | concatenación (lo que compila el `+`) | ⏳ (necesita `StringBuilder`) |

## 5. Sistema / tiempo / concurrencia
| Intrínseco | Qué hace | Estado |
|---|---|---|
| `System.currentTimeMillis()` / `nanoTime()` | leer el reloj del SO (native) | ⏳ (devuelven `long`, que no modelamos) |
| `System.exit(int)` | terminar la VM | 🟢 (necesita una señal especial para frenar la VM) |
| `Thread.currentThread()` | el hilo actual | ⏳ (necesita modelar hilos → A6) |

## Lo que tenemos hoy (✅)

**Introspección/identidad:** `Object.getClass()`, `Object.hashCode()`, `System.identityHashCode()`.
**Arrays:** `System.arraycopy()` (elementos de 4 bytes).
**Matemática:** `Math.abs/max/min` (int), `Integer.bitCount`, `Integer.numberOfLeadingZeros`.
**Strings:** `String.length()`.
**I/O (native, no intrínseco estricto):** `PrintStream.println(int)` y `println(String)`.

Cableado: despacho nativo en `invokestatic` *y* `invokevirtual`; `natives::dispatch` con `&mut Heap`; clases `boot/` `Math`/`Integer`/`Class` + `Object`/`System`/`String`/`PrintStream` extendidas.

## Candidatos recomendados como próximos

1. **`Class.isInstance` / `getSuperclass`** — reusan `is_subtype`/`superclass_name`; necesitan pasar el `Metaspace` al `dispatch`.
2. **`Integer.toString` / `String.valueOf(int)`** — desbloquea imprimir **ints como texto** y abre el camino a la concatenación (`+`).
3. **`String.charAt` / `equals` / `hashCode`** — completar `String`.
4. **`System.exit`** — frenar la VM (necesita una señal especial en el loop).
5. **`long`/`double`** (otro hito) — desbloquea `currentTimeMillis`, `Math.sqrt`, etc.
