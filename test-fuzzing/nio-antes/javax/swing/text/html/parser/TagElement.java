package javax.swing.text.html.parser;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML$Tag;

/**
 * A tag found in the document, tied to its DTD element.
 *
 * <h2>The bridge between the two halves</h2>
 *
 * <p>The parser works with {@link Element}, which comes from the DTD and knows about rules. The
 * document works with {@link HTML.Tag}, which knows about how it is shown. This class is what
 * joins the two: given an element, it looks up the tag that corresponds to it, and if there is
 * none it builds an {@link HTML.UnknownTag}.
 *
 * <h2>Tags nobody wrote</h2>
 *
 * <p>{@link #fictional} marks those the parser invented to close the tree: the
 * <code>&lt;p&gt;</code> missing before a loose text, the <code>&lt;/li&gt;</code> the author did
 * not put in. Whoever receives the tag may want to treat them differently, for instance when
 * writing the document back just as it was.
 */
public class TagElement {

    private final Element elem;
    private final HTML$Tag htmlTag;
    private final boolean insertedByErrorRecovery;

    /** A real tag for that element. */
    public TagElement(Element elem) {
        this(elem, false);
    }

    /** A tag for that element, real or invented. */
    public TagElement(Element elem, boolean fictional) {
        this.elem = elem;
        htmlTag = HTML.getTag(elem.getName()) == null
                ? new HTML.UnknownTag(elem.getName()) : HTML.getTag(elem.getName());
        insertedByErrorRecovery = fictional;
    }

    /** Whether it breaks the line; the tag answers, not the element. */
    public boolean breaksFlow() {
        return htmlTag.breaksFlow();
    }

    public boolean isPreformatted() {
        return htmlTag.isPreformatted();
    }

    public Element getElement() {
        return elem;
    }

    public HTML$Tag getHTMLTag() {
        return htmlTag;
    }

    /** Whether the parser invented it; see the class note. */
    public boolean fictional() {
        return insertedByErrorRecovery;
    }
}
