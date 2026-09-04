"""Compara la superficie declarada de KajiLibrary contra la de OpenJDK (java.base).

Uso:
    python tools/apidiff/apidiff.py <reporte.json> [dir_biblioteca]

`dir_biblioteca` por defecto es `KajiLibrary/` del repo — pero lo normal es pasarle la salida
de `recompile.py`, porque los `.class` versionados son artefactos de **compiladores viejos** y
compararlos mide una mezcla de "que dice la fuente" y "que javac la compilo, y cuando".

Nuestro lado lo desensambla la herramienta congelada `bin/jvm.exe --javap -p`; el lado real, el
`javap -p` del JDK. Los dos emiten el mismo formato brief, asi que la comparacion es un diff de
conjuntos de miembros.

Normalizacion, para no ahogar la senal en ruido conocido:
  - se quita la clausula `throws ...` (el atributo `Exceptions` figura como pendiente en A0)
  - se quitan los argumentos genericos `<...>` (idem `Signature`)
  - se colapsa el espacio en blanco y se saca el `;` final
Lo que queda es la firma erasada, que es lo comparable hoy.

Ojo con la lectura del resultado: una firma que aparece a la vez en `missing` y en `extra` con
distintos modificadores **no es** un miembro faltante mas uno sobrante, es **un** miembro con
los modificadores mal. El reporte los separa por eso; ver `resumen()`.

Variables de entorno:
    KAJI_ROOT   raiz del repo (por defecto: dos niveles arriba de este script)
    JDK_HOME    JDK de referencia (por defecto: el que este en PATH)
"""
import json
import os
import re
import subprocess
import sys
from collections import Counter, defaultdict

ROOT = os.environ.get(
    "KAJI_ROOT", os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
)
OURS_TOOL = os.path.join(ROOT, "bin", "jvm.exe")
JDK_HOME = os.environ.get("JDK_HOME", "")
JAVAP = os.path.join(JDK_HOME, "bin", "javap.exe") if JDK_HOME else "javap"
JIMAGE = os.path.join(JDK_HOME, "bin", "jimage.exe") if JDK_HOME else "jimage"
MODULES = os.path.join(JDK_HOME, "lib", "modules") if JDK_HOME else None
BS = chr(92)

GENERIC = re.compile(r"<[^<>]*(?:<[^<>]*>[^<>]*)*>")
DECL = re.compile(r"^[\w. ]*\b(?:class|interface|enum|record)\s+([\w.$]+)")
MODS = frozenset("public protected private static final native synchronized abstract "
                 "transient volatile strictfp default".split())


def run(cmd):
    proc = subprocess.run(cmd, capture_output=True, encoding="utf-8", errors="replace")
    return (proc.stdout or "").replace("\r", "")


def _grupo_balanceado(s, inicio):
    """El `<...>` balanceado que empieza en `inicio`. Devuelve (contenido, indice_despues)."""
    if inicio >= len(s) or s[inicio] != "<":
        return None, inicio
    nivel, i = 0, inicio
    while i < len(s):
        if s[i] == "<":
            nivel += 1
        elif s[i] == ">":
            nivel -= 1
            if nivel == 0:
                return s[inicio + 1:i], i + 1
        i += 1
    return None, inicio


def _partir_en_comas(s):
    """Parte por las comas de **nivel cero**: `A, B<C, D>` son dos, no tres."""
    partes, nivel, actual = [], 0, []
    for c in s:
        if c == "<":
            nivel += 1
        elif c == ">":
            nivel -= 1
        if c == "," and nivel == 0:
            partes.append("".join(actual))
            actual = []
        else:
            actual.append(c)
    if actual:
        partes.append("".join(actual))
    return [p.strip() for p in partes if p.strip()]


