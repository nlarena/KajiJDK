package java.util.jar;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The name/value pairs of one section of a JAR's manifest.
 *
 * <p>It is a `Map` with two restrictions that do not show in the signature: the keys are
 * {@link Attributes.Name} --never `String`-- and the values are `String`. `put` **casts**, so putting
 * a `String` in as a key throws `ClassCastException`; the shortcut for not writing the `Name` by
 * hand is {@link #putValue}.
 *
 * <p>Order matters and that is why the map below is a `LinkedHashMap`: the manifest is written in
 * the order the attributes were loaded in, and a round trip has to give the same bytes. The JDK does
 * the same since 9.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>Nothing of the public surface. The four members the JDK declares and are not here --`write`,
 * `writeMain` and the two `read`-- are **package-private** in the JDK too, that is, internal; here
 * the first two exist with that same visibility and the `read` live in `Manifest`, which is the one
 * with the line reader. By the contract rule, the internals are free.
 */
public class Attributes implements Map<Object, Object>, Cloneable {

    /** The map that holds the pairs. It is `protected` because the JDK exposes it that way. */
    protected Map<Object, Object> map;

    /** An empty attribute set. */
    public Attributes() {
        this(11);
    }

    /** An empty attribute set with room for `size` pairs. */
    public Attributes(int size) {
        this.map = new LinkedHashMap<Object, Object>(size);
    }

    /** A copy of `attr`. */
    public Attributes(Attributes attr) {
        this.map = new LinkedHashMap<Object, Object>(attr.map);
    }

    /**
     * That attribute's value, or `null`.
     *
     * <p>The key has to be a `Name`: handing it a `String` returns `null`, not the value. It is this
     * class's classic trap and it is in the JDK just the same.
     */
    public Object get(Object name) {
        return this.map.get(name);
    }

    /** That attribute's value, looked up by name and **case-insensitively**. */
    public String getValue(String name) {
        return (String) this.map.get(new Name(name));
    }

    /** That attribute's value. */
    public String getValue(Name name) {
        return (String) this.map.get(name);
    }

    /**
     * It associates a value with an attribute.
     *
     * @throws ClassCastException if `name` is not a `Name` or `value` is not a `String`
     */
    public Object put(Object name, Object value) {
        Name k = (Name) name;
        String v = (String) value;
        return this.map.put(k, v);
    }

    /** Like `put`, but building the `Name` from the text. */
    public String putValue(String name, String value) {
        return (String) put(new Name(name), value);
    }

    /** It removes that attribute and returns the value it had. */
    public Object remove(Object name) {
        return this.map.remove(name);
    }

    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    public boolean containsKey(Object name) {
        return this.map.containsKey(name);
    }

    /**
     * It copies every pair from `attr`.
     *
     * @throws ClassCastException if `attr` is not an `Attributes`
     */
    public void putAll(Map<?, ?> attr) {
        // The JDK demands an `Attributes` and not just any map. It is not a whim: any old map can
        // have `String` keys, and then the `put` above would throw halfway through leaving the copy
        // half done. Checking first makes it fail whole or not fail.
        if (!(attr instanceof Attributes)) {
            throw new ClassCastException();
        }
        Attributes other = (Attributes) attr;
        for (Map.Entry<Object, Object> e : other.map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        this.map.clear();
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public Set<Object> keySet() {
        return this.map.keySet();
    }

    public Collection<Object> values() {
        return this.map.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Attributes)) {
            return false;
        }
        return this.map.equals(((Attributes) o).map);
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    /** A copy. */
    public Object clone() {
        return new Attributes(this);
    }

    // ---- writing --------------------------------------------------------------------------------

    /** It writes this section --without the `Name:` line-- and ends it with the blank line. */
    void write(DataOutputStream out) throws IOException {
        for (Map.Entry<Object, Object> e : this.map.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(((Name) e.getKey()).toString());
            sb.append(": ");
            sb.append((String) e.getValue());
            Manifest.println72(out, sb.toString());
        }
        Manifest.println(out);
    }

    /**
     * It writes the **main** section, which has two rules of its own.
     *
     * <p>The first: `Manifest-Version` goes **first** even if it was not the first loaded, and if it
     * is not there `Signature-Version` is tried. It is what makes a manifest recognisable by its
     * first line.
     *
     * <p>The second is odd and is replicated on purpose: if **neither** is there, the JDK writes
     * **no** attribute at all --only the blank line--. It was checked against JDK 25 before being
     * copied. A manifest with no version is not a manifest, so losing the attributes is consistent
     * with that, but the underlying reason is that the loop is guarded by `version != null`.
     */
    void writeMain(DataOutputStream out) throws IOException {
        String vername = Name.MANIFEST_VERSION.toString();
        String version = getValue(vername);
        if (version == null) {
            vername = Name.SIGNATURE_VERSION.toString();
            version = getValue(vername);
        }
        if (version != null) {
            // The JDK uses `writeBytes`, which keeps each `char`'s low byte and does not fold. Here
            // it goes through `println72`: for the real versions --"1.0"-- it comes out byte for byte
            // the same, and for a long or non-ASCII version it comes out right instead of wrong.
            Manifest.println72(out, vername + ": " + version);
        }
        if (version != null) {
            for (Map.Entry<Object, Object> e : this.map.entrySet()) {
                String name = ((Name) e.getKey()).toString();
                if (!name.equalsIgnoreCase(vername)) {
                    Manifest.println72(out, name + ": " + (String) e.getValue());
                }
            }
        }
        Manifest.println(out);
    }

    /**
     * A manifest attribute's name.
     *
     * <p>It is a class of its own and not a `String` for a single reason, and it is the one that
     * justifies everything else: attribute names are **case-insensitive**. `Class-Path` and
     * `class-path` are the same attribute, and a `String` as the map's key cannot express that.
     *
     * <p>The allowed character set is closed --letters, digits, `-` and `_`-- and the length runs
     * from 1 to 70. It is not decorative: a name with a `:` or a space generates a manifest that
     * cannot be read back, so it is rejected on construction and not on writing.
     */
    public static class Name {

        private final String name;
        private int hash;

        /**
         * An attribute name.
         *
         * @throws NullPointerException if `name` is `null`
         * @throws IllegalArgumentException if it is not a valid header name
         */
        public Name(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (!isValid(name)) {
                throw new IllegalArgumentException(name);
            }
            this.name = name;
            this.hash = 0;
        }

        private static boolean isValid(String s) {
            int n = s.length();
            if (n > 70 || n == 0) {
                return false;
            }
            int i = 0;
            while (i < n) {
                char c = s.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9')
                        || c == '_' || c == '-';
                if (!ok) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }

        /** Two names are equal if they differ only in case. */
        public boolean equals(Object o) {
            if (!(o instanceof Name)) {
                return false;
            }
            return this.name.equalsIgnoreCase(((Name) o).name);
        }

        /**
         * The hash of the lower-cased version, which is the only thing compatible with the `equals`
         * above.
         *
         * <p>Only the ASCII range is folded by hand instead of calling `toLowerCase()`: the
         * constructor already guaranteed there is nothing outside ASCII, and this way the hash does
         * not depend on the locale.
         */
        public int hashCode() {
            if (this.hash == 0) {
                int h = 0;
                int i = 0;
                while (i < this.name.length()) {
                    char c = this.name.charAt(i);
                    if (c >= 'A' && c <= 'Z') {
                        c = (char) (c + 32);
                    }
                    h = 31 * h + c;
                    i = i + 1;
                }
                this.hash = h;
            }
            return this.hash;
        }

        /** The name exactly as it was written. */
        public String toString() {
            return this.name;
        }

        /** `Manifest-Version` */
        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");
        /** `Signature-Version` */
        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");
        /** `Content-Type` */
        public static final Name CONTENT_TYPE = new Name("Content-Type");
        /** `Class-Path` */
        public static final Name CLASS_PATH = new Name("Class-Path");
        /** `Main-Class` */
        public static final Name MAIN_CLASS = new Name("Main-Class");
        /** `Sealed` */
        public static final Name SEALED = new Name("Sealed");
        /** `Extension-List` */
        public static final Name EXTENSION_LIST = new Name("Extension-List");
        /** `Extension-Name` */
        public static final Name EXTENSION_NAME = new Name("Extension-Name");
        /** `Extension-Installation` */
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");
        /** `Implementation-Title` */
        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");
        /** `Implementation-Version` */
        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");
        /** `Implementation-Vendor` */
        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");
        /** `Implementation-Vendor-Id` */
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");
        /** `Implementation-URL` */
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");
        /** `Specification-Title` */
        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");
        /** `Specification-Version` */
        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");
        /** `Specification-Vendor` */
        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");
        /** `Multi-Release` */
        public static final Name MULTI_RELEASE = new Name("Multi-Release");
    }
}
