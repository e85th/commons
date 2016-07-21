package e85th.commons.exceptions;

public class GeocodingException extends RuntimeException {
    public GeocodingException(String msg) {
        super(msg);
    }

    public String toString() {
        return "GeocodingException: " + this.getMessage();
    }
}
