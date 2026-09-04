package javax.management.relation;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * El aviso de que una relacion se creo, cambio o se borro.
 *
 * <h2>Los seis tipos, y por que son seis y no tres</h2>
 *
 * <p>Las tres cosas que pueden pasar —creacion, actualizacion, borrado— vienen cada una en dos
 * sabores: <strong>BASIC</strong> y <strong>MBEAN</strong>. La diferencia es de que estaba hecha la
 * relacion.
 *
 * <p>Una relacion "basica" la administra el servicio internamente; una "MBean" es un objeto
 * registrado en el servidor, con su propio {@link ObjectName}. Quien escucha suele querer tratar las
 * dos igual, pero quien limpia recursos no: solo la segunda deja un MBean que quizas haya que
 * desregistrar, y {@link #getMBeansToUnregister} existe justamente para eso.
 *
 * <h2>Los tres campos de la actualizacion</h2>
 *
 * <p>{@link #getRoleName}, {@link #getOldRoleValue} y {@link #getNewRoleValue} solo tienen valor en
 * las notificaciones de actualizacion. Que venga el valor <em>viejo</em> ademas del nuevo es lo que
 * permite reaccionar al cambio —saber que MBean dejo de estar referenciado— sin haber guardado el
 * estado anterior por las dudas.
 */
public class RelationNotification extends Notification {

    private static final long serialVersionUID = -6871117877523310399L;

    /** Se creo una relacion administrada internamente. */
    public static final String RELATION_BASIC_CREATION = "jmx.relation.creation.basic";

    /** Se agrego una relacion que es un MBean. */
    public static final String RELATION_MBEAN_CREATION = "jmx.relation.creation.mbean";

    /** Cambio un rol de una relacion interna. */
    public static final String RELATION_BASIC_UPDATE = "jmx.relation.update.basic";

    /** Cambio un rol de una relacion que es un MBean. */
    public static final String RELATION_MBEAN_UPDATE = "jmx.relation.update.mbean";

    /** Se borro una relacion interna. */
    public static final String RELATION_BASIC_REMOVAL = "jmx.relation.removal.basic";

    /** Se saco una relacion que es un MBean. */
    public static final String RELATION_MBEAN_REMOVAL = "jmx.relation.removal.mbean";

    private String relationId;
    private String relationTypeName;
    private ObjectName relationObjName;
    private List<ObjectName> unregisterMBeanList;
    private String roleName;
    private List<ObjectName> oldRoleValue;
    private List<ObjectName> newRoleValue;

    /**
     * Para creacion y borrado.
     *
     * @throws IllegalArgumentException si el tipo de notificacion no es de creacion ni de borrado,
     *     o si falta algo obligatorio
     */
    public RelationNotification(String notifType, Object sourceObj, long sequence,
            long timeStamp, String message, String id, String typeName, ObjectName objectName,
            List<ObjectName> unregMBeanList) throws IllegalArgumentException {
        super(notifType, sourceObj, sequence, timeStamp, message);
        if (notifType == null || !esCreacionOBorrado(notifType)) {
            throw new IllegalArgumentException(
                    "el tipo no es de creacion ni de borrado: " + String.valueOf(notifType));
        }
        revisarComun(sourceObj, id, typeName);
        this.relationId = id;
        this.relationTypeName = typeName;
        this.relationObjName = objectName;
        this.unregisterMBeanList = copiar(unregMBeanList);
    }

    /**
     * Para actualizacion.
     *
     * @throws IllegalArgumentException si el tipo no es de actualizacion, o si falta el rol
     */
    public RelationNotification(String notifType, Object sourceObj, long sequence,
            long timeStamp, String message, String id, String typeName, ObjectName objectName,
            String name, List<ObjectName> newValue, List<ObjectName> oldValue)
            throws IllegalArgumentException {
        super(notifType, sourceObj, sequence, timeStamp, message);
        if (notifType == null || !esActualizacion(notifType)) {
            throw new IllegalArgumentException(
                    "el tipo no es de actualizacion: " + String.valueOf(notifType));
        }
        revisarComun(sourceObj, id, typeName);
        if (name == null || newValue == null || oldValue == null) {
            throw new IllegalArgumentException(
                    "una actualizacion necesita el rol y sus dos valores");
        }
        this.relationId = id;
        this.relationTypeName = typeName;
        this.relationObjName = objectName;
        this.roleName = name;
        this.newRoleValue = copiar(newValue);
        this.oldRoleValue = copiar(oldValue);
    }

    private static void revisarComun(Object sourceObj, String id, String typeName) {
        if (id == null || typeName == null) {
            throw new IllegalArgumentException("faltan el identificador o el tipo de la relacion");
        }
        if (sourceObj == null) {
            throw new IllegalArgumentException("falta la fuente de la notificacion");
        }
    }

    private static boolean esCreacionOBorrado(String t) {
        return t.equals(RELATION_BASIC_CREATION) || t.equals(RELATION_MBEAN_CREATION)
                || t.equals(RELATION_BASIC_REMOVAL) || t.equals(RELATION_MBEAN_REMOVAL);
    }

    private static boolean esActualizacion(String t) {
        return t.equals(RELATION_BASIC_UPDATE) || t.equals(RELATION_MBEAN_UPDATE);
    }

    private static List<ObjectName> copiar(List<ObjectName> l) {
        return l == null ? null : new ArrayList<ObjectName>(l);
    }

    /** El identificador de la relacion. */
    public String getRelationId() {
        return this.relationId;
    }

    /** El nombre de su tipo. */
    public String getRelationTypeName() {
        return this.relationTypeName;
    }

    /** El nombre del MBean de la relacion, o {@code null} si es interna. */
    public ObjectName getObjectName() {
        return this.relationObjName;
    }

    /**
     * Los MBeans que quedaron sin referencia y se pueden desregistrar.
     *
     * <p>Solo en las notificaciones de borrado. Es una <strong>sugerencia</strong>, no una orden: el
     * servicio de relaciones no desregistra nada por su cuenta, porque no sabe si esos MBeans le
     * importan a alguien mas.
     */
    public List<ObjectName> getMBeansToUnregister() {
        return this.unregisterMBeanList == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.unregisterMBeanList);
    }

    /** El rol que cambio, o {@code null} si no es una actualizacion. */
    public String getRoleName() {
        return this.roleName;
    }

    /** Lo que el rol tenia antes; ver la nota de la clase. */
    public List<ObjectName> getOldRoleValue() {
        return this.oldRoleValue == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.oldRoleValue);
    }

    /** Lo que tiene ahora. */
    public List<ObjectName> getNewRoleValue() {
        return this.newRoleValue == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.newRoleValue);
    }
}
