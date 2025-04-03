package utils


fun normalizeLineEnding(text: String): String {
    /*对于所有的CRLF和CR转化为LF*/
    return text.replace("\r\n", "\n").replace("\r", "\n")
}