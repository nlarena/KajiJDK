# `invokedynamic`: correlativas, bloqueantes y ruta (D0–D6)

> Nota de diseño (A6). El opcode 0xba **despacha y verifica**, pero su *capacidad* cubre
> una de las seis fábricas que `javac` emite. Este documento mapea qué depende de qué,
> cuáles son los pocos bloqueantes con palanca real, y en qué orden conviene atacarlos.
> Complementa a `subrutinas-jsr-ret.md`: allá se documenta lo que **no** vamos a hacer;
> acá, lo que falta y por qué en ese orden.

## Estado actual

Lo que corre hoy: resolución del `InvokeDynamic` en el pool, lectura de
`BootstrapMethods`, resolución del `MethodHandle` del bootstrap, brazo en el verificador,
y **`StringConcatFactory.makeConcatWithConstants`** implementado como intrínseco.

Lo que `javac` de JDK 25 emite, medido sobre un archivo de prueba:

| Fábrica | La dispara | Estado |
|---|---|---|
| `StringConcatFactory.makeConcatWithConstants` | `"a" + b` | ✅ |
| `LambdaMetafactory.metafactory` | lambdas, method references | ⬜ |
| `LambdaMetafactory.altMetafactory` | lambdas `Serializable` | ⛔ fuera de alcance |
| `SwitchBootstraps.typeSwitch` | `switch` con patrones | ⬜ |
| `ObjectMethods.bootstrap` | `equals`/`hashCode`/`toString` de `record` | ⬜ |
| `ConstantBootstraps.invoke` | **condy** (`ldc` de tag 17) | ⬜ |

Nota: el `switch` sobre `String` **no** usa indy (compila a `hashCode` + `equals`), y un
`record` emite indy aunque nunca se llame a sus métodos.

## El grafo de dependencias

Lo que importa no es la lista de fábricas sino qué las bloquea. Casi todo cuelga de
**tres piezas**, y ninguna de las tres es "más `invokedynamic`":

```
                        ┌──────────────────────────────┐
                        │  D6 · LambdaMetafactory      │
                        │  lambdas + method refs       │
                        └──────────┬───────────────────┘
                                   │ necesita
              ┌────────────────────┴───────────┬─────────────────────┐
              ▼                                ▼                     ▼
   ┌────────────────────┐         ┌─────────────────────┐   ┌──────────────────┐
   │ objeto invocable   │         │ D2 · reference kinds│   │ captura de       │
   │ sintético          │         │ del MethodHandle    │   │ variables        │
   │ (NO generar clase) │         │ (hoy: sólo 1 de 9)  │   └──────────────────┘
   └────────────────────┘         └─────────┬───────────┘
                                            │ también lo necesita
                        ┌───────────────────┴────────────┐
                        ▼                                │
             ┌─────────────────────┐                     │
             │ D5 · ObjectMethods  │                     │
             │ records             │                     │
             └────────┬────────────┘                     │
                      │ necesita                         │
                      ▼                                  │
   ┌──────────────────────────────┐                      │
   │ D0 · conversión valor→texto  │◀── también la usa ───┘
   │ (hoy: `render`, con bug)     │      StringConcat ✅
   └──────────────────────────────┘
                      ▲
                      │
   ┌──────────────────┴───────────┐      ┌──────────────────────────┐
   │ D3 · SwitchBootstraps        │      │ D4 · condy (ldc Dynamic) │
   │ typeSwitch (patrones de tipo)│      │ ConstantBootstraps       │
   └──────────┬───────────────────┘      └──────────┬───────────────┘
              │ necesita                            │ necesita
              └──────────────┬──────────────────────┘
                             ▼
              ┌─────────────────────────────────┐
              │ D1 · `ldc` de constantes        │  ◀── EL BLOQUEANTE
              │ Class / MethodType / Dynamic    │      de mayor palanca
              │ (hoy: String/Integer/Float)     │
              └─────────────────────────────────┘
```

### Los tres bloqueantes con palanca

**1 · `ldc` de constantes ampliado (D1).** Hoy `ldc` modela `String`/`Integer`/`Float` y
`ldc2_w` sólo `Long`/`Double`. Bloquea a D3 (las etiquetas de un `typeSwitch` son
constantes `Class`), a D4 (condy *es* un `ldc`) y a D5 (`ObjectMethods` recibe la clase
del record). **No es un problema de `invokedynamic` en absoluto** — es un hueco de un
opcode que dábamos por hecho, y es el que más desbloquea por unidad de trabajo. Los
mirrors de `Class` ya existen en el heap, así que `ldc` de un `Class` es cargar la clase
y empujar su mirror.

