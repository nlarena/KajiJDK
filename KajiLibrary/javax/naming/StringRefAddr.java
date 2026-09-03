package javax.naming;

/**
 * Una direccion que es texto: una URL, un nombre de host, un identificador.
 *
 * <p>Es el caso comun y no agrega nada mas que el contenido. `equals`, `hashCode` y `toString`
 * salen tal cual de `RefAddr`, y ahi funcionan bien porque el `equals` de `String` es el correcto
 * --que es justo lo que no pasa con `BinaryRefAddr`, ver ahi--.
 *
 * <p>El contenido puede ser `null`: hay direcciones que se identifican solo por su tipo.
 */
public class StringRefAddr extends RefAddr {

    private static final long serialVersionUID = -8913762495138505527L;

    private String contents;

    public StringRefAddr(String addrType, String addr) {
        super(addrType);
        contents = addr;
    }

    @Override
    public Object getContent() {
        return contents;
    }
}
