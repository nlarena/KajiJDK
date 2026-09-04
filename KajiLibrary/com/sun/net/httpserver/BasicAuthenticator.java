package com.sun.net.httpserver;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Autenticacion HTTP basica: usuario y clave en un encabezado.
 *
 * <h2>Lo que hay que saber antes de usarla</h2>
 *
 * <p>La clave viaja en <strong>Base64, que no es cifrado</strong>: es una codificacion reversible
 * que cualquiera deshace de memoria. Sobre HTTP en claro, mandar autenticacion basica es mandar la
 * clave en texto plano. Solo tiene sentido sobre TLS.
 *
 * <p>Y va en <em>cada</em> pedido, no solo en el primero: no hay sesion, asi que la clave se repite
 * indefinidamente mientras dure la navegacion.
 *
 * <h2>Lo unico que hay que escribir</h2>
 *
 * <p>{@link #checkCredentials}. Todo lo demas —parsear el encabezado, decodificar, armar el desafio
 * con el reino, distinguir "no mando nada" de "mando mal"— ya esta.
 *
 * <h2>El juego de caracteres</h2>
 *
 * <p>El constructor de dos argumentos existe porque la especificacion original no decia como
 * codificar los no-ASCII, y cada cliente hizo lo suyo. Fijarlo en UTF-8 es lo correcto hoy; el
 * constructor de un argumento usa ese mismo valor.
 */
public abstract class BasicAuthenticator extends Authenticator {

    /** El reino que se anuncia en el desafio. */
    protected final String realm;

    private final Charset charset;

    /**
     * En ese reino, con UTF-8.
     *
     * @throws IllegalArgumentException si el reino esta vacio o tiene caracteres no ASCII — viaja
     *     en un encabezado, y ahi no entra otra cosa
     */
    public BasicAuthenticator(String realm) {
        this(realm, StandardCharsets.UTF_8);
    }

    /**
     * En ese reino, con ese juego de caracteres para decodificar las credenciales.
     *
     * @throws NullPointerException si alguno es {@code null}
     * @throws IllegalArgumentException si el reino esta vacio o no es ASCII
     */
    public BasicAuthenticator(String realm, Charset charset) {
        if (realm == null) {
            throw new NullPointerException("realm");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (realm.isEmpty()) {
            throw new IllegalArgumentException("el reino no puede estar vacio");
        }
        for (int i = 0; i < realm.length(); i++) {
            if (realm.charAt(i) > 127) {
                throw new IllegalArgumentException("el reino tiene que ser ASCII: " + realm);
            }
        }
        this.realm = realm;
        this.charset = charset;
    }

    /** El reino. */
    public String getRealm() {
        return this.realm;
    }

    /**
     * Parsea el encabezado y consulta a {@link #checkCredentials}.
     *
     * <p>Devuelve {@link Retry} cuando no vino credencial o vino mal formada, y tambien cuando la
     * credencial no valida. Lo segundo es a proposito y no un descuido: un {@link Failure} le diria
     * al navegador que deje de intentar, cuando lo que corresponde es volver a pedirle la clave.
     */
    public Result authenticate(HttpExchange t) {
        String header = t.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return desafiar(t);
        }
        int sp = header.indexOf(' ');
        if (sp == -1 || !header.substring(0, sp).equalsIgnoreCase("Basic")) {
            return desafiar(t);
        }
        byte[] crudo;
        try {
            crudo = Base64.getDecoder().decode(header.substring(sp + 1).trim());
        } catch (IllegalArgumentException e) {
            return desafiar(t);
        }
        String userpass = new String(crudo, this.charset);
        // El PRIMER `:`, no el ultimo: una clave puede contener dos puntos y un usuario no.
        int corte = userpass.indexOf(':');
        if (corte == -1) {
            return desafiar(t);
        }
        String usuario = userpass.substring(0, corte);
        String clave = userpass.substring(corte + 1);
        if (!checkCredentials(usuario, clave)) {
            return desafiar(t);
        }
        return new Success(new HttpPrincipal(usuario, this.realm));
    }

    private Result desafiar(HttpExchange t) {
        t.getResponseHeaders().set("WWW-Authenticate",
                "Basic realm=\"" + this.realm + "\", charset=\"" + this.charset.name() + "\"");
        return new Retry(401);
    }

    /**
     * Si esas credenciales son validas.
     *
     * <p>Conviene compararlas en tiempo constante: un {@code equals} de {@code String} corta en la
     * primera diferencia, y eso deja medir cuantos caracteres se acerto.
     */
    public abstract boolean checkCredentials(String username, String password);
}
