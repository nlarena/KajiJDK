package jdk.internal.classfile.impl;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.ClassReader;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.CatchTarget;
import java.lang.classfile.TypeAnnotation.EmptyTarget;
import java.lang.classfile.TypeAnnotation.FormalParameterTarget;
import java.lang.classfile.TypeAnnotation.LocalVarTarget;
import java.lang.classfile.TypeAnnotation.LocalVarTargetInfo;
import java.lang.classfile.TypeAnnotation.OffsetTarget;
import java.lang.classfile.TypeAnnotation.SupertypeTarget;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.ThrowsTarget;
import java.lang.classfile.TypeAnnotation.TypeArgumentTarget;
import java.lang.classfile.TypeAnnotation.TypeParameterBoundTarget;
import java.lang.classfile.TypeAnnotation.TypeParameterTarget;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Las implementaciones de `Annotation`, `AnnotationElement`, `AnnotationValue` y `TypeAnnotation`,
// más el lector que las saca de un `.class` (JVMS §4.7.16 y §4.7.20) y el escritor que las vuelve a
// poner.
//
// Las entradas de pool que hacen falta cuando la fábrica pública recibe un `String` o un `int` en
// vez de una entrada salen de {@link PoolTemporal}; ahí está explicado por qué y qué implica.
public final class Annotations {

    private Annotations() {
    }

    /** Un `Utf8` suelto con este texto. */
    public static Utf8Entry utf8(String s) {
        return TemporaryConstantPool.utf8(s);
    }

    /** Un `CONSTANT_Integer` suelto. */
    public static IntegerEntry intEntry(int v) {
        return TemporaryConstantPool.intEntry(v);
    }

    /** Un `CONSTANT_Long` suelto. */
    public static LongEntry longEntry(long v) {
        return TemporaryConstantPool.longEntry(v);
    }

    /** Un `CONSTANT_Float` suelto. */
    public static FloatEntry floatEntry(float v) {
        return TemporaryConstantPool.floatEntry(v);
    }

    /** Un `CONSTANT_Double` suelto. */
    public static DoubleEntry doubleEntry(double v) {
        return TemporaryConstantPool.doubleEntry(v);
    }

    /** Una lista inmutable con estos elementos. */
    public static List<AnnotationElement> listOf(AnnotationElement[] elems) {
        List<AnnotationElement> list = new ArrayList<AnnotationElement>();
        for (int i = 0; i < elems.length; i++) {
            list.add(elems[i]);
        }
        return Collections.unmodifiableList(list);
    }

    // --- Annotation / AnnotationElement ---

    public static Annotation annotation(Utf8Entry className, List<AnnotationElement> elements) {
        return new AnnotationImpl(className, copyOf(elements));
    }

    public static AnnotationElement element(Utf8Entry name, AnnotationValue value) {
        return new ElementImpl(name, value);
    }

    // --- AnnotationValue ---

    public static AnnotationValue.OfEnum ofEnum(Utf8Entry className, Utf8Entry constantName) {
        return new EnumValue(className, constantName);
    }

    public static AnnotationValue.OfClass ofClass(Utf8Entry className) {
        return new ClassLiteralValue(className);
    }

    public static AnnotationValue.OfString ofString(Utf8Entry v) {
        return new StringValue(v);
    }

    public static AnnotationValue.OfDouble ofDouble(DoubleEntry v) {
        return new DoubleValue(v);
    }

    public static AnnotationValue.OfFloat ofFloat(FloatEntry v) {
        return new FloatValue(v);
    }

    public static AnnotationValue.OfLong ofLong(LongEntry v) {
        return new LongValue(v);
    }

    public static AnnotationValue.OfInt ofInt(IntegerEntry v) {
        return new IntValue(v);
    }

    public static AnnotationValue.OfShort ofShort(IntegerEntry v) {
        return new ShortValue(v);
    }

    public static AnnotationValue.OfChar ofChar(IntegerEntry v) {
        return new CharValue(v);
    }

