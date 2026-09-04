// Bean de prueba para la persistencia XML. Publico y en su propio archivo porque XMLEncoder del
// JDK real solo puede introspeccionar e instanciar clases publicas.
public class XPunto {
    private int x;
    private int y;
    private String etiqueta;
    public int getX() { return x; }
    public void setX(int v) { x = v; }
    public int getY() { return y; }
    public void setY(int v) { y = v; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String v) { etiqueta = v; }
}
