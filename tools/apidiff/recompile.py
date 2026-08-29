"""Recompila TODA la fuente de KajiLibrary con el javac congelado, en un arbol aparte.

Uso:
    python tools/apidiff/recompile.py <dir_salida> <reporte.json>

`bin/javac.exe --emit X.java` escribe `X.class` **al lado de la fuente** y no tiene `-d`, asi
que compilar in situ pisaria los `.class` versionados. Por eso se copia el arbol de fuentes al
directorio de salida y se compila ahi, con `-cp` apuntando al KajiLibrary del repo (de donde
salen los `.class` de las dependencias — el mismo arreglo del workflow documentado).

El javac imprime diagnosticos pero **sale 0**, asi que el veredicto se toma por dos cosas: si
escribio el `.class`, y que dijo por salida. Las fuentes que no producen `.class` son el
resultado que importa.

Variables de entorno:
    KAJI_ROOT   raiz del repo (por defecto: dos niveles arriba de este script)
    KAJI_JAVAC  el javac a usar (por defecto: bin/javac.exe, el congelado). Sirve para medir el
                efecto de un arreglo *antes* de congelarlo.
"""
import json
import os
import shutil
import subprocess
import sys

ROOT = os.environ.get(
    "KAJI_ROOT", os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
)
LIB = os.path.join(ROOT, "KajiLibrary")
JAVAC = os.environ.get("KAJI_JAVAC", os.path.join(ROOT, "bin", "javac.exe"))
BS = chr(92)


def main(out_dir, report_path):
    if not os.path.exists(JAVAC):
        sys.exit(f"no esta el javac congelado en {JAVAC} — ver bin/FROZEN.md")

    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    sources = []
    for dirpath, _, filenames in os.walk(LIB):
        if "repros" in dirpath.replace(BS, "/").split("/"):
            continue
        for name in filenames:
            if not name.endswith(".java"):
                continue
            src = os.path.join(dirpath, name)
            rel = os.path.relpath(src, LIB)
            dst = os.path.join(out_dir, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)
            sources.append((rel.replace(BS, "/"), dst))
    sources.sort()
    print(f"fuentes copiadas: {len(sources)}", flush=True)

    results = []
    for i, (rel, path) in enumerate(sources):
        expected = path[:-5] + ".class"
        before = os.path.getmtime(expected) if os.path.exists(expected) else None
        proc = subprocess.run(
            [JAVAC, "--emit", "-cp", LIB, path],
            capture_output=True, encoding="utf-8", errors="replace",
        )
        out = ((proc.stdout or "") + (proc.stderr or "")).replace("\r", "").strip()
        wrote = os.path.exists(expected) and (
            before is None or os.path.getmtime(expected) != before
        )
        results.append({"file": rel, "wrote": wrote, "exit": proc.returncode, "out": out})
        if (i + 1) % 100 == 0:
            print(f"  {i + 1}/{len(sources)}", flush=True)

    with open(report_path, "w", encoding="utf-8") as fh:
        json.dump(results, fh, ensure_ascii=False, indent=1)

    bad = [r for r in results if not r["wrote"]]
    print(f"compiladas: {len(results) - len(bad)}   sin .class: {len(bad)}")
    for r in bad:
        first = next((l for l in r["out"].splitlines() if l.strip()), "")
        print(f"  FALLA {r['file']}: {first[-160:]}")
    print("escrito", report_path)
    return 1 if bad else 0


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1], sys.argv[2]))
