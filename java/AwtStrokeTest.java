import java.awt.BasicStroke;

/**
 * java.awt.BasicStroke: el pincel como valor.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25, no de leer la documentacion, y las cuatro
 * que importan son al reves de lo intuitivo: un ancho de cero es valido, el limite de inglete solo
 * se valida con JOIN_MITER, un patron de guiones con ALGUN cero es valido, y los constructores
 * cortos usan CAP_SQUARE y no CAP_BUTT.
 *
 * <p>Los tres hashCode son literales sacados del JDK. Estan a proposito: reproducen la formula bit a
 * bit, que es lo que hace que un BasicStroke sirva de clave en una cache compartida entre
 * bibliotecas.
 */
public class AwtStrokeTest {

    public static int run() {
        int i = 0;

        // -- las constantes son parte del formato: un numero distinto es otra punta
        if (BasicStroke.CAP_BUTT != 0) { return i; } i++;
        if (BasicStroke.CAP_ROUND != 1) { return i; } i++;
        if (BasicStroke.CAP_SQUARE != 2) { return i; } i++;
        if (BasicStroke.JOIN_MITER != 0) { return i; } i++;
        if (BasicStroke.JOIN_ROUND != 1) { return i; } i++;
        if (BasicStroke.JOIN_BEVEL != 2) { return i; } i++;

        // -- el pincel por omision
        BasicStroke porOmision = new BasicStroke();
        if (porOmision.getLineWidth() != 1.0f) { return i; } i++;
        // CAP_SQUARE, no CAP_BUTT: es lo que sorprende.
        if (porOmision.getEndCap() != BasicStroke.CAP_SQUARE) { return i; } i++;
        if (porOmision.getLineJoin() != BasicStroke.JOIN_MITER) { return i; } i++;
        if (porOmision.getMiterLimit() != 10.0f) { return i; } i++;
        if (porOmision.getDashArray() != null) { return i; } i++;
        if (porOmision.getDashPhase() != 0.0f) { return i; } i++;

        BasicStroke soloAncho = new BasicStroke(3f);
        if (soloAncho.getLineWidth() != 3.0f) { return i; } i++;
        if (soloAncho.getEndCap() != BasicStroke.CAP_SQUARE) { return i; } i++;
        if (soloAncho.getLineJoin() != BasicStroke.JOIN_MITER) { return i; } i++;
        if (soloAncho.getMiterLimit() != 10.0f) { return i; } i++;

        BasicStroke tres = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        if (tres.getMiterLimit() != 10.0f) { return i; } i++;
        if (tres.getEndCap() != BasicStroke.CAP_ROUND) { return i; } i++;
        if (tres.getLineJoin() != BasicStroke.JOIN_BEVEL) { return i; } i++;

        // -- el patron de guiones se copia al entrar y al salir
        float[] patron = new float[] {5f, 3f};
        BasicStroke conGuiones = new BasicStroke(1f, 0, 0, 10f, patron, 2f);
        if (conGuiones.getDashArray().length != 2) { return i; } i++;
        if (conGuiones.getDashArray()[0] != 5.0f) { return i; } i++;
        patron[0] = 99f;
        if (conGuiones.getDashArray()[0] != 5.0f) { return i; } i++;
        conGuiones.getDashArray()[0] = 99f;
        if (conGuiones.getDashArray()[0] != 5.0f) { return i; } i++;
        if (conGuiones.getDashPhase() != 2.0f) { return i; } i++;

        // -- hashCode: la formula del JDK, bit a bit
        if (porOmision.hashCode() != -778043330) { return i; } i++;
        if (soloAncho.hashCode() != 417333310) { return i; } i++;
        if (conGuiones.hashCode() != -1644167168) { return i; } i++;

        // -- equals
        if (!porOmision.equals(new BasicStroke())) { return i; } i++;
        if (porOmision.hashCode() != new BasicStroke().hashCode()) { return i; } i++;
        if (!conGuiones.equals(new BasicStroke(1f, 0, 0, 10f, new float[] {5f, 3f}, 2f))) { return i; } i++;
        if (conGuiones.equals(new BasicStroke(1f, 0, 0, 10f, new float[] {5f, 4f}, 2f))) { return i; } i++;
        // Una fase distinta es otro pincel.
        if (conGuiones.equals(new BasicStroke(1f, 0, 0, 10f, new float[] {5f, 3f}, 1f))) { return i; } i++;
        // Con guiones nunca es igual a sin guiones.
        if (soloAncho.equals(conGuiones)) { return i; } i++;
        if (conGuiones.equals(soloAncho)) { return i; } i++;
        if (porOmision.equals(null)) { return i; } i++;
        if (porOmision.equals("x")) { return i; } i++;
        if (!porOmision.equals(porOmision)) { return i; } i++;

        // ======================================================================================
        // las validaciones, y sobre todo las que NO son
        // ======================================================================================
        boolean threw = false;
        try { new BasicStroke(-1f); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Un ancho de cero SI vale: es "la linea mas fina que se pueda dibujar".
        if (new BasicStroke(0f).getLineWidth() != 0.0f) { return i; } i++;

        threw = false;
        try { new BasicStroke(1f, 9, 0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new BasicStroke(1f, -1, 0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new BasicStroke(1f, 0, 9); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // El limite de inglete se valida SOLO con JOIN_MITER.
        threw = false;
        try { new BasicStroke(1f, 0, BasicStroke.JOIN_MITER, 0.5f); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Con JOIN_ROUND el mismo valor pasa sin protestar, porque ahi no se usa.
        if (new BasicStroke(1f, 0, BasicStroke.JOIN_ROUND, 0.5f).getMiterLimit() != 0.5f) { return i; } i++;
        if (new BasicStroke(1f, 0, BasicStroke.JOIN_BEVEL, 0.5f).getMiterLimit() != 0.5f) { return i; } i++;

        // Un patron con TODOS los tramos en cero no avanza nunca y se rechaza.
        threw = false;
        try { new BasicStroke(1f, 0, 0, 10f, new float[] {0f, 0f}, 0f); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new BasicStroke(1f, 0, 0, 10f, new float[0], 0f); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Con ALGUN cero, en cambio, si vale: es como se piden puntos.
        if (new BasicStroke(1f, 0, 0, 10f, new float[] {0f, 3f}, 0f).getDashArray()[0] != 0f) { return i; } i++;

        threw = false;
        try { new BasicStroke(1f, 0, 0, 10f, new float[] {-1f, 3f}, 0f); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new BasicStroke(1f, 0, 0, 10f, new float[] {5f, 3f}, -1f); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Sin patron, una fase negativa no se mira siquiera.
        if (new BasicStroke(1f, 0, 0, 10f, null, -1f).getDashPhase() != -1.0f) { return i; } i++;
        if (new BasicStroke(1f, 0, 0, 10f, null, 0f).getDashArray() != null) { return i; } i++;

        // -- es un Stroke, y eso es parte de su identidad
        if (!(porOmision instanceof java.awt.Stroke)) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
