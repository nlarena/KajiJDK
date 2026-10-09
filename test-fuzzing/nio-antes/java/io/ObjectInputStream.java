package java.io;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * KajiLibrary's java.io.ObjectInputStream -- the reading side of the serialization format.
 *
 * <p>It is {@link ObjectOutputStream}'s exact counterpart: what that one writes, this one builds
 * back up. This class's test is not "it reads what I wrote" --any pair of routines that get it
 * wrong the same way passes that-- but that it reads **the stream the real JDK produces**, byte for
 * byte, and that a stream written here is read by the JDK.
 *
 * <h2>Rebuilding is not constructing</h2>
 *
 * <p>The object comes out of {@link ObjectStreamClass#allocateInstance}, which gives an instance
 * with every field at its default value and **without running any constructor**. It is no shortcut:
 * running the constructor would carry out its effects --validations, counters, registrations in
 * global tables-- for an object that is not being created but read, and it would then overwrite
 * with the stream's values what the constructor had just worked out. The specification says exactly
 * that, and it sounds the reverse of what one would expect.
 *
 * <p>The consequence to keep in mind: **the class cannot defend itself in the constructor**. Any
 * invariant depending on validation in the constructor does not hold for an object that arrived
 * this way; for that there are a `readObject` of one's own, {@link #registerValidation} and {@link
 * ObjectInputFilter}.
 *
 * <h2>The three layers, from the reading side</h2>
 *
 * <ol>
 *   <li><b>The block bytes.</b> Everything a user's {@code writeObject} wrote came out wrapped in
 *       {@code TC_BLOCKDATA} records with their length in front. Here that is unwrapped, and --the
 *       important part-- it can be **skipped**: when the class on this side does not have the
 *       {@code readObject} the other's had, its data is discarded up to the
 *       {@code TC_ENDBLOCKDATA} with no need to understand it. Without that framing, a single extra
 *       field on the other side would misalign the stream for ever.
 *   <li><b>The handles.</b> Every object, string, class and desc coming out of the stream is
 *       numbered in the same order the writer numbered it, and a {@code TC_REFERENCE} returns **the
 *       same object** that was built already. It is what makes a graph with cycles terminate and
 *       what preserves shared identity. The handle is reserved **before** the fields are read,
 *       precisely so that a field pointing at the object containing it finds it numbered already.
 *   <li><b>The descriptors.</b> The class's shape comes in the stream and **rules over this
 *       side's**: the fields are read in the order and with the types the stream states, and only
 *       then is it looked up which local field each value belongs to. A field the stream brings and
 *       the local class no longer has is discarded; one the local class has and the stream does not
 *       bring stays at its default value. That is what allows reading an object written by another
 *       version of the class.
 * </ol>
 *
 * <h2>What this class does not do, and why</h2>
 *
 * <p><b>{@code readResolve} is not consulted.</b> It is the only deviation from the format, and it
 * is the same one --and for the same reason-- as {@code writeReplace} in {@link
 * ObjectOutputStream}: deciding whether the method counts asks for reproducing the specification's
 * accessibility rules (private to the class, or reachable from it by inheritance within the same
 * package) and getting it wrong in either direction returns, silently, an object different from the
 * one the JDK would return. Notice is given here instead of guessing. Since both sides of this
 * library do the same, they go on understanding each other; against the real JDK the difference
 * shows in the classes that use {@code readResolve} to preserve a singleton.
 *
 * <p><b>{@code readObjectNoData} is not called.</b> It only applies when the stream does **not**
 * bring data for a stretch of the hierarchy the local class does have, which is the case of a
 * superclass added after writing. That stretch's fields stay at their default value, which is what
 * would have happened anyway if the method had not been declared.
 *
 * <p><b>A class the stream names and that does not exist on this side cuts the reading short.</b>
 * Its bytes are consumed whole --the stream is not left misaligned in the middle of that object--
 * and then {@link ClassNotFoundException} comes out. The JDK defers that exception to the end of
 * the topmost {@code readObject} so as to be able to return the rest of the graph with that field
 * at {@code null}; here it does not, because carrying the exception around the whole graph and
 * deciding at each field whether it propagates or is left at {@code null} is precisely the kind of
 * detail that gets it wrong silently. What is lost is being able to go on using the stream after
 * the error.
 */
