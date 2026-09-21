package java.io;

import java.util.function.Predicate;

/**
 * KajiLibrary's java.io.ObjectInputFilter -- the policy that decides which classes a stream of
 * objects may rebuild.
 *
 * <p>It exists because deserializing is, by definition, letting bytes from outside choose which
 * constructors run. A filter is where the limit is put: it is consulted **before** each class is
 * resolved, and its answer is {@code ALLOWED}, {@code REJECTED} or {@code UNDECIDED}.
 *
 * <p>{@code UNDECIDED} is not "I do not know": it is "I have no opinion, let the next one decide".
 * Having three answers and not two is what makes filters composable -- one that knows about a
 * single class can say its piece and keep quiet about the rest without authorizing anything by
 * default.
 *
 * <h2>How it is installed, and the only thing missing</h2>
 *
 * <p>With {@link ObjectInputStream#setObjectInputFilter}, and from there it really is consulted:
 * once for each class the stream is about to build, and with the length when what comes is an
 * array. A {@code REJECTED} cuts the reading short with {@link InvalidClassException} **before**
 * anything is constructed.
 *
 * <p><strong>{@code ObjectInputFilter.Config} is not here.</strong> That class sets the
 * <em>global</em> filter, the one consulted when the stream brings none of its own, and its central
 * piece is {@code createFilter(String)}: a language of patterns with package wildcards and limits
 * (`maxarray`, `maxdepth`, `maxrefs`, `maxbytes`) whose exact semantics is precisely what decides
 * what gets through and what does not. A parser that gets one wildcard wrong lets in what whoever
 * wrote it believed they had closed off, and there is no way of telling by looking at the result.
 * The absence is declared instead of approximating the grammar; meanwhile the per-stream filter,
 * which is explicit, is here and is honoured.
 */
public interface ObjectInputFilter {

    /**
     * The decision about an object of the stream.
     *
     * <p>It is called once per class to be resolved and also --with {@link
     * FilterInfo#serialClass()} at {@code null}-- for the stream's size limits, which is how a
     * filter can cut short an array of a thousand million elements without knowing what class it
     * is.
     */
    Status checkInput(FilterInfo filterInfo);

    /**
     * A filter that approves whatever satisfies `predicate` and answers `otherStatus` for the rest.
     *
     * <p><strong>The predicate sees the class as it comes, arrays included.</strong> A predicate
     * written as {@code c -> c == String.class} does **not** let a {@code String[]} through: they
     * are different classes and the one arriving is the array's. It is not unwrapped for
     * convenience because an array's length is precisely one of the attack vectors, and whoever
     * wants to allow them has to say so.
     *
     * <p>With the class at `null` --the size queries-- it returns {@code UNDECIDED}: the predicate
     * talks about classes and there is none there to have an opinion about.
     *
     * @throws NullPointerException if `predicate` or `otherStatus` is `null`
     */
    static ObjectInputFilter allowFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        if (predicate == null || otherStatus == null) {
            throw new NullPointerException();
        }
        return new Filters.ByPredicate(predicate, Status.ALLOWED, otherStatus);
    }

    /**
     * {@link #allowFilter}'s mirror: it rejects whatever satisfies `predicate`, and answers
     * `otherStatus` for the rest.
     *
     * <p>Both exist because a whitelist and a blacklist are not the same policy written backwards:
     * with `allowFilter` what was not named is left out, with `rejectFilter` it is left in. The
     * difference shows the day a class nobody thought of turns up.
     *
     * @throws NullPointerException if `predicate` or `otherStatus` is `null`
     */
    static ObjectInputFilter rejectFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        if (predicate == null || otherStatus == null) {
            throw new NullPointerException();
        }
        return new Filters.ByPredicate(predicate, Status.REJECTED, otherStatus);
    }

    /**
     * It combines two filters: **any** rejection wins, and if neither rejects it is enough for one
     * to approve.
     *
     * <p>That rejection wins is the only thing that makes the whole composable: if approving could
     * annul a rejection, adding a filter could **open** what another was closing, and nobody could
     * reason about a policy without reading it whole.
     *
     * @throws NullPointerException if `filter` is `null`
     */
    static ObjectInputFilter merge(ObjectInputFilter filter, ObjectInputFilter anotherFilter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        return new Filters.Union(filter, anotherFilter);
    }

    /**
     * It turns into a rejection whatever {@code UNDECIDED} is left over a concrete class.
     *
     * <p>It is a whitelist's lid: without this, a class no filter named comes out {@code
     * UNDECIDED}, and the caller has to remember to treat that as negative. Wrapping the policy
     * puts the default answer in writing in one place instead of depending on every use
     * interpreting it the same way.
     *
     * <p>The queries with no class --the size ones-- pass untouched: there {@code UNDECIDED} means
     * "this filter sets no limits", which is a legitimate answer and not an oversight.
     *
     * @throws NullPointerException if `filter` is `null`
     */
    static ObjectInputFilter rejectUndecidedClass(ObjectInputFilter filter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        return new Filters.RejectUndecided(filter);
    }

    /** What is known about the object about to be read when the filter is consulted. */
    interface FilterInfo {

        /**
         * The class to resolve, or `null` if this query is not about a class.
         *
         * <p>The `null` is information and not a gap: it is how the stream asks about the size
         * limits --how many references have gone by, how many bytes-- which hold whatever the
         * class.
         */
        Class<?> serialClass();

        /** The length of the array about to be read, or -1 if this is not an array. */
        long arrayLength();

        /** How deeply nested the object is; the topmost one is 1. */
        long depth();

        /** How many references the stream has read so far. */
        long references();

        /** How many bytes the stream has consumed so far. */
        long streamBytes();
    }

    /** The three possible answers. See {@code UNDECIDED}'s note in the class's header. */
    enum Status {
        UNDECIDED,
        ALLOWED,
        REJECTED
    }
}