    public static AnnotationValue.OfByte ofByte(IntegerEntry v) {
        return new ByteValue(v);
    }

    public static AnnotationValue.OfBoolean ofBoolean(IntegerEntry v) {
        return new BooleanValue(v);
    }

    public static AnnotationValue.OfAnnotation ofAnnotation(Annotation v) {
        return new NestedAnnotationValue(v);
    }

    public static AnnotationValue.OfArray ofArray(AnnotationValue[] v) {
        List<AnnotationValue> list = new ArrayList<AnnotationValue>();
        for (int i = 0; i < v.length; i++) {
            list.add(v[i]);
        }
        return new ArrayValue(Collections.unmodifiableList(list));
    }

    public static AnnotationValue.OfArray ofArrayOfList(List<AnnotationValue> v) {
        List<AnnotationValue> list = new ArrayList<AnnotationValue>();
        for (int i = 0; i < v.size(); i++) {
            list.add(v.get(i));
        }
        return new ArrayValue(Collections.unmodifiableList(list));
    }

    /**
     * El `element_value` que corresponde a un objeto de Java. Las cajas, `String` y `ClassDesc`
     * salen directo; un arreglo se convierte elemento por elemento. Cualquier otra cosa es un
     * error, y tiene que serlo: `element_value` no tiene forma de guardarla.
     */
    public static AnnotationValue ofObject(Object v) {
        if (v instanceof Integer) {
            return AnnotationValue.ofInt(((Integer) v).intValue());
        }
        if (v instanceof Long) {
            return AnnotationValue.ofLong(((Long) v).longValue());
        }
        if (v instanceof Short) {
            return AnnotationValue.ofShort(((Short) v).shortValue());
        }
        if (v instanceof Byte) {
            return AnnotationValue.ofByte(((Byte) v).byteValue());
        }
        if (v instanceof Character) {
            return AnnotationValue.ofChar(((Character) v).charValue());
        }
        if (v instanceof Boolean) {
            return AnnotationValue.ofBoolean(((Boolean) v).booleanValue());
        }
        if (v instanceof Float) {
            return AnnotationValue.ofFloat(((Float) v).floatValue());
        }
        if (v instanceof Double) {
            return AnnotationValue.ofDouble(((Double) v).doubleValue());
        }
        if (v instanceof String) {
            return AnnotationValue.ofString((String) v);
        }
        if (v instanceof ClassDesc) {
            return AnnotationValue.ofClass((ClassDesc) v);
        }
        if (v instanceof AnnotationValue) {
            return (AnnotationValue) v;
        }
        if (v instanceof Annotation) {
            return AnnotationValue.ofAnnotation((Annotation) v);
        }
        if (v != null && v.getClass().isArray()) {
            Object[] items = (Object[]) v;
            AnnotationValue[] values = new AnnotationValue[items.length];
            for (int i = 0; i < items.length; i++) {
                values[i] = ofObject(items[i]);
            }
            return AnnotationValue.ofArray(values);
        }
        throw new IllegalArgumentException(
                "no hay element_value para " + (v == null ? "null" : v.getClass().getName()));
    }

    // --- TypeAnnotation ---

    public static TypeAnnotation typeAnnotationOf(TargetInfo target,
            List<TypePathComponent> path, Annotation annotation) {
        List<TypePathComponent> list = new ArrayList<TypePathComponent>();
        for (int i = 0; i < path.size(); i++) {
            list.add(path.get(i));
        }
        return new TypeAnnotationImpl(target, Collections.unmodifiableList(list), annotation);
    }

    public static TypeParameterTarget typeParameterTarget(TargetType t, int i) {
        require(t, TargetType.CLASS_TYPE_PARAMETER, TargetType.METHOD_TYPE_PARAMETER);
        return new TypeParameterTargetImpl(t, i);
    }

    public static SupertypeTarget supertypeTarget(int i) {
        return new SupertypeTargetImpl(i);
    }

