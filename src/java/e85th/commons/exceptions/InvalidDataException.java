package e85th.commons.exceptions;

import java.util.List;
import java.util.ArrayList;

public class InvalidDataException extends RuntimeException {
    private List<String> errors;
    public InvalidDataException(List<String> errors) {
        super();
        if (null==errors) {
            throw new IllegalArgumentException("Null errors");
        }
        this.errors = new ArrayList<String>(errors);
    }
    public List<String> getErrors() {
        return new ArrayList<String>(this.errors);
    }
}
