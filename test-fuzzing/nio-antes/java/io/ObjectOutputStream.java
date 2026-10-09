package java.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * KajiLibrary's java.io.ObjectOutputStream -- the writing side of the serialization format.
 *
 * <p>It produces **the same bytes as the JDK**, and that is no luxury: the format's only purpose is
 * for another JVM to read what this one wrote. A stream that is similar but not identical is no use
 * at all, so this class's test is not "it can be read back" but the byte-for-byte comparison
 * against the stream the real JDK produces for the same object.
 *
 * <h2>The three layers</h2>
 *
 * <p>A serialized stream has three things stacked on top of each other, and confusing them is where
 * nearly every format mistake comes from:
 *
 * <ol>
 *   <li><b>The block bytes.</b> Everything written with {@link DataOutput}'s methods
 *       --{@code writeInt}, {@code write(int)}-- while the stream is in "block mode" gathers in a
 *       buffer and comes out wrapped in a {@code TC_BLOCKDATA} record with its length in front.
 *       Without that length, the reader would not know where what a hand-written
 *       {@code writeObject} wrote ends.
 *   <li><b>The handles.</b> Every object, string, class and descriptor going out to the stream is
 *       numbered from {@code baseWireHandle}, and the second time it appears it comes out as
 *       {@code TC_REFERENCE + number}. It is what makes a graph with cycles terminate, and what
 *       preserves shared identity: two fields that pointed at the same object go on pointing at the
 *       same object on the other side.
 *   <li><b>The descriptors.</b> Before an object's data comes its class's shape: name,
 *       {@code serialVersionUID}, flags and fields, and then its superclass's descriptor. The chain
 *       of descriptors is what allows reading an object whose class on this side has different
 *       fields from the other's.
 * </ol>
 *
 * <h2>What is written of each object</h2>
 *
 * <p>The data goes out **from the top of the hierarchy downwards**: first the highest serializable
 * superclass's fields and last the concrete class's. The order matters because the reader rebuilds
 * in the same direction, and because a class that adds fields at the bottom has to be readable by a
 * reader that only knows the top part.
 *
 * <p>A class declaring {@code private void writeObject(ObjectOutputStream)} writes its own portion
 * **itself**, in block mode and ended with {@code TC_ENDBLOCKDATA}. That closing is what lets the
 * reader skip data it does not understand: without it, a class that wrote too much would leave the
 * stream misaligned for ever.
 *
 * <h2>{@code writeReplace} is not consulted</h2>
 *
 * <p>The specification allows a class to declare {@code Object writeReplace()} in order to send
 * another object in its place. Here it is **not called**, and it is this class's only deviation
 * from the format: doing it right asks for reproducing the specification's accessibility rules
 * --the method counts if it is private to the class, or if it is reachable from it by inheritance
 * within the same package-- and getting that detail wrong in either direction writes an object
 * different from the one the JDK would write, silently. Notice is given here instead of guessing.
 * Its counterpart {@code readResolve} is not called in {@link ObjectInputStream} either, so both
 * sides of this library go on understanding each other.
 */
