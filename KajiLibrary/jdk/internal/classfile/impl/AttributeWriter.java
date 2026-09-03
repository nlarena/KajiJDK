package jdk.internal.classfile.impl;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attribute;
import java.lang.classfile.BufWriter;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.LocalVarTargetInfo;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.classfile.attribute.CharacterRangeInfo;
import java.lang.classfile.attribute.CharacterRangeTableAttribute;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.LineNumberInfo;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.classfile.attribute.LocalVariableInfo;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.attribute.LocalVariableTypeInfo;
import java.lang.classfile.attribute.LocalVariableTypeTableAttribute;
import java.lang.classfile.attribute.MethodParameterInfo;
import java.lang.classfile.attribute.MethodParametersAttribute;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleHashInfo;
import java.lang.classfile.attribute.ModuleHashesAttribute;
import java.lang.classfile.attribute.ModuleMainClassAttribute;
import java.lang.classfile.attribute.ModuleOpenInfo;
import java.lang.classfile.attribute.ModulePackagesAttribute;
import java.lang.classfile.attribute.ModuleProvideInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.attribute.ModuleResolutionAttribute;
import java.lang.classfile.attribute.ModuleTargetAttribute;
import java.lang.classfile.attribute.NestHostAttribute;
import java.lang.classfile.attribute.NestMembersAttribute;
import java.lang.classfile.attribute.PermittedSubclassesAttribute;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.attribute.SourceDebugExtensionAttribute;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.classfile.attribute.SourceIDAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.ObjectVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.UninitializedVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.List;
import java.util.Optional;

/**
 * La escritura de los atributos tipados: del objeto de `java.lang.classfile.attribute` a los bytes.
 *
 * <p>El inverso exacto de {@link AttributeReader}, y conviene leerlos de a pares.
 *
 * <p>El largo no se puede escribir de una: se sabe recién cuando el cuerpo está escrito. Por eso
 * {@link #write} escribe el nombre, deja cuatro bytes reservados, escribe el cuerpo y después
 * parcha el largo con lo que se escribió. Es el mismo baile que hace cualquier escritor de `.class`
 * y está acá una sola vez, en vez de en cada atributo.
 */
final class AttributeWriter {

    private AttributeWriter() {
    }

    /** El atributo entero: nombre, largo y cuerpo. */
    static void write(BufWriter buf, Attribute<?> attr) {
        buf.writeIndex(attr.attributeName());
        int lenPos = buf.size();
        buf.writeInt(0);
        writeBody(buf, attr);
        buf.patchInt(lenPos, 4, buf.size() - lenPos - 4);
    }

