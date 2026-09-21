package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/*
 * FAMILY HEADER -- this package's {@code IntegerSyntax} attributes.
 *
 * <p>An integer attribute is a number with a legal range and a name. The whole mechanism is in
 * {@link javax.print.attribute.IntegerSyntax IntegerSyntax}; each subclass here only chooses the
 * two ends and passes them to the three-argument constructor, which is the one that throws {@code
 * IllegalArgumentException} when the value goes out.
 *
 * <p>The range is the only thing that really tells them apart, and almost all fall into three
 * moulds:
 * <ul>
 * <li><b>1..MAX_VALUE</b> -- the ones that count things one asks for and where asking for zero
 *     makes no sense: {@link Copies}, {@link NumberUp}.</li>
 * <li><b>0..MAX_VALUE</b> -- the ones that <em>measure</em> something already done or the size of
 *     something, where zero is a legitimate measure: {@link JobKOctets}, {@link QueuedJobCount},
 *     all the {@code ...Completed}.</li>
 * <li><b>1..100</b> -- the two priorities, which IPP fixes on that scale.</li>
 * </ul>
 *
 * <p>Each subclass's {@code equals()} adds an {@code instanceof} over the base's: without it
 * {@code new Copies(1)} and {@code new NumberUp(1)} would come out equal, because the base's only
 * compares the integer. The comparison is not symmetric between a class and its subclass, and it is
 * so in the JDK too.
 */

/**
 * How many copies of the document are printed.
 *
 * <p>It starts at 1 and not at 0 because asking for zero copies is not asking for anything, it is
 * the asker's error. How those copies are laid out on the paper is decided by {@link SheetCollate}
 * and {@link MultipleDocumentHandling}.
 */
public final class Copies extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -6426631521680023833L;

    public Copies(int value) {
        super(value, 1, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a Copies from being equal to another
     * integer attribute with the same number.
     */
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
