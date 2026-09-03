package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Comment -- el contenido de un `&lt;!-- ... --&gt;`.
 *
 * <p>No agrega miembros sobre `CharacterData` por la misma razon que `CDATASection` no agrega nada
 * sobre `Text`: un comentario es una tira de caracteres y la unica diferencia esta en como se
 * escribe. El texto que guarda es el de **adentro** de los delimitadores.
 */
public interface Comment extends CharacterData {
}
