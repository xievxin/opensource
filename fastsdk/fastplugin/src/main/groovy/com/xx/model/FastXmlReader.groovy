package com.xx.model

import java.util.regex.Matcher
import java.util.regex.Pattern

class FastXmlReader {

    Map<String, String> readNamespace(File file) {
        println("FastXmlReader.readNamespace() : " + file?.path)
        FileReader fileReader = new FileReader(file)

        StringBuffer sb = new StringBuffer()

        boolean _xml_end = false
        int len
        char c
        while ((len = fileReader.read()) != -1) {
            c = (char) len
            if (c == '<') {
                continue
            } else if (c == '>') {
                if (!_xml_end) {
                    _xml_end = true
                    continue
                }
                break
            } else {
                if (_xml_end) {
                    sb.append(c)
                }
            }
        }
        fileReader.close()
        println("FastXmlReader.readNamespace() : " + sb.toString() + "\n-------------\n")

        def xmlnsMap = new HashMap()
        def attrs = sb.toString().trim().split("\\s+")
        attrs.each {
            if (it.toString().startsWith("xmlns:")) {
                def str = it.toString().replaceFirst("xmlns:", "").split("=")
                xmlnsMap.put(str[0], str[1].replaceAll("['\"]", ""))
            }
        }

//        xmlnsMap.each {key,value->
//            println(key+"_"+value)
//        }
        xmlnsMap
    }

    String getFileText(File file) {
        FileReader fileReader = new FileReader(file)

        StringBuffer sb = new StringBuffer()
        String str
        while ((str = fileReader.readLine()) != null) {
            sb.append(str)
        }
        try {
            fileReader.close()
        } catch (any) {
        }
        return sb.toString()
    }

    HashMap<String, Object> readComments(File xmlFile) {
        println("FastXmlReader.readComments() : " + xmlFile?.path)

        HashMap<String, Object> map = new HashMap<>()
        Matcher matcher = Pattern.compile("<!--(.*?)-->").matcher(getFileText(xmlFile))
        while (matcher.find()) {
            map.put(matcher.group()
                    ?.replaceFirst("<!--", "")
                    ?.replaceFirst("-->", ""),
                    null)
        }
        map
    }
}
