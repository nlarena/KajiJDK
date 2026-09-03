package jdk.internal.classfile.impl;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.ClassReader;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.LocalVarTargetInfo;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.classfile.attribute.CharacterRangeInfo;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.LineNumberInfo;
import java.lang.classfile.attribute.LocalVariableInfo;
import java.lang.classfile.attribute.LocalVariableTypeInfo;
import java.lang.classfile.attribute.MethodParameterInfo;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleHashInfo;
import java.lang.classfile.attribute.ModuleOpenInfo;
import java.lang.classfile.attribute.ModuleProvideInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.SimpleVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.UnknownAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantValueEntry;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La lectura de los atributos tipados: de los bytes del `.class` al objeto de
 * `java.lang.classfile.attribute`.
 *
 * <p>Es la mitad que le falta a {@link AttributeMappers}, cuya otra mitad es {@link AttributeWriter}.
 * Están juntos y no repartidos en una subclase de mapeador por atributo para poder compararlos: el
 * lector y el escritor de un atributo tienen que ser inversos, y eso se ve leyendo los dos casos
 * seguidos, no saltando entre treinta y cuatro archivos.
 *
 * <h2>El cursor</h2>
 *
 * <p>Varios lectores toman un `int[] p` de un elemento en vez de recibir y devolver el offset. No es
 * capricho: una anotación **no tiene largo propio** —para saber dónde termina hay que recorrerla
 * entera— y un `element_value` puede tener adentro otro. Con recursión, devolver la posición nueva
 * obligaría a un tipo par (valor, posición) en cada nivel; el arreglo de un elemento es el mismo
 * truco con menos ceremonia.
 *
 * <h2>Las etiquetas</h2>
 *
 * <p>Todo offset que el modelo expone como {@link java.lang.classfile.Label} se envuelve en un
 * {@link LabelImpl}, que es una etiqueta que **ya sabe su posición**. Es lo correcto acá: estos
 * offsets salieron de un archivo, no son incógnitas por resolver.
 */
final class AttributeReader {

    private AttributeReader() {
    }

    // ---- lectura de entradas de pool ------------------------------------------------------
    //
    // Nuestro javac borra la `T` de `readEntry(int, Class<T>)` a su cota cuando el resultado va
    // derecho como argumento de otra llamada, y ahí no encuentra el método. Con un local del tipo
    // declarado en el medio resuelve; es el mismo rodeo que ya documenta `AttributeMapperImpl`, y
    // acá está una vez por tipo en lugar de repetido en cada sitio de lectura.

    private static Utf8Entry utf8At(ClassReader cf, int at) {
        Utf8Entry e = cf.readEntry(at, Utf8Entry.class);
        return e;
    }

    private static ClassEntry classAt(ClassReader cf, int at) {
        ClassEntry e = cf.readEntry(at, ClassEntry.class);
        return e;
    }

    private static ModuleEntry moduleAt(ClassReader cf, int at) {
        ModuleEntry e = cf.readEntry(at, ModuleEntry.class);
        return e;
    }

    private static PackageEntry packageAt(ClassReader cf, int at) {
        PackageEntry e = cf.readEntry(at, PackageEntry.class);
        return e;
    }

    private static ConstantValueEntry constantValueAt(ClassReader cf, int at) {
        ConstantValueEntry e = cf.readEntry(at, ConstantValueEntry.class);
        return e;
    }

    private static IntegerEntry intAt(ClassReader cf, int at) {
        IntegerEntry e = cf.readEntry(at, IntegerEntry.class);
        return e;
    }

    private static DoubleEntry doubleAt(ClassReader cf, int at) {
        DoubleEntry e = cf.readEntry(at, DoubleEntry.class);
        return e;
    }

    private static FloatEntry floatAt(ClassReader cf, int at) {
        FloatEntry e = cf.readEntry(at, FloatEntry.class);
        return e;
    }

    private static LongEntry longAt(ClassReader cf, int at) {
        LongEntry e = cf.readEntry(at, LongEntry.class);
        return e;
    }

