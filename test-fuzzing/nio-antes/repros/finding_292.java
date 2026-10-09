// Repro de #292 - `AbstractList` y `AbstractMap` no tenian `equals` ni `hashCode`, y `Hashtable`
// tampoco.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_292.java
//   bin\run-headless.exe KajiLibrary\repros\finding_292.class listas
//
// ANTES compilaba todo y daba las respuestas equivocadas, que es el peor de los dos modos:
//
//   new ArrayList(...).equals(new ArrayList(...))   con el mismo contenido -> false
//   new HashMap(...).equals(new LinkedHashMap(...)) con el mismo contenido -> false
//   List.of("x").equals(List.of("x"))                                      -> false
//
// Lo que se heredaba de Object es la igualdad por IDENTIDAD. Con eso ninguna List ni ningun Map
// servia de clave de otro mapa, `assertEquals` sobre listas no podia funcionar, y la simetria que
// el contrato exige entre implementaciones distintas -- un ArrayList tiene que ser igual a un
// LinkedList con los mismos elementos -- no existia.
//
// POR QUE NO SE VIO ANTES: el diff de API compara firmas, y `equals`/`hashCode` estan en la lista
// de miembros de Object que el medidor excluye a proposito. El conteo no podia verlo. Es el
// segundo punto ciego de la medicion por firmas; el primero, el cuerpo vacio, lo dejaron los cinco
// stubs de `Arrays` en la tanda anterior.
//
// Quienes SI lo tenian, y por eso el agujero paso desapercibido: AbstractSet (y con el HashSet,
// TreeSet, LinkedHashSet), Vector, EnumMap, y FixedMap/FixedEntry (los de `Map.of`).
//
// AHORA: escrito en AbstractList, en AbstractMap y en Hashtable -- las tres raices que faltaban --
// con las formulas que la especificacion fija al detalle. Son fijas justamente porque el contrato
// exige que dos colecciones iguales de clases distintas den el mismo numero.
//
// `listas` -> 127, `mapas` -> 127, `hashes` -> 7, `comoClave` -> 3.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class finding_292 {

    private static List<String> unaLista(List<String> destino) {
        destino.add("a");
        destino.add("b");
        return destino;
    }

    private static Map<String, String> unMapa(Map<String, String> destino) {
        destino.put("k", "v");
        return destino;
    }

    // Igualdad de listas, incluida la simetria entre implementaciones distintas.
    public static int listas() {
        List<String> arr = unaLista(new ArrayList<String>());
        List<String> otra = unaLista(new ArrayList<String>());
        List<String> enl = unaLista(new LinkedList<String>());
        List<String> vec = unaLista(new Vector<String>());
        List<String> fija = List.of("a", "b");

        int r = 0;
        r = r + (arr.equals(otra) ? 1 : 0);
        r = r + (arr.equals(enl) ? 2 : 0);          // ArrayList vs LinkedList
        r = r + (enl.equals(arr) ? 4 : 0);          // y al reves: tiene que ser simetrico
        r = r + (arr.equals(vec) ? 8 : 0);
        r = r + (fija.equals(arr) ? 16 : 0);        // FixedList, la de List.of
        r = r + (arr.equals(fija) ? 32 : 0);
        r = r + (arr.equals(List.of("a")) ? 0 : 64);   // distinto largo: NO iguales
        return r;
    }

    // Lo mismo para mapas. Hashtable entra porque desciende de Dictionary y no de AbstractMap,
    // asi que necesitaba su propia copia.
    public static int mapas() {
        Map<String, String> hm = unMapa(new HashMap<String, String>());
        Map<String, String> lhm = unMapa(new LinkedHashMap<String, String>());
        Map<String, String> tm = unMapa(new TreeMap<String, String>());
        Map<String, String> ht = unMapa(new Hashtable<String, String>());
        Map<String, String> fijo = Map.of("k", "v");

        int r = 0;
        r = r + (hm.equals(lhm) ? 1 : 0);
        r = r + (hm.equals(tm) ? 2 : 0);
        r = r + (tm.equals(hm) ? 4 : 0);
        r = r + (hm.equals(ht) ? 8 : 0);
        r = r + (ht.equals(hm) ? 16 : 0);
        r = r + (fijo.equals(hm) ? 32 : 0);
        r = r + (hm.equals(Map.of("k", "otro")) ? 0 : 64);   // mismo par, otro valor
        return r;
    }

    // El hash tiene que coincidir entre implementaciones, o de nada sirve el equals: es lo que
    // permite usar una coleccion como clave de un mapa.
    public static int hashes() {
        int r = 0;
        r = r + (unaLista(new ArrayList<String>()).hashCode()
                == unaLista(new LinkedList<String>()).hashCode() ? 1 : 0);
        r = r + (unMapa(new HashMap<String, String>()).hashCode()
                == unMapa(new TreeMap<String, String>()).hashCode() ? 2 : 0);
        // La formula de List esta especificada: 31*h + hash(e), arrancando en 1.
        List<String> l = new ArrayList<String>();
        l.add("a");
        int esperado = 31 * 1 + "a".hashCode();
        r = r + (l.hashCode() == esperado ? 4 : 0);
        return r;
    }

    // La consecuencia practica de todo lo anterior.
    public static int comoClave() {
        Map<List<String>, String> por = new HashMap<List<String>, String>();
        por.put(unaLista(new ArrayList<String>()), "si");
        int r = 0;
        r = r + (por.get(unaLista(new ArrayList<String>())) != null ? 1 : 0);
        r = r + (por.get(unaLista(new LinkedList<String>())) != null ? 2 : 0);
        return r;
    }
}
