package javax.swing.plaf;

/**
 * El aspecto de un selector de valor con flechitas.
 *
 * <h2>Una clase vacia con un proposito</h2>
 *
 * <p>No agrega ningun metodo sobre {@link ComponentUI}: existe para nombrar el tipo. El componente
 * declara que su aspecto es un {@code SpinnerUI} y no un {@code ComponentUI} cualquiera, y eso hace
 * que ponerle el aspecto de otro componente sea un error de compilacion en lugar de una falla al
 * dibujar.
 *
 * <p>Los aspectos que si tienen algo que preguntar -- {@link ListUI}, {@link ComboBoxUI},
 * {@link SplitPaneUI} -- declaran sus metodos; los que no, quedan asi.
 */
public abstract class SpinnerUI extends ComponentUI {

    protected SpinnerUI() {
    }
}
