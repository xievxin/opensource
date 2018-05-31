package com.xx.model

import com.xx.interfaces.DownloadListener
import org.gradle.api.Project

/**
 * Created by xievxin on 2018/5/22
 */
class HttpUtil {

    boolean download(Project project, String url, def outFile, DownloadListener listener) {
        boolean flag = false
        InputStream is
        FileOutputStream fos
        try {
            listener.onStart()
            URL httpUrl = new URL(url)
            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection()
            conn.setConnectTimeout(20_000)
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream()
                fos = new FileOutputStream(outFile)

                int contentLen = conn.getContentLength()
                int totalLen
                int lastPercent

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
                fos.flush()
                b = null
                flag = true
                listener.onFinished()
            } else {
                listener.onError("gtPlugins download err!!errCode is : " + conn.getResponseCode())
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
