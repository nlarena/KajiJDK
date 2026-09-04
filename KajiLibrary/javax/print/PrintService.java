package javax.print;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

/**
 * KajiLibrary's javax.print.PrintService -- una impresora.
 *
 * <p>Sirve para dos cosas: crear trabajos, y sobre todo <b>preguntarle que sabe hacer</b> antes de
 * mandarle nada. La mitad de la interfaz es eso.
 *
 * <h2>Los cuatro niveles de pregunta</h2>
 *
 * <p>Se ven parecidos y no lo son, y elegir mal es el error tipico:
 *
 * <ul>
 *   <li>{@link #isAttributeCategorySupported} pregunta por la <b>categoria</b>: si la impresora
 *       entiende el concepto de duplex;
 *   <li>{@link #getSupportedAttributeValues} devuelve <b>que valores</b> puede tomar esa categoria,
 *       para un formato y un contexto dados;
 *   <li>{@link #isAttributeValueSupported} pregunta por un valor concreto;
 *   <li>{@link #getDefaultAttributeValue} devuelve el que se usa si no se pide nada.
 * </ul>
 *
 * <p>Que las dos ultimas tomen el {@link DocFlavor} y un {@link AttributeSet} es lo que las hace
 * utiles: una impresora puede hacer duplex en PostScript y no en texto plano, o no poder combinar
 * duplex con cierto tamano de papel. El conjunto que se pasa es el resto de lo que se piensa pedir.
 *
 * <h2>{@code getSupportedAttributeValues} devuelve {@code Object}</h2>
 *
 * <p>Es incomodo y no hay alternativa: segun la categoria devuelve un arreglo de valores, un valor
 * suelto que representa un rango, o null. La documentacion de cada atributo estandar dice cual.
 *
 * <h2>{@link #equals} y {@link #hashCode} estan declarados</h2>
 *
 * <p>Redeclarar los de {@code Object} en una interfaz no cambia nada tecnicamente. Esta puesto para
 * documentar el contrato: dos objetos que representan <b>la misma impresora</b> tienen que ser
 * iguales, aunque sean instancias distintas obtenidas en busquedas distintas.
 */
public interface PrintService {

    /** El nombre, para mostrar. */
    String getName();

    /** Un trabajo nuevo. Ver {@link DocPrintJob}: sirve una sola vez. */
    DocPrintJob createPrintJob();

    /** Registra un escucha de cambios de la impresora. */
    void addPrintServiceAttributeListener(PrintServiceAttributeListener listener);

    /** Lo da de baja. */
    void removePrintServiceAttributeListener(PrintServiceAttributeListener listener);

    /** Los atributos actuales de la impresora. */
    PrintServiceAttributeSet getAttributes();

    /**
     * Uno solo, por categoria.
     *
     * @throws NullPointerException si la categoria es null
     * @throws IllegalArgumentException si no es un {@link PrintServiceAttribute}
     */
    <T extends PrintServiceAttribute> T getAttribute(Class<T> category);

    /** Los formatos que acepta. */
    DocFlavor[] getSupportedDocFlavors();

    /** Si acepta ese formato. */
    boolean isDocFlavorSupported(DocFlavor flavor);

    /** Las categorias de atributo que entiende. */
    Class<?>[] getSupportedAttributeCategories();

    /** Si entiende esa categoria. Ver la nota de la clase. */
    boolean isAttributeCategorySupported(Class<? extends Attribute> category);

    /** El valor que usa si no se pide nada, o null. */
    Object getDefaultAttributeValue(Class<? extends Attribute> category);

    /**
     * Que valores puede tomar esa categoria en ese contexto.
     *
     * <p>Ver la nota de la clase sobre por que devuelve {@code Object}.
     *
     * @param flavor el formato, o null para preguntar en general
     * @param attributes el resto de lo que se piensa pedir, o null
     */
    Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor,
                                       AttributeSet attributes);

    /** Si puede dar ese valor en ese contexto. */
    boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes);

    /**
     * Cuales de esos atributos no puede cumplir, o null si puede con todos.
     *
     * <p>Es la forma de preguntar por todo el pedido de una vez en lugar de atributo por atributo.
     */
    AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes);

    /** La fabrica de pantallas propias de esta impresora, o null. */
    ServiceUIFactory getServiceUIFactory();

    /** Igual si es la misma impresora. Ver la nota de la clase. */
    boolean equals(Object obj);

    /** Coherente con {@link #equals}. */
    int hashCode();
}
