package java.security;

// Lo mismo que `PrivilegedAction`, pero para acciones que pueden tirar una excepcion chequeada.
//
// Son dos interfaces y no una con `throws Exception` porque el caso comun —una accion que no tira
// nada— no tiene por que obligar a su llamador a escribir un `catch` vacio. La que tira se envuelve
// en `PrivilegedActionException`; la que no, no necesita envoltorio.
@FunctionalInterface
@Deprecated
public interface PrivilegedExceptionAction<T> {

    T run() throws Exception;
}
