package network;

public class URLConstants {

    public static final String CALL_ZAPPER_BASE = "base_call";
    public static final String CALL_ZAPPER_DETAIL = "detail_call";

    public static class URLS {
        public static final String URL = "http://demo4012764.mockable.io/person";

        public static String BASE_URL(){return URL;}
        public static String DETAIL_URL(String id){return URL + "/" + id;}

    }
}
