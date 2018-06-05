package com.xx.fastsdkimpl

import com.xx.bean.GtUserBean
import com.xx.exception.GroovyException
import com.xx.impl.getui.GetuiManifest
import com.xx.impl.getyan.GetyanManifest
import com.xx.interfaces.DownloadListener
import com.xx.interfaces.IManifest
import com.xx.model.HttpUtil
import com.xx.model.RuntimeDataManager
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.tasks.DefaultTaskContainer

import java.lang.reflect.Method
import java.util.zip.ZipFile

class FastSdkPlugin extends BasePlugin {

    Project project
    def process = [""]
    def space = [""]

    @Override
    void apply(Project project) {
        println("*******************fastsdk*******************")
        this.project = project
        if (!project.plugins.hasPlugin('com.android.application')) {
            System.err.println("application required!")
            return
        }
        initArr()

        project.extensions.create('gtUser', GtUserBean)
        RuntimeDataManager.mProject = project

        readLocalProperties()
        if (downloadSDK()) {
            configLibs()
            try {
                configManifest()
            } catch (GroovyException e) {
                System.out.println("err : " + e.toString())
            }
        }

        println("*******************fastsdk OVER*******************")
    }

    void initArr() {
        int index = 20
        StringBuilder sb = new StringBuilder()
        while (index-- > 0) {
            process << sb.append("=").toString()
        }

        index = 20
        sb.delete(0, sb.size())
        while (index-- > 0) {
            space << sb.append("-").toString()
        }
    }

