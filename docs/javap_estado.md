# Estado del clon de `javap` — handoff de sesión (2026-06-02)

Documento para **retomar después**. Resume dónde está el clon byte-idéntico de
`javap`, la infraestructura de testing, y el **backlog de errores priorizado**
que surgió del differential testing contra el JDK 25.

---

## TL;DR

- **Nivel 0 (clonar `javap -v` byte a byte) prácticamente terminado.**
- Consistencia contra `javap` del **JDK 25** sobre `java.base` (**7379 clases**):
  **99.93% byte-idéntico** (7374) vs javap-UTF-8, **0 crashes**, **0 bugs de
  lógica reales** restantes. Es el **máximo alcanzable**: de los 5 restantes,
  4 son clases donde javap *mismo* reporta error y 1 es file-not-found.
- Todos los atributos del formato están implementados (incl. Parameter/Type
  annotations y los del sistema de módulos: Module/ModulePackages/ModuleMainClass/
  ModuleTarget/ModuleHashes).
- Con javap por defecto (cp1252) la cifra es 98%, pero esos diffs son corrupción
  de charset de la consola de javap, no nuestra (ver "Nota de charset").

---

## Qué está implementado

**Núcleo:** estructura `.class`, constant pool (17 tags + Tombstone), header,
`fields`/`methods`/`attributes`, flags de visibilidad CLI
(`-public`/`-protected`/`-package`/`-p`).

**Code:** desensamblado completo de opcodes (incl. `tableswitch`/`lookupswitch`
multilínea, `wide`, `invokedynamic`, `invokeinterface`), **Exception table**,
`args_size` (conteo de args, long/double = 1).

**Atributos:** `SourceFile`, `LineNumberTable`, `StackMapTable` (los 7 frames),
`Signature` (parser de la gramática de genéricos), `BootstrapMethods`,
`InnerClasses`, `Exceptions`, `ConstantValue`, `RuntimeVisible/InvisibleAnnotations`
(recursivo), `Record`, `MethodParameters`, `NestHost`/`NestMembers`,
`LocalVariableTable`/`LocalVariableTypeTable`, `Deprecated`.

**Formateo numérico:** float/double fiel a `Double.toString`/`Float.toString` de
Java (Dragon4 con big-integer propio + special-case de `MIN_VALUE`). Ver
`src/jvm/parser/float_to_decimal.rs`.

**Fixes sistémicos de esta sesión** (los que subieron 45%→73%):
1. **Trailing-whitespace trim global** — macro `crate::pln!` (en `main.rs`) que
   recorta el espacio final de cada línea, como hace javap. Reemplazó a `println!`
   en todo el path de javap.
2. **Lista vacía `[]`** en StackMapTable (era `[ ]`).
3. **`InnerClasses:` vacío** — si el filtro de visibilidad oculta todas las
   entradas, javap omite el header (nosotros lo imprimíamos vacío).
4. **`tableswitch`/`lookupswitch`** — formato multilínea correcto.
5. **Escape de strings** (`\t \n \r \b \f \" \' \\` + `\uXXXX`).
6. **`newarray`** — un espacio extra (quirk de javap).

Otros fixes (sesiones recientes): `enum`→`class` en la declaración, tablas de
flags de miembro completas (ENUM/BRIDGE/VARARGS/STRICT/VOLATILE/TRANSIENT),
`extends Super`, constructor genérico, `bootstrap` String/Class args sin prefijo,
quoting de nombres de clase array.

---

## Infraestructura de testing

- **`java/`** — ~33 fixtures byte-idénticos (Add, Sample, Branch, Generic, Lam,
  Record/Point/Rec, Color enum, Inner, LocalVars, Catch, Switches, Esc…).
