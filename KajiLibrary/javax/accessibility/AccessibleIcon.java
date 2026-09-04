package javax.accessibility;

/**
 * La descripción de un ícono, para quien no lo puede ver.
 *
 * <p>La descripción es lo único que importa acá: un ícono sin descripción es invisible para una
 * ayuda técnica, aunque esté perfectamente dibujado. El tamaño está para que se pueda maquetar un
 * hueco equivalente.
 */
public interface AccessibleIcon {

    /**
     * Qué representa el ícono, en palabras.
     *
     * @return la descripción, o `null` si no tiene
     */
    String getAccessibleIconDescription();

    /** Cambia la descripción. */
    void setAccessibleIconDescription(String description);

    /** Ancho del ícono, o -1 si no se sabe. */
    int getAccessibleIconWidth();

    /** Alto del ícono, o -1 si no se sabe. */
    int getAccessibleIconHeight();
}