    public static TypeParameterBoundTarget typeParameterBoundTarget(TargetType t, int i, int j) {
        require(t, TargetType.CLASS_TYPE_PARAMETER_BOUND, TargetType.METHOD_TYPE_PARAMETER_BOUND);
        return new TypeParameterBoundTargetImpl(t, i, j);
    }

    public static EmptyTarget emptyTarget(TargetType t) {
        if (t != TargetType.FIELD && t != TargetType.METHOD_RETURN
                && t != TargetType.METHOD_RECEIVER) {
            throw new IllegalArgumentException(t + " no es un empty_target");
        }
        return new EmptyTargetImpl(t);
    }

    public static FormalParameterTarget formalParameterTarget(int i) {
        return new FormalParameterTargetImpl(i);
    }

    public static ThrowsTarget throwsTarget(int i) {
        return new ThrowsTargetImpl(i);
    }

    public static LocalVarTarget localVarTarget(TargetType t, List<LocalVarTargetInfo> table) {
        require(t, TargetType.LOCAL_VARIABLE, TargetType.RESOURCE_VARIABLE);
        List<LocalVarTargetInfo> list = new ArrayList<LocalVarTargetInfo>();
        for (int i = 0; i < table.size(); i++) {
            list.add(table.get(i));
        }
        return new LocalVarTargetImpl(t, Collections.unmodifiableList(list));
    }

    public static LocalVarTargetInfo localVarTargetInfo(Label start, Label end, int slot) {
        return new LocalVarTargetInfoImpl(start, end, slot);
    }

    public static CatchTarget catchTarget(int i) {
        return new CatchTargetImpl(i);
    }

    public static OffsetTarget offsetTarget(TargetType t, Label target) {
        if (t != TargetType.INSTANCEOF && t != TargetType.NEW
                && t != TargetType.CONSTRUCTOR_REFERENCE && t != TargetType.METHOD_REFERENCE) {
            throw new IllegalArgumentException(t + " no es un offset_target");
        }
        return new OffsetTargetImpl(t, target);
    }

    public static TypeArgumentTarget typeArgumentTarget(TargetType t, Label target, int i) {
        if (t != TargetType.CAST && t != TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                && t != TargetType.METHOD_INVOCATION_TYPE_ARGUMENT
                && t != TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                && t != TargetType.METHOD_REFERENCE_TYPE_ARGUMENT) {
            throw new IllegalArgumentException(t + " no es un type_argument_target");
        }
        return new TypeArgumentTargetImpl(t, target, i);
    }

    public static TypePathComponent typePathComponent(TypePathComponent.Kind k, int i) {
        return new TypePathComponentImpl(k, i);
    }

    private static void require(TargetType t, TargetType a, TargetType b) {
        if (t != a && t != b) {
            throw new IllegalArgumentException(
                    t + " no sirve acá; se esperaba " + a + " o " + b);
        }
    }

    private static List<AnnotationElement> copyOf(List<AnnotationElement> elems) {
        List<AnnotationElement> list = new ArrayList<AnnotationElement>();
        for (int i = 0; i < elems.size(); i++) {
            list.add(elems.get(i));
        }
        return Collections.unmodifiableList(list);
    }

    // --- Lectura ---

    /**
     * Lee un `annotation` que empieza en `c.p` y deja `c.p` justo después. El cursor explícito es
     * porque el largo de un `element_value` depende de su contenido: no hay forma de saltear una
     * anotación sin decodificarla entera.
     */
    public static Annotation readAnnotation(ClassReader cf, Cursor c) {
        Utf8Entry className = cf.readEntry(c.p, Utf8Entry.class);
        int n = cf.readU2(c.p + 2);
        c.p += 4;
        List<AnnotationElement> elems = new ArrayList<AnnotationElement>();
        for (int i = 0; i < n; i++) {
            Utf8Entry name = cf.readEntry(c.p, Utf8Entry.class);
            c.p += 2;
            elems.add(new ElementImpl(name, readValue(cf, c)));
        }
        return new AnnotationImpl(className, Collections.unmodifiableList(elems));
    }

