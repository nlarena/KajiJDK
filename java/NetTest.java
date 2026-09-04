import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.HttpCookie;
import java.net.HttpRetryException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetPermission;
import java.net.NoRouteToHostException;
import java.net.PasswordAuthentication;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.UnknownServiceException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLPermission;
import java.net.UnixDomainSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

// Prueba de comportamiento de java.net: corre igual en KajiJDK y en el JDK real, y el numero que
// devuelve tiene que ser el mismo en los dos. Nada de aca toca la red -- no hay una sola
// comprobacion que dependa de que exista un DNS, una placa o un socket, ni una que mire un
// `hashCode` de identidad.
//
// `run()` devuelve -1 si todo pasa, o el indice de la primera comprobacion que fallo. Los indices
// son posicionales: agregar una comprobacion en el medio corre a todas las de abajo, cosa que
// importa solo mientras se depura.
public class NetTest {

    private static int n;
    private static int failed;

    private static void ck(boolean cond) {
        if (failed < 0 && !cond) {
            failed = n;
        }
        n = n + 1;
    }

    private static void eq(Object a, Object b) {
        ck(a == null ? b == null : a.equals(b));
    }

    public static int run() {
        n = 0;
        failed = -1;
        try {
            body();
        } catch (Throwable t) {
            if (failed < 0) {
                failed = n;
            }
        }
        return failed;
    }

    // Un bloque de codigo que puede tirar cualquier cosa, incluso chequeadas.
    private interface Act {
        void run() throws Exception;
    }

    // Corre `a` y devuelve el nombre de la clase de lo que tiro, o "" si no tiro nada.
    private static String threw(Act a) {
        try {
            a.run();
            return "";
        } catch (Throwable t) {
            return t.getClass().getName();
        }
    }

    private static final String IAE = "java.lang.IllegalArgumentException";

    private static void body() throws Exception {
        // ---- jerarquia de excepciones ----
        ck(new BindException() instanceof SocketException);
        ck(new ConnectException("x") instanceof SocketException);
        ck(new NoRouteToHostException() instanceof SocketException);
        ck(new PortUnreachableException() instanceof SocketException);
        ck(new SocketException("m", new RuntimeException()).getCause() != null);
        ck(new ProtocolException("p") instanceof IOException);
        // `instanceof` no compila entre tipos que el compilador ya sabe que no se relacionan, asi
        // que estas dos --que justamente comprueban que NO se relacionan-- van por reflexion.
        ck(!SocketException.class.isInstance(new ProtocolException("p")));
        ck(new SocketTimeoutException() instanceof java.io.InterruptedIOException);
        ck(!SocketException.class.isInstance(new SocketTimeoutException()));
        eq(new ConnectException("x").getMessage(), "x");
        HttpRetryException hre = new HttpRetryException("razon", 302, "http://d/");
        ck(hre.responseCode() == 302);
        eq(hre.getReason(), "razon");
        eq(hre.getLocation(), "http://d/");
        ck(new HttpRetryException("r", 500).getLocation() == null);

        // ---- StandardProtocolFamily ----
        eq(StandardProtocolFamily.INET.name(), "INET");
        ck(StandardProtocolFamily.values().length == 3);
        ck(StandardProtocolFamily.valueOf("UNIX") == StandardProtocolFamily.UNIX);
        ck(StandardProtocolFamily.INET6 instanceof java.net.ProtocolFamily);

        // ---- Inet4Address: literales y predicados ----
        InetAddress a1 = InetAddress.getByName("1.2.3.4");
        ck(a1 instanceof Inet4Address);
        eq(a1.getHostAddress(), "1.2.3.4");
        // Ojo: `getHostName()` sobre una direccion sin nombre dispara una busqueda inversa en el
        // JDK real **y cachea el resultado**, con lo que cambia el `toString` de ese mismo objeto.
        // Depende de la red y por eso no se comprueba aca.
        eq(a1.toString(), "/1.2.3.4");
        ck(a1.hashCode() == 16909060);
        eq(InetAddress.getByName("localhost").toString(), "localhost/127.0.0.1");
        eq(InetAddress.getByName(null).toString(), "localhost/127.0.0.1");
        eq(InetAddress.getByName("").toString(), "localhost/127.0.0.1");
        eq(InetAddress.getLoopbackAddress().toString(), "localhost/127.0.0.1");
        ck(InetAddress.getLoopbackAddress() instanceof Inet4Address);
        eq(InetAddress.ofLiteral("1.2.3").getHostAddress(), "1.2.0.3");
        eq(InetAddress.ofLiteral("2130706433").getHostAddress(), "127.0.0.1");
        eq(InetAddress.ofLiteral("01.2.3.4").getHostAddress(), "1.2.3.4");
        eq(Inet4Address.ofLiteral("1.2.3").getHostAddress(), "1.2.0.3");
        eq(Inet4Address.ofPosixLiteral("0x7f000001").getHostAddress(), "127.0.0.1");
        eq(Inet4Address.ofPosixLiteral("010.2.3.4").getHostAddress(), "8.2.3.4");
        eq(Inet4Address.ofPosixLiteral("127.1").getHostAddress(), "127.0.0.1");
        eq(Inet4Address.ofPosixLiteral("1").getHostAddress(), "0.0.0.1");
        eq(Inet4Address.ofPosixLiteral("0x1.0x2.0x3.0x4").getHostAddress(), "1.2.3.4");
        eq(threw(() -> InetAddress.ofLiteral("1.2.3.4.5")), IAE);
        eq(threw(() -> InetAddress.ofLiteral("256.1.1.1")), IAE);
        eq(threw(() -> Inet4Address.ofPosixLiteral("256.1.1.1")), IAE);
        eq(threw(() -> Inet4Address.ofLiteral("::1")), IAE);
        InetAddress mc = InetAddress.ofLiteral("224.0.0.1");
        ck(mc.isMulticastAddress() && mc.isMCLinkLocal() && !mc.isMCGlobal());
        ck(InetAddress.ofLiteral("239.255.1.1").isMCSiteLocal());
        ck(InetAddress.ofLiteral("239.192.1.1").isMCOrgLocal());
        ck(InetAddress.ofLiteral("225.1.1.1").isMCGlobal());
        ck(InetAddress.ofLiteral("0.0.0.0").isAnyLocalAddress());
        ck(InetAddress.ofLiteral("127.5.5.5").isLoopbackAddress());
        ck(InetAddress.ofLiteral("169.254.1.1").isLinkLocalAddress());
        ck(InetAddress.ofLiteral("10.0.0.1").isSiteLocalAddress());
        ck(InetAddress.ofLiteral("172.20.0.1").isSiteLocalAddress());
        ck(InetAddress.ofLiteral("192.168.0.1").isSiteLocalAddress());
        ck(!InetAddress.ofLiteral("172.32.0.1").isSiteLocalAddress());
        ck(!InetAddress.ofLiteral("1.2.3.4").isMCNodeLocal());
        ck(!InetAddress.ofLiteral("1.2.3.4").isReachable(0));
        eq(threw(() -> InetAddress.ofLiteral("1.2.3.4").isReachable(-1)), IAE);
        byte[] raw = InetAddress.ofLiteral("1.2.3.4").getAddress();
        ck(raw.length == 4 && raw[0] == 1 && raw[3] == 4);
        ck(InetAddress.getByAddress(new byte[] {1, 2, 3, 4}) instanceof Inet4Address);
        ck(InetAddress.getByAddress(new byte[16]) instanceof Inet6Address);
        byte[] mapped = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 1, 2, 3, 4};
        ck(InetAddress.getByAddress(mapped) instanceof Inet4Address);
        eq(InetAddress.getByAddress(mapped).getHostAddress(), "1.2.3.4");
        eq(InetAddress.getByAddress("h", new byte[] {1, 2, 3, 4}).toString(), "h/1.2.3.4");
        ck(InetAddress.getAllByName("1.2.3.4").length == 1);
        eq(threw(() -> InetAddress.getByAddress(new byte[5])),
                "java.net.UnknownHostException");

