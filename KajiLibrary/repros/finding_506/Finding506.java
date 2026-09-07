import javax.swing.JViewport;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

// #506 -- los modificadores de acceso de un miembro leido de un `.class` no se chequean.
//
// Da igual que el miembro sea `private`, `protected` o de paquete: si viene de otra unidad de
// compilacion, se puede usar desde cualquier lado. El JDK rechaza las seis lineas marcadas;
// nuestro javac las compila todas.
//
// La prueba usa `javax.swing.JViewport`, que esta en la biblioteca ya compilada: es la unica forma
// de que el miembro llegue leido de un `.class` y no del arbol del mismo archivo.
public class Finding506 {

    // --- falla en el JDK, compila aca ---------------------------------------------------------

    // Metodo protegido, llamado desde una clase que no es subclase.
    boolean metodoProtegido(JViewport vp) {
        return vp.computeBlit(0, 12, new Point(), new Point(), new Dimension(), new Rectangle());
    }

    // Campo protegido, leido desde una clase que no es subclase.
    boolean campoProtegido(JViewport vp) {
        return vp.isViewSizeSet;
    }

    // Campo protegido, escrito desde una clase que no es subclase.
    void escrituraDeCampoProtegido(JViewport vp) {
        vp.scrollUnderway = true;
    }

    // Metodo protegido heredado, invocado sobre una instancia ajena desde la subclase: tampoco
    // vale en Java, porque el acceso protegido entre paquetes es solo sobre `this` y los suyos.
    static class Subclase extends JViewport {
        boolean sobreOtro(JViewport ajeno) {
            return ajeno.computeBlit(0, 1, new Point(), new Point(), new Dimension(),
                    new Rectangle());
        }
    }

    // Privado de otro paquete.
    int campoPrivado(JViewport vp) {
        return vp.scrollMode;
    }

    // De paquete (sin modificador) de otro paquete: mismo caso.
    Object metodoDePaquete(JViewport vp) {
        return vp.createLayoutManager();
    }

    // --- anda en los dos, y esta aca para delimitar --------------------------------------------

    // La subclase sobre si misma: es el uso legal de `protected` entre paquetes.
    static class SubclaseSobreSi extends JViewport {
        boolean sobreMi() {
            return computeBlit(0, 1, new Point(), new Point(), new Dimension(), new Rectangle());
        }

        boolean campoPropio() {
            return isViewSizeSet;
        }
    }

    // Publico de otro paquete: siempre legal.
    int publicoAjeno(JViewport vp) {
        return vp.getScrollMode();
    }
}