    // El reparto va por el tipo del atributo y no por su código porque acá lo que hay es el objeto,
    // no el mapeador: el `instanceof` es la misma pregunta que el código, hecha del lado en que se
    // puede hacer.
    private static void writeBody(BufWriter buf, Attribute<?> attr) {
        if (attr instanceof SourceFileAttribute) {
            buf.writeIndex(((SourceFileAttribute) attr).sourceFile());
            return;
        }
        if (attr instanceof SourceIDAttribute) {
            buf.writeIndex(((SourceIDAttribute) attr).sourceId());
            return;
        }
        if (attr instanceof SignatureAttribute) {
            buf.writeIndex(((SignatureAttribute) attr).signature());
            return;
        }
        if (attr instanceof ModuleTargetAttribute) {
            buf.writeIndex(((ModuleTargetAttribute) attr).targetPlatform());
            return;
        }
        if (attr instanceof NestHostAttribute) {
            buf.writeIndex(((NestHostAttribute) attr).nestHost());
            return;
        }
        if (attr instanceof ModuleMainClassAttribute) {
            buf.writeIndex(((ModuleMainClassAttribute) attr).mainClass());
            return;
        }
        if (attr instanceof ConstantValueAttribute) {
            buf.writeIndex(((ConstantValueAttribute) attr).constant());
            return;
        }
        if (attr instanceof ModuleResolutionAttribute) {
            buf.writeU2(((ModuleResolutionAttribute) attr).resolutionFlags());
            return;
        }
        if (attr instanceof SourceDebugExtensionAttribute) {
            buf.writeBytes(((SourceDebugExtensionAttribute) attr).contents());
            return;
        }
        if (attr instanceof UnknownAttributeImpl) {
            buf.writeBytes(((UnknownAttributeImpl) attr).raw());
            return;
        }
        if (attr instanceof ExceptionsAttribute) {
            writeClasses(buf, ((ExceptionsAttribute) attr).exceptions());
            return;
        }
        if (attr instanceof NestMembersAttribute) {
            writeClasses(buf, ((NestMembersAttribute) attr).nestMembers());
            return;
        }
        if (attr instanceof PermittedSubclassesAttribute) {
            writeClasses(buf, ((PermittedSubclassesAttribute) attr).permittedSubclasses());
            return;
        }
        if (attr instanceof ModulePackagesAttribute) {
            List<PackageEntry> ps = ((ModulePackagesAttribute) attr).packages();
            buf.writeU2(ps.size());
            for (int i = 0; i < ps.size(); i++) {
                buf.writeIndex(ps.get(i));
            }
            return;
        }
        if (attr instanceof EnclosingMethodAttribute) {
            EnclosingMethodAttribute a = (EnclosingMethodAttribute) attr;
            buf.writeIndex(a.enclosingClass());
            Optional<NameAndTypeEntry> m = a.enclosingMethod();
            buf.writeIndexOrZero(m.isPresent() ? m.get() : null);
            return;
        }
        if (attr instanceof InnerClassesAttribute) {
            writeInnerClasses(buf, (InnerClassesAttribute) attr);
            return;
        }
        if (attr instanceof LineNumberTableAttribute) {
            List<LineNumberInfo> ls = ((LineNumberTableAttribute) attr).lineNumbers();
            buf.writeU2(ls.size());
            for (int i = 0; i < ls.size(); i++) {
                buf.writeU2(ls.get(i).startPc());
                buf.writeU2(ls.get(i).lineNumber());
            }
            return;
        }
        if (attr instanceof LocalVariableTableAttribute) {
            List<LocalVariableInfo> vs = ((LocalVariableTableAttribute) attr).localVariables();
            buf.writeU2(vs.size());
            for (int i = 0; i < vs.size(); i++) {
                LocalVariableInfo v = vs.get(i);
                buf.writeU2(v.startPc());
                buf.writeU2(v.length());
                buf.writeIndex(v.name());
                buf.writeIndex(v.type());
                buf.writeU2(v.slot());
            }
            return;
        }
        if (attr instanceof LocalVariableTypeTableAttribute) {
            List<LocalVariableTypeInfo> vs =
                    ((LocalVariableTypeTableAttribute) attr).localVariableTypes();
            buf.writeU2(vs.size());
            for (int i = 0; i < vs.size(); i++) {
                LocalVariableTypeInfo v = vs.get(i);
                buf.writeU2(v.startPc());
                buf.writeU2(v.length());
                buf.writeIndex(v.name());
                buf.writeIndex(v.signature());
                buf.writeU2(v.slot());
            }
            return;
        }
        if (attr instanceof CharacterRangeTableAttribute) {
            List<CharacterRangeInfo> rs =
                    ((CharacterRangeTableAttribute) attr).characterRangeTable();
            buf.writeU2(rs.size());
            for (int i = 0; i < rs.size(); i++) {
                CharacterRangeInfo r = rs.get(i);
                buf.writeU2(r.startPc());
                buf.writeU2(r.endPc());
                buf.writeInt(r.characterRangeStart());
                buf.writeInt(r.characterRangeEnd());
                buf.writeU2(r.flags());
            }
            return;
        }
        if (attr instanceof MethodParametersAttribute) {
            // La cantidad va en UN byte, no en dos. Ver la nota del lector.
            List<MethodParameterInfo> ps = ((MethodParametersAttribute) attr).parameters();
            buf.writeU1(ps.size());
            for (int i = 0; i < ps.size(); i++) {
                Optional<Utf8Entry> name = ps.get(i).name();
                buf.writeIndexOrZero(name.isPresent() ? name.get() : null);
                buf.writeU2(ps.get(i).flagsMask());
            }
            return;
        }
        if (attr instanceof ModuleHashesAttribute) {
            ModuleHashesAttribute a = (ModuleHashesAttribute) attr;
            buf.writeIndex(a.algorithm());
            List<ModuleHashInfo> hs = a.hashes();
            buf.writeU2(hs.size());
            for (int i = 0; i < hs.size(); i++) {
                buf.writeIndex(hs.get(i).moduleName());
                byte[] h = hs.get(i).hash();
                buf.writeU2(h.length);
                buf.writeBytes(h);
            }
            return;
        }
        if (attr instanceof ModuleAttribute) {
            writeModule(buf, (ModuleAttribute) attr);
            return;
        }
        if (attr instanceof RecordAttribute) {
            writeRecord(buf, (RecordAttribute) attr);
            return;
        }
        if (attr instanceof RuntimeVisibleAnnotationsAttribute) {
            writeAnnotations(buf, ((RuntimeVisibleAnnotationsAttribute) attr).annotations());
            return;
        }
        if (attr instanceof RuntimeInvisibleAnnotationsAttribute) {
            writeAnnotations(buf, ((RuntimeInvisibleAnnotationsAttribute) attr).annotations());
            return;
        }
        if (attr instanceof RuntimeVisibleParameterAnnotationsAttribute) {
            writeByParameter(buf,
                    ((RuntimeVisibleParameterAnnotationsAttribute) attr).parameterAnnotations());
            return;
        }
        if (attr instanceof RuntimeInvisibleParameterAnnotationsAttribute) {
            writeByParameter(buf,
                    ((RuntimeInvisibleParameterAnnotationsAttribute) attr).parameterAnnotations());
            return;
        }
        if (attr instanceof RuntimeVisibleTypeAnnotationsAttribute) {
            writeTypeAnnotations(buf,
                    ((RuntimeVisibleTypeAnnotationsAttribute) attr).annotations());
            return;
        }
        if (attr instanceof RuntimeInvisibleTypeAnnotationsAttribute) {
            writeTypeAnnotations(buf,
                    ((RuntimeInvisibleTypeAnnotationsAttribute) attr).annotations());
            return;
        }
        if (attr instanceof java.lang.classfile.attribute.AnnotationDefaultAttribute) {
            writeElementValue(buf,
                    ((java.lang.classfile.attribute.AnnotationDefaultAttribute) attr)
                            .defaultValue());
            return;
        }
        if (attr instanceof StackMapTableAttribute) {
            writeStackMapTable(buf, (StackMapTableAttribute) attr);
            return;
        }
        // `Deprecated` y `Synthetic` no tienen cuerpo: existir es todo lo que dicen. Caer acá sin
        // haber escrito nada es correcto para ellos y sólo para ellos, y por eso se comprueba en vez
        // de dejar que cualquier atributo desconocido se escriba vacío en silencio.
        if (attr instanceof java.lang.classfile.attribute.DeprecatedAttribute
                || attr instanceof java.lang.classfile.attribute.SyntheticAttribute) {
            return;
        }
        throw new IllegalArgumentException(
                "no hay escritor para " + attr.attributeName().stringValue());
    }

