import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * Un argumento con comodin a un constructor de una clase generica.
 *
 * <p>El JDK compila las cuatro. Este compilador falla solo en la primera: constructor, tipo del
 * parametro que menciona la variable de la clase, y argumento con comodin. La misma llamada escrita
 * como metodo anda, y el mismo constructor con un argumento sin comodin tambien.
 */
public class Finding519 {

    static class Caja<E> {

        Caja(Collection<? extends E> c) {
        }

        void meter(Collection<? extends E> c) {
        }
    }

    /** Constructor + comodin: <strong>falla</strong>. */
    static Caja<Object> porConstructor(Vector<?> v) {
        return new Caja<Object>(v);
    }

    /** El mismo constructor, argumento sin comodin: anda. */
    static Caja<Object> constructorSinComodin(List<String> l) {
        return new Caja<Object>(l);
    }

    /** El mismo tipo de parametro, pero por metodo, con comodin: anda. */
    static void porMetodo(Caja<Object> c, Vector<?> v) {
        c.meter(v);
    }

    /** Y un metodo estatico con el mismo parametro, con comodin: anda. */
    static void aObjeto(Collection<? extends Object> c) {
    }

    static void porEstatico(Vector<?> v) {
        aObjeto(v);
    }

    public static void main(String[] a) {
        System.out.println("ok");
    }
}
