package com.xx.fastsdkimpl

import com.android.build.gradle.AppExtension
import com.google.gson.JsonObject
import com.xx.bean.UserBean
import com.xx.exception.GroovyException
import com.xx.impl.geshu.GeshuManifest
import com.xx.impl.getui.GetuiManifest
import com.xx.impl.getyan.GeyanManifest
import com.xx.interfaces.DownloadListener
import com.xx.interfaces.IManifest
import com.xx.model.RuntimeDataManager
import com.xx.util.CheckUtil
import com.xx.util.HttpUtil
import org.gradle.api.Project

import java.util.zip.ZipFile

class FastSdkPlugin extends BasePlugin {

    static final int TYPE_NONE = 0
    static final int TYPE_ERROR = 1 << 0
    static final int TYPE_GETUI = 1 << 1
    static final int TYPE_GESHU = 1 << 2
    static final int TYPE_GEXIANG = 1 << 3
    static final int TYPE_GEYAN = 1 << 4

    Project project
    int openedType = TYPE_NONE
    def process = [""]
    def space = [""]
    JsonObject respJo

    @Override
    void apply(Project project) {
        println("*******************fastsdk*******************")
        this.project = project
        if (!project.plugins.hasPlugin('com.android.application')) {
            System.err.println("application required!")
            return
        }

        RuntimeDataManager.mProject = project
        project.task("fastsdkCheck").doLast {
            FastSDKChecker.notAskJustWaiting(project)
        }
        project.extensions.create('xxSDKUser', UserBean)

        initArr()
        requestType()

        if ((openedType > TYPE_ERROR && downloadSDK()) || shouldSkipNetCheck()) {
            configLibs()
            try {
                configManifest()
            } catch (GroovyException e) {
                System.out.println("err : " + e.toString())
            }
        }

        println("*******************fastsdk OVER*******************")
    }

    String skipNetCheck
    boolean shouldSkipNetCheck() {
        if(CheckUtil.isEmpty(skipNetCheck)) {
            Properties properties = new Properties()
            properties.load(project.rootProject.file('local.properties').newDataInputStream())
            skipNetCheck = properties.getProperty("skipNetCheck")
        }
        return Boolean.parseBoolean(skipNetCheck)
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

    void requestType() {
        openedType = TYPE_GETUI | TYPE_GESHU   // 模拟从服务器取到的已开通功能

        respJo = new JsonObject()

        // 模拟数据
        JsonObject getuiJo = new JsonObject()
        getuiJo.addProperty("url", "https://raw.githubusercontent.com/xievxin/GitWorkspace/master/gtSDK.zip")
        respJo.add("getui", getuiJo)

        JsonObject geshuJo = new JsonObject()
        geshuJo.addProperty("url", "https://raw.githubusercontent.com/xievxin/GitWorkspace/master/geshuSDK.zip")
        respJo.add("geshu", geshuJo)
    }

    /**
     * 下载最新sdk包
     * @param project
     * @return true download success or already exist, false otherwise
     */
    private boolean downloadSDK() {
        String libFilePath = createLibFile().path

        if (respJo == null) {
            return false
        }

        if (respJo.has("getui")) {
            File outFile = new File(libFilePath + File.separator + "getui.zip")
            JsonObject jsonObject = respJo.getAsJsonObject("getui")
            realDownload(jsonObject.("url").getAsString(), outFile)
        }
        if (respJo.has("geshu")) {
            File outFile = new File(libFilePath + File.separator + "geshu.zip")
            JsonObject jsonObject = respJo.getAsJsonObject("geshu")
            realDownload(jsonObject.("url").getAsString(), outFile)
        }

        return true
    }

    private void realDownload(String url, File outFile) {
        int retryCount = 3
        while (retryCount-- > 0) {
            boolean flag = new HttpUtil().download(url, outFile, new DownloadListener() {

                Writer writer = System.out.newPrintWriter()

                @Override
                void onStart() {
                    println("start downloadSDK ${outFile.getName()}")
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
                        println("\n${outFile.getName()}下载成功")
                    }
                }

                @Override
                void onError(String errMsg) {
                    System.err.println(errMsg)
                }
            })

            if (flag) {
                return
            } else {
                println("还剩余${retryCount}次下载重试次数")
            }
        }

        if (!shouldSkipNetCheck()) {
            throw new GroovyException("SDK download failed")
        }
    }

