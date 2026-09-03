package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceDebugExtension` (JVMS §4.7.11): bytes libres para el depurador, en la práctica el mapa
// SMAP de JSR-45 que relaciona el bytecode con un fuente que no es Java (un JSP, por ejemplo). El
// JVMS dice que es UTF-8 modificado pero no impone estructura, así que la API lo entrega en bruto.
public interface SourceDebugExtensionAttribute
        extends Attribute<SourceDebugExtensionAttribute>, ClassElement {

    /** Una copia del cuerpo. */
    byte[] contents();

    /** El atributo con estos bytes. */
    public static SourceDebugExtensionAttribute of(byte[] contents) {
        return TypedAttributes.sourceDebugExtension(contents);
    }
}
