"""Genera `docs/completitud.html`: dos barras por paquete, clases y miembros.

Existia como un script suelto en un scratchpad de sesion, que es como se pierden las cosas: el HTML
quedaba en el arbol y la manera de regenerarlo no. Ahora vive aca.

Las dos medidas hay que leerlas juntas, y por eso van una encima de la otra en la misma fila. La de
miembros sola tiene un punto ciego grande --una clase que no esta en el arbol es invisible para
ella-- y la de clases sola no distingue una clase escrita entera de una con dos metodos. El tono de
la barra codifica la magnitud del paquete, no su completitud: un 100 % sobre tres clases y un 100 %
sobre ciento nueve no son el mismo logro.

Uso:
    python tools/grafico.py                 # mide y escribe docs/completitud.html
    python tools/grafico.py --cache F.json  # reusa medidas ya tomadas (o las guarda ahi)
"""
import argparse
import json
import os
import re
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLASES = os.path.join(RAIZ, "tools", "apidiff", "clases.json")
MEDIR = os.path.join(RAIZ, "tools", "apidiff", "medir.py")
SALIDA = os.path.join(RAIZ, "docs", "completitud.html")

# Geometria, en el sistema del viewBox.
X0, X1 = 190.0, 772.0        # donde empieza y termina la zona de barras
ALTO_FILA = 34
ARRIBA = 17


def medir(pkg):
    """Los miembros de un paquete, via `medir.py`. Devuelve (tengo, total)."""
    p = subprocess.run([sys.executable, MEDIR, pkg], capture_output=True, text=True,
                       stdin=subprocess.DEVNULL, cwd=RAIZ)
    m = re.search(r"TOTAL (\d+)/(\d+)", p.stdout or "")
    return (int(m.group(1)), int(m.group(2))) if m else (0, 0)


