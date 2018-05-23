package com.xx.fastsdkimpl;

/**
 * Created by xievxin on 2018/5/23
 */
public class GroovyException extends Exception {
    public GroovyException() {}

    public GroovyException(String message) {
        super(message);
    }

    public GroovyException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroovyException(Throwable cause) {
        super(cause);
    }

    public GroovyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
