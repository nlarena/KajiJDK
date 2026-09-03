package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.constantpool.ClassEntry;
import java.util.Optional;

// Una fila de la tabla `exception_table` del atributo `Code` (JVMS §4.7.3): el rango protegido, el
// destino del manejador y el tipo capturado. El tipo vacío es el `catch` de todo — lo que el
// compilador emite para un `finally`.
//
// Es una pseudoinstrucción y no una instrucción porque no ocupa bytes en el arreglo `code`: vive en
// una tabla aparte del atributo `Code` y sólo se refiere a posiciones de aquél.
public interface ExceptionCatch extends PseudoInstruction {

    /** Dónde empieza el manejador. */
    Label handler();

    /** Dónde empieza el rango protegido. */
    Label tryStart();

    /** Dónde termina el rango protegido, sin incluirlo. */
    Label tryEnd();

    /** El tipo capturado; vacío si captura todo. */
    Optional<ClassEntry> catchType();

    /** Una fila con estos valores. */
    public static ExceptionCatch of(Label handler, Label tryStart, Label tryEnd,
            Optional<ClassEntry> catchType) {
        return new jdk.internal.classfile.impl.ExceptionCatchImpl(handler, tryStart, tryEnd,
                catchType);
    }

    /** Una fila que captura todo, o sea la que el compilador emite para un `finally`. */
    public static ExceptionCatch of(Label handler, Label tryStart, Label tryEnd) {
        return new jdk.internal.classfile.impl.ExceptionCatchImpl(handler, tryStart, tryEnd,
                Optional.<ClassEntry>empty());
    }
}
