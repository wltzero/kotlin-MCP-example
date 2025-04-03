package utils


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