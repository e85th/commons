package e85th.commons.exceptions;

import clojure.lang.ExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import java.util.Collections;
import java.util.HashMap;


public class AuthExceptionInfo extends ExceptionInfo {
    public AuthExceptionInfo(String s) {
        this(s, PersistentHashMap.create(Collections.emptyMap()));
    }
    public AuthExceptionInfo(String s, IPersistentMap data) {
        this(s, data, null);
    }

    public AuthExceptionInfo(String s, IPersistentMap data, Throwable t) {
        super(s, data, t);
    }
}
