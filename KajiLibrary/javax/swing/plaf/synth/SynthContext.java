package javax.swing.plaf.synth;

import javax.swing.JComponent;

/**
 * Todo lo que hace falta saber para dibujar una parte de un componente.
 *
 * <h2>Los cuatro datos</h2>
 *
 * <p>El componente, que parte de el se esta dibujando, con que estilo, y en que estado. Van juntos
 * porque siempre se necesitan juntos: el color de un boton depende de que boton es, de si se esta
 * dibujando su fondo o su borde, del estilo, y de si esta apretado.
 *
 * <p>Que sea un objeto y no cuatro parametros es lo que permite que un pintor reciba una sola cosa y
 * que agregarle un dato mas al contexto no cambie ciento treinta firmas.
 *
 * <h2>Es inmutable</h2>
 *
 * <p>Se construye uno por cada operacion de dibujo. En el JDK esto llego a reusarse por razones de
 * rendimiento y se volvio atras: un contexto compartido que cambia bajo los pies del que lo esta
 * mirando es una fuente de errores que aparecen solo al repintar rapido.
 *
 * @since 1.5
 */
public class SynthContext {

    private final JComponent component;
    private final Region region;
    private final SynthStyle style;
    private final int state;

    /**
     * Un contexto con esos datos.
     *
     * @param component el componente
     * @param region la parte que se esta dibujando
     * @param style el estilo
     * @param state el estado, combinacion de las banderas de {@link SynthConstants}
     * @throws NullPointerException si alguno de los tres primeros es {@code null}
     */
    public SynthContext(JComponent component, Region region, SynthStyle style, int state) {
        if (component == null || region == null || style == null) {
            throw new NullPointerException("You must supply a non-null component, region and style");
        }
        this.component = component;
        this.region = region;
        this.style = style;
        this.state = state;
    }

    /**
     * Uno que acepta un estilo nulo.
     *
     * <p>El constructor publico exige los tres, y esta bien: un programa que arma un contexto a
     * mano y le pasa un estilo nulo se equivoco. Pero las clases {@code SynthXxxUI} del paquete
     * necesitan armar el contexto <em>antes</em> de tener el estilo -- es con ese contexto que se
     * lo piden a la fabrica -- y por eso existe esta version. El JDK hace lo mismo, y por eso
     * {@code getContext} de una interfaz grafica sin instalar contesta con estilo nulo en vez de
     * reventar. Medido.
     *
     * @param component el componente
     * @param region la parte
     * @param style el estilo, que puede ser nulo
     * @param state el estado
     * @param interno para distinguir esta version de la publica
     */
    SynthContext(JComponent component, Region region, SynthStyle style, int state,
            boolean interno) {
        this.component = component;
        this.region = region;
        this.style = style;
        this.state = state;
    }

    /**
     * El componente.
     *
     * @return el componente
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * La parte que se esta dibujando.
     *
     * @return la region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * El estilo.
     *
     * @return el estilo
     */
    public SynthStyle getStyle() {
        return style;
    }

    /**
     * El estado del componente.
     *
     * @return la combinacion de banderas de {@link SynthConstants}
     */
    public int getComponentState() {
        return state;
    }
}