    private static Utf8Entry utf8OrNullAt(ClassReader cf, int at) {
        Utf8Entry e = cf.readEntryOrNull(at, Utf8Entry.class);
        return e;
    }

    private static ClassEntry classOrNullAt(ClassReader cf, int at) {
        ClassEntry e = cf.readEntryOrNull(at, ClassEntry.class);
        return e;
    }

    private static NameAndTypeEntry nameAndTypeOrNullAt(ClassReader cf, int at) {
        NameAndTypeEntry e = cf.readEntryOrNull(at, NameAndTypeEntry.class);
        return e;
    }


    /**
     * El atributo que empieza en `pos` (el primer byte del cuerpo), según su código de reparto.
     *
     * <p>`length` es el largo del cuerpo, que ya validó {@link TypedAttributeMapper}. Sólo lo usan
     * los dos atributos cuyo cuerpo no lleva un contador propio.
     */
    static Attribute<?> read(int code, AttributeMapper<?> mapper, Utf8Entry name,
            AttributedElement enclosing, ClassReader cf, int pos, int length) {
        if (code == AttributeMappers.C_SOURCE_FILE) {
            return TypedAttributes.sourceFile(utf8At(cf, pos));
        }
        if (code == AttributeMappers.C_SOURCE_ID) {
            return TypedAttributes.sourceId(utf8At(cf, pos));
        }
        if (code == AttributeMappers.C_COMPILATION_ID) {
            return TypedAttributes.compilationId(utf8At(cf, pos));
        }
        if (code == AttributeMappers.C_SIGNATURE) {
            return TypedAttributes.signature(utf8At(cf, pos));
        }
        if (code == AttributeMappers.C_MODULE_TARGET) {
            return TypedAttributes.moduleTarget(utf8At(cf, pos));
        }
        if (code == AttributeMappers.C_NEST_HOST) {
            return TypedAttributes.nestHost(classAt(cf, pos));
        }
        if (code == AttributeMappers.C_MODULE_MAIN_CLASS) {
            return TypedAttributes.moduleMainClass(classAt(cf, pos));
        }
        if (code == AttributeMappers.C_CONSTANT_VALUE) {
            return TypedAttributes.constantValue(constantValueAt(cf, pos));
        }
        if (code == AttributeMappers.C_DEPRECATED) {
            return TypedAttributes.deprecated();
        }
        if (code == AttributeMappers.C_SYNTHETIC) {
            return TypedAttributes.synthetic();
        }
        if (code == AttributeMappers.C_MODULE_RESOLUTION) {
            return TypedAttributes.moduleResolution(cf.readU2(pos));
        }
        // `SourceDebugExtension` es el único atributo del JVMS cuyo cuerpo NO tiene estructura: es
        // UTF-8 modificado de punta a punta, sin contador. De ahí que necesite el `length`.
        if (code == AttributeMappers.C_SOURCE_DEBUG_EXTENSION) {
            return TypedAttributes.sourceDebugExtension(cf.readBytes(pos, length));
        }
        if (code == AttributeMappers.C_EXCEPTIONS) {
            return TypedAttributes.exceptions(readClasses(cf, pos));
        }
        if (code == AttributeMappers.C_NEST_MEMBERS) {
            return TypedAttributes.nestMembers(readClasses(cf, pos));
        }
        if (code == AttributeMappers.C_PERMITTED_SUBCLASSES) {
            return TypedAttributes.permittedSubclasses(readClasses(cf, pos));
        }
        if (code == AttributeMappers.C_MODULE_PACKAGES) {
            return TypedAttributes.modulePackages(readPackages(cf, pos));
        }
        if (code == AttributeMappers.C_ENCLOSING_METHOD) {
            ClassEntry owner = classAt(cf, pos);
            // Índice cero significa "no hay método": la clase está en un inicializador o en un
            // cuerpo de clase, no dentro de un método. `readEntryOrNull` distingue eso de un índice
            // roto, que sí tiene que romper.
            NameAndTypeEntry nat = nameAndTypeOrNullAt(cf, pos + 2);
            return TypedAttributes.enclosingMethod(owner, Optional.ofNullable(nat));
        }
        if (code == AttributeMappers.C_INNER_CLASSES) {
            return readInnerClasses(cf, pos);
        }
        if (code == AttributeMappers.C_LINE_NUMBER_TABLE) {
            return readLineNumbers(cf, pos);
        }
        if (code == AttributeMappers.C_LOCAL_VARIABLE_TABLE) {
            return readLocalVariables(cf, pos);
        }
        if (code == AttributeMappers.C_LOCAL_VARIABLE_TYPE_TABLE) {
            return readLocalVariableTypes(cf, pos);
        }
        if (code == AttributeMappers.C_CHARACTER_RANGE_TABLE) {
            return readCharacterRanges(cf, pos);
        }
        if (code == AttributeMappers.C_METHOD_PARAMETERS) {
            return readMethodParameters(cf, pos);
        }
        if (code == AttributeMappers.C_MODULE_HASHES) {
            return readModuleHashes(cf, pos);
        }
        if (code == AttributeMappers.C_MODULE) {
            return readModule(cf, pos);
        }
        if (code == AttributeMappers.C_RECORD) {
            return readRecord(cf, pos);
        }
        if (code == AttributeMappers.C_RUNTIME_VISIBLE_ANNOTATIONS) {
            return TypedAttributes.runtimeVisibleAnnotations(Annotations.readAnnotations(cf, pos));
        }
        if (code == AttributeMappers.C_RUNTIME_INVISIBLE_ANNOTATIONS) {
            return TypedAttributes.runtimeInvisibleAnnotations(Annotations.readAnnotations(cf, pos));
        }
        if (code == AttributeMappers.C_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS) {
            return TypedAttributes.runtimeVisibleParameterAnnotations(readByParameter(cf, pos));
        }
        if (code == AttributeMappers.C_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS) {
            return TypedAttributes.runtimeInvisibleParameterAnnotations(readByParameter(cf, pos));
        }
        if (code == AttributeMappers.C_RUNTIME_VISIBLE_TYPE_ANNOTATIONS) {
            return TypedAttributes.runtimeVisibleTypeAnnotations(Annotations.readTypeAnnotations(cf, pos));
        }
        if (code == AttributeMappers.C_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS) {
            return TypedAttributes.runtimeInvisibleTypeAnnotations(Annotations.readTypeAnnotations(cf, pos));
        }
        if (code == AttributeMappers.C_ANNOTATION_DEFAULT) {
            Annotations.Cursor c = new Annotations.Cursor();
            c.p = pos;
            return TypedAttributes.annotationDefault(Annotations.readValue(cf, c));
        }
        if (code == AttributeMappers.C_STACK_MAP_TABLE) {
            return readStackMapTable(cf, pos);
        }
        if (code == AttributeMappers.C_UNKNOWN) {
            return new UnknownAttributeImpl(name, mapper, cf.readBytes(pos, length));
        }
        throw new IllegalArgumentException("código de atributo desconocido: " + code);
    }

