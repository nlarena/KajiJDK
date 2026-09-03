package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Label;

// `Code` (JVMS §4.7.3) visto como atributo. Es la vista de BAJO nivel del mismo objeto que
// {@link CodeModel} muestra pieza por pieza: acá están los bytes crudos y los bci, allá las
// instrucciones. Los dos son la misma instancia — por eso esta interfaz extiende a aquélla.
//
// No tiene fábrica: un `Code` sólo existe dentro de un método y su contenido se arma con un
// `CodeBuilder`, que es a la vez quien resuelve las etiquetas a bci. Fabricarlo suelto daría un
// atributo cuyos `labelToBci` no significan nada.
public interface CodeAttribute extends Attribute<CodeAttribute>, CodeModel {

    /** El `max_locals` del atributo. */
    int maxLocals();

    /** El `max_stack` del atributo. */
    int maxStack();

    /** El largo del arreglo `code`. */
    int codeLength();

    /** Una copia del arreglo `code`. */
    byte[] codeArray();

    /** El bci de esta etiqueta, o -1 si no es de este cuerpo. */
    int labelToBci(Label label);
}
