package cn.isliu.core.exception;

public class FsHelperException  extends RuntimeException {

    public FsHelperException(String message) {
        super(message);
    }

    public FsHelperException(String message, Throwable cause) {
        super(message, cause);
    }
}
