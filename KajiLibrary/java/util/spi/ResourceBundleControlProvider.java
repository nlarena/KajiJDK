package java.util.spi;

import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.ResourceBundleControlProvider -- cambia como se buscan los bundles.
 *
 * <p>Es la via <b>vieja</b>, y esta desaconsejada por un motivo concreto: un
 * {@code ResourceBundle.Control} cambia la estrategia de busqueda para <b>todo el proceso</b>, asi
 * que dos bibliotecas con estrategias distintas se pisan y gana la que se cargo primero.
 * {@link ResourceBundleProvider} resuelve lo mismo sin ese efecto global, porque ahi cada modulo
 * provee lo suyo y nadie decide por los demas.
 *
 * <p>Sigue en el API porque hay codigo que la usa. Un proveedor devuelve null para los nombres que
 * no le interesan, y ahi vale la estrategia por omision.
 */
public interface ResourceBundleControlProvider {

    /**
     * El {@code Control} para ese nombre de bundle, o null para no intervenir.
     *
     * @param baseName el nombre completo del bundle
     */
    ResourceBundle.Control getControl(String baseName);
}
