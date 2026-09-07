# -*- coding: utf-8 -*-
"""Que clases nuestras deberian implementar una interfaz y no la implementan.

    python tools/apidiff/interfaces.py java.lang java.util
    python tools/apidiff/interfaces.py --iface java.lang.Cloneable java.util
    python tools/apidiff/interfaces.py --arreglar java.util

**El censo no puede ver esto.** Una clausula `implements` no es un miembro, asi que una clase puede
tener sus doscientos metodos completos, contar como COMPLETO, y no ser serializable. Y sin embargo
se nota, y de la peor manera: `"hola" instanceof Serializable` dando falso no rompe nada en el acto
--hace que cualquier cosa que filtre por serializabilidad tire lo que no tenia que tirar, en
silencio--.

Asi aparecio, el 2026-09-05: `RMIConnectorServer.getAttributes()` descarta lo que no puede viajar y
descartaba todo. El error no estaba ahi sino en `java.lang.String`, que no declaraba `Serializable`.
Tampoco lo declaraban `Throwable` --y con el todas las excepciones--, `Class`, ni las colecciones:
102 de 226 clases de `java.lang` y `java.util`.

**Se comparan los archivos compilados, no la reflexion.** Los nombres chocan: una JVM real cargando
nuestro `java.lang.String` carga el suyo. Y se compara la **clausura** de supertipos, porque casi
todas heredan la interfaz en vez de declararla.

Con `--arreglar` edita el fuente, y solo donde el JDK la declara en su propia clausula: las que la
heredan se arreglan al arreglar el padre, y ponersela igual seria escribir una clausula redundante
que despues nadie sabe si esta por algo.
"""
import io
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ.setdefault("JDK_HOME", r"H:\jdk-25.0.2")

JDK = os.environ["JDK_HOME"]
JAVAP = os.path.join(JDK, "bin", "javap.exe")
JIMAGE = os.path.join(JDK, "bin", "jimage.exe")
MODULES = os.path.join(JDK, "lib", "modules")
RAIZ = "KajiLibrary"

DECL = re.compile(
    r"^(?:public\s+)?(?:final\s+|abstract\s+|static\s+|sealed\s+|non-sealed\s+)*"
    r"(?:class|interface|enum|record)\s+([\w.$]+)")


def corrida(cmd):
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return p.stdout.decode("utf-8", "replace")


def sin_genericos(l):
    """La linea sin los `<...>`, contando anidamiento.

    Hace falta antes de buscar `extends`: en `class RegularEnumSet<E extends Enum> extends EnumSet`
    hay dos, y el primero es el del parametro de tipo. Buscar sin sacarlos da `Enum` como
    superclase, que es justo la respuesta equivocada y ademas creible.
    """
    out, prof = [], 0
    for c in l:
        if c == "<":
            prof += 1
        elif c == ">":
            prof -= 1
        elif prof == 0:
            out.append(c)
    return "".join(out)


def declaracion(texto, fq):
    """Los supertipos de `fq` segun la salida de javap, o None si no aparece."""
    for l in texto.split("\n"):
        m = DECL.match(l.strip())
        if m and m.group(1) == fq:
            plana = sin_genericos(l)
            sups = []
            me = re.search(r"\bextends\s+([\w.$, ]+?)(?:\s+implements\b|\s*\{|$)", plana)
            if me:
                sups += [x.strip() for x in me.group(1).split(",")]
            mi = re.search(r"\bimplements\s+([\w.$, ]+?)(?:\s*\{|$)", plana)
            if mi:
                sups += [x.strip() for x in mi.group(1).split(",")]
            return [x for x in sups if x]
    return None


CACHE = {}


def supers_jdk(fq):
    if ("jdk", fq) not in CACHE:
        CACHE[("jdk", fq)] = declaracion(corrida([JAVAP, fq]), fq)
    return CACHE[("jdk", fq)]


def supers_nuestros(fq):
    if ("nue", fq) not in CACHE:
        ruta = os.path.join(RAIZ, *fq.split(".")) + ".class"
        CACHE[("nue", fq)] = declaracion(corrida([JAVAP, ruta]), fq) \
            if os.path.exists(ruta) else None
    return CACHE[("nue", fq)]


