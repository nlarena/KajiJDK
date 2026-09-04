import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.net.URL;

// Prueba de comportamiento de la mitad de `java.net` que NO necesita red: el datagrama como
// estructura de datos, el vocabulario viejo de opciones, el SPI de socket, y la parte de
// configuracion y de parseo de `HttpURLConnection`.
//
// Corre igual en la VM de KajiJDK y en una JVM real: no hay nada aca que dependa de una conexion.
// Devuelve -1 si esta todo bien, o el numero del caso que fallo.
public class NetDgramTest {

    // Una `HttpURLConnection` de mentira, pero honesta: no conecta, y lo unico que aporta es la
    // linea de estado, que es lo que `getResponseCode` tiene que saber parsear.
    static class ConexionDePrueba extends HttpURLConnection {
        private final String lineaDeEstado;

        ConexionDePrueba(URL u, String lineaDeEstado) {
            super(u);
            this.lineaDeEstado = lineaDeEstado;
        }

        @Override
        public void connect() throws IOException {
            this.connected = true;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public String getHeaderField(int n) {
            if (n == 0) {
                return this.lineaDeEstado;
            }
            return null;
        }

        // Los campos protegidos solo se ven desde el cuerpo de la subclase, y el cuerpo de la
        // prueba no lo es; por eso se los expone con un accesor.
        int chunkLengthDePrueba() {
            return this.chunkLength;
        }
    }

    // Un `SocketImpl` de mentira que solo recuerda las opciones que le fijaron: alcanza para
    // verificar la traduccion entre los dos vocabularios de opciones.
    static class ImplDePrueba extends SocketImpl {
        int ultimaOpcion = -1;
        Object ultimoValor;

        ImplDePrueba() {
            this.address = null;
            this.port = 8080;
            this.localport = 1234;
        }

        @Override
        protected void create(boolean stream) throws IOException {
        }

        @Override
        protected void connect(String host, int port) throws IOException {
        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
        }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException {
        }

        @Override
        protected void bind(InetAddress host, int port) throws IOException {
        }

        @Override
        protected void listen(int backlog) throws IOException {
        }

        @Override
        protected void accept(SocketImpl s) throws IOException {
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        protected int available() throws IOException {
            return 0;
        }

        @Override
        protected void close() throws IOException {
        }

        @Override
        protected void sendUrgentData(int data) throws IOException {
        }

        @Override
        public void setOption(int optID, Object value) throws SocketException {
            this.ultimaOpcion = optID;
            this.ultimoValor = value;
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            this.ultimaOpcion = optID;
            return Boolean.TRUE;
        }

        // Igual que arriba: los protegidos se exponen para poder mirarlos desde la prueba.
        int getPortDePrueba() {
            return getPort();
        }

        int getLocalPortDePrueba() {
            return getLocalPort();
        }

        boolean supportsUrgentDataDePrueba() {
            return supportsUrgentData();
        }

        FileDescriptor getFileDescriptorDePrueba() {
            return getFileDescriptor();
        }

        <T> void setOptionDePrueba(SocketOption<T> name, T value) throws IOException {
            setOption(name, value);
        }

        java.util.Set<SocketOption<?>> supportedOptionsDePrueba() {
            return supportedOptions();
        }

        void shutdownInputDePrueba() throws IOException {
            shutdownInput();
        }
    }

