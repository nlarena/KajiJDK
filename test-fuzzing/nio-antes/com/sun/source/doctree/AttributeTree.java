package com.sun.source.doctree;

import java.util.List;

import javax.lang.model.element.Name;

/**
 * An attribute of an HTML tag inside the comment, such as the {@code href} of an {@code <a>}.
 *
 * <p>The value is a <strong>list of nodes</strong> and not a string because inside it there may
 * be things the tree has to represent: an HTML entity, a {@code {@docRoot}}. Flattening it to
 * text would lose that structure right where it is needed -- an {@code href} that starts with
 * {@code {@docRoot}} is what makes a link work from any depth.
 */
public interface AttributeTree extends DocTree {

    /**
     * How the attribute's value was written.
     *
     * <p>It is kept instead of being normalized because javadoc re-emits the HTML, and re-emitting
     * {@code width=5} as {@code width="5"} would change what the author wrote. Telling single
     * quotes from double ones matters for the same reason.
     */
    enum ValueKind {

        /**
         * With no value: the attribute is alone, like the {@code checked} of an {@code <input>}.
         */
        EMPTY,
        /** With a value and no quotes: {@code width=5}. */
        UNQUOTED,
        /** Between single quotes. */
        SINGLE,
        /** Between double quotes. */
        DOUBLE
    }

    /** The attribute's name. */
    Name getName();

    /** How the value came written; see {@link ValueKind}. */
    ValueKind getValueKind();

    /** The value, or {@code null} if the {@link ValueKind} is {@link ValueKind#EMPTY}. */
    List<? extends DocTree> getValue();
}
