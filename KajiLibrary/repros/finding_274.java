// #274: un nombre CALIFICADO no se reconoce como tipo dentro de una expresion.
//
// En una declaracion resuelve; en una expresion el compilador lee `java.lang.reflect.Modifier`
// como una cadena de accesos a campo que arranca en una variable llamada `java`, y el error lo
// dice: "simbolo: variable java". Es la reclasificacion de nombres ambiguos de JLS 6.5.2 --
// frente a `a.b.c.d` hay que probar prefijos de izquierda a derecha hasta que uno resuelva como
// tipo, y recien lo que sigue es acceso a miembro.
//
// El JDK 25 compila este archivo sin decir nada.
public class finding_274 {

    // Resuelve: un nombre calificado en una DECLARACION.
    static java.lang.reflect.Method campo;

    // No resuelve: el mismo nombre en una EXPRESION, leyendo un campo estatico.
    public static int lectura() {
        return java.lang.reflect.Modifier.PUBLIC;
    }

    // Tampoco: el mismo nombre en una EXPRESION, llamando un metodo estatico.
    public static boolean llamada() {
        return java.lang.reflect.Modifier.isPublic(1);
    }
}
