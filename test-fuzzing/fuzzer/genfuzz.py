# -*- coding: utf-8 -*-
"""Campaña diferencial GENÉRICA: se le nombran clases y sale a buscar diferencias solo.

`bigfuzz.py` y `strfuzz.py` no son genéricos: cada uno lleva escrita a mano la lista de métodos
de su clase, y por eso un método nuevo en la biblioteca no entra nunca a una campaña. Este
programa hace lo mismo que ellos pero **descubre la superficie en vez de tenerla clavada**:

    python genfuzz.py --clases java.lang.String java.math.BigInteger --seed 1

Cómo descubre. Con `javap -s` de los dos lados --el `.class` de KajiLibrary y la clase homónima
del JDK de referencia-- y se queda con la **intersección** de (nombre, descriptor) públicos. La
intersección y no la unión: lo que está de un solo lado no se puede comparar, y aparece en el
informe de cobertura como "falta de un lado" en vez de ensuciar las divergencias.

Qué sigue siendo datos y no código. Tres cosas, y están en un JSON que se puede editar sin tocar
este archivo (`genfuzz-config.json`, o el que se pase por `--config`):

  * **semillas** por tipo --los enteros, los caracteres y las cadenas donde viven los errores--,
    que antes estaban duplicadas en los dos fuzzers y ahora las comparten todas las clases;
  * **exclusiones**, la lista corta de lo que no se puede comparar porque no es determinista
    (identidad de objeto, reloj, azar, ambiente);
  * **fabricas**, para tipos que no se pueden escribir como literal y hacen falta como argumento.

Un caso de prueba **no es una expresión sino una secuencia** de declaraciones, cada una pudiendo
usar las anteriores:

    java.lang.String v0 = s(931, 927, 934);
    java.lang.String v1 = v0.toLowerCase();
    int v2 = v1.indexOf(v0, 3);

Es más trabajo que un árbol de expresiones y se eligió a propósito: con una secuencia entran las
clases con estado --un `StringBuilder`, una lista-- donde el error aparece recién en la tercera
llamada, y con un árbol de expresiones no entran nunca.

Qué se observa. Al final de la secuencia se imprime la codificación de **todas** las variables
observables, no solo la última: si una llamada dejó mal al receptor, se ve aunque lo que devolvió
haya estado bien. Observable quiere decir que el tipo es primitivo, `String`, arreglo de esos, o
una clase que **declara** su propio `toString()` en los dos lados --eso se descubre con javap, no
se supone--. Un objeto que hereda el `toString()` de `Object` lleva la identidad adentro y no se
puede comparar entre dos VM.

Las cadenas entran como códigos numéricos, `s(65, 0xD800)`, y salen como la lista de sus códigos,
`S[65,55296]`: así ni el archivo fuente ni la consola pueden inventar una diferencia. Es la misma
decisión de `strfuzz.py`, que ahí está explicada en detalle.
"""
import argparse
import datetime
import json
import os
import random
import re
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
KAJILIB = REPO / "KajiLibrary"
HEADLESS = REPO / "target" / "release" / "run-headless.exe"
WORK = Path(__file__).resolve().parent / "runs"
JAVAC = JAVA = JAVAP = None
BOOT = None   # con --boot, el directorio de biblioteca contra el que corre nuestra VM


# =============================================================================================
# descriptores
# =============================================================================================
PRIMS = {"B": "byte", "C": "char", "D": "double", "F": "float", "I": "int", "J": "long",
         "S": "short", "Z": "boolean", "V": "void"}


def parse_tipo(d, i):
    """Un tipo del descriptor que arranca en `i`. Devuelve (tipo, i siguiente)."""
    c = d[i]
    if c in PRIMS:
        return c, i + 1
    if c == "[":
        inner, j = parse_tipo(d, i + 1)
        return "[" + inner, j
    if c == "L":
        j = d.index(";", i)
        return d[i:j + 1], j + 1
    raise ValueError("descriptor raro: " + d[i:])


def parse_desc(d):
    """`(Ljava/lang/String;I)Z` -> (['Ljava/lang/String;', 'I'], 'Z')."""
    assert d.startswith("(")
    args, i = [], 1
    while d[i] != ")":
        t, i = parse_tipo(d, i)
        args.append(t)
    ret, _ = parse_tipo(d, i + 1)
    return args, ret


def java_tipo(t):
    """El tipo como se escribe en el fuente: `[Ljava/lang/String;` -> `java.lang.String[]`."""
    if t in PRIMS:
        return PRIMS[t]
    if t.startswith("["):
        return java_tipo(t[1:]) + "[]"
    return t[1:-1].replace("/", ".")


def es_ref(t):
    return not (t in PRIMS)


# =============================================================================================
# descubrir
# =============================================================================================
MODS = {"public", "protected", "private", "static", "final", "abstract", "native",
        "synchronized", "transient", "volatile", "strictfp", "default"}

# Dos espacios EXACTOS: los miembros van con esa sangría y sus `descriptor:` con cuatro.
# La primera version usaba `\s{2}` y las lineas `descriptor:` tambien entraban --terminan
# en `;` cuando el tipo es una clase-- pisando la declaracion que acababa de leer.
DECL = re.compile(r"^ {2}(?! )(?!#)(.*);\s*$")
DESC = re.compile(r"^\s+descriptor:\s*(\S+)\s*$")
FLAGS = re.compile(r"^\s+flags:\s*\(0x[0-9a-fA-F]+\)\s*(.*)$")


def javap(objetivo):
    """javap sobre un nombre de clase (el JDK de referencia) o sobre un archivo .class.

    **El lado de KajiLibrary se lee SIEMPRE por ruta.** La primera version pasaba
    `-cp KajiLibrary java.lang.String` y javap devolvia la clase de la PLATAFORMA: los paquetes
    `java.*` se resuelven desde los modulos del sistema antes que desde el classpath. La
    herramienta comparaba entonces el JDK contra si mismo e informaba 102 miembros iguales y
    cero diferencias, que es justo lo que uno quiere leer y no tenia nada que ver con la
    biblioteca.
    """
    cmd = [str(JAVAP), "-v", "-p", str(objetivo)]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if p.returncode != 0 or "Error:" in p.stdout[:200]:
        return None
    return p.stdout


