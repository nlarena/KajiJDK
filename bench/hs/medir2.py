# -*- coding: utf-8 -*-
"""Mide nuestro motor contra HotSpot sobre EL MISMO `.class`, clasificando antes de resumir.

Reemplaza a `medir.py`, que daba 32-94% de dispersion contra el techo de ruido documentado de esta
maquina (+-12%). Lo que cambia son cuatro cosas, y cada una tapa un agujero distinto:

1. **Se mide adentro del proceso.** `medir.py` usaba un proceso por punto, asi que cada medicion
   arrastraba el arranque de la VM, la carga de la biblioteca y el layout de un proceso distinto. Esa
   varianza entraba entera en el numero. Ahora el workload corre K iteraciones en un solo proceso y
   emite la serie completa.

2. **Se CLASIFICA la serie antes de resumirla.** Tomar la mediana asume estado estacionario, que es
   justo lo que Barrett et al. (OOPSLA 2017) midieron y encontraron falso: a lo sumo 43.5% de los
   pares VM/benchmark llegan consistentemente a pico, ninguna de sus siete VMs lo hizo siempre, y
   algunos benchmarks se vuelven MAS lentos con las iteraciones. Aca se detectan puntos de cambio y
   se clasifica en `flat`, `warmup`, `slowdown` o `sin-estacionario`, y **solo** se resume el ultimo
   segmento.

   El detector es **binary segmentation sobre la media**, no el PELT del paper: se parte donde mas
   baja el costo cuadratico mientras la baja supere una penalidad tipo BIC. Es un sustituto
   simplificado y conviene saberlo -- alcanza para distinguir "bajo y se quedo" de "nunca se quedo",
   que es la decision que necesitamos, y no pretende reproducir su clasificacion.

3. **Intervalos de confianza en vez de un umbral.** Georges et al. (OOPSLA 2007) muestran que las
   practicas corrientes --el mejor de N, el peor, la media pelada-- llevan a conclusiones incorrectas
   porque no dicen si la diferencia vista es ruido. Se reporta media +- IC al 95% sobre las corridas,
   y se **sigue muestreando** hasta que el semiancho del IC baje del objetivo o se agote el tope, que
   es la regla de su `JavaStats` en vez de fijar las repeticiones de antemano.

4. **Las corridas van intercaladas** entre brazos, alternando el orden: la maquina deriva y medir un
   brazo entero y despues el otro le regala la deriva al segundo.

**Que VM se mide.** Por defecto el `bin/run-headless.exe` **congelado**, igual que `tools/suite.py` y
`tools/texto.py`. Un arreglo de la VM que todavia no se congelo **no se ve** con el default: hay que
pasarselo con `--vm target/release/run-headless.exe` (o por `KAJI_RUN`). Esto falto desde el primer
dia y costo una tanda entera: las cinco dimensiones se midieron contra el congelado despues de tres
cambios del JIT, salieron todas planas, y la explicacion que se escribio para ese resultado --que los
workloads no tocan la biblioteca-- era plausible y no era la causa. La causa era que el binario
medido no tenia los cambios. Un arnes que no puede nombrar lo que mide invita exactamente a eso.

Uso:  python bench/hs/medir2.py [clase] [iteraciones] [objetivo_ic_%] [--vm RUTA]
"""
import math
import os
import re
import statistics
import subprocess
import sys
import time
from pathlib import Path

import numpy as np

REPO = Path(__file__).resolve().parents[2]
JAVA = Path(r"C:\Program Files\Java\jdk-25\bin\java.exe")
CLASES = REPO / "bench" / "hs"
CLASE = "HsBench"      # lo pisa el argumento 1; ver `main`
# **Que VM se mide**, con la misma convencion que `tools/texto.py`: `KAJI_RUN` manda, `--vm` la pisa,
# y el default es el congelado. Se imprime en el encabezado de cada corrida, que es la mitad que
# importa: una medicion que no dice que binario midio no se puede leer dos dias despues.
VM = Path(os.environ.get("KAJI_RUN") or (REPO / "bin" / "run-headless.exe"))

ITER_INTERNAS = 300   # el lazo de `step`, fijo en la fuente
MS_POR_ITER = 25        # cuanto queremos que dure UNA iteracion, al calibrar
CORRIDAS_MIN = 6        # corridas (procesos) por brazo antes de mirar el IC
CORRIDAS_MAX = 20       # tope: mas que esto no se paga solo
DESCARTE = 0.0          # no se descarta nada a mano: lo decide la clasificacion