**2 · Reference kinds del `MethodHandle` (D2).** `method_handle_target` resuelve el
`reference_index` con `methodref_target`, que sólo entiende `MethodRef`. De los **9**
reference kinds del JVMS resolvemos, en la práctica, **uno** (`REF_invokeStatic`). Un
`REF_getField` —los que un `record` le pasa a `ObjectMethods`— apunta a un `FieldRef` y
hoy hace `panic`. Bloquea D5 y D6.

**3 · La conversión valor→texto (D0).** Hoy vive en `render`, dentro del módulo de
`invokedynamic`, y **tiene un bug confirmado**: usa el `Display` de Rust en vez de las
reglas de `Double.toString`. La comparten `StringConcatFactory` (ya) y
`ObjectMethods.toString` (cuando llegue). Es la única divergencia que produce un
resultado **silenciosamente incorrecto**.

## Correlativas entre fases — lo que el diagrama de los tres pilares aplana

El pilar dibuja `A · JVM ──→ B · Compilador ──→ C · Bibliotecas`, una flecha en un solo
sentido. Alrededor de 0xba las dependencias van **en los dos sentidos**, y conviene
tenerlas escritas:

**A ↔ B — qué emite tu `javac` decide qué debe soportar tu VM.** Si tu compilador
resuelve la concatenación con cadenas de `StringBuilder` (estilo pre-Java 9), el circuito
de la Fase E cierra **sin** que la VM ejecute un solo indy. Si emite indy, no. Es una
decisión de la Fase B que fija requisitos de la Fase A, y hoy no está tomada.

**A ↔ B — la generación de clases en runtime necesitaría el escritor de la Fase B.** El
camino "fiel" para `LambdaMetafactory` es generar una clase que implemente la interfaz
funcional; eso pide un **escritor de `.class`**, que es exactamente el hito **B3**. Pero
hay un atajo que HotSpot no puede darse y vos sí: **no generar ninguna clase**. Alcanza
con un objeto sintético que lleve `(MethodId, argumentos capturados)` y que el despacho
de `invokeinterface` reconozca. Ese camino **no depende de la Fase B**.

**A ↔ C — `render` es `String.valueOf` disfrazado.** Arreglar el formato de flotantes en
Rust es reimplementar una porción de `java.lang`. Es correcto hacerlo **ahora** como
andamio —igual que el escalón de `fushite.Throwable`—, pero con la misma condición: el
día que la Fase C tenga tu `String.valueOf` compilado por tu `javac`, **`render` se
borra** y la concatenación llama a la biblioteca. Conviene que quede escrito para que no
se fosilice.

**A ↔ C — `altMetafactory` está bloqueada por serialización.** Las lambdas
`Serializable` necesitan `writeReplace`/`SerializedLambda`, o sea `java.io` de verdad:
Fase C avanzada. Por eso está marcada fuera de alcance y no como pendiente.

## La ruta

### D0 · Cerrar lo que ya está — ✅ **hecho** · Base

Sin dependencias. Todo lo que quedaba mal en la fábrica que ya cubrimos.

- ✅ **Formato de flotantes.** El arreglo no fue escribir el algoritmo sino **dejar de
  duplicarlo**: `parser::float_to_decimal` ya implementaba `Double.toString`/
  `Float.toString` con Dragon4, *round-half-to-even* y los denormales, y está validado
  byte a byte contra `javap` sobre `java.base`. `render` ahora lo usa en vez de
  `to_string()`. Una implementación nueva habría sido **peor**: la forma corta de Rust
  difiere de Java justamente en el desempate del último dígito y en los subnormales, o
  sea que habría pasado los tests fáciles y fallado en los bordes.
- ✅ **Verificador.** El nombre del call site pasa por `reject_special_name`
  (`<init>`/`<clinit>` rechazados), y `check_invokedynamic_zeros` valida que los dos
  operandos reservados de `ba idx1 idx2 00 00` sean cero (§4.9.1) — el análogo del
  chequeo que ya existía para `invokeinterface`. Ambas con tests que prueban el
  **rechazo**, no sólo que lo válido sigue pasando.
