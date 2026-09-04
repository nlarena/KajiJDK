package java.beans;

// Falla al introspeccionar un bean: un metodo que se esperaba y no esta, un par getter/setter
// con tipos que no cierran, un nombre de propiedad que no resuelve a ningun accesor.
public class IntrospectionException extends Exception {

    public IntrospectionException(String mess) {
        super(mess);
    }
}