    private final void configLibs() {
        println("configLibs...")

        File libDir = new File(project.name + "/libs")
        if (!libDir.exists()) {
            libDir.mkdirs()
        }

        File jniDir = new File(project.name + "/src/main/jniLibs")
        if (!jniDir.exists()) {
            jniDir.mkdirs()
        }

        List<String> aarList = []
        File pluginFile = createLibFile()
        File[] files = pluginFile.listFiles()
        files?.each {
            if (!it.isDirectory() && it.getName().endsWith(".zip")) {
                ZipFile pluginLibFile = new ZipFile(it)

                byte[] buffer = new byte[1024]
                String name
                pluginLibFile.entries()?.each { entry ->
                    name = entry.getName()
                    if (name.startsWith("__")) {
                        return
                    }
                    FileOutputStream fos
                    if (name.endsWith(".jar") || name.endsWith(".aar")) {
                        name = name.substring(Math.max(name.indexOf("/") + 1, 0))
                        def jarFile = new File(libDir.getAbsolutePath() + File.separator + name)
                        if (!jarFile.exists()) {
                            fos = new FileOutputStream(jarFile)
                        }
                        if (name.endsWith(".aar")) {
                            aarList << name.replace(".aar", "")
                        }
                    } else if (name.endsWith(".so")) {
                        def soFile = new File(jniDir.getAbsolutePath() + name.substring(name.indexOf("/")))
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
        }

        boolean flag = CheckUtil.isGradleUper3_0_0(project)
        project.dependencies {
            if (flag) {
                implementation project.fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
            } else {
                compile project.fileTree(dir: 'libs', include: ['*.jar'])
                project.repositories.flatDir {
                    dir project.file('libs')
                }
                aarList?.each {
                    compile(name: it, ext: 'aar')
                }
            }
        }
    }

    private final void configManifest() {
        println("configManifest...")

        def android = project.extensions.getByType(AppExtension)

        project.afterEvaluate {
            android.applicationVariants.all { variant ->

//                String pkgName = [variant.mergedFlavor.applicationId, variant.buildType.applicationIdSuffix].findAll().join()

                variant.outputs.each { output ->
                    output.processManifest.doLast {
                        output.processManifest.outputs.files.each { File file ->
                            if (file.isDirectory()) {
                                // 在gradle plugin 3.0.0之后，file是目录，且不包含AndroidManifest.xml，需要自己拼接
                                letMeShowYouWhatIsTheManifestShouldBe(new File(file, "AndroidManifest.xml"))
                            } else if (file.name.equalsIgnoreCase("AndroidManifest.xml")) {
                                // 在gradle plugin 3.0.0之前，file是文件，且文件名为AndroidManifest.xml
                                letMeShowYouWhatIsTheManifestShouldBe(file)
                            }
                        }
                    }

                }
            }
        }
    }

    private final void letMeShowYouWhatIsTheManifestShouldBe(File manifestFile) {
        println("letMeShowYouWhatIsTheManifestShouldBe()...")
        if (!manifestFile.exists()) {
            System.err.println("can't find AndroidManifest.xml....")
            return
        }

        List<IManifest> list = []
        if (openedType & TYPE_GETUI) {
            list << new GetuiManifest()
        }
        if (openedType & TYPE_GESHU) {
            list << new GeshuManifest()
        }
        if (openedType & TYPE_GEYAN) {
            list << new GeyanManifest()
        }

        if (list == null || list.size() == 0) {
            System.err.println("服务器取到的type not found : " + openedType)
        } else {
            list.each { IManifest manifest ->
                manifest.checkInfo()
                manifest.write(manifestFile, "UTF-8")
            }
            println("IManifest.write() over...\n\t" /*+ manifestFile.getAbsolutePath()*/)
        }
    }

    File createLibFile() {
        File pluginFile = new File(RuntimeDataManager.getInstance().pluginDir)
        if (!pluginFile.exists()) {
            pluginFile.mkdirs()
        }
        return pluginFile
    }
}

