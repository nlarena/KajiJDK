package javax.management.timer;

import java.util.Date;
import java.util.Vector;
import javax.management.InstanceNotFoundException;

/**
 * KajiLibrary's javax.management.timer.TimerMBean -- la interfaz de administracion del reloj.
 *
 * <p>Es un MBean estandar, asi que esta interfaz <b>es</b> el API remoto: cada metodo de aca se
 * puede llamar desde una consola JMX sin saber nada de la clase que lo implementa.
 *
 * <h2>Por que devuelve envoltorios y {@code Vector}</h2>
 *
 * <p>Los consultores devuelven {@code Integer}, {@code Long}, {@code Boolean} y no primitivos porque
 * tienen que poder contestar <b>null</b>: preguntar por un identificador que no existe no es un
 * error, y con un {@code long} habria que inventar un valor centinela. {@code getNbNotifications}
 * si es primitivo, porque "cuantas hay" siempre tiene respuesta.
 *
 * <p>Los {@code Vector} son de 1998 y hoy nadie los elegiria, pero el tipo de retorno de un MBean es
 * parte del protocolo: cambiarlo por {@code List} rompe a todo cliente compilado contra el original.
 * Es la misma razon por la que sobrevive el error de tipeo de {@link #getNbOccurences}.
 */
public interface TimerMBean {

    /** Arranca el reloj. Si ya estaba activo no hace nada. */
    void start();

    /** Lo para. Las inscripciones <b>no</b> se pierden: vuelven a valer en el proximo arranque. */
    void stop();

    /**
     * Inscribe una notificacion.
     *
     * @param period milisegundos entre repeticiones; 0 significa una sola vez
     * @param nbOccurences cuantas veces; 0 significa para siempre
     * @param fixedRate true para contar desde la fecha original, false desde cada envio
     * @return el identificador con el que se la consulta o se la da de baja
     * @throws IllegalArgumentException si la fecha es null, o el periodo o las ocurrencias son
     *     negativos
     */
    Integer addNotification(String type, String message, Object userData, Date date, long period,
                            long nbOccurences, boolean fixedRate) throws IllegalArgumentException;

    /** Igual, con reloj de retardo fijo. */
    Integer addNotification(String type, String message, Object userData, Date date, long period,
                            long nbOccurences) throws IllegalArgumentException;

    /** Igual, repitiendo para siempre. */
    Integer addNotification(String type, String message, Object userData, Date date, long period)
        throws IllegalArgumentException;

    /** Igual, una sola vez. */
    Integer addNotification(String type, String message, Object userData, Date date)
        throws IllegalArgumentException;

    /**
     * Da de baja una inscripcion.
     *
     * @throws InstanceNotFoundException si ese identificador no existe
     */
    void removeNotification(Integer id) throws InstanceNotFoundException;

    /**
     * Da de baja todas las de ese tipo.
     *
     * @throws InstanceNotFoundException si no hay ninguna de ese tipo
     */
    void removeNotifications(String type) throws InstanceNotFoundException;

    /** Las da de baja a todas. */
    void removeAllNotifications();

    /** Cuantas inscripciones hay. */
    int getNbNotifications();

    /** Los identificadores de todas. */
    Vector<Integer> getAllNotificationIDs();

    /** Los de ese tipo; vacio si no hay ninguna, que no es un error. */
    Vector<Integer> getNotificationIDs(String type);

    /** El tipo de esa inscripcion, o null si no existe. */
    String getNotificationType(Integer id);

    /** Su mensaje, o null. */
    String getNotificationMessage(Integer id);

    /** Su dato adjunto, o null. */
    Object getNotificationUserData(Integer id);

    /** Su proxima fecha de disparo, o null. */
    Date getDate(Integer id);

    /** Su periodo en milisegundos, o null. */
    Long getPeriod(Integer id);

    /**
     * Cuantos disparos le quedan, o null.
     *
     * <p>El nombre esta mal escrito --seria "occurrences"-- desde 1.5 y quedo asi: corregirlo
     * cambiaria el API de administracion y romperia a los clientes.
     */
    Long getNbOccurences(Integer id);

    /** Si cuenta desde la fecha original, o null. */
    Boolean getFixedRate(Integer id);

    /** Si al arrancar se mandan las que quedaron atrasadas. */
    boolean getSendPastNotifications();

    /** Ver {@link #getSendPastNotifications}. */
    void setSendPastNotifications(boolean value);

    /** Si esta corriendo. */
    boolean isActive();

    /** Si no hay ninguna inscripcion. */
    boolean isEmpty();
}
