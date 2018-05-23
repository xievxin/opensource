package com.xx.fastsdkimpl

import org.gradle.api.Project

/**
 * Created by xievxin on 2018/5/22
 */
class HttpUtil {

    boolean download(Project project, String url, def outFile) {
        boolean flag = false
        InputStream is
        FileOutputStream fos
        try {
            URL httpUrl = new URL(url)
            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection()
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream()
                fos = new FileOutputStream(outFile)

                byte[] b = new byte[1024]
                int len = 0
                while ((len = is.read(b)) != -1) {  //先读到内存
                    fos.write(b, 0, len)
                }
                fos.flush()
                b = null
                System.out.println("下载成功")
                flag = true
            } else {
                System.err.println("gtPlugins download err!!errCode is : " + conn.getResponseCode())
            }
        } catch (Exception e) {
            e.printStackTrace()
            System.err.println("gtPlugins download err!!cause: " + e.printStackTrace())
        } finally {
            is?.close()
            fos?.close()
        }
        flag
    }
}
