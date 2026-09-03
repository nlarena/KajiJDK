package java.util.prefs;

// La fabrica por omision: dos {@link AlmacenDeArchivos}, uno por arbol.
//
// DONDE. El JDK usa el registro en Windows y `~/.java/.userPrefs` en POSIX. Aca no hay ninguno de
// los dos: no hay acceso al registro, y `user.home`, `user.dir` y `java.io.tmpdir` valen `null`
// --igual que todo `System.getenv`-- asi que no hay un directorio del usuario que nombrar. Lo que
// si funciona es el sistema de archivos por rutas **relativas** al directorio de trabajo del
// proceso, y ahi es donde va: `.java/.userPrefs` y `.java/.systemPrefs`. Es el mismo arbol que el
// del JDK bajo POSIX menos el prefijo del hogar, que es lo que no tenemos.
//
// Se puede mover con `java.util.prefs.userRoot` y `java.util.prefs.systemRoot`, que son las mismas
// propiedades que reconoce el JDK y con el mismo significado: el directorio **padre** dentro del
// cual se crea `.userPrefs` o `.systemPrefs`.
//
// Las dos raices se construyen una sola vez y se guardan porque
// {@link AbstractPreferences#isUserNode} compara la raiz por identidad: una fabrica que devolviera
// un objeto nuevo en cada llamada haria que `isUserNode()` diera `false` sobre el arbol del
// usuario.
final class FabricaDeArchivos implements PreferencesFactory {

    private final AlmacenDeArchivos usuario;
    private final AlmacenDeArchivos sistema;

    FabricaDeArchivos() {
        usuario = new AlmacenDeArchivos(padre("java.util.prefs.userRoot") + "/.userPrefs");
        sistema = new AlmacenDeArchivos(padre("java.util.prefs.systemRoot") + "/.systemPrefs");
    }

    private static String padre(String propiedad) {
        String v = System.getProperty(propiedad);
        return (v == null || v.length() == 0) ? ".java" : v;
    }

    public Preferences userRoot() {
        return usuario;
    }

    public Preferences systemRoot() {
        return sistema;
    }
}
