"""Censo de la API publica de OpenJDK 25 contra KajiLibrary, en las dos dimensiones, a JSON.

    python tools/apidiff/censo.py docs/censo.json              # el objetivo: TODA la API publica
    python tools/apidiff/censo.py docs/censo.json --empezados   # solo lo ya arrancado

**El universo son los paquetes EXPORTADOS, no los empezados.** Hasta el 2026-09-04 este script
recorria `empezados` --los paquetes con al menos un `.class` nuestro-- con el argumento de que un
paquete sin empezar "no es un hueco de cobertura sino una decision de alcance". Eso hacia que el
denominador se moviera con el numerador: cada paquete nuevo se sumaba a los dos lados a la vez, y el
porcentaje media *que tan completo esta lo que tocamos*, no cuanto falta. Leia **91.4% de clases**
sobre 164 paquetes cuando el objetivo son **233**, y los 75 sin empezar --1335 clases-- eran
literalmente invisibles.

Exportado sin calificar es el criterio correcto de "API publica", y no una eleccion de gusto: un
paquete que su modulo no exporta (`sun.*`, `jdk.internal.*`, `com.sun.*` no exportados) **no tiene
contrato que cumplir** -- ningun programa puede importarlo sin `--add-exports`. Por eso el universo
sale de `java --describe-module`, y no de listar el jimage entero: eso daria 877 paquetes y 14681
clases, con `jdk.localedata` y `jdk.hotspot.agent` adentro.

**Solo cuenta lo publico.** El jimage lista todo lo que hay en el paquete, y la mitad de `java.lang`
son internos --`StringLatin1`, `CharacterData00`, `Shutdown`-- que no son API y que nadie tiene por
que reimplementar. Contarlos hacia que `java.lang` leyera 110/145 cuando lo que de verdad falta son
tres clases.

**Y cuenta los miembros de las clases que no estan.** Es la unica manera de que la dimension de
miembros no mienta: si el denominador saliera solo de las clases que tenemos, un paquete al que le
falta la mitad de sus tipos podria leer 100%. Una clase ausente entra con **cero** en el numerador y
con todos sus miembros en el denominador, que es exactamente lo que significa no tenerla.

El javap del JDK se corre **una vez por paquete** y no una por clase: con cuarenta y cinco paquetes y
casi dos mil clases, la diferencia es de minutos a segundos.
"""
import json, os, re, subprocess, sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
os.environ.setdefault("JDK_HOME", r"H:\jdk-25.0.2")
import apidiff as A  # noqa: E402

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "KajiLibrary")
RAIZ = os.path.normpath(RAIZ)

# Los de `Object`: los hereda todo el mundo y contarlos por clase inflaria el total sin decir nada.
OBJ = {"boolean equals(java.lang.Object)", "int hashCode()", "java.lang.String toString()",
       "java.lang.Object clone()", "java.lang.Class getClass()", "void finalize()",
       "void wait()", "void notify()", "void notifyAll()"}

DECL = re.compile(
    r"^(public\s+)?(?:final\s+|abstract\s+|static\s+|sealed\s+|non-sealed\s+)*"
    r"(?:class|interface|enum|record|@interface)\s+([\w.$]+)")


def supertipos(d):
    d = A.GENERIC.sub("", (d or "").split("{")[0])
    m = re.search(r"\b(?:extends|implements)\b(.*)", d)
    if not m:
        return []
    return [t.strip() for t in re.split(r",|\bextends\b|\bimplements\b", m.group(1)) if t.strip()]


def exportados():
    """Los paquetes que algun modulo del JDK exporta **sin calificar**.

    Un `exports x.y to z` no cuenta: es un contrato entre dos modulos del JDK, no API para nadie
    mas, y meterlo en el denominador seria comprometerse con algo que ni siquiera es alcanzable
    desde codigo de usuario.
    """
    java = os.path.join(os.environ["JDK_HOME"], "bin", "java.exe")
    mods = [l.split("@")[0].strip()
            for l in subprocess.run([java, "--list-modules"], capture_output=True,
                                    text=True).stdout.splitlines() if l.strip()]
    out = set()
    for m in mods:
        txt = subprocess.run([java, "--describe-module", m], capture_output=True, text=True).stdout
        for l in txt.splitlines():
            t = l.strip()
            if t.startswith("exports ") and " to " not in t:
                out.add(t.split()[1])
    return out


# ---- el lado del JDK: un javap por paquete ----------------------------------------------------------

