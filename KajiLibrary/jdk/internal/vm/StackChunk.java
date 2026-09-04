package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.StackChunk — un pedazo de pila guardado en el montón.
 *
 * <p>Es el objeto donde la VM copia los cuadros de una continuación cuando la suspende. Se encadenan
 * hacia atrás con {@link #parent()}, de modo que una pila profunda queda partida en varios.
 *
 * <p><strong>La VM es la única que los llena.</strong> No hay campos que Java pueda escribir: los
 * cuadros los copia el runtime con conocimiento del marco de pila, y por eso en el JDK todos los
 * accesos van por intrínsecos. Esta VM no tiene continuaciones --ver
 * {@link ContinuationSupport}--, así que ninguno de estos objetos se llena nunca.
 *
 * <p>De ahí que {@link #isEmpty()} devuelva `true` y {@link #parent()} `null`. **No es una
 * simulación: es la verdad sobre este objeto.** Un `StackChunk` recién construido está vacío también
 * en el JDK; lo que allá cambia es que después la VM lo llena, y acá no.
 */
public final class StackChunk {

    private final StackChunk padre;

    public StackChunk() {
        this.padre = null;
    }

    /**
     * Prepara la clase.
     *
     * <p>En el JDK deja los offsets de los campos donde el runtime los va a buscar. Acá no hay nada
     * que preparar; existe porque la VM la nombra en el arranque.
     */
    public static void init() {
    }

    /** El pedazo anterior de la cadena, o `null` si éste es el último. */
    public StackChunk parent() {
        return this.padre;
    }

    /** Si no tiene cuadros. */
    public boolean isEmpty() {
        return true;
    }
}
