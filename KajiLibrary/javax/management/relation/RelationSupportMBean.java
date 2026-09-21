package javax.management.relation;

/**
 * The management interface of {@link RelationSupport} when it is registered as an MBean.
 *
 * <h2>Why it exists and what it adds</h2>
 *
 * <p>A relation can live in two ways: managed internally by the service, or registered in the MBean
 * server as one more object. The second is the one that allows seeing and manipulating it from a
 * management console.
 *
 * <p>What it adds over {@link Relation} are the two methods that reflect <em>which of the two
 * forms</em> it is living in. They are for the service's internal use; user code reads them but
 * does not write them.
 */
public interface RelationSupportMBean extends Relation {

    /**
     * Whether the relation service is managing it.
     *
     * <p>{@code false} means the object exists but has not been added to the service yet, and then
     * almost no operation works: without the service there is nobody to ask about the type nor
     * anything to verify the referenced MBeans with.
     */
    Boolean isInRelationService();

    /** The service calls it when taking it and when releasing it. */
    void setRelationServiceManagementFlag(Boolean flag) throws IllegalArgumentException;
}
