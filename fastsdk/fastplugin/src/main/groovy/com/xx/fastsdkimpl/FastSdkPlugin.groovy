package com.xx.fastsdkimpl

import com.android.build.gradle.AppExtension
import com.xx.bean.GetuiUserBean
import com.xx.exception.GroovyException
import com.xx.impl.getui.GetuiManifest
import com.xx.impl.getyan.GeyanManifest
import com.xx.interfaces.DownloadListener
import com.xx.interfaces.IManifest
import com.xx.model.HttpUtil
import com.xx.model.RuntimeDataManager
import org.gradle.api.Project

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

        RuntimeDataManager.mProject = project
        project.task("fastsdkCheck").doLast {
            FastSDKChecker.notAskJustWaiting(project)
        }
        project.extensions.create('getuiSDKUser', GetuiUserBean)

        initArr()

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

        def usr = project.extensions.findByType(GetuiUserBean)
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
        pluginLibFile.entries()?.each { entry ->
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

        project.dependencies {
            try {
                implementation project.fileTree(dir: 'libs', include: ['*.jar'])
            }catch (Exception e) {
                compile project.fileTree(dir: 'libs', include: ['*.jar'])
            }
        }
    }

    private final void configManifest() {
        println("configManifest...")

        def android = project.extensions.getByType(AppExtension)
        println(android.class)

        project.afterEvaluate {
            android.applicationVariants.all { variant ->

                // todo 多个application，只有一个想集成个推捏？
                String pkgName = [variant.mergedFlavor.applicationId, variant.buildType.applicationIdSuffix].findAll().join()
                println "pkgName:" + pkgName

                variant.outputs.each { output ->

                    output.processManifest.doLast {

                        println(output.processManifest.class)
                        println(output.processManifest.outputs.class)

                        // output.getProcessManifest().manifestOutputDirectory
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

        // 解析
//        def xmlRoot = new XmlParser().parse(manifestFile)

        int type = 1    // 模拟从服务器取到的已开通功能
        IManifest manifest
        if (type == 1) {
            manifest = new GetuiManifest()
        } else if (type == 2) {
            manifest = new GeyanManifest()
        } else {
            System.err.println("服务器取到的type : " + type)
        }

        if (manifest != null) {
            manifest.checkInfo()
            manifest.write(manifestFile, "UTF-8")
            println("IManifest.write() over...\n\t" + manifestFile.getAbsolutePath())
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