def leer_miembros(texto, clase):
    """Los miembros que javap imprimió, como {(nombre, descriptor): {mods, clase_decl}}."""
    if texto is None:
        return None, None
    fuera = {}
    lineas = texto.splitlines()
    cabecera = ""
    for ln in lineas:
        if ln.startswith("public ") or ln.startswith("final ") or ln.startswith("class ") \
                or ln.startswith("abstract ") or ln.startswith("interface "):
            cabecera = ln
            break
    pend = ultimo = None
    for ln in lineas:
        if " = " in ln:                        # entrada del pool de constantes, no un miembro
            continue
        m = DECL.match(ln)
        if m:
            pend = m.group(1).strip()
            ultimo = None
            continue
        m = DESC.match(ln)
        if m and pend is not None:
            d = m.group(1)
            toks = pend.split("(")[0].split()
            mods = {t for t in toks if t in MODS}
            if "(" in pend:
                nombre = toks[-1] if toks else ""
                if nombre == clase:            # constructor
                    nombre = "<init>"
                elif "." in nombre:
                    nombre = "<init>" if nombre == clase else nombre.rsplit(".", 1)[-1]
                ultimo = (nombre, d)
                fuera[ultimo] = mods
            pend = None
            continue
        m = FLAGS.match(ln)
        if m and ultimo is not None:
            # **Los puentes se van.** `BigInteger` implementa `Comparable<BigInteger>`, asi que
            # tiene DOS `compareTo`: el de verdad y un puente sintetico `(Ljava/lang/Object;)I`.
            # El puente no existe en el fuente --javac resuelve la llamada al generico-- y
            # generar `bi.compareTo(unaCadena)` a partir de su descriptor borrado hacia que el
            # compilador rechazara el lote entero. Por eso se lee `javap -v` y no `javap -s`.
            if "ACC_BRIDGE" in m.group(1) or "ACC_SYNTHETIC" in m.group(1):
                fuera.pop(ultimo, None)
            ultimo = None
    return fuera, cabecera


def superficie(clase, cp_kaji):
    """Lo público que las DOS implementaciones tienen con el mismo descriptor, más el informe."""
    ruta = Path(cp_kaji) / (clase.replace(".", "/") + ".class")
    t_kaji = javap(ruta) if ruta.exists() else None
    t_ref = javap(clase)
    m_kaji, cab_kaji = leer_miembros(t_kaji, clase)
    m_ref, cab_ref = leer_miembros(t_ref, clase)
    if m_kaji is None:
        return None, {"clase": clase, "error": "KajiLibrary no tiene " + clase}
    if m_ref is None:
        return None, {"clase": clase, "error": "el JDK de referencia no tiene " + clase}
    pub_k = {k for k, v in m_kaji.items() if "public" in v}
    pub_r = {k for k, v in m_ref.items() if "public" in v}
    comun = pub_k & pub_r
    info = {
        "clase": clase,
        "publicos_kaji": len(pub_k),
        "publicos_referencia": len(pub_r),
        "comunes": len(comun),
        "solo_en_kaji": sorted(n + d for n, d in (pub_k - pub_r)),
        "solo_en_referencia": sorted(n + d for n, d in (pub_r - pub_k)),
        "interfaz": cab_ref.startswith("public interface") or cab_ref.startswith("interface"),
        "abstracta": " abstract " in (" " + cab_ref),
    }
    ops = []
    for (nombre, d) in sorted(comun):
        args, ret = parse_desc(d)
        ops.append({
            "clase": clase, "nombre": nombre, "desc": d, "args": args, "ret": ret,
            "estatico": "static" in m_kaji[(nombre, d)],
            "ctor": nombre == "<init>",
        })
    # `toString` propio: lo que decide si un valor de esta clase se puede observar.
    info["declara_toString"] = ("toString", "()Ljava/lang/String;") in comun
    return ops, info


# =============================================================================================
# configuración: semillas, exclusiones, fábricas
# =============================================================================================
CONFIG_POR_DEFECTO = {
    "semillas": {
        "I": [0, 1, -1, 2, 3, 5, 8, 31, 32, 33, 63, 64, 65, 100, 127, 128, 255, 256, 65535,
              65536, 2147483647, -2147483648, -1000000, 1000000],
        "J": [0, 1, -1, 2, 255, 256, 2147483647, 2147483648, -2147483649,
              9223372036854775807, -9223372036854775808],
        "S": [0, 1, -1, 32767, -32768],
        "B": [0, 1, -1, 127, -128],
        "C": [0, 9, 32, 65, 97, 223, 304, 305, 931, 963, 962, 8486, 55296, 56320, 65535],
        "Z": [True, False],
        "F": ["0.0f", "1.0f", "-1.0f", "0.5f", "Float.NaN", "Float.POSITIVE_INFINITY",
              "Float.MIN_VALUE", "Float.MAX_VALUE"],
        "D": ["0.0d", "-0.0d", "1.0d", "-1.0d", "0.1d", "Double.NaN",
              "Double.POSITIVE_INFINITY", "Double.NEGATIVE_INFINITY", "Double.MIN_VALUE",
              "Double.MAX_VALUE", "4.9E-324d", "1.7976931348623157E308d"],
        "Ljava/lang/String;": "__cadenas__"
    },
    # Cadenas que **significan algo** para quien las recibe. Sin esto, todo `String` sale del
    # pozo de bordes de Unicode, y entonces `new BigInteger(cadena)` tira NumberFormatException
    # practicamente siempre: la clase entera queda sin fuzzear y la campana no se entera. Es la
    # diferencia entre un generador generico y uno util, y por eso vive en la configuracion.
    "cadenas_utiles": [
        "", "0", "1", "-1", "2", "-2", "10", "255", "-256", "65536", "-65536",
        "2147483647", "-2147483648", "9223372036854775807", "-9223372036854775808",
        "18446744073709551616", "123456789012345678901234567890",
        "-98765432109876543210987654321", "007", "+5", " 1", "1 ", "ff", "-ff", "z",
        "0x10", "1e3", "NaN", "Infinity", "1.5", "-0.0", "true", "false",
        "a", "A", "abc", "ABC", "hola mundo", "  ", "	", "a,b,,c", "a.b.c"
    ],
    "prob_cadena_util": 0.3,
    # Rangos de letras de un mismo sistema de escritura. El generador arma de a ratos una
    # "palabra" tomando todos los caracteres de UN solo rango, en vez de mezclar scripts al
    # azar. No es un detalle: las reglas de mayusculas y minusculas dependientes del contexto
    # --la sigma final del griego es la que tenemos-- solo se activan cuando hay letras
    # alrededor, y una cadena de caracteres sueltos de cinco alfabetos distintos no las toca
    # nunca. Con texto al azar se prueban los bordes; con palabras se prueban las reglas.
    "rangos_palabra": [
        [97, 122], [65, 90],            # latino
        [224, 255], [192, 222],         # latino con diacriticos
        [945, 969], [913, 937],         # griego (incluye la sigma)
        [1072, 1103], [1040, 1071],     # cirilico
        [1488, 1514],                   # hebreo
        [12354, 12435]                  # hiragana
    ],
    "prob_palabra": 0.25,
    # Cuanto pesa el caracter focal del turno cuando hay que inventar una cadena nueva. Alto a
    # proposito: el par (operacion, caracter) recibe UN caso por pasada, y si adentro de ese caso
    # el foco aparece la mitad de las veces, la pasada se desperdicia la mitad de las veces.
    "prob_foco": 0.85,
    # Clases cuyo `toString()` fija la especificacion, y que por lo tanto se pueden comparar
    # entre dos implementaciones. Si una clase no esta aca, sus valores no se observan: se
    # pueden seguir usando como argumento, pero no se miran. Agregar una clase a esta lista es
    # afirmar que su texto es parte del contrato, no un detalle de implementacion.
    "tipos_valor": [
        "java.lang.String", "java.math.BigInteger", "java.math.BigDecimal",
        "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
        "java.lang.Character", "java.lang.Boolean", "java.lang.Double", "java.lang.Float",
        "java.lang.StringBuilder", "java.lang.StringBuffer"
    ],
    # Cada cuanto un argumento sale del caché de los que ya funcionaron, en vez de inventarse.
    "prob_cache": 0.35,
    "cache_por_tipo": 60,
    "cache_largo_max": 40,
    # Caracteres donde viven los errores de una implementación de texto. Cada tupla es lo que
    # ocupa un solo carácter escrito, que a veces son dos `char`.
    "bordes_texto": [
        [0], [9], [10], [32], [160], [8195], [12288], [65279],
        [97], [65], [122], [90], [48],
        [223], [7838],
        [304], [305],
        [233], [769],
        [931], [963], [962],
        [453],
        [8486],
        [64259],
        [55296], [56320], [65535],
        [55357, 56832],
        [55297, 56320],
        [55297, 56360]
    ],
    # Lo que no se puede comparar entre dos VM porque no depende de la biblioteca.
    "exclusiones": {
        "por_nombre": ["getClass", "wait", "notify", "notifyAll", "intern", "identityHashCode",
                       "currentTimeMillis", "nanoTime", "gc", "exit", "halt", "arraycopy",
                       "finalize", "clone", "describeConstable", "resolveConstantDesc"],
        "por_miembro": [
            "java.lang.String.formatted", "java.lang.String.format",
            "java.math.BigInteger.<init>(ILjava/util/Random;)V",
            "java.math.BigInteger.<init>(IILjava/util/Random;)V"
        ],
        "por_retorno": ["Ljava/util/stream/Stream;", "Ljava/util/stream/IntStream;",
                        "Ljava/util/Iterator;", "Ljava/util/Spliterator;",
                        "Ljava/util/Optional;", "Ljava/lang/Class;"]
    },
    # Expresiones literales para tipos que no se pueden escribir de otra forma y hacen falta
    # como argumento. Es la puerta para crecer sin tocar el código.
    "fabricas": {
        "Ljava/lang/Object;": ["s(97)", "java.math.BigInteger.ONE"],
        "Ljava/lang/CharSequence;": ["__cadena__"],
        "Ljava/lang/Comparable;": ["s(97)"]
    }
}


