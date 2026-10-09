package java.lang.classfile;

import java.lang.classfile.instruction.ExceptionCatch;
import java.util.List;
import java.util.Optional;

// A method's body: the `Code` attribute (JVMS §4.7.3) seen as a sequence of pieces.
public interface CodeModel extends CompoundElement<CodeElement>, AttributedElement, MethodElement {

    /** The method containing it, if this model came out of reading one. */
    Optional<MethodModel> parent();

    /** The exception handler table, in file order. */
    List<ExceptionCatch> exceptionHandlers();
}