    /** Lee un `element_value` que empieza en `c.p` y deja `c.p` justo después. */
    public static AnnotationValue readValue(ClassReader cf, Cursor c) {
        int tag = cf.readU1(c.p);
        c.p += 1;
        switch (tag) {
            case 'B': return new ByteValue(intEntry(cf, c));
            case 'C': return new CharValue(intEntry(cf, c));
            case 'S': return new ShortValue(intEntry(cf, c));
            case 'Z': return new BooleanValue(intEntry(cf, c));
            case 'I': return new IntValue(intEntry(cf, c));
            case 'J': {
                LongEntry e = cf.readEntry(c.p, LongEntry.class);
                c.p += 2;
                return new LongValue(e);
            }
            case 'F': {
                FloatEntry e = cf.readEntry(c.p, FloatEntry.class);
                c.p += 2;
                return new FloatValue(e);
            }
            case 'D': {
                DoubleEntry e = cf.readEntry(c.p, DoubleEntry.class);
                c.p += 2;
                return new DoubleValue(e);
            }
            case 's': {
                Utf8Entry e = cf.readEntry(c.p, Utf8Entry.class);
                c.p += 2;
                return new StringValue(e);
            }
            case 'c': {
                Utf8Entry e = cf.readEntry(c.p, Utf8Entry.class);
                c.p += 2;
                return new ClassLiteralValue(e);
            }
            case 'e': {
                Utf8Entry className = cf.readEntry(c.p, Utf8Entry.class);
                Utf8Entry name = cf.readEntry(c.p + 2, Utf8Entry.class);
                c.p += 4;
                return new EnumValue(className, name);
            }
            case '@':
                return new NestedAnnotationValue(readAnnotation(cf, c));
            case '[': {
                int n = cf.readU2(c.p);
                c.p += 2;
                List<AnnotationValue> values = new ArrayList<AnnotationValue>();
                for (int i = 0; i < n; i++) {
                    values.add(readValue(cf, c));
                }
                return new ArrayValue(Collections.unmodifiableList(values));
            }
            default:
                throw new IllegalArgumentException(
                        "etiqueta de element_value desconocida: 0x" + Integer.toHexString(tag));
        }
    }

    private static IntegerEntry intEntry(ClassReader cf, Cursor c) {
        IntegerEntry e = cf.readEntry(c.p, IntegerEntry.class);
        c.p += 2;
        return e;
    }

    /** Lee la lista de `annotation` de un `RuntimeXxxAnnotations` a partir del cuerpo. */
    public static List<Annotation> readAnnotations(ClassReader cf, int pos) {
        Cursor c = new Cursor();
        int n = cf.readU2(pos);
        c.p = pos + 2;
        List<Annotation> list = new ArrayList<Annotation>();
        for (int i = 0; i < n; i++) {
            list.add(readAnnotation(cf, c));
        }
        return Collections.unmodifiableList(list);
    }

