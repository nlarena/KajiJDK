package java.beans;

// Recibe las excepciones que un Encoder o un decodificador prefiere reportar antes que propagar:
// serializar un grafo entero no deberia abortar por un nodo que falla.
public interface ExceptionListener {

    void exceptionThrown(Exception e);
}
