package javax.xml.crypto;

import java.security.Key;

/**
 * KajiLibrary's javax.xml.crypto.KeySelectorResult -- la clave que eligio un {@link KeySelector}.
 *
 * <p>Un envoltorio de una sola clave, y parece de mas hasta que se ve para que esta: quien valida una
 * firma necesita saber <b>con que clave</b> se valido, no solo si valido. Sin esto, el resultado
 * seria un booleano y la aplicacion no podria decidir si esa clave era una en la que confia.
 *
 * <p>Esa es la trampa central de XML-DSig y vale decirla: una firma que valida solo demuestra que
 * quien tiene <b>esa</b> clave la produjo. Si la clave salio del propio documento --de su
 * {@code KeyInfo}-- eso no demuestra nada, porque quien escribio el documento eligio la clave.
 * Comparar la clave de aca contra una lista de confianza es el paso que falta, y el que se olvida.
 */
public interface KeySelectorResult {

    /** La clave elegida. Ver la nota de la clase sobre por que hay que mirarla. */
    Key getKey();
}
