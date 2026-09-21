package jdk.internal.classfile.impl;

import java.lang.classfile.ClassSignature;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.Signature.ArrayTypeSig;
import java.lang.classfile.Signature.BaseTypeSig;
import java.lang.classfile.Signature.ClassTypeSig;
import java.lang.classfile.Signature.RefTypeSig;
import java.lang.classfile.Signature.ThrowableSig;
import java.lang.classfile.Signature.TypeArg;
import java.lang.classfile.Signature.TypeArg.Bounded;
import java.lang.classfile.Signature.TypeArg.Bounded.WildcardIndicator;
import java.lang.classfile.Signature.TypeArg.Unbounded;
import java.lang.classfile.Signature.TypeParam;
import java.lang.classfile.Signature.TypeVarSig;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// The generic signatures of JVMS §4.7.9.1: the parser, the printer and the nodes of the tree.
//
// It is all in one class with nested classes and not in ten files for a practical reason: the nodes
// build one another and from here the constructor is written unqualified. The note said a
// `new Signatures.ClassTypeSigImpl(...)` from outside is exactly the form this project's compiler
// does not resolve ("bug #356"), so there is none. That bug is in no findings file, and the frozen
// javac compiles that form (checked 2026-09-18); the single-file layout stands on the first reason.
//
// The parser is strict in the only way that serves: it consumes the whole string and fails with
// `IllegalArgumentException` at the first character the grammar does not admit, including what is
// left over at the end. A signature is a datum that comes from the file; accepting it halfway is
// inventing a type.
public final class Signatures {

    private Signatures() {
    }

    // ------------------------------------------------------------------ nodes

    /** A primitive, or `V` in the result position. */
    public static final class BaseTypeSigImpl implements BaseTypeSig {

        private final char letter;

        BaseTypeSigImpl(char letter) {
            this.letter = letter;
        }

        public char baseType() {
            return this.letter;
        }

        public String signatureString() {
            return String.valueOf(this.letter);
        }

        public boolean equals(Object o) {
            return o instanceof BaseTypeSigImpl && ((BaseTypeSigImpl) o).letter == this.letter;
        }

        public int hashCode() {
            return this.letter;
        }

        public String toString() {
            return signatureString();
        }
    }

    /** A class or interface with its type arguments. */
    public static final class ClassTypeSigImpl implements ClassTypeSig {

        private final ClassTypeSig outer;
        private final String name;
        private final List<TypeArg> args;

        ClassTypeSigImpl(ClassTypeSig outer, String name, List<TypeArg> args) {
            this.outer = outer;
            this.name = name;
            this.args = args;
        }

        public Optional<ClassTypeSig> outerType() {
            return Optional.ofNullable(this.outer);
        }

        public String className() {
            return this.name;
        }

        public List<TypeArg> typeArgs() {
            return this.args;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            if (this.outer == null) {
                sb.append('L').append(this.name);
            } else {
                String s = this.outer.signatureString();
                // The outer one's `;` is replaced by the `.` that opens the nested one.
                sb.append(s, 0, s.length() - 1).append('.').append(this.name);
            }
            writeArgs(sb, this.args);
            return sb.append(';').toString();
        }

