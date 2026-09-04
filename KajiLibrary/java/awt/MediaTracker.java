package java.awt;

import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Espera a que terminen de cargarse un grupo de imágenes.
 *
 * <p>Resuelve un problema concreto de AWT: {@link Toolkit#getImage} devuelve en el acto una
 * {@link Image} que **todavía no tiene píxeles**, y la carga sigue en otro hilo. Sin esto, un
 * programa que quiere dibujar tres imágenes juntas tendría que implementar
 * {@link ImageObserver} y llevar la cuenta a mano.
 *
 * <p>Las imágenes se agrupan por un **identificador** que elige quien las agrega, y todo se puede
 * preguntar por grupo o por el conjunto entero. La idea es cargar por prioridades: esperar el grupo
 * 0 —lo que hace falta para mostrar algo— y dejar el resto cargando.
 *
 * <p>La distinción entre `checkID` y `statusID` es la que más confunde: `check` pregunta **si
 * terminó**, `status` devuelve **en qué anda** como una combinación de las cuatro banderas. Y las dos
 * tienen una variante que arranca la carga y otra que no —el `boolean load`—, porque preguntar por
 * una imagen no debería obligar a bajarla.
 */
public class MediaTracker implements Serializable {

    private static final long serialVersionUID = -483174189758638095L;

    /** La imagen está cargando. */
    public static final int LOADING = 1;

    /** La carga se abortó. */
    public static final int ABORTED = 2;

    /** La carga falló. */
    public static final int ERRORED = 4;

    /** La imagen terminó de cargar bien. */
    public static final int COMPLETE = 8;

    /** El componente al que se le van a dibujar; es el que observa la carga. */
    Component target;

    /** Una imagen anotada, con su grupo y su tamaño pedido. */
    private static final class Anotada implements ImageObserver {
        private final Image imagen;
        private final int id;
        private final int ancho;
        private final int alto;
        private int estado;
        private boolean arrancada;

        private Anotada(Image imagen, int id, int ancho, int alto) {
            this.imagen = imagen;
            this.id = id;
            this.ancho = ancho;
            this.alto = alto;
        }

        /**
         * Recibe los avisos de la carga.
         *
         * @return `true` mientras falte algo, que es como {@link ImageObserver} dice "seguime
         *     avisando"
         */
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
            synchronized (this) {
                if ((infoflags & ImageObserver.ERROR) != 0) {
                    this.estado = ERRORED;
                } else if ((infoflags & ImageObserver.ABORT) != 0) {
                    this.estado = ABORTED;
                } else if ((infoflags & ImageObserver.ALLBITS) != 0) {
                    this.estado = COMPLETE;
                }
                this.notifyAll();
            }
            return (this.estado & (COMPLETE | ERRORED | ABORTED)) == 0;
        }
    }

    /** Las imágenes anotadas, en el orden en que se agregaron. */
    private final ArrayList<Anotada> anotadas = new ArrayList<Anotada>();

    /**
     * Un seguidor para las imágenes que se van a dibujar sobre ese componente.
     *
     * @param comp el componente; es quien va a recibir los avisos de repintado
     */
    public MediaTracker(Component comp) {
        this.target = comp;
    }

    /** Agrega una imagen a ese grupo, sin pedir un tamaño en particular. */
    public void addImage(Image image, int id) {
        this.addImage(image, id, -1, -1);
    }

    /**
     * Agrega una imagen a ese grupo, pidiendo que se escale a ese tamaño.
     *
     * <p>Una imagen se puede agregar **más de una vez** con tamaños distintos: cada anotación se
     * sigue por separado, que es lo que permite esperar la miniatura sin esperar la grande.
     */
    public synchronized void addImage(Image image, int id, int w, int h) {
        this.anotadas.add(new Anotada(image, id, w, h));
    }

    /**
     * Si terminaron **todas** las imágenes.
     *
     * <p>No arranca ninguna carga: pregunta y se va.
     */
    public boolean checkAll() {
        return this.checkAll(false);
    }

    /**
     * Si terminaron todas, arrancando la carga de las que no empezaron si `load` es `true`.
     *
     * @return `true` si ninguna quedó cargando; una que falló o se abortó también cuenta como
     *     terminada, porque no va a cambiar más
     */
    public boolean checkAll(boolean load) {
        return this.chequear(-1, false, load);
    }

    /** Si alguna falló. */
    public synchronized boolean isErrorAny() {
        return this.conError(-1).length > 0;
    }

    /**
     * Las imágenes que fallaron.
     *
     * @return las imágenes, o `null` si no falló ninguna
     */
    public synchronized Object[] getErrorsAny() {
        Object[] r = this.conError(-1);
        return r.length == 0 ? null : r;
    }

    /**
     * Espera a que terminen todas.
     *
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public void waitForAll() throws InterruptedException {
        this.waitForAll(0);
    }

    /**
     * Espera a que terminen todas, hasta ese tiempo.
     *
     * <p>Devuelve `true` sólo si terminaron **todas y bien**. Un seguidor vacío devuelve `false`, que
     * sorprende hasta que se lee al derecho: la pregunta no es "¿dejé de esperar?" sino "¿están todas
     * cargadas?", y sin ninguna imagen la respuesta es que no. Una que falló o se abortó también da
     * `false`, aunque ya no esté cargando.
     *
     * @param ms cuánto esperar como mucho; 0 quiere decir sin límite, y un negativo es tiempo ya
     *     vencido —no tira, contesta con lo que haya—
     * @return `true` si todas terminaron bien
     * @throws InterruptedException si interrumpen al hilo
     */
    public synchronized boolean waitForAll(long ms) throws InterruptedException {
        return this.esperar(-1, ms);
    }

    /**
     * En qué andan todas, como una combinación de las cuatro banderas.
     *
     * @param load si arrancar la carga de las que no empezaron
     */
    public int statusAll(boolean load) {
        return this.estado(-1, load);
    }

    /** Si terminó ese grupo. */
    public boolean checkID(int id) {
        return this.checkID(id, false);
    }

    /** Si terminó ese grupo, arrancando la carga si `load` es `true`. */
    public boolean checkID(int id, boolean load) {
        return this.chequear(id, true, load);
    }

    /** Si alguna de ese grupo falló. */
    public synchronized boolean isErrorID(int id) {
        return this.conError(id).length > 0;
    }

    /**
     * Las de ese grupo que fallaron.
     *
     * @return las imágenes, o `null` si no falló ninguna
     */
    public synchronized Object[] getErrorsID(int id) {
        Object[] r = this.conError(id);
        return r.length == 0 ? null : r;
    }

    /**
     * Espera a que termine ese grupo.
     *
     * @throws InterruptedException si interrumpen al hilo
     */
    public void waitForID(int id) throws InterruptedException {
        this.waitForID(id, 0);
    }

    /**
     * Espera a que termine ese grupo, hasta ese tiempo.
     *
     * @return `true` sólo si todas las del grupo terminaron bien; ver {@link #waitForAll(long)}
     * @throws InterruptedException si interrumpen al hilo
     */
    public synchronized boolean waitForID(int id, long ms) throws InterruptedException {
        return this.esperar(id, ms);
    }

    /** En qué anda ese grupo. */
    public int statusID(int id, boolean load) {
        return this.estado(id, load);
    }

    /** Deja de seguir esa imagen, en todos los grupos y tamaños. */
    public synchronized void removeImage(Image image) {
        int i = 0;
        while (i < this.anotadas.size()) {
            if (this.anotadas.get(i).imagen == image) {
                this.anotadas.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /** Deja de seguirla en ese grupo. */
    public synchronized void removeImage(Image image, int id) {
        int i = 0;
        while (i < this.anotadas.size()) {
            Anotada a = this.anotadas.get(i);
            if (a.imagen == image && a.id == id) {
                this.anotadas.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /** Deja de seguirla en ese grupo y con ese tamaño exacto. */
    public synchronized void removeImage(Image image, int id, int width, int height) {
        int i = 0;
        while (i < this.anotadas.size()) {
            Anotada a = this.anotadas.get(i);
            if (a.imagen == image && a.id == id && a.ancho == width && a.alto == height) {
                this.anotadas.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /** Marca todo como terminado; lo usa la espera cuando se acaba el tiempo. */
    synchronized void setDone() {
        this.notifyAll();
    }

    /**
     * Arranca la carga de una imagen anotada.
     *
     * <p>Pedirle el ancho con el observador puesto es lo que hace que la fuente de la imagen empiece
     * a producir píxeles: es la forma de arrancar una carga en AWT, y es tan poco evidente que vale
     * decirlo.
     */
    private void arrancar(Anotada a) {
        if (a.arrancada) {
            return;
        }
        a.arrancada = true;
        a.estado = LOADING;
        int ancho = a.imagen.getWidth(a);
        if (ancho >= 0) {
            // La imagen ya tenía sus dimensiones, así que ya está entera en memoria.
            a.estado = COMPLETE;
        }
    }

    /** Las anotadas de ese grupo, o todas si `id` es -1 y `porGrupo` es `false`. */
    private ArrayList<Anotada> elegir(int id, boolean porGrupo) {
        ArrayList<Anotada> out = new ArrayList<Anotada>();
        for (int i = 0; i < this.anotadas.size(); i++) {
            Anotada a = this.anotadas.get(i);
            if (!porGrupo || a.id == id) {
                out.add(a);
            }
        }
        return out;
    }

    /** Si terminaron las del grupo pedido. */
    private boolean chequear(int id, boolean porGrupo, boolean load) {
        ArrayList<Anotada> as;
        synchronized (this) {
            as = this.elegir(id, porGrupo);
        }
        boolean todas = true;
        for (int i = 0; i < as.size(); i++) {
            Anotada a = as.get(i);
            if (load) {
                synchronized (this) {
                    this.arrancar(a);
                }
            }
            if ((a.estado & (COMPLETE | ERRORED | ABORTED)) == 0) {
                todas = false;
            }
        }
        return todas;
    }

    /** La combinación de banderas del grupo pedido. */
    private int estado(int id, boolean load) {
        ArrayList<Anotada> as;
        synchronized (this) {
            as = this.elegir(id, id != -1);
        }
        int r = 0;
        for (int i = 0; i < as.size(); i++) {
            Anotada a = as.get(i);
            if (load) {
                synchronized (this) {
                    this.arrancar(a);
                }
            }
            r = r | a.estado;
        }
        return r;
    }

    /** Las imágenes del grupo pedido que fallaron. */
    private Object[] conError(int id) {
        ArrayList<Object> out = new ArrayList<Object>();
        ArrayList<Anotada> as = this.elegir(id, id != -1);
        for (int i = 0; i < as.size(); i++) {
            if ((as.get(i).estado & ERRORED) != 0) {
                out.add(as.get(i).imagen);
            }
        }
        return out.toArray();
    }

    /**
     * Espera al grupo pedido, con o sin tope de tiempo.
     *
     * <p>Sólo la **primera** vuelta arranca las cargas. Las siguientes son puro esperar: volver a
     * arrancar lo ya arrancado no haría nada y recorrería la lista entera en cada aviso.
     */
    private boolean esperar(int id, long ms) throws InterruptedException {
        long fin = System.currentTimeMillis() + ms;
        boolean primera = true;
        while (true) {
            int st = this.estado(id, primera);
            primera = false;
            if ((st & LOADING) == 0) {
                return st == COMPLETE;
            }
            if (ms > 0) {
                long queda = fin - System.currentTimeMillis();
                if (queda <= 0) {
                    return false;
                }
                this.wait(queda);
            } else {
                this.wait();
            }
        }
    }
}
