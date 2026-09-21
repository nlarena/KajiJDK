package com.sun.source.doctree;

/**
 * The half of the hierarchy that groups the **inline** tags: those that go between
 * braces, in the middle of the text, such as `{@link}` or `{@code}`.
 *
 * <p>The separation from {@link BlockTagTree} is not one of style but grammatical: a block tag
 * ends where the next one or the comment starts, and an inline one ends at its closing brace.
 * They are two different parsing rules, and that is why they are two types.
 *
 * <p>{@link ReturnTree} implements both, because `@return` exists in the two forms.
 */
public interface InlineTagTree extends DocTree {

    String getTagName();
}
