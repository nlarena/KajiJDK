// El `invokedynamic` de concatenación, en un bucle caliente — la única forma de comprobar que el
// JIT lo compila, porque el censo no ejecuta nada y por lo tanto ninguna clase spun existe cuando
// él mira.
//
// **La concatenación va DENTRO del método caliente, y eso es la prueba y no un detalle de estilo.**
// Con el `+` en un helper aparte, `run` compila igual aunque el indy se rechace —el bucle no tiene
// nada de raro— y entonces `compiled > 0` se cumple sin que lo que se quiere medir haya pasado.
// Estando en el mismo método, refutar el indy refuta `run`, y la aserción vuelve a significar algo.
public class JcCat {
    // **No `final` a propósito.** Con `static final char SEP = ';'` javac **pliega la constante
    // dentro de la receta** y el sitio queda con descriptor `(I)`: el `char` nunca viaja como
    // argumento y `append(C)` no se llama nunca, así que el test no distinguiría un emisor correcto
    // de uno que promueve todo a `int`. Que sea variable lo vuelve un valor de runtime y fuerza el
    // descriptor `(IC)`.
    static char sep = ';';

    static int run() {
        int acc = 0;
        for (int i = 0; i < 2000; i++) {
            String s = "n=" + i + sep;
            acc = (acc + s.length() + s.charAt(0)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