def carga_config(ruta):
    cfg = json.loads(json.dumps(CONFIG_POR_DEFECTO))
    if ruta and Path(ruta).exists():
        puesto = json.loads(Path(ruta).read_text(encoding="utf-8"))
        for k, v in puesto.items():
            if isinstance(v, dict) and isinstance(cfg.get(k), dict):
                cfg[k].update(v)
            else:
                cfg[k] = v
    return cfg


# =============================================================================================
# generar
# =============================================================================================
class Mundo(object):
    """Lo que se descubrió: qué operaciones hay y qué tipo produce cada una."""

    def __init__(self, ops, cfg, infos):
        self.cfg = cfg
        self.infos = infos
        self.ops = ops
        self.por_retorno = {}
        for o in ops:
            t = ("L" + o["clase"].replace(".", "/") + ";") if o["ctor"] else o["ret"]
            self.por_retorno.setdefault(t, []).append(o)
        # Observable = se puede comparar entre dos implementaciones distintas. La primera
        # version decia "cualquier clase que declare su propio toString()", y eso es demasiado
        # flojo: `toString()` solo es comparable cuando la especificacion dice que texto tiene
        # que dar. Si no lo dice, dos implementaciones pueden devolver cosas distintas y las dos
        # estar bien --y nosotros lo contamos como error--. Nos paso con los arreglos, donde
        # `toString()` es la identidad del objeto: dos corridas daban `@232204a1` y `@76044dfb`
        # y eso ensucio una campana entera. Mica lo dice en una linea: comparar solo en tipos
        # CONCRETOS, porque las representaciones internas pueden diferir arbitrariamente.
        #
        # Asi que ahora hacen falta las dos cosas: que la clase declare `toString()` **y** que
        # este en `tipos_valor`, la lista de clases cuyo texto la especificacion fija.
        self.con_toString = {"L" + i["clase"].replace(".", "/") + ";"
                             for i in infos if i.get("declara_toString")}
        self.tipos_valor = {"L" + c.replace(".", "/") + ";"
                            for c in cfg.get("tipos_valor", [])}

    def observable(self, t):
        if t == "V":
            return False
        if t in PRIMS:
            return True
        if t == "Ljava/lang/String;":
            return True
        if t.startswith("["):
            return self.observable(t[1:])
        return t in self.con_toString and t in self.tipos_valor


def literal(t, r, cfg):
    """Una expresión literal del tipo `t`, o None si no hay semilla para ese tipo."""
    sem = cfg["semillas"]
    if t == "Ljava/lang/String;":
        return cadena(r, cfg)
    if t in ("Z",):
        return "true" if r.choice(sem["Z"]) else "false"
    if t in ("I",):
        return str(r.choice(sem["I"]))
    if t in ("J",):
        return str(r.choice(sem["J"])) + "L"
    if t in ("S",):
        return "(short) " + str(r.choice(sem["S"]))
    if t in ("B",):
        return "(byte) " + str(r.choice(sem["B"]))
    if t in ("C",):
        return "(char) " + str(r.choice(sem["C"]))
    if t in ("F", "D"):
        return str(r.choice(sem[t]))
    if t.startswith("["):
        n = r.randint(0, 4)
        elems = [literal(t[1:], r, cfg) for _ in range(n)]
        if any(e is None for e in elems):
            return None
        return "new " + java_tipo(t[1:]) + "[] {" + ", ".join(elems) + "}"
    return None


def escapar(t):
    """El literal Java de una cadena ASCII, para las semillas que se leen mejor escritas."""
    out = []
    for ch in t:
        if ch == chr(92):
            out.append(chr(92) * 2)
        elif ch == '"':
            out.append(chr(92) + '"')
        elif ch == chr(9):
            out.append(chr(92) + "t")
        elif ch == chr(10):
            out.append(chr(92) + "n")
        elif 32 <= ord(ch) < 127:
            out.append(ch)
        else:
            out.append(chr(92) + "u%04x" % ord(ch))
    return '"' + "".join(out) + '"'


