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
 * Lo que devuelve `ClassFile.of()`. La puerta de entrada, y nada más: leer lo hace
 * {@link ClassReaderImpl} con {@link ClassModelImpl}, y escribir {@link DirectClassBuilder}.
 *
 * <p>ALCANCE: `withOptions` no guarda nada. Las opciones del JDK (`StackMapsOption`,
 * `DeadCodeOption`, `AttributeMapperOption`, …) prenden y apagan comportamientos que acá no tienen
 * dos formas: no se generan mapas de pila --con lo cual `StackMapsOption` no tiene qué elegir-- y no
 * hay mapeadores a medida que registrar. Devolver `this` no es ignorar una opción en silencio: es
 * que no hay ninguna instancia de `Option` que se le pueda pasar, y el método lo dice tirando.
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
                    "opcion no reconocida: " + options[i]
                            + " (esta implementacion no define ninguna Option)");
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
     * Copia el modelo elemento por elemento a través de la transformación.
     *
     * <p>La versión del formato se copia primero y aparte: no es un elemento que el modelo emita
     * --`ClassModel` la expone como dos enteros-- así que sin esto la clase nueva saldría con la
     * versión por omisión del constructor y no con la del original.
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

    /** Ver el javadoc de {@link ClassFile#verify}: comprueba la estructura, no el flujo de tipos. */
    public List<VerifyError> verify(byte[] bytes) {
        List<VerifyError> out = new ArrayList<VerifyError>();
        if (bytes == null) {
            out.add(new VerifyError("no hay bytes que verificar"));
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

    /** Ver el javadoc de {@link ClassFile#verify}. */
    public List<VerifyError> verify(ClassModel model) {
        List<VerifyError> out = new ArrayList<VerifyError>();
        if (model == null) {
            out.add(new VerifyError("no hay modelo que verificar"));
            return out;
        }
        try {
            ClassFileImpl.walk(model, out);
        } catch (IllegalArgumentException e) {
            out.add(new VerifyError(e.getMessage()));
        }
        return out;
    }

    // El recorrido completo del modelo. Parece no hacer nada y hace lo único que este `verify` puede
    // hacer honestamente: **forzar la lectura de todo**. El modelo es perezoso -- los atributos de un
    // método no se leen hasta que alguien los pide-- así que un archivo con un atributo roto en el
    // último método se parsea sin quejarse. Tocar cada pieza es lo que hace salir esos errores.
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
                        "campo " + fs.get(i).fieldName().stringValue() + ": " + e.getMessage()));
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
                        "metodo " + ms.get(i).methodName().stringValue() + ": " + e.getMessage()));
            }
        }
    }

    public String toString() {
        return "ClassFile[]";
    }
}
