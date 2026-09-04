package com.sun.nio.sctp;

/**
 * La asociacion cambio de estado.
 *
 * <p>Es la notificacion que cuenta el ciclo de vida entero: nacio, se murio, se reinicio, se
 * esta cerrando, o no se pudo establecer. {@link AssocChangeEvent} es cual de las cinco.
 */
public abstract class AssociationChangeNotification implements Notification {

    /**
     * Que le paso a la asociacion.
     *
     * <p>La distincion que mas importa es {@link #COMM_LOST} contra {@link #SHUTDOWN}: la primera
     * es que se perdio, la segunda que se cerro como corresponde. Confundirlas hace que un cierre
     * normal se reporte como una falla de red.
     */
    public enum AssocChangeEvent {

        /** Quedo establecida y se puede usar. */
        COMM_UP,
        /** Se perdio: el par dejo de responder. */
        COMM_LOST,
        /** El par la reinicio. Lo que estaba en vuelo se perdio. */
        RESTART,
        /** Se cerro ordenadamente. */
        SHUTDOWN,
        /** No se pudo establecer. */
        CANT_START
    }

    /** Para las implementaciones de SCTP. */
    protected AssociationChangeNotification() {
    }

    /** La asociacion; puede ser {@code null} con {@link AssocChangeEvent#CANT_START}. */
    public abstract Association association();

    /** Cual de los cinco eventos fue. */
    public abstract AssocChangeEvent event();
}