def cadena(r, cfg):
    """Una cadena escrita como `s(codigos...)`, para que el fuente quede en ASCII puro."""
    if FOCO is not None and r.random() < cfg.get("prob_foco", 0.85):
        return palabra_con(FOCO, r, cfg)
    utiles = cfg.get("cadenas_utiles") or []
    if utiles and r.random() < cfg.get("prob_cadena_util", 0.3):
        return escapar(r.choice(utiles))
    rangos = cfg.get("rangos_palabra") or []
    if rangos and r.random() < cfg.get("prob_palabra", 0.25):
        lo, hi = r.choice(rangos)
        n = r.randint(1, 7)
        cs = [r.randint(lo, hi) for _ in range(n)]
        if r.random() < 0.3:                       # a veces, dos palabras separadas
            cs = cs + [32] + [r.randint(lo, hi) for _ in range(r.randint(1, 5))]
        return "s(" + ", ".join(str(c) for c in cs) + ")"
    bordes = cfg["bordes_texto"]
    n = r.randint(0, 8) if r.random() < 0.85 else r.randint(9, 12)
    cs = []
    while len(cs) < n:
        x = r.random()
        if x < 0.45:
            cs.extend(r.choice(bordes))
        elif x < 0.80:
            cs.append(r.randint(0x20, 0x7E))
        elif x < 0.92:
            cs.append(r.randint(0, 0xFFFF))
        else:
            cp = r.randint(0x10000, 0x10FFFF) - 0x10000
            cs.extend([0xD800 + (cp >> 10), 0xDC00 + (cp & 0x3FF)])
    return "s(" + ", ".join(str(c) for c in cs) + ")"


# El caracter "interesante" que le toca por turno al caso que se esta generando. Cubrir las
# operaciones por turno no alcanza: `toLowerCase` recibe su cuota de casos, pero si el argumento
# sale del pozo general, la probabilidad de que sea una palabra griega terminada en sigma es del
# orden de 1 en 1000 y la regla dependiente del contexto no se ejercita nunca. Los fuzzers a mano
# no tenian este problema porque su vocabulario chico concentraba la probabilidad: en `strfuzz`,
# `lower` era 1 de 6 operaciones unarias y el 45% de los caracteres salia de una lista de 29. Lo
# que aca hay que recuperar a proposito es esa densidad, cruzando cada operacion con cada valor.
FOCO = None

# Los argumentos que ya demostraron servir, por tipo. Un fuzzer generico no sabe que
# `new BigInteger(String)` quiere digitos: lo descubre cuando una llamada **no** tira excepcion,
# y a partir de ahi conviene reusar ese valor. Sin esto hubo que escribirle a mano un pozo de
# cadenas numericas, porque con texto Unicode al azar la clase entera quedaba sin ejercitar y la
# campana no tenia como avisarlo. Es la idea de DeepREL --prestarle a una API los argumentos que
# le sirvieron a otra-- y el cache de argumentos efectivos de Hopper.
CACHE = {}
# Los literales que uso el caso que se esta generando, para poder premiarlos si sale bien.
USADOS = []
# Solo se recolecta mientras se GENERA. El reductor tambien pide literales, y los suyos estan
# elegidos para achicar el caso, no porque sirvan.
RECOLECTAR = False


GRANDE = re.compile(r"\d{10,}")


def recordar(tipo, expr, cfg):
    # **No se cachean los valores enormes.** El cache existe para aprender la FORMA que una API
    # acepta --que `new BigInteger(String)` quiere digitos-- y no para repetir el valor extremo
    # que una vez paso. Al principio no estaba esta guarda y se vio enseguida: el lote 0 tardaba
    # 91 segundos y el lote 1 no terminaba en ocho minutos, porque al reusar argumentos validos
    # las llamadas dejaban de fallar al instante y empezaban a ejecutarse de verdad --un
    # `repeat(2147483647)` o un `pow` sobre un entero gigante cuestan el tope entero--. Hopper
    # llega a la misma guarda por el otro lado, aprendiendo un rango maximo cuando un caso se
    # pasa de tiempo. Los valores extremos siguen entrando por las semillas, que es donde deben
    # estar: se prueban, pero no se multiplican.
    if len(expr) > cfg.get("cache_largo_max", 40) or GRANDE.search(expr):
        return
    pozo = CACHE.setdefault(tipo, [])
    if expr in pozo:
        return
    pozo.append(expr)
    tope = cfg.get("cache_por_tipo", 60)
    if len(pozo) > tope:
        # Se va el mas viejo: el cache tiene que seguir a lo que la campana esta encontrando
        # ahora, no acumular todo lo que alguna vez anduvo.
        pozo.pop(0)


def premiar(cfg, lits):
    for t, e in lits:
        recordar(t, e, cfg)


