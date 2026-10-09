package java.beans;

// A vetoable-change listener refused the change. It carries the vetoed event with it, so that
// whoever receives it knows which property it was and with what values; without that the veto would
// not be actionable.
public class PropertyVetoException extends Exception {

    // The vetoed event. `serialVersionUID` apart, this is the class's only state of its own.
    private PropertyChangeEvent evt;

    public PropertyVetoException(String mess, PropertyChangeEvent evt) {
        super(mess);
        this.evt = evt;
    }

    public PropertyChangeEvent getPropertyChangeEvent() {
        return this.evt;
    }
}