    void addTask() {
        try {
            def taskContainerField = DefaultProject.class.getDeclaredField("taskContainer")
            taskContainerField.setAccessible(true)
            def taskContainer = taskContainerField.get(project)

            // public <T extends Task> T create(String name, Class<T> type) {
            Method createMhd = DefaultTaskContainer.class.getDeclaredMethod("create", String.class, Class.class)
            createMhd.invoke(taskContainer, "fastsdk", FastTask.class)
            println("addTask 'fastsdk'")

//            project.getTasksByName("fastsdk", false).each {Task task->
//                if(task.getClass().simpleName.startsWith("FastTask")) {
//                    task.doFirst {}
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 后期服务端提供 applicationId查APPID的接口后可删除
     */
    private final void readLocalProperties() {
        println("readLocalProperties...")
        Properties properties = new Properties()
        properties.load(project.rootProject.file('local.properties').newDataInputStream())

        def usr = project.gtUser as GtUserBean
        usr.APP_ID = properties.getProperty("GETUI_APP_ID")
        usr.APP_KEY = properties.getProperty("GETUI_APP_KEY")
        usr.APP_SECRET = properties.getProperty("GETUI_APP_SECRET")
        usr.skipNetCheck = Boolean.parseBoolean(properties.getProperty("skipNetCheck"))

        if (!usr.APP_ID) {
            System.err.println("GETUI_APP_ID not found")
        }
        if (!usr.APP_KEY) {
            System.err.println("GETUI_APP_KEY not found")
        }
        if (!usr.APP_SECRET) {
            System.err.println("GETUI_APP_SECRET not found")
        }
    }

    /**
     * 下载最新sdk包
     * todo 应该先调个检查更新的接口
     * @param project
     * @return true download success or already exist, false otherwise
     */
    private final boolean downloadSDK() {
        final def url = "https://raw.githubusercontent.com/xievxin/GitWorkspace/master/gtSDK.zip"
        File libFile = createLibFile()
        int retryCount = 5
        while (retryCount-- > 0) {
            boolean flag = new HttpUtil().download(url, libFile, new DownloadListener() {

                Writer writer = System.out.newPrintWriter()

                @Override
                void onStart() {
                    println("start downloadSDK")
                }

                @Override
                void onBuffer(int percent) {
//                    println("onBuffer : " + percent)
                    int index = (int) (percent / 5)
                    writer.write("\r<" +
                            (index ? "\033[1;32m" + process[index] + "\033[0m" : "")
                            + space[20 - index] + ">\t" + percent + "%")
                    writer.flush()
                    if (percent == 100) {
                        println("\n下载成功 End downloadSDK, libFile is " + (libFile?.exists() ? "loaded" : "no exist"))
                    }
                }

                @Override
                void onError(String errMsg) {
                    System.err.println(errMsg)
                }
            })

            if (flag) {
                return true
            } else {
                println("还剩余${retryCount}次下载重试次数")
            }
        }

        def usr = project.gtUser as GtUserBean
        if (usr.skipNetCheck) {
            System.err.println("Network err!!Suggest you 'Rebuild Project' when network is fine")
            return true
        }

        throw new GroovyException("SDK download failed")
    }

    private final void configLibs() {
        println("configLibs...")
        ZipFile pluginLibFile = new ZipFile(createLibFile())

        File libDir = new File(project.name + "/libs")
        if (!libDir.exists()) {
            libDir.mkdirs()
        }
//        println(libDir.getAbsolutePath())

        File jniDir = new File(project.name + "/src/main/jniLibs")
        if (!jniDir.exists()) {
            jniDir.mkdirs()
        }
//        println(jniDir.getAbsolutePath())

        byte[] buffer = new byte[1024]
        String name
        pluginLibFile.entries().each { entry ->
            name = entry.getName()
            if (name.startsWith("__")) {
                return
            }
            FileOutputStream fos
            if (name.endsWith(".jar")) {
                def jarFile = new File(libDir.getAbsolutePath() + File.separator + name)
                if (!jarFile.exists()) {
                    fos = new FileOutputStream(jarFile)
                }
            } else if (name.endsWith(".so")) {
                def soFile = new File(jniDir.getAbsolutePath() + name.substring(name.indexOf(File.separator)))
                // 先创建上级目录
                def parentFile = soFile.getParentFile()
                if (!parentFile.exists()) {
                    parentFile.mkdirs()
                }
                if (!soFile.exists()) {
                    fos = new FileOutputStream(soFile)
                }
            }
            if (fos != null) {
                println("copying " + name)

                InputStream is = pluginLibFile.getInputStream(entry)

                int length
                while ((length = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, length)
                }
                is.close()
                fos.close()
            }
        }
        buffer = null
    }

    private final void configManifest() {
        println("configManifest...")
        def manifestFile = new File(project.name + "/src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            System.err.println("can't find AndroidManifest.xml....")
            return
        }

        // 解析
        def xmlRoot = new XmlParser().parse(manifestFile)
        println("parse 'manifest.xml' success...")

        xmlRoot.application?."meta-data"?.each { Node node ->
            node.attributes().each {
                String[] arr = it.toString().split("=")
                if (arr?.length == 2 && "PUSH_FLAG" == arr[1]) {
                    throw new GroovyException("manifest was configured")
                }
            }
        }

        int type = 1    // 模拟从服务器取到的已开通功能
        IManifest manifest
        if (type == 1) {
            manifest = new GetuiManifest()
        } else if (type == 2) {
            manifest = new GetyanManifest()
        }

        if (manifest) {
            manifest.init(project)
            def doc = new StreamingMarkupBuilder().bind(manifest.result)
            def writer = new FileWriter(manifestFile)
//            def writer = new FileWriter(new File(project.name + "/src/main/test.xml"))
            try {
                writer << doc
            } finally {
                writer.close()
            }
        }
    }

    File createLibFile() {
        File pluginFile = new File(RuntimeDataManager.getInstance().pluginDir)
        if (!pluginFile.exists()) {
            pluginFile.mkdirs()
        }
        File sdkFile = new File(RuntimeDataManager.getInstance().zipSDKPath)
//        if (sdkFile.exists()) {
//            sdkFile.delete();
//        }
//        try {
//            sdkFile.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        sdkFile
    }
}

