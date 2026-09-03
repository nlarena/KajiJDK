package org.xml.sax.ext;

import org.xml.sax.Locator;

/**
 * KajiLibrary's org.xml.sax.ext.Locator2Impl -- un {@link Locator2} que se puede guardar, y el que
 * un parser usa para entregar la posicion.
 *
 * <p>Sirve para las dos cosas de siempre, igual que `LocatorImpl`: **congelar** una posicion --el
 * `Locator` que el parser presta es vivo y preguntarle despues del evento contesta por otro lado--
 * y **construir** una cuando uno mismo genera los eventos.
 *
 * <p>El detalle que no se ve en la firma esta en el constructor de copia: si el `Locator` que le
 * pasan **no** es un `Locator2`, la version y la codificacion quedan en `null` en vez de inventarse
 * un `"1.0"` por omision. Es la unica respuesta honesta --no se sabe cual era-- y ademas la que
 * hace distinguible el caso de un `Locator2` que devolvio `null` porque el parser tampoco sabia.
 */
public class Locator2Impl extends org.xml.sax.helpers.LocatorImpl implements Locator2 {

    private String encoding;
    private String version;

    /** Todo sin valor, listo para que le pongan los campos. */
    public Locator2Impl() {
    }

    /**
     * La foto descrita arriba. Copia siempre los cuatro campos del `Locator`, y los dos de
     * `Locator2` solo cuando el objeto los tiene.
     */
    public Locator2Impl(Locator locator) {
        super(locator);
        if (locator instanceof Locator2) {
            Locator2 l2 = (Locator2) locator;
            version = l2.getXMLVersion();
            encoding = l2.getEncoding();
        }
    }

    public String getXMLVersion() {
        return version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setXMLVersion(String version) {
        this.version = version;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