def javap_paquete(pkg, nombres):
    """`{simple: (es_publica, miembros)}` para todo el paquete, de una sola llamada."""
    if not nombres:
        return {}
    try:
        txt = A.run([A.JAVAP, "-p"] + [pkg + "." + n for n in sorted(nombres)])
    except Exception:
        return {}
    # Las declaraciones de tipo van sin sangrar; los miembros, sangrados. Eso alcanza para partir.
    trozos, actual = [], []
    for l in txt.splitlines():
        if l and not l[0].isspace() and DECL.match(l):
            trozos.append(actual)
            actual = []
        actual.append(l)
    trozos.append(actual)

    salida = {}
    for t in trozos:
        cabeza = next((l for l in t if l and not l[0].isspace() and DECL.match(l)), None)
        if cabeza is None:
            continue
        m = DECL.match(cabeza)
        fq = m.group(2)
        if not fq.startswith(pkg + "."):
            continue
        simple = fq[len(pkg) + 1:]
        if "." in simple or "$" in simple:
            continue
        _, mem = A.parse("\n".join(t))
        salida[simple] = (bool(m.group(1)), mem)
    return salida


# ---- el lado nuestro: por clase, con cierre por herencia --------------------------------------------

cache = {}


def nuestra(fq):
    if fq in cache:
        return cache[fq]
    p = os.path.join(RAIZ, *fq.split(".")) + ".class"
    cache[fq] = (set(), [])
    if os.path.exists(p):
        try:
            d, mem = A.parse(A.run([A.OURS_TOOL, "--javap", "-p", p]))
            cache[fq] = (set(mem), supertipos(d))
        except Exception:
            pass
    return cache[fq]


def cierre(fq, visto=None):
    visto = visto if visto is not None else set()
    if fq in visto:
        return set()
    visto.add(fq)
    mem, sups = nuestra(fq)
    out = set(mem)
    for s in sups:
        out |= cierre(s, visto)
    return out


def main():
    destino = sys.argv[1]

    # Todo lo que el jimage lista, por paquete.
    jimage = os.path.join(os.environ["JDK_HOME"], "bin", "jimage.exe")
    modules = os.path.join(os.environ["JDK_HOME"], "lib", "modules")
    listado = subprocess.run([jimage, "list", modules], capture_output=True, text=True).stdout
    del_jdk = {}
    for l in listado.splitlines():
        l = l.strip()
        if not l.endswith(".class") or "$" in l or "/" not in l:
            continue
        pkg, _, simple = l[:-len(".class")].rpartition("/")
        del_jdk.setdefault(pkg.replace("/", "."), set()).add(simple)

    # El universo: los paquetes que algun modulo exporta sin calificar.
    universo = exportados()
    if "--empezados" in sys.argv:
        # El modo viejo, conservado para poder comparar contra `docs/censo.json` historicos.
        empezados = set()
        for dp, _, fs in os.walk(RAIZ):
            if not any(f.endswith(".class") and "$" not in f for f in fs):
                continue
            pkg = os.path.relpath(dp, RAIZ).replace("\\", ".").replace("/", ".")
            if pkg.startswith(("repros", "boot", ".")):
                continue
            empezados.add(pkg)
        universo &= empezados

    salida = []
    for pkg in sorted(universo):
        todas = del_jdk.get(pkg)
        if not todas:
            continue
        info = javap_paquete(pkg, todas)
        publicas = sorted(n for n, (es_pub, _) in info.items() if es_pub)
        if not publicas:
            continue

        hay = 0
        tb = tf = 0
        for simple in publicas:
            fq = pkg + "." + simple
            teoricos = [m for m in info[simple][1] if m.startswith(("public ", "protected "))]
            existe = os.path.exists(os.path.join(RAIZ, *fq.split(".")) + ".class")
            tf += len(teoricos)
            if not existe:
                # Cero en el numerador y todo en el denominador: eso es no tenerla.
                continue
            hay += 1
            nuestros = cierre(fq)
            firmas = {A.split_mods(x)[1] for x in nuestros}
            faltan = [m for m in teoricos
                      if m not in nuestros
                      and A.split_mods(m)[1] not in firmas
                      and A.split_mods(m)[1] not in OBJ]
            tb += len(teoricos) - len(faltan)

        if tf == 0:
            continue
        salida.append({"pkg": pkg, "clases": hay, "clasesJdk": len(publicas),
                       "miembros": tb, "miembrosJdk": tf})
        print("%-34s %3d/%-3d  %5d/%-5d" % (pkg, hay, len(publicas), tb, tf))

    json.dump(salida, open(destino, "w"), indent=1)
    tc = sum(x["clases"] for x in salida)
    tcj = sum(x["clasesJdk"] for x in salida)
    tm = sum(x["miembros"] for x in salida)
    tmj = sum(x["miembrosJdk"] for x in salida)
    print("TOTAL  clases %d/%d   miembros %d/%d  (%d paquetes)" % (tc, tcj, tm, tmj, len(salida)))


if __name__ == "__main__":
    main()