- ✅ **Argumentos estáticos.** Se leen por posición y un argumento que no sabemos leer
  **detiene la VM** en vez de descartarse: saltearlo correría los siguientes y splicearía
  el texto equivocado en cada U+0002. Se leen sólo para la fábrica que modelamos, así
  las demás siguen reportando el honesto "no hay intrínseco para esta fábrica".

**✓ Logrado:** `ConcatFloat` pasa sin `#[ignore]`, y ninguna ruta de 0xba produce ya un
resultado silenciosamente distinto al del `java` de JDK 25.

> **Nota de arquitectura.** `float_to_decimal` nació para `javap` —es *herramienta*, no
> biblioteca— y ahora lo consume también el runtime. Es la decisión correcta hoy (una
> implementación probada gana a dos), pero deja una coupling que conviene mirar: vive en
> `parser/` y ya tiene tres consumidores previstos (`javap`, la concatenación, y el
> `toString` de records en D5). Probablemente deba mudarse a un lugar compartido.

### D1 · `ldc` de constantes ampliado — ✅ **`Class` hecho** · Base

- ✅ **Literales de clase.** `ldc` de una constante `Class` empuja el mirror `Class<…>`.
  Dos propiedades que el demo fija: el mirror está **cacheado por Class ID**, así que el
  mismo literal evaluado dos veces da la *misma* referencia (`Foo.class == Foo.class`), y
  clases distintas nunca colapsan en un mirror. En el verificador el tipo empujado es
  `Class` — **no** la clase que la constante nombra —, que es lo que permite que
  `Object o = Foo.class` verifique.
  Resolución **sin inicializar**: un literal de clase no es un *uso activo* (§5.5), así
  que `Foo.class` no corre `Foo.<clinit>`; `load_class` frena en preparación, que es
  exactamente lo correcto.
- ⬜ **Literales de array** (`int[].class`). Las clases de array no tienen `.class`, así
  que `load_class` no les prepara mirror y el `ldc` **detiene la VM con un mensaje
  explícito** en vez de empujar `null`. La pieza que falta ya existe: el mirror sintético
  que arma `anewarray` (`array_class_mirror`), que habría que compartir.
- ⬜ **`MethodType`.** Postergado a propósito: necesita un objeto
  `java/lang/invoke/MethodType` real y **hoy no lo consume nadie** — sólo haría falta con
  bootstrap methods de verdad, que están fuera de alcance. Sigue en el `panic!` explícito.

**✓ Logrado:** `Foo.class` carga, mantiene identidad entre evaluaciones, y
`Class.isInstance` funciona sobre el mirror. Con esto **D3 y D5 quedan destrabados**.

### D2 · Reference kinds del `MethodHandle` — ✅ **hecho** · Avanzado

`method_handle_target` (que sólo miraba el lado de métodos) se reemplazó por
`method_handle`, que devuelve un `MethodHandleRef { kind, class, name, descriptor }`.

Dos cosas que el diseño anterior perdía:

- **El kind decide cómo se resuelve el índice.** Los kinds 1–4 (`getField`…`putStatic`)
  nombran un `Fieldref`; el resto un `Methodref`/`InterfaceMethodref`. Resolver todo como
  método era exactamente lo que dejaba irresolubles los getters de un `record`.
- **El kind es parte de la respuesta, no un detalle a descartar.** `REF_invokeVirtual` y
  `REF_invokeStatic` pueden nombrar el *mismo* método y significar llamadas distintas;
  `REF_newInvokeSpecial` sobre un `<init>` significa construir. Devolver sólo la terna
  `(clase, nombre, descriptor)` borraba esa información justo antes de que D5 y D6 la
  necesiten.

De paso, el consumidor mejoró: el bootstrap de un indy ahora falla con "el handle nombra
un campo, y un bootstrap method tiene que ser invocable" en vez de un `expect` opaco.

**✓ Logrado:** el fixture `java/Point.java` —un `record`, o sea el class file más chico
que lleva handles de dos kinds distintos— resuelve su `REF_invokeStatic`
(`ObjectMethods.bootstrap`) y sus dos `REF_getField` (`Point.x:I`, `Point.y:I`), con la
tabla de los 9 kinds cubierta por test y los valores fuera de `1..=9` rechazados.

