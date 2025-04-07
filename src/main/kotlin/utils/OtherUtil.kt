package utils

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist


fun generateDiff(filePath: String, originText: String, modifiedText: String): String {
    val normalOriginText = normalizeLineEnding(originText)
    val normalModifiedText = normalizeLineEnding(modifiedText)

    var diff = ""

    diff+="--- $filePath\t(original)\n"
    diff+="+++ $filePath\t(modified)\n"

    val originLines = normalOriginText.split("\n")
    val modifiedLines = normalModifiedText.split("\n")

    if(normalOriginText==normalModifiedText){
        diff+="No changes\n"
    }else{
        diff+="@@ -1,${originLines.size} +1,${modifiedLines.size} @@\n"

        for(line in originLines){
            diff+="-$line\n"
        }
        for(line in modifiedLines){
            diff+="+$line\n"
        }
    }
    return diff
}

fun cleanHtmlThoroughly(html: String): String {
    // 首先使用 Whitelist 清理
    var cleaned = Jsoup.clean(html, Safelist()
        .addTags("p", "div", "span"))

    // 再次解析并移除可能遗漏的内容
    val doc = Jsoup.parse(cleaned)
    doc.select("script, style, link[rel=stylesheet]").remove()
    doc.select("*").removeAttr("style").removeAttr("class").removeAttr("id")
    // 移除所有 <a> 标签的 href 属性
    doc.select("a[href]").removeAttr("href")
    // 移除所有 <img> 标签的 src 属性
    doc.select("img[src]").removeAttr("src")
     // 移除所有 <iframe>, <embed>, <object> 等可能包含外部资源的标签
    doc.select("iframe, embed, object, frame").remove()
    return doc.html()
}