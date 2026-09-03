package java.lang.classfile.attribute;

import java.lang.classfile.Label;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// Un cuadro de `StackMapTable` (JVMS §4.7.4): el estado de tipos —variables locales y pila de
// operandos— que el verificador espera en un punto del código al que se puede llegar por más de un
// camino.
//
// El formato guarda los cuadros COMPRIMIDOS: cada uno se expresa como una diferencia contra el
// anterior (`same_frame`, `chop_frame`, `append_frame`, …) y el bci es un delta. Esta interfaz los
// entrega ya expandidos —`locals()` y `stack()` son el estado completo— porque una diferencia sólo
// significa algo en la posición donde está. `frameType()` conserva el byte original, que es lo que
// hace falta para reescribir el atributo tal cual estaba.
public interface StackMapFrameInfo {

    /** El byte `frame_type` original. */
    int frameType();

    /** El bci al que corresponde el cuadro. */
    Label target();

    /** Las variables locales, expandidas. */
    List<VerificationTypeInfo> locals();

    /** La pila de operandos, expandida. */
    List<VerificationTypeInfo> stack();

    /** Un cuadro con este estado, en la forma más comprimida no la elige quien lo construye. */
    public static StackMapFrameInfo of(Label target, List<VerificationTypeInfo> locals,
            List<VerificationTypeInfo> stack) {
        return TypedAttributes.stackMapFrame(target, locals, stack);
    }

    /** Un tipo del verificador: qué hay en una ranura o en una posición de la pila. */
    public interface VerificationTypeInfo {

        /** `top`: la ranura no tiene un valor utilizable. */
        public static final int ITEM_TOP = 0;
        /** `int`, y también `boolean`, `byte`, `char` y `short`. */
        public static final int ITEM_INTEGER = 1;
        /** `float`. */
        public static final int ITEM_FLOAT = 2;
        /** `double`. */
        public static final int ITEM_DOUBLE = 3;
        /** `long`. */
        public static final int ITEM_LONG = 4;
        /** `null`. */
        public static final int ITEM_NULL = 5;
        /** El `this` de un constructor, antes de llamar al de la superclase. */
        public static final int ITEM_UNINITIALIZED_THIS = 6;
        /** Una referencia a una clase concreta. */
        public static final int ITEM_OBJECT = 7;
        /** Un objeto recién creado por un `new` que todavía no se inicializó. */
        public static final int ITEM_UNINITIALIZED = 8;

        /** La etiqueta `ITEM_*`. */
        int tag();
    }

    /** Los siete tipos del verificador que no llevan nada más que su etiqueta. */
    public enum SimpleVerificationTypeInfo implements VerificationTypeInfo {

        TOP(0),
        INTEGER(1),
        FLOAT(2),
        DOUBLE(3),
        LONG(4),
        NULL(5),
        UNINITIALIZED_THIS(6);

        private final int tag;

        private SimpleVerificationTypeInfo(int tag) {
            this.tag = tag;
        }

        public int tag() {
            return this.tag;
        }
    }

    /** Una referencia a una clase concreta. */
    public interface ObjectVerificationTypeInfo extends VerificationTypeInfo {

        /** La clase. */
        ClassEntry className();

        /** La clase. */
        default ClassDesc classSymbol() {
            return className().asSymbol();
        }

        /** El tipo de esta clase. */
        public static ObjectVerificationTypeInfo of(ClassEntry className) {
            return TypedAttributes.objectVerificationType(className);
        }

        /** El tipo de esta clase. */
        public static ObjectVerificationTypeInfo of(ClassDesc classDesc) {
            return TypedAttributes.objectVerificationType(TypedAttributes.classEntry(classDesc));
        }
    }

    /**
     * Un objeto creado por el `new` que está en `newTarget()` y todavía sin inicializar. El bci del
     * `new` es parte del TIPO: dos objetos sin inicializar creados en lugares distintos son tipos
     * distintos para el verificador, que es lo que le permite comprobar que cada uno recibe su
     * `invokespecial`.
     */
    public interface UninitializedVerificationTypeInfo extends VerificationTypeInfo {

        /** El bci del `new` que lo creó. */
        Label newTarget();

        /** El tipo del objeto creado en `newTarget`. */
        public static UninitializedVerificationTypeInfo of(Label newTarget) {
            return TypedAttributes.uninitializedVerificationType(newTarget);
        }
    }
}
