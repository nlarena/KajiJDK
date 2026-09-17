"""Campaña diferencial sobre java.lang.String: OpenJDK contra KajiJDK + KajiLibrary.

La misma mecánica que bigfuzz.py, con cadenas de caracteres en vez de números:
  generar  -> expresiones de String sobre cadenas armadas con caracteres sesgados a los bordes
  ejecutar -> un .class por lote, compilado UNA vez con el javac de referencia, corrido por los dos lados
  oráculo  -> igualdad del texto de cada resultado (o de la clase de la excepción)
  reducir  -> achicar cada divergencia mientras siga divergiendo del mismo modo

Dos decisiones hacen que la comparación mida la biblioteca y no la consola:
  - las cadenas entran al programa como códigos numéricos, `s(65, 0xD800)`, así el código fuente es
    ASCII puro y no depende de cómo javac lea un carácter raro;
  - las cadenas salen como la lista de sus códigos, `[65,55296]`, así la codificación de la salida de
    cada lado no puede inventar diferencias.

    python test-fuzzing/fuzzer/strfuzz.py --jdk "$OPENJDK" --seed 1 --lotes 6 --por-lote 120 --reducir 8
"""
import argparse
import datetime
import json
import random
import re
import subprocess
import time
from pathlib import Path

REPO = Path(r"C:\Users\nicol\Sources\Larena\KajiJVM\KajiJDK\repo")
HEADLESS = REPO / "target" / "release" / "run-headless.exe"
WORK = Path(__file__).resolve().parent / "runs"
JAVAC = JAVA = None

# --------------------------------------------------------------------------------------------
# generar
# --------------------------------------------------------------------------------------------
# Caracteres donde viven los errores de una implementación de String. Algunos son dos `char`
# (un par de surrogates), por eso cada entrada es una tupla.
BORDES = [
    (0x0000,), (0x0009,), (0x000A,), (0x0020,), (0x00A0,), (0x2003,), (0x3000,), (0xFEFF,),
    (0x61,), (0x41,), (0x7A,), (0x5A,), (0x30,),
    (0x00DF,),            # ß: en mayúscula son dos letras, SS
    (0x1E9E,),            # ẞ: la ß mayúscula
    (0x0130,), (0x0131,), # İ e ı turcas
    (0x00E9,), (0x0301,), # é precompuesta y la tilde combinable
    (0x03A3,), (0x03C3,), (0x03C2,),  # Σ, σ y la sigma final ς
    (0x01C5,),            # Dž, una letra "título"
    (0x2126,),            # Ω, el signo ohm
    (0xFB03,),            # ﬃ, ligadura que en mayúscula son tres letras
    (0xD800,), (0xDC00,), (0xFFFF,),  # surrogates sueltos y el último char
    (0xD83D, 0xDE00),     # 😀, fuera del plano básico
    (0xD801, 0xDC00),     # 𐐀, mayúscula de Deseret, fuera del plano básico
    (0xD801, 0xDC28),     # 𐐨, su minúscula
]
ENTEROS = [-1, 0, 1, 2, 3, 5, 8, 100]
CODEPOINTS = [0x61, 0x41, 0x1F600, 0xD83D, 0xDE00, 0x0130, 0x0131, 0xDF, 0, 0x110000, -1]
CHARS = [0x61, 0x41, 0xD800, 0xDF, 0x0301, 0x20]

UN = ["upper", "lower", "trim", "strip", "stripLeading", "stripTrailing"]
VISTAS = (["enc"] * 8) + ["length", "isEmpty", "isBlank", "charAt", "codePointAt", "codePointCount",
                           "indexOf", "indexOfFrom", "lastIndexOf", "indexOfChar", "contains",
                           "startsWith", "endsWith", "equals", "equalsIgnoreCase", "compareTo",
                           "compareToIgnoreCase", "hashCode", "utf8", "utf8RoundTrip", "latin1"]
