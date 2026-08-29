// Repro de #204 (= #215) - un parametro de tipo DEL METODO no resolvia dentro de su propio cuerpo.
//
// Se reporto como un fallo de INFERENCIA ("no se pueden inferir los argumentos de tipo de `id`:
// restricciones de tipo incompatibles") y no tenia nada que ver con inferir. La causa: los
// parametros de tipo de un metodo generico viven en un scope PROPIO, colgado del de la clase
// (JLS 8.4.4), y la pasada 2 resolvia la firma y el cuerpo en el de la CLASE. La `A` de
// `<A> A f(A x)` quedaba `Unresolved`, el parametro `x` con ella, y de ahi se caia todo lo que
// dependiera de su tipo.
//
// La pasada 1 ya lo hacia bien (`enter::owner_scope`): las dos pasadas discrepaban, y por eso el
// `Signature` salia correcto mientras el cuerpo no compilaba.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_204.java
//   bin\run-headless.exe KajiLibrary\repros\finding_204.class run   -> 0
//
// Desbloqueo `java/util/Collections.java` y `java/util/Optional.java`, que no compilaban.
public class finding_204 {

    static <T> T id(T x) { return x; }

    /* El caso del finding: inferir `id` con el argumento tipado por la `A` del metodo que envuelve. */
    static <A> A porMetodo(A x) { return finding_204.id(x); }

    /* La raiz, mas ancha que la inferencia: un miembro sobre un parametro de tipo del metodo. */
    static <A> int miembro(A x) { return x.hashCode() == 0 ? 0 : 1; }

    /* Un local declarado con el parametro de tipo del metodo. */
    static <A> A local(A x) { A y = x; return y; }

    /* Control que ya andaba: `A` como parametro de tipo de la CLASE. */
    static class Caja<A> {
        A porClase(A x) { return finding_204.id(x); }
    }

    public static int run() {
        Caja<Integer> c = new Caja<Integer>();
        Integer siete = Integer.valueOf(7);
        if (finding_204.porMetodo(siete).intValue() != 7) { return 1; }
        if (finding_204.local(siete).intValue() != 7) { return 2; }
        if (c.porClase(siete).intValue() != 7) { return 3; }
        return 0;
    }
}
