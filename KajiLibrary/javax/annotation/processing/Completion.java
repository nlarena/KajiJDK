package javax.annotation.processing;

// A completion suggestion a processor offers for the value of an annotation element (JSR 269
// §Completion). It is two strings and nothing more: `getValue()` is the text the tool would insert,
// `getMessage()` the explanation it shows the person. The contract defines neither state nor
// identity, so the interface promises no equals/hashCode.
public interface Completion {

    /** The text proposed for insertion. */
    String getValue();

    /** The informative explanation accompanying {@link #getValue()}; it may be empty. */
    String getMessage();
}