    // ---- las listas de entradas de pool -------------------------------------------------------

    private static List<ClassEntry> readClasses(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < n; i++) {
            out.add(classAt(cf, pos + 2 + i * 2));
        }
        return out;
    }

    private static List<PackageEntry> readPackages(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<PackageEntry> out = new ArrayList<PackageEntry>();
        for (int i = 0; i < n; i++) {
            out.add(packageAt(cf, pos + 2 + i * 2));
        }
        return out;
    }

    // ---- las tablas ---------------------------------------------------------------------------

    private static Attribute<?> readInnerClasses(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<InnerClassInfo> out = new ArrayList<InnerClassInfo>();
        int p = pos + 2;
        for (int i = 0; i < n; i++) {
            ClassEntry inner = classAt(cf, p);
            ClassEntry outer = classOrNullAt(cf, p + 2);
            Utf8Entry name = utf8OrNullAt(cf, p + 4);
            out.add(TypedAttributes.innerClassInfo(inner, Optional.ofNullable(outer),
                    Optional.ofNullable(name), cf.readU2(p + 6)));
            p = p + 8;
        }
        return TypedAttributes.innerClasses(out);
    }

    private static Attribute<?> readLineNumbers(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<LineNumberInfo> out = new ArrayList<LineNumberInfo>();
        for (int i = 0; i < n; i++) {
            int p = pos + 2 + i * 4;
            out.add(TypedAttributes.lineNumberInfo(cf.readU2(p), cf.readU2(p + 2)));
        }
        return TypedAttributes.lineNumberTable(out);
    }

    private static Attribute<?> readLocalVariables(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<LocalVariableInfo> out = new ArrayList<LocalVariableInfo>();
        for (int i = 0; i < n; i++) {
            int p = pos + 2 + i * 10;
            out.add(new LocalVariableInfoImpl(cf.readU2(p), cf.readU2(p + 2),
                    utf8At(cf, p + 4), utf8At(cf, p + 6),
                    cf.readU2(p + 8)));
        }
        return TypedAttributes.localVariableTable(out);
    }

    private static Attribute<?> readLocalVariableTypes(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<LocalVariableTypeInfo> out = new ArrayList<LocalVariableTypeInfo>();
        for (int i = 0; i < n; i++) {
            int p = pos + 2 + i * 10;
            out.add(new LocalVariableTypeInfoImpl(cf.readU2(p), cf.readU2(p + 2),
                    utf8At(cf, p + 4), utf8At(cf, p + 6),
                    cf.readU2(p + 8)));
        }
        return TypedAttributes.localVariableTypeTable(out);
    }

    private static Attribute<?> readCharacterRanges(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<CharacterRangeInfo> out = new ArrayList<CharacterRangeInfo>();
        for (int i = 0; i < n; i++) {
            int p = pos + 2 + i * 14;
            out.add(TypedAttributes.characterRangeInfo(cf.readU2(p), cf.readU2(p + 2),
                    cf.readInt(p + 4), cf.readInt(p + 8), cf.readU2(p + 12)));
        }
        return TypedAttributes.characterRangeTable(out);
    }

    // Ojo: la cantidad de parámetros va en UN byte, no en dos. Es el único contador `u1` de esta
    // familia de tablas, y un `readU2` acá se lleva puesto el primer nombre.
    private static Attribute<?> readMethodParameters(ClassReader cf, int pos) {
        int n = cf.readU1(pos);
        List<MethodParameterInfo> out = new ArrayList<MethodParameterInfo>();
        for (int i = 0; i < n; i++) {
            int p = pos + 1 + i * 4;
            Utf8Entry name = utf8OrNullAt(cf, p);
            out.add(TypedAttributes.methodParameterInfo(Optional.ofNullable(name),
                    cf.readU2(p + 2)));
        }
        return TypedAttributes.methodParameters(out);
    }

    private static Attribute<?> readModuleHashes(ClassReader cf, int pos) {
        Utf8Entry algorithm = utf8At(cf, pos);
        int n = cf.readU2(pos + 2);
        List<ModuleHashInfo> out = new ArrayList<ModuleHashInfo>();
        int p = pos + 4;
        for (int i = 0; i < n; i++) {
            ModuleEntry m = moduleAt(cf, p);
            int len = cf.readU2(p + 2);
            out.add(TypedAttributes.moduleHashInfo(m, cf.readBytes(p + 4, len)));
            p = p + 4 + len;
        }
        return TypedAttributes.moduleHashes(algorithm, out);
    }

    private static Attribute<?> readModule(ClassReader cf, int pos) {
        ModuleEntry name = moduleAt(cf, pos);
        int flags = cf.readU2(pos + 2);
        Utf8Entry version = utf8OrNullAt(cf, pos + 4);
        int p = pos + 6;

        int nRequires = cf.readU2(p);
        p = p + 2;
        List<ModuleRequireInfo> requires = new ArrayList<ModuleRequireInfo>();
        for (int i = 0; i < nRequires; i++) {
            requires.add(TypedAttributes.moduleRequireInfo(moduleAt(cf, p), cf.readU2(p + 2),
                    utf8OrNullAt(cf, p + 4)));
            p = p + 6;
        }

        int nExports = cf.readU2(p);
        p = p + 2;
        List<ModuleExportInfo> exports = new ArrayList<ModuleExportInfo>();
        for (int i = 0; i < nExports; i++) {
            PackageEntry pkg = packageAt(cf, p);
            int f = cf.readU2(p + 2);
            int nTo = cf.readU2(p + 4);
            List<ModuleEntry> to = new ArrayList<ModuleEntry>();
            for (int j = 0; j < nTo; j++) {
                to.add(moduleAt(cf, p + 6 + j * 2));
            }
            exports.add(TypedAttributes.moduleExportInfo(pkg, f, to));
            p = p + 6 + nTo * 2;
        }

        int nOpens = cf.readU2(p);
        p = p + 2;
        List<ModuleOpenInfo> opens = new ArrayList<ModuleOpenInfo>();
        for (int i = 0; i < nOpens; i++) {
            PackageEntry pkg = packageAt(cf, p);
            int f = cf.readU2(p + 2);
            int nTo = cf.readU2(p + 4);
            List<ModuleEntry> to = new ArrayList<ModuleEntry>();
            for (int j = 0; j < nTo; j++) {
                to.add(moduleAt(cf, p + 6 + j * 2));
            }
            opens.add(TypedAttributes.moduleOpenInfo(pkg, f, to));
            p = p + 6 + nTo * 2;
        }

        int nUses = cf.readU2(p);
        p = p + 2;
        List<ClassEntry> uses = new ArrayList<ClassEntry>();
        for (int i = 0; i < nUses; i++) {
            uses.add(classAt(cf, p + i * 2));
        }
        p = p + nUses * 2;

        int nProvides = cf.readU2(p);
        p = p + 2;
        List<ModuleProvideInfo> provides = new ArrayList<ModuleProvideInfo>();
        for (int i = 0; i < nProvides; i++) {
            ClassEntry service = classAt(cf, p);
            int nWith = cf.readU2(p + 2);
            List<ClassEntry> with = new ArrayList<ClassEntry>();
            for (int j = 0; j < nWith; j++) {
                with.add(classAt(cf, p + 4 + j * 2));
            }
            provides.add(TypedAttributes.moduleProvideInfo(service, with));
            p = p + 4 + nWith * 2;
        }

        return TypedAttributes.module(name, flags, version, requires, exports, opens, uses,
                provides);
    }

    // Los atributos de un componente se leen con el registro CRUDO, no con éste. No es
    // inconsistencia: un componente puede llevar cualquier atributo, incluidos los que esta
    // biblioteca no conoce, y `Mappers` es justamente el índice que sabe contestar por nombre para
    // todos. Se pierde el tipado de esos atributos anidados y se gana que un `.class` con un
    // atributo raro adentro de un componente se lea igual.
    private static Attribute<?> readRecord(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        List<RecordComponentInfo> out = new ArrayList<RecordComponentInfo>();
        int p = pos + 2;
        for (int i = 0; i < n; i++) {
            Utf8Entry name = utf8At(cf, p);
            Utf8Entry descriptor = utf8At(cf, p + 2);
            int nAttrs = cf.readU2(p + 4);
            p = p + 6;
            List<Attribute<?>> attrs = new ArrayList<Attribute<?>>();
            for (int j = 0; j < nAttrs; j++) {
                Utf8Entry attrName = utf8At(cf, p);
                int len = cf.readInt(p + 2);
                attrs.add(Mappers.forName(attrName.stringValue()).readAttribute(null, cf, p + 6));
                p = p + 6 + len;
            }
            out.add(TypedAttributes.recordComponentInfo(name, descriptor, attrs));
        }
        return TypedAttributes.record(out);
    }

    // ---- las anotaciones ----------------------------------------------------------------------
    //
    // El grueso lo hace `Annotations`, que ya tiene el parser de `annotation` y de `element_value`
    // con su cursor. Aca queda solo la capa de arriba de las anotaciones POR PARAMETRO, que es la
    // unica forma que `Annotations` no cubre: una lista de listas con el contador en un byte.

    private static List<List<Annotation>> readByParameter(ClassReader cf, int pos) {
        // La cantidad de parametros va en un byte, como en `MethodParameters`.
        int n = cf.readU1(pos);
        Annotations.Cursor c = new Annotations.Cursor();
        c.p = pos + 1;
        List<List<Annotation>> out = new ArrayList<List<Annotation>>();
        for (int i = 0; i < n; i++) {
            int m = cf.readU2(c.p);
            c.p = c.p + 2;
            List<Annotation> uno = new ArrayList<Annotation>();
            for (int j = 0; j < m; j++) {
                uno.add(Annotations.readAnnotation(cf, c));
            }
            out.add(uno);
        }
        return out;
    }

    // ---- `StackMapTable` ----------------------------------------------------------------------

    // Las seis formas de frame son una COMPRESIÓN del mismo estado: cada una se lee contra el frame
    // anterior. Acá se descomprimen todas a la forma completa —locales y pila explícitas— porque
    // `StackMapFrameInfo` describe el estado, no su codificación.
    //
    // El offset también es relativo (`offset_delta`): el primero está en `offset_delta` y cada uno
    // de los siguientes en `anterior + offset_delta + 1`. Ese `+1` es la parte que se olvida.
    private static Attribute<?> readStackMapTable(ClassReader cf, int pos) {
        int n = cf.readU2(pos);
        int[] p = new int[] { pos + 2 };
        List<StackMapFrameInfo> out = new ArrayList<StackMapFrameInfo>();
        List<VerificationTypeInfo> locals = new ArrayList<VerificationTypeInfo>();
        int bci = -1;
        for (int i = 0; i < n; i++) {
            int frameType = cf.readU1(p[0]);
            p[0] = p[0] + 1;
            List<VerificationTypeInfo> stack = new ArrayList<VerificationTypeInfo>();
            if (frameType < 64) {
                bci = bci + frameType + 1;
            } else if (frameType < 128) {
                bci = bci + (frameType - 64) + 1;
                stack.add(readVerificationType(cf, p));
            } else if (frameType == 247) {
                bci = bci + cf.readU2(p[0]) + 1;
                p[0] = p[0] + 2;
                stack.add(readVerificationType(cf, p));
            } else if (frameType >= 248 && frameType <= 250) {
                bci = bci + cf.readU2(p[0]) + 1;
                p[0] = p[0] + 2;
                // `chop`: se sacan del final tantas variables como diga la etiqueta.
                for (int k = 0; k < 251 - frameType; k++) {
                    locals.remove(locals.size() - 1);
                }
            } else if (frameType == 251) {
                bci = bci + cf.readU2(p[0]) + 1;
                p[0] = p[0] + 2;
            } else if (frameType >= 252 && frameType <= 254) {
                bci = bci + cf.readU2(p[0]) + 1;
                p[0] = p[0] + 2;
                for (int k = 0; k < frameType - 251; k++) {
                    locals.add(readVerificationType(cf, p));
                }
            } else if (frameType == 255) {
                bci = bci + cf.readU2(p[0]) + 1;
                p[0] = p[0] + 2;
                int nLocals = cf.readU2(p[0]);
                p[0] = p[0] + 2;
                locals = new ArrayList<VerificationTypeInfo>();
                for (int k = 0; k < nLocals; k++) {
                    locals.add(readVerificationType(cf, p));
                }
                int nStack = cf.readU2(p[0]);
                p[0] = p[0] + 2;
                for (int k = 0; k < nStack; k++) {
                    stack.add(readVerificationType(cf, p));
                }
            } else {
                throw new IllegalArgumentException("frame_type reservado: " + frameType);
            }
            // La copia de `locals` no es defensa: la lista se sigue usando como acumulador para el
            // frame siguiente, así que sin copiar los frames terminarían compartiendo el estado
            // final en vez de tener cada uno el suyo.
            out.add(TypedAttributes.stackMapFrame(new LabelImpl(bci),
                    new ArrayList<VerificationTypeInfo>(locals), stack));
        }
        return TypedAttributes.stackMapTable(out);
    }

    private static VerificationTypeInfo readVerificationType(ClassReader cf, int[] p) {
        int tag = cf.readU1(p[0]);
        p[0] = p[0] + 1;
        if (tag == VerificationTypeInfo.ITEM_OBJECT) {
            ClassEntry c = classAt(cf, p[0]);
            p[0] = p[0] + 2;
            return TypedAttributes.objectVerificationType(c);
        }
        if (tag == VerificationTypeInfo.ITEM_UNINITIALIZED) {
            int offset = cf.readU2(p[0]);
            p[0] = p[0] + 2;
            return TypedAttributes.uninitializedVerificationType(new LabelImpl(offset));
        }
        SimpleVerificationTypeInfo[] simples = SimpleVerificationTypeInfo.values();
        for (int i = 0; i < simples.length; i++) {
            if (simples[i].tag() == tag) {
                return simples[i];
            }
        }
        throw new IllegalArgumentException("verification_type_info desconocido: " + tag);
    }
}