        // ---- Inet6Address ----
        InetAddress v6 = InetAddress.ofLiteral("::1");
        ck(v6 instanceof Inet6Address);
        eq(v6.getHostAddress(), "0:0:0:0:0:0:0:1");
        eq(v6.toString(), "/0:0:0:0:0:0:0:1");
        ck(v6.hashCode() == 1);
        ck(v6.isLoopbackAddress());
        eq(InetAddress.ofLiteral("2001:db8::1").getHostAddress(), "2001:db8:0:0:0:0:0:1");
        eq(InetAddress.ofLiteral("2001:DB8:0:0:0:0:0:1").getHostAddress(), "2001:db8:0:0:0:0:0:1");
        eq(InetAddress.ofLiteral("[::1]").getHostAddress(), "0:0:0:0:0:0:0:1");
        eq(InetAddress.getByName("[::1]").getHostAddress(), "0:0:0:0:0:0:0:1");
        eq(InetAddress.getByName("::1").getHostAddress(), "0:0:0:0:0:0:0:1");
        eq(InetAddress.ofLiteral("::").getHostAddress(), "0:0:0:0:0:0:0:0");
        ck(InetAddress.ofLiteral("::").isAnyLocalAddress());
        ck(InetAddress.ofLiteral("::ffff:1.2.3.4") instanceof Inet4Address);
        eq(InetAddress.ofLiteral("::ffff:1.2.3.4").getHostAddress(), "1.2.3.4");
        ck(InetAddress.ofLiteral("::1.2.3.4") instanceof Inet6Address);
        eq(InetAddress.ofLiteral("::1.2.3.4").getHostAddress(), "0:0:0:0:0:0:102:304");
        eq(InetAddress.ofLiteral("1:2:3:4:5:6:1.2.3.4").getHostAddress(), "1:2:3:4:5:6:102:304");
        eq(InetAddress.ofLiteral("::ffff:0:1.2.3.4").getHostAddress(), "0:0:0:0:ffff:0:102:304");
        eq(InetAddress.ofLiteral("fe80::1%3").getHostAddress(), "fe80:0:0:0:0:0:0:1%3");
        ck(((Inet6Address) InetAddress.ofLiteral("fe80::1%3")).getScopeId() == 3);
        ck(((Inet6Address) InetAddress.ofLiteral("::1")).getScopeId() == 0);
        eq(Inet6Address.getByAddress("h", new byte[16], 5).toString(), "h/0:0:0:0:0:0:0:0%5");
        eq(Inet6Address.getByAddress("h", new byte[16], 0).toString(), "h/0:0:0:0:0:0:0:0%0");
        eq(Inet6Address.getByAddress("h", new byte[16], -1).getHostAddress(), "0:0:0:0:0:0:0:0");
        eq(threw(() -> Inet6Address.getByAddress("h", new byte[4], 1)),
                "java.net.UnknownHostException");
        ck(((Inet6Address) InetAddress.ofLiteral("::1")).isIPv4CompatibleAddress());
        ck(!((Inet6Address) InetAddress.ofLiteral("2001:db8::1")).isIPv4CompatibleAddress());
        ck(InetAddress.ofLiteral("ff02::1").isMulticastAddress());
        ck(InetAddress.ofLiteral("ff02::1").isMCLinkLocal());
        ck(InetAddress.ofLiteral("ff0e::1").isMCGlobal());
        ck(InetAddress.ofLiteral("ff01::1").isMCNodeLocal());
        ck(InetAddress.ofLiteral("ff05::1").isMCSiteLocal());
        ck(InetAddress.ofLiteral("ff08::1").isMCOrgLocal());
        ck(InetAddress.ofLiteral("fe80::1").isLinkLocalAddress());
        ck(InetAddress.ofLiteral("fec0::1").isSiteLocalAddress());
        ck(InetAddress.ofLiteral("::1").equals(InetAddress.ofLiteral("::1")));
        ck(!InetAddress.ofLiteral("::1").equals(InetAddress.ofLiteral("1.2.3.4")));
        ck(InetAddress.ofLiteral("::1").getAddress().length == 16);
        eq(threw(() -> InetAddress.ofLiteral(":::1")), IAE);
        eq(threw(() -> InetAddress.ofLiteral("1::2::3")), IAE);
        eq(threw(() -> InetAddress.ofLiteral("1:2:3:4:5:6:7:8:9")), IAE);
        eq(threw(() -> InetAddress.ofLiteral("1:2:3:4:5:6:7")), IAE);
        eq(threw(() -> Inet6Address.ofLiteral("1.2.3.4")), IAE);
        ck(Inet6Address.ofLiteral("::ffff:1.2.3.4") instanceof Inet4Address);