### D3 · `SwitchBootstraps.typeSwitch` — patrones de tipo — ✅ **hecho** · Avanzado

El call site tiene forma `(Object, int) -> int`: recibe el valor y un **índice de
reinicio**, y responde *qué caso correr* como un índice que `javac` mete directo en un
`tableswitch`. Tres desenlaces:

| Situación | Respuesta |
|---|---|
| el selector es `null` | **-1** (el arm `case null`, o un error si no existe) |
| la primera label desde `restart` que matchea | **su índice** |
| ninguna matchea | **`labels.len()`** → cae en `default` |

El índice de reinicio no es decorativo: existe para los **patrones guardados**
(`case Foo f when …`). Una guarda que falla vuelve a entrar al *mismo* call site pidiendo
la *siguiente* coincidencia en vez de empezar de cero — por eso el match no puede
simplemente escanear desde 0.

De paso, el módulo se reestructuró: los argumentos estáticos ahora se decodifican
**por fábrica** dentro del borrow del pool y se llevan como un `enum Bootstrap` de datos
propios. Los mismos bytes significan cosas distintas según quién los reciba —una receta
de concatenación o una lista de labels—, y el diseño anterior los leía como si siempre
fueran texto.

**✓ Logrado:** `java/TypeSwitch.java` corre un `switch` sobre patrones de tipo con los
tres desenlaces cubiertos, incluido que **una subclase matchee la label de su
superclase** (el match camina la jerarquía con `is_subtype`, no compara identidad). Da
42 en el `java` de JDK 25 y en la nuestra.

> **Detalle del demo.** Lleva un arm `case null` a propósito: sin él `javac` protege el
> switch con `Objects.requireNonNull`, que arrastraría `java.util` antes de que el opcode
> bajo prueba llegue a ejecutarse. Con él, `null` pasa a ser el desenlace -1 del propio
> call site. Es una restricción de nuestro entorno, no del opcode.

**⬜ Falta**: labels que no sean `Class` — los patrones constantes (`case "a"`) llegan
como `String`/`Integer`, y los de enum como `EnumDesc`, que es un **condy** y por lo
tanto depende de D4. Hoy fallan fuerte con un mensaje que lo dice.

### D4 · Condy — `ldc` de constantes dinámicas · ⛔ **bloqueado** · Cumbre

> **Corrección.** La primera versión de este documento decía que D4 era *"el hito más
> barato en relación a lo que habilita, porque reusa toda la maquinaria de resolución de
> bootstrap"*. **Eso era falso**, y conviene dejar escrito por qué: la afirmación miraba
> el *mecanismo* e ignoraba para qué lo usa `javac` en la práctica.

El mecanismo sí es barato — tag 17 (`Dynamic`) leído desde `ldc`, reusando la resolución
de bootstrap que ya existe. El problema es lo que hay del otro lado.

**Medición.** `javac` de JDK 25 emite condy en **un solo** escenario —las etiquetas de un
`switch` con patrones sobre enums— y siempre con la misma fábrica:

```
Dynamic #2:invoke:Ljava/lang/Enum$EnumDesc;      → ConstantBootstraps.invoke
Dynamic #4:invoke:Ljava/lang/constant/ClassDesc; → ConstantBootstraps.invoke
```

`ConstantBootstraps.invoke` **ejecuta un `MethodHandle`** — acá, handles a
`Enum$EnumDesc.of` y `ClassDesc.of`. O sea que evaluar esa constante exigía dos cosas que
no teníamos:

1. ~~**Invocar Java desde un nativo**~~ → ✅ **resuelto**: `JVM::call_java` (ver «Estado»).
2. ~~**`java.lang.constant`**~~ → ✅ **escrito, y en Java** como correspondía:
   - `java/lang/constant/ClassDesc` — **interfaz** con `of(String)` estático, porque javac
     la referencia como `InterfaceMethodref`: la forma es parte del contrato. Su
     implementación va aparte (`ConstantClassDesc`), ya que un método estático de interfaz
     no puede instanciar su propia interfaz — el JDK real tiene la misma división.
   - `java/lang/Enum` — **clase**, con el `Enum(String, int)` que llama el `<init>` de todo
     enum, y la anidada `Enum$EnumDesc` con `of(ClassDesc, String)`. Tenía que existir bajo
     ese nombre binario exacto para que el condy resuelva.

   Agregar `java.lang.Enum` era el riesgo anotado: los enums funcionaban *porque* el super
   no resolvía y su `<init>` no-opeaba. Verificado que no rompe nada — y ahora los
   constantes además **llevan estado**: `name()` y `ordinal()` funcionan, que es lo que
   distingue "el superclase existe" de "la llamada no-opea".

