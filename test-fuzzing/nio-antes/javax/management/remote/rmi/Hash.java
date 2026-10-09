package javax.management.remote.rmi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The number by which an RMI stub names a method on the other side.
 *
 * <h2>Why a number and not the name</h2>
 *
 * <p>When a stub calls, it has to tell the server <strong>which</strong> of the interface's
 * methods it is invoking. Sending the name and the signature as text would be expensive on every
 * call and ambiguous with overloads. RMI sends a 64-bit number derived from the name and the
 * descriptor, and both ends compute it the same way: if they differ, the interfaces are not the
 * same version, and the call is rejected instead of going to the wrong method.
 *
 * <h2>How it is computed</h2>
 *
 * <p>It is from the RMI specification, not a choice of this library: {@code name(descriptor)} is
 * written with {@code writeUTF} --that is, the length in two bytes and then the bytes--, SHA-1 is
 * taken of it, and the digest's first eight bytes are assembled into a {@code long} in
 * least-significant-byte-first order.
 *
 * <h2>Why it is computed here instead of being written down</h2>
 *
 * <p>In the JDK these numbers are constants in the compiled file, because {@code rmic} computed
 * them while building it. Copying them by hand would be copying sixty eighteen-digit literals
 * with no way of checking any of them. Computing them gives the same --it was checked against
 * JDK 25's-- and on top of that it cannot get out of step with the signature if the signature
 * changes.
 */
final class Hash {

    private Hash() {
    }

    /**
     * That method's number.
     *
     * @param m the method
     * @return the number
     * @throws Error if this VM has no SHA-1, which is the only thing that would prevent computing
     *     it
     */
    static long de(Method m) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA-1 not available", e);
        }
        try {
            final DataOutputStream out = new DataOutputStream(
                    new DigestOutputStream(new ByteArrayOutputStream(), md));
            out.writeUTF(descriptor(m));
            out.flush();
        } catch (IOException e) {
            // The destination is an array in memory: there is no device that can fail.
            throw new Error("the hash could not be computed", e);
        }
        final byte[] h = md.digest();
        long hash = 0;
        for (int i = 0; i < 8 && i < h.length; i++) {
            hash += (long) (h[i] & 0xFF) << i * 8;
        }
        return hash;
    }

    /** {@code name(parameterTypes)returnType}, in the compiled file's notation. */
    private static String descriptor(Method m) {
        final StringBuilder b = new StringBuilder(m.getName()).append('(');
        for (final Class<?> p : m.getParameterTypes()) {
            b.append(typeName(p));
        }
        return b.append(')').append(typeName(m.getReturnType())).toString();
    }

    private static String typeName(Class<?> c) {
        if (c.isArray()) {
            return "[" + typeName(c.getComponentType());
        }
        if (!c.isPrimitive()) {
            return "L" + c.getName().replace('.', '/') + ";";
        }
        if (c == boolean.class) {
            return "Z";
        }
        if (c == byte.class) {
            return "B";
        }
        if (c == char.class) {
            return "C";
        }
        if (c == short.class) {
            return "S";
        }
        if (c == int.class) {
            return "I";
        }
        if (c == long.class) {
            return "J";
        }
        if (c == float.class) {
            return "F";
        }
        if (c == double.class) {
            return "D";
        }
        return "V";
    }
}
