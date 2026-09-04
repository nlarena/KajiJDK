package com.sun.net.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * Un servidor HTTP chico, que viene con el JDK.
 *
 * <h2>Que es y que no</h2>
 *
 * <p>Es lo suficiente para exponer un endpoint, servir unos archivos o levantar un mock en una
 * prueba, sin traer un contenedor entero. No pretende ser eso: no tiene servlets, ni sesiones, ni
 * pool de conexiones configurable.
 *
 * <h2>El ejecutor, que es la decision que mas importa</h2>
 *
 * <p>Por omision es {@code null}, y eso significa que <strong>todos los pedidos se atienden en un
 * solo hilo</strong>, uno detras de otro. Anda para una prueba y es una trampa en cualquier otro
 * lado: un manejador lento bloquea a todos los demas. Ponerle un pool con
 * {@link #setExecutor} es lo primero que hay que hacer para uso real.
 *
 * <h2>Como se resuelven los contextos</h2>
 *
 * <p>Por prefijo mas largo. Con {@code /} y {@code /api} registrados, un pedido a {@code /api/x} va
 * al segundo. Es lo que permite tener un manejador general y excepciones mas especificas sin
 * ordenarlos a mano.
 *
 * <h2>Sin proveedor instalado</h2>
 *
 * <p>Los {@link #create} delegan en {@link HttpServerProvider}, que se busca por
 * {@link java.util.ServiceLoader}. Esta VM no trae ninguno, asi que tiran
 * {@link UnsupportedOperationException} con el motivo. El mecanismo esta entero: lo que falta es
 * alguien que se registre en el.
 */
public abstract class HttpServer {

    /** Para las implementaciones. */
    protected HttpServer() {
    }

    /** Un servidor sin ligar; hay que llamarle {@link #bind}. */
    public static HttpServer create() throws IOException {
        return HttpServerProvider.provider().createHttpServer(null, 0);
    }

    /**
     * Ligado a {@code addr}, con esa cantidad de conexiones en espera.
     *
     * @param backlog {@code 0} o menos deja el valor del sistema
     */
    public static HttpServer create(InetSocketAddress addr, int backlog) throws IOException {
        return HttpServerProvider.provider().createHttpServer(addr, backlog);
    }

    /**
     * Ligado, con un contexto y sus filtros ya puestos.
     *
     * <p>El atajo para el caso comun: crear, registrar una ruta y arrancar en una sola linea.
     *
     * @throws NullPointerException si falta la ruta o el manejador
     * @throws IllegalArgumentException si la ruta no es absoluta
     */
    public static HttpServer create(InetSocketAddress addr, int backlog, String path,
            HttpHandler handler, Filter... filters) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        HttpServer s = create(addr, backlog);
        HttpContext c = s.createContext(path, handler);
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == null) {
                throw new NullPointerException("un filtro es null");
            }
            c.getFilters().add(filters[i]);
        }
        return s;
    }

    /**
     * Liga el servidor a una direccion.
     *
     * @throws java.net.BindException si el puerto ya esta tomado
     */
    public abstract void bind(InetSocketAddress addr, int backlog) throws IOException;

    /**
     * Arranca a atender, en un hilo aparte.
     *
     * <p>No bloquea, que es la otra mitad de por que el ejecutor importa: quien llama sigue con lo
     * suyo y no se entera de si hay un hilo o veinte atendiendo.
     */
    public abstract void start();

    /**
     * Quien corre los manejadores; {@code null} vuelve al hilo unico.
     *
     * <p>Ver la nota de la clase: dejarlo en {@code null} es la configuracion por omision y casi
     * nunca la que se quiere.
     */
    public abstract void setExecutor(Executor executor);

    /** El ejecutor puesto, o {@code null}. */
    public abstract Executor getExecutor();

    /**
     * Deja de atender, esperando hasta {@code delay} segundos a los pedidos en curso.
     *
     * <p>Los pedidos que sigan abiertos despues de ese plazo se cortan. Un {@code 0} corta todo
     * enseguida.
     */
    public abstract void stop(int delay);

    /**
     * Registra una ruta con su manejador.
     *
     * @throws IllegalArgumentException si la ruta es invalida o ya estaba registrada
     */
    public abstract HttpContext createContext(String path, HttpHandler handler);

    /**
     * Registra una ruta sin manejador todavia.
     *
     * <p>Sirve para configurar filtros y autenticador primero y poner el manejador despues, con
     * {@link HttpContext#setHandler}. Un pedido que llegue antes de eso da error.
     */
    public abstract HttpContext createContext(String path);

    /** Saca la ruta. */
    public abstract void removeContext(String path) throws IllegalArgumentException;

    /** Saca ese contexto. */
    public abstract void removeContext(HttpContext context);

    /**
     * La direccion donde escucha.
     *
     * <p>Vale consultarla aunque uno haya elegido el puerto: con el puerto {@code 0} lo elige el
     * sistema, y esta es la unica forma de saber cual toco.
     */
    public abstract InetSocketAddress getAddress();
}