Falta entonces: `ldc` de tag 17 con caché de condy, `ConstantBootstraps.invoke` como
intrínseco sobre `call_java`, y enseñarle a `typeSwitch` a matchear una label `EnumDesc`
(leer sus campos y comparar el selector contra el campo estático del enum con ese nombre).

> **Gotcha del build de `bootstrap/`.** Compilar una clase nueva con
> `--patch-module java.base=bootstrap` hace que javac **recompile implícitamente** las
> dependencias que encuentre en `bootstrap/` y las reescriba en `boot/` — con la versión
> de class file del JDK actual, no la original. Eso tocó `String`/`Class`/`Throwable`
> subiéndolas de la versión 65 a la 69 sin que nadie lo pidiera. La receta correcta es
> **`--release 21 -implicit:none`**: fija la versión y evita arrastrar dependencias.

Las variantes que *sí* serían implementables como intrínsecos —`nullConstant`,
`primitiveClass`, `enumConstant`, `getStaticFinal`, `explicitCast`— **`javac` no las
emite nunca**. Verificado también: cero condy en records, en concatenación y en `switch`
sobre `String`.

### ✅ Destrabado y hecho

Los dos bloqueantes cayeron: `call_java` primero, y `java.lang.constant` escrito en Java
después. Lo que quedó fue **medir dónde se dispara realmente**, y ahí hubo una segunda
corrección: yo había planeado "`ldc` de tag 17".

**`javac` no emite nunca un `ldc` de un `Dynamic`.** Los condy aparecen exclusivamente
como **argumentos estáticos de bootstrap**, formando un árbol:

```
typeSwitch  labels: condy#38, condy#42
  condy#38 → invoke(EnumDesc.of, condy#50, "RED")
  condy#42 → invoke(EnumDesc.of, condy#50, "GREEN")
                                 └─ condy#50 → invoke(ClassDesc.of, "EP$Color")
```

Así que el entry point no es `ldc` sino la **resolución de argumentos estáticos**, y la
caché no es una optimización: `condy#50` lo comparten las dos labels.

Eso forzó el cambio de forma que faltaba: `invokedynamic` pasó de función libre sobre
campos sueltos a **método `impl JVM`**, como sus hermanos `invoke*`. Era un outlier desde
el principio, y se cobró apenas resolver un argumento pudo implicar *correr Java*.

Piezas: `ClassFile::dynamic_constant` (el gemelo de `invokedynamic_site` para el tag 17),
`JVM::static_argument` (resuelve cualquier argumento estático, recursivo para condys),
`JVM::dynamic_constant` (evalúa y cachea), y `typeSwitch` distinguiendo labels de **tipo**
de labels **dinámicas**.

El matcheo de un enum no necesita `getstatic`: un `EnumDesc` es *nominal* —lleva nombre de
clase y nombre de constante—, y desde que `java.lang.Enum` existe, **la constante también
sabe cómo se llama**. Se comparan nombres de los dos lados.

**✓ Logrado:** `java/EnumSwitch.java` corre un `switch` sobre patrones de enum con los
tres desenlaces. 42 en JDK 25 y en la nuestra.

### Qué le falta a `ConstantBootstraps.invoke`

Auditado después de que funcionara, que es cuando conviene mirar:

- ✅ **El reference kind ahora se honra.** Estaba resuelto desde D2 y `dynamic_constant`
  lo descartaba, tratando siempre al target como estático. Un `REF_newInvokeSpecial`
  *construye* y un `REF_invokeVirtual` toma receptor: llamarlos como estáticos corría
  todos los argumentos un lugar y devolvía una constante equivocada **en silencio**. Sólo
  se modela la forma estática, y cualquier otra ahora **frena**.
- ✅ **El tipo declarado se verifica.** Una constante dinámica declara su propio
  descriptor, y no se comparaba nunca contra lo que devuelve el handle. El `invoke` real
  *adapta* el resultado a ese tipo; nosotros exigimos que ya coincidan, que es lo que
  `javac` emite.
