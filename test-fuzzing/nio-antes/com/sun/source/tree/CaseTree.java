package com.sun.source.tree;

import java.util.List;

/**
 * A `case` of a {@link SwitchTree} or of a {@link SwitchExpressionTree}.
 *
 * <h2>The node with the most accumulated history in the package</h2>
 *
 * <p>It shows in the methods: there are three ways of asking it for its labels, and the three
 * are there because the `switch` changed three times without being able to break whoever was
 * already walking it.
 *
 * <ul>
 * <li>{@link #getExpression} -- the single label of the original `switch`. {@code null} in the
 *     `default`, and {@code null} too when there is more than one.</li>
 * <li>{@link #getExpressions} -- the list, since a `case` was able to carry several constants
 *     separated by a comma.</li>
 * <li>{@link #getLabels} -- the list of {@link CaseLabelTree}, since a label was able to be a
 *     pattern and expressions were no longer enough.</li>
 * </ul>
 *
 * <p>The last one is the only one that represents everything that may be written today; the
 * first two are left for compatibility and answer what they can. Walking a modern tree with
 * {@link #getExpression} does not give an error: it gives {@code null}, which is worse, and it
 * is this node's trap.
 *
 * <h2>The two bodies</h2>
 *
 * <p>{@link #getCaseKind} says whether the `case` was written with `:` or with `->`, and on
 * that depends which of the two accesses to the body serves: {@link #getStatements} in the old
 * form, {@link #getBody} in the new one. The other gives {@code null} -- again, without saying
 * so.
 */
public interface CaseTree extends Tree {

    /**
     * How the `case` was written.
     *
     * <p>The difference is not typographical: with `:` the `case`s fall through in a cascade to
     * the next one and with `->` they do not, which was the problem the arrow came to resolve.
     */
    enum CaseKind {

        /** `case X:`, with fall-through to the next one. */
        STATEMENT,
        /** `case X ->`, with no fall-through. */
        RULE
    }

    /** The single label, or {@code null} if it is the `default` or if there is more than one. */
    ExpressionTree getExpression();

    /** The labels as expressions; empty in the `default`. See the class note. */
    List<? extends ExpressionTree> getExpressions();

    /** The labels, which is the form that represents everything that may be written today. */
    List<? extends CaseLabelTree> getLabels();

    /** The `when` guard, or {@code null} if it has none. */
    ExpressionTree getGuard();

    /** The statements, in the form with `:`; {@code null} in the form with `->`. */
    List<? extends StatementTree> getStatements();

    /** The body, in the form with `->`; {@code null} in the form with `:`. */
    default Tree getBody() {
        return null;
    }

    /** Whether the `case` uses `:` or `->`. */
    default CaseKind getCaseKind() {
        return CaseKind.STATEMENT;
    }
}
