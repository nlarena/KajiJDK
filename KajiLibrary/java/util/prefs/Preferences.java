package java.util.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// KajiLibrary's java.util.prefs.Preferences -- a tree of keys and values per user and per
// application, with persistence.
//
// THE IDEA. There are two trees: the user's and the system's. Each node of the tree has a name,
// children, and its own table of keys to strings. The path is written with slashes like a file's,
// and by convention each Java package keeps the node matching its name --`com.acme.db` lives at
// `/com/acme/db`-- which is what {@link #userNodeForPackage} and {@link #systemNodeForPackage} do.
// That way two different libraries do not step on each other without having to agree on anything.
//
// THE VALUES ARE STRINGS AND NOTHING ELSE. `putInt`, `putDouble`, `putByteArray` are all sugar over
// `put(String, String)`: they store the textual representation and that is that. That explains the
// rule of this package that is easiest to implement wrong: **a badly typed value behaves like a
// missing key**. `getInt("k", 7)` over a value `"hello"` returns `7`; it does not throw. It is
// deliberate -- a corrupt preference may have been written by an old version of the program, or by
// a user editing the file by hand, and dumping an exception on the program over that would leave it
// unable to start.
//
// FOR THE SAME REASON NO `get` AND NO `put` THROWS `BackingStoreException`. It is thrown only by the
// operations that have no default answer: enumerating (`keys`, `childrenNames`, `nodeExists`),
// deleting (`removeNode`, `clear`) and forcing the write (`flush`, `sync`).
//
// THE LIMITS ARE PART OF THE CONTRACT, not of the implementation: a key cannot exceed
// {@link #MAX_KEY_LENGTH} characters, a node name {@link #MAX_NAME_LENGTH}, a value
// {@link #MAX_VALUE_LENGTH}. They are fixed precisely so that a program can be written once and work
// over any store, the Windows registry included.
//
// ---------------------------------------------------------------------------------------------
// WHERE THIS IS STORED. In this JDK `user.home`, `user.dir` and `java.io.tmpdir` are `null` and
// `System.getenv` returns nothing, so there is no user directory to point at. What does work is the
// filesystem through paths **relative** to the process's working directory. That is why the default
// store --{@link FilePreferences}, via {@link FilePreferencesFactory}-- lives at `.java/.userPrefs`
// and `.java/.systemPrefs` hanging off the working directory, which is the same structure the JDK
// uses under POSIX minus the home prefix. It can be moved with the properties
// `java.util.prefs.userRoot` and `java.util.prefs.systemRoot`, just as in the JDK. Nothing is created
// on disk until something is written: asking for `userRoot()` and reading does not dirty the
// directory.
//
// Two honest consequences of that, because they are not the JDK's. The first: the user tree and the
// system tree are two directories and not two permission scopes, so
// {@link AbstractPreferences#isUserNode} tells which tree you came from but implies no different
// privilege. The second: "the user" is in fact "the directory the VM was launched from"; two users
// running from the same directory share the preferences. Neither of the two is a lie about the
// contract --the contract promises no permissions-- but they are worth knowing.
//
// If the directory cannot be created (a read-only working directory, for instance) the tree still
// works in memory and it is `flush()` and `sync()` that throw {@link BackingStoreException} saying
// which directory failed. It is the only honest way to degrade: what cannot be done is reported
// through the place the contract leaves for reporting it.
// ---------------------------------------------------------------------------------------------
//
// WHAT IS NOT HERE. Nothing: the class's 42 public members are all here. `importPreferences`
// included -- see {@link Xml}, which brings a parser of its own because this tree has neither
// `org.w3c.dom` nor `org.xml.sax` nor `javax.xml.parsers`.
public abstract class Preferences {

    /** The maximum length of a key, in characters. */
    public static final int MAX_KEY_LENGTH = 80;

    /** The maximum length of a node's name, in characters. */
    public static final int MAX_NAME_LENGTH = 80;

    /** The maximum length of a value, in characters. */
    public static final int MAX_VALUE_LENGTH = 8192;

    // It is resolved once and kept: `isUserNode()` compares the root by identity, so a factory that
    // returned a new object on every call would break that comparison.
    private static final PreferencesFactory FACTORY = chooseFactory();

    private static PreferencesFactory chooseFactory() {
        String name = System.getProperty("java.util.prefs.PreferencesFactory");
        if (name != null && name.length() != 0) {
            try {
                Class<?> c = Class.forName(name, true, ClassLoader.getSystemClassLoader());
                return (PreferencesFactory) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // If a factory was asked for by name and could not be had, keeping quiet and using
                // another would be worse than failing: the preferences would go somewhere that was
                // not chosen.
                throw new InternalError(
                        "could not instantiate java.util.prefs.PreferencesFactory=" + name, e);
            }
        }
        return new FilePreferencesFactory();
    }

    /** For the subclasses; there is nothing to initialise. */
    protected Preferences() {
    }

    // ---- the roots  ------------------------------------------------------------------------

    /** The root of the user tree. */
    public static Preferences userRoot() {
        return FACTORY.userRoot();
    }

    /** The root of the system tree. */
    public static Preferences systemRoot() {
        return FACTORY.systemRoot();
    }

    /**
     * The node of the user tree that matches `c`'s package.
     *
     * <p>`com.acme.Db` gives `/com/acme`. A class of the default package gives `/<unnamed>`, which is
     * not a node name that can be written by hand and therefore collides with nothing.
     */
    public static Preferences userNodeForPackage(Class<?> c) {
        return packageNode(c, true);
    }

