package java.lang.classfile;

import java.lang.classfile.instruction.ExceptionCatch;
import java.util.List;
import java.util.Optional;

// El cuerpo de un método: el atributo `Code` (JVMS §4.7.3) visto como una secuencia de piezas.
public interface CodeModel extends CompoundElement<CodeElement>, AttributedElement, MethodElement {

    /** El método que lo contiene, si este modelo salió de leer uno. */
    Optional<MethodModel> parent();

    /** La tabla de manejadores de excepción, en el orden del archivo. */
    List<ExceptionCatch> exceptionHandlers();
}
