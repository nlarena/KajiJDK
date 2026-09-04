package com.sun.management;

/**
 * El sistema operativo, con lo que la interfaz estandar no se anima a prometer.
 *
 * <h2>Por que hay una version ampliada</h2>
 *
 * <p>Porque {@link java.lang.management.OperatingSystemMXBean} solo declara lo que existe en
 * <strong>todo</strong> sistema donde corra una JVM: el nombre, la arquitectura, la cantidad de
 * procesadores. La memoria fisica, el intercambio y el tiempo de CPU del proceso no estan ahi
 * porque no en todos lados se pueden medir.
 *
 * <p>Esta interfaz los agrega. El precio es que ya no es portable: quien la use tiene que
 * comprobar con {@code instanceof} que el bean de la plataforma sea de este tipo.
 *
 * <h2>Los pares de metodos que parecen repetidos</h2>
 *
 * <p>{@link #getFreePhysicalMemorySize} y {@link #getFreeMemorySize} devuelven lo mismo, y lo mismo
 * pasa con los otros dos pares. El primero de cada par es el nombre viejo, que quedo como
 * {@code default} delegando en el nuevo; el segundo es el que hay que implementar.
 *
 * <p>El renombre no fue cosmetico: dentro de un contenedor, "memoria fisica" es una mentira —lo que
 * el proceso puede usar es el limite del contenedor, no lo que tiene la maquina—. Los nombres
 * nuevos dicen "memoria" a secas justamente para no prometer de donde sale.
 *
 * @since 1.5
 */
public interface OperatingSystemMXBean extends java.lang.management.OperatingSystemMXBean {

    /**
     * La memoria virtual que el proceso tiene reservada, en bytes.
     *
     * @return los bytes, o {@code -1} si no se puede medir
     */
    long getCommittedVirtualMemorySize();

    /**
     * El tamano total del area de intercambio, en bytes.
     *
     * @return los bytes
     */
    long getTotalSwapSpaceSize();

    /**
     * Cuanto queda libre del area de intercambio, en bytes.
     *
     * @return los bytes
     */
    long getFreeSwapSpaceSize();

    /**
     * El tiempo de CPU consumido por el proceso, en nanosegundos.
     *
     * <p>La precision puede ser mucho peor que un nanosegundo; la unidad solo fija la escala.
     *
     * @return los nanosegundos, o {@code -1} si no se puede medir
     */
    long getProcessCpuTime();

    /**
     * La memoria libre, en bytes.
     *
     * @return los bytes
     * @deprecated El nombre promete memoria fisica de la maquina, que dentro de un contenedor no
     *     es lo que el proceso puede usar. Usar {@link #getFreeMemorySize}.
     */
    @Deprecated(since = "14")
    default long getFreePhysicalMemorySize() {
        return getFreeMemorySize();
    }

    /**
     * La memoria libre, en bytes.
     *
     * @return los bytes
     */
    long getFreeMemorySize();

    /**
     * La memoria total, en bytes.
     *
     * @return los bytes
     * @deprecated Por la misma razon que {@link #getFreePhysicalMemorySize}. Usar
     *     {@link #getTotalMemorySize}.
     */
    @Deprecated(since = "14")
    default long getTotalPhysicalMemorySize() {
        return getTotalMemorySize();
    }

    /**
     * La memoria total, en bytes.
     *
     * @return los bytes
     */
    long getTotalMemorySize();

    /**
     * La carga de CPU de todo el sistema, entre 0.0 y 1.0.
     *
     * @return la carga, o un valor negativo si no se pudo medir
     * @deprecated Usar {@link #getCpuLoad}, que es el mismo valor con un nombre que no sugiere que
     *     sea distinto de la carga del proceso por decir "system".
     */
    @Deprecated(since = "14")
    default double getSystemCpuLoad() {
        return getCpuLoad();
    }

    /**
     * La carga de CPU de todo el sistema, entre 0.0 y 1.0.
     *
     * <p>El valor es un promedio sobre el intervalo desde la consulta anterior. La primera consulta
     * no tiene intervalo del cual promediar y por eso devuelve un valor negativo.
     *
     * @return la carga, o un valor negativo si no se pudo medir
     */
    double getCpuLoad();

    /**
     * La carga de CPU que causa este proceso, entre 0.0 y 1.0.
     *
     * <p>Es fraccion de <strong>toda</strong> la CPU disponible: en una maquina de ocho nucleos, un
     * proceso que satura un nucleo da alrededor de 0.125 y no 1.0.
     *
     * @return la carga, o un valor negativo si no se pudo medir
     */
    double getProcessCpuLoad();
}
