package java.security;

// Una entrada del almacen de claves no se pudo recuperar: tipicamente, la contraseña esta mal.
//
// Dos constructores y no cuatro: no lleva causa encadenada porque la causa real —"la contraseña no
// era la correcta"— es justo lo que no conviene exponer. Un stack trace que distinga "clave mal
// puesta" de "entrada corrupta" le dice a un atacante cuando acerto la mitad del problema.
public class UnrecoverableEntryException extends GeneralSecurityException {

    public UnrecoverableEntryException() {
        super();
    }

    public UnrecoverableEntryException(String message) {
        super(message);
    }
}
