package org.w3c.dom.bootstrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMImplementationList;
import org.w3c.dom.DOMImplementationSource;

/**
 * KajiLibrary's org.w3c.dom.bootstrap.DOMImplementationRegistry -- por donde se entra al DOM.
 *
 * <p>El problema que resuelve es de arranque: para usar DOM hace falta un
 * {@link DOMImplementation}, y para conseguir uno haria falta ya tener uno. Esta clase corta ese
 * circulo, y por eso vive en un paquete que se llama <b>bootstrap</b>.
 *
 * <p>Se piden implementaciones por <b>caracteristicas</b>, no por nombre de clase:
 * {@code "XML 3.0 LS"} pide una que sepa XML nivel 3 y ademas cargar y guardar. Es lo que hace que
 * el codigo no dependa de quien la provee, que es todo el punto de la indireccion.
 *
 * <h2>El orden de busqueda</h2>
 *
 * <p>{@link #newInstance} arma la lista de fuentes con la propiedad de sistema {@link #PROPERTY} --un
 * texto con nombres de clase separados por espacios-- y con los proveedores registrados como
 * servicio. Despues, cada consulta le pregunta a las fuentes <b>en ese orden</b> y se queda con la
 * primera que conteste.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae una implementacion de DOM incluida, asi que sin una fuente registrada el
 * registro esta vacio: {@link #getDOMImplementation} devuelve null y {@link #getDOMImplementationList}
 * una lista de largo cero. Las dos son respuestas <b>definidas</b> del contrato --significan "no hay
 * ninguna con esas caracteristicas"-- y son exactamente lo que contesta el JDK cuando se le piden
 * caracteristicas que nadie soporta. Registrando una fuente, el registro funciona como siempre.
 */
public final class DOMImplementationRegistry {

    /** La propiedad de sistema con los nombres de clase, separados por espacios. */
    public static final String PROPERTY = "org.w3c.dom.DOMImplementationSourceList";

    /** Las fuentes, en orden de consulta. */
    private final List<DOMImplementationSource> sources;

    /** Privado: se entra por {@link #newInstance}. */
    private DOMImplementationRegistry(List<DOMImplementationSource> sources) {
        this.sources = sources;
    }

    /**
     * Un registro con las fuentes configuradas.
     *
     * <p>Ver el orden en la nota de la clase.
     *
     * @throws ClassNotFoundException si la propiedad nombra una clase que no existe
     * @throws InstantiationException si una no se puede construir
     * @throws IllegalAccessException si su constructor no es accesible
     * @throws ClassCastException si una no es un {@link DOMImplementationSource}
     */
    public static DOMImplementationRegistry newInstance()
        throws ClassNotFoundException, InstantiationException, IllegalAccessException,
               ClassCastException {
        List<DOMImplementationSource> found = new ArrayList<DOMImplementationSource>();
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // Sin permiso para leerla: quedan solo los servicios.
        }
        if (configured != null) {
            // Espacios como separador: es lo que dice la especificacion, y por eso un nombre de
            // clase con espacios no se puede configurar por esta via.
            String[] names = configured.split(" ");
            int i = 0;
            while (i < names.length) {
                String name = names[i].trim();
                if (name.length() > 0) {
                    found.add(build(name));
                }
                i = i + 1;
            }
        }
        ServiceLoader<DOMImplementationSource> loader =
            ServiceLoader.load(DOMImplementationSource.class);
        Iterator<DOMImplementationSource> it = loader.iterator();
        while (it.hasNext()) {
            found.add(it.next());
        }
        return new DOMImplementationRegistry(found);
    }

    /** Construye una fuente por nombre, traduciendo las fallas a las que declara el contrato. */
    private static DOMImplementationSource build(String className)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = DOMImplementationRegistry.class.getClassLoader();
        }
        Class<?> found = Class.forName(className, true, loader);
        Object made;
        try {
            made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (InstantiationException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Exception e) {
            // Sin constructor sin argumentos, o el constructor tiro: para el contrato es lo mismo
            // que no haberla podido instanciar.
            throw new InstantiationException(className + ": " + e);
        }
        return (DOMImplementationSource) made;
    }

    /**
     * La primera implementacion que tenga esas caracteristicas.
     *
     * @param features una lista separada por espacios, por ejemplo {@code "XML 3.0 LS"}
     * @return null si ninguna fuente la tiene
     */
    public DOMImplementation getDOMImplementation(String features) {
        int i = 0;
        while (i < this.sources.size()) {
            DOMImplementation found = this.sources.get(i).getDOMImplementation(features);
            if (found != null) {
                return found;
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Todas las que tengan esas caracteristicas, de todas las fuentes.
     *
     * @return una lista de largo cero si no hay ninguna
     */
    public DOMImplementationList getDOMImplementationList(String features) {
        List<DOMImplementation> all = new ArrayList<DOMImplementation>();
        int i = 0;
        while (i < this.sources.size()) {
            DOMImplementationList some = this.sources.get(i).getDOMImplementationList(features);
            if (some != null) {
                int j = 0;
                while (j < some.getLength()) {
                    all.add(some.item(j));
                    j = j + 1;
                }
            }
            i = i + 1;
        }
        return new ListOfImplementations(all);
    }

    /**
     * Agrega una fuente al final.
     *
     * <p>Al final y no al principio: las configuradas ganan sobre las agregadas a mano, que es lo
     * que hace que la propiedad de sistema sirva para forzar una implementacion.
     *
     * @throws NullPointerException si es null
     */
    public void addSource(DOMImplementationSource s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.sources.add(s);
    }

    /** La lista que devuelve {@link #getDOMImplementationList}. */
    private static final class ListOfImplementations implements DOMImplementationList {

        private final List<DOMImplementation> items;

        ListOfImplementations(List<DOMImplementation> items) {
            this.items = items;
        }

        /** Null fuera de rango, como pide el DOM; no lanza. */
        public DOMImplementation item(int index) {
            if (index < 0 || index >= this.items.size()) {
                return null;
            }
            return this.items.get(index);
        }

        public int getLength() {
            return this.items.size();
        }
    }
}
