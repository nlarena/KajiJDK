package java.time.format;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalQueries;
import java.util.Set;

// El NOMBRE de la zona: "Pacific Standard Time", "hora estandar del Pacifico".
//
// ===============================================================================================
// POR QUE ESTO SE PUEDE Y LA TABLA DE NOMBRES NO HACE FALTA
// ===============================================================================================
//
// Un nombre de zona es dato del CLDR: una tabla por zona y por idioma, y ademas por si el instante
// cae en horario de verano o no. Esta biblioteca no la trae, y por eso las cuatro formas de
// `appendZoneText` estuvieron ausentes: escribir `Europe/Paris` donde va un nombre no es estar
// incompleto, es decir "este es el nombre" y que no lo sea.
//
// Lo que cambio es una medicion, no una decision. `ZoneId.of("Europe/Madrid")` TIRA en esta
// biblioteca --no hay base de zonas: ver `ZoneId.getAvailableZoneIds`, que devuelve un conjunto
// vacio-- asi que la unica zona que un formateador puede llegar a recibir aca es un `ZoneOffset`. Y
// para un `ZoneOffset` el JDK no usa ninguna tabla: escribe el identificador tal cual, `+05:00`, o
// `Z` para el cero, en cualquier idioma y en cualquier estilo. Medido contra el JDK 25 en los seis
// locales de esta biblioteca y en los dos estilos.
//
// **Entonces esto es completo por lo que la biblioteca puede representar, no por lo que la API
// promete.** El dia que `ZoneId.of` acepte identificadores de region, esta pieza queda mintiendo
// para toda zona con nombre y hay que traer la tabla del CLDR. Queda escrito aca para que ese dia se
// vea de una.
//
// `appendGenericZoneText` es la version que no distingue horario de verano --"hora del Pacifico" en
// vez de "hora estandar del Pacifico"--. Sobre un `ZoneOffset` las dos dan lo mismo, por lo mismo:
// no hay nombre que variar.
final class PiezaZonaTexto extends Pieza {

    private final TextStyle estilo;
    private final Set<ZoneId> preferidas;
    private final boolean generica;

    PiezaZonaTexto(TextStyle estilo, Set<ZoneId> preferidas, boolean generica) {
        this.estilo = estilo;
        this.preferidas = preferidas;
        this.generica = generica;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        ZoneId z = ctx.consultar(TemporalQueries.zoneId());
        if (z == null) {
            return ctx.faltaOTira("ZoneText");
        }
        if (z instanceof ZoneOffset) {
            salida.append(z.getId());
            return true;
        }
        // Una zona con nombre. No puede llegar aca --`ZoneId.of` no las construye-- y si algun dia
        // llega, lo unico que hay es el identificador, que no es el nombre. Se escribe igual, que es
        // la reserva del JDK cuando el locale no tiene nombre para esa zona.
        salida.append(z.getId());
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        // Lo unico que se puede reconocer es lo unico que se puede escribir: un desplazamiento.
        // Reconocer nombres necesitaria la misma tabla del CLDR, y ademas al reves.
        return new PiezaZonaId(PiezaZonaId.ZONA_U_OFFSET).parsear(ctx, texto, pos);
    }

    // Ni el estilo ni el conjunto de preferidas cambian lo que sale sobre un desplazamiento: el
    // primero porque no hay nombre que acortar, el segundo porque solo sirve para desempatar entre
    // zonas que comparten nombre. Se guardan igual --son parte de lo que se pidio-- y aparecen aca.
    // El JDK escribe `ZoneText(...)` tambien para la version generica; se copia el nombre.
    @Override
    public String toString() {
        return "ZoneText(" + this.estilo + ")";
    }
}
