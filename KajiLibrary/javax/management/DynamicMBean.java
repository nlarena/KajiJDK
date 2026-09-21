package javax.management;

/**
 * The MBean that describes itself at run time.
 *
 * <p>It is the counterpoint of the standard MBean. A standard one declares its interface in Java
 * methods and the agent discovers it by reflection over the names; a dynamic one has no interface
 * to look at -- it returns its {@link MBeanInfo} from {@link #getMBeanInfo()} and serves requests
 * by name.
 *
 * <p>The practical consequence: a dynamic MBean can change its attributes between two calls. It is
 * how things whose shape is not known at compile time --a configuration table, a model loaded from
 * a file-- are instrumented without generating classes.
 */
public interface DynamicMBean {

    /** Reads an attribute by name. */
    Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException;

    /** Writes an attribute. */
    void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
                   MBeanException, ReflectionException;

    /**
     * Reads several.
     *
     * <p>It declares no exceptions: the attributes that fail are left out of the answer, which is
     * why the returned list may be shorter than the request.
     */
    AttributeList getAttributes(String[] attributes);

    /** Writes several; returns the ones actually written. */
    AttributeList setAttributes(AttributeList attributes);

    /**
     * Invokes an operation.
     *
     * @param signature the class names of the parameters, to disambiguate overloads
     */
    Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException;

    /** Which attributes, operations, constructors and notifications it has <b>now</b>. */
    MBeanInfo getMBeanInfo();
}
