package org.xml.sax;

import org.xml.sax.AttributeList;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.DocumentHandler -- el antecesor de ContentHandler en SAX1.
//
// Las diferencias son exactamente dos, y las dos son por los espacios de nombres: un elemento
// llega con un solo nombre en vez de la terna (uri, localName, qName), y no hay eventos de
// mapeo de prefijos. Todo lo demas --el orden de las llamadas, la regla de que `characters`
// puede venir partido, la regla de que el char[] es una ventana prestada-- es igual.
//
// helpers.XMLReaderAdapter convierte un XMLReader de SAX2 en uno de estos;
// helpers.ParserAdapter va al reves. Deprecada en el JDK en favor de ContentHandler; ver
// AttributeList por que aca no se escribe @Deprecated.
public interface DocumentHandler {

    // El oraculo de posicion en vivo; ver ContentHandler.setDocumentLocator.
    void setDocumentLocator(Locator locator);

    // El comienzo del documento.
    void startDocument() throws SAXException;

    // El fin del documento.
    void endDocument() throws SAXException;

    // El comienzo de un elemento, nombrado tal cual estaba escrito en el documento.
    void startElement(String name, AttributeList atts) throws SAXException;

    // El fin de un elemento.
    void endElement(String name) throws SAXException;

    // Datos de caracteres; pueden venir partidos entre varias llamadas.
    void characters(char[] ch, int start, int length) throws SAXException;

    // Blanco del contenido de un elemento, reportado por un parser que valida.
    void ignorableWhitespace(char[] ch, int start, int length) throws SAXException;

    // Una instruccion de procesamiento.
    void processingInstruction(String target, String data) throws SAXException;
}