CON_OTRA = {"indexOf", "indexOfFrom", "lastIndexOf", "contains", "startsWith", "endsWith", "equals",
            "equalsIgnoreCase", "compareTo", "compareToIgnoreCase"}


def rand_lit(r):
    n = r.randint(0, 8) if r.random() < 0.85 else r.randint(9, 12)
    cs = []
    while len(cs) < n:
        x = r.random()
        if x < 0.45:
            cs.extend(r.choice(BORDES))
        elif x < 0.80:
            cs.append(r.randint(0x20, 0x7E))
        elif x < 0.92:
            cs.append(r.randint(0, 0xFFFF))
        else:
            cp = r.randint(0x10000, 0x10FFFF) - 0x10000
            cs.extend([0xD800 + (cp >> 10), 0xDC00 + (cp & 0x3FF)])
    return ("lit", tuple(cs))


def gen(r, d):
    if d == 0 or r.random() < 0.35:
        return rand_lit(r)
    x = r.random()
    if x < 0.35:
        return ("u", r.choice(UN), gen(r, d - 1))
    if x < 0.55:
        return ("b", "concat", gen(r, d - 1), gen(r, d - 1))
    if x < 0.65:
        return ("rep", "replaceSeq", gen(r, d - 1), gen(r, d - 1), gen(r, d - 1))
    if x < 0.75:
        return ("i", r.choice(["substring1", "repeat"]), gen(r, d - 1), r.choice(ENTEROS))
    if x < 0.88:
        return ("ii", "substring2", gen(r, d - 1), r.choice(ENTEROS), r.choice(ENTEROS))
    return ("cc", "replaceChar", gen(r, d - 1), r.choice(CHARS), r.choice(CHARS))


def gen_view(r):
    kind = r.choice(VISTAS)
    node = gen(r, 3)
    arg = None
    if kind in CON_OTRA:
        arg = gen(r, 2)
    if kind in ("charAt", "codePointAt"):
        arg = r.choice(ENTEROS)
    if kind == "indexOfChar":
        arg = r.choice(CODEPOINTS)
    if kind == "indexOfFrom":
        arg = (arg, r.choice(ENTEROS))
    return ("view", kind, node, arg)


def render(n):
    t = n[0]
    if t == "lit":
        return "s(" + ", ".join(str(c) for c in n[1]) + ")"
    if t == "u":
        x = render(n[2])
        return {"upper": f"{x}.toUpperCase(Locale.ROOT)", "lower": f"{x}.toLowerCase(Locale.ROOT)"}.get(n[1], f"{x}.{n[1]}()")
    if t == "b":
        return f"{render(n[2])}.concat({render(n[3])})"
    if t == "rep":
        return f"{render(n[2])}.replace({render(n[3])}, {render(n[4])})"
    if t == "i":
        return f"{render(n[2])}.{'substring' if n[1] == 'substring1' else 'repeat'}({n[3]})"
    if t == "ii":
        return f"{render(n[2])}.substring({n[3]}, {n[4]})"
    return f"{render(n[2])}.replace((char) {n[3]}, (char) {n[4]})"


