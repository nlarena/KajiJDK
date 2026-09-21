package org.w3c.dom.events;

import org.w3c.dom.views.AbstractView;

/**
 * KajiLibrary's org.w3c.dom.events.UIEvent -- a user interface event.
 *
 * <p>It adds two things over {@link Event}: in <b>which view</b> it happened, which is the only
 * reason why {@code org.w3c.dom.views} exists, and a counter whose meaning depends on the event
 * --for a click, how many clicks in a row; for a scroll, how many lines--.
 */
public interface UIEvent extends Event {

    /** The view where it happened, or null. */
    AbstractView getView();

    /** A number whose meaning depends on the type of event. See the note of the class. */
    int getDetail();

    /** It initialises a newly created interface event. */
    void initUIEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        AbstractView viewArg, int detailArg);
}
