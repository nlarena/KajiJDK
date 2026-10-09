package java.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * KajiLibrary's java.io.ObjectStreamClass -- a class's serializable form: which fields go out to
 * the stream, in what order and in what place.
 *
 * <p>It is a reflective query and that is why it can really be answered: "what would be stored of
 * this class?" needs to store nothing. It serves as it stands for inspecting a hierarchy before
 * deciding on a {@code transient}, or for comparing two versions of a class field by field.
 *
 * <h2>How the fields are chosen</h2>
 *
 * <p>If the class declares {@code private static final ObjectStreamField[] serialPersistentFields},
 * that list **rules** and the real fields are not looked at: it is the way of fixing a class's
 * serialized representation so that it survives its fields being renamed inside. If it does not
 * declare one, the fields **declared** by the class go out --not the inherited ones, which belong
 * to their own descriptor-- skipping the {@code static} and the {@code transient} ones.
 *
 * <p>The order is not the declaration one but the stream's: primitives first, and by name within
 * each group. The why is in {@link ObjectStreamField}.
 *
 * <h2>{@code getSerialVersionUID()} and the datum reflection does not give</h2>
 *
 * <p>When the class declares its {@code serialVersionUID} the value is right there and there is
 * nothing to work out. When it does not declare one, the specification orders it computed: a SHA-1
 * over a canonical form including the name, the modifiers, the interfaces, the fields, the
 * constructors, the methods, and --the delicate point-- **whether the class has a static
 * initializer**.
 *
 * <p>That last datum cannot be found out by reflection: {@code getDeclaredMethods()} filters {@code
 * <clinit>} on purpose, on this VM and in the JDK. That is why the JDK does not resolve it by
 * reflection either but with a **native**, and here it is done the same way: {@link
 * #hasStaticInitializer} reads the class file. Without that native the number would come out right
 * for the classes with no static block and wrong for the rest --which are most of them, because any
 * static field with a non-constant initializer generates one-- and a wrong {@code serialVersionUID}
 * is the worst way of being wrong this API has: its only purpose is for two JVMs to agree on
 * whether two classes are the same version, and the caller receives a perfectly believable {@code
 * long} with no way of noticing that it does not match the JDK's.
 */
public final class ObjectStreamClass implements Serializable {

    /** The empty array, for a class contributing no fields. Shared: it has no state. */
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];

    // It is not `final` because of the reading side: a descriptor coming from the stream is born
    // **without** a local class --the name is all it brought-- and only gets one when
    // `resolveClass` finds it. A descriptor built by reflection sets it in the constructor and
    // never changes it.
    private Class<?> cls;
    private final String name;
    private final ObjectStreamField[] fields;
    // Lazy and cached: working out the UID is a SHA-1 over the class's whole reflective surface,
    // and most descriptors never ask for it. `Long` and not `long` because 0 is a valid UID and a
    // separate sentinel would be needed.
    private Long uid;

    // ---- only for a descriptor read from a stream ----
    // The `SC_*` flags as they came. They matter when reading and not when describing: they say
    // whether the writer used a `writeObject` of its own, and therefore whether this stretch's data
    // is framed in block records or goes raw. Guessing that by looking at the local class would be
    // the classic mistake: the class on this side may have --or not have-- the method the other one
    // had, and the stream's framing was decided by whoever wrote it.
    private int streamFlags;
    // The superclass's descriptor, also from the stream. The chain ends in `null`, and that `null`
    // is the one that says where the part of the hierarchy that contributed data ends.
    private ObjectStreamClass streamSuper;
    private boolean fromStream;

    private ObjectStreamClass(Class<?> cls, ObjectStreamField[] fields) {
        this.cls = cls;
        this.name = cls.getName();
        // An enum has no serialized form of its own: what goes to the stream is **the constant's
        // name** and nothing else, because the constant already exists on the other side and all
        // there is to say is which one it is. Hence the two exceptions the specification makes and
        // that have to be honoured to the byte, because the JDK looks at them when reading and
        // rejects the stream if they are not there:
        //
        //   - `serialVersionUID` is **zero**, and not the computed fingerprint. It is not that it
        //     does not matter: an enum cannot change its serialized form, so versioning it means
        //     nothing, and the JDK's reader treats a non-zero value as an invalid stream.
        //   - **no fields**, even though `name` and `ordinal` are there. Writing them on top of the
        //     name would be saying the same thing twice, and `ordinal` on top of that ties the
        //     stream to the order the constants are declared in today.
        boolean isEnum = Enum.class.isAssignableFrom(cls);
        this.fields = isEnum ? NO_FIELDS : fields;
        if (isEnum) {
            this.uid = Long.valueOf(0L);
        }
    }

    /**
     * The descriptor as it came in the stream, still with no local class.
     *
     * <p>The UID is set from the start with the stream's instead of being computed: for a
     * descriptor that was read, the number **is** the one that came, and computing the local
     * class's would give the other version's -- exactly the one that is to be compared against
     * this, not the one that is to be reported.
     */
    ObjectStreamClass(String name, long uid, int flags, ObjectStreamField[] fields) {
        this.cls = null;
        this.name = name;
        this.fields = fields;
        this.uid = Long.valueOf(uid);
        this.streamFlags = flags;
        this.fromStream = true;
        assignOffsets(fields);
    }

    /** The local class this read descriptor was given, or `null` if there is none on this
     * side. */
    void resolvedTo(Class<?> cl) {
        this.cls = cl;
    }

    void streamSuper(ObjectStreamClass sup) {
        this.streamSuper = sup;
    }

    ObjectStreamClass streamSuper() {
        return this.streamSuper;
    }

    int streamFlags() {
        return this.streamFlags;
    }

    boolean fromStream() {
        return this.fromStream;
    }

    /**
     * `cl`'s descriptor, or `null` if `cl` is not serializable.
     *
     * <p>The `null` is the answer, not an error: asking about a class that is not serialized is
     * legitimate --it is how one finds out that it is not-- and returning an empty descriptor would
     * make "it does not take part" be confused with "it takes part with no fields", which is what
     * happens to an {@link Externalizable}.
     *
     * @throws NullPointerException if `cl` is `null`
     */
    public static ObjectStreamClass lookup(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        return new ObjectStreamClass(cl, fieldsOf(cl));
    }

    /**
     * `cl`'s descriptor, serializable or not.
     *
     * <p>It exists so that a class turning up in a stream can be described even if on this side it
     * does not implement {@link Serializable}: without this there would be nothing to name it with
     * when reporting the mismatch.
     *
     * @throws NullPointerException if `cl` is `null`
     */
    public static ObjectStreamClass lookupAny(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            // No fields, and not its own: a class that is not serialized has no serialized form,
            // and listing its fields would suggest that some of them would go out to the stream.
            return new ObjectStreamClass(cl, NO_FIELDS);
        }
        return new ObjectStreamClass(cl, fieldsOf(cl));
    }

    public String getName() {
        return this.name;
    }

    public Class<?> forClass() {
        return this.cls;
    }

    /**
     * The fields that would go out to the stream, already sorted and with their offsets.
     *
     * <p>It returns **a copy** on every call. It is not tidiness: `setOffset` is `protected` but
     * reachable from a subclass of {@link ObjectStreamField}, and a descriptor whose offsets could
     * be moved from outside would describe a format different from the one the stream uses.
     */
    public ObjectStreamField[] getFields() {
        ObjectStreamField[] copy = new ObjectStreamField[this.fields.length];
        System.arraycopy(this.fields, 0, copy, 0, this.fields.length);
        return copy;
    }

    /** The field called `name`, or `null` if there is none. */
    public ObjectStreamField getField(String name) {
        int i = 0;
        while (i < this.fields.length) {
            if (this.fields[i].getName().equals(name)) {
                return this.fields[i];
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * The version number of this class's serialized form.
     *
     * <p>If the class declares {@code static final long serialVersionUID}, that value **rules** and
     * nothing is computed: declaring it is precisely the way of saying "my format did not change
     * even though the code did". If it does not declare one, it comes from the SHA-1 of the
     * canonical form {@link #fingerprintOf} describes.
     *
     * <p>Zero for what has no serialized form --an array, a primitive-- which is what the JDK
     * returns.
     */
    public long getSerialVersionUID() {
        if (this.uid == null) {
            this.uid = Long.valueOf(computeUid(this.cls));
        }
        return this.uid.longValue();
    }

    /** The JDK's format: the name and then the line of the SUID's declaration. */
    public String toString() {
        return this.name + ": static final long serialVersionUID = "
                + this.getSerialVersionUID() + "L;";
    }

    // ---- serialVersionUID ------------------------------------------------------------------------

    // Whether the class has a `<clinit>`. Native for the same reason as in the JDK:
    // `getDeclaredMethods` filters `<clinit>` out, so reflection cannot answer it and the datum
    // goes into the fingerprint.
    private static native boolean hasStaticInitializer(Class<?> cl);

    /**
     * An instance of `cl` with every field at its default value and **without running any
     * constructor**, or `null` if `cl` cannot be instantiated.
     *
     * <p>It is deserialization's only piece that cannot be written in Java, and that is why it is
     * native. Rebuilding is not constructing: the fields come from the stream, and running the
     * constructor would carry out its effects --validations, counters, registrations in global
     * tables-- for an object that is not being created. It lives here, package-private, because its
     * only legitimate caller is {@link ObjectInputStream}: exposed it would be a way of bypassing
     * every constructor in the system.
     */
    static native Object allocateInstance(Class<?> cl);

    private static long computeUid(Class<?> cl) {
        long declared = declaredUid(cl);
        if (declared != NOT_DECLARED) {
            return declared;
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            // What is not serialized has no format version. Zero, and not a computed fingerprint: a
            // number there would suggest there is a format to compare against.
            return 0L;
        }
        try {
            byte[] h = sha1(fingerprintOf(cl));
            // The SHA-1's first eight bytes, read **backwards**: byte 0 ends up in the long's low
            // part. It is no choice, it is what the JDK does, and the number has to come out the
            // same byte for byte or the two sides do not understand each other.
            long uid = 0L;
            int i = 7;
            while (i >= 0) {
                uid = (uid << 8) | ((long) (h[i] & 0xFF));
                i = i - 1;
            }
            return uid;
        } catch (IOException impossible) {
            // The fingerprint is built over a `ByteArrayOutputStream`, which has nothing to fail
            // with.
            throw new InternalError(impossible);
        }
    }

    // A `long` has no "absent" value, so a sentinel is needed for "the class does not declare it"
    // -- and it cannot be 0, which is a perfectly valid declared SUID.
    private static final long NOT_DECLARED = 0x8000_0000_0000_0001L;

    private static long declaredUid(Class<?> cl) {
        Field f;
        try {
            f = cl.getDeclaredField("serialVersionUID");
        } catch (NoSuchFieldException ex) {
            return NOT_DECLARED;
        }
        int m = f.getModifiers();
        if (!Modifier.isStatic(m) || !Modifier.isFinal(m) || f.getType() != long.class) {
            return NOT_DECLARED;
        }
        f.setAccessible(true);
        return f.getLong(null);
    }

    /**
     * The canonical form the SHA-1 comes out of, byte by byte as the serialization specification
     * defines it.
     *
     * <p>The order is fixed and so are the sortings --interfaces and fields by name, constructors
     * by signature, methods by name and then by signature-- because the number has to come out the
     * same on two JVMs that saw the same class, and neither reflection nor the class file
     * guarantees a stable declaration order.
     *
     * <p>The `private` ones are left out of methods and constructors, and of the fields only the
     * `private static` and `private transient` ones: what is private and does not go out to the
     * stream is no part of the contract with the other JVM, so renaming it has no reason to change
     * the version.
     */
    private static byte[] fingerprintOf(Class<?> cl) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bytes);
        d.writeUTF(cl.getName());

        int mods = cl.getModifiers()
                & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
        java.lang.reflect.Method[] methods = cl.getDeclaredMethods();
        if ((mods & Modifier.INTERFACE) != 0) {
            // An interface with no methods does not carry ABSTRACT and one with methods does. It
            // looks arbitrary and it is: it comes from how old compilers marked interfaces, and it
            // is kept because changing it would move the UID of every interface in the world.
            mods = methods.length > 0 ? (mods | Modifier.ABSTRACT) : (mods & ~Modifier.ABSTRACT);
        }
        d.writeInt(mods);

        if (cl.isArray()) {
            // **An array ends here**: name and modifiers, and nothing else. It is no simplification
            // of ours but a historical compensation of the JDK's -- up to 1.2, an array's
            // `getInterfaces()` returned empty, and when it started returning `Cloneable` and
            // `Serializable` it would have moved the UID of every array in the world. The old form
            // was frozen. Without this, `int[]` gives a number different from the real one and no
            // stream with arrays reads on the other side.
            return bytes.toByteArray();
        }

        Class<?>[] ifaces = cl.getInterfaces();
        String[] ifaceNames = new String[ifaces.length];
        int i = 0;
        while (i < ifaces.length) {
            ifaceNames[i] = ifaces[i].getName();
            i = i + 1;
        }
        sortStrings(ifaceNames);
        i = 0;
        while (i < ifaceNames.length) {
            d.writeUTF(ifaceNames[i]);
            i = i + 1;
        }

        Field[] fields = cl.getDeclaredFields();
        String[] fieldKeys = new String[fields.length];
        i = 0;
        while (i < fields.length) {
            fieldKeys[i] = fields[i].getName();
            i = i + 1;
        }
        sortByKey(fieldKeys, fields);
        i = 0;
        while (i < fields.length) {
            int fm = fields[i].getModifiers()
                    & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC
                            | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT);
            if ((fm & Modifier.PRIVATE) == 0
                    || (fm & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
                d.writeUTF(fields[i].getName());
                d.writeInt(fm);
                d.writeUTF(fields[i].getType().descriptorString());
            }
            i = i + 1;
        }

        if (hasStaticInitializer(cl)) {
            d.writeUTF("<clinit>");
            d.writeInt(Modifier.STATIC);
            d.writeUTF("()V");
        }

        java.lang.reflect.Constructor<?>[] ctors = cl.getDeclaredConstructors();
        String[] ctorKeys = new String[ctors.length];
        i = 0;
        while (i < ctors.length) {
            ctorKeys[i] = descriptorOf(ctors[i].getParameterTypes(), void.class);
            i = i + 1;
        }
        sortByKey(ctorKeys, ctors);
        i = 0;
        while (i < ctors.length) {
            int cm = ctors[i].getModifiers() & EXECUTABLE_MODS;
            if ((cm & Modifier.PRIVATE) == 0) {
                d.writeUTF("<init>");
                d.writeInt(cm);
                d.writeUTF(descriptorOf(ctors[i].getParameterTypes(), void.class).replace('/', '.'));
            }
            i = i + 1;
        }

        String[] methodKeys = new String[methods.length];
        i = 0;
        while (i < methods.length) {
            // Name and signature in the same key: the order is by name and **then** by signature,
            // and gluing them with a separator that cannot appear in a name gives that order with a
            // single string comparison.
            methodKeys[i] = methods[i].getName() + " "
                    + descriptorOf(methods[i].getParameterTypes(), methods[i].getReturnType());
            i = i + 1;
        }
        sortByKey(methodKeys, methods);
        i = 0;
        while (i < methods.length) {
            int mm = methods[i].getModifiers() & EXECUTABLE_MODS;
            if ((mm & Modifier.PRIVATE) == 0) {
                d.writeUTF(methods[i].getName());
                d.writeInt(mm);
                d.writeUTF(descriptorOf(methods[i].getParameterTypes(),
                        methods[i].getReturnType()).replace('/', '.'));
            }
            i = i + 1;
        }
        return bytes.toByteArray();
    }

    private static final int EXECUTABLE_MODS = Modifier.PUBLIC | Modifier.PRIVATE
            | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED
            | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT;

    private static String descriptorOf(Class<?>[] params, Class<?> returnType) {
        StringBuilder sb = new StringBuilder("(");
        int i = 0;
        while (i < params.length) {
            sb.append(params[i].descriptorString());
            i = i + 1;
        }
        sb.append(')').append(returnType.descriptorString());
        return sb.toString();
    }

    /**
     * SHA-1 of `data`, twenty bytes.
     *
     * <p>Written in here and not by calling {@code java.security.MessageDigest}, which is what the
     * JDK does. The reason is about dependencies and not about taste: {@code java.io} is the base
     * of half the library --{@code java.security} itself rests on it, its digests are {@link
     * FilterOutputStream}-- and making a {@code serialVersionUID} drag in the provider register,
     * {@code Security}, {@code Provider.Service} and the map that indexes them is putting a cycle
     * from the base towards a very high layer. The algorithm is fifty stateless lines; the provider
     * register is not.
     *
     * <p>It is a digest function used as a **version identifier**, not as a defence: that SHA-1 is
     * broken for signing changes nothing here, and swapping it for another algorithm would change
     * every UID in the world.
     */
    private static byte[] sha1(byte[] data) {
        int[] h = new int[] { 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };

        // The padding: one bit set, zeros, and the length in **bits** as a 64-bit big-endian. The
        // length goes inside the hash on purpose -- without it, "abc" and "abc" with zeros behind
        // it would give the same thing.
        int rest = data.length % 64;
        int zeros = (rest < 56 ? 56 : 120) - rest;
        byte[] m = new byte[data.length + zeros + 8];
        System.arraycopy(data, 0, m, 0, data.length);
        m[data.length] = (byte) 0x80;
        long bits = ((long) data.length) * 8L;
        int i = 0;
        while (i < 8) {
            m[m.length - 1 - i] = (byte) (bits >>> (8 * i));
            i = i + 1;
        }

        int[] w = new int[80];
        int base = 0;
        while (base < m.length) {
            i = 0;
            while (i < 16) {
                int p = base + i * 4;
                w[i] = ((m[p] & 0xFF) << 24) | ((m[p + 1] & 0xFF) << 16)
                        | ((m[p + 2] & 0xFF) << 8) | (m[p + 3] & 0xFF);
                i = i + 1;
            }
            while (i < 80) {
                int x = w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16];
                w[i] = (x << 1) | (x >>> 31);
                i = i + 1;
            }
            int a = h[0];
            int b = h[1];
            int c = h[2];
            int d = h[3];
            int e = h[4];
            i = 0;
            while (i < 80) {
                int f;
                int k;
                if (i < 20) {
                    f = (b & c) | ((~b) & d);
                    k = 0x5A827999;
                } else if (i < 40) {
                    f = b ^ c ^ d;
                    k = 0x6ED9EBA1;
                } else if (i < 60) {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8F1BBCDC;
                } else {
                    f = b ^ c ^ d;
                    k = 0xCA62C1D6;
                }
                int t = ((a << 5) | (a >>> 27)) + f + e + k + w[i];
                e = d;
                d = c;
                c = (b << 30) | (b >>> 2);
                b = a;
                a = t;
                i = i + 1;
            }
            h[0] = h[0] + a;
            h[1] = h[1] + b;
            h[2] = h[2] + c;
            h[3] = h[3] + d;
            h[4] = h[4] + e;
            base = base + 64;
        }

        byte[] out = new byte[20];
        i = 0;
        while (i < 5) {
            out[i * 4] = (byte) (h[i] >>> 24);
            out[i * 4 + 1] = (byte) (h[i] >>> 16);
            out[i * 4 + 2] = (byte) (h[i] >>> 8);
            out[i * 4 + 3] = (byte) h[i];
            i = i + 1;
        }
        return out;
    }

    private static void sortStrings(String[] a) {
        int i = 1;
        while (i < a.length) {
            String x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = x;
            i = i + 1;
        }
    }

    // It sorts `data` by `keys`, moving both together. Insertion sort, like the rest of the class:
    // it is a few dozen members and it is not worth dragging in a generic `sort`.
    private static void sortByKey(String[] keys, Object[] data) {
        int i = 1;
        while (i < keys.length) {
            String ck = keys[i];
            Object cd = data[i];
            int j = i - 1;
            while (j >= 0 && keys[j].compareTo(ck) > 0) {
                keys[j + 1] = keys[j];
                data[j + 1] = data[j];
                j = j - 1;
            }
            keys[j + 1] = ck;
            data[j + 1] = cd;
            i = i + 1;
        }
    }

    // ---- where the fields come from --------------------------------------------------------------

    private static ObjectStreamField[] fieldsOf(Class<?> cl) {
        if (cl.isArray() || cl.isPrimitive() || cl.isInterface()) {
            // An array is written with its length and its elements, and an interface has no state:
            // in both cases there are no fields to enumerate.
            return NO_FIELDS;
        }
        if (Externalizable.class.isAssignableFrom(cl)) {
            // An externalizable writes its own things **itself** in `writeExternal`. Listing its
            // fields would say the stream is going to store them by itself, which is exactly what
            // does not happen.
            return NO_FIELDS;
        }
        ObjectStreamField[] declared = serialPersistentFields(cl);
        if (declared == null) {
            declared = fromRealFields(cl);
        }
        sortFields(declared);
        assignOffsets(declared);
        return declared;
    }

    // The explicit list, if the class declares it with the exact type, name and modifiers.
    //
    // All three have to match: `private static final ObjectStreamField[]`. The specification is
    // strict there because a field with that name but public or non-static is another thing --it
    // may be a legitimate instance field-- and taking it for the format declaration would silently
    // change what the class writes.
    private static ObjectStreamField[] serialPersistentFields(Class<?> cl) {
        Field f;
        try {
            f = cl.getDeclaredField("serialPersistentFields");
        } catch (NoSuchFieldException ex) {
            return null;
        }
        int m = f.getModifiers();
        if (!Modifier.isPrivate(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)) {
            return null;
        }
        f.setAccessible(true);
        Object v = f.get(null);
        if (!(v instanceof ObjectStreamField[])) {
            return null;
        }
        ObjectStreamField[] src = (ObjectStreamField[]) v;
        // A copy, for the same reason as in `getFields`: the class kept a reference to the array
        // and could touch it afterwards.
        ObjectStreamField[] out = new ObjectStreamField[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    private static ObjectStreamField[] fromRealFields(Class<?> cl) {
        Field[] fs = cl.getDeclaredFields();
        int howMany = 0;
        int i = 0;
        while (i < fs.length) {
            if (takesPart(fs[i])) {
                howMany = howMany + 1;
            }
            i = i + 1;
        }
        ObjectStreamField[] out = new ObjectStreamField[howMany];
        int k = 0;
        i = 0;
        while (i < fs.length) {
            if (takesPart(fs[i])) {
                out[k] = new ObjectStreamField(fs[i].getName(), fs[i].getType());
                k = k + 1;
            }
            i = i + 1;
        }
        return out;
    }

    // `static` is left out because it belongs to the class and not to the object; `transient`,
    // because it is the author's only way of saying "this is not stored" -- a password in the
    // clear, an open connection, a cache that has to be rebuilt.
    private static boolean takesPart(Field f) {
        int m = f.getModifiers();
        return !Modifier.isStatic(m) && !Modifier.isTransient(m);
    }

    // Insertion sort, not a library `sort`: it is a handful of fields and the comparison is
    // `ObjectStreamField`'s, which has the criterion written already.
    private static void sortFields(ObjectStreamField[] a) {
        int i = 1;
        while (i < a.length) {
            ObjectStreamField x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = x;
            i = i + 1;
        }
    }

    // Two independent counters, and hence the primitives coming first in the order: the primitives'
    // one counts **bytes** inside the data block, the references' one counts **positions** in the
    // object table. They are different units, and if the two groups were interleaved there would be
    // no single number serving both.
    private static void assignOffsets(ObjectStreamField[] a) {
        int bytes = 0;
        int refs = 0;
        int i = 0;
        while (i < a.length) {
            char c = a[i].getTypeCode();
            if (c == 'L' || c == '[') {
                a[i].setOffset(refs);
                refs = refs + 1;
            } else {
                a[i].setOffset(bytes);
                bytes = bytes + widthOf(c);
            }
            i = i + 1;
        }
    }

    private static int widthOf(char c) {
        if (c == 'Z' || c == 'B') {
            return 1;
        }
        if (c == 'C' || c == 'S') {
            return 2;
        }
        if (c == 'J' || c == 'D') {
            return 8;
        }
        return 4;                      // I y F
    }
}
