package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Lo que toda interfaz grafica de Synth tiene que saber contestar.
 *
 * <p>Son dos metodos y alcanzan para todo el aspecto.
 *
 * <p>{@link #getContext} devuelve el {@link SynthContext} de ese componente: que region es, en que
 * estado esta y que estilo le toca. Todo lo demas de Synth arranca ahi -- los colores, la
 * tipografia, los margenes y el pintor salen del estilo del contexto --, y por eso el metodo se
 * llama en cada dibujado y no una sola vez al instalar: el estado cambia con el mouse y con el
 * foco.
 *
 * <p>{@link #paintBorder} esta separado del dibujo del contenido porque el borde de un componente
 * de Synth no lo dibuja un {@code Border}: lo dibuja el estilo, con la misma imagen de la que sale
 * el fondo. Un {@code Border} no tendria como saber en que estado esta el componente.
 *
 * <p>Hereda {@link SynthConstants} nada mas que para que las clases que la implementan puedan
 * escribir {@code ENABLED} en vez de {@code SynthConstants.ENABLED}.
 */
public interface SynthUI extends SynthConstants {

    /**
     * El contexto de ese componente, con su estado de ahora.
     *
     * @param c el componente
     * @return el contexto
     */
    SynthContext getContext(JComponent c);

    /**
     * Dibuja el borde.
     *
     * @param context el contexto
     * @param g donde dibujar
     * @param x la esquina
     * @param y la esquina
     * @param w el ancho
     * @param h el alto
     */
    void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h);
}
