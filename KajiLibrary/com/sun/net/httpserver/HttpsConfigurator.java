package com.sun.net.httpserver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Decide como se configura TLS en cada conexion entrante de un {@link HttpsServer}.
 *
 * <h2>Por que es una clase con un metodo sobrescribible y no un objeto de configuracion</h2>
 *
 * <p>Porque la configuracion puede depender <strong>de quien se conecta</strong>. {@link #configure}
 * recibe unos {@link HttpsParameters} que ya traen la direccion del cliente, asi que se puede exigir
 * certificado a unos y a otros no, o restringir las suites por origen. Un objeto fijo no permitiria
 * eso.
 *
 * <p>La implementacion por omision aplica los parametros por omision del contexto, que es lo
 * razonable cuando no hace falta distinguir.
 */
public class HttpsConfigurator {

    private final SSLContext context;

    /**
     * @throws NullPointerException si el contexto es {@code null}
     */
    public HttpsConfigurator(SSLContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        this.context = context;
    }

    /** El contexto que provee las credenciales y la politica de confianza. */
    public SSLContext getSSLContext() {
        return this.context;
    }

    /**
     * Ajusta los parametros de una conexion entrante.
     *
     * <p>Por omision le pone los del contexto. Quien la sobrescriba tiene que llamar a
     * {@link HttpsParameters#setSSLParameters} o la conexion queda sin configurar.
     */
    public void configure(HttpsParameters params) {
        params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
    }
}
