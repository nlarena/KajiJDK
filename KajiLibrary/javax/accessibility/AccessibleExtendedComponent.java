package javax.accessibility;

/**
 * Lo que un componente accesible puede contar de más allá de su geometría.
 *
 * <p>Las tres cosas que agrega tienen algo en común: son texto que el usuario **ve pero que no es el
 * contenido** del control. La ayudita que aparece al pasar el puntero, el título del recuadro que lo
 * rodea, la tecla que lo activa. Para quien no ve la pantalla, eso es información que se perdería.
 */
public interface AccessibleExtendedComponent extends AccessibleComponent {

    /** La ayudita emergente, o `null` si no tiene. */
    String getToolTipText();

    /** El título del recuadro que lo rodea, o `null` si no tiene. */
    String getTitledBorderText();

    /** Los atajos de teclado que lo activan, o `null` si no tiene. */
    AccessibleKeyBinding getAccessibleKeyBinding();
}
