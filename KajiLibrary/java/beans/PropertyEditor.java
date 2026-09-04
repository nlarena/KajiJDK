package java.beans;

// Como una herramienta muestra y edita el valor de una propiedad: convertirlo a texto y de vuelta,
// ofrecer una lista cerrada de opciones, dar el codigo Java que lo reconstruye, y --si sabe--
// dibujarse o traer un panel propio.
public interface PropertyEditor {

    void setValue(Object value);

    Object getValue();

    // Si el editor sabe dibujarse con paintValue. Un editor que contesta true tiene que estar
    // dispuesto a que la herramienta lo llame en vez de mostrarle getAsText().
    boolean isPaintable();

    /**
     * Dibuja una representación del valor en ese rectángulo.
     *
     * <p>Es lo que una herramienta usa en vez de {@link #getAsText} cuando {@link #isPaintable} dio
     * `true`: un editor de color pinta una muestra, uno de tipografía escribe con ella. Si el editor
     * no sabe dibujarse, no hace nada.
     */
    void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box);

    // El fragmento de codigo Java que reconstruye este valor, para generadores de codigo.
    String getJavaInitializationString();

    String getAsText();

    void setAsText(String text) throws IllegalArgumentException;

    // Los valores validos, cuando la propiedad es de lista cerrada (un enum, por ejemplo).
    // null significa "no es de lista cerrada".
    String[] getTags();

    /**
     * Un panel propio para editar el valor, cuando texto y lista no alcanzan.
     *
     * @return el componente, o `null` si el editor no tiene uno; {@link #supportsCustomEditor} lo
     *     anticipa
     */
    java.awt.Component getCustomEditor();

    boolean supportsCustomEditor();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