def la_tiene(fq, iface, supers, visto=None):
    if visto is None:
        visto = set()
    if fq in visto:
        return False
    visto.add(fq)
    s = supers(fq)
    if s is None:
        return False
    for x in s:
        if x == iface or la_tiene(x, iface, supers, visto):
            return True
    return False


INDICE = {}


def indice():
    """Las clases del jimage, por paquete.

    Se arma una sola vez: `jimage list` recorre el archivo entero de modulos y tarda varios
    segundos. Llamarlo una vez por paquete --que es como estaba-- hacia que escanear seis paquetes
    costara seis recorridas completas para leer siempre lo mismo.
    """
    if not INDICE:
        for l in corrida([JIMAGE, "list", MODULES]).splitlines():
            s = l.strip()
            if s.endswith(".class") and "$" not in s and "/" in s:
                p, _, simple = s[:-6].rpartition("/")
                INDICE.setdefault(p.replace("/", "."), []).append(simple)
    return INDICE


def clases_de(pkg):
    return sorted(pkg + "." + s for s in indice().get(pkg, ()))


def directo(fq, iface):
    """Cierto si el JDK declara la interfaz en la propia clausula de `fq`."""
    l = next((x for x in corrida([JAVAP, fq]).split("\n")
              if DECL.match(x.strip()) and DECL.match(x.strip()).group(1) == fq), "")
    m = re.search(r"\bimplements\s+(.*?)\s*\{", sin_genericos(l))
    return bool(m) and any(x.strip() == iface for x in m.group(1).split(","))


def parchear(fq, iface):
    ruta = os.path.join(RAIZ, *fq.split(".")) + ".java"
    if not os.path.exists(ruta):
        return "no esta el fuente"
    s = io.open(ruta, encoding="utf-8", newline="").read().replace("\r\n", "\n")
    simple_iface = iface.split(".")[-1]
    simple = fq.split(".")[-1]
    m = re.search(r"^((?:public |final |abstract |sealed |non-sealed )*"
                  r"(?:class|interface|enum)\s+" + re.escape(simple) + r"\b[^{]*?)\s*\{",
                  s, re.M | re.S)
    if m is None:
        return "no se encontro la declaracion"
    decl = m.group(1)
    if re.search(r"\b%s\b" % re.escape(simple_iface), decl):
        return "ya la tiene"
    cola = ", " + simple_iface if re.search(r"\bimplements\b", decl) \
        else " implements " + simple_iface
    s = s[:m.start(1)] + decl.rstrip() + cola + s[m.end(1):]
    if not re.search(r"^import %s;$" % re.escape(iface), s, re.M):
        if "\nimport " in s:
            i = s.index("\nimport ")
            s = s[:i + 1] + "import %s;\n" % iface + s[i + 1:]
        else:
            i = s.index("\n", s.index("package ")) + 1
            s = s[:i] + "\nimport %s;\n" % iface + s[i:]
    io.open(ruta, "w", encoding="utf-8", newline="\n").write(s)
    return "parcheada"


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    iface = "java.io.Serializable"
    if "--iface" in sys.argv:
        iface = args.pop(0)
    arreglar = "--arreglar" in sys.argv

    total = mal = 0
    for pkg in args:
        faltan = []
        for fq in clases_de(pkg):
            if supers_nuestros(fq) is None:
                continue
            total += 1
            if la_tiene(fq, iface, supers_jdk) and not la_tiene(fq, iface, supers_nuestros):
                faltan.append(fq)
                mal += 1
        if faltan:
            print("%s: %d sin %s" % (pkg, len(faltan), iface))
            for f in faltan:
                if arreglar and directo(f, iface):
                    print("    %-40s %s" % (f, parchear(f, iface)))
                elif arreglar:
                    print("    %-40s la hereda; se arregla al arreglar el padre" % f)
                else:
                    print("    " + f)
    print("revisadas %d, les falta a %d" % (total, mal))


main()
