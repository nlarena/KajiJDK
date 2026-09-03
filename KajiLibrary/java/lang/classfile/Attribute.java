package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;

// Un atributo (JVMS §4.7): un nombre y un cuerpo cuyo formato depende del nombre. El parámetro de
// tipo se refiere a sí mismo —`Attribute<A extends Attribute<A>>`— para que `attributeMapper()`
// devuelva el mapeador de *este* atributo y no uno cualquiera.
public interface Attribute<A extends Attribute<A>> extends ClassFileElement {

    /** El `Utf8` con el nombre del atributo. */
    Utf8Entry attributeName();

    /** El mapeador que sabe leerlo y escribirlo. */
    AttributeMapper<A> attributeMapper();
}