// Las entradas de las dos tablas de variables locales. No tienen fábrica pública —el JDK las expone
// sólo como resultado de leer— así que viven acá, del lado del lector.
final class LocalVariableInfoImpl implements LocalVariableInfo {

    private final int startPc;
    private final int length;
    private final Utf8Entry name;
    private final Utf8Entry type;
    private final int slot;

    LocalVariableInfoImpl(int startPc, int length, Utf8Entry name, Utf8Entry type, int slot) {
        this.startPc = startPc;
        this.length = length;
        this.name = name;
        this.type = type;
        this.slot = slot;
    }

    public int startPc() {
        return this.startPc;
    }

    public int length() {
        return this.length;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public Utf8Entry type() {
        return this.type;
    }

    public int slot() {
        return this.slot;
    }
}

final class LocalVariableTypeInfoImpl implements LocalVariableTypeInfo {

    private final int startPc;
    private final int length;
    private final Utf8Entry name;
    private final Utf8Entry signature;
    private final int slot;

    LocalVariableTypeInfoImpl(int startPc, int length, Utf8Entry name, Utf8Entry signature,
            int slot) {
        this.startPc = startPc;
        this.length = length;
        this.name = name;
        this.signature = signature;
        this.slot = slot;
    }

    public int startPc() {
        return this.startPc;
    }

