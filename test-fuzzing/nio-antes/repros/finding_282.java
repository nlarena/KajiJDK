// Repro de #282 - la concatenacion de una referencia no-String producia basura en silencio.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_282.java
//   run-headless KajiLibrary\repros\finding_282.class unaReferencia         -> 2
//   run-headless KajiLibrary\repros\finding_282.class dosReferencias        -> 4
//   run-headless KajiLibrary\repros\finding_282.class conToStringExplicito  -> 4
//   run-headless KajiLibrary\repros\finding_282.class valueOfAnda           -> 2
//   run-headless KajiLibrary\repros\finding_282.class primitivos            -> 6
//
// ANTES, y compilando sin una sola queja:
//
//   "" + new Locale("es")                         -> 168 caracteres de basura (java real: 2)
//   "" + new Locale("es") + new Locale("es")      -> panic:
//        heap.rs:878: range end index 4672 out of range for slice of length 4670
//
// Los caracteres devueltos eran NUL y valores arbitrarios: memoria del objeto leida como UTF-16.
//
// La causa se veia comparando los dos emisores para la misma linea. El JDK inserta
// `invokestatic String.valueOf(Object)` ANTES del `invokedynamic` y su call site lleva
// `(Ljava/lang/String;)`; el nuestro mandaba el objeto crudo con `(Ljava/util/Locale;)`. Del otro
// lado, `render` en `invokedynamic.rs` hacia `strings::read` sobre CUALQUIER referencia no nula,
// o sea leia los campos del `Locale` como si fueran el `char[]` de un `String`.
//
// Lo notable: el comentario de `render` ya lo habia previsto textualmente — "javac never sends
// anything else - it inserts String.valueOf(Object) before the call site - but another compiler
// could, ours included". Ours did.
//
// AHORA: arreglado de los dos lados.
//
//   desugar/codegen  una referencia no-String viaja convertida, con `String.valueOf(Object)`
//                    delante y `Ljava/lang/String;` en el descriptor. El bytecode de
//                    `"" + locale` sale instruccion por instruccion igual al del JDK 25.
//   invokedynamic.rs `render` exige `Ljava/lang/String;` y falla con un mensaje que nombra el
//                    descriptor que llego. La suposicion pasa a estar comprobada en runtime.
//
// NO es duplicado de #114, es su sucesor: aquel era el error DURO por falta de
// `StringBuilder.append(Object)`, y se dio por cerrado con el argumento de que "la familia entera
// desaparecio con el cambio de estrategia" a invokedynamic. El cambio saco el sintoma viejo y
// metio este, que era peor: antes no compilaba, despues compilaba y corrompia.
//
// Los tres controles siguen ahi porque acotan el alcance: solo las referencias no-`String`. Los
// operandos `String` y los primitivos nunca estuvieron afectados.
//
// Queda como REGRESION.
public class finding_282 {

    // 2 ("es"). Antes del arreglo: 168 caracteres de basura.
    public static int unaReferencia() {
        String s = "" + new java.util.Locale("es");
        return s.length();
    }

    // 4. Antes del arreglo: panic leyendo fuera del heap.
    public static int dosReferencias() {
        String s = "" + new java.util.Locale("es") + new java.util.Locale("es");
        return s.length();
    }

    // Control: con .toString() explicito el resultado es correcto (4), porque entonces el
    // operando ya es un String y `render` acierta por casualidad.
    public static int conToStringExplicito() {
        String s = new java.util.Locale("es").toString() + new java.util.Locale("es").toString();
        return s.length();
    }

    // Control: String.valueOf(Object) por si solo funciona bien -> 2.
    public static int valueOfAnda() {
        return String.valueOf(new java.util.Locale("es")).length();
    }

    // Control: los primitivos y los String no estan afectados -> 6.
    public static int primitivos() {
        String s = "a" + 5 + true;
        return s.length();
    }
}
