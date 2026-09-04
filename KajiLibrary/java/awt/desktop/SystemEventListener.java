package java.awt.desktop;

import java.util.EventListener;

/**
 * KajiLibrary's java.awt.desktop.SystemEventListener -- la marca de los escuchas del escritorio.
 *
 * <p>No declara nada. Existe para que {@code Desktop.addAppEventListener} tenga un solo parametro en
 * lugar de una sobrecarga por cada tipo de escucha, y para que el escritorio pueda repartir cada
 * evento preguntando con {@code instanceof}.
 *
 * <p>La diferencia con los manejadores de este paquete --{@link AboutHandler} y compania-- es de
 * naturaleza, no de forma: un escucha recibe un <b>aviso</b> y puede haber muchos; un manejador toma
 * una <b>responsabilidad</b> y hay uno solo.
 */
public interface SystemEventListener extends EventListener {
}
