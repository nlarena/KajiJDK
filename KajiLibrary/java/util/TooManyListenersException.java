package java.util;

// Señala que una fuente de eventos **unicast** ya tiene su oyente.
//
// Es una excepcion chequeada, y eso es el mecanismo entero: el modelo de eventos de Java es
// multicast por defecto, y la unica forma de declarar "esta fuente admite un solo oyente" es que
// su `addXListener` declare `throws TooManyListenersException`. La firma es la documentacion.
public class TooManyListenersException extends Exception {

    // Sin mensaje.
    public TooManyListenersException() {
        super();
    }

    // Con el mensaje dado.
    public TooManyListenersException(String s) {
        super(s);
    }
}
