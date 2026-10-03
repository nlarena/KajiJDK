# -*- coding: utf-8 -*-
"""Corre el oraculo diferencial de `tools/difnum.py` sobre todo `java/texto/` y resume.

Las 48 pruebas de `java/texto/` imprimen lineas que empiezan con `//`, y la compuerta es que esas
lineas sean **las mismas** que las del JDK 25 corriendo el mismo fuente. Es la unica de las cuatro
compuertas que mira *texto formateado* --`DecimalFormat`, `DateTimeFormatter`, los parsers de HTML y
de DTD--, donde un entero de retorno no distingue "igual" de "parecido".

**La linea base es `48 pruebas, 2 con diferencias`: `Dtd1` y `Html4`.** Cualquier otro numero --y
sobre todo `48 de 48`-- es la compuerta rota, no el arbol. Vale la pena escribirlo porque esta
herramienta ya fallo en verde dos veces de dos maneras distintas (ver el encabezado de `difnum.py`), y
las dos veces el sintoma fue justamente que *todas* fallaran a la vez.

**Que VM se mide.** Por defecto el `bin/run-headless.exe` congelado, igual que `tools/suite.py`. Un
arreglo de la VM que todavia no se congelo **no se ve** con el default: hay que pasarselo con
`--vm target/release/run-headless.exe`. Lo mismo del lado del compilador con `--javac`.

Uso:

    python tools/texto.py                 # las 48
    python tools/texto.py Dtd Html        # solo las que contengan eso en el nombre
    python tools/texto.py --vm target/release/run-headless.exe
"""
import argparse
import glob
import os
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# El oraculo va **al lado de este archivo**. Apuntando a un directorio fijo --el scratchpad de otra
# sesion-- las 48 pruebas daban "sin salida" en verde.
DIFNUM = os.path.join(os.path.dirname(os.path.abspath(__file__)), "difnum.py")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("filtros", nargs="*", help="subcadenas del nombre; vacio = todas")
    # `KAJI_JAVAC`/`KAJI_RUN` siguen valiendo: permiten medir un arreglo **antes** de congelarlo sin
    # repetir la ruta en cada invocacion.
    ap.add_argument("--javac", default=os.environ.get("KAJI_JAVAC")
                    or os.path.join(RAIZ, "bin", "javac.exe"))
    ap.add_argument("--vm", default=os.environ.get("KAJI_RUN")
                    or os.path.join(RAIZ, "bin", "run-headless.exe"))
    args = ap.parse_args()

    # Absolutas y con los separadores de la plataforma: `CreateProcess` **no** encuentra una ruta
    # relativa con `/`, y `--vm target/release/run-headless.exe` es justo como se escribe a mano.
    args.javac = os.path.abspath(args.javac)
    entorno = dict(os.environ)
    entorno["KAJI_RUN"] = os.path.abspath(args.vm)

    fuentes = sorted(glob.glob(os.path.join(RAIZ, "java", "texto", "*.java")))
    if args.filtros:
        fuentes = [f for f in fuentes
                   if any(x.lower() in os.path.basename(f).lower() for x in args.filtros)]

    malas = []
    for f in fuentes:
        nombre = os.path.basename(f)[:-5]
        rel = "java/texto/" + os.path.basename(f)
        c = subprocess.run([args.javac, "--emit", "-cp", "KajiLibrary", rel],
                           capture_output=True, text=True, cwd=RAIZ, encoding="utf-8",
                           errors="replace")
        if c.returncode != 0:
            print("%-14s NO COMPILA" % nombre)
            malas.append(nombre)
            continue
        p = subprocess.run([sys.executable, DIFNUM, rel], capture_output=True, text=True,
                           cwd=RAIZ, env=entorno, encoding="utf-8", errors="replace")
        cola = [l for l in p.stdout.splitlines() if l.startswith("lineas:")]
        if cola:
            resumen = cola[-1]
        else:
            # Ni una linea de resumen: el oraculo no llego a comparar nada. Es la forma que toma la
            # compuerta cuando esta rota ella, asi que se muestra su error en vez de taparlo.
            resumen = (p.stdout.strip().splitlines() or ["sin salida"])[-1]
            if p.stderr.strip():
                resumen += "  |  " + p.stderr.strip().splitlines()[-1]
        print("%-14s %s" % (nombre, resumen))
        if p.returncode != 0:
            malas.append(nombre)
            for l in p.stdout.splitlines():
                if l.startswith("  JDK") or l.startswith("  nuestro"):
                    print("    " + l.strip())

    print("\n%d pruebas, %d con diferencias" % (len(fuentes), len(malas)))
    for m in malas:
        print("  " + m)
    return 1 if malas else 0


if __name__ == "__main__":
    sys.exit(main())
