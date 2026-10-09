# -*- coding: utf-8 -*-
"""El **censo por tiempo** del JIT: que rechazo cae en camino caliente.

El censo de `burst::jit_tests` cuenta **metodos** -- cuantos compilan, cuantos no, y por que --.
Esa tabla decide donde trabajar y tiene un agujero: un metodo que nadie llama pesa lo mismo que
`String.charAt`. Esto cuenta **opcodes interpretados**, que es el trabajo que el JIT no se llevo.

La medida sale de la VM (`JVM_PROFILE=1`, una fila por metodo) y el motivo del rechazo sale del log
que ya existia (`JVM_JIT_LOG=1`); las dos de la **misma corrida**, porque el motivo depende del
estado de la VM y no solo del bytecode, asi que unir dos corridas distintas puede unir un rechazo
con un motivo que esa corrida no tuvo.

Por que un contador y no un reloj: es **deterministico**. El ruido de layout de esta maquina
(+-3-12%, ver `bench/BASELINE.tsv`) tapa la mayoria de los efectos que buscamos, y dos corridas de
esto dan exactamente el mismo numero. No mide nanosegundos; mide trabajo, que es lo que hace
comparables dos metodos con costos por opcode parecidos.

Lo que **no** mide, y conviene tenerlo a la vista al leer el resultado:

- Un opcode no es un nanosegundo. Un `invokevirtual` interpretado cuesta mucho mas que un `iadd`,
  asi que un metodo lleno de llamadas esta subestimado en esta tabla.
- Un metodo **compilado** sale de la tabla el instante en que se compila, que es lo que la hace la
  medida correcta para «donde sigue trabajando el interprete» y la hace inservible para «cuanto
  tiempo total gasta este metodo».
- El corpus es el que se le pase. `java/texto/` son 48 programas de formateo y parseo reales, que
  es lo mas parecido a una aplicacion que tenemos; `java/` son sondas de una construccion cada una
  y estan dominadas por el arranque.

Uso:

    python tools/perfil.py                        # java/texto/, con el run-headless de release
    python tools/perfil.py --corpus java/texto    # lo mismo, explicito
    python tools/perfil.py --top 40
    python tools/perfil.py --vm bin/run-headless.exe
"""
import argparse
import collections
import glob
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# `[perfil] <pasos>\t<veredicto>\t<frio>\t<bytes>\t<metodo>`
FILA = re.compile(r"^\[perfil\] (\d+)\t(\w+)\t(\d+)\t(\d+)\t(\d+)\t(.+)$")
# `[jit] rechaza <Motivo> { ... } [<clase>] <metodo>`. El ultimo token es siempre el metodo; lo
# del medio es la carga del motivo, que para `Opcode` trae el byte y para los `Unresolved*` trae
# el nombre que la VM resolvio del constant pool.
RECHAZO = re.compile(r"^\[jit\] rechaza (\w+) (.*) (\S+)$")
CLASE = re.compile(r"<([^>]+)>")
OPCODE = re.compile(r"opcode: (\d+)")

# Los opcodes que aparecen en los rechazos, por nombre: un `184` no se lee, un `invokestatic` si.
# Parcial a proposito -- lo que no esta sale en hexa, que es mejor que una tabla que miente.
MNEMONICO = {
    0xb2: "getstatic", 0xb3: "putstatic", 0xb6: "invokevirtual", 0xb7: "invokespecial",
    0xb8: "invokestatic", 0xb9: "invokeinterface", 0xba: "invokedynamic", 0xbf: "athrow",
    0xc2: "monitorenter", 0xc3: "monitorexit", 0xa8: "jsr", 0xa9: "ret",
    0xaa: "tableswitch", 0xab: "lookupswitch", 0xc1: "instanceof", 0xc0: "checkcast",
    0x8a: "l2d", 0x8b: "f2i", 0x62: "fadd", 0x63: "dadd", 0x6f: "ddiv", 0x6e: "fdiv",
}