        public boolean equals(Object o) {
            if (!(o instanceof ClassTypeSigImpl)) {
                return false;
            }
            return signatureString().equals(((ClassTypeSigImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** A one-dimensional array over its component. */
    public static final class ArrayTypeSigImpl implements ArrayTypeSig {

        private final Signature component;

        ArrayTypeSigImpl(Signature component) {
            this.component = component;
        }

        public Signature componentSignature() {
            return this.component;
        }

        public String signatureString() {
            return "[" + this.component.signatureString();
        }

        public boolean equals(Object o) {
            return o instanceof ArrayTypeSigImpl
                    && signatureString().equals(((ArrayTypeSigImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** A type variable. */
    public static final class TypeVarSigImpl implements TypeVarSig {

        private final String id;

        TypeVarSigImpl(String id) {
            this.id = id;
        }

        public String identifier() {
            return this.id;
        }

        public String signatureString() {
            return "T" + this.id + ";";
        }

        public boolean equals(Object o) {
            return o instanceof TypeVarSigImpl && this.id.equals(((TypeVarSigImpl) o).id);
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** The declaration of a type variable with its bounds. */
    public static final class TypeParamImpl implements TypeParam {

        private final String id;
        private final RefTypeSig classBound;
        private final List<RefTypeSig> interfaceBounds;

        TypeParamImpl(String id, RefTypeSig classBound, List<RefTypeSig> interfaceBounds) {
            this.id = id;
            this.classBound = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        public String identifier() {
            return this.id;
        }

        public Optional<RefTypeSig> classBound() {
            return Optional.ofNullable(this.classBound);
        }

        public List<RefTypeSig> interfaceBounds() {
            return this.interfaceBounds;
        }

        /** The text of the declaration, that is `id : bound { : bound }`. */
        public String declaration() {
            StringBuilder sb = new StringBuilder(this.id);
            sb.append(':');
            if (this.classBound != null) {
                sb.append(this.classBound.signatureString());
            }
            for (int i = 0; i < this.interfaceBounds.size(); i++) {
                sb.append(':').append(this.interfaceBounds.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof TypeParamImpl
                    && declaration().equals(((TypeParamImpl) o).declaration());
        }

        public int hashCode() {
            return declaration().hashCode();
        }

        public String toString() {
            return declaration();
        }
    }

    /** A type argument that names a type, with or without a wildcard. */
    public static final class TypeArgBoundedImpl implements Bounded {

        private final WildcardIndicator unbounded;
        private final RefTypeSig type;

        TypeArgBoundedImpl(WildcardIndicator unbounded, RefTypeSig type) {
            this.unbounded = unbounded;
            this.type = type;
        }

        public WildcardIndicator wildcardIndicator() {
            return this.unbounded;
        }

        public RefTypeSig boundType() {
            return this.type;
        }

        public boolean equals(Object o) {
            return o instanceof TypeArgBoundedImpl && toString().equals(o.toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public String toString() {
            return argText(this);
        }
    }

    /** The `*` argument. */
    public static final class TypeArgUnboundedImpl implements Unbounded {

        static final TypeArgUnboundedImpl INSTANCE = new TypeArgUnboundedImpl();

        private TypeArgUnboundedImpl() {
        }

        public String toString() {
            return "*";
        }
    }

    /** The signature of a class. */
    public static final class ClassSignatureImpl implements ClassSignature {

        private final List<TypeParam> typeParameters;
        private final ClassTypeSig superClass;
        private final List<ClassTypeSig> interfaces;

        ClassSignatureImpl(List<TypeParam> typeParameters, ClassTypeSig superClass,
                List<ClassTypeSig> interfaces) {
            this.typeParameters = typeParameters;
            this.superClass = superClass;
            this.interfaces = interfaces;
        }

        public List<TypeParam> typeParameters() {
            return this.typeParameters;
        }

        public ClassTypeSig superclassSignature() {
            return this.superClass;
        }

        public List<ClassTypeSig> superinterfaceSignatures() {
            return this.interfaces;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            writeParameters(sb, this.typeParameters);
            sb.append(this.superClass.signatureString());
            for (int i = 0; i < this.interfaces.size(); i++) {
                sb.append(this.interfaces.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof ClassSignatureImpl
                    && signatureString().equals(((ClassSignatureImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** The signature of a method. */
    public static final class MethodSignatureImpl implements MethodSignature {

        private final List<TypeParam> typeParameters;
        private final List<ThrowableSig> thrown;
        private final Signature result;
        private final List<Signature> arguments;

        MethodSignatureImpl(List<TypeParam> typeParameters, List<ThrowableSig> thrown,
                Signature result, List<Signature> arguments) {
            this.typeParameters = typeParameters;
            this.thrown = thrown;
            this.result = result;
            this.arguments = arguments;
        }

        public List<TypeParam> typeParameters() {
            return this.typeParameters;
        }

        public List<Signature> arguments() {
            return this.arguments;
        }

        public Signature result() {
            return this.result;
        }

        public List<ThrowableSig> throwableSignatures() {
            return this.thrown;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            writeParameters(sb, this.typeParameters);
            sb.append('(');
            for (int i = 0; i < this.arguments.size(); i++) {
                sb.append(this.arguments.get(i).signatureString());
            }
            sb.append(')').append(this.result.signatureString());
            for (int i = 0; i < this.thrown.size(); i++) {
                sb.append('^').append(this.thrown.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof MethodSignatureImpl
                    && signatureString().equals(((MethodSignatureImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    // ------------------------------------------------------------- printing

    static String argText(TypeArg a) {
        if (a instanceof Unbounded) {
            return "*";
        }
        Bounded b = (Bounded) a;
        WildcardIndicator w = b.wildcardIndicator();
        String body = b.boundType().signatureString();
        if (w == WildcardIndicator.EXTENDS) {
            return "+" + body;
        }
        if (w == WildcardIndicator.SUPER) {
            return "-" + body;
        }
        return body;
    }

    static void writeArgs(StringBuilder sb, List<TypeArg> args) {
        if (args.isEmpty()) {
            return;
        }
        sb.append('<');
        for (int i = 0; i < args.size(); i++) {
            sb.append(argText(args.get(i)));
        }
        sb.append('>');
    }

    static void writeParameters(StringBuilder sb, List<TypeParam> ps) {
        if (ps.isEmpty()) {
            return;
        }
        sb.append('<');
        for (int i = 0; i < ps.size(); i++) {
            TypeParam p = ps.get(i);
            sb.append(p.identifier()).append(':');
            Optional<RefTypeSig> bound = p.classBound();
            if (bound.isPresent()) {
                sb.append(bound.get().signatureString());
            }
            List<RefTypeSig> ifs = p.interfaceBounds();
            for (int j = 0; j < ifs.size(); j++) {
                sb.append(':').append(ifs.get(j).signatureString());
            }
        }
        sb.append('>');
    }

    // ------------------------------------------------------------- factories

    private static final BaseTypeSigImpl[] PRIMITIVES = new BaseTypeSigImpl[128];

    /** The node of the primitive whose letter is `letter`. */
    public static BaseTypeSig baseTypeSig(char letter) {
        if ("BCDFIJSZV".indexOf(letter) < 0) {
            throw new IllegalArgumentException("not a base type letter: " + letter);
        }
        synchronized (PRIMITIVES) {
            BaseTypeSigImpl b = PRIMITIVES[letter];
            if (b == null) {
                b = new BaseTypeSigImpl(letter);
                PRIMITIVES[letter] = b;
            }
            return b;
        }
    }

    /** The node of the class `name` (internal name, or simple if nested) with these arguments. */
    public static ClassTypeSig classTypeSig(ClassTypeSig outer, String name,
            TypeArg... args) {
        requireClassName(name);
        List<TypeArg> list = new ArrayList<TypeArg>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    throw new NullPointerException("typeArgs[" + i + "]");
                }
                list.add(args[i]);
            }
        }
        return new ClassTypeSigImpl(outer, name, Collections.unmodifiableList(list));
    }

    /** The node of the array of `dims` dimensions over `component`. */
    public static ArrayTypeSig arrayTypeSig(int dims, Signature component) {
        if (component == null) {
            throw new NullPointerException("componentSignature");
        }
        if (dims < 1 || dims > 255) {
            throw new IllegalArgumentException("dimensions out of range: " + dims);
        }
        if (component instanceof BaseTypeSig && ((BaseTypeSig) component).baseType() == 'V') {
            throw new IllegalArgumentException("there are no arrays of void");
        }
        Signature s = component;
        for (int i = 0; i < dims; i++) {
            s = new ArrayTypeSigImpl(s);
        }
        return (ArrayTypeSig) s;
    }

    /** The node of the type variable `id`. */
    public static TypeVarSig typeVarSig(String id) {
        requireIdentifier(id);
        return new TypeVarSigImpl(id);
    }

    /** The declaration of the variable `id` with these bounds. */
    public static TypeParam typeParam(String id, Optional<RefTypeSig> classBound,
            RefTypeSig... interfaceBounds) {
        requireIdentifier(id);
        if (classBound == null) {
            throw new NullPointerException("classBound");
        }
        List<RefTypeSig> ifs = new ArrayList<RefTypeSig>();
        if (interfaceBounds != null) {
            for (int i = 0; i < interfaceBounds.length; i++) {
                if (interfaceBounds[i] == null) {
                    throw new NullPointerException("interfaceBounds[" + i + "]");
                }
                ifs.add(interfaceBounds[i]);
            }
        }
        return new TypeParamImpl(id, classBound.orElse(null),
                Collections.unmodifiableList(ifs));
    }

    /** The `*` argument. */
    public static Unbounded unbounded() {
        return TypeArgUnboundedImpl.INSTANCE;
    }

    /** The argument with this wildcard over this type. */
    public static Bounded bounded(WildcardIndicator unbounded, RefTypeSig type) {
        if (unbounded == null) {
            throw new NullPointerException("wildcardIndicator");
        }
        if (type == null) {
            throw new NullPointerException("boundType");
        }
        return new TypeArgBoundedImpl(unbounded, type);
    }

    /** The non-generic signature of the type `desc` describes. */
    public static Signature ofDescriptor(ClassDesc desc) {
        if (desc == null) {
            throw new NullPointerException("classDesc");
        }
        if (desc.isPrimitive()) {
            return baseTypeSig(desc.descriptorString().charAt(0));
        }
        if (desc.isArray()) {
            return new ArrayTypeSigImpl(ofDescriptor(desc.componentType()));
        }
        String d = desc.descriptorString();
        return classTypeSig(null, d.substring(1, d.length() - 1));
    }

    /** The class signature with these parameters (null = none), superclass and interfaces. */
    public static ClassSignature classSignature(List<TypeParam> typeParameters,
            ClassTypeSig superClass, ClassTypeSig... interfaces) {
        if (superClass == null) {
            throw new NullPointerException("superclassSignature");
        }
        List<ClassTypeSig> ifs = new ArrayList<ClassTypeSig>();
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i] == null) {
                    throw new NullPointerException("superinterfaceSignatures[" + i + "]");
                }
                ifs.add(interfaces[i]);
            }
        }
        return new ClassSignatureImpl(freezeParameters(typeParameters), superClass,
                Collections.unmodifiableList(ifs));
    }

    /** The method signature with these parts; `typeParameters` and `thrown` may be null. */
    public static MethodSignature methodSignature(List<TypeParam> typeParameters,
            List<ThrowableSig> thrown, Signature result, Signature... arguments) {
        if (result == null) {
            throw new NullPointerException("result");
        }
        List<Signature> args = new ArrayList<Signature>();
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] == null) {
                    throw new NullPointerException("arguments[" + i + "]");
                }
                args.add(arguments[i]);
            }
        }
        List<ThrowableSig> ts = new ArrayList<ThrowableSig>();
        if (thrown != null) {
            for (int i = 0; i < thrown.size(); i++) {
                ThrowableSig t = thrown.get(i);
                if (t == null) {
                    throw new NullPointerException("exceptions[" + i + "]");
                }
                ts.add(t);
            }
        }
        return new MethodSignatureImpl(freezeParameters(typeParameters),
                Collections.unmodifiableList(ts), result,
                Collections.unmodifiableList(args));
    }

    /** The non-generic signature of the method `desc` describes. */
    public static MethodSignature methodSignatureOf(MethodTypeDesc desc) {
        if (desc == null) {
            throw new NullPointerException("descriptor");
        }
        List<Signature> args = new ArrayList<Signature>();
        for (int i = 0; i < desc.parameterCount(); i++) {
            args.add(ofDescriptor(desc.parameterType(i)));
        }
        return new MethodSignatureImpl(Collections.<TypeParam>emptyList(),
                Collections.<ThrowableSig>emptyList(), ofDescriptor(desc.returnType()),
                Collections.unmodifiableList(args));
    }

    private static List<TypeParam> freezeParameters(List<TypeParam> ps) {
        List<TypeParam> list = new ArrayList<TypeParam>();
        if (ps != null) {
            for (int i = 0; i < ps.size(); i++) {
                TypeParam p = ps.get(i);
                if (p == null) {
                    throw new NullPointerException("typeParameters[" + i + "]");
                }
                list.add(p);
            }
        }
        return Collections.unmodifiableList(list);
    }

    // -------------------------------------------------------------- parsing

    /** The cursor over the text of a signature. */
    static final class Parser {

        final String s;
        int i;

        Parser(String s) {
            if (s == null) {
                throw new NullPointerException("signature");
            }
            this.s = s;
            this.i = 0;
        }

        boolean hasMore() {
            return this.i < this.s.length();
        }

        char peek() {
            if (!hasMore()) {
                throw error("the signature ended");
            }
            return this.s.charAt(this.i);
        }

        char take() {
            char c = peek();
            this.i++;
            return c;
        }

        void require(char c) {
            char v = take();
            if (v != c) {
                this.i--;
                throw error("expected '" + c + "' and found '" + v + "'");
            }
        }

        IllegalArgumentException error(String what) {
            return new IllegalArgumentException(
                    what + " at position " + this.i + " of: " + this.s);
        }

        /** It reads up to the first of `stops`, without consuming it. */
        String identifierUntil(String stops) {
            int from = this.i;
            while (hasMore() && stops.indexOf(this.s.charAt(this.i)) < 0) {
                this.i++;
            }
            if (this.i == from) {
                throw error("empty identifier");
            }
            return this.s.substring(from, this.i);
        }

        Signature type() {
            char c = peek();
            // The `V` comes in here and not only in the result position: it is what the JDK does,
            // and there is a differential test that pins it.
            if ("BCDFIJSZV".indexOf(c) >= 0) {
                this.i++;
                return baseTypeSig(c);
            }
            return referenceKind();
        }

        RefTypeSig referenceKind() {
            char c = peek();
            if (c == 'L') {
                return classSig();
            }
            if (c == 'T') {
                this.i++;
                String id = identifierUntil(";");
                require(';');
                return new TypeVarSigImpl(id);
            }
            if (c == '[') {
                this.i++;
                return new ArrayTypeSigImpl(type());
            }
            throw error("not the start of a reference type: '" + c + "'");
        }

        ClassTypeSig classSig() {
            require('L');
            ClassTypeSig current = null;
            while (true) {
                String name = identifierUntil("<;.");
                List<TypeArg> args = arguments();
                current = new ClassTypeSigImpl(current, name,
                        Collections.unmodifiableList(args));
                char c = take();
                if (c == ';') {
                    return current;
                }
                if (c != '.') {
                    this.i--;
                    throw error("expected '.' or ';' and found '" + c + "'");
                }
            }
        }

        List<TypeArg> arguments() {
            List<TypeArg> args = new ArrayList<TypeArg>();
            if (!hasMore() || peek() != '<') {
                return args;
            }
            this.i++;
            if (peek() == '>') {
                throw error("empty type argument list");
            }
            while (peek() != '>') {
                char c = peek();
                if (c == '*') {
                    this.i++;
                    args.add(TypeArgUnboundedImpl.INSTANCE);
                } else if (c == '+') {
                    this.i++;
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.EXTENDS,
                            referenceKind()));
                } else if (c == '-') {
                    this.i++;
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.SUPER,
                            referenceKind()));
                } else {
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.NONE,
                            referenceKind()));
                }
            }
            this.i++;
            return args;
        }

        List<TypeParam> typeParameters() {
            List<TypeParam> ps = new ArrayList<TypeParam>();
            if (!hasMore() || peek() != '<') {
                return ps;
            }
            this.i++;
            if (peek() == '>') {
                throw error("empty type parameter list");
            }
            while (peek() != '>') {
                String id = identifierUntil(":");
                require(':');
                RefTypeSig classBound = null;
                if (peek() != ':') {
                    classBound = referenceKind();
                }
                List<RefTypeSig> ifs = new ArrayList<RefTypeSig>();
                while (hasMore() && peek() == ':') {
                    this.i++;
                    ifs.add(referenceKind());
                }
                ps.add(new TypeParamImpl(id, classBound, Collections.unmodifiableList(ifs)));
            }
            this.i++;
            return ps;
        }

        void requireEnd() {
            if (hasMore()) {
                throw error("text left over after the signature");
            }
        }
    }

    /** It parses a complete type signature. */
    public static Signature parseType(String text) {
        Parser a = new Parser(text);
        Signature s = a.type();
        a.requireEnd();
        return s;
    }

    /** It parses a complete class signature. */
    public static ClassSignature parseClassSignature(String text) {
        Parser a = new Parser(text);
        List<TypeParam> ps = a.typeParameters();
        ClassTypeSig sup = a.classSig();
        List<ClassTypeSig> ifs = new ArrayList<ClassTypeSig>();
        while (a.hasMore()) {
            ifs.add(a.classSig());
        }
        return new ClassSignatureImpl(Collections.unmodifiableList(ps), sup,
                Collections.unmodifiableList(ifs));
    }

    /** It parses a complete method signature. */
    public static MethodSignature parseMethodSignature(String text) {
        Parser a = new Parser(text);
        List<TypeParam> ps = a.typeParameters();
        a.require('(');
        List<Signature> args = new ArrayList<Signature>();
        while (a.peek() != ')') {
            args.add(a.type());
        }
        a.require(')');
        Signature result = a.type();
        List<ThrowableSig> thrown = new ArrayList<ThrowableSig>();
        while (a.hasMore()) {
            a.require('^');
            char c = a.peek();
            if (c == 'L') {
                thrown.add(a.classSig());
            } else if (c == 'T') {
                a.take();
                String id = a.identifierUntil(";");
                a.require(';');
                thrown.add(new TypeVarSigImpl(id));
            } else {
                throw a.error("a `throws` admits only a class or a type variable");
            }
        }
        return new MethodSignatureImpl(Collections.unmodifiableList(ps),
                Collections.unmodifiableList(thrown), result,
                Collections.unmodifiableList(args));
    }

    // ------------------------------------------------------------ validation

    private static void requireIdentifier(String id) {
        if (id == null) {
            throw new NullPointerException("identifier");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("empty identifier");
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (".;[/<>:".indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "an identifier cannot contain '" + c + "': " + id);
            }
        }
    }

    private static void requireClassName(String name) {
        if (name == null) {
            throw new NullPointerException("className");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty class name");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (".;[<>:".indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "an internal name cannot contain '" + c + "': " + name);
            }
        }
    }
}
