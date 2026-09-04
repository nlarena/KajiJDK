package java.beans;

import java.util.ArrayList;
import java.util.List;

// Base comoda para escribir un PropertyEditor: guarda el valor, avisa de los cambios y da
// respuestas razonables al resto. Un editor concreto normalmente solo redefine getAsText/setAsText.
//
// El `source` existe porque un editor suele ser creado por una herramienta y no por el bean: los
// eventos tienen que decir que el origen es el bean editado, no el editor. Por defecto es el editor
// mismo, que es lo correcto cuando nadie dijo otra cosa.
//
// **Omitidos a proposito: `getCustomEditor()` y `paintValue(Graphics, Rectangle)`.** Sus tipos son
// de java.awt, que no existe en este arbol. Las dos consultas que los acompanan —isPaintable() y
// supportsCustomEditor()— si estan y contestan false, que es la verdad aca: este editor no puede
// pintarse ni ofrecer un panel propio.
public class PropertyEditorSupport implements PropertyEditor {

    private Object value;
    private Object source;
    private List<PropertyChangeListener> oyentes;

    // El editor es su propio origen de eventos.
    public PropertyEditorSupport() {
        this.source = this;
        this.oyentes = new ArrayList<PropertyChangeListener>();
    }

    // Los eventos van a decir que vienen de `source`, no de este editor.
    public PropertyEditorSupport(Object source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
        this.oyentes = new ArrayList<PropertyChangeListener>();
    }

    public Object getSource() {
        return this.source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public Object getValue() {
        return this.value;
    }

    // Fijar el valor avisa siempre, aunque sea el mismo: a diferencia de PropertyChangeSupport, un
    // editor no compara. Quien lo llama fue una accion explicita del usuario.
    public void setValue(Object value) {
        this.value = value;
        this.firePropertyChange();
    }

    // Sin java.awt no hay con que pintar; decir true seria prometer un paintValue que no existe.
    public boolean isPaintable() {
        return false;
    }

    public boolean supportsCustomEditor() {
        return false;
    }

    public String getAsText() {
        String s = null;
        if (this.value != null) {
            s = this.value.toString();
        }
        return s;
    }

    // La base no sabe convertir texto a ningun tipo en particular. Aceptarlo cuando el valor ya es
    // texto es lo unico honesto que puede hacer; para cualquier otro tipo, rechazar.
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.value == null || this.value instanceof String) {
            this.setValue(text);
        } else {
            throw new IllegalArgumentException(text);
        }
    }

    // La propiedad no es de lista cerrada mientras una subclase no diga lo contrario.
    public String[] getTags() {
        return null;
    }

    // El codigo Java que reconstruye el valor. "???" es literalmente lo que devuelve el JDK cuando
    // no sabe: un generador que lo reciba produce codigo que no compila, y eso es preferible a
    // producir codigo que compile y arme otra cosa.
    public String getJavaInitializationString() {
        String s = "???";
        if (this.value == null) {
            s = "null";
        } else if (this.value instanceof String) {
            s = "\"" + this.value + "\"";
        }
        return s;
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.oyentes.add(listener);
        }
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.oyentes.remove(listener);
        }
    }

    // Avisa que el valor cambio. Igual que en PropertyChangeSupport se despacha sobre una copia,
    // por la misma razon: un oyente puede desuscribirse desde adentro.
    public void firePropertyChange() {
        PropertyChangeListener[] copia = this.instantanea();
        if (copia.length > 0) {
            // El JDK manda los tres campos en null: el editor no lleva el nombre de la propiedad
            // que edita, y un evento con nombre inventado seria peor que uno sin nombre.
            PropertyChangeEvent evt = new PropertyChangeEvent(this.source, null, null, null);
            for (int i = 0; i < copia.length; i++) {
                copia[i].propertyChange(evt);
            }
        }
    }

    private synchronized PropertyChangeListener[] instantanea() {
        PropertyChangeListener[] a = new PropertyChangeListener[this.oyentes.size()];
        for (int i = 0; i < this.oyentes.size(); i++) {
            a[i] = this.oyentes.get(i);
        }
        return a;
    }
}
