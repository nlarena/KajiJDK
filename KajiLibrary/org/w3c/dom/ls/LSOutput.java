package org.w3c.dom.ls;

import java.io.OutputStream;
import java.io.Writer;

/**
 * KajiLibrary's org.w3c.dom.ls.LSOutput -- adonde escribir un documento.
 *
 * <p>El espejo de {@link LSInput}, con el mismo orden de preferencia:
 * {@link #getCharacterStream}, despues {@link #getByteStream}, y por ultimo {@link #getSystemId}. Se
 * usa el primero que no sea null.
 *
 * <p>Hay una asimetria con la entrada que vale notar: no existe {@code stringData}. Tiene sentido --
 * escribir a una cadena no es un destino sino un resultado-- y para eso esta
 * {@link LSSerializer#writeToString}.
 */
public interface LSOutput {

    /** El flujo de caracteres, o null. Es el que gana si esta. */
    Writer getCharacterStream();

    /** Ver {@link #getCharacterStream}. */
    void setCharacterStream(Writer characterStream);

    /** El flujo de bytes, o null. Se codifica con {@link #getEncoding}. */
    OutputStream getByteStream();

    /** Ver {@link #getByteStream}. */
    void setByteStream(OutputStream byteStream);

    /** El URI adonde escribir, si no se dio ningun flujo. */
    String getSystemId();

    /** Ver {@link #getSystemId}. */
    void setSystemId(String systemId);

    /**
     * La codificacion de salida.
     *
     * <p>Solo aplica al flujo de bytes: un flujo de caracteres ya la tiene fijada por quien lo
     * abrio, y ponerla aca no lo cambia.
     */
    String getEncoding();

    /** Ver {@link #getEncoding}. */
    void setEncoding(String encoding);
}
