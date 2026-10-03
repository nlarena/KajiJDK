# -*- coding: utf-8 -*-
"""Oraculo diferencial de texto: corre una prueba de `java/texto/` en el JDK 25 y en nuestra VM y
compara las lineas de resultado. Lo usa `tools/texto.py`, que es por donde se lo corre normalmente.

**Por que esta en `tools/` y no en un scratchpad.** Vivio tres veces en el scratchpad de una sesion, y
las tres veces el directorio se limpio y la compuerta **fallo en verde**: las 48 pruebas daban "sin
salida" o `<falta>`, que se lee como "no hay nada que comparar" y no como "la herramienta esta rota".
Una compuerta que miente en verde es peor que no tenerla, y el unico arreglo que no se vuelve a
perder es que viva en el arbol, al lado de `recompile.py` y `suite.py`.

Uso:  python tools/difnum.py java/texto/Doc1.java
Sale con codigo != 0 si las lineas difieren, para que `texto.py` lo cuente como falla.

La prueba expone `public static int run()` e imprime sus resultados en lineas que empiezan con `//`.
Solo esas se comparan: el resto es ruido de la VM (el `[gc]`, la linea de entrada) y del JDK.

Dos reescrituras para el lado del JDK, porque `java` en modo fuente-suelto no acepta lo que nuestro
javac si:
  - `import a.b$C;`  ->  `import a.b;`      (el `$` de un anidado no es separador en el fuente Java)
  - `X$Y`            ->  `X.Y`
  - y se le agrega un `main` que llame a `run()`, que nuestra VM no necesita.
"""
import io
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Misma convencion que `tools/apidiff/`: `JDK_HOME` manda y hay un default para esta maquina.
os.environ.setdefault("JDK_HOME", r"C:\Program Files\Java\jdk-25")
JAVA = os.path.join(os.environ["JDK_HOME"], "bin", "java.exe")
# El fuente reescrito para el JDK es derivado y se regenera solo: va a un `.`-directorio de la raiz,
# ignorado como `.jdk25_tmp/` y `.corpus_tmp/`. Nunca al lado de este archivo, que esta versionado.
TMP = os.path.join(RAIZ, ".texto_tmp")

IMPORTA = re.compile(r"import\s+([\w.]+)[$](\w+)\s*;")
BINARIO = re.compile(r"\b([A-Z][A-Za-z0-9_]*)[$]([A-Z][A-Za-z0-9_]*)")


def lineas(txt):
    """Las lineas de resultado, sin el ruido de la VM."""
    return [l.rstrip() for l in txt.splitlines() if l.startswith("//")]


def main():
    fuente = sys.argv[1].replace("\\", "/")
    nombre = os.path.basename(fuente)[:-5]
    src = io.open(os.path.join(RAIZ, fuente), encoding="utf-8").read()

    # El fuente que ve el JDK: sin nombres binarios y con un `main` que arranque `run()`.
    jdk_src = IMPORTA.sub(lambda m: "import " + m.group(1) + ";", src)
    jdk_src = BINARIO.sub(lambda m: m.group(1) + "." + m.group(2), jdk_src)
    if "public static void main(" not in jdk_src:
        jdk_src = jdk_src.replace(
            "public static int run()",
            "public static void main(String[] a) throws Exception { run(); }\n"
            "    public static int run()",
            1,
        )

    os.makedirs(TMP, exist_ok=True)
    ruta = os.path.join(TMP, nombre + ".java")
    io.open(ruta, "w", encoding="utf-8", newline="\n").write(jdk_src)

    real = subprocess.run([JAVA, ruta], capture_output=True, text=True, cwd=RAIZ,
                          encoding="utf-8", errors="replace")
    clase = fuente[:-5].replace("/", os.sep) + ".class"
    corredor = os.environ.get("KAJI_RUN") or os.path.join(RAIZ, "bin", "run-headless.exe")
    nues = subprocess.run([corredor, clase, "run"], capture_output=True, text=True, cwd=RAIZ,
                          encoding="utf-8", errors="replace")

    esperado = lineas(real.stdout or "")
    # `run-headless` escribe la salida del programa en **stderr**; se leen los dos flujos para
    # no depender de cual sea. Leyendo solo stdout, las 48 pruebas daban "<falta>" en verde.
    obtenido = lineas((nues.stdout or "") + (nues.stderr or ""))

    if not esperado:
        print("el JDK no imprimio nada:")
        print((real.stderr or real.stdout or "").strip()[:800])
        return 2

    iguales = sum(1 for a, b in zip(esperado, obtenido) if a == b)
    print("lineas: %d/%d iguales" % (iguales, len(esperado)))
    if iguales == len(esperado) and len(obtenido) == len(esperado):
        return 0

    # Las primeras diferencias, con las dos versiones enfrentadas.
    mostradas = 0
    for i in range(max(len(esperado), len(obtenido))):
        a = esperado[i] if i < len(esperado) else "<falta>"
        b = obtenido[i] if i < len(obtenido) else "<falta>"
        if a != b:
            print("  JDK     " + a)
            print("  nuestro " + b)
            mostradas += 1
            if mostradas >= 6:
                break
    if not obtenido:
        print((nues.stderr or "").strip()[:500])
    return 1


if __name__ == "__main__":
    sys.exit(main())
