package jdk.internal.classfile.impl;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.Utf8Entry;

// Un atributo leído sin interpretar: el `Utf8` del nombre, el mapeador que lo reconoció y el cuerpo
// en bytes. Es lo que KajiLibrary devuelve para TODOS los atributos, incluidos los que el JVMS
// define — ver la nota de alcance en `java.lang.classfile.Attributes`.
//
// Implementa las cuatro interfaces de elemento porque un atributo puede aparecer en los cuatro
// lugares donde el formato los admite, y el modelo que lo contiene lo emite como una de sus piezas.
public final class RawAttribute
        implements Attribute<RawAttribute>, ClassElement, MethodElement, FieldElement, CodeElement {

    private final Utf8Entry name;
    private final AttributeMapper<RawAttribute> mapper;
    private final byte[] payload;

    public RawAttribute(Utf8Entry name, AttributeMapper<RawAttribute> mapper, byte[] payload) {
        this.name = name;
        this.mapper = mapper;
        this.payload = payload;
    }

    public Utf8Entry attributeName() {
        return this.name;
    }

    public AttributeMapper<RawAttribute> attributeMapper() {
        return this.mapper;
    }

    /** Una copia del cuerpo del atributo, sin el nombre ni el largo. */
    public byte[] payload() {
        byte[] copia = new byte[this.payload.length];
        System.arraycopy(this.payload, 0, copia, 0, this.payload.length);
        return copia;
    }

    /** El largo del cuerpo. */
    public int payloadLength() {
        return this.payload.length;
    }

    // Sin copiar: para quien escribe el atributo de vuelta.
    byte[] crudo() {
        return this.payload;
    }

    public String toString() {
        return "Attribute[" + this.name.stringValue() + ", " + this.payload.length + " bytes]";
    }
}
