package javax.accessibility;

import java.util.ListResourceBundle;

/**
 * El catálogo de nombres traducibles de roles, estados y relaciones.
 *
 * <p>Está **vacío**, y eso es lo correcto acá: esta biblioteca no trae traducciones, así que
 * {@link AccessibleBundle#toDisplayString()} devuelve la clave. Un catálogo con las claves en inglés
 * mapeadas a sí mismas sería el mismo resultado con más ceremonia y con la falsa apariencia de estar
 * traducido.
 *
 * @deprecated el JDK dejó de usarla; las traducciones se buscan por otro camino. Se mantiene porque
 *     está en la API pública.
 */
@Deprecated
public class AccessibleResourceBundle extends ListResourceBundle {

    /** Un catálogo vacío. */
    public AccessibleResourceBundle() {
    }

    /** Sin entradas. */
    public Object[][] getContents() {
        return new Object[0][];
    }
}
