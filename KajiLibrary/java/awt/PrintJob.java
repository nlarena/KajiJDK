package java.awt;

/**
 * Un trabajo de impresión en curso.
 *
 * <p>El modelo es de 1.1 y se nota: se pide una página con {@link #getGraphics}, se dibuja en ella
 * como en cualquier otra superficie, y se la suelta con {@code dispose}. La página siguiente es otro
 * {@code getGraphics}. Al final, {@link #end}.
 *
 * <p>{@link #lastPageFirst} existe por una peculiaridad de las impresoras de la época: muchas
 * apilaban las hojas boca arriba, así que imprimir en orden dejaba el documento al revés. Preguntarlo
 * permitía dibujar las páginas en el orden que hiciera falta.
 *
 * <p>La API moderna es {@code java.awt.print}; ésta se conserva porque {@link Toolkit} la devuelve.
 */
public abstract class PrintJob {

    /** Para las subclases. */
    protected PrintJob() {
    }

    /**
     * Una página nueva para dibujar.
     *
     * @return el contexto de la página, o `null` si no hay más páginas
     */
    public abstract Graphics getGraphics();

    /** Cuánto mide una página, en píxeles de impresión. */
    public abstract Dimension getPageDimension();

    /** Cuántos puntos por pulgada tiene la página. */
    public abstract int getPageResolution();

    /** Si conviene dibujar la última página primero. */
    public abstract boolean lastPageFirst();

    /** Termina el trabajo y lo manda a imprimir. */
    public abstract void end();

    /**
     * Termina el trabajo si nadie lo terminó.
     *
     * @deprecated depende de la recolección de basura, que no garantiza ni cuándo corre ni que
     *     corra. Hay que llamar a {@link #end} a mano.
     */
    @Deprecated
    public void finalize() {
        this.end();
    }
}