- **`corpus/`** *(gitignored)* — 114 clases del JDK 21 curadas por feature.
- **JDK 25** descargado en **`.jdk25_tmp/`** *(gitignored, ~650 MB)*:
  `jdk-25.0.3+9` + **7379 clases de `java.base`** extraídas en `.jdk25_tmp/classes/`.
  Listas en `.jdk25_tmp/{all_classes,half1,half2}.txt`.
- **`tools/diffcheck.py`** *(versionado)* — harness diferencial: batchea `javap`
  (amortiza el arranque de JVM, ~30x más rápido), paraleliza, categoriza
  divergencias y reporta progreso.
  Uso (desde la raíz del repo): `python tools/diffcheck.py <ruta-a-javap25.exe> <lista.txt>`
  - javap 25: `.jdk25_tmp/jdk-25.0.3+9/bin/javap.exe`
  - **Nota:** javap 25 == javap 21 en formato para lo que renderizamos (verificado
    con `Comparable`/`Object`), así que el differential es válido. Las clases v69
    necesitan el javap 25 (el 21 no las lee).

---

## Resultado del differential (JDK 25 `java.base`, 7379 clases)

**Última corrida: 99.93% byte-idéntico** (7374/7379) comparando contra javap
forzado a **stdout UTF-8** (la comparación justa — ver nota de charset abajo).
Evolución: 45% → 73% → 91% → 96% → 97% → 98% → 99.8% → 99.92% → **99.93%**.

```
OK (byte-idéntico)                       7374  (99.93%)
DIFF:Unmatched bit  (clases *$Holder)       4   ← javap emite Error: por bit 0x2
ERR  (file not found)                       1   ← pre-existente
```

**No queda NINGÚN bug de lógica real** ni atributo sin implementar. Los 5
restantes son no-implementables: 4 clases internas del JDK (`*$Holder`) donde
javap *mismo* escupe `Error: Access Flags: Unmatched bit position 0x2` a stdout,
y 1 file-not-found. → de hecho 7374/7375 = **99.99%** de las clases comparables.

**Parameter/Type annotations** (✅): ParameterAnnotations reusa el renderer
agrupando por `parameter N:`. TypeAnnotations parsea `target_type`/`target_info`
(tabla completa de JVMS §4.7.20.1) + `type_path`, anexando la descripción del
target a la línea cruda (`i: #t(): CLASS_TYPE_PARAMETER, param_index=0`).

**Module / módulos** (✅): `attributes/module.rs`. Hallazgos clave de javap:
(a) los constantes `Module`/`Package` (tags 19/20) se imprimen como `Unknown` en
el pool; (b) la declaración de cabecera es `module java.base@25.0.3`; (c) el
bloque `Module:` lista name/flags/version + `requires`/`exports`/`opens`/`uses`/
`provides` con `//` en columna `indent+40`; nombres de módulo citados y dotted,
de paquete con slashes; (d) `ModulePackages` usa nombres dotted sin comillas;
(e) atributos JDK-internos `ModuleTarget` y `ModuleHashes` (hash en hex). El
único `module-info` del corpus coincide byte-a-byte.

### ⚠️ Nota de charset (cp1252 vs UTF-8) — importante

Con javap **por defecto** (sin forzar encoding) la cifra baja a **98%**: ~119
clases difieren *sólo* en caracteres no-ASCII. Causa: en Windows javap escribe a
stdout con el code page de consola (cp1252), que **corrompe** caracteres no
representables (`¤`→bytes inválidos, `‰`/U+2028→`?`, etc.). **Nuestro output
UTF-8 es el correcto.** Verificado: con `javap -J-Dstdout.encoding=UTF-8` esas
~119 clases pasan a **OK exacto**. No debemos emular la pérdida de cp1252 (sería
una regresión en uso real). Por eso la métrica oficial es contra javap-UTF-8.

**Fixes de esta sesión** (91%→99.8%, todos byte-idénticos contra javap 25):
1. **Decoder de modified UTF-8** (`constant_pool.rs`): `null`=`C0 80` y pares
   surrogate CESU-8 para chars >U+FFFF. Eliminó **54 de 55 ERR**.
