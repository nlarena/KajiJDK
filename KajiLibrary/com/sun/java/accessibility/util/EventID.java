package com.sun.java.accessibility.util;

/**
 * The identifiers of each family of events, so as to be able to talk about "a kind of event"
 * as a datum.
 *
 * <h2>Why numbers and not classes</h2>
 *
 * <p>Because these monitors register listeners <strong>by type</strong> in a single list, and a
 * numeric key makes adding, taking out and handing out a comparison of integers. With
 * {@code Class} hierarchies would have to be resolved on each dispatch.
 *
 * <p>It is the reason {@link AccessibilityListenerList} exists: a list of (type, listener) pairs
 * in a single array.
 *
 * <p>The first eleven constants are AWT's and the seventeen following ones Swing's, which is the
 * same cut that separates {@link AWTEventMonitor} from {@link SwingEventMonitor}.
 */
public class EventID {

    /** A button or similar was activated. */
    public static final int ACTION = 0;
    /** A scroll bar changed. */
    public static final int ADJUSTMENT = 1;
    /** A component moved, changed size or changed visibility. */
    public static final int COMPONENT = 2;
    /** A container gained or lost a child. */
    public static final int CONTAINER = 3;
    /** The focus changed. */
    public static final int FOCUS = 4;
    /** An item was selected or deselected. */
    public static final int ITEM = 5;
    /** The keyboard. */
    public static final int KEY = 6;
    /** Mouse buttons. */
    public static final int MOUSE = 7;
    /** Mouse movement; it goes separately because it arrives very much more often. */
    public static final int MOTION = 8;
    /** A text field changed. */
    public static final int TEXT = 9;
    /** A window opened, closed, minimized. */
    public static final int WINDOW = 10;

    /** A component's chain of ancestors changed. */
    public static final int ANCESTOR = 11;
    /** The text cursor moved. */
    public static final int CARET = 12;
    /** The editing of a cell finished. */
    public static final int CELLEDITOR = 13;
    /** A generic state change. */
    public static final int CHANGE = 14;
    /** A table's column model changed. */
    public static final int COLUMNMODEL = 15;
    /** A text document changed. */
    public static final int DOCUMENT = 16;
    /** A list's content changed. */
    public static final int LISTDATA = 17;
    /** A list's selection changed. */
    public static final int LISTSELECTION = 18;
    /** A menu opened or closed. */
    public static final int MENU = 19;
    /** A context menu opened or closed. */
    public static final int POPUPMENU = 20;
    /** A table's model changed. */
    public static final int TABLEMODEL = 21;
    /** A tree node expanded or collapsed. */
    public static final int TREEEXPANSION = 22;
    /** A tree's model changed. */
    public static final int TREEMODEL = 23;
    /** A tree's selection changed. */
    public static final int TREESELECTION = 24;
    /** Something undoable was done. */
    public static final int UNDOABLEEDIT = 25;
    /** A property changed. */
    public static final int PROPERTYCHANGE = 26;
    /** A property is going to be changed, and it may be vetoed. */
    public static final int VETOABLECHANGE = 27;
    /** An internal window changed state. */
    public static final int INTERNALFRAME = 28;

    public EventID() {
    }
}
