package javax.xml.stream;

import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.EventFilter -- the same criterion as {@link StreamFilter}, but for
 * the event model.
 *
 * <p>The difference from {@link StreamFilter} is not cosmetic: here the filter receives an {@link
 * XMLEvent}, which is a complete and immutable object, and not a reader standing at a position.
 * That means it **can** be kept, compared with another and looked at later, and that the rule of
 * "do not advance the reader" does not exist because there is no reader to advance.
 *
 * <p>That is exactly the trade-off between StAX's two models: the cursor one does not allocate an
 * object per event and in exchange gives you something valid only until the next {@code next()};
 * the event one allocates and in exchange gives you something that lasts.
 */
public interface EventFilter {

    /**
     * Decides whether the event is let through.
     *
     * @param event the event to judge
     * @return true for it to reach the caller, false to skip it
     */
    boolean accept(XMLEvent event);
}
