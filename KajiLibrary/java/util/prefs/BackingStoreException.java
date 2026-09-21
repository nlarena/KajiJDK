package java.util.prefs;

// The exception that says "I could not talk to the store".
//
// It is the package's only *checked* exception, and it is put on very few methods on purpose:
// `keys`, `childrenNames`, `nodeExists`, `removeNode`, `clear`, `flush` and `sync`. The `put`s and
// the `get`s do NOT throw it, and that is `Preferences`'s central design: storing and reading a
// preference has to be writable without a `try`, because a preference that could not be read is
// settled with the default value and there is nothing to report. Only the operations that do *not*
// have a reasonable default answer --enumerating, removing, forcing the write-- can fail visibly.
//
// It has no no-argument constructor: a store failure with neither a cause nor a message is of use to
// nobody.
public class BackingStoreException extends Exception {

    private static final long serialVersionUID = 859796500401108469L;

    // A failure described by `s`.
    public BackingStoreException(String s) {
        super(s);
    }

    // A failure brought on by `cause` --typically the I/O exception that started it.
    public BackingStoreException(Throwable cause) {
        super(cause);
    }
}
