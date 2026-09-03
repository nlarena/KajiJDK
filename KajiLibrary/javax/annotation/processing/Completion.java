package javax.annotation.processing;

// Una sugerencia de completado que un procesador ofrece para el valor de un elemento de anotacion
// (JSR 269 §Completion). Son dos cadenas y nada mas: `getValue()` es el texto que la herramienta
// insertaria, `getMessage()` la explicacion que le muestra a la persona. No hay estado ni identidad
// definidos por el contrato, asi que la interfaz no promete equals/hashCode.
public interface Completion {

    /** El texto que se propone insertar. */
    String getValue();

    /** La explicacion informativa que acompana a {@link #getValue()}; puede ser vacia. */
    String getMessage();
}
