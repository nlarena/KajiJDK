package javax.management;

import java.security.Permission;

/**
 * Permission for <b>a specific operation against a specific MBean</b>.
 *
 * <p>The name has three parts, {@code class#member[objectName]}, and any of them may be missing or
 * be {@code *}, which means "any". The three are compared differently, and that is the whole
 * class:
 *
 * <ul>
 *   <li><b>the class</b> admits a suffix wildcard --{@code com.foo.*} covers the whole package--,
 *       because class names are hierarchical by prefix;
 *   <li><b>the member</b> does not: an attribute {@code Count} and another {@code CountTotal} are
 *       unrelated, so there is only equality or {@code *};
 *   <li><b>the object name</b> is compared with {@link ObjectName#apply}, which already knows about
 *       domain and property wildcards. Reimplementing it here would mean two definitions of the
 *       same thing.
 * </ul>
 *
 * <p>The actions go in a mask, and one implies another: {@code queryMBeans} implies
 * {@code queryNames}, because whoever can fetch the instances has already seen the names. As in
 * {@link MBeanServerPermission}, that implication is closed when the mask is built and not in
 * {@code implies}.
 *
 * <p>A detail that breaks intuition and is in the specification: a <b>granted</b> permission with
 * an empty part means "any", but the permission being <b>checked</b> with an empty part means "I do
 * not know which", and then only a granted one that also accepts any covers it. It is the right
 * asymmetry: when in doubt, it is not enough.
 */
public class MBeanPermission extends Permission {

    private static final long serialVersionUID = -2416928705275160661L;

    /** The actions that exist, in the order the canonical string is rebuilt in. */
    private static final String[] ACTIONS = {
        "addNotificationListener", "getAttribute", "getClassLoader", "getClassLoaderFor",
        "getClassLoaderRepository", "getDomains", "getMBeanInfo", "getObjectInstance",
        "instantiate", "invoke", "isInstanceOf", "queryMBeans", "queryNames", "registerMBean",
        "removeNotificationListener", "setAttribute", "unregisterMBean"
    };

    private static final int BIT_QUERY_MBEANS = 1 << 11;
    private static final int BIT_QUERY_NAMES = 1 << 12;
    private static final int ALL = (1 << ACTIONS.length) - 1;

    /**
     * Everything below derives from the name and the actions, which is the only thing serialized.
     */
    private transient String classPattern;
    private transient boolean classSuffixWildcard;
    private transient String memberPattern;
    private transient ObjectName namePattern;
    private transient int mask;

    /**
     * @param name {@code class#member[objectName]}, or {@code *}
     * @param actions comma-separated list, or {@code *}
     * @throws IllegalArgumentException if the name or the actions do not parse
     */
    public MBeanPermission(String name, String actions) {
        super(name);
        parseName(name);
        this.mask = maskOf(actions);
    }

    /**
     * Builds the name from the parts, so as not to force concatenating by hand.
     *
     * @param className {@code null} means "any"
     * @param member {@code null} means "any"
     * @param objectName {@code null} means "any"
     */
    public MBeanPermission(String className, String member, ObjectName objectName, String actions) {
        this(buildName(className, member, objectName), actions);
    }

    private static String buildName(String className, String member, ObjectName objectName) {
        // The dash, and not the asterisk, is how the specification writes "any" in a part built
        // from `null`. It matters for the object name, where `*` is not a legal `ObjectName` and
        // would be an invalid name.
        StringBuilder sb = new StringBuilder();
        sb.append(className == null ? "-" : className);
        sb.append('#');
        sb.append(member == null ? "-" : member);
        sb.append('[');
        sb.append(objectName == null ? "-" : objectName.getCanonicalName());
        sb.append(']');
        return sb.toString();
    }

    /** The three ways of writing "any" in a part of the name. */
    private static boolean anything(String part) {
        return part.length() == 0 || part.equals("*") || part.equals("-");
    }

