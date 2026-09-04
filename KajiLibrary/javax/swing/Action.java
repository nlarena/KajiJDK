package javax.swing;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/**
 * Una accion: lo que hace un boton o un menu, separado del boton o del menu.
 *
 * <p>Es un {@link ActionListener} con propiedades —nombre, icono, mnemonico, si esta
 * habilitada— y con avisos cuando cambian. La separacion es lo que permite que un mismo "Guardar"
 * viva en un boton, en un menu y en un atajo, y que deshabilitarlo una vez lo deshabilite en los
 * tres: cada componente escucha las propiedades y se acomoda.
 *
 * <p>Las claves son cadenas porque las propiedades son abiertas: una aplicacion puede guardar las
 * suyas junto a las estandar.
 */
public interface Action extends ActionListener {

    /** Clave de una propiedad por omision; no la usa nadie del JDK, existe por historia. */
    String DEFAULT = "Default";

    /** El nombre: el texto del boton o del menu. */
    String NAME = "Name";

    /** Una descripcion corta: el texto de ayuda flotante. */
    String SHORT_DESCRIPTION = "ShortDescription";

    /** Una descripcion larga, para ayuda contextual. */
    String LONG_DESCRIPTION = "LongDescription";

    /** El icono chico: el de un menu, y el de un boton si no hay grande. */
    String SMALL_ICON = "SmallIcon";

    /** El comando que va en el {@code ActionEvent}. */
    String ACTION_COMMAND_KEY = "ActionCommandKey";

    /** El acelerador de teclado, un {@code KeyStroke}. */
    String ACCELERATOR_KEY = "AcceleratorKey";

    /** El mnemonico, un {@code Integer} con la tecla virtual. */
    String MNEMONIC_KEY = "MnemonicKey";

    /** Si esta seleccionada, para botones con estado; un {@code Boolean}. */
    String SELECTED_KEY = "SwingSelectedKey";

    /** Que caracter del nombre subrayar; un {@code Integer}. */
    String DISPLAYED_MNEMONIC_INDEX_KEY = "SwingDisplayedMnemonicIndexKey";

    /** El icono grande: el de un boton, si esta. */
    String LARGE_ICON_KEY = "SwingLargeIconKey";

    /** La propiedad con esa clave, o {@code null}. */
    Object getValue(String key);

    /** Pone la propiedad con esa clave, avisando a los escuchas si cambio. */
    void putValue(String key, Object value);

    /** Habilita o deshabilita; los componentes que la usan se enteran y se acomodan. */
    void setEnabled(boolean b);

    boolean isEnabled();

    /**
     * Si esta accion acepta dispararse desde ese origen.
     *
     * <p>Por omision acepta a cualquiera. Es la manera de que una accion diga "desde este
     * componente no", sin deshabilitarse para los demas.
     */
    default boolean accept(Object sender) {
        return true;
    }

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