def render_view(v):
    _, kind, node, arg = v
    x = render(node)
    val = lambda e: f"String.valueOf({e})"
    utf8 = "java.nio.charset.StandardCharsets.UTF_8"
    return {
        "enc": lambda: f"enc({x})",
        "length": lambda: val(f"{x}.length()"),
        "isEmpty": lambda: val(f"{x}.isEmpty()"),
        "isBlank": lambda: val(f"{x}.isBlank()"),
        "charAt": lambda: val(f"(int) {x}.charAt({arg})"),
        "codePointAt": lambda: val(f"{x}.codePointAt({arg})"),
        "codePointCount": lambda: val(f"{x}.codePointCount(0, {x}.length())"),
        "indexOf": lambda: val(f"{x}.indexOf({render(arg)})"),
        "indexOfFrom": lambda: val(f"{x}.indexOf({render(arg[0])}, {arg[1]})"),
        "lastIndexOf": lambda: val(f"{x}.lastIndexOf({render(arg)})"),
        "indexOfChar": lambda: val(f"{x}.indexOf({arg})"),
        "contains": lambda: val(f"{x}.contains({render(arg)})"),
        "startsWith": lambda: val(f"{x}.startsWith({render(arg)})"),
        "endsWith": lambda: val(f"{x}.endsWith({render(arg)})"),
        "equals": lambda: val(f"{x}.equals({render(arg)})"),
        "equalsIgnoreCase": lambda: val(f"{x}.equalsIgnoreCase({render(arg)})"),
        "compareTo": lambda: val(f"{x}.compareTo({render(arg)})"),
        "compareToIgnoreCase": lambda: val(f"{x}.compareToIgnoreCase({render(arg)})"),
        "hashCode": lambda: val(f"{x}.hashCode()"),
        "utf8": lambda: f"java.util.Arrays.toString({x}.getBytes({utf8}))",
        "utf8RoundTrip": lambda: f"enc(new String({x}.getBytes({utf8}), {utf8}))",
        "latin1": lambda: f"java.util.Arrays.toString({x}.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))",
    }[kind]()


# --------------------------------------------------------------------------------------------
# ejecutar
# --------------------------------------------------------------------------------------------
def program(name, views):
    lines = [
        "import java.util.Locale;",
        f"public class {name} {{",
        # Una cadena sale como la lista de sus códigos: la salida es ASCII puro en los dos lados.
        '  static String enc(String s) { if (s == null) return "null"; StringBuilder b = new StringBuilder("["); '
        "for (int i = 0; i < s.length(); i++) { if (i > 0) b.append(','); b.append((int) s.charAt(i)); } "
        "return b.append(']').toString(); }",
        # Una cadena entra como sus códigos: el código fuente es ASCII puro.
        "  static String s(int... cs) { char[] a = new char[cs.length]; "
        "for (int i = 0; i < cs.length; i++) a[i] = (char) cs[i]; return new String(a); }",
    ]
    for k, v in enumerate(views):
        lines.append(
            f"  static String e{k}() {{ try {{ return {render_view(v)}; }} "
            f'catch (Throwable t) {{ return "EXC:" + t.getClass().getName(); }} }}'
        )
    lines.append("  static int run() {")
    for k in range(len(views)):
        lines.append(f'    System.out.println("{k}=" + e{k}());')
    lines.append(f"    return {len(views)};")
    lines.append("  }")
    lines.append("  public static void main(String[] a) { System.out.println(run()); }")
    lines.append("}")
    return "\n".join(lines) + "\n"


LINE = re.compile(r"^(\d+)=(.*)$")


def parse(out):
    got = {}
    for ln in out.splitlines():
        m = LINE.match(ln.rstrip("\r"))
        if m:
            got[int(m.group(1))] = m.group(2)
    return got


def run_both(name, views):
    d = WORK / name
    d.mkdir(parents=True, exist_ok=True)
    src = d / f"{name}.java"
    src.write_text(program(name, views), encoding="utf-8")
    c = subprocess.run([str(JAVAC), "-d", str(d), str(src)], capture_output=True, text=True,
                       encoding="utf-8", errors="replace", timeout=300)
    if c.returncode != 0:
        raise RuntimeError("javac rechazó el programa generado:\n" + c.stderr[-2000:])
    t0 = time.time()
    jdk = subprocess.run([str(JAVA), "-cp", str(d), name], capture_output=True, text=True,
                         encoding="utf-8", errors="replace", timeout=300)
    t1 = time.time()
    try:
        ours = subprocess.run([str(HEADLESS), str(d / f"{name}.class"), "run"], cwd=str(REPO),
                              capture_output=True, text=True, encoding="utf-8", errors="replace",
                              timeout=600)
        ours_out, ours_code, ours_err = ours.stdout, ours.returncode, ours.stderr
    except subprocess.TimeoutExpired as e:
        dec = lambda b: b.decode("utf-8", "replace") if isinstance(b, bytes) else (b or "")
        ours_out, ours_code, ours_err = dec(e.stdout), "timeout", dec(e.stderr)
    t2 = time.time()
    # KajiJDK escribe `System.out` en stderr, el JDK de referencia en stdout: se leen los dos flujos.
    err_tail = "\n".join(ln for ln in ours_err.splitlines()
                         if not ln.startswith("[gc]") and not LINE.match(ln))[-600:]
    return parse(jdk.stdout), parse(ours_out + "\n" + ours_err), {
        "jdk_s": t1 - t0, "ours_s": t2 - t1, "ours_exit": ours_code, "ours_err": err_tail}


