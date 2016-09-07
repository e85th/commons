package e85th.commons.exceptions;

import clojure.lang.ExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import java.util.Collections;
import java.util.HashMap;


public class ValidationExceptionInfo extends ExceptionInfo {
    public ValidationExceptionInfo(String s) {
        this(s, PersistentHashMap.create(Collections.emptyMap()));
    }
    public ValidationExceptionInfo(String s, IPersistentMap data) {
        this(s, data, null);
    }

    public ValidationExceptionInfo(String s, IPersistentMap data, Throwable t) {
        super(s, data, t);
    }
}
