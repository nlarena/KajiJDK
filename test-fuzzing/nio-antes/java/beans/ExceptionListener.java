package java.beans;

// It receives the exceptions an Encoder or a decoder would rather report than propagate:
// serializing a whole graph should not abort because of one node that fails.
public interface ExceptionListener {

    void exceptionThrown(Exception e);
}
