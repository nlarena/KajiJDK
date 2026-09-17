"""Mini campaña diferencial sobre java.math.BigInteger: JDK real contra KajiJDK + KajiLibrary.

Los cuatro verbos del fuzzer del proyecto, en chico:
  generar  -> expresiones de BigInteger con constantes sesgadas a los bordes
  ejecutar -> un .class por lote, compilado UNA vez con el javac real, corrido por los dos lados
  oráculo  -> igualdad del texto de cada resultado (o de la clase de la excepción)
  reducir  -> achicar cada divergencia mientras siga divergiendo del mismo modo
"""
import argparse
import json
import random
import re
import subprocess
import time
from pathlib import Path

REPO = Path(r"C:\Users\nicol\Sources\Larena\KajiJVM\KajiJDK\repo")
JDK = Path(r"C:\Program Files\Java\jdk-25\bin")
JAVAC, JAVA = JDK / "javac.exe", JDK / "java.exe"
HEADLESS = REPO / "target" / "release" / "run-headless.exe"
WORK = Path(__file__).resolve().parent / "runs"

# --------------------------------------------------------------------------------------------
# generar
# --------------------------------------------------------------------------------------------
POOL = [
    "0", "1", "-1", "2", "-2", "10", "255", "256", "-256", "65535", "65536",
    "2147483647", "-2147483648", "2147483648", "4294967295", "4294967296", "-4294967296",
    "9223372036854775807", "-9223372036854775808", "9223372036854775808",
    "18446744073709551615", "18446744073709551616", "-18446744073709551616",
    "340282366920938463463374607431768211455", "340282366920938463463374607431768211456",
    "123456789012345678901234567890", "-98765432109876543210987654321",
]
UN = ["negate", "abs", "not", "sqrt"]
BIN = ["add", "subtract", "multiply", "divide", "remainder", "mod", "gcd", "min", "max",
       "and", "or", "xor", "andNot", "modInverse"]
INTK = {
    "pow": [0, 1, 2, 3, 7],
    "shiftLeft": [0, 1, 7, 31, 32, 33, 63, 64, 65, 100, -1, -33],
    "shiftRight": [0, 1, 7, 31, 32, 33, 63, 64, 65, 100, -1, -33],
    "setBit": [0, 1, 31, 32, 63, 64, 100],
    "clearBit": [0, 1, 31, 32, 63, 64, 100],
    "flipBit": [0, 1, 31, 32, 63, 64, 100],
}
VIEWS = (["str"] * 9) + ["radix", "bitLength", "bitCount", "signum", "getLowestSetBit", "intValue",
                         "longValue", "doubleValue", "hashCode", "testBit", "bytes", "cmp",
                         "longExact", "parse", "divRem"]


def rand_const(r):
    if r.random() < 0.6:
        return r.choice(POOL)
    n = r.randint(1, 60)
    digits = str(r.randint(1, 9)) + "".join(str(r.randint(0, 9)) for _ in range(n - 1))
    return ("-" if r.random() < 0.4 else "") + digits


def gen(r, d):
    if d == 0 or r.random() < 0.3:
        return ("c", rand_const(r))
    x = r.random()
    if x < 0.2:
        return ("u", r.choice(UN), gen(r, d - 1))
    if x < 0.65:
        return ("b", r.choice(BIN), gen(r, d - 1), gen(r, d - 1))
    if x < 0.92:
        op = r.choice(list(INTK))
        return ("i", op, gen(r, d - 1), r.choice(INTK[op]))
    return ("t", "modPow", gen(r, d - 1), gen(r, d - 1), gen(r, d - 1))


def gen_view(r):
    kind = r.choice(VIEWS)
    node = gen(r, 3)
    arg = None
    if kind in ("radix", "parse"):
        arg = r.choice([2, 8, 10, 16, 36])
    elif kind == "testBit":
        arg = r.choice([0, 1, 31, 32, 63, 64, 100])
    elif kind in ("cmp", "divRem"):
        arg = gen(r, 2)
    return ("view", kind, node, arg)


def render(n):
    t = n[0]
    if t == "c":
        return f'new BigInteger("{n[1]}")'
    if t == "u":
        return f"{render(n[2])}.{n[1]}()"
    if t == "b":
        return f"{render(n[2])}.{n[1]}({render(n[3])})"
    if t == "i":
        return f"{render(n[2])}.{n[1]}({n[3]})"
    return f"{render(n[2])}.modPow({render(n[3])}, {render(n[4])})"


