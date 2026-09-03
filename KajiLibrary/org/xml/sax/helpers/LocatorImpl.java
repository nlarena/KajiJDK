package org.xml.sax.helpers;

import org.xml.sax.Locator;

// KajiLibrary's org.xml.sax.helpers.LocatorImpl -- un Locator que se puede guardar.
//
// El Locator que el parser entrega en setDocumentLocator() esta vivo: contesta por donde va el
// analisis en este momento, y preguntarle una vez que el evento ya paso da una respuesta sobre
// otro lado. Asi que un manejador que quiera recordar donde ocurrio un evento no puede guardarse
// el Locator; tiene que copiarlo. Justamente para eso esta aca el constructor de copia:
//
//     locatorForThisEvent = new LocatorImpl(theParsersLocator);
//
// Todo lo demas es una bolsa mutable de cuatro campos, que es tambien la razon por la que los
// parsers lo usan de entrada como el Locator que reparten.
public class LocatorImpl implements Locator {

    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    // Los cuatro campos sin asignar: ids en null y posiciones en cero.
    public LocatorImpl() {
    }

    // El constructor de copia descrito arriba.
    public LocatorImpl(Locator locator) {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }
}
