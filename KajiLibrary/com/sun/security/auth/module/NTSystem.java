package com.sun.security.auth.module;

/**
 * Quien es el usuario del proceso, segun Windows: nombre, dominio y los SID.
 *
 * <h2>Que es un SID y por que no es un numero</h2>
 *
 * <p>Un SID identifica una cuenta de forma unica <strong>y para siempre</strong>: borrar un usuario
 * y crear otro con el mismo nombre da SIDs distintos, que es lo que impide que el nuevo herede los
 * permisos del viejo. Por eso Windows guarda SIDs en las listas de control de acceso y no nombres.
 *
 * <p>Se escribe {@code S-1-5-21-...-1001}: la autoridad, el dominio y el identificador relativo
 * dentro de ese dominio. El nombre es solo una etiqueta encima de eso.
 *
 * <h2>Por que el constructor falla en vez de contestar algo</h2>
 *
 * <p>Porque no hay forma de obtener un SID en Java puro. Vienen de {@code OpenProcessToken} y
 * {@code GetTokenInformation}, que son llamadas a la API de Windows.
 *
 * <p>Y un SID inventado es peor que ninguno, por la misma razon que un uid inventado: se usa para
 * comparar contra listas de control de acceso, y una comparacion contra un valor fabricado puede
 * dar verdadera. Por eso falla de entrada.
 *
 * <p>{@link #getImpersonationToken} es todavia mas claro: devuelve un descriptor del sistema
 * operativo, un puntero. No hay valor honesto que devolver sin el sistema operativo del otro lado.
 *
 * @since 1.4
 */
public class NTSystem {

    /**
     * Consulta a Windows quien es el usuario del proceso.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca: los SID vienen de la API
     *     de Windows y esta VM no tiene como llamarla
     */
    public NTSystem() {
        throw new UnsupportedOperationException(
                "los datos de NTSystem vienen de OpenProcessToken/GetTokenInformation, que esta VM "
                + "no puede llamar; un SID inventado se compara contra listas de control de acceso "
                + "y puede dar verdadero, asi que fallar es lo unico defendible");
    }

    /**
     * El nombre del usuario.
     *
     * @return el nombre, o {@code null} si no se pudo obtener
     */
    public String getName() {
        return null;
    }

    /**
     * El dominio al que pertenece.
     *
     * @return el dominio, o {@code null} si no se pudo obtener
     */
    public String getDomain() {
        return null;
    }

    /**
     * El SID del dominio.
     *
     * @return el SID, o {@code null} si no se pudo obtener
     */
    public String getDomainSID() {
        return null;
    }

    /**
     * El SID del usuario.
     *
     * @return el SID, o {@code null} si no se pudo obtener
     */
    public String getUserSID() {
        return null;
    }

    /**
     * El SID del grupo principal.
     *
     * @return el SID, o {@code null} si no se pudo obtener
     */
    public String getPrimaryGroupID() {
        return null;
    }

    /**
     * Los SID de los demas grupos.
     *
     * @return los SID, o {@code null} si no se pudieron obtener
     */
    public String[] getGroupIDs() {
        return null;
    }

    /**
     * El descriptor del token de suplantacion del proceso.
     *
     * <p>Es un puntero del sistema operativo, no un dato: quien lo recibe se lo pasa de vuelta a
     * Windows. Sin Windows del otro lado no hay ningun valor que signifique algo.
     *
     * @return el descriptor
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public synchronized long getImpersonationToken() {
        throw new UnsupportedOperationException(
                "el token de suplantacion es un descriptor del sistema operativo; no hay valor que "
                + "devolver sin Windows del otro lado");
    }
}
