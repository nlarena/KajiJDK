package jdk.javadoc.doclet;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

/**
 * A documentation tag of one's own, which is added to the ones javadoc already understands.
 *
 * <h2>Of block or inline</h2>
 *
 * <p>A block tag takes up its own paragraph and is written {@code @name ...}; an inline one goes
 * inside the text and is written <code>{&#64;name ...}</code>. It is the only structural
 * distinction, and that is why {@link #isBlockTag} comes by default as the negation of
 * {@link #isInlineTag} -- a tag is one thing or the other, never both.
 *
 * <h2>Where it can appear</h2>
 *
 * <p>{@link #getAllowedLocations} returns a set and not a place: {@code @since} makes sense almost
 * everywhere, {@code @return} only on a method. javadoc uses that answer to warn when a tag appears
 * where it does not belong, which is an error that would otherwise pass as loose text.
 *
 * <h2>How it is translated</h2>
 *
 * <p>{@link #toString(List, Element)} receives the trees of the comment already analysed, not the
 * raw text. That means the tag does not have to analyse anything: it receives the structure and
 * returns what corresponds in the output format. The {@link Element} is the one that was being
 * documented, and it serves for what depends on the context -- an {@code @implNote} may be written
 * differently in an interface than in a class.
 *
 * @since 9
 */
public interface Taglet {

    /**
     * Where this tag can appear.
     *
     * @return the allowed places
     */
    Set<Location> getAllowedLocations();

    /**
     * Whether it goes inside the text, between braces.
     *
     * @return whether it is inline
     */
    boolean isInlineTag();

    /**
     * Whether it takes up its own paragraph.
     *
     * <p>By default it is the opposite of {@link #isInlineTag}: there is no third form.
     *
     * @return whether it is of block
     */
    default boolean isBlockTag() {
        return !isInlineTag();
    }

    /**
     * The name, without the at sign.
     *
     * @return the name
     */
    String getName();

    /**
     * The notice of start-up, with the model and the plug-in that hosts it.
     *
     * <p>By default it does nothing: a tag that only looks at its own arguments does not need to
     * find out about anything else, and forcing it to write an empty method would be noise.
     *
     * @param env the model of the analysed code
     * @param doclet the plug-in that is going to use it
     */
    default void init(DocletEnvironment env, Doclet doclet) {
    }

    /**
     * The output this tag produces.
     *
     * @param tags the appearances of the tag, already analysed
     * @param element the element that was being documented
     * @return the text to insert, in the output format of the plug-in
     */
    String toString(List<? extends DocTree> tags, Element element);

    /** The places where a tag can appear. */
    enum Location {
        /** In the file of the overall summary. */
        OVERVIEW,
        /** In the documentation of a module. */
        MODULE,
        /** In that of a package. */
        PACKAGE,
        /** In that of a class or interface. */
        TYPE,
        /** In that of a constructor. */
        CONSTRUCTOR,
        /** In that of a method. */
        METHOD,
        /** In that of a field. */
        FIELD
    }
}
