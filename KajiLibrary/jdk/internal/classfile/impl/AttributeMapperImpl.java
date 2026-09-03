package jdk.internal.classfile.impl;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributeMapper.AttributeStability;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassReader;

// El mapeador de un atributo conocido por su nombre, que lo lee y lo escribe sin interpretarlo.
// Lee: copia el cuerpo. Escribe: nombre, largo y el mismo cuerpo. Esa simetría es lo que permite
// copiar un atributo de un archivo a otro aunque no se sepa qué dice.
public final class AttributeMapperImpl implements AttributeMapper<RawAttribute> {

    private final String name;
    private final AttributeStability stability;
    private final boolean allowMultiple;

    public AttributeMapperImpl(String name, AttributeStability stability, boolean allowMultiple) {
        this.name = name;
        this.stability = stability;
        this.allowMultiple = allowMultiple;
    }

    public String name() {
        return this.name;
    }

    // `pos` es el offset del primer byte del cuerpo. El largo se lee de los cuatro bytes que están
    // justo antes, que es donde el formato lo pone (§4.7).
    public RawAttribute readAttribute(AttributedElement enclosing, ClassReader cf, int pos) {
        int len = cf.readInt(pos - 4);
        if (len < 0 || pos + len > cf.classfileLength()) {
            throw new IllegalArgumentException(
                    "atributo " + this.name + " con largo " + len + " que no entra en el archivo");
        }
        // El local intermedio no es estilo: pasar la llamada genérica directo como argumento hace
        // que el compilador borre `T` a su cota y no encuentre el constructor (ver el informe).
        java.lang.classfile.constantpool.Utf8Entry name =
                cf.readEntryOrNull(pos - 6, java.lang.classfile.constantpool.Utf8Entry.class);
        return new RawAttribute(name, this, cf.readBytes(pos, len));
    }

    public void writeAttribute(BufWriter buf, RawAttribute attr) {
        buf.writeIndex(attr.attributeName());
        byte[] cuerpo = attr.crudo();
        buf.writeInt(cuerpo.length);
        buf.writeBytes(cuerpo);
    }

    public boolean allowMultiple() {
        return this.allowMultiple;
    }

    public AttributeStability stability() {
        return this.stability;
    }

    public String toString() {
        return "AttributeMapper[" + this.name + "]";
    }
}