def render_view(v):
    _, kind, node, arg = v
    x = render(node)
    return {
        "str": lambda: f"{x}.toString()",
        "radix": lambda: f"{x}.toString({arg})",
        "bitLength": lambda: f"String.valueOf({x}.bitLength())",
        "bitCount": lambda: f"String.valueOf({x}.bitCount())",
        "signum": lambda: f"String.valueOf({x}.signum())",
        "getLowestSetBit": lambda: f"String.valueOf({x}.getLowestSetBit())",
        "intValue": lambda: f"String.valueOf({x}.intValue())",
        "longValue": lambda: f"String.valueOf({x}.longValue())",
        "doubleValue": lambda: f"String.valueOf({x}.doubleValue())",
        "hashCode": lambda: f"String.valueOf({x}.hashCode())",
        "testBit": lambda: f"String.valueOf({x}.testBit({arg}))",
        "bytes": lambda: f"java.util.Arrays.toString({x}.toByteArray())",
        "cmp": lambda: f"String.valueOf({x}.compareTo({render(arg)}))",
        "longExact": lambda: f"String.valueOf({x}.longValueExact())",
        "parse": lambda: f"new BigInteger({x}.toString({arg}), {arg}).toString()",
        "divRem": lambda: f"java.util.Arrays.toString({x}.divideAndRemainder({render(arg)}))",
    }[kind]()


# --------------------------------------------------------------------------------------------
# ejecutar
# --------------------------------------------------------------------------------------------
def program(name, views):
    lines = ["import java.math.BigInteger;", f"public class {name} {{"]
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
    # **Nuestra VM escribe `System.out` en stderr**, y el JDK real en stdout. La primera versión de
    # este arnés leía sólo stdout de los dos lados y reportó 30 de 30 expresiones "sin salida":
    # la herramienta midiendo su propio canal, no la biblioteca. Se leen los dos flujos.
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
    # Los hijos de tipo BigInteger de un nodo. Un `if` por forma y no un diccionario: un literal de
    # diccionario evalúa todas sus ramas, y `n[3]` no existe en un nodo unario.
    if n[0] == "c":
        return []
    if n[0] in ("u", "i"):
        return [n[2]]
    if n[0] == "b":
        return [n[2], n[3]]
    return [n[2], n[3], n[4]]


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


