"""El grafico de completitud, en dos dimensiones, a un HTML local.

    python tools/apidiff/censo.py docs/censo.json
    python tools/apidiff/grafico.py docs/censo.json docs/completitud.html

La pagina lleva el censo **adentro**, y ademas sabe leer uno nuevo: el boton abre un `censo.json` y
vuelve a dibujar. No puede medir sola --una pagina no ejecuta `javap`-- asi que lo que ofrece es la
otra mitad: el comando que produce el censo, listo para copiar, y el lector para el resultado.
"""
import json, sys

# Los paquetes que se marcan como recien llegados. Es una lista y no una fecha porque el censo no
# guarda historia: se cambia a mano cuando cambia lo que vale la pena senalar.
NUEVOS = {"javax.sql", "java.sql", "javax.transaction.xa", "javax.tools", "java.util.logging",
          "javax.xml.transform", "java.lang.foreign", "java.nio.channels", "java.security.spec",
          "java.security.cert", "java.security.interfaces"}


def main():
    datos = json.load(open(sys.argv[1]))
    salida = sys.argv[2]
    html = PAGINA.replace("__DATOS__", json.dumps(datos, separators=(",", ":"))) \
                 .replace("__NUEVOS__", json.dumps(sorted(NUEVOS)))
    open(salida, "w", encoding="utf-8").write(html)
    print("escrito %s (%d paquetes)" % (salida, len(datos)))


