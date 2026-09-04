package javax.net.ssl;

import java.util.Enumeration;

/**
 * El conjunto de sesiones que se pueden reanudar, con su politica de vencimiento.
 *
 * <p>Reanudar ahorra un handshake completo, pero guardar sesiones para siempre tiene dos costos:
 * memoria, y —el que importa— <strong>seguridad</strong>. Una sesion vieja sigue teniendo un secreto
 * maestro utilizable, asi que cuanto mas vive, mas vale robarla. Los dos limites de esta interfaz
 * son ese compromiso: {@link #setSessionTimeout} por tiempo y {@link #setSessionCacheSize} por
 * cantidad.
 *
 * <p>Hay uno del lado cliente y otro del lado servidor, y {@link SSLContext} los da por separado:
 * los dos roles guardan cosas distintas y tienen razones distintas para olvidarlas.
 */
public interface SSLSessionContext {

    /** La sesion con ese identificador, o {@code null} si no esta o vencio. */
    SSLSession getSession(byte[] sessionId);

    /** Los identificadores de las sesiones vigentes. */
    Enumeration<byte[]> getIds();

    /**
     * Cuantos segundos vive una sesion sin usarse; {@code 0} es sin limite.
     *
     * @throws IllegalArgumentException si es negativo
     */
    void setSessionTimeout(int seconds);

    /** El limite de tiempo actual. */
    int getSessionTimeout();

    /**
     * Cuantas sesiones guardar; {@code 0} es sin limite.
     *
     * @throws IllegalArgumentException si es negativo
     */
    void setSessionCacheSize(int size);

    /** El limite de cantidad actual. */
    int getSessionCacheSize();
}