- ✅ **Detección de ciclos.** Los argumentos de un condy pueden ser condys, así que la
  resolución camina un grafo; una constante que se alcance a sí misma recursaba hasta
  matar la pila de Rust. Ahora da un error diagnosticable.
- ⬜ **Las otras variantes** (`nullConstant`, `primitiveClass`, `enumConstant`,
  `getStaticFinal`, `explicitCast`, los `*VarHandle`) no están. Fallan fuerte, y `javac`
  no las emite.
- 🔴 **Una excepción del bootstrap se escapa del frame sintético.** Ver «Estado».

> **Sobre probar las tres primeras:** ninguna se puede disparar con bytecode de `javac`,
> porque son guardas contra formas que el compilador no produce. Lo testeable de verdad
> —la extracción del tipo de retorno de un descriptor— se sacó a `return_descriptor` y
> tiene su test. Las otras dos son comparaciones directas sin lógica propia.

### D5 · `ObjectMethods.bootstrap` — records — ✅ **hecho** · Avanzado

Los **tres** métodos de un record salen de **una sola** entrada de `BootstrapMethods`, y
lo único que los distingue es el **nombre del call site** (`equals`/`hashCode`/
`toString`). El código descartaba ese nombre — servía mientras cada fábrica tenía un
comportamiento único; acá los habría colapsado en uno.

Argumentos estáticos: la `Class` del record, la lista de nombres separada por `;`, y un
handle `REF_getField` por componente (de ahí la dependencia con D2). Los nombres y los
getters describen lo mismo desde dos lados, así que se cruzan: si no coinciden, leímos
mal la lista.

Semántica, medida contra JDK 25 y no supuesta:

| | |
|---|---|
| `toString` | `Point[x=1, y=2]` — nombre **simple** (después del último `/` y `$`) |
| `hashCode` | `31 * acumulador + hash(componente)` desde 0 → `Point(1,2)` = 33 |
| `equals` | misma clase exacta (los records son `final`) **y** todos los componentes |

El hash por componente **no** es el valor: un `boolean` hashea 1231/1237 y un `long`
pliega sus dos mitades.

**Componentes de tipo referencia** — cerrado después, una vez que existió `call_java`.
Los tres métodos preguntan ahora **al componente**, vía `JVM::call_virtual` (despacho por
la clase runtime, con el desvío al puente de nativos cuando corresponde):

- `equals` sigue `Objects.equals`: referencias idénticas son iguales sin preguntar, un
  `null` solo nunca lo es, y si no, decide el componente. Comparar las referencias
  —que es lo que hacía— es `==`, no `equals`, y **contestaba `false` para dos Strings
  iguales construidos por separado**. Era la única respuesta silenciosamente incorrecta
  que quedaba en toda la superficie de 0xba.
- `hashCode` sigue `Objects.hashCode`: `null` es 0, el resto responde por sí mismo.
- `toString` pide el texto al componente (`JVM::text_of`, nuestro `String.valueOf`).
  Antes leía los bytes de cualquier referencia como si fuera un `String`, lo que daba
  basura para todo lo que no lo fuera.

Un detalle que salió lindo: nuestro `java.lang.Object` **no declara `equals`**, así que
"no hay slot" *es* la manera en que una clase dice que hereda comparación por identidad —
y la identidad ya se probó antes. El `None` de `call_virtual` no es un error, es la
respuesta.

**✓ Logrado:** `java/RecordOps.java` maneja el fixture `Point` — igualdad por valor entre
objetos distintos, rechazo de `null` y de otra clase, el folding exacto y el layout de
`toString`. Da 42 en el `java` de JDK 25 y en la nuestra.

> **Hallazgo aparte: faltaba `java.lang.Record`.** El verificador rechazaba
> `record.equals(other)` con *"expected Object, found Point"*. No era un problema de
> D5: un record extiende `java.lang.Record`, y si esa clase no es cargable el recorrido
> de supertipos **se corta antes de llegar a `Object`**. Se agregó
> `bootstrap/java/lang/Record.java` (compilada con
> `--patch-module java.base=bootstrap`, como el resto del bootstrap). No aporta
> comportamiento: aporta el escalón que faltaba en la jerarquía.

### D6 · `LambdaMetafactory.metafactory` — lambdas y method references — ✅ **hecho** · Cumbre