# --------------------------------------------------------------------------------------------
# oráculo
# --------------------------------------------------------------------------------------------
MISSING = {"java.lang.NoSuchMethodError", "java.lang.AbstractMethodError",
           "java.lang.UnsatisfiedLinkError", "java.lang.NoClassDefFoundError"}


def classify(jdk, ours):
    if jdk == ours:
        return None
    if ours is None:
        return "sin salida (crash o cuelgue)"
    if ours.startswith("EXC:") and ours[4:] in MISSING:
        return "falta en la biblioteca"
    if jdk.startswith("EXC:") and ours.startswith("EXC:"):
        return "otra excepción"
    if jdk.startswith("EXC:") != ours.startswith("EXC:"):
        return "excepción contra valor"
    return "otro valor"


# --------------------------------------------------------------------------------------------
# reducir
# --------------------------------------------------------------------------------------------
def kids(n):
    if n[0] == "lit":
        return []
    if n[0] == "b":
        return [n[2], n[3]]
    if n[0] == "rep":
        return [n[2], n[3], n[4]]
    return [n[2]]


def paths(n, p=()):
    yield p
    for i, ch in enumerate(kids(n)):
        yield from paths(ch, p + (i,))


def get(n, p):
    for i in p:
        n = kids(n)[i]
    return n


def put(n, p, new):
    if not p:
        return new
    lst = list(n)
    lst[2 + p[0]] = put(kids(n)[p[0]], p[1:], new)
    return tuple(lst)


