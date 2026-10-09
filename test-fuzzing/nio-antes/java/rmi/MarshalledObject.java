package java.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * KajiLibrary's java.rmi.MarshalledObject -- an object kept as serialised bytes.
 *
 * <p>It serialises at construction time and keeps the bytes; {@link #get} deserialises a
 * <b>new</b> copy every time. It is an object frozen in time: changing the original after putting
 * it in here does not change what comes out.
 *
 * <h2>What it is really for</h2>
 *
 * <p>For <b>deferring</b> the deserialisation. A remote object can receive one of these and pass
 * it from hand to hand without needing the class inside; only whoever calls {@code get} has to be
 * able to load it. Without this, an intermediary server would need on its class path everything
 * that passes through it.
 *
 * <h2>{@link #equals} compares the bytes</h2>
 *
 * <p>And it does not call the stored object's {@code equals}, which has two consequences:
 *
 * <ul>
 *   <li>it works with objects that do not override {@code equals}, comparing their content;
 *   <li>two <b>equal</b> objects can give false if they serialise differently -- for example two
 *       hash tables with the same content in a different order.
 * </ul>
 *
 * <p>{@link #hashCode} starts at 13 and mixes the bytes in; that is why a null object's is
 * exactly 13.
 */
public final class MarshalledObject<T> implements Serializable {

    private static final long serialVersionUID = 8988374069173025854L;

    /** The object's bytes, or null if the object was null. */
    private byte[] objBytes = null;

    /**
     * The ones for the classes' location annotations.
     *
     * <p>This note used to say they go separately from the object's bytes. Nothing fills them in:
     * the field is private, nothing in this class assigns it, and the constructor writes through a
     * plain {@code ObjectOutputStream} that records no class locations, so it is always null. It
     * does <b>not</b> enter {@link #equals} either, which is the intent: two equal objects that
     * came from different places are still equal.
     */
    private byte[] locBytes = null;

    /** The hash, computed once over the bytes. */
    private int hash;

    /**
     * It serialises that object.
     *
     * @throws IOException if it could not be serialised
     */
    public MarshalledObject(T obj) throws IOException {
        if (obj == null) {
            this.hash = 13;
            return;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        out.flush();
        this.objBytes = bout.toByteArray();
        int h = 13;
        int i = 0;
        while (i < this.objBytes.length) {
            h = 37 * h + this.objBytes[i];
            i = i + 1;
        }
        this.hash = h;
    }

    /**
     * A new copy of the stored object.
     *
     * <p>Every call deserialises again, so it returns distinct objects.
     *
     * @return the object, or null if null was stored
     * @throws IOException if it could not be read
     * @throws ClassNotFoundException if some class is missing
     */
    public T get() throws IOException, ClassNotFoundException {
        if (this.objBytes == null) {
            return null;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.objBytes));
        @SuppressWarnings("unchecked")
        T result = (T) in.readObject();
        return result;
    }

    /** Over the bytes. See the class note: null gives 13. */
    @Override
    public int hashCode() {
        return this.hash;
    }

    /** It compares the bytes, not the objects. See the class note. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MarshalledObject)) {
            return false;
        }
        MarshalledObject<?> other = (MarshalledObject<?>) obj;
        if (this.objBytes == null || other.objBytes == null) {
            return this.objBytes == other.objBytes;
        }
        if (this.objBytes.length != other.objBytes.length) {
            return false;
        }
        int i = 0;
        while (i < this.objBytes.length) {
            if (this.objBytes[i] != other.objBytes[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
