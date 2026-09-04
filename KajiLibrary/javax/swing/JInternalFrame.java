package javax.swing;

/**
 * <strong>Un lugar reservado, no una implementacion.</strong> Misma situacion que {@link JTable}:
 * el {@link JComponent} del que hereda es real y pinta, y lo que falta es la ventana, no el suelo.
 *
 * <p><strong>Existe por un motivo concreto</strong>: {@code javax.swing.event.InternalFrameEvent} la
 * nombra en su constructor y en {@code getInternalFrame}, y sin ella ese evento, su oyente y su
 * adaptador —tres de las cuarenta y cinco clases de {@code javax.swing.event}— no se pueden declarar.
 *
 * <p><strong>Que falta:</strong> sus ochenta y ocho miembros publicos. Una ventana interna es un
 * escritorio dentro de una ventana —se mueve, se maximiza, se minimiza a un icono, tiene barra de
 * titulo y foco propio— y nada de eso existe sin un sistema de ventanas debajo.
 *
 * <p>Vacia y anunciada, por el criterio de la casa: media ventana interna se parece demasiado a
 * una entera, y un miembro que miente es peor que uno que falta.
 */
public class JInternalFrame extends JComponent {

    private static final long serialVersionUID = -5425177187760310384L;

    /** Para las subclases. */
    public JInternalFrame() {
    }
}