        // ---- InetSocketAddress ----
        InetSocketAddress s1 = new InetSocketAddress(80);
        eq(s1.toString(), "0.0.0.0/0.0.0.0:80");
        eq(s1.getHostName(), "0.0.0.0");
        eq(s1.getHostString(), "0.0.0.0");
        ck(!s1.isUnresolved());
        ck(s1.getPort() == 80);
        ck(s1 instanceof SocketAddress);
        InetSocketAddress s2 = InetSocketAddress.createUnresolved("ejemplo.org", 8080);
        eq(s2.toString(), "ejemplo.org/<unresolved>:8080");
        ck(s2.getAddress() == null);
        eq(s2.getHostName(), "ejemplo.org");
        eq(s2.getHostString(), "ejemplo.org");
        ck(s2.isUnresolved());
        ck(s2.hashCode() == "ejemplo.org".hashCode() + 8080);
        ck(s2.equals(InetSocketAddress.createUnresolved("ejemplo.org", 8080)));
        ck(!s2.equals(InetSocketAddress.createUnresolved("ejemplo.org", 8081)));
        InetSocketAddress s3 =
                new InetSocketAddress(InetAddress.getByAddress(new byte[] {1, 2, 3, 4}), 99);
        eq(s3.toString(), "/1.2.3.4:99");
        eq(s3.getHostString(), "1.2.3.4");
        ck(s3.hashCode() == 16909060 + 99);
        eq(new InetSocketAddress((InetAddress) null, 1).toString(), "0.0.0.0/0.0.0.0:1");
        eq(new InetSocketAddress("1.2.3.4", 7).toString(), "/1.2.3.4:7");
        ck(!new InetSocketAddress("1.2.3.4", 7).isUnresolved());
        eq(new InetSocketAddress(InetAddress.ofLiteral("::1"), 7).toString(),
                "/[0:0:0:0:0:0:0:1]:7");
        eq(InetSocketAddress.createUnresolved("::1", 7).toString(), "::1/<unresolved>:7");
        eq(threw(() -> new InetSocketAddress(-1)), IAE);
        eq(threw(() -> new InetSocketAddress(65536)), IAE);
        eq(threw(() -> new InetSocketAddress((String) null, 1)), IAE);

        // ---- URLEncoder / URLDecoder ----
        eq(URLEncoder.encode("a b+c/d?e=f&g~h*i.j-k_l", "UTF-8"),
                "a+b%2Bc%2Fd%3Fe%3Df%26g%7Eh*i.j-k_l");
        eq(URLEncoder.encode("holañ", "UTF-8"), "hola%C3%B1");
        eq(URLEncoder.encode("😀", "UTF-8"), "%F0%9F%98%80");
        eq(URLEncoder.encode("", "UTF-8"), "");
        eq(URLEncoder.encode("abcXYZ0189", "UTF-8"), "abcXYZ0189");
        eq(URLDecoder.decode("a+b%20c", "UTF-8"), "a b c");
        eq(URLDecoder.decode("%C3%B1", "UTF-8"), "ñ");
        eq(URLDecoder.decode("abc", "UTF-8"), "abc");
        eq(URLDecoder.decode("%F0%9F%98%80", "UTF-8"), "😀");
        eq(threw(() -> URLDecoder.decode("%zz", StandardCharsets.UTF_8)), IAE);
        eq(threw(() -> URLDecoder.decode("%A", StandardCharsets.UTF_8)), IAE);
        eq(threw(() -> URLEncoder.encode("x", "NOPE")),
                "java.io.UnsupportedEncodingException");
        eq(URLEncoder.encode("a b", StandardCharsets.UTF_8), "a+b");
        eq(URLDecoder.decode("a+b", StandardCharsets.UTF_8), "a b");

