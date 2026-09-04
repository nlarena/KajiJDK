"""Cuantas clases **publicas** de cada paquete tenemos, y cuantas hay.

Es la otra mitad de la medicion, y la que faltaba. `medir_todo.py` recorre **nuestras** clases: una
clase que no existe en el arbol es invisible para el, asi que un paquete al que le falta la mitad de
sus tipos puede figurar al 100 %. Paso exactamente eso con `java.util.concurrent.locks`, que no tiene
`AbstractQueuedSynchronizer` --la clase sobre la que el paquete entero se apoya-- y figuraba en 42/59.

Solo se cuentan las **publicas**: las de paquete del JDK son detalle de implementacion y no hay
contrato que cumplir con ellas.
"""
import io
import json
import os
import sys

sys.path.insert(0, os.path.join("tools", "apidiff"))
os.environ.setdefault("JDK_HOME", r"H:\jdk-25.0.2")
import apidiff as A

RAIZ = "KajiLibrary"


def nuestras():
    """Los FQCN top-level que hay en el arbol."""
    out = set()
    for dp, _, fs in os.walk(RAIZ):
        for f in fs:
            if f.endswith(".class") and "$" not in f:
                rel = os.path.relpath(os.path.join(dp, f), RAIZ)
                out.add(rel[: -len(".class")].replace(os.sep, "."))
    return out


def todos_los_modulos():
    """Los FQCN top-level de **toda** la imagen del JDK, no solo de `java.base`."""
    out = A.run([A.JIMAGE, "list", A.MODULES])
    index = set()
    for line in out.splitlines():
        t = line.strip()
        if t.endswith(".class") and "$" not in t and not t.startswith("Module:"):
            index.add(t[:-6].replace("/", "."))
    return index


def main():
    mias = nuestras()
    paquetes_mios = {fq.rsplit(".", 1)[0] for fq in mias if "." in fq}

    # El indice del JDK, filtrado a los paquetes que ya empezamos: uno que no empezamos no es un
    # hueco de cobertura sino una decision de alcance, y mezclarlos haria el numero ilegible.
    #
    # **Todos los modulos**, no solo `java.base`: los `javax.*` viven en `java.compiler` y en
    # `java.sql`, y con el indice de `java.base` solo quedaban afuera -- o sea, invisibles, que es
    # justo el error que esta medicion vino a arreglar.
    todas = todos_los_modulos()

    filas = []
    for pkg in sorted(paquetes_mios):
        delJdk = sorted(c for c in todas if c.rsplit(".", 1)[0] == pkg)
        if not delJdk:
            continue
        # Publicas segun `javap`: si no lo es, no hay contrato publico que cumplir.
        publicas = []
        for c in delJdk:
            try:
                decl, _ = A.parse(A.run([A.JAVAP, "-p", c]))
            except Exception:
                continue
            if decl and decl.startswith("public "):
                publicas.append(c)
        if not publicas:
            continue
        tengo = [c for c in publicas if c in mias]
        filas.append({"pkg": pkg, "tengo": len(tengo), "total": len(publicas),
                      "faltan": [c.rsplit(".", 1)[-1] for c in publicas if c not in mias]})
        print("%-30s %3d/%-3d clases publicas" % (pkg, len(tengo), len(publicas)))
        sys.stdout.flush()

    destino = os.path.join(os.path.dirname(os.path.abspath(__file__)), "clases.json")
    with io.open(destino, "w", encoding="utf-8") as fh:
        json.dump(filas, fh, ensure_ascii=False, indent=1)
    print("escrito", destino)


if __name__ == "__main__":
    main()
