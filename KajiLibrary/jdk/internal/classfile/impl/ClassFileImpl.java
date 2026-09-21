package jdk.internal.classfile.impl;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What `ClassFile.of()` returns. The entry door, and nothing more: reading is done by
 * {@link ClassReaderImpl} with {@link ClassModelImpl}, and writing by {@link DirectClassBuilder}.
 *
 * <p>SCOPE: `withOptions` keeps nothing. The JDK's options (`StackMapsOption`, `DeadCodeOption`,
 * `AttributeMapperOption`, ...) turn on and off behaviours that here have no two forms: no stack
 * maps are generated --so `StackMapsOption` has nothing to choose-- and there are no custom mappers
 * to register. Returning `this` is not ignoring an option in silence: there is no `Option` instance
 * that can be passed to it, and the method says so by throwing.
 */
public final class ClassFileImpl implements ClassFile {

    public ClassFileImpl() {
    }

    public ClassFile withOptions(Option... options) {
        if (options == null) {
            throw new NullPointerException("options");
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i] == null) {
                throw new NullPointerException("options[" + i + "]");
            }
            throw new IllegalArgumentException(
                    "unrecognised option: " + options[i]
                            + " (this implementation defines no Option)");
        }
        return this;
    }

    public ClassModel parse(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        return new ClassModelImpl(new ClassReaderImpl(bytes, NoCustomMappers.INSTANCE));
    }

    public byte[] build(ClassEntry thisClassEntry, ConstantPoolBuilder constantPool,
            Consumer<ClassBuilder> handler) {
        if (thisClassEntry == null || constantPool == null || handler == null) {
            throw new NullPointerException();
        }
        DirectClassBuilder cb = new DirectClassBuilder(constantPool, thisClassEntry);
        handler.accept(cb);
        return cb.build();
    }

    /**
     * It copies the model element by element through the transformation.
     *
     * <p>The format version is copied first and separately: it is not an element the model emits
     * --`ClassModel` exposes it as two integers-- so without this the new class would come out with
     * the builder's default version and not with the original's.
     */
    public byte[] transformClass(ClassModel model, ClassEntry newClassName,
            ClassTransform transform) {
        if (model == null || newClassName == null || transform == null) {
            throw new NullPointerException();
        }
        ConstantPoolBuilder cp = ConstantPoolBuilder.of();
        DirectClassBuilder cb = new DirectClassBuilder(cp, newClassName);
        cb.withVersion(model.majorVersion(), model.minorVersion());
        transform.atStart(cb);
        for (ClassElement e : model) {
            transform.accept(cb, e);
        }
        transform.atEnd(cb);
        return cb.build();
    }

    /** See the javadoc of {@link ClassFile#verify}: it checks the structure, not the type flow. */
    public List<VerifyError> verify(byte[] bytes) {
        List<VerifyError> out = new ArrayList<VerifyError>();
        if (bytes == null) {
            out.add(new VerifyError("there are no bytes to verify"));
            return out;
        }
        try {
            ClassModel m = this.parse(bytes);
            ClassFileImpl.walk(m, out);
        } catch (IllegalArgumentException e) {
            out.add(new VerifyError(e.getMessage()));
        }
        return out;
    }

    /** See the javadoc of {@link ClassFile#verify}. */
    public List<VerifyError> verify(ClassModel model) {
        List<VerifyError> out = new ArrayList<VerifyError>();
        if (model == null) {
            out.add(new VerifyError("there is no model to verify"));
            return out;
        }
        try {
            ClassFileImpl.walk(model, out);
        } catch (IllegalArgumentException e) {
            out.add(new VerifyError(e.getMessage()));
        }
        return out;
    }

    // The complete walk of the model. It looks like it does nothing, and it does the only thing
    // this `verify` can honestly do: **force everything to be read**. The field and method
    // attributes are read when the model is built; what is lazy is a method's `Code` body, which is
    // decoded the first time it is asked for -- so a file with a broken body in the last method
    // parses without complaint. Touching each piece is what brings those errors out. (The note said
    // a method's attributes are not read until somebody asks for them; they are read in the
    // constructor, and only the body waits.)
    private static void walk(ClassModel m, List<VerifyError> out) {
        m.thisClass();
        m.superclass();
        m.interfaces();
        m.attributes();
        List<java.lang.classfile.FieldModel> fs = m.fields();
        for (int i = 0; i < fs.size(); i++) {
            try {
                fs.get(i).attributes();
            } catch (IllegalArgumentException e) {
                out.add(new VerifyError(
                        "field " + fs.get(i).fieldName().stringValue() + ": " + e.getMessage()));
            }
        }
        List<java.lang.classfile.MethodModel> ms = m.methods();
        for (int i = 0; i < ms.size(); i++) {
            try {
                ms.get(i).attributes();
                java.util.Optional<java.lang.classfile.CodeModel> c = ms.get(i).code();
                if (c.isPresent()) {
                    c.get().elementList();
                }
            } catch (IllegalArgumentException e) {
                out.add(new VerifyError(
                        "method " + ms.get(i).methodName().stringValue() + ": " + e.getMessage()));
            }
        }
    }

    public String toString() {
        return "ClassFile[]";
    }
}
