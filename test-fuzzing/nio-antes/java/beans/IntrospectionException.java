package java.beans;

// A failure while introspecting a bean: a method that was expected and is not there, a
// getter/setter pair whose types do not add up, a property name resolving to no accessor.
public class IntrospectionException extends Exception {

    public IntrospectionException(String mess) {
        super(mess);
    }
}