def desglose(motivo, carga):
    """El motivo en dos granularidades: `(medio, fino)`.

    Hacen falta las dos y por razones distintas. `Opcode` a secas no se puede accionar --hay que
    saber **cual**--, pero el callee concreto parte `invokestatic` en cuarenta filas y pierde el
    total. Asi que `medio` es el opcode (`Opcode invokestatic`) y `fino` es a quien llama
    (`Opcode invokestatic java/lang/String.rawCharAt(...)`), que es donde esta el arreglo.

    Para `UnresolvedClass` el `fino` es la clase, que es la distincion que decide el trabajo: un
    `new` de una excepcion vive en la rama de error --que no corre nunca-- y rechaza igual el
    metodo entero, y eso se arregla distinto que un `new` en el camino caliente.
    """
    clase = CLASE.search(carga)
    if motivo == "Opcode":
        m = OPCODE.search(carga)
        if m:
            op = int(m.group(1))
            medio = f"Opcode {MNEMONICO.get(op, f'0x{op:02x}')}"
            return medio, (f"{medio} {clase.group(1)}" if clase else medio)
    if clase:
        return motivo, f"{motivo} {clase.group(1)}"
    return motivo, motivo


def una_corrida(vm, clase):
    """Corre un programa y devuelve (filas del perfil, motivo por metodo).

    La entrada es `run` o `main` segun la prueba, igual que en `tools/suite.py`: se prueba `run` y
    si la clase no lo declara --`run-headless` entra en panico con `method descriptor`-- se
    reintenta con `main`. Una clase sin ninguno de los dos se cuenta como saltada.
    """
    entorno = dict(os.environ)
    entorno["JVM_PROFILE"] = "1"
    entorno["JVM_JIT_LOG"] = "1"
    for entrada in ("run", "main"):
        try:
            p = subprocess.run([vm, clase, entrada], capture_output=True, text=True, cwd=RAIZ,
                               env=entorno, timeout=300, errors="replace")
        except subprocess.TimeoutExpired:
            return [], {}, "timeout"
        if "method descriptor" not in (p.stderr or ""):
            break
    else:
        return [], {}, "sin entrada"
    filas, motivos = [], {}
    for linea in (p.stderr or "").splitlines():
        m = FILA.match(linea)
        if m:
            pasos, veredicto, frio, bytes_, post, metodo = m.groups()
            filas.append((int(pasos), veredicto, int(frio), int(bytes_), int(post), metodo))
            continue
        m = RECHAZO.match(linea)
        if m:
            medio, fino = desglose(m.group(1), m.group(2))
            motivos[m.group(3)] = (m.group(1), medio, fino)
    # Una corrida sin ninguna fila es una corrida que no llego a terminar: la VM vuelca el perfil
    # al **salir** del bucle, asi que un programa que se cae no deja tabla. Se reporta y no se
    # cuenta, en vez de desaparecer en silencio.
    return filas, motivos, None if filas else "sin perfil"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--corpus", default=os.path.join("java", "texto"))
    ap.add_argument("--vm", default=os.path.join("target", "release", "run-headless.exe"))
    ap.add_argument("--top", type=int, default=25)
    ap.add_argument("--filtro", default="", help="subcadena del nombre del programa")
    args = ap.parse_args()

    vm = os.path.abspath(os.path.join(RAIZ, args.vm))
    # Solo las clases de nivel superior: una anidada (`Foo$Bar.class`) no tiene `run`.
    fuentes = sorted(glob.glob(os.path.join(RAIZ, args.corpus, "*.java")))
    programas = [os.path.relpath(f, RAIZ).replace("\\", "/")[:-5] + ".class" for f in fuentes]
    if args.filtro:
        programas = [c for c in programas if args.filtro in c]
    if not programas:
        sys.exit(f"sin programas en {args.corpus}")

    pasos = collections.Counter()          # metodo -> opcodes interpretados
    # metodo -> cuantos de esos opcodes corrieron **con el nativo ya instalado**. No son
    # calentamiento: son el interprete retomando donde el codigo nativo se rindio, o sea deopts.
    post_nativo = collections.Counter()
    veredicto = {}                         # metodo -> compila/rechaza/frio
    motivo = {}                            # metodo -> motivo del rechazo
    tam = {}                               # metodo -> bytes de cuerpo
    saltados = []
    print(f"censo por tiempo sobre {len(programas)} programas de {args.corpus}", file=sys.stderr)
    for i, clase in enumerate(programas, 1):
        filas, motivos, error = una_corrida(vm, clase)
        if error:
            saltados.append((clase, error))
            continue
        for p, v, _frio, b, post, m in filas:
            pasos[m] += p
            post_nativo[m] += post
            tam[m] = b
            # `rechaza` gana sobre `frio`: en un programa el metodo puede no haber llegado al
            # umbral y en otro si, y lo que interesa es que pasa **cuando** llega.
            if veredicto.get(m) != "rechaza":
                veredicto[m] = v
        motivo.update(motivos)
        print(f"  [{i}/{len(programas)}] {clase}", file=sys.stderr)

    total = sum(pasos.values())
    if not total:
        sys.exit("ninguna corrida dejo perfil")

    por_veredicto = collections.Counter()
    for m, p in pasos.items():
        por_veredicto[veredicto.get(m, "?")] += p

    print()
    print(f"=== {total:,} opcodes interpretados en {len(pasos)} metodos "
          f"({len(programas) - len(saltados)} programas) ===")
    post_total = sum(post_nativo.values())
    print()
    print("por veredicto del JIT:")
    for v, p in por_veredicto.most_common():
        print(f"  {v:<10} {p:>12,}  {100.0 * p / total:5.1f}%")

    # **El balde de «compila», partido.** Es el mas grande del censo y es dos problemas con
    # arreglos opuestos: lo de antes de la instalacion es calentamiento --umbral-- y lo de despues
    # es el interprete retomando donde el nativo se rindio --deopts--. Sumados no se puede elegir.
    compila = por_veredicto["compila"] or 1
    print()
    print("el balde de «compila», partido:")
    print(f"  {'calentamiento (sin nativo todavia)':<38} {compila - post_total:>12,} "
          f"{100.0 * (compila - post_total) / compila:5.1f}%")
    print(f"  {'con el nativo YA instalado (deopts)':<38} {post_total:>12,} "
          f"{100.0 * post_total / compila:5.1f}%")

    # El cuadro que decide donde trabajar: motivos de rechazo ordenados por **tiempo**, no por
    # cantidad de metodos. Las dos columnas juntas porque la diferencia entre ellas es el
    # resultado: un motivo con pocos metodos y mucho tiempo es el que hay que arreglar.
    por_motivo = collections.Counter()
    metodos_por_motivo = collections.Counter()
    por_medio = collections.Counter()
    metodos_por_medio = collections.Counter()
    por_detalle = collections.Counter()
    metodos_por_detalle = collections.Counter()
    SIN = ("(sin log)", "(sin log)", "(sin log)")
    for m, p in pasos.items():
        if veredicto.get(m) == "rechaza":
            k, medio, detalle = motivo.get(m, SIN)
            por_motivo[k] += p
            metodos_por_motivo[k] += 1
            por_medio[medio] += p
            metodos_por_medio[medio] += 1
            por_detalle[detalle] += p
            metodos_por_detalle[detalle] += 1
    rechazado = por_veredicto["rechaza"] or 1
    print()
    print("motivos de rechazo, por tiempo interpretado:")
    print(f"  {'motivo':<24} {'opcodes':>12} {'% de lo rechazado':>18} {'metodos':>8}")
    for k, p in por_motivo.most_common():
        print(f"  {k:<24} {p:>12,} {100.0 * p / rechazado:>17.1f}% {metodos_por_motivo[k]:>8}")

    print()
    print("el mismo cuadro con el opcode concreto:")
    print(f"  {'motivo':<28} {'opcodes':>12} {'%':>7} {'metodos':>8}")
    for k, p in por_medio.most_common():
        print(f"  {k:<28} {p:>12,} {100.0 * p / rechazado:>6.1f}% {metodos_por_medio[k]:>8}")

    print()
    print(f"los {args.top} motivos afinados (a quien llama, que clase):")
    print(f"  {'motivo':<62} {'opcodes':>12} {'%':>7} {'metodos':>8}")
    for k, p in por_detalle.most_common(args.top):
        print(f"  {k:<62} {p:>12,} {100.0 * p / rechazado:>6.1f}% {metodos_por_detalle[k]:>8}")

    print()
    print(f"los {args.top} metodos que mas interprete gastan:")
    print(f"  {'opcodes':>10} {'%':>5}  {'veredicto':<9} {'bytes':>6}  motivo / metodo")
    for m, p in pasos.most_common(args.top):
        v = veredicto.get(m, "?")
        razon = motivo.get(m, ("", "", ""))[2] if v == "rechaza" else ""
        if v == "compila" and pasos[m]:
            razon = f"post={100.0 * post_nativo[m] / pasos[m]:.0f}%"
        print(f"  {p:>10,} {100.0 * p / total:>4.1f}%  {v:<9} {tam.get(m, 0):>6}  "
              f"{razon + ' ' if razon else ''}{m}")

    if saltados:
        print()
        print(f"{len(saltados)} programas sin perfil (no terminaron):")
        for clase, error in saltados:
            print(f"  {error:<12} {clase}")


if __name__ == "__main__":
    main()
