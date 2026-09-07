package javax.swing.text;

/**
 * Quien sabe donde cae la proxima tabulacion.
 *
 * <p>Una sola pregunta, y con dos argumentos que parecen de mas: la posicion en pixeles y el
 * <em>desplazamiento en el modelo</em> del caracter de tabulacion. El segundo hace falta porque las
 * paradas pueden ser distintas segun el parrafo, y el parrafo se sabe por el desplazamiento.
 */
public interface TabExpander {

    /**
     * Donde termina la tabulacion que empieza en {@code x}.
     *
     * @param x la posicion actual, en pixeles
     * @param tabOffset el desplazamiento del caracter de tabulacion en el documento
     */
    float nextTabStop(float x, int tabOffset);
}
