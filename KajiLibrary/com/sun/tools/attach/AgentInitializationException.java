package com.sun.tools.attach;

/**
 * El agente se cargo pero su {@code agentmain} fallo.
 *
 * <h2>Por que es distinta de {@link AgentLoadException}</h2>
 *
 * <p>Porque el agente <strong>ya esta adentro</strong> de la VM destino. Con un
 * {@code AgentLoadException} no paso nada; con esta, el codigo del agente corrio y tiro algo, o
 * devolvio un codigo distinto de cero. La VM destino quedo con lo que ese agente haya alcanzado a
 * hacer antes de fallar, que no es lo mismo que quedar intacta.
 *
 * <p>{@link #returnValue} es lo que devolvio un agente nativo. Para uno escrito en Java es
 * siempre {@code 0}: alli la falla llega como excepcion y no como codigo.
 */
public class AgentInitializationException extends Exception {

    private static final long serialVersionUID = -1508756333332806353L;

    private final int returnValue;

    /** Sin detalle. */
    public AgentInitializationException() {
        super();
        this.returnValue = 0;
    }

    /** Con un mensaje. */
    public AgentInitializationException(String s) {
        super(s);
        this.returnValue = 0;
    }

    /** Con un mensaje y el codigo que devolvio un agente nativo. */
    public AgentInitializationException(String s, int returnValue) {
        super(s);
        this.returnValue = returnValue;
    }

    /** El codigo que devolvio el {@code Agent_OnAttach} nativo; {@code 0} para un agente Java. */
    public int returnValue() {
        return this.returnValue;
    }
}
