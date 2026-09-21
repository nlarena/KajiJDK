package java.util.prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

// KajiLibrary's java.util.prefs.AbstractPreferences -- the whole logic of the tree, so that a new
// store is nine methods and not thirty-four.
//
// THE SPLIT. The nine `...Spi` are the only thing a subclass writes, and they are deliberately
// stupid: `getSpi`/`putSpi`/`removeSpi` touch **one** key of **this** node, `keysSpi` and
// `childrenNamesSpi` enumerate, `childSpi` makes a child, `removeNodeSpi` removes this node,
// `flushSpi`/`syncSpi` talk to the store. None of them validates anything, none of them tells
// anyone, none of them knows a path exists. All of that --validating lengths, splitting paths,
// falling back to the default, keeping the child cache, firing the notifications, marking the node
// as removed-- happens up here, once.
//
// THE CHILD CACHE IS THE HEART. A node that has already been materialised stays in `kidCache` and is
// not made again, and two things in the contract depend on that. The first is that `node("/a/b")`
// called twice returns **the same object**, which is what makes registering a listener on the result
// worth anything. The second is `childrenNames()`, which joins what the store says with what is in
// the cache: a freshly created child not yet written has to show up all the same.
//
// A REMOVED NODE DOES NOT COME BACK. After `removeNode()` almost everything throws
// `IllegalStateException`, the `get`s included. The two exceptions are put in by hand and they are
// worth it: `nodeExists("")` returns `false` instead of throwing --it is the only way to ask "are
// you still alive?" without a `try`-- and `flush()` works, because there are stores that need that
// last push for the removal to reach disk. Mind the asymmetry: `node("")` **does** throw on a
// removed node, because there the check comes first.
//
// THE NOTIFICATIONS GO OUT OUTSIDE THE LOCK. The JDK queues them and delivers them on a separate
// thread; here they are delivered on the thread that made the change, but **after** releasing
// `lock`. It is a real difference and it is worth being clear about: in favour, the notification has
// already arrived when `put()` returns --deterministic, with no daemon thread alive forever-- and a
// listener that re-enters the node does not jam against its own lock. Against, a slow listener holds
// up whoever wrote. The contract says nothing about which thread delivers nor when, so neither of
// the two ways breaks it.
public abstract class AbstractPreferences extends Preferences {

    private final String name;
    private final AbstractPreferences parent;

    // The root of the tree. It is kept and not walked every time because `isUserNode()` compares it
    // by identity on every call.
    final AbstractPreferences root;

    private final String absolutePath;

    /**
     * This node's lock. It is `protected` and not private because a subclass that needs to do two
     * store operations atomically has to be able to take it.
     *
     * <p>It is **per node** and not a global one: two different nodes can be touched in parallel. The
     * price is that the operations crossing levels --`removeNode`, `node` with a path-- take several,
     * and that is why they always do it **from the top down**, which is what avoids the deadlock.
     */
    protected final Object lock = new Object();

    /**
     * Whether this node did not exist in the store when {@link #childSpi} made it.
     *
     * <p>The subclass sets it in the constructor, and whether {@link NodeChangeListener#childAdded}
     * fires depends on it: without this flag this class has no way of telling a freshly created node
     * from one that was already on disk.
     */
    protected boolean newNode = false;

    // The children already materialised, by simple name.
    private final Map<String, AbstractPreferences> kidCache =
            new HashMap<String, AbstractPreferences>();

    private boolean removed = false;

    private final ArrayList<PreferenceChangeListener> keyListeners =
            new ArrayList<PreferenceChangeListener>();
    private final ArrayList<NodeChangeListener> nodeListeners =
            new ArrayList<NodeChangeListener>();

    private static final String[] NO_STRINGS = new String[0];
    private static final AbstractPreferences[] NO_NODES = new AbstractPreferences[0];

