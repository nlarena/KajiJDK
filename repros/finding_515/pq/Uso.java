package pq;

/**
 * Que pasa al ejecutar lo que se emitio.
 *
 * <p>El error no se ve al compilar sino al construir: el {@code invokespecial} apunta a un
 * {@code Base."<init>":()V} que no existe en el archivo compilado de {@link Base}.
 */
public class Uso extends Hija {

    @Override
    public int f() {
        return 7;
    }

    /**
     * Construye uno y devuelve su {@code f()}.
     *
     * @return 7 si anduvo
     */
    public static int run() {
        return new Uso().f();
    }
}
