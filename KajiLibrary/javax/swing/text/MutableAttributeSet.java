package javax.swing.text;

import java.util.Enumeration;

/**
 * Un conjunto de atributos que se puede cambiar.
 *
 * <p>{@link AttributeSet} es de solo lectura a proposito: los conjuntos que un documento comparte
 * entre miles de caracteres tienen que ser inmutables para poder compartirse. Esta interfaz es la
 * otra mitad, la que usa quien esta armando o editando un conjunto.
 *
 * <p>El <em>padre de resolucion</em> es lo que hace que los estilos se encadenen: un atributo que
 * este conjunto no define se le pregunta al padre, y asi hasta el estilo por omision del documento.
 */
public interface MutableAttributeSet extends AttributeSet {

    void addAttribute(Object name, Object value);

    void addAttributes(AttributeSet attributes);

    void removeAttribute(Object name);

    /** Quita esos nombres; el valor que tuvieran no importa. */
    void removeAttributes(Enumeration<?> names);

    /**
     * Quita los que este conjunto tenga con el mismo valor que el otro.
     *
     * <p>Con el mismo valor, no solo el mismo nombre: quitar "negrita = false" de un conjunto donde
     * la negrita esta en {@code true} no hace nada.
     */
    void removeAttributes(AttributeSet attributes);

    /** Ver la nota de la interfaz. */
    void setResolveParent(AttributeSet parent);
}