    private void parseName(String name) {
        if (name == null) {
            throw new NullPointerException("The name cannot be null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("The name cannot be empty");
        }
        if (name.equals("*")) {
            return; // all three stay null, that is, "any"
        }

        String rest = name;
        // The bracket is looked for from the end: an ObjectName may carry '#' inside a quoted
        // value, but the '[' that opens the object name part is the last one in the text.
        int opens = rest.indexOf('[');
        if (opens >= 0) {
            if (!rest.endsWith("]")) {
                throw new IllegalArgumentException("Missing closing bracket: " + name);
            }
            String on = rest.substring(opens + 1, rest.length() - 1);
            rest = rest.substring(0, opens);
            // Here `*` does NOT mean "any": the part is parsed as an `ObjectName`, and a lone `*`
            // is not one. For "any" there are the empty part or the dash. It is like that in the
            // specification and worth respecting: `[*]` has to fail, because whoever wrote it
            // believes they are asking for all MBeans and actually wrote an invalid name.
            if (on.length() > 0 && !on.equals("-")) {
                try {
                    namePattern = new ObjectName(on);
                } catch (MalformedObjectNameException e) {
                    throw new IllegalArgumentException("ObjectName invalido: " + on, e);
                }
            }
        }

        int num = rest.indexOf('#');
        String cls;
        if (num >= 0) {
            cls = rest.substring(0, num);
            String member = rest.substring(num + 1);
            if (!anything(member)) {
                memberPattern = member;
            }
        } else {
            cls = rest;
        }

        if (anything(cls)) {
            return;
        }
        if (cls.endsWith(".*")) {
            classSuffixWildcard = true;
            classPattern = cls.substring(0, cls.length() - 1); // the dot stays
        } else if (cls.endsWith("*")) {
            classSuffixWildcard = true;
            classPattern = cls.substring(0, cls.length() - 1);
        } else {
            classPattern = cls;
        }
    }

    private static int maskOf(String actions) {
        if (actions == null) {
            throw new IllegalArgumentException("The actions cannot be null");
        }
        String a = actions.trim();
        if (a.equals("*")) {
            return ALL;
        }
        if (a.length() == 0) {
            throw new IllegalArgumentException("The actions cannot be empty");
        }
        int m = 0;
        for (String part : a.split(",", -1)) {
            String p = part.trim();
            int bit = 0;
            for (int i = 0; i < ACTIONS.length; i++) {
                if (ACTIONS[i].equals(p)) {
                    bit = 1 << i;
                    break;
                }
            }
            if (bit == 0) {
                throw new IllegalArgumentException("Accion invalida: " + p);
            }
            m |= bit;
        }
        if ((m & BIT_QUERY_MBEANS) != 0) {
            m |= BIT_QUERY_NAMES;
        }
        return m;
    }

    /** The canonical list: same actions, always the same text and the same order. */
    public String getActions() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ACTIONS.length; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(ACTIONS[i]);
            }
        }
        return sb.toString();
    }

    public int hashCode() {
        return getName().hashCode() + getActions().hashCode();
    }

    /**
     * Covers {@code p} if its actions are a subset of this one's and if the three parts of the name
     * match.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof MBeanPermission)) {
            return false;
        }
        MBeanPermission q = (MBeanPermission) p;

        if ((mask & q.mask) != q.mask) {
            return false;
        }

        if (classPattern != null) {
            if (q.classPattern == null) {
                // the request does not say which class: a restricted permission is not enough
                return false;
            }
            if (classSuffixWildcard) {
                if (!q.classPattern.startsWith(classPattern)) {
                    return false;
                }
            } else if (!classPattern.equals(q.classPattern) || q.classSuffixWildcard) {
                return false;
            }
        }

        if (memberPattern != null && !memberPattern.equals(q.memberPattern)) {
            return false;
        }

        if (namePattern != null) {
            if (q.namePattern == null || !namePattern.apply(q.namePattern)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MBeanPermission)) {
            return false;
        }
        MBeanPermission q = (MBeanPermission) obj;
        return mask == q.mask && getName().equals(q.getName());
    }
}
