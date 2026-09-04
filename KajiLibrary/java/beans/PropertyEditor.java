package java.beans;

// Como una herramienta muestra y edita el valor de una propiedad: convertirlo a texto y de vuelta,
// ofrecer una lista cerrada de opciones, y dar el codigo Java que lo reconstruye.
//
// **Omitidos a proposito: `java.awt.Component getCustomEditor()` y
// `paintValue(java.awt.Graphics, java.awt.Rectangle)`.** Los dos son de la mitad grafica y sus
// tipos vienen de `java.awt`, que no existe en este arbol. `isPaintable()` y
// `supportsCustomEditor()` si estan —son boolean— y contestan false, que es la respuesta honesta
// cuando los metodos que habilitarian no estan.
public interface PropertyEditor {

    void setValue(Object value);

    Object getValue();

    // Si el editor sabe dibujarse. Sin paintValue en este arbol, una implementacion honesta
    // devuelve false.
    boolean isPaintable();

    // El fragmento de codigo Java que reconstruye este valor, para generadores de codigo.
    String getJavaInitializationString();

    String getAsText();

    void setAsText(String text) throws IllegalArgumentException;

    // Los valores validos, cuando la propiedad es de lista cerrada (un enum, por ejemplo).
    // null significa "no es de lista cerrada".
    String[] getTags();

    boolean supportsCustomEditor();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
