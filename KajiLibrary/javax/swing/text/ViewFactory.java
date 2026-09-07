package javax.swing.text;

/**
 * Quien decide que vista le corresponde a cada elemento.
 *
 * <p>Un solo metodo, y en el esta la libertad del sistema: el mismo documento se ve como texto
 * plano, como HTML o como una lista, segun quien fabrique las vistas. Un editor cambia de aspecto
 * cambiando esta fabrica, no el documento.
 */
public interface ViewFactory {

    /** La vista que le corresponde a ese elemento. */
    View create(Element elem);
}
