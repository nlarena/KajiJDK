# Pasada 2 — Attribute (atribución) — documentación de referencia

Documento de diseño de la **segunda pasada** del análisis semántico: la que entra a los
**cuerpos** de los métodos, resuelve nombres y **tipa** cada expresión, **decorando** el AST.
En javac es la fase `Attr`. Cada regla cita su sección normativa.

- **Fuente normativa:** JLS Java SE 25 (`docs/semantica-jdk25.md` mapea el lenguaje completo).
  De acá, **JLS §N**; para el bytecode, **JVMS §N**.
- **Entrada:** el AST + la **tabla de símbolos con el grafo tipado** que produjo la pasada 1
  (ver `src/javac/enter.rs`, `src/javac/symbol.rs`: `Resolved`, `super_class`, `interfaces`).
- **Salida:** el mismo AST **decorado** (tipo por nodo, binding por nombre, conversiones
  insertadas) — entrada de Flow (B4) y Codegen (B3).
- **Objetivo:** cubrir la semántica **completa** de JDK 25; el subconjunto inicial se marca en
  §12.

---

## 1. Posición en el pipeline y qué cambia respecto de la pasada 1

```
.java → lexer → parser → AST → [ Enter/MemberEnter = pasada 1 ] → tabla+grafo
                                → [ ATTRIBUTE = pasada 2 ] → AST decorado → Flow/Codegen
```

| | Pasada 1 (Enter) | Pasada 2 (Attribute) |
|---|---|---|
| Pregunta | *¿qué existe?* | *¿qué significa cada uso?* |
| Alcance | encabezados/firmas; **no** entra a cuerpos | **entra a los cuerpos** |
| Recorrido | a lo ancho (declaraciones) | a lo hondo (sentencias/expresiones) |
| Produce | la tabla + el grafo tipado | el AST **decorado** |

La 2 **consume** lo que la 1 escribió — no reconstruye la tabla.

---

## 2. Los dos trabajos, ahora entrelazados

- **Resolución de nombres en cuerpos** — cada identificador → su binding (local, param,
  campo, método, tipo). Orden JLS [§6.5.6](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.5.6)
  (nombres de expresión), [§6.5.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.5.7)
  (nombres de método).
- **Type checking / síntesis** — el tipo de cada expresión ([Cap. 15](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html)).

Van **pegados**: para resolver `a.b` hace falta el **tipo de `a`**; para resolver `f(x)` hacen
falta los **tipos de los argumentos** (overload). En la 1 se podían separar (firmas); en la 2 no.

---

## 3. El entorno (Env) y el manejo de scopes

La 2 arrastra un **Env transitorio** hacia abajo en el recorrido:

- el **scope actual** (se apoya en la tabla de la 1 y le agrega los efímeros: params, locales),
- la **clase actual** (para `this`/miembros sin cualificar),
- el **método actual** (tipo de retorno, si es `static`),
- el **tipo esperado** (*target type*, ver §4).