        // ---- Proxy / ProxySelector ----
        eq(Proxy.NO_PROXY.toString(), "DIRECT");
        ck(Proxy.NO_PROXY.type() == Proxy.Type.DIRECT);
        ck(Proxy.NO_PROXY.address() == null);
        Proxy px = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("p", 8080));
        eq(px.toString(), "HTTP @ p/<unresolved>:8080");
        ck(px.equals(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("p", 8080))));
        ck(!px.equals(Proxy.NO_PROXY));
        eq(threw(() -> new Proxy(Proxy.Type.DIRECT,
                InetSocketAddress.createUnresolved("p", 8080))), IAE);
        eq(threw(() -> new Proxy(Proxy.Type.HTTP, null)), IAE);
        ck(Proxy.Type.values().length == 3);
        eq(ProxySelector.of(InetSocketAddress.createUnresolved("p", 8080))
                .select(new URI("http://x/")).toString(), "[HTTP @ p/<unresolved>:8080]");
        eq(ProxySelector.of(null).select(new URI("http://x/")).toString(), "[DIRECT]");
        eq(ProxySelector.of(InetSocketAddress.createUnresolved("p", 8080))
                .select(new URI("ftp://x/")).toString(), "[DIRECT]");
        eq(threw(() -> ProxySelector.of(null).select(null)), IAE);

        // ---- PasswordAuthentication ----
        PasswordAuthentication pa = new PasswordAuthentication("u", new char[] {'p', 'w'});
        eq(pa.getUserName(), "u");
        eq(new String(pa.getPassword()), "pw");
        char[] pw = new char[] {'a', 'b'};
        PasswordAuthentication pa2 = new PasswordAuthentication("u", pw);
        pw[0] = 'z';
        eq(new String(pa2.getPassword()), "ab");

        // ---- StandardSocketOptions ----
        SocketOption<Boolean> ka = StandardSocketOptions.SO_KEEPALIVE;
        eq(ka.name(), "SO_KEEPALIVE");
        ck(ka.type() == Boolean.class);
        eq(ka.toString(), "SO_KEEPALIVE");
        ck(StandardSocketOptions.SO_SNDBUF.type() == Integer.class);
        eq(StandardSocketOptions.TCP_NODELAY.name(), "TCP_NODELAY");
        eq(StandardSocketOptions.IP_TOS.name(), "IP_TOS");
        eq(StandardSocketOptions.SO_REUSEPORT.name(), "SO_REUSEPORT");

        // ---- UnixDomainSocketAddress ----
        UnixDomainSocketAddress u1 = UnixDomainSocketAddress.of("/tmp/s");
        eq(u1.toString(), u1.getPath().toString());
        ck(u1.equals(UnixDomainSocketAddress.of("/tmp/s")));
        ck(!u1.equals(UnixDomainSocketAddress.of("/tmp/t")));
        ck(u1.hashCode() == u1.getPath().hashCode());
        ck(u1 instanceof SocketAddress);
        eq(UnixDomainSocketAddress.of("").getPath().toString(), "");

        // ---- HttpCookie ----
        List<HttpCookie> cs = HttpCookie.parse("foo=bar");
        ck(cs.size() == 1);
        HttpCookie c0 = cs.get(0);
        eq(c0.getName(), "foo");
        eq(c0.getValue(), "bar");
        ck(c0.getVersion() == 0);
        ck(c0.getMaxAge() == -1);
        ck(!c0.hasExpired());
        eq(c0.toString(), "foo=bar");
        eq(HttpCookie.parse("Set-Cookie: foo=bar").get(0).getName(), "foo");
        HttpCookie c2 = HttpCookie.parse("set-cookie2: foo=bar").get(0);
        ck(c2.getVersion() == 1);
        eq(c2.toString(), "foo=\"bar\"");
        HttpCookie c3 = HttpCookie.parse(
                "foo=bar; Path=/x; Domain=.a.com; Secure; HttpOnly; Max-Age=100").get(0);
        ck(c3.getVersion() == 1);
        eq(c3.getPath(), "/x");
        eq(c3.getDomain(), ".a.com");
        ck(c3.getSecure() && c3.isHttpOnly());
        ck(c3.getMaxAge() == 100);
        eq(c3.toString(), "foo=\"bar\";$Path=\"/x\";$Domain=\".a.com\"");
        HttpCookie c4 = HttpCookie.parse("foo=bar; Version=1; Comment=hi; "
                + "CommentURL=\"http://c\"; Discard; Port=\"80,81\"").get(0);
        eq(c4.getComment(), "hi");
        eq(c4.getCommentURL(), "http://c");
        ck(c4.getDiscard());
        eq(c4.getPortlist(), "80,81");
        eq(c4.toString(), "foo=\"bar\";$Port=\"80,81\"");
        eq(HttpCookie.parse("foo=\"quoted value\"").get(0).getValue(), "quoted value");
        HttpCookie c5 = HttpCookie.parse("foo=bar; Expires=Thu, 01 Jan 1970 00:00:00 GMT").get(0);
        ck(c5.getMaxAge() == 0);
        ck(c5.hasExpired());
        ck(c5.getVersion() == 0);
        // Una fecha lejana en el futuro tiene que dar un maxAge grande; el valor exacto depende del
        // reloj, asi que se comprueba el orden de magnitud y no el numero.
        HttpCookie c5b = HttpCookie.parse("foo=bar; Expires=Wed, 09 Jun 2100 10:18:14 GMT").get(0);
        ck(c5b.getMaxAge() > 2000000000L && !c5b.hasExpired());
        // Una fecha que no se entiende vence la cookie en el acto.
        ck(HttpCookie.parse("foo=bar; Expires=no es una fecha").get(0).getMaxAge() == 0);
        List<HttpCookie> c6 = HttpCookie.parse("a=1, b=2");
        ck(c6.size() == 1);
        eq(c6.get(0).getValue(), "1, b=2");
        eq(HttpCookie.parse("foo=bar; badattr=1").get(0).getValue(), "bar");
        eq(threw(() -> HttpCookie.parse("foo")), IAE);
        eq(threw(() -> HttpCookie.parse("=bar")), IAE);
        HttpCookie c7 = new HttpCookie("n", "v");
        ck(c7.getVersion() == 1);
        eq(c7.toString(), "n=\"v\"");
        c7.setPath("/p");
        c7.setDomain(".d.com");
        c7.setPortlist("80");
        eq(c7.toString(), "n=\"v\";$Path=\"/p\";$Domain=\".d.com\";$Port=\"80\"");
        eq(threw(() -> new HttpCookie("$n", "v")), IAE);
        eq(threw(() -> new HttpCookie("a,b", "v")), IAE);
        eq(threw(() -> new HttpCookie("n", "v").setVersion(2)), IAE);
        ck(HttpCookie.domainMatches(".foo.com", "x.foo.com"));
        ck(HttpCookie.domainMatches(".foo.com", "foo.com"));
        ck(!HttpCookie.domainMatches(".foo.com", "a.b.foo.com"));
        ck(HttpCookie.domainMatches("foo.com", "foo.com"));
        ck(!HttpCookie.domainMatches(".com", "x.com"));
        ck(!HttpCookie.domainMatches("local", "local"));
        ck(!HttpCookie.domainMatches(null, "x"));
        ck(new HttpCookie("A", "1").equals(new HttpCookie("a", "2")));
        ck(new HttpCookie("A", "1").hashCode() == "a".hashCode());
        HttpCookie cl = (HttpCookie) new HttpCookie("n", "v").clone();
        eq(cl.getName(), "n");

        // ---- CookieManager / CookieStore / CookiePolicy ----
        ck(CookieHandler.getDefault() == null);
        CookieManager cm = new CookieManager();
        ck(cm instanceof CookieHandler);
        ck(cm.getCookieStore().getCookies().isEmpty());
        Map<String, List<String>> hdrs = new HashMap<String, List<String>>();
        List<String> setCookie = new ArrayList<String>();
        setCookie.add("foo=bar; Path=/");
        hdrs.put("Set-Cookie", setCookie);
        cm.put(new URI("http://ejemplo.org/dir/pag"), hdrs);
        eq(cm.getCookieStore().getCookies().toString(), "[foo=bar]");
        eq(cm.get(new URI("http://ejemplo.org/dir/pag"),
                new HashMap<String, List<String>>()).toString(), "{Cookie=[foo=bar]}");
        eq(cm.get(new URI("http://otro.org/"),
                new HashMap<String, List<String>>()).toString(), "{Cookie=[]}");
        eq(cm.getCookieStore().getURIs().toString(), "[http://ejemplo.org]");
        eq(cm.getCookieStore().get(new URI("http://ejemplo.org/")).toString(), "[foo=bar]");
        ck(!cm.getCookieStore().remove(new URI("http://ejemplo.org/"),
                new HttpCookie("foo", "bar")));
        HttpCookie exact = new HttpCookie("foo", "bar");
        exact.setPath("/");
        exact.setDomain("ejemplo.org");
        ck(cm.getCookieStore().remove(new URI("http://ejemplo.org/"), exact));
        ck(cm.getCookieStore().getCookies().isEmpty());
        cm.put(new URI("http://ejemplo.org/dir/pag"), hdrs);
        ck(cm.getCookieStore().removeAll());
        ck(!cm.getCookieStore().removeAll());
        CookieManager cm2 = new CookieManager(null, CookiePolicy.ACCEPT_NONE);
        cm2.put(new URI("http://ejemplo.org/"), hdrs);
        ck(cm2.getCookieStore().getCookies().isEmpty());
        ck(CookiePolicy.ACCEPT_ALL.shouldAccept(new URI("http://x/"), new HttpCookie("a", "b")));
        ck(!CookiePolicy.ACCEPT_NONE.shouldAccept(new URI("http://x/"), new HttpCookie("a", "b")));
        ck(!CookiePolicy.ACCEPT_ORIGINAL_SERVER.shouldAccept(
                new URI("http://ejemplo.org/"), new HttpCookie("a", "b")));
        HttpCookie same = new HttpCookie("a", "b");
        same.setDomain("ejemplo.org");
        ck(CookiePolicy.ACCEPT_ORIGINAL_SERVER.shouldAccept(new URI("http://ejemplo.org/"), same));
        CookieStore st = cm2.getCookieStore();
        ck(st.getURIs().isEmpty());
        // La ruta por defecto es el directorio de la pagina, no la pagina.
        CookieManager cm3 = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        List<String> sc3 = new ArrayList<String>();
        sc3.add("k=v");
        Map<String, List<String>> h3 = new HashMap<String, List<String>>();
        h3.put("Set-Cookie", sc3);
        cm3.put(new URI("http://sitio.org/dir/pag"), h3);
        eq(cm3.getCookieStore().getCookies().get(0).getPath(), "/dir/");
        eq(cm3.getCookieStore().getCookies().get(0).getDomain(), "sitio.org");
        eq(cm3.get(new URI("http://sitio.org/otro"),
                new HashMap<String, List<String>>()).toString(), "{Cookie=[]}");
        eq(cm3.get(new URI("http://sitio.org/dir/x"),
                new HashMap<String, List<String>>()).toString(), "{Cookie=[k=v]}");

        // ---- permisos ----
        NetPermission np = new NetPermission("setDefaultAuthenticator");
        eq(np.getName(), "setDefaultAuthenticator");
        eq(np.getActions(), "");
        ck(np.implies(new NetPermission("setDefaultAuthenticator")));
        ck(!np.implies(new NetPermission("otro")));
        ck(new NetPermission("*").implies(new NetPermission("cualquiera")));
        URLPermission up = new URLPermission("http://x.com/a/*", "GET,POST:Accept");
        eq(up.getName(), "http://x.com/a/*");
        eq(up.getActions(), "GET,POST:Accept");
        ck(up.implies(new URLPermission("http://x.com/a/b", "GET:Accept")));
        ck(!up.implies(new URLPermission("http://y.com/a/b", "GET:Accept")));
        ck(!up.implies(new URLPermission("http://x.com/a/b/c", "GET:Accept")));
        ck(new URLPermission("http://x.com/-", "*:*")
                .implies(new URLPermission("http://x.com/a/b/c", "GET")));
        eq(new URLPermission("http://x.com/-").getActions(), "*:*");
        eq(new URLPermission("http://x.com/a", "GET").getActions(), "GET:");
        eq(new URLPermission("http://x.com/a", ":X").getActions(), ":X");
        eq(new URLPermission("http://x.com/a", "GET:X-A,X-B").getActions(), "GET:X-A,X-B");
        ck(up.equals(new URLPermission("http://x.com/a/*", "GET,POST:Accept")));
        ck(up.hashCode() == new URLPermission("http://x.com/a/*", "GET,POST:Accept").hashCode());
        eq(threw(() -> new URLPermission("no-scheme")), IAE);

        // ---- URLConnection: configuracion y lectura de una conexion base ----
        // La subclase anonima es la forma en que el JDK deja instanciar la clase: `connect()` es su
        // unico abstracto, y una que no conecta sigue siendo una URLConnection valida para todo lo
        // que se configura antes de conectar.
        URL uc0 = new URL("http://ejemplo.org/a");
        URLConnection uc = new URLConnection(uc0) {
            public void connect() throws IOException {
            }
        };
        ck(uc.getURL() == uc0);
        ck(uc.getDoInput());
        ck(!uc.getDoOutput());
        ck(!uc.getAllowUserInteraction());
        ck(!URLConnection.getDefaultAllowUserInteraction());
        ck(uc.getUseCaches());
        ck(uc.getIfModifiedSince() == 0);
        ck(uc.getConnectTimeout() == 0);
        ck(uc.getReadTimeout() == 0);
        ck(uc.getContentLength() == -1);
        ck(uc.getContentLengthLong() == -1L);
        ck(uc.getContentType() == null);
        ck(uc.getContentEncoding() == null);
        ck(uc.getExpiration() == 0);
        ck(uc.getDate() == 0);
        ck(uc.getLastModified() == 0);
        ck(uc.getHeaderField("x") == null);
        ck(uc.getHeaderFields().isEmpty());
        ck(uc.getHeaderFieldInt("x", 7) == 7);
        ck(uc.getHeaderFieldLong("x", 8L) == 8L);
        ck(uc.getHeaderFieldDate("x", 9L) == 9L);
        ck(uc.getHeaderFieldKey(0) == null);
        ck(uc.getHeaderField(0) == null);
        ck(uc.toString().endsWith(":http://ejemplo.org/a"));
        uc.setDoOutput(true);
        ck(uc.getDoOutput());
        uc.setConnectTimeout(1500);
        uc.setReadTimeout(700);
        ck(uc.getConnectTimeout() == 1500 && uc.getReadTimeout() == 700);
        eq(threw(() -> uc.setConnectTimeout(-1)), IAE);
        uc.setIfModifiedSince(1234L);
        ck(uc.getIfModifiedSince() == 1234L);
        uc.setUseCaches(false);
        ck(!uc.getUseCaches());
        // Las cabeceras del pedido no distinguen mayusculas para buscar, pero conservan la caja con
        // que se escribieron y el orden en que se agregaron.
        uc.setRequestProperty("A", "1");
        uc.addRequestProperty("A", "2");
        uc.addRequestProperty("b", "3");
        eq(uc.getRequestProperty("a"), "2");
        eq(uc.getRequestProperties().toString(), "{A=[1, 2], b=[3]}");
        ck(URLConnection.getDefaultRequestProperty("A") == null);
        ck(uc.getPermission() instanceof java.security.AllPermission);
        eq(threw(() -> uc.getInputStream()), "java.net.UnknownServiceException");
        eq(threw(() -> uc.getOutputStream()), "java.net.UnknownServiceException");
        eq(threw(() -> uc.getContent()), "java.net.UnknownServiceException");
        ck(URLConnection.getDefaultUseCaches("HTTP"));
        URLConnection.setDefaultUseCaches("http", false);
        ck(!URLConnection.getDefaultUseCaches("HTTP"));
        URLConnection.setDefaultUseCaches("http", true);
        ck(URLConnection.getDefaultUseCaches("http"));

        // Las cabeceras las pone la subclase; de ahi salen todos los getX derivados.
        URLConnection uh = new URLConnection(uc0) {
            public void connect() throws IOException {
            }

            public String getHeaderField(String n) {
                if (n.equals("content-length")) {
                    return "42";
                }
                if (n.equals("content-type")) {
                    return "text/html; charset=utf-8";
                }
                if (n.equals("last-modified")) {
                    return "Sun, 06 Nov 1994 08:49:37 GMT";
                }
                return null;
            }
        };
        ck(uh.getContentLength() == 42);
        ck(uh.getContentLengthLong() == 42L);
        eq(uh.getContentType(), "text/html; charset=utf-8");
        ck(uh.getLastModified() == 784111777000L);
        ck(uh.getDate() == 0);

        // ---- URLConnection: adivinar el tipo ----
        eq(URLConnection.guessContentTypeFromName("x.html"), "text/html");
        eq(URLConnection.guessContentTypeFromName("x.htm"), "text/html");
        eq(URLConnection.guessContentTypeFromName("x.txt"), "text/plain");
        eq(URLConnection.guessContentTypeFromName("x.gif"), "image/gif");
        eq(URLConnection.guessContentTypeFromName("x.png"), "image/png");
        eq(URLConnection.guessContentTypeFromName("x.jpg"), "image/jpeg");
        eq(URLConnection.guessContentTypeFromName("x.json"), "application/json");
        eq(URLConnection.guessContentTypeFromName("x.xml"), "application/xml");
        eq(URLConnection.guessContentTypeFromName("x.zip"), "application/zip");
        eq(URLConnection.guessContentTypeFromName("x.pdf"), "application/pdf");
        eq(URLConnection.guessContentTypeFromName("x.css"), "text/css");
        ck(URLConnection.guessContentTypeFromName("x") == null);
        ck(URLConnection.guessContentTypeFromName("x.noexiste") == null);
        eq(sniff(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}), "image/png");
        eq(sniff(new byte[] {'G', 'I', 'F', '8', '7', 'a'}), "image/gif");
        eq(sniff(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}), "image/jpeg");
        eq(sniff(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}),
                "application/java-vm");
        eq(sniff(new byte[] {(byte) 0xAC, (byte) 0xED, 0, 5}),
                "application/x-java-serialized-object");
        eq(sniff("<html>".getBytes(StandardCharsets.US_ASCII)), "text/html");
        eq(sniff("<!DOCTYPE html".getBytes(StandardCharsets.US_ASCII)), "text/html");
        eq(sniff("<?xml ".getBytes(StandardCharsets.US_ASCII)), "application/xml");
        eq(sniff("#define".getBytes(StandardCharsets.US_ASCII)), "image/x-bitmap");
        eq(sniff(".snd".getBytes(StandardCharsets.US_ASCII)), "audio/basic");
        ck(sniff("zzzz".getBytes(StandardCharsets.US_ASCII)) == null);
        ck(URLConnection.guessContentTypeFromStream(new InputStream() {
            public int read() {
                return -1;
            }
        }) == null);

        // ---- HttpURLConnection ----
        HttpURLConnection hc = mkHttp("http://ejemplo.org:8080/a");
        ck(hc instanceof URLConnection);
        ck(HttpURLConnection.HTTP_OK == 200);
        ck(HttpURLConnection.HTTP_CREATED == 201);
        ck(HttpURLConnection.HTTP_RESET == 205);
        ck(HttpURLConnection.HTTP_MULT_CHOICE == 300);
        ck(HttpURLConnection.HTTP_NOT_MODIFIED == 304);
        ck(HttpURLConnection.HTTP_BAD_REQUEST == 400);
        ck(HttpURLConnection.HTTP_NOT_FOUND == 404);
        ck(HttpURLConnection.HTTP_REQ_TOO_LONG == 414);
        ck(HttpURLConnection.HTTP_UNSUPPORTED_TYPE == 415);
        ck(HttpURLConnection.HTTP_INTERNAL_ERROR == 500);
        ck(HttpURLConnection.HTTP_GATEWAY_TIMEOUT == 504);
        ck(HttpURLConnection.HTTP_VERSION == 505);
        eq(hc.getRequestMethod(), "GET");
        ck(HttpURLConnection.getFollowRedirects());
        ck(hc.getInstanceFollowRedirects());
        hc.setInstanceFollowRedirects(false);
        ck(!hc.getInstanceFollowRedirects());
        hc.setRequestMethod("POST");
        eq(hc.getRequestMethod(), "POST");
        eq(threw(() -> hc.setRequestMethod("PATCH")), "java.net.ProtocolException");
        eq(threw(() -> hc.setRequestMethod("get")), "java.net.ProtocolException");
        eq(hc.getRequestMethod(), "POST");
        ck(hc.getErrorStream() == null);
        ck(hc.getHeaderFieldKey(0) == null);
        ck(hc.getHeaderField(0) == null);
        eq(threw(() -> hc.getResponseCode()), "java.net.UnknownServiceException");
        eq(threw(() -> hc.setAuthenticator(null)), "java.lang.UnsupportedOperationException");
        eq(threw(() -> hc.setFixedLengthStreamingMode(-1)), IAE);
        eq(threw(() -> hc.setFixedLengthStreamingMode(-1L)), IAE);
        hc.setFixedLengthStreamingMode(10);
        eq(threw(() -> hc.setChunkedStreamingMode(5)), "java.lang.IllegalStateException");
        HttpURLConnection hc2 = mkHttp("http://ejemplo.org:8080/a");
        hc2.setChunkedStreamingMode(-5);
        eq(threw(() -> hc2.setFixedLengthStreamingMode(5)), "java.lang.IllegalStateException");
        java.security.Permission hp = hc.getPermission();
        ck(hp instanceof SocketPermission);
        eq(hp.getName(), "ejemplo.org:8080");
        eq(hp.getActions(), "connect,resolve");
        // El formato asctime no trae zona; `HttpURLConnection` la fuerza a GMT, asi que da el mismo
        // instante que el formato de preferencia.
        HttpURLConnection hd = new HttpURLConnection(new URL("http://x/")) {
            public void connect() throws IOException {
            }

            public void disconnect() {
            }

            public boolean usingProxy() {
                return false;
            }

            public String getHeaderField(String n) {
                if (n.equals("d1")) {
                    return "Sun, 06 Nov 1994 08:49:37 GMT";
                }
                if (n.equals("d2")) {
                    return "Sunday, 06-Nov-94 08:49:37 GMT";
                }
                if (n.equals("d3")) {
                    return "Sun Nov  6 08:49:37 1994";
                }
                return null;
            }
        };
        ck(hd.getHeaderFieldDate("d1", -1L) == 784111777000L);
        ck(hd.getHeaderFieldDate("d2", -1L) == 784111777000L);
        ck(hd.getHeaderFieldDate("d3", -1L) == 784111777000L);
        ck(hd.getHeaderFieldDate("d4", -1L) == -1L);

        // ---- SocketPermission ----
        SocketPermission sp = new SocketPermission("host.com:80", "connect");
        eq(sp.getName(), "host.com:80");
        eq(sp.getActions(), "connect,resolve");
        eq(new SocketPermission("host.com:80", "CONNECT, Resolve").getActions(), "connect,resolve");
        eq(new SocketPermission("host.com", "accept,connect,listen,resolve").getActions(),
                "connect,listen,accept,resolve");
        eq(new SocketPermission("host.com", "listen").getActions(), "listen,resolve");
        eq(new SocketPermission("", "listen").getName(), "localhost");
        eq(threw(() -> new SocketPermission("host.com", "")), IAE);
        eq(threw(() -> new SocketPermission("host.com", "bogus")), IAE);
        eq(threw(() -> new SocketPermission("host.com", null)), "java.lang.NullPointerException");
        ck(sp.implies(new SocketPermission("host.com:80", "connect")));
        ck(sp.implies(new SocketPermission("host.com:80", "resolve")));
        ck(!new SocketPermission("host.com:80", "resolve")
                .implies(new SocketPermission("host.com:80", "connect")));
        ck(new SocketPermission("*.host.com:80", "connect")
                .implies(new SocketPermission("a.host.com:80", "connect")));
        ck(!new SocketPermission("*.host.com:80", "connect")
                .implies(new SocketPermission("host.com:80", "connect")));
        ck(new SocketPermission("host.com:1-100", "connect")
                .implies(new SocketPermission("host.com:50", "connect")));
        ck(!new SocketPermission("host.com:1-100", "connect")
                .implies(new SocketPermission("host.com:200", "connect")));
        ck(new SocketPermission("*", "connect")
                .implies(new SocketPermission("host.com:80", "connect")));
        ck(new SocketPermission("host.com:80", "connect,resolve")
                .equals(new SocketPermission("host.com:80", "resolve,connect")));
        ck(new SocketPermission("host.com:80", "connect").hashCode()
                == new SocketPermission("host.com:80", "resolve").hashCode());
        java.security.PermissionCollection spc = sp.newPermissionCollection();
        spc.add(sp);
        ck(spc.implies(new SocketPermission("host.com:80", "connect")));
        ck(!spc.implies(new SocketPermission("otro.com:80", "connect")));

        // ---- Socket sin conectar ----
        Socket sk = new Socket();
        eq(sk.toString(), "Socket[unconnected]");
        ck(!sk.isConnected() && !sk.isBound() && !sk.isClosed());
        ck(!sk.isInputShutdown() && !sk.isOutputShutdown());
        ck(sk.getInetAddress() == null);
        eq(sk.getLocalAddress().getHostAddress(), "0.0.0.0");
        ck(sk.getPort() == 0);
        ck(sk.getLocalPort() == -1);
        ck(sk.getRemoteSocketAddress() == null);
        ck(sk.getLocalSocketAddress() == null);
        ck(sk.getChannel() == null);
        ck(!sk.getTcpNoDelay());
        ck(sk.getSoLinger() == -1);
        ck(!sk.getOOBInline());
        ck(sk.getSoTimeout() == 0);
        ck(!sk.getKeepAlive());
        ck(sk.getTrafficClass() == 0);
        ck(!sk.getReuseAddress());
        sk.setTcpNoDelay(true);
        ck(sk.getTcpNoDelay());
        sk.setKeepAlive(true);
        ck(sk.getKeepAlive());
        sk.setOOBInline(true);
        ck(sk.getOOBInline());
        sk.setSoTimeout(250);
        ck(sk.getSoTimeout() == 250);
        sk.setSoLinger(true, 5);
        ck(sk.getSoLinger() == 5);
        sk.setSoLinger(false, 5);
        ck(sk.getSoLinger() == -1);
        // El valor de `IP_TOS` NO se comprueba de ida y vuelta: el JDK real lo delega al socket del
        // sistema, que sobre un socket sin conectar puede no guardarlo. Lo que si es identico en las
        // dos VMs es el rechazo de un valor fuera de rango, que es validacion nuestra.
        sk.setReuseAddress(true);
        ck(sk.getReuseAddress());
        sk.setSendBufferSize(4096);
        ck(sk.getSendBufferSize() == 4096);
        sk.setReceiveBufferSize(8192);
        ck(sk.getReceiveBufferSize() == 8192);
        sk.setPerformancePreferences(1, 2, 3);
        eq(threw(() -> sk.setSoTimeout(-1)), IAE);
        eq(threw(() -> sk.setTrafficClass(-1)), IAE);
        eq(threw(() -> sk.setTrafficClass(256)), IAE);
        eq(threw(() -> sk.setReceiveBufferSize(0)), IAE);
        eq(threw(() -> sk.setSendBufferSize(0)), IAE);
        eq(threw(() -> sk.setSoLinger(true, -1)), IAE);
        eq(threw(() -> sk.shutdownInput()), "java.net.SocketException");
        eq(threw(() -> sk.shutdownOutput()), "java.net.SocketException");
        ck(sk.getOption(StandardSocketOptions.TCP_NODELAY).booleanValue());
        sk.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.FALSE);
        ck(!sk.getOption(StandardSocketOptions.TCP_NODELAY).booleanValue());
        ck(sk.getOption(StandardSocketOptions.SO_RCVBUF).intValue() == 8192);
        ck(sk.supportedOptions().contains(StandardSocketOptions.TCP_NODELAY));
        ck(sk.supportedOptions().contains(StandardSocketOptions.SO_LINGER));
        eq(threw(() -> new Socket((Proxy) null)), IAE);
        eq(new Socket(Proxy.NO_PROXY).toString(), "Socket[unconnected]");
        sk.close();
        ck(sk.isClosed());
        sk.close();
        eq(threw(() -> sk.getTcpNoDelay()), "java.net.SocketException");
        eq(threw(() -> sk.setSoTimeout(1)), "java.net.SocketException");

        // ---- ServerSocket sin atar ----
        ServerSocket ss = new ServerSocket();
        eq(ss.toString(), "ServerSocket[unbound]");
        ck(!ss.isBound() && !ss.isClosed());
        ck(ss.getInetAddress() == null);
        ck(ss.getLocalPort() == -1);
        ck(ss.getLocalSocketAddress() == null);
        ck(ss.getChannel() == null);
        ck(ss.getSoTimeout() == 0);
        ck(!ss.getReuseAddress());
        ss.setSoTimeout(300);
        ck(ss.getSoTimeout() == 300);
        ss.setReuseAddress(true);
        ck(ss.getReuseAddress());
        ss.setReceiveBufferSize(4096);
        ck(ss.getReceiveBufferSize() == 4096);
        ss.setPerformancePreferences(1, 2, 3);
        eq(threw(() -> ss.setSoTimeout(-1)), IAE);
        eq(threw(() -> ss.setReceiveBufferSize(0)), IAE);
        ck(ss.getOption(StandardSocketOptions.SO_RCVBUF).intValue() == 4096);
        ss.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE);
        ck(!ss.getReuseAddress());
        ck(ss.supportedOptions().contains(StandardSocketOptions.SO_REUSEADDR));
        ss.close();
        ck(ss.isClosed());
        eq(threw(() -> ss.getReuseAddress()), "java.net.SocketException");

        // ---- DatagramSocket sin atar ----
        // `new DatagramSocket(null)` es el caso que el JDK documenta como "socket sin atar", y es
        // el unico que KajiJDK puede dar: los otros constructores atan, y atar necesita al sistema.
        DatagramSocket ds = new DatagramSocket((SocketAddress) null);
        ck(!ds.isBound() && !ds.isConnected() && !ds.isClosed());
        ck(ds.getInetAddress() == null);
        ck(ds.getPort() == -1);
        ck(ds.getRemoteSocketAddress() == null);
        ck(ds.getLocalSocketAddress() == null);
        ck(ds.getLocalPort() == 0);
        eq(ds.getLocalAddress().getHostAddress(), "0.0.0.0");
        ck(ds.getChannel() == null);
        ck(ds.getSoTimeout() == 0);
        ds.setSoTimeout(120);
        ck(ds.getSoTimeout() == 120);
        ds.setBroadcast(false);
        ck(!ds.getBroadcast());
        ds.setReuseAddress(true);
        ck(ds.getReuseAddress());
        ds.setSendBufferSize(2048);
        ck(ds.getSendBufferSize() == 2048);
        ds.setReceiveBufferSize(4096);
        ck(ds.getReceiveBufferSize() == 4096);
        eq(threw(() -> ds.setSoTimeout(-1)), IAE);
        eq(threw(() -> ds.setTrafficClass(256)), IAE);
        eq(threw(() -> ds.setSendBufferSize(0)), IAE);
        InetAddress dip = InetAddress.getByAddress(new byte[] {10, 0, 0, 1});
        ds.connect(dip, 9999);
        ck(ds.isConnected());
        ck(ds.getPort() == 9999);
        ck(ds.getInetAddress().equals(dip));
        eq(ds.getRemoteSocketAddress().toString(), new InetSocketAddress(dip, 9999).toString());
        ds.disconnect();
        ck(!ds.isConnected() && ds.getPort() == -1);
        eq(threw(() -> ds.connect(null, 1)), IAE);
        eq(threw(() -> ds.connect(dip, -1)), IAE);
        eq(threw(() -> ds.connect((SocketAddress) null)), IAE);
        eq(threw(() -> ds.connect(InetSocketAddress.createUnresolved("h", 1))),
                "java.net.SocketException");
        ck(ds.getOption(StandardSocketOptions.SO_RCVBUF).intValue() == 4096);
        ck(ds.supportedOptions().contains(StandardSocketOptions.SO_BROADCAST));
        ds.close();
        ck(ds.isClosed());
        eq(threw(() -> ds.getSoTimeout()), "java.net.SocketException");

        // ---- MulticastSocket sin atar ----
        MulticastSocket ms = new MulticastSocket((SocketAddress) null);
        ck(ms instanceof DatagramSocket);
        ck(ms.getTimeToLive() == 1);
        ms.setTimeToLive(9);
        ck(ms.getTimeToLive() == 9);
        ck(ms.getTTL() == (byte) 9);
        ms.setTTL((byte) 4);
        ck(ms.getTimeToLive() == 4);
        eq(threw(() -> ms.setTimeToLive(-1)), IAE);
        eq(threw(() -> ms.setTimeToLive(256)), IAE);
        // `setInterface` NO se comprueba: el JDK real la delega al socket del sistema, que sobre un
        // socket sin atar todavia no existe, y tira `SocketException`. El TTL si se comprueba porque
        // el JDK lo guarda en Java hasta que haya socket.
        ms.close();
        ck(ms.isClosed());

        // ---- URLStreamHandler: la mitad que compara y escribe ----
        Ush ush = new Ush();
        ck(ush.puertoPorDefecto() == -1);
        eq(threw(() -> ush.porProxy(new URL("http://x/"))),
                "java.lang.UnsupportedOperationException");
        eq(ush.texto(new URL("http://x.com:8080/a/b?q=1#frag")), "http://x.com:8080/a/b?q=1#frag");
        eq(ush.texto(new URL("http://x.com/a")), "http://x.com/a");
        ck(ush.mismoArchivo(new URL("http://x.com/a"), new URL("http://x.com/a#f")));
        ck(!ush.iguales(new URL("http://x.com/a"), new URL("http://x.com/a#f")));
        ck(ush.iguales(new URL("http://x.com/a#f"), new URL("http://x.com/a#f")));
        ck(!ush.mismoArchivo(new URL("http://x.com/a"), new URL("http://x.com/b")));

        // ---- URLClassLoader ----
        // Se lo apunta a un directorio que no existe a proposito: lo que se comprueba es la lista de
        // URLs, el "no lo encontre" y el cierre, que son iguales en las dos VMs. Cargar de verdad
        // depende de donde este parada cada una y no se puede comparar.
        URL[] ucls = new URL[] {new URL("file:/no/such/dir/")};
        java.net.URLClassLoader ucl = new java.net.URLClassLoader(ucls, null);
        ck(ucl.getURLs().length == 1);
        eq(ucl.getURLs()[0].toString(), "file:/no/such/dir/");
        ck(ucl.getURLs() != ucl.getURLs());
        ck(ucl.findResource("no/such/Recurso.txt") == null);
        ck(!ucl.findResources("no/such/Recurso.txt").hasMoreElements());
        ck(ucl.getResourceAsStream("no/such/Recurso.txt") == null);
        ck(java.net.URLClassLoader.newInstance(ucls) != null);
        ck(ucl instanceof java.security.SecureClassLoader);
        ck(ucl instanceof java.io.Closeable);
        ucl.close();
        ck(ucl.findResource("no/such/Recurso.txt") == null);
    }

    // Los metodos utiles de `URLStreamHandler` son `protected`, y `protected` en Java no alcanza
    // desde afuera del paquete ni siquiera sobre una instancia propia: hay que llamarlos desde
    // adentro de la subclase. De ahi los envoltorios.
    private static class Ush extends URLStreamHandler {

        protected URLConnection openConnection(URL u) throws IOException {
            return null;
        }

        int puertoPorDefecto() {
            return this.getDefaultPort();
        }

        void porProxy(URL u) throws IOException {
            this.openConnection(u, Proxy.NO_PROXY);
        }

        String texto(URL u) {
            return this.toExternalForm(u);
        }

        boolean mismoArchivo(URL a, URL b) {
            return this.sameFile(a, b);
        }

        boolean iguales(URL a, URL b) {
            return this.equals(a, b);
        }
    }

    // Un flujo de bytes en memoria para `guessContentTypeFromStream`: soporta marcas, asi que el
    // metodo puede espiar y devolver el flujo intacto.
    private static String sniff(byte[] b) throws IOException {
        return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(b));
    }

    // Una `HttpURLConnection` que no conecta. Los tres abstractos son los unicos que necesitan una
    // conexion viva; todo lo que esta prueba mira es configuracion y parseo.
    private static HttpURLConnection mkHttp(String spec) throws Exception {
        return new HttpURLConnection(new URL(spec)) {
            public void connect() throws IOException {
            }

            public void disconnect() {
            }

            public boolean usingProxy() {
                return false;
            }
        };
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
