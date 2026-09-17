"""Convierte los datos de una campaña (datos/campaña-<semilla>.json) en una página HTML autónoma.

Los datos van embebidos en la página: así se puede abrir sin servidor, mandar como archivo o publicar.

    python test-fuzzing/presentar.py [--prueba bigint|strings] [--semilla 1] [--fragmento]

Por defecto escribe un documento completo, para abrir como archivo. Con --fragmento
escribe sólo el contenido, sin doctype ni head, que es lo que espera el publicador
de páginas.
"""
import argparse
import json
from pathlib import Path

AQUI = Path(__file__).resolve().parent

PLANTILLA = r"""<title>__CLASE__ contra OpenJDK</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap">
<style>
  :root {
    --ground: #F5F7F4; --surface: #FFFFFF; --sunken: #EBEFEA; --ink: #15201B; --muted: #5D6962;
    --rule: #D6DDD7; --accent: #1E6B50; --accent-soft: #DCEBE3; --bad: #B2382D; --bad-soft: #F6E0DD;
    --warn: #9C6400; --warn-soft: #F5E8CF; --bar: #1E6B50; --bar-bad: #B2382D;
    --sans: "IBM Plex Sans", system-ui, -apple-system, "Segoe UI", sans-serif;
    --mono: "IBM Plex Mono", ui-monospace, "Cascadia Mono", Consolas, monospace;
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      --ground: #0F1513; --surface: #161F1B; --sunken: #1C2722; --ink: #E4EBE7; --muted: #93A19A;
      --rule: #2B3731; --accent: #56B690; --accent-soft: #1B3329; --bad: #E26C5F; --bad-soft: #3A1F1C;
      --warn: #D9A548; --warn-soft: #362A15; --bar: #56B690; --bar-bad: #E26C5F;
    }
  }
  :root[data-theme="dark"] {
    --ground: #0F1513; --surface: #161F1B; --sunken: #1C2722; --ink: #E4EBE7; --muted: #93A19A;
    --rule: #2B3731; --accent: #56B690; --accent-soft: #1B3329; --bad: #E26C5F; --bad-soft: #3A1F1C;
    --warn: #D9A548; --warn-soft: #362A15; --bar: #56B690; --bar-bad: #E26C5F;
  }
  * { box-sizing: border-box; }
  body { background: var(--ground); color: var(--ink); font-family: var(--sans); font-size: 15px; line-height: 1.55; padding-inline: 20px; padding-block: 32px 56px; }
  .wrap { max-width: 1080px; margin: 0 auto; display: grid; gap: 40px; }
  h1, h2, h3 { margin: 0; text-wrap: balance; line-height: 1.2; }
  h1 { font-size: clamp(26px, 4vw, 34px); font-weight: 700; letter-spacing: -0.01em; }
  h2 { font-size: 19px; font-weight: 600; }
  h3 { font-size: 15px; font-weight: 600; }
  p { margin: 0; }
  section { display: grid; gap: 14px; }
  section > p { max-width: 72ch; color: var(--muted); }
  .eyebrow { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; color: var(--accent); }
  header { display: grid; gap: 12px; }
  .lede { max-width: 70ch; color: var(--muted); font-size: 16px; }
  .lede strong { color: var(--ink); font-weight: 600; }
  .meta { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 13px; color: var(--muted); }
  .meta b { color: var(--ink); font-weight: 600; }
  code, pre, .mono { font-family: var(--mono); font-variant-numeric: tabular-nums; }
  .pill { display: inline-block; font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: 999px; white-space: nowrap; }
  .pill.bad { background: var(--bad-soft); color: var(--bad); }
  .pill.ok { background: var(--accent-soft); color: var(--accent); }
  .pill.warn { background: var(--warn-soft); color: var(--warn); }

  .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 0; border: 1px solid var(--rule); border-radius: 8px; background: var(--surface); overflow: hidden; }
  .tile { padding: 16px; display: grid; gap: 2px; border-left: 1px solid var(--rule); }
  .tile:first-child { border-left: 0; }
  .tile .big { font: 600 28px var(--mono); letter-spacing: -0.02em; }
  .tile .big.bad { color: var(--bad); }
  .tile .lbl { font-size: 13px; color: var(--muted); }
  @media (max-width: 560px) { .tile { border-left: 0; border-top: 1px solid var(--rule); } .tile:first-child { border-top: 0; } }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .divexp { display: grid; gap: 10px; max-width: 72ch; }
  .divexp p { color: var(--muted); }
  .divexp p strong, .divexp li strong { color: var(--ink); font-weight: 600; }
  .divexp ul { margin: 0; padding-left: 20px; display: grid; gap: 6px; color: var(--muted); }
  .divexp code { font-size: 0.92em; }

  .cases { display: grid; gap: 14px; }
  .case { background: var(--surface); border: 1px solid var(--rule); border-radius: 8px; padding: 16px; display: grid; gap: 12px; }
  .case-head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; }
  pre { margin: 0; padding: 10px 12px; background: var(--sunken); border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.5; }
  .vs { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; }
  .side { display: grid; gap: 2px; }
  .side .who { font-size: 12px; color: var(--muted); text-transform: uppercase; letter-spacing: 0.06em; font-weight: 600; }
  .side .val { font: 500 14px var(--mono); word-break: break-all; }
  .side.kaji .val { color: var(--bad); font-weight: 600; }
  details { border-top: 1px solid var(--rule); padding-top: 10px; }
  summary { cursor: pointer; font-weight: 600; font-size: 14px; }
  .steps { margin: 10px 0 0; padding-left: 22px; display: grid; gap: 10px; }
  .steps li { display: grid; gap: 4px; }
  .steps .res { font-size: 13px; color: var(--muted); }
  .steps .res .mono { color: var(--ink); }
  .why { max-width: 72ch; }
  .why code { font-size: 0.92em; }
  .where { font-size: 13px; color: var(--muted); }
  .minimo { display: grid; gap: 10px; border-top: 1px solid var(--rule); padding-top: 12px; }
  .count { font-size: 13px; color: var(--muted); }
  :focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }

  .bars { display: grid; gap: 8px; background: var(--surface); border: 1px solid var(--rule); border-radius: 8px; padding: 16px; }
  .bar-row { display: grid; grid-template-columns: 110px 1fr 150px; gap: 12px; align-items: center; font-size: 13px; }
  .bar-row .name { font-family: var(--mono); }
  .bar-track { height: 10px; background: var(--sunken); border-radius: 5px; overflow: hidden; }
  .bar-fill { height: 100%; background: var(--bar); border-radius: 5px; }
  .bar-fill.hot { background: var(--bar-bad); }
  .bar-row .num { font-family: var(--mono); color: var(--muted); text-align: right; font-variant-numeric: tabular-nums; }
  @media (max-width: 560px) { .bar-row { grid-template-columns: 90px 1fr; } .bar-row .num { grid-column: 1 / -1; text-align: left; } }

  .table-scroll { overflow-x: auto; border: 1px solid var(--rule); border-radius: 8px; background: var(--surface); }
  table { border-collapse: collapse; width: 100%; font-size: 13px; }
  th, td { padding: 8px 10px; text-align: left; vertical-align: top; }
  th { font: 600 11px var(--sans); text-transform: uppercase; letter-spacing: 0.06em; color: var(--muted); background: var(--sunken); white-space: nowrap; }
  td { border-top: 1px solid var(--rule); }
  td code { font-size: 12px; word-break: break-all; }
  td.expr { min-width: 320px; }
  td.v { font-family: var(--mono); font-size: 12px; word-break: break-all; min-width: 140px; }
  td.v.kaji { color: var(--bad); }
  ul.plain, ol.plain { margin: 0; padding-left: 20px; display: grid; gap: 8px; max-width: 72ch; }
  ul.plain li strong, ol.plain li strong { font-weight: 600; }
  footer { font-size: 13px; color: var(--muted); max-width: 72ch; }
  .uses { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 28px; border-top: 1px solid var(--rule); padding-top: 28px; }
  .uses ul { margin: 10px 0 0; padding-left: 20px; display: grid; gap: 8px; max-width: 60ch; }
  .uses li strong { font-weight: 600; }
  .intro { display: grid; gap: 14px; }
  .intro > p { max-width: 72ch; }
  .intro > p strong, .intro li strong { font-weight: 600; }
  .oracles { list-style: none; margin: 4px 0 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
  .oracles li { display: grid; gap: 4px; align-content: start; border-top: 2px solid var(--rule); padding-top: 10px; }
  .oracles li.here { border-top-color: var(--accent); }
  .oracles h3 { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
  .oracles p { color: var(--muted); font-size: 14px; }
  .args { margin: 0; padding-left: 20px; display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px 28px; }
  .args li p { color: var(--muted); font-size: 14px; }
</style>

<div class="wrap">
  <header>
    <span class="eyebrow">Campaña de fuzzing diferencial · datos reales</span>
    <h1>__CLASE__ contra OpenJDK</h1>
  </header>

__INTRO_INICIO__  <section class="intro">
    <h2>Qué es el fuzzing</h2>
    <p>
      El fuzzing es una forma de probar software sin escribir los casos a mano. Un programa genera
      entradas en cantidad, las ejecuta y vigila que no pase nada que no debería. Nació buscando cuelgues
      con datos al azar, y hoy es una de las técnicas que más errores encuentra en compiladores,
      navegadores, parsers y bibliotecas.
    </p>
    <p>
      Generar entradas es lo fácil. Lo difícil es <strong>saber si una respuesta está mal sin conocer la
      correcta</strong>. A eso se le llama el problema del oráculo, y hay tres formas habituales de
      resolverlo:
    </p>
    <ul class="oracles">
      <li>
        <h3>Que no se rompa</h3>
        <p>Solo se vigilan cuelgues, errores inesperados o memoria corrupta. No hace falta saber nada del resultado.</p>
      </li>
      <li>
        <h3>Propiedades</h3>
        <p>Algo que vale siempre: decodificar lo codificado devuelve la entrada, ordenar dos veces da lo mismo que ordenar una.</p>
      </li>
      <li class="here">
        <h3>Diferencial <span class="pill ok">el de este informe</span></h3>
        <p>Dos implementaciones de lo mismo tienen que coincidir. Cualquier diferencia es un error de una de las dos.</p>
      </li>
    </ul>
    <p>
      Dos técnicas lo vuelven práctico. <strong>Generar con sesgo hacia los bordes</strong>, porque los
      errores se concentran en ceros, negativos y límites de tamaño. Y <strong>reducir</strong> cada falla a
      la entrada más chica que todavía la reproduce, para que una persona la entienda de un vistazo.
    </p>

    <h3>Por qué vale la pena</h3>
    <ul class="args">
      <li>
        <strong>No hay que escribir resultados esperados.</strong>
        <p>La versión de referencia cumple ese papel, así que probar mil entradas cuesta lo mismo que probar diez.</p>
      </li>
      <li>
        <strong>Encuentra lo que a nadie se le ocurre probar a mano.</strong>
        <p>Negativos, ceros, límites de 32 y 64 bits, números enormes: los casos que un test escrito a mano suele saltear.</p>
      </li>
      <li>
        <strong>Todo se reproduce desde una semilla.</strong>
        <p>Cualquier hallazgo se puede volver a mostrar, en otra máquina y otro día.</p>
      </li>
      <li>
        <strong>Entrega el caso más chico que falla.</strong>
        <p>La entrada que dispara un error suele ser larga y estar llena de detalles que no tienen nada que ver. El fuzzer la achica sola, quitando pedazos mientras el error siga apareciendo, y quien lo arregla ve directamente dónde está el problema.</p>
      </li>
    </ul>
  </section>

  <section>
    <h2>Cómo implementamos el fuzzer</h2>
    <p>
      El proyecto ya tenía un fuzzer, escrito en Rust, que prueba la máquina virtual y el compilador con
      programas generados. Para la biblioteca escribimos uno más chico, en Python:
      <code>test-fuzzing/fuzzer/__SCRIPT__</code>. Se corre a mano, guarda todo lo que mide en
      <code>test-fuzzing/datos/</code>, y esta página solo muestra esos datos.
    </p>
    <ol class="plain">
      <li><strong>Generar.</strong> Arma al azar expresiones de <code>__CLASE__</code> como árboles de operaciones, de hasta tres niveles, y las escribe como código Java. Nadie las escribe a mano: las operaciones y cómo se anidan salen al azar, y __SORTEO__. Todo parte de una semilla, así que la misma semilla arma siempre las mismas expresiones.</li>
      <li><strong>Ejecutar.</strong> Junta 120 de las expresiones generadas en una sola clase, cada una en su propio método con un <code>try</code>/<code>catch</code> que devuelve el resultado o el nombre de la excepción. La compila una vez con el <code>javac</code> de OpenJDK y corre el mismo <code>.class</code> en OpenJDK y en KajiJDK.</li>
      <li><strong>Comparar.</strong> Lee lo que imprimió cada lado, línea por línea, y compara el texto de cada resultado.</li>
      <li><strong>Reducir.</strong> Para cada tipo de divergencia arma versiones más chicas de la expresión, las corre todas juntas en un solo programa y se queda con la más chica que sigue divergiendo. Repite hasta que no puede achicar más.</li>
    </ol>
__EXTRA__    <p>
      <strong>De dónde sale el azar.</strong> Del generador pseudoaleatorio de Python,
      <code>random.Random</code>, que usa el algoritmo Mersenne Twister y arranca desde la semilla que
      recibe el script con <code>--seed</code>. No es azar verdadero: es una secuencia de números que
      parece aleatoria pero que la semilla deja fija. Cada decisión del generador toma el número
      siguiente de esa secuencia: qué operación usar, si una constante sale de la lista de límites o se
      inventa, cuántos dígitos tiene, de 1 a 60, y si es negativa. Por eso la misma semilla repite la
      campaña entera, y otra semilla prueba expresiones distintas.
    </p>
  </section>
__INTRO_FIN__
  <section>
    <h2>La prueba</h2>
    <p class="lede" id="lede"></p>
    <div class="meta" id="meta"></div>
    <div class="tiles" id="tiles"></div>
    <div class="chips" id="chips"></div>
    <div class="divexp">
      <h3>Qué es una divergencia</h3>
      <div id="divexp"></div>
    </div>
  </section>

  <section>
    <h2>Qué encontró</h2>
    <p>El reductor achicó cada divergencia distinta hasta su caso mínimo, y varias quedaron en la misma expresión. Acá están agrupadas por causa. Los números salen de la campaña; la explicación de cada causa se escribió a mano, después de leer el código de las dos implementaciones y verificarla en OpenJDK.</p>
    <div class="cases" id="cases"></div>
  </section>

  <section>
    <h2>Dónde se concentran las divergencias</h2>
    <p>Para cada operación, en cuántas de las expresiones que la usan hubo divergencia. Que una operación aparezca en una expresión que diverge no prueba que sea la culpable: eso lo dicen los casos mínimos.</p>
    <div class="bars" id="bars"></div>
  </section>

  <section>
    <h2>Todas las divergencias</h2>
    <p>Cada expresión en la que OpenJDK y KajiJDK dieron un resultado distinto, en el orden en que aparecieron.</p>
    <div class="table-scroll">
      <table>
        <thead><tr><th>#</th><th>Tipo</th><th>Expresión generada</th><th>OpenJDK</th><th>KajiJDK</th></tr></thead>
        <tbody id="all"></tbody>
      </table>
    </div>
  </section>

  <section>
    <h2>Cómo se obtuvo</h2>
    <ul class="plain" id="method"></ul>
  </section>

  <section class="uses">
    <div>
      <h2>Dónde sirve</h2>
      <ul>
        <li><strong>Reescrituras y migraciones.</strong> La versión vieja hace de oráculo, así que no hay que escribir el resultado esperado de cada caso.</li>
        <li><strong>Versión contra versión.</strong> La release de hoy contra la de ayer, sobre las mismas entradas generadas.</li>
        <li><strong>Código numérico.</strong> Redondeos, desbordes, divisiones, fechas y coma flotante: donde viven los bordes.</li>
        <li><strong>Parsers y serializadores.</strong> Lo que se codifica y se decodifica tiene que volver igual.</li>
        <li><strong>Dos implementaciones de una misma especificación.</strong> Cualquier diferencia es un error de una de las dos, como KajiLibrary y OpenJDK en este informe.</li>
      </ul>
    </div>
    <div>
      <h2>Lo que no resuelve</h2>
      <ul>
        <li><strong>Necesita una referencia.</strong> Una implementación confiable, o una propiedad que valga siempre, como que decodificar lo codificado devuelva la entrada.</li>
        <li><strong>Necesita determinismo.</strong> Fecha, hora y todo lo aleatorio tienen que quedar fijos, o el oráculo reporta ruido.</li>
        <li><strong>Encuentra diferencias, no culpables.</strong> Qué versión tiene razón lo decide una persona con la especificación en la mano.</li>
        <li><strong>Hay que auditar el instrumento.</strong> Un oráculo roto, por ejemplo uno que compare una implementación consigo misma, reporta cero divergencias aunque haya errores.</li>
      </ul>
    </div>
  </section>

  <footer id="footer"></footer>
</div>

<script id="datos" type="application/json">__DATOS__</script>
<script>
(() => {
  const RAW = JSON.parse(document.getElementById("datos").textContent);
  const D = RAW.campaña;
  const G = RAW.diagnostico || { causas: [] };
  const $ = id => document.getElementById(id);
  const n = x => Number(x).toLocaleString("es-AR");
  const esc = s => String(s ?? "—").replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const S = D.stats, M = D.meta;
  const fecha = new Date(M.fecha).toLocaleString("es-AR", { dateStyle: "long", timeStyle: "short" });
  const minutos = S.segundos_total / 60;

  $("lede").innerHTML =
    `El fuzzer generó expresiones de <strong>__FQN__</strong> con valores elegidos hacia los bordes, ` +
    `y las corrió en <strong>${esc(M.referencia)}</strong> y en <strong>KajiJDK con KajiLibrary</strong>. ` +
    `O sea, se llamó a dos clases que deberían dar el mismo resultado, el <code>__CLASE__</code> de OpenJDK y el de KajiLibrary, ` +
    `y se las probó con entradas aleatorias. ` +
    `Después comparó los resultados uno por uno y redujo cada divergencia distinta a su caso más chico.`;

  const estado = D.estado === "terminada"
    ? `<span class="pill ok">Campaña terminada</span>`
    : `<span class="pill warn">Campaña incompleta: ${esc(D.estado)}</span>`;
  $("meta").innerHTML = [
    estado,
    `<span>Referencia <b>${esc(M.referencia)}</b></span>`,
    `<span>Bajo prueba <b>${esc(M.bajo_prueba)}</b></span>`,
    `<span>Semilla <b>${esc(M.semilla)}</b> · ${n(M.lotes)} lotes de ${n(M.por_lote)}</span>`,
    `<span>${esc(fecha)}</span>`,
  ].join("");

  const tiles = [
    [n(S.expresiones), "expresiones comparadas", ""],
    [n(S.coinciden), "dieron lo mismo", ""],
    [n(S.divergen), "divergieron", "bad"],
    [n(G.causas.filter(c => c.tipo === "error").length), "errores distintos en KajiLibrary", "bad"],
    [n(D.reducidas.length), "casos mínimos", ""],
    [minutos >= 1 ? `${minutos.toLocaleString("es-AR", { maximumFractionDigits: 1 })} min` : `${n(Math.round(S.segundos_total))} s`, "de campaña", ""],
  ];
  $("tiles").innerHTML = tiles.map(([v, l, c]) => `<div class="tile"><span class="big ${c}">${v}</span><span class="lbl">${l}</span></div>`).join("");
  $("chips").innerHTML = Object.entries(S.por_tipo).sort((a, b) => b[1] - a[1])
    .map(([t, c]) => `<span class="pill bad">${esc(t)}: ${n(c)}</span>`).join("");

  // Cada expresión mínima puede venir de varias divergencias; cada causa diagnosticada, de varias
  // expresiones mínimas. Lo que no figura en el diagnóstico se muestra igual, como pendiente.
  const porMinimo = new Map();
  for (const r of D.reducidas) {
    if (!porMinimo.has(r.minimo)) porMinimo.set(r.minimo, []);
    porMinimo.get(r.minimo).push(r);
  }
  const grupos = G.causas
    .map(c => ({ c, minimos: c.casos.filter(m => porMinimo.has(m)) }))
    .filter(g => g.minimos.length);
  const sinCausa = [...porMinimo.keys()].filter(m => !G.causas.some(c => c.casos.includes(m)));
  if (sinCausa.length) {
    grupos.push({ c: { tipo: "pendiente", titulo: "Sin diagnosticar", explicacion: "Estos casos todavía no se analizaron." }, minimos: sinCausa });
  }
  const pill = c => c.tipo === "error"
    ? `<span class="pill bad">Error de KajiLibrary</span>`
    : c.tipo === "consecuencia"
      ? `<span class="pill warn">Consecuencia de otro error</span>`
      : `<span class="pill warn">Pendiente</span>`;
  const reduccion = r => `
    <details>
      <summary>Cómo se redujo: de ${n(r.original.length)} a ${n(r.minimo.length)} caracteres en ${n(r.pasos.length)} pasos</summary>
      <ol class="steps">
        <li><span>Expresión generada:</span><pre><code>${esc(r.original)}</code></pre>
          <span class="res">OpenJDK <span class="mono">${esc(r.original_jdk)}</span> · KajiJDK <span class="mono">${esc(r.original_kaji)}</span></span></li>
        ${r.pasos.map(p => `<li><pre><code>${esc(p.java)}</code></pre>
          <span class="res">OpenJDK <span class="mono">${esc(p.jdk)}</span> · KajiJDK <span class="mono">${esc(p.ours)}</span></span></li>`).join("")}
      </ol>
    </details>`;
  // --- qué es una divergencia, con un ejemplo de esta misma campaña ---
  const TIPOS = {
    "otro valor": "las dos devolvieron un resultado, pero distinto.",
    "excepción contra valor": "una lanzó una excepción y la otra devolvió un resultado.",
    "otra excepción": "las dos lanzaron una excepción, pero de clases distintas.",
    "falta en la biblioteca": "KajiJDK no encontró el método o la clase que usa la expresión.",
    "sin salida (crash o cuelgue)": "KajiJDK no llegó a devolver un resultado: se cayó o no terminó a tiempo.",
  };
  const causaError = G.causas.find(c => c.tipo === "error");
  const ejemplo = (causaError && D.reducidas.find(r => causaError.casos.includes(r.minimo))) || D.reducidas[0];
  const errores = G.causas.filter(c => c.tipo === "error").length;
  $("divexp").innerHTML =
    `<p>Una <strong>divergencia</strong> es una expresión en la que OpenJDK y KajiJDK devolvieron resultados distintos. ` +
    (ejemplo ? `Por ejemplo, <code>${esc(ejemplo.minimo)}</code> da <code>${esc(ejemplo.minimo_jdk)}</code> en OpenJDK y <code>${esc(ejemplo.minimo_kaji)}</code> en KajiJDK. ` : "") +
    `Como OpenJDK hace de referencia, cada divergencia indica que KajiLibrary se aparta de lo que tendría que hacer.</p>` +
    `<p>Las etiquetas de arriba dicen de qué forma difieren:</p>` +
    `<ul>${Object.keys(S.por_tipo).sort((a, b) => S.por_tipo[b] - S.por_tipo[a])
      .map(t => `<li><strong>${esc(t)}</strong>: ${esc(TIPOS[t] || "los resultados no coinciden.")}</li>`).join("")}</ul>` +
    `<p>Una divergencia no es lo mismo que un error: varias pueden salir del mismo error, con entradas distintas. ` +
    (errores ? `Las ${n(S.divergen)} divergencias de esta campaña se explican con ${n(errores)} ${errores === 1 ? "error" : "errores"}, que se detallan abajo.` : "") +
    `</p>`;

  $("cases").innerHTML = grupos.length ? grupos.map(({ c, minimos }) => `
    <article class="case">
      <div class="case-head"><h3>${esc(c.titulo)}</h3>${pill(c)}</div>
      <p class="why">${c.explicacion}</p>
      ${c.donde ? `<p class="where">${esc(c.donde)}</p>` : ""}
      ${minimos.map(m => {
        const rs = porMinimo.get(m), r0 = rs[0];
        return `
        <div class="minimo">
          <pre><code>${esc(m)}</code></pre>
          <div class="vs">
            <div class="side"><span class="who">OpenJDK</span><span class="val">${esc(r0.minimo_jdk)}</span></div>
            <div class="side kaji"><span class="who">KajiJDK</span><span class="val">${esc(r0.minimo_kaji)}</span></div>
          </div>
          <p class="count">${rs.length === 1 ? "Una divergencia de la campaña se redujo" : `${n(rs.length)} divergencias de la campaña se redujeron`} a esta expresión.</p>
          ${rs.map(reduccion).join("")}
        </div>`;
      }).join("")}
    </article>`).join("") : `<p>Esta campaña todavía no tiene casos reducidos.</p>`;

  const ops = Object.keys(S.ops_total).map(op => {
    const t = S.ops_total[op], d = S.ops_divergentes[op] || 0;
    return { op, t, d, r: t ? d / t : 0 };
  }).sort((a, b) => b.r - a.r || b.t - a.t);
  const maxR = Math.max(...ops.map(o => o.r), 0.0001);
  $("bars").innerHTML = ops.map(o => `
    <div class="bar-row">
      <span class="name">${esc(o.op)}</span>
      <span class="bar-track"><span class="bar-fill ${o.r >= maxR * 0.5 && o.d ? "hot" : ""}" style="width:${(o.r / maxR) * 100}%; display:block"></span></span>
      <span class="num">${n(o.d)} de ${n(o.t)} · ${(o.r * 100).toLocaleString("es-AR", { maximumFractionDigits: 1 })} %</span>
    </div>`).join("");

  $("all").innerHTML = D.divergencias.map((d, i) => `
    <tr>
      <td class="mono">${i + 1}</td>
      <td><span class="pill bad">${esc(d.tipo)}</span></td>
      <td class="expr"><code>${esc(d.java)}</code></td>
      <td class="v">${esc(d.jdk)}</td>
      <td class="v kaji">${esc(d.kaji)}</td>
    </tr>`).join("");

  $("method").innerHTML = [
    `<li><strong>Generar.</strong> ${n(M.lotes)} lotes de ${n(M.por_lote)} expresiones de <code>__CLASE__</code>, de hasta tres niveles de operaciones anidadas. __BORDES__</li>`,
    `<li><strong>Ejecutar.</strong> Cada lote es un único programa Java, compilado una sola vez con el <code>javac</code> de ${esc(M.referencia)}. El mismo <code>.class</code> corre en ese JDK y en KajiJDK con KajiLibrary.</li>`,
    `<li><strong>Comparar.</strong> Dos resultados coinciden si su texto es idéntico. Cuando una expresión lanza una excepción se compara la clase de la excepción, no el mensaje, que puede variar entre implementaciones sin estar mal.</li>`,
    `<li><strong>Reducir.</strong> De cada tipo de divergencia distinto se toma la primera, y se prueban versiones más chicas: quitar una operación, cambiar una constante por 0, 1, −1 o 2, acortar sus dígitos. Se queda con la más chica que todavía diverge del mismo modo, y repite.</li>`,
    `<li><strong>Reproducir.</strong> La misma semilla genera las mismas expresiones: <code>python test-fuzzing/fuzzer/__SCRIPT__ --jdk "$OPENJDK" --seed ${esc(M.semilla)} --lotes ${esc(M.lotes)} --por-lote ${esc(M.por_lote)}</code>.</li>`,
  ].join("");

  $("footer").textContent =
    `Datos de ${fecha}. KajiJDK compilado el ${new Date(M.binario_kaji_fecha).toLocaleString("es-AR", { dateStyle: "long", timeStyle: "short" })}. ` +
    `Tiempo de ejecución: ${n(Math.round(S.segundos_jdk))} s en OpenJDK y ${n(Math.round(S.segundos_kaji))} s en KajiJDK, sin contar la reducción.`;
})();
</script>
"""


