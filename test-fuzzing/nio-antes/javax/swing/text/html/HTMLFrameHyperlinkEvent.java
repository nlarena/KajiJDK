package javax.swing.text.html;

import java.awt.event.InputEvent;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent$EventType;
import javax.swing.text.Element;

/**
 * A link that has to be opened in a particular frame.
 *
 * <h2>What it adds</h2>
 *
 * <p>An ordinary {@link HyperlinkEvent} says where to go. This one also says <em>where</em> to
 * show it: the value of the <code>target</code> attribute, which may be a frame's name or one of
 * the reserved words (<code>_self</code>, <code>_parent</code>, <code>_top</code>,
 * <code>_blank</code>).
 *
 * <p>Whoever handles the event has to look at that target. If they ignore it and load the
 * document in the frame where the click happened, a link with <code>target="_top"</code> will
 * replace the frame instead of the whole window, which is just the opposite of what the page
 * asks for.
 */
public class HTMLFrameHyperlinkEvent extends HyperlinkEvent {

    private String targetFrame;

    /** An event with that address and that target. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String targetFrame) {
        super(source, type, targetURL);
        this.targetFrame = targetFrame;
    }

    /** An event with an address, a description and a target. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, String targetFrame) {
        super(source, type, targetURL, desc);
        this.targetFrame = targetFrame;
    }

    /** An event that also knows which document element it came from. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            Element sourceElement, String targetFrame) {
        super(source, type, targetURL, null, sourceElement);
        this.targetFrame = targetFrame;
    }

    /** An event with a description and a source element. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, Element sourceElement, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement);
        this.targetFrame = targetFrame;
    }

    /** A complete event, with the input event that caused it. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, Element sourceElement, InputEvent inputEvent, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement, inputEvent);
        this.targetFrame = targetFrame;
    }

    /** The frame where the document has to be shown; see the class note. */
    public String getTarget() {
        return targetFrame;
    }
}
