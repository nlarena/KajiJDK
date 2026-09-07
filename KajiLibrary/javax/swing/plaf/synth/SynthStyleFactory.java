package javax.swing.plaf.synth;

import javax.swing.JComponent;

/**
 * De donde salen los estilos.
 *
 * <h2>Por que una fabrica y no una tabla</h2>
 *
 * <p>Porque el estilo de un componente puede depender de mas cosas que su tipo: de su nombre, de
 * quien es su contenedor, de una propiedad que le pusieron. Una tabla solo podria mirar el tipo; una
 * fabrica mira lo que quiera.
 *
 * <p>Es el punto donde un aspecto grafico decide como se ve todo. {@link SynthLookAndFeel} le pide
 * un estilo por cada componente y por cada region de cada componente.
 *
 * @since 1.5
 */
public abstract class SynthStyleFactory {

    /** Una fabrica. */
    public SynthStyleFactory() {
    }

    /**
     * El estilo de esa region de ese componente.
     *
     * @param c el componente
     * @param id la region
     * @return el estilo; no puede ser {@code null}
     */
    public abstract SynthStyle getStyle(JComponent c, Region id);
}