Por el camino del **objeto sintético**, no por generación de clases. Y esto es lo único
del proyecto donde nuestro diseño es *más simple* que el de HotSpot en vez de una
concesión: el JDK genera una clase en runtime porque la interfaz de la JVM le está fijada
desde afuera. A nosotros no. Como somos dueños del despacho, alcanza con:

- una **clase sintética por call site** (nombre estable, así una lambda en un bucle no
  mintea una clase por vuelta), con mirror header-only como el de las clases de array;
- los **valores capturados en cada objeto** — dos closures sobre valores distintos salen
  del mismo call site, así que compartirlos en la clase los mezclaría;
- un gancho en `invokeinterface`: si el receptor es una lambda, saltar directo a la
  implementación anteponiendo las capturas, que son sus parámetros **iniciales**.

**✓ Logrado:** `java/Lambdas.java` corre lambda sin captura, method reference, lambda con
captura, y el caso decisivo — `adder(1)` y `adder(2)`, un call site y dos objetos con
capturas distintas que no se pisan. 42 en JDK 25 y en la nuestra.

> **El bug que costó encontrarlo.** Un mismo helper devolvía "el ancho" de una captura y
> se usaba para dos cosas distintas: el layout en el heap (4/8 **bytes**) y los slots del
> frame del callee (1/2 **slots**). Con 4 en vez de 1, el argumento propio del método de
> la interfaz aterrizaba cuatro slots más allá — pasado `max_locals`, donde
> `Frame::for_call` lo descarta silenciosamente. El síntoma era `a + n` devolviendo `n`.
> Ahora son dos funciones separadas (`capture_bytes` y `capture_slots`) precisamente para
> que no se puedan volver a confundir.

**✅ Capturas por referencia — cerrado.** El problema era real: el GC encuentra los
campos-referencia caminando los descriptores del `ClassFile`, y una clase sintética no
tiene class file, así que la captura era **invisible al colector** — ni se marcaba ni se
reescribía al mover el objeto.

La solución evitó el refactor caro. Enhebrar el registro de lambdas por el GC habría
tocado 6 call sites y los 4 entrypoints públicos (`mark`/`sweep`/`minor`/`compact`) — el
subsistema más delicado del proyecto. Pero **`reference_slots` ya consulta el metaspace**,
así que la clase sintética simplemente **declara ahí su layout de referencias** cuando se
mintea, y el colector la traza sin que nada cambie de firma. El layout depende sólo de los
descriptores, o sea que es el mismo para toda instancia del call site: se declara una vez,
junto al mirror.

Las capturas se escriben además por `store_reference`, no por `write_u32`: una lambda en
Old que captura un objeto joven es exactamente el puntero `old→young` para el que existe
el *remembered set*.

Verificado con `java/LambdaRef.java`, que captura un `String` y hace `System.gc()` en el
medio — falla de dos maneras distintas si algo está mal: la referencia puede colectarse
bajo los pies de la lambda, o quedar apuntando a la dirección vieja.

## Fuera de alcance (por ahora, y con razón)

- **`altMetafactory`** — bloqueada por serialización (Fase C avanzada).
- **Bootstrap methods definidos por el usuario** — imposibles bajo el enfoque de
  intrínsecos: exigirían `MethodHandle`/`MethodType`/`Lookup` como objetos reales y
  **ejecutar el bootstrap como código Java**, o sea empujar un frame desde el medio de un
  opcode y reanudar. Ese es el camino "fiel", y es un hito propio, no un paso de esta ruta.
- **Linkage real** (`CallSite` cacheado en vez de re-resolver por ejecución) — sin JIT no
  compra nada; se revisita en la Fase F.

## Estado

- ✅ 0xba despacha y verifica; `StringConcatFactory` implementado como intrínseco.
- ✅ **D0 cerrado.** No queda ninguna divergencia observable contra JDK 25 en las rutas
  que ejecutamos: lo que no soportamos **falla fuerte**, no en silencio.
- ✅ **D1 en su parte que importa**: `ldc` de literales de clase. Era el bloqueante que
  más desbloquea — y, notablemente, **no era un problema de `invokedynamic`**. Quedan
  pendientes los literales de array (falla fuerte) y `MethodType` (sin consumidores).
