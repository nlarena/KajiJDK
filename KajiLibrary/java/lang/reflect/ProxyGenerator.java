package java.lang.reflect;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * KajiLibrary's java.lang.reflect.ProxyGenerator -- the class file writer behind {@link Proxy}.
 * Package-private, just as in the JDK: nobody outside `Proxy` has any business knowing that a proxy
 * is, literally, a `.class` built in memory.
 *
 * <h2>Why this generator and not `java.lang.classfile`</h2>
 *
 * <p>`java.lang.classfile` is in KajiLibrary as source, but only on the READING side: writing code
 * with it demands the whole `java.lang.classfile.instruction` package and a `StackMapTable`
 * generator, and neither exists. A proxy needs none of that, so this file emits the exact subset a
 * proxy uses and nothing else.
 *
 * <h2>Why class file version 49</h2>
 *
 * <p>The `StackMapTable` is mandatory from version 50 (Java 6) on and is, by a distance, the
 * expensive part of generating bytecode: the type state at every jump target has to be computed.
 * Version 49 (Java 5) uses the old verifier, the INFERENCE one, which works those types out by
 * itself -- and KajiJDK's verifier implements both forms (see `src/jvm/verifier.rs`), so emitting
 * 49 is legal here and saves the entire stack map generator.
 *
 * <p>The cost of that choice is zero in practice: the methods this generator emits have NOT ONE
 * jump. They are straight from end to end -- build the array, call the dispatcher, cast the return
 * --, and a method with no jumps needs no stack map even in version 69. Version 49 is what makes
 * that *legal* as well, not merely sufficient.
 *
 * <h2>What each method emits</h2>
 *
 * <p>The second decision that saves half the work: the bytecode does NOT look the {@link Method} up
 * nor read the `h` field. Each generated method pushes `this`, an `int` with its index in that
 * class's method table, and an `Object[]` with the arguments already boxed; then it calls one
 * single static method, `jdk.internal.reflect.ProxyDispatcher.dispatch`, which does all the rest
 * IN JAVA: it looks the `Method` up, gets the handler, calls it and translates the exceptions. What
 * is left in bytecode is the only thing that cannot be written in Java -- a signature that does not
 * exist in the source.
 *
 * <p><strong>That method's name travels as a string literal</strong> into the constant pool of every
 * proxy this generator emits, so the name here and the name on `ProxyDispatcher` have to match
 * character for character. Renaming it on one side alone would produce proxies that compile, load,
 * and fail at the first call. The two moved together when the class was translated, and they have
 * to keep moving together.
 *
 * <p>The cast/unbox of the return is no decoration either: it is what gives the proxy the semantics
 * the JDK documents, and for free. `checkcast Integer` over a `null` passes, and the `intValue()`
 * that comes after throws `NullPointerException` -- which is exactly what the JDK promises for a
 * `null` returned by a method with a primitive return. A value of the wrong type dies at the
 * `checkcast` with `ClassCastException`, which is the other half of the promise. Neither is
 * programmed; both fall out of choosing these two instructions.
 */
final class ProxyGenerator {

    /** See the class's note: 49 is the last version without a mandatory `StackMapTable`. */
    private static final int VERSION_MAJOR = 49;
    private static final int VERSION_MINOR = 0;

    static final int ACC_PUBLIC = 0x0001;
    static final int ACC_FINAL = 0x0010;
    static final int ACC_SUPER = 0x0020;

    // Constant pool tags (JVMS 4.4). Only these five: with no invokedynamic, no String literals and
    // no 8-byte constants, a proxy's pool is tiny.
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_CLASS = 7;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_NAMEANDTYPE = 12;

