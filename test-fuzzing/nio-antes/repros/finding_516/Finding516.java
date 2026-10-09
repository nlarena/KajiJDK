/** Aisla la herencia: el campo de comodines se HEREDA de una superclase generica. */
public class Finding516 {

    static class Caja<T> {
        void tomar(Caja<? extends T> otra) {
        }
    }

    static class Base<M> {
        Caja<? super M> propio = null;
        Caja<? super M>[] arreglo = null;
    }

    static class Hija<M> extends Base<M> {
        void a(Caja<? extends M> v) {
            propio.tomar(v);        // campo simple heredado
        }

        void b(Caja<? extends M> v) {
            arreglo[0].tomar(v);    // arreglo heredado
        }
    }

    public static void main(String[] a) {
        System.out.println("ok");
    }
}
