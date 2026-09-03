package org.w3c.dom.ls;

/**
 * KajiLibrary's org.w3c.dom.ls.LSResourceResolver -- quien decide de donde sale cada recurso.
 *
 * <p>Un metodo, y es de las piezas mas utiles de todo el XML de la plataforma. Un documento que
 * declara una DTD o importa un esquema nombra un URI, y por omision el analizador va a buscarlo.
 * Poniendo un resolvedor, esos nombres se atienden localmente.
 *
 * <p>Hay dos motivos y los dos pesan. El primero es que sin esto un XML puede hacer que el programa
 * haga pedidos de red que nadie pidio, o lea archivos locales --el ataque XXE-- solo por nombrarlos.
 * El segundo es mas prosaico: un esquema que se resuelve por red hace que el programa deje de andar
 * el dia que ese servidor se cae, que es lo que pasa cada tanto con los DTD del W3C.
 *
 * <p>Devolver null significa "resolvelo vos como siempre", asi que un resolvedor que solo quiere
 * atajar unos pocos nombres es corto de escribir. Uno que devuelve un {@link LSInput} vacio para
 * todo lo desconocido es la forma de <b>prohibir</b> lo externo.
 */
public interface LSResourceResolver {

    /**
     * De donde sale ese recurso.
     *
     * @param type el tipo, por ejemplo el espacio de nombres de XML Schema
     * @param namespaceURI el espacio de nombres del recurso, o null
     * @param publicId el identificador publico, o null
     * @param systemId el identificador de sistema, tal como lo escribio el documento
     * @param baseURI contra el que se resuelve un {@code systemId} relativo
     * @return de donde leerlo, o null para dejar que lo resuelva el analizador
     */
    LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
                            String baseURI);
}
