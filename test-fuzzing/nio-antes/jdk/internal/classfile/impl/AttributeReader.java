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
 * The reading of the typed attributes: from the bytes of the `.class` to the
 * `java.lang.classfile.attribute` object.
 *
 * <p>It is the half {@link AttributeMappers} lacks, whose other half is {@link AttributeWriter}.
 * They are together and not spread over one mapper subclass per attribute so that they can be
 * compared: the reader and the writer of an attribute have to be inverses, and that is seen reading
 * the two cases one after the other, not jumping between thirty-four files.
 *
 * <h2>The cursor</h2>
 *
 * <p>Several readers take a one-element `int[] p` instead of receiving and returning the offset. It
 * is not a whim: an annotation **has no length of its own** --to know where it ends it has to be
 * walked whole-- and an `element_value` may have another inside. With recursion, returning the new
 * position would force a (value, position) pair type at each level; the one-element array is the
 * same trick with less ceremony.
 *
 * <h2>The labels</h2>
 *
 * <p>Every offset the model exposes as a {@link java.lang.classfile.Label} is wrapped in a {@link
 * LabelImpl}, which is a label that **already knows its position**. It is what is right here: these
 * offsets came out of a file, they are not unknowns to be resolved.
 */
final class AttributeReader {

    private AttributeReader() {
    }

    // ---- reading pool entries ------------------------------------------------------------------
    //
    // These helpers put a local of the declared type between `readEntry(int, Class<T>)` and whoever
    // takes its result, because our javac used to erase `T` to its bound when the result went
    // straight in as an argument of another call, and then did not find the method. It is the same
    // detour `AttributeMapperImpl` documents, here once per type instead of repeated at each
    // reading site. The frozen javac compiles the direct form now (checked 2026-09-18), so the
    // detour is no longer needed; it is harmless.

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
     * The attribute starting at `pos` (the first byte of the body), according to its dispatch code.
     *
     * <p>`length` is the length of the body, already validated by {@link TypedAttributeMapper}.
     * Only the two attributes whose body carries no counter of its own use it.
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
        // `SourceDebugExtension` is the only JVMS attribute whose body has NO structure: it is
        // modified UTF-8 from end to end, with no counter. Hence it needs the `length`.
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
            // Index zero means "there is no method": the class is in an initialiser or in a class
            // body, not inside a method. `readEntryOrNull` tells that apart from a broken index,
            // which does have to break.
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
        throw new IllegalArgumentException("unknown attribute code: " + code);
    }

    // ---- the lists of pool entries -------------------------------------------------------------

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

    // ---- the tables ----------------------------------------------------------------------------

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

    // Careful: the parameter count goes in ONE byte, not in two. It is the only `u1` counter of
    // this family of tables, and a `readU2` here swallows the first name.
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

    // The attributes of a component are read with the RAW registry, not with this one. It is not an
    // inconsistency: a component may carry any attribute, including the ones this library does not
    // know, and `Mappers` is precisely the index that knows how to answer by name for all of them.
    // The typing of those nested attributes is lost, and what is gained is that a `.class` with an
    // odd attribute inside a component is read all the same.
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

    // ---- the annotations -----------------------------------------------------------------------
    //
    // The bulk is done by `Annotations`, which already has the parser of `annotation` and of
    // `element_value` with its cursor. What is left here is only the upper layer of the
    // PER-PARAMETER annotations, which is the one form `Annotations` does not cover: a list of
    // lists with the counter in one byte.

    private static List<List<Annotation>> readByParameter(ClassReader cf, int pos) {
        // The parameter count goes in one byte, as in `MethodParameters`.
        int n = cf.readU1(pos);
        Annotations.Cursor c = new Annotations.Cursor();
        c.p = pos + 1;
        List<List<Annotation>> out = new ArrayList<List<Annotation>>();
        for (int i = 0; i < n; i++) {
            int m = cf.readU2(c.p);
            c.p = c.p + 2;
            List<Annotation> single = new ArrayList<Annotation>();
            for (int j = 0; j < m; j++) {
                single.add(Annotations.readAnnotation(cf, c));
            }
            out.add(single);
        }
        return out;
    }

    // ---- `StackMapTable` ----------------------------------------------------------------------

    // The six frame forms are a COMPRESSION of the same state: each one is read against the
    // previous frame. Here they are all decompressed to the full form --explicit locals and stack--
    // because `StackMapFrameInfo` describes the state, not its encoding.
    //
    // The offset is relative too (`offset_delta`): the first is at `offset_delta` and each
    // following one at `previous + offset_delta + 1`. That `+1` is the part that gets forgotten.
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
                // `chop`: as many variables as the tag says are removed from the end.
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
                throw new IllegalArgumentException("reserved frame_type: " + frameType);
            }
            // The copy of `locals` is not defensive: the list keeps being used as the accumulator
            // for the next frame, so without copying the frames would end up sharing the final
            // state instead of each having its own.
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
        SimpleVerificationTypeInfo[] simpleTypes = SimpleVerificationTypeInfo.values();
        for (int i = 0; i < simpleTypes.length; i++) {
            if (simpleTypes[i].tag() == tag) {
                return simpleTypes[i];
            }
        }
        throw new IllegalArgumentException("unknown verification_type_info: " + tag);
    }
}

// The entries of the two local variable tables. They have no public factory --the JDK exposes them
// only as the result of reading-- so they live here, on the reader's side.
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

// An attribute whose name is not in the JVMS. It keeps the three things known about it --the name,
// the mapper that recognised it and its bytes-- and nothing more. It is what allows copying it from
// one file to another without understanding it.
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
        byte[] copy = new byte[this.contents.length];
        System.arraycopy(this.contents, 0, copy, 0, this.contents.length);
        return copy;
    }

    // Without copying, for the writer.
    byte[] raw() {
        return this.contents;
    }

    public String toString() {
        return "Attribute[" + this.name.stringValue() + ", " + this.contents.length + " bytes]";
    }
}