# t de Student al 95% dos colas, por grados de libertad. Sin scipy, y con la tabla a la vista en vez
# de un 1.96 que solo vale para n grande.
T95 = {1: 12.706, 2: 4.303, 3: 3.182, 4: 2.776, 5: 2.571, 6: 2.447, 7: 2.365, 8: 2.306,
       9: 2.262, 10: 2.228, 12: 2.179, 15: 2.131, 20: 2.086, 25: 2.060, 30: 2.042}


def t95(df):
    if df <= 0:
        return float("inf")
    for k in sorted(T95):
        if df <= k:
            return T95[k]
    return 1.96


def brazos():
    return ["hotspot", "nuestro-con-jit", "nuestro-sin-jit"]


def corrida(brazo, iters, reps):
    """Una corrida (un proceso) del brazo; devuelve la serie de nanos por iteracion."""
    if brazo == "hotspot":
        cmd = [str(JAVA), "-cp", str(CLASES), CLASE, str(iters), str(reps)]
        env = None
    else:
        cmd = [str(VM), str(CLASES / f"{CLASE}.class"), "bench", str(iters), str(reps)]
        env = dict(os.environ, JVM_JIT="0") if brazo == "nuestro-sin-jit" else None
    r = subprocess.run(cmd, capture_output=True, cwd=REPO, env=env)
    # Nuestra VM escribe `System.out` a **stderr** e intercala lineas `[gc]`, asi que se leen los dos
    # flujos y se filtra. Es la misma trampa que ya nos mordio en el arnes de fuzzing.
    texto = r.stdout.decode("utf-8", "replace") + "\n" + r.stderr.decode("utf-8", "replace")
    nums = [int(m) for m in re.findall(r"^\s*(\d+)\s*$", texto, re.M)]
    if len(nums) < iters:
        raise SystemExit(f"{brazo}: salieron {len(nums)} numeros, se esperaban >= {iters}\n"
                         f"{texto[:400]}")
    return nums[:iters]   # lo que sigue es el acumulador que `bench` devuelve


def calibrar(brazo):
    """`reps` tal que una iteracion dure ~MS_POR_ITER. Cada brazo necesita el suyo: un interprete y
    C2 difieren en dos ordenes de magnitud, y un `reps` comun deja a uno midiendo ruido de reloj."""
    reps = 50
    while reps < 20_000_000:
        serie = corrida(brazo, 12, reps)
        # La mediana de la segunda mitad: la primera puede estar calentando.
        ms = statistics.median(serie[6:]) / 1e6
        if ms >= MS_POR_ITER:
            return reps, ms
        factor = max(2, min(16, int(MS_POR_ITER / max(ms, 0.05))))
        reps *= factor
    return reps, ms


def puntos_de_cambio(xs, max_cortes=4):
    """Binary segmentation sobre la media. Devuelve los indices de corte, ordenados.

    Costo de un segmento = suma de desvios al cuadrado respecto de su media. Se parte donde mas baja
    el costo total, mientras la baja supere una penalidad `log(n) * var` (forma BIC): sin penalidad,
    partir siempre baja el costo y saldrian cortes en el ruido.
    """
    xs = np.asarray(xs, dtype=float)
    n = len(xs)
    if n < 8:
        return []

    def costo(a, b):
        seg = xs[a:b]
        return float(((seg - seg.mean()) ** 2).sum()) if len(seg) else 0.0

    penal = float(np.log(n) * xs.var()) if xs.var() > 0 else 0.0
    cortes = []
    segmentos = [(0, n)]
    for _ in range(max_cortes):
        mejor = None
        for (a, b) in segmentos:
            if b - a < 8:
                continue
            base = costo(a, b)
            for c in range(a + 4, b - 3):
                baja = base - (costo(a, c) + costo(c, b))
                if baja > penal and (mejor is None or baja > mejor[0]):
                    mejor = (baja, a, b, c)
        if mejor is None:
            break
        _, a, b, c = mejor
        cortes.append(c)
        segmentos.remove((a, b))
        segmentos += [(a, c), (c, b)]
    return sorted(cortes)


