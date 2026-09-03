package java.util.spi;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * KajiLibrary's java.util.spi.AbstractResourceBundleProvider -- el proveedor que ya sabe armar el
 * nombre.
 *
 * <p>Implementa {@link ResourceBundleProvider} haciendo lo unico que casi todos necesitan: armar el
 * nombre del recurso a partir del nombre base y el local, y cargarlo. Un proveedor concreto
 * normalmente solo declara <b>que formatos</b> maneja y no escribe nada mas.
 *
 * <h2>El nombre se arma con guiones bajos, y los huecos cuentan</h2>
 *
 * <p>{@link #toBundleName} produce {@code Msg_es_AR} para {@code ("Msg", es-AR)}: idioma, script,
 * pais y variante, en ese orden, separados por {@code _}. Un local raiz da el nombre base pelado.
 *
 * <p>Los dos casos que no son obvios son los <b>huecos</b>. Con script, el idioma se escribe aunque
 * este vacio -- si no, {@code Msg_Latn_AR} seria indistinguible de {@code Msg_es_AR} --. Y una
 * variante sin pais deja el hueco a la vista: {@code Msg_es__POSIX}, con dos guiones bajos.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #getBundle} <b>no carga nada</b> y devuelve null. Cargar desde un proveedor pide el
 * sistema de modulos: hay que buscar el recurso <b>en el modulo del proveedor</b>, que es justamente
 * lo que distingue esta via de la vieja, y esta biblioteca no lo tiene. Null es lo que el contrato
 * define como "no lo tengo", asi que un llamador cae a la busqueda por convencion sin enterarse de
 * nada raro -- que es mejor que devolver un bundle sacado del classpath y decir que vino del modulo.
 *
 * <p>{@link #toBundleName} si esta implementado de verdad: es aritmetica de cadenas y no depende de
 * nada.
 */
public abstract class AbstractResourceBundleProvider implements ResourceBundleProvider {

    private final String[] formats;

    /** Sin formatos declarados. */
    protected AbstractResourceBundleProvider() {
        this.formats = new String[0];
    }

    /**
     * Con los formatos que este proveedor maneja: {@code "java.class"}, {@code "java.properties"}.
     *
     * @throws IllegalArgumentException si alguno no es uno de esos dos
     */
    protected AbstractResourceBundleProvider(String... formats) {
        if (formats == null) {
            throw new NullPointerException("formats is null");
        }
        int i = 0;
        while (i < formats.length) {
            if (!"java.class".equals(formats[i]) && !"java.properties".equals(formats[i])) {
                throw new IllegalArgumentException("unknown format: " + formats[i]);
            }
            i = i + 1;
        }
        String[] copy = new String[formats.length];
        System.arraycopy(formats, 0, copy, 0, formats.length);
        this.formats = copy;
    }

    /**
     * El nombre del recurso para ese nombre base y ese local.
     *
     * <p>Ver la nota de la clase para el orden de las partes y los dos huecos.
     */
    protected String toBundleName(String baseName, Locale locale) {
        if (Locale.ROOT.equals(locale)) {
            return baseName;
        }
        String language = locale.getLanguage();
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (language.length() == 0 && script.length() == 0 && country.length() == 0) {
            return baseName;
        }
        StringBuilder sb = new StringBuilder(baseName);
        sb.append("_");
        if (script.length() > 0) {
            // El idioma va aunque este vacio: ver la nota de la clase.
            sb.append(language).append("_").append(script);
            if (country.length() > 0) {
                sb.append("_").append(country);
                if (variant.length() > 0) {
                    sb.append("_").append(variant);
                }
            }
            return sb.toString();
        }
        sb.append(language);
        if (country.length() > 0) {
            sb.append("_").append(country);
            if (variant.length() > 0) {
                sb.append("_").append(variant);
            }
        } else if (variant.length() > 0) {
            sb.append("__").append(variant);
        }
        return sb.toString();
    }

    /** Los formatos declarados al construir. Copia. */
    protected final String[] declaredFormats() {
        String[] copy = new String[this.formats.length];
        System.arraycopy(this.formats, 0, copy, 0, this.formats.length);
        return copy;
    }

    /** Devuelve null: ver la nota de la clase. */
    public ResourceBundle getBundle(String baseName, Locale locale) {
        return null;
    }
}
