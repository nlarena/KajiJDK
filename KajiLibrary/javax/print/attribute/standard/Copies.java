package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/*
 * CABECERA DE FAMILIA -- los atributos {@code IntegerSyntax} de este paquete.
 *
 * <p>Un atributo entero es un numero con un rango legal y un nombre. Todo el mecanismo esta en
 * {@link javax.print.attribute.IntegerSyntax IntegerSyntax}; cada subclase de aca solo elige los
 * dos extremos y los pasa al constructor de tres argumentos, que es el que tira
 * {@code IllegalArgumentException} cuando el valor se sale.
 *
 * <p>El rango es lo unico que las distingue de verdad, y casi todas caen en tres moldes:
 * <ul>
 * <li><b>1..MAX_VALUE</b> -- las que cuentan cosas que se piden y no tiene sentido pedir cero:
 *     {@link Copies}, {@link NumberUp}.</li>
 * <li><b>0..MAX_VALUE</b> -- las que <em>miden</em> algo ya hecho o el tamano de algo, donde cero
 *     es una medida legitima: {@link JobKOctets}, {@link QueuedJobCount}, todas las
 *     {@code ...Completed}.</li>
 * <li><b>1..100</b> -- las dos prioridades, que IPP fija en esa escala.</li>
 * </ul>
 *
 * <p>El {@code equals()} de cada subclase agrega un {@code instanceof} sobre el de la base: sin
 * eso {@code new Copies(1)} y {@code new NumberUp(1)} darian iguales, porque el de la base solo
 * compara el entero. La comparacion no es simetrica entre una clase y su subclase, y es asi en el
 * JDK tambien.
 */

/**
 * Cuantas copias del documento se imprimen.
 *
 * <p>Arranca en 1 y no en 0 porque pedir cero copias no es pedir nada, es un error de quien pide.
 * Como se acomodan esas copias en el papel lo deciden {@link SheetCollate} y {@link
 * MultipleDocumentHandling}.
 */
public final class Copies extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -6426631521680023833L;

    public Copies(int value) {
        super(value, 1, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un Copies de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof Copies;
    }

    public final Class<? extends Attribute> getCategory() {
        return Copies.class;
    }

    public final String getName() {
        return "copies";
    }
}
