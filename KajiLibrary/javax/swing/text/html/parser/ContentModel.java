package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.Vector;

/**
 * What may be inside an element, according to the DTD.
 *
 * <h2>A tree where the type is a character</h2>
 *
 * <p>A rule such as <code>(#PCDATA | B | I)*</code> is kept as a tree of these. The {@link #type}
 * field is not a number from a list but the operator's <em>character</em>: <code>'|'</code> to
 * choose one, <code>','</code> for one after another, <code>'&amp;'</code> for all in any order,
 * and <code>'*'</code>, <code>'?'</code>, <code>'+'</code> to repeat. Zero marks a leaf, and then
 * {@link #content} is an {@link Element}.
 *
 * <p>Keeping the operator as its character looks like a shortcut, and it is, but it is also what
 * lets {@link #toString} rebuild the rule just as it was written.
 *
 * <h2>The children go in a linked list</h2>
 *
 * <p>For the operators with several children, {@code content} is the first child and the rest
 * hang from its {@code next}. The same as in {@link AttributeList}: there is no separate list
 * class.
 */
public final class ContentModel implements Serializable {

    /** The operator, as a character; zero if it is a leaf. */
    public int type;

    /** An {@link Element} if it is a leaf, or the first child if it is an operator. */
    public Object content;

    /** The next sibling, when this model is an operator's child. */
    public ContentModel next;

    /** An empty model, to fill in later. */
    public ContentModel() {
    }

    /** A leaf: that element and nothing else. */
    public ContentModel(Element content) {
        this(0, content, null);
    }

    /** An operator with a single child, such as {@code *} or {@code ?}. */
    public ContentModel(int type, ContentModel content) {
        this(type, content, null);
    }

    /** A model with that operator, that content and that sibling. */
    public ContentModel(int type, Object content, ContentModel next) {
        this.type = type;
        this.content = content;
        this.next = next;
    }

    /**
     * Whether the element may have nothing inside.
     *
     * <p>It is what decides whether a tag can be closed right away. A <code>*</code> or a
     * <code>?</code> can always be empty; a sequence only if all its members can.
     */
    public boolean empty() {
        switch (type) {
            case '*':
            case '?':
                return true;

            case '+':
            case '|':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.empty()) {
                        return true;
                    }
                }
                return false;

            case ',':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (!m.empty()) {
                        return false;
                    }
                }
                return true;

            default:
                return false;
        }
    }

    /** Adds to the vector every element that appears in the model. */
    public void getElements(Vector<Element> elemVec) {
        switch (type) {
            case '*':
            case '?':
            case '+':
                ((ContentModel) content).getElements(elemVec);
                break;
            case ',':
            case '|':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    m.getElements(elemVec);
                }
                break;
            default:
                elemVec.addElement((Element) content);
        }
    }

    /**
     * Whether that element can be the first one.
     *
     * <p>It is the question the parser asks to decide whether it has to open a tag the author
     * skipped. The sequence is the interesting case: the next one can go on being looked at only
     * while the previous ones can be empty.
     */
    public boolean first(Object token) {
        switch (type) {
            case '*':
            case '?':
            case '|':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.first(token)) {
                        return true;
                    }
                }
                return false;

            case '+':
                return ((ContentModel) content).first(token);

            case ',':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.first(token)) {
                        return true;
                    }
                    if (!m.empty()) {
                        return false;
                    }
                }
                return false;

            default:
                return (content == token);
        }
    }

    /**
     * The only element that can go first, if there is only one.
     *
     * <p>It returns null when there is a choice or when the model can be empty: in those cases
     * there is no forced first one, and returning any would make the parser open a tag the
     * document was not asking for.
     */
    public Element first() {
        switch (type) {
            case '&':
            case '|':
            case '*':
            case '?':
                return null;

            case '+':
            case ',':
                return ((ContentModel) content).first();

            default:
                return (Element) content;
        }
    }

    /** The rule, written as in the DTD. */
    public String toString() {
        switch (type) {
            case '*':
                return content + "*";
            case '?':
                return content + "?";
            case '+':
                return content + "+";

            case ',':
            case '|':
            case '&': {
                char[] data = {' ', (char) type, ' '};
                String str = "";
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    str = str + m;
                    if (m.next != null) {
                        str = str + new String(data);
                    }
                }
                return "(" + str + ")";
            }

            default:
                return content.toString();
        }
    }
}