def clasificar(xs):
    """`(clase, (ini, fin))` del segmento estacionario, o `(clase, None)` si no hay.

    **Robusto a picos aislados, y esto se aprendio midiendo.** La primera version resumia cada
    segmento por su MEDIA, y con eso un unico pico --una iteracion de 637 ms contra una mediana de
    97-- alcanzaba para que la corrida saliera clasificada `slowdown`. Perseguir eso costo un rato
    hasta descubrir que los picos no eran del motor: con la maquina quieta no se reproducen, y la
    mediana de la propia serie se habia movido 2x entre tandas, que es firma de carga externa y no de
    un motor que se degrada.
    
    Barrett et al. no tienen este problema porque corren 2000 iteraciones, donde un pico pesa nada.
    Con 40-60, hay que resumir por **mediana** y pedirle al desplazamiento que supere unos cuantos
    MAD, no un 5% de la media.
    """
    n = len(xs)
    cortes = puntos_de_cambio(xs)
    bordes = [0] + cortes + [n]
    tramos = [np.asarray(xs[bordes[i]:bordes[i + 1]], dtype=float) for i in range(len(bordes) - 1)]
    medianas = [float(np.median(t)) for t in tramos]
    ultimo = (bordes[-2], bordes[-1])

    if not cortes:
        return "flat", ultimo
    # Un corte en el ultimo 10% de la serie: todavia estaba cambiando cuando se corto de medir.
    if cortes[-1] > n * 0.9:
        return "sin-estacionario", None
    # Un ultimo tramo demasiado corto no sostiene una clasificacion propia: se lo lee como la cola
    # del anterior.
    if (ultimo[1] - ultimo[0]) < max(5, n * 0.1):
        return "flat", (bordes[-3] if len(bordes) >= 3 else 0, n)
    # Degradacion: el ultimo tramo mas lento que el mejor, con margen en MAD del propio ultimo tramo
    # --escala del ruido de la serie, no un porcentaje elegido--.
    mad = float(np.median(np.abs(tramos[-1] - medianas[-1]))) or 1.0
    if medianas[-1] - min(medianas) > 3 * mad:
        return "slowdown", ultimo
    return "warmup", ultimo


# ---------------------------------------------------------------------------------------------
#  ANOVA de una via + Tukey HSD
#
#  Por que no los cocientes pareados que habia antes. Georges et al. (OOPSLA 2007) dedican una
#  seccion a esto: con MAS DE DOS alternativas, comparar de a pares no controla la probabilidad de
#  concluir mal. El ANOVA pregunta si la variacion ENTRE brazos supera la variacion DENTRO de cada
#  brazo; y como el ANOVA dice que hay diferencia pero no entre cuales, hace falta un post-hoc. Ellos
#  usan **Tukey HSD** y dan el motivo: acota esa probabilidad cuando todas las medias son iguales, o
#  cuando casi todas lo son y una o dos difieren -- que es exactamente nuestro caso, con dos brazos
#  parecidos y uno a tres ordenes de magnitud.
#
#  **Se trabaja en escala logaritmica**, y no es un detalle: los tres brazos difieren en ordenes de
#  magnitud (1.2, 3.2 y 868 ns/trabajo), asi que en escala lineal la varianza del brazo lento domina
#  todo y se rompe el supuesto de varianzas iguales del ANOVA. En logaritmo, lo que comparamos son
#  **razones** --que es lo que nos interesa-- y las dispersiones relativas quedan comparables. Las
#  diferencias de medias logaritmicas se exponencian de vuelta a razones al reportar.
#
#  Sin scipy: el p-valor de F sale de la beta incompleta regularizada (fraccion continua clasica), y
#  el valor critico del rango estudentizado de una tabla al 5%, leida de forma **conservadora** --se
#  usa la fila de menos grados de libertad, que da un q mayor y por lo tanto un intervalo mas ancho--.
# ---------------------------------------------------------------------------------------------

def _betacf(a, b, x):
    """Fraccion continua de la beta incompleta (Lentz)."""
    tiny = 1e-30
    qab, qap, qam = a + b, a + 1.0, a - 1.0
    c, d = 1.0, 1.0 - qab * x / qap
    if abs(d) < tiny:
        d = tiny
    d = 1.0 / d
    h = d
    for m in range(1, 300):
        m2 = 2 * m
        aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        if abs(d) < tiny:
            d = tiny
        c = 1.0 + aa / c
        if abs(c) < tiny:
            c = tiny
        d = 1.0 / d
        h *= d * c
        aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa * d
        if abs(d) < tiny:
            d = tiny
        c = 1.0 + aa / c
        if abs(c) < tiny:
            c = tiny
        d = 1.0 / d
        de = d * c
        h *= de
        if abs(de - 1.0) < 3e-12:
            break
    return h


