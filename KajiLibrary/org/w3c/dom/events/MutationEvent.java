package org.w3c.dom.events;

import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.events.MutationEvent -- the document changed.
 *
 * <p>It is the only DOM event that does not come from a person: the tree itself fires it when
 * something is added to it, removed or modified. It serves for keeping a view in step without
 * having to check the whole document.
 *
 * <h2>relatedNode is the one that is NOT the target</h2>
 *
 * <p>And which one it is depends on the event, which is what confuses: in {@code DOMNodeInserted}
 * the target is the inserted node and the related one is its <b>new parent</b>; in
 * {@code DOMAttrModified} the target is the element and the related one is the <b>attribute</b>.
 *
 * <p>The three value fields only make sense for changes of attribute or of text, and for the rest
 * they are null. {@link #getAttrChange()} says whether the attribute was added, removed or changed.
 */
public interface MutationEvent extends Event {

    /** The attribute was modified. */
    short MODIFICATION = 1;

    /** The attribute was added. */
    short ADDITION = 2;

    /** The attribute was removed. */
    short REMOVAL = 3;

    /** The other node involved. Which one depends on the event; see the note of the class. */
    Node getRelatedNode();

    /** The previous value, or null if the event does not change a value. */
    String getPrevValue();

    /** The new value, or null. */
    String getNewValue();

    /** The name of the attribute that changed, or null. */
    String getAttrName();

    /** Which of the three changes it was. It only makes sense for a change of attribute. */
    short getAttrChange();

    /** It initialises a newly created mutation event. */
    void initMutationEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        Node relatedNodeArg, String prevValueArg, String newValueArg, String attrNameArg,
        short attrChangeArg);
}
