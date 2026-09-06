package javax.swing.plaf.nimbus;

import javax.swing.JComponent;

/**
 * Un estado propio, para que un aspecto pueda dibujar algo que Swing no nombra.
 *
 * <h2>Para que</h2>
 *
 * <p>Los estados de {@code SynthConstants} --apretado, deshabilitado, con el foco-- son los que
 * Swing conoce. Un aspecto grafico puede querer distinguir mas: un boton dentro de una barra de
 * herramientas, una barra de progreso que ya termino, un campo de texto que no esta dentro de un
 * panel con desplazamiento. Ninguno de esos es un estado de Swing, y los tres cambian como se ve.
 *
 * <p>Un estado propio se define heredando y contestando {@link #isInState}. Nimbus define una
 * veintena asi.
 *
 * <h2>Por que la pregunta y no una bandera</h2>
 *
 * <p>Porque el estado no se guarda en ningun lado: se calcula mirando el componente en el momento de
 * dibujarlo. Guardarlo obligaria a mantenerlo al dia ante cada cambio, que es exactamente el tipo de
 * estado duplicado que se desincroniza.
 *
 * @param <T> el tipo de componente al que se le puede preguntar
 * @since 1.7
 */
public abstract class State<T extends JComponent> {

    private final String name;

    /**
     * Un estado con ese nombre.
     *
     * <p>El nombre es lo que lo identifica en la descripcion del aspecto, y por eso no puede
     * repetirse entre los estados de un mismo componente.
     *
     * @param name como se llama
     */
    protected State(String name) {
        this.name = name;
    }

    /**
     * El nombre.
     *
     * @return como se llama
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Si el componente esta en este estado ahora.
     *
     * <p>Se lo llama al dibujar, asi que tiene que ser barato: mirar propiedades del componente,
     * no recorrer arboles ni consultar nada de afuera.
     *
     * @param c el componente
     * @return cierto si esta en este estado
     */
    protected abstract boolean isInState(T c);
}