def _betai(a, b, x):
    """Beta incompleta regularizada I_x(a, b)."""
    if x <= 0.0:
        return 0.0
    if x >= 1.0:
        return 1.0
    lb = (math.lgamma(a + b) - math.lgamma(a) - math.lgamma(b)
          + a * math.log(x) + b * math.log1p(-x))
    if x < (a + 1.0) / (a + b + 2.0):
        return math.exp(lb) * _betacf(a, b, x) / a
    return 1.0 - math.exp(lb) * _betacf(b, a, 1.0 - x) / b


def f_p_valor(f, df1, df2):
    """P(F > f) para la F de Snedecor."""
    if f <= 0 or df1 <= 0 or df2 <= 0:
        return 1.0
    return _betai(df2 / 2.0, df1 / 2.0, df2 / (df2 + df1 * f))


# Rango estudentizado al 5%, por numero de grupos k y grados de libertad del error.
Q05 = {
    2: {5: 3.64, 10: 3.15, 15: 3.01, 20: 2.95, 24: 2.92, 30: 2.89, 40: 2.86, 60: 2.83, 120: 2.80},
    3: {5: 4.60, 10: 3.88, 15: 3.67, 20: 3.58, 24: 3.53, 30: 3.49, 40: 3.44, 60: 3.40, 120: 3.36},
    4: {5: 5.22, 10: 4.33, 15: 4.08, 20: 3.96, 24: 3.90, 30: 3.85, 40: 3.79, 60: 3.74, 120: 3.68},
    5: {5: 5.67, 10: 4.65, 15: 4.37, 20: 4.23, 24: 4.17, 30: 4.10, 40: 4.04, 60: 3.98, 120: 3.92},
    6: {5: 6.03, 10: 4.91, 15: 4.59, 20: 4.45, 24: 4.37, 30: 4.30, 40: 4.23, 60: 4.16, 120: 4.10},
}
Q05_INF = {2: 2.77, 3: 3.31, 4: 3.63, 5: 3.86, 6: 4.03}


def q_critico(k, df):
    """Valor critico conservador: la fila de MENOS grados de libertad que no exceda `df`."""
    tabla = Q05.get(min(max(k, 2), 6), Q05[3])
    candidatos = [d for d in sorted(tabla) if d <= df]
    if not candidatos:
        return tabla[min(tabla)]          # menos df que la tabla: el mas ancho que hay
    if df >= 120:
        return Q05_INF.get(min(max(k, 2), 6), 3.31)
    return tabla[candidatos[-1]]


def anova_y_tukey(grupos):
    """`grupos` es {nombre: [valores]}. Devuelve `(anova, comparaciones)` en escala log.

    `anova` = (F, df1, df2, p). Cada comparacion es
    `(a, b, razon, lo, hi, significativa)`, ya exponenciada a razones.
    """
    nombres = [n for n, xs in grupos.items() if len(xs) >= 2]
    datos = {n: [math.log(x) for x in grupos[n]] for n in nombres}
    k = len(nombres)
    if k < 2:
        return None, []
    N = sum(len(datos[n]) for n in nombres)
    gran = sum(sum(datos[n]) for n in nombres) / N
    ssa = sum(len(datos[n]) * (statistics.mean(datos[n]) - gran) ** 2 for n in nombres)
    sse = sum((x - statistics.mean(datos[n])) ** 2 for n in nombres for x in datos[n])
    df1, df2 = k - 1, N - k
    if df2 <= 0 or sse <= 0:
        return None, []
    msa, mse = ssa / df1, sse / df2
    f = msa / mse
    anova = (f, df1, df2, f_p_valor(f, df1, df2))

    q = q_critico(k, df2)
    comps = []
    for i in range(k):
        for j in range(i + 1, k):
            a, b = nombres[i], nombres[j]
            na, nb = len(datos[a]), len(datos[b])
            # Tukey-Kramer: admite n distintos, que es nuestro caso (no todas las corridas dan
            # un segmento estacionario usable).
            hsd = q * math.sqrt(mse / 2.0 * (1.0 / na + 1.0 / nb))
            d = statistics.mean(datos[b]) - statistics.mean(datos[a])
            comps.append((a, b, math.exp(d), math.exp(d - hsd), math.exp(d + hsd),
                          abs(d) > hsd))
    return anova, comps


