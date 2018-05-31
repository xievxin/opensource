package com.xx.interfaces

import com.xx.exception.UnCaughtException
import com.xx.model.FastXmlNamespaceReader
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

    static final String BackupManifestName = "OldAndroidManifest"

    static def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    boolean isInited = false
    Project project
    File manifestFile
    File oldManifestFile
    Node xmlRoot

    /**
     * 必须调用
     * @param project
     * @param manifestFile
     * @param xmlRoot
     */
    void init(Project project) {
        this.project = project
        // 顺序别换
        realInit()

        // 先备份一下
        backUpManifest()
        isInited = true
    }

    private final void realInit() {
        manifestFile = new File(project.name + "/src/main/AndroidManifest.xml")
        oldManifestFile = new File(project.name + "/src/main/${BackupManifestName}.xml")
        xmlRoot = new XmlParser().parse(manifestFile)
    }

    /**
     *  application节点下拼接
     *  activity、service、receiver、provider等
     * @param root < manifest>
     */
    protected abstract void appendApplicationNodes(Node root)

    /**
     * 权限节点拼接
     * @param root < manifest>
     */
    protected abstract void appendPermissionNodes(Node root)


    final def result = {
        if (!isInited) {
            throw new UnCaughtException(" : u must init() it before use")
        }
        def xmlnsMap = new FastXmlNamespaceReader().read(oldManifestFile)
        Node applicationRootNode = new Node(null, "appRoot")
        Node permissionRootNode = new Node(null, "perRoot")

        mkp.xmlDeclaration()
        xmlnsMap?.each { key, value ->
            mkp.declareNamespace("${key}": value)
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
            def parseChild = { Iterator<Node> nodeIt, int deepCount, Callback callback ->
                while (nodeIt.hasNext()) {
                    mkp.yield("\n" + tabs[deepCount])
                    def nd = nodeIt.next()

                    if (nd.name() == NODE_COMMENT) {
                        mkp.comment(nd.text())
                        continue
                    } else if (nd.name() == NODE_YIELD) {
                        mkp.yield(nd.text())
                        continue
                    }

                    // 没有子节点就以“/>”结尾
                    if (!nd.children()) {
                        "${nd.name()}"(getAttrs(nd, deepCount + 1))
                        continue
                    }
                    "${nd.name()}"(getAttrs(nd, deepCount + 1)) {
                        callback.onCall(nd, deepCount + 1)
                        if ("application".equalsIgnoreCase(nd.name())) {
                            appendApplicationNodes(applicationRootNode)
                            callback.onCall(applicationRootNode, deepCount + 1)
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
            }
            getChildStr(xmlRoot)
            appendPermissionNodes(permissionRootNode)
            getChildStr(permissionRootNode)
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
