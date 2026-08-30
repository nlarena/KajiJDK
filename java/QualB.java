public class QualB {
    // Homonimo con distinta firma en ESTA clase.
    public static int f(Object x, Object y) { return 2; }

    // Deberia llamar a QualA.f(Object[], int) -> 1. Si resuelve contra QualB.f -> 2.
    public static int run() {
        return QualA.f(new Object[0], 0);
    }
}
