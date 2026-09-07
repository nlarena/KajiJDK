package javax.swing.text.html.parser;

/**
 * Los numeros con los que la DTD nombra tipos y modificadores.
 *
 * <h2>Tres familias de numeros en una sola lista</h2>
 *
 * <p>Los valores se repiten a proposito: {@code CDATA} y {@code FIXED} valen los dos 1, y
 * {@code ENTITY} y {@code REQUIRED} valen los dos 2. No es un descuido: son familias distintas que
 * nunca se comparan entre si. Los primeros diecinueve son <em>tipos</em> de atributo o de
 * contenido; {@code FIXED} a {@code IMPLIED} son <em>modificadores</em> de un atributo; y de
 * {@code PUBLIC} a {@code SYSTEM} son tipos de <em>entidad</em>.
 *
 * <p>Los tres ultimos son banderas de bit, no numeros de una lista: se combinan con un
 * {@code |} con el tipo de entidad para decir si es general o de parametro.
 *
 * <p>Es una interfaz sin metodos y las clases del paquete la implementan para nombrar las
 * constantes sin calificarlas. Es una forma que hoy no se usa, pero cambiarla cambiaria la firma
 * publica de {@link Element}, {@link Entity} y {@link AttributeList}.
 */
public interface DTDConstants {

    int CDATA = 1;
    int ENTITY = 2;
    int ENTITIES = 3;
    int ID = 4;
    int IDREF = 5;
    int IDREFS = 6;
    int NAME = 7;
    int NAMES = 8;
    int NMTOKEN = 9;
    int NMTOKENS = 10;
    int NOTATION = 11;
    int NUMBER = 12;
    int NUMBERS = 13;
    int NUTOKEN = 14;
    int NUTOKENS = 15;

    int RCDATA = 16;
    int EMPTY = 17;
    int MODEL = 18;
    int ANY = 19;

    int FIXED = 1;
    int REQUIRED = 2;
    int CURRENT = 3;
    int CONREF = 4;
    int IMPLIED = 5;

    int PUBLIC = 10;
    int SDATA = 11;
    int PI = 12;
    int STARTTAG = 13;
    int ENDTAG = 14;
    int MS = 15;
    int MD = 16;
    int SYSTEM = 17;

    int GENERAL = 1 << 16;
    int DEFAULT = 1 << 17;
    int PARAMETER = 1 << 18;
}