    /** Lee un `type_annotation` que empieza en `c.p`. */
    public static TypeAnnotation readTypeAnnotation(ClassReader cf, Cursor c) {
        int tag = cf.readU1(c.p);
        c.p += 1;
        TargetType t = targetTypeOf(tag);
        TargetInfo target;
        // El reparto va por la etiqueta cruda y no por el `TargetType` porque nuestro javac todavía
        // no baja un `switch` con selector `enum` (finding #401), y porque el byte es justamente lo
        // que el formato discrimina. Las etiquetas van como literales y no como
        // `TargetInfo.TARGET_*` por el finding #461: una constante de otro archivo no se pliega en
        // un `case`. El comentario al lado de cada una dice cuál es.
        switch (tag) {
            case 0x00:  // TARGET_CLASS_TYPE_PARAMETER
            case 0x01:  // TARGET_METHOD_TYPE_PARAMETER
                target = new TypeParameterTargetImpl(t, cf.readU1(c.p));
                c.p += 1;
                break;
            case 0x10:  // TARGET_CLASS_EXTENDS
                target = new SupertypeTargetImpl(cf.readU2(c.p));
                c.p += 2;
                break;
            case 0x11:  // TARGET_CLASS_TYPE_PARAMETER_BOUND
            case 0x12:  // TARGET_METHOD_TYPE_PARAMETER_BOUND
                target = new TypeParameterBoundTargetImpl(t, cf.readU1(c.p), cf.readU1(c.p + 1));
                c.p += 2;
                break;
            case 0x13:  // TARGET_FIELD
            case 0x14:  // TARGET_METHOD_RETURN
            case 0x15:  // TARGET_METHOD_RECEIVER
                target = new EmptyTargetImpl(t);
                break;
            case 0x16:  // TARGET_METHOD_FORMAL_PARAMETER
                target = new FormalParameterTargetImpl(cf.readU1(c.p));
                c.p += 1;
                break;
            case 0x17:  // TARGET_THROWS
                target = new ThrowsTargetImpl(cf.readU2(c.p));
                c.p += 2;
                break;
            case 0x40:  // TARGET_LOCAL_VARIABLE
            case 0x41: {  // TARGET_RESOURCE_VARIABLE
                int n = cf.readU2(c.p);
                c.p += 2;
                List<LocalVarTargetInfo> rows = new ArrayList<LocalVarTargetInfo>();
                for (int i = 0; i < n; i++) {
                    int start = cf.readU2(c.p);
                    int length = cf.readU2(c.p + 2);
                    int slot = cf.readU2(c.p + 4);
                    rows.add(new LocalVarTargetInfoImpl(new LabelImpl(start),
                            new LabelImpl(start + length), slot));
                    c.p += 6;
                }
                target = new LocalVarTargetImpl(t, Collections.unmodifiableList(rows));
                break;
            }
            case 0x42:  // TARGET_EXCEPTION_PARAMETER
                target = new CatchTargetImpl(cf.readU2(c.p));
                c.p += 2;
                break;
            case 0x43:  // TARGET_INSTANCEOF
            case 0x44:  // TARGET_NEW
            case 0x45:  // TARGET_CONSTRUCTOR_REFERENCE
            case 0x46:  // TARGET_METHOD_REFERENCE
                target = new OffsetTargetImpl(t, new LabelImpl(cf.readU2(c.p)));
                c.p += 2;
                break;
            default:
                target = new TypeArgumentTargetImpl(t, new LabelImpl(cf.readU2(c.p)), cf.readU1(c.p + 2));
                c.p += 3;
                break;
        }
        int pathLength = cf.readU1(c.p);
        c.p += 1;
        List<TypePathComponent> path = new ArrayList<TypePathComponent>();
        for (int i = 0; i < pathLength; i++) {
            path.add(new TypePathComponentImpl(pathKindOf(cf.readU1(c.p)), cf.readU1(c.p + 1)));
            c.p += 2;
        }
        return new TypeAnnotationImpl(target, Collections.unmodifiableList(path),
                readAnnotation(cf, c));
    }

    /** Lee la lista de `type_annotation` de un `RuntimeXxxTypeAnnotations` a partir del cuerpo. */
    public static List<TypeAnnotation> readTypeAnnotations(ClassReader cf, int pos) {
        Cursor c = new Cursor();
        int n = cf.readU2(pos);
        c.p = pos + 2;
        List<TypeAnnotation> list = new ArrayList<TypeAnnotation>();
        for (int i = 0; i < n; i++) {
            list.add(readTypeAnnotation(cf, c));
        }
        return Collections.unmodifiableList(list);
    }

    private static TargetType targetTypeOf(int tag) {
        TargetType[] todos = TargetType.values();
        for (int i = 0; i < todos.length; i++) {
            if (todos[i].targetTypeValue() == tag) {
                return todos[i];
            }
        }
        throw new IllegalArgumentException(
                "target_type desconocido: 0x" + Integer.toHexString(tag));
    }

    private static TypePathComponent.Kind pathKindOf(int tag) {
        TypePathComponent.Kind[] todos = TypePathComponent.Kind.values();
        for (int i = 0; i < todos.length; i++) {
            if (todos[i].tag() == tag) {
                return todos[i];
            }
        }
        throw new IllegalArgumentException("type_path_kind desconocido: " + tag);
    }

