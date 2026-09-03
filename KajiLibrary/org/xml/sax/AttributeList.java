package org.xml.sax;

// KajiLibrary's org.xml.sax.AttributeList -- la lista de atributos de SAX1, reemplazada por
// Attributes.
//
// Se conserva porque las formas de SAX1 (DocumentHandler, Parser) todavia la nombran, y porque
// el puente helpers.ParserAdapter existe justamente para convertir una de estas en un
// Attributes. La diferencia con Attributes es que aca no hay espacios de nombres en absoluto: un
// atributo tiene un solo nombre, el que estaba escrito en el documento, con prefijo y todo.
//
// Vale la misma regla de vigencia que en Attributes: solo sirve dentro de la llamada a
// startElement; para conservarla hay que copiarla con helpers.AttributeListImpl.
//
// Esta deprecada en el JDK. Aca no lleva @Deprecated: la anotacion es metadato y no un miembro,
// asi que no entra en el contrato contra el que se mide esta biblioteca, y dejarla afuera
// mantiene el archivo compilando en el javac congelado sin depender de como se retienen las
// anotaciones.
public interface AttributeList {

    // La cantidad de atributos de la lista.
    int getLength();

    // El nombre del atributo en `index`, o null si el indice esta fuera de rango.
    String getName(int i);

    // El tipo del atributo en `index` ("CDATA" y demas), o null si esta fuera de rango.
    String getType(int i);

    // El valor del atributo en `index`, o null si esta fuera de rango.
    String getValue(int i);

    // El tipo del atributo con ese nombre, o null si no existe tal atributo.
    String getType(String name);

    // El valor del atributo con ese nombre, o null si no existe tal atributo.
    String getValue(String name);
}
