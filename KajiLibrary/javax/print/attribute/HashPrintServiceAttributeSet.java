package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintServiceAttributeSet -- un {@link HashAttributeSet} que
 * solo acepta {@link PrintServiceAttribute}, o sea atributos de servicio de impresion.
 *
 * <p>Sin cuerpo, igual que sus tres hermanas: los constructores le pasan
 * {@code PrintServiceAttribute.class} a la clase base y la restriccion la hace
 * {@code HashAttributeSet.add}. Ver {@link HashDocAttributeSet} para la explicacion completa del
 * mecanismo.
 */
public class HashPrintServiceAttributeSet extends HashAttributeSet
        implements PrintServiceAttributeSet, Serializable {

    private static final long serialVersionUID = 6642904616179203070L;

    /** Vacio. */
    public HashPrintServiceAttributeSet() {
        super(PrintServiceAttribute.class);
    }

    /** Con un atributo. NullPointerException si es null. */
    public HashPrintServiceAttributeSet(PrintServiceAttribute attribute) {
        super(attribute, PrintServiceAttribute.class);
    }

    /** Con los de otro conjunto del mismo tipo. Un conjunto null da el conjunto vacio. */
    public HashPrintServiceAttributeSet(PrintServiceAttributeSet attributes) {
        super(attributes, PrintServiceAttribute.class);
    }

    /** Con los del arreglo, en orden: si hay dos de la misma categoria gana el ultimo. */
    public HashPrintServiceAttributeSet(PrintServiceAttribute[] attributes) {
        super(attributes, PrintServiceAttribute.class);
    }
}
