package com.sun.net.httpserver;

/**
 * Decide si un pedido puede pasar, y con que identidad.
 *
 * <h2>Por que el resultado es un objeto y no un booleano</h2>
 *
 * <p>Porque autenticar tiene <strong>tres</strong> respuestas, no dos, y la tercera es la que hace
 * funcionar a HTTP. Un pedido puede venir bien ({@link Success}), venir mal sin arreglo posible
 * ({@link Failure}) o venir <em>sin credenciales todavia</em> — que no es un rechazo sino el primer
 * paso normal: el navegador manda el pedido pelado, el servidor contesta con un desafio, y recien
 * el segundo pedido trae la clave. Eso es {@link Retry}.
 *
 * <p>Confundir {@code Retry} con {@code Failure} rompe la autenticacion entera: el cliente nunca
 * recibe el desafio y nunca sabe que tenia que mandar algo.
 *
 * <p>La otra mitad de la respuesta es la <strong>identidad</strong>. {@link Success} lleva un
 * {@link HttpPrincipal} porque saber que alguien es legitimo no alcanza: el manejador necesita saber
 * quien es para decidir que puede hacer.
 */
public abstract class Authenticator {

    /** Para las implementaciones. */
    protected Authenticator() {
    }

    /**
     * El resultado de autenticar.
     *
     * <p>Abstracta con constructor {@code protected} y tres subclases fijas, que es la forma de
     * escribir un tipo suma en Java sin sellarlo: nadie de afuera puede agregar un cuarto caso, asi
     * que el servidor puede repartir por tipo con la certeza de haberlos cubierto todos.
     */
    public abstract static class Result {

        protected Result() {
        }
    }

    /** El pedido esta autenticado, y esta es la identidad. */
    public static class Success extends Result {

        private final HttpPrincipal principal;

        public Success(HttpPrincipal p) {
            this.principal = p;
        }

        /** Quien mando el pedido. */
        public HttpPrincipal getPrincipal() {
            return this.principal;
        }
    }

    /**
     * El pedido no esta autenticado y no hay nada que reintentar.
     *
     * <p>Distinta de {@link Retry}: aca las credenciales llegaron y estaban mal, o el recurso no es
     * accesible para nadie. Mandar un desafio seria invitar a probar de nuevo con lo mismo.
     */
    public static class Failure extends Result {

        private final int responseCode;

        public Failure(int responseCode) {
            this.responseCode = responseCode;
        }

        /** El codigo HTTP a devolver. */
        public int getResponseCode() {
            return this.responseCode;
        }
    }

    /**
     * Faltan credenciales: hay que desafiar al cliente y esperar que vuelva.
     *
     * <p>El caso normal del primer pedido. El manejador <strong>no</strong> corre: el servidor
     * contesta con el codigo y los encabezados de desafio que el autenticador ya dejo puestos en la
     * respuesta.
     */
    public static class Retry extends Result {

        private final int responseCode;

        public Retry(int responseCode) {
            this.responseCode = responseCode;
        }

        /** El codigo HTTP del desafio, tipicamente {@code 401}. */
        public int getResponseCode() {
            return this.responseCode;
        }
    }

    /**
     * Autentica el pedido.
     *
     * <p>Puede escribir encabezados en la respuesta —es como se manda el desafio de {@link Retry}—
     * pero no debe escribir el cuerpo ni cerrarla.
     */
    public abstract Result authenticate(HttpExchange exch);
}
