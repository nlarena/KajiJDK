package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.util.function.Consumer;

/**
 * Donde se escribe lo que va a ser un `.class`: se le van dando elementos y él los acumula.
 *
 * <p>Es un {@link Consumer} de sus elementos, y eso no es decoración: hace que un constructor se
 * pueda pasar donde se espera un consumidor —a un `forEach`, a un `Stream`— y que copiar un modelo
 * entero sea `model.forEach(builder)`.
 *
 * <p>Los dos parámetros de tipo son el elemento que acepta y el propio constructor, para que
 * {@link #with} devuelva el tipo concreto y las llamadas se encadenen.
 */
public interface ClassFileBuilder<E extends ClassFileElement, B extends ClassFileBuilder<E, B>>
        extends Consumer<E> {

    /** Agrega ese elemento y devuelve este constructor, para encadenar. */
    B with(E e);

    /**
     * El pool de constantes de lo que se está escribiendo.
     *
     * <p>Se expone porque quien escribe instrucciones necesita entradas: un `invokevirtual` lleva un
     * índice, no un nombre. Las fábricas de {@link CodeBuilder} que reciben un `ClassDesc` lo usan
     * por dentro, así que sólo hace falta tocarlo para algo que esas fábricas no cubran.
     */
    ConstantPoolBuilder constantPool();

    /** Igual que {@link #with}, para poder pasar el constructor como {@link Consumer}. */
    default void accept(E e) {
        this.with(e);
    }

    /**
     * Recorre ese modelo aplicando esa transformación, y escribe el resultado acá.
     *
     * <p>El orden --`atStart`, un `accept` por elemento, `atEnd`-- es parte del contrato: es lo que
     * le permite a una transformación agregar algo que en el original no estaba. Ver
     * {@link ClassFileTransform}.
     */
    default B transform(CompoundElement<E> model, ClassFileTransform<?, E, B> transform) {
        // El cast de `this` a `B`: la interfaz no puede declarar que `this` es un `B`, y toda
        // implementación lo es por construcción -- es lo que significa el parámetro-- así que no
        // puede fallar.
        B self = (B) this;
        transform.atStart(self);
        for (E e : model) {
            transform.accept(self, e);
        }
        transform.atEnd(self);
        return self;
    }
}
