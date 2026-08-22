#!/usr/bin/env python3
"""Diferencial de **emisión**: compila cada .java con NUESTRO javac y con el javac real, desensambla
ambos .class con el MISMO javap (`-p -c`) y compara el resultado **normalizado** (sin los índices del
constant pool, que difieren por el orden de emisión pero cuyos comentarios resueltos coinciden).

Mide B6 (fidelidad): un DIFF es una divergencia de emisión real (un ítem concreto de B6). Distinto del
`diffcheck.py`, que compara nuestro *javap* contra el real sobre el MISMO .class (fidelidad del lector).

Uso:  python emitdiff.py <javac_real> <javap_real> <dir_corpus> [-cp <classpath>]
Los .java del corpus deben estar en el **paquete por defecto** (sin `package`) para que ambos
compiladores emitan .class planos y el emparejamiento sea por nombre de archivo.
"""
import subprocess, sys, os, re, difflib, shutil, tempfile
from collections import Counter

OUR = os.path.abspath("./target/debug/javac")


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, encoding="utf-8", errors="replace")
    return r.returncode, (r.stdout or "").replace("\r", ""), (r.stderr or "").replace("\r", "")


def normalize(javap_out):
    """Deja el desensamblado comparable entre compiladores: quita el encabezado, reemplaza los índices
    del pool `#\\d+` por `#` (el orden de emisión difiere; el comentario resuelto no) y colapsa los
    espacios (el ancho del índice cambiaba el padding hasta el comentario)."""
    lines = []
    for line in javap_out.split("\n"):
        if line.startswith("Compiled from"):
            continue
        line = re.sub(r"#\d+", "#", line)
        line = re.sub(r"\s+", " ", line).strip()
        if line:
            lines.append(line)
    return "\n".join(lines)


def javap(javap_bin, path):
    _, out, _ = run([javap_bin, "-p", "-c", path])
    return normalize(out)


def classes_in(root):
    """Mapa nombre_archivo.class -> ruta, recursivo (el javac real usa subdirs por paquete)."""
    out = {}
    for dp, _, fs in os.walk(root):
        for f in fs:
            if f.endswith(".class"):
                out[f] = os.path.join(dp, f)
    return out


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(2)
    javac_ref, javap_bin, corpus = sys.argv[1], sys.argv[2], sys.argv[3]
    extra_cp = []
    if "-cp" in sys.argv:
        extra_cp = ["-cp", sys.argv[sys.argv.index("-cp") + 1]]

    javas = sorted(
        os.path.join(dp, f)
        for dp, _, fs in os.walk(corpus)
        for f in fs
        if f.endswith(".java")
    )
    res = Counter()
    diffs = []
    for jf in javas:
        name = os.path.basename(jf)
        tmp = tempfile.mkdtemp()
        ours_dir = os.path.join(tmp, "ours")
        ref_dir = os.path.join(tmp, "ref")
        os.makedirs(ours_dir)
        os.makedirs(ref_dir)
        shutil.copy(jf, ours_dir)  # nuestro --emit escribe el .class al lado del fuente

        rc_r, _, err_r = run([javac_ref, "-d", ref_dir, jf])
        if rc_r != 0:
            res["REFERR"] += 1  # el corpus no compila ni con el real: problema del corpus, no nuestro
            diffs.append((name, "REFERR (el javac real no compila):\n" + err_r[:400]))
            shutil.rmtree(tmp, ignore_errors=True)
            continue
        rc_o, _, err_o = run([OUR, *extra_cp, "--emit", os.path.join(ours_dir, name)])
        ours = classes_in(ours_dir)
        refs = classes_in(ref_dir)
        if not ours:
            res["ERR"] += 1  # nuestro compilador no emitió nada
            diffs.append((name, "ERR (nuestro javac no emite):\n" + err_o[:400]))
            shutil.rmtree(tmp, ignore_errors=True)
            continue

        file_status = "OK"
        for cls in sorted(refs):
            if cls not in ours:
                file_status = "DIFF"
                diffs.append((f"{name} [{cls}]", "nuestro javac no produjo esta clase"))
                continue
            a = javap(javap_bin, refs[cls])
            b = javap(javap_bin, ours[cls])
            if a != b:
                file_status = "DIFF"
                ud = "\n".join(
                    difflib.unified_diff(a.split("\n"), b.split("\n"), "ref", "ours", lineterm="", n=1)
                )
                diffs.append((f"{name} [{cls}]", ud))
        # clases que emitimos de más (sintéticas que el real no genera igual) — informativo, no falla
        extra = sorted(set(ours) - set(refs))
        if extra:
            diffs.append((f"{name} (+extra)", "clases extra nuestras: " + ", ".join(extra)))
        res[file_status] += 1
        shutil.rmtree(tmp, ignore_errors=True)

    print("== resumen ==", dict(res), f"(de {len(javas)} archivos)")
    for title, body in diffs:
        print("=" * 70)
        print(title)
        print(body[:2000])


if __name__ == "__main__":
    main()