def main():
    # `medir2.py [clase] [iteraciones] [objetivo_ic_%]`. La clase es cualquiera de las de `bench/hs/`:
    # cada una aisla una dimension distinta y todas tienen la misma forma `bench(iters, reps)`.
    global CLASE, VM
    args = sys.argv[1:]
    # `--vm RUTA` se saca antes del parseo posicional, que es por posicion y no sabe de banderas.
    if "--vm" in args:
        i = args.index("--vm")
        VM = Path(args[i + 1]).resolve()
        del args[i:i + 2]
    if args and not args[0].replace(".", "").isdigit():
        CLASE = args.pop(0)
    iters = int(args[0]) if len(args) > 0 else 200
    objetivo = float(args[1]) if len(args) > 1 else 2.0
    print(f"### dimension: {CLASE} ###")
    print(f"### vm: {VM} ###")

    print(f"=== calibracion: una iteracion ~{MS_POR_ITER} ms, {iters} iteraciones por corrida ===")
    reps = {}
    for b in brazos():
        r, ms = calibrar(b)
        reps[b] = r
        print(f"  {b:16} reps={r:>9}   ({ms:.1f} ms/iter)")

    print(f"\n=== corridas intercaladas, hasta IC95 < {objetivo}% o {CORRIDAS_MAX} corridas ===")
    medias = {b: [] for b in brazos()}     # una media de estacionario por corrida
    clases = {b: [] for b in brazos()}
    i = 0
    while i < CORRIDAS_MAX:
        orden = brazos() if i % 2 == 0 else list(reversed(brazos()))
        for b in orden:
            serie = corrida(b, iters, reps[b])
            clase, seg = clasificar(serie)
            clases[b].append(clase)
            if seg is not None:
                medias[b].append(float(np.mean(serie[seg[0]:seg[1]])))
        i += 1
        if i < CORRIDAS_MIN:
            continue
        anchos = []
        for b in brazos():
            xs = medias[b]
            if len(xs) < 2:
                anchos.append(float("inf"))
                continue
            s = statistics.stdev(xs)
            anchos.append(t95(len(xs) - 1) * s / (len(xs) ** 0.5) / statistics.mean(xs) * 100)
        if max(anchos) < objetivo:
            break
    print(f"  {i} corridas por brazo")

    print(f"\n{'brazo':16} {'ns/trabajo':>12} {'IC95':>9}  {'usables':>8}  clases")
    est = {}
    for b in brazos():
        xs = medias[b]
        cl = clases[b]
        resumen = ", ".join(f"{c}x{cl.count(c)}" for c in sorted(set(cl)))
        if len(xs) < 2:
            print(f"{b:16} {'-':>12} {'-':>9}  {len(xs):>3}/{len(cl):<4} {resumen}")
            continue
        # **Normalizado por el trabajo**, y esto no es cosmetico: la calibracion le da a cada brazo
        # el `reps` que hace que UNA iteracion dure lo mismo, asi que comparar ns por iteracion
        # compara tiempos iguales por construccion -- la primera version de esto daba que nuestro
        # interprete era 0.82x HotSpot, o sea mas rapido, que es imposible y fue como se encontro.
        trabajo = reps[b] * ITER_INTERNAS
        m = statistics.mean(xs) / trabajo
        ic = t95(len(xs) - 1) * statistics.stdev(xs) / (len(xs) ** 0.5) / trabajo
        est[b] = (m, ic)
        print(f"{b:16} {m:>12.4f} {ic / m * 100:>8.2f}%  {len(xs):>3}/{len(cl):<4} {resumen}")

    if "hotspot" not in est:
        print("\nHotSpot no dio ninguna corrida con estacionario: no hay razon que valga.")
        return

    # Los valores por corrida, ya normalizados por el trabajo de su brazo.
    norm = {b: [x / (reps[b] * ITER_INTERNAS) for x in medias[b]]
            for b in brazos() if len(medias[b]) >= 2}
    anova, comps = anova_y_tukey(norm)
    if anova is None:
        print("\nNo hay datos suficientes para el ANOVA.")
        return
    f, df1, df2, p = anova
    print(f"\n=== ANOVA de una via (sobre log del ns/trabajo) ===")
    print(f"  F({df1},{df2}) = {f:.2f}   p = {p:.3g}"
          + ("   -> hay diferencia entre brazos" if p < 0.05
             else "   -> NO se distingue del ruido"))
    if p >= 0.05:
        print("  Sin diferencia global, las comparaciones de a pares no se interpretan.")
        return

    print("\n=== Tukey HSD al 5% (razones, con su intervalo simultaneo) ===")
    for a, b, razon, lo, hi, sig in comps:
        marca = "si" if sig else "NO"
        print(f"  {b:16} / {a:16} {razon:>9.2f}x  ({lo:.2f}x a {hi:.2f}x)  significativa: {marca}")


if __name__ == "__main__":
    main()
