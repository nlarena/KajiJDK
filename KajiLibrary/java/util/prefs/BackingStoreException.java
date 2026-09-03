package java.util.prefs;

// La excepcion que dice "no pude hablar con el deposito".
//
// Es la unica excepcion *comprobada* del paquete, y esta puesta en muy pocos metodos a proposito:
// `keys`, `childrenNames`, `nodeExists`, `removeNode`, `clear`, `flush` y `sync`. Los `put` y los
// `get` NO la tiran, y eso es el diseño central de `Preferences`: guardar y leer una preferencia
// tiene que poder escribirse sin un `try`, porque una preferencia que no se pudo leer se resuelve
// con el valor por omision y no hay nada que informar. Solo las operaciones que *no* tienen una
// respuesta por omision razonable --enumerar, borrar, forzar la escritura-- pueden fracasar de
// manera visible.
//
// No tiene constructor sin argumentos: una falla del deposito sin una causa ni un mensaje no le
// sirve a nadie.
public class BackingStoreException extends Exception {

    private static final long serialVersionUID = 859796500401108469L;

    // Una falla descrita por `s`.
    public BackingStoreException(String s) {
        super(s);
    }

    // Una falla provocada por `cause` --tipicamente la excepcion de entrada/salida que la origino.
    public BackingStoreException(Throwable cause) {
        super(cause);
    }
}
