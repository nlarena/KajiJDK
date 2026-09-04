package javax.swing.table;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Una columna de una tabla: su ancho, su encabezado, y como se dibujan y editan sus celdas.
 *
 * <h2>{@link #getModelIndex} es la clave de toda la clase</h2>
 *
 * <p>Una columna sabe <strong>de que columna del modelo saca sus datos</strong>, y ese numero no
 * tiene por que coincidir con donde esta en pantalla. Es lo que permite reordenar columnas
 * arrastrandolas, o esconder algunas, sin tocar el modelo: se mueve la columna en la vista y su
 * indice de modelo viaja con ella.
 *
 * <p>Confundir los dos indices es el bug clasico de una tabla, y es silencioso: devuelve el dato de
 * otra columna, no un error.
 *
 * <h2>Tres anchos, no uno</h2>
 *
 * <p>El minimo, el preferido y el maximo. Cuando la tabla se agranda o achica, el reparto respeta
 * los limites y estira lo que puede — sin los tres, redimensionar una tabla o le rompe el layout a
 * una columna o se lo rompe a todas.
 *
 * <p>{@link #setWidth} recorta contra el minimo y el maximo en vez de aceptar cualquier numero:
 * dejar pasar un ancho fuera de rango dejaria la columna en un estado que ella misma declara
 * invalido.
 */
public class TableColumn implements java.io.Serializable {

    private static final long serialVersionUID = -6113660025878112608L;

    /** El nombre de la propiedad del ancho, para {@link PropertyChangeListener}. */
    public static final String COLUMN_WIDTH_PROPERTY = "columWidth";

    /** El nombre de la propiedad del valor del encabezado. */
    public static final String HEADER_VALUE_PROPERTY = "headerValue";

    /** El nombre de la propiedad del dibujante del encabezado. */
    public static final String HEADER_RENDERER_PROPERTY = "headerRenderer";

    /** El nombre de la propiedad del dibujante de las celdas. */
    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";

    /** De que columna del modelo saca los datos; ver la nota de la clase. */
    protected int modelIndex;

    /** Con que se la identifica; si es {@code null}, se usa el valor del encabezado. */
    protected Object identifier;

    /** El ancho actual. */
    protected int width;

    /** El ancho minimo. */
    protected int minWidth;

    private int preferredWidth;

    /** El ancho maximo. */
    protected int maxWidth;

    /** Como se dibuja el encabezado; {@code null} para el de la tabla. */
    protected TableCellRenderer headerRenderer;

    /** Que dice el encabezado. */
    protected Object headerValue;

    /** Como se dibujan las celdas; {@code null} para el de la tabla. */
    protected TableCellRenderer cellRenderer;

    /** Como se editan las celdas; {@code null} para el de la tabla. */
    protected TableCellEditor cellEditor;

    /** Si el usuario la puede redimensionar. */
    protected boolean isResizable;

    /** Cuantas veces se pidio callar los avisos de redimension; ver {@link #disableResizedPosting}. */
    protected transient int resizedPostingDisableCount;

    private PropertyChangeSupport cambios;

    /** La columna 0, con el ancho por omision. */
    public TableColumn() {
        this(0);
    }

    /** Sobre esa columna del modelo. */
    public TableColumn(int modelIndex) {
        this(modelIndex, 75, null, null);
    }

    /** Sobre esa columna del modelo, con ese ancho. */
    public TableColumn(int modelIndex, int width) {
        this(modelIndex, width, null, null);
    }

    /** Con todo explicito. */
    public TableColumn(int modelIndex, int width, TableCellRenderer cellRenderer,
            TableCellEditor cellEditor) {
        super();
        this.modelIndex = modelIndex;
        this.width = width;
        this.preferredWidth = width;
        this.cellRenderer = cellRenderer;
        this.cellEditor = cellEditor;
        this.minWidth = 15;
        this.maxWidth = Integer.MAX_VALUE;
        this.isResizable = true;
        this.resizedPostingDisableCount = 0;
        this.headerValue = null;
    }

    /** Cambia de que columna del modelo saca los datos. */
    public void setModelIndex(int modelIndex) {
        int viejo = this.modelIndex;
        this.modelIndex = modelIndex;
        avisar("modelIndex", Integer.valueOf(viejo), Integer.valueOf(modelIndex));
    }

    /** De que columna del modelo saca los datos. */
    public int getModelIndex() {
        return this.modelIndex;
    }

    /** Cambia el identificador. */
    public void setIdentifier(Object identifier) {
        Object viejo = this.identifier;
        this.identifier = identifier;
        avisar("identifier", viejo, identifier);
    }

    /**
     * El identificador; si no se fijo ninguno, el valor del encabezado.
     *
     * <p>La caida al encabezado es comoda y tiene un filo: dos columnas con el mismo titulo tienen
     * el mismo identificador, y buscar por identificador devuelve la primera.
     */
    public Object getIdentifier() {
        if (this.identifier != null) {
            return this.identifier;
        }
        return getHeaderValue();
    }

    /** Cambia lo que dice el encabezado. */
    public void setHeaderValue(Object headerValue) {
        Object viejo = this.headerValue;
        this.headerValue = headerValue;
        avisar(HEADER_VALUE_PROPERTY, viejo, headerValue);
    }

    /** Lo que dice el encabezado. */
    public Object getHeaderValue() {
        return this.headerValue;
    }

    /** Cambia como se dibuja el encabezado. */
    public void setHeaderRenderer(TableCellRenderer headerRenderer) {
        TableCellRenderer viejo = this.headerRenderer;
        this.headerRenderer = headerRenderer;
        avisar(HEADER_RENDERER_PROPERTY, viejo, headerRenderer);
    }

    /** Como se dibuja el encabezado, o {@code null} para el de la tabla. */
    public TableCellRenderer getHeaderRenderer() {
        return this.headerRenderer;
    }

    /** Cambia como se dibujan las celdas. */
    public void setCellRenderer(TableCellRenderer cellRenderer) {
        TableCellRenderer viejo = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        avisar(CELL_RENDERER_PROPERTY, viejo, cellRenderer);
    }

    /** Como se dibujan las celdas, o {@code null} para el de la tabla. */
    public TableCellRenderer getCellRenderer() {
        return this.cellRenderer;
    }

    /** Cambia como se editan las celdas. */
    public void setCellEditor(TableCellEditor cellEditor) {
        TableCellEditor viejo = this.cellEditor;
        this.cellEditor = cellEditor;
        avisar("cellEditor", viejo, cellEditor);
    }

    /** Como se editan las celdas, o {@code null} para el de la tabla. */
    public TableCellEditor getCellEditor() {
        return this.cellEditor;
    }

    /**
     * Cambia el ancho, recortando contra el minimo y el maximo.
     *
     * <p>El aviso se puede haber silenciado con {@link #disableResizedPosting}: mientras el usuario
     * arrastra el borde, cada pixel dispararia un aviso y un relayout.
     */
    public void setWidth(int width) {
        int viejo = this.width;
        int nuevo = Math.min(Math.max(width, this.minWidth), this.maxWidth);
        this.width = nuevo;
        if (nuevo == viejo) {
            return;
        }
        if (this.resizedPostingDisableCount == 0) {
            avisar(COLUMN_WIDTH_PROPERTY, Integer.valueOf(viejo), Integer.valueOf(nuevo));
        }
    }

    /** El ancho actual. */
    public int getWidth() {
        return this.width;
    }

    /** Cambia el ancho preferido, recortando contra el minimo y el maximo. */
    public void setPreferredWidth(int preferredWidth) {
        int viejo = this.preferredWidth;
        this.preferredWidth = Math.min(Math.max(preferredWidth, this.minWidth), this.maxWidth);
        avisar("preferredWidth", Integer.valueOf(viejo), Integer.valueOf(this.preferredWidth));
    }

    /** El ancho preferido. */
    public int getPreferredWidth() {
        return this.preferredWidth;
    }

    /**
     * Cambia el ancho minimo.
     *
     * <p>Sube el actual y el preferido si quedaron por debajo: dejarlos abajo del minimo seria dejar
     * la columna violando su propia restriccion.
     */
    public void setMinWidth(int minWidth) {
        int viejo = this.minWidth;
        this.minWidth = Math.max(Math.min(minWidth, this.maxWidth), 0);
        if (this.width < this.minWidth) {
            setWidth(this.minWidth);
        }
        if (this.preferredWidth < this.minWidth) {
            setPreferredWidth(this.minWidth);
        }
        avisar("minWidth", Integer.valueOf(viejo), Integer.valueOf(this.minWidth));
    }

    /** El ancho minimo. */
    public int getMinWidth() {
        return this.minWidth;
    }

    /** Cambia el ancho maximo, bajando el actual y el preferido si hace falta. */
    public void setMaxWidth(int maxWidth) {
        int viejo = this.maxWidth;
        this.maxWidth = Math.max(this.minWidth, maxWidth);
        if (this.width > this.maxWidth) {
            setWidth(this.maxWidth);
        }
        if (this.preferredWidth > this.maxWidth) {
            setPreferredWidth(this.maxWidth);
        }
        avisar("maxWidth", Integer.valueOf(viejo), Integer.valueOf(this.maxWidth));
    }

    /** El ancho maximo. */
    public int getMaxWidth() {
        return this.maxWidth;
    }

    /** Cambia si el usuario la puede redimensionar. */
    public void setResizable(boolean isResizable) {
        boolean viejo = this.isResizable;
        this.isResizable = isResizable;
        avisar("isResizable", Boolean.valueOf(viejo), Boolean.valueOf(isResizable));
    }

    /** Si el usuario la puede redimensionar. */
    public boolean getResizable() {
        return this.isResizable;
    }

    /** Fija los tres anchos al preferido, dejando la columna sin margen de estiramiento. */
    public void sizeWidthToFit() {
        if (this.headerRenderer == null) {
            return;
        }
        setMinWidth(this.preferredWidth);
        setMaxWidth(this.preferredWidth);
        setWidth(this.preferredWidth);
    }

    /**
     * Calla los avisos de cambio de ancho.
     *
     * <p>Es un contador y no una bandera para que dos silenciamientos anidados no se pisen: el
     * interno no puede volver a prender los avisos que el externo apago.
     *
     * @deprecated es de la epoca en que la tabla lo usaba mientras el usuario arrastraba; hoy lo
     *     resuelve el propio arrastre
     */
    @Deprecated
    public void disableResizedPosting() {
        this.resizedPostingDisableCount = this.resizedPostingDisableCount + 1;
    }

    /**
     * Vuelve a permitir los avisos, y manda uno si el ancho cambio mientras estaban callados.
     *
     * @deprecated ver {@link #disableResizedPosting}
     */
    @Deprecated
    public void enableResizedPosting() {
        this.resizedPostingDisableCount = this.resizedPostingDisableCount - 1;
        if (this.resizedPostingDisableCount < 0) {
            this.resizedPostingDisableCount = 0;
        }
    }

    /** Agrega un oyente de cambios de propiedad. */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (this.cambios == null) {
            this.cambios = new PropertyChangeSupport(this);
        }
        this.cambios.addPropertyChangeListener(listener);
    }

    /** Saca un oyente. */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (this.cambios != null) {
            this.cambios.removePropertyChangeListener(listener);
        }
    }

    /** Los oyentes de cambios de propiedad. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (this.cambios == null) {
            return new PropertyChangeListener[0];
        }
        return this.cambios.getPropertyChangeListeners();
    }

    /**
     * El dibujante por omision del encabezado.
     *
     * @return {@code null} en esta VM: el del JDK es un componente que se pinta, y esta biblioteca
     *     no trae Swing dibujable. Ver la nota de {@link javax.swing.JTable}. Devolver {@code null}
     *     es lo que ya significa "usa el de la tabla", asi que no inventa nada
     */
    protected TableCellRenderer createDefaultHeaderRenderer() {
        return null;
    }

    private void avisar(String propiedad, Object viejo, Object nuevo) {
        if (this.cambios != null) {
            this.cambios.firePropertyChange(propiedad, viejo, nuevo);
        }
    }
}
