package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.Schema -- an already compiled schema.
 *
 * <p>It represents a set of rules --XML Schema, RELAX NG, whatever-- <b>already read and
 * checked</b>. The class has two methods and neither validates: both make something that validates.
 *
 * <h2>Why the split into three</h2>
 *
 * <p>{@code SchemaFactory} reads the schema, {@code Schema} keeps it compiled, and {@link
 * Validator} validates <b>one</b> document. It could be a single class with a {@code
 * validate(schema, document)} method, and it would be much slower: compiling a schema is expensive
 * and validating against an already compiled one is cheap. The split makes that cost be paid once.
 *
 * <p>From there comes the usage rule that matters: a {@code Schema} is <b>immutable and shareable
 * between threads</b>; a {@link Validator} is not. What is kept in a static field is the schema,
 * and the validator is made on each use -- the other way round from what one would do out of habit.
 *
 * <h2>The two ways of validating</h2>
 *
 * <p>{@link #newValidator} validates something that already exists: a tree, a file, a stream.
 * {@link #newValidatorHandler} validates <b>while</b> reading, plugging into a SAX chain. The
 * second does not need the whole document in memory, and it can also pass the already validated
 * content on to another handler.
 */
public abstract class Schema {

    /** For the subclasses. */
    protected Schema() {
    }

    /**
     * A validator for documents that already exist.
     *
     * <p>A new one for each use, or at least one per thread: see the class note.
     */
    public abstract Validator newValidator();

    /** A validator that plugs into a SAX chain. */
    public abstract ValidatorHandler newValidatorHandler();
}
