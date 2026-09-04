package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AsynchronousChannel — un canal cuyas operaciones no esperan.
 *
 * <p>La diferencia con un canal no bloqueante comun no es de grado sino de forma: uno no bloqueante
 * **hace lo que puede ahora** y devuelve cuanto hizo, mientras que uno asincronico **acepta el pedido
 * entero** y avisa despues, por un `Future` o por un {@link CompletionHandler}.
 *
 * <p>{@link #close()} se redeclara para documentar lo que pasa con lo que quedo pendiente: toda
 * operacion en curso termina con {@link AsynchronousCloseException}. Esa garantia es lo que hace que
 * cerrar sea suficiente para limpiar --ningun pedido queda esperando una respuesta que no va a
 * llegar--.
 *
 */
public interface AsynchronousChannel extends Channel {

    /** Cierra el canal; lo pendiente termina con una excepcion, no en silencio. */
    void close() throws java.io.IOException;
}
