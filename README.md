# 项目概述
本项目是一个基于 Kotlin 的 MCP（Model Context Protocol）Server项目，旨在提供一个简单的纯 Kotlin 环境的 MCP 示例。项目包含了一些常用的工具函数，用于处理 Markdown 文件和文件路径操作，也用于个人的MCP工具存放。

## 目前包含的工具
1. 为指定的markdown文件生成标签，并追加在H1标题后面
2. 读取指定的文件
3. 读取指定目录下的所有文件及文件夹
4. 创建目录
5. 编辑替换文本文件中的内容
6. 检索对应网页的内容
7. 查找文件
8. 写入文件
9. 读取excel的sheets
10. 读取excel


## 运行项目

```bash
./gradle build 
java -jar project/build/libs/mcp-server-0.0.1-SNAPSHOT.jar
```

本项目使用SSE进行client及server之间的交互，选用ktor作为http框架进行server的构建，因此需要暴露一个端口，通常使用该地址**http://localhost:8080/sse** 注册到MCP client
