# Android NFC Card Manager

项目根目录，后续文档与代码统一放置于此。

## 目录结构

- `docs/PRD.md`：产品需求文档
- `docs/页面原型说明文档.md`：页面原型说明
- `docs/权限矩阵文档.md`：权限矩阵说明
- `docs/本地构建环境检查文档.md`：本地构建环境与排查说明
- `app/`：后续 Android 代码目录

## 构建说明

当前项目已补齐 Gradle Wrapper：

- `./gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

本地构建前需要先安装并配置：

1. JDK 17
2. Android SDK
3. `ANDROID_HOME` 或 Android Studio 默认 SDK 环境

常用命令：

- `./gradlew tasks`
- `./gradlew assembleDebug`
- `./gradlew test`

说明：当前环境未安装 Java Runtime，因此暂时无法在本机完成实际构建验证。

当前机器已完成：

- 已安装 `openjdk@17`
- 已安装 Android Command-line Tools
- 已安装 Android SDK Platform 35
- 已安装 Android SDK Build-Tools 35.0.0
- 已生成 `local.properties`

## CI 说明

已接入 GitHub Actions：

- 工作流文件：`.github/workflows/android-build.yml`
- 触发条件：推送到 `master`、针对 `master` 的 PR、手动触发
- 执行动作：
  - 安装 JDK 17
  - 安装 Android SDK 依赖
  - 执行 `./gradlew test`
  - 执行 `./gradlew assembleDebug`
  - 上传 `app-debug.apk` 构建产物
