// #460 -- la asignacion encadenada sobre dos CAMPOS emite bytecode que vacia la pila.
//
// Los cuatro metodos deberian dar -1. Con nuestro javac, los que asignan encadenado sobre campos
// revientan con `frame.rs:236: operand stack underflow`; el JDK 25 compila y corre los cuatro.
//
// El constructor se deja vacio a proposito: si llevara la asignacion encadenada, los controles
// tambien reventarian al construir el objeto y no acotarian nada.
public class finding_460 {

    Object p, q;
    int a, b;

    /** El que falla: `p = q = valor` sobre dos campos de referencia. */
    public static int run() {
        finding_460 x = new finding_460();
        x.p = x.q = "z";
        return ("z".equals(x.p) && "z".equals(x.q)) ? -1 : 0;
    }

    /** El que falla tambien: los mismos campos, primitivos. No es cosa de referencias. */
    public static int primitivos() {
        finding_460 x = new finding_460();
        x.a = x.b = 7;
        return (x.a == 7 && x.b == 7) ? -1 : 0;
    }

    /** Control: la misma forma sobre locales. Anda. */
    public static int conLocales() {
        Object p, q;
        p = q = "z";
        return ("z".equals(p) && "z".equals(q)) ? -1 : 0;
    }

    /** Control: los mismos dos campos, en sentencias separadas. Anda. */
    public static int separadas() {
        finding_460 x = new finding_460();
        x.q = "z";
        x.p = "z";
        return ("z".equals(x.p) && "z".equals(x.q)) ? -1 : 0;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
