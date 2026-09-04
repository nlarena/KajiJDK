package javax.management;

import java.util.EventObject;

/**
 * El aviso que un MBean emite cuando pasa algo.
 *
 * <p>Hereda de `EventObject` y por eso lleva `source`, pero con una vuelta que hay que conocer: el
 * emisor pone ahi <b>el objeto</b> MBean, y el servidor de MBeans lo <b>reemplaza por el
 * {@link ObjectName}</b> antes de reenviarlo. Un oyente registrado por el agente ve un `ObjectName`
 * en `getSource()`; uno registrado directo contra el MBean ve el objeto. De ahi que `setSource` sea
 * publico: no es un descuido, es el mecanismo.
 *
 * <p>El otro campo que importa es el numero de secuencia. Es del <b>emisor</b>, no global, y sirve
 * para que el que recibe detecte huecos: si le llega el 5 y el 7, sabe que perdio el 6. Vale cero
 * si el emisor no lleva la cuenta.
 */
public class Notification extends EventObject {

    private static final long serialVersionUID = -7516092053498031989L;

    /**
     * @serial el tipo, con la convencion de puntos
     */
    private String type;

    /**
     * @serial numero de secuencia del emisor
     */
    private long sequenceNumber;

    /**
     * @serial cuando ocurrio, en milisegundos
     */
    private long timeStamp;

    /**
     * @serial datos libres del emisor
     */
    private Object userData = null;

    /**
     * @serial texto para leer
     */
    private String message = "";

    /**
     * @serial la fuente, que el agente puede reemplazar por el ObjectName
     */
    protected Object source = null;

    /** Con la hora tomada del reloj. */
    public Notification(String type, Object source, long sequenceNumber) {
        super(source);
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = System.currentTimeMillis();
    }

    /** Con la hora tomada del reloj y un texto. */
    public Notification(String type, Object source, long sequenceNumber, String message) {
        this(type, source, sequenceNumber);
        this.message = message;
    }

    /** Con hora explicita: para reproducir eventos que ya ocurrieron. */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp) {
        super(source);
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = timeStamp;
    }

    /** Con hora explicita y texto. */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp,
                        String message) {
        this(type, source, sequenceNumber, timeStamp);
        this.message = message;
    }

    /**
     * Cambia la fuente.
     *
     * <p>Publico porque el servidor de MBeans lo usa para poner el {@link ObjectName} en lugar del
     * objeto; ver la nota de la clase.
     */
    public void setSource(Object source) {
        super.source = source;
        this.source = source;
    }

    /** El numero de secuencia del emisor. */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /** Lo fija; lo usa quien reenvia. */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * El tipo, por convencion en puntos y de lo general a lo particular
     * ({@code jmx.attribute.change}), para que un filtro por prefijo tenga sentido.
     */
    public String getType() {
        return type;
    }

    /** Cuando ocurrio, en milisegundos desde la epoca. */
    public long getTimeStamp() {
        return timeStamp;
    }

    /** La fija. */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /** El texto; cadena vacia si no se dio, nunca `null`. */
    public String getMessage() {
        return message;
    }

    /** Los datos libres del emisor, o `null`. */
    public Object getUserData() {
        return userData;
    }

    /** Los fija. Si viajan a un cliente remoto tienen que ser serializables. */
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /** {@code clase[source=...][type=...][message=...]}. */
    public String toString() {
        return super.toString() + "[type=" + type + "][message=" + message + "]";
    }
}
