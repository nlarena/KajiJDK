package javax.swing;

/**
 * Decide si un componente puede soltar el foco.
 *
 * <h2>Valida al salir, no al escribir</h2>
 *
 * <p>Se lo consulta cuando el foco se va del componente, no en cada tecla. Es lo que permite escribir
 * un numero de a un digito -- que pasa por estados invalidos -- sin que nada se queje hasta que uno
 * termina.
 *
 * <p>Un verificador que devuelve falso <strong>encierra el foco</strong>: no se puede pasar al
 * siguiente campo hasta corregir. Es la parte que hay que tener presente, porque un verificador con
 * un error deja al usuario sin poder hacer nada, ni siquiera cerrar la ventana.
 *
 * <h2>Verificar y ceder son dos preguntas</h2>
 *
 * <p>{@link #verify} responde "esto es valido". {@link #shouldYieldFocus} responde "dejalo salir", y
 * por omision es lo mismo, pero se la sobreescribe para que ademas corrija el valor, pite, o marque
 * el campo en rojo. La distincion importa porque {@code verify} tiene que ser <em>sin efectos</em>:
 * se la llama tambien desde afuera, para preguntar sin querer que pase nada.
 */
public abstract class InputVerifier {

    /** Para las subclases. */
    protected InputVerifier() {
    }

    /** Si lo que hay en el componente es valido; sin efectos. Ver la nota de la clase. */
    public abstract boolean verify(JComponent input);

    /**
     * Si el componente puede soltar el foco.
     *
     * @deprecated Usar {@link #shouldYieldFocus(JComponent, JComponent)}, que ademas sabe a donde
     *     va el foco.
     */
    @Deprecated
    public boolean shouldYieldFocus(JComponent input) {
        return verify(input);
    }

    /**
     * Si vale la pena verificar a ese componente.
     *
     * <p>Cierto por omision. Devolver falso saltea la verificacion entera, y es para el caso en que
     * el componente esta apagado o vacio a proposito.
     */
    public boolean verifyTarget(JComponent input) {
        return true;
    }

    /**
     * Si el foco puede pasar de un componente al otro.
     *
     * <p>Recibe los dos: a veces la respuesta depende de a donde va. Un boton de cancelar tiene que
     * poder recibir el foco aunque el campo este mal.
     */
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        return shouldYieldFocus(source);
    }
}
