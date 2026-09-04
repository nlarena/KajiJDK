package java.awt;

/**
 * Un evento que sabe atenderse solo.
 *
 * <p>Lo normal es que la cola de eventos decida a quién entregarle cada evento. Uno que implemente
 * esto se despacha a sí mismo: la cola le llama {@link #dispatch} y listo.
 *
 * <p>Es lo que hace posible {@link java.awt.event.InvocationEvent}, y con él poner trabajo a correr
 * en el hilo de eventos desde otro hilo — que es la única forma legítima de tocar la interfaz desde
 * afuera.
 */
public interface ActiveEvent {

    /** Atiende el evento. */
    void dispatch();
}
