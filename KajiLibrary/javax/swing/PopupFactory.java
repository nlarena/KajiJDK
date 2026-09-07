package javax.swing;

import java.awt.Component;

/**
 * Quien arma las ventanitas de los desplegables.
 *
 * <h2>Por que hay una fabrica y no un constructor</h2>
 *
 * <p>Hay dos formas de mostrar un desplegable y la eleccion depende del caso; ver la nota de
 * {@link Popup}. Concentrarla en una fabrica permite ademas dos cosas que un constructor no daria:
 * reusar las ventanitas en lugar de armar una por cada apertura, y que un programa reemplace la
 * fabrica entera para cambiar como se ven todos los desplegables.
 *
 * <h2>La compartida</h2>
 *
 * <p>{@link #getSharedInstance} devuelve una sola para todo el programa. Que sea compartida es lo
 * que hace que el reuso sirva: una fabrica por menu no tendria nada que reusar.
 */
public class PopupFactory {

    private static PopupFactory compartida = new PopupFactory();

    /** Una fabrica nueva; lo normal es usar la compartida. */
    public PopupFactory() {
    }

    /**
     * Cambia la fabrica de todo el programa.
     *
     * @throws IllegalArgumentException si es nula.
     */
    public static void setSharedInstance(PopupFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("PopupFactory can not be null");
        }
        compartida = factory;
    }

    public static PopupFactory getSharedInstance() {
        return compartida;
    }

    /**
     * Una ventanita con ese contenido, en ese punto de la pantalla.
     *
     * @throws IllegalArgumentException si el contenido es nulo.
     */
    public Popup getPopup(Component owner, Component contents, int x, int y) {
        return getPopup(owner, contents, x, y, false);
    }

    /**
     * Igual, diciendo si conviene la forma liviana.
     *
     * <p>La bandera es un pedido, no una orden: si el contenido no entra en la ventana, se usa la
     * pesada igual. Al reves seria peor -- un menu recortado --, y por eso la decision no se le
     * deja del todo a quien llama.
     */
    protected Popup getPopup(Component owner, Component contents, int x, int y,
            boolean isHeavyWeightPopup) {
        if (contents == null) {
            throw new IllegalArgumentException(
                    "Popup.getPopup must be passed non-null contents");
        }
        return new Popup(owner, contents, x, y);
    }
}
