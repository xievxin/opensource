package com.xx.interfaces;

/**
 * Created by xievxin on 2018/5/29
 */
public interface DownloadListener {
    void onStart();

    void onBuffer(int percent);

    void onFinished();

    void onError(String errMsg);
}
