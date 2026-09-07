package javax.swing.text;

/**
 * Un {@link Error} de invariante rota: el documento quedaria en un estado imposible.
 *
 * <p>Es un error y no una excepcion a proposito. No se lanza por lo que hizo el usuario —eso es
 * {@link BadLocationException}— sino por lo que hizo el programa: cambiar atributos sin el candado
 * de escritura, o soltar un candado de lectura que nunca se tomo. Nada de eso se puede manejar; se
 * arregla en el codigo.
 *
 * <p>No es publica: el JDK tampoco la expone, y quien la vea la vera como {@code Error}.
 */
class StateInvariantError extends Error {

    public StateInvariantError(String s) {
        super(s);
    }
}
