package java.util;

// A ResourceBundle whose contents are written as a literal array of key/value pairs: the subclass
// returns them from getContents() and this handles the rest.
//
// It is the simplest bundle to write by hand, and the reason handleGetObject is final here: the
// contract of "the contents are exactly what getContents returned" would mean nothing if a
// subclass could also answer keys some other way.
public abstract class ListResourceBundle extends ResourceBundle {

    // The contents of getContents(), indexed by key. Built once, on first use — getContents() is
    // written to build a fresh array on every call, so calling it repeatedly would be wasteful.
    private HashMap<String, Object> lookup;

    // A bundle with no contents yet; getContents() supplies them.
    public ListResourceBundle() {
    }

    // The value this bundle itself holds for `key`, or null.
    public final Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return this.contents().get(key);
    }

    // Every key visible through this bundle, its parents included.
    public Enumeration<String> getKeys() {
        Enumeration<String> parentKeys = null;
        if (this.parent != null) {
            parentKeys = this.parent.getKeys();
        }
        return new BundleKeyEnumeration(this.contents().keySet().iterator(), parentKeys);
    }

    // The keys this bundle defines itself.
    //
    // Overridden rather than inherited because the inherited version derives the set by walking
    // getKeys() and probing handleGetObject for each key — correct, but pointless here, where the
    // set is already sitting in the lookup table.
    protected Set<String> handleKeySet() {
        return this.contents().keySet();
    }

    // The key/value pairs of this bundle. Each element is a two-element array: the key at 0, the
    // value at 1.
    protected abstract Object[][] getContents();

    // The lookup table, built on first use from getContents().
    private HashMap<String, Object> contents() {
        if (this.lookup == null) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            Object[][] pairs = this.getContents();
            int i = 0;
            while (i < pairs.length) {
                Object[] pair = pairs[i];
                if (pair.length < 2 || pair[0] == null || pair[1] == null) {
                    throw new NullPointerException();
                }
                map.put((String) pair[0], pair[1]);
                i = i + 1;
            }
            this.lookup = map;
        }
        return this.lookup;
    }
}

// The enumeration ListResourceBundle.getKeys() hands back: this bundle's own keys, then whatever
// the parent adds that this bundle did not already define.
//
// The de-duplication is the whole job. Without it a key overridden in a child bundle would be
// enumerated twice, once per level of the chain, which is visible to anyone building a list of
// keys from it.
final class BundleKeyEnumeration implements Enumeration<String> {

    // This bundle's own keys.
    private final Iterator<String> own;

    // The parent's keys, or null when there is no parent.
    private final Enumeration<String> parent;

    // The keys already handed out, so a parent key that the child overrode is skipped.
    private final HashSet<String> seen;

    // The next key to hand out, staged by hasMoreElements. Null when none is staged.
    private String staged;

    BundleKeyEnumeration(Iterator<String> own, Enumeration<String> parent) {
        this.own = own;
        this.parent = parent;
        this.seen = new HashSet<String>();
        this.staged = null;
    }

    public boolean hasMoreElements() {
        if (this.staged != null) {
            return true;
        }
        if (this.own.hasNext()) {
            this.staged = this.own.next();
            this.seen.add(this.staged);
            return true;
        }
        if (this.parent == null) {
            return false;
        }
        while (this.parent.hasMoreElements()) {
            String key = this.parent.nextElement();
            if (!this.seen.contains(key)) {
                this.seen.add(key);
                this.staged = key;
                return true;
            }
        }
        return false;
    }

    public String nextElement() {
        if (!this.hasMoreElements()) {
            throw new NoSuchElementException();
        }
        String key = this.staged;
        this.staged = null;
        return key;
    }
}
