package jdk.incubator.vector;

/**
 * El mensaje que comparten las operaciones que necesitan los intrinsecos de la VM. No es API.
 *
 * <p>Esta en un solo lugar y no repetido en cada clase por una razon concreta: las clases de este
 * paquete forman una jerarquia --{@code Vector}, {@code AbstractVector}, {@code IntVector}-- y una
 * constante con el mismo nombre en varios niveles se tapa entre si, que es justo lo que no se quiere
 * de un texto que tiene que ser identico en todos lados.
 */
final class Msg {

    /** Por que una operacion de vector no puede funcionar en esta biblioteca. */
    static final String NO_HAY =
            "el API de vectores se apoya en intrinsecos de la VM --cada operacion se reemplaza por "
            + "una instruccion vectorial de la maquina-- y esta VM no los tiene; sin ellos no hay "
            + "forma de crear ni de operar un vector";

    private Msg() {
    }
}