def palabra_con(foco, r, cfg):
    """Una palabra de un solo alfabeto con `foco` metido adentro, casi siempre al final."""
    rangos = cfg.get("rangos_palabra") or [[97, 122]]
    lo, hi = r.choice(rangos)
    n = r.randint(1, 4)
    cs = [r.randint(lo, hi) for _ in range(n)]
    x = r.random()
    if x < 0.55:
        cs = cs + list(foco)                      # al final: donde viven las reglas de cierre
    elif x < 0.85:
        cs = cs[:n // 2] + list(foco) + cs[n // 2:]
    else:
        cs = list(foco) + cs
    return "s(" + ", ".join(str(c) for c in cs) + ")"


ACEPTA = {
    "Ljava/lang/Object;": lambda t: es_ref(t),
    "Ljava/lang/CharSequence;": lambda t: t in ("Ljava/lang/String;",
                                                "Ljava/lang/StringBuilder;"),
}


def compatible(quiere, tiene):
    if quiere == tiene:
        return True
    f = ACEPTA.get(quiere)
    return bool(f and f(tiene))


def arg(t, r, mundo, vars_, prof):
    """Una expresión del tipo `t`: una variable viva, un literal, o una fábrica. None si no hay."""
    cands = [i for i, (vt, _) in enumerate(vars_) if compatible(t, vt)]
    if cands and r.random() < 0.6:
        return "v" + str(r.choice(cands))
    pozo = CACHE.get(t)
    if pozo and r.random() < mundo.cfg.get("prob_cache", 0.35):
        e = r.choice(pozo)
        if RECOLECTAR:
            USADOS.append((t, e))
        return e
    lit = literal(t, r, mundo.cfg)
    if lit is not None:
        if RECOLECTAR:
            USADOS.append((t, lit))
        return lit
    fab = mundo.cfg["fabricas"].get(t)
    if fab:
        e = r.choice(fab)
        return cadena(r, mundo.cfg) if e == "__cadena__" else e
    if cands:
        return "v" + str(r.choice(cands))
    if prof > 0:
        for o in r.sample(mundo.por_retorno.get(t, []), min(3, len(mundo.por_retorno.get(t, [])))):
            e = llamada(o, r, mundo, vars_, prof - 1)
            if e is not None:
                return e
    return None


def llamada(o, r, mundo, vars_, prof):
    """La expresión que invoca `o`, o None si algún argumento no se pudo fabricar."""
    args = []
    for t in o["args"]:
        e = arg(t, r, mundo, vars_, prof)
        if e is None:
            return None
        args.append(e)
    lista = ", ".join(args)
    if o["ctor"]:
        return "new " + o["clase"] + "(" + lista + ")"
    if o["estatico"]:
        return o["clase"] + "." + o["nombre"] + "(" + lista + ")"
    recv = arg("L" + o["clase"].replace(".", "/") + ";", r, mundo, vars_, prof)
    if recv is None:
        return None
    return recv + "." + o["nombre"] + "(" + lista + ")"


FIRMA_NUM = re.compile(r"-?\d+")


def firma_caso(vars_):
    """El esqueleto del caso, con los numeros borrados. Dos casos con la misma firma prueban lo
    mismo con otras constantes, y gastar el presupuesto en el segundo es tirarlo: el estudio de
    semillas iniciales para fuzzing de JVM mide entre 142% y 269% de mejora sólo por sacarle la
    redundancia al conjunto de semillas."""
    return tuple(FIRMA_NUM.sub("#", e) for _, e in vars_)


def gen_caso(r, mundo, largo, objetivo=None):
    """Una secuencia: lista de (tipo, expresión). El tipo `V` es una llamada sin resultado.

    `objetivo` es la operación que este caso tiene que ejercitar sí o sí. Sin eso la elección es
    uniforme sobre todas las operaciones descubiertas, y entonces **agregar una clase diluye a
    todas las demás**: con 130 operaciones y 2400 casos de largo 2, a cada una le tocan unas 37
    apariciones, repartidas entre todos los argumentos posibles. Una regla que depende del
    contexto --la sigma final-- necesita una cadena griega que termine en sigma Y que encima le
    toque a `toLowerCase`, y el producto de las dos probabilidades no sale nunca. Por turno,
    cada operación tiene su cuota garantizada.
    """
    vars_ = []
    intentos = 0
    while len(vars_) < largo and intentos < largo * 12:
        intentos += 1
        # El objetivo va ULTIMO: así puede consumir lo que construyeron las anteriores.
        falta = largo - len(vars_)
        o = objetivo if (objetivo is not None and falta == 1) else r.choice(mundo.ops)
        e = llamada(o, r, mundo, vars_, 1)
        if e is None:
            if o is objetivo and intentos > largo * 6:
                break
            continue
        t = ("L" + o["clase"].replace(".", "/") + ";") if o["ctor"] else o["ret"]
        vars_.append((t, e))
    return vars_


# =============================================================================================
# el programa que se corre de los dos lados
# =============================================================================================
AYUDAS = r"""
  static String s(int... c) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < c.length; i++) { b.append((char) c[i]); }
    return b.toString();
  }
  static String plano(String x) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < x.length(); i++) {
      char ch = x.charAt(i);
      b.append(ch == '\n' || ch == '\r' ? ' ' : ch);
    }
    return b.toString();
  }
  static String enc(String v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("S[");
    for (int i = 0; i < v.length(); i++) {
      if (i > 0) { b.append(','); }
      b.append((int) v.charAt(i));
    }
    return b.append(']').toString();
  }
  static String enc(boolean v) { return String.valueOf(v); }
  static String enc(byte v) { return String.valueOf(v); }
  static String enc(short v) { return String.valueOf(v); }
  static String enc(int v) { return String.valueOf(v); }
  static String enc(long v) { return String.valueOf(v); }
  static String enc(char v) { return "c" + (int) v; }
  static String enc(float v) { return "f" + Float.toString(v) + "/" + Float.floatToRawIntBits(v); }
  static String enc(double v) { return "d" + Double.toString(v) + "/" + Double.doubleToRawLongBits(v); }
  static String enc(Object v) { return v == null ? "null" : "T[" + plano(v.toString()) + "]"; }
  static String enc(boolean[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(byte[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(short[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(int[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(long[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(char[] v) { return v == null ? "null" : enc(new String(v)); }
  static String enc(float[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(double[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(Object[] v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("A[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) { b.append(';'); }
      b.append(enc(v[i]));
    }
    return b.append(']').toString();
  }
  static String enc(String[] v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("A[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) { b.append(';'); }
      b.append(enc(v[i]));
    }
    return b.append(']').toString();
  }
  static String arr(String v) { return v == null ? "null" : "A" + v; }
"""


def render_caso(vars_, mundo):
    cuerpo = []
    obs = []
    for i, (t, e) in enumerate(vars_):
        if t == "V":
            cuerpo.append("      " + e + ";")
        else:
            cuerpo.append("      " + java_tipo(t) + " v" + str(i) + " = " + e + ";")
            if mundo.observable(t):
                obs.append("v" + str(i))
    if not obs:
        return None
    ret = ' + "|" + '.join("enc(" + v + ")" for v in obs)
    return cuerpo, ret


def programa(nombre, casos, mundo):
    ls = ["public class " + nombre + " {", AYUDAS]
    vivos = []
    for k, vars_ in enumerate(casos):
        hecho = render_caso(vars_, mundo)
        if hecho is None:
            continue
        cuerpo, ret = hecho
        vivos.append(k)
        ls.append("  static String e" + str(k) + "() {")
        ls.append("    try {")
        ls.extend(cuerpo)
        ls.append("      return " + ret + ";")
        ls.append("    } catch (Throwable t) {")
        ls.append('      return "EXC:" + t.getClass().getName();')
        ls.append("    }")
        ls.append("  }")
    ls.append("  static int run() {")
    for k in vivos:
        ls.append('    System.out.println("' + str(k) + '=" + e' + str(k) + "());")
    ls.append("    return " + str(len(vivos)) + ";")
    ls.append("  }")
    ls.append("  public static void main(String[] a) { System.out.println(run()); }")
    ls.append("}")
    return "\n".join(ls) + "\n", vivos


# =============================================================================================
# ejecutar
# =============================================================================================
LINEA = re.compile(r"^(\d+)=(.*)$")


def leer(out):
    got = {}
    for ln in out.splitlines():
        m = LINEA.match(ln.rstrip("\r"))
        if m:
            got[int(m.group(1))] = m.group(2)
    return got


def correr(nombre, casos, mundo, tope):
    d = WORK / nombre
    d.mkdir(parents=True, exist_ok=True)
    src = d / (nombre + ".java")
    texto, vivos = programa(nombre, casos, mundo)
    src.write_text(texto, encoding="utf-8")
    c = subprocess.run([str(JAVAC), "-nowarn", "-d", str(d), str(src)], capture_output=True,
                       text=True, encoding="utf-8", errors="replace", timeout=600)
    if c.returncode != 0:
        return None, None, {"javac": c.stderr[-4000:]}
    t0 = time.time()
    jdk = subprocess.run([str(JAVA), "-cp", str(d), nombre], capture_output=True, text=True,
                         encoding="utf-8", errors="replace", timeout=tope)
    t1 = time.time()
    try:
        cmd = [str(HEADLESS), str(d / (nombre + ".class")), "run"]
        if BOOT:
            cmd += ["--boot", str(BOOT)]
        n = subprocess.run(cmd, cwd=str(REPO), capture_output=True, text=True, encoding="utf-8",
                           errors="replace", timeout=tope)
        n_out, n_code, n_err = n.stdout, n.returncode, n.stderr
    except subprocess.TimeoutExpired as e:
        dec = lambda b: b.decode("utf-8", "replace") if isinstance(b, bytes) else (b or "")
        n_out, n_code, n_err = dec(e.stdout), "timeout", dec(e.stderr)
    t2 = time.time()
    # Nuestra VM escribe `System.out` en stderr y el JDK real en stdout: se leen los dos flujos.
    cola = "\n".join(l for l in n_err.splitlines()
                     if not l.startswith("[gc]") and not LINEA.match(l))[-600:]
    return leer(jdk.stdout), leer(n_out + "\n" + n_err), {
        "jdk_s": t1 - t0, "kaji_s": t2 - t1, "salida_kaji": n_code, "stderr_kaji": cola,
        "vivos": vivos}


MINIMO = 1          # hasta que tamano de grupo se parte; lo fija --minimo
MAX_PARTICIONES = 6    # cuantas corridas extra puede gastar la biseccion en UN lote
_particiones = [0]


def correr_partido(nombre, casos, mundo, tope, idx=None, minimo=None):
    """Corre el lote y, si nuestra VM se murio a mitad, parte en dos y reintenta.

    Un solo caso que cuelga o revienta la VM se lleva puesto todo el lote, y sin esto las 119
    expresiones que venian atras se pierden --o peor, se cuentan como "sin salida" y aparecen
    como 119 divergencias que son una sola.

    Se parte sobre una LISTA DE INDICES y no sobre la lista de casos: la primera version dejaba
    la lista del mismo largo rellenando con casos vacios, asi que `len(casos)` nunca bajaba y la
    recursion no terminaba nunca.
    """
    if minimo is None:
        minimo = MINIMO
    if idx is None:
        # **El costo de la biseccion tiene que estar acotado.** Sin techo, un lote con muchos
        # casos que cuelgan gasta una corrida de 30 s por lado en cada particion y no termina
        # mas: en el banco, la semilla 1002 tardo 474 s en un lote de 400 donde las otras
        # tardaban 55. Lo que se pierde al cortar es precision --algunos vecinos del que mata
        # la VM quedan marcados "sin salida" sin serlo-- y eso el banco ya lo descarta, porque
        # sin salida no hay posicion que comparar.
        idx = list(range(len(casos)))
        _particiones[0] = 0
    vivos_idx = set(idx)
    sub = [casos[i] if i in vivos_idx else [] for i in range(len(casos))]
    jdk, kaji, info = correr(nombre, sub, mundo, tope)
    if jdk is None:
        # javac rechazo el lote. Se parte igual: asi se aisla y se tira el caso mal generado en
        # vez de perder los otros 149. Un generador generico siempre va a encontrar una esquina
        # nueva del lenguaje, y perder el lote entero cada vez deja la campana en cero.
        if len(idx) <= 1:
            return {}, {}, {"jdk_s": 0.0, "kaji_s": 0.0, "salida_kaji": "javac", "vivos": [],
                            "stderr_kaji": "", "javac": info.get("javac", "")[:600]}
        mitad = len(idx) // 2
        jdk, kaji, info = {}, {}, {"jdk_s": 0.0, "kaji_s": 0.0, "salida_kaji": 0, "vivos": [],
                                   "stderr_kaji": ""}
        for sufijo, parte in (("a", idx[:mitad]), ("b", idx[mitad:])):
            if not parte:
                continue
            j2, k2, i2 = correr_partido(nombre + sufijo, casos, mundo, tope, parte, minimo)
            jdk.update(j2 or {})
            kaji.update(k2 or {})
            info["vivos"] = info["vivos"] + i2.get("vivos", [])
            info["jdk_s"] += i2.get("jdk_s", 0.0)
            info["kaji_s"] += i2.get("kaji_s", 0.0)
        return jdk, kaji, info
    faltan = [k for k in info["vivos"] if k in jdk and k not in kaji]
    if not faltan or len(idx) <= minimo or _particiones[0] >= MAX_PARTICIONES:
        return jdk, kaji, info
    _particiones[0] += 1
    mitad = len(idx) // 2
    for sufijo, parte in (("a", idx[:mitad]), ("b", idx[mitad:])):
        if not parte:
            continue
        j2, k2, _ = correr_partido(nombre + sufijo, casos, mundo, tope, parte, minimo)
        if j2 is not None:
            jdk.update(j2)
            kaji.update(k2)
    return jdk, kaji, info


# =============================================================================================
# oráculo
# =============================================================================================
FALTA = {"java.lang.NoSuchMethodError", "java.lang.AbstractMethodError",
         "java.lang.UnsatisfiedLinkError", "java.lang.NoClassDefFoundError"}

# Quien hereda de quien, para las excepciones que aparecen. Lanzar una SUBCLASE de la que pide
# la especificacion **cumple** la especificacion: `charAt` promete IndexOutOfBoundsException y
# una ArrayIndexOutOfBoundsException lo es. Sin esta tabla, 126 de 387 divergencias de una
# campana eran ese par, que es ruido de prioridad baja tapando lo que importa. Al reves --el
# JDK lanza la especifica y nosotros la general-- si es una diferencia observable.
PADRES = {
    "java.lang.ArrayIndexOutOfBoundsException": "java.lang.IndexOutOfBoundsException",
    "java.lang.StringIndexOutOfBoundsException": "java.lang.IndexOutOfBoundsException",
    "java.lang.IndexOutOfBoundsException": "java.lang.RuntimeException",
    "java.lang.NumberFormatException": "java.lang.IllegalArgumentException",
    "java.lang.IllegalArgumentException": "java.lang.RuntimeException",
    "java.lang.NegativeArraySizeException": "java.lang.RuntimeException",
    "java.lang.ArithmeticException": "java.lang.RuntimeException",
    "java.lang.NullPointerException": "java.lang.RuntimeException",
    "java.lang.ClassCastException": "java.lang.RuntimeException",
    "java.lang.UnsupportedOperationException": "java.lang.RuntimeException",
    "java.lang.RuntimeException": "java.lang.Exception",
    "java.io.UnsupportedEncodingException": "java.io.IOException",
    "java.io.IOException": "java.lang.Exception",
}


def desciende_de(hijo, padre):
    visto = set()
    while hijo and hijo not in visto:
        if hijo == padre:
            return True
        visto.add(hijo)
        hijo = PADRES.get(hijo)
    return False


def clasificar(jdk, kaji):
    if jdk == kaji:
        return None
    if kaji is None:
        return "sin salida (crash o cuelgue)"
    if kaji.startswith("EXC:") and kaji[4:] in FALTA:
        return "falta en la biblioteca"
    if jdk.startswith("EXC:") and kaji.startswith("EXC:"):
        if desciende_de(kaji[4:], jdk[4:]):
            return "excepción más específica"
        return "otra excepción"
    if jdk.startswith("EXC:") != kaji.startswith("EXC:"):
        return "excepción contra valor"
    return "otro valor"


# =============================================================================================
# reducir
# =============================================================================================
def tam(vars_):
    return sum(1000 + len(e) for _, e in vars_)


def achicar(vars_, r, mundo):
    """Secuencias más chicas que `vars_`: sacar una declaración, o simplificar una expresión."""
    out = []
    for i in range(len(vars_)):
        # Sacar la declaración i, si nadie de más abajo la nombra.
        usada = re.compile(r"\bv" + str(i) + r"\b")
        if not any(usada.search(e) for _, e in vars_[i + 1:]):
            out.append(vars_[:i] + vars_[i + 1:])
    for i, (t, e) in enumerate(vars_):
        # Reemplazar la expresión por un literal del mismo tipo: corta el árbol de una.
        for _ in range(4):
            lit = literal(t, r, mundo.cfg)
            if lit is not None and len(lit) < len(e):
                out.append(vars_[:i] + [(t, lit)] + vars_[i + 1:])
        # Achicar las cadenas y los enteros que aparecen adentro.
        for e2 in simplificar(e):
            if e2 != e:
                out.append(vars_[:i] + [(t, e2)] + vars_[i + 1:])
    return out


CAD = re.compile(r"s\(([-\d,\s]*)\)")
ENT = re.compile(r"(?<![\w.])(-?\d{2,})(?![\w.])")


def simplificar(e):
    """Variantes de la expresión con menos caracteres adentro y números más chicos."""
    out = []
    for m in CAD.finditer(e):
        cs = [c.strip() for c in m.group(1).split(",") if c.strip()]
        for menos in (cs[:len(cs) // 2], cs[len(cs) // 2:], cs[:-1], cs[1:]):
            if len(menos) < len(cs):
                out.append(e[:m.start()] + "s(" + ", ".join(menos) + ")" + e[m.end():])
    for m in ENT.finditer(e):
        v = int(m.group(1))
        for menor in (0, 1, -1, v // 2):
            if abs(menor) < abs(v):
                out.append(e[:m.start()] + str(menor) + e[m.end():])
    return out


def reducir(vars_, tipo, mundo, r, tag, tope, rondas=25):
    cur, pasos = vars_, []
    for ronda in range(rondas):
        cands = [c for c in achicar(cur, r, mundo) if c and tam(c) < tam(cur)]
        cands = sorted(cands, key=tam)[:120]
        if not cands:
            break
        jdk, kaji, info = correr("R" + tag + "x" + str(ronda), cands, mundo, tope)
        if jdk is None:
            break
        ok = [i for i in info["vivos"] if i in jdk and clasificar(jdk[i], kaji.get(i)) == tipo]
        if not ok:
            break
        i = min(ok, key=lambda k: tam(cands[k]))
        pasos.append({"java": texto_caso(cands[i]), "jdk": jdk[i], "kaji": kaji.get(i)})
        cur = cands[i]
    jdk, kaji, info = correr("R" + tag + "fin", [cur], mundo, tope)
    return cur, pasos, (jdk or {}).get(0), (kaji or {}).get(0)


def texto_caso(vars_):
    out = []
    for i, (t, e) in enumerate(vars_):
        out.append(e + ";" if t == "V" else java_tipo(t) + " v" + str(i) + " = " + e + ";")
    return " ".join(out)


# =============================================================================================
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--clases", nargs="+", required=True,
                    help="nombres completos, por ejemplo java.lang.String java.math.BigInteger")
    ap.add_argument("--jdk", required=True, help="carpeta del JDK de referencia")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--lotes", type=int, default=6)
    ap.add_argument("--por-lote", type=int, default=120)
    ap.add_argument("--largo", type=int, default=3, help="declaraciones por caso")
    ap.add_argument("--reducir", type=int, default=8)
    ap.add_argument("--tope", type=int, default=120, help="segundos por lote y por lado")
    ap.add_argument("--config", default=str(Path(__file__).resolve().parent / "genfuzz-config.json"))
    ap.add_argument("--kajilib", default=str(KAJILIB),
                    help="de donde se leen los .class para descubrir la superficie")
    ap.add_argument("--boot", default=None,
                    help="bootclasspath para nuestra VM. Sirve para medir una biblioteca que no "
                         "es la del arbol -- por ejemplo una copia con la version anterior de una "
                         "clase, para comprobar que el fuzzer encuentra un error ya conocido")
    ap.add_argument("--salida", default=None)
    # Interruptores para medir CADA mejora por separado en el banco. Una mejora que no se
    # puede apagar no se puede medir, y una que no se midió es una creencia.
    # Cada nivel de biseccion vuelve a COMPILAR el lote, y ese es el costo que manda. Bajar a 1
    # aisla al caso exacto que mata la VM, pero con decenas de casos que la matan son decenas de
    # compilaciones y la campana se arrastra. Con 4, los vecinos del que crashea quedan marcados
    # como "sin salida" --ruido que el banco descarta igual, porque sin salida no hay posicion
    # que comparar-- y la campana corre varias veces mas rapido.
    ap.add_argument("--max-particiones", type=int, default=6,
                    help="techo de corridas extra que la biseccion puede gastar en un lote")
    ap.add_argument("--minimo", type=int, default=4,
                    help="tamano de grupo por debajo del cual la biseccion deja de partir")
    ap.add_argument("--sin-cache", action="store_true",
                    help="no reusar los argumentos que ya funcionaron")
    ap.add_argument("--sin-dedup", action="store_true",
                    help="no saltear casos con el mismo esqueleto")
    ap.add_argument("--solo-cobertura", action="store_true",
                    help="descubre e informa la superficie, sin generar ni correr nada")
    a = ap.parse_args()

    global JAVAC, JAVA, JAVAP, BOOT
    b = Path(a.jdk) / "bin"
    JAVAC, JAVA, JAVAP = b / "javac.exe", b / "java.exe", b / "javap.exe"
    BOOT = a.boot
    global MINIMO, MAX_PARTICIONES
    MINIMO = a.minimo
    MAX_PARTICIONES = a.max_particiones
    if a.sin_cache:
        cfg["prob_cache"] = 0.0
    cfg = carga_config(a.config)

    ops, infos = [], []
    for clase in a.clases:
        o, info = superficie(clase, a.kajilib)
        infos.append(info)
        if o is None:
            print("AVISO:", info["error"], file=sys.stderr)
            continue
        ops.extend(o)

    excl = cfg["exclusiones"]
    descartadas = {"excluida": [], "retorno_no_comparable": [], "sin_argumentos": []}
    usables = []
    for o in ops:
        clave = o["clase"] + "." + o["nombre"]
        if o["nombre"] in excl["por_nombre"] or clave in excl["por_miembro"] \
                or clave + o["desc"] in excl["por_miembro"]:
            descartadas["excluida"].append(clave + o["desc"])
            continue
        if o["ret"] in excl["por_retorno"]:
            descartadas["retorno_no_comparable"].append(clave + o["desc"])
            continue
        usables.append(o)

    mundo = Mundo(usables, cfg, infos)

    # Qué se puede invocar de verdad: una operación entra sólo si cada uno de sus argumentos se
    # puede fabricar. Esto es lo que dice cuán genérico resultó ser en esta clase.
    r0 = random.Random(0)
    invocables = []
    for o in usables:
        if any(arg(t, r0, mundo, [], 1) is None for t in o["args"]):
            descartadas["sin_argumentos"].append(o["clase"] + "." + o["nombre"] + o["desc"])
        else:
            invocables.append(o)
    mundo.ops = invocables

    cobertura = {
        "clases": infos,
        "operaciones_comunes": len(ops),
        "invocables": len(invocables),
        "descartadas": {k: {"n": len(v), "cuales": sorted(v)} for k, v in descartadas.items()},
    }
    print("superficie:", len(ops), "operaciones comunes,", len(invocables), "invocables", flush=True)
    for k, v in descartadas.items():
        if v:
            print("  descartadas por %-22s %3d" % (k + ":", len(v)), flush=True)
    if a.solo_cobertura:
        print(json.dumps(cobertura, ensure_ascii=False, indent=1))
        return
    if not invocables:
        print("no quedó nada invocable", file=sys.stderr)
        sys.exit(1)

    release = (Path(a.jdk) / "release").read_text(encoding="utf-8", errors="replace")
    campos = {k: v.strip().strip('"')
              for k, v in (ln.split("=", 1) for ln in release.splitlines() if "=" in ln)}
    meta = {
        "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
        "herramienta": "genfuzz.py (genérico, superficie descubierta con javap)",
        "clases": a.clases,
        "referencia": campos.get("IMPLEMENTOR", "?") + " " + campos.get("JAVA_VERSION", "?"),
        "bajo_prueba": "KajiJDK (run-headless) con " + (a.boot or "KajiLibrary"),
        "binario_kaji_fecha": datetime.datetime.fromtimestamp(
            HEADLESS.stat().st_mtime).isoformat(timespec="seconds"),
        "semilla": a.seed, "lotes": a.lotes, "por_lote": a.por_lote, "largo": a.largo,
        "sin_cache": a.sin_cache, "sin_dedup": a.sin_dedup,
    }
    nombre_salida = "gen-" + "-".join(c.rsplit(".", 1)[-1] for c in a.clases) + "-" + str(a.seed)
    salida = Path(a.salida) if a.salida else \
        Path(__file__).resolve().parent.parent / "datos" / (nombre_salida + ".json")
    salida.parent.mkdir(parents=True, exist_ok=True)
    print("referencia:", meta["referencia"], "·", "salida:", salida, flush=True)

    r = random.Random(a.seed)
    stats = {"casos": 0, "coinciden": 0, "divergen": 0, "repetidos": 0, "premiados": 0,
             "por_tipo": {}, "por_op": {}, "ops_total": {},
             "segundos_jdk": 0.0, "segundos_kaji": 0.0}
    lotes, divs, reducidas = [], [], []
    firmas = set()
    t0 = time.time()

    def guardar(estado):
        stats["segundos_total"] = round(time.time() - t0, 1)
        salida.write_text(json.dumps({
            "estado": estado, "meta": meta, "cobertura": cobertura,
            "stats": {k: (round(v, 1) if isinstance(v, float) else v) for k, v in stats.items()},
            "lotes": lotes,
            "divergencias": [{k: v for k, v in d.items() if k != "vars"} for d in divs],
            "reducidas": reducidas,
        }, ensure_ascii=False, indent=2), encoding="utf-8")

    def sumar(t, k):
        t[k] = t.get(k, 0) + 1

    NOMBRES = {o["clase"] + "." + o["nombre"] for o in invocables}

    for lote in range(a.lotes):
        global FOCO, RECOLECTAR
        casos, lits_de = [], {}
        bordes = cfg["bordes_texto"]
        RECOLECTAR = True
        i = reintentos = 0
        while len(casos) < a.por_lote and reintentos < a.por_lote * 4:
            n = lote * a.por_lote + i
            i += 1
            FOCO = tuple(bordes[(n // len(invocables)) % len(bordes)])
            del USADOS[:]
            caso = gen_caso(r, mundo, a.largo, invocables[n % len(invocables)])
            if not caso:
                reintentos += 1
                continue
            f = firma_caso(caso)
            if not a.sin_dedup and f in firmas:
                stats["repetidos"] += 1
                reintentos += 1
                continue
            firmas.add(f)
            lits_de[len(casos)] = list(USADOS)
            casos.append(caso)
        RECOLECTAR = False
        FOCO = None
        jdk, kaji, info = correr_partido("G" + str(a.seed) + "x" + str(lote), casos, mundo, a.tope)
        if jdk is None:
            print("javac rechazó el lote", lote, "-", info.get("javac", "")[:400], flush=True)
            lotes.append({"lote": lote, "javac": info.get("javac", "")[:2000]})
            continue
        stats["segundos_jdk"] += info["jdk_s"]
        stats["segundos_kaji"] += info["kaji_s"]
        lotes.append({"lote": lote, "salida_kaji": info["salida_kaji"],
                      "stderr_kaji": info["stderr_kaji"],
                      "segundos_jdk": round(info["jdk_s"], 2),
                      "segundos_kaji": round(info["kaji_s"], 2)})
        for k in info["vivos"]:
            if k not in jdk:
                continue
            java = texto_caso(casos[k])
            ops_usadas = sorted({m for m in re.findall(r"\.(\w+)\(", java)})
            stats["casos"] += 1
            for op in ops_usadas:
                sumar(stats["ops_total"], op)
            # Si la referencia ejecuto el caso sin lanzar, los argumentos eran validos para
            # esa API: van al cache para que otras los aprovechen.
            if not str(jdk[k]).startswith("EXC:"):
                premiar(cfg, lits_de.get(k, []))
                stats["premiados"] += len(lits_de.get(k, []))
            tipo = clasificar(jdk[k], kaji.get(k))
            if tipo is None:
                stats["coinciden"] += 1
                continue
            stats["divergen"] += 1
            sumar(stats["por_tipo"], tipo)
            for op in ops_usadas:
                sumar(stats["por_op"], op)
            divs.append({"lote": lote, "k": k, "tipo": tipo, "ops": ops_usadas, "vars": casos[k],
                         "java": java, "jdk": jdk[k], "kaji": kaji.get(k)})
        stats["cache"] = {t: len(v) for t, v in sorted(CACHE.items()) if v}
        print("lote %d: %d casos, %d divergencias acumuladas, %d repetidos salteados"
              % (lote, len(info["vivos"]), stats["divergen"], stats["repetidos"]), flush=True)
        guardar("generando")

    vistos = set()
    for i, d in enumerate(divs):
        firma = (d["tipo"], tuple(d["ops"]))
        if firma in vistos or len(reducidas) >= a.reducir:
            continue
        vistos.add(firma)
        print("reduciendo [%s] %s..." % (d["tipo"], d["java"][:90]), flush=True)
        t = time.time()
        minimo, pasos, jv, kv = reducir(d["vars"], d["tipo"], mundo, r,
                                        str(a.seed) + "x" + str(len(reducidas)), a.tope)
        reducidas.append({
            "divergencia": i, "tipo": d["tipo"],
            "original": d["java"], "original_jdk": d["jdk"], "original_kaji": d["kaji"],
            "minimo": texto_caso(minimo), "minimo_jdk": jv, "minimo_kaji": kv,
            "pasos": pasos, "segundos": round(time.time() - t, 1),
        })
        guardar("reduciendo")

    guardar("terminada")
    print(json.dumps({"stats": stats, "reducidas": [
        {k: v for k, v in x.items() if k != "pasos"} for x in reducidas]},
        ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()
