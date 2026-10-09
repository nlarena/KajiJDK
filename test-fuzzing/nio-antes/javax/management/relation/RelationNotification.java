package javax.management.relation;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * The notice that a relation was created, changed or removed.
 *
 * <h2>The six types, and why six and not three</h2>
 *
 * <p>The three things that can happen --creation, update, removal-- come in two flavours each:
 * <b>BASIC</b> and <b>MBEAN</b>. The difference is what the relation was made of.
 *
 * <p>A "basic" relation is managed internally by the service; an "MBean" one is an object
 * registered in the server, with its own {@link ObjectName}. Whoever listens usually wants to treat
 * both the same, but whoever cleans up resources does not: only the second leaves an MBean that may
 * have to be unregistered, and {@link #getMBeansToUnregister} exists precisely for that.
 *
 * <h2>The three update fields</h2>
 *
 * <p>{@link #getRoleName}, {@link #getOldRoleValue} and {@link #getNewRoleValue} only have a value
 * in update notifications. That the <em>old</em> value comes along with the new one is what allows
 * reacting to the change --knowing which MBean stopped being referenced-- without having kept the
 * previous state just in case.
 */
public class RelationNotification extends Notification {

    private static final long serialVersionUID = -6871117877523310399L;

    /** An internally managed relation was created. */
    public static final String RELATION_BASIC_CREATION = "jmx.relation.creation.basic";

    /** A relation that is an MBean was added. */
    public static final String RELATION_MBEAN_CREATION = "jmx.relation.creation.mbean";

    /** A role of an internal relation changed. */
    public static final String RELATION_BASIC_UPDATE = "jmx.relation.update.basic";

    /** A role of a relation that is an MBean changed. */
    public static final String RELATION_MBEAN_UPDATE = "jmx.relation.update.mbean";

    /** An internal relation was removed. */
    public static final String RELATION_BASIC_REMOVAL = "jmx.relation.removal.basic";

    /** A relation that is an MBean was removed. */
    public static final String RELATION_MBEAN_REMOVAL = "jmx.relation.removal.mbean";

    private String relationId;
    private String relationTypeName;
    private ObjectName relationObjName;
    private List<ObjectName> unregisterMBeanList;
    private String roleName;
    private List<ObjectName> oldRoleValue;
    private List<ObjectName> newRoleValue;

    /**
     * For creation and removal.
     *
     * @throws IllegalArgumentException if the notification type is neither creation nor removal, or
     *     if something mandatory is missing
     */
    public RelationNotification(String notifType, Object sourceObj, long sequence,
            long timeStamp, String message, String id, String typeName, ObjectName objectName,
            List<ObjectName> unregMBeanList) throws IllegalArgumentException {
        super(notifType, sourceObj, sequence, timeStamp, message);
        if (notifType == null || !isCreationOrRemoval(notifType)) {
            throw new IllegalArgumentException(
                    "the type is neither creation nor removal: " + String.valueOf(notifType));
        }
        checkCommon(sourceObj, id, typeName);
        this.relationId = id;
        this.relationTypeName = typeName;
        this.relationObjName = objectName;
        this.unregisterMBeanList = copy(unregMBeanList);
    }

    /**
     * For an update.
     *
     * @throws IllegalArgumentException if the type is not an update, or if the role is missing
     */
    public RelationNotification(String notifType, Object sourceObj, long sequence,
            long timeStamp, String message, String id, String typeName, ObjectName objectName,
            String name, List<ObjectName> newValue, List<ObjectName> oldValue)
            throws IllegalArgumentException {
        super(notifType, sourceObj, sequence, timeStamp, message);
        if (notifType == null || !isUpdate(notifType)) {
            throw new IllegalArgumentException(
                    "the type is not an update: " + String.valueOf(notifType));
        }
        checkCommon(sourceObj, id, typeName);
        if (name == null || newValue == null || oldValue == null) {
            throw new IllegalArgumentException(
                    "an update needs the role and its two values");
        }
        this.relationId = id;
        this.relationTypeName = typeName;
        this.relationObjName = objectName;
        this.roleName = name;
        this.newRoleValue = copy(newValue);
        this.oldRoleValue = copy(oldValue);
    }

    private static void checkCommon(Object sourceObj, String id, String typeName) {
        if (id == null || typeName == null) {
            throw new IllegalArgumentException("the relation's identifier or type is missing");
        }
        if (sourceObj == null) {
            throw new IllegalArgumentException("the notification source is missing");
        }
    }

    private static boolean isCreationOrRemoval(String t) {
        return t.equals(RELATION_BASIC_CREATION) || t.equals(RELATION_MBEAN_CREATION)
                || t.equals(RELATION_BASIC_REMOVAL) || t.equals(RELATION_MBEAN_REMOVAL);
    }

    private static boolean isUpdate(String t) {
        return t.equals(RELATION_BASIC_UPDATE) || t.equals(RELATION_MBEAN_UPDATE);
    }

    private static List<ObjectName> copy(List<ObjectName> l) {
        return l == null ? null : new ArrayList<ObjectName>(l);
    }

    /** The relation's identifier. */
    public String getRelationId() {
        return this.relationId;
    }

    /** The name of its type. */
    public String getRelationTypeName() {
        return this.relationTypeName;
    }

    /** The name of the relation's MBean, or {@code null} if it is internal. */
    public ObjectName getObjectName() {
        return this.relationObjName;
    }

    /**
     * The MBeans left with no reference that can be unregistered.
     *
     * <p>Only in removal notifications. It is a <b>suggestion</b>, not an order: the relation
     * service unregisters nothing on its own, because it does not know whether those MBeans matter
     * to someone else.
     */
    public List<ObjectName> getMBeansToUnregister() {
        return this.unregisterMBeanList == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.unregisterMBeanList);
    }

    /** The role that changed, or {@code null} if this is not an update. */
    public String getRoleName() {
        return this.roleName;
    }

    /** What the role had before; see the class note. */
    public List<ObjectName> getOldRoleValue() {
        return this.oldRoleValue == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.oldRoleValue);
    }

    /** What it has now. */
    public List<ObjectName> getNewRoleValue() {
        return this.newRoleValue == null
                ? new ArrayList<ObjectName>()
                : new ArrayList<ObjectName>(this.newRoleValue);
    }
}
