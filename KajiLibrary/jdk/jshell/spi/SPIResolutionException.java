package jdk.jshell.spi;

/**
 * La marca que `jshell` deja en un fragmento que usa algo que todavia no existe.
 *
 * <p>Es la pieza del truco que hace que una consola se pueda usar de arriba hacia abajo: en
 * `jshell` uno puede escribir un metodo que llame a otro que va a definir despues, y el fragmento
 * **compila**. Como compila, si lo que falta no estuviera representado de algun modo, el `.class`
 * no seria valido; asi que `jshell` genera en su lugar un cuerpo que lanza esto, con un
 * identificador que dice **cual** de las referencias pendientes se toco.
 *
 * <p>El motor la reconoce por el nombre de su clase, le saca el {@link #id()} y se la devuelve a
 * `jshell` como una {@link ExecutionControl.ResolutionException}, que es la que `jshell` sabe
 * traducir a "todavia no definiste tal cosa".
 *
 * <p>La lanza el codigo generado, no el motor: nadie deberia construirla a mano.
 *
 * @since 9
 */
public class SPIResolutionException extends RuntimeException {

    private final int id;

    /**
     * Con ese identificador de referencia pendiente.
     *
     * @param id el identificador que `jshell` le asigno a lo que falta
     */
    public SPIResolutionException(int id) {
        super("resolution exception " + id);
        this.id = id;
    }

    /** El identificador de la referencia pendiente. */
    public int id() {
        return this.id;
    }
}
