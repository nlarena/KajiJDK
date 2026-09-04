package java.rmi.server;

/**
 * Decide que hacer cuando el runtime no pudo crear un socket.
 *
 * <p>Existe porque quedarse sin sockets suele ser <strong>transitorio</strong>: un pico de
 * conexiones agota los descriptores y un segundo despues hay lugar. Sin este enganche, RMI tendria
 * que elegir de antemano entre rendirse —perdiendo un servidor por un pico— o reintentar para
 * siempre. Devolver {@code true} es pedir un reintento.
 */
public interface RMIFailureHandler {

    /** @return {@code true} para reintentar, {@code false} para abandonar */
    boolean failure(Exception ex);
}
