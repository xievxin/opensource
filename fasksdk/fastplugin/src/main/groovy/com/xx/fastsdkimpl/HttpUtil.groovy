package com.xx.fastsdkimpl

import org.gradle.api.Project

/**
 * Created by xievxin on 2018/5/22
 */
class HttpUtil {

    void download(Project project, String url) {
        InputStream is
        FileOutputStream fos
        try {
            URL httpUrl = new URL(url)
            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection()
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream()
                fos = new FileOutputStream(createLibFile(project))

                byte[] b = new byte[1024]
                int len = 0
                while ((len = is.read(b)) != -1) {  //先读到内存
                    fos.write(b, 0, len)
                }
                fos.flush()
                System.err.println("下载成功")
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            is?.close()
            fos?.close()
        }
    }

    /*private*/

    File createLibFile(Project project) {
        def pluginFile = new File(project.getBuildDir().getAbsolutePath() + File.separator + "gtPlugins")
        println(pluginFile.toString() + "----...")
        if (!pluginFile.exists()) {
            pluginFile.mkdirs()
        }
        return pluginFile
    }
}
