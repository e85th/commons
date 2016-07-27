package e85th.commons.exceptions;

import java.sql.SQLException;

public class NoRowsUpdatedException extends SQLException {
    public NoRowsUpdatedException() { super();}
    public NoRowsUpdatedException(String msg) { super(msg);}
}
