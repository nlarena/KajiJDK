package com.sun.nio.sctp;

/**
 * Una asociacion SCTP: la relacion entre dos puntas, con sus flujos.
 *
 * <h2>Que aporta SCTP sobre TCP, y por que hace falta este objeto</h2>
 *
 * <p>Una conexion TCP es un unico flujo de bytes, y eso trae el bloqueo de cabecera: un segmento
 * perdido frena todo lo que venia atras, aunque fuera independiente. Una asociacion SCTP lleva
 * <strong>varios flujos</strong> en paralelo, cada uno con su orden propio, asi que una perdida en
 * uno no detiene a los otros.
 *
 * <p>Esa es la razon de que exista este objeto y de que no alcance con un descriptor: una asociacion
 * tiene identidad ({@link #associationID}) y una capacidad negociada de cuantos flujos admite en
 * cada sentido, y esos numeros hacen falta para saber que {@code streamNumber} es valido en un
 * {@link MessageInfo}.
 *
 * <p>Los maximos son <strong>asimetricos</strong> a proposito: cada punta declara cuantos flujos
 * acepta recibir, y las dos declaraciones no tienen por que coincidir.
 */
public class Association {

    private final int associationID;
    private final int maxInStreams;
    private final int maxOutStreams;

    /**
     * Para las implementaciones de SCTP.
     *
     * <p>{@code protected} porque una asociacion la crea la pila del protocolo cuando se negocia,
     * no el codigo de usuario: fabricar una a mano daria un objeto que no describe ninguna
     * conexion real.
     */
    protected Association(int associationID, int maxInStreams, int maxOutStreams) {
        this.associationID = associationID;
        this.maxInStreams = maxInStreams;
        this.maxOutStreams = maxOutStreams;
    }

    /** El identificador que le dio la pila local. Unico mientras la asociacion viva. */
    public final int associationID() {
        return this.associationID;
    }

    /** Cuantos flujos entrantes admite. */
    public final int maxInboundStreams() {
        return this.maxInStreams;
    }

    /** Cuantos flujos salientes admite. */
    public final int maxOutboundStreams() {
        return this.maxOutStreams;
    }
}
