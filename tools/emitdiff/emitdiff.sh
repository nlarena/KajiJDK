#!/usr/bin/env bash
# Diferencial de EMISIÓN: compila cada .java con NUESTRO javac y con el javac real, desensambla ambos
# .class con el MISMO javap (`-p -c`) y compara el resultado NORMALIZADO (sin los índices del constant
# pool, que difieren por el orden de emisión, pero cuyos comentarios resueltos coinciden).
#
# Mide B6 (fidelidad): un DIFF es una divergencia de emisión real. Distinto de `diffcheck.sh`, que
# compara nuestro javap contra el real sobre el MISMO .class (fidelidad del lector).
#
# Uso:  bash emitdiff.sh <javac_real> <javap_real> <dir_corpus> [-cp <classpath>]
# Corpus en el paquete por defecto (sin `package`), así ambos emiten .class planos.
set -u
JAVAC="$1"; JAVAP="$2"; CORPUS="$3"; shift 3
EXTRA_CP=()
if [ "${1:-}" = "-cp" ]; then EXTRA_CP=(-cp "$2"); fi
OUR="./target/debug/javac"

# Normaliza el desensamblado: quita el encabezado, reemplaza `#\d+` por `#` (el orden del pool difiere;
# el comentario resuelto no), colapsa espacios (el ancho del índice cambiaba el padding) y borra vacías.
# Además **ordena las líneas**: así el orden de los miembros (posición del constructor, etc.) —que
# javac fija distinto y es incidental— no genera ruido; una diferencia real de codegen cambia el
# **contenido** de alguna línea y sí aparece. Se pierde el contexto de qué método, pero la línea (p.ej.
# `13: iinc 3, 1` vs `13: iload_3`) suele bastar para ubicarla.
norm() { sed -E '/^Compiled from/d; s/#[0-9]+/#/g; s/[[:space:]]+/ /g; s/^ //; s/ $//' | grep -v '^$' | sort; }

ok=0; diffc=0; err=0; referr=0
shopt -s nullglob
for jf in "$CORPUS"/*.java; do
  name=$(basename "$jf")
  tmp=$(mktemp -d)
  mkdir -p "$tmp/ours" "$tmp/ref"
  cp "$jf" "$tmp/ours/"
  if ! "$JAVAC" -d "$tmp/ref" "$jf" >/dev/null 2>"$tmp/referr"; then
    referr=$((referr+1)); echo "REFERR: $name"; head -3 "$tmp/referr" | sed 's/^/    /'; rm -rf "$tmp"; continue
  fi
  "$OUR" "${EXTRA_CP[@]}" --emit "$tmp/ours/$name" >/dev/null 2>"$tmp/ourerr"
  ours_classes=("$tmp/ours"/*.class)
  if [ ${#ours_classes[@]} -eq 0 ]; then
    err=$((err+1)); echo "ERR (nuestro javac no emite): $name"; head -4 "$tmp/ourerr" | sed 's/^/    /'; rm -rf "$tmp"; continue
  fi
  status="OK"; report=""
  for refc in "$tmp/ref"/*.class; do
    c=$(basename "$refc")
    ourc="$tmp/ours/$c"
    if [ ! -f "$ourc" ]; then status="DIFF"; report+=$'\n'"  [falta] $c"; continue; fi
    d=$(diff <("$JAVAP" -p -c "$refc" 2>/dev/null | tr -d '\r' | norm) \
             <("$JAVAP" -p -c "$ourc" 2>/dev/null | tr -d '\r' | norm))
    if [ -n "$d" ]; then
      status="DIFF"
      report+=$'\n'"  [~] $c"$'\n'"$(printf '%s\n' "$d" | head -30 | sed 's/^/    /')"
    fi
  done
  # clases que emitimos de más (sintéticas que el real nombra distinto) — informativo
  for ourc in "$tmp/ours"/*.class; do
    c=$(basename "$ourc"); [ -f "$tmp/ref/$c" ] || report+=$'\n'"  [extra] $c (no la produce el real)"
  done
  if [ "$status" = "OK" ] && [ -z "$report" ]; then
    ok=$((ok+1))
  else
    [ "$status" = "DIFF" ] && diffc=$((diffc+1)) || ok=$((ok+1))
    echo "${status}: $name$report"
  fi
  rm -rf "$tmp"
done
echo ""
echo "== resumen == OK=$ok DIFF=$diffc ERR=$err REFERR=$referr"