    // The opcodes that get used. They are named constants because a loose `0x2a` in the middle of
    // an emitter is indistinguishable from a typo.
    private static final int OP_ACONST_NULL = 0x01;
    private static final int OP_ICONST_0 = 0x03;
    private static final int OP_BIPUSH = 0x10;
    private static final int OP_SIPUSH = 0x11;
    private static final int OP_LDC_W = 0x13;
    private static final int OP_ILOAD = 0x15;
    private static final int OP_LLOAD = 0x16;
    private static final int OP_FLOAD = 0x17;
    private static final int OP_DLOAD = 0x18;
    private static final int OP_ALOAD = 0x19;
    private static final int OP_ILOAD_0 = 0x1a;
    private static final int OP_LLOAD_0 = 0x1e;
    private static final int OP_FLOAD_0 = 0x22;
    private static final int OP_DLOAD_0 = 0x26;
    private static final int OP_ALOAD_0 = 0x2a;
    private static final int OP_AASTORE = 0x53;
    private static final int OP_POP = 0x57;
    private static final int OP_DUP = 0x59;
    private static final int OP_IRETURN = 0xac;
    private static final int OP_LRETURN = 0xad;
    private static final int OP_FRETURN = 0xae;
    private static final int OP_DRETURN = 0xaf;
    private static final int OP_ARETURN = 0xb0;
    private static final int OP_RETURN = 0xb1;
    private static final int OP_INVOKEVIRTUAL = 0xb6;
    private static final int OP_INVOKESPECIAL = 0xb7;
    private static final int OP_INVOKESTATIC = 0xb8;
    private static final int OP_ANEWARRAY = 0xbd;
    private static final int OP_CHECKCAST = 0xc0;

    private static final String PROXY = "java/lang/reflect/Proxy";
    private static final String HANDLER = "Ljava/lang/reflect/InvocationHandler;";
    private static final String DISPATCHER = "jdk/internal/reflect/ProxyDispatcher";
    private static final String DISPATCH_SIGNATURE =
            "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;";

    /** The pool's entries, already serialised, in order; an entry's index is its position + 1. */
    private final ArrayList<byte[]> pool = new ArrayList<byte[]>();

    /** From key (see {@link #intern}) to index, so a repeated constant goes in once. */
    private final HashMap<String, Integer> indices = new HashMap<String, Integer>();

    private ProxyGenerator() {
    }

    // ------------------------------------------------------------------ buffer

    /**
     * A growing array of bytes. `ByteArrayOutputStream` is not used because this generator lives in
     * `java.lang.reflect` and dragging `java.io` in here to write four large integers
     * buys nothing.
     */
    private static final class Buf {
        private byte[] data = new byte[128];
        private int length;

        void u1(int v) {
            this.ensure(1);
            this.data[this.length] = (byte) v;
            this.length = this.length + 1;
        }

        void u2(int v) {
            this.u1(v >> 8);
            this.u1(v);
        }

        void u4(int v) {
            this.u2(v >> 16);
            this.u2(v);
        }

        void raw(byte[] others) {
            this.ensure(others.length);
            System.arraycopy(others, 0, this.data, this.length, others.length);
            this.length = this.length + others.length;
        }

        private void ensure(int n) {
            if (this.length + n <= this.data.length) {
                return;
            }
            int grown = this.data.length * 2;
            if (grown < this.length + n) {
                grown = this.length + n;
            }
            byte[] bigger = new byte[grown];
            System.arraycopy(this.data, 0, bigger, 0, this.length);
            this.data = bigger;
        }

        byte[] bytes() {
            byte[] out = new byte[this.length];
            System.arraycopy(this.data, 0, out, 0, this.length);
            return out;
        }

        int size() {
            return this.length;
        }
    }

    /**
     * A method's body, with the stack height counted as it is emitted.
     *
     * <p>It is counted rather than estimated because `max_stack` is no decoration: the interpreter
     * sizes the frame with it, and a low value is silent corruption while a high one is waste.
     * Since there are no jumps, the height at each point is a single number -- counting it is
     * adding one delta per instruction, and it gives the exact maximum.
     */
    private static final class Code {
        final Buf b = new Buf();
        private int stack;
        private int maxStack;

        void shift(int delta) {
            this.stack = this.stack + delta;
            if (this.stack > this.maxStack) {
                this.maxStack = this.stack;
            }
        }

        void op(int opcode, int delta) {
            this.b.u1(opcode);
            this.shift(delta);
        }

        int maxStack() {
            return this.maxStack;
        }
    }

    // ------------------------------------------------------------ the constant pool

    /**
     * It returns a constant's index, adding it if it was not there.
     *
     * <p>The key carries a per-tag prefix because the name spaces cross: the Utf8
     * "java/lang/Object" and the Class "java/lang/Object" are different entries with the same
     * text.
     */
    private int intern(String key, byte[] entry) {
        Integer already = this.indices.get(key);
        if (already != null) {
            return already.intValue();
        }
        this.pool.add(entry);
        int index = this.pool.size();
        this.indices.put(key, Integer.valueOf(index));
        return index;
    }

