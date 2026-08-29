// Probe extra para #225: despacho por interfaz con implementacion de JAVA PURO.
// El hallazgo dice que la nativez no es el disparador; esto lo comprueba por separado.
public class P225Pure {
    interface Greeter { int n(); }
    static class Impl implements Greeter { public int n() { return 5; } }

    public static int run() {
        Greeter g = new Impl();
        return g.n();
    }
}