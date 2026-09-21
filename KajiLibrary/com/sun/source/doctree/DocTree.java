package com.sun.source.doctree;

/**
 * Any node of the tree of a documentation comment.
 *
 * <h2>What this package is</h2>
 *
 * <p>A javadoc comment is not text: it has structure -- tags, HTML, references to code -- and a
 * tool that wants to do something with it needs that structure, not the string. This package is
 * the syntax tree of that structure, brother of {@code com.sun.source.tree}'s for the code.
 *
 * <h2>The two ways of walking it</h2>
 *
 * <p>{@link #getKind} and {@link #accept} are the same question answered in two ways, and the
 * two are there on purpose. The {@link Kind} serves for a loose decision -- "is this a
 * `@param`?" -- without writing a whole visitor. The {@link DocTreeVisitor} serves when all of
 * them have to be attended to: the compiler says so if a kind of node is added and the method
 * is missing, whereas a {@code switch} over the {@code Kind} keeps quiet.
 *
 * <h2>The trap: more nodes than types</h2>
 *
 * <p>{@link Kind} has more constants than there are interfaces in the package, and it is not an
 * oversight. `{@code @throws}` and `{@code @exception}` are the same {@link ThrowsTree} with a
 * different {@code tagName}; the same for `{@code {@link}}` and `{@code {@linkplain}}`, and
 * `{@code {@code}}` with `{@code {@literal}}`. Asking for the Java type is not enough in order
 * to tell them apart -- the tag's name has to be looked at.
 */
public interface DocTree {

    /**
     * What kind of node this is.
     *
     * <p>Each constant carries the {@code tagName} it is written with, or {@code null} for the
     * nodes that are not a tag: text, HTML, entities, the whole comment.
     */
    enum Kind {

        /** An attribute of an HTML tag. */
        ATTRIBUTE(null),
        /** `@author`. */
        AUTHOR("author"),
        /** `{@code}` -- the same {@link LiteralTree} as {@link #LITERAL}. */
        CODE("code"),
        /** An HTML comment. */
        COMMENT(null),
        /** `@deprecated`. */
        DEPRECATED("deprecated"),
        /** The whole comment: the root. */
        DOC_COMMENT(null),
        /** `{@docRoot}`. */
        DOC_ROOT("docRoot"),
        /** A `<!DOCTYPE>`. */
        DOC_TYPE(null),
        /** A closing HTML tag. */
        END_ELEMENT(null),
        /** An HTML entity. */
        ENTITY(null),
        /** Something that could not be parsed. */
        ERRONEOUS(null),
        /** A Markdown escape. */
        ESCAPE(null),
        /** `@exception` -- the same {@link ThrowsTree} as {@link #THROWS}. */
        EXCEPTION("exception"),
        /** `@hidden`. */
        HIDDEN("hidden"),
        /** A Java identifier inside a tag. */
        IDENTIFIER(null),
        /** `{@index}`. */
        INDEX("index"),
        /** `{@inheritDoc}`. */
        INHERIT_DOC("inheritDoc"),
        /** `{@link}`. */
        LINK("link"),
        /** `{@linkplain}` -- the same {@link LinkTree} as {@link #LINK}. */
        LINK_PLAIN("linkplain"),
        /** `{@literal}`. */
        LITERAL("literal"),
        /** Markdown content, uninterpreted. */
        MARKDOWN(null),
        /** `@param`. */
        PARAM("param"),
        /** `@provides`. */
        PROVIDES("provides"),
        /** A reference to a Java element. */
        REFERENCE(null),
        /** `@return`, in either of its two forms. */
        RETURN("return"),
        /** `@see`. */
        SEE("see"),
        /** `@serial`. */
        SERIAL("serial"),
        /** `@serialData`. */
        SERIAL_DATA("serialData"),
        /** `@serialField`. */
        SERIAL_FIELD("serialField"),
        /** `@since`. */
        SINCE("since"),
        /** `{@snippet}`. */
        SNIPPET("snippet"),
        /** `@spec`. */
        SPEC("spec"),
        /** An opening HTML tag. */
        START_ELEMENT(null),
        /** `{@systemProperty}`. */
        SYSTEM_PROPERTY("systemProperty"),
        /** `{@summary}`. */
        SUMMARY("summary"),
        /** Plain text. */
        TEXT(null),
        /** `@throws`. */
        THROWS("throws"),
        /** A block tag this tree does not know. */
        UNKNOWN_BLOCK_TAG(null),
        /** An inline tag this tree does not know. */
        UNKNOWN_INLINE_TAG(null),
        /** `@uses`. */
        USES("uses"),
        /** `{@value}`. */
        VALUE("value"),
        /** `@version`. */
        VERSION("version"),
        /**
         * An implementation of one's own that is none of the previous ones.
         *
         * <p>It exists for the same reason as {@link #UNKNOWN_BLOCK_TAG}: this tree is a public
         * interface and somebody may implement it with nodes the JDK did not foresee. Without this
         * constant, {@link #getKind} would have nothing to return.
         */
        OTHER(null);

        /**
         * The name the tag is written with, or {@code null} if this node is not a tag.
         *
         * <p>Public and final, not a getter, and that is how it is in the JDK: it is a datum of the
         * enum, not a computation. It is besides the only thing that separates {@link #THROWS} from
         * {@link #EXCEPTION}, which share an interface.
         */
        public final String tagName;

        Kind(String tagName) {
            this.tagName = tagName;
        }
    }

    /** What kind of node it is. */
    Kind getKind();

    /**
     * It passes this node to the visitor.
     *
     * <p>Double dispatch: the node knows which its type is and the visitor knows what to do with
     * each one; neither of the two knows both things, and this method is where they meet.
     *
     * @param <R> what the visitor returns
     * @param <D> the datum that is carried along to it
     */
    <R, D> R accept(DocTreeVisitor<R, D> visitor, D data);
}
