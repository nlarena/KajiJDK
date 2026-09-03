package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;

// Un atributo cuyo nombre no está en {@link java.lang.classfile.Attributes} ni entre los mapeadores
// a medida del lector. El formato obliga a poder saltearlo —el largo está en la cabecera— y esta
// interfaz permite además conservarlo: el nombre y los bytes salen tal cual entraron.
//
// No tiene fábrica, y no es un olvido: un atributo desconocido sólo aparece al LEER. Para inventar
// uno está {@link java.lang.classfile.CustomAttribute}, que trae su propio mapeador.
public interface UnknownAttribute extends Attribute<UnknownAttribute>, ClassElement, MethodElement,
        FieldElement, CodeElement {

    /** Una copia del cuerpo del atributo, sin el nombre ni el largo. */
    byte[] contents();
}
