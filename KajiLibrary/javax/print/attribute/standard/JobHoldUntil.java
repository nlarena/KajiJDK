package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Hasta cuando retener el trabajo antes de imprimirlo.
 *
 * <p>Es el unico de la familia que se <em>pide</em> en vez de reportarse. Un instante ya pasado
 * significa "ahora": el trabajo sale enseguida en vez de fallar.
 */
public final class JobHoldUntil extends DateTimeSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -1664471048860415024L;

    public JobHoldUntil(Date jobHoldUntil) {
        super(jobHoldUntil);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobHoldUntil;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobHoldUntil.class;
    }

    public final String getName() {
        return "job-hold-until";
    }
}
