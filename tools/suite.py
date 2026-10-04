"""Corre la bateria de pruebas de comportamiento de `java/` y dice cuales estan rotas.

No existia, y por eso nueve pruebas se pudrieron sin que nadie se enterara: sus `.class` seguian en el
arbol desde una version vieja de la biblioteca, la fuente ya no compilaba contra la actual, y correr
el `.class` daba un `NoSuchMethodError` que parecia un fallo de la VM.

Dos decisiones que valen la explicacion:

**Compila antes de correr.** Un `.class` que quedo en el arbol no prueba nada sobre la fuente que
esta al lado. La mitad del valor de esta herramienta es que un test que dejo de compilar aparece como
lo que es --roto-- y no como un resultado raro en tiempo de ejecucion.

**OJO con `--javac`: compilar deja los `.class` en `java/`, que estan VERSIONADOS.** Correr la suite con
un javac que no es el congelado los sobreescribe con la salida de otro compilador, y eso rompe el
invariante del arbol (un `.class` distinto tiene que significar "cambio la fuente"). Paso el 2026-10-03
midiendo un arreglo: 71 archivos. Se restauran con `git checkout -- java/`, porque estan commiteados; y
despues de medir con `--javac` conviene mirar `git status java/`.

**Una prueba que tira es una prueba rota.** La VM sale con un codigo != 0 cuando una excepcion
escapa del hilo principal, y hasta que esta herramienta lo miro una prueba que tiraba se reportaba
como buena: no devolvia valor, no imprimia su `TOTAL`, y eso se leia como "esta prueba no tiene
veredicto". `VhTest` --y con ella todo el camino de `VarHandle`-- y `PrefsTreeTest` estuvieron
caidas asi, con la suite en verde, que es peor que no tenerla.

**Pero "no se puede juzgar" no es "esta rota", y se cuentan aparte.** Una prueba cuyo `main` imprime
valores sueltos --sin `TOTAL` y sin devolver un entero-- corrio bien y esta herramienta no sabe si
paso: es un **hueco de la compuerta**, no un fallo del arbol. Meterlas en "rotas" pintaba de rojo
trabajo ajeno que esta sano; dejarlas en verde era el punto ciego de antes. Van en su propia cuenta,
`sin veredicto`, visible y sin mentir para ninguno de los dos lados.

**El punto de entrada se deduce, no se supone.** Las pruebas de esta casa usan tres formas: `run()`
devolviendo un entero (la mayoria), `main` imprimiendo su propio resumen, y unas pocas con varios
metodos estaticos con nombre propio. Suponer `run` hacia que las de las otras dos formas se
reportaran como fallas con un panic de la VM, que es ruido puro. Se leen de la fuente.

Uso:

    python tools/suite.py                # todas
    python tools/suite.py Fmt Chrono     # solo las que contengan eso en el nombre
    python tools/suite.py --javac X.exe --vm Y.exe
"""
import argparse
import glob
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(RAIZ, "java")
LIB = os.path.join(RAIZ, "KajiLibrary")
# El classpath lleva **tambien** `java/`: varias pruebas se apoyan en una clase auxiliar del mismo
# directorio (`SpiProvTest` en `SpiProvSel`, que instala como proveedor de SPI). Compilando de a un
# archivo, sin esto el tipo no resuelve y la prueba figuraba como "NO COMPILA" -- un defecto de esta
# herramienta que se leia como un bug del compilador.
CP = LIB + os.pathsep + JAVA

RE_RUN = re.compile(r"^\s+(?:public\s+)?static\s+int\s+run\s*\(\s*\)", re.M)
RE_MAIN = re.compile(r"^\s+public\s+static\s+void\s+main\s*\(", re.M)
RE_INT = re.compile(r"^\s+public\s+static\s+int\s+(\w+)\s*\(\s*\)", re.M)
RE_VALOR = re.compile(r"->\s*Some\(Int\((-?\d+)\)\)")


def puntos_de_entrada(fuente):
    """Los metodos con los que se invoca esta prueba, en el orden en que se prefieren."""
    if RE_RUN.search(fuente):
        return ["run"]
    if RE_MAIN.search(fuente):
        return ["main"]
    # Las que no tienen ninguno de los dos exponen varios estaticos con nombre propio, y hay que
    # llamarlos a todos: cada uno es una prueba.
    return RE_INT.findall(fuente)


