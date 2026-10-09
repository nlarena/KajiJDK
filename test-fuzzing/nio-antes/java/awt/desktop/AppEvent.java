package java.awt.desktop;

import java.util.EventObject;

/**
 * KajiLibrary's java.awt.desktop.AppEvent -- the root of the desktop's events.
 *
 * <p>It adds nothing over {@link EventObject}: it exists so that the package's thirteen events have a
 * common type, and so that its constructor --package-private-- always sets the same source.
 *
 * <p>That the constructor is not public is what guarantees these events are emitted by the desktop
 * and not by anyone. The subclasses do have a public constructor, so that they can be tested.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>In the JDK the source is the {@code java.awt.Desktop} instance. That class is here, but there is
 * no instance to be had: this library is headless, {@code Desktop.isDesktopSupported} answers false
 * and {@code Desktop.getDesktop} throws. So the source is an object of this package's own.
 * {@link EventObject} demands a non-null source and {@code AppEvent}'s documentation does not promise
 * which class it is, so this is legal; what cannot be done is casting {@link #getSource} to
 * {@code Desktop}.
 */
public class AppEvent extends EventObject {

    private static final long serialVersionUID = -5958503993556009432L;

    /**
     * The common source of all these events. See the class note.
     *
     * <p>Its {@code toString} says so, so that whoever inspects an event understands what they are
     * looking at instead of assuming it is a {@code Desktop}.
     */
    private static final Object SOURCE = new EventSource();

    /** The common source. Named and not anonymous so that its {@code toString} reads well in a dump. */
    private static final class EventSource {
        @Override
        public String toString() {
            return "java.awt.desktop (no Desktop in this library)";
        }
    }

    /** Package-private on purpose; see the class note. */
    AppEvent() {
        super(SOURCE);
    }
}
