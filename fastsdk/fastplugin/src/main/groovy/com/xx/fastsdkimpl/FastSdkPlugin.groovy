package com.xx.fastsdkimpl

import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.util.zip.ZipFile

class FastSdkPlugin implements Plugin<Project> {

    Project project
    def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    @Override
    void apply(Project project) {
        println("*******************fastsdk*******************")
        this.project = project
        if (!project.plugins.hasPlugin('com.android.application')) {
            System.err.println("application required!")
            return
        }

        project.extensions.create('gtUser', GtUserBean)
        RuntimeDataManager.mProject = project

        if (downloadLibs()) {
            readLocalProperties()
            configLibs()
            try {
                configManifest()
            } catch (GroovyException e) {
                System.err.println("err : " + e.toString())
            }
        }
        println("*******************fastsdk OVER*******************")
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
     * //todo 应该先调个检查更新的接口
     * @param project
     * @return true download success or already exist, false otherwise
     */
    private final boolean downloadLibs() {
        def url = "https://nj01ct01.baidupcs.com/file/06d4fbd2064dba0bd58dd20f816ca9ec?bkt=p3-140006d4fbd2064dba0bd58dd20f816ca9ecd53f038d0000000850fe&fid=2735751894-250528-284663320752002&time=1527057674&sign=FDTAXGERLQBHSK-DCb740ccc5511e5e8fedcff06b081203-1UByBCC6VAXFBWBo%2FNrP4c6S3i8%3D&to=63&size=545022&sta_dx=545022&sta_cs=1&sta_ft=zip&sta_ct=1&sta_mt=1&fm2=MH%2CYangquan%2CAnywhere%2C%2Czhejiang%2Cct&vuk=2735751894&iv=0&newver=1&newfm=1&secfm=1&flow_ver=3&pkey=140006d4fbd2064dba0bd58dd20f816ca9ecd53f038d0000000850fe&sl=76480590&expires=8h&rt=sh&r=668692554&mlogid=3307573999573532340&vbdid=2202615538&fin=gtSDK.zip&fn=gtSDK.zip&rtype=1&dp-logid=3307573999573532340&dp-callid=0.1.1&hps=1&tsl=80&csl=80&csign=LraYTFNsTcoKxtvKhl9vaKsgqBk%3D&so=0&ut=6&uter=4&serv=0&uc=2361768026&ic=1484612705&ti=26fa64dbec28822423b7a5bc41b22f8db3b1e649745ca328305a5e1275657320&by=themis"
        File libFile = createLibFile()
        if (!libFile.exists()) {
            println("start downloadLibs")
            boolean flag = new HttpUtil().download(project, url, libFile)
            println("End downloadLibs, libFile is " + (libFile?.exists() ? "loaded" : "no exist"))
            return flag
        }
        true
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
        def appFile = new File(project.name + "/src/main/AndroidManifest.xml")
        if (!appFile.exists()) {
            System.err.println("can't find AndroidManifest.xml....")
            return
        }

        // 先备份一下
        backUpManifest()

        // 解析
        def xmlParser = new XmlParser().parse(appFile)
        println("parse 'manifest.xml' success...")

        xmlParser.application?."meta-data"?.each { Node node ->
            node.attributes().each {
                String[] arr = it.toString().split("=")
                if (arr?.length == 2 && "PUSH_FLAG" == arr[1]) {
                    throw new GroovyException("manifest was configured")
                }
            }
        }

        def result = {
            mkp.xmlDeclaration()
            // todo 最好从xmlParser中识别namaspace
            mkp.declareNamespace(android: "http://schemas.android.com/apk/res/android")
            manifest(xmlParser.attributes()) {
                def getAttrs = { Node node, int tabCount ->
                    def attrMap = [:]
                    int size = node.attributes().size()
                    node?.attributes()?.each { key, value ->
                        attrMap.put((size>1 ? "\n"+ tabs[tabCount] : "") +
                                key.toString().replace("{http://schemas.android.com/apk/res/android}", "android:"), value)
                    }
                    attrMap
                }
                def appXml = { int count ->
                    mkp.yield("\n" + tabs[count])
                    mkp.comment("个推SDK配置开始")
                    mkp.yield("\n" + tabs[count])
                    mkp.comment("配置的第三方参数属性")
                    mkp.yield("\n" + tabs[count])
                    "meta-data"("android:name": "PUSH_FLAG", "android:value": "")    // 配置标识，Manifest中有的话，中断
                    mkp.comment("插件标识，请勿删除")
                    mkp.yield("\n" + tabs[count])
                    "meta-data"("android:name": "PUSH_APPID", "android:value": "${project.gtUser.APP_ID}")
                    mkp.yield("\n" + tabs[count])
                    "meta-data"("android:name": "PUSH_APPKEY", "android:value": "${project.gtUser.APP_KEY}")
                    mkp.yield("\n" + tabs[count])
                    "meta-data"("android:name": "PUSH_APPSECRET", "android:value": "${project.gtUser.APP_SECRET}}")
                    mkp.yield("\n" + tabs[count])
                    mkp.comment("配置SDK核心服务")
                    mkp.yield("\n" + tabs[count])
                    service("\n${tabs[count + 1]}android:name": "com.igexin.sdk.PushService",
                            "\n${tabs[count + 1]}android:exported": "true",
                            "\n${tabs[count + 1]}android:label": "NotificationCenter",
                            "\n${tabs[count + 1]}android:process": ":pushservice") {
                        mkp.yield("\n" + tabs[count + 1])
                        "intent-filter" {
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "com.igexin.sdk.action.service.message")
                            mkp.yield("\n" + tabs[count + 1])
                        }
                        mkp.yield("\n" + tabs[count])
                    }
                    mkp.yield("\n" + tabs[count])
                    receiver("android:name": "com.igexin.sdk.PushReceiver") {
                        mkp.yield("\n" + tabs[count + 1])
                        "intent-filter" {
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.intent.action.BOOT_COMPLETED")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.net.conn.CONNECTIVITY_CHANGE")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.intent.action.USER_PRESENT")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "com.igexin.sdk.action.refreshls")
                            mkp.yield("\n" + tabs[count + 2])
                            mkp.comment("以下三项为可选的action声明，可大大提高service存活率和消息到达速度")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.intent.action.MEDIA_MOUNTED")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.intent.action.ACTION_POWER_CONNECTED")
                            mkp.yield("\n" + tabs[count + 2])
                            action("android:name": "android.intent.action.ACTION_POWER_DISCONNECTED")
                            mkp.yield("\n" + tabs[count + 1])
                        }
                        mkp.yield("\n" + tabs[count])
                    }
                    mkp.yield("\n" + tabs[count])
                    activity("\n${tabs[count + 1]}android:name": "com.igexin.sdk.PushActivity",
                            "\n${tabs[count + 1]}android:excludeFromRecents": "true",
                            "\n${tabs[count + 1]}android:exported": "false",
                            "\n${tabs[count + 1]}android:process": ":pushservice",
                            "\n${tabs[count + 1]}android:taskAffinity": "com.igexin.sdk.PushActivityTask",
                            "\n${tabs[count + 1]}android:theme": "@android:style/Theme.Translucent.NoTitleBar")
                    mkp.yield("\n" + tabs[count])
                    activity("\n${tabs[count + 1]}android:name": "com.igexin.sdk.GActivity",
                            "\n${tabs[count + 1]}android:excludeFromRecents": "true",
                            "\n${tabs[count + 1]}android:exported": "true",
                            "\n${tabs[count + 1]}android:process": ":pushservice",
                            "\n${tabs[count + 1]}android:taskAffinity": "com.igexin.sdk.PushActivityTask",
                            "\n${tabs[count + 1]}android:theme": "@android:style/Theme.Translucent.NoTitleBar")
                    mkp.yield("\n" + tabs[count])
                    mkp.comment("个推SDK配置结束")
                    mkp.yield("\n\t")
                }
                def parseChild = { Iterator<Node> nodeIt, int deepCount, Callback callback ->
                    while (nodeIt.hasNext()) {
                        mkp.yield("\n" + tabs[deepCount])
                        def nd = nodeIt.next()
                        // 没有子节点就以“/>”结尾
                        if(!nd.children()) {
                            "${nd.name()}"(getAttrs(nd, deepCount + 1))
                            continue
                        }
                        "${nd.name()}"(getAttrs(nd, deepCount + 1))
                        {
                            callback.onCall(nd, deepCount + 1)
                            if ("application".equalsIgnoreCase(nd.name())) {
                                appXml(2)
                            }
                        }
                    }
                    mkp.yield("\n" + tabs[deepCount - 1])
                }
                def getChildStr = { Node node ->
                    if (node.children()) {
                        parseChild(node.iterator(), 1, new Callback() {
                            @Override
                            void onCall(Node nd, int curDeepCount) {
                                parseChild(nd.iterator(), curDeepCount, this)
                            }
                        })
                    }
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.INTERNET")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.READ_PHONE_STATE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.ACCESS_NETWORK_STATE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.CHANGE_WIFI_STATE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.ACCESS_WIFI_STATE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.WAKE_LOCK")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.RECEIVE_BOOT_COMPLETED")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.WRITE_EXTERNAL_STORAGE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.VIBRATE")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.GET_TASKS")
                    mkp.yield("\n" + tabs[1])
                    mkp.comment("支持iBeancon 需要蓝牙权限")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.BLUETOOTH")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.BLUETOOTH_ADMIN")
                    mkp.yield("\n" + tabs[1])
                    mkp.comment("支持个推3.0 电子围栏功能")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.ACCESS_FINE_LOCATION")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.ACCESS_COARSE_LOCATION")
                    mkp.yield("\n" + tabs[1])
                    mkp.comment("浮动通知权限")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "android.permission.SYSTEM_ALERT_WINDOW")
                    mkp.yield("\n" + tabs[1])
                    mkp.comment("自定义权限")
                    mkp.yield("\n" + tabs[1])
                    "uses-permission"("android:name": "getui.permission.GetuiService.\${applicationId}")
                    mkp.yield("\n" + tabs[1])
                    "permission"("android:name": "getui.permission.GetuiService.\${applicationId}", "android:protectionLevel": "normal")
                    mkp.yield "\n"
                }
                getChildStr(xmlParser)
            }
        }

        def doc = new StreamingMarkupBuilder().bind(result)
        def writer = new FileWriter(appFile)
        try {
            writer << doc
        } finally {
            writer.close()
        }
    }

    private void backUpManifest() {
        println("backUpManifest...")
        def appFile = new File(project.name + "/src/main/AndroidManifest.xml")
        def backUpFile = new File(project.name + "/src/main/OldAndroidManifest.xml")
        if (backUpFile.exists()) {
            return
        }
        try {
            Files.copy(appFile.toPath(), backUpFile.toPath())
        } catch (FileAlreadyExistsException e) {
            System.err.println("backUpManifest() err : " + e.toString())
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

