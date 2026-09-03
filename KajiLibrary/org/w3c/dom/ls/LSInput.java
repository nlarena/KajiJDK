package org.w3c.dom.ls;

import java.io.InputStream;
import java.io.Reader;

/**
 * KajiLibrary's org.w3c.dom.ls.LSInput -- de donde leer un documento.
 *
 * <p>Cuatro formas de decir lo mismo --caracteres, bytes, una cadena, un URI-- y un orden de
 * preferencia que <b>si</b> importa: se mira primero {@link #getCharacterStream}, despues
 * {@link #getByteStream}, despues {@link #getStringData} y por ultimo {@link #getSystemId}. Se usa
 * la primera que no sea null y las demas se ignoran.
 *
 * <p>El orden no es arbitrario: va de lo mas resuelto a lo menos. Un flujo de caracteres ya tiene la
 * codificacion decidida por quien lo abrio; un flujo de bytes todavia hay que decodificarlo; un URI
 * ni siquiera se leyo. Quien implementa un {@link LSResourceResolver} suele devolver lo de mas
 * arriba que tenga a mano, y por eso conviene tener el orden presente: poner un flujo de caracteres
 * y ademas un {@code systemId} no ofrece dos opciones, silencia la segunda.
 *
 * <p>Es una interfaz con setters, que en Java no es lo comun. Viene de que la especificacion es del
 * W3C y esta escrita en IDL, donde estos son <b>atributos</b> de lectura y escritura.
 */
public interface LSInput {

    /** El flujo de caracteres, o null. Es el que gana si esta. */
    Reader getCharacterStream();

    /** Ver {@link #getCharacterStream}. */
    void setCharacterStream(Reader characterStream);

    /** El flujo de bytes, o null. Se decodifica con {@link #getEncoding}. */
    InputStream getByteStream();

    /** Ver {@link #getByteStream}. */
    void setByteStream(InputStream byteStream);

    /** El documento entero como cadena, o null. */
    String getStringData();

    /** Ver {@link #getStringData}. */
    void setStringData(String stringData);

    /**
     * De donde salio, para poder resolver lo relativo.
     *
     * <p>Sirve para dos cosas distintas: como <b>ultimo</b> recurso para leer, y como base de las
     * referencias relativas del documento aunque el contenido haya venido por otra via.
     */
    String getSystemId();

    /** Ver {@link #getSystemId}. */
    void setSystemId(String systemId);

    /** El identificador publico, si lo tiene. */
    String getPublicId();

    /** Ver {@link #getPublicId}. */
    void setPublicId(String publicId);

    /** La base contra la que se resuelve un {@link #getSystemId} relativo. */
    String getBaseURI();

    /** Ver {@link #getBaseURI}. */
    void setBaseURI(String baseURI);

    /** La codificacion del flujo de bytes; se ignora con flujo de caracteres o cadena. */
    String getEncoding();

    /** Ver {@link #getEncoding}. */
    void setEncoding(String encoding);

    /**
     * Si quien lo entrega certifica que ya esta bien formado.
     *
     * <p>Con esto en true el analizador puede saltear chequeos de codificacion. Es una promesa de
     * quien provee, no una comprobacion: si es mentira, el resultado no esta definido.
     */
    boolean getCertifiedText();

    /** Ver {@link #getCertifiedText}. */
    void setCertifiedText(boolean certifiedText);
}
