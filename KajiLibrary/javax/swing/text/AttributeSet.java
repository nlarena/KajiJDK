package javax.swing.text;

import java.util.Enumeration;

/**
 * Un conjunto de atributos de estilo, de sólo lectura y encadenable.
 *
 * <p>Lo que lo distingue de un mapa común es el **padre**. Un conjunto puede resolver un atributo
 * que no tiene delegando en otro, y así un párrafo hereda el estilo del documento sin copiarlo. Por
 * eso {@link #getAttribute} puede contestar algo que {@link #isDefined} niega: lo primero busca en
 * la cadena, lo segundo mira sólo este eslabón.
 *
 * <p>Las cuatro interfaces anidadas no declaran nada: son **marcas** que clasifican una clave según
 * a qué se aplica —al carácter, al párrafo, al color, a la fuente— para que quien componga estilos
 * sepa qué puede mezclar con qué.
 *
 * <p><strong>Es lo único de `javax.swing.text` que esta biblioteca trae.</strong> Está porque
 * {@code javax.accessibility.AccessibleText} la nombra, y sin ella ese paquete no podría declarar
 * dos de sus métodos. Escribirla entera —es autocontenida y son ocho métodos— era mejor que dejar
 * incompletos a los que dependen de ella.
 */
public interface AttributeSet {

    /** La clave con la que un conjunto guarda su nombre. */
    Object NameAttribute = "name";

    /** La clave con la que un conjunto guarda a su padre. */
    Object ResolveAttribute = "resolver";

    /** Marca de las claves que se aplican a un carácter. */
    public interface CharacterAttribute {
    }

    /** Marca de las claves que se aplican a un párrafo. */
    public interface ParagraphAttribute {
    }

    /** Marca de las claves de color. */
    public interface ColorAttribute {
    }

    /** Marca de las claves de fuente. */
    public interface FontAttribute {
    }

    /** Cuántos atributos tiene **este** conjunto, sin contar los heredados. */
    int getAttributeCount();

    /** Si este conjunto define esa clave por sí mismo. */
    boolean isDefined(Object attrName);

    /** Si los dos conjuntos definen exactamente lo mismo. */
    boolean isEqual(AttributeSet attr);

    /** Una copia independiente. */
    AttributeSet copyAttributes();

    /**
     * El valor de esa clave.
     *
     * <p>Busca en la cadena de padres, así que puede devolver algo que {@link #isDefined} niegue.
     *
     * @return el valor, o `null` si no está en ningún eslabón
     */
    Object getAttribute(Object key);

    /** Las claves de **este** conjunto, sin las heredadas. */
    Enumeration<?> getAttributeNames();

    /** Si ese par clave-valor está en la cadena. */
    boolean containsAttribute(Object name, Object value);

    /** Si todos esos pares están en la cadena. */
    boolean containsAttributes(AttributeSet attributes);

    /** El conjunto en el que se sigue buscando, o `null` si éste es el último. */
    AttributeSet getResolveParent();
}
