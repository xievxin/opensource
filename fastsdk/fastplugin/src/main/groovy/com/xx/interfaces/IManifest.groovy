package com.xx.interfaces

import com.xx.exception.UnCaughtException
import com.xx.model.FastXmlReader
import org.gradle.api.Project

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

/**
 * 2018-6-5 将集成代码集中在一个区域模块，如果用户进行了移动，下一次Build会将其还原到区域内
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
    static final String BackupManifestName = "backupManifest.xml"

    static
    def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    boolean isInited = false
    Project project
    File manifestFile
    File oldManifestFile
    Node xmlRoot
    def xmlnsMap

    def curRoot
    Node compRoot = new Node(null, "compRoot")
    Node pmsRoot = new Node(null, "pmsRoot")
    HashMap<String, Object> compMap = new HashMap()   // 已存在的四大组件
    HashMap<String, Object> pmsMap = new HashMap()   // 权限

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
        backUpManifest()
        isInited = true
    }

    private final void realInit() {
        manifestFile = new File(project.name + "/src/main/AndroidManifest.xml")
        oldManifestFile = new File(project.name + "/src/main/${BackupManifestName}")

        xmlnsMap = new FastXmlReader().readNamespace(manifestFile)
        xmlRoot = new XmlParser().parse(manifestFile)

        recoverNamespace1(xmlRoot)
    }

    /**
     * 把转换了的“{http....}”域名替换掉
     * @param root
     */
    void recoverNamespace1(Node root) {
        def map = root.attributes()
        def temMap = [:]
        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
            def entry = it.next()
            String key = entry.getKey().toString()
            xmlnsMap?.each { String xkey, String xvalue ->
                if (key.contains(xvalue)) {
                    temMap.put(xkey + ":" + key.substring(key.lastIndexOf("}") + 1), entry.getValue())
                    it.remove()
                }
            }
        }
        temMap.each {
            map.put(it.key, it.value)
        }
        temMap.clear()

        root.each { Node node ->
            recoverNamespace1(node)
        }
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

    private Node _appendNode(String name, Map attributes, Object value) {
        attributes.keySet().each {
            if (it.toString() == "android:name") {
                if (isPermissionTag(name)) {
                    pmsMap.put(attributes.get(it), null)
                } else {
                    compMap.put(attributes.get(it), null)
                }
            }
        }
        return new Node(curRoot, name, attributes, value)
    }


    final def result = {
        if (!isInited) {
            throw new UnCaughtException(" u must init() it before use")
        }

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
                    attrMap.put((size > 1 ? "\n" + tabs[tabCount] : "") + key.toString(), value)
                }
                attrMap
            }
            def parseChild = { Iterator<Node> nodeIt, int deepCount, NodeCallback callback ->
                while (nodeIt.hasNext()) {
                    def nd = nodeIt.next()
                    def name = nd.name()

                    if (name == NODE_COMMENT) {
                        mkp.yield("\n" + tabs[deepCount])
                        mkp.comment(nd.text())
                        continue
                    } else if (name == NODE_YIELD) {
                        mkp.yield("\n" + tabs[deepCount])
                        mkp.yield(nd.text())
                        continue
                    } else if (isPermissionTag(name)) {
                        if (pmsMap.containsKey(nd.attribute("android:name")))
                            continue
                    } else if (compMap.containsKey(nd.attribute("android:name"))) {
                        continue
                    }

                    mkp.yield("\n" + tabs[deepCount])

                    // 没有子节点就以“/>”结尾
                    if (!nd.children()) {
                        "${name}"(getAttrs(nd, deepCount + 1))
                        continue
                    }
                    "${name}"(getAttrs(nd, deepCount + 1)) {
                        callback.onCall(nd, deepCount + 1)
                        if ("application" == name) {
                            compMap.clear()
                            callback.onCall(compRoot, deepCount + 1)
                        }
                    }
                    if ("application" == name) {
                        // 和permission节点间隔2行
                        mkp.yield("\n\n")
                    }
                }
                mkp.yield("\n" + tabs[deepCount - 1])
            }
            def getChildStr = { Node node ->
                if (node?.children()) {
                    parseChild(node.iterator(), 1, new NodeCallback() {
                        @Override
                        void onCall(Node nd, int curDeepCount) {
                            parseChild(nd.iterator(), curDeepCount, this)
                        }
                    })
                }
            }

            curRoot = compRoot
            appendApplicationNodes()
            curRoot = pmsRoot
            appendPermissionNodes()
            curRoot = null

            getChildStr(xmlRoot)
            pmsMap.clear()
            getChildStr(pmsRoot)
        }
    }

    boolean isPermissionTag(String tag) {
        tag == "permission" || tag == "uses-permission"
    }

    /**
     * 每次运行前给用户备份一下
     */
    private void backUpManifest() {
        if (oldManifestFile.exists()) {
            if (!oldManifestFile.delete()) {
                return
            }
        }
        Files.copy(manifestFile.toPath(), oldManifestFile.toPath())
    }

}
