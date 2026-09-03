package java.util.prefs;

// De donde salen las dos raices.
//
// Es el unico punto de extension del paquete: `Preferences.userRoot()` y `Preferences.systemRoot()`
// no hacen otra cosa que preguntarle a la fabrica. Quien quiera guardar las preferencias en una
// base de datos, en un servidor o en memoria implementa esta interfaz y no toca nada mas.
//
// Las dos raices se piden por separado y se esperan **estables**: llamar dos veces tiene que dar el
// mismo objeto, porque `AbstractPreferences.isUserNode()` compara la raiz por identidad.
public interface PreferencesFactory {

    // La raiz del arbol del sistema, compartida por todos los usuarios de la maquina.
    Preferences systemRoot();

    // La raiz del arbol del usuario que corre la VM.
    Preferences userRoot();
}
