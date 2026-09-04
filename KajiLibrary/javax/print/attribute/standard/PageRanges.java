package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.SetOfIntegerSyntax;

/**
 * Que paginas del documento se imprimen.
 *
 * <p>Es la unica de la familia {@code SetOfIntegerSyntax} --ver la cabecera en
 * {@link CopiesSupported}-- que se <em>pide</em> en vez de reportarse, y la unica que acepta la
 * forma de texto: {@code "1-3,7,10-"} tal como se escribe en un dialogo de impresion.
 *
 * <p>La canonicalizacion de la base es lo que hace que este atributo se pueda comparar: pedir
 * {@code "3-5,1-2"} y pedir {@code "1-5"} es pedir lo mismo, y despues de construirlos los dos
 * objetos son iguales y los dos imprimen {@code "1-5"}. Fusiona incluso los rangos que solo se
 * tocan, porque 1-3 y 4-6 seguidos no dejan ninguna pagina afuera.
 *
 * <p>Lo unico que agrega esta clase sobre la base son dos reglas: no puede quedar vacio y ninguna
 * pagina puede ser menor que 1. La primera es la que hace fallar a {@code new PageRanges("5-1")},
 * que la base habia reducido al conjunto vacio sin quejarse.
 *
 * <p>Las paginas se cuentan sobre el documento, no sobre las hojas: cuales caen en cada hoja lo
 * deciden {@link NumberUp} y {@link Sides}. Un numero mas alto que la ultima pagina no es error --
 * simplemente no hay nada que imprimir ahi.
 */
public final class PageRanges extends SetOfIntegerSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 8639895197656148392L;

    /** Rangos crudos {@code {{lb, ub}, ...}}; la base los ordena y los fusiona. */
    public PageRanges(int[][] members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        myPageRanges();
    }

    /** La forma de texto: {@code "1-3,7,10-12"}, con {@code ':'} valido en lugar de {@code '-'}. */
    public PageRanges(String members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        myPageRanges();
    }

    // Las dos reglas propias, sobre el conjunto ya canonico. Que se revise despues de super() es
    // lo que explica el mensaje de "longitud cero" para entradas como "5-1".
    private void myPageRanges() {
        int[][] myMembers = getMembers();
        int n = myMembers.length;
        if (n == 0) {
            throw new IllegalArgumentException("members is zero-length");
        }
        for (int i = 0; i < n; i++) {
            if (myMembers[i][0] < 1) {
                throw new IllegalArgumentException("Page value < 1 specified");
            }
        }
    }

    /** Una sola pagina. */
    public PageRanges(int member) {
        super(member);
        myPageRanges();
    }

    public PageRanges(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        myPageRanges();
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PageRanges;
    }

    public final Class<? extends Attribute> getCategory() {
        return PageRanges.class;
    }

    public final String getName() {
        return "page-ranges";
    }
}