    public static int run() throws Exception {
        // ---- DatagramPacket: el tramo dentro del buffer -----------------------------------------
        byte[] buf = new byte[100];
        DatagramPacket p = new DatagramPacket(buf, 10, 20);
        if (p.getOffset() != 10 || p.getLength() != 20 || p.getData() != buf) {
            return 1;
        }
        // Sin direccion todavia.
        if (p.getAddress() != null || p.getPort() != 0) {
            return 2;
        }

        // `getData` NO copia: es del contrato, y es lo que hace barato reusar un paquete.
        buf[10] = 42;
        if (p.getData()[10] != 42) {
            return 3;
        }

        // Un tramo que no entra se rechaza.
        if (!tiraIAE(buf, 90, 20)) {
            return 4;
        }
        if (!tiraIAE(buf, -1, 5)) {
            return 5;
        }
        if (!tiraIAE(buf, 0, -5)) {
            return 6;
        }
        // Y el chequeo no se lo puede saltear un desbordamiento de int.
        if (!tiraIAE(buf, Integer.MAX_VALUE, Integer.MAX_VALUE)) {
            return 7;
        }

        // `setLength` respeta el offset que ya habia.
        p.setLength(90);
        if (p.getLength() != 90) {
            return 8;
        }
        try {
            p.setLength(91);
            return 9;
        } catch (IllegalArgumentException esperado) {
        }

        // ---- DatagramPacket: la direccion -------------------------------------------------------
        InetAddress local = InetAddress.getByName("127.0.0.1");
        DatagramPacket q = new DatagramPacket(buf, 0, 10, local, 9999);
        if (q.getPort() != 9999 || !q.getAddress().equals(local)) {
            return 10;
        }
        SocketAddress sa = q.getSocketAddress();
        if (!(sa instanceof InetSocketAddress)) {
            return 11;
        }
        InetSocketAddress isa = (InetSocketAddress) sa;
        if (isa.getPort() != 9999 || !isa.getAddress().equals(local)) {
            return 12;
        }

        // Puerto fuera de rango.
        try {
            q.setPort(70000);
            return 13;
        } catch (IllegalArgumentException esperado) {
        }
        try {
            q.setPort(-1);
            return 14;
        } catch (IllegalArgumentException esperado) {
        }

        // Una direccion sin resolver no se puede usar de destino: no hay a donde mandar.
        InetSocketAddress sinResolver = InetSocketAddress.createUnresolved("no.existe.invalido", 80);
        if (!sinResolver.isUnresolved()) {
            return 15;
        }
        try {
            q.setSocketAddress(sinResolver);
            return 16;
        } catch (IllegalArgumentException esperado) {
        }

        // `setData(byte[])` reinicia el tramo entero.
        byte[] otro = new byte[7];
        q.setData(otro);
        if (q.getOffset() != 0 || q.getLength() != 7 || q.getData() != otro) {
            return 17;
        }

        // ---- SocketOptions: los numeros acordados -----------------------------------------------
        if (SocketOptions.TCP_NODELAY != 0x0001) {
            return 20;
        }
        if (SocketOptions.SO_TIMEOUT != 0x1006) {
            return 21;
        }
        if (SocketOptions.SO_LINGER != 0x0080) {
            return 22;
        }
        if (SocketOptions.SO_BINDADDR != 0x000F) {
            return 23;
        }

        // ---- SocketImpl: accesores y traduccion de opciones -------------------------------------
        ImplDePrueba impl = new ImplDePrueba();
        if (impl.getPortDePrueba() != 8080 || impl.getLocalPortDePrueba() != 1234) {
            return 30;
        }
        if (impl.supportsUrgentDataDePrueba()) {
            return 31;
        }
        if (impl.getFileDescriptorDePrueba() != null) {
            return 32;
        }
        // La forma vieja de nombrar una opcion --enteros-- llega a la implementacion.
        impl.setOption(SocketOptions.TCP_NODELAY, Boolean.TRUE);
        if (impl.ultimaOpcion != SocketOptions.TCP_NODELAY) {
            return 33;
        }
        if (!Boolean.TRUE.equals(impl.ultimoValor)) {
            return 34;
        }

        // La forma NUEVA no se puentea sola a la vieja: la base la rechaza siempre. Es el contrato
        // del JDK, y es el correcto -- una implementacion que solo atiende enteros no declaro nada
        // sobre estos nombres, y darla por soportada seria inventarle capacidades.
        if (!impl.supportedOptionsDePrueba().isEmpty()) {
            return 35;
        }
        try {
            impl.setOptionDePrueba(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
            return 36;
        } catch (UnsupportedOperationException esperado) {
        }
        try {
            impl.setOptionDePrueba(null, Boolean.TRUE);
            return 39;
        } catch (NullPointerException esperado) {
        }
        // `shutdownInput` de la clase base tira: es la implementacion base del JDK, no un stub.
        try {
            impl.shutdownInputDePrueba();
            return 37;
        } catch (IOException esperado) {
        }
        String texto = impl.toString();
        if (texto.indexOf("port=8080") < 0 || texto.indexOf("localport=1234") < 0) {
            return 38;
        }

        // ---- HttpURLConnection: configuracion del pedido ----------------------------------------
        URL u = new URL("http://ejemplo.org:8080/a/b?c=d");
        ConexionDePrueba c = new ConexionDePrueba(u, "HTTP/1.1 404 Not Found");

        if (!c.getRequestMethod().equals("GET")) {
            return 40;
        }
        c.setRequestMethod("POST");
        if (!c.getRequestMethod().equals("POST")) {
            return 41;
        }
        try {
            c.setRequestMethod("BORRAR");
            return 42;
        } catch (ProtocolException esperado) {
        }
        // El metodo malo no piso al bueno.
        if (!c.getRequestMethod().equals("POST")) {
            return 43;
        }

        // Los dos modos de streaming son excluyentes.
        c.setChunkedStreamingMode(1024);
        if (c.chunkLengthDePrueba() != 1024) {
            return 44;
        }
        try {
            c.setFixedLengthStreamingMode(100L);
            return 45;
        } catch (IllegalStateException esperado) {
        }

        // Una vez conectado ya no se puede fijar el modo.
        c.connect();
        try {
            c.setChunkedStreamingMode(64);
            return 46;
        } catch (IllegalStateException esperado) {
        }
        // Ni cambiar el metodo.
        try {
            c.setRequestMethod("GET");
            return 47;
        } catch (ProtocolException esperado) {
        }

        // ---- HttpURLConnection: la linea de estado ----------------------------------------------
        if (c.getResponseCode() != 404) {
            return 50;
        }
        if (!"Not Found".equals(c.getResponseMessage())) {
            return 51;
        }
        // Sin texto detras del codigo.
        ConexionDePrueba c2 = new ConexionDePrueba(u, "HTTP/1.1 200 OK");
        if (c2.getResponseCode() != 200 || !"OK".equals(c2.getResponseMessage())) {
            return 52;
        }
        // Una linea que no es una linea de estado no inventa un codigo.
        ConexionDePrueba c3 = new ConexionDePrueba(u, "cualquier cosa");
        if (c3.getResponseCode() != -1) {
            return 53;
        }
        // Sin linea de estado no hubo respuesta, y ahi `getResponseCode` re-tira la excepcion que
        // `getInputStream` habia dado, en vez de devolver -1: devolver -1 dejaria al que llamo sin
        // saber por que no hay codigo. `ConexionDePrueba` no sabe leer, asi que la excepcion es la
        // `UnknownServiceException` de la clase base.
        ConexionDePrueba c4 = new ConexionDePrueba(u, null);
        try {
            c4.getResponseCode();
            return 54;
        } catch (java.net.UnknownServiceException esperado) {
        }

        // Las constantes.
        if (HttpURLConnection.HTTP_OK != 200 || HttpURLConnection.HTTP_NOT_FOUND != 404) {
            return 55;
        }
        if (HttpURLConnection.HTTP_INTERNAL_ERROR != 500
                || HttpURLConnection.HTTP_VERSION != 505) {
            return 56;
        }

        // `getErrorStream` de la base es null, no una excepcion.
        if (c.getErrorStream() != null) {
            return 57;
        }

        // Un autenticador por conexion no se acepta en silencio.
        try {
            c.setAuthenticator(null);
            return 58;
        } catch (UnsupportedOperationException esperado) {
        }

        // La bandera de redirecciones es por instancia y por VM a la vez.
        boolean antes = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
        if (HttpURLConnection.getFollowRedirects()) {
            return 59;
        }
        HttpURLConnection.setFollowRedirects(antes);
        c.setInstanceFollowRedirects(false);
        if (c.getInstanceFollowRedirects()) {
            return 60;
        }

        return -1;
    }

    private static boolean tiraIAE(byte[] buf, int offset, int length) {
        try {
            new DatagramPacket(buf, offset, length);
            return false;
        } catch (IllegalArgumentException esperado) {
            return true;
        }
    }
}
