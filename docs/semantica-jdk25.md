# Semántica de Java SE 25 — referencia completa

Mapa de la **semántica completa del lenguaje Java SE 25**: qué significa cada construcción,
qué reglas debe hacer cumplir un compilador conforme, y dónde lo define la norma. **No** es
un subconjunto — cubre todo el lenguaje. (El alcance que tome nuestro `javac` propio se
documenta aparte; esto es el **target completo**.)

- **Fuente normativa:** *The Java® Language Specification, Java SE 25 Edition* (2025-07-29) —
  `https://docs.oracle.com/javase/specs/jls/se25/html/`. De acá en más, **JLS §N**. Los
  números de sección son de esa edición; los links apuntan al capítulo (`jls-N.html`).
- **Complemento:** *The Java Virtual Machine Specification, Java SE 25* — JVMS — para
  descriptores, formato `.class`, carga/enlace y el bytecode (`https://docs.oracle.com/javase/specs/jvms/se25/`).
- Java **no usa RFCs**; JLS + JVMS son los documentos normativos, y los cambios se procesan
  por **JEP/JSR**.

Índice por capítulo de la JLS:

| Cap. | Tema | Peso semántico |
|---|---|---|
| 3 | Estructura léxica | literales, identificadores, keywords |
| 4 | Tipos, valores y variables | **tipos + subtipado + genéricos** |
| 5 | Conversiones y contextos | **conversiones implícitas** |
| 6 | Nombres | **resolución de nombres** |
| 7 | Paquetes y módulos | imports, módulos |
| 8 | Clases | **miembros, herencia, init, records/enums/sealed** |
| 9 | Interfaces | métodos default/static, funcionales |
| 10 | Arrays | tipos array, covarianza |
| 11 | Excepciones | checked/unchecked, análisis |
| 12 | Ejecución | carga, enlace, **inicialización** |
| 14 | Bloques, sentencias y patrones | **control de flujo + pattern matching** |
| 15 | Expresiones | **tipado, operadores, overload, lambdas** |
| 16 | Asignación definitiva | flujo de datos |
| 17 | Hilos y bloqueos | **modelo de memoria (JMM)** |
| 18 | Inferencia de tipos | genéricos, `var`, lambdas |

---

## 3. Estructura léxica — JLS Cap. 3

Lo semántico del léxico (el resto es B0):

