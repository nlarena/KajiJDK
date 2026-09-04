package java.beans;

import java.applet.Applet;
import java.beans.beancontext.BeanContext;

/**
 * Quien prepara un bean que resulta ser un applet, cuando lo trae {@link Beans#instantiate}.
 *
 * <p>Un applet suelto no sirve: necesita un {@link java.applet.AppletStub} que le diga dónde está y
 * un contexto que lo aloje, y eso normalmente lo pone el navegador. Cuando el applet lo crea
 * `Beans.instantiate` no hay navegador, así que alguien tiene que hacer ese trabajo: es este tipo.
 * Los dos pasos están separados porque van en momentos distintos —preparar antes de entrar al
 * contexto, arrancar después—.
 *
 * <p>Acá ninguno de los dos métodos llega a llamarse: un {@link Applet} no se puede construir sin
 * pantalla. El tipo está entero igual, porque `Beans.instantiate` lo nombra.
 *
 * @deprecated el modelo de applets está en desuso desde Java 9 y marcado para borrarse desde 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletInitializer {

    /**
     * Prepara el applet: le pone el representante y lo que haga falta para que pueda correr.
     *
     * @param bCtxt el contexto que lo va a alojar, o `null`
     */
    void initialize(Applet newAppletBean, BeanContext bCtxt);

    /** Lo arranca: es el momento de llamar a {@code start()}. */
    void activate(Applet newApplet);
}
