package org.xml.sax.ext;

import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.DeclHandler -- lo que la DTD *declara*, no lo que el documento
 * dice.
 *
 * <p>El `DTDHandler` del nucleo reporta dos cosas y nada mas: notaciones y entidades sin analizar.
 * Eso alcanza para resolver referencias a datos binarios y para nada mas. Quien quiera saber el
 * modelo de contenido de un elemento, o el valor por omision de un atributo, o el texto de una
 * entidad interna --es decir, quien quiera **copiar la DTD**-- necesita esta interfaz.
 *
 * <p>Se instala con la propiedad `http://xml.org/sax/properties/declaration-handler` del
 * `XMLReader`, y como toda extension el parser puede no reconocerla.
 *
 * <p>Los eventos caen entre `startDTD` y `endDTD` del {@link LexicalHandler}, cuando hay uno
 * instalado. Un parser **no validante** puede saltearse el subconjunto externo entero, y entonces
 * de aca no sale nada aunque la DTD exista: no es un incumplimiento, es que leer la DTD externa es
 * opcional. La forma de saberlo es la feature `http://xml.org/sax/features/external-parameter-entities`.
 *
 * <p>Sobre lo que **no** esta: las entidades sin analizar y las notaciones siguen yendo por
 * `org.xml.sax.DTDHandler`. Estan repartidas asi desde SAX1 y duplicarlas aca seria inventar
 * miembros que el contrato no tiene.
 *
 * <p><strong>En KajiLibrary nadie produce estos eventos todavia</strong>: el arbol no trae un parser
 * XML, asi que la interfaz esta completa pero sin emisor propio.
 */
public interface DeclHandler {

    /**
     * El modelo de contenido llega **como texto**, ya normalizado a la forma de la norma:
     * `EMPTY`, `ANY`, o una expresion con parentesis como `(#PCDATA|a|b)*`. No viene analizado, y
     * eso es a proposito: analizarlo es trabajo de quien lo necesite, y devolverlo crudo no pierde
     * informacion.
     */
    void elementDecl(String name, String model) throws SAXException;

    /**
     * `type` es el tipo declarado --`CDATA`, `ID`, una lista `NOTATION (a|b)` o una enumeracion
     * `(a|b)`--. `valueDefault` es `#IMPLIED`, `#REQUIRED`, `#FIXED` o `null` cuando hay un valor
     * por omision comun; `value` es ese valor, o `null` si no hay. Los dos ultimos se leen juntos:
     * `#FIXED` con `value` es un valor fijo, `null` con `value` es un valor por omision comun.
     */
    void attributeDecl(String eName, String aName, String type,
                       String valueDefault, String value) throws SAXException;

    /**
     * El valor viene con las referencias a caracter y a entidad de parametro ya expandidas, pero
     * **sin** expandir las referencias a entidad general: expandirlas aca daria el texto final en
     * vez de la declaracion, que es justo lo que se esta reportando.
     */
    void internalEntityDecl(String name, String value) throws SAXException;

    /** Las entidades de parametro llegan con `%` adelante, igual que en `startEntity`. */
    void externalEntityDecl(String name, String publicId, String systemId) throws SAXException;
}