- **Literales** ([§3.10](https://docs.oracle.com/javase/specs/jls/se25/html/jls-3.html#jls-3.10)):
  enteros (decimal/hex/octal/binario, sufijo `L`), de punto flotante (`f`/`d`, hex float,
  exponente), `boolean`, `char` (con escapes y `\uXXXX`), `String` (incl. **text blocks**),
  y `null`. Cada literal tiene un **tipo** y un **valor** determinados acá.
- **Keywords** ([§3.9](https://docs.oracle.com/javase/specs/jls/se25/html/jls-3.html#jls-3.9)):
  50 reservadas + literales-palabra. Las **contextuales** (`var`, `yield`, `record`,
  `sealed`, `permits`, `module`…) son identificadores que el parser/sema desambigua por
  contexto. Detalle: [`tokens-jdk25.md`](tokens-jdk25.md).
- **Identificadores** ([§3.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-3.html#jls-3.8)):
  reglas Unicode; `_` es keyword (variable/patrón sin nombre).

---

## 4. Tipos, valores y variables — JLS Cap. 4

El corazón del sistema de tipos.

- **Tipos primitivos** ([§4.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2)):
  `byte`/`short`/`int`/`long` (enteros con complemento a dos), `char` (16-bit sin signo),
  `float`/`double` (IEEE-754), `boolean`. Sus rangos y operaciones se definen acá.
- **Tipos referencia** ([§4.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.3)):
  clases, interfaces, arrays, variables de tipo. El valor especial `null`.
- **Genéricos**: tipos parametrizados, **variables de tipo** ([§4.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.4)),
  **wildcards** (`? extends`/`? super`, [§4.5.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.5)),
  tipos raw, tipos intersección (`A & B`). **Type erasure** ([§4.6](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.6)):
  los genéricos se borran en runtime — el compilador chequea, la VM no ve parámetros de tipo.
- **Subtipado** ([§4.10](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10)):
  la relación `<:` (directo y transitivo), la **covarianza de arrays** (§4.10.3, `S[] <: T[]`
  si `S <: T` — de ahí el `ArrayStoreException` en runtime), y las reglas de genéricos
  (§4.10.2, con wildcards).
- **Kinds de variables** ([§4.12](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.12)):
  campos, params, locales, componentes de array; valores por defecto; `final`.

---

## 5. Conversiones y contextos — JLS Cap. 5

**Qué conversión implícita** se aplica **según dónde** aparece la expresión.

Conversiones ([§5.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.1)):
identidad; **widening/narrowing primitivo**; **widening/narrowing de referencia**;
**boxing/unboxing**; unchecked (genéricos); captura; a `String`; a valor.

Contextos (dónde y cuáles se permiten):

| Contexto | JLS | Qué conversiones |
|---|---|---|
| **Asignación** | [§5.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.2) | widening + boxing + **narrowing de constante** (`byte b = 10`) |
| **Invocación** (args) | [§5.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.3) | loose vs. strict (las fases del overload) |
| **Cast** | [§5.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.5) | qué casts son legales |
| **String** (`+`) | [§5.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.4) | todo a `String` |
| **Numérico** (promoción) | [§5.6](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.6) | promoción unaria/binaria (dónde va un `i2l`/`i2d`) |

---

## 6. Nombres — JLS Cap. 6

Resolver **a qué se refiere** cada nombre.

- **Declaraciones y scope** ([§6.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.3)):
  unidad ⊃ clase ⊃ método ⊃ bloque; alcance de variables de patrón.
- **Shadowing / obscuring** ([§6.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.4)):
  un local tapa un campo homónimo; un bloque interno tapa el externo.
- **Determinar el significado de un nombre** ([§6.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.5)):
  la parte peluda — reclasificar `a.b.c` entre expression name / type name / package name
  (§6.5.2), y el significado de expression names (§6.5.6) y method names (§6.5.7).
- **Control de acceso** ([§6.6](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.6)):
  `public`/`protected`/package/`private`, incluida la regla `protected` (acceso desde
  subclase en otro paquete).
- **Nombres cualificados y canónicos** ([§6.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.7)).

---

## 7. Paquetes y módulos — JLS Cap. 7

- **Paquetes** ([§7.1–7.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html)): la
  unidad de nombres; `package` declaration.
- **Imports** ([§7.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html#jls-7.5)):
  single-type, on-demand (`.*`), static single, static on-demand. Afectan la resolución de
  nombres (Cap. 6). JDK 25 agrega **module import declarations** (`import module M;`).
- **Módulos** ([§7.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html#jls-7.7)):
  `requires`/`exports`/`opens`/`uses`/`provides`; readability y accesibilidad entre módulos.

---

## 8. Clases — JLS Cap. 8

- **Declaración de clase** ([§8.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.1)):
  modificadores, parámetros de tipo, `extends`, `implements`, `permits`.
- **Miembros y herencia** ([§8.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.2)):
  qué se hereda; hiding de campos y métodos estáticos.
- **Campos** ([§8.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.3)):
  `static`/instancia, `final`, `volatile`, `transient`; inicializadores.
- **Métodos** ([§8.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.4)):
  firma, `throws`, **overloading** (§8.4.9), **overriding** y su chequeo de compatibilidad
  (§8.4.8, covarianza de retorno, no debilitar acceso, `@Override`).
- **Clases anidadas**: estáticas, **internas** (con captura de `this` externo), **locales**
  y **anónimas** ([§8.1.3, §15.9.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html)).
- **Constructores** ([§8.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.8)):
  `this(...)`/`super(...)`, constructor por defecto, orden de invocación.
- **Inicializadores** ([§8.6–8.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.6)):
  bloques de instancia y estáticos.
- **`enum`** ([§8.9](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.9)):
  constantes, cuerpos, métodos implícitos (`values`/`valueOf`).
- **`record`** ([§8.10](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.10)):
  componentes, constructor canónico/compacto, accessors, `equals`/`hashCode`/`toString`
  derivados; deconstrucción para record patterns (Cap. 14).
- **`sealed`** ([§8.1.1.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.1.1)):
  `permits` + subclases `final`/`sealed`/`non-sealed`; habilita el chequeo de exhaustividad
  en `switch`.

---

## 9. Interfaces — JLS Cap. 9

- Miembros: métodos **abstractos**, **`default`**, **`static`**, **`private`**; campos
  `public static final` implícitos ([§9.3–9.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.4)).
- **Interfaces funcionales** ([§9.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.8)):
  un solo método abstracto → target de lambdas y method references.
- **Tipos anotación** ([§9.6](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6)):
  elementos, valores por defecto, meta-anotaciones.
- Herencia múltiple de tipo; resolución de conflictos de `default` methods.

---

## 10. Arrays — JLS Cap. 10

Tipos array ([§10.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-10.html)), creación
e inicializadores (`new T[n]`, `{...}`), acceso, `length`, y la **covarianza** con su
consecuencia en runtime: `ArrayStoreException` cuando el tipo dinámico no encaja ([§10.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-10.html#jls-10.5)).

---

## 11. Excepciones — JLS Cap. 11

- **Jerarquía** ([§11.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html#jls-11.1)):
  `Throwable` → `Error`/`Exception` → `RuntimeException`; **checked vs. unchecked**.
- **Análisis de excepciones** ([§11.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html#jls-11.2)):
  qué checked exceptions puede lanzar una sentencia; regla "catch or declare".
- Semántica de `try`/`catch`/`finally` y multi-catch (la sintaxis en Cap. 14).

---

## 12. Ejecución — JLS Cap. 12

- **Carga y enlace** ([§12.2–12.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-12.html#jls-12.2)):
  loading, verification, preparation, resolution.
- **Inicialización de clases** ([§12.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-12.html#jls-12.4)):
  cuándo corre `<clinit>` (perezosa, super-primero, thread-safe), qué la dispara. Es la regla
  que tu JVM ya implementa en `ensure_initialized`.
- Orden de inicialización de instancia ([§12.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-12.html#jls-12.5)):
  `super()` → campos/inicializadores → cuerpo del constructor.

---

## 14. Bloques, sentencias y patrones — JLS Cap. 14

Control de flujo + el gran agregado moderno: **pattern matching**.

- Sentencias: bloque, `if`/`while`/`do`/`for`/`for-each` ([§14.9–14.14](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html)),
  `return`/`break`/`continue`/`yield`, `throw`, `synchronized`, `try`
  (con **try-with-resources**, [§14.20.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.20)),
  `assert`, etiquetadas.
- **Declaración de variable local** ([§14.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.4)):
  con `var` (inferencia, Cap. 18).
- **`switch`** ([§14.11](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.11)):
  statement y arrow labels; **exhaustividad** con `sealed`/`enum`.
- **Patrones** ([§14.30](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.30)):
  type patterns (`x instanceof Foo f`), **record patterns** (deconstrucción), patrones sin
  nombre (`_`); su **scope de variable de patrón** (§6.3.1) y las reglas de aplicabilidad.
- **Alcanzabilidad** ([§14.22](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.22)):
  código muerto tras `return`/`throw` es error de compilación; `while(true)`.

---

## 15. Expresiones — JLS Cap. 15

El capítulo más grande: **tipado y evaluación** de cada expresión.

- **Orden de evaluación** ([§15.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.7)):
  izquierda-a-derecha, operandos antes que la operación.
- **Operadores** — cada uno con sus tipos de operando, promoción y tipo de resultado:
  multiplicativos §15.17, aditivos §15.18 (**`+` = concatenación si hay `String`**, §15.18.1),
  shift §15.19, relacionales §15.20, `instanceof` **con patrón** §15.20.2, igualdad §15.21,
  bit/lógicos §15.22, `&&`/`||` con **short-circuit** §15.23–15.24, ternario §15.25,
  asignación §15.26 (el compuesto lleva **cast implícito**, §15.26.2).
- **Invocación de método** ([§15.12](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.12)):
  la joya — **overload resolution en 3 fases** (§15.12.2: sin boxing/varargs → con boxing →
  con varargs; "más específico aplicable").
- **Creación**: `new` objeto (§15.9, incl. clases anónimas), `new` array (§15.10).
- **Lambdas** ([§15.27](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.27))
  y **method references** ([§15.13](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13)):
  compatibilidad con la interfaz funcional target, captura de variables *effectively final*.
- **`switch` expression** ([§15.28](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.28)):
  produce valor, `yield`, exhaustividad.
- **Expresiones constantes** ([§15.29](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.29)):
  las que habilitan narrowing de constante y `case` labels.

---

## 16. Asignación definitiva — JLS Cap. 16

Análisis de flujo de datos: garantizar que **toda variable se asigna antes de leerse**
([Cap. 16](https://docs.oracle.com/javase/specs/jls/se25/html/jls-16.html)). Se formaliza con
los predicados `V is definitely assigned / definitely unassigned` antes/después de cada
construcción. También sostiene la regla de que un `final` se asigna **exactamente una vez**.

---

## 17. Hilos y bloqueos (JMM) — JLS Cap. 17

El **Modelo de Memoria de Java** — la semántica de la concurrencia (conecta con el trabajo de
hilos/monitores de tu JVM):

- **Acciones y orden** ([§17.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.4)):
  la relación **happens-before**, que define qué escrituras ve una lectura.
- **`synchronized`** ([§17.1](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.1)):
  monitores; unlock happens-before el siguiente lock del mismo monitor.
- **`volatile`** ([§17.4](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.4)):
  visibilidad y no-reordenamiento; una escritura volatile happens-before su lectura.
- **Semántica de `final`** ([§17.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.5)):
  garantías de visibilidad de campos `final` tras la construcción.
- `wait`/`notify` ([§17.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.2)).

> Es la "Planta 4" del rascacielos de concurrencia: invisible con green threads, obligatoria
> con hilos de SO.

---

## 18. Inferencia de tipos — JLS Cap. 18

- Inferencia de argumentos de tipo en métodos genéricos, `diamond` (`new ArrayList<>()`),
  y en **lambdas** contra su interfaz funcional ([Cap. 18](https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html)).
- Inferencia de `var` (local variable type inference): del tipo del inicializador; **no** hay
  `var` en campos ni params (excepto lambda).

---

## Referencias

- **JLS SE 25** — `https://docs.oracle.com/javase/specs/jls/se25/html/` (índice) — un archivo
  por capítulo (`jls-4.html`, `jls-5.html`, …).
- **JVMS SE 25** — `https://docs.oracle.com/javase/specs/jvms/se25/` — la otra mitad
  (descriptores §4.3, formato `.class`, opcodes, JMM a nivel bytecode).
- Léxico del proyecto: [`tokens-jdk25.md`](tokens-jdk25.md).
