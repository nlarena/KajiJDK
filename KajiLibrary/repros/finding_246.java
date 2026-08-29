// Finding #246 - la consecuencia MEDIBLE de que KajiLibrary tape a boot/.
//
// run-headless bootea `KajiLibrary` primero y `boot/` como relleno, asi que para una clase que
// existe en las dos gana la de KajiLibrary. Y `KajiLibrary/java/lang/Integer` NO declara
// equals/hashCode (boot/ si), de modo que dos Integer con el mismo valor son distintos como
// claves: cada uno cae en su propio bucket.
//
// Correr las dos veces y comparar:
//   run-headless finding_246.class clavesInteger               -> con KajiLibrary adelante
//   run-headless finding_246.class clavesInteger --boot boot   -> solo boot/
//
// Esperado si la divergencia esta cerrada: 1 en los dos casos.
// Esperado hoy: 2 con KajiLibrary (dos claves iguales cuentan como dos), 1 con boot/.
public class finding_246 {

    // Dos Integer con el MISMO valor: si equals/hashCode existen, la segunda pisa a la primera
    // y el mapa queda con un solo par.
    public static int clavesInteger() {
        java.util.HashMap<Integer, String> m = new java.util.HashMap<Integer, String>();
        Integer a = Integer.valueOf(1000);
        Integer b = Integer.valueOf(1000);
        m.put(a, "primero");
        m.put(b, "segundo");
        return m.size();
    }

    // Contraprueba con String, que si tiene equals/hashCode en las dos copias: debe dar 1 siempre.
    public static int clavesString() {
        java.util.HashMap<String, String> m = new java.util.HashMap<String, String>();
        m.put("k", "primero");
        m.put("k", "segundo");
        return m.size();
    }

    // El equals directo, sin mapa de por medio: 1 = son iguales, 0 = no.
    public static int igualdadDirecta() {
        Integer a = Integer.valueOf(1000);
        Integer b = Integer.valueOf(1000);
        if (a.equals(b)) { return 1; }
        return 0;
    }
}