public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {

    private final BlockOutput bout;
    private final Handles handles = new Handles();
    // The descriptors are cached **per stream**: `ObjectStreamClass.lookup` returns a fresh
    // instance every time, and the handles are handed out by identity. Without the cache, the same
    // class would put out a complete descriptor for each object instead of a five-byte reference.
    private Class<?>[] classCache = new Class<?>[16];
    private ObjectStreamClass[] descCache = new ObjectStreamClass[16];
    private int cacheCount;

    // The type signatures already written, with the handle each was given. It exists so that the
    // same signature --`Ljava/lang/String;` turns up in nearly every descriptor-- goes out once
    // only and by reference afterwards, which is what the JDK does and therefore what has to be
    // written in order to produce its same bytes. The JDK achieves that by interning the signature
    // in `ObjectStreamField` and looking it up by identity in the handle table; this VM has no
    // `String.intern` --its native is not there-- so the table looks up by value. The only thing
    // that does not match is a case that does not occur: a user writing with `writeObject` the
    // interned literal equal to a signature would share a handle with it in the JDK and not here.
    private String[] signatures = new String[8];
    private int[] signatureHandles = new int[8];
    private int signatureCount;

    private int protocol = ObjectStreamConstants.PROTOCOL_VERSION_2;
    private boolean replaceEnabled;
    // The no-argument constructor is for a subclass that reimplements everything. With `delegating`
    // at `true` there is no stream underneath, and `writeObject` goes to `writeObjectOverride`.
    private final boolean delegating;

    private PutFieldImpl currentPut;
    private Object currentObject;
    private ObjectStreamClass currentDesc;

    /**
     * It wraps `out` and **writes the header right there**.
     *
     * <p>Writing in the constructor surprises, and it is what the specification orders: the four
     * header bytes have to be there before anything else, and a reader connecting at the other end
     * of a socket sits waiting for them. If they were deferred to the first {@code writeObject},
     * two processes opening a stream to each other would deadlock, each waiting for the other's
     * header.
     */
    public ObjectOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        this.bout = new BlockOutput(out);
        this.delegating = false;
        this.writeStreamHeader();
        this.bout.blockMode(true);
    }

    /**
     * For a subclass that reimplements serialization entirely.
     *
     * <p>There is no stream underneath: every method that would write bytes throws
     * {@link NotActiveException} or does nothing, and {@link #writeObject} calls
     * {@link #writeObjectOverride}. It is `protected` because it only makes sense from inside a
     * subclass.
     */
    protected ObjectOutputStream() throws IOException, SecurityException {
        this.bout = null;
        this.delegating = true;
    }

    // ---- the header and the subclass hooks -------------------------------------------------------

    /** The four bytes every stream begins with: the magic and the version. */
    protected void writeStreamHeader() throws IOException {
        this.bout.writeShort0(ObjectStreamConstants.STREAM_MAGIC);
        this.bout.writeShort0(ObjectStreamConstants.STREAM_VERSION);
    }

    /**
     * It writes a class's descriptor. A subclass may change the format; the only thing it has to
     * uphold is that {@link ObjectInputStream#readClassDescriptor} reads it the same way.
     */
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        this.writeNonProxyDesc(desc);
    }

    /**
     * A hook for adding data of one's own to `cl`'s descriptor. Empty here, as in the JDK: whatever
     * is written has to be read by {@code resolveClass} on the other side.
     */
    protected void annotateClass(Class<?> cl) throws IOException {
    }

    /** The same for a proxy class. Empty, and for the same reason. */
    protected void annotateProxyClass(Class<?> cl) throws IOException {
    }

    /**
     * Where {@link #writeObject} goes when the stream was constructed with the no-argument
     * constructor. Here it does nothing: the subclass using that constructor is the one that has to
     * write.
     */
    protected void writeObjectOverride(Object obj) throws IOException {
    }

    /**
     * The last filter before writing: if {@link #enableReplaceObject} is on, every object goes
     * through here and what is returned goes out to the stream. Identity here, as in the JDK.
     */
    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * It turns {@link #replaceObject}'s filter on or off, and returns how it was.
     *
     * <p>The JDK asks the security manager for permission because replacing objects in mid-flight
     * is a way for a stream to say one thing and carry another. Here there is no security manager,
     * so the permission is not consulted and the flag is taken as it stands.
     */
    protected boolean enableReplaceObject(boolean enable) throws SecurityException {
        boolean before = this.replaceEnabled;
        this.replaceEnabled = enable;
        return before;
    }

    /**
     * It empties the block buffer into the stream below **without flushing the stream below**.
     *
     * <p>The difference from {@link #flush} is exactly that, and it is the reason both exist:
     * `drain` closes the block record in progress, `flush` also pushes the bytes towards the
     * device.
     */
    protected void drain() throws IOException {
        this.bout.flushBlock();
    }

    // ---- the stream's state ----------------------------------------------------------------------

    /**
     * It chooses the protocol's version.
     *
     * <p>The only visible difference is how an {@link Externalizable}'s data is framed: in
     * {@code PROTOCOL_VERSION_1} it goes out raw and in {@code PROTOCOL_VERSION_2} in block mode,
     * with its {@code TC_ENDBLOCKDATA}. It sounds minor and it is not: only the second allows
     * skipping the object without understanding its class.
     *
     * @throws IllegalStateException if some object has already been written
     * @throws IllegalArgumentException if the version is neither of the two
     */
    public void useProtocolVersion(int version) throws IOException {
        if (this.handles.howMany() != 0) {
            throw new IllegalStateException("stream non-empty");
        }
        if (version != ObjectStreamConstants.PROTOCOL_VERSION_1 && version != ObjectStreamConstants.PROTOCOL_VERSION_2) {
            throw new IllegalArgumentException("unknown version: " + version);
        }
        this.protocol = version;
    }

    /**
     * It cuts the stream's memory: handles and descriptors start from scratch.
     *
     * <p>It serves to avoid accumulating the whole written graph on a long connection. The price is
     * that what comes afterwards is written whole again, and that **shared identity does not cross
     * the cut**: an already written object comes out complete again and on the other side they will
     * be two.
     *
     * @throws IOException if it is called from inside a class's {@code writeObject}
     */
    public void reset() throws IOException {
        if (this.objectDepth()) {
            throw new IOException("stream active");
        }
        this.bout.blockMode(false);
        this.bout.writeByte0(ObjectStreamConstants.TC_RESET);
        this.handles.clear();
        this.cacheCount = 0;
        // The signatures store handles, and after the cut those handles no longer exist: leaving
        // them would write a reference to a number that on the other side was left unassigned.
        this.signatureCount = 0;
        this.bout.blockMode(true);
    }

    private boolean objectDepth() {
        return this.currentObject != null;
    }

    // ---- writing objects --------------------------------------------------------------------------

    /**
     * It writes `obj` and everything hanging off it.
     *
     * <p>It is `final` as in the JDK: a subclass wanting to change what gets written has {@link
     * #writeObjectOverride} and {@link #replaceObject}, and allowing the entry point to be
     * overridden would permit writing an object while skipping the handles, which is how a graph
     * with cycles gets broken.
     */
    public final void writeObject(Object obj) throws IOException {
        if (this.delegating) {
            this.writeObjectOverride(obj);
            return;
        }
        this.writeObject0(obj, false);
    }

    /**
     * It writes `obj` **unshared**: it is not tied to a reusable handle, so if the same object
     * turns up again it comes out whole once more and on the other side they will be two different
     * objects.
     *
     * <p>It is what one wants for a field that has to be private to the object containing it --an
     * internal array nobody else can see-- and it stops the other side from receiving an alias to
     * something nobody on this side was sharing.
     */
    public void writeUnshared(Object obj) throws IOException {
        this.writeObject0(obj, true);
    }

    private void writeObject0(Object obj, boolean unshared) throws IOException {
        boolean blockBefore = this.bout.blockMode(false);
        try {
            if (obj == null) {
                this.bout.writeByte0(ObjectStreamConstants.TC_NULL);
                return;
            }
            if (!unshared) {
                int m = this.handles.find(obj);
                if (m >= 0) {
                    this.bout.writeByte0(ObjectStreamConstants.TC_REFERENCE);
                    this.bout.writeInt0(ObjectStreamConstants.baseWireHandle + m);
                    return;
                }
            }
            // A `Class` and an `ObjectStreamClass` are written before the replacement filter: they
            // are part of the stream's description and not the user's data.
            if (obj instanceof Class) {
                this.writeClass((Class<?>) obj, unshared);
                return;
            }
            if (obj instanceof ObjectStreamClass) {
                this.writeDesc((ObjectStreamClass) obj, unshared);
                return;
            }

            Object value = obj;
            if (this.replaceEnabled) {
                Object rep = this.replaceObject(value);
                if (rep != value) {
                    value = rep;
                    // The replacement may have returned something already written.
                    if (value == null) {
                        this.bout.writeByte0(ObjectStreamConstants.TC_NULL);
                        return;
                    }
                    if (!unshared) {
                        int m = this.handles.find(value);
                        if (m >= 0) {
                            this.bout.writeByte0(ObjectStreamConstants.TC_REFERENCE);
                            this.bout.writeInt0(ObjectStreamConstants.baseWireHandle + m);
                            return;
                        }
                    }
                }
            }

            Class<?> cl = value.getClass();
            if (value instanceof String) {
                this.writeString((String) value, unshared);
            } else if (cl.isArray()) {
                this.writeArray(value, cl, unshared);
            } else if (value instanceof Enum) {
                this.writeEnum((Enum<?>) value, unshared);
            } else if (value instanceof Serializable) {
                this.writeOrdinaryObject(value, cl, unshared);
            } else {
                throw new NotSerializableException(cl.getName());
            }
        } finally {
            this.bout.blockMode(blockBefore);
        }
    }

    private void writeString(String s, boolean unshared) throws IOException {
        long len = BlockOutput.utfLength(s);
        if (len <= 0xFFFFL) {
            this.bout.writeByte0(ObjectStreamConstants.TC_STRING);
            this.handles.assign(unshared ? null : s);
            this.bout.writeShortUtf(s, (int) len);
        } else {
            // More than 64 KB encoded does not fit in the classic format's two-byte length. The
            // long record exists precisely for that and carries the length in eight.
            this.bout.writeByte0(ObjectStreamConstants.TC_LONGSTRING);
            this.handles.assign(unshared ? null : s);
            this.bout.writeLong0(len);
            this.bout.writeRawUtf(s);
        }
    }

    /**
     * A field's type signature: whole the first time, and by reference afterwards.
     *
     * <p>The handle registered is the one the string is going to receive, and that is why it is
     * taken **before** writing it: the number is handed out on writing, not on registering.
     */
    private void writeTypeString(String s) throws IOException {
        if (s == null) {
            this.bout.writeByte0(ObjectStreamConstants.TC_NULL);
            return;
        }
        int i = 0;
        while (i < this.signatureCount) {
            if (this.signatures[i].equals(s)) {
                this.bout.writeByte0(ObjectStreamConstants.TC_REFERENCE);
                this.bout.writeInt0(ObjectStreamConstants.baseWireHandle + this.signatureHandles[i]);
                return;
            }
            i = i + 1;
        }
        int m = this.handles.howMany();
        this.writeString(s, false);
        if (this.signatureCount == this.signatures.length) {
            String[] f2 = new String[this.signatureCount * 2];
            int[] m2 = new int[this.signatureCount * 2];
            System.arraycopy(this.signatures, 0, f2, 0, this.signatureCount);
            System.arraycopy(this.signatureHandles, 0, m2, 0, this.signatureCount);
            this.signatures = f2;
            this.signatureHandles = m2;
        }
        this.signatures[this.signatureCount] = s;
        this.signatureHandles[this.signatureCount] = m;
        this.signatureCount = this.signatureCount + 1;
    }

    private void writeClass(Class<?> cl, boolean unshared) throws IOException {
        this.bout.writeByte0(ObjectStreamConstants.TC_CLASS);
        this.writeDesc(this.descOf(cl), false);
        this.handles.assign(unshared ? null : cl);
    }

    private void writeEnum(Enum<?> en, boolean unshared) throws IOException {
        this.bout.writeByte0(ObjectStreamConstants.TC_ENUM);
        // A constant with a body of its own is an anonymous subclass of the enum, and what goes to
        // the stream is **the enum**: on the other side the constant is looked up by name, and the
        // name belongs to the declared type, not to the subclass that gave it the body.
        Class<?> cl = en.getClass();
        if (cl.getSuperclass() != Enum.class) {
            cl = cl.getSuperclass();
        }
        this.writeDesc(this.descOf(cl), false);
        this.handles.assign(unshared ? null : en);
        this.writeString(en.name(), false);
    }

    private void writeArray(Object arr, Class<?> cl, boolean unshared) throws IOException {
        this.bout.writeByte0(ObjectStreamConstants.TC_ARRAY);
        this.writeDesc(this.descOf(cl), false);
        this.handles.assign(unshared ? null : arr);
        Class<?> comp = cl.getComponentType();
        if (comp.isPrimitive()) {
            this.writePrimitives(arr, comp);
        } else {
            Object[] a = (Object[]) arr;
            this.bout.writeInt0(a.length);
            int i = 0;
            while (i < a.length) {
                this.writeObject0(a[i], false);
                i = i + 1;
            }
        }
    }

    private void writePrimitives(Object arr, Class<?> comp) throws IOException {
        if (comp == int.class) {
            int[] a = (int[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeInt0(a[i]);
            }
        } else if (comp == byte.class) {
            byte[] a = (byte[]) arr;
            this.bout.writeInt0(a.length);
            this.bout.writeRaw(a, 0, a.length);
        } else if (comp == long.class) {
            long[] a = (long[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeLong0(a[i]);
            }
        } else if (comp == boolean.class) {
            boolean[] a = (boolean[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeByte0(a[i] ? 1 : 0);
            }
        } else if (comp == char.class) {
            char[] a = (char[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeShort0(a[i]);
            }
        } else if (comp == short.class) {
            short[] a = (short[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeShort0(a[i]);
            }
        } else if (comp == float.class) {
            float[] a = (float[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeInt0(Float.floatToIntBits(a[i]));
            }
        } else {
            double[] a = (double[]) arr;
            this.bout.writeInt0(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.writeLong0(Double.doubleToLongBits(a[i]));
            }
        }
    }

    private void writeOrdinaryObject(Object obj, Class<?> cl, boolean unshared)
            throws IOException {
        ObjectStreamClass desc = this.descOf(cl);
        this.bout.writeByte0(ObjectStreamConstants.TC_OBJECT);
        this.writeDesc(desc, false);
        this.handles.assign(unshared ? null : obj);
        if (obj instanceof Externalizable) {
            this.writeExternalData((Externalizable) obj);
        } else {
            this.writeSerialData(obj, cl);
        }
    }

    private void writeExternalData(Externalizable obj) throws IOException {
        Object objBefore = this.currentObject;
        PutFieldImpl putBefore = this.currentPut;
        this.currentObject = obj;
        this.currentPut = null;
        try {
            if (this.protocol == ObjectStreamConstants.PROTOCOL_VERSION_1) {
                // Protocol 1 writes raw: with no framing, what is not understood cannot be
                // skipped.
                obj.writeExternal(this);
            } else {
                this.bout.blockMode(true);
                obj.writeExternal(this);
                this.bout.blockMode(false);
                this.bout.writeByte0(ObjectStreamConstants.TC_ENDBLOCKDATA);
            }
        } finally {
            this.currentObject = objBefore;
            this.currentPut = putBefore;
        }
    }

    /**
     * An ordinary object's data, **from the highest serializable superclass downwards**.
     *
     * <p>That direction is what lets a reader knowing only the top part of the hierarchy read what
     * it understands and skip the rest: if the subclass's data came first, there would be no way of
     * getting to the superclass's without first understanding the one below.
     */
    private void writeSerialData(Object obj, Class<?> cl) throws IOException {
        Class<?>[] hierarchy = serializableHierarchy(cl);
        int i = 0;
        while (i < hierarchy.length) {
            Class<?> c = hierarchy[i];
            ObjectStreamClass slot = this.descOf(c);
            Method writer = writeObjectMethod(c);
            Object objBefore = this.currentObject;
            ObjectStreamClass descBefore = this.currentDesc;
            PutFieldImpl putBefore = this.currentPut;
            this.currentObject = obj;
            this.currentDesc = slot;
            this.currentPut = null;
            try {
                if (writer != null) {
                    this.bout.blockMode(true);
                    writer.invoke(obj, new Object[] { this });
                    this.bout.blockMode(false);
                    this.bout.writeByte0(ObjectStreamConstants.TC_ENDBLOCKDATA);
                } else {
                    this.writeDefaultFieldValues(obj, c, slot);
                }
            } finally {
                this.currentObject = objBefore;
                this.currentDesc = descBefore;
                this.currentPut = putBefore;
            }
            i = i + 1;
        }
    }

    private void writeDefaultFieldValues(Object obj, Class<?> cl, ObjectStreamClass desc)
            throws IOException {
        ObjectStreamField[] fields = desc.getFields();
        // The primitives first and packed together, which is how they are sorted and what their
        // offsets point at; then the references, each as a whole object.
        int i = 0;
        while (i < fields.length && fields[i].isPrimitive()) {
            Field f = realField(cl, fields[i].getName(), fields[i].getType());
            char t = fields[i].getTypeCode();
            if (t == 'B') {
                this.bout.writeByte0(f == null ? 0 : f.getByte(obj));
            } else if (t == 'Z') {
                this.bout.writeByte0(f != null && f.getBoolean(obj) ? 1 : 0);
            } else if (t == 'C') {
                this.bout.writeShort0(f == null ? 0 : f.getChar(obj));
            } else if (t == 'S') {
                this.bout.writeShort0(f == null ? 0 : f.getShort(obj));
            } else if (t == 'I') {
                this.bout.writeInt0(f == null ? 0 : f.getInt(obj));
            } else if (t == 'J') {
                this.bout.writeLong0(f == null ? 0L : f.getLong(obj));
            } else if (t == 'F') {
                this.bout.writeInt0(Float.floatToIntBits(f == null ? 0F : f.getFloat(obj)));
            } else {
                this.bout.writeLong0(Double.doubleToLongBits(f == null ? 0D : f.getDouble(obj)));
            }
            i = i + 1;
        }
        while (i < fields.length) {
            Field f = realField(cl, fields[i].getName(), fields[i].getType());
            this.writeObject0(f == null ? null : f.get(obj), fields[i].isUnshared());
            i = i + 1;
        }
    }

    // ---- descriptors ------------------------------------------------------------------------------

    private void writeDesc(ObjectStreamClass desc, boolean unshared) throws IOException {
        if (desc == null) {
            this.bout.writeByte0(ObjectStreamConstants.TC_NULL);
            return;
        }
        int m = this.handles.find(desc);
        if (m >= 0) {
            this.bout.writeByte0(ObjectStreamConstants.TC_REFERENCE);
            this.bout.writeInt0(ObjectStreamConstants.baseWireHandle + m);
            return;
        }
        this.writeClassDescriptor(desc);
    }

    private void writeNonProxyDesc(ObjectStreamClass desc) throws IOException {
        Class<?> cl = desc.forClass();
        this.bout.writeByte0(ObjectStreamConstants.TC_CLASSDESC);
        this.bout.writeUtf(desc.getName());
        this.bout.writeLong0(desc.getSerialVersionUID());
        // The handle is assigned **after the name and the UID and before the fields**: a field
        // whose type is the class itself has to be able to reference it, and it only can if it is
        // numbered already.
        this.handles.assign(desc);

        boolean externalizable = Externalizable.class.isAssignableFrom(cl);
        boolean serializable = Serializable.class.isAssignableFrom(cl);
        int flags = 0;
        if (externalizable) {
            flags = flags | ObjectStreamConstants.SC_EXTERNALIZABLE;
            if (this.protocol != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                flags = flags | ObjectStreamConstants.SC_BLOCK_DATA;
            }
        } else if (serializable) {
            flags = flags | ObjectStreamConstants.SC_SERIALIZABLE;
        }
        if (!externalizable && writeObjectMethod(cl) != null) {
            flags = flags | ObjectStreamConstants.SC_WRITE_METHOD;
        }
        if (Enum.class.isAssignableFrom(cl)) {
            flags = flags | ObjectStreamConstants.SC_ENUM;
        }
        this.bout.writeByte0(flags);

        ObjectStreamField[] fields = desc.getFields();
        this.bout.writeShort0(fields.length);
        int i = 0;
        while (i < fields.length) {
            this.bout.writeByte0(fields[i].getTypeCode());
            this.bout.writeUtf(fields[i].getName());
            if (!fields[i].isPrimitive()) {
                // The type's name is **a stream string with a handle of its own**, not a loose UTF:
                // the same signature repeats across a great many fields and sharing it is half the
                // size of a large descriptor.
                this.writeTypeString(fields[i].getTypeString());
            }
            i = i + 1;
        }

        this.bout.blockMode(true);
        if (cl != null) {
            this.annotateClass(cl);
        }
        this.bout.blockMode(false);
        this.bout.writeByte0(ObjectStreamConstants.TC_ENDBLOCKDATA);

        this.writeDesc(this.topDesc(cl), false);
    }

    // The descriptor of the superclass that still takes part, or `null` when the serializable part
    // of the hierarchy has run out. That `null` is the one that tells the reader where the chain
    // ends.
    private ObjectStreamClass topDesc(Class<?> cl) {
        if (cl == null || cl.isArray()) {
            return null;
        }
        Class<?> sup = cl.getSuperclass();
        if (sup == null || !Serializable.class.isAssignableFrom(sup)) {
            return null;
        }
        return this.descOf(sup);
    }

    private ObjectStreamClass descOf(Class<?> cl) {
        int i = 0;
        while (i < this.cacheCount) {
            if (this.classCache[i] == cl) {
                return this.descCache[i];
            }
            i = i + 1;
        }
        ObjectStreamClass d = ObjectStreamClass.lookupAny(cl);
        if (this.cacheCount == this.classCache.length) {
            Class<?>[] c2 = new Class<?>[this.cacheCount * 2];
            ObjectStreamClass[] d2 = new ObjectStreamClass[this.cacheCount * 2];
            System.arraycopy(this.classCache, 0, c2, 0, this.cacheCount);
            System.arraycopy(this.descCache, 0, d2, 0, this.cacheCount);
            this.classCache = c2;
            this.descCache = d2;
        }
        this.classCache[this.cacheCount] = cl;
        this.descCache[this.cacheCount] = d;
        this.cacheCount = this.cacheCount + 1;
        return d;
    }

    // ---- fields by name (PutField) ----------------------------------------------------------------

    /**
     * A buffer for leaving the fields by name instead of by reflection, so that
     * {@link #writeFields} writes them afterwards.
     *
     * <p>It is the way out for a class that wants to choose what value goes out in each declared
     * field --writing a normalized version of a field, say, or filling in one that no longer exists
     * in the code-- while still producing exactly the same format as the default writing.
     *
     * @throws NotActiveException if no object is being written
     */
    public ObjectOutputStream.PutField putFields() throws IOException {
        if (this.currentPut == null) {
            if (this.currentObject == null || this.currentDesc == null) {
                throw new NotActiveException("not in call to writeObject");
            }
            this.currentPut = new PutFieldImpl(this.currentDesc);
        }
        return this.currentPut;
    }

    /**
     * It writes what was left in {@link #putFields}, with the same format as the default writing.
     *
     * @throws NotActiveException if {@link #putFields} was not called beforehand
     */
    public void writeFields() throws IOException {
        if (this.currentPut == null) {
            throw new NotActiveException("no current PutField object");
        }
        boolean blockBefore = this.bout.blockMode(false);
        try {
            this.currentPut.writeTo(this);
        } finally {
            this.bout.blockMode(blockBefore);
        }
    }

    /**
     * It writes the default fields of the object being serialized.
     *
     * <p>It exists so that a `writeObject` of one's own can do "the usual and this as well":
     * calling it first and then writing one's own things is the normal pattern of a class adding
     * data without changing the fields' shape.
     *
     * @throws NotActiveException if no object is being written
     */
    public void defaultWriteObject() throws IOException {
        if (this.currentObject == null || this.currentDesc == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        boolean blockBefore = this.bout.blockMode(false);
        try {
            this.writeDefaultFieldValues(this.currentObject, this.currentDesc.forClass(),
                    this.currentDesc);
        } finally {
            this.bout.blockMode(blockBefore);
        }
    }

    /** {@link ObjectOutputStream#putFields}'s by-name buffer. */
    public abstract static class PutField {

        public abstract void put(String name, boolean val);

        public abstract void put(String name, byte val);

        public abstract void put(String name, char val);

        public abstract void put(String name, short val);

        public abstract void put(String name, int val);

        public abstract void put(String name, long val);

        public abstract void put(String name, float val);

        public abstract void put(String name, double val);

        public abstract void put(String name, Object val);

        /**
         * @deprecated It wrote the fields straight to the stream skipping the descriptor's format,
         *     so what it produced could not be read back with {@code readFields}. Use {@link
         *     ObjectOutputStream#writeFields}.
         */
        @Deprecated
        public abstract void write(ObjectOutput out) throws IOException;
    }

    private static final class PutFieldImpl extends ObjectOutputStream.PutField {
        private final ObjectStreamField[] fields;
        private final byte[] primitives;
        private final Object[] references;

        PutFieldImpl(ObjectStreamClass desc) {
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

        private ObjectStreamField find(String name, boolean primitive) {
            int i = 0;
            while (i < this.fields.length) {
                if (this.fields[i].getName().equals(name)
                        && this.fields[i].isPrimitive() == primitive) {
                    return this.fields[i];
                }
                i = i + 1;
            }
            throw new IllegalArgumentException("no such field " + name);
        }

        public void put(String name, boolean val) {
            this.primitives[this.find(name, true).getOffset()] = (byte) (val ? 1 : 0);
        }

        public void put(String name, byte val) {
            this.primitives[this.find(name, true).getOffset()] = val;
        }

        public void put(String name, char val) {
            this.putShort(this.find(name, true).getOffset(), val);
        }

        public void put(String name, short val) {
            this.putShort(this.find(name, true).getOffset(), val);
        }

        public void put(String name, int val) {
            this.putInt(this.find(name, true).getOffset(), val);
        }

        public void put(String name, long val) {
            this.putLong(this.find(name, true).getOffset(), val);
        }

        public void put(String name, float val) {
            this.putInt(this.find(name, true).getOffset(), Float.floatToIntBits(val));
        }

        public void put(String name, double val) {
            this.putLong(this.find(name, true).getOffset(), Double.doubleToLongBits(val));
        }

        public void put(String name, Object val) {
            this.references[this.find(name, false).getOffset()] = val;
        }

        public void write(ObjectOutput out) throws IOException {
            throw new UnsupportedOperationException(
                    "PutField.write does not produce a format readFields can read; use writeFields");
        }

        private void putShort(int off, int v) {
            this.primitives[off] = (byte) (v >>> 8);
            this.primitives[off + 1] = (byte) v;
        }

        private void putInt(int off, int v) {
            this.primitives[off] = (byte) (v >>> 24);
            this.primitives[off + 1] = (byte) (v >>> 16);
            this.primitives[off + 2] = (byte) (v >>> 8);
            this.primitives[off + 3] = (byte) v;
        }

        private void putLong(int off, long v) {
            this.putInt(off, (int) (v >>> 32));
            this.putInt(off + 4, (int) v);
        }

        void writeTo(ObjectOutputStream oos) throws IOException {
            oos.bout.writeRaw(this.primitives, 0, this.primitives.length);
            int i = 0;
            while (i < this.fields.length) {
                if (!this.fields[i].isPrimitive()) {
                    oos.writeObject0(this.references[this.fields[i].getOffset()],
                            this.fields[i].isUnshared());
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
            return 4;
        }
    }

    // ---- DataOutput: all of this goes to the block buffer -----------------------------------------

    public void write(int val) throws IOException {
        this.bout.writeByte0(val);
    }

    public void write(byte[] buf) throws IOException {
        this.bout.writeBytes0(buf, 0, buf.length);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        this.bout.writeBytes0(buf, off, len);
    }

    public void writeBoolean(boolean val) throws IOException {
        this.bout.writeByte0(val ? 1 : 0);
    }

    public void writeByte(int val) throws IOException {
        this.bout.writeByte0(val);
    }

    public void writeShort(int val) throws IOException {
        this.bout.writeShort0(val);
    }

    public void writeChar(int val) throws IOException {
        this.bout.writeShort0(val);
    }

    public void writeInt(int val) throws IOException {
        this.bout.writeInt0(val);
    }

    public void writeLong(long val) throws IOException {
        this.bout.writeLong0(val);
    }

    public void writeFloat(float val) throws IOException {
        this.bout.writeInt0(Float.floatToIntBits(val));
    }

    public void writeDouble(double val) throws IOException {
        this.bout.writeLong0(Double.doubleToLongBits(val));
    }

    /** The low byte of each character, and the high one is lost. It is what {@link DataOutput}
     * says. */
    public void writeBytes(String str) throws IOException {
        int i = 0;
        while (i < str.length()) {
            this.bout.writeByte0(str.charAt(i));
            i = i + 1;
        }
    }

    /** Two bytes per character, unencoded. */
    public void writeChars(String str) throws IOException {
        int i = 0;
        while (i < str.length()) {
            this.bout.writeShort0(str.charAt(i));
            i = i + 1;
        }
    }

    /** Modified UTF-8 with the length in two bytes. It is **not** a string with a handle. */
    public void writeUTF(String str) throws IOException {
        this.bout.writeUtf(str);
    }

    public void flush() throws IOException {
        this.drain();
        this.bout.flushDown();
    }

    /**
     * It closes the stream below, after emptying whatever is left in the block buffer.
     *
     * <p>Emptying before closing is no courtesy: what was left in the buffer does not have its
     * block record written yet, and a stream closed without that ends cut off in the middle of a
     * record and cannot be read.
     */
    public void close() throws IOException {
        this.flush();
        this.bout.close();
    }

    // ---- the handle table -------------------------------------------------------------------------

    /**
     * Object -> handle number, **by identity**.
     *
     * <p>By identity and not by {@code equals}: two equal but distinct strings are two objects, and
     * merging them would change the graph the other side rebuilds. The probing is linear over an
     * open array, with the identity hash as the starting point.
     */
    private static final class Handles {
        private Object[] keys = new Object[64];
        private int[] values = new int[64];
        private int used;
        private int next;

        int howMany() {
            return this.next;
        }

        int find(Object o) {
            int i = this.slotFor(o);
            while (this.keys[i] != null) {
                if (this.keys[i] == o) {
                    return this.values[i];
                }
                i = i + 1;
                if (i == this.keys.length) {
                    i = 0;
                }
            }
            return -1;
        }

        /**
         * It numbers the next object. **A `null` consumes a number all the same**: it is how
         * something "unshared" is written, taking its place in the numbering but with nobody able
         * to reference it.
         */
        int assign(Object o) {
            int m = this.next;
            this.next = this.next + 1;
            if (o != null) {
                if ((this.used + 1) * 2 > this.keys.length) {
                    this.grow();
                }
                int i = this.slotFor(o);
                while (this.keys[i] != null) {
                    if (this.keys[i] == o) {
                        return m;
                    }
                    i = i + 1;
                    if (i == this.keys.length) {
                        i = 0;
                    }
                }
                this.keys[i] = o;
                this.values[i] = m;
                this.used = this.used + 1;
            }
            return m;
        }

        void clear() {
            this.keys = new Object[64];
            this.values = new int[64];
            this.used = 0;
            this.next = 0;
        }

        private int slotFor(Object o) {
            int h = System.identityHashCode(o);
            return (h ^ (h >>> 16)) & (this.keys.length - 1);
        }

        private void grow() {
            Object[] oldKeys = this.keys;
            int[] oldValues = this.values;
            this.keys = new Object[oldKeys.length * 2];
            this.values = new int[oldKeys.length * 2];
            int i = 0;
            while (i < oldKeys.length) {
                if (oldKeys[i] != null) {
                    int j = this.slotFor(oldKeys[i]);
                    while (this.keys[j] != null) {
                        j = j + 1;
                        if (j == this.keys.length) {
                            j = 0;
                        }
                    }
                    this.keys[j] = oldKeys[i];
                    this.values[j] = oldValues[i];
                }
                i = i + 1;
            }
        }
    }

    // ---- the block buffer -------------------------------------------------------------------------

    /**
     * The stream below with the format's two modes.
     *
     * <p>In **block mode** everything written gathers and comes out wrapped in a {@code
     * TC_BLOCKDATA} record (or {@code TC_BLOCKDATALONG} if it goes past 255 bytes) with the length
     * in front; outside block mode the bytes go out raw. Both are needed: the tags and the
     * descriptors have to go raw --the reader interprets them byte by byte-- and what a user's
     * {@code writeObject} writes has to be framed, because it is the only thing that lets the
     * reader skip it without understanding it.
     */
    private static final class BlockOutput {
        private static final int MAX = 1024;

        private final OutputStream out;
        private final byte[] buf = new byte[MAX];
        private int howMany;
        private boolean inBlock;

        BlockOutput(OutputStream out) {
            this.out = out;
        }

        /** It changes mode and returns the previous one. Changing **empties** whatever had
         * gathered. */
        boolean blockMode(boolean fresh) throws IOException {
            boolean before = this.inBlock;
            if (before != fresh) {
                this.flushBlock();
                this.inBlock = fresh;
            }
            return before;
        }

        void flushBlock() throws IOException {
            if (this.howMany == 0) {
                return;
            }
            if (this.inBlock) {
                if (this.howMany <= 0xFF) {
                    this.out.write(ObjectStreamConstants.TC_BLOCKDATA);
                    this.out.write(this.howMany);
                } else {
                    this.out.write(ObjectStreamConstants.TC_BLOCKDATALONG);
                    this.out.write(this.howMany >>> 24);
                    this.out.write(this.howMany >>> 16);
                    this.out.write(this.howMany >>> 8);
                    this.out.write(this.howMany);
                }
            }
            this.out.write(this.buf, 0, this.howMany);
            this.howMany = 0;
        }

        private void oneByte(int b) throws IOException {
            if (this.howMany == MAX) {
                this.flushBlock();
            }
            this.buf[this.howMany] = (byte) b;
            this.howMany = this.howMany + 1;
        }

        void writeByte0(int b) throws IOException {
            this.oneByte(b);
        }

        void writeShort0(int v) throws IOException {
            this.oneByte(v >>> 8);
            this.oneByte(v);
        }

        void writeInt0(int v) throws IOException {
            this.oneByte(v >>> 24);
            this.oneByte(v >>> 16);
            this.oneByte(v >>> 8);
            this.oneByte(v);
        }

        void writeLong0(long v) throws IOException {
            this.writeInt0((int) (v >>> 32));
            this.writeInt0((int) v);
        }

        void writeBytes0(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            int i = 0;
            while (i < len) {
                this.oneByte(b[off + i]);
                i = i + 1;
            }
        }

        /** Without going through the block framing: for the raw data of an array or of the
         * fields. */
        void writeRaw(byte[] b, int off, int len) throws IOException {
            this.writeBytes0(b, off, len);
        }

        void writeUtf(String s) throws IOException {
            long len = utfLength(s);
            if (len > 0xFFFFL) {
                throw new UTFDataFormatException("encoded string too long: " + len + " bytes");
            }
            this.writeShortUtf(s, (int) len);
        }

        void writeShortUtf(String s, int len) throws IOException {
            this.writeShort0(len);
            this.writeRawUtf(s);
        }

        /**
         * **Modified** UTF-8: the zero comes out in two bytes and not in one, and each `char` is
         * encoded on its own --a surrogate pair is two three-byte sequences, not one of four. Both
         * differences from real UTF-8 exist so that the encoded string never contains a zero byte,
         * which is what allowed passing it through C APIs.
         */
        void writeRawUtf(String s) throws IOException {
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= 0x0001 && c <= 0x007F) {
                    this.oneByte(c);
                } else if (c <= 0x07FF) {
                    this.oneByte(0xC0 | ((c >> 6) & 0x1F));
                    this.oneByte(0x80 | (c & 0x3F));
                } else {
                    this.oneByte(0xE0 | ((c >> 12) & 0x0F));
                    this.oneByte(0x80 | ((c >> 6) & 0x3F));
                    this.oneByte(0x80 | (c & 0x3F));
                }
                i = i + 1;
            }
        }

        static long utfLength(String s) {
            long n = 0;
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= 0x0001 && c <= 0x007F) {
                    n = n + 1;
                } else if (c <= 0x07FF) {
                    n = n + 2;
                } else {
                    n = n + 3;
                }
                i = i + 1;
            }
            return n;
        }

        void flushDown() throws IOException {
            this.out.flush();
        }

        void close() throws IOException {
            this.flushBlock();
            this.out.close();
        }
    }

    // ---- shared reflection ----------------------------------------------------------------------

    /**
     * The classes that contribute data, from the **highest** serializable superclass to the
     * concrete class.
     *
     * <p>It stops where {@link Serializable} stops: whatever is above that has no serialized form
     * and its fields do not go out, which is what makes that part be built on the other side by the
     * constructor and not by the stream.
     */
    static Class<?>[] serializableHierarchy(Class<?> cl) {
        int howMany = 0;
        Class<?> c = cl;
        while (c != null && Serializable.class.isAssignableFrom(c)) {
            howMany = howMany + 1;
            c = c.getSuperclass();
        }
        Class<?>[] out = new Class<?>[howMany];
        c = cl;
        int i = howMany - 1;
        while (i >= 0) {
            out[i] = c;
            c = c.getSuperclass();
            i = i - 1;
        }
        return out;
    }

    /**
     * The {@code private void writeObject(ObjectOutputStream)} declared by `cl`, or `null`.
     *
     * <p>It has to be **private and declared by that exact class**: it is not an override but a
     * hook keyed on a position in the hierarchy, and an inherited one would run twice --once for
     * its own class and once for the subclass-- writing the same data twice.
     */
    static Method writeObjectMethod(Class<?> cl) {
        return serialMethod(cl, "writeObject", ObjectOutputStream.class, void.class);
    }

    static Method serialMethod(Class<?> cl, String name, Class<?> param, Class<?> returnType) {
        Method[] ms = cl.getDeclaredMethods();
        int i = 0;
        while (i < ms.length) {
            Method m = ms[i];
            if (m.getName().equals(name)
                    && java.lang.reflect.Modifier.isPrivate(m.getModifiers())
                    && !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getReturnType() == returnType) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1 && ps[0] == param) {
                    m.setAccessible(true);
                    return m;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * The real field behind an {@link ObjectStreamField}, or `null` if the class does not have
     * it.
     *
     * <p>The `null` really does happen and is no error: a class declaring {@code
     * serialPersistentFields} may name a field that no longer exists in the code --that is
     * precisely what it is for-- and then the default value goes to the stream. The type has to
     * match as well as the name, because a field of the same name but another type is another
     * field.
     */
    static Field realField(Class<?> cl, String name, Class<?> kind) {
        Field f;
        try {
            f = cl.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            return null;
        }
        if (f.getType() != kind) {
            return null;
        }
        f.setAccessible(true);
        return f;
    }
}
