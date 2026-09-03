package org.ietf.jgss;

import java.security.Provider;

/**
 * KajiLibrary's org.ietf.jgss.GSSManager -- por donde se entra a GSS-API.
 *
 * <p>Es la fabrica de todo lo demas: nombres, credenciales y contextos. No tiene constructor publico
 * util --se llega por {@link #getInstance}-- porque la instancia lleva la lista de mecanismos
 * disponibles y esa lista es del proceso.
 *
 * <p>Los {@code addProviderAt*} son lo que hace que esa lista sea configurable en caliente:
 * {@code Front} le da prioridad a un proveedor sobre los que ya estaban y {@code End} lo deja de
 * ultimo. Sirve para forzar que un mecanismo lo atienda una implementacion concreta sin tocar la
 * instalacion.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca <b>no trae ningun mecanismo</b> GSS-API: no hay Kerberos, y sin un mecanismo no
 * hay nombres que resolver ni contextos que establecer. {@link #getInstance} devuelve un gestor sin
 * mecanismos, y eso se nota asi:
 *
 * <ul>
 *   <li>{@link #getMechs} y {@link #getMechsForName} devuelven arreglos <b>vacios</b>, que es la
 *       verdad --no hay ninguno-- y no una falla;
 *   <li>los {@code createName}, {@code createCredential} y {@code createContext} lanzan
 *       {@link GSSException} con {@link GSSException#UNAVAILABLE}, y {@link #getNamesForMech} con
 *       {@link GSSException#BAD_MECH}. Los dos codigos estan declarados y significan exactamente lo
 *       que pasa;
 *   <li>los {@code addProviderAt*} lanzan {@code UNAVAILABLE}: no hay donde agregarlo.
 * </ul>
 *
 * <p>Lo que <b>si</b> anda entero es el resto del paquete: {@link Oid} codifica y decodifica DER de
 * verdad, y {@link GSSException}, {@link MessageProp} y {@link ChannelBinding} son completos. Una
 * implementacion de mecanismo que se escriba contra estas interfaces no necesita nada mas de aca.
 */
public abstract class GSSManager {

    /** Publico porque las subclases lo necesitan; para conseguir uno va {@link #getInstance}. */
    public GSSManager() {
    }

    /**
     * El gestor por omision.
     *
     * <p>En KajiLibrary, uno sin mecanismos; ver la nota de la clase.
     */
    public static GSSManager getInstance() {
        return new EmptyManager();
    }

    /** Los mecanismos disponibles. */
    public abstract Oid[] getMechs();

    /**
     * Los tipos de nombre que ese mecanismo entiende.
     *
     * @throws GSSException con {@link GSSException#BAD_MECH} si no conoce ese mecanismo
     */
    public abstract Oid[] getNamesForMech(Oid mech) throws GSSException;

    /** Los mecanismos que entienden ese tipo de nombre. */
    public abstract Oid[] getMechsForName(Oid nameType);

    /** Un nombre desde texto. */
    public abstract GSSName createName(String nameStr, Oid nameType) throws GSSException;

    /** Un nombre desde bytes. */
    public abstract GSSName createName(byte[] name, Oid nameType) throws GSSException;

    /** Un nombre desde texto, ya resuelto para un mecanismo. */
    public abstract GSSName createName(String nameStr, Oid nameType, Oid mech) throws GSSException;

    /** Un nombre desde bytes, ya resuelto para un mecanismo. */
    public abstract GSSName createName(byte[] name, Oid nameType, Oid mech) throws GSSException;

    /**
     * La credencial por omision.
     *
     * @param usage una de las constantes de {@link GSSCredential}
     */
    public abstract GSSCredential createCredential(int usage) throws GSSException;

    /** Una credencial para esa identidad y ese mecanismo. */
    public abstract GSSCredential createCredential(GSSName name, int lifetime, Oid mech, int usage)
        throws GSSException;

    /** Idem, para varios mecanismos de una. */
    public abstract GSSCredential createCredential(GSSName name, int lifetime, Oid[] mechs,
                                                   int usage) throws GSSException;

    /** Un contexto del lado que inicia. */
    public abstract GSSContext createContext(GSSName peer, Oid mech, GSSCredential myCred,
                                             int lifetime) throws GSSException;

    /** Un contexto del lado que acepta. */
    public abstract GSSContext createContext(GSSCredential myCred) throws GSSException;

    /** Un contexto reconstruido desde {@link GSSContext#export}. */
    public abstract GSSContext createContext(byte[] interProcessToken) throws GSSException;

    /** Pone ese proveedor primero para ese mecanismo. */
    public abstract void addProviderAtFront(Provider p, Oid mech) throws GSSException;

    /** Lo pone ultimo. */
    public abstract void addProviderAtEnd(Provider p, Oid mech) throws GSSException;

    /**
     * El gestor sin mecanismos que devuelve {@link #getInstance}.
     *
     * <p>Ver la nota de la clase: contesta vacio donde vacio es la verdad, y lanza el codigo que
     * corresponde donde no hay nada que devolver.
     */
    private static final class EmptyManager extends GSSManager {

        /** Vacio: no hay ninguno. */
        public Oid[] getMechs() {
            return new Oid[0];
        }

        public Oid[] getNamesForMech(Oid mech) throws GSSException {
            throw new GSSException(GSSException.BAD_MECH, 0,
                "KajiLibrary includes no GSS-API mechanism");
        }

        /** Vacio: ningun mecanismo entiende ningun tipo de nombre. */
        public Oid[] getMechsForName(Oid nameType) {
            return new Oid[0];
        }

        public GSSName createName(String nameStr, Oid nameType) throws GSSException {
            throw unavailable();
        }

        public GSSName createName(byte[] name, Oid nameType) throws GSSException {
            throw unavailable();
        }

        public GSSName createName(String nameStr, Oid nameType, Oid mech) throws GSSException {
            throw unavailable();
        }

        public GSSName createName(byte[] name, Oid nameType, Oid mech) throws GSSException {
            throw unavailable();
        }

        public GSSCredential createCredential(int usage) throws GSSException {
            throw unavailable();
        }

        public GSSCredential createCredential(GSSName name, int lifetime, Oid mech, int usage)
            throws GSSException {
            throw unavailable();
        }

        public GSSCredential createCredential(GSSName name, int lifetime, Oid[] mechs, int usage)
            throws GSSException {
            throw unavailable();
        }

        public GSSContext createContext(GSSName peer, Oid mech, GSSCredential myCred, int lifetime)
            throws GSSException {
            throw unavailable();
        }

        public GSSContext createContext(GSSCredential myCred) throws GSSException {
            throw unavailable();
        }

        public GSSContext createContext(byte[] interProcessToken) throws GSSException {
            throw unavailable();
        }

        public void addProviderAtFront(Provider p, Oid mech) throws GSSException {
            throw unavailable();
        }

        public void addProviderAtEnd(Provider p, Oid mech) throws GSSException {
            throw unavailable();
        }

        /** El mismo motivo para todos los que no tienen nada que devolver. */
        private static GSSException unavailable() {
            return new GSSException(GSSException.UNAVAILABLE, 0,
                "KajiLibrary includes no GSS-API mechanism");
        }
    }
}