    // El bci de una etiqueta que salió de leer un archivo. Una etiqueta de un `CodeBuilder` es una
    // incógnita que se resuelve al cerrar el método, y acá no hay método que cerrar: decirlo es
    // mejor que escribir un cero que produciría un `.class` inverificable.
    private static int bci(Label label) {
        if (label instanceof LabelImpl) {
            return ((LabelImpl) label).bci();
        }
        throw new IllegalArgumentException("esta etiqueta todavía no tiene posición: " + label);
    }

    private static void writeClasses(BufWriter buf, List<ClassEntry> classes) {
        buf.writeU2(classes.size());
        for (int i = 0; i < classes.size(); i++) {
            buf.writeIndex(classes.get(i));
        }
    }

    private static void writeModules(BufWriter buf, List<ModuleEntry> modules) {
        buf.writeU2(modules.size());
        for (int i = 0; i < modules.size(); i++) {
            buf.writeIndex(modules.get(i));
        }
    }

    private static void writeInnerClasses(BufWriter buf, InnerClassesAttribute attr) {
        List<InnerClassInfo> cs = attr.classes();
        buf.writeU2(cs.size());
        for (int i = 0; i < cs.size(); i++) {
            InnerClassInfo c = cs.get(i);
            buf.writeIndex(c.innerClass());
            Optional<ClassEntry> outer = c.outerClass();
            buf.writeIndexOrZero(outer.isPresent() ? outer.get() : null);
            Optional<Utf8Entry> name = c.innerName();
            buf.writeIndexOrZero(name.isPresent() ? name.get() : null);
            buf.writeU2(c.flagsMask());
        }
    }

