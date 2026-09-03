package javax.print.attribute;

import java.io.Serializable;

// La clase de sintaxis de los atributos que son una resolucion de impresion: dos numeros, uno a lo
// ancho del papel (cross feed) y otro a lo largo (feed).
//
// Adentro se guarda todo en **dphi** -- puntos por cien pulgadas --, que es un entero, para que la
// comparacion sea exacta y no dependa de en que unidad se construyo. Las constantes DPI y DPCM son
// justamente el factor de conversion a dphi: 100 dphi = 1 dpi, 254 dphi = 1 dpcm. Esto es lo que
// hace que `new R(300, 300, DPI)` y `new R(300, 300, DPI)` sean iguales sin punto flotante.
//
// A diferencia del resto de las clases de sintaxis, su constructor es **public**, no protected.
public abstract class ResolutionSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 2706743076526672017L;

    private int crossFeedResolution;
    private int feedResolution;

    // Los dos factores a dphi. No son un enum: son el numero por el que se multiplica.
    public static final int DPI = 100;
    public static final int DPCM = 254;

    public ResolutionSyntax(int crossFeedResolution, int feedResolution, int units) {
        if (crossFeedResolution < 1) {
            throw new IllegalArgumentException("crossFeedResolution is < 1");
        }
        if (feedResolution < 1) {
            throw new IllegalArgumentException("feedResolution is < 1");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        this.crossFeedResolution = crossFeedResolution * units;
        this.feedResolution = feedResolution * units;
    }

    // Vuelta de dphi a la unidad pedida, redondeando al entero mas cercano.
    private static int convertFromDphi(int dphi, int units) {
        if (units < 1) {
            throw new IllegalArgumentException(": units is < 1");
        }
        int round = units / 2;
        return (dphi + round) / units;
    }

    // Los dos numeros juntos: [cross feed, feed].
    public int[] getResolution(int units) {
        int[] result = new int[2];
        result[0] = getCrossFeedResolution(units);
        result[1] = getFeedResolution(units);
        return result;
    }

    public int getCrossFeedResolution(int units) {
        return convertFromDphi(this.crossFeedResolution, units);
    }

    public int getFeedResolution(int units) {
        return convertFromDphi(this.feedResolution, units);
    }

    // "300x600 dpi". Con `unitsName` null se omite el sufijo y el espacio.
    public String toString(int units, String unitsName) {
        StringBuilder result = new StringBuilder();
        result.append(getCrossFeedResolution(units));
        result.append('x');
        result.append(getFeedResolution(units));
        if (unitsName != null) {
            result.append(' ');
            result.append(unitsName);
        }
        return result.toString();
    }

    // Orden parcial, no total: pide que **las dos** componentes sean menores o iguales. Dos
    // resoluciones como 300x600 y 600x300 no estan ordenadas entre si en ningun sentido.
    public boolean lessThanOrEquals(ResolutionSyntax other) {
        if (other == null) {
            throw new NullPointerException("other is null");
        }
        return this.crossFeedResolution <= other.crossFeedResolution
                && this.feedResolution <= other.feedResolution;
    }

    public boolean equals(Object object) {
        if (!(object instanceof ResolutionSyntax)) {
            return false;
        }
        ResolutionSyntax other = (ResolutionSyntax) object;
        return this.crossFeedResolution == other.crossFeedResolution
                && this.feedResolution == other.feedResolution;
    }

    // Los 16 bits bajos de cada componente, empaquetados. Choca para resoluciones enormes, pero es
    // el del JDK y hay que replicarlo: un hash distinto rompe cualquier tabla compartida.
    public int hashCode() {
        return (this.crossFeedResolution & 0x0000FFFF)
                | ((this.feedResolution & 0x0000FFFF) << 16);
    }

    // En dphi, la unidad interna: "30000x60000 dphi".
    public String toString() {
        return toString(1, "dphi");
    }

    protected int getCrossFeedResolutionDphi() {
        return this.crossFeedResolution;
    }

    protected int getFeedResolutionDphi() {
        return this.feedResolution;
    }
}
