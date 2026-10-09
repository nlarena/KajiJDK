import java.util.List;

/**
 * Llamar un metodo <em>directamente</em> sobre el resultado de un metodo cuyo tipo es un comodin.
 *
 * <p>El JDK compila las cuatro. Este compilador falla en las tres que pasan por un comodin, con
 * "el generador de bytecode todavia no soporta una llamada que no resolvio a ningun metodo".
 */
public class Finding517 {

    static class X {
        public int valor() {
            return 1;
        }
    }

    /** Sin comodin: anda. */
    static int exacto(List<X> l) {
        return l.get(0).valor();
    }

    /** Por una local del tipo declarado: anda, y es el rodeo. */
    static int conLocal(List<? extends X> l) {
        X x = l.get(0);
        return x.valor();
    }

    /** `? extends X`: falla. */
    static int comodinExtends(List<? extends X> l) {
        return l.get(0).valor();
    }

    /** `? super X`, y ni siquiera un metodo de X -- uno de Object: falla igual. */
    static int comodinSuper(List<? super X> l) {
        return l.get(0).hashCode();
    }

    /** `?` pelado: falla. */
    static int comodinPelado(List<?> l) {
        return l.get(0).hashCode();
    }

    public static void main(String[] a) {
        System.out.println("ok");
    }
}
