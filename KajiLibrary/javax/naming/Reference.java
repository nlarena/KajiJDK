package javax.naming;

import java.util.Enumeration;
import java.util.Vector;

/**
 * The recipe to rebuild an object: its class name, its addresses, and who builds it.
 *
 * <h2>Why the object itself is not bound</h2>
 *
 * <p>A naming service stores data, not another process's memory. Binding a live `DataSource` means
 * nothing to whoever does a `lookup` from another machine half an hour later. What gets bound is
 * this: "class `javax.sql.DataSource`, factory `com.x.DsFactory`, and these addresses --this URL,
 * this user--". Whoever resolves it loads the factory and passes it the reference, and gets an
 * equivalent object, not the same one.
 *
 * <p>Hence the three identity fields. `className` is what whoever does the `lookup` **expects** to
 * get, and it serves to filter without building anything. `classFactory` is who knows how to build
 * it, and `classFactoryLocation` where to download that factory if it is not on the class path --a
 * codebase URL--, which can be `null` and almost always is.
 *
 * <h2>Why the addresses are a list and not a map</h2>
 *
 * <p>Because order matters and types repeat: a service with three replicas has three addresses of
 * type `"URL"`, and the order is the preference. `get(String)` returns **the first** of that type,
 * which is the useful reading of "give me the URL"; whoever wants them all uses `getAll()`.
 *
 * <h2>Equality and cloning</h2>
 *
 * <p>`equals` compares the class and the addresses **in order**, and deliberately ignores the
 * factory: two references to the same object with a different way of building it describe the same
 * object. The JDK's javadoc says so explicitly and it is not an oversight.
 *
 * <p>`clone` is shallow in the addresses --it copies the list, not the `RefAddr`s. That is fine
 * because a `RefAddr` is immutable in practice: no subclass in the package has setters.
 */
public class Reference implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -1673475790065791735L;

    /** The class of the object this reference describes. */
    protected String className;

    /** The addresses, in order of preference. */
    protected Vector<RefAddr> addrs = null;

    /** Who knows how to build the object; `null` if whoever resolves has to manage on their own. */
    protected String classFactory = null;

    /** Where to download the factory if it is not on the class path. Almost always `null`. */
    protected String classFactoryLocation = null;

    public Reference(String className) {
        this.className = className;
        addrs = new Vector<RefAddr>();
    }

    public Reference(String className, RefAddr addr) {
        this.className = className;
        addrs = new Vector<RefAddr>();
        addrs.addElement(addr);
    }

    public Reference(String className, String factory, String factoryLocation) {
        this(className);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    public Reference(String className, RefAddr addr, String factory, String factoryLocation) {
        this(className, addr);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    public String getClassName() {
        return className;
    }

    public String getFactoryClassName() {
        return classFactory;
    }

    public String getFactoryClassLocation() {
        return classFactoryLocation;
    }

    /** The **first** address of that type, which is the preferred one; `null` if there is none. */
    public RefAddr get(String addrType) {
        int len = addrs.size();
        for (int i = 0; i < len; i++) {
            RefAddr addr = addrs.elementAt(i);
            if (addr.getType().compareTo(addrType) == 0) {
                return addr;
            }
        }
        return null;
    }

    public RefAddr get(int posn) {
        return addrs.elementAt(posn);
    }

    public Enumeration<RefAddr> getAll() {
        return addrs.elements();
    }

    public int size() {
        return addrs.size();
    }

    public void add(RefAddr addr) {
        addrs.addElement(addr);
    }

    public void add(int posn, RefAddr addr) {
        addrs.insertElementAt(addr, posn);
    }

    /** Returns `Object` and not `RefAddr` because of the API's age; it is always a `RefAddr`. */
    public Object remove(int posn) {
        Object r = addrs.elementAt(posn);
        addrs.removeElementAt(posn);
        return r;
    }

    public void clear() {
        addrs.setSize(0);
    }

    /**
     * Class and addresses in order. It does **not** look at the factory: contract, not oversight.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reference) {
            Reference target = (Reference) obj;
            if (target.className.equals(this.className) && target.size() == this.size()) {
                Enumeration<RefAddr> mycomps = getAll();
                Enumeration<RefAddr> comps = target.getAll();
                while (mycomps.hasMoreElements()) {
                    if (!mycomps.nextElement().equals(comps.nextElement())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = className.hashCode();
        for (Enumeration<RefAddr> e = getAll(); e.hasMoreElements(); ) {
            hash += e.nextElement().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Reference Class Name: ");
        buf.append(className).append("\n");
        int len = addrs.size();
        for (int i = 0; i < len; i++) {
            buf.append(get(i).toString());
        }
        return buf.toString();
    }

    /**
     * A copy with its own address list, but sharing the `RefAddr`s.
     *
     * <p>That is enough because a `RefAddr` has no setters: it can be shared without anyone
     * changing it underneath. What does have to be copied is the list, which is what `add`/`remove`
     * mutate.
     */
    @Override
    public Object clone() {
        Reference r = new Reference(className, classFactory, classFactoryLocation);
        Enumeration<RefAddr> a = getAll();
        r.addrs = new Vector<RefAddr>();
        while (a.hasMoreElements()) {
            r.addrs.addElement(a.nextElement());
        }
        return r;
    }
}
