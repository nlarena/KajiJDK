package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.Xid -- el identificador de una transaccion distribuida.
 *
 * <p>Tiene **tres** partes y no un numero, y ahi esta toda la idea: un identificador global que es el
 * mismo en todos los sistemas que participan, un calificador de rama que distingue la parte de cada
 * uno, y un formato que dice quien invento el identificador. Un entero suelto no serviria -- dos
 * gestores de transacciones distintos elegirian el mismo y no habria como notarlo.
 */
public interface Xid {

    /** El maximo de bytes del identificador global. */
    int MAXGTRIDSIZE = 64;

    /** El maximo de bytes del calificador de rama. */
    int MAXBQUALSIZE = 64;

    /** Quien definio el formato de este identificador. */
    int getFormatId();

    /** El identificador global, comun a todas las ramas. */
    byte[] getGlobalTransactionId();

    /** Que rama de esa transaccion es esta. */
    byte[] getBranchQualifier();
}
