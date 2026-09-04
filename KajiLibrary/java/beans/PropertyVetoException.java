package java.beans;

// Un oyente de cambios vetables rechazo el cambio. Lleva consigo el evento que se vetó, para que
// quien lo reciba sepa que propiedad era y con que valores; sin eso el veto no seria accionable.
public class PropertyVetoException extends Exception {

    // El evento vetado. `serialVersionUID` aparte, este es el unico estado propio de la clase.
    private PropertyChangeEvent evt;

    public PropertyVetoException(String mess, PropertyChangeEvent evt) {
        super(mess);
        this.evt = evt;
    }

    public PropertyChangeEvent getPropertyChangeEvent() {
        return this.evt;
    }
}
