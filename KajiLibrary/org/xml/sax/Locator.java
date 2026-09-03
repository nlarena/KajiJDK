package org.xml.sax;

// KajiLibrary's org.xml.sax.Locator -- "en que parte del documento estamos ahora?".
//
// El parser le entrega uno de estos a la aplicacion una sola vez, por
// ContentHandler.setDocumentLocator, *antes* de startDocument. El objeto esta vivo: la misma
// instancia sigue contestando con la posicion actual a medida que avanza el analisis. Por eso el
// contrato dice que solo vale adentro de la llamada de un evento --guardarlo y consultarlo
// despues devuelve lo que el parser estuviera haciendo en ese momento, o basura. Una aplicacion
// que quiera recordar una posicion la copia en una foto LocatorImpl.
//
// La linea y la columna empiezan en 1, y cualquiera de las dos puede ser -1 cuando el parser no
// sabe.
public interface Locator {

    // El identificador publico del evento actual del documento, o null si no hay ninguno.
    String getPublicId();

    // El identificador de sistema (tipicamente un URI) del evento actual del documento, o null.
    String getSystemId();

    // El numero de linea donde termina el evento actual del documento, o -1 si no se sabe. Apunta
    // al *fin* de la construccion que produjo el evento, no a su comienzo.
    int getLineNumber();

    // El numero de columna donde termina el evento actual del documento, o -1 si no se sabe. La
    // primera columna es la 1.
    int getColumnNumber();
}