def type_params(raw):
    """Los parametros de tipo declarados en `raw`, mapeados a su **borrado** (JLS 4.6).

    Una variable de tipo borra a su **primera cota**, o a `Object` si no tiene ninguna. Es lo que
    hace `javac` al emitir el descriptor, asi que es lo que hay que comparar: el JDK imprime
    `public default D toLocalDate()` y una implementacion correcta declara `ChronoLocalDate`.

    Sin esto, cada miembro cuyo tipo es una variable contaba como **faltante** aunque estuviera
    escrito bien. Es un error del medidor, no de la biblioteca -- y de los peores, porque empuja a
    "arreglar" codigo que no esta roto.
    """
    s = raw.strip()
    i = 0
    # Saltear los modificadores para encontrar un `<...>` que sea una **declaracion** de parametros
    # y no una lista de argumentos: `static <R> Foo<R> of(R)` declara `R`, pero
    # `public List<String> f()` no declara nada.
    while True:
        m = re.match(r"([\w$.]+)\s+", s[i:])
        if not m:
            break
        if m.group(1) not in MODS and not (s[i:].startswith("class ")
                                           or s[i:].startswith("interface ")
                                           or s[i:].startswith("enum ")
                                           or s[i:].startswith("record ")):
            break
        i += m.end()
    # En una declaracion de tipo, el `<...>` viene pegado al nombre: `class Foo<D extends X>`.
    if i < len(s) and s[i] != "<":
        m = re.match(r"[\w$.]+", s[i:])
        if m:
            i += m.end()
    contenido, _ = _grupo_balanceado(s, i)
    if contenido is None:
        return {}
    fuera = {}
    for parte in _partir_en_comas(contenido):
        m = re.match(r"([\w$]+)(?:\s+extends\s+(.+))?$", parte.strip())
        if not m:
            continue
        nombre = m.group(1)
        cota = m.group(2)
        if cota:
            # `T extends A & B` borra a `A`, la primera.
            cota = cota.split("&")[0].strip()
            cota = GENERIC.sub("", cota).strip()
        fuera[nombre] = cota or "java.lang.Object"
    return fuera


def _sustituir(s, tvars):
    """Reemplaza cada variable de tipo por su borrado, como palabra entera."""
    if not tvars:
        return s
    for nombre in sorted(tvars, key=len, reverse=True):
        s = re.sub(r"\b" + re.escape(nombre) + r"\b", tvars[nombre], s)
    return s


def normalize(line, tvars=None):
    s = line.strip()
    if not s or s == "}":
        return None
    s = s.split(" throws ")[0]
    # Los parametros del **propio** miembro se suman a los de la clase: un
    # `static <R extends X> Foo<R> of(R)` declara su `R` y hay que borrarlo igual.
    propios = type_params(s)
    prev = None
    while prev != s:                       # genericos anidados
        prev = s
        s = GENERIC.sub("", s)
    s = re.sub(r"\s+", " ", s.rstrip(";").strip())
    if not s:
        return None
    todos = dict(tvars or {})
    todos.update(propios)
    return _sustituir(s, todos) or None


def parse(text):
    """Primera linea util = declaracion de la clase; el resto, miembros.

    Los parametros de tipo **de la clase** se leen de esa primera linea y se aplican a todos los
    miembros: en `javap` un miembro los nombra pelados (`D toLocalDate()`) y solo la declaracion
    dice a que borran.
    """
    decl, members = None, set()
    tvars = {}
    for line in text.splitlines():
        if line.startswith("Compiled from"):
            continue
        crudo = line.strip()
        if decl is None and DECL.match(normalize(line) or ""):
            tvars = type_params(crudo)
            decl = _sustituir(normalize(line), tvars).rstrip("{").strip()
            continue
        n = normalize(line, tvars)
        if n is None or n == "{":
            continue
        members.add(n.rstrip("{").strip())
    return decl, members


def split_mods(member):
    tokens = member.split()
    i = 0
    while i < len(tokens) and tokens[i] in MODS:
        i += 1
    return frozenset(tokens[:i]), " ".join(tokens[i:])


def java_base_index():
    """Nombres FQCN top-level de java.base, leidos de la imagen del JDK de referencia."""
    if MODULES and os.path.exists(MODULES):
        out = run([JIMAGE, "list", MODULES])
    else:
        out = run([JIMAGE, "list", os.path.join(os.path.dirname(JAVAP), "..", "lib", "modules")])
    index, module = set(), None
    for line in out.splitlines():
        s = line.strip()
        if s.startswith("Module: "):
            module = s[8:]
            continue
        if module == "java.base" and s.endswith(".class") and "$" not in s:
            index.add(s[:-6].replace("/", "."))
    if not index:
        sys.exit("no se pudo leer java.base — pasa JDK_HOME apuntando al JDK de referencia")
    return index


def split_real(out):
    chunks, cur, key = {}, [], None
    for line in out.split("\n"):
        m = DECL.match(line)
        if m:
            if key:
                chunks[key] = "\n".join(cur)
            key, cur = GENERIC.sub("", m.group(1)), [line]
            continue
        if key:
            cur.append(line)
    if key:
        chunks[key] = "\n".join(cur)
    return chunks