    public int length() {
        return this.length;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public Utf8Entry signature() {
        return this.signature;
    }

    public int slot() {
        return this.slot;
    }
}

// Un atributo cuyo nombre no está en el JVMS. Conserva las tres cosas que se saben de él —el nombre,
// el mapeador que lo reconoció y sus bytes— y ninguna más. Es lo que permite copiarlo de un archivo
// a otro sin entenderlo.
final class UnknownAttributeImpl implements UnknownAttribute {

    private final Utf8Entry name;
    private final AttributeMapper<UnknownAttribute> mapper;
    private final byte[] contents;

    UnknownAttributeImpl(Utf8Entry name, AttributeMapper<?> mapper, byte[] contents) {
        this.name = name;
        this.mapper = (AttributeMapper<UnknownAttribute>) mapper;
        this.contents = contents;
    }

    public Utf8Entry attributeName() {
        return this.name;
    }

    public AttributeMapper<UnknownAttribute> attributeMapper() {
        return this.mapper;
    }

    public byte[] contents() {
        byte[] copia = new byte[this.contents.length];
        System.arraycopy(this.contents, 0, copia, 0, this.contents.length);
        return copia;
    }

    // Sin copiar, para el escritor.
    byte[] raw() {
        return this.contents;
    }

    public String toString() {
        return "Attribute[" + this.name.stringValue() + ", " + this.contents.length + " bytes]";
    }
}
