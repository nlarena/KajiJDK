package java.sql;

/**
 * KajiLibrary's java.sql.RowId -- la direccion de una fila dentro de la base.
 *
 * <p>Es el identificador mas rapido que hay para volver a una fila --mas que la clave primaria,
 * porque suele ser la posicion fisica-- y por eso mismo el menos confiable: **no es estable**. Puede
 * cambiar si la fila se mueve, y el ciclo de vida depende de la base. Sirve dentro de una
 * transaccion, no para guardar.
 *
 * <p>Declara `equals`, `hashCode` y `toString` aunque los herede de `Object`: es la forma de exigir
 * que se implementen de verdad, porque comparar dos identificadores de fila por identidad de objeto
 * seria siempre falso.
 */
public interface RowId {

    /** Si los dos identifican la misma fila. */
    boolean equals(Object obj);

    /** Los bytes del identificador. */
    byte[] getBytes();

    /** Una forma legible. */
    String toString();

    int hashCode();
}
