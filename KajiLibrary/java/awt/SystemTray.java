package java.awt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

/**
 * La bandeja del sistema: la zona de íconos al lado del reloj.
 *
 * <p>Es de la sesión de escritorio, no del programa, así que hay **una sola** y se la pide con
 * {@link #getSystemTray}. Varios programas ponen íconos en la misma bandeja, pero cada uno ve sólo
 * los suyos: {@link #getTrayIcons} devuelve los de este programa y no los del vecino.
 *
 * <p>Antes de usarla hay que preguntar {@link #isSupported}, y no es una formalidad: hay escritorios
 * enteros que no tienen bandeja. <strong>Acá nunca la hay</strong> —no hay sistema de ventanas— así
 * que `isSupported` da `false` y {@link #getSystemTray} tira {@link UnsupportedOperationException},
 * que es exactamente lo que hace el JDK en esa situación.
 */
public class SystemTray {

    /** La única bandeja, si alguna vez se llega a pedir. */
    private static SystemTray unica;

    /** Los íconos de este programa. */
    private final ArrayList<TrayIcon> iconos = new ArrayList<TrayIcon>();

    /** Los oyentes de cambios. */
    private final PropertyChangeSupport cambios = new PropertyChangeSupport(this);

    /** No se instancia desde afuera: la bandeja es una sola. */
    private SystemTray() {
    }

    /**
     * La bandeja del sistema.
     *
     * @throws UnsupportedOperationException siempre acá: no hay bandeja
     * @throws HeadlessException si no hay pantalla
     */
    public static SystemTray getSystemTray() {
        if (!isSupported()) {
            throw new UnsupportedOperationException("The system tray is not supported on the current platform.");
        }
        synchronized (SystemTray.class) {
            if (unica == null) {
                unica = new SystemTray();
            }
            return unica;
        }
    }

    /**
     * Si esta plataforma tiene bandeja.
     *
     * @return `false` siempre: sin sistema de ventanas no hay ninguna
     */
    public static boolean isSupported() {
        return false;
    }

    /**
     * Agrega un ícono a la bandeja.
     *
     * <p>El mismo ícono no se puede agregar dos veces, ni a dos bandejas: sería el mismo objeto
     * pretendiendo estar en dos lugares.
     *
     * @throws AWTException si la bandeja no se puede usar
     * @throws NullPointerException si el ícono es `null`
     * @throws IllegalArgumentException si ese ícono ya está en una bandeja
     */
    public void add(TrayIcon trayIcon) throws AWTException {
        if (trayIcon == null) {
            throw new NullPointerException("adding null TrayIcon");
        }
        synchronized (this) {
            if (this.iconos.contains(trayIcon)) {
                throw new IllegalArgumentException("adding TrayIcon that is already added");
            }
            this.iconos.add(trayIcon);
        }
        this.cambios.firePropertyChange("trayIcons", null, this.getTrayIcons());
    }

    /**
     * Saca un ícono de la bandeja.
     *
     * <p>Un ícono que no está, o un `null`, no hacen nada: sacar lo que no está ya dejó el mundo como
     * se quería.
     */
    public void remove(TrayIcon trayIcon) {
        if (trayIcon == null) {
            return;
        }
        boolean saco;
        synchronized (this) {
            saco = this.iconos.remove(trayIcon);
        }
        if (saco) {
            this.cambios.firePropertyChange("trayIcons", null, this.getTrayIcons());
        }
    }

    /**
     * Los íconos que este programa puso.
     *
     * @return una copia; un arreglo vacío si no puso ninguno. Nunca `null`.
     */
    public TrayIcon[] getTrayIcons() {
        synchronized (this) {
            return this.iconos.toArray(new TrayIcon[0]);
        }
    }

    /**
     * De qué tamaño quiere la bandeja los íconos.
     *
     * <p>No es un tope sino una recomendación: un ícono de otro tamaño se escala o se recorta según
     * {@link TrayIcon#setImageAutoSize}.
     *
     * @throws UnsupportedOperationException nunca se llega acá: no hay instancias
     */
    public Dimension getTrayIconSize() {
        throw new UnsupportedOperationException("The system tray is not supported on the current platform.");
    }

    /** Agrega un oyente para esa propiedad; `null` no hace nada. */
    public synchronized void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Saca un oyente de esa propiedad. */
    public synchronized void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Los oyentes de esa propiedad.
     *
     * @return los oyentes; un arreglo vacío si no hay ninguno
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.cambios.getPropertyChangeListeners(propertyName);
    }
}