def tono(total_clases):
    """La rampa por **magnitud**: cuantas clases publicas tiene el paquete en el JDK."""
    for corte, clase in ((5, "p0"), (15, "p1"), (40, "p2"), (90, "p3")):
        if total_clases < corte:
            return clase
    return "p4"


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def fila(y, pkg, ct, cT, mt, mT, cls):
    """Una fila: el nombre, la barra de clases, la barra de miembros calada, y los dos numeros."""
    pc = (ct / cT * 100) if cT else 0
    pm = (mt / mT * 100) if mT else 0
    etiqueta = "%s — clases %d/%d (%.0f %%), miembros %d/%d (%.1f %%)" % (
        pkg, ct, cT, pc, mt, mT, pm)
    ancho = X1 - X0
    partes = [
        '<g class="fila" tabindex="0" role="listitem" aria-label="%s">' % esc(etiqueta),
        '<rect class="hit" x="0" y="%d" width="900" height="%d"/>' % (y, ALTO_FILA),
        '<text class="nombre" x="180" y="%.1f" text-anchor="end">%s</text>' % (y + 16.0, esc(pkg)),
    ]
    for i, (t, T, extra) in enumerate(((ct, cT, "clases"), (mt, mT, "miembros"))):
        frac = (t / T) if T else 0.0
        by = y + 5.0 + i * 11
        partes.append('<rect class="barra %s %s" x="%.1f" y="%.1f" width="%.1f" height="9" rx="4"/>'
                      % (cls, extra, X0, by, ancho * frac))
        # Un tope minimo visible: una barra de ancho cero no se distingue de una fila vacia.
        partes.append('<rect class="barra %s %s" x="%.1f" y="%.1f" width="4" height="9"/>'
                      % (cls, extra, X0, by))
        partes.append('<text class="valor" x="%.1f" y="%.1f">%d/%d</text>'
                      % (X1 + 8, by + 8.0, t, T))
    partes.append("<title>%s</title></g>" % esc(etiqueta))
    return "".join(partes)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", help="json con las medidas de miembros ya tomadas")
    args = ap.parse_args()

    paquetes = json.load(open(CLASES, encoding="utf-8"))

    miembros = {}
    if args.cache and os.path.exists(args.cache):
        miembros = json.load(open(args.cache, encoding="utf-8"))
    for r in paquetes:
        if r["pkg"] not in miembros:
            miembros[r["pkg"]] = list(medir(r["pkg"]))
            print("  %-30s %d/%d" % (r["pkg"], *miembros[r["pkg"]]), flush=True)
    if args.cache:
        json.dump(miembros, open(args.cache, "w", encoding="utf-8"), indent=1)

    # Orden: por completitud de clases y, a igualdad, por tamanio. Los completos arriba.
    def clave(r):
        ct, cT = r["tengo"], r["total"]
        return (-(ct / cT if cT else 0), -cT)
    paquetes = sorted(paquetes, key=clave)

    filas, y = [], ARRIBA
    for r in paquetes:
        mt, mT = miembros[r["pkg"]]
        filas.append(fila(y, r["pkg"], r["tengo"], r["total"], mt, mT, tono(r["total"])))
        y += ALTO_FILA
    alto = y + 12

    rejilla = []
    for pct in (0, 25, 50, 75, 100):
        x = X0 + (X1 - X0) * pct / 100
        rejilla.append('<line class="rejilla" x1="%.1f" y1="18" x2="%.1f" y2="%d"/>' % (x, x, alto - 12))
        rejilla.append('<text class="tick" x="%.1f" y="12" text-anchor="middle">%d%%</text>' % (x, pct))

    svg = ('<svg class="gr" viewBox="0 0 900 %d" width="100%%" role="list" '
           'aria-label="Completitud por paquete">%s%s</svg>' % (alto, "".join(rejilla), "".join(filas)))

    ct = sum(r["tengo"] for r in paquetes)
    cT = sum(r["total"] for r in paquetes)
    mt = sum(miembros[r["pkg"]][0] for r in paquetes)
    mT = sum(miembros[r["pkg"]][1] for r in paquetes)
    completos = sum(1 for r in paquetes
                    if r["tengo"] == r["total"] and miembros[r["pkg"]][0] == miembros[r["pkg"]][1]
                    and r["total"])

    tabla = "".join(
        "<tr><td>%s</td><td class=\"n\">%d</td><td class=\"n\">%d</td>"
        "<td class=\"n\">%d</td><td class=\"n\">%d</td><td class=\"n\">%.1f&nbsp;%%</td></tr>"
        % (esc(r["pkg"]), r["tengo"], r["total"], miembros[r["pkg"]][0], miembros[r["pkg"]][1],
           (miembros[r["pkg"]][0] / miembros[r["pkg"]][1] * 100) if miembros[r["pkg"]][1] else 0)
        for r in paquetes)

    viejo = open(SALIDA, encoding="utf-8").read()
    nuevo = viejo
    nuevo = re.sub(r'<svg class="gr".*?</svg>', lambda _: svg, nuevo, count=1, flags=re.S)
    nuevo = re.sub(r'<div class="v">[\d/]+</div><div class="k">clases públicas[^<]*</div>',
                   '<div class="v">%d/%d</div><div class="k">clases públicas — %d&nbsp;%%</div>'
                   % (ct, cT, round(ct / cT * 100)), nuevo, count=1)
    nuevo = re.sub(r'<div class="v">[\d.]+</div><div class="k">de [\d.]+ miembros[^<]*</div>',
                   '<div class="v">%s</div><div class="k">de %s miembros — %.1f&nbsp;%%</div>'
                   % ("{:,}".format(mt).replace(",", "."), "{:,}".format(mT).replace(",", "."),
                      mt / mT * 100), nuevo, count=1)
    nuevo = re.sub(r'<div class="v">\d+</div><div class="k">de \d+ paquetes completos[^<]*</div>',
                   '<div class="v">%d</div><div class="k">de %d paquetes completos en ambas</div>'
                   % (completos, len(paquetes)), nuevo, count=1)
    nuevo = re.sub(r"(<tbody>).*?(</tbody>|\s*</table>)",
                   lambda m: "<tbody>" + tabla + "</tbody>\n    ", nuevo, count=1, flags=re.S)
    if "</tbody>" not in nuevo:
        nuevo = nuevo.replace(tabla, tabla + "</tbody>")

    with open(SALIDA, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(nuevo)
    print("escrito %s — %d/%d clases, %d/%d miembros, %d paquetes completos"
          % (SALIDA, ct, cT, mt, mT, completos))


if __name__ == "__main__":
    main()
