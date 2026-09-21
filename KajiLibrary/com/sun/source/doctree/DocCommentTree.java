package com.sun.source.doctree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The whole comment, and the root of every tree of this package.
 *
 * <p>The partition into {@link #getFirstSentence} and {@link #getBody} is not decorative: the
 * first sentence is the one javadoc shows in the summary tables, so where it ends changes what
 * is seen. {@link #getFullBody} is the concatenation, for whoever does not need that
 * distinction.
 */
public interface DocCommentTree extends DocTree {

    /** The first sentence, which is what appears in the summary tables. */
    List<? extends DocTree> getFirstSentence();

    /** The first sentence and the body, concatenated. */
    default List<? extends DocTree> getFullBody() {
        List<DocTree> completo = new ArrayList<DocTree>();
        completo.addAll(getFirstSentence());
        completo.addAll(getBody());
        return completo;
    }

    /** The body, without the first sentence. */
    List<? extends DocTree> getBody();

    /** The block tags, in the order in which they were written. */
    List<? extends DocTree> getBlockTags();

    /** What there is before the content in a loose file: the `<!DOCTYPE>`, the `<head>`. */
    default List<? extends DocTree> getPreamble() {
        return Collections.<DocTree>emptyList();
    }

    /** What there is after the content in a loose file. */
    default List<? extends DocTree> getPostamble() {
        return Collections.<DocTree>emptyList();
    }
}