Reglas de scope: entrar a un método empuja un scope con sus params; entrar a un bloque empuja
un scope; cada **local** se agrega **en orden** y está en alcance **desde su declaración hacia
abajo** ([§6.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-6.html#jls-6.3)) — a
diferencia de un campo, visible en toda la clase.

---

## 4. El algoritmo: type checking **bidireccional**

Dos modos, y la razón de que la atribución no sea solo bottom-up:

- **Síntesis** (`Γ ⊢ e ⇒ τ`): el tipo de `e` sale de sus partes. `a + b` → tipos de `a`/`b` →
  regla del `+` → resultado.
- **Checking** (`Γ ⊢ e ⇐ τ`): se verifica `e` **contra** un tipo esperado que baja del contexto.

En javac: `attribExpr(árbol, env, pt)`, con `pt` = prototipo/tipo esperado.

### Poly expressions (JLS [§15.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.2))
Las expresiones cuyo tipo **depende del contexto** (target type), y que obligan al modo checking:
**lambdas** ([§15.27](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.27)),
**method references** ([§15.13](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13)),
el **ternario** con ramas poly ([§15.25](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.25)),
las **llamadas genéricas** que infieren, y la **switch expression**. Contra las *standalone*
(literal, `a+b`), cuyo tipo no depende del contexto.

> Es la diferencia con un compilador clásico: con lambdas/inferencia el **contexto empuja el
> tipo hacia abajo**. Por eso Attr es bidireccional.

---

## 5. Tipado de expresiones — nodo por nodo

| Nodo del AST | Regla | JLS |
|---|---|---|
| Literal | tipo directo | §3.10 |
| `Name` | resolver a símbolo → su tipo | §6.5.6 |
| `Binary` aritmético | promoción numérica → resultado; conversiones insertadas | §15.17–15.20, §5.6 |
| `Binary` con `+` y `String` | **concatenación**, no suma | §15.18.1 |
| `Binary` lógico/relacional | `&&`/`\|\|` exigen boolean; relacionales → boolean | §15.22–15.24 |
| `Unary` | `!`→boolean; `~ - +`→numérico; `++/--`→lvalue | §15.14–15.15 |
| `Assign` | RHS **asignable** al LHS; compuesto lleva cast implícito | §15.26, §5.2 |
| `Ternary` | LUB de ramas / target type (poly) | §15.25 |
| `Call` | **overload resolution** (§7) + tipo de retorno; static vs. instancia | §15.12 |
| `Field` | resolver el campo en el **tipo del receptor**, subiendo por la jerarquía | §15.11 |
| `Index` | array indexado; índice `int`; resultado = elemento | §15.10.3 |
| `Cast` | legalidad del cast | §15.16, §5.5 |
| `InstanceOf` (+ patrón) | operando/tipo referencia → boolean; bind del patrón | §15.20.2 |
| `NewObject` | resolución de **constructor** | §15.9 |
| `NewArray` | dimensiones `int` → tipo array | §15.10 |
| Lambda / method ref | compatibilidad con la interfaz funcional **target** | §15.27 / §15.13 |
| Expresión constante | *constant folding* | §15.29 |

---

## 6. Chequeo de sentencias

| Sentencia | Regla | JLS |
|---|---|---|
| `LocalVar` | resolver tipo (o **inferir `var`**), init asignable, agregar al scope con su slot | §14.4 |
| `Return` | void ↔ sin valor; no-void ↔ valor asignable al retorno del método | §14.17 |
| `If`/`While`/`For`/`do` | condición **boolean** | §14.9–14.14 |
| `ForEach` | iterable = array o `Iterable`; elemento asignable | §14.14.2 |
| `Switch` | selector válido; etiquetas del tipo del selector; patrones; exhaustividad | §14.11 |
| `Throw` | expresión = subtipo de `Throwable` | §14.18 |
| `Try`/`catch` | tipos de `catch` = subtipos de `Throwable`; recursos `AutoCloseable` | §14.20 |
| `Synchronized` | el lock = tipo referencia | §14.19 |

---

## 7. La joya: overload resolution — JLS [§15.12.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.12.2)

`f(args)` con varios candidatos. Java elige en **3 fases** y para en la primera con aplicables:

1. **Invocación estricta** — sin boxing/unboxing, sin varargs.
2. **Invocación laxa** — con boxing/unboxing, sin varargs.
3. **Aridad variable** — con varargs.

Dentro de una fase, si hay varias aplicables → la **más específica** (§15.12.2.5). Si el método
es **genérico** → **inferencia de tipos** ([Cap. 18](https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html))
para deducir los argumentos de tipo. Es el algoritmo más complejo del compilador.

---

## 8. Qué **consume** de la pasada 1 (el grafo)

La 2 no reconstruye nada — navega lo que la 1 persistió (`Resolved` en `symbol.rs`):

- **Lookup de miembros** (`a.b`, un método heredado): sube por `super_class`/`interfaces`.
- **Subtipado** (`¿A es-un B?`, para asignabilidad y args): recorre esa jerarquía.
- **Overload resolution**: usa los **tipos de parámetro resueltos** (`Resolved::Method`).

Por eso la 1 **persistió** los tipos resueltos: eran la **entrada** de esta pasada.

---

## 9. Qué **produce** (decoración del AST)

- **tipo** de cada expresión,
- **símbolo/binding** de cada nombre y llamada (qué campo/método exacto),
- **conversiones implícitas** insertadas (boxing, widening) — marcadas para el codegen,
- info de **slots** de locales (con categoría-2: `long`/`double` = 2 slots).

Ese árbol decorado alimenta **Flow** (B4) y **Codegen** (B3).

---

## 10. El ángulo **JVM**: de la decoración al bytecode

Lo que distingue a un compilador que apunta a bytecode: la decoración de la pasada 2 **decide
los opcodes**.

- Resolver una llamada = elegir el **método exacto** (clase dueña + descriptor) **y cuál
  `invoke`**: `invokevirtual` (instancia, dispatch dinámico), `invokestatic`, `invokespecial`
  (`private`/`super`/`<init>`), `invokeinterface`. Se decide **acá** (JVMS §6.5).
- Acceso a campo → `getfield`/`getstatic` con el `Fieldref` resuelto.
- Los **descriptores** (`Ljava/lang/String;`, `(II)I`) salen de los tipos resueltos (JVMS §4.3).
- **Categoría-2** — la contabilidad de slots depende de los tipos que fija la 2 (JVMS §2.6.1).

> La pasada 2 traduce *significado* → *referencias de constant pool + selección de opcode*. Sin
> ella, el codegen no sabría qué `invoke` emitir ni contra qué símbolo.

---

## 11. El borde — qué **NO** es la pasada 2

- **Asignación definitiva** y **alcanzabilidad** → **Flow (B4)** ([Cap. 16](https://docs.oracle.com/javase/specs/jls/se25/html/jls-16.html), §14.22).
- **Bajar azúcar** (concat→StringBuilder, for-each→loop, boxing→llamadas) → **Desugar**.
- **Emitir bytecode** → **Codegen (B3)**.

La 2 **tipa y resuelve**; no baja azúcar ni emite.

---

## 12. Alcance para el compilador propio

### Núcleo inicial
Resolución de nombres en cuerpos (locales/params/campos/`this`); tipado de literales, binarios
con promoción, asignación, llamadas **sin** las fases complejas de overload, acceso a campo/array,
`new`; sentencias (locals con slots categoría-2, `return`, `if`/`while`/`for`); decorar cada nodo
con su tipo y binding. Suficiente para que el codegen (B3) elija `invoke*`/`get*` y emita.

### Cola larga (semántica completa de JDK 25)
Overload en 3 fases (§15.12.2), **inferencia** (Cap. 18), **poly expressions/target typing**
(lambdas, method refs, ternario, switch expr), boxing/unboxing, subtipado con genéricos/wildcards,
pattern matching completo. Se documenta al toparse.

---

## 13. Referencias

- **JLS SE 25** — Cap. 15 (expresiones), 5 (conversiones), 6 (nombres), 14 (sentencias),
  18 (inferencia): `https://docs.oracle.com/javase/specs/jls/se25/html/`.
- **JVMS SE 25** — §4.3 (descriptores), §6.5 (`invoke*`/`get*`), §2.6.1 (slots): `https://docs.oracle.com/javase/specs/jvms/se25/`.
- Referencias del proyecto: [`semantica-jdk25.md`](semantica-jdk25.md) (semántica del lenguaje),
  y la pasada 1 en `src/javac/enter.rs` + `src/javac/symbol.rs` (el grafo que esta pasada consume).
