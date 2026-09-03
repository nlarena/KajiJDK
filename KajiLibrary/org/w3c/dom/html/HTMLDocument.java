package org.w3c.dom.html;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Un documento HTML: la raiz, mas los atajos que HTML agrega sobre un `Document`.
 *
 * <p>Las colecciones que devuelve --`getImages`, `getLinks`, `getForms`, `getAnchors`,
 * `getApplets`-- son **vivas**: reflejan el documento en el momento en que se las consulta, no una
 * foto de cuando se las pidio. Agregar un `<img>` cambia lo que `getImages().getLength()` contesta
 * sin volver a llamarlo. Es la misma regla que en `NodeList` y es lo que hace que guardarse una
 * coleccion sea barato y a la vez peligroso si uno itera mientras modifica.
 *
 * <p>`open`, `close`, `write` y `writeln` son la interfaz del `document.write` de los navegadores:
 * escribir sobre un documento ya cargado lo **reemplaza** en vez de agregarle. Estan en la API
 * porque el DOM Nivel 1 de HTML las define; lo que hagan depende de la implementacion.
 */
public interface HTMLDocument extends Document {

    /** El atributo `title`. */
    String getTitle();

    /** Fija el atributo `title`. */
    void setTitle(String title);

    /** La URI de la que se llego a este documento, o la cadena vacia. */
    String getReferrer();

    /** El dominio del servidor que sirvio el documento, o la cadena vacia. */
    String getDomain();

    /** El atributo `uRL`. */
    String getURL();

    /** El `<body>`, o el `<frameset>` si el documento tiene marcos. */
    HTMLElement getBody();

    /** Fija el `<body>`, o el `<frameset>` si el documento tiene marcos. */
    void setBody(HTMLElement body);

    /** Los `<img>` del documento, en una coleccion viva. */
    HTMLCollection getImages();

    /** Los `<applet>` y los `<object>` que son applets, en una coleccion viva. */
    HTMLCollection getApplets();

    /** Los `<a>` y `<area>` con `href`, en una coleccion viva. */
    HTMLCollection getLinks();

    /** Los `<form>` del documento, en una coleccion viva. */
    HTMLCollection getForms();

    /** Los `<a>` con `name`, en una coleccion viva. */
    HTMLCollection getAnchors();

    /** Las cookies del documento, en el formato de la cabecera `Cookie`. */
    String getCookie();

    /** Fija las cookies del documento, en el formato de la cabecera `Cookie`. */
    void setCookie(String cookie);

    /** Abre el documento para escribir. Lo que hubiera se descarta. */
    void open();

    /** Cierra el flujo abierto por {@link #open}. */
    void close();

    /** Escribe ese texto en el documento abierto. */
    void write(String text);

    /** Como {@link #write}, mas un fin de linea. */
    void writeln(String text);

    /** El atributo `elementsByName`. */
    NodeList getElementsByName(String elementName);
}
