# -*- coding: utf-8 -*-
"""Cruce con la JVM real: corre **nuestro propio `.class`** en nuestra VM y en el `java` del JDK 25 y
compara el entero que imprime.

Es la cuarta compuerta --- la unica que verifica que el `.class` que emitimos es **valido**, y no solo
que nuestra VM lo acepta --. Las otras tres (recompilacion, suite, los 48 diferenciales de texto)
preguntan por nuestra implementacion; esta pregunta por el artefacto.

**Por que vive en `tools/`.** `bin/FROZEN.md` la pide en cada refresco --- "de las 45 pruebas
comparables, 43 dan el mismo entero que `java` real; las dos que no son `JcIc` y `WdWide`" --- y la
herramienta que lo medía era un `cruce.sh` en el scratchpad de una sesion, que se perdio. Es la misma
historia que `difnum.py`, que se perdio tres veces antes de mudarse aca. Un refresco que no puede
reproducir su propia verificacion no la tiene.

**Que cuenta como comparable, y por que tan angosto.** Los **dos** lados tienen que imprimir un
**entero pelado** como ultima linea. Nada mas alcanza: las pruebas de `java/` imprimen de todo ---
prosa, rutas absolutas de otra maquina, avisos del JDK sobre metodos restringidos, trazas de un modulo
que no exporta lo que la prueba toca ---, y comparar esa linea da ruido y no senal. La primera version
de esta herramienta contaba **57 divergencias sobre 287 pruebas** y casi todas eran falsas. Una
herramienta que grita en falso es una a la que se le deja de mirar la salida, y asi es como pasa
desapercibida una de verdad. Por eso la pregunta es la mas estrecha que sirve: *los dos imprimieron un
numero, y es el mismo numero*.

**Los cuatro veredictos:**

- `=` los dos imprimen el mismo entero.
- `X` **divergencia numerica**: los dos imprimen un entero y no es el mismo.
- `R` **la JVM real RECHAZA nuestra clase** (`VerifyError`, `ClassFormatError`,
  `IncompatibleClassChangeError`, `UnsupportedClassVersionError`). Es la peor forma --- un `.class` que
  nuestra VM corre y la de verdad no carga --- y se cuenta aparte, **sin pedir que el otro lado sea un
  entero**, porque el rechazo ya es el resultado. Es para ver esto que el cruce existe.
- `-` **no comparable**: a alguno de los dos lados le falta algo para arrancar, o lo que imprime no es
  un numero. No cuenta como falla; con `-v` se listan.

Uso:

    python tools/cruce.py                  # todas las de java/
    python tools/cruce.py JcIc WdWide      # solo las que contengan eso en el nombre
    python tools/cruce.py -v               # una linea por prueba, y el detalle de las no comparables
    python tools/cruce.py --vm target/release/run-headless.exe

Por defecto corre el `bin/run-headless.exe` **congelado** sobre los `.class` **versionados** del arbol,
que es lo que un refresco tiene que verificar. Para medir un arreglo antes de congelarlo hay que
pasarle `--vm` y recompilar los `.class` antes --- ojo, que eso los pisa (ver el aviso de
`tools/suite.py`).
"""
import argparse
import glob
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.environ.setdefault("JDK_HOME", r"C:\Program Files\Java\jdk-25")
JAVA = os.path.join(os.environ["JDK_HOME"], "bin", "java.exe")

# Lo que nuestra VM imprime y no es de la prueba: el GC, la linea de entrada, la cola de un panic.
RUIDO = re.compile(r"^\[gc\]|\.class \w+\(|^note: run with `RUST_BACKTRACE")
ENTERO = re.compile(r"^-?\d+$")

# El `java` real no pudo **empezar**: no es una divergencia nuestra.
NO_COMPARABLE = (
    "Could not find or load main class",
    "NoClassDefFoundError",
    "ClassNotFoundException",
    "no main method",
    "must extend javafx.application.Application",
    "NoSuchMethodError",
)
# El `java` real **rechaza el artefacto**: eso si es nuestro, y es la peor forma.
RECHAZO = (
    "VerifyError",
    "ClassFormatError",
    "IncompatibleClassChangeError",
    "UnsupportedClassVersionError",
)


def ultima_linea(texto):
    lineas = [l.strip() for l in texto.splitlines() if l.strip() and not RUIDO.search(l.strip())]
    return lineas[-1] if lineas else None


def correr(cmd, cwd=None):
    """La salida entera y su ultima linea util. Nuestra VM escribe el programa por **stderr**."""
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, encoding="utf-8",
                           errors="replace", timeout=300)
    except subprocess.TimeoutExpired:
        return "timeout", "timeout"
    todo = (p.stdout or "") + "\n" + (p.stderr or "")
    return todo, ultima_linea(todo)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("filtros", nargs="*", help="subcadenas del nombre; vacio = todas")
    ap.add_argument("--vm", default=os.path.join(RAIZ, "bin", "run-headless.exe"))
    ap.add_argument("-v", "--verbose", action="store_true")
    args = ap.parse_args()

    clases = sorted(glob.glob(os.path.join(RAIZ, "java", "*.class")))
    # Una anidada (`X$Y.class`) no es un punto de entrada.
    clases = [c for c in clases if "$" not in os.path.basename(c)]
    if args.filtros:
        clases = [c for c in clases if any(f in os.path.basename(c) for f in args.filtros)]

    iguales, distintos, rechazadas, no_comp = [], [], [], []
    for c in clases:
        nombre = os.path.basename(c)[: -len(".class")]
        todo_j, jdk = correr([JAVA, "-cp", "java", nombre], cwd=RAIZ)
        rechazo = next((r for r in RECHAZO if r in todo_j), None)
        if rechazo is None and any(m in todo_j for m in NO_COMPARABLE):
            no_comp.append((nombre, "el `java` real no arranco: %s" % (jdk or "")[:60]))
            continue
        _, nuestro = correr([args.vm, c, "main"])
        if rechazo:
            rechazadas.append(nombre)
            print("  R %-22s la JVM real RECHAZA nuestra clase: %s  (nuestra VM: %s)"
                  % (nombre, rechazo, nuestro))
        elif not (ENTERO.match(nuestro or "") and ENTERO.match(jdk or "")):
            no_comp.append((nombre, "nuestro=%s  jdk=%s" % (nuestro, jdk)))
        elif nuestro == jdk:
            iguales.append(nombre)
            if args.verbose:
                print("  = %-22s %s" % (nombre, jdk))
        else:
            distintos.append(nombre)
            print("  X %-22s nuestro=%s  jdk=%s" % (nombre, nuestro, jdk))

    print("\n%d comparables, %d iguales, %d DISTINTOS; %d RECHAZADAS por la JVM real"
          "  (%d no comparables)"
          % (len(iguales) + len(distintos), len(iguales), len(distintos), len(rechazadas),
             len(no_comp)))
    if args.verbose and no_comp:
        print("\nno comparables (falta un lado, o lo que imprime no es un numero):")
        for n, salida in no_comp:
            print("  %-22s %s" % (n, (salida or "(sin salida)")[:90]))
    sys.exit(1 if distintos or rechazadas else 0)


if __name__ == "__main__":
    main()