def shrink_lit(cs):
    out = []
    if cs:
        out.append(())
        out.append(cs[: len(cs) // 2])
        out.append(cs[len(cs) // 2:])
    for i in range(len(cs)):
        out.append(cs[:i] + cs[i + 1:])
        if cs[i] != 0x61:
            out.append(cs[:i] + (0x61,) + cs[i + 1:])
    return out


def shrink_node(n):
    out = []
    for p in paths(n):
        sub = get(n, p)
        for ch in kids(sub):
            out.append(put(n, p, ch))
        t = sub[0]
        if t == "lit":
            out += [put(n, p, ("lit", cs)) for cs in shrink_lit(sub[1])]
        elif t == "i":
            out += [put(n, p, ("i", sub[1], sub[2], k)) for k in (0, 1) if abs(k) < abs(sub[3])]
        elif t == "ii":
            out += [put(n, p, ("ii", sub[1], sub[2], k, sub[4])) for k in (0, 1) if abs(k) < abs(sub[3])]
            out += [put(n, p, ("ii", sub[1], sub[2], sub[3], k)) for k in (0, 1) if abs(k) < abs(sub[4])]
        elif t == "cc":
            if sub[3] != 0x61:
                out.append(put(n, p, ("cc", sub[1], sub[2], 0x61, sub[4])))
            if sub[4] != 0x61:
                out.append(put(n, p, ("cc", sub[1], sub[2], sub[3], 0x61)))
    return out


def shrink_view(v):
    _, kind, node, arg = v
    out = [("view", kind, n2, arg) for n2 in shrink_node(node)]
    if kind in CON_OTRA and kind != "indexOfFrom":
        out += [("view", kind, node, a2) for a2 in shrink_node(arg)]
    if kind == "indexOfFrom":
        out += [("view", kind, node, (a2, arg[1])) for a2 in shrink_node(arg[0])]
        out += [("view", kind, node, (arg[0], k)) for k in (0, 1) if abs(k) < abs(arg[1])]
    if kind in ("charAt", "codePointAt"):
        out += [("view", kind, node, k) for k in (0, 1) if abs(k) < abs(arg)]
    if kind == "indexOfChar" and arg != 0x61:
        out.append(("view", kind, node, 0x61))
    if kind != "enc":
        out.append(("view", "enc", node, None))
    return out


def size(v):
    def s(n):
        if n[0] == "lit":
            return 1000 + 10 * len(n[1])
        extra = sum(abs(x) for x in n[3:] if isinstance(x, int))
        return 1000 + extra + sum(s(ch) for ch in kids(n))
    _, kind, node, arg = v
    total = s(node) + (0 if kind == "enc" else 500)
    if isinstance(arg, tuple) and arg and arg[0] == "indexOfFrom":
        pass
    if kind in CON_OTRA and kind != "indexOfFrom":
        total += s(arg)
    elif kind == "indexOfFrom":
        total += s(arg[0]) + abs(arg[1])
    elif isinstance(arg, int):
        total += abs(arg) if arg < 0x10000 else 100
    return total


def reduce(v, kind, tag):
    cur, pasos = v, []
    for ronda in range(30):
        cands = sorted({c for c in shrink_view(cur) if size(c) < size(cur)}, key=size)[:150]
        if not cands:
            break
        jdk, ours, _ = run_both(f"SR{tag}x{ronda}", cands)
        ok = [c for i, c in enumerate(cands) if i in jdk and classify(jdk[i], ours.get(i)) == kind]
        if not ok:
            break
        best = min(ok, key=size)
        i = cands.index(best)
        pasos.append({"java": render_view(best), "jdk": jdk[i], "ours": ours.get(i)})
        cur = best
    jdk, ours, _ = run_both(f"SR{tag}fin", [cur])
    return cur, pasos, jdk.get(0), ours.get(0)


# --------------------------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--lotes", type=int, default=6)
    ap.add_argument("--por-lote", type=int, default=120)
    ap.add_argument("--reducir", type=int, default=8)
    ap.add_argument("--jdk", required=True, help="carpeta del JDK de referencia (la que tiene bin/ y release)")
    ap.add_argument("--salida", default=None)
    a = ap.parse_args()

    global JAVAC, JAVA
    JAVAC, JAVA = Path(a.jdk) / "bin" / "javac.exe", Path(a.jdk) / "bin" / "java.exe"
    release = (Path(a.jdk) / "release").read_text(encoding="utf-8", errors="replace")
    campos = {k: v.strip().strip('"') for k, v in (ln.split("=", 1) for ln in release.splitlines() if "=" in ln)}
    meta = {
        "prueba": "java.lang.String",
        "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
        "referencia": f'{campos.get("IMPLEMENTOR", "?")} {campos.get("JAVA_VERSION", "?")}',
        "referencia_ruta": a.jdk,
        "bajo_prueba": "KajiJDK (run-headless) con KajiLibrary",
        "binario_kaji_fecha": datetime.datetime.fromtimestamp(HEADLESS.stat().st_mtime).isoformat(timespec="seconds"),
        "semilla": a.seed, "lotes": a.lotes, "por_lote": a.por_lote,
    }
    salida = Path(a.salida) if a.salida else Path(__file__).resolve().parent.parent / "datos" / f"strings-{a.seed}.json"
    salida.parent.mkdir(parents=True, exist_ok=True)
    print("referencia:", meta["referencia"], "·", "salida:", salida, flush=True)

    OPS = {"toUpperCase", "toLowerCase", "trim", "strip", "stripLeading", "stripTrailing", "concat",
           "replace", "substring", "repeat", "length", "isEmpty", "isBlank", "charAt", "codePointAt",
           "codePointCount", "indexOf", "lastIndexOf", "contains", "startsWith", "endsWith", "equals",
           "equalsIgnoreCase", "compareTo", "compareToIgnoreCase", "hashCode", "getBytes"}
    r = random.Random(a.seed)
    stats = {"expresiones": 0, "coinciden": 0, "divergen": 0, "por_tipo": {}, "por_vista": {},
             "ops_total": {}, "ops_divergentes": {}, "segundos_jdk": 0.0, "segundos_kaji": 0.0}
    lotes, divs, reducidas = [], [], []
    t_inicio = time.time()

    def guardar(estado):
        stats["segundos_total"] = round(time.time() - t_inicio, 1)
        doc = {"estado": estado, "meta": meta,
               "stats": {k: (round(v, 1) if isinstance(v, float) else v) for k, v in stats.items()},
               "lotes": lotes,
               "divergencias": [{k: v for k, v in d.items() if k != "view"} for d in divs],
               "reducidas": reducidas}
        salida.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")

    def sumar(tabla, clave):
        tabla[clave] = tabla.get(clave, 0) + 1

    for lote in range(a.lotes):
        views = [gen_view(r) for _ in range(a.por_lote)]
        jdk, ours, info = run_both(f"S{a.seed}x{lote}", views)
        stats["segundos_jdk"] += info["jdk_s"]
        stats["segundos_kaji"] += info["ours_s"]
        lotes.append({"lote": lote, "salida_kaji": info["ours_exit"], "stderr_kaji": info["ours_err"],
                      "segundos_jdk": round(info["jdk_s"], 2), "segundos_kaji": round(info["ours_s"], 2)})
        for k, v in enumerate(views):
            if k not in jdk:
                continue
            java = render_view(v)
            ops = sorted(set(re.findall(r"\.(\w+)\(", java)) & OPS)
            stats["expresiones"] += 1
            for op in ops:
                sumar(stats["ops_total"], op)
            kind = classify(jdk[k], ours.get(k))
            if kind is None:
                stats["coinciden"] += 1
                continue
            stats["divergen"] += 1
            sumar(stats["por_tipo"], kind)
            sumar(stats["por_vista"], v[1])
            for op in ops:
                sumar(stats["ops_divergentes"], op)
            divs.append({"lote": lote, "k": k, "tipo": kind, "vista": v[1], "ops": ops, "view": v,
                         "java": java, "jdk": jdk[k], "kaji": ours.get(k)})
        print(f"lote {lote}: {len(views)} expresiones, {stats['divergen']} divergencias acumuladas", flush=True)
        guardar("generando")

    vistos = set()
    for i, d in enumerate(divs):
        n = d["view"][2]
        firma = (d["tipo"], d["view"][1], n[1] if n[0] != "lit" else "lit")
        if firma in vistos or len(reducidas) >= a.reducir:
            continue
        vistos.add(firma)
        print(f"reduciendo [{d['tipo']}] {d['java'][:90]}...", flush=True)
        t = time.time()
        minimo, pasos, jdk_v, ours_v = reduce(d["view"], d["tipo"], f"{a.seed}x{len(reducidas)}")
        reducidas.append({"divergencia": i, "tipo": d["tipo"],
                          "original": d["java"], "original_jdk": d["jdk"], "original_kaji": d["kaji"],
                          "minimo": render_view(minimo), "minimo_jdk": jdk_v, "minimo_kaji": ours_v,
                          "pasos": pasos, "segundos": round(time.time() - t, 1)})
        guardar("reduciendo")

    guardar("terminada")
    print(json.dumps({"stats": stats, "reducidas": [{k: v for k, v in x.items() if k != "pasos"} for x in reducidas]},
                     ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()