2. **Líneas en blanco en `tableswitch`/`lookupswitch`**: javap intercala una
   línea en blanco *antes de cada case numerado* (no antes de `default`/`}`).
3. **`wide` → sufijo `_w`**: javap renderiza `iinc_w 10, 600` y `iload_w N`
   (no `wide iinc 10 by 600`).
4. **Blank line tras campos**: javap pone blank después de *cada* field (y de
   cada método salvo el último) — afecta clases que terminan en field (sin
   métodos).
5. **`uninitialized_this`→`this`** en `verification_type_info` (StackMapTable).
6. **`multianewarray #N,  dim`** (coma + 2 espacios, no `#N dim N`).
7. **Comillas en nombres no-identificador** (`"java/net/package-info"`): regla
   general (cualquier char fuera de `[A-Za-z0-9/_$]`), subsume la de arrays.
8. **Args numéricos de `BootstrapMethods`**: valor desnudo (`#352 5`, no
   `#352 int 5`); con sufijo `l/f/d` para wide.
9. **`escape()` extendido** a DEL + bloque C1 (0x7F..=0x9F) como `\uXXXX`.

**Hechos en sesiones previas**: `EnclosingMethod`, `PermittedSubclasses`,
`AnnotationDefault`, `volatile`/`transient`, `//` vacío en String del pool.

---

## Backlog de errores (lo que queda — 5 clases, todas no-implementables)

**No quedan atributos ni bugs por implementar.** Todos los atributos del formato
`.class` están cubiertos. Lo que resta son artefactos:

### No-implementables / artefactos (no son bugs)
- **CHARSET (~119 con javap por defecto)** — artefacto de cp1252 de la consola
  de javap; nuestro UTF-8 es correcto. Ver "Nota de charset" arriba. **No tocar.**
- **`*$Holder` (4)** — `Invokers$Holder`, `LambdaForm$Holder`,
  `DelegatingMethodHandle$Holder`, `DirectMethodHandle$Holder`: el JDK las genera
  con el bit `0x2` (reservado para CLASS), y javap mismo escupe a stdout
  `Error: Access Flags: Unmatched bit position 0x2 …` + muestra `, 0x2` en flags.
  Para igualar habría que emular esos `Error:` y el render de bits desconocidos.
  Marginal.
- **ERR file-not-found (1)** — pre-existente, una clase de la lista no se extrajo.

### ✅ Hechos (esta sesión y previas)
`EnclosingMethod`, `PermittedSubclasses`, `AnnotationDefault`, modified UTF-8,
blank lines de switch, `wide`→`_w`, blank tras fields, `uninitialized_this`→
`this`, `multianewarray`, quoting de `package-info`, args numéricos de
BootstrapMethods, `escape()` C1.

---

## Cómo retomar

1. Recompilar: `cargo build` (debería seguir en 0 warnings, ~31 fixtures verdes,
   `cargo test` ok — el test de la batería float skipea sin `.float_battery.txt`).
2. Re-correr la consistencia:
   `python tools/diffcheck.py .jdk25_tmp/jdk-25.0.3+9/bin/javap.exe .jdk25_tmp/all_classes.txt`
3. Atacar el backlog en orden: **modified UTF-8** (robustez) → **blank lines** y
   **volatile/transient** (OTHER) → **EnclosingMethod**/**PermittedSubclasses**.
4. Si se borró `.jdk25_tmp/`, re-bajar con la API de Adoptium:
   `curl -sL -o jdk25.zip "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse"`
   y `jimage extract --include "glob:/java.base/**"`.

---

*Fixtures nuevos de esta sesión en `java/`: Switches, Esc, Catch, LocalVars,
ArgSz, Point, Rec, Color, Inner, Annotated/RichUse/NestUse, Generic/Sig*, etc.*
