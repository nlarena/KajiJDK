package jdk.internal.classfile.impl;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFileVersion;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.FieldTransform;
import java.lang.classfile.Interfaces;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.Superclass;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * El {@link ClassBuilder} que escribe bytes de verdad.
 *
 * <p>Acumula lo que le dan y al final lo serializa en el orden del formato (JVMS 4.1), que **no** es
 * el orden en que llega: el pool va primero en el archivo y se termina de llenar último, porque cada
 * método que se escribe puede agregarle entradas. Por eso el cuerpo se arma en un búfer aparte y
 * recién al cerrar se pegan pool y cuerpo.
 *
 * <p>Cuando un elemento se da dos veces --dos `AccessFlags`, dos superclases-- **gana el último**.
 * Es lo que hace componibles a las transformaciones: una que quiere cambiar las banderas escribe las
 * suyas después de dejar pasar las originales, sin tener que filtrarlas primero.
 */
public final class DirectClassBuilder implements ClassBuilder {

    private final ConstantPoolBuilder pool;
    private final ClassEntry thisClass;
    private int major = 69;
    private int minor;
    private int flags = 0x0020; // ACC_SUPER, que es lo que emite cualquier clase moderna
    private ClassEntry superclass;
    private List<ClassEntry> interfaces = new ArrayList<ClassEntry>();
    private final List<byte[]> fields = new ArrayList<byte[]>();
    private final List<byte[]> methods = new ArrayList<byte[]>();
    private final List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();

    /** Un constructor para esa clase, con ese pool. */
    public DirectClassBuilder(ConstantPoolBuilder pool, ClassEntry thisClass) {
        this.pool = pool;
        this.thisClass = thisClass;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    public ClassBuilder with(ClassElement e) {
        if (e instanceof AccessFlags) {
            this.flags = ((AccessFlags) e).flagsMask();
            return this;
        }
        if (e instanceof ClassFileVersion) {
            this.major = ((ClassFileVersion) e).majorVersion();
            this.minor = ((ClassFileVersion) e).minorVersion();
            return this;
        }
        if (e instanceof Superclass) {
            this.superclass = ((Superclass) e).superclassEntry();
            return this;
        }
        if (e instanceof Interfaces) {
            this.interfaces = new ArrayList<ClassEntry>(((Interfaces) e).interfaces());
            return this;
        }
        if (e instanceof FieldModel) {
            FieldModel f = (FieldModel) e;
            return this.transformField(f, FieldTransform.ACCEPT_ALL);
        }
        if (e instanceof MethodModel) {
            MethodModel m = (MethodModel) e;
            return this.transformMethod(m, MethodTransform.ACCEPT_ALL);
        }
        if (e instanceof Attribute) {
            this.attributes.add((Attribute<?>) e);
            return this;
        }
        throw new IllegalArgumentException("no se sabe escribir el elemento de clase " + e);
    }

    public ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor,
            Consumer<FieldBuilder> handler) {
        DirectFieldBuilder fb = new DirectFieldBuilder(this.pool, name, descriptor);
        handler.accept(fb);
        this.fields.add(fb.serialize());
        return this;
    }

    public ClassBuilder transformField(FieldModel field, FieldTransform transform) {
        DirectFieldBuilder fb =
                new DirectFieldBuilder(this.pool, field.fieldName(), field.fieldType());
        transform.atStart(fb);
        for (FieldElement e : field) {
            transform.accept(fb, e);
        }
        transform.atEnd(fb);
        this.fields.add(fb.serialize());
        return this;
    }

