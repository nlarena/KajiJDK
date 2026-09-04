package javax.imageio.spi;

import java.util.Locale;

/**
 * KajiLibrary's javax.imageio.spi.IIOServiceProvider -- la base de todos los proveedores de imagenes.
 *
 * <p>Lo que comparten los seis tipos de proveedor: quien lo hizo, que version, y una descripcion
 * traducible.
 *
 * <p>Implementa {@link RegisterableService} con los dos metodos vacios, asi que una subclase que no
 * necesite enterarse de nada no tiene que escribirlos.
 *
 * <p>El constructor sin argumentos existe para los proveedores que se cargan por
 * {@link java.util.ServiceLoader}, que exige uno publico sin parametros. Deja los dos campos en null,
 * y la subclase los tiene que llenar antes de que alguien los lea.
 */
public abstract class IIOServiceProvider implements RegisterableService {

    /** Quien lo hizo. */
    protected String vendorName;

    /** Que version. */
    protected String version;

    /**
     * @throws IllegalArgumentException si alguno de los dos es null
     */
    public IIOServiceProvider(String vendorName, String version) {
        if (vendorName == null) {
            throw new IllegalArgumentException("vendorName == null!");
        }
        if (version == null) {
            throw new IllegalArgumentException("version == null!");
        }
        this.vendorName = vendorName;
        this.version = version;
    }

    /** El que exige el cargador de servicios. Ver la nota de la clase. */
    public IIOServiceProvider() {
    }

    /** No hace nada; una subclase que necesite enterarse lo redefine. */
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
    }

    /** No hace nada. */
    public void onDeregistration(ServiceRegistry registry, Class<?> category) {
    }

    /** Quien lo hizo. */
    public String getVendorName() {
        return this.vendorName;
    }

    /** Que version. */
    public String getVersion() {
        return this.version;
    }

    /**
     * Que hace este proveedor, en palabras.
     *
     * @param locale en que idioma, o null para el del sistema
     */
    public abstract String getDescription(Locale locale);
}
