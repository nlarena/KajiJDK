// Objetivo de un setOwner: el documento tiene que poder llamarle algo al owner.
public class XDuenio {
    private int veces;
    public int getVeces() { return veces; }
    public void setVeces(int v) { veces = v; }
    public void hacer() { veces++; }
}