    /** The node of the system tree that matches `c`'s package. */
    public static Preferences systemNodeForPackage(Class<?> c) {
        return packageNode(c, false);
    }

    private static Preferences packageNode(Class<?> c, boolean isUser) {
        if (c.isArray()) {
            // An array belongs to no package anyone wrote: `int[]` has no owner, and `String[]`
            // would give `java.lang`'s node, which is nobody's.
            throw new IllegalArgumentException("Arrays have no associated preferences node.");
        }
        String className = c.getName();
        int dot = className.lastIndexOf('.');
        String pkg = (dot < 0) ? "" : className.substring(0, dot);
        String path = pkg.length() == 0 ? "/<unnamed>" : "/" + pkg.replace('.', '/');
        return isUser ? userRoot().node(path) : systemRoot().node(path);
    }

    // ---- keys and values  ------------------------------------------------------------------

    /** It associates `value` with `key` in this node. */
    public abstract void put(String key, String value);

    /** `key`'s value, or `def` if it is not there (or if the store could not be consulted). */
    public abstract String get(String key, String def);

    /** It removes `key` from this node. */
    public abstract void remove(String key);

    /** It removes every key of this node. It does not touch the children. */
    public abstract void clear() throws BackingStoreException;

    /** It stores `value` as its decimal representation. */
    public abstract void putInt(String key, int value);

    /** The `int` stored at `key`, or `def` if it is missing or is not an `int`. */
    public abstract int getInt(String key, int def);

    /** It stores `value` as its decimal representation. */
    public abstract void putLong(String key, long value);

    /** The `long` stored at `key`, or `def` if it is missing or is not a `long`. */
    public abstract long getLong(String key, long def);

    /** It stores `"true"` or `"false"`. */
    public abstract void putBoolean(String key, boolean value);

    /** The `boolean` stored at `key`, or `def` if it is missing or is not `"true"`/`"false"`. */
    public abstract boolean getBoolean(String key, boolean def);

    /** It stores `value` with {@link Float#toString}. */
    public abstract void putFloat(String key, float value);

    /** The `float` stored at `key`, or `def` if it is missing or is not a `float`. */
    public abstract float getFloat(String key, float def);

    /** It stores `value` with {@link Double#toString}. */
    public abstract void putDouble(String key, double value);

    /** The `double` stored at `key`, or `def` if it is missing or is not a `double`. */
    public abstract double getDouble(String key, double def);

    /** It stores `value` in Base64: it is the only way to put bytes into a store of strings. */
    public abstract void putByteArray(String key, byte[] value);

    /** The bytes stored at `key`, or `def` if it is missing or is not valid Base64. */
    public abstract byte[] getByteArray(String key, byte[] def);

    /** This node's keys, in any order. */
    public abstract String[] keys() throws BackingStoreException;

    /** The simple names of this node's children. */
    public abstract String[] childrenNames() throws BackingStoreException;

    /** The parent, or `null` if this is the root. */
    public abstract Preferences parent();

    /**
     * The node at `pathName`, creating it --and any missing ancestors-- if it did not exist.
     *
     * <p>A path that starts with `/` resolves from the root of **this** tree; any other, from this
     * node. `""` is this very node.
     */
    public abstract Preferences node(String pathName);

    /** Whether the node at `pathName` exists. `""` asks about this node and does not throw even if it has been removed. */
    public abstract boolean nodeExists(String pathName) throws BackingStoreException;

    /** It removes this node and all its descendants. */
    public abstract void removeNode() throws BackingStoreException;

    /** This node's simple name; `""` for the root. */
    public abstract String name();

    /** This node's absolute path within its tree. */
    public abstract String absolutePath();

    /** Whether this node is in the user tree. */
    public abstract boolean isUserNode();

    /** `"User Preference Node: <path>"` or `"System Preference Node: <path>"`. */
    public abstract String toString();

    // ---- backing store --------------------------------------------------------------------------

    /** It pushes to the store the changes of this node and of whichever descendants are in memory. */
    public abstract void flush() throws BackingStoreException;

    /** Like {@link #flush}, and it also brings in the changes another VM made. */
    public abstract void sync() throws BackingStoreException;

    // ---- notifications --------------------------------------------------------------------------

    /** It starts telling `pcl` about **this** node's key changes. */
    public abstract void addPreferenceChangeListener(PreferenceChangeListener pcl);

    /** It stops telling `pcl`. */
    public abstract void removePreferenceChangeListener(PreferenceChangeListener pcl);

    /** It starts telling `ncl` about **this** node's children being added and removed. */
    public abstract void addNodeChangeListener(NodeChangeListener ncl);

    /** It stops telling `ncl`. */
    public abstract void removeNodeChangeListener(NodeChangeListener ncl);

    // ---- XML -------------------------------------------------------------------------------

    /** It writes to `os` an XML document with this node's keys and no children. */
    public abstract void exportNode(OutputStream os) throws IOException, BackingStoreException;

    /** It writes to `os` an XML document with this node and its whole subtree. */
    public abstract void exportSubtree(OutputStream os) throws IOException, BackingStoreException;

    /**
     * It reads a document like the ones {@link #exportNode} and {@link #exportSubtree} write and
     * applies what it says.
     *
     * <p>The document chooses by itself which tree it goes to --`<root>`'s `type` attribute-- and
     * that is why the method is static and not an instance one: there is no node "on which" to
     * import.
     */
    public static void importPreferences(InputStream is)
            throws IOException, InvalidPreferencesFormatException {
        Xml.doImport(is);
    }
}