    /**
     * A node named `name` hanging off `parent`.
     *
     * <p>The root is built with `parent` at `null` and `name` at `""`, and the two go together: a
     * root with a name or a child without one would be trees that cannot be walked, so both
     * combinations throw `IllegalArgumentException`. A name with `/` does too: the slash is the path
     * separator and a node carrying it in its name would make ambiguous any path that goes through
     * it.
     *
     * <p>The name's length is **not** checked here --`node()` checks it before calling `childSpi`--
     * because a subclass may legitimately rebuild from the store a node an earlier version wrote.
     */
    protected AbstractPreferences(AbstractPreferences parent, String name) {
        if (parent == null) {
            if (!name.equals("")) {
                throw new IllegalArgumentException("Root name '" + name + "' must be \"\"");
            }
            this.absolutePath = "/";
            this.root = this;
        } else {
            if (name.indexOf('/') != -1) {
                throw new IllegalArgumentException("Name '" + name + "' contains '/'");
            }
            if (name.equals("")) {
                throw new IllegalArgumentException("Illegal name: empty string");
            }
            this.root = parent.root;
            this.absolutePath = (parent == this.root ? "/" + name
                                                     : parent.absolutePath() + "/" + name);
        }
        this.name = name;
        this.parent = parent;
    }

    // ---- keys and values  ------------------------------------------------------------------

