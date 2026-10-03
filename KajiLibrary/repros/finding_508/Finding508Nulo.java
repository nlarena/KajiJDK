/**
 * La otra mitad del #508, que el reporte original tenia anotada como un caso que <em>andaba</em>.
 *
 * <p>La tabla del hallazgo decia que con la clase externa anidada la figura compilaba, y de ahi
 * concluia que el mecanismo de instancia envolvente implicita "funciona un nivel mas abajo". Medido
 * caso por caso, la variable no era esa: es si el <b>constructor lleva argumentos</b>. Con
 * argumentos falla el nivel superior <em>y</em> la anidada; sin argumentos compilan las dos.
 *
 * <p>Y "compilan" no queria decir que anduvieran. Sin argumentos, el `new` de la hermana sale con la
 * instancia envolvente en <b>null</b>: el `.class` se escribe sin una palabra de aviso y el programa
 * muere con {@code NullPointerException} en cuanto la hermana toca un campo de la externa. Por eso
 * hace falta que la hermana <em>use</em> la envolvente --{@code leer()} devuelve {@code dato}--: un
 * metodo que devuelva una constante corre igual con la envolvente en null y no delata nada.
 *
 * <p>El JDK 25 imprime 42.
 */
public class Finding508Nulo {

    int dato = 42;

    class Hermana {
        Hermana() {
        }

        int leer() {
            return dato;
        }
    }

    class Otra {
        int usa() {
            return new Hermana().leer();
        }
    }

    public static int run() {
        Finding508Nulo a = new Finding508Nulo();
        System.out.println("//r " + a.new Otra().usa());
        return 0;
    }

    public static void main(String[] x) {
        run();
    }
}
