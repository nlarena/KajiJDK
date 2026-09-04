package com.sun.nio.file;

import java.nio.file.WatchEvent;

/**
 * Cada cuanto sondear, cuando el {@link java.nio.file.WatchService} no tiene notificaciones del
 * sistema y tiene que preguntar.
 *
 * <h2>Por que existe algo asi</h2>
 *
 * <p>La implementacion buena de un servicio de vigilancia usa la notificacion del sistema operativo
 * y se entera al instante. Cuando eso no esta —un sistema de archivos de red, una plataforma sin
 * soporte— el JDK cae a **sondear**, y ahi aparece un compromiso que nadie puede resolver por el
 * usuario: sondear seguido detecta rapido y cuesta E/S; sondear espaciado es barato y llega tarde.
 *
 * <p>Estas tres constantes son ese compromiso, dicho por quien registra. Sobre una implementacion
 * que no sondea, no hacen nada — y eso esta bien: es una pista, no un requisito.
 *
 * @deprecated el JDK dejo de mirarlo: las implementaciones que sondeaban se reemplazaron, asi que
 *     hoy el modificador se acepta y se ignora.
 */
@Deprecated(since = "23", forRemoval = true)
public enum SensitivityWatchEventModifier implements WatchEvent.Modifier {

    /** Sondear seguido: cada 2 segundos. */
    HIGH(2),
    /** El punto medio: cada 10 segundos. */
    MEDIUM(10),
    /** Sondear poco: cada 30 segundos. */
    LOW(30);

    private final int sensitivity;

    SensitivityWatchEventModifier(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    /** Cada cuantos segundos sondear. */
    public int sensitivityValueInSeconds() {
        return this.sensitivity;
    }
}