public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {

    /**
     * What is put in the table in place of an object read with {@link #readUnshared}.
     *
     * <p>The handle is reserved all the same --the writer counted it and the numbers have to
     * match-- but it points at this and not at the object. That way a later {@code TC_REFERENCE} to
     * that handle is detected and rejected instead of returning the very object that was asked not
     * to be shared.
     */
    private static final Object UNSHARED_MARKER = new Object();

    private final BlockInput bin;

    /** Numbered in the same order the writer numbered them; see the class note. */
    private Object[] handles = new Object[16];
    private int handleCount;

    /** A stream constructed with the no-argument constructor: there are no bytes underneath. */
    private final boolean delegating;

    private boolean resolveEnabled;
    private ObjectInputFilter filter;
    private boolean filterSet;

    // The state of the stretch being read, for `defaultReadObject` and `readFields`.
    private Object currentObject;
    private ObjectStreamClass currentDesc;
    private GetFieldImpl currentGet;

    /** {@code readObject}'s nesting; the topmost object is at 1. */
    private int depth;

    private ObjectInputValidation[] validations = new ObjectInputValidation[4];
    private int[] priorities = new int[4];
    private int validationCount;

    /**
     * It reads the stream's header and leaves everything ready for the first {@link #readObject}.
     *
     * @throws StreamCorruptedException if the first four bytes are not the format's
     */
    public ObjectInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        this.bin = new BlockInput(in);
        this.delegating = false;
        this.bin.blockMode(false);
        this.readStreamHeader();
        this.bin.blockMode(true);
    }

    /**
     * For a subclass that reimplements deserialization entirely.
     *
     * <p>There is no stream underneath: {@link #readObject} calls {@link #readObjectOverride} and
     * every method that would read bytes fails. It is `protected` because it only makes sense from
     * inside a subclass.
     */
    protected ObjectInputStream() throws IOException, SecurityException {
        this.bin = null;
        this.delegating = true;
    }

    // ---- the header and the subclass hooks -------------------------------------------------------

    /** The four bytes every stream begins with: the magic and the version. */
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        short magic = (short) this.bin.readUnsignedShort0();
        short version = (short) this.bin.readUnsignedShort0();
        if (magic != ObjectStreamConstants.STREAM_MAGIC
                || version != ObjectStreamConstants.STREAM_VERSION) {
            throw new StreamCorruptedException("invalid stream header: "
                    + hex4(magic & 0xFFFF) + hex4(version & 0xFFFF));
        }
    }

    /**
     * It reads a class desc from the stream. {@link ObjectOutputStream#writeClassDescriptor}'s
     * mirror: a subclass that changed the format over there has to change it here too.
     *
     * <p>What it returns is a desc **from the stream**: name, UID, flags and fields as they
     * came, still with no local class. The one that resolves that is {@link #resolveClass}.
     */
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        String name = this.bin.readUtf();
        long uid = this.bin.readLong0();
        int flags = this.bin.readUnsignedByte0();
        int howMany = this.bin.readUnsignedShort0();
        ObjectStreamField[] fields = howMany == 0
                ? ObjectStreamClass.NO_FIELDS
                : new ObjectStreamField[howMany];
        int i = 0;
        while (i < howMany) {
            char kind = (char) this.bin.readUnsignedByte0();
            String nm = this.bin.readUtf();
            String signature;
            if (kind == 'L' || kind == '[') {
                // The type's name is **a stream string with a handle of its own**, not a loose UTF:
                // the writer shares it across every field of the same signature, and reading it as
                // a raw UTF would skip a handle and shift every number.
                signature = this.readTypeString();
            } else {
                signature = String.valueOf(kind);
            }
            if (signature == null) {
                throw new StreamCorruptedException("null field type string");
            }
            fields[i] = new ObjectStreamField(nm, signature);
            i = i + 1;
        }
        return new ObjectStreamClass(name, uid, flags, fields);
    }

    /**
     * The local class corresponding to a stream desc.
     *
     * <p>Here by name and nothing else. The JDK looks it up with the class loader of the nearest
     * caller on the stack --which lets it resolve a class only that loader sees-- and this VM has a
     * single loader, so the lookup by name is the same answer and not an approximation.
     */
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        Class<?> prim = primitiveByName(name);
        if (prim != null) {
            return prim;
        }
        return Class.forName(name);
    }

    /**
     * The proxy class implementing `interfaces`.
     *
     * @throws ClassNotFoundException if some of the interfaces do not exist on this side
     */
    protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {
        Class<?>[] cls = new Class<?>[interfaces.length];
        int i = 0;
        while (i < interfaces.length) {
            cls[i] = Class.forName(interfaces[i]);
            i = i + 1;
        }
        return Proxy.getProxyClass(ObjectInputStream.class.getClassLoader(), cls);
    }

    /**
     * Where {@link #readObject} goes when the stream was constructed with the no-argument
     * constructor. It returns `null` here, as in the JDK: the subclass using that constructor is
     * the one that has to read.
     */
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return null;
    }

    /**
     * The last filter before returning: if {@link #enableResolveObject} is on, every object goes
     * through here and what this method says is what is returned. Identity here, as in the JDK.
     */
    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * It turns {@link #resolveObject}'s filter on or off, and returns how it was.
     *
     * <p>The JDK asks the security manager for permission, because swapping objects in mid-flight
     * is a way for the stream to say one thing and hand over another. Here there is no manager, so
     * the flag is taken as it stands.
     */
    protected boolean enableResolveObject(boolean enable) {
        boolean before = this.resolveEnabled;
        this.resolveEnabled = enable;
        return before;
    }

    // ---- reading objects ------------------------------------------------------------------------

    /**
     * It reads the next object and everything hanging off it.
     *
     * <p>It is `final` as in the JDK: a subclass wanting to change what gets read has {@link
     * #readObjectOverride} and {@link #resolveObject}, and allowing the entry point to be
     * overridden would permit reading an object while skipping the handles, which is how a graph
     * with cycles gets broken.
     *
     * @throws ClassNotFoundException if the stream names a class that does not exist on this side
     * @throws OptionalDataException if what follows is primitive data and not an object
     */
    public final Object readObject() throws IOException, ClassNotFoundException {
        if (this.delegating) {
            return this.readObjectOverride();
        }
        return this.readTopLevel(false);
    }

    /**
     * It reads the next object **unshared**: it is not tied to a reusable handle, so a later
     * reference to it is rejected instead of returning it.
     *
     * <p>It is {@link ObjectOutputStream#writeUnshared}'s counterpart, and it serves the same
     * purpose: a field that has to be private to the object containing it.
     *
     * @throws InvalidObjectException if what is in the stream is a reference to something already
     *     read
     */
    public Object readUnshared() throws IOException, ClassNotFoundException {
        if (this.delegating) {
            return this.readObjectOverride();
        }
        return this.readTopLevel(true);
    }

    /**
     * A topmost {@code readObject}: it reads and, if nobody else was reading, runs whatever
     * validations were registered.
     */
    private Object readTopLevel(boolean unshared) throws IOException, ClassNotFoundException {
        boolean atRoot = this.depth == 0;
        Object obj = this.readObject0(unshared);
        if (atRoot) {
            this.runValidations();
        }
        return obj;
    }

    private Object readObject0(boolean unshared) throws IOException, ClassNotFoundException {
        boolean blockBefore = this.bin.inBlockMode();
        if (blockBefore) {
            // Inside a user's `readObject`, what is left of the block record in progress is
            // primitive data the caller has not read yet: asking it for an object there is no error
            // of the stream's but a mismatch between what was written and what is being read, and
            // that is why it comes out as an `OptionalDataException` with the length, which is the
            // only way for the caller to be able to carry on.
            int left = this.bin.refill();
            if (left > 0) {
                throw new OptionalDataException(left);
            }
            this.bin.blockMode(false);
        }
        this.depth = this.depth + 1;
        try {
            int tc = this.bin.peekRequired();
            while (tc == ObjectStreamConstants.TC_RESET) {
                if (this.depth > 1) {
                    throw new StreamCorruptedException("unexpected reset");
                }
                this.bin.readRawByte();
                this.clearHandles();
                tc = this.bin.peekRequired();
            }
            if (tc == ObjectStreamConstants.TC_NULL) {
                this.bin.readRawByte();
                return null;
            }
            if (tc == ObjectStreamConstants.TC_REFERENCE) {
                return this.replaceOnRead(this.readHandleRef(unshared));
            }
            if (tc == ObjectStreamConstants.TC_CLASS) {
                return this.replaceOnRead(this.readClass(unshared));
            }
            if (tc == ObjectStreamConstants.TC_CLASSDESC
                    || tc == ObjectStreamConstants.TC_PROXYCLASSDESC) {
                return this.replaceOnRead(this.readDesc(unshared));
            }
            if (tc == ObjectStreamConstants.TC_STRING
                    || tc == ObjectStreamConstants.TC_LONGSTRING) {
                return this.replaceOnRead(this.readString(unshared));
            }
            if (tc == ObjectStreamConstants.TC_ARRAY) {
                this.bin.readRawByte();
                return this.replaceOnRead(this.readArray(unshared));
            }
            if (tc == ObjectStreamConstants.TC_ENUM) {
                this.bin.readRawByte();
                return this.replaceOnRead(this.readEnum(unshared));
            }
            if (tc == ObjectStreamConstants.TC_OBJECT) {
                this.bin.readRawByte();
                return this.replaceOnRead(this.readOrdinaryObject(unshared));
            }
            if (tc == ObjectStreamConstants.TC_EXCEPTION) {
                this.bin.readRawByte();
                // The writer fell over in the middle of the graph and wrote the exception in place
                // of the rest. It is read with a clean table --the JDK does the same-- because the
                // handles of the aborted graph are no use to whoever reads it.
                this.clearHandles();
                Object cause = this.readObject0(false);
                this.clearHandles();
                throw new WriteAbortedException("writing aborted", (Exception) cause);
            }
            if (tc == ObjectStreamConstants.TC_BLOCKDATA
                    || tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                this.bin.blockMode(true);
                throw new OptionalDataException(this.bin.refill());
            }
            if (tc == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                throw new OptionalDataException(true);
            }
            throw new StreamCorruptedException("invalid type code: " + hex2(tc));
        } finally {
            this.depth = this.depth - 1;
            if (blockBefore) {
                this.bin.blockMode(true);
            }
        }
    }

    /** {@link #resolveObject}'s hook, if it is on. */
    private Object replaceOnRead(Object obj) throws IOException {
        if (this.resolveEnabled && obj != null) {
            return this.resolveObject(obj);
        }
        return obj;
    }

    private Object readHandleRef(boolean unshared) throws IOException {
        this.bin.readRawByte();
        int m = this.bin.readInt0() - ObjectStreamConstants.baseWireHandle;
        if (m < 0 || m >= this.handleCount) {
            throw new StreamCorruptedException("invalid handle value: " + hex4(m));
        }
        if (unshared) {
            throw new InvalidObjectException("cannot read back reference as unshared");
        }
        Object o = this.handles[m];
        if (o == UNSHARED_MARKER) {
            throw new InvalidObjectException("cannot read back reference to unshared object");
        }
        return o;
    }

    private String readString(boolean unshared) throws IOException {
        int tc = this.bin.readRawByte();
        String s;
        if (tc == ObjectStreamConstants.TC_LONGSTRING) {
            long len = this.bin.readLong0();
            if (len < 0 || len > Integer.MAX_VALUE) {
                throw new StreamCorruptedException("long string length out of range: " + len);
            }
            s = this.bin.readRawUtf(len);
        } else {
            s = this.bin.readUtf();
        }
        this.assignHandle(unshared ? UNSHARED_MARKER : s);
        return s;
    }

    /**
     * A reference field's type inside a desc.
     *
     * <p>Package-private and not public, just as in the JDK: it is a stream string with a handle of
     * its own, and reading it from outside would shift the numbering.
     */
    String readTypeString() throws IOException {
        int tc = this.bin.peekRequired();
        if (tc == ObjectStreamConstants.TC_NULL) {
            this.bin.readRawByte();
            return null;
        }
        if (tc == ObjectStreamConstants.TC_REFERENCE) {
            return (String) this.readHandleRef(false);
        }
        if (tc == ObjectStreamConstants.TC_STRING || tc == ObjectStreamConstants.TC_LONGSTRING) {
            return this.readString(false);
        }
        throw new StreamCorruptedException("invalid type code: " + hex2(tc));
    }

    private Class<?> readClass(boolean unshared) throws IOException, ClassNotFoundException {
        this.bin.readRawByte();
        ObjectStreamClass desc = this.readDesc(false);
        Class<?> cl = desc == null ? null : desc.forClass();
        this.assignHandle(unshared ? UNSHARED_MARKER : cl);
        if (cl == null) {
            throw new ClassNotFoundException(desc == null ? "null class desc" : desc.getName());
        }
        return cl;
    }

    // ---- descriptors ----------------------------------------------------------------------------

    private ObjectStreamClass readDesc(boolean unshared) throws IOException, ClassNotFoundException {
        int tc = this.bin.peekRequired();
        if (tc == ObjectStreamConstants.TC_NULL) {
            this.bin.readRawByte();
            return null;
        }
        if (tc == ObjectStreamConstants.TC_REFERENCE) {
            Object o = this.readHandleRef(unshared);
            if (!(o instanceof ObjectStreamClass)) {
                throw new StreamCorruptedException("handle does not refer to a class desc");
            }
            return (ObjectStreamClass) o;
        }
        if (tc == ObjectStreamConstants.TC_PROXYCLASSDESC) {
            return this.readProxyDesc(unshared);
        }
        if (tc == ObjectStreamConstants.TC_CLASSDESC) {
            return this.readNonProxyDesc(unshared);
        }
        throw new StreamCorruptedException("invalid type code: " + hex2(tc));
    }

    private ObjectStreamClass readNonProxyDesc(boolean unshared)
            throws IOException, ClassNotFoundException {
        this.bin.readRawByte();
        // The handle is reserved **before** the body is read, and with that it lands on the same
        // number the writer gave it: it writes the name and the UID before numbering, and in those
        // two there is nothing that consumes handles. A field whose type is the class itself
        // references it by this number, so reserving it afterwards would break the first recursive
        // class that turned up.
        int m = this.assignHandle(null);
        ObjectStreamClass desc = this.readClassDescriptor();
        if (!unshared) {
            this.handles[m] = desc;
        } else {
            this.handles[m] = UNSHARED_MARKER;
        }

        // The annotation block is read in block mode, so that an overridden `resolveClass` can read
        // what its `annotateClass` wrote.
        this.bin.blockMode(true);
        Class<?> cl = null;
        try {
            cl = this.resolveClass(desc);
        } catch (ClassNotFoundException missing) {
            cl = null;
        }
        this.skipCustomData();
        desc.resolvedTo(cl);
        desc.streamSuper(this.readDesc(false));
        return desc;
    }

    private ObjectStreamClass readProxyDesc(boolean unshared)
            throws IOException, ClassNotFoundException {
        this.bin.readRawByte();
        int m = this.assignHandle(null);
        int howMany = this.bin.readInt0();
        if (howMany < 0) {
            throw new StreamCorruptedException("invalid interface count: " + howMany);
        }
        String[] names = new String[howMany];
        int i = 0;
        while (i < howMany) {
            names[i] = this.bin.readUtf();
            i = i + 1;
        }
        this.bin.blockMode(true);
        Class<?> cl = null;
        try {
            cl = this.resolveProxyClass(names);
        } catch (ClassNotFoundException missing) {
            cl = null;
        }
        this.skipCustomData();
        // A proxy contributes no fields of its own and carries no UID: its serialized form is its
        // handler's, which travels as a field of the superclass `java.lang.reflect.Proxy`.
        ObjectStreamClass desc = new ObjectStreamClass(
                cl == null ? "" : cl.getName(), 0L,
                ObjectStreamConstants.SC_SERIALIZABLE, ObjectStreamClass.NO_FIELDS);
        desc.resolvedTo(cl);
        this.handles[m] = unshared ? UNSHARED_MARKER : desc;
        desc.streamSuper(this.readDesc(false));
        return desc;
    }

    /**
     * It consumes whatever a user's {@code annotateClass} or {@code writeObject} wrote in excess,
     * up to the {@code TC_ENDBLOCKDATA} that closes it.
     *
     * <p>It is the only thing that lets an old version of the code read a stream written by a new
     * one: the data that is not understood is thrown away without having to interpret it. The
     * objects that turn up loose in there **do** have to be read, not skipped: each one consumes
     * handles, and discarding them by length would shift the numbering of everything that comes
     * afterwards.
     */
    private void skipCustomData() throws IOException, ClassNotFoundException {
        for (;;) {
            if (this.bin.inBlockMode()) {
                this.bin.skipBlock();
                this.bin.blockMode(false);
            }
            int tc = this.bin.peekRequired();
            if (tc == ObjectStreamConstants.TC_BLOCKDATA
                    || tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                this.bin.blockMode(true);
            } else if (tc == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                this.bin.readRawByte();
                return;
            } else {
                this.readObject0(false);
            }
        }
    }

    // ---- objects, arrays and enums ---------------------------------------------------------------

    private Object readArray(boolean unshared) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.readDesc(false);
        int len = this.bin.readInt0();
        if (len < 0) {
            throw new StreamCorruptedException("invalid array length: " + len);
        }
        Class<?> cl = desc == null ? null : desc.forClass();
        Class<?> comp = cl == null ? null : cl.getComponentType();
        this.checkFilter(cl, len);

        Object arr = comp == null ? null : Array.newInstance(comp, len);
        int m = this.assignHandle(unshared ? UNSHARED_MARKER : arr);
        if (comp != null && comp.isPrimitive()) {
            this.readPrimitives(arr, comp, len);
        } else {
            // `(Object[])` and not `Array.set`: every array of references **is** an `Object[]`, and
            // the store checks the element's type all the same (`ArrayStoreException`). It is the
            // same route the writer uses, and it does not depend on a reflection native.
            Object[] a = (Object[]) arr;
            int i = 0;
            while (i < len) {
                Object v = this.readObject0(false);
                if (a != null) {
                    a[i] = v;
                }
                i = i + 1;
            }
        }
        if (!unshared) {
            this.handles[m] = arr;
        }
        if (arr == null) {
            throw new ClassNotFoundException(desc == null ? "null array desc" : desc.getName());
        }
        return arr;
    }

    private void readPrimitives(Object arr, Class<?> comp, int len) throws IOException {
        int i = 0;
        if (comp == Byte.TYPE) {
            byte[] a = (byte[]) arr;
            while (i < len) {
                a[i] = (byte) this.bin.readUnsignedByte0();
                i = i + 1;
            }
        } else if (comp == Boolean.TYPE) {
            boolean[] a = (boolean[]) arr;
            while (i < len) {
                a[i] = this.bin.readUnsignedByte0() != 0;
                i = i + 1;
            }
        } else if (comp == Character.TYPE) {
            char[] a = (char[]) arr;
            while (i < len) {
                a[i] = (char) this.bin.readUnsignedShort0();
                i = i + 1;
            }
        } else if (comp == Short.TYPE) {
            short[] a = (short[]) arr;
            while (i < len) {
                a[i] = (short) this.bin.readUnsignedShort0();
                i = i + 1;
            }
        } else if (comp == Integer.TYPE) {
            int[] a = (int[]) arr;
            while (i < len) {
                a[i] = this.bin.readInt0();
                i = i + 1;
            }
        } else if (comp == Long.TYPE) {
            long[] a = (long[]) arr;
            while (i < len) {
                a[i] = this.bin.readLong0();
                i = i + 1;
            }
        } else if (comp == Float.TYPE) {
            float[] a = (float[]) arr;
            while (i < len) {
                a[i] = Float.intBitsToFloat(this.bin.readInt0());
                i = i + 1;
            }
        } else {
            double[] a = (double[]) arr;
            while (i < len) {
                a[i] = Double.longBitsToDouble(this.bin.readLong0());
                i = i + 1;
            }
        }
    }

    private Object readEnum(boolean unshared) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.readDesc(false);
        Class<?> cl = desc == null ? null : desc.forClass();
        this.checkFilter(cl, -1);
        int m = this.assignHandle(unshared ? UNSHARED_MARKER : null);
        String name = this.readString(false);
        Object value = null;
        if (cl != null) {
            // By name and not by ordinal: reordering an enum's constants is a change the source
            // permits and that does not touch the serialized form, and reading by position would
            // return another constant silently.
            value = enumConstant(cl, name);
            if (value == null) {
                throw new InvalidObjectException("enum constant " + name + " does not exist in "
                        + cl.getName());
            }
        }
        if (!unshared) {
            this.handles[m] = value;
        }
        if (cl == null) {
            throw new ClassNotFoundException(desc == null ? "null enum desc" : desc.getName());
        }
        return value;
    }

    private static Object enumConstant(Class<?> cl, String name) {
        Object[] cs = cl.getEnumConstants();
        if (cs == null) {
            return null;
        }
        int i = 0;
        while (i < cs.length) {
            if (((Enum<?>) cs[i]).name().equals(name)) {
                return cs[i];
            }
            i = i + 1;
        }
        return null;
    }

    private Object readOrdinaryObject(boolean unshared) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.readDesc(false);
        if (desc == null) {
            throw new StreamCorruptedException("null class desc for object");
        }
        Class<?> cl = desc.forClass();
        this.checkFilter(cl, -1);

        Object obj = null;
        if (cl != null) {
            obj = ObjectStreamClass.allocateInstance(cl);
            if (obj == null) {
                // An interface, an abstract class or something this VM cannot instantiate. It
                // cannot carry on: the data could be consumed, but there would be nowhere to put it
                // and returning `null` would pass off an unreadable stream as one that carried a
                // `null`.
                throw new InvalidClassException(desc.getName(), "unable to create instance");
            }
        }
        int m = this.assignHandle(unshared ? UNSHARED_MARKER : obj);
        if ((desc.streamFlags() & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
            this.readExternalData(obj, desc);
        } else {
            this.readSerialData(obj, desc);
        }
        if (!unshared) {
            this.handles[m] = obj;
        }
        if (cl == null) {
            throw new ClassNotFoundException(desc.getName());
        }
        return obj;
    }

    private void readExternalData(Object obj, ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        // `SC_BLOCK_DATA` is what tells protocol 2 from 1: with it, what `writeExternal` wrote
        // comes framed and can be skipped; without it, it comes raw, and if there is nothing on
        // this side to read it with there is no choice but to give up.
        boolean framed = (desc.streamFlags() & ObjectStreamConstants.SC_BLOCK_DATA) != 0;
        Externalizable ext = obj instanceof Externalizable ? (Externalizable) obj : null;
        if (ext == null && !framed) {
            throw new StreamCorruptedException(
                    "unreadable external data for " + desc.getName() + " (protocol 1)");
        }
        Object objBefore = this.currentObject;
        ObjectStreamClass descBefore = this.currentDesc;
        GetFieldImpl getBefore = this.currentGet;
        this.currentObject = obj;
        this.currentDesc = null;
        this.currentGet = null;
        try {
            if (framed) {
                this.bin.blockMode(true);
            }
            if (ext != null) {
                ext.readExternal(this);
            }
        } finally {
            this.currentObject = objBefore;
            this.currentDesc = descBefore;
            this.currentGet = getBefore;
        }
        if (framed) {
            this.skipCustomData();
        }
    }

    /**
     * An ordinary object's data, **from the highest serializable superclass downwards**, which is
     * the direction the writer put it in.
     */
    private void readSerialData(Object obj, ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        ObjectStreamClass[] slots = streamChain(desc);
        int i = 0;
        while (i < slots.length) {
            ObjectStreamClass slot = slots[i];
            Class<?> cl = slot.forClass();
            boolean hasWriteMethod = (slot.streamFlags() & ObjectStreamConstants.SC_WRITE_METHOD) != 0;
            Method reader = cl == null ? null : readObjectMethod(cl);
            if (obj != null && reader != null) {
                Object objBefore = this.currentObject;
                ObjectStreamClass descBefore = this.currentDesc;
                GetFieldImpl getBefore = this.currentGet;
                this.currentObject = obj;
                this.currentDesc = slot;
                this.currentGet = null;
                try {
                    if (hasWriteMethod) {
                        this.bin.blockMode(true);
                    }
                    reader.invoke(obj, new Object[] { this });
                } finally {
                    this.currentObject = objBefore;
                    this.currentDesc = descBefore;
                    this.currentGet = getBefore;
                }
            } else {
                // With no local method: the default fields have to be consumed all the same,
                // whether or not this VM has anywhere to put them. If the writer used a
                // `writeObject` of its own, the fields are there only if it called
                // `defaultWriteObject`; the rest is cleaned up by the skip below, which is exactly
                // what the block framing exists for.
                this.readDefaultFieldValues(obj, cl, slot);
            }
            if (hasWriteMethod) {
                this.skipCustomData();
            }
            i = i + 1;
        }
    }

    /** The stream's chain of descriptors, reversed: `[0]` is the highest superclass. */
    private static ObjectStreamClass[] streamChain(ObjectStreamClass desc) {
        int howMany = 0;
        ObjectStreamClass d = desc;
        while (d != null) {
            howMany = howMany + 1;
            d = d.streamSuper();
        }
        ObjectStreamClass[] out = new ObjectStreamClass[howMany];
        d = desc;
        int i = howMany - 1;
        while (i >= 0) {
            out[i] = d;
            d = d.streamSuper();
            i = i - 1;
        }
        return out;
    }

    /**
     * It reads a stretch's fields as the stream describes them and leaves them in `obj`.
     *
     * <p>The order and the types are dictated by **the stream**, not by the local class: first
     * every primitive packed together, then the references. Only with the value already read is it
     * looked up which local field it belongs to, and if there is none it is discarded. Doing it the
     * other way round --walking the local fields and looking for the value-- would misalign the
     * stream as soon as the other version had one extra field.
     *
     * <p>`obj` may be `null`: it is how the stretch of a class that does not exist on this side is
     * consumed without losing the alignment.
     */
    private void readDefaultFieldValues(Object obj, Class<?> cl, ObjectStreamClass slot)
            throws IOException, ClassNotFoundException {
        ObjectStreamField[] fields = slot.getFields();
        // The **local** serialized form is what decides which field may be written to: a
        // `transient` or `static` field on this side does not take part, and a class with
        // `serialPersistentFields` decides for itself which are its own. Going straight to
        // `getDeclaredField` would skip both rules and write into fields the class had taken out of
        // the format on purpose.
        ObjectStreamClass local = (obj == null || cl == null) ? null : ObjectStreamClass.lookup(cl);
        int i = 0;
        while (i < fields.length && fields[i].isPrimitive()) {
            char t = fields[i].getTypeCode();
            Field f = localField(local, cl, fields[i]);
            // The typed `setXxx` and not `set` with a wrapper: they are the exact mirror of the
            // `getXxx` the writer used, and they do not depend on `set`'s unwrapping getting the
            // conversion right.
            if (t == 'B') {
                byte v = (byte) this.bin.readUnsignedByte0();
                if (f != null) {
                    f.setByte(obj, v);
                }
            } else if (t == 'Z') {
                boolean v = this.bin.readUnsignedByte0() != 0;
                if (f != null) {
                    f.setBoolean(obj, v);
                }
            } else if (t == 'C') {
                char v = (char) this.bin.readUnsignedShort0();
                if (f != null) {
                    f.setChar(obj, v);
                }
            } else if (t == 'S') {
                short v = (short) this.bin.readUnsignedShort0();
                if (f != null) {
                    f.setShort(obj, v);
                }
            } else if (t == 'I') {
                int v = this.bin.readInt0();
                if (f != null) {
                    f.setInt(obj, v);
                }
            } else if (t == 'J') {
                long v = this.bin.readLong0();
                if (f != null) {
                    f.setLong(obj, v);
                }
            } else if (t == 'F') {
                float v = Float.intBitsToFloat(this.bin.readInt0());
                if (f != null) {
                    f.setFloat(obj, v);
                }
            } else {
                double v = Double.longBitsToDouble(this.bin.readLong0());
                if (f != null) {
                    f.setDouble(obj, v);
                }
            }
            i = i + 1;
        }
        while (i < fields.length) {
            Object v = this.readObject0(fields[i].isUnshared());
            Field f = localField(local, cl, fields[i]);
            if (f != null && (v == null || f.getType().isInstance(v))) {
                f.set(obj, v);
            }
            i = i + 1;
        }
    }

    /**
     * The local field corresponding to a stream field, or `null` if there is none that will do.
     *
     * <p>**The name and the signature** have to match: a field with the same name and another type
     * is another field, and putting the stream's value into it would write rubbish where whoever
     * compiled expected their own. Returning `null` is no error: it is how a field the writing
     * version had and this one no longer does gets discarded.
     */
    private static Field localField(ObjectStreamClass local, Class<?> cl, ObjectStreamField fld) {
        if (local == null) {
            return null;
        }
        ObjectStreamField lf = local.getField(fld.getName());
        if (lf == null) {
            return null;
        }
        String localSignature = lf.isPrimitive()
                ? String.valueOf(lf.getTypeCode()) : lf.getTypeString();
        String streamSignature = fld.isPrimitive()
                ? String.valueOf(fld.getTypeCode()) : fld.getTypeString();
        if (!localSignature.equals(streamSignature)) {
            return null;
        }
        return ObjectOutputStream.realField(cl, lf.getName(), lf.getType());
    }

    /**
     * The {@code private void readObject(ObjectInputStream)} declared by `cl`, or `null`.
     *
     * <p>It has to be **private and declared by that exact class**, for the same reason as its
     * writing counterpart: it is not an override but a hook keyed on a position in the hierarchy,
     * and an inherited one would run twice reading the same data.
     */
    static Method readObjectMethod(Class<?> cl) {
        return ObjectOutputStream.serialMethod(cl, "readObject", ObjectInputStream.class, void.class);
    }

    // ---- what a user's readObject may call --------------------------------------------------------

    /**
     * It reads the default fields of the stretch being deserialized.
     *
     * <p>It exists so that a `readObject` of one's own can do "the usual and this as well": calling
     * it first and then reading one's own things is the normal pattern of a class that added data
     * without changing its fields' shape.
     *
     * @throws NotActiveException if no object is being read
     */
    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (this.currentObject == null || this.currentDesc == null) {
            throw new NotActiveException("not in call to readObject");
        }
        boolean blockBefore = this.bin.blockMode(false);
        try {
            this.readDefaultFieldValues(this.currentObject, this.currentDesc.forClass(), this.currentDesc);
        } finally {
            this.bin.blockMode(blockBefore);
        }
    }

    /**
     * The current stretch's fields, by name, so as to read them without depending on the local
     * class still declaring each one.
     *
     * <p>It is {@link ObjectOutputStream#putFields}'s counterpart, and the way out for a class that
     * changed its fields: {@link GetField#get(String, int)} and its siblings return the default
     * value when the stream did not bring that field, so a new version can read an old stream
     * without guessing what it carried.
     *
     * @throws NotActiveException if no object is being read
     */
    public ObjectInputStream.GetField readFields() throws IOException, ClassNotFoundException {
        if (this.currentObject == null || this.currentDesc == null) {
            throw new NotActiveException("not in call to readObject");
        }
        if (this.currentGet == null) {
            GetFieldImpl g = new GetFieldImpl(this.currentDesc);
            boolean blockBefore = this.bin.blockMode(false);
            try {
                g.read0(this);
            } finally {
                this.bin.blockMode(blockBefore);
            }
            this.currentGet = g;
        }
        return this.currentGet;
    }

    /**
     * It asks for {@code obj.validateObject()} to be called once the whole graph is built.
     *
     * <p>It serves for the invariants that span several objects: inside a {@code readObject} the
     * object's references may point at instances whose fields have not been filled in yet, so
     * checking them there gives false negatives. The validations run from higher to lower priority.
     *
     * @throws NotActiveException if no object is being read
     * @throws InvalidObjectException if `obj` is `null`
     */
    public void registerValidation(ObjectInputValidation obj, int prio)
            throws NotActiveException, InvalidObjectException {
        if (this.depth == 0) {
            throw new NotActiveException("stream inactive");
        }
        if (obj == null) {
            throw new InvalidObjectException("null callback");
        }
        if (this.validationCount == this.validations.length) {
            ObjectInputValidation[] v2 = new ObjectInputValidation[this.validationCount * 2];
            int[] p2 = new int[this.validationCount * 2];
            System.arraycopy(this.validations, 0, v2, 0, this.validationCount);
            System.arraycopy(this.priorities, 0, p2, 0, this.validationCount);
            this.validations = v2;
            this.priorities = p2;
        }
        this.validations[this.validationCount] = obj;
        this.priorities[this.validationCount] = prio;
        this.validationCount = this.validationCount + 1;
    }

    private void runValidations() throws InvalidObjectException {
        int howMany = this.validationCount;
        if (howMany == 0) {
            return;
        }
        ObjectInputValidation[] vs = this.validations;
        int[] ps = this.priorities;
        // The table is emptied **before** anything runs: a validation that failed would otherwise
        // leave its own registered for this same stream's next `readObject`.
        this.validations = new ObjectInputValidation[4];
        this.priorities = new int[4];
        this.validationCount = 0;
        // From higher to lower priority, and stable among equals (insertion sort over few elements:
        // they are the ones one read registered, not a collection).
        int i = 1;
        while (i < howMany) {
            ObjectInputValidation v = vs[i];
            int p = ps[i];
            int j = i - 1;
            while (j >= 0 && ps[j] < p) {
                vs[j + 1] = vs[j];
                ps[j + 1] = ps[j];
                j = j - 1;
            }
            vs[j + 1] = v;
            ps[j + 1] = p;
            i = i + 1;
        }
        i = 0;
        while (i < howMany) {
            vs[i].validateObject();
            i = i + 1;
        }
    }

    // ---- the filter -----------------------------------------------------------------------------

    /** The filter installed on this stream, or `null` if there is none. */
    public final ObjectInputFilter getObjectInputFilter() {
        return this.filter;
    }

    /**
     * It installs the filter that decides which classes this stream may rebuild.
     *
     * <p>It can be set **once only and before anything is read**: a filter that could be changed in
     * the middle of a graph would not be a policy but a suggestion, because it would be enough for
     * the stream itself to lead to the change being carried out in order to switch it off.
     *
     * @throws IllegalStateException if one was set already, or if some object has already been read
     */
    public final void setObjectInputFilter(ObjectInputFilter filter) {
        if (this.filterSet) {
            throw new IllegalStateException("filter can not be set more than once");
        }
        if (this.handleCount != 0) {
            throw new IllegalStateException("filter can not be set after an object has been read");
        }
        this.filter = filter;
        this.filterSet = true;
    }

    /**
     * It asks the filter about the class that is about to be built.
     *
     * @throws InvalidClassException if the filter rejects it
     */
    private void checkFilter(Class<?> cl, long arrayLength) throws InvalidClassException {
        if (this.filter == null) {
            return;
        }
        ObjectInputFilter.Status s = this.filter.checkInput(
                new FilterInfoImpl(cl, arrayLength, this.depth,
                        this.handleCount, this.bin.bytesRead()));
        if (s == null || s == ObjectInputFilter.Status.REJECTED) {
            throw new InvalidClassException("filter status: " + s);
        }
    }

    private static final class FilterInfoImpl implements ObjectInputFilter.FilterInfo {
        private final Class<?> cls;
        private final long len;
        private final long depth;
        private final long references;
        private final long bytes;

        FilterInfoImpl(Class<?> cls, long len, long depth, long references, long bytes) {
            this.cls = cls;
            this.len = len;
            this.depth = depth;
            this.references = references;
            this.bytes = bytes;
        }

        public Class<?> serialClass() {
            return this.cls;
        }

        public long arrayLength() {
            return this.len;
        }

        public long depth() {
            return this.depth;
        }

        public long references() {
            return this.references;
        }

        public long streamBytes() {
            return this.bytes;
        }
    }

    // ---- the handles ----------------------------------------------------------------------------

    private int assignHandle(Object o) {
        if (this.handleCount == this.handles.length) {
            Object[] n = new Object[this.handleCount * 2];
            System.arraycopy(this.handles, 0, n, 0, this.handleCount);
            this.handles = n;
        }
        this.handles[this.handleCount] = o;
        this.handleCount = this.handleCount + 1;
        return this.handleCount - 1;
    }

    private void clearHandles() {
        int i = 0;
        while (i < this.handleCount) {
            this.handles[i] = null;
            i = i + 1;
        }
        this.handleCount = 0;
    }

    // ---- DataInput y InputStream ------------------------------------------------------------------

    public int read() throws IOException {
        return this.bin.read0();
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        return this.bin.read0(buf, off, len);
    }

    public int available() throws IOException {
        return this.bin.available();
    }

    public void close() throws IOException {
        this.bin.close();
    }

    public boolean readBoolean() throws IOException {
        return this.bin.readUnsignedByte0() != 0;
    }

    public byte readByte() throws IOException {
        return (byte) this.bin.readUnsignedByte0();
    }

    public int readUnsignedByte() throws IOException {
        return this.bin.readUnsignedByte0();
    }

    public char readChar() throws IOException {
        return (char) this.bin.readUnsignedShort0();
    }

    public short readShort() throws IOException {
        return (short) this.bin.readUnsignedShort0();
    }

    public int readUnsignedShort() throws IOException {
        return this.bin.readUnsignedShort0();
    }

    public int readInt() throws IOException {
        return this.bin.readInt0();
    }

    public long readLong() throws IOException {
        return this.bin.readLong0();
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(this.bin.readInt0());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(this.bin.readLong0());
    }

    public void readFully(byte[] buf) throws IOException {
        this.readFully(buf, 0, buf.length);
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        int i = 0;
        while (i < len) {
            buf[off + i] = (byte) this.bin.readUnsignedByte0();
            i = i + 1;
        }
    }

    public int skipBytes(int len) throws IOException {
        int i = 0;
        while (i < len) {
            if (this.bin.read0() < 0) {
                return i;
            }
            i = i + 1;
        }
        return i;
    }

    /**
     * A line of bytes, each widened to a `char`.
     *
     * @deprecated It does not convert from bytes to characters: each byte becomes the `char` of its
     *     value, which only coincides with the text for Latin-1. For reading text there is a
     *     {@link BufferedReader} over an {@link InputStreamReader} with the character set stated.
     */
    @Deprecated
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = this.bin.read0();
        if (c < 0) {
            return null;
        }
        while (c >= 0 && c != '\n') {
            if (c == '\r') {
                // The `\r\n` is consumed whole, but a lone `\r` also ends the line: looking at the
                // next byte without consuming it is the only way of telling them apart without
                // eating the first character of the line that follows.
                if (this.bin.peek() == '\n') {
                    this.bin.read0();
                }
                return sb.toString();
            }
            sb.append((char) c);
            c = this.bin.read0();
        }
        return sb.toString();
    }

    public String readUTF() throws IOException {
        return this.bin.readUtf();
    }

    // ---- GetField --------------------------------------------------------------------------------

    /**
     * The current stretch's fields, read by name.
     *
     * <p>Every `get` takes a default value, and that is the point: the stream may have been written
     * by a version of the class that did not have that field, and what is returned then is the
     * value the caller chose. {@link #defaulted} tells the two situations apart when it matters.
     */
    public abstract static class GetField {

        /** The stretch's desc, as it came from the stream. */
        public abstract ObjectStreamClass getObjectStreamClass();

        /**
         * Whether `name` did **not** come in the stream and therefore its `get` would return the
         * default value.
         *
         * @throws IllegalArgumentException if `name` is a field neither of this stretch nor of the
         *     class
         */
        public abstract boolean defaulted(String name) throws IOException;

        public abstract boolean get(String name, boolean val) throws IOException;

        public abstract byte get(String name, byte val) throws IOException;

        public abstract char get(String name, char val) throws IOException;

        public abstract short get(String name, short val) throws IOException;

        public abstract int get(String name, int val) throws IOException;

        public abstract long get(String name, long val) throws IOException;

        public abstract float get(String name, float val) throws IOException;

        public abstract double get(String name, double val) throws IOException;

        public abstract Object get(String name, Object val) throws IOException, ClassNotFoundException;
    }

    /**
     * The stretch's values, already read from the stream and stored until somebody asks for them.
     *
     * <p>They are all read at once and not one by one on demand because the stream is sequential:
     * leaving the reading until `get` is called would tie the order of the `get`s to the format's
     * order, and whoever asked for two fields the other way round would read one instead of the
     * other.
     */
    private static final class GetFieldImpl extends ObjectInputStream.GetField {
        private final ObjectStreamClass desc;
        private final ObjectStreamField[] fields;
        private final byte[] primitives;
        private final Object[] references;

        GetFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
            this.fields = desc.getFields();
            int bytes = 0;
            int refs = 0;
            int i = 0;
            while (i < this.fields.length) {
                if (this.fields[i].isPrimitive()) {
                    bytes = bytes + widthOf(this.fields[i].getTypeCode());
                } else {
                    refs = refs + 1;
                }
                i = i + 1;
            }
            this.primitives = new byte[bytes];
            this.references = new Object[refs];
        }

        void read0(ObjectInputStream in) throws IOException, ClassNotFoundException {
            int i = 0;
            while (i < this.fields.length && this.fields[i].isPrimitive()) {
                in.bin.readRaw(this.primitives, this.fields[i].getOffset(),
                        widthOf(this.fields[i].getTypeCode()));
                i = i + 1;
            }
            while (i < this.fields.length) {
                this.references[this.fields[i].getOffset()] =
                        in.readObject0(this.fields[i].isUnshared());
                i = i + 1;
            }
        }

        public ObjectStreamClass getObjectStreamClass() {
            return this.desc;
        }

        public boolean defaulted(String name) throws IOException {
            return this.lookUp(name, (char) 0) == null;
        }

        /**
         * The field `name` inside the stream, or `null` if the stream did not bring it but the
         * local class does have it --which is the case where returning the default value is right.
         *
         * <p>The three answers are different and have to be told apart. The stream not bringing a
         * field the class has is the normal thing when reading something written by an earlier
         * version. Asking about a name that **exists on neither side** is not that: it means the
         * caller got the name --or the type-- wrong, and returning them their own default value
         * would hide the mistake from them for ever, because they would never see anything other
         * than what they passed in.
         *
         * @throws IllegalArgumentException if `name` with that type is a field neither of the
         *     stream nor of the class
         */
        private ObjectStreamField lookUp(String name, char kind) {
            if (name == null) {
                throw new NullPointerException();
            }
            ObjectStreamField f = coincide(this.fields, name, kind);
            if (f != null) {
                return f;
            }
            ObjectStreamClass local = this.desc.forClass() == null
                    ? null : ObjectStreamClass.lookup(this.desc.forClass());
            if (local != null && coincide(local.getFields(), name, kind) != null) {
                return null;
            }
            throw new IllegalArgumentException("no such field " + name + " with type "
                    + typeName(kind));
        }

        private static ObjectStreamField coincide(ObjectStreamField[] cs, String name, char kind) {
            int i = 0;
            while (i < cs.length) {
                if (cs[i].getName().equals(name)) {
                    char t = cs[i].getTypeCode();
                    // `kind == 0` is `defaulted`'s query, which asks by name alone. An `'L'` asked
                    // for matches any reference --arrays included-- because `get(String, Object)`'s
                    // signature cannot say which; the primitives have to match exactly, since an
                    // `int` is not a `short`.
                    if (kind == 0 || (kind == 'L' ? (t == 'L' || t == '[') : t == kind)) {
                        return cs[i];
                    }
                    return null;
                }
                i = i + 1;
            }
            return null;
        }

        private static String typeName(char kind) {
            if (kind == 'Z') {
                return "boolean";
            }
            if (kind == 'B') {
                return "byte";
            }
            if (kind == 'C') {
                return "char";
            }
            if (kind == 'S') {
                return "short";
            }
            if (kind == 'I') {
                return "int";
            }
            if (kind == 'J') {
                return "long";
            }
            if (kind == 'F') {
                return "float";
            }
            if (kind == 'D') {
                return "double";
            }
            return "class java.lang.Object";
        }

        public boolean get(String name, boolean val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'Z');
            return f == null ? val : this.primitives[f.getOffset()] != 0;
        }

        public byte get(String name, byte val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'B');
            return f == null ? val : this.primitives[f.getOffset()];
        }

        public char get(String name, char val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'C');
            return f == null ? val : (char) this.shortAt(f.getOffset());
        }

        public short get(String name, short val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'S');
            return f == null ? val : (short) this.shortAt(f.getOffset());
        }

        public int get(String name, int val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'I');
            return f == null ? val : this.intAt(f.getOffset());
        }

        public long get(String name, long val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'J');
            return f == null ? val : this.len(f.getOffset());
        }

        public float get(String name, float val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'F');
            return f == null ? val : Float.intBitsToFloat(this.intAt(f.getOffset()));
        }

        public double get(String name, double val) throws IOException {
            ObjectStreamField f = this.lookUp(name, 'D');
            return f == null ? val : Double.longBitsToDouble(this.len(f.getOffset()));
        }

        public Object get(String name, Object val) throws IOException, ClassNotFoundException {
            ObjectStreamField f = this.lookUp(name, 'L');
            return f == null ? val : this.references[f.getOffset()];
        }

        private int shortAt(int off) {
            return ((this.primitives[off] & 0xFF) << 8) | (this.primitives[off + 1] & 0xFF);
        }

        private int intAt(int off) {
            return ((this.primitives[off] & 0xFF) << 24)
                    | ((this.primitives[off + 1] & 0xFF) << 16)
                    | ((this.primitives[off + 2] & 0xFF) << 8)
                    | (this.primitives[off + 3] & 0xFF);
        }

        private long len(int off) {
            return (((long) this.intAt(off)) << 32) | (((long) this.intAt(off + 4)) & 0xFFFFFFFFL);
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
            return 4;                  // I y F
        }
    }

    // ---- the block layer --------------------------------------------------------------------------

    /**
     * The layer that unwraps the block records, mirror of the writer's `BlockOutput`.
     *
     * <p>In block mode each read comes out of the {@code TC_BLOCKDATA} record in progress, and when
     * that runs out it **does not carry straight on**: it has to see whether what comes next is
     * another record or the {@code TC_ENDBLOCKDATA} that closes it. Outside block mode the bytes
     * come out raw, which is how the typecodes and the default fields are read.
     *
     * <p>**One byte of look-ahead** is enough: everything there is to decide --whether a record
     * follows, whether a typecode follows-- is decided by looking at the next byte, and more than
     * one never has to be given back.
     */
    private static final class BlockInput {
        private final InputStream in;

        /** The byte read and not consumed, or -1 if none is stored. */
        private int peeked = -1;
        private boolean inBlock;
        /** Bytes left of the block record in progress. */
        private int remaining;
        private long consumed;

        BlockInput(InputStream in) {
            this.in = in;
        }

        boolean inBlockMode() {
            return this.inBlock;
        }

        /** It changes mode and returns the previous one. */
        boolean blockMode(boolean fresh) throws IOException {
            boolean before = this.inBlock;
            if (before != fresh) {
                if (before && this.remaining > 0) {
                    // Leaving block mode with unconsumed data would make the next data byte be read
                    // as a typecode: everything that came afterwards would be misaligned, and the
                    // symptom would turn up a long way from the cause.
                    throw new IllegalStateException("unread block data");
                }
                this.inBlock = fresh;
                this.remaining = 0;
            }
            return before;
        }

        long bytesRead() {
            return this.consumed;
        }

        /** The next byte without consuming it, or -1 at end of stream. */
        int peek() throws IOException {
            if (this.peeked < 0) {
                this.peeked = this.in.read();
            }
            return this.peeked;
        }

        int readRawByte() throws IOException {
            int b = this.peek();
            this.peeked = -1;
            if (b >= 0) {
                this.consumed = this.consumed + 1;
            }
            return b;
        }

        /**
         * It leaves `remaining` holding the bytes available in the record in progress, reading the
         * next one's header if need be. On returning, `remaining == 0` means what follows is
         * **not** a block record.
         */
        private void ensureBlock() throws IOException {
            while (this.inBlock && this.remaining == 0) {
                int tc = this.peek();
                if (tc == ObjectStreamConstants.TC_BLOCKDATA) {
                    this.readRawByte();
                    int n = this.readRawByte();
                    if (n < 0) {
                        throw new EOFException();
                    }
                    this.remaining = n;
                } else if (tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                    this.readRawByte();
                    int n = this.readRawInt();
                    if (n < 0) {
                        throw new StreamCorruptedException("illegal block data header length: " + n);
                    }
                    this.remaining = n;
                } else {
                    return;
                }
            }
        }

        /** How many data bytes are left in the record in progress. */
        int refill() throws IOException {
            this.ensureBlock();
            return this.remaining;
        }

        void skipBlock() throws IOException {
            for (;;) {
                this.ensureBlock();
                if (this.remaining == 0) {
                    return;
                }
                while (this.remaining > 0) {
                    if (this.readRawByte() < 0) {
                        throw new EOFException();
                    }
                    this.remaining = this.remaining - 1;
                }
            }
        }

        /** One data byte; it fails at end of stream. It is what `DataInput`'s `readXxx` use. */
        private int oneByte() throws IOException {
            int b = this.read0();
            if (b < 0) {
                throw new EOFException();
            }
            return b;
        }

        /** One data byte, or -1 when there is no more. It is what `read()` uses. */
        int read0() throws IOException {
            if (this.inBlock) {
                this.ensureBlock();
                if (this.remaining == 0) {
                    return -1;
                }
                this.remaining = this.remaining - 1;
            }
            return this.readRawByte();
        }

        int read0(byte[] buf, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int i = 0;
            while (i < len) {
                int b = this.read0();
                if (b < 0) {
                    return i == 0 ? -1 : i;
                }
                buf[off + i] = (byte) b;
                i = i + 1;
            }
            return i;
        }

        /** Exactly `len` data bytes; it fails if they are not there. */
        void readRaw(byte[] buf, int off, int len) throws IOException {
            int i = 0;
            while (i < len) {
                buf[off + i] = (byte) this.oneByte();
                i = i + 1;
            }
        }

        /** The next typecode. There is no block mode here: a typecode is never framed. */
        int peekRequired() throws IOException {
            int b = this.peek();
            if (b < 0) {
                throw new EOFException();
            }
            return b;
        }

        int readUnsignedByte0() throws IOException {
            return this.oneByte();
        }

        int readUnsignedShort0() throws IOException {
            return (this.oneByte() << 8) | this.oneByte();
        }

        int readInt0() throws IOException {
            return (this.oneByte() << 24) | (this.oneByte() << 16) | (this.oneByte() << 8)
                    | this.oneByte();
        }

        private int readRawInt() throws IOException {
            int a = this.readRawByte();
            int b = this.readRawByte();
            int c = this.readRawByte();
            int d = this.readRawByte();
            if ((a | b | c | d) < 0) {
                throw new EOFException();
            }
            return (a << 24) | (b << 16) | (c << 8) | d;
        }

        long readLong0() throws IOException {
            long alta = this.readInt0();
            long low = this.readInt0();
            return (alta << 32) | (low & 0xFFFFFFFFL);
        }

        String readUtf() throws IOException {
            return this.readRawUtf(this.readUnsignedShort0());
        }

        /**
         * **Modified** UTF-8: the zero comes in two bytes and each `char` is encoded on its own, so
         * a surrogate pair is two three-byte sequences and not one of four. Decoding it with real
         * UTF-8 would get exactly those two cases wrong.
         */
        String readRawUtf(long bytes) throws IOException {
            StringBuilder sb = new StringBuilder();
            long done = 0;
            while (done < bytes) {
                int b1 = this.oneByte();
                done = done + 1;
                if (b1 < 0x80) {
                    if (b1 == 0) {
                        throw new UTFDataFormatException("malformed input: zero byte");
                    }
                    sb.append((char) b1);
                } else if ((b1 & 0xE0) == 0xC0) {
                    if (done >= bytes) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int b2 = this.oneByte();
                    done = done + 1;
                    if ((b2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + done);
                    }
                    sb.append((char) (((b1 & 0x1F) << 6) | (b2 & 0x3F)));
                } else if ((b1 & 0xF0) == 0xE0) {
                    if (done + 1 >= bytes) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int b2 = this.oneByte();
                    int b3 = this.oneByte();
                    done = done + 2;
                    if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + done);
                    }
                    sb.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
                } else {
                    throw new UTFDataFormatException("malformed input around byte " + done);
                }
            }
            return sb.toString();
        }

        int available() throws IOException {
            if (this.inBlock) {
                this.ensureBlock();
                return this.remaining;
            }
            int n = this.in.available();
            return this.peeked >= 0 ? n + 1 : n;
        }

        void close() throws IOException {
            this.in.close();
        }
    }

    // ---- utilities -------------------------------------------------------------------------------

    private static Class<?> primitiveByName(String n) {
        if (n.equals("int")) {
            return Integer.TYPE;
        }
        if (n.equals("long")) {
            return Long.TYPE;
        }
        if (n.equals("double")) {
            return Double.TYPE;
        }
        if (n.equals("float")) {
            return Float.TYPE;
        }
        if (n.equals("byte")) {
            return Byte.TYPE;
        }
        if (n.equals("short")) {
            return Short.TYPE;
        }
        if (n.equals("char")) {
            return Character.TYPE;
        }
        if (n.equals("boolean")) {
            return Boolean.TYPE;
        }
        if (n.equals("void")) {
            return Void.TYPE;
        }
        return null;
    }

    private static String hex2(int v) {
        String s = Integer.toHexString(v & 0xFF);
        return s.length() < 2 ? "0" + s : s;
    }

    private static String hex4(int v) {
        String s = Integer.toHexString(v & 0xFFFF);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
}
