package com.xx.interfaces

import com.xx.exception.GroovyException
import com.xx.exception.UnCaughtException
import com.xx.model.FastXmlNamespaceReader
import org.gradle.api.Project

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

    def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    boolean isInited = false
    Project project
    File appFile
    Node xmlParser

    /**
     * 必须调用
     * @param project
     * @param appFile
     * @param xmlParser
     */
    void init(Project project, File appFile, Node xmlParser) {
        this.project = project
        this.appFile = appFile
        this.xmlParser = xmlParser
        isInited = true
    }

    /**
     *
     * @return a Closure
     */
    protected abstract void appendApplicationNodes(def mkp, Node root)

    /**
     *
     * @return a Closure
     */
    protected abstract void appendPermissionNodes(def mkp, Node root)


    final def result = {
        if (!isInited) {
            throw new UnCaughtException(" : u must init() it before use")
        }
        println("===========ree=r============")
        def xmlnsMap = new FastXmlNamespaceReader().read(appFile)
        Node applicationRootNode = new Node(null, "appRoot")
        Node permissionRootNode = new Node(null, "perRoot")

        mkp.xmlDeclaration()
        xmlnsMap?.each { key, value ->
            mkp.declareNamespace("${key}": value)
        }
        //  todo manifest中有“android:”--BUG
        manifest(xmlParser.attributes()) {
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
                            appendApplicationNodes(mkp, applicationRootNode)
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
            getChildStr(xmlParser)
            appendPermissionNodes(mkp, permissionRootNode)
            getChildStr(permissionRootNode)
        }
    }

}
