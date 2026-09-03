package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import jdk.internal.classfile.impl.AccessFlagsImpl;
import jdk.internal.classfile.impl.ClassFileVersionImpl;
import jdk.internal.classfile.impl.InterfacesImpl;
import jdk.internal.classfile.impl.SuperclassImpl;

/**
 * Donde se escribe una clase.
 *
 * <p>Todo lo que una clase tiene --version, banderas, superclase, interfaces, campos, metodos,
 * atributos-- entra por {@link #with}, y estos metodos son atajos que arman el elemento que
 * corresponde. Uno puede escribir una clase entera sin usar ninguno; existen porque
 * `withSuperclass(CD_Object)` se lee mejor que `with(SuperclassImpl.of(...))`.
 *
 * <p><strong>Dos cosas no entran por elementos y por eso no estan aca</strong>: el nombre de la
 * clase y su pool de constantes. Los dos se fijan al empezar --son argumentos de
 * {@link ClassFile#build}-- porque una clase no puede cambiar de nombre a mitad de escritura sin
 * invalidar todas las referencias que ya se escribieron a si misma.
 */
public interface ClassBuilder extends ClassFileBuilder<ClassElement, ClassBuilder> {

    /** La version del formato. */
    default ClassBuilder withVersion(int major, int minor) {
        return this.with(new ClassFileVersionImpl(major, minor));
    }

    /** Las banderas de la clase, como mascara de bits. */
    default ClassBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.CLASS));
    }

    /** Las banderas de la clase. */
    default ClassBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.CLASS));
    }

    /** La superclase. */
    default ClassBuilder withSuperclass(ClassEntry superclassEntry) {
        return this.with(new SuperclassImpl(superclassEntry));
    }

    /** La superclase, por su descriptor. */
    default ClassBuilder withSuperclass(ClassDesc desc) {
        return this.withSuperclass(this.constantPool().classEntry(desc));
    }

    /** Las interfaces que implementa. */
    default ClassBuilder withInterfaces(List<ClassEntry> interfaces) {
        return this.with(new InterfacesImpl(interfaces));
    }

    /** Las interfaces que implementa. */
    default ClassBuilder withInterfaces(ClassEntry... interfaces) {
        List<ClassEntry> list = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.length; i++) {
            list.add(interfaces[i]);
        }
        return this.withInterfaces(list);
    }

    /** Las interfaces que implementa, por sus descriptores. */
    default ClassBuilder withInterfaceSymbols(List<ClassDesc> interfaces) {
        List<ClassEntry> list = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.size(); i++) {
            list.add(this.constantPool().classEntry(interfaces.get(i)));
        }
        return this.withInterfaces(list);
    }

    /** Las interfaces que implementa, por sus descriptores. */
    default ClassBuilder withInterfaceSymbols(ClassDesc... interfaces) {
        List<ClassDesc> list = new ArrayList<ClassDesc>();
        for (int i = 0; i < interfaces.length; i++) {
            list.add(interfaces[i]);
        }
        return this.withInterfaceSymbols(list);
    }

    /**
     * Un campo, con su cuerpo escrito por ese `Consumer`.
     *
     * <p>El nombre y el descriptor son argumentos y no elementos por lo mismo que el nombre de la
     * clase: identifican al campo, y cambiarlos a mitad seria estar escribiendo otro.
     */
    ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor, Consumer<FieldBuilder> handler);

    /** Un campo con esas banderas y nada mas. */
    default ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor, int flags) {
        return this.withField(name, descriptor, new FlagsOnlyField(flags));
    }

    /** Un campo, nombrandolo con texto y descriptor. */
    default ClassBuilder withField(String name, ClassDesc descriptor,
            Consumer<FieldBuilder> handler) {
        return this.withField(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), handler);
    }

    /** Un campo con esas banderas, nombrandolo con texto y descriptor. */
    default ClassBuilder withField(String name, ClassDesc descriptor, int flags) {
        return this.withField(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags);
    }

    /** Copia ese campo a traves de esa transformacion. */
    ClassBuilder transformField(FieldModel field, FieldTransform transform);

    /** Un metodo, con su cuerpo escrito por ese `Consumer`. */
    ClassBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<MethodBuilder> handler);

    /** Un metodo, nombrandolo con texto y descriptor. */
    default ClassBuilder withMethod(String name, MethodTypeDesc descriptor, int flags,
            Consumer<MethodBuilder> handler) {
        return this.withMethod(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags, handler);
    }

    /**
     * Un metodo cuyo `Consumer` escribe directamente el **cuerpo**.
     *
     * <p>La diferencia con {@link #withMethod} es una capa: alla el `Consumer` recibe un
     * {@link MethodBuilder} y tiene que llamar a `withCode`; aca recibe el {@link CodeBuilder} ya
     * abierto. Es el caso comun --un metodo con cuerpo y nada mas-- y ahorra el paso intermedio.
     */
    default ClassBuilder withMethodBody(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<CodeBuilder> handler) {
        return this.withMethod(name, descriptor, flags, new BodyOnlyMethod(handler));
    }

    /** Lo mismo, nombrando el metodo con texto y descriptor. */
    default ClassBuilder withMethodBody(String name, MethodTypeDesc descriptor, int flags,
            Consumer<CodeBuilder> handler) {
        return this.withMethodBody(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags, handler);
    }

    /** Copia ese metodo a traves de esa transformacion. */
    ClassBuilder transformMethod(MethodModel method, MethodTransform transform);
}

// Los dos `Consumer` que los `default` de arriba necesitan.
//
// Son clases y no lambdas por una razon concreta y no por estilo: una lambda dentro de un `default`
// de una interfaz publica del arranque de la biblioteca obliga a que `LambdaMetafactory` este
// disponible en ese momento, y estas interfaces las carga el propio compilador. Con clases nombradas
// no hay `invokedynamic` que resolver.

final class FlagsOnlyField implements Consumer<FieldBuilder> {

    private final int flags;

    FlagsOnlyField(int flags) {
        this.flags = flags;
    }

    public void accept(FieldBuilder fb) {
        fb.withFlags(this.flags);
    }
}

final class BodyOnlyMethod implements Consumer<MethodBuilder> {

    private final Consumer<CodeBuilder> body;

    BodyOnlyMethod(Consumer<CodeBuilder> body) {
        this.body = body;
    }

    public void accept(MethodBuilder mb) {
        mb.withCode(this.body);
    }
}
