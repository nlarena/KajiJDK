package org.ietf.jgss;

import java.security.Provider;

/**
 * KajiLibrary's org.ietf.jgss.GSSManager -- where one enters GSS-API.
 *
 * <p>It is the factory of everything else: names, credentials and contexts. It has no useful public
 * constructor --one gets there through {@link #getInstance}-- because the instance carries the list
 * of available mechanisms and that list belongs to the process.
 *
 * <p>The {@code addProviderAt*} are what make that list configurable on the fly: {@code Front}
 * gives a provider priority over the ones that were already there and {@code End} leaves it last.
 * It serves for forcing a concrete implementation to attend to a mechanism without touching the
 * installation.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library <b>brings no GSS-API mechanism</b>: there is no Kerberos, and without a mechanism
 * there are no names to resolve nor contexts to establish. {@link #getInstance} returns a manager
 * with no mechanisms, and that shows like this:
 *
 * <ul>
 *   <li>{@link #getMechs} and {@link #getMechsForName} return <b>empty</b> arrays, which is the
 *       truth --there is none-- and not a failure;
 *   <li>the {@code createName}, {@code createCredential} and {@code createContext} throw {@link
 *       GSSException} with {@link GSSException#UNAVAILABLE}, and {@link #getNamesForMech} with
 *       {@link GSSException#BAD_MECH}. The two codes are declared and mean exactly what is
 *       happening;
 *   <li>the {@code addProviderAt*} throw {@code UNAVAILABLE}: there is nowhere to add it.
 * </ul>
 *
 * <p>What <b>does</b> work whole is the rest of the package: {@link Oid} encodes and decodes DER
 * for real, and {@link GSSException}, {@link MessageProp} and {@link ChannelBinding} are complete.
 * An implementation of a mechanism written against these interfaces needs nothing more from here.
 */
public abstract class GSSManager {

    /** Public because the subclasses need it; to get one, go through {@link #getInstance}. */
    public GSSManager() {
    }

    /**
     * The default manager.
     *
     * <p>In KajiLibrary, one with no mechanisms; see the note of the class.
     */
    public static GSSManager getInstance() {
        return new EmptyManager();
    }

    /** The available mechanisms. */
    public abstract Oid[] getMechs();

    /**
     * The types of name that mechanism understands.
     *
     * @throws GSSException with {@link GSSException#BAD_MECH} if it does not know that mechanism
     */
    public abstract Oid[] getNamesForMech(Oid mech) throws GSSException;

    /** The mechanisms that understand that type of name. */
    public abstract Oid[] getMechsForName(Oid nameType);

    /** A name from text. */
    public abstract GSSName createName(String nameStr, Oid nameType) throws GSSException;

    /** A name from bytes. */
    public abstract GSSName createName(byte[] name, Oid nameType) throws GSSException;

    /** A name from text, already resolved for a mechanism. */
    public abstract GSSName createName(String nameStr, Oid nameType, Oid mech) throws GSSException;

    /** A name from bytes, already resolved for a mechanism. */
    public abstract GSSName createName(byte[] name, Oid nameType, Oid mech) throws GSSException;

    /**
     * The default credential.
     *
     * @param usage one of the constants of {@link GSSCredential}
     */
    public abstract GSSCredential createCredential(int usage) throws GSSException;

    /** A credential for that identity and that mechanism. */
    public abstract GSSCredential createCredential(GSSName name, int lifetime, Oid mech, int usage)
        throws GSSException;

    /** The same, for several mechanisms at once. */
    public abstract GSSCredential createCredential(GSSName name, int lifetime, Oid[] mechs,
                                                   int usage) throws GSSException;

    /** A context on the initiating side. */
    public abstract GSSContext createContext(GSSName peer, Oid mech, GSSCredential myCred,
                                             int lifetime) throws GSSException;

    /** A context on the accepting side. */
    public abstract GSSContext createContext(GSSCredential myCred) throws GSSException;

    /** A context rebuilt from {@link GSSContext#export}. */
    public abstract GSSContext createContext(byte[] interProcessToken) throws GSSException;

    /** It puts that provider first for that mechanism. */
    public abstract void addProviderAtFront(Provider p, Oid mech) throws GSSException;

    /** It puts it last. */
    public abstract void addProviderAtEnd(Provider p, Oid mech) throws GSSException;

    /**
     * The manager with no mechanisms {@link #getInstance} returns.
     *
     * <p>See the note of the class: it answers empty where empty is the truth, and throws the code
     * that corresponds where there is nothing to return.
     */
    private static final class EmptyManager extends GSSManager {

        /** Empty: there is none. */
        public Oid[] getMechs() {
            return new Oid[0];
        }

        public Oid[] getNamesForMech(Oid mech) throws GSSException {
            throw new GSSException(GSSException.BAD_MECH, 0,
                "KajiLibrary includes no GSS-API mechanism");
        }

        /** Empty: no mechanism understands any type of name. */
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

        /** The same reason for all the ones that have nothing to return. */
        private static GSSException unavailable() {
            return new GSSException(GSSException.UNAVAILABLE, 0,
                "KajiLibrary includes no GSS-API mechanism");
        }
    }
}
