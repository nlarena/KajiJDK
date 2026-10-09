package org.w3c.dom.events;

import org.w3c.dom.views.AbstractView;

/**
 * KajiLibrary's org.w3c.dom.events.MouseEvent -- a pointer event.
 *
 * <h2>Two coordinate systems, and neither is the one one wants</h2>
 *
 * <ul>
 *   <li><b>screen</b> -- relative to the physical screen. It serves for positioning something
 *       outside the document; inside it says nothing.
 *   <li><b>client</b> -- relative to the visible area of the client. <b>It does not include the
 *       scrolling</b>: the same point of the document gives different coordinates if the page is
 *       scrolled.
 * </ul>
 *
 * <p>The coordinate relative to the document --the one almost always sought-- <b>is not</b> at this
 * level of the DOM; the scrolling has to be added by hand.
 *
 * <h2>relatedTarget changes meaning according to the event</h2>
 *
 * <p>For {@code mouseover} it is where the pointer <b>came from</b>; for {@code mouseout}, where it
 * <b>is going</b>. For the rest it is null. Reading it without looking at the type of the event
 * gives the wrong node half of the time.
 */
public interface MouseEvent extends UIEvent {

    /** X relative to the screen. */
    int getScreenX();

    /** Y relative to the screen. */
    int getScreenY();

    /**
     * X relative to the visible area. It does not include the scrolling; see the note of the class.
     */
    int getClientX();

    /** Y relative to the visible area. */
    int getClientY();

    /** Whether Control was held down. */
    boolean getCtrlKey();

    /** Whether Shift was held down. */
    boolean getShiftKey();

    /** Whether Alt was held down. */
    boolean getAltKey();

    /** Whether Meta was held down. */
    boolean getMetaKey();

    /**
     * Which button: 0 the primary, 1 the middle one, 2 the secondary.
     *
     * <p>They are logical positions, not physical ones: on a left-handed mouse 0 is the right one.
     */
    short getButton();

    /** The other node involved. Its meaning depends on the event; see the note of the class. */
    EventTarget getRelatedTarget();

    /** It initialises a newly created pointer event. */
    void initMouseEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        AbstractView viewArg, int detailArg, int screenXArg, int screenYArg, int clientXArg,
        int clientYArg, boolean ctrlKeyArg, boolean altKeyArg, boolean shiftKeyArg,
        boolean metaKeyArg, short buttonArg, EventTarget relatedTargetArg);
}