def resumen(rows):
    ok = [r for r in rows if r["status"] == "ok"]
    real = sum(r["n_real"] for r in ok)
    missing = sum(len(r["missing"]) for r in ok)
    pub_missing = sum(1 for r in ok for m in r["missing"]
                      if m.startswith(("public ", "protected ")))
    kinds, varargs, foreign = Counter(), 0, []
    for r in ok:
        ours = {sig: mods for mods, sig in map(split_mods, r["extra"])}
        theirs = {sig: mods for mods, sig in map(split_mods, r["missing"])}
        for sig in set(ours) & set(theirs):
            kinds[(tuple(sorted(ours[sig] - theirs[sig])),
                   tuple(sorted(theirs[sig] - ours[sig])))] += 1
        for m in r["missing"]:
            if "..." in m and m.replace("...", "[]") in set(r["extra"]):
                varargs += 1
        for sig in set(ours) - set(theirs):
            if ours[sig] & {"public", "protected"}:
                foreign.append((r["class"], sig))
    print(f"clases comparadas: {len(ok)}")
    print(f"declaracion de clase coincide: {sum(1 for r in ok if r['decl_match'])}/{len(ok)}")
    # El titular es el CONTRATO (public + protected): los privados de HotSpot no son API y por
    # regla del proyecto nunca se implementan, asi que contarlos en el denominador subestima.
    have = real - missing
    contract_total = have + pub_missing
    print(f"CONTRATO (public+protected): {have}/{contract_total}   faltan: {pub_missing}   "
          f"cobertura: {100 * have / max(contract_total, 1):.1f}%")
    print(f"  (referencia, contando tambien privados de HotSpot: {have}/{real} = "
          f"{100 * have / real:.1f}% — denominador inflado, no usar como titular)")
    print(f"firmas identicas salvo ACC_VARARGS: {varargs}")
    print(f"superficie publica ajena al JDK: {len(foreign)}")
    print("modificadores divergentes (+nuestro / -del JDK):")
    for (added, removed), n in kinds.most_common(12):
        a = "+" + ",".join(added) if added else ""
        rem = "-" + ",".join(removed) if removed else ""
        print(f"  {n:>5}  {a:<22}{rem}")


def main(report_path, lib_dir):
    real_index = java_base_index()
    targets = []
    for dirpath, _, filenames in os.walk(lib_dir):
        if "repros" in dirpath.replace(BS, "/").split("/"):
            continue
        for name in filenames:
            if name.endswith(".class") and "$" not in name:
                path = os.path.join(dirpath, name)
                fq = os.path.relpath(path, lib_dir).replace(BS, "/")[:-6].replace("/", ".")
                if fq in real_index:
                    targets.append((fq, path))
    targets.sort()
    print(f"clases nuestras que existen en java.base: {len(targets)}", flush=True)

    real_out, batch_size = {}, 80
    names = [n for n, _ in targets]
    for i in range(0, len(names), batch_size):
        real_out.update(split_real(run([JAVAP, "-p"] + names[i:i + batch_size])))
        print(f"  javap de referencia: {min(i + batch_size, len(names))}/{len(names)}", flush=True)

    rows = []
    for i, (name, path) in enumerate(targets):
        our_decl, our_members = parse(run([OURS_TOOL, "--javap", "-p", path]))
        reference = real_out.get(name)
        if reference is None:
            rows.append({"class": name, "status": "SIN_REFERENCIA"})
            continue
        real_decl, real_members = parse(reference)
        rows.append({
            "class": name, "status": "ok",
            "decl_ours": our_decl, "decl_real": real_decl,
            "decl_match": our_decl == real_decl,
            "n_ours": len(our_members), "n_real": len(real_members),
            "missing": sorted(real_members - our_members),
            "extra": sorted(our_members - real_members),
        })
        if (i + 1) % 100 == 0:
            print(f"  nuestro javap: {i + 1}/{len(targets)}", flush=True)

    with open(report_path, "w", encoding="utf-8") as fh:
        json.dump(rows, fh, ensure_ascii=False, indent=1)
    print("escrito", report_path)
    print()
    resumen(rows)
    return 0


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    default_lib = os.path.join(ROOT, "KajiLibrary")
    sys.exit(main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else default_lib))
