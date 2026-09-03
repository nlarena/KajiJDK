package javax.annotation.processing;

import java.io.IOException;

// El error que un {@link Filer} tira cuando se le pide algo que viola su contrato: crear dos veces
// el mismo tipo, escribir sobre una fuente que ya existia, o un nombre que no es valido. Es una
// `IOException` y no una no-comprobada a proposito: el `Filer` hace E/S, y quien genera codigo
// tiene que decidir que hacer si no puede escribir.
public class FilerException extends IOException {

    // `static final` explicito, como en el resto de la biblioteca: el valor es el del JDK real para
    // que un flujo serializado cruce en las dos direcciones.
    static final long serialVersionUID = 8426423106453163293L;

    /**
     * @param s la razon por la que la operacion del `Filer` no se pudo hacer
     */
    public FilerException(String s) {
        super(s);
    }
}
