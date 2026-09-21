package java.io;

// KajiLibrary's java.io.ObjectStreamConstants -- the constants of Java's serialization format.
//
// **This is no implementation choice: it is the format, and it is specified byte by byte** in the
// Java Object Serialization Specification. The values below are the ones that exist, not the ones
// somebody found convenient, because a `.ser` written by one VM has to be readable by another.
// Changing any of these numbers does not "change our format": it produces a file no Java reader
// understands.
//
// It is worth having on its own even if nobody uses it: with these constants a serialized stream
// can be recognized or walked without depending on the classes that produce it, and they are pure
// data checkable against the specification. It also has its two users today, `ObjectOutputStream`
// and `ObjectInputStream`.
//
// A serialized stream always begins with `STREAM_MAGIC` and `STREAM_VERSION`: the four bytes
// `AC ED 00 05`. If a file does not start like that, it is not a Java serialization stream.
//
// Nothing is missing here. `ObjectOutputStream` and `ObjectInputStream` **are here**, and they
// produce and read the same bytes as the JDK: the proof is `java/IoTest.java`, which gives the
// reader the streams the real JDK wrote and compares the result, on both VMs.
public interface ObjectStreamConstants {

    // -------------------------------------------------------------------------------------------
    // The header
    // -------------------------------------------------------------------------------------------

    /** The first two bytes of every serialized stream: `0xACED`, as a signed `short`. */
    short STREAM_MAGIC = (short) 0xaced;

    /** The format's version. It has been 5 since JDK 1.2 and has not changed since. */
    short STREAM_VERSION = 5;

    // -------------------------------------------------------------------------------------------
    // The type codes: what comes next in the stream
    // -------------------------------------------------------------------------------------------

    /** The first of the codes. It is a floor, not a code: `TC_NULL` is worth the same. */
    byte TC_BASE = 0x70;

    /** A null reference. */
    byte TC_NULL = (byte) 0x70;

    /** A reference to an object that already came out in the stream, by its handle. */
    byte TC_REFERENCE = (byte) 0x71;

    /** A class's description: name, serialVersionUID, flags and fields. */
    byte TC_CLASSDESC = (byte) 0x72;

    /** A new object. */
    byte TC_OBJECT = (byte) 0x73;

    /** A `String` of up to 65535 bytes in modified UTF. */
    byte TC_STRING = (byte) 0x74;

    /** An array. */
    byte TC_ARRAY = (byte) 0x75;

    /** A `Class`. */
    byte TC_CLASS = (byte) 0x76;

    /** A block of primitive data of up to 255 bytes, with the length in one byte. */
    byte TC_BLOCKDATA = (byte) 0x77;

    /** The end of an object's data written by its own `writeObject`. */
    byte TC_ENDBLOCKDATA = (byte) 0x78;

    /** It clears the handle table: what already came out is written whole again. */
    byte TC_RESET = (byte) 0x79;

    /** A block of primitive data with the length in four bytes, for those that do not fit in
     * one. */
    byte TC_BLOCKDATALONG = (byte) 0x7A;

    /** An exception that happened while writing. */
    byte TC_EXCEPTION = (byte) 0x7B;

    /** A `String` of more than 65535 bytes, with the length in eight bytes. */
    byte TC_LONGSTRING = (byte) 0x7C;

    /** A dynamic proxy class's description. */
    byte TC_PROXYCLASSDESC = (byte) 0x7D;

    /** An enum constant: it is serialized by name, not by fields. */
    byte TC_ENUM = (byte) 0x7E;

    /** The last of the codes. It is a ceiling, not a code. */
    byte TC_MAX = (byte) 0x7E;

    // -------------------------------------------------------------------------------------------
    // The handles
    // -------------------------------------------------------------------------------------------

    /**
     * The first handle handed out. Every object, string or class descriptor coming out for the
     * first time keeps the next number, and later appearances are written as `TC_REFERENCE` plus
     * that number.
     *
     * <p>That is the mechanism that makes serialization preserve <b>the graph's shape</b> and not
     * just the values: two fields pointing at the same object go on pointing at the same object
     * after deserializing, and a cycle does not hang the writer.
     */
    int baseWireHandle = 0x7E0000;

    // -------------------------------------------------------------------------------------------
    // A class descriptor's flags
    // -------------------------------------------------------------------------------------------

    /** The class defines its own `writeObject`, so its data comes in blocks. */
    byte SC_WRITE_METHOD = 0x01;

    /** The `Externalizable`'s data comes in blocks (protocol 2). */
    byte SC_BLOCK_DATA = 0x08;

    /** The class is `Serializable`. */
    byte SC_SERIALIZABLE = 0x02;

    /** The class is `Externalizable`: it writes and reads itself. */
    byte SC_EXTERNALIZABLE = 0x04;

    /** The class is an enum. */
    byte SC_ENUM = 0x10;

    // -------------------------------------------------------------------------------------------
    // The permissions
    // -------------------------------------------------------------------------------------------

    /** It allows swapping one object for another when writing or reading. */
    SerializablePermission SUBSTITUTION_PERMISSION =
        new SerializablePermission("enableSubstitution");

    /** It allows subclassing the object streams and changing how they are written or read. */
    SerializablePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
        new SerializablePermission("enableSubclassImplementation");

    /** It allows setting the whole VM's deserialization filter. */
    SerializablePermission SERIAL_FILTER_PERMISSION =
        new SerializablePermission("serialFilter");

    // -------------------------------------------------------------------------------------------
    // The protocol versions
    // -------------------------------------------------------------------------------------------

    /**
     * JDK 1.1's protocol. An `Externalizable`'s data goes undelimited, so a reader that does not
     * know the class cannot skip over it.
     */
    int PROTOCOL_VERSION_1 = 1;

    /**
     * The protocol from JDK 1.2 on, which is the one used. An `Externalizable`'s data goes in
     * blocks with a length, so it can be skipped without being understood -- which is what allows
     * reading a stream with classes one does not have.
     */
    int PROTOCOL_VERSION_2 = 2;
}