    public void put(String key, String value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Key too long: " + key);
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Value too long: " + value.length());
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            putSpi(key, value);
        }
        firePreferenceChange(key, value);
    }

    public String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            String result = null;
            try {
                result = getSpi(key);
            } catch (Exception e) {
                // The store failed. There is nobody to tell --`get` does not throw-- and the
                // contract already has an answer ready for "it is not there": the default value.
            }
            return result == null ? def : result;
        }
    }

    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            removeSpi(key);
        }
        firePreferenceChange(key, null);
    }

    public void clear() throws BackingStoreException {
        synchronized (lock) {
            String[] keyNames = keys();
            for (int i = 0; i < keyNames.length; i++) {
                remove(keyNames[i]);
            }
        }
    }

    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    public int getInt(String key, int def) {
        int result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // A badly typed value behaves like a missing key: see Preferences's header.
        }
        return result;
    }

    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    public long getLong(String key, long def) {
        long result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean def) {
        boolean result = def;
        String value = get(key, null);
        if (value != null) {
            // No `Boolean.parseBoolean`: that one returns `false` for anything that is not "true",
            // and here "anything" has to give the default value, not `false`.
            if (value.equalsIgnoreCase("true")) {
                result = true;
            } else if (value.equalsIgnoreCase("false")) {
                result = false;
            }
        }
        return result;
    }

    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    public float getFloat(String key, float def) {
        float result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    public double getDouble(String key, double def) {
        double result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putByteArray(String key, byte[] value) {
        put(key, java.util.Base64.getEncoder().encodeToString(value));
    }

    public byte[] getByteArray(String key, byte[] def) {
        byte[] result = def;
        String value = get(key, null);
        try {
            if (value != null) {
                // The length being a multiple of four is required by hand: `java.util`'s decoder
                // tolerates missing padding and the store does not, so without this a truncated value
                // would read back as good bytes instead of falling to the default.
                if (value.length() % 4 != 0) {
                    return def;
                }
                result = java.util.Base64.getDecoder().decode(value);
            }
        } catch (RuntimeException e) {
            result = def;
        }
        return result;
    }

    public String[] keys() throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            return keysSpi();
        }
    }

    // ---- the tree --------------------------------------------------------------------------

    public String[] childrenNames() throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            // The union of what is on disk and what is in the cache: a freshly created child may
            // not be written yet, and leaving it out would be saying it does not exist.
            TreeSet<String> s = new TreeSet<String>(kidCache.keySet());
            String[] fromStore = childrenNamesSpi();
            for (int i = 0; i < fromStore.length; i++) {
                s.add(fromStore[i]);
            }
            return s.toArray(NO_STRINGS);
        }
    }

    /** The children already materialised in memory, without touching the store. */
    protected final AbstractPreferences[] cachedChildren() {
        synchronized (lock) {
            return kidCache.values().toArray(NO_NODES);
        }
    }

    public Preferences parent() {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            return parent;
        }
    }

    public Preferences node(String path) {
        ArrayList<AbstractPreferences> fresh = new ArrayList<AbstractPreferences>();
        Preferences result;
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (path.equals("")) {
                return this;
            }
            if (path.equals("/")) {
                return root;
            }
            if (path.charAt(0) != '/') {
                result = node(new StringTokenizer(path, "/", true), fresh);
                fireChildAdded(fresh);
                return result;
            }
        }
        // An absolute path. This node's own lock is released on purpose before taking the root's:
        // taking them the other way round --from the bottom up-- is the only way to jam two
        // threads.
        result = root.node(new StringTokenizer(path.substring(1), "/", true), fresh);
        fireChildAdded(fresh);
        return result;
    }

    private Preferences node(StringTokenizer path, ArrayList<AbstractPreferences> fresh) {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null) {
                if (token.length() > MAX_NAME_LENGTH) {
                    throw new IllegalArgumentException("Node name " + token + " too long");
                }
                child = childSpi(token);
                if (child.newNode) {
                    fresh.add(child);
                }
                kidCache.put(token, child);
            }
            if (!path.hasMoreTokens()) {
                return child;
            }
            path.nextToken(); // consume the slash
            if (!path.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child.node(path, fresh);
        }
    }

    public boolean nodeExists(String path) throws BackingStoreException {
        synchronized (lock) {
            // "" before the removal check, and not the other way round: it is the only question a
            // removed node has to be able to answer.
            if (path.equals("")) {
                return !removed;
            }
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (path.equals("/")) {
                return true;
            }
            if (path.charAt(0) != '/') {
                return nodeExists(new StringTokenizer(path, "/", true));
            }
        }
        return root.nodeExists(new StringTokenizer(path.substring(1), "/", true));
    }

    private boolean nodeExists(StringTokenizer path) throws BackingStoreException {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null) {
                if (token.length() > MAX_NAME_LENGTH) {
                    throw new IllegalArgumentException("Node name " + token + " too long");
                }
                child = getChild(token);
                if (child == null) {
                    return false;
                }
                kidCache.put(token, child);
            }
            if (!path.hasMoreTokens()) {
                return true;
            }
            path.nextToken();
            if (!path.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child.nodeExists(path);
        }
    }

    public void removeNode() throws BackingStoreException {
        if (this == root) {
            // There is nobody to take it out of: the root has no parent to forget it, and
            // `Preferences.userRoot()` would hand it back the moment after.
            throw new UnsupportedOperationException("Can't remove the root!");
        }
        ArrayList<AbstractPreferences> removals = new ArrayList<AbstractPreferences>();
        synchronized (parent.lock) {
            removeNode2(removals);
            parent.kidCache.remove(name);
        }
        fireChildRemoved(removals);
    }

    private void removeNode2(ArrayList<AbstractPreferences> removals) throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node already removed.");
            }
            // The children that are on disk but not yet in memory are materialised here: without
            // that the removal would leave live descendants in the store.
            String[] names = childrenNamesSpi();
            for (int i = 0; i < names.length; i++) {
                if (!kidCache.containsKey(names[i])) {
                    kidCache.put(names[i], childSpi(names[i]));
                }
            }
            for (Iterator<AbstractPreferences> i = kidCache.values().iterator(); i.hasNext();) {
                AbstractPreferences child = i.next();
                try {
                    child.removeNode2(removals);
                } catch (BackingStoreException x) {
                    // A child that could not be removed cannot abort the parent's removal: the tree
                    // would be left half way with no way of finishing it.
                }
                i.remove();
            }
            removeNodeSpi();
            removed = true;
            removals.add(this);
        }
    }

    public String name() {
        return name;
    }

    public String absolutePath() {
        return absolutePath;
    }

    public boolean isUserNode() {
        return root == Preferences.userRoot();
    }

    public String toString() {
        return (isUserNode() ? "User" : "System") + " Preference Node: " + absolutePath();
    }

    // ---- notifications --------------------------------------------------------------------------

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        if (pcl == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (keyListeners) {
                keyListeners.add(pcl);
            }
        }
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (keyListeners) {
                if (!keyListeners.remove(pcl)) {
                    throw new IllegalArgumentException("Listener not registered.");
                }
            }
        }
    }

    public void addNodeChangeListener(NodeChangeListener ncl) {
        if (ncl == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (nodeListeners) {
                nodeListeners.add(ncl);
            }
        }
    }

    public void removeNodeChangeListener(NodeChangeListener ncl) {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (nodeListeners) {
                if (!nodeListeners.remove(ncl)) {
                    throw new IllegalArgumentException("Listener not registered.");
                }
            }
        }
    }

    private void firePreferenceChange(String key, String isNew) {
        PreferenceChangeListener[] copy;
        synchronized (keyListeners) {
            if (keyListeners.isEmpty()) {
                return;
            }
            copy = keyListeners.toArray(new PreferenceChangeListener[0]);
        }
        PreferenceChangeEvent evt = new PreferenceChangeEvent(this, key, isNew);
        for (int i = 0; i < copy.length; i++) {
            copy[i].preferenceChange(evt);
        }
    }

    private static void fireChildAdded(ArrayList<AbstractPreferences> fresh) {
        for (int i = 0; i < fresh.size(); i++) {
            AbstractPreferences child = fresh.get(i);
            child.parent.fireNodeChange(child, true);
        }
    }

    private static void fireChildRemoved(ArrayList<AbstractPreferences> removals) {
        for (int i = 0; i < removals.size(); i++) {
            AbstractPreferences child = removals.get(i);
            child.parent.fireNodeChange(child, false);
        }
    }

    private void fireNodeChange(AbstractPreferences child, boolean added) {
        NodeChangeListener[] copy;
        synchronized (nodeListeners) {
            if (nodeListeners.isEmpty()) {
                return;
            }
            copy = nodeListeners.toArray(new NodeChangeListener[0]);
        }
        NodeChangeEvent evt = new NodeChangeEvent(this, child);
        for (int i = 0; i < copy.length; i++) {
            if (added) {
                copy[i].childAdded(evt);
            } else {
                copy[i].childRemoved(evt);
            }
        }
    }

    // ---- backing store --------------------------------------------------------------------------

    public void sync() throws BackingStoreException {
        sync2();
    }

    private void sync2() throws BackingStoreException {
        AbstractPreferences[] children;
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed");
            }
            syncSpi();
            children = cachedChildren();
        }
        // The children are walked **outside** the parent's lock: doing it inside would take the
        // whole subtree at once and any other thread touching a leaf would end up waiting on the
        // root.
        for (int i = 0; i < children.length; i++) {
            children[i].sync2();
        }
    }

    public void flush() throws BackingStoreException {
        flush2();
    }

    private void flush2() throws BackingStoreException {
        AbstractPreferences[] children;
        synchronized (lock) {
            flushSpi();
            // Unlike `sync`, on a removed node it does not throw: there are stores that need this
            // last `flushSpi` for the removal to reach disk.
            if (removed) {
                return;
            }
            children = cachedChildren();
        }
        for (int i = 0; i < children.length; i++) {
            children[i].flush2();
        }
    }

    /** Whether this node has already been removed. */
    protected boolean isRemoved() {
        synchronized (lock) {
            return removed;
        }
    }

    /**
     * The child named `nodeName` if it **already exists** in the store, or `null`.
     *
     * <p>It is the other side of {@link #childSpi}, which creates. The default implementation
     * enumerates and compares, which always works but costs; a subclass with a way of asking about a
     * node directly should replace it.
     */
    protected AbstractPreferences getChild(String nodeName) throws BackingStoreException {
        synchronized (lock) {
            String[] names = childrenNames();
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(nodeName)) {
                    return childSpi(names[i]);
                }
            }
        }
        return null;
    }

    // ---- XML -------------------------------------------------------------------------------

    public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        Xml.export(os, this, false);
    }

    public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        Xml.export(os, this, true);
    }

    // ---- what the subclass writes   --------------------------------------------------------

    /** It associates `value` with `key` in this node, validating nothing. */
    protected abstract void putSpi(String key, String value);

    /** `key`'s value in this node, or `null` if it is not there. */
    protected abstract String getSpi(String key);

    /** It removes `key` from this node. */
    protected abstract void removeSpi(String key);

    /**
     * It removes this node from the store.
     *
     * <p>{@link #removeNode} calls it once the children have been removed, so the implementation can
     * take for granted that the node is empty of descendants.
     */
    protected abstract void removeNodeSpi() throws BackingStoreException;

    /** This node's keys. Never `null`. */
    protected abstract String[] keysSpi() throws BackingStoreException;

    /** The simple names of the children the store has. Never `null`. */
    protected abstract String[] childrenNamesSpi() throws BackingStoreException;

    /**
     * The object standing for the child `name`, creating it in the store if it did not exist.
     *
     * <p>It does not have to consult `kidCache` --{@link #node} takes care of that, and it is the
     * only caller and only when the child is not in the cache-- but it **does** have to set
     * {@link #newNode} when it has just created it.
     */
    protected abstract AbstractPreferences childSpi(String name);

    /** It pushes this node's changes to the store. */
    protected abstract void flushSpi() throws BackingStoreException;

    /** Like {@link #flushSpi}, and it also brings in the changes another process made. */
    protected abstract void syncSpi() throws BackingStoreException;
}
