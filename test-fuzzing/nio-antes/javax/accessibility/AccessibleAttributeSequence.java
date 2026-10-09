package javax.accessibility;

import javax.swing.text.AttributeSet;

/**
 * A stretch of text that shares the same attributes.
 *
 * <p>It is a three-field record and that is why it has them **public**: adding accessor methods to
 * something returned by the dozen when walking a document would be ceremony without content.
 */
public class AccessibleAttributeSequence {

    /** Where the stretch starts. */
    public int startIndex;

    /** Where it ends. */
    public int endIndex;

    /** The attributes it shares. */
    public AttributeSet attributes;

    /** An empty stretch. */
    public AccessibleAttributeSequence() {
    }

    /** With the stretch and its attributes. */
    public AccessibleAttributeSequence(int start, int end, AttributeSet a) {
        this.startIndex = start;
        this.endIndex = end;
        this.attributes = a;
    }
}