PAGINA = r"""<title>Completitud de KajiLibrary</title>
<style>
  :root {
    --fondo: #fbfaf8; --tinta: #1b1a18; --suave: #6d6a64; --linea: #e2ded6;
    --clases: #1f6f8b; --miembros: #b4652a; --pista: #ece8e0; --acento: #7a5c2e;
    --panel: #ffffff; --sombra: 0 1px 2px rgba(0,0,0,.05);
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      --fondo: #16151a; --tinta: #eae7e0; --suave: #9b968c; --linea: #302e36;
      --clases: #6fb6cf; --miembros: #e0975c; --pista: #262430; --acento: #d8b271;
      --panel: #1d1c23; --sombra: 0 1px 2px rgba(0,0,0,.3);
    }
  }
  :root[data-theme="dark"] {
    --fondo: #16151a; --tinta: #eae7e0; --suave: #9b968c; --linea: #302e36;
    --clases: #6fb6cf; --miembros: #e0975c; --pista: #262430; --acento: #d8b271;
    --panel: #1d1c23; --sombra: 0 1px 2px rgba(0,0,0,.3);
  }
  body {
    background: var(--fondo); color: var(--tinta); margin: 0;
    font: 15px/1.55 ui-sans-serif, system-ui, "Segoe UI", Roboto, sans-serif;
  }
  body.arrastrando { outline: 2px dashed var(--acento); outline-offset: -10px; }
  .hoja { max-width: 1020px; margin: 0 auto; padding: 44px 24px 72px; }
  h1 { font-size: 27px; margin: 0 0 6px; letter-spacing: -.015em; font-weight: 620; }
  .sub { color: var(--suave); margin: 0 0 26px; max-width: 68ch; }

  .barra-sup {
    display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
    margin: 0 0 22px; padding-bottom: 20px; border-bottom: 1px solid var(--linea);
  }
  button {
    font: inherit; font-size: 13.5px; color: var(--tinta); background: var(--panel);
    border: 1px solid var(--linea); border-radius: 7px; padding: 6px 13px; cursor: pointer;
    box-shadow: var(--sombra);
  }
  button:hover { border-color: var(--acento); color: var(--acento); }
  button.principal { background: var(--acento); color: var(--fondo); border-color: var(--acento); }
  button.principal:hover { filter: brightness(1.1); color: var(--fondo); }
  button[hidden] { display: none; }
  .fuente { color: var(--suave); font-size: 12.5px; margin-left: auto; text-align: right; }

  .comando {
    background: var(--panel); border: 1px solid var(--linea); border-radius: 9px;
    padding: 12px 14px; margin: 0 0 26px; display: none;
  }
  .comando.abierto { display: block; }
  .comando p { margin: 0 0 9px; font-size: 13.5px; color: var(--suave); }
  .comando .fila-cmd { display: flex; gap: 9px; align-items: flex-start; }
  .comando code {
    flex: 1; display: block; font-family: ui-monospace, "Cascadia Mono", Menlo, monospace;
    font-size: 12.5px; background: var(--pista); border-radius: 6px; padding: 9px 11px;
    white-space: pre-wrap; word-break: break-all; line-height: 1.5;
  }

  .resumen { display: flex; flex-wrap: wrap; gap: 14px; margin: 0 0 30px; }
  .tarjeta {
    background: var(--panel); border: 1px solid var(--linea); border-radius: 10px;
    padding: 13px 17px; min-width: 150px; flex: 1 1 150px; box-shadow: var(--sombra);
  }
  .tarjeta .k { font-size: 11px; text-transform: uppercase; letter-spacing: .075em; color: var(--suave); }
  .tarjeta .v { font-size: 24px; font-variant-numeric: tabular-nums; margin-top: 3px; font-weight: 600; }
  .tarjeta .v small { font-size: 14px; color: var(--suave); font-weight: 400; }

  .leyenda { display: flex; gap: 20px; margin: 0 0 14px; font-size: 13px; color: var(--suave); flex-wrap: wrap; }
  .leyenda span { display: inline-flex; align-items: center; gap: 7px; }
  .punto { width: 11px; height: 11px; border-radius: 3px; display: inline-block; }

  .envoltorio { overflow-x: auto; }
  table { border-collapse: collapse; width: 100%; min-width: 640px; }
  thead th {
    text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .07em;
    color: var(--suave); font-weight: 600; padding: 0 10px 7px; border-bottom: 1px solid var(--linea);
  }
  tbody th {
    text-align: left; font-weight: 450; padding: 5px 10px; white-space: nowrap;
    font-family: ui-monospace, "Cascadia Mono", Menlo, monospace; font-size: 13px;
  }
  td { padding: 5px 10px; }
  td.n {
    font-variant-numeric: tabular-nums; font-size: 12.5px; color: var(--suave);
    white-space: nowrap; text-align: right;
    font-family: ui-monospace, "Cascadia Mono", Menlo, monospace;
  }
  td.barra { width: 27%; }
  .pista { background: var(--pista); border-radius: 3px; height: 9px; overflow: hidden; }
  .rel { height: 100%; border-radius: 3px; transition: width .35s ease; }
  .rel.c { background: var(--clases); }
  .rel.m { background: var(--miembros); }
  tr.fila:hover { background: color-mix(in srgb, var(--acento) 8%, transparent); }
  tr.cerrado th { color: var(--acento); font-weight: 600; }
  tr.subio th::after { content: "▲"; color: var(--acento); font-size: 9px; margin-left: 6px; vertical-align: 2px; }
  tr.bajo th::after { content: "▼"; color: var(--suave); font-size: 9px; margin-left: 6px; vertical-align: 2px; }
  .etq {
    font-family: ui-sans-serif, system-ui, sans-serif; font-size: 10px; letter-spacing: .06em;
    text-transform: uppercase; color: var(--fondo); background: var(--acento);
    border-radius: 3px; padding: 1px 5px; margin-left: 6px; vertical-align: 1px;
  }
  .nota {
    margin-top: 34px; padding-top: 18px; border-top: 1px solid var(--linea);
    color: var(--suave); font-size: 13.5px; max-width: 72ch;
  }
  .nota strong { color: var(--tinta); font-weight: 600; }
  .nota + .nota { border-top: 0; padding-top: 0; margin-top: 12px; }
</style>

<div class="hoja">
<h1>Completitud de KajiLibrary</h1>
<p class="sub">Contra el JDK 25. Dos dimensiones y no una: cuántas de las clases públicas del paquete
existen, y cuántos de sus miembros —contando el cierre por herencia— están declarados.</p>

<div class="barra-sup">
  <button class="principal" id="b-abrir">Actualizar con un censo…</button>
  <button id="b-como">¿Cómo lo genero?</button>
  <button id="b-volver" hidden>Volver al guardado</button>
  <input type="file" id="archivo" accept=".json,application/json" hidden>
  <span class="fuente" id="fuente">censo incluido en la página</span>
</div>

<div class="comando" id="comando">
  <p>La página no puede medir sola: el censo sale de correr <code style="all:unset;font-family:ui-monospace,monospace">javap</code>
     contra el JDK y contra el árbol. Corré esto y después soltá el <code style="all:unset;font-family:ui-monospace,monospace">censo.json</code> acá
     (o usá el botón). Tarda unos treinta segundos.</p>
  <div class="fila-cmd">
    <code id="cmd">python tools/apidiff/censo.py docs/censo.json</code>
    <button id="b-copiar">Copiar</button>
  </div>
  <p style="margin:10px 0 0">Para regenerar la página entera, en vez de cargarla acá:<br>
    <code style="all:unset;font-family:ui-monospace,monospace;font-size:12.5px">python tools/apidiff/grafico.py docs/censo.json docs/completitud.html</code></p>
</div>

<div class="resumen" id="resumen"></div>

<div class="leyenda">
  <span><i class="punto" style="background:var(--clases)"></i>clases</span>
  <span><i class="punto" style="background:var(--miembros)"></i>miembros</span>
</div>

<div class="envoltorio">
<table>
<thead><tr><th>Paquete</th><th>Clases</th><th></th><th>Miembros</th><th></th></tr></thead>
<tbody id="cuerpo"></tbody>
</table>
</div>

<p class="nota"><strong>Qué se cuenta.</strong> Solo lo <strong>público</strong>: los internos del
JDK —<code style="all:unset;font-family:ui-monospace,monospace">StringLatin1</code>,
<code style="all:unset;font-family:ui-monospace,monospace">CharacterData00</code>,
<code style="all:unset;font-family:ui-monospace,monospace">TimSort</code>— no son API y nadie tiene
por qué reimplementarlos. Y los miembros de una clase que <em>no</em> existe entran igual en el
denominador, con cero en el numerador: si salieran solo de las clases que tenemos, a un paquete al
que le falta la mitad de sus tipos le podría dar 100&nbsp;%.</p>
<p class="nota"><strong>Por qué dos barras.</strong> Una clase completa y una clase ausente no son lo
mismo aunque los miembros cuadren, así que las dimensiones van separadas. El orden de la tabla usa la
<strong>peor</strong> de las dos.</p>
<p class="nota">Al cargar un censo nuevo, un <strong>▲</strong> marca los paquetes que mejoraron
respecto del guardado y un <strong>▼</strong> los que empeoraron.</p>
</div>

<script id="censo" type="application/json">__DATOS__</script>
<script>
(function () {
  "use strict";

  var GUARDADO = JSON.parse(document.getElementById("censo").textContent);
  var NUEVOS = __NUEVOS__;
  var actual = GUARDADO;

  function conPorcentajes(datos) {
    return datos.map(function (d) {
      var pc = d.clasesJdk ? 100 * d.clases / d.clasesJdk : 0;
      var pm = d.miembrosJdk ? 100 * d.miembros / d.miembrosJdk : 0;
      return Object.assign({}, d, { pc: pc, pm: pm, peor: Math.min(pc, pm) });
    }).sort(function (a, b) {
      return (b.peor - a.peor) || (b.miembrosJdk - a.miembrosJdk);
    });
  }

  // El estado guardado, por paquete, para poder marcar qué se movió.
  var base = {};
  GUARDADO.forEach(function (d) { base[d.pkg] = d; });

  function texto(el, t) { el.textContent = t; }

  function pintar(datos, esNuevoCenso) {
    var filas = conPorcentajes(datos);
    var tc = 0, tcj = 0, tm = 0, tmj = 0, cerrados = 0;

    var cuerpo = document.getElementById("cuerpo");
    cuerpo.textContent = "";

    filas.forEach(function (d) {
      tc += d.clases; tcj += d.clasesJdk; tm += d.miembros; tmj += d.miembrosJdk;
      var cerrado = d.pc >= 100 && d.pm >= 100;
      if (cerrado) { cerrados++; }

      var tr = document.createElement("tr");
      tr.className = "fila" + (cerrado ? " cerrado" : "");

      if (esNuevoCenso && base[d.pkg]) {
        var b = base[d.pkg];
        var antes = b.clases + b.miembros;
        var ahora = d.clases + d.miembros;
        if (ahora > antes) { tr.classList.add("subio"); }
        else if (ahora < antes) { tr.classList.add("bajo"); }
      }

      var th = document.createElement("th");
      th.scope = "row";
      th.appendChild(document.createTextNode(d.pkg));
      if (!esNuevoCenso && NUEVOS.indexOf(d.pkg) >= 0) {
        var etq = document.createElement("span");
        etq.className = "etq";
        texto(etq, "nuevo");
        th.appendChild(etq);
      }
      tr.appendChild(th);

      tr.appendChild(celdaNumero(d.clases + "/" + d.clasesJdk));
      tr.appendChild(celdaBarra(d.pc, "c"));
      tr.appendChild(celdaNumero(d.miembros + "/" + d.miembrosJdk));
      tr.appendChild(celdaBarra(d.pm, "m"));
      cuerpo.appendChild(tr);
    });

    var resumen = document.getElementById("resumen");
    resumen.textContent = "";
    [["Paquetes empezados", String(filas.length), ""],
     ["Clases", String(tc), "/ " + tcj],
     ["Miembros", String(tm), "/ " + tmj],
     ["Cerrados", String(cerrados), "/ " + filas.length]
    ].forEach(function (t) {
      var div = document.createElement("div");
      div.className = "tarjeta";
      var k = document.createElement("div"); k.className = "k"; texto(k, t[0]);
      var v = document.createElement("div"); v.className = "v";
      v.appendChild(document.createTextNode(t[1]));
      if (t[2]) {
        var s = document.createElement("small");
        texto(s, " " + t[2].replace("/ ", "/ "));
        v.appendChild(s);
      }
      div.appendChild(k); div.appendChild(v);
      resumen.appendChild(div);
    });
  }

  function celdaNumero(t) {
    var td = document.createElement("td");
    td.className = "n";
    texto(td, t);
    return td;
  }

  function celdaBarra(pct, clase) {
    var td = document.createElement("td");
    td.className = "barra";
    var pista = document.createElement("div");
    pista.className = "pista";
    var rel = document.createElement("div");
    rel.className = "rel " + clase;
    rel.style.width = pct.toFixed(4) + "%";
    pista.appendChild(rel);
    td.appendChild(pista);
    return td;
  }

  function cargar(texto_json, nombre) {
    var datos;
    try {
      datos = JSON.parse(texto_json);
    } catch (e) {
      texto(document.getElementById("fuente"), "ese archivo no es JSON válido");
      return;
    }
    if (!Array.isArray(datos) || !datos.length || !("pkg" in datos[0]) || !("miembrosJdk" in datos[0])) {
      texto(document.getElementById("fuente"), "ese JSON no es un censo (falta pkg / miembrosJdk)");
      return;
    }
    actual = datos;
    pintar(datos, true);
    texto(document.getElementById("fuente"), nombre + " · " + datos.length + " paquetes");
    document.getElementById("b-volver").hidden = false;
  }

  document.getElementById("b-abrir").addEventListener("click", function () {
    document.getElementById("archivo").click();
  });

  document.getElementById("archivo").addEventListener("change", function (ev) {
    var f = ev.target.files && ev.target.files[0];
    if (!f) { return; }
    var lector = new FileReader();
    lector.onload = function () { cargar(String(lector.result), f.name); };
    lector.readAsText(f);
    ev.target.value = "";
  });

  document.getElementById("b-volver").addEventListener("click", function () {
    actual = GUARDADO;
    pintar(GUARDADO, false);
    texto(document.getElementById("fuente"), "censo incluido en la página");
    this.hidden = true;
  });

  document.getElementById("b-como").addEventListener("click", function () {
    document.getElementById("comando").classList.toggle("abierto");
  });

  document.getElementById("b-copiar").addEventListener("click", function () {
    var b = this;
    var t = document.getElementById("cmd").textContent;
    var listo = function () { texto(b, "Copiado"); setTimeout(function () { texto(b, "Copiar"); }, 1400); };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(t).then(listo, function () { texto(b, "No se pudo"); });
    } else {
      texto(b, "No se pudo");
    }
  });

  // Soltar el censo en cualquier parte de la página.
  ["dragenter", "dragover"].forEach(function (n) {
    document.addEventListener(n, function (e) {
      e.preventDefault();
      document.body.classList.add("arrastrando");
    });
  });
  ["dragleave", "drop"].forEach(function (n) {
    document.addEventListener(n, function (e) {
      e.preventDefault();
      if (n === "dragleave" && e.relatedTarget) { return; }
      document.body.classList.remove("arrastrando");
    });
  });
  document.addEventListener("drop", function (e) {
    var f = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
    if (!f) { return; }
    var lector = new FileReader();
    lector.onload = function () { cargar(String(lector.result), f.name); };
    lector.readAsText(f);
  });

  pintar(GUARDADO, false);
}());
</script>
"""

if __name__ == "__main__":
    main()
