package javax.swing.text;

import javax.swing.event.ChangeListener;

/**
 * Un conjunto de atributos con nombre, que avisa cuando cambia.
 *
 * <p>Es lo que hace que cambiar un estilo cambie de golpe todo lo que lo usa: los parrafos no
 * copian sus atributos, se cuelgan del estilo como padre de resolucion, y cuando el estilo cambia
 * avisa y todos se repintan.
 *
 * <p>El nombre puede ser {@code null}: un estilo anonimo sirve igual para colgarse de el, solo que
 * no se puede pedir por nombre.
 */
public interface Style extends MutableAttributeSet {

    String getName();

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
