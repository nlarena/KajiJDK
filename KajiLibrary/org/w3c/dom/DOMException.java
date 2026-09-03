package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMException -- la unica excepcion del nucleo del DOM.
 *
 * <p>Es la rareza del paquete: todo lo demas en {@code org.w3c.dom} son interfaces, y esto es una
 * clase concreta que extiende {@link RuntimeException}. Que sea **no chequeada** es deliberado y no
 * un descuido: si fuera chequeada, cada uno de los treinta y pico de metodos de {@link Node} y de
 * {@link Document} obligaria a un {@code try} en el llamador, y recorrer un arbol --que casi nunca
 * falla-- seria intolerable de escribir.
 *
 * <p>El diseño viene de OMG IDL, donde no hay jerarquias de excepciones portables entre lenguajes:
 * en vez de una subclase por error hay **una sola clase con un campo {@link #code}** que dice cual
 * de los diecisiete errores ocurrio. Por eso el {@code catch} util es siempre un {@code switch}
 * sobre el codigo, y no un catch por tipo.
 *
 * <p>Y por eso mismo hay que tener cuidado: {@link #code} es un {@code short} **publico y mutable**,
 * asi lo declara la spec y asi se copia aca. No es un getter, no es final. Es API, no un desliz que
 * convenga arreglar.
 *
 * <p>Los diecisiete valores son numeros fijos de la especificacion --DOM Level 1 aporto del 1 al 8,
 * Level 2 del 9 al 15 y Level 3 el 16 y el 17-- y se copiaron de ahi. Equivocar uno no rompe
 * ninguna compilacion: el error viaja silencioso hasta el {@code switch} de otro, que entra por la
 * rama equivocada. La prueba {@code java/W3cDomCodigosTest.java} los verifica uno por uno **por
 * reflexion** contra los valores de la spec escritos a mano, justamente para que la copia no dependa
 * de que alguien la lea con atencion.
 */
public class DOMException extends RuntimeException {

    static final long serialVersionUID = 6627732366795969916L;

    /**
     * Cual de los errores fue, uno de los {@code *_ERR} de abajo.
     *
     * <p>Publico y modificable porque asi esta especificado.
     */
    public short code;

    /**
     * @param code uno de los {@code *_ERR}
     * @param message la descripcion, que va a {@link Throwable#getMessage}
     */
    public DOMException(short code, String message) {
        super(message);
        this.code = code;
    }

    /** El indice o el tamaño es negativo, o pasa el maximo. */
    public static final short INDEX_SIZE_ERR = 1;

    /** El texto no entra en un {@code DOMString}. */
    public static final short DOMSTRING_SIZE_ERR = 2;

    /** Se quiso insertar un nodo donde no va. */
    public static final short HIERARCHY_REQUEST_ERR = 3;

    /** El nodo se usa en un documento distinto del que lo creo. */
    public static final short WRONG_DOCUMENT_ERR = 4;

    /** Un caracter invalido, tipicamente en un nombre mal formado. */
    public static final short INVALID_CHARACTER_ERR = 5;

    /** Se quiso poner datos en un nodo que no los admite. */
    public static final short NO_DATA_ALLOWED_ERR = 6;

    /** Se quiso modificar un nodo de solo lectura. */
    public static final short NO_MODIFICATION_ALLOWED_ERR = 7;

    /** El nodo no esta en el contexto donde se lo busco. */
    public static final short NOT_FOUND_ERR = 8;

    /** La implementacion no soporta esa operacion o ese tipo de objeto. */
    public static final short NOT_SUPPORTED_ERR = 9;

    /** El atributo ya esta en uso en otro elemento. */
    public static final short INUSE_ATTRIBUTE_ERR = 10;

    /** Se uso un objeto que no esta --o ya no esta-- en estado utilizable. */
    public static final short INVALID_STATE_ERR = 11;

    /** Se especifico una cadena invalida, por ejemplo una expresion mal formada. */
    public static final short SYNTAX_ERR = 12;

    /** Se quiso cambiar el tipo de un objeto que no lo permite. */
    public static final short INVALID_MODIFICATION_ERR = 13;

    /** Un error con los espacios de nombres: nombre y URI que no pueden ir juntos. */
    public static final short NAMESPACE_ERR = 14;

    /** El objeto no soporta el parametro o la operacion pedida. */
    public static final short INVALID_ACCESS_ERR = 15;

    /** La operacion dejaria al documento invalido respecto de su gramatica. */
    public static final short VALIDATION_ERR = 16;

    /** El tipo del valor no es el que el parametro esperaba. */
    public static final short TYPE_MISMATCH_ERR = 17;
}
