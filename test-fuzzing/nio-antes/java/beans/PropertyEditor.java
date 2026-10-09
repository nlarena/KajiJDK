package java.beans;

// How a tool shows and edits a property's value: converting it to text and back, offering a closed
// list of options, giving the Java code that rebuilds it, and --if it knows how-- drawing itself or
// bringing a panel of its own.
public interface PropertyEditor {

    void setValue(Object value);

    Object getValue();

    // Whether the editor knows how to draw itself with paintValue. An editor answering true has to
    // be prepared for the tool to call it instead of showing it getAsText().
    boolean isPaintable();

    /**
     * Draws a representation of the value in that rectangle.
     *
     * <p>It is what a tool uses instead of {@link #getAsText} when {@link #isPaintable} gave `true`:
     * a colour editor paints a swatch, a typeface one writes with it. If the editor does not know
     * how to draw itself, it does nothing.
     */
    void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box);

    // The fragment of Java code that rebuilds this value, for code generators.
    String getJavaInitializationString();

    String getAsText();

    void setAsText(String text) throws IllegalArgumentException;

    // The valid values, when the property is of a closed list (an enum, say). null means "it is not
    // of a closed list".
    String[] getTags();

    /**
     * A panel of its own for editing the value, when text and list are not enough.
     *
     * @return the component, or `null` if the editor has none; {@link #supportsCustomEditor}
     *     announces it in advance
     */
    java.awt.Component getCustomEditor();

    boolean supportsCustomEditor();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
