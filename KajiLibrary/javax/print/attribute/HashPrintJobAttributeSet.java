package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintJobAttributeSet -- un {@link HashAttributeSet} que
 * solo acepta {@link PrintJobAttribute}, o sea atributos de trabajo de impresion.
 *
 * <p>Sin cuerpo, igual que sus tres hermanas: los constructores le pasan
 * {@code PrintJobAttribute.class} a la clase base y la restriccion la hace
 * {@code HashAttributeSet.add}. Ver {@link HashDocAttributeSet} para la explicacion completa del
 * mecanismo.
 */
public class HashPrintJobAttributeSet extends HashAttributeSet
        implements PrintJobAttributeSet, Serializable {

    private static final long serialVersionUID = -4204473656070350348L;

    /** Vacio. */
    public HashPrintJobAttributeSet() {
        super(PrintJobAttribute.class);
    }

    /** Con un atributo. NullPointerException si es null. */
    public HashPrintJobAttributeSet(PrintJobAttribute attribute) {
        super(attribute, PrintJobAttribute.class);
    }

    /** Con los de otro conjunto del mismo tipo. Un conjunto null da el conjunto vacio. */
    public HashPrintJobAttributeSet(PrintJobAttributeSet attributes) {
        super(attributes, PrintJobAttribute.class);
    }

    /** Con los del arreglo, en orden: si hay dos de la misma categoria gana el ultimo. */
    public HashPrintJobAttributeSet(PrintJobAttribute[] attributes) {
        super(attributes, PrintJobAttribute.class);
    }
}