def documento(fragmento):
    """Envuelve el fragmento en un documento completo, para abrirlo como archivo.

    Una página publicada no lleva doctype, charset ni head: se los pone el publicador. Abierta desde el
    disco, sin `<meta charset>` el navegador puede leer los acentos con otra codificación, y sin
    doctype la renderiza en modo de compatibilidad.
    """
    corte = fragmento.index("</style>") + len("</style>")
    cabeza, cuerpo = fragmento[:corte], fragmento[corte:]
    return (
        "<!doctype html>\n<html lang=\"es\">\n<head>\n"
        "<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
        + cabeza.replace("<style>", "<style>\n  body { margin: 0; }", 1)
        + "\n</head>\n<body>\n" + cuerpo.strip() + "\n</body>\n</html>\n"
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--prueba", choices=("bigint", "strings"), default="bigint",
                    help="qué campaña presentar: la de BigInteger o la de String")
    ap.add_argument("--semilla", type=int, default=1)
    ap.add_argument("--salida", default=None)
    ap.add_argument("--fragmento", action="store_true",
                    help="escribir sólo el contenido, para publicar la página")
    a = ap.parse_args()
    PRUEBAS = {
        "bigint": {
            "datos": f"campaña-{a.semilla}.json", "diagnostico": "diagnostico.json",
            "html": "informe.html", "clase": "BigInteger", "fqn": "java.math.BigInteger",
            "script": "bigfuzz.py",
            "sorteo": "las constantes salen, seis de cada diez veces, de una lista fija de valores límite",
            "bordes": "Las constantes salen, seis de cada diez veces, de una lista de bordes: cero, uno, menos uno, los límites de 32 y 64 bits y números de cientos de dígitos.",
            "extra": "",
            "intro": True,
        },
        "strings": {
            "datos": f"strings-{a.semilla}.json", "diagnostico": "diagnostico-strings.json",
            "html": "informe-strings.html", "clase": "String", "fqn": "java.lang.String",
            "script": "strfuzz.py",
            "sorteo": "los caracteres de cada cadena salen, casi la mitad de las veces, de una lista fija de casos límite",
            "bordes": "Los caracteres salen, casi la mitad de las veces, de una lista de bordes: espacios raros, la ß alemana, la I y la i turcas, la sigma final griega, marcas combinables, surrogates sueltos y emojis de fuera del plano básico.",
            "extra": """    <p>
      <strong>Los caracteres no viajan como caracteres.</strong> Cada cadena se escribe en el programa
      generado como la lista de sus códigos, <code>s(65, 55296)</code>, y cada resultado se imprime
      igual, <code>[65,55296]</code>. Así ni el código fuente ni la consola de cada lado pueden cambiar
      un carácter, y lo que se compara es lo que devolvió la biblioteca.
    </p>
""",
            "intro": False,
        },
    }
    P = PRUEBAS[a.prueba]
    datos = json.loads((AQUI / "datos" / P["datos"]).read_text(encoding="utf-8"))
    diag_path = AQUI / "datos" / P["diagnostico"]
    diagnostico = json.loads(diag_path.read_text(encoding="utf-8")) if diag_path.exists() else None
    embebido = json.dumps({"campaña": datos, "diagnostico": diagnostico}, ensure_ascii=False).replace("</", "<\\/")
    salida = Path(a.salida) if a.salida else AQUI / P["html"]
    pagina = PLANTILLA.replace("__DATOS__", embebido)
    if P["intro"]:
        pagina = pagina.replace("__INTRO_INICIO__", "").replace("__INTRO_FIN__", "")
    else:
        marca = "__INTRO_FIN__" + chr(10)
        ini, fin = pagina.index("__INTRO_INICIO__"), pagina.index(marca) + len(marca)
        pagina = pagina[:ini] + pagina[fin:]
    for marca, valor in (("__CLASE__", P["clase"]), ("__FQN__", P["fqn"]), ("__SCRIPT__", P["script"]),
                         ("__SORTEO__", P["sorteo"]), ("__BORDES__", P["bordes"]), ("__EXTRA__", P["extra"])):
        pagina = pagina.replace(marca, valor)
    if not a.fragmento:
        pagina = documento(pagina)
    salida.write_text(pagina, encoding="utf-8")
    print(f"{salida} · estado {datos['estado']} · {datos['stats']['divergen']} divergencias · "
          f"{len(datos['reducidas'])} casos mínimos")


if __name__ == "__main__":
    main()
