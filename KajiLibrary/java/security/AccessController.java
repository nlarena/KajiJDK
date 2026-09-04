package java.security;

// Correr un bloque de codigo con los privilegios de quien lo escribio, no con los de quien lo
// llamo.
//
// ===============================================================================================
// QUE SIGNIFICABA `doPrivileged`, Y QUE HACE HOY
// ===============================================================================================
//
// El chequeo de acceso normal exige que **todos** los dominios de la pila tengan el permiso. Eso
// deja a una biblioteca confiable sin poder abrir su propio archivo de configuracion si la llamo
// un applet. `doPrivileged` cortaba la pila ahi: de ese marco para arriba no se miraba mas, asi
// que la biblioteca podia hacer lo suyo aunque su llamador no pudiera.
//
// Era la construccion mas delicada del modelo, porque un `doPrivileged` que ademas usa un dato que
// vino del llamador —un nombre de archivo, por ejemplo— le presta sus privilegios a quien mando el
// dato. Ese es el "diputado confundido" clasico.
//
// Hoy no corta nada: desde JDK 24 no hay `SecurityManager` y nunca hubo control de acceso en
// KajiJDK. `doPrivileged` **ejecuta la accion y devuelve lo que devuelva**, que es exactamente lo
// que hace el JDK 25 —verificado contra el— y lo unico honesto: no hay privilegios que elevar
// porque no hay ninguno que restringir.
//
// `checkPermission`, en cambio, **tira**. Es la asimetria importante: correr codigo sin control es
// lo mismo que correrlo, pero preguntar "¿tengo este permiso?" y contestar que si sin haberlo
// mirado seria fabricar una autorizacion. El JDK 25 tira `AccessControlException` y aca tambien.
@Deprecated
public final class AccessController {

    // Estatica pura: no se instancia.
    private AccessController() {
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context,
                                     Permission... perms) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action,
                                                 AccessControlContext context,
                                                 Permission... perms) {
        return action.run();
    }

    // La variante para acciones que tiran chequeadas: lo que salga se envuelve.
    //
    // Solo las **chequeadas** se envuelven. Una `RuntimeException` sale tal cual, y esa distincion
    // es del contrato: envolver todo obligaria a desenvolver hasta los errores de programacion.
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
                                     AccessControlContext context)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
                                     AccessControlContext context, Permission... perms)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action,
                                                 AccessControlContext context,
                                                 Permission... perms)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    // El contexto de la ejecucion actual.
    //
    // Devuelve uno **vacio** y no null: la VM no expone los dominios de la pila, asi que no hay
    // dominios que enumerar. Un contexto vacio es la verdad —"no hay nada anotado aca"— y ademas
    // es lo que evita que quien lo guarde para usarlo despues se encuentre con un null.
    public static AccessControlContext getContext() {
        return new AccessControlContext(new ProtectionDomain[0]);
    }

    // Siempre tira. Ver la cabecera: contestar que si sin haber mirado nada seria fabricar una
    // autorizacion.
    public static void checkPermission(Permission perm) throws AccessControlException {
        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }
        throw new AccessControlException("checking permissions is not supported", perm);
    }
}
