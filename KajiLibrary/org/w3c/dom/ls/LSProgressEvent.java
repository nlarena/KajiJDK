package org.w3c.dom.ls;

import org.w3c.dom.events.Event;

/**
 * KajiLibrary's org.w3c.dom.ls.LSProgressEvent -- the progress of a load.
 *
 * <p>It is sent every so often while an asynchronous {@link LSParser} works, so that a progress bar
 * can be shown.
 *
 * <p>The two numbers are in bytes of the source document, not in nodes of the tree. It is the right
 * unit --the parser knows how much it read, not how much it has left to build-- and it has a
 * consequence worth keeping in mind: {@link #getTotalSize} <b>is not always known</b>. A document
 * that arrives through a stream with no declared length has no total, and the specification defines
 * no value for that case, so whoever draws the bar has to be ready not to have it.
 *
 * <p>How often it is sent is left to the implementation. There is no way of asking for a
 * granularity, and that is why it does not serve for counting: it serves for showing.
 */
public interface LSProgressEvent extends Event {

    /** Where it is being loaded from. */
    LSInput getInput();

    /** How many bytes were read. */
    int getPosition();

    /** How many there are in total, if it is known. See the note of the class. */
    int getTotalSize();
}
