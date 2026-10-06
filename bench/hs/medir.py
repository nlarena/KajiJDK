# -*- coding: utf-8 -*-
"""Mide nuestro motor contra HotSpot sobre EL MISMO `.class`, por pendiente.

Por que por pendiente y no por tiempo total. Una corrida entera incluye el arranque de la VM
--decenas de ms en HotSpot, y lo nuestro tambien levanta la biblioteca-- y el calentamiento de
entrada. Midiendo el mismo workload a N y a 2N repeticiones, la diferencia `t(2N) - t(N)` contiene
**solo** las N repeticiones de mas: el arranque y todo lo que no depende de N se cancelan exacto,
sin tener que estimarlos. La pendiente resultante es ns por iteracion interna, y es comparable entre
motores aunque cada uno necesite un N distinto para tardar lo mismo.

Cada brazo se **calibra solo**: se arranca con un N chico y se duplica hasta que la corrida tarde
`OBJETIVO_S`, porque un brazo interpretado y uno con C2 difieren en dos ordenes de magnitud y usar el
mismo N para los dos dejaria a uno midiendo ruido y al otro esperando minutos.

Las repeticiones van **intercaladas** entre brazos y no en bloque: la maquina se calienta y deriva, y
medir todo un brazo y despues el otro le regala la deriva al segundo. Se reportan medianas y la
dispersion observada, que es el unico piso de ruido legitimo -- el protocolo del proyecto dice que en
esta maquina el ruido de layout llega a +-12%, asi que una diferencia por debajo de la dispersion
medida no sostiene ninguna afirmacion.
"""
import statistics
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
JAVA = Path(r"C:\Program Files\Java\jdk-25\bin\java.exe")
CLASES = REPO / "bench" / "hs"

ITER_INTERNAS = 300   # el lazo de `step`, fijo en la fuente
OBJETIVO_S = 1.2      # cuanto queremos que tarde una corrida al calibrar
REPS = 5              # repeticiones medidas por punto


def corrida(brazo, clase, n):
    """Una corrida del brazo con `n` repeticiones; devuelve segundos de pared."""
    if brazo == "hotspot":
        cmd = [str(JAVA), "-cp", str(CLASES), clase, str(n)]
        env = None
    else:
        cmd = [str(REPO / "bin" / "run-headless.exe"), str(CLASES / f"{clase}.class"), "run", str(n)]
        env = None
        if brazo == "nuestro-sin-jit":
            import os
            env = dict(os.environ, JVM_JIT="0")
    t0 = time.perf_counter()
    r = subprocess.run(cmd, capture_output=True, cwd=REPO, env=env)
    t1 = time.perf_counter()
    if r.returncode != 0:
        raise SystemExit(f"{brazo} fallo: {r.stderr.decode('utf-8', 'replace')[:300]}")
    return t1 - t0


def calibrar(brazo, clase):
    """El N mas chico (duplicando) con el que una corrida alcanza `OBJETIVO_S`."""
    n = 250
    while n < 40_000_000:
        t = corrida(brazo, clase, n)
        if t >= OBJETIVO_S:
            return n, t
        n *= 4
    return n, t


def main():
    clase = sys.argv[1] if len(sys.argv) > 1 else "HsArith"
    brazos = ["hotspot", "nuestro-con-jit", "nuestro-sin-jit"]

    print(f"=== {clase}: calibracion (objetivo {OBJETIVO_S}s por corrida) ===")
    enes = {}
    for b in brazos:
        n, t = calibrar(b, clase)
        enes[b] = n
        print(f"  {b:16} N={n:>10}  ({t:.2f}s)")

    # Medidas intercaladas: en cada vuelta, los tres brazos en N y en 2N.
    print(f"\n=== medidas: {REPS} repeticiones intercaladas, en N y 2N ===")
    bruto = {(b, k): [] for b in brazos for k in (1, 2)}
    for i in range(REPS):
        orden = brazos if i % 2 == 0 else list(reversed(brazos))
        for b in orden:
            for k in (1, 2):
                bruto[(b, k)].append(corrida(b, clase, enes[b] * k))

    print(f"\n{'brazo':16} {'ns/iter':>10} {'disp.':>8}   mediana N / 2N")
    pend = {}
    for b in brazos:
        t1 = statistics.median(bruto[(b, 1)])
        t2 = statistics.median(bruto[(b, 2)])
        n = enes[b]
        # La pendiente: el delta son N repeticiones de mas, cada una de ITER_INTERNAS iteraciones.
        ns_por_iter = (t2 - t1) * 1e9 / (n * ITER_INTERNAS)
        pend[b] = ns_por_iter
        # Dispersion: el rango relativo del punto N, que es el piso de ruido de ESTA tanda.
        xs = bruto[(b, 1)]
        disp = (max(xs) - min(xs)) / statistics.median(xs) * 100
        print(f"{b:16} {ns_por_iter:>10.3f} {disp:>7.1f}%   {t1:.3f}s / {t2:.3f}s")

    print("\n=== razones (sobre la pendiente, o sea sin el arranque) ===")
    hs = pend["hotspot"]
    for b in brazos[1:]:
        print(f"  {b:16} es {pend[b] / hs:>8.1f}x mas lento que HotSpot")
    print(f"  {'y el JIT propio':16} da {pend['nuestro-sin-jit'] / pend['nuestro-con-jit']:>8.1f}x "
          f"sobre nuestro interprete")


if __name__ == "__main__":
    main()
