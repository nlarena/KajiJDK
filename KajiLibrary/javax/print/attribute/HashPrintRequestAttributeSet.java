package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintRequestAttributeSet -- un {@link HashAttributeSet} que
 * solo acepta {@link PrintRequestAttribute}, o sea atributos de pedido de impresion.
 *
 * <p>Sin cuerpo, igual que sus tres hermanas: los constructores le pasan
 * {@code PrintRequestAttribute.class} a la clase base y la restriccion la hace
 * {@code HashAttributeSet.add}. Ver {@link HashDocAttributeSet} para la explicacion completa del
 * mecanismo.
 */
public class HashPrintRequestAttributeSet extends HashAttributeSet
        implements PrintRequestAttributeSet, Serializable {

    private static final long serialVersionUID = 2364756266107751933L;

    /** Vacio. */
    public HashPrintRequestAttributeSet() {
        super(PrintRequestAttribute.class);
    }

    /** Con un atributo. NullPointerException si es null. */
    public HashPrintRequestAttributeSet(PrintRequestAttribute attribute) {
        super(attribute, PrintRequestAttribute.class);
    }

    /** Con los de otro conjunto del mismo tipo. Un conjunto null da el conjunto vacio. */
    public HashPrintRequestAttributeSet(PrintRequestAttributeSet attributes) {
        super(attributes, PrintRequestAttribute.class);
    }

    /** Con los del arreglo, en orden: si hay dos de la misma categoria gana el ultimo. */
    public HashPrintRequestAttributeSet(PrintRequestAttribute[] attributes) {
        super(attributes, PrintRequestAttribute.class);
    }
}
