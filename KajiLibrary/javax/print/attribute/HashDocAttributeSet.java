package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashDocAttributeSet -- un {@link HashAttributeSet} que solo
 * acepta {@link DocAttribute}.
 *
 * <p>No tiene cuerpo, y eso es lo interesante: los cuatro constructores le pasan
 * {@code DocAttribute.class} al de la clase base y ahi termina la clase. Toda la restriccion la
 * hace {@code HashAttributeSet.add} verificando contra esa interfaz, asi que meter un atributo que
 * no sea de documento --por ejemplo por {@code addAll}, donde el tipo estatico no ayuda-- sale por
 * {@code ClassCastException} en tiempo de ejecucion.
 *
 * <p>Las firmas si estrechan el tipo donde pueden: el constructor toma un {@code DocAttribute[]} y
 * no un {@code Attribute[]}, de modo que el caso comun se atrapa en compilacion y solo queda para
 * la ejecucion lo que la firma no puede expresar.
 */
public class HashDocAttributeSet extends HashAttributeSet implements DocAttributeSet, Serializable {

    private static final long serialVersionUID = -1128534486061432528L;

    /** Vacio. */
    public HashDocAttributeSet() {
        super(DocAttribute.class);
    }

    /** Con un atributo de documento. NullPointerException si es null. */
    public HashDocAttributeSet(DocAttribute attribute) {
        super(attribute, DocAttribute.class);
    }

    /** Con los de otro conjunto de documento. Un conjunto null da el conjunto vacio. */
    public HashDocAttributeSet(DocAttributeSet attributes) {
        super(attributes, DocAttribute.class);
    }

    /** Con los del arreglo, en orden: si hay dos de la misma categoria gana el ultimo. */
    public HashDocAttributeSet(DocAttribute[] attributes) {
        super(attributes, DocAttribute.class);
    }
}