    private static void writeModule(BufWriter buf, ModuleAttribute attr) {
        buf.writeIndex(attr.moduleName());
        buf.writeU2(attr.moduleFlagsMask());
        Optional<Utf8Entry> version = attr.moduleVersion();
        buf.writeIndexOrZero(version.isPresent() ? version.get() : null);

        List<ModuleRequireInfo> requires = attr.requires();
        buf.writeU2(requires.size());
        for (int i = 0; i < requires.size(); i++) {
            ModuleRequireInfo r = requires.get(i);
            buf.writeIndex(r.requires());
            buf.writeU2(r.requiresFlagsMask());
            Optional<Utf8Entry> v = r.requiresVersion();
            buf.writeIndexOrZero(v.isPresent() ? v.get() : null);
        }

        List<ModuleExportInfo> exports = attr.exports();
        buf.writeU2(exports.size());
        for (int i = 0; i < exports.size(); i++) {
            ModuleExportInfo e = exports.get(i);
            buf.writeIndex(e.exportedPackage());
            buf.writeU2(e.exportsFlagsMask());
            writeModules(buf, e.exportsTo());
        }

        List<ModuleOpenInfo> opens = attr.opens();
        buf.writeU2(opens.size());
        for (int i = 0; i < opens.size(); i++) {
            ModuleOpenInfo o = opens.get(i);
            buf.writeIndex(o.openedPackage());
            buf.writeU2(o.opensFlagsMask());
            writeModules(buf, o.opensTo());
        }

        writeClasses(buf, attr.uses());

        List<ModuleProvideInfo> provides = attr.provides();
        buf.writeU2(provides.size());
        for (int i = 0; i < provides.size(); i++) {
            buf.writeIndex(provides.get(i).provides());
            writeClasses(buf, provides.get(i).providesWith());
        }
    }

    private static void writeRecord(BufWriter buf, RecordAttribute attr) {
        List<RecordComponentInfo> cs = attr.components();
        buf.writeU2(cs.size());
        for (int i = 0; i < cs.size(); i++) {
            RecordComponentInfo c = cs.get(i);
            buf.writeIndex(c.name());
            buf.writeIndex(c.descriptor());
            List<Attribute<?>> attrs = c.attributes();
            buf.writeU2(attrs.size());
            for (int j = 0; j < attrs.size(); j++) {
                writeNested(buf, attrs.get(j));
            }
        }
    }

    // Un atributo anidado se escribe con SU propio mapeador, que puede ser el crudo si el componente
    // trae un atributo que esta biblioteca no interpreta. El `unchecked` es inevitable y está
    // acotado a estas dos líneas: `Attribute<A>` y `AttributeMapper<A>` comparten el parámetro por
    // construcción —lo dice `Attribute<A extends Attribute<A>>`— pero el comodín ya perdió el nombre
    // de ese tipo y no hay forma de recuperarlo.
    private static void writeNested(BufWriter buf, Attribute<?> attr) {
        java.lang.classfile.AttributeMapper m = attr.attributeMapper();
        m.writeAttribute(buf, attr);
    }

    // ---- las anotaciones ----------------------------------------------------------------------

    private static void writeAnnotations(BufWriter buf, List<Annotation> annotations) {
        buf.writeU2(annotations.size());
        for (int i = 0; i < annotations.size(); i++) {
            writeAnnotation(buf, annotations.get(i));
        }
    }

    private static void writeByParameter(BufWriter buf, List<List<Annotation>> byParameter) {
        buf.writeU1(byParameter.size());
        for (int i = 0; i < byParameter.size(); i++) {
            writeAnnotations(buf, byParameter.get(i));
        }
    }

    private static void writeAnnotation(BufWriter buf, Annotation a) {
        buf.writeIndex(a.className());
        List<AnnotationElement> es = a.elements();
        buf.writeU2(es.size());
        for (int i = 0; i < es.size(); i++) {
            buf.writeIndex(es.get(i).name());
            writeElementValue(buf, es.get(i).value());
        }
    }

    private static void writeElementValue(BufWriter buf, AnnotationValue v) {
        int tag = v.tag();
        buf.writeU1(tag);
        if (tag == '[') {
            List<AnnotationValue> vs = ((AnnotationValue.OfArray) v).values();
            buf.writeU2(vs.size());
            for (int i = 0; i < vs.size(); i++) {
                writeElementValue(buf, vs.get(i));
            }
            return;
        }
        if (tag == '@') {
            writeAnnotation(buf, ((AnnotationValue.OfAnnotation) v).annotation());
            return;
        }
        if (tag == 'e') {
            buf.writeIndex(((AnnotationValue.OfEnum) v).className());
            buf.writeIndex(((AnnotationValue.OfEnum) v).constantName());
            return;
        }
        if (tag == 'c') {
            buf.writeIndex(((AnnotationValue.OfClass) v).className());
            return;
        }
        // Los diez restantes son una sola entrada de pool, y `OfConstant` es justamente lo que
        // tienen en común.
        buf.writeIndex(((AnnotationValue.OfConstant) v).constant());
    }

    private static void writeTypeAnnotations(BufWriter buf, List<TypeAnnotation> annotations) {
        buf.writeU2(annotations.size());
        for (int i = 0; i < annotations.size(); i++) {
            TypeAnnotation ta = annotations.get(i);
            writeTargetInfo(buf, ta.targetInfo());
            List<TypePathComponent> path = ta.targetPath();
            buf.writeU1(path.size());
            for (int j = 0; j < path.size(); j++) {
                buf.writeU1(path.get(j).typePathKind().tag());
                buf.writeU1(path.get(j).typeArgumentIndex());
            }
            writeAnnotation(buf, ta.annotation());
        }
    }

