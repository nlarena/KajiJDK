package java.awt;

/**
 * Something that can contain menu elements.
 *
 * <p>{@link Menu}, {@link MenuBar} and {@link Frame} implement it — the places a menu can hang from
 * — and so does every {@link Component}. (This note said those three were the only ones.) That the
 * interface is so small is not poverty: it is all a child needs to know about its container, and
 * keeping it so lets a frame and a menu, which resemble each other in nothing else, serve alike as
 * parents.
 */
public interface MenuContainer {

    /** The font the children are drawn with. */
    Font getFont();

    /** Takes that child out. */
    void remove(MenuComponent comp);

    /**
     * Sends it an event of the old model.
     *
     * @deprecated it is from the 1.0 event model. It stays because it is in the interface.
     */
    @Deprecated
    boolean postEvent(Event e);
}