    /** Un puntero de lectura que avanza. Ver `readAnnotation`. */
    public static final class Cursor {

        /** El offset actual dentro del archivo. */
        public int p;
    }
}

// --- Implementaciones ---

final class AnnotationImpl implements Annotation {

    private final Utf8Entry className;
    private final List<AnnotationElement> elements;

    AnnotationImpl(Utf8Entry className, List<AnnotationElement> elements) {
        this.className = className;
        this.elements = elements;
    }

    public Utf8Entry className() {
        return this.className;
    }

    public List<AnnotationElement> elements() {
        return this.elements;
    }

    public String toString() {
        return "Annotation[" + this.className.stringValue() + ", " + this.elements + "]";
    }
}

final class ElementImpl implements AnnotationElement {

    private final Utf8Entry name;
    private final AnnotationValue value;

    ElementImpl(Utf8Entry name, AnnotationValue value) {
        this.name = name;
        this.value = value;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public AnnotationValue value() {
        return this.value;
    }

    public String toString() {
        return this.name.stringValue() + "=" + this.value;
    }
}

final class StringValue implements AnnotationValue.OfString {

    private final Utf8Entry v;

    StringValue(Utf8Entry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_STRING;
    }

    public Utf8Entry constant() {
        return this.v;
    }

    public String stringValue() {
        return this.v.stringValue();
    }

    public String toString() {
        return "\"" + this.v.stringValue() + "\"";
    }
}

final class IntValue implements AnnotationValue.OfInt {

    private final IntegerEntry v;

    IntValue(IntegerEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_INT;
    }

    public IntegerEntry constant() {
        return this.v;
    }

    public int intValue() {
        return this.v.intValue();
    }

    public String toString() {
        return String.valueOf(this.v.intValue());
    }
}

final class ShortValue implements AnnotationValue.OfShort {

    private final IntegerEntry v;

    ShortValue(IntegerEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_SHORT;
    }

    public IntegerEntry constant() {
        return this.v;
    }

    public short shortValue() {
        return (short) this.v.intValue();
    }

    public String toString() {
        return String.valueOf((short) this.v.intValue());
    }
}

final class CharValue implements AnnotationValue.OfChar {

    private final IntegerEntry v;

    CharValue(IntegerEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_CHAR;
    }

    public IntegerEntry constant() {
        return this.v;
    }

    public char charValue() {
        return (char) this.v.intValue();
    }

    public String toString() {
        return "'" + ((char) this.v.intValue()) + "'";
    }
}

final class ByteValue implements AnnotationValue.OfByte {

    private final IntegerEntry v;

    ByteValue(IntegerEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_BYTE;
    }

    public IntegerEntry constant() {
        return this.v;
    }

    public byte byteValue() {
        return (byte) this.v.intValue();
    }

    public String toString() {
        return String.valueOf((byte) this.v.intValue());
    }
}

final class BooleanValue implements AnnotationValue.OfBoolean {

    private final IntegerEntry v;

    BooleanValue(IntegerEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_BOOLEAN;
    }

    public IntegerEntry constant() {
        return this.v;
    }

    public boolean booleanValue() {
        return this.v.intValue() != 0;
    }

    public String toString() {
        return String.valueOf(this.v.intValue() != 0);
    }
}

final class LongValue implements AnnotationValue.OfLong {

    private final LongEntry v;

    LongValue(LongEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_LONG;
    }

    public LongEntry constant() {
        return this.v;
    }

    public long longValue() {
        return this.v.longValue();
    }

    public String toString() {
        return this.v.longValue() + "L";
    }
}

final class FloatValue implements AnnotationValue.OfFloat {

    private final FloatEntry v;

    FloatValue(FloatEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_FLOAT;
    }

    public FloatEntry constant() {
        return this.v;
    }

    public float floatValue() {
        return this.v.floatValue();
    }

