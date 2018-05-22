package com.xx.fastsdkimpl

import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.w3c.dom.Node

class FastSdkPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("*******************fastsdk*******************")

//        if (!project.plugins.hasPlugin('com.android.application')) {
//            System.err.println("application required!")
//            return
//        }

        project.extensions.create('gtConstant', Constants)

//        watchMyShow()
        downloadLibs(project)
    }

    void downloadLibs(Project project) {
        println("start downloadLibs")
        new HttpUtil().createLibFile(project)
        println("End downloadLibs")

//        project.afterEvaluate {
//            project.allprojects.repositories.each {
//            project.allprojects.repositories << {
//                maven {
//                    url "http://mvn.gt.igexin.com/nexus/content/repositories/releases/"
//                }
//            }
//            println(project.dependencies)
//            project.dependencies << {
//                compile 'com.getui:sdk:2.12.3.0'
//            }
//            project.getRootProject().allprojects.each {
//                def childPro = it as Project
//                if(childPro.getName()?.equalsIgnoreCase(project.getRootProject()?.getName())) {
//                    childPro.repositories.add("desc", {
//                        maven {
//                            url "http://mvn.gt.igexin.com/nexus/content/repositories/releases/"
//                        }
//                    })
//                }
//            }
//            println(project.android.applicationVariants.ma)
//        }
//        project.afterEvaluate {
//            project.android.applicationVariants.each { ApkVariant variant ->
//                if (variant.buildType.name.equalsIgnoreCase("release")) {
//                    final def variantPluginTaskName = "assemblePlugin${variant.name.capitalize()}"
//                    final def configAction = new AssemblePlugin.ConfigAction(project, variant)
//
//                    taskFactory.create(variantPluginTaskName, AssemblePlugin, configAction)
//
//                    taskFactory.named("assemblePlugin", new Action<Task>() {
//                        @Override
//                        void execute(Task task) {
//                            task.dependsOn(variantPluginTaskName)
//                        }
//                    })
//                }
//            }
//        }
//        android.applicationVariants.all { variant ->
//            def mergedFlavor = variant.getMergedFlavor()
//            // Defines the value of a build variable you can use in the manifest.
//            mergedFlavor.manifestPlaceholders = [hostName:"www.example.com/${variant.versionName}"]
//        }
    }

    void watchMyShow() {
        def appFile = new File("app/src/main/AndroidManifest.xml")
        if (!appFile.exists()) {
            println("can't find AndroidManifest.xml....")
            return
        }

        // 解析
        def xmlParser = new XmlParser().parse(appFile)

        def result = {
            mkp.xmlDeclaration()
            mkp.declareNamespace(android: "http://schemas.android.com/apk/res/android")
            manifest(xmlParser.attributes()) {
                def getAttrs = { Node node ->
                    def attrMap = [:]
                    node?.attributes()?.each { key, value ->
                        attrMap.put(key.toString().replace("{http://schemas.android.com/apk/res/android}", "android:"), value)
                    }
                    println("getAttrs() : " + attrMap)
                    attrMap
                }
                def parseChild = { Iterator<Node> nodeIt, Callback callback ->
                    while (nodeIt.hasNext()) {
                        def nd = nodeIt.next()
                        "${nd.name()}"(getAttrs(nd)) {
                            callback.onCall(nd)
                        }
                    }
                }
                def getChildStr = { Node node ->
                    if (node.children()) {
                        parseChild(node.iterator(), new Callback() {
                            @Override
                            void onCall(Node nd) {
                                if (node.children())
                                    parseChild(nd.iterator(), this)
                            }
                        })
                    }
                }
                getChildStr(xmlParser)
            }
        }

        def doc = new StreamingMarkupBuilder().bind(result)
        def writer = new FileWriter(new File("app/src/main/testWrite.xml"))
        try {
            writer << doc
        } finally {
            writer?.close()
        }
    }
}

