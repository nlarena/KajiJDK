package java.util.spi;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.ResourceBundleProvider -- de donde salen los textos traducidos.
 *
 * <p>Es la via moderna para empaquetar traducciones: en vez de que
 * {@code ResourceBundle.getBundle} busque archivos en el classpath por convencion de nombre, un
 * modulo declara que provee los textos de tal paquete y este metodo se los entrega.
 *
 * <p>La diferencia practica es que el buscador por convencion tiene que <b>adivinar</b> -- probar
 * {@code Msg_es_AR}, despues {@code Msg_es}, despues {@code Msg} -- mientras que un proveedor sabe
 * lo que tiene. Por eso puede devolver null sin costo: es "no lo tengo", no "no lo encontre".
 */
public interface ResourceBundleProvider {

    /**
     * El bundle de ese nombre para ese local, o null si este proveedor no lo tiene.
     *
     * @param baseName el nombre <b>completo</b> del bundle, con paquete
     */
    ResourceBundle getBundle(String baseName, Locale locale);
}
