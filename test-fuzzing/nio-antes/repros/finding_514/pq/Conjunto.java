// #514 -- el nombre binario de un tipo anidado (`Externa$Anidada`) en el FUENTE anda cuando el
// tipo se lee de un `.class`, y NO anda cuando el tipo viene en el mismo lote de compilacion.
//
//   bin/javac.exe --emit pq/Conjunto.java pq/Constantes.java     # EN LOTE
//     pq/Constantes.java:6: error: no se encuentra el simbolo: Conjunto$Atributo
//
//   bin/javac.exe --emit pq/Conjunto.java                        # primero
//   bin/javac.exe --emit -cp . pq/Constantes.java                # despues, contra el .class
//     compila
//
// OJO: aca el javac del JDK NO es el arbitro. El `$` no es el separador de tipos anidados en el
// fuente de Java, asi que el JDK rechaza las dos formas ("cannot find symbol"). Escribir nombres
// binarios en el fuente es una EXTENSION nuestra, y esta biblioteca la usa en todas partes
// (`import java.text.Format$Field;`). El defecto es que la extension no es consistente consigo
// misma: el mismo nombre resuelve o no segun de donde venga el tipo.
//
package pq;

public interface Conjunto {

    interface Atributo {
    }

    Object obtener(Object clave);
}
