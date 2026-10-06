# -*- coding: utf-8 -*-
"""Vuelve a poner los `.class` VERSIONADOS en sincronia con el javac congelado.

Es el paso que `bin/FROZEN.md` llama "la obligacion de recompilar el arbol" y para el que no habia
herramienta: por eso la deuda se acumulo hasta **366 clases** desfasadas de su propio compilador
congelado para el refresco del 2026-10-06. El invariante que esto restituye es el de siempre --- un
`.class` del arbol es lo que el javac **congelado** emite de su `.java` ---, y lo que ese invariante
compra es que un `.class` distinto signifique "cambio la fuente" y no "cambio el compilador".

Dos mitades, porque los `.class` del arbol se producen de dos maneras:

1. **La biblioteca.** `tools/apidiff/recompile.py` ya compila las 4822 fuentes en un arbol aparte ---
   tiene que ser aparte, porque `--emit` escribe al lado de la fuente ---, asi que aca solo se copia
   su salida sobre `KajiLibrary/`. Se le pasa ese directorio como argumento.
2. **Las pruebas y los repros.** `java/`, `java/texto/` y `KajiLibrary/repros/` se compilan **in
   situ**, que para ellos es justamente donde va el `.class` versionado.

**Nunca agranda el conjunto versionado, y esto importa.** El conjunto es **parcial a proposito**: de
los repros se versionan 234 `.class` y no todos, en `java/` hay fuentes sin `.class` seguido, y muchos
repros **no compilan a proposito** (son el reproductor de un finding abierto). La primera version de
esta herramienta compilaba todo y dejaba **160 `.class` nuevos sin seguir** mas una pagina de falsas
fallas. Asi que la regla es: se recompila una fuente **solo si ya tiene un `.class` al lado**, y si
aparece alguno nuevo se borra. Expandir el conjunto es una decision de quien lo mantiene, no de esto.

Uso:

    python tools/apidiff/recompile.py /tmp/rc /tmp/rc.json     # primero esto
    python tools/resync.py /tmp/rc                             # y despues esto

    python tools/resync.py /tmp/rc --solo-pruebas   # sin tocar la biblioteca
    python tools/resync.py --solo-pruebas           # idem, sin pasar directorio

Variables de entorno: `KAJI_JAVAC` (por defecto `bin/javac.exe`, el congelado), igual que
`recompile.py`.

**Lo que NO hace, a proposito:** no commitea, no borra un `.class` cuya fuente ya no exista, y no toca
`bin/`. Y no verifica el punto fijo: para eso se vuelve a correr `recompile.py` y se diffea su salida
contra la anterior, que es lo que la entrada del refresco anota.
"""
import argparse
import os
import shutil
import subprocess
import sys

