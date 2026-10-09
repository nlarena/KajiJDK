package java.beans;

// The bespoke panel a tool edits a whole bean with, when editing property by property is not
// enough. Whoever implements it receives the bean through setObject and reports the changes like any
// source of bound properties.
//
// In the JDK a Customizer also inherits from java.awt.Component; here it cannot, because java.awt
// does not exist in this tree. The interface itself —its three methods— does not touch awt and is
// complete.
public interface Customizer {

    // The bean to edit. It is called once only, before showing the panel.
    void setObject(Object bean);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
