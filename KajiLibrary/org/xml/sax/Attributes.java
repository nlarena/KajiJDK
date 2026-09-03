package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.Attributes -- los atributos de un elemento, tal como llegan a
 * `ContentHandler.startElement`.
 *
 * <p>Es una lista con indice **y** un mapa con dos claves distintas a la vez: se puede recorrer por
 * posicion (`getURI(i)`, `getValue(i)`) o buscar por nombre calificado (`getValue(String)`) o por
 * (URI, nombre local) (`getValue(String, String)`). Las tres vistas son sobre lo mismo y estan las
 * tres porque un parser puede estar configurado para reportar solo una de las dos formas de nombre;
 * cual funcione depende de las *features* `namespaces` y `namespace-prefixes` del lector.
 *
 * <p><strong>El objeto es prestado y vale solo dentro de la llamada.</strong> El parser lo reusa para
 * el elemento siguiente. Guardarlo es el bug clasico de SAX: el manejador se queda con una referencia
 * que despues describe otro elemento. Para conservarlo hay que copiarlo.
 *
 * <p><strong>El orden no significa nada.</strong> XML no le da orden a los atributos y un parser
 * puede entregarlos en cualquiera; codigo que dependa del indice para identificar cual es cual se
 * rompe al cambiar de implementacion.
 *
 * <p>Los tipos que devuelve `getType` son los de la DTD --`CDATA`, `ID`, `IDREF`, `IDREFS`,
 * `NMTOKEN`, `NMTOKENS`, `ENTITY`, `ENTITIES`, `NOTATION`-- y sin DTD son todos `CDATA`: es lo que
 * dice la norma para "no se sabe", no un tipo averiguado.
 */
public interface Attributes {

    int getLength();

    /** Cadena vacia si no tiene espacio de nombres o si el parser no los procesa; nunca `null`. */
    String getURI(int index);

    String getLocalName(int index);

    /** El nombre tal cual estaba escrito, con prefijo. Vacio si el parser no lo reporta. */
    String getQName(int index);

    String getType(int index);

    /** Las entidades ya expandidas y el blanco ya normalizado, como manda XML. */
    String getValue(int index);

    /** -1 si no esta. Los cuatro `getXxx(int)` devuelven `null` con un indice fuera de rango. */
    int getIndex(String uri, String localName);

    int getIndex(String qName);

    /** `null` si no hay atributo con ese nombre; **no** una excepcion. */
    String getType(String uri, String localName);

    String getType(String qName);

    String getValue(String uri, String localName);

    String getValue(String qName);
}
