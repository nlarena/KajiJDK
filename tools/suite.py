"""Corre la bateria de pruebas de comportamiento de `java/` y dice cuales estan rotas.

No existia, y por eso nueve pruebas se pudrieron sin que nadie se enterara: sus `.class` seguian en el
arbol desde una version vieja de la biblioteca, la fuente ya no compilaba contra la actual, y correr
el `.class` daba un `NoSuchMethodError` que parecia un fallo de la VM.

Dos decisiones que valen la explicacion:

**Compila antes de correr.** Un `.class` que quedo en el arbol no prueba nada sobre la fuente que
esta al lado. La mitad del valor de esta herramienta es que un test que dejo de compilar aparece como
lo que es --roto-- y no como un resultado raro en tiempo de ejecucion.

**El punto de entrada se deduce, no se supone.** Las pruebas de esta casa usan tres formas: `run()`
devolviendo un entero (la mayoria), `main` imprimiendo su propio resumen, y unas pocas con varios
metodos estaticos con nombre propio. Suponer `run` hacia que las de las otras dos formas se
reportaran como fallas con un panic de la VM, que es ruido puro. Se leen de la fuente.

Uso:

    python tools/suite.py                # todas
    python tools/suite.py Fmt Chrono     # solo las que contengan eso en el nombre
    python tools/suite.py --javac X.exe --vm Y.exe
"""
import argparse
import glob
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(RAIZ, "java")
LIB = os.path.join(RAIZ, "KajiLibrary")

RE_RUN = re.compile(r"^\s+(?:public\s+)?static\s+int\s+run\s*\(\s*\)", re.M)
RE_MAIN = re.compile(r"^\s+public\s+static\s+void\s+main\s*\(", re.M)
RE_INT = re.compile(r"^\s+public\s+static\s+int\s+(\w+)\s*\(\s*\)", re.M)
RE_VALOR = re.compile(r"->\s*Some\(Int\((-?\d+)\)\)")


def puntos_de_entrada(fuente):
    """Los metodos con los que se invoca esta prueba, en el orden en que se prefieren."""
    if RE_RUN.search(fuente):
        return ["run"]
    if RE_MAIN.search(fuente):
        return ["main"]
    # Las que no tienen ninguno de los dos exponen varios estaticos con nombre propio, y hay que
    # llamarlos a todos: cada uno es una prueba.
    return RE_INT.findall(fuente)


def correr(vm, clase, metodo, timeout):
    try:
        p = subprocess.run([vm, clase, metodo], capture_output=True, text=True, encoding="utf-8", errors="replace",
                           timeout=timeout)
    except subprocess.TimeoutExpired:
        return "TIMEOUT", None
    salida = p.stdout + p.stderr
    if "panicked at" in salida:
        # La VM se cayo. Casi siempre es que el metodo no existe con esa firma.
        motivo = "panic"
        for l in salida.splitlines():
            if "panicked at" in l:
                motivo = "panic: " + salida.splitlines()[salida.splitlines().index(l) + 1].strip()
                break
        return motivo, None
    m = RE_VALOR.search(salida)
    if m:
        return "ok", int(m.group(1))
    if "-> None" in salida:
        # `main` no devuelve nada: el veredicto lo imprime la prueba misma.
        resumen = [l.strip() for l in salida.splitlines() if l.strip().startswith("TOTAL")]
        return "ok", (resumen[-1] if resumen else "(sin valor)")
    return "sin resultado", None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("filtros", nargs="*", help="subcadenas del nombre; vacio = todas")
    ap.add_argument("--javac", default=os.path.join(RAIZ, "bin", "javac.exe"))
    ap.add_argument("--vm", default=os.path.join(RAIZ, "bin", "run-headless.exe"))
    # 60 y no 30: con 30, `BeansTest` (35 s) y `StatTest` (34 s) se reportaban como TIMEOUT estando
    # las dos sanas, que es exactamente el falso positivo que esta herramienta existe para evitar.
    ap.add_argument("--timeout", type=int, default=60)
    ap.add_argument("--sin-compilar", action="store_true",
                    help="usa los .class que ya estan (mas rapido, menos honesto)")
    args = ap.parse_args()

    fuentes = sorted(glob.glob(os.path.join(JAVA, "*Test.java")))
    if args.filtros:
        fuentes = [f for f in fuentes
                   if any(x.lower() in os.path.basename(f).lower() for x in args.filtros)]

    rotas, corridas = [], 0
    for f in fuentes:
        nombre = os.path.basename(f)[:-5]
        fuente = open(f, encoding="utf-8", errors="replace").read()

        if not args.sin_compilar:
            p = subprocess.run([args.javac, "--emit", "-cp", LIB, f],
                               capture_output=True, text=True,
                               encoding="utf-8", errors="replace")
            err = [l for l in (p.stdout + p.stderr).splitlines() if "error" in l]
            if err:
                rotas.append((nombre, "NO COMPILA", err[0].strip()))
                print("%-22s NO COMPILA  %s" % (nombre, err[0].strip()))
                continue

        entradas = puntos_de_entrada(fuente)
        if not entradas:
            rotas.append((nombre, "SIN ENTRADA", "no expone run(), main ni estaticos int"))
            print("%-22s SIN ENTRADA" % nombre)
            continue

        for m in entradas:
            corridas += 1
            estado, valor = correr(args.vm, os.path.join(JAVA, nombre + ".class"), m,
                                   args.timeout)
            etiqueta = nombre if m in ("run", "main") else "%s.%s" % (nombre, m)
            if estado == "ok":
                print("%-22s %s" % (etiqueta, valor))
            else:
                rotas.append((etiqueta, estado, ""))
                print("%-22s %s" % (etiqueta, estado.upper()))

    print("\n%d invocaciones, %d rotas" % (corridas, len(rotas)))
    for n, e, d in rotas:
        print("  %-22s %s %s" % (n, e, d))
    return 1 if rotas else 0


if __name__ == "__main__":
    sys.exit(main())
