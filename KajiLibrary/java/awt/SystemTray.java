package java.awt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

/**
 * The system tray: the strip of icons next to the clock.
 *
 * <p>It belongs to the desktop session, not to the program, so there is **one only** and it is
 * asked for with {@link #getSystemTray}. Several programs put icons in the same tray, but each one
 * sees only its own: {@link #getTrayIcons} returns this program's and not the neighbour's.
 *
 * <p>Before using it one has to ask {@link #isSupported}, and that is not a formality: there are
 * whole desktops with no tray. <strong>Here there never is one</strong> —there is no windowing
 * system— so `isSupported` gives `false` and {@link #getSystemTray} throws
 * {@link UnsupportedOperationException}, which is exactly what the JDK does in that situation.
 */
public class SystemTray {

    /** The only tray, if it ever gets asked for. */
    private static SystemTray instance;

    /** This program's icons. */
    private final ArrayList<TrayIcon> icons = new ArrayList<TrayIcon>();

    /** The change listeners. */
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /** Not instantiated from outside: there is a single tray. */
    private SystemTray() {
    }

    /**
     * The system tray.
     *
     * <p>The JDK checks for a screen first and throws {@link HeadlessException} there; here the
     * answer comes earlier, because the tray is not supported at all.
     *
     * @throws UnsupportedOperationException always here: there is no tray
     */
    public static SystemTray getSystemTray() {
        if (!isSupported()) {
            throw new UnsupportedOperationException("The system tray is not supported on the current platform.");
        }
        synchronized (SystemTray.class) {
            if (instance == null) {
                instance = new SystemTray();
            }
            return instance;
        }
    }

    /**
     * Whether this platform has a tray.
     *
     * @return `false` always: without a windowing system there is none
     */
    public static boolean isSupported() {
        return false;
    }

    /**
     * Adds an icon to the tray.
     *
     * <p>The same icon cannot be added twice, nor to two trays: it would be the same object
     * pretending to be in two places.
     *
     * @throws AWTException if the tray cannot be used
     * @throws NullPointerException if the icon is `null`
     * @throws IllegalArgumentException if that icon is already in a tray
     */
    public void add(TrayIcon trayIcon) throws AWTException {
        if (trayIcon == null) {
            throw new NullPointerException("adding null TrayIcon");
        }
        synchronized (this) {
            if (this.icons.contains(trayIcon)) {
                throw new IllegalArgumentException("adding TrayIcon that is already added");
            }
            this.icons.add(trayIcon);
        }
        this.changeSupport.firePropertyChange("trayIcons", null, this.getTrayIcons());
    }

    /**
     * Takes an icon out of the tray.
     *
     * <p>An icon that is not there, or a `null`, do nothing: taking out what is not there already
     * left the world the way it was wanted.
     */
    public void remove(TrayIcon trayIcon) {
        if (trayIcon == null) {
            return;
        }
        boolean removed;
        synchronized (this) {
            removed = this.icons.remove(trayIcon);
        }
        if (removed) {
            this.changeSupport.firePropertyChange("trayIcons", null, this.getTrayIcons());
        }
    }

    /**
     * The icons this program put in.
     *
     * @return a copy; an empty array if it put none in. Never `null`.
     */
    public TrayIcon[] getTrayIcons() {
        synchronized (this) {
            return this.icons.toArray(new TrayIcon[0]);
        }
    }

    /**
     * What size the tray wants the icons.
     *
     * <p>It is not a limit but a recommendation: an icon of another size is scaled or cropped
     * according to {@link TrayIcon#setImageAutoSize}.
     *
     * @throws UnsupportedOperationException always —though nothing gets here: there is no way to
     *     obtain an instance
     */
    public Dimension getTrayIconSize() {
        throw new UnsupportedOperationException("The system tray is not supported on the current platform.");
    }

    /** Adds a listener for that property; `null` does nothing. */
    public synchronized void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Removes a listener of that property. */
    public synchronized void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * The listeners of that property.
     *
     * @return the listeners; an empty array if there is none
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.changeSupport.getPropertyChangeListeners(propertyName);
    }
}
