package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.CompletionHandler — que hacer cuando una operacion asincronica
 * termina.
 *
 * <p>Dos metodos y no uno, y ahi esta todo el diseno: el exito y el fracaso llegan por caminos
 * separados, asi que no hay forma de escribir el caso feliz y olvidarse del otro. Con un solo
 * callback que recibiera "resultado o error", olvidarse de mirar el error seria lo comodo.
 *
 * <p>El `attachment` es un objeto que el que pidio la operacion entrega y recibe de vuelta sin que
 * nadie lo toque. Existe porque un manejador suele ser compartido por muchas operaciones y necesita
 * saber a cual corresponde cada aviso — sin obligar a crear un manejador nuevo por operacion.
 *
 * @param <V> el tipo del resultado
 * @param <A> el tipo del objeto adjunto
 */
public interface CompletionHandler<V, A> {

    /** La operacion termino bien. */
    void completed(V result, A attachment);

    /** La operacion fallo. */
    void failed(Throwable exc, A attachment);
}
