package java.beans.beancontext;

import java.util.EventListener;

/**
 * Escucha la revocación de un servicio.
 *
 * <p>Quien pide un servicio con `getService` pasa uno de éstos, y por ahí se entera de que el
 * servicio deja de estar disponible. Es lo que evita que un usuario se quede con una referencia a
 * algo que ya no vale: sin este aviso, la única forma de enterarse sería que fallara al usarlo.
 */
public interface BeanContextServiceRevokedListener extends EventListener {

    /** El servicio que el evento nombra fue revocado. */
    void serviceRevoked(BeanContextServiceRevokedEvent bcsre);
}
