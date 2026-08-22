interface Greeter { String hello(); default String hi() { return "hi"; } }
public class Iface implements Greeter { public String hello() { return "hello"; } }
