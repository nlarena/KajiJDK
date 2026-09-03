package jdk.internal.classfile.impl;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

// Lo que devuelve `ClassFile.of()`. Es deliberadamente delgado: todo el trabajo de leer está en
// `ClassReaderImpl` y `ClassModelImpl`; esta clase sólo es la puerta de entrada.
//
// ALCANCE: `withOptions` no guarda nada. Las opciones del JDK (`StackMapsOption`,
// `DeadCodeOption`, `AttributeMapperOption`, …) gobiernan al **escritor**, y el escritor no está;
// las dos que tocarían al lector —`AttributesProcessingOption` y `ConstantPoolSharingOption`— sólo
// tienen sentido con las clases anidadas de `ClassFile` que tampoco están, así que hoy no hay
// ninguna instancia de `Option` que se le pueda pasar a este método. Devolver `this` no es ignorar
// una opción en silencio: es que no hay opción que ignorar. Lo que sí hace es rechazar un `null`,
// para que el día que las haya el error salga acá y no al escribir.
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

    public String toString() {
        return "ClassFile[]";
    }
}
