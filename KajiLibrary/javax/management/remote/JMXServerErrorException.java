package javax.management.remote;

import java.io.IOException;

/**
 * KajiLibrary's javax.management.remote.JMXServerErrorException -- el servidor tiro un {@link Error}.
 *
 * <p>Existe por un problema concreto de las llamadas remotas. Un {@code Error} del lado servidor
 * --sin memoria, una clase que falta-- no se puede propagar tal cual al cliente: alla significaria que
 * <b>el cliente</b> esta roto, y no lo esta.
 *
 * <p>Asi que se envuelve en una {@link IOException}, que es lo que el cliente ya esta preparado para
 * atajar cuando habla por red. La causa sigue siendo el {@code Error} original, para poder verlo.
 *
 * <p>El unico constructor exige el {@code Error}: sin el la clase no tendria sentido.
 */
public class JMXServerErrorException extends IOException {

    private static final long serialVersionUID = 3996732239558744666L;

    /** El del servidor. */
    private Error cause = null;

    /**
     * @param s el mensaje
     * @param err el error del servidor
     */
    public JMXServerErrorException(String s, Error err) {
        super(s);
        this.cause = err;
    }

    /** El error del servidor. */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
