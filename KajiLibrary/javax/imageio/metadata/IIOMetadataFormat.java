package javax.imageio.metadata;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataFormat -- el esquema de un arbol de metadatos.
 *
 * <p>Un DTD o un esquema XML, expresado como llamadas a metodos en lugar de como documento. Dice que
 * elementos existen, que hijos puede tener cada uno, y que atributos con que tipos y que rangos.
 *
 * <p>Existe porque un arbol de metadatos <b>se puede editar</b>: un programa lo lee, lo modifica y lo
 * vuelve a escribir. Sin un esquema, la unica forma de saber si lo que quedo es valido seria intentar
 * escribir el archivo y ver si el codificador se queja.
 *
 * <h2>Las seis politicas de hijos</h2>
 *
 * <p>Es la parte con mas contenido, y las tres del medio se confunden:
 *
 * <ul>
 *   <li>{@link #CHILD_POLICY_EMPTY}: sin hijos;
 *   <li>{@link #CHILD_POLICY_ALL}: <b>todos</b> los hijos declarados, en ese orden, obligatorios;
 *   <li>{@link #CHILD_POLICY_SOME}: <b>algunos</b> de los declarados, en ese orden. Es
 *       {@code ALL} pero opcionales;
 *   <li>{@link #CHILD_POLICY_CHOICE}: <b>uno</b> de los declarados;
 *   <li>{@link #CHILD_POLICY_SEQUENCE}: cualquier cantidad, en cualquier orden, de los declarados;
 *   <li>{@link #CHILD_POLICY_REPEAT}: cualquier cantidad de <b>un solo</b> tipo de hijo, con minimo y
 *       maximo. Es la unica donde {@link #getElementMinChildren} significa algo.
 * </ul>
 *
 * <h2>Los tipos de valor son una mascara</h2>
 *
 * <p>{@link #VALUE_RANGE_MIN_INCLUSIVE} vale 6, que es {@code VALUE_RANGE | 4}. Las constantes de
 * rango se arman combinando {@link #VALUE_RANGE} con los dos bits de inclusividad, y por eso hay que
 * compararlas con mascaras y no con igualdad.
 *
 * <p>{@link #VALUE_LIST} es aparte: significa que el atributo es una lista de valores separados por
 * espacios, y ahi valen {@link #getAttributeListMinLength} y su par.
 *
 * <h2>{@link #canNodeAppear} depende del tipo de imagen</h2>
 *
 * <p>Es lo que hace este esquema mas expresivo que un DTD. Un nodo de paleta solo tiene sentido en una
 * imagen indexada, y este metodo lo puede decir mirando el {@link ImageTypeSpecifier} concreto.
 */
public interface IIOMetadataFormat {

    /** Sin hijos. */
    int CHILD_POLICY_EMPTY = 0;

    /** Todos los declarados, en orden. Ver la nota de la clase. */
    int CHILD_POLICY_ALL = 1;

    /** Algunos de los declarados, en orden. */
    int CHILD_POLICY_SOME = 2;

    /** Uno de los declarados. */
    int CHILD_POLICY_CHOICE = 3;

    /** Cualquier cantidad y orden de los declarados. */
    int CHILD_POLICY_SEQUENCE = 4;

    /** Cualquier cantidad de un solo tipo. */
    int CHILD_POLICY_REPEAT = 5;

    /** La ultima politica; sirve para validar un valor. */
    int CHILD_POLICY_MAX = 5;

    /** El atributo no lleva valor. */
    int VALUE_NONE = 0;

    /** Cualquier valor del tipo declarado. */
    int VALUE_ARBITRARY = 1;

    /** Un valor entre un minimo y un maximo. Ver la nota de la clase. */
    int VALUE_RANGE = 2;

    /** El bit que dice que el minimo esta incluido. */
    int VALUE_RANGE_MIN_INCLUSIVE_MASK = 4;

    /** El que dice que el maximo esta incluido. */
    int VALUE_RANGE_MAX_INCLUSIVE_MASK = 8;

    /** Rango con el minimo incluido. */
    int VALUE_RANGE_MIN_INCLUSIVE = VALUE_RANGE | VALUE_RANGE_MIN_INCLUSIVE_MASK;

    /** Rango con el maximo incluido. */
    int VALUE_RANGE_MAX_INCLUSIVE = VALUE_RANGE | VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /** Rango cerrado por los dos lados. */
    int VALUE_RANGE_MIN_MAX_INCLUSIVE =
        VALUE_RANGE | VALUE_RANGE_MIN_INCLUSIVE_MASK | VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /** Uno de una lista cerrada. */
    int VALUE_ENUMERATION = 16;

    /** Una lista de valores separados por espacios. Ver la nota de la clase. */
    int VALUE_LIST = 32;

    /** El valor es texto. */
    int DATATYPE_STRING = 0;

    /** Es {@code true} o {@code false}. */
    int DATATYPE_BOOLEAN = 1;

    /** Es un entero. */
    int DATATYPE_INTEGER = 2;

    /** Es coma flotante de cuatro bytes. */
    int DATATYPE_FLOAT = 3;

    /** De ocho. */
    int DATATYPE_DOUBLE = 4;

    /** Como se llama la raiz del arbol. */
    String getRootName();

    /**
     * Si ese elemento puede aparecer en el arbol de una imagen de ese tipo.
     *
     * <p>Ver la nota de la clase: es lo que un DTD no puede expresar.
     */
    boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    /**
     * Cuantos hijos como minimo.
     *
     * <p>Solo significa algo con {@link #CHILD_POLICY_REPEAT}.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    int getElementMinChildren(String elementName);

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    int getElementMaxChildren(String elementName);

    /**
     * Que es ese elemento, en palabras.
     *
     * @param locale en que idioma, o null para el del sistema
     */
    String getElementDescription(String elementName, Locale locale);

    /**
     * Cual de las seis politicas de hijos. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    int getChildPolicy(String elementName);

    /**
     * Que hijos puede tener.
     *
     * @return null si la politica es {@link #CHILD_POLICY_EMPTY}
     * @throws IllegalArgumentException si el elemento no existe
     */
    String[] getChildNames(String elementName);

    /**
     * Que atributos puede tener.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    String[] getAttributeNames(String elementName);

    /**
     * Que forma tiene el valor de ese atributo. Ver la nota de la clase sobre las mascaras.
     *
     * @throws IllegalArgumentException si el elemento o el atributo no existen
     */
    int getAttributeValueType(String elementName, String attrName);

    /**
     * De que tipo es.
     *
     * @throws IllegalArgumentException si el elemento o el atributo no existen
     */
    int getAttributeDataType(String elementName, String attrName);

    /**
     * Si tiene que estar.
     *
     * @throws IllegalArgumentException si el elemento o el atributo no existen
     */
    boolean isAttributeRequired(String elementName, String attrName);

    /**
     * Que vale si no se pone, o null si no hay omision.
     *
     * @throws IllegalArgumentException si el elemento o el atributo no existen
     */
    String getAttributeDefaultValue(String elementName, String attrName);

    /**
     * Los valores permitidos.
     *
     * @throws IllegalArgumentException si el atributo no es de tipo enumeracion
     */
    String[] getAttributeEnumerations(String elementName, String attrName);

    /**
     * El minimo del rango, o null si no hay minimo.
     *
     * @throws IllegalArgumentException si el atributo no es de tipo rango
     */
    String getAttributeMinValue(String elementName, String attrName);

    /**
     * El maximo, o null.
     *
     * @throws IllegalArgumentException si el atributo no es de tipo rango
     */
    String getAttributeMaxValue(String elementName, String attrName);

    /**
     * Cuantos valores como minimo en la lista.
     *
     * @throws IllegalArgumentException si el atributo no es de tipo lista
     */
    int getAttributeListMinLength(String elementName, String attrName);

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si el atributo no es de tipo lista
     */
    int getAttributeListMaxLength(String elementName, String attrName);

    /** Que es ese atributo, en palabras. */
    String getAttributeDescription(String elementName, String attrName, Locale locale);

    /**
     * Que forma tiene el objeto de usuario de ese elemento.
     *
     * <p>Los elementos que llevan un dato que no es texto --ver
     * {@link IIOMetadataNode#getUserObject}-- lo declaran aca.
     *
     * @return {@link #VALUE_NONE} si ese elemento no lleva objeto
     * @throws IllegalArgumentException si el elemento no existe
     */
    int getObjectValueType(String elementName);

    /**
     * De que clase es ese objeto.
     *
     * @throws IllegalArgumentException si el elemento no lleva objeto
     */
    Class<?> getObjectClass(String elementName);

    /**
     * Que objeto va si no se pone ninguno, o null.
     *
     * @throws IllegalArgumentException si el elemento no lleva objeto
     */
    Object getObjectDefaultValue(String elementName);

    /**
     * Los objetos permitidos.
     *
     * @throws IllegalArgumentException si el objeto no es de tipo enumeracion
     */
    Object[] getObjectEnumerations(String elementName);

    /**
     * El minimo, si el objeto es un rango.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    Comparable<?> getObjectMinValue(String elementName);

    /**
     * El maximo.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    Comparable<?> getObjectMaxValue(String elementName);

    /**
     * Cuantos elementos como minimo, si el objeto es un arreglo.
     *
     * @throws IllegalArgumentException si no es un arreglo
     */
    int getObjectArrayMinLength(String elementName);

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si no es un arreglo
     */
    int getObjectArrayMaxLength(String elementName);
}
