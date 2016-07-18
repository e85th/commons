package e85th.commons.exception;

import java.sql.SQLException;

public class SqlNoRowsUpdatedException extends SQLException {
    public SqlNoRowsUpdatedException() { super();}
    public SqlNoRowsUpdatedException(String msg) { super(msg);}
}
