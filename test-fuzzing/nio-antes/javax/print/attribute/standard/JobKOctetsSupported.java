package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Which values of {@link JobKOctets} the printer accepts.
 *
 * <p>See the family header in {@link CopiesSupported} for the mechanism. Unlike that one the legal
 * minimum is zero, for the same reason as in {@link JobKOctets}: it is a measure, not a quantity
 * one asks for.
 *
 * <p>It only has the range constructor. A single supported value is declared with both ends equal.
 */
public final class JobKOctetsSupported extends SetOfIntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = -2867871140549897443L;

    public JobKOctetsSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 0) {
            throw new IllegalArgumentException("Job K octets value < 0 specified");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobKOctetsSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobKOctetsSupported.class;
    }

    public final String getName() {
        return "job-k-octets-supported";
    }
}
