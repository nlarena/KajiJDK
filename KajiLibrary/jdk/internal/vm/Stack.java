package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.Stack — los cuadros de la pila de llamadas.
 *
 * <p>Es la costura que le faltaba a todo lo que necesita saber **quién llamó**:
 * {@link SecurityManager#getClassContext()}, `StackWalker`, y las trazas de `Throwable`. Hasta que
 * existió, ninguno de esos tenía de dónde sacar la respuesta, y por eso estaban afuera o vacíos.
 *
 * <p>No es un `native` del puente sino un **intrínseco del intérprete**, y la razón es estructural:
 * el puente de nativos recibe el metaspace y el montón, y **no la pila de frames**. El único lugar
 * donde los cuadros están a la vista es adentro del intérprete.
 *
 * <p>Cada entrada es `"clase|método"`, con la clase en forma binaria (`java/lang/String`). El orden
 * es de arriba hacia abajo: el primero es quien llamó a {@link #frames()}.
 *
 * <p><strong>No se recorta nada.</strong> Se devuelve la pila tal como está, incluido el cuadro del
 * que llamó, y quien la use decide cuántos niveles suyos descartar. Recortar acá obligaría a adivinar
 * cuántos cuadros de envoltorio puso el llamador, y ese número la VM no lo sabe.
 */
public final class Stack {

    private Stack() {
    }

    /** Los cuadros de la pila, de arriba hacia abajo, como `"clase|método"`. */
    public static native String[] frames();
}