    private static void writeTargetInfo(BufWriter buf, TargetInfo target) {
        TargetType t = target.targetType();
        buf.writeU1(t.targetTypeValue());
        if (t == TargetType.CLASS_TYPE_PARAMETER || t == TargetType.METHOD_TYPE_PARAMETER) {
            buf.writeU1(((TypeAnnotation.TypeParameterTarget) target).typeParameterIndex());
            return;
        }
        if (t == TargetType.CLASS_EXTENDS) {
            buf.writeU2(((TypeAnnotation.SupertypeTarget) target).supertypeIndex());
            return;
        }
        if (t == TargetType.CLASS_TYPE_PARAMETER_BOUND
                || t == TargetType.METHOD_TYPE_PARAMETER_BOUND) {
            TypeAnnotation.TypeParameterBoundTarget b =
                    (TypeAnnotation.TypeParameterBoundTarget) target;
            buf.writeU1(b.typeParameterIndex());
            buf.writeU1(b.boundIndex());
            return;
        }
        if (t == TargetType.FIELD || t == TargetType.METHOD_RETURN
                || t == TargetType.METHOD_RECEIVER) {
            return;
        }
        if (t == TargetType.METHOD_FORMAL_PARAMETER) {
            buf.writeU1(((TypeAnnotation.FormalParameterTarget) target).formalParameterIndex());
            return;
        }
        if (t == TargetType.THROWS) {
            buf.writeU2(((TypeAnnotation.ThrowsTarget) target).throwsTargetIndex());
            return;
        }
        if (t == TargetType.LOCAL_VARIABLE || t == TargetType.RESOURCE_VARIABLE) {
            List<LocalVarTargetInfo> table = ((TypeAnnotation.LocalVarTarget) target).table();
            buf.writeU2(table.size());
            for (int i = 0; i < table.size(); i++) {
                int start = bci(table.get(i).startLabel());
                buf.writeU2(start);
                buf.writeU2(bci(table.get(i).endLabel()) - start);
                buf.writeU2(table.get(i).index());
            }
            return;
        }
        if (t == TargetType.EXCEPTION_PARAMETER) {
            buf.writeU2(((TypeAnnotation.CatchTarget) target).exceptionTableIndex());
            return;
        }
        if (t == TargetType.INSTANCEOF || t == TargetType.NEW
                || t == TargetType.CONSTRUCTOR_REFERENCE || t == TargetType.METHOD_REFERENCE) {
            buf.writeU2(bci(((TypeAnnotation.OffsetTarget) target).target()));
            return;
        }
        TypeAnnotation.TypeArgumentTarget a = (TypeAnnotation.TypeArgumentTarget) target;
        buf.writeU2(bci(a.target()));
        buf.writeU1(a.typeArgumentIndex());
    }

    // ---- `StackMapTable` ----------------------------------------------------------------------

    // Se escriben todos como `full_frame`. Es válido —§4.10.1 acepta cualquier codificación que
    // describa el estado correcto— y es la única forma posible sin reconstruir la cadena de deltas
    // que el modelo ya descomprimió al leer. El archivo queda más grande, no más flojo.
    private static void writeStackMapTable(BufWriter buf, StackMapTableAttribute attr) {
        List<StackMapFrameInfo> fs = attr.entries();
        buf.writeU2(fs.size());
        int previous = -1;
        for (int i = 0; i < fs.size(); i++) {
            StackMapFrameInfo f = fs.get(i);
            int offset = bci(f.target());
            buf.writeU1(255);
            buf.writeU2(offset - previous - 1);
            previous = offset;
            writeVerificationTypes(buf, f.locals());
            writeVerificationTypes(buf, f.stack());
        }
    }

    private static void writeVerificationTypes(BufWriter buf, List<VerificationTypeInfo> ts) {
        buf.writeU2(ts.size());
        for (int i = 0; i < ts.size(); i++) {
            VerificationTypeInfo t = ts.get(i);
            buf.writeU1(t.tag());
            if (t.tag() == VerificationTypeInfo.ITEM_OBJECT) {
                buf.writeIndex(((ObjectVerificationTypeInfo) t).className());
            } else if (t.tag() == VerificationTypeInfo.ITEM_UNINITIALIZED) {
                buf.writeU2(bci(((UninitializedVerificationTypeInfo) t).newTarget()));
            }
        }
    }
}
