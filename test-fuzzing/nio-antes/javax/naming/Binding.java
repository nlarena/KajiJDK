package javax.naming;

/**
 * A `NameClassPair` **with the object inside**: what `Context.listBindings()` returns.
 *
 * <p>The difference from listing names is the cost, and it is explained in `NameClassPair`. What
 * this class adds is the already materialized object, for whoever really is going to use them all.
 *
 * <h2>Why there are four constructors and why `getClassName` is redefined</h2>
 *
 * <p>The class name can be deduced from the object, so half of the constructors do not ask for it:
 * `getClassName()` looks first at the declared one and, if there is none, asks
 * `getObject().getClass().getName()`. The deduction is not always enough --the provider may know
 * that the bound object is of a class that does not even exist here, or the object may be `null`
 * and the class name known anyway--, which is why the other constructors let you declare it. (An
 * earlier note said eight constructors; there are four, as in the JDK.)
 *
 * <p>And it returns `null` when there is neither, instead of throwing: a `list` of a half-broken
 * context must still be able to return the row.
 */
public class Binding extends NameClassPair {

    private static final long serialVersionUID = 8839217842691845890L;

    private Object boundObj;

    /** The class name is left undeclared: it is deduced from the object in `getClassName()`. */
    public Binding(String name, Object obj) {
        super(name, null);
        this.boundObj = obj;
    }

    public Binding(String name, Object obj, boolean isRelative) {
        super(name, null, isRelative);
        this.boundObj = obj;
    }

    public Binding(String name, String className, Object obj) {
        super(name, className);
        this.boundObj = obj;
    }

    public Binding(String name, String className, Object obj, boolean isRelative) {
        super(name, className, isRelative);
        this.boundObj = obj;
    }

    /**
     * The declared one wins; if there is none, it is deduced from the object; if no object, `null`.
     */
    @Override
    public String getClassName() {
        String cname = super.getClassName();
        if (cname != null) {
            return cname;
        }
        Object obj = getObject();
        return (obj != null) ? obj.getClass().getName() : null;
    }

    public Object getObject() {
        return boundObj;
    }

    public void setObject(Object obj) {
        this.boundObj = obj;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getObject();
    }
}
