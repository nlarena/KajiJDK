package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.SetOfIntegerSyntax;

/**
 * Which pages of the document are printed.
 *
 * <p>It is the only one of the {@code SetOfIntegerSyntax} family --see the header in
 * {@link CopiesSupported}-- that is <em>requested</em> instead of reported, and the only one that
 * accepts the text form: {@code "1-3,7,10-"} as it is written in a print dialog.
 *
 * <p>The base's canonicalization is what makes this attribute comparable: asking for
 * {@code "3-5,1-2"} and asking for {@code "1-5"} is asking for the same, and after building them
 * both objects are equal and both print {@code "1-5"}. It merges even the ranges that only touch,
 * because 1-3 and 4-6 in a row leave no page out.
 *
 * <p>The only thing this class adds over the base is two rules: it cannot be left empty and no page
 * can be less than 1. The first is what makes {@code new PageRanges("5-1")} fail, which the base
 * had reduced to the empty set without complaint.
 *
 * <p>The pages are counted on the document, not on the sheets: which fall on each sheet is decided
 * by {@link NumberUp} and {@link Sides}. A number higher than the last page is not an error --
 * there is simply nothing to print there.
 */
public final class PageRanges extends SetOfIntegerSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 8639895197656148392L;

    /** Raw ranges {@code {{lb, ub}, ...}}; the base sorts and merges them. */
    public PageRanges(int[][] members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        myPageRanges();
    }

    /** The text form: {@code "1-3,7,10-12"}, with {@code ':'} valid instead of {@code '-'}. */
    public PageRanges(String members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        myPageRanges();
    }

    // The two rules of its own, on the already canonical set. That it is checked after super() is
    // what explains the "zero length" message for inputs such as "5-1".
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

    /** A single page. */
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
