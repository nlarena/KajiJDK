package jdk.internal.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * KajiLibrary's jdk.internal.vm.VMSupport -- the bridge through which the VM hands things to Java.
 *
 * <p>It gathers three jobs that do not look like one another and that are here for the same reason:
 * the three cross the border between the runtime and the library. Serialising properties so that an
 * agent reads them from another process, translating exceptions born on the side of the JIT
 * compiler, and encoding annotations for the same one.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>Four of the JDK's ten members are not here. The reason is not the same for the four, and it is
 * as well to separate them because not all of them age the same way.
 *
 * <ul>
 * <li><strong>{@code encodeThrowable(Throwable, long, int)}</strong> and <strong>{@code
 *     decodeAndThrowThrowable(int, long, boolean, boolean)}</strong> -- the {@code long} is a **raw
 *     memory address** where the bytes are written or read from. This VM has no memory addressable
 *     from Java; the buffer does not exist and there is nothing to point at.</li>
 * <li><strong>{@code encodeAnnotations(Collection)}</strong> -- the members of each annotation have
 *     to be read. The JDK does not read them by reflection: it uses {@code
 *     sun.reflect.annotation.AnnotationSupport.memberValues(a)}, which casts {@code
 *     Proxy.getInvocationHandler(a)} to {@code AnnotationInvocationHandler} and asks it for the map
 *     already built. **Here that road does not exist**: an annotation of this VM is not a `Proxy`
 *     but a synthetic class the compiler makes (`Marked$$Anno$0`), so there is no
 *     `InvocationHandler` to take the map from. The only portable road left is invoking the member
 *     methods on the instance, and that **brings the VM down** (`index out of bounds` in the
 *     interpreter). The ablation separated the steps: `getAnnotations()` and `annotationType()`
 *     work (`A1`), `getDeclaredMethods()` on the type works and returns the member (`A2`), and the
 *     one that kills it is `Method.invoke` **on the annotation instance** (`A4`). (The ablation,
 *     `A1`-`A4` under `scratchpad/zz350/`, is not in the tree.)</li>
 * <li><strong>{@code encodeAnnotations(byte[], Class, ConstantPool, boolean, Class[])}</strong> --
 *     its parameter type {@link jdk.internal.reflect.ConstantPool} **is already in this library**,
 *     so that stopped being the reason. Three remain, and any one is enough: the JDK's body is
 *     {@code AnnotationParser.parseSelectAnnotations(raw, cp, ...)}, and `sun.reflect.annotation`
 *     is not here; that parsing needs a `ConstantPool` **with data**, and ours cannot have any
 *     because the VM does not expose its pool (it is said in that class); and it ends up delegating
 *     to {@code encodeAnnotations(Collection)}, which is the block above. Writing `ConstantPool`
 *     was necessary to even name the signature, but it is far from sufficient.</li>
 * </ul>
 *
 * <p>{@link #decodeAnnotations(byte[], AnnotationDecoder)} **is here, and complete**. It used to be
 * listed as blocked, with the reason of `encodeAnnotations(Collection)` copied on top; it was
 * false. Decoding touches no annotation and no class: it reads bytes and hands what it finds to the
 * {@link AnnotationDecoder} it is given, which is the one that decides how to represent it. It
 * needs no reflection, no constant pool, no native memory -- only a `DataInputStream`.
 *
 * <p>The nested interface {@link AnnotationDecoder} is a pure declaration and its contract does not
 * depend on there being somebody who uses it.
 */
public class VMSupport {

    // The agent properties are a map separate from the system one: they are written by whoever
    // plugs into the process, not by the process. Empty and not `null`, because "there is no agent"
    // is a state and not the lack of an answer -- the caller iterates the result without asking.
    private static final Properties AGENT = new Properties();

    public VMSupport() {
    }

    /**
     * The properties left by an agent plugged into the process.
     *
     * <p>Empty: this VM accepts no agents. It is `synchronized` as in the JDK because the map may
     * be written by an outside thread while another reads it.
     */
    public static synchronized Properties getAgentProperties() {
        return VMSupport.AGENT;
    }

    /**
     * The system properties, serialised.
     *
     * <p>The format is `key=value` pairs separated by line breaks, in UTF-8, deliberately dumb
     * because on the other side it is read by code that may be running in another process and that
     * is not going to deserialise Java objects. The note said this is the format the JDK uses here;
     * **it is not**. The JDK writes with {@link Properties#store}: ISO 8859-1, with non-Latin-1
     * characters and the separators escaped, a leading date comment, and only the entries whose key
     * and value are both `String`. This one writes raw UTF-8 with no escaping and every non-null
     * entry through `String.valueOf`, so a key or value holding `=` or a line break is ambiguous on
     * the other side.
     *
     * @throws IOException if building the array fails
     */
    public static byte[] serializePropertiesToByteArray() throws IOException {
        return VMSupport.serialize(System.getProperties());
    }

    /** The agent's ones, in the same format. */
    public static byte[] serializeAgentPropertiesToByteArray() throws IOException {
        return VMSupport.serialize(VMSupport.getAgentProperties());
    }

    /**
     * The VM's temporary directory.
     *
     * <p><strong>Here it is not `native`, and the JDK does declare it so.</strong> It is the only
     * modifier divergence of the file and it is as well to justify it: on this VM, a `native`
     * method with no registered implementation does not throw an exception -- **it brings the
     * process down**. So declaring it `native` to respect the modifier would give a member that
     * kills the program that calls it, and writing it in Java gives one that answers. Between
     * respecting a word and respecting the behaviour, the behaviour wins.
     *
     * <p>The answer comes from `java.io.tmpdir`. The note said that is where the JDK's native gets
     * it too; **it is not**: the JDK's comment says this directory must be well known and the same
     * for all VM instances, so it "cannot be affected by configuration variables such as
     * java.io.tmpdir". The two coincide only while nobody overrides `java.io.tmpdir`.
     *
     * <p>The note also said it returns `null` today because this VM did not define that property.
     * It does now: the VM fills `java.io.tmpdir` from the environment's temporary directory, so
     * this returns a real path. (`System.getenv` still exposes no variables.)
     */
    public static String getVMTemporaryDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    private static byte[] serialize(Properties props) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k == null || v == null) {
                continue;
            }
            out.write(String.valueOf(k).getBytes(StandardCharsets.UTF_8));
            out.write('=');
            out.write(String.valueOf(v).getBytes(StandardCharsets.UTF_8));
            out.write('\n');
        }
        return out.toByteArray();
    }

    /**
     * How to rebuild a decoded annotation.
     *
     * <p>The four type variables are the four worlds the decoder chooses: {@code T} the type,
     * {@code A} the built annotation, {@code E} an enum value, {@code X} an error. The interface
     * makes nothing -- it tells the decoder *what* it found and lets it decide how to represent it.
     * That is what allows the same decoder to serve for building real annotations or for building a
     * description of them without loading their classes.
     *
     * <p>{@link #newErrorValue} is the part that tends to surprise: a value that cannot be resolved
     * **is not an exception**, it is one more value. An annotation that mentions a class that is no
     * longer there has to be describable all the same, with the error inside, instead of making the
     * whole read fail.
     */
    public interface AnnotationDecoder<T, A, E, X> {

        /** The type that corresponds to that descriptor. */
        T resolveType(String name);

        /** An annotation of that type with those members. */
        A newAnnotation(T type, Map.Entry<String, Object>[] elements);

        /** An enum value of that type and that name. */
        E newEnumValue(T enumType, String name);

        /** A value that could not be resolved, with the reason. */
        X newErrorValue(String description);
    }

    /**
     * It rebuilds the annotations that {@code encodeAnnotations} serialised.
     *
     * <p>The format is the JDK's and not one of ours, because on the other side there may be a JIT
     * compiler written separately: a length integer, and then that many annotations. Each
     * annotation is the binary name of its type, another length, and that many name/value pairs
     * where the value starts with a tag byte --the {@code JVM_SIGNATURE} ones for the primitives,
     * {@code 's'} text, {@code 'c'} class, {@code 'e'} enum constant, {@code '@'} nested
     * annotation, {@code '['} array and {@code 'x'} a value that could not be resolved--.
     *
     * <p>The length goes **in one or in four bytes**: if it fits in seven bits a single one is
     * written with the high bit set, and if not, a four-byte `int`. That is why on reading the sign
     * of the first byte is looked at, which is the mark of which of the two forms came. A typical
     * annotation has two or three members, so the short case is the one that always happens.
     *
     * <p>This method **touches no annotation and loads no class**: each type that appears is passed
     * to {@link AnnotationDecoder#resolveType} and each odd value to {@link
     * AnnotationDecoder#newErrorValue}, so the caller can read annotations that mention classes
     * that are not there without the read falling over. It is what makes it implementable here and
     * what separates it from {@code encodeAnnotations}.
     *
     * @return an immutable list with what the decoder made, in the order they came
     */
    @SuppressWarnings("unchecked")
    public static <T, A, E, X> List<A> decodeAnnotations(byte[] encoded,
                                                         AnnotationDecoder<T, A, E, X> decoder) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encoded));
            int n = VMSupport.readLength(dis);
            Object[] out = new Object[n];
            for (int i = 0; i < n; i++) {
                out[i] = VMSupport.readAnnotation(dis, decoder);
            }
            return (List<A>) List.of(out);
        } catch (Exception e) {
            // As in the JDK: a malformed array is an error of whoever produced it, not a condition
            // the caller can handle.
            throw new InternalError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, A, E, X> A readAnnotation(DataInputStream dis,
                                                AnnotationDecoder<T, A, E, X> decoder)
            throws IOException {
        T type = decoder.resolveType(dis.readUTF());
        int n = VMSupport.readLength(dis);
        Map.Entry[] members = new Map.Entry[n];
        for (int i = 0; i < n; i++) {
            String name = dis.readUTF();
            byte tag = dis.readByte();
            members[i] = Map.entry(name, VMSupport.readValue(dis, decoder, tag));
        }
        return decoder.newAnnotation(type, (Map.Entry<String, Object>[]) members);
    }

    private static <T, A, E, X> Object readValue(DataInputStream dis,
                                                 AnnotationDecoder<T, A, E, X> decoder,
                                                 byte tag) throws IOException {
        switch (tag) {
            case 'B': return Byte.valueOf(dis.readByte());
            case 'C': return Character.valueOf(dis.readChar());
            case 'D': return Double.valueOf(dis.readDouble());
            case 'F': return Float.valueOf(dis.readFloat());
            case 'I': return Integer.valueOf(dis.readInt());
            case 'J': return Long.valueOf(dis.readLong());
            case 'S': return Short.valueOf(dis.readShort());
            case 'Z': return Boolean.valueOf(dis.readBoolean());
            case 's': return dis.readUTF();
            case 'c': return decoder.resolveType(dis.readUTF());
            case 'e': {
                // In two steps and not nested: the order of the two reads is part of the format,
                // and leaving it to argument evaluation hides it.
                T enumType = decoder.resolveType(dis.readUTF());
                return decoder.newEnumValue(enumType, dis.readUTF());
            }
            case '@': return VMSupport.readAnnotation(dis, decoder);
            case '[': return VMSupport.readArray(dis, decoder);
            case 'x': return decoder.newErrorValue(dis.readUTF());
            default: throw new InternalError("unsupported tag: " + tag);
        }
    }

    // Arrays come back as an immutable `List` and not as an array of the component type: the
    // decoder chose how to represent each value, so the element type is theirs and not ours, and
    // there is no concrete array we could make without guessing it.
    private static <T, A, E, X> Object readArray(DataInputStream dis,
                                                   AnnotationDecoder<T, A, E, X> decoder)
            throws IOException {
        byte comp = dis.readByte();
        // The enum is the only one that brings its type BEFORE the length, because it is a single
        // one for the whole array. That is why it is read here and not inside the loop.
        T enumType = comp == 'e' ? decoder.resolveType(dis.readUTF()) : null;
        int n = VMSupport.readLength(dis);
        Object[] out = new Object[n];
        for (int i = 0; i < n; i++) {
            switch (comp) {
                case 'B': out[i] = Byte.valueOf(dis.readByte()); break;
                case 'C': out[i] = Character.valueOf(dis.readChar()); break;
                case 'D': out[i] = Double.valueOf(dis.readDouble()); break;
                case 'F': out[i] = Float.valueOf(dis.readFloat()); break;
                case 'I': out[i] = Integer.valueOf(dis.readInt()); break;
                case 'J': out[i] = Long.valueOf(dis.readLong()); break;
                case 'S': out[i] = Short.valueOf(dis.readShort()); break;
                case 'Z': out[i] = Boolean.valueOf(dis.readBoolean()); break;
                case 's': out[i] = dis.readUTF(); break;
                case 'c': out[i] = decoder.resolveType(dis.readUTF()); break;
                case 'e': out[i] = decoder.newEnumValue(enumType, dis.readUTF()); break;
                case '@': out[i] = VMSupport.readAnnotation(dis, decoder); break;
                default: throw new InternalError("unsupported component tag: " + comp);
            }
        }
        return List.of(out);
    }

    // The length comes in one byte with the high bit set if it fits in seven bits, and otherwise in
    // four. The first byte read as signed is negative exactly in the short case, and that is what
    // tells the two forms apart.
    //
    // The three bytes of the long form are read with `readUnsignedByte` and not with `read` as in
    // the JDK. It is the only difference with the original and it is on purpose: `read` returns -1
    // on reaching the end instead of failing, so an array truncated right there gave a huge length
    // built from those -1s and only blew up further on, far from the cause. `readUnsignedByte`
    // throws `EOFException` at the missing byte. With valid bytes the two forms give the same.
    private static int readLength(DataInputStream dis) throws IOException {
        int b1 = dis.readByte();
        if (b1 < 0) {
            return b1 & 0x7F;
        }
        int b2 = dis.readUnsignedByte();
        int b3 = dis.readUnsignedByte();
        int b4 = dis.readUnsignedByte();
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
    }
}
