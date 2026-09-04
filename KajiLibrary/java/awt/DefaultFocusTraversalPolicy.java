package java.awt;

/**
 * La política de recorrido de fábrica: sigue el orden en que se agregaron los hijos.
 *
 * <p>Lo único que le agrega a {@link ContainerOrderFocusTraversalPolicy} es **cómo decide si un
 * componente entra en el recorrido**, y la diferencia importa. La de orden de contenedor pregunta
 * `isFocusable()`; ésta, además, mira si el componente **fijó** su focabilidad a mano
 * ({@link Component#isFocusTraversalPolicySet}, que acá se resuelve mirando si alguien llamó a
 * {@code setFocusable}). Si nadie la fijó, cae en la respuesta vieja de AWT
 * ({@link Component#isFocusTraversable}).
 *
 * <p>El motivo es de compatibilidad: antes de 1.4 un componente decía si entraba al recorrido
 * redefiniendo `isFocusTraversable()`. Esta política sigue respetando esa redefinición mientras nadie
 * haya dicho lo contrario con la API nueva, así que el código viejo sigue recorriendo igual.
 */
public class DefaultFocusTraversalPolicy extends ContainerOrderFocusTraversalPolicy {

    private static final long serialVersionUID = 8876966522510157497L;

    /** Una política de fábrica. */
    public DefaultFocusTraversalPolicy() {
    }

    /**
     * Si ese componente entra en el recorrido.
     *
     * <p>Tiene que estar visible, habilitado y mostrable, además de admitir el foco.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable() || !aComponent.isEnabled()) {
            return false;
        }
        return aComponent.isFocusable();
    }
}
