// Repro de #128 — no hay forma de meter un caracter ASTRAL (fuera del BMP) en una constante.
//
// Las dos mitades del problema son independientes y se prueban por separado:
//
//   1. ESCAPE SUBROGADO. `astralEscapado()` no compila: "literal string invalido". Un String de
//      Java es UTF-16, asi que la forma portable y canonica de escribir U+1D160 es el par
//      subrogado "\ud834\udd60" — es lo que emite el propio javac al leer la fuente. Nuestro
//      lexer decodifica cada \uXXXX a un `char` de Rust, y D800..DFFF no es un `char` de Rust
//      valido, asi que lo rechaza. Lo mismo pasa en un literal de char ('\ud800').
//      Los escapes del BMP andan bien en AMBOS tipos de literal — el rango subrogado es lo unico
//      que falla, y es justo el rango que hace falta.
//
//   2. UTF-8 MODIFICADO. `astralLiteral()` SI compila, escribiendo el caracter directo en la
//      fuente UTF-8, pero el .class que sale esta mal formado: el emisor escribe el code point en
//      UTF-8 estandar (f0 9d 85 a0, 4 bytes) cuando CONSTANT_Utf8 exige UTF-8 MODIFICADO, o sea el
//      par subrogado con cada mitad en 3 bytes (ed a0 b4 ed b5 a0). Nuestro propio cargador lo
//      rechaza con BadUtf8, asi que ni siquiera es "no estandar pero anda".
//
// Juntas: no queda ninguna via. Descomentar el metodo de abajo para ver (1).
public class finding_128 {

    // No compila: "literal string invalido".
    // static int astralEscapado() {
    //     return "\ud834\udd60".length();
    // }

    /** Compila, pero produce un .class que nuestro cargador rechaza con BadUtf8. Debe dar 2. */
    static int astralLiteral() {
        return "𝅘𝅥𝅮".length();
    }
}