- ✅ **D2 cerrado**: los 9 reference kinds resueltos, con el kind conservado en la
  respuesta porque D5 y D6 lo necesitan para saber *cómo* se invoca lo que nombran.
- ✅ **D3 cerrado** para patrones de tipo: es la primera construcción del lenguaje que
  antes no corría y ahora sí. Cubrimos **2 de las 6** fábricas.
- ✅ **D4 cerrado.** Estuvo bloqueado hasta que cayeron sus dos prerrequisitos
  (`call_java` y `java.lang.constant`); el entry point resultó ser la resolución de
  argumentos estáticos, **no** `ldc`. Con esto van **5 de las 6** fábricas, y la sexta
  —`altMetafactory`— está fuera de alcance por serialización.
- ✅ **D5 cerrado**: los records comparan, hashean e imprimen. Van **3 de las 6**
  fábricas. De paso apareció y se tapó un agujero de la jerarquía: faltaba
  `java.lang.Record`.
- ✅ **D6 cerrado** por el camino del objeto sintético: lambdas y method references
  corren. Van **4 de las 6** fábricas — y las dos que faltan no son "las que siguen":
  `altMetafactory` está fuera de alcance (serialización) y D4 está bloqueado.
- ✅ **`JVM::call_java` — la capacidad transversal, hecha.** Resultó ser mucho más chica
  de lo que dije dos veces en este documento, y por la misma razón las dos veces: no
  miré el código. **El mecanismo ya existía entero**: `ensure_initialized` empuja un
  frame sintético y lo corre con un bucle de `run_one` anidado, en cada inicialización de
  clase desde hace meses. Sólo estaba especializado a `<clinit>` — sin argumentos y sin
  resultado. Generalizarlo fue agregar argumentos y capturar el valor de retorno; el
  propio `ensure_initialized` es ahora su primer consumidor, así que **todo el suite lo
  ejercita**.
  De paso apareció un bug latente: `return_void` respetaba `is_synthetic` para no avanzar
  el pc del llamador, pero **`ireturn` avanzaba siempre**. Un frame sintético que
  devolviera valor habría movido el pc del llamador por el ancho de cualquier opcode que
  estuviera ahí. Ahora los dos siguen la misma regla.
  Lo que destraba: `String.valueOf(Object)` en concatenación, componentes-referencia en
  `toString`/`equals` de records, `ConstantBootstraps.invoke` (D4), y bootstrap methods
  definidos por el usuario. **Los intrínsecos dejan de ser terminales.**
- 📌 **Quedan dos capacidades de VM pendientes**, y ninguna es de `invokedynamic`:
  1. **El GC no traza objetos de clases sintéticas**, sin lo cual una lambda no puede
     capturar referencias (D6).
  2. **Una excepción se escapa del frame sintético.** `unwind_with` popea frames hasta
     encontrar handler *o vaciar la pila* — no frena en el borde de un frame que empujó
     la VM. Si el `<clinit>` de una clase o el bootstrap de un condy lanza sin atrapar, el
     desenrollado sigue hacia los frames del llamador, y el bucle de `call_java` sale con
     menos frames de los que había. Es **preexistente** —le pasa igual a `<clinit>` desde
     siempre— pero `call_java` lo vuelve mucho más alcanzable. No es casualidad que el JVM
     real envuelva estos casos (`ExceptionInInitializerError`, `BootstrapMethodError`):
     ese wrapping existe precisamente **para frenar el unwind en el borde**. Arreglarlo es
     un hito chico pero propio.
- 📌 Y por encima de toda la ruta hay una capacidad transversal: **que un nativo pueda
  invocar un método Java y reanudar**. Hace falta para ejecutar bootstrap methods de
  verdad, para `String.valueOf(Object)`, para el `toString` de D5 y para D6. El
  mecanismo **ya existe** para `<clinit>` (frames sintéticos: la VM empuja el frame desde
  el medio de un opcode y la instrucción que lo disparó se reanuda sin avanzar el pc —
  ver `bytecode_interpreter.rs`, el manejo de `is_synthetic` al retornar). Generalizarlo
  es probablemente la pieza de mayor palanca de todo este documento.
- 📌 Decisión pendiente de la Fase B: **si tu `javac` emitirá indy o cadenas de
  `StringBuilder`.** De eso depende que D3–D6 sean obligatorios para cerrar el círculo
  (Fase E) o sólo para ejecutar bytecode ajeno.
