package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.DTDHandler -- las dos declaraciones de la DTD que SAX le reporta a
// *toda* aplicacion, valide o no.
//
// Por que solo estas dos: una entidad no analizada es contenido que no es XML (una imagen, por
// ejemplo) al que el documento se refiere por nombre, y una notacion dice de que tipo de cosa se
// trata. Una aplicacion que quiera seguir esa referencia no tiene otra manera de resolver el
// nombre, asi que SAX considera estas dos declaraciones parte del contrato basico y deja el
// resto de la DTD (elementos, atributos, entidades analizadas) al opcional ext.DeclHandler.
//
// Los dos eventos se reportan antes de que empiece el elemento del documento.
public interface DTDHandler {

    // Una declaracion de notacion. Exactamente uno de publicId/systemId puede ser null.
    void notationDecl(String name, String publicId, String systemId) throws SAXException;

    // Una declaracion de entidad no analizada. `notationName` nombra una notacion declarada en
    // otra parte de la DTD, y publicId puede ser null.
    void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
            throws SAXException;
}
