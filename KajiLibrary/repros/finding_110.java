// #110 -- leer un campo ESTATICO de otra unidad de compilacion emite `getfield` en vez de
// `getstatic`, y la VM panica con "field_offset: field not found".
//
// Repro de un solo archivo: no hace falta armar dos clases propias, alcanza con cualquier campo
// estatico no-constante de la biblioteca. (Un `static final int` no sirve: es constante de
// compilacion y se inlinea, asi que nunca llega a emitirse un acceso a campo.)
//
//   nuestro javac:  0: getfield  #34  // Field java/lang/String.CASE_INSENSITIVE_ORDER:...
//   javac del JDK:  0: getstatic #13  // Field java/lang/String.CASE_INSENSITIVE_ORDER:...
//
// El control de abajo aisla la causa: la MISMA clase, por un metodo estatico, anda. O sea que no
// es resolver la clase lo que falla, es el flag ACC_STATIC del campo, que se pierde al leer el
// .class del classpath.
import java.util.Comparator;

public class finding_110 {

    // Panica.
    public static int viaCampo() {
        Comparator<String> c = String.CASE_INSENSITIVE_ORDER;
        return c == null ? 1 : 0;
    }

    // Control: anda, devuelve 0.
    public static int viaMetodo() {
        return String.valueOf(1).equals("1") ? 0 : 1;
    }
}