def correr(vm, clase, metodo, timeout):
    try:
        p = subprocess.run([vm, clase, metodo], capture_output=True, text=True, encoding="utf-8", errors="replace",
                           timeout=timeout)
    except subprocess.TimeoutExpired:
        return "TIMEOUT", None
    salida = p.stdout + p.stderr
    if "panicked at" in salida:
        # La VM se cayo. Casi siempre es que el metodo no existe con esa firma.
        motivo = "panic"
        for l in salida.splitlines():
            if "panicked at" in l:
                motivo = "panic: " + salida.splitlines()[salida.splitlines().index(l) + 1].strip()
                break
        return motivo, None
    # **Una excepcion que escapa es una prueba rota**, y hasta el 2026-10-03 esta herramienta no lo
    # miraba. `run-headless` sale con 1 cuando el hilo principal muere con una excepcion --igual que
    # `java` real-- y con 0 cuando el programa termino, devuelva valor o no. Sin leer ese codigo, una
    # prueba que tiraba imprimia `-> None`, no tenia linea `TOTAL`, y caia en el `(sin valor)` de mas
    # abajo: se reportaba **como buena** y no entraba en la cuenta de rotas. Asi estuvieron caidas
    # `VhTest` --y con ella toda la familia `VarHandle`-- y `PrefsTreeTest`, con la suite en verde.
    if p.returncode != 0:
        escapo = [l.strip() for l in salida.splitlines()
                  if l.startswith("Exception in thread")]
        return "EXCEPCION: " + (escapo[-1] if escapo else "salio con %d" % p.returncode), None
    m = RE_VALOR.search(salida)
    if m:
        return "ok", int(m.group(1))
    if "-> None" in salida:
        # `main` no devuelve nada: el veredicto lo imprime la prueba misma.
        resumen = [l.strip() for l in salida.splitlines() if l.strip().startswith("TOTAL")]
        if resumen:
            return "ok", resumen[-1]
        # Ni valor de retorno ni linea `TOTAL`: nadie puede leer el veredicto, asi que la prueba no
        # prueba nada. Antes esto imprimia `(sin valor)` y contaba como buena -- la otra mitad del
        # mismo punto ciego.
        return "SIN VEREDICTO", None
    return "sin resultado", None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("filtros", nargs="*", help="subcadenas del nombre; vacio = todas")
    ap.add_argument("--javac", default=os.path.join(RAIZ, "bin", "javac.exe"))
    ap.add_argument("--vm", default=os.path.join(RAIZ, "bin", "run-headless.exe"))
    # 60 y no 30: con 30, `BeansTest` (35 s) y `StatTest` (34 s) se reportaban como TIMEOUT estando
    # las dos sanas, que es exactamente el falso positivo que esta herramienta existe para evitar.
    ap.add_argument("--timeout", type=int, default=60)
    ap.add_argument("--sin-compilar", action="store_true",
                    help="usa los .class que ya estan (mas rapido, menos honesto)")
    args = ap.parse_args()
    # Absolutas y con los separadores de la plataforma. `CreateProcess` **no** encuentra una ruta
    # relativa con `/` --`--vm target/release/run-headless.exe` moria con un `FileNotFoundError` de
    # Python-- y esa es justamente la forma en que se escribe a mano para medir un arreglo sin congelar.
    args.javac = os.path.abspath(args.javac)
    args.vm = os.path.abspath(args.vm)

    fuentes = sorted(glob.glob(os.path.join(JAVA, "*Test.java")))
    if args.filtros:
        fuentes = [f for f in fuentes
                   if any(x.lower() in os.path.basename(f).lower() for x in args.filtros)]

    rotas, sin_veredicto, corridas = [], [], 0
    for f in fuentes:
        nombre = os.path.basename(f)[:-5]
        fuente = open(f, encoding="utf-8", errors="replace").read()

        if not args.sin_compilar:
            p = subprocess.run([args.javac, "--emit", "-cp", CP, f],
                               capture_output=True, text=True,
                               encoding="utf-8", errors="replace")
            err = [l for l in (p.stdout + p.stderr).splitlines() if "error" in l]
            if err:
                rotas.append((nombre, "NO COMPILA", err[0].strip()))
                print("%-22s NO COMPILA  %s" % (nombre, err[0].strip()))
                continue

        entradas = puntos_de_entrada(fuente)
        if not entradas:
            rotas.append((nombre, "SIN ENTRADA", "no expone run(), main ni estaticos int"))
            print("%-22s SIN ENTRADA" % nombre)
            continue

        for m in entradas:
            corridas += 1
            estado, valor = correr(args.vm, os.path.join(JAVA, nombre + ".class"), m,
                                   args.timeout)
            etiqueta = nombre if m in ("run", "main") else "%s.%s" % (nombre, m)
            if estado == "ok":
                print("%-22s %s" % (etiqueta, valor))
            elif estado == "SIN VEREDICTO":
                sin_veredicto.append(etiqueta)
                print("%-22s %s" % (etiqueta, estado))
            else:
                rotas.append((etiqueta, estado, ""))
                # Los estados que ya traen un detalle --el panic, la excepcion-- se imprimen tal
                # cual: en mayusculas el nombre de la clase que tiro se vuelve ilegible.
                print("%-22s %s" % (etiqueta, estado if ":" in estado else estado.upper()))

    cola = ", %d sin veredicto" % len(sin_veredicto) if sin_veredicto else ""
    print("\n%d invocaciones, %d rotas%s" % (corridas, len(rotas), cola))
    for n, e, d in rotas:
        print("  %-22s %s %s" % (n, e, d))
    if sin_veredicto:
        print("  (sin veredicto: la prueba corrio y no dijo si paso --- ni `TOTAL` ni entero)")
        for n in sin_veredicto:
            print("    %s" % n)
    return 1 if rotas else 0


if __name__ == "__main__":
    sys.exit(main())
