# -*- coding: utf-8 -*-
"""Banco de prueba de `genfuzz.py` contra errores CONOCIDOS, con muchas semillas.

  python test-fuzzing/fuzzer/banco.py --jdk "$OPENJDK" --vieja /ruta/a/la/biblioteca/anterior

Para qué. Hasta ahora, para decir si un cambio al fuzzer servía, se corría una campaña y se
miraba. Eso no alcanza: con la semilla 11 encontró 4 de las 5 causas conocidas, con la 21
encontró 2, y con la 31 otras 4. La diferencia era el azar, no el fuzzer. Klees et al. lo ponen
primero en su lista de recomendaciones --múltiples ensayos y una medición sobre errores
conocidos, no sobre heurísticas-- y el SoK de 2024 lo repite como Recomendación 2. Mica mide
exactamente así: el **número promedio de casos hasta provocar la falla**, sobre muchas semillas.

Cómo. Tenemos el banco servido: una copia de KajiLibrary anterior a los arreglos, con cinco
errores conocidos y documentados en `datos/diagnostico*.json`. Para cada semilla se corre la
misma campaña dos veces --contra la biblioteca vieja y contra la del árbol-- y una divergencia
cuenta como "detectó el error X" sólo si **desaparece** al cambiar de biblioteca. Es la única
forma honesta: una divergencia que sigue estando después del arreglo no la causaba el arreglo.

La atribución es por POSICIÓN. Un caso de dos declaraciones imprime `enc(v0) + "|" + enc(v1)`,
así que comparando campo por campo se sabe cuál declaración cambió. Buscar el nombre del método
en el texto del caso cuenta como acierto un caso donde el método estaba pero devolvía lo mismo
de los dos lados; ya me mintió dos veces.

Qué informa, por causa: en cuántas semillas se encontró, y en qué número de caso apareció la
primera vez. Un fuzzer mejor es el que sube la primera columna y baja la segunda.
"""
import argparse
import io
import json
import os
import re
import statistics
import shutil
import subprocess
import sys
import hashlib
import time
from pathlib import Path

AQUI = Path(__file__).resolve().parent
GENFUZZ = AQUI / "genfuzz.py"
REPO = AQUI.parent.parent

# El patrón de referencia: qué archivos hay que volver atrás, y hasta dónde, para reconstruir la
# biblioteca con los cinco errores conocidos. **Se reconstruye, no se guarda.** La primera
# versión del banco usaba una copia en el directorio temporal del sistema; diez días después el
# limpiador de Windows se había llevado los 6865 `.class` y había dejado los directorios vacíos,
# así que el banco corría contra una biblioteca sin clases e informaba que no encontraba nada.
# Un banco que se puede evaporar no es un banco.
VIEJA_REF = "50ce81c6"
VIEJA_ARCHIVOS = ["java/lang/String.class", "java/math/BigInteger.class"]


def git(*args):
    p = subprocess.run(["git"] + list(args), cwd=str(REPO), capture_output=True)
    if p.returncode != 0:
        raise RuntimeError("git " + " ".join(args) + ": " + p.stderr.decode("utf-8", "replace"))
    return p.stdout


def preparar_vieja(destino, ref, archivos):
    """Arma (o valida) la biblioteca del banco: KajiLibrary + boot, con `archivos` vueltos a `ref`."""
    destino = Path(destino)
    quiero = {f: git("show", ref + ":KajiLibrary/" + f) for f in archivos}
    ok = destino.exists() and all((destino / f).exists() and (destino / f).read_bytes() == b
                                  for f, b in quiero.items())
    if ok and (destino / "java" / "lang" / "Object.class").exists():
        print("banco: la biblioteca de referencia ya está en", destino, flush=True)
        return str(destino)
    print("banco: construyendo la biblioteca de referencia en", destino, "...", flush=True)
    if destino.exists():
        shutil.rmtree(str(destino))
    shutil.copytree(str(REPO / "KajiLibrary"), str(destino))
    for raiz, _, nombres in os.walk(str(REPO / "boot")):
        rel = os.path.relpath(raiz, str(REPO / "boot"))
        for n in nombres:
            d = destino / rel / n if rel != "." else destino / n
            if not d.exists():                 # KajiLibrary gana, igual que en run-headless
                d.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(os.path.join(raiz, n), str(d))
    for f, b in quiero.items():
        (destino / f).write_bytes(b)
    print("banco: lista (%s vueltos a %s)" % (", ".join(archivos), ref), flush=True)
    return str(destino)

# Las causas conocidas, con el patrón que identifica la DECLARACIÓN que las dispara --no el
# caso entero--. Viven acá y no en el código del fuzzer: son el banco, no la herramienta.
CAUSAS = [
    ("shiftRight", r"\.shift(Right|Left)\("),
    ("testBit", r"\.testBit\("),
    ("plegado", r"(equalsIgnoreCase|compareToIgnoreCase|regionMatches)\("),
    ("indexOf-vacia", r"\.indexOf\("),
    ("sigma-final", r"\.toLowerCase\("),
]


