"""Mide un paquete (o clases sueltas) por **cierre de herencia** contra `javap` del JDK 25.

    python tools/apidiff/medir.py java.util.stream          # el paquete entero
    python tools/apidiff/medir.py java.util.stream.Stream   # una clase
    DET=1 python tools/apidiff/medir.py java.text           # con el detalle de lo que falta

Que mide: los miembros `public`/`protected` que el JDK declara para la clase **y toda su cadena de
supertipos**, comparados por firma con los genericos borrados y las variables de tipo reducidas a su
cota. Los heredados de `Object` quedan afuera.

Que **no** mide: que el miembro haga lo correcto. Para eso estan las pruebas de comportamiento de
`java/*Test.java`, que se corren con las dos VMs y se comparan.

De donde sale la lista de clases, que es lo que hace honesto al numero: de **el JDK**, no de nuestro
arbol. Antes se recorrian nuestras clases, y una que no existiera era invisible -- sus miembros no
entraban ni al numerador ni al **denominador**, asi que un paquete al que le faltaran tipos enteros
podia figurar al 100 %. Ahora una clase ausente cuenta todos sus miembros como faltantes, que es lo
que son.

Se cuentan solo las clases **publicas** del JDK: con una de paquete no hay contrato que cumplir. Y
solo las de primer nivel; los miembros de las anidadas entran por el cierre de herencia de quien las
usa, no como clases sueltas.

`tools/apidiff/clases.py` sigue siendo util y mide otra cosa: **cuantas** clases faltan, no cuantos
miembros. Un paquete al que le falta una clase enorme y otro al que le faltan cinco chicas se ven
distinto en una medida y parecido en la otra.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ.setdefault("JDK_HOME", r"H:\jdk-25.0.2")
import apidiff as A

RAIZ = "KajiLibrary"

OBJETO = {
    "boolean equals(java.lang.Object)", "int hashCode()", "java.lang.String toString()",
    "java.lang.Object clone()", "java.lang.Class getClass()", "void finalize()",
    "void wait()", "void notify()", "void notifyAll()",
}


def supertipos(decl):
    d = A.GENERIC.sub("", (decl or "").split("{")[0])
    m = re.search(r"\b(?:extends|implements)\b(.*)", d)
    if not m:
        return []
    return [t.strip() for t in re.split(r",|\bextends\b|\bimplements\b", m.group(1)) if t.strip()]


_cache = {}


def _info(fq):
    if fq in _cache:
        return _cache[fq]
    p = os.path.join(RAIZ, *fq.split(".")) + ".class"
    _cache[fq] = (set(), [])
    if os.path.exists(p):
        decl, miembros = A.parse(A.run([A.OURS_TOOL, "--javap", "-p", p]))
        _cache[fq] = (miembros, supertipos(decl))
    return _cache[fq]


def cierre(fq, visto=None):
    visto = visto if visto is not None else set()
    if fq in visto:
        return set()
    visto.add(fq)
    miembros, sups = _info(fq)
    out = set(miembros)
    for s in sups:
        out |= cierre(s, visto)
    return out


_indice = None


def indice_del_jdk():
    """Los FQCN de primer nivel de **toda** la imagen del JDK, no solo de `java.base`.

    Todos los modulos y no solo `java.base` porque los `javax.*` viven en `java.compiler` y en
    `java.sql`: con el indice de `java.base` a secas quedaban afuera, o sea invisibles, que es el
    error que esta medicion vino a arreglar.
    """
    global _indice
    if _indice is None:
        _indice = set()
        for linea in A.run([A.JIMAGE, "list", A.MODULES]).splitlines():
            t = linea.strip()
            if t.endswith(".class") and "$" not in t and not t.startswith("Module:"):
                _indice.add(t[: -len(".class")].replace("/", "."))
    return _indice


def clases_de(objetivo):
    """Si `objetivo` es un paquete, **las clases que el JDK tiene** ahi; si no, el mismo.

    Del JDK y no del arbol: es lo que hace que una clase que no escribimos cuente sus miembros como
    faltantes en vez de desaparecer del denominador.
    """
    delJdk = sorted(c for c in indice_del_jdk() if c.rsplit(".", 1)[0] == objetivo)
    if not delJdk:
        # No es un paquete del JDK: o es una clase suelta, o un paquete nuestro sin contraparte.
        return [objetivo]
    return delJdk


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    detalle = bool(os.environ.get("DET"))
    tengo = total = 0
    for objetivo in sys.argv[1:]:
        for fq in clases_de(objetivo):
            try:
                decl, teoricos = A.parse(A.run([A.JAVAP, "-p", fq]))
            except Exception:
                continue
            # Solo las publicas: con una clase de paquete del JDK no hay contrato que cumplir.
            if not (decl or "").startswith("public "):
                continue
            nuestros = cierre(fq)
            podados = {A.split_mods(x)[1] for x in nuestros}
            # Los heredados de `Object` quedan afuera del **denominador**, no solo de la
            # comparacion. Estaban afuera de `faltan` pero adentro de `todos`, o sea que se contaban
            # como escritos: una clase que no existe en el arbol figuraba 1/31 en vez de 0/31. Un
            # miembro que no se compara tampoco se cuenta.
            todos = [m for m in teoricos
                     if m.startswith(("public ", "protected "))
                     and A.split_mods(m)[1] not in OBJETO]
            if not todos:
                continue
            faltan = [m for m in todos
                      if m not in nuestros
                      and A.split_mods(m)[1] not in podados
                      and A.split_mods(m)[1] not in OBJETO]
            tengo += len(todos) - len(faltan)
            total += len(todos)
            if detalle or faltan:
                # La marca distingue "esta escrita y le faltan miembros" de "no existe en el arbol",
                # que son dos situaciones muy distintas y el cociente solo no las separa.
                ausente = "  (no esta)" if not nuestros else ""
                print("%-34s %4d/%-4d%s"
                      % (fq.rsplit(".", 1)[-1], len(todos) - len(faltan), len(todos), ausente))
            if detalle:
                for m in sorted(faltan):
                    print("      ", m)
    print("TOTAL %d/%d" % (tengo, total))


if __name__ == "__main__":
    main()
