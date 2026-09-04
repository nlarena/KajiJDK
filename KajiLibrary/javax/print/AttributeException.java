package javax.print;

import javax.print.attribute.Attribute;

/**
 * KajiLibrary's javax.print.AttributeException -- el fallo fue por atributos.
 *
 * <p>Una interfaz, no una excepcion; ver la nota de {@link PrintException} sobre por que.
 *
 * <p>Distingue dos cosas que se confunden:
 *
 * <ul>
 *   <li>{@link #getUnsupportedAttributes} son <b>categorias</b> enteras que la impresora no entiende
 *       --no sabe que es el duplex--;
 *   <li>{@link #getUnsupportedValues} son atributos que si entiende con un valor que no puede dar
 *       --entiende el duplex, no lo tiene--.
 * </ul>
 *
 * <p>Los dos pueden devolver null si no hay nada de esa clase.
 */
public interface AttributeException {

    /** Las categorias que no entiende, o null. */
    Class<?>[] getUnsupportedAttributes();

    /** Los valores que no puede dar, o null. */
    Attribute[] getUnsupportedValues();
}