    public ClassBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int methodFlags,
            Consumer<MethodBuilder> handler) {
        DirectMethodBuilder mb =
                new DirectMethodBuilder(this.pool, name, descriptor, methodFlags);
        handler.accept(mb);
        this.methods.add(mb.serialize());
        return this;
    }

    public ClassBuilder transformMethod(MethodModel method, MethodTransform transform) {
        DirectMethodBuilder mb = new DirectMethodBuilder(this.pool, method.methodName(),
                method.methodType(), method.flags().flagsMask());
        transform.atStart(mb);
        for (MethodElement e : method) {
            transform.accept(mb, e);
        }
        transform.atEnd(mb);
        this.methods.add(mb.serialize());
        return this;
    }

    /** Los bytes de la clase. */
    public byte[] build() {
        // El cuerpo primero, para que el pool termine de llenarse; el encabezado después.
        BufWriterImpl body = new BufWriterImpl(this.pool);
        body.writeU2(this.flags);
        body.writeIndex(this.thisClass);
        body.writeIndexOrZero(this.superclass);
        body.writeU2(this.interfaces.size());
        for (int i = 0; i < this.interfaces.size(); i++) {
            body.writeIndex(this.interfaces.get(i));
        }
        body.writeU2(this.fields.size());
        for (int i = 0; i < this.fields.size(); i++) {
            body.writeBytes(this.fields.get(i));
        }
        body.writeU2(this.methods.size());
        for (int i = 0; i < this.methods.size(); i++) {
            body.writeBytes(this.methods.get(i));
        }
        // `BootstrapMethods` no es un atributo que el llamador escriba: lo genera el pool cuando
        // alguien pide un `invokedynamic`. Por eso se cuenta acá y no en la lista de atributos.
        boolean bsm = PoolWriter.hasBootstrapMethods(this.pool);
        body.writeU2(this.attributes.size() + (bsm ? 1 : 0));
        for (int i = 0; i < this.attributes.size(); i++) {
            AttributeWriter.write(body, this.attributes.get(i));
        }
        if (bsm) {
            PoolWriter.writeBootstrapMethods(body, this.pool);
        }

        BufWriterImpl out = new BufWriterImpl(this.pool);
        out.writeInt(0xCAFEBABE);
        out.writeU2(this.minor);
        out.writeU2(this.major);
        PoolWriter.writePool(out, this.pool);
        out.writeBytes(body.toByteArray());
        return out.toByteArray();
    }
}

/** El {@link FieldBuilder} que escribe bytes. */
final class DirectFieldBuilder implements FieldBuilder {

    private final ConstantPoolBuilder pool;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private int flags;
    private final List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();

    DirectFieldBuilder(ConstantPoolBuilder pool, Utf8Entry name, Utf8Entry descriptor) {
        this.pool = pool;
        this.name = name;
        this.descriptor = descriptor;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    public FieldBuilder with(FieldElement e) {
        if (e instanceof AccessFlags) {
            this.flags = ((AccessFlags) e).flagsMask();
            return this;
        }
        if (e instanceof Attribute) {
            this.attributes.add((Attribute<?>) e);
            return this;
        }
        throw new IllegalArgumentException("no se sabe escribir el elemento de campo " + e);
    }

    byte[] serialize() {
        BufWriterImpl b = new BufWriterImpl(this.pool);
        b.writeU2(this.flags);
        b.writeIndex(this.name);
        b.writeIndex(this.descriptor);
        b.writeU2(this.attributes.size());
        for (int i = 0; i < this.attributes.size(); i++) {
            AttributeWriter.write(b, this.attributes.get(i));
        }
        return b.toByteArray();
    }
}

/** El {@link MethodBuilder} que escribe bytes. */
final class DirectMethodBuilder implements MethodBuilder {

    private final ConstantPoolBuilder pool;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private int flags;
    private final List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
    private DirectCodeBuilder code;

    DirectMethodBuilder(ConstantPoolBuilder pool, Utf8Entry name, Utf8Entry descriptor,
            int flags) {
        this.pool = pool;
        this.name = name;
        this.descriptor = descriptor;
        this.flags = flags;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    public MethodBuilder with(MethodElement e) {
        if (e instanceof AccessFlags) {
            this.flags = ((AccessFlags) e).flagsMask();
            return this;
        }
        if (e instanceof CodeModel) {
            return this.transformCode((CodeModel) e, CodeTransform.ACCEPT_ALL);
        }
        if (e instanceof Attribute) {
            this.attributes.add((Attribute<?>) e);
            return this;
        }
        throw new IllegalArgumentException("no se sabe escribir el elemento de metodo " + e);
    }

    public MethodBuilder withCode(Consumer<CodeBuilder> handler) {
        DirectCodeBuilder cb = this.newCodeBuilder();
        handler.accept(cb);
        this.code = cb;
        return this;
    }

    public MethodBuilder transformCode(CodeModel model, CodeTransform transform) {
        DirectCodeBuilder cb = this.newCodeBuilder();
        transform.atStart(cb);
        for (java.lang.classfile.CodeElement e : model) {
            transform.accept(cb, e);
        }
        transform.atEnd(cb);
        this.code = cb;
        return this;
    }

    private DirectCodeBuilder newCodeBuilder() {
        return new DirectCodeBuilder(this.pool, this.descriptor.stringValue(),
                (this.flags & 0x0008) != 0);
    }

    byte[] serialize() {
        BufWriterImpl b = new BufWriterImpl(this.pool);
        b.writeU2(this.flags);
        b.writeIndex(this.name);
        b.writeIndex(this.descriptor);
        b.writeU2(this.attributes.size() + (this.code == null ? 0 : 1));
        if (this.code != null) {
            this.code.writeCode(b);
        }
        for (int i = 0; i < this.attributes.size(); i++) {
            AttributeWriter.write(b, this.attributes.get(i));
        }
        return b.toByteArray();
    }
}
