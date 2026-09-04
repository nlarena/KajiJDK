package com.sun.management;

/**
 * Lo que se puede preguntar del sistema operativo <strong>solo</strong> en Unix.
 *
 * <p>Son los descriptores de archivo, que en Unix son un recurso contado y agotable: cada socket,
 * cada archivo abierto y cada tuberia gasta uno, y al llegar al tope el proceso deja de poder
 * abrir nada. Es una de las causas mas comunes de que un servidor deje de aceptar conexiones sin
 * que la memoria ni la CPU muestren nada raro.
 *
 * <p>Esta separado en su propia interfaz, y no agregado a {@link OperatingSystemMXBean}, porque en
 * Windows la pregunta no tiene respuesta. Quien quiera el dato tiene que preguntar primero si el
 * bean es de este tipo, y eso es exactamente lo que se pretende.
 *
 * @since 1.5
 */
public interface UnixOperatingSystemMXBean extends OperatingSystemMXBean {

    /**
     * Cuantos descriptores tiene abiertos el proceso ahora.
     *
     * @return la cantidad
     */
    long getOpenFileDescriptorCount();

    /**
     * Cuantos puede tener abiertos como maximo.
     *
     * @return el tope
     */
    long getMaxFileDescriptorCount();
}
