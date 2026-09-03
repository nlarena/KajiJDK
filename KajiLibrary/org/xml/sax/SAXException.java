package org.xml.sax;

// KajiLibrary's org.xml.sax.SAXException -- la excepcion chequeada que toda callback de SAX
// tiene permitido lanzar. Es anterior a las excepciones encadenadas de JDK 1.4, asi que le
// crecio su propio casillero de "excepcion envuelta"; hoy ese casillero *es* la causa de
// Throwable, y los dos constructores que toman una Exception simplemente se la pasan a super.
// getException() lee la causa de vuelta, angostada a Exception (una causa que sea un Error o un
// Throwable pelado se lee como null, que es lo que hace el JDK).
//
// La regla que vale la pena leer dos veces es getMessage(): cuando esta excepcion no tiene
// mensaje propio pero si tiene causa, contesta con el mensaje *de la causa* en vez de null. Asi
//
//     new SAXException(new java.io.IOException("disk on fire")).getMessage()
//
// da "disk on fire", no null. Solo con los dos en null sale null, y un mensaje propio siempre le
// gana al de la causa.
//
// Lo que queda afuera, y por que: los ganchos de serializacion (writeObject/readObject y el
// arreglo serialPersistentFields que mantiene en el alambre el nombre historico de campo
// "exception"). Son privados, asi que ningun contrato depende de ellos, y harian falta
// java.io.ObjectStreamField mas ObjectOutputStream.PutField / ObjectInputStream.GetField, que
// esta biblioteca no tiene. serialVersionUID se deja en el valor del JDK para que un flujo
// escrito en otro lado siga nombrando la misma clase.
public class SAXException extends Exception {

    static final long serialVersionUID = 583241635256073760L;

    public SAXException() {
        super();
    }

    public SAXException(String message) {
        super(message);
    }

    public SAXException(Exception e) {
        super(e);
    }

    public SAXException(String message, Exception e) {
        super(message, e);
    }

    // La regla de delegacion descripta arriba. Notar que lee super.getMessage(), no
    // getMessage(): lo segundo se llamaria a si mismo.
    public String getMessage() {
        String message = super.getMessage();
        Throwable cause = super.getCause();

        if (message == null && cause != null) {
            return cause.getMessage();
        } else {
            return message;
        }
    }

    // La excepcion envuelta, es decir la causa cuando resulta ser una Exception.
    public Exception getException() {
        return getExceptionInternal();
    }

    // Declarado explicitamente (en vez de heredado) porque el JDK tambien lo declara aca: el
    // contrato lista getCause() como miembro de esta clase.
    public Throwable getCause() {
        return super.getCause();
    }

    public String toString() {
        Throwable exception = super.getCause();
        if (exception != null) {
            return super.toString() + "\n" + exception.toString();
        } else {
            return super.toString();
        }
    }

    private Exception getExceptionInternal() {
        Throwable cause = super.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        } else {
            return null;
        }
    }
}