    public String toString() {
        return this.v.floatValue() + "f";
    }
}

final class DoubleValue implements AnnotationValue.OfDouble {

    private final DoubleEntry v;

    DoubleValue(DoubleEntry v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_DOUBLE;
    }

    public DoubleEntry constant() {
        return this.v;
    }

    public double doubleValue() {
        return this.v.doubleValue();
    }

    public String toString() {
        return String.valueOf(this.v.doubleValue());
    }
}

final class ClassLiteralValue implements AnnotationValue.OfClass {

    private final Utf8Entry className;

    ClassLiteralValue(Utf8Entry className) {
        this.className = className;
    }

    public int tag() {
        return AnnotationValue.TAG_CLASS;
    }

    public Utf8Entry className() {
        return this.className;
    }

    public String toString() {
        return this.className.stringValue() + ".class";
    }
}

final class EnumValue implements AnnotationValue.OfEnum {

    private final Utf8Entry className;
    private final Utf8Entry constantName;

    EnumValue(Utf8Entry className, Utf8Entry constantName) {
        this.className = className;
        this.constantName = constantName;
    }

    public int tag() {
        return AnnotationValue.TAG_ENUM;
    }

    public Utf8Entry className() {
        return this.className;
    }

    public Utf8Entry constantName() {
        return this.constantName;
    }

    public String toString() {
        return this.className.stringValue() + "." + this.constantName.stringValue();
    }
}

final class NestedAnnotationValue implements AnnotationValue.OfAnnotation {

    private final Annotation v;

    NestedAnnotationValue(Annotation v) {
        this.v = v;
    }

    public int tag() {
        return AnnotationValue.TAG_ANNOTATION;
    }

    public Annotation annotation() {
        return this.v;
    }

    public String toString() {
        return String.valueOf(this.v);
    }
}

final class ArrayValue implements AnnotationValue.OfArray {

    private final List<AnnotationValue> values;

    ArrayValue(List<AnnotationValue> values) {
        this.values = values;
    }

    public int tag() {
        return AnnotationValue.TAG_ARRAY;
    }

    public List<AnnotationValue> values() {
        return this.values;
    }

    public String toString() {
        return String.valueOf(this.values);
    }
}

final class TypeAnnotationImpl implements TypeAnnotation {

    private final TargetInfo target;
    private final List<TypePathComponent> path;
    private final Annotation annotation;

    TypeAnnotationImpl(TargetInfo target, List<TypePathComponent> path, Annotation annotation) {
        this.target = target;
        this.path = path;
        this.annotation = annotation;
    }

    public TargetInfo targetInfo() {
        return this.target;
    }

    public List<TypePathComponent> targetPath() {
        return this.path;
    }

    public Annotation annotation() {
        return this.annotation;
    }

    public String toString() {
        return "TypeAnnotation[" + this.target + ", " + this.path + ", " + this.annotation + "]";
    }
}

final class TypeParameterTargetImpl implements TypeParameterTarget {

    private final TargetType t;
    private final int i;

    TypeParameterTargetImpl(TargetType t, int i) {
        this.t = t;
        this.i = i;
    }

    public TargetType targetType() {
        return this.t;
    }

    public int typeParameterIndex() {
        return this.i;
    }

    public String toString() {
        return this.t + "(" + this.i + ")";
    }
}

final class SupertypeTargetImpl implements SupertypeTarget {

    private final int i;

    SupertypeTargetImpl(int i) {
        this.i = i;
    }

    public TargetType targetType() {
        return TargetType.CLASS_EXTENDS;
    }

    public int supertypeIndex() {
        return this.i;
    }

    public String toString() {
        return "CLASS_EXTENDS(" + this.i + ")";
    }
}

final class TypeParameterBoundTargetImpl implements TypeParameterBoundTarget {

    private final TargetType t;
    private final int i;
    private final int j;

    TypeParameterBoundTargetImpl(TargetType t, int i, int j) {
        this.t = t;
        this.i = i;
        this.j = j;
    }

    public TargetType targetType() {
        return this.t;
    }