class Presupuesto(Exception):
    """Se acabó el trabajo permitido para esta invocación. No es un error: es la forma de
    terminar ordenado en vez de que nos maten a mitad de una campaña."""


HECHAS = [0]


def correr_campana(a, semilla, boot, salida):
    """Corre una campaña, o devuelve la que ya estaba hecha.

    **La unidad reanudable es la campaña, no la semilla.** Guardar después de cada semilla no
    alcanzó: una semilla son dos campañas y las dos juntas no entran en la ventana de tiempo
    que tiene una corrida en segundo plano, así que el banco perdía las dos y avanzaba cero.
    Con las campañas en disco, cada relanzamiento aprovecha todo lo que ya se calculó.
    """
    if os.path.exists(salida):
        try:
            d = json.load(io.open(salida, encoding="utf-8"))
            if d.get("estado") == "terminada":
                return d
        except Exception:
            pass
    if a.max_campanas and HECHAS[0] >= a.max_campanas:
        raise Presupuesto()
    HECHAS[0] += 1
    cmd = [sys.executable, str(GENFUZZ), "--clases"] + a.clases + [
        "--jdk", a.jdk, "--seed", str(semilla), "--lotes", str(a.lotes),
        "--por-lote", str(a.por_lote), "--largo", str(a.largo), "--reducir", "0",
        "--tope", str(a.tope), "--salida", str(salida)] + list(a.variante)
    if boot:
        cmd += ["--boot", boot, "--kajilib", boot]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if not os.path.exists(salida):
        raise RuntimeError("la campaña no dejó salida:\n" + (p.stderr or p.stdout)[-1500:])
    return json.load(io.open(salida, encoding="utf-8"))


def declaraciones(java):
    return [d.strip() for d in java.split(";") if d.strip()]


def atribuir(x):
    """La causa de lo que efectivamente cambió, o None si no se puede atribuir."""
    if x["kaji"] is None:
        return None                      # la VM murió: no hay posición que comparar
    jdk, kaji = str(x["jdk"]).split("|"), str(x["kaji"]).split("|")
    if len(jdk) != len(kaji):
        return None
    decls = declaraciones(x["java"])
    for i, (dj, dk) in enumerate(zip(jdk, kaji)):
        if dj == dk:
            continue
        d = decls[i] if i < len(decls) else ""
        for nombre, rx in CAUSAS:
            if re.search(rx, d):
                return nombre
        return "(sin causa conocida)"
    return None


def una_semilla(a, semilla, tmp):
    vieja = correr_campana(a, semilla, a.vieja, tmp / ("v%d.json" % semilla))
    nueva = correr_campana(a, semilla, None, tmp / ("n%d.json" % semilla))

    dn = {(x["lote"], x["k"]) for x in nueva["divergencias"]}
    primera = {}
    extra = 0
    for x in vieja["divergencias"]:
        if (x["lote"], x["k"]) in dn:
            continue                     # sigue después del arreglo: no la causaba el arreglo
        causa = atribuir(x)
        if causa is None:
            continue
        if causa == "(sin causa conocida)":
            extra += 1
            continue
        orden = x["lote"] * a.por_lote + x["k"]
        if causa not in primera or orden < primera[causa]:
            primera[causa] = orden
    return {
        "semilla": semilla,
        "casos": vieja["stats"]["casos"],
        "divergen_vieja": vieja["stats"]["divergen"],
        "divergen_nueva": nueva["stats"]["divergen"],
        "repetidos": vieja["stats"].get("repetidos", 0),
        "primera_deteccion": primera,
        "desaparecen_sin_causa": extra,
    }


def firma_corrida(a):
    """Qué hace comparables dos corridas del banco. Si cambia, lo guardado no se puede reusar."""
    return "|".join([",".join(a.clases), str(a.lotes), str(a.por_lote), str(a.largo),
                     str(a.vieja_ref), ",".join(a.variante)])


def resumir(filas):
    out = {}
    for nombre, _ in CAUSAS:
        ords = [f["primera_deteccion"][nombre] for f in filas if nombre in f["primera_deteccion"]]
        out[nombre] = {"semillas_con_deteccion": len(ords), "semillas": len(filas),
                       "casos_hasta_detectar": sorted(ords),
                       "mediana": int(statistics.median(ords)) if ords else None,
                       "peor": max(ords) if ords else None}
    return out


