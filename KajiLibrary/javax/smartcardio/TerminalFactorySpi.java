package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.TerminalFactorySpi -- lo que implementa un proveedor de lectores.
 *
 * <p>Un proveedor que quiera dar acceso a lectores registra un servicio {@code TerminalFactory} cuya
 * clase extiende esto. {@link TerminalFactory} es la cara publica; esto es lo unico que hay que
 * escribir.
 *
 * <p>El constructor de la subclase recibe el parametro que se le paso a
 * {@link TerminalFactory#getInstance}, y ahi es donde va la configuracion --que biblioteca cargar, a
 * que servidor conectarse--.
 */
public abstract class TerminalFactorySpi {

    /** Para las subclases. */
    protected TerminalFactorySpi() {
    }

    /** Los lectores de este proveedor. */
    protected abstract CardTerminals engineTerminals();
}
