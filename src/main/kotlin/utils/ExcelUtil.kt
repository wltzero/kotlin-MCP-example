package utils

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

/**
 * 读取Excel文件并返回Workbook对象
 * @param filePath Excel文件路径
 * @return Workbook对象
 */
fun readExcel(filePath: String): Workbook{
    val file = File(filePath)
    if (!file.exists()) {
        throw IllegalArgumentException("当前文件不存在: $filePath")
    }
    val fileType = file.name.split(".")[1]
    val workbook: Workbook = when (fileType.lowercase()) {
        "xls" -> HSSFWorkbook()
        "xlsx" -> XSSFWorkbook()
        else -> throw IllegalArgumentException("不支持的文件类型: $fileType")
    }
    return workbook
}