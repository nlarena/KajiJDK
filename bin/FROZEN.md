# `bin/` — herramientas congeladas

Snapshot binario de **todas** las herramientas del proyecto, commiteado a propósito.

**Por qué se commitea.** La sesión de biblioteca compila KajiLibrary con un `javac` *fijo*:
si cada máquina usa el que le salga de su `src/javac` del momento, un `.class` distinto deja
de significar "la fuente cambió" y pasa a significar "el compilador cambió", que es
exactamente la señal que el dogfooding necesita leer. Congelarlas también desacopla el
trabajo de biblioteca de que el árbol de `src/` compile en ese instante — que no es
hipotético: hay sesiones editando `src/jvm/` en paralelo.

> **Costo, para tenerlo presente:** son ~48 MB y los `.exe` no deltifican, así que **cada
> refresco suma otros ~48 MB al historial**. Refrescar sólo cuando haya una razón (un fix de
> compilador que la biblioteca necesita), no por rutina.

## Procedencia

| | |
|---|---|
| Commit | `8bbae1b` |
| Construido desde | una copia **limpia de HEAD** (`git archive HEAD`), no del árbol de trabajo |
| Toolchain | `rustc 1.98.0 (88d9e12ae 2026-08-18)`, host `x86_64-pc-windows-gnu`, LLVM 22.1.8 |
| Perfil | `cargo build --release` |
| Fecha | 2026-08-22 |

Se construyó desde una copia limpia y no desde el working tree a propósito: en ese momento
`src/javac` era idéntico a HEAD, pero `src/jvm` tenía 222 líneas sin commitear de otra
sesión. Un snapshot "congelado" que se lleve cambios a medio hacer no sirve de referencia.

## Las herramientas

| Binario | SHA-256 | Qué es |
|---|---|---|
| `javac.exe` | `0c407e84…e583` | El compilador. `--emit X.java` → `X.class` |
| `jvm.exe` | `a8555611…787a` | El desensamblador estilo `javap` (Nivel 0) y CLI principal |
| `run-headless.exe` | `a2532f0a…387f` | Corre `<File.class> <método>` con el intérprete (modo green) |
| `jvm-step.exe` | `e8023a12…316b` | El visor paso a paso del intérprete |
| `javac-step.exe` | `6f2e5b89…14fe` | El visor paso a paso del compilador |
| `jdb.exe` | `98ca399e…6508` | El depurador (Fase I, JPDA) |
| `jvm-jdwp.exe` | `a6ad1272…7948` | Servidor JDWP (`dt_socket`) sobre nuestra VM |
| `jdi-attach.exe` | `fa8ff62a…7d5a` | Cliente JDI de attach |
| `jimage.exe` | `7cea42d4…ba89` | Lector/escritor del contenedor `jimage` |
| `jlink.exe` | `072794ab…1fbf` | Enlazador de imágenes de runtime |

Hashes completos: `sha256sum bin/*.exe` (o `Get-FileHash`).

## Nombres — ojo al leer la documentación vieja

`KajiLibrary/COMPILER_FINDINGS.md` menciona `bin/javac-frozen.exe` y `bin/javap-clon.exe`.
Acá los binarios llevan **el nombre que les da cargo**, sin duplicados:

| Nombre en los docs | Archivo real |
|---|---|
| `bin/javac-frozen.exe` | `bin/javac.exe` |
| `bin/javap-clon.exe` | `bin/jvm.exe` |

(`javac-step.exe` es el visor del compilador, **no** el javac congelado.)

## Refrescar el snapshot

Desde una copia limpia, para que la procedencia siga siendo un commit y no un working tree:

```bash
git archive HEAD | tar -x -C /tmp/kaji-frozen-src
CARGO_TARGET_DIR=/tmp/kaji-frozen-target cargo build --release --manifest-path /tmp/kaji-frozen-src/Cargo.toml
cp /tmp/kaji-frozen-target/release/*.exe bin/
```

Y actualizar la tabla de procedencia de arriba. Si el árbol de trabajo tiene cambios que el
snapshot necesita, commitealos primero — no construyas desde el working tree.

## Verificado al congelar

- `javac.exe --emit KajiLibrary/repros/finding_09.java` → emite el `.class`.
- `jvm.exe java/Add.class` → desensambla.
- `run-headless`, `jimage`, `jlink`, `jdb`, `jdi-attach`, `jvm-jdwp`, `jvm-step`,
  `javac-step` → arrancan y muestran su uso.

Es un smoke test de que los binarios están sanos, no una validación funcional.
