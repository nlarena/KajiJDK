package com.sun.tools.attach;

/**
 * El agente no se pudo cargar en la VM destino.
 *
 * <p>Las causas tipicas son tres, y todas del lado del agente y no del canal. Que el JAR no exista;
 * que exista pero no declare {@code Agent-Class} en su manifiesto; o que la clase que declara no
 * tenga el {@code agentmain} que se le pide.
 *
 * <p>Cuando esto pasa la VM destino queda <strong>intacta</strong>: el agente no llego a correr.
 * Si llego a correr y fallo, la excepcion es {@link AgentInitializationException}, y ahi el destino
 * si quedo con lo que el agente alcanzo a hacer.
 */
public class AgentLoadException extends Exception {

    private static final long serialVersionUID = -688265984016827025L;

    /** Sin detalle. */
    public AgentLoadException() {
        super();
    }

    /** Con un mensaje que explique el caso. */
    public AgentLoadException(String s) {
        super(s);
    }
}
