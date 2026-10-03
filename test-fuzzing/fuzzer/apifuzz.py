"""Campaña diferencial sobre TODA la API de KajiLibrary: referencia contra KajiJDK.

A diferencia de bigfuzz.py y strfuzz.py, que conocen de antemano las operaciones de una clase, este
descubre la API por reflexion (ver Catalog.java) y arma expresiones bien tipadas con lo que encuentra.

Los cinco pasos:
  catalogo -> que se puede construir y que se puede llamar, sacado de la referencia por reflexion
  generar  -> expresiones `new C(...)` o `C.estatico(...)` encadenando metodos, con literales de borde
  ejecutar -> un .class por lote, compilado UNA vez con el javac de referencia, corrido por los dos lados
  determinismo -> cada lote se corre DOS veces en la referencia, en procesos distintos; lo que no
                  coincide consigo mismo se descarta antes de comparar. Eso saca la hora, el hash de
                  identidad, el azar y cualquier otra cosa que no sea funcion de sus entradas, sin que
                  nadie tenga que clasificar API por API
  oraculo  -> igualdad del texto de cada resultado (o de la clase de la excepcion)
  reducir  -> achicar cada divergencia mientras siga divergiendo del mismo modo

    python test-fuzzing/fuzzer/apifuzz.py --jdk "$JDK" --catalogo cat.json --seed 1
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
# literales: los valores con los que se alimenta cualquier API
# --------------------------------------------------------------------------------------------
LITERALES = {
    "int": ["0", "1", "-1", "2", "7", "31", "32", "-128", "127", "255", "65535",
            "2147483647", "-2147483648"],
    "long": ["0L", "1L", "-1L", "255L", "4294967296L", "9223372036854775807L", "-9223372036854775808L"],
    "double": ["0.0", "1.0", "-1.0", "0.5", "-0.0", "3.141592653589793", "1e308", "1e-308",
               "Double.NaN", "Double.POSITIVE_INFINITY", "Double.NEGATIVE_INFINITY"],
    "float": ["0.0f", "1.0f", "-1.0f", "0.5f", "-0.0f", "3.4028235E38f", "Float.NaN"],
    "boolean": ["true", "false"],
    "char": ["'a'", "'A'", "'0'", "' '", "'\\n'", "(char) 0", "(char) 65535", "(char) 223"],
    "byte": ["(byte) 0", "(byte) 1", "(byte) -1", "(byte) 127", "(byte) -128"],
    "short": ["(short) 0", "(short) 1", "(short) -1", "(short) 32767", "(short) -32768"],
    "java.lang.String": ['""', '"a"', '"A"', '"abc"', '" "', '"\\n"', '"0"', '"-1"', '"ABC abc"',
                         '"\\u00df"', '"\\u03a3"', '"hola mundo"', '"1234567890"'],
    "int[]": ["new int[0]", "new int[] {1}", "new int[] {1, 2, 3}", "new int[] {-1, 0, 2147483647}"],
    "long[]": ["new long[0]", "new long[] {1L, -1L}"],
    "double[]": ["new double[0]", "new double[] {1.0, -0.0, Double.NaN}"],
    "char[]": ["new char[0]", "new char[] {'a', 'b'}", "new char[] {(char) 0, (char) 65535}"],
    "byte[]": ["new byte[0]", "new byte[] {1, 2, 3}", "new byte[] {-1, 0, 127}"],
    "java.lang.String[]": ["new String[0]", 'new String[] {"a", "b"}'],
}
# Tipos que se satisfacen con un literal de otro tipo.
ALIAS = {
    "java.lang.CharSequence": "java.lang.String",
    "java.lang.Object": "java.lang.String",
    "java.lang.Comparable": "java.lang.String",
    "java.lang.Integer": "int",
    "java.lang.Long": "long",
    "java.lang.Double": "double",
    "java.lang.Boolean": "boolean",
    "java.lang.Character": "char",
    "java.lang.Number": "int",
}
ENVOLTORIOS = {"java.lang.Integer": "Integer.valueOf({})", "java.lang.Long": "Long.valueOf({})",
               "java.lang.Double": "Double.valueOf({})", "java.lang.Boolean": "Boolean.valueOf({})",
               "java.lang.Character": "Character.valueOf({})", "java.lang.Number": "Integer.valueOf({})"}


# Clases sujeto que no son funcion de sus entradas: tocan disco, red, reloj, consola o el estado de
# la VM. Una diferencia ahi no dice nada sobre la biblioteca como implementacion de la especificacion.
ENTORNO = ("java.util.zip.ZipFile", "java.util.zip.ZipInputStream", "java.util.logging",
           "java.util.prefs", "java.util.ServiceLoader", "java.util.Scanner", "java.util.Timer",
           "java.util.concurrent", "java.io", "java.net", "java.nio.file", "java.nio.channels",
           "java.lang.Thread", "java.lang.Runtime", "java.lang.System", "java.lang.Process",
           "java.lang.ref", "java.lang.ProcessBuilder", "java.lang.ClassLoader", "javax.", "com.sun.",
           "jdk.internal", "sun.", "java.awt", "java.applet", "java.beans", "java.rmi", "jakarta.")

# Una salida con la forma `algo@1f2e3d` lleva el hash de identidad: no se compara.
DIRECCION = re.compile(r"@[0-9a-f]{4,}")

# Metodos cuyo resultado depende de donde se los llamo, no de sus argumentos.
METODOS_FUERA = {"getStackTrace", "fillInStackTrace", "getSuppressed", "notify", "notifyAll",
                 "identityHashCode", "nanoTime", "currentTimeMillis", "lineSeparator"}


def del_entorno(clase):
    return any(clase.startswith(p) for p in ENTORNO)


class Catalogo:
    def __init__(self, ruta):
        d = json.loads(Path(ruta).read_text(encoding="utf-8"))
        self.ctors, self.estaticos, self.metodos = {}, {}, {}
        for c in d["constructores"]:
            self.ctors.setdefault(c["clase"], []).append(c)
        for m in d["estaticos"]:
            if m["nombre"] in METODOS_FUERA:
                continue
            self.estaticos.setdefault(m["clase"], []).append(m)
        for m in d["metodos"]:
            if m["nombre"] in METODOS_FUERA:
                continue
            self.metodos.setdefault(m["clase"], []).append(m)
        self.con_to_string = set(d["conToString"])
        self.clases = sorted(set(self.ctors) | set(self.estaticos))

    def puede_armar(self, tipo, prof):
        if tipo in LITERALES or tipo in ALIAS:
            return True
        return prof > 0 and bool(self.ctors.get(tipo) or self.estaticos.get(tipo))


def corto(tipo):
    """El nombre tal como se escribe en el programa generado: siempre calificado."""
    return tipo


# --------------------------------------------------------------------------------------------
# generar
# --------------------------------------------------------------------------------------------
def literal(r, tipo):
    base = ALIAS.get(tipo, tipo)
    if base not in LITERALES:
        return None
    v = r.choice(LITERALES[base])
    if tipo in ENVOLTORIOS:
        return ENVOLTORIOS[tipo].format(v)
    if tipo == "java.lang.CharSequence" or tipo == "java.lang.Object" or tipo == "java.lang.Comparable":
        return v
    return v


def arg(cat, r, tipo, prof):
    """Un argumento del tipo pedido: un literal, o algo construido si no hay literal."""
    lit = literal(r, tipo)
    if lit is not None and (prof <= 0 or r.random() < 0.8):
        return {"k": "lit", "tipo": tipo, "java": lit}
    hecho = construir(cat, r, tipo, prof - 1)
    return hecho if hecho is not None else ({"k": "lit", "tipo": tipo, "java": lit} if lit else None)


def construir(cat, r, tipo, prof):
    """Un valor del tipo pedido, por constructor o por metodo estatico."""
    if prof < 0:
        return None
    opciones = [("new", c) for c in cat.ctors.get(tipo, [])]
    opciones += [("est", m) for m in cat.estaticos.get(tipo, []) if m["ret"] == tipo]
    r.shuffle(opciones)
    for clase, firma in opciones[:6]:
        args = []
        for p in firma["params"]:
            a = arg(cat, r, p, prof - 1)
            if a is None:
                args = None
                break
            args.append(a)
        if args is None:
            continue
        return {"k": clase, "tipo": tipo, "firma": firma, "args": args}
    return None


def expresion(cat, r, clase):
    """Una expresion sobre `clase`: construirla (o llamar un estatico) y encadenar metodos."""
    base = construir(cat, r, clase, 2)
    if base is None:
        estaticos = cat.estaticos.get(clase, [])
        if not estaticos:
            return None
        m = r.choice(estaticos)
        args = []
        for p in m["params"]:
            a = arg(cat, r, p, 1)
            if a is None:
                return None
            args.append(a)
        base = {"k": "est", "tipo": m["ret"], "firma": m, "args": args}
    cur = base
    for _ in range(r.randint(0, 2)):
        candidatos = cat.metodos.get(cur["tipo"], [])
        if not candidatos:
            break
        r.shuffle(candidatos)
        elegido = None
        for m in candidatos[:8]:
            args = []
            for p in m["params"]:
                a = arg(cat, r, p, 1)
                if a is None:
                    args = None
                    break
                args.append(a)
            if args is not None:
                elegido = {"k": "call", "tipo": m["ret"], "firma": m, "args": args, "recv": cur}
                break
        if elegido is None:
            break
        cur = elegido
    return cur


def render(n):
    if n["k"] == "lit":
        return n["java"]
    args = ", ".join(render(a) for a in n["args"])
    f = n["firma"]
    if n["k"] == "new":
        return f"new {f['clase']}({args})"
    if n["k"] == "est":
        return f"{f['clase']}.{f['nombre']}({args})"
    return f"{render(n['recv'])}.{f['nombre']}({args})"


# --------------------------------------------------------------------------------------------
# ejecutar
# --------------------------------------------------------------------------------------------
AYUDA = r'''
    /** Imprime cualquier valor de forma estable: sin hash de identidad y sin orden de iteracion. */
    static String render(Object o) {
        if (o == null) return "null";
        if (o instanceof Object[]) return renderArray((Object[]) o);
        if (o instanceof int[]) { int[] a = (int[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf(a[i]); return join(p, false); }
        if (o instanceof long[]) { long[] a = (long[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf(a[i]); return join(p, false); }
        if (o instanceof double[]) { double[] a = (double[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf(a[i]); return join(p, false); }
        if (o instanceof char[]) { char[] a = (char[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf((int) a[i]); return join(p, false); }
        if (o instanceof byte[]) { byte[] a = (byte[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf(a[i]); return join(p, false); }
        if (o instanceof boolean[]) { boolean[] a = (boolean[]) o; String[] p = new String[a.length];
            for (int i = 0; i < a.length; i++) p[i] = String.valueOf(a[i]); return join(p, false); }
        if (o instanceof java.util.Map) {
            java.util.Map<?, ?> m = (java.util.Map<?, ?>) o;
            String[] p = new String[m.size()]; int i = 0;
            for (java.util.Map.Entry<?, ?> e : m.entrySet()) p[i++] = render(e.getKey()) + "=" + render(e.getValue());
            return join(p, true);   // ordenado: el orden de iteracion de un mapa no es parte del contrato
        }
        if (o instanceof java.util.Collection) {
            java.util.Collection<?> c = (java.util.Collection<?>) o;
            String[] p = new String[c.size()]; int i = 0;
            for (Object e : c) p[i++] = render(e);
            return join(p, !(o instanceof java.util.List));
        }
        if (o instanceof CharSequence) return "\"" + o + "\"";
        String s = o.toString();
        // Un toString heredado de Object lleva el hash de identidad, que cambia en cada corrida.
        return s.startsWith(o.getClass().getName() + "@") ? "<objeto>" : s;
    }

    static String renderArray(Object[] a) {
        String[] p = new String[a.length];
        for (int i = 0; i < a.length; i++) p[i] = render(a[i]);
        return join(p, false);
    }

    static String join(String[] p, boolean ordenar) {
        if (ordenar) {
            for (int i = 1; i < p.length; i++) {          // insercion: sin depender de Arrays.sort
                String x = p[i]; int j = i - 1;
                while (j >= 0 && p[j].compareTo(x) > 0) { p[j + 1] = p[j]; j--; }
                p[j + 1] = x;
            }
        }
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < p.length; i++) b.append(i > 0 ? ", " : "").append(p[i]);
        return b.append(']').toString();
    }
'''


def program(name, exprs):
    lines = [f"public class {name} {{", AYUDA]
    for k, e in enumerate(exprs):
        lines.append(
            f"  static String e{k}() {{ try {{ return render({render(e)}); }} "
            f'catch (Throwable t) {{ return "EXC:" + t.getClass().getName(); }} }}'
        )
    lines.append("  public static int run() {")
    for k in range(len(exprs)):
        lines.append(f'    System.out.println("{k}=" + e{k}());')
    lines.append(f"    return {len(exprs)};")
    lines.append("  }")
    lines.append("  public static void main(String[] a) { run(); }")
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


def corre_jdk(d, name):
    # `-Dstdout.encoding=UTF-8`: sin eso, en Windows la referencia escribe '?' donde haya un
    # caracter que la consola no sabe, y el oraculo ve una diferencia que no existe.
    p = subprocess.run([str(JAVA), "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8",
                        "-cp", str(d), name],
                       capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=300)
    return parse(p.stdout)


CULPABLE = re.compile(r"\.java:(\d+): error")


def compila(d, name, exprs):
    """Compila el lote. Si javac rechaza algo, saca las expresiones culpables y reintenta.

    Una sola expresion mal tipada --los genericos dan varias-- tiraba el lote entero: cuarenta
    expresiones perdidas por una. javac nombra la linea, y cada expresion ocupa una linea conocida.
    """
    vivas = list(range(len(exprs)))
    for _ in range(4):
        fuente = program(name, [exprs[i] for i in vivas])
        (d / f"{name}.java").write_text(fuente, encoding="utf-8")
        c = subprocess.run([str(JAVAC), "-nowarn", "-d", str(d), str(d / f"{name}.java")],
                           capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=600)
        if c.returncode == 0:
            return vivas, c.stderr
        lineas = {int(m) for m in CULPABLE.findall(c.stderr)}
        culpables = set()
        # cada expresion es un metodo `e<k>()` en su propia linea: se mapea linea -> indice
        mapa = {}
        for i, ln in enumerate(fuente.splitlines(), 1):
            m = re.match(r"\s*static String e(\d+)\(\)", ln)
            if m:
                mapa[i] = int(m.group(1))
        for ln in lineas:
            if ln in mapa:
                culpables.add(vivas[mapa[ln]])
        if not culpables:
            return [], c.stderr
        vivas = [i for i in vivas if i not in culpables]
        if not vivas:
            return [], c.stderr
    return [], "javac sigue rechazando el lote"


def run_both(name, exprs):
    """Compila el lote y lo corre: dos veces en la referencia (determinismo) y una en KajiJDK."""
    d = WORK / name
    d.mkdir(parents=True, exist_ok=True)
    vivas, err = compila(d, name, exprs)
    if not vivas:
        return None, None, None, {"javac": err[-4000:]}
    t0 = time.time()
    jdk1 = corre_jdk(d, name)
    jdk2 = corre_jdk(d, name)
    t1 = time.time()
    try:
        ours = subprocess.run([str(HEADLESS), str(d / f"{name}.class"), "run"], cwd=str(REPO),
                              capture_output=True, text=True, encoding="utf-8", errors="replace",
                              timeout=600)
        salida, codigo, err = ours.stdout, ours.returncode, ours.stderr
    except subprocess.TimeoutExpired as e:
        dec = lambda b: b.decode("utf-8", "replace") if isinstance(b, bytes) else (b or "")
        salida, codigo, err = dec(e.stdout), "timeout", dec(e.stderr)
    t2 = time.time()
    # KajiJDK escribe `System.out` por stderr; el JDK de referencia por stdout.
    cola = "\n".join(l for l in err.splitlines() if not l.startswith("[gc]") and not LINE.match(l))[-600:]
    return jdk1, jdk2, parse(salida + "\n" + err), {
        "jdk_s": t1 - t0, "ours_s": t2 - t1, "ours_exit": codigo, "ours_err": cola, "vivas": vivas}


# --------------------------------------------------------------------------------------------
# oraculo
# --------------------------------------------------------------------------------------------
FALTA = {"java.lang.NoSuchMethodError", "java.lang.AbstractMethodError", "java.lang.NoClassDefFoundError",
         "java.lang.UnsatisfiedLinkError", "java.lang.ClassNotFoundException"}


def classify(jdk, ours):
    if jdk == ours:
        return None
    if ours is None:
        return "sin salida (crash o cuelgue)"
    if ours.startswith("EXC:") and ours[4:] in FALTA:
        return "falta en la biblioteca"
    if jdk.startswith("EXC:") and ours.startswith("EXC:"):
        return "otra excepción"
    if jdk.startswith("EXC:") != ours.startswith("EXC:"):
        return "excepción contra valor"
    return "otro valor"


# --------------------------------------------------------------------------------------------
# reducir
# --------------------------------------------------------------------------------------------
def hijos(n):
    return ([] if n["k"] == "lit" else
            (n["args"] + ([n["recv"]] if n["k"] == "call" else [])))


def reemplazos(cat, n):
    """Versiones mas chicas de la expresion: sacar la llamada de afuera, simplificar un argumento."""
    fuera = []
    if n["k"] == "call":
        fuera.append(n["recv"])                       # la cadena sin su ultimo paso
    for i, a in enumerate(n["args"]):
        if a["k"] != "lit":
            fuera.append({**n, "args": [*n["args"][:i], simplificar(a), *n["args"][i + 1:]]})
        else:
            base = ALIAS.get(a["tipo"], a["tipo"])
            for simple in LITERALES.get(base, [])[:3]:
                if simple != a["java"]:
                    nuevo = dict(a, java=ENVOLTORIOS[a["tipo"]].format(simple) if a["tipo"] in ENVOLTORIOS else simple)
                    fuera.append({**n, "args": [*n["args"][:i], nuevo, *n["args"][i + 1:]]})
    if n["k"] == "call":
        fuera += [{**n, "recv": r} for r in reemplazos(cat, n["recv"])]
    return fuera


def simplificar(n):
    """Un argumento construido se reemplaza por su primer argumento literal, si tiene."""
    for a in n.get("args", []):
        if a["k"] == "lit" and a["tipo"] == n["tipo"]:
            return a
    return n


def tamano(n):
    return 1 + len(render(n)) / 1000 + sum(tamano(h) for h in hijos(n))


def reducir(cat, n, tipo, tag):
    cur, pasos = n, []
    for ronda in range(12):
        cands = [c for c in reemplazos(cat, cur) if tamano(c) < tamano(cur)]
        vistos, unicos = set(), []
        for c in cands:
            j = render(c)
            if j not in vistos:
                vistos.add(j)
                unicos.append(c)
        if not unicos:
            break
        unicos = sorted(unicos, key=tamano)[:60]
        jdk1, jdk2, ours, info = run_both(f"AR{tag}x{ronda}", unicos)
        if jdk1 is None:
            break
        vivos = [unicos[i] for i in info["vivas"]]
        ok = [c for i, c in enumerate(vivos)
              if i in jdk1 and jdk1[i] == jdk2.get(i) and classify(jdk1[i], ours.get(i)) == tipo]
        if not ok:
            break
        mejor = min(ok, key=tamano)
        i = vivos.index(mejor)
        pasos.append({"java": render(mejor), "jdk": jdk1[i], "ours": ours.get(i)})
        cur = mejor
    jdk1, jdk2, ours, _ = run_both(f"AR{tag}fin", [cur])
    return cur, pasos, (jdk1 or {}).get(0), (ours or {}).get(0)


# --------------------------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", required=True, help="carpeta del JDK de referencia")
    ap.add_argument("--catalogo", required=True, help="el JSON que escribe Catalog.java")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--clases", type=int, default=40, help="cuantas clases sujeto prueba la campaña")
    ap.add_argument("--por-clase", type=int, default=40, help="expresiones por clase")
    ap.add_argument("--reducir", type=int, default=8)
    ap.add_argument("--solo", default=None, help="prefijo de paquete, por ejemplo java.util")
    ap.add_argument("--salida", default=None)
    a = ap.parse_args()

    global JAVAC, JAVA
    JAVAC, JAVA = Path(a.jdk) / "bin" / "javac.exe", Path(a.jdk) / "bin" / "java.exe"
    campos = {k: v.strip().strip(chr(34)) for k, v in
              (l.split("=", 1) for l in (Path(a.jdk) / "release").read_text(encoding="utf-8").splitlines() if "=" in l)}

    cat = Catalogo(a.catalogo)
    candidatas = [c for c in cat.clases
                  if (not a.solo or c.startswith(a.solo)) and not del_entorno(c)]
    r = random.Random(a.seed)
    r.shuffle(candidatas)
    sujetos = candidatas[: a.clases]

    meta = {"prueba": "API de KajiLibrary", "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
            "referencia": f'{campos.get("IMPLEMENTOR", "?")} {campos.get("JAVA_VERSION", "?")}',
            "bajo_prueba": "KajiJDK (run-headless) con KajiLibrary",
            "semilla": a.seed, "clases": len(sujetos), "por_clase": a.por_clase,
            "paquete": a.solo or "todos"}
    salida = Path(a.salida) if a.salida else REPO / "test-fuzzing" / "datos" / f"api-{a.seed}.json"
    salida.parent.mkdir(parents=True, exist_ok=True)
    print("referencia:", meta["referencia"], "· clases sujeto:", len(sujetos), flush=True)

    stats = {"expresiones": 0, "no_deterministas": 0, "con_direccion": 0, "expresiones_descartadas": 0,
             "sin_forma_observable": 0, "no_medidas_tras_crash": 0, "coinciden": 0, "divergen": 0,
             "lotes_sin_compilar": 0, "clases_probadas": 0, "por_tipo": {}, "por_clase": {},
             "segundos_jdk": 0.0, "segundos_kaji": 0.0}
    divs, reducidas, descartadas = [], [], []
    t0 = time.time()

    def guardar(estado):
        stats["segundos_total"] = round(time.time() - t0, 1)
        salida.write_text(json.dumps({
            "estado": estado, "meta": meta,
            "stats": {k: (round(v, 1) if isinstance(v, float) else v) for k, v in stats.items()},
            "sin_compilar": descartadas[:40],
            "divergencias": [{k: v for k, v in d.items() if k != "expr"} for d in divs],
            "reducidas": reducidas,
        }, ensure_ascii=False, indent=2), encoding="utf-8")

    for idx, clase in enumerate(sujetos):
        exprs = []
        for _ in range(a.por_clase * 3):
            if len(exprs) >= a.por_clase:
                break
            e = expresion(cat, r, clase)
            if e is not None:
                exprs.append(e)
        if not exprs:
            continue
        nombre = f"A{a.seed}x{idx}"
        jdk1, jdk2, ours, info = run_both(nombre, exprs)
        if jdk1 is None:
            stats["lotes_sin_compilar"] += 1
            descartadas.append({"clase": clase, "javac": info["javac"][:300]})
            print(f"{idx+1}/{len(sujetos)} {clase}: el lote no compila", flush=True)
            continue
        stats["clases_probadas"] += 1
        stats["segundos_jdk"] += info["jdk_s"]
        stats["segundos_kaji"] += info["ours_s"]
        stats["expresiones_descartadas"] += len(exprs) - len(info["vivas"])
        usadas = [exprs[i] for i in info["vivas"]]
        # Si nuestra VM se cayo, dejo de imprimir: la primera que falta es el caso, y las de atras
        # no se midieron. Contarlas a todas como divergencia inventaria veinte hallazgos de uno.
        corte = next((k for k in range(len(usadas)) if k in jdk1 and k not in ours), None)
        for k, e in enumerate(usadas):
            if k not in jdk1:
                continue
            if corte is not None and k > corte:
                stats["no_medidas_tras_crash"] += 1
                continue
            if jdk1[k] != jdk2.get(k):
                stats["no_deterministas"] += 1       # la referencia no coincide consigo misma
                continue
            if DIRECCION.search(jdk1[k]) or DIRECCION.search(ours.get(k) or ""):
                stats["con_direccion"] += 1          # el hash de identidad se filtro hasta la salida
                continue
            if "<objeto>" in jdk1[k]:
                stats["sin_forma_observable"] += 1   # la referencia tampoco puede mostrar que es
                continue
            stats["expresiones"] += 1
            tipo = classify(jdk1[k], ours.get(k))
            if tipo is None:
                stats["coinciden"] += 1
                continue
            stats["divergen"] += 1
            stats["por_tipo"][tipo] = stats["por_tipo"].get(tipo, 0) + 1
            stats["por_clase"][clase] = stats["por_clase"].get(clase, 0) + 1
            divs.append({"clase": clase, "tipo": tipo, "expr": e, "java": render(e),
                         "jdk": jdk1[k], "kaji": ours.get(k)})
        print(f"{idx+1}/{len(sujetos)} {clase}: {len(exprs)} expresiones, "
              f"{stats['divergen']} divergencias acumuladas", flush=True)
        guardar("generando")

    vistos = set()
    for i, d in enumerate(divs):
        firma = (d["tipo"], d["clase"])
        if firma in vistos or len(reducidas) >= a.reducir:
            continue
        vistos.add(firma)
        print(f"reduciendo [{d['tipo']}] {d['java'][:90]}...", flush=True)
        t = time.time()
        minimo, pasos, jdk_v, kaji_v = reducir(cat, d["expr"], d["tipo"], f"{a.seed}x{len(reducidas)}")
        reducidas.append({"divergencia": i, "clase": d["clase"], "tipo": d["tipo"],
                          "original": d["java"], "original_jdk": d["jdk"], "original_kaji": d["kaji"],
                          "minimo": render(minimo), "minimo_jdk": jdk_v, "minimo_kaji": kaji_v,
                          "pasos": pasos, "segundos": round(time.time() - t, 1)})
        guardar("reduciendo")

    guardar("terminada")
    print(json.dumps({"stats": stats}, ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()
