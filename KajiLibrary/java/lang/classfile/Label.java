package java.lang.classfile;

// Una posición dentro del cuerpo de un método, tratada como una identidad y no como un número. Es lo
// que hace que se pueda insertar código sin recalcular saltos: los destinos se nombran, y el offset
// se resuelve recién al escribir.
//
// No declara ningún miembro a propósito — el JDK tampoco. Una etiqueta sólo se compara por identidad
// y se resuelve contra el `CodeModel` o el `CodeBuilder` que la creó.
public interface Label {
}
