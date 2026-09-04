package java.net;

import java.io.InterruptedIOException;

// Se vencio el plazo de una operacion de socket.
//
// Hereda de `InterruptedIOException`, no de `SocketException`, y ahi esta todo el sentido de la
// clase: `InterruptedIOException` trae `bytesTransferred`, o sea **cuanto se alcanzo a mover antes
// de cortar**. Un timeout no invalida el socket -- lo que ya paso, paso -- y quien lo atrapa suele
// querer seguir desde donde quedo. Si colgara de `SocketException` diria "el socket murio", que es
// otra cosa.
public class SocketTimeoutException extends InterruptedIOException {

    private static final long serialVersionUID = -8846654841826352300L;

    public SocketTimeoutException(String msg) {
        super(msg);
    }

    public SocketTimeoutException() {
    }
}
