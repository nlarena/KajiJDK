package java.beans.beancontext;

import java.beans.DesignMode;
import java.beans.Visibility;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 * Un contenedor de beans: la colección de sus hijos, más el entorno que les ofrece.
 *
 * <p>Es una {@link Collection} y hay que leerlo así --agregar un bean al contexto **es** `add`-arlo--
 * pero además es un {@link BeanContextChild}, y de ahí sale lo que da forma a toda la API: los
 * contextos se anidan. Un contexto tiene hijos y a la vez es hijo de otro, y por eso las búsquedas
 * de recursos y de servicios suben por la cadena hasta que alguien contesta.
 *
 * <h2>El candado global</h2>
 *
 * <p>{@link #globalHierarchyLock} es **uno solo para toda la jerarquía**, no uno por contexto, y la
 * razón es que una operación puede tocar varios contextos a la vez: mudar un hijo lo saca de uno y
 * lo mete en otro. Con un candado por contexto, dos mudanzas cruzadas se abrazarían. Con uno solo no
 * hay orden que respetar porque no hay dos candados que tomar.
 */
public interface BeanContext extends BeanContextChild, Collection, DesignMode, Visibility {

    /**
     * El candado que serializa toda operación sobre la jerarquía. Ver la nota de la interfaz sobre
     * por qué es uno y no uno por contexto.
     */
    public static final Object globalHierarchyLock = new Object();

    /**
     * Instancia ese bean **dentro de este contexto**, por su nombre.
     *
     * @throws IOException si el bean no se pudo leer
     * @throws ClassNotFoundException si no se encontró la clase
     */
    Object instantiateChild(String beanName) throws IOException, ClassNotFoundException;

    /** El recurso, buscado como lo vería ese hijo. */
    InputStream getResourceAsStream(String name, BeanContextChild bcc);

    /** La URL del recurso, buscada como la vería ese hijo. */
    URL getResource(String name, BeanContextChild bcc);

    /** Registra un oyente de altas y bajas de hijos. */
    void addBeanContextMembershipListener(BeanContextMembershipListener bcml);

    /** Lo quita. */
    void removeBeanContextMembershipListener(BeanContextMembershipListener bcml);
}
