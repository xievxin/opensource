package com.xx.interfaces

import com.xx.exception.UnCaughtException
import com.xx.model.FastXmlReader
import org.gradle.api.Project

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

/**
 * Created by xievxin on 2018/5/30
 */
abstract class IManifest {

    /**
     * 注释节点
     */
    static final String NODE_COMMENT = "gtp_comment"

    /**
     * 输出文本节点，like '\n\r\t'
     */
    static final String NODE_YIELD = "gtp_yield"

    /**
     * 备份Manifest，稳定后可拿掉
     */
    static final String BackupManifestName = "OldAndroidManifest.xml"

    static
    def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    boolean isInited = false
    Project project
    File manifestFile
    File oldManifestFile
    Node xmlRoot
    def xmlnsMap
    Node curRoot

    def commentList = [:]    // 已有的注释
    HashMap<String, Object> nodeList = new HashMap()   // 已存在的四大组件

    /**
     * 必须调用
     * @param project
     * @param manifestFile
     * @param xmlRoot
     */
    void init(Project project) {
        this.project = project
        // 顺序hin重要
        realInit()

        // 先备份一下
        backUpManifest()

        commentList = new FastXmlReader().readComments(manifestFile)

        isInited = true
    }

    private final void realInit() {
        manifestFile = new File(project.name + "/src/main/AndroidManifest.xml")
        oldManifestFile = new File(project.name + "/src/main/${BackupManifestName}")
        xmlRoot = new XmlParser().parse(manifestFile)
        xmlnsMap = new FastXmlReader().readNamespace(manifestFile)
    }

    /**
     * application节点下拼接
     * activity、service、receiver、provider等
     * @param root < manifest>
     */
    protected abstract void appendApplicationNodes()

    /**
     * 权限节点拼接
     * @param root < manifest>
     */
    protected abstract void appendPermissionNodes()

    Node appendNode(String name, Map attributes) {
        return _appendNode(name, attributes, new NodeList())
    }

    Node appendNode(String name, Object value) {
        return _appendNode(name, new HashMap(), value)
    }

    final
    def componentsArr = ["meta-data", "activity", "service", "receiver", "provider", "uses-permission", "permission"]

    private Node _appendNode(String name, Map attributes, Object value) {
        if (curRoot == null) {
            System.err.println("curRoot is null")
            return null
        }
//        if (name == NODE_COMMENT && commentList.containsKey(value)) {
//            println("comment exist : " + value)
//            return null
//        }
        if ((name in componentsArr) && nodeList.containsKey(attributes.get("android:name"))) {
            return null
        }
        return new Node(curRoot, name, attributes, value)
    }


    final def result = {
        if (!isInited) {
            throw new UnCaughtException(" u must init() it before use")
        }

        mkp.xmlDeclaration()
        xmlnsMap?.each { key, value ->
            mkp.declareNamespace("${key}": "${value}")
        }
        //  todo manifest中有“android:”--BUG
        manifest(xmlRoot.attributes()) {
            def getAttrs = { Node node, int tabCount ->
                def attrMap = [:]
                int size = node.attributes().size()
                node?.attributes()?.each { key, value ->
                    String keyStr = key.toString()
                    xmlnsMap?.each { xkey, xvalue ->
                        if (keyStr.contains(xvalue.toString())) {
                            keyStr = keyStr.replaceFirst("\\{${xvalue.toString()}\\}", "${xkey}:")
                        }
                    }
                    attrMap.put((size > 1 ? "\n" + tabs[tabCount] : "") + keyStr, value)
                }
                attrMap
            }
            def parseChild = { Iterator<Node> nodeIt, int deepCount, NodeCallback callback ->
                while (nodeIt.hasNext()) {
                    mkp.yield("\n" + tabs[deepCount])
                    def nd = nodeIt.next()
                    def name = nd.name()

                    if (name == NODE_COMMENT) {
                        mkp.comment(nd.text())
                        continue
                    } else if (name == NODE_YIELD) {
                        mkp.yield(nd.text())
                        continue
                    }

                    if (name in componentsArr) {
                        nd.attributes().keySet().each {
                            // todo "name"优化 精准一些
                            if(it.toString().endsWith("name")) {
                                nodeList.put(nd.attribute(it), null)
                            }
                        }
                    }

                    // 没有子节点就以“/>”结尾
                    if (!nd.children()) {
                        "${name}"(getAttrs(nd, deepCount + 1))
                        continue
                    }
                    "${nd.name()}"(getAttrs(nd, deepCount + 1)) {
                        callback.onCall(nd, deepCount + 1)
                        if ("application" == name) {
                            curRoot = new Node(null, "")
                            println("appendApplicationNodes()...")
                            appendApplicationNodes()
                            callback.onCall(curRoot, deepCount + 1)
                        }
                    }
                }
                mkp.yield("\n" + tabs[deepCount - 1])
            }
            def getChildStr = { Node node ->
                if (node.children()) {
                    parseChild(node.iterator(), 1, new NodeCallback() {
                        @Override
                        void onCall(Node nd, int curDeepCount) {
                            parseChild(nd.iterator(), curDeepCount, this)
                        }
                    })
                }
            }
            getChildStr(xmlRoot)

            println(nodeList.keySet().toString())
            /*println("_b")
            curRoot = new Node(null, "")
            println(curRoot+"_c")
            appendPermissionNodes()
            println(curRoot+"_d")
            getChildStr(permissionRootNode)*/
        }
    }

    private void backUpManifest() {
        println("backUpManifest...")
        if (oldManifestFile.exists()) {
            return
        }
        try {
            Files.copy(manifestFile.toPath(), oldManifestFile.toPath())
        } catch (FileAlreadyExistsException e) {
            System.err.println("backUpManifest() err : " + e.toString())
        }
    }

}
