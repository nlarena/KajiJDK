package org.w3c.dom.ls;

/**
 * KajiLibrary's org.w3c.dom.ls.LSResourceResolver -- the one that decides where each resource comes
 * from.
 *
 * <p>One method, and it is one of the most useful pieces of all the XML of the platform. A document
 * that declares a DTD or imports a schema names a URI, and by default the parser goes to fetch it.
 * Setting a resolver, those names are attended to locally.
 *
 * <p>There are two motives and both weigh. The first is that without this an XML can make the
 * program make network requests nobody asked for, or read local files --the XXE attack-- just by
 * naming them. The second is more prosaic: a schema resolved over the network makes the program
 * stop working the day that server goes down, which is what happens every so often with the W3C
 * DTDs.
 *
 * <p>Returning null means "resolve it yourself as usual", so a resolver that only wants to catch a
 * few names is short to write. One that returns an empty {@link LSInput} for everything unknown is
 * the way of <b>forbidding</b> the external.
 */
public interface LSResourceResolver {

    /**
     * Where that resource comes from.
     *
     * @param type the type, for example the namespace of XML Schema
     * @param namespaceURI the namespace of the resource, or null
     * @param publicId the public identifier, or null
     * @param systemId the system identifier, as the document wrote it
     * @param baseURI what a relative {@code systemId} is resolved against
     * @return where to read it from, or null to let the parser resolve it
     */
    LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
                            String baseURI);
}
