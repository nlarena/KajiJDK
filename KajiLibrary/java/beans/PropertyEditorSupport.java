package java.beans;

import java.util.ArrayList;
import java.util.List;

// A comfortable base for writing a PropertyEditor: it keeps the value, reports the changes and
// gives reasonable answers to the rest. A concrete editor normally only overrides
// getAsText/setAsText.
//
// The `source` exists because an editor is usually created by a tool and not by the bean: the events
// have to say that the source is the bean being edited, not the editor. By default it is the editor
// itself, which is right when nobody said otherwise.
public class PropertyEditorSupport implements PropertyEditor {

    private Object value;
    private Object source;
    private List<PropertyChangeListener> oyentes;

    // The editor is its own event source.
    public PropertyEditorSupport() {
        this.source = this;
        this.oyentes = new ArrayList<PropertyChangeListener>();
    }

    // The events are going to say they come from `source`, not from this editor.
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

    // Setting the value always reports, even if it is the same one: unlike PropertyChangeSupport,
    // an editor does not compare. Whoever called it was an explicit action of the user's.
    public void setValue(Object value) {
        this.value = value;
        this.firePropertyChange();
    }

    // The base one does not know how to draw itself: the tool is going to show getAsText().
    public boolean isPaintable() {
        return false;
    }

    /**
     * Draws the value.
     *
     * <p>The base one does nothing, and it is consistent with {@link #isPaintable}: it said it does
     * not know how. A subclass overriding this has to override that one too, or nobody will call
     * it.
     */
    public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {
    }

    public boolean supportsCustomEditor() {
        return false;
    }

    /**
     * A panel of its own for editing the value.
     *
     * @return `null`: the base one has none, as {@link #supportsCustomEditor} announces
     */
    public java.awt.Component getCustomEditor() {
        return null;
    }

    public String getAsText() {
        String s = null;
        if (this.value != null) {
            s = this.value.toString();
        }
        return s;
    }

    // The base does not know how to convert text into any particular type. Accepting it when the
    // value is already text is the only honest thing it can do; for any other type, refuse.
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.value == null || this.value instanceof String) {
            this.setValue(text);
        } else {
            throw new IllegalArgumentException(text);
        }
    }

    // The property is not of a closed list unless a subclass says otherwise.
    public String[] getTags() {
        return null;
    }

    // The Java code that rebuilds the value. "???" is literally what the JDK returns when it does
    // not know: a generator receiving it produces code that does not compile, and that is preferable
    // to producing code that compiles and builds something else.
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

    // It reports that the value changed. Just as in PropertyChangeSupport it dispatches over a
    // copy, for the same reason: a listener may unsubscribe from inside.
    public void firePropertyChange() {
        PropertyChangeListener[] copia = this.instantanea();
        if (copia.length > 0) {
            // The JDK sends the three fields as null: the editor does not carry the name of the
            // property it edits, and an event with an invented name would be worse than one with no
            // name.
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
