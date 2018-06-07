package com.xx.exception;

/**
 * 有问题直接中断Build，哥哥别抓(catch)我
 * 
 * Created by xievxin on 2018/5/31
 */
public class UnCaughtException extends Exception {
    public UnCaughtException() {}

    public UnCaughtException(String message) {
        super(message);
    }

    public UnCaughtException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnCaughtException(Throwable cause) {
        super(cause);
    }

    public UnCaughtException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
