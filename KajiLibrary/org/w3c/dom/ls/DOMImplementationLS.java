package org.w3c.dom.ls;

import org.w3c.dom.DOMException;

/**
 * KajiLibrary's org.w3c.dom.ls.DOMImplementationLS -- la fabrica de todo este paquete.
 *
 * <p>Se obtiene preguntandole a un {@code DOMImplementation} por la caracteristica {@code "LS"}, y
 * de ahi salen las cuatro piezas: analizador, serializador, entrada y salida.
 *
 * <p>Las dos ultimas --{@link #createLSInput} y {@link #createLSOutput}-- son fabricas de objetos que
 * <b>no tienen constructor publico</b>. Es lo que hace usable a {@link LSResourceResolver}: quien
 * escribe uno necesita devolver un {@code LSInput}, y sin esto tendria que implementar la interfaz a
 * mano cada vez.
 *
 * <p>El modo asincronico se elige al crear el analizador y no despues, porque cambia como esta
 * construido por dentro y no solo como se lo llama.
 */
public interface DOMImplementationLS {

    /** El analizador bloquea hasta terminar. */
    short MODE_SYNCHRONOUS = 1;

    /** El analizador vuelve enseguida y avisa por eventos. */
    short MODE_ASYNCHRONOUS = 2;

    /**
     * Un analizador.
     *
     * @param mode uno de los dos de arriba
     * @param schemaType el espacio de nombres del lenguaje de esquema con el que validar, o null
     * @throws DOMException si esta implementacion no soporta ese modo o ese tipo de esquema
     */
    LSParser createLSParser(short mode, String schemaType) throws DOMException;

    /** Un serializador. */
    LSSerializer createLSSerializer();

    /** Una entrada vacia, para llenarla. Ver la nota de la clase. */
    LSInput createLSInput();

    /** Una salida vacia, para llenarla. */
    LSOutput createLSOutput();
}