    private int utf(String text) {
        byte[] encoded = ProxyGenerator.modifiedUtf8(text);
        Buf b = new Buf();
        b.u1(TAG_UTF8);
        b.u2(encoded.length);
        b.raw(encoded);
        return this.intern("u:" + text, b.bytes());
    }

    private int clazz(String internalName) {
        int name = this.utf(internalName);
        Buf b = new Buf();
        b.u1(TAG_CLASS);
        b.u2(name);
        return this.intern("c:" + internalName, b.bytes());
    }

    private int nameAndType(String name, String descriptor) {
        int n = this.utf(name);
        int d = this.utf(descriptor);
        Buf b = new Buf();
        b.u1(TAG_NAMEANDTYPE);
        b.u2(n);
        b.u2(d);
        return this.intern("n:" + name + " " + descriptor, b.bytes());
    }

    private int methodRef(String owner, String name, String descriptor) {
        int c = this.clazz(owner);
        int nt = this.nameAndType(name, descriptor);
        Buf b = new Buf();
        b.u1(TAG_METHODREF);
        b.u2(c);
        b.u2(nt);
        return this.intern("m:" + owner + " " + name + " " + descriptor, b.bytes());
    }

    private int intConstant(int value) {
        Buf b = new Buf();
        b.u1(TAG_INTEGER);
        b.u4(value);
        return this.intern("I:" + value, b.bytes());
    }

