// Bean mutable que ADEMAS declara equals. Es la forma que destapa la regla de `mutatesTo`: si el
// codificador le creyera a `equals` para un objeto mutable, la bolsa recien creada (vacia) nunca
// seria igual a la que tiene contenido, y volveria a crearla para siempre.
public class XBolsa {
    private String contenido = "";
    public String getContenido() { return contenido; }
    public void setContenido(String v) { contenido = v; }
    public boolean equals(Object o) {
        return o instanceof XBolsa && ((XBolsa) o).contenido.equals(this.contenido);
    }
    public int hashCode() { return contenido.hashCode(); }
}
