package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;

// La marca de que una {@link Label} cae en este punto del cuerpo. No ocupa bytes en el arreglo
// `code`: es lo que convierte una posición en una identidad, y por eso es una pseudoinstrucción.
public interface LabelTarget extends PseudoInstruction {

    /** La etiqueta que se resuelve acá. */
    Label label();
}