    /**
     * Modified UTF-8 (JVMS 4.4.7): the same as real UTF-8 except that zero is encoded in two bytes
     * and surrogate pairs are encoded separately. It is implemented by hand and not with
     * `String.getBytes` because `getBytes` produces real UTF-8, which for those two cases is a
     * different array of bytes and a class file the parser rejects.
     */
    private static byte[] modifiedUtf8(String text) {
        Buf b = new Buf();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                b.u1(c);
            } else if (c <= 0x07FF) {
                b.u1(0xC0 | (c >> 6));
                b.u1(0x80 | (c & 0x3F));
            } else {
                b.u1(0xE0 | (c >> 12));
                b.u1(0x80 | ((c >> 6) & 0x3F));
                b.u1(0x80 | (c & 0x3F));
            }
            i = i + 1;
        }
        return b.bytes();
    }

    // ------------------------------------------------------------------ descriptors

    /** A type's internal name: `java.util.List` -> `java/util/List`. */
    static String internalName(Class<?> type) {
        return type.getName().replace('.', '/');
    }

    /** A type's field descriptor (JVMS 4.3.2). */
    static String descriptor(Class<?> type) {
        if (type.isPrimitive()) {
            return ProxyGenerator.primitiveDescriptor(type);
        }
        if (type.isArray()) {
            return "[" + ProxyGenerator.descriptor(type.getComponentType());
        }
        return "L" + ProxyGenerator.internalName(type) + ";";
    }

    private static String primitiveDescriptor(Class<?> type) {
        if (type == Void.TYPE) {
            return "V";
        }
        if (type == Boolean.TYPE) {
            return "Z";
        }
        if (type == Byte.TYPE) {
            return "B";
        }
        if (type == Character.TYPE) {
            return "C";
        }
        if (type == Short.TYPE) {
            return "S";
        }
        if (type == Integer.TYPE) {
            return "I";
        }
        if (type == Long.TYPE) {
            return "J";
        }
        if (type == Float.TYPE) {
            return "F";
        }
        return "D";
    }

    /** A signature's method descriptor: `(params)return`. */
    static String methodDescriptor(Class<?>[] params, Class<?> returnType) {
        StringBuilder out = new StringBuilder("(");
        int i = 0;
        while (i < params.length) {
            out.append(ProxyGenerator.descriptor(params[i]));
            i = i + 1;
        }
        out.append(")");
        out.append(ProxyGenerator.descriptor(returnType));
        return out.toString();
    }

    /** How many frame slots a type takes: two for `long` and `double`, one for the rest. */
    private static int slotsOf(Class<?> type) {
        if (type == Long.TYPE || type == Double.TYPE) {
            return 2;
        }
        return 1;
    }

    /** A primitive's wrapper class, in internal form; `null` if the type is not primitive. */
    private static String wrapperOf(Class<?> type) {
        if (type == Boolean.TYPE) {
            return "java/lang/Boolean";
        }
        if (type == Byte.TYPE) {
            return "java/lang/Byte";
        }
        if (type == Character.TYPE) {
            return "java/lang/Character";
        }
        if (type == Short.TYPE) {
            return "java/lang/Short";
        }
        if (type == Integer.TYPE) {
            return "java/lang/Integer";
        }
        if (type == Long.TYPE) {
            return "java/lang/Long";
        }
        if (type == Float.TYPE) {
            return "java/lang/Float";
        }
        if (type == Double.TYPE) {
            return "java/lang/Double";
        }
        return null;
    }

    /** The name of the method that gets the primitive out of its wrapper: `intValue`,
     * `charValue`... */
    private static String unboxerOf(Class<?> type) {
        if (type == Boolean.TYPE) {
            return "booleanValue";
        }
        if (type == Byte.TYPE) {
            return "byteValue";
        }
        if (type == Character.TYPE) {
            return "charValue";
        }
        if (type == Short.TYPE) {
            return "shortValue";
        }
        if (type == Integer.TYPE) {
            return "intValue";
        }
        if (type == Long.TYPE) {
            return "longValue";
        }
        if (type == Float.TYPE) {
            return "floatValue";
        }
        return "doubleValue";
    }

    // ------------------------------------------------------------------ emission

    /**
     * It builds a proxy's class file.
     *
     * @param binaryName the dotted name of the class to generate
     * @param interfaces the interfaces it implements, in order
     * @param isPublic whether the class carries `ACC_PUBLIC`
     * @param methods the method table, in the same order the dispatcher will index it
     * @return the `.class` bytes
     */
    static byte[] generate(String binaryName, Class<?>[] interfaces, boolean isPublic,
            Method[] methods) {
        ProxyGenerator gen = new ProxyGenerator();
        return gen.build(binaryName, interfaces, isPublic, methods);
    }

    private byte[] build(String binaryName, Class<?>[] interfaces, boolean isPublic,
            Method[] methods) {
        String thisClass = binaryName.replace('.', '/');

        // The methods are serialised FIRST: each one puts constants into the pool, and the pool can
        // only be written once nobody is going to add anything more to it.
        ArrayList<byte[]> bodies = new ArrayList<byte[]>();
        bodies.add(this.constructor());
        int i = 0;
        while (i < methods.length) {
            bodies.add(this.proxyMethod(thisClass, methods[i], i));
            i = i + 1;
        }

        int thisIndex = this.clazz(thisClass);
        int superIndex = this.clazz(PROXY);
        int[] interfaceIndices = new int[interfaces.length];
        i = 0;
        while (i < interfaces.length) {
            interfaceIndices[i] = this.clazz(ProxyGenerator.internalName(interfaces[i]));
            i = i + 1;
        }

        Buf out = new Buf();
        out.u4(0xCAFEBABE);
        out.u2(VERSION_MINOR);
        out.u2(VERSION_MAJOR);
        // constant_pool_count is "the count + 1": entry 0 does not exist and is counted anyway.
        out.u2(this.pool.size() + 1);
        i = 0;
        while (i < this.pool.size()) {
            out.raw(this.pool.get(i));
            i = i + 1;
        }
        int access = ACC_FINAL | ACC_SUPER;
        if (isPublic) {
            access = access | ACC_PUBLIC;
        }
        out.u2(access);
        out.u2(thisIndex);
        out.u2(superIndex);
        out.u2(interfaceIndices.length);
        i = 0;
        while (i < interfaceIndices.length) {
            out.u2(interfaceIndices[i]);
            i = i + 1;
        }
        out.u2(0); // fields_count: the only field a proxy has is `h`, and it inherits it
        out.u2(bodies.size());
        i = 0;
        while (i < bodies.size()) {
            out.raw(bodies.get(i));
            i = i + 1;
        }
        out.u2(0); // attributes_count
        return out.bytes();
    }

    /**
     * `public $Proxy0(InvocationHandler h) { super(h); }`.
     *
     * <p>It is public and not protected on purpose: `Proxy.newProxyInstance` looks it up by
     * reflection and invokes it from another package, which is exactly what the JDK does.
     */
    private byte[] constructor() {
        Code c = new Code();
        c.op(OP_ALOAD_0, 1);
        c.op(OP_ALOAD_0 + 1, 1); // aload_1: the handler
        c.b.u1(OP_INVOKESPECIAL);
        c.b.u2(this.methodRef(PROXY, "<init>", "(" + HANDLER + ")V"));
        c.shift(-2);
        c.op(OP_RETURN, 0);
        return this.methodInfo(ACC_PUBLIC, "<init>", "(" + HANDLER + ")V",
                c.b.bytes(), c.maxStack(), 2, null);
    }

    /**
     * One of the proxy's methods: pack, dispatch, cast.
     *
     * <p>The index is the method's position in the table `ProxyDispatcher` has registered for this
     * class. Numbering instead of naming is what leaves the bytecode with no lookup at all: the
     * dispatcher indexes an array and has the {@link Method} already.
     */
    private byte[] proxyMethod(String thisClass, Method method, int index) {
        Class<?>[] params = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        Code c = new Code();

        c.op(OP_ALOAD_0, 1); // the proxy, the dispatcher's first argument
        this.pushInt(c, index);

        if (params.length == 0) {
            // `null` and not an empty array: it is what the JDK passes the handler when the
            // method takes nothing, and there is code written against that.
            c.op(OP_ACONST_NULL, 1);
        } else {
            this.pushInt(c, params.length);
            c.b.u1(OP_ANEWARRAY);
            c.b.u2(this.clazz("java/lang/Object"));
            c.shift(0); // it consumes the length, leaves the reference
            int slot = 1; // 0 is `this`
            int i = 0;
            while (i < params.length) {
                c.op(OP_DUP, 1);
                this.pushInt(c, i);
                this.loadArg(c, params[i], slot);
                this.boxArg(c, params[i]);
                c.op(OP_AASTORE, -3);
                slot = slot + ProxyGenerator.slotsOf(params[i]);
                i = i + 1;
            }
        }

        c.b.u1(OP_INVOKESTATIC);
        c.b.u2(this.methodRef(DISPATCHER, "dispatch", DISPATCH_SIGNATURE));
        c.shift(-2); // three go in, one comes out

        this.returnOpcode(c, returnType);

        int maxLocals = 1;
        int i2 = 0;
        while (i2 < params.length) {
            maxLocals = maxLocals + ProxyGenerator.slotsOf(params[i2]);
            i2 = i2 + 1;
        }
        String descriptor = ProxyGenerator.methodDescriptor(params, returnType);
        return this.methodInfo(ACC_PUBLIC | ACC_FINAL, method.getName(), descriptor,
                c.b.bytes(), c.maxStack(), maxLocals, method.getExceptionTypes());
    }

    /** The shortest integer literal that will serve: `iconst_N`, `bipush`, `sipush` or `ldc_w`. */
    private void pushInt(Code c, int value) {
        if (value >= 0 && value <= 5) {
            c.op(OP_ICONST_0 + value, 1);
        } else if (value >= -128 && value <= 127) {
            c.b.u1(OP_BIPUSH);
            c.b.u1(value);
            c.shift(1);
        } else if (value >= -32768 && value <= 32767) {
            c.b.u1(OP_SIPUSH);
            c.b.u2(value);
            c.shift(1);
        } else {
            c.b.u1(OP_LDC_W);
            c.b.u2(this.intConstant(value));
            c.shift(1);
        }
    }

    /** It loads the parameter in that slot with the instruction its type calls for. */
    private void loadArg(Code c, Class<?> type, int slot) {
        int base;
        int width;
        if (type == Long.TYPE) {
            base = OP_LLOAD_0;
            width = OP_LLOAD;
        } else if (type == Float.TYPE) {
            base = OP_FLOAD_0;
            width = OP_FLOAD;
        } else if (type == Double.TYPE) {
            base = OP_DLOAD_0;
            width = OP_DLOAD;
        } else if (type.isPrimitive()) {
            base = OP_ILOAD_0;
            width = OP_ILOAD;
        } else {
            base = OP_ALOAD_0;
            width = OP_ALOAD;
        }
        int delta = ProxyGenerator.slotsOf(type);
        if (slot <= 3) {
            c.op(base + slot, delta);
        } else {
            // No `wide` form: not even javac emits more than 255 parameter slots, because a method
            // descriptor does not admit them either (JVMS 4.3.3).
            c.b.u1(width);
            c.b.u1(slot);
            c.shift(delta);
        }
    }

    /** It wraps the primitive left on top of the stack; a non-primitive is ready already. */
    private void boxArg(Code c, Class<?> type) {
        String box = ProxyGenerator.wrapperOf(type);
        if (box == null) {
            return;
        }
        String signature = "(" + ProxyGenerator.descriptor(type) + ")L" + box + ";";
        c.b.u1(OP_INVOKESTATIC);
        c.b.u2(this.methodRef(box, "valueOf", signature));
        c.shift(1 - ProxyGenerator.slotsOf(type));
    }

    /**
     * It closes the method with the return its type calls for.
     *
     * <p>This is where two clauses of the contract fall out by themselves: over `null` the
     * `checkcast` passes and the unboxing throws `NullPointerException`; over a value of another
     * type the `checkcast` throws `ClassCastException`. Neither is written anywhere.
     */
    private void returnOpcode(Code c, Class<?> returnType) {
        if (returnType == Void.TYPE) {
            // Whatever the handler returned is discarded, even if it is not `null`.
            c.op(OP_POP, -1);
            c.op(OP_RETURN, 0);
            return;
        }
        String box = ProxyGenerator.wrapperOf(returnType);
        if (box == null) {
            c.b.u1(OP_CHECKCAST);
            c.b.u2(this.clazz(ProxyGenerator.internalName(returnType)));
            c.shift(0);
            c.op(OP_ARETURN, -1);
            return;
        }
        c.b.u1(OP_CHECKCAST);
        c.b.u2(this.clazz(box));
        c.shift(0);
        String name = ProxyGenerator.unboxerOf(returnType);
        c.b.u1(OP_INVOKEVIRTUAL);
        c.b.u2(this.methodRef(box, name, "()" + ProxyGenerator.descriptor(returnType)));
        c.shift(ProxyGenerator.slotsOf(returnType) - 1);
        if (returnType == Long.TYPE) {
            c.op(OP_LRETURN, -2);
        } else if (returnType == Float.TYPE) {
            c.op(OP_FRETURN, -1);
        } else if (returnType == Double.TYPE) {
            c.op(OP_DRETURN, -2);
        } else {
            c.op(OP_IRETURN, -1);
        }
    }

    /**
     * A whole `method_info`: the header, the `Code` and -- if the method declares any -- the
     * `Exceptions` attribute.
     *
     * <p>Neither the verifier nor the interpreter reads the `Exceptions`: it is so
     * `getDeclaredMethods()` over the generated class tells the truth about what its methods
     * declare. A proxy that lies about that is a proxy that does not stand in for the interface.
     */
    private byte[] methodInfo(int access, String name, String descriptor, byte[] code,
            int maxStack, int maxLocals, Class<?>[] exceptions) {
        int exceptionCount = exceptions == null ? 0 : exceptions.length;
        Buf b = new Buf();
        b.u2(access);
        b.u2(this.utf(name));
        b.u2(this.utf(descriptor));
        b.u2(exceptionCount > 0 ? 2 : 1);

        b.u2(this.utf("Code"));
        // max_stack + max_locals + code_length + the code + exception_table_length +
        // attributes_count = 2 + 2 + 4 + n + 2 + 2.
        b.u4(12 + code.length);
        b.u2(maxStack);
        b.u2(maxLocals);
        b.u4(code.length);
        b.raw(code);
        b.u2(0); // no handlers: the dispatcher translates exceptions, in Java
        b.u2(0); // no LineNumberTable and no StackMapTable

        if (exceptionCount > 0) {
            b.u2(this.utf("Exceptions"));
            b.u4(2 + 2 * exceptionCount);
            b.u2(exceptionCount);
            int i = 0;
            while (i < exceptionCount) {
                b.u2(this.clazz(ProxyGenerator.internalName(exceptions[i])));
                i = i + 1;
            }
        }
        return b.bytes();
    }
}
