package repros.finding_522;

/**
 * Una clase interna que llama a un metodo HEREDADO de la clase externa compila y revienta.
 *
 * <p>{@code Interna.usarHeredado()} invoca {@code saludo()}, que {@code Externa} no declara: lo
 * hereda de {@code Base}. El compilador acepta la llamada y emite la invocacion contra
 * {@code Externa}, que no tiene ese metodo. En ejecucion sale {@code NoSuchMethodError}.
 *
 * <p>Con un metodo declarado en la propia {@code Externa} -- {@code propio()} -- anda bien, asi que
 * lo que falla es solo la resolucion por la cadena de herencia de la clase de afuera.
 *
 * <p>Es una miscompilacion silenciosa: no hay aviso al compilar.
 *
 * <p>Salida del JDK real:
 *
 * <pre>
 * propio desde adentro: soy propio
 * heredado desde adentro: hola
 * </pre>
 *
 * <p>Aca la segunda linea es {@code java.lang.NoSuchMethodError}.
 */
public class Finding522 {

    static class Base {
        String saludo() {
            return "hola";
        }
    }

    static class Externa extends Base {

        String propio() {
            return "soy propio";
        }

        Interna nueva() {
            return new Interna();
        }

        class Interna {
            String usarPropio() {
                return propio();
            }

            String usarHeredado() {
                return saludo();
            }
        }
    }

    public static int run() {
        Externa e = new Externa();
        Externa.Interna i = e.nueva();
        System.out.println("propio desde adentro: " + i.usarPropio());
        try {
            System.out.println("heredado desde adentro: " + i.usarHeredado());
        } catch (Throwable t) {
            System.out.println("heredado desde adentro: " + t);
        }
        return 0;
    }

    public static void main(String[] a) {
        run();
    }
}
