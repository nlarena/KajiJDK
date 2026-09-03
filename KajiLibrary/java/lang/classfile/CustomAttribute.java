package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;

// La clase base para un atributo que el JVMS no define y que una aplicación quiere modelar igual.
// Quien la extiende trae su propio {@link AttributeMapper} y con eso el atributo entra y sale del
// archivo como cualquiera de los conocidos.
//
// Implementa las cuatro interfaces de elemento porque un atributo a medida puede aparecer en
// cualquiera de los cuatro lugares donde el formato admite atributos.
public abstract class CustomAttribute<T extends CustomAttribute<T>>
        implements Attribute<T>, CodeElement, ClassElement, MethodElement, FieldElement {

    private final AttributeMapper<T> mapper;

    /** Con el mapeador que sabe leer y escribir este atributo. */
    protected CustomAttribute(AttributeMapper<T> mapper) {
        this.mapper = mapper;
    }

    public final AttributeMapper<T> attributeMapper() {
        return this.mapper;
    }

    public Utf8Entry attributeName() {
        throw new UnsupportedOperationException(
                "un CustomAttribute construido a mano no tiene entrada de pool todavía");
    }
}
