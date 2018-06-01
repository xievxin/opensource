package com.xx.interfaces;

/**
 * Created by xievxin on 2018/5/29
 */
public interface DownloadListener {
    void onStart();

    /**
     * 下载进度
     * @param percent 0~100
     */
    void onBuffer(int percent);

    void onError(String errMsg);
}
