package com.xx.util

import com.xx.interfaces.DownloadListener

/**
 * Created by xievxin on 2018/5/22
 */
class HttpUtil {

    boolean download(String url, File outFile, DownloadListener listener) {
        boolean flag = false
        InputStream is
        RandomAccessFile fos
        int curLen = outFile.length()
        try {
            listener.onStart()
            URL httpUrl = new URL(url)
            URLConnection conn = httpUrl.openConnection()
            conn.setConnectTimeout(20_000)
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                int contentLen = conn.getContentLength()
                if (contentLen == curLen) {
                    // already exist
                    listener.onBuffer(100)
                    return true
                }
                int totalLen
                int lastPercent

                conn = httpUrl.openConnection()
                conn.addRequestProperty("Range", "bytes=" + curLen + "-" + contentLen)
                if (contentLen != conn.getContentLength()) {
                    throw new IllegalArgumentException("server has changed SDK file, pls try again")
                }

                is = conn.getInputStream()
                fos = new RandomAccessFile(outFile, "rw")
                fos.seek(curLen)

                byte[] b = new byte[1024]
                int len = 0
                int percent
                while ((len = is.read(b)) != -1) {  //先读到内存
                    fos.write(b, 0, len)
                    percent = (int) ((totalLen += len) * 100 / contentLen)
                    if (lastPercent != percent) {
                        listener.onBuffer(lastPercent = percent)
                    }
                }
                b = null
                flag = true
            } else {
                listener.onError("gtPlugins download err!!resCode is : " + conn.getResponseCode())
            }
        } catch (Exception e) {
            e.printStackTrace()
            listener.onError("gtPlugins download err!!cause: " + e.printStackTrace())
        } finally {
            is?.close()
            fos?.close()
        }
        flag
    }
}