    public int typeParameterIndex() {
        return this.i;
    }

    public int boundIndex() {
        return this.j;
    }

    public String toString() {
        return this.t + "(" + this.i + "," + this.j + ")";
    }
}

final class EmptyTargetImpl implements EmptyTarget {

    private final TargetType t;

    EmptyTargetImpl(TargetType t) {
        this.t = t;
    }

    public TargetType targetType() {
        return this.t;
    }

    public String toString() {
        return this.t.toString();
    }
}

final class FormalParameterTargetImpl implements FormalParameterTarget {

    private final int i;

    FormalParameterTargetImpl(int i) {
        this.i = i;
    }

    public TargetType targetType() {
        return TargetType.METHOD_FORMAL_PARAMETER;
    }

    public int formalParameterIndex() {
        return this.i;
    }

    public String toString() {
        return "METHOD_FORMAL_PARAMETER(" + this.i + ")";
    }
}

final class ThrowsTargetImpl implements ThrowsTarget {

    private final int i;

    ThrowsTargetImpl(int i) {
        this.i = i;
    }

    public TargetType targetType() {
        return TargetType.THROWS;
    }

    public int throwsTargetIndex() {
        return this.i;
    }

    public String toString() {
        return "THROWS(" + this.i + ")";
    }
}

final class LocalVarTargetImpl implements LocalVarTarget {

    private final TargetType t;
    private final List<LocalVarTargetInfo> table;

    LocalVarTargetImpl(TargetType t, List<LocalVarTargetInfo> table) {
        this.t = t;
        this.table = table;
    }

    public TargetType targetType() {
        return this.t;
    }

    public List<LocalVarTargetInfo> table() {
        return this.table;
    }

    public String toString() {
        return this.t + this.table.toString();
    }
}

final class LocalVarTargetInfoImpl implements LocalVarTargetInfo {

    private final Label start;
    private final Label end;
    private final int slot;

    LocalVarTargetInfoImpl(Label start, Label end, int slot) {
        this.start = start;
        this.end = end;
        this.slot = slot;
    }

    public Label startLabel() {
        return this.start;
    }

    public Label endLabel() {
        return this.end;
    }

    public int index() {
        return this.slot;
    }

    public String toString() {
        return "[" + this.start + ".." + this.end + " #" + this.slot + "]";
    }
}

final class CatchTargetImpl implements CatchTarget {

    private final int i;

    CatchTargetImpl(int i) {
        this.i = i;
    }

    public TargetType targetType() {
        return TargetType.EXCEPTION_PARAMETER;
    }

    public int exceptionTableIndex() {
        return this.i;
    }

    public String toString() {
        return "EXCEPTION_PARAMETER(" + this.i + ")";
    }
}

final class OffsetTargetImpl implements OffsetTarget {

    private final TargetType t;
    private final Label target;

    OffsetTargetImpl(TargetType t, Label target) {
        this.t = t;
        this.target = target;
    }

    public TargetType targetType() {
        return this.t;
    }

    public Label target() {
        return this.target;
    }

    public String toString() {
        return this.t + "(" + this.target + ")";
    }
}

final class TypeArgumentTargetImpl implements TypeArgumentTarget {

    private final TargetType t;
    private final Label target;
    private final int i;

    TypeArgumentTargetImpl(TargetType t, Label target, int i) {
        this.t = t;
        this.target = target;
        this.i = i;
    }

    public TargetType targetType() {
        return this.t;
    }

    public Label target() {
        return this.target;
    }

    public int typeArgumentIndex() {
        return this.i;
    }

    public String toString() {
        return this.t + "(" + this.target + "," + this.i + ")";
    }
}

final class TypePathComponentImpl implements TypePathComponent {

    private final Kind k;
    private final int i;

    TypePathComponentImpl(Kind k, int i) {
        this.k = k;
        this.i = i;
    }

    public Kind typePathKind() {
        return this.k;
    }

    public int typeArgumentIndex() {
        return this.i;
    }

    public String toString() {
        return this.k + "(" + this.i + ")";
    }
}