def shrink_node(n):
    out = []
    for p in paths(n):
        sub = get(n, p)
        for ch in kids(sub):
            out.append(put(n, p, ch))
        if sub[0] == "c":
            s = sub[1]
            for simpler in ["0", "1", "-1", "2"]:
                if simpler != s and len(simpler) < len(s) + (1 if simpler.startswith("-") else 0):
                    out.append(put(n, p, ("c", simpler)))
            body = s.lstrip("-")
            if s.startswith("-"):
                out.append(put(n, p, ("c", body)))
            if len(body) > 2:
                out.append(put(n, p, ("c", ("-" if s.startswith("-") else "") + body[: len(body) // 2])))
        if sub[0] == "i":
            for k2 in (0, 1):
                if abs(k2) < abs(sub[3]):
                    out.append(put(n, p, ("i", sub[1], sub[2], k2)))
    return out


def shrink_view(v):
    _, kind, node, arg = v
    out = [("view", kind, n2, arg) for n2 in shrink_node(node)]
    if kind in ("cmp", "divRem"):
        out += [("view", kind, node, a2) for a2 in shrink_node(arg)]
    if kind != "str":
        out.append(("view", "str", node, None))
    return out


def size(v):
    def s(n):
        if n[0] == "c":
            return 1000 + len(n[1])
        extra = abs(n[3]) if n[0] == "i" else 0
        return 1000 + extra + sum(s(ch) for ch in kids(n))
    _, kind, node, arg = v
    return s(node) + (s(arg) if isinstance(arg, tuple) else 0) + (0 if kind == "str" else 500)


def reduce(v, kind, tag):
    cur, pasos = v, []
    for ronda in range(30):
        cands = sorted({c for c in shrink_view(cur) if size(c) < size(cur)}, key=size)[:150]
        if not cands:
            break
        jdk, ours, _ = run_both(f"R{tag}x{ronda}", cands)
        ok = [c for i, c in enumerate(cands) if i in jdk and classify(jdk[i], ours.get(i)) == kind]
        if not ok:
            break
        best = min(ok, key=size)
        i = cands.index(best)
        pasos.append({"java": render_view(best), "jdk": jdk[i], "ours": ours.get(i)})
        cur = best
    jdk, ours, _ = run_both(f"R{tag}fin", [cur])
    return cur, pasos, jdk.get(0), ours.get(0)


# --------------------------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--lotes", type=int, default=6)
    ap.add_argument("--por-lote", type=int, default=120)
    ap.add_argument("--reducir", type=int, default=8)
    ap.add_argument("--jdk", default=str(JDK.parent),
                    help="carpeta del JDK de referencia (la que tiene bin/ y release)")
    ap.add_argument("--salida", default=None,
                    help="archivo JSON donde se guardan los datos (por defecto, test-fuzzing/datos/)")
    a = ap.parse_args()

    global JAVAC, JAVA
    JAVAC, JAVA = Path(a.jdk) / "bin" / "javac.exe", Path(a.jdk) / "bin" / "java.exe"
    release = (Path(a.jdk) / "release").read_text(encoding="utf-8", errors="replace")
    campos = {k: v.strip().strip('"') for k, v in (ln.split("=", 1) for ln in release.splitlines() if "=" in ln)}
    import datetime
    meta = {
        "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
        "referencia": f'{campos.get("IMPLEMENTOR", "?")} {campos.get("JAVA_VERSION", "?")}',
        "referencia_ruta": a.jdk,
        "bajo_prueba": "KajiJDK (run-headless) con KajiLibrary",
        "binario_kaji_fecha": datetime.datetime.fromtimestamp(HEADLESS.stat().st_mtime).isoformat(timespec="seconds"),
        "semilla": a.seed,
        "lotes": a.lotes,
        "por_lote": a.por_lote,
    }
    salida = Path(a.salida) if a.salida else Path(__file__).resolve().parent.parent / "datos" / f"campaña-{a.seed}.json"
    salida.parent.mkdir(parents=True, exist_ok=True)
    print("referencia:", meta["referencia"], "·", "salida:", salida, flush=True)

    OPS = set(UN) | set(BIN) | set(INTK) | {"modPow"}
    r = random.Random(a.seed)
    stats = {"expresiones": 0, "coinciden": 0, "divergen": 0, "por_tipo": {}, "por_vista": {},
             "ops_total": {}, "ops_divergentes": {}, "segundos_jdk": 0.0, "segundos_kaji": 0.0}
    lotes, divs, reducidas = [], [], []
    t_inicio = time.time()

    # **Se guarda después de cada lote y de cada reducción.** Una campaña larga se puede cortar —la
    # primera se cortó al cerrarse la sesión con 720 expresiones corridas y nada escrito—, y lo ya
    # medido no tiene por qué perderse con ella.
    def guardar(estado):
        stats["segundos_total"] = round(time.time() - t_inicio, 1)
        doc = {
            "estado": estado,
            "meta": meta,
            "stats": {k: (round(v, 1) if isinstance(v, float) else v) for k, v in stats.items()},
            "lotes": lotes,
            "divergencias": [{k: v for k, v in d.items() if k != "view"} for d in divs],
            "reducidas": reducidas,
        }
        salida.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")

    def sumar(tabla, clave):
        tabla[clave] = tabla.get(clave, 0) + 1

    for lote in range(a.lotes):
        views = [gen_view(r) for _ in range(a.por_lote)]
        jdk, ours, info = run_both(f"B{a.seed}x{lote}", views)
        stats["segundos_jdk"] += info["jdk_s"]
        stats["segundos_kaji"] += info["ours_s"]
        lotes.append({"lote": lote, "salida_kaji": info["ours_exit"], "stderr_kaji": info["ours_err"],
                      "segundos_jdk": round(info["jdk_s"], 2), "segundos_kaji": round(info["ours_s"], 2)})
        for k, v in enumerate(views):
            if k not in jdk:
                continue  # el JDK de referencia no respondió: no hay contra qué comparar
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

    # Una reducción por firma distinta: tipo de divergencia, vista y operación de más afuera.
    vistos = set()
    for i, d in enumerate(divs):
        firma = (d["tipo"], d["view"][1], d["view"][2][1] if d["view"][2][0] != "c" else "const")
        if firma in vistos or len(reducidas) >= a.reducir:
            continue
        vistos.add(firma)
        print(f"reduciendo [{d['tipo']}] {d['java'][:90]}...", flush=True)
        t = time.time()
        minimo, pasos, jdk_v, ours_v = reduce(d["view"], d["tipo"], f"{a.seed}x{len(reducidas)}")
        reducidas.append({
            "divergencia": i,
            "tipo": d["tipo"],
            "original": d["java"], "original_jdk": d["jdk"], "original_kaji": d["kaji"],
            "minimo": render_view(minimo), "minimo_jdk": jdk_v, "minimo_kaji": ours_v,
            "pasos": pasos,
            "segundos": round(time.time() - t, 1),
        })
        guardar("reduciendo")

    guardar("terminada")
    print(json.dumps({"stats": stats, "reducidas": [{k: v for k, v in x.items() if k != "pasos"} for x in reducidas]},
                     ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()