def guardar(a, filas, t0):
    doc = {"meta": {"clases": a.clases, "semillas": a.semillas, "desde": a.desde,
                    "variante": a.variante, "firma": firma_corrida(a),
                    "casos_por_semilla": a.lotes * a.por_lote, "largo": a.largo,
                    "vieja": a.vieja, "vieja_ref": a.vieja_ref,
                    "segundos": round(time.time() - t0, 1)},
           "por_causa": resumir(filas), "por_semilla": filas}
    Path(a.salida).parent.mkdir(parents=True, exist_ok=True)
    io.open(a.salida, "w", encoding="utf-8", newline="").write(
        json.dumps(doc, ensure_ascii=False, indent=2) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", required=True)
    ap.add_argument("--vieja", default=str(REPO / "test-fuzzing" / "banco-vieja"),
                    help="dónde vive la biblioteca de referencia; se construye sola si falta")
    ap.add_argument("--vieja-ref", default=VIEJA_REF,
                    help="commit del que salen los archivos con los errores conocidos")
    ap.add_argument("--clases", nargs="+",
                    default=["java.lang.String", "java.math.BigInteger"])
    ap.add_argument("--semillas", type=int, default=10)
    ap.add_argument("--desde", type=int, default=1000, help="primera semilla")
    ap.add_argument("--lotes", type=int, default=4)
    ap.add_argument("--por-lote", type=int, default=300)
    ap.add_argument("--largo", type=int, default=2)
    ap.add_argument("--tope", type=int, default=90)
    ap.add_argument("--max-campanas", type=int, default=0,
                    help="cuántas campañas correr como máximo en ESTA invocación (0 = sin "
                         "límite). Sirve para que la corrida termine ordenada dentro de una "
                         "ventana de tiempo acotada en vez de que la maten a mitad de camino")
    ap.add_argument("--corridas", default=str(REPO / "test-fuzzing" / "banco-corridas"),
                    help="donde se guardan las campañas ya calculadas, para poder reanudar")
    ap.add_argument("--desde-cero", action="store_true",
                    help="ignorar lo ya guardado y rehacer todas las semillas")
    ap.add_argument("--variante", nargs="*", default=[],
                    help="banderas que se le pasan tal cual a genfuzz, para medir una variante "
                         "contra la base; por ejemplo --variante --sin-cache")
    ap.add_argument("--salida", default=str(AQUI.parent / "datos" / "banco.json"))
    a = ap.parse_args()

    a.vieja = preparar_vieja(a.vieja, a.vieja_ref, VIEJA_ARCHIVOS)
    # Las campañas van a un directorio ESTABLE, nombrado por la configuración: así dos
    # corridas con la misma firma comparten lo ya calculado y una distinta no lo pisa.
    tmp = Path(a.corridas) / hashlib.sha1(firma_corrida(a).encode("utf-8")).hexdigest()[:12]
    tmp.mkdir(parents=True, exist_ok=True)
    t0 = time.time()

    # **Se guarda después de CADA semilla, y se reanuda.** Un banco de 8 semillas son 16
    # campañas y no entra en una sola corrida: la primera versión escribía sólo al final y la
    # mataron antes de terminar la primera semilla, tirando todo. Es la misma decisión que ya
    # estaba tomada en `bigfuzz.py`, y volví a no tomarla.
    filas = []
    if os.path.exists(a.salida) and not a.desde_cero:
        try:
            previo = json.load(io.open(a.salida, encoding="utf-8"))
            if previo.get("meta", {}).get("firma") == firma_corrida(a):
                filas = previo.get("por_semilla", [])
                print("banco: retomando, ya hay %d semillas hechas" % len(filas), flush=True)
            else:
                print("banco: el archivo previo es de otra configuración, se empieza de nuevo",
                      flush=True)
        except Exception as e:
            print("banco: no se pudo leer lo previo (%s), se empieza de nuevo" % e, flush=True)
    hechas = {f["semilla"] for f in filas}

    print("banco: %d semillas x %d casos, clases %s"
          % (a.semillas, a.lotes * a.por_lote, " ".join(a.clases)), flush=True)
    for j in range(a.semillas):
        semilla = a.desde + j
        if semilla in hechas:
            continue
        try:
            f = una_semilla(a, semilla, tmp)
        except Presupuesto:
            print("banco: se acabó el presupuesto de esta corrida (%d campañas). "
                  "Volvé a lanzarlo y sigue donde quedó." % a.max_campanas, flush=True)
            break
        filas.append(f)
        print("  semilla %-5d %5d casos · %3d diverg (vieja) %3d (nueva) · %5.1f min · %s"
              % (semilla, f["casos"], f["divergen_vieja"], f["divergen_nueva"],
                 (time.time() - t0) / 60.0,
                 ", ".join(sorted(f["primera_deteccion"])) or "nada"), flush=True)
        guardar(a, filas, t0)

    print()
    if not filas:
        print("todavía no hay ninguna semilla completa; volvé a lanzarlo")
        return
    print("%d de %d semillas completas" % (len(filas), a.semillas))
    print("%-16s %10s %12s %12s" % ("causa", "semillas", "mediana", "peor"))
    print("-" * 54)
    resumen = resumir(filas)
    for nombre, _ in CAUSAS:
        v = resumen[nombre]
        print("%-16s %10s %12s %12s"
              % (nombre, "%d/%d" % (v["semillas_con_deteccion"], v["semillas"]),
                 v["mediana"] if v["mediana"] is not None else "-",
                 v["peor"] if v["peor"] is not None else "-"))
    print("-" * 54)
    todas = sum(1 for f in filas if len(f["primera_deteccion"]) == len(CAUSAS))
    print("semillas que encontraron LAS CINCO: %d/%d" % (todas, len(filas)))
    print("promedio de causas por semilla: %.1f"
          % (sum(len(f["primera_deteccion"]) for f in filas) / float(len(filas))))
    guardar(a, filas, t0)
    print("escrito", a.salida)


if __name__ == "__main__":
    main()
