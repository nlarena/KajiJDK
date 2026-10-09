package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.DynamicConstantPoolEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.MethodTypeEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.List;

/**
 * It serialises the constant pool (JVMS 4.4) and the `BootstrapMethods` attribute that goes with
 * it.
 *
 * <p>It is separate from the class writer for a reason of order and not of size: the pool is
 * written **at the end** even though it goes at the start of the file. The entries keep being
 * created while the methods are written --each `invokevirtual` may add one-- so how many there are
 * is not known until everything else has been written. The class writer builds the body first, asks
 * for the pool afterwards, and only then joins them in the order of the format.
 *
 * <h2>The **modified** UTF-8</h2>
 *
 * <p>The encoding of a `CONSTANT_Utf8` is not UTF-8. It departs from it at two points, and both
 * matter: the null character goes in **two** bytes instead of one --so that no byte of the contents
 * is zero-- and the characters outside the basic plane go as **two surrogate pairs encoded
 * separately**, six bytes, instead of the four of real UTF-8. Using the system's UTF-8 produces a
 * file the JVM rejects as soon as an emoji appears in a text constant.
 */
final class PoolWriter {

    private PoolWriter() {
    }

    /** The whole pool: the count and then each entry. */
    static void writePool(BufWriterImpl buf, ConstantPoolBuilder pool) {
        int n = pool.size();
        buf.writeU2(n);
        // From 1: index 0 does not exist in the format. A `long` or a `double` takes two indices
        // and the second one is left empty -- that is why the entry is asked for and the missing
        // one is skipped.
        for (int i = 1; i < n; i++) {
            PoolEntry e = pool.entryByIndex(i);
            if (e == null) {
                continue;
            }
            PoolWriter.writeEntry(buf, e);
        }
    }

    private static void writeEntry(BufWriterImpl buf, PoolEntry e) {
        int tag = e.tag();
        buf.writeU1(tag);
        if (tag == PoolEntry.TAG_UTF8) {
            PoolWriter.writeModifiedUtf8(buf, ((Utf8Entry) e).stringValue());
            return;
        }
        if (tag == PoolEntry.TAG_INTEGER) {
            buf.writeInt(((IntegerEntry) e).intValue());
            return;
        }
        if (tag == PoolEntry.TAG_FLOAT) {
            buf.writeFloat(((FloatEntry) e).floatValue());
            return;
        }
        if (tag == PoolEntry.TAG_LONG) {
            buf.writeLong(((LongEntry) e).longValue());
            return;
        }
        if (tag == PoolEntry.TAG_DOUBLE) {
            buf.writeDouble(((DoubleEntry) e).doubleValue());
            return;
        }
        if (tag == PoolEntry.TAG_CLASS) {
            buf.writeIndex(((ClassEntry) e).name());
            return;
        }
        if (tag == PoolEntry.TAG_STRING) {
            buf.writeIndex(((StringEntry) e).utf8());
            return;
        }
        if (tag == PoolEntry.TAG_FIELDREF || tag == PoolEntry.TAG_METHODREF
                || tag == PoolEntry.TAG_INTERFACE_METHODREF) {
            MemberRefEntry m = (MemberRefEntry) e;
            buf.writeIndex(m.owner());
            buf.writeIndex(m.nameAndType());
            return;
        }
        if (tag == PoolEntry.TAG_NAME_AND_TYPE) {
            NameAndTypeEntry nt = (NameAndTypeEntry) e;
            buf.writeIndex(nt.name());
            buf.writeIndex(nt.type());
            return;
        }
        if (tag == PoolEntry.TAG_METHOD_HANDLE) {
            MethodHandleEntry mh = (MethodHandleEntry) e;
            buf.writeU1(mh.kind());
            buf.writeIndex(mh.reference());
            return;
        }
        if (tag == PoolEntry.TAG_METHOD_TYPE) {
            buf.writeIndex(((MethodTypeEntry) e).descriptor());
            return;
        }
        if (tag == PoolEntry.TAG_DYNAMIC || tag == PoolEntry.TAG_INVOKE_DYNAMIC) {
            DynamicConstantPoolEntry d = (DynamicConstantPoolEntry) e;
            buf.writeU2(d.bootstrapMethodIndex());
            buf.writeIndex(d.nameAndType());
            return;
        }
        if (tag == PoolEntry.TAG_MODULE) {
            buf.writeIndex(((ModuleEntry) e).name());
            return;
        }
        if (tag == PoolEntry.TAG_PACKAGE) {
            buf.writeIndex(((PackageEntry) e).name());
            return;
        }
        throw new IllegalArgumentException("unknown pool tag: " + tag);
    }

    /** The `BootstrapMethods` attribute, or nothing if the pool has none. */
    static void writeBootstrapMethods(BufWriterImpl buf, ConstantPoolBuilder pool) {
        int n = pool.bootstrapMethodCount();
        buf.writeIndex(pool.utf8Entry("BootstrapMethods"));
        int lenPos = buf.size();
        buf.writeInt(0);
        buf.writeU2(n);
        for (int i = 0; i < n; i++) {
            BootstrapMethodEntry b = pool.bootstrapMethodEntry(i);
            buf.writeIndex(b.bootstrapMethod());
            List<LoadableConstantEntry> args = b.arguments();
            buf.writeU2(args.size());
            for (int j = 0; j < args.size(); j++) {
                buf.writeIndex(args.get(j));
            }
        }
        buf.patchInt(lenPos, 4, buf.size() - lenPos - 4);
    }

    /** Whether `BootstrapMethods` has to be written. */
    static boolean hasBootstrapMethods(ConstantPoolBuilder pool) {
        return pool.bootstrapMethodCount() > 0;
    }

    // The modified UTF-8 of JVMS 4.4.7. See the class note on where it departs from the real one.
    //
    // The length goes in front in two bytes and **it is that of the bytes, not that of the
    // characters**: one has to encode first to know it, or leave the gap and fill it. The second is
    // done.
    private static void writeModifiedUtf8(BufWriterImpl buf, String s) {
        int lenPos = buf.size();
        buf.writeU2(0);
        int from = buf.size();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                buf.writeU1(c);
            } else if (c <= 0x07FF) {
                // The null falls here and not above: that is why the one-byte range starts at 1.
                buf.writeU1(0xC0 | ((c >> 6) & 0x1F));
                buf.writeU1(0x80 | (c & 0x3F));
            } else {
                buf.writeU1(0xE0 | ((c >> 12) & 0x0F));
                buf.writeU1(0x80 | ((c >> 6) & 0x3F));
                buf.writeU1(0x80 | (c & 0x3F));
            }
        }
        int length = buf.size() - from;
        if (length > 65535) {
            throw new IllegalArgumentException(
                    "a text constant cannot exceed 65535 bytes; this one measures " + length);
        }
        buf.patchInt(lenPos, 2, length);
    }
}