RAIZ = os.environ.get("KAJI_ROOT", os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
LIB = os.path.join(RAIZ, "KajiLibrary")
JAVAC = os.environ.get("KAJI_JAVAC", os.path.join(RAIZ, "bin", "javac.exe"))

# Donde se compila in situ, relativo a la raiz. `KajiLibrary/` entero queda afuera: esa mitad entra
# copiada desde la salida de `recompile.py`.
EN_SITIO = [("java", False), (os.path.join("java", "texto"), False),
            (os.path.join("KajiLibrary", "repros"), True)]


def copiar_biblioteca(out_dir):
    """Copia los `.class` de la recompilacion sobre `KajiLibrary/`, **solo donde ya habia uno**."""
    cambiados = iguales = ignorados = 0
    for dirpath, _, filenames in os.walk(out_dir):
        for name in filenames:
            if not name.endswith(".class"):
                continue
            src = os.path.join(dirpath, name)
            dst = os.path.join(LIB, os.path.relpath(src, out_dir))
            if not os.path.exists(dst):
                ignorados += 1          # el arbol no lo versiona; no se agrega
            elif open(src, "rb").read() == open(dst, "rb").read():
                iguales += 1
            else:
                shutil.copy2(src, dst)
                cambiados += 1
    print("biblioteca: %d actualizados, %d ya estaban bien, %d que el arbol no versiona"
          % (cambiados, iguales, ignorados))
    return cambiados


def tiene_class(src):
    """¿Esta fuente ya tiene algun `.class` al lado? (`X.class` o un anidado `X$...class`)."""
    carpeta = os.path.dirname(src)
    base = os.path.basename(src)[: -len(".java")]
    try:
        vecinos = os.listdir(carpeta)
    except OSError:
        return False
    return any(v == base + ".class" or v.startswith(base + "$") and v.endswith(".class")
               for v in vecinos)


def compilar_en_sitio(subdir, recursivo):
    carpeta = os.path.join(RAIZ, subdir)
    if not os.path.isdir(carpeta):
        return 0, []
    fuentes = []
    if recursivo:
        for dirpath, _, filenames in os.walk(carpeta):
            fuentes += [os.path.join(dirpath, f) for f in filenames if f.endswith(".java")]
    else:
        fuentes = [os.path.join(carpeta, f) for f in sorted(os.listdir(carpeta))
                   if f.endswith(".java")]
    fuentes = sorted(f for f in fuentes if tiene_class(f))

    # Para poder borrar lo que aparezca de nuevo: el conjunto de `.class` de antes.
    antes = set()
    for dirpath, _, filenames in os.walk(carpeta):
        if not recursivo and dirpath != carpeta:
            continue
        antes |= {os.path.join(dirpath, f) for f in filenames if f.endswith(".class")}

    fallas = []
    for i, src in enumerate(fuentes):
        # **El classpath lleva la carpeta de la propia fuente, y la raiz del subdirectorio.** Estas
        # pruebas se referencian entre si --- `java/Sub.java` extiende una clase de `java/pkg/`, un
        # repro de `finding_480/` usa su `Sub` de al lado ---, y los `.class` de esos vecinos son la
        # dependencia. Sin esto fallaban **73** con "no se encuentra el simbolo", y algunas de rebote
        # con "el generador de bytecode todavia no", que es el sintoma de haberse quedado sin tipo.
        cp = os.pathsep.join(dict.fromkeys([LIB, carpeta, os.path.dirname(src)]))
        p = subprocess.run([JAVAC, "--emit", "-cp", cp, src],
                           capture_output=True, encoding="utf-8", errors="replace")
        out = ((p.stdout or "") + (p.stderr or "")).replace("\r", "")
        # El mismo veredicto que `recompile.py`: lo que el javac **dijo**.
        linea = next((l for l in out.splitlines() if ": error" in l or "error:" in l), None)
        if linea:
            fallas.append((os.path.relpath(src, RAIZ), linea.strip()))
        if (i + 1) % 100 == 0:
            print("  %s: %d/%d" % (subdir, i + 1, len(fuentes)), flush=True)

    nuevos = 0
    for dirpath, _, filenames in os.walk(carpeta):
        if not recursivo and dirpath != carpeta:
            continue
        for f in filenames:
            p = os.path.join(dirpath, f)
            if f.endswith(".class") and p not in antes:
                os.remove(p)
                nuevos += 1
    extra = ", %d nuevos descartados" % nuevos if nuevos else ""
    print("%s: %d fuentes con `.class` versionado, %d con error%s"
          % (subdir, len(fuentes), len(fallas), extra))
    return len(fuentes), fallas


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out_dir", nargs="?", help="la salida de tools/apidiff/recompile.py")
    ap.add_argument("--solo-pruebas", action="store_true",
                    help="no tocar la biblioteca, solo java/, java/texto/ y los repros")
    args = ap.parse_args()
    if not os.path.exists(JAVAC):
        sys.exit("no esta el javac en %s — ver bin/FROZEN.md" % JAVAC)
    if not args.solo_pruebas:
        if not args.out_dir or not os.path.isdir(args.out_dir):
            sys.exit("falta el directorio de recompile.py (o pasa --solo-pruebas)")
        copiar_biblioteca(args.out_dir)

    todas = []
    for subdir, recursivo in EN_SITIO:
        _, fallas = compilar_en_sitio(subdir, recursivo)
        todas += fallas
    if todas:
        print("\n%d fuentes con `.class` versionado que NO compilan --- hay que mirarlas una por una,"
              " algunas son el repro de un finding abierto:" % len(todas))
        for rel, linea in todas:
            print("  %-46s %s" % (rel, linea[:110]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
