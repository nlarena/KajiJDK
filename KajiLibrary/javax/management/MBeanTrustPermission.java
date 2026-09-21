package javax.management;

import java.security.BasicPermission;

/**
 * The permission of <b>whoever signed the code</b>, not of whoever calls it.
 *
 * <p>It is the odd piece of the model: the other JMX permissions are checked against the call
 * stack, this one against the origin of the <b>MBean being registered</b>. The question it answers
 * is not "can this thread register" but "do we trust this class's code enough to let it into the
 * agent". Hence the only useful name, {@code register}.
 *
 * <p>The {@code actions} argument of the second form has to be {@code null} or empty. It only
 * exists because the policy loader builds every permission with two arguments and cannot know which
 * ones use them.
 */
public class MBeanTrustPermission extends BasicPermission {

    private static final long serialVersionUID = -2952178077029017036L;

    /** @throws IllegalArgumentException if the name is neither {@code register} nor {@code *} */
    public MBeanTrustPermission(String name) {
        this(name, null);
    }

    /**
     * @param actions has to be {@code null} or the empty string
     * @throws IllegalArgumentException if the name is not valid or if actions come
     */
    public MBeanTrustPermission(String name, String actions) {
        super(name, actions);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("MBeanTrustPermission takes no actions: " + actions);
        }
        if (!name.equals("register") && !name.equals("*")) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
    }
}
