package javax.swing.plaf;

/**
 * El aspecto de el icono de una ventana interna minimizada.
 *
 * <h2>Una clase vacia con un proposito</h2>
 *
 * <p>No agrega ningun metodo sobre {@link ComponentUI}: existe para nombrar el tipo. El componente
 * declara que su aspecto es un {@code DesktopIconUI} y no un {@code ComponentUI} cualquiera, y eso hace
 * que ponerle el aspecto de otro componente sea un error de compilacion en lugar de una falla al
 * dibujar.
 *
 * <p>Los aspectos que si tienen algo que preguntar -- {@link ListUI}, {@link ComboBoxUI},
 * {@link SplitPaneUI} -- declaran sus metodos; los que no, quedan asi.
 */
public abstract class DesktopIconUI extends ComponentUI {

    protected DesktopIconUI() {
    }
}
