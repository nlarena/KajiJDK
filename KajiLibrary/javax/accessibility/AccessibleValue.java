package javax.accessibility;

/**
 * Lo implementa lo que representa un **número dentro de un rango**: una barra, un deslizador, una
 * barra de progreso.
 *
 * <p>Los tres métodos de consulta devuelven {@link Number} y no un tipo concreto porque el rango
 * puede ser entero o de coma flotante según el componente, y forzar uno de los dos obligaría a
 * redondear en la mitad de los casos.
 */
public interface AccessibleValue {

    /** El valor actual. */
    Number getCurrentAccessibleValue();

    /**
     * Cambia el valor.
     *
     * @return `true` si se pudo
     */
    boolean setCurrentAccessibleValue(Number n);

    /** El menor valor posible. */
    Number getMinimumAccessibleValue();

    /** El mayor valor posible. */
    Number getMaximumAccessibleValue();
}
