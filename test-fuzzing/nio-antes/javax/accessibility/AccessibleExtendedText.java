package javax.accessibility;

import java.awt.Rectangle;

/**
 * Accessible text that can be asked for **by ranges** instead of by units.
 *
 * <p>The difference from {@link AccessibleText} is in what it returns: there a loose string, here
 * an {@link AccessibleTextSequence} that also says **where it starts and where it ends**. Without
 * that, whoever reads has to guess the position of the text received, and with units of variable
 * length that cannot be done.
 *
 * <p>It adds two units the other lacks: the line and the "homogeneous attribute", which is the
 * longest stretch around a position that is all drawn alike.
 */
public interface AccessibleExtendedText {

    /** The unit "one line". */
    int LINE = 4;

    /** The unit "a stretch all drawn alike". */
    int ATTRIBUTE_RUN = 5;

    /** The text of that range. */
    String getTextRange(int startIndex, int endIndex);

    /** The unit that contains that index, with its bounds. */
    AccessibleTextSequence getTextSequenceAt(int part, int index);

    /** The next unit, with its bounds. */
    AccessibleTextSequence getTextSequenceAfter(int part, int index);

    /** The previous unit, with its bounds. */
    AccessibleTextSequence getTextSequenceBefore(int part, int index);

    /** Where that range falls on the screen. */
    Rectangle getTextBounds(int startIndex, int endIndex);
}
