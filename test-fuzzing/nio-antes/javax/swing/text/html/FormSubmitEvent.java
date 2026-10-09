package javax.swing.text.html;

import java.net.URL;

import javax.swing.event.HyperlinkEvent$EventType;
import javax.swing.text.Element;

/**
 * A form submission, reported as if it were a link.
 *
 * <h2>Why it is a link event</h2>
 *
 * <p>Submitting a form and following a link end up the same: a document from an address has to be
 * loaded. Making the submission arrive by the same path allows a program that already knows how
 * to attend to links to attend to forms too without changing anything.
 *
 * <p>What it adds is the only thing the link cannot carry: the method ({@code GET} or
 * {@code POST}) and the data already assembled. In a {@code GET} the data also goes stuck to the
 * address; in a {@code POST} it is only here, and whoever does not look at {@link #getData} loses
 * it.
 *
 * <h2>It is not built from outside</h2>
 *
 * <p>The class is final and its constructor is not public: the {@link FormView} creates them on
 * submitting. For a program to build one would make no sense, because there would be no form for
 * the data to come from.
 */
public final class FormSubmitEvent extends HTMLFrameHyperlinkEvent {

    private MethodType method;
    private String data;

    FormSubmitEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            Element sourceElement, String targetFrame, MethodType method, String data) {
        super(source, type, targetURL, sourceElement, targetFrame);
        this.method = method;
        this.data = data;
    }

    /** {@code GET} or {@code POST}. */
    public MethodType getMethod() {
        return method;
    }

    /** The form's data, already encoded. */
    public String getData() {
        return data;
    }

    /** The two methods an HTML form may use. */
    public enum MethodType {
        GET, POST
    }
}
