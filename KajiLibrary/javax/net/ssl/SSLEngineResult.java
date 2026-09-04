package javax.net.ssl;

/**
 * Lo que devolvio un {@code wrap} o un {@code unwrap} de {@link SSLEngine}.
 *
 * <h2>Por que hacen falta dos estados y no uno</h2>
 *
 * <p>Porque un {@link SSLEngine} contesta dos preguntas a la vez, y son independientes.
 * {@link Status} dice <strong>que paso con esta llamada</strong> —si consumio, si le falto lugar, si
 * el motor esta cerrado—. {@link HandshakeStatus} dice <strong>que hay que hacer despues</strong>,
 * que es lo que gobierna el bucle de quien lo usa.
 *
 * <p>Mezclarlos seria el error clasico: una llamada puede terminar {@code OK} y aun asi necesitar
 * otro {@code wrap} antes de que pase nada util, porque el handshake sigue en curso. Son dos ejes.
 */
public class SSLEngineResult {

    /**
     * Como termino la llamada.
     *
     * <p>Los dos primeros no son errores sino <strong>pedidos</strong>: el motor no puede seguir con
     * los buffers que le dieron y hay que agrandarlos o vaciarlos y reintentar. Tratarlos como
     * fallas es la manera mas comun de escribir mal un bucle de {@code SSLEngine}.
     */
    public enum Status {

        /** Falta entrada: llego un registro incompleto. Hay que leer mas de la red y reintentar. */
        BUFFER_UNDERFLOW,
        /** Falta lugar en la salida. Hay que vaciar el buffer destino y reintentar. */
        BUFFER_OVERFLOW,
        /** Anduvo. */
        OK,
        /** El motor esta cerrado en ese sentido. */
        CLOSED
    }

    /**
     * Que hace falta hacer a continuacion.
     *
     * <p>Es el estado que maneja el bucle. {@link #NEED_TASK} es el mas facil de pasar por alto:
     * significa que el motor tiene trabajo pesado pendiente —criptografia asimetrica— que
     * deliberadamente <em>no</em> hace en el hilo que llamo, para no bloquearlo. Hay que sacarlo con
     * {@link SSLEngine#getDelegatedTask} y correrlo, si no el handshake no avanza nunca.
     */
    public enum HandshakeStatus {

        /** No hay handshake en curso. */
        NOT_HANDSHAKING,
        /** El handshake acaba de terminar. Se reporta una sola vez. */
        FINISHED,
        /** Hay tareas pendientes; sacarlas con {@link SSLEngine#getDelegatedTask} y correrlas. */
        NEED_TASK,
        /** El motor necesita producir datos: llamar a {@code wrap}. */
        NEED_WRAP,
        /** El motor necesita consumir datos: llamar a {@code unwrap}. */
        NEED_UNWRAP,
        /**
         * Como {@link #NEED_UNWRAP}, pero sin leer nada nuevo de la red.
         *
         * <p>Existe por DTLS, que corre sobre datagramas: el motor puede tener adentro un mensaje
         * que llego desordenado y que ahora si puede procesar. Leer de la red aca bloquearia
         * esperando algo que ya se tiene.
         */
        NEED_UNWRAP_AGAIN
    }

    private final Status status;
    private final HandshakeStatus handshakeStatus;
    private final int bytesConsumed;
    private final int bytesProduced;
    private final long sequenceNumber;

    /**
     * @throws IllegalArgumentException si algun estado es {@code null} o si algun conteo es negativo
     */
    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus, int bytesConsumed,
            int bytesProduced) {
        this(status, handshakeStatus, bytesConsumed, bytesProduced, -1L);
    }

    /**
     * Igual, con el numero de secuencia del registro — solo tiene sentido en DTLS.
     *
     * @throws IllegalArgumentException si algun estado es {@code null} o si algun conteo es negativo
     */
    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus, int bytesConsumed,
            int bytesProduced, long sequenceNumber) {
        if (status == null) {
            throw new IllegalArgumentException("falta el estado");
        }
        if (handshakeStatus == null) {
            throw new IllegalArgumentException("falta el estado de handshake");
        }
        if (bytesConsumed < 0 || bytesProduced < 0) {
            throw new IllegalArgumentException("los conteos de bytes no pueden ser negativos");
        }
        this.status = status;
        this.handshakeStatus = handshakeStatus;
        this.bytesConsumed = bytesConsumed;
        this.bytesProduced = bytesProduced;
        this.sequenceNumber = sequenceNumber;
    }

    /** Como termino la llamada. */
    public final Status getStatus() {
        return this.status;
    }

    /** Que hace falta hacer despues. */
    public final HandshakeStatus getHandshakeStatus() {
        return this.handshakeStatus;
    }

    /** Cuantos bytes se leyeron de la entrada. */
    public final int bytesConsumed() {
        return this.bytesConsumed;
    }

    /** Cuantos bytes se escribieron en la salida. */
    public final int bytesProduced() {
        return this.bytesProduced;
    }

    /**
     * El numero de secuencia del registro, sin signo.
     *
     * <p>{@code -1} cuando no aplica: en TLS sobre TCP el transporte ya garantiza el orden y no hay
     * nada que numerar. Es un {@code long} leido como <strong>sin signo</strong>, asi que compararlo
     * con {@code <} da mal para valores altos — hay que usar {@link Long#compareUnsigned}.
     */
    public final long sequenceNumber() {
        return this.sequenceNumber;
    }

    public String toString() {
        return "Status = " + this.status.toString()
                + " HandshakeStatus = " + this.handshakeStatus.toString()
                + "\nbytesConsumed = " + String.valueOf(this.bytesConsumed)
                + " bytesProduced = " + String.valueOf(this.bytesProduced)
                + (this.sequenceNumber == -1L ? ""
                        : " sequenceNumber = " + Long.toUnsignedString(this.sequenceNumber));
    }
}
