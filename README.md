# Android NFC Card Manager

项目根目录，后续文档与代码统一放置于此。

## 目录结构

- `docs/PRD.md`：产品需求文档
- `docs/页面原型说明文档.md`：页面原型说明
- `docs/权限矩阵文档.md`：权限矩阵说明
- `docs/本地构建环境检查文档.md`：本地构建环境与排查说明
- `app/`：后续 Android 代码目录

## 构建说明

## 安装与运行说明

## 1. 环境要求

本地运行前建议准备：

- macOS / Windows / Linux
- JDK 17
- Android Studio（推荐）
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- 可用的 Android 真机或模拟器

说明：

- NFC 功能建议使用 **真机** 测试
- 模拟器通常不能完整验证 NFC 读写流程

## 2. 获取项目

```bash
git clone https://github.com/easywangzhe/android-nfc-card-manager.git
cd android-nfc-card-manager
```

## 3. 配置 Java

先确认 Java 版本：

```bash
java -version
```

建议输出为 JDK 17。

如果本机装的是 Homebrew OpenJDK 17，可参考：

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## 4. 配置 Android SDK

项目根目录需要 `local.properties`：

```properties
sdk.dir=/Users/你的用户名/Library/Android/sdk
```

你可以直接复制示例文件：

```bash
cp local.properties.example local.properties
```

然后把 `sdk.dir` 改成你机器上的真实路径。

## 5. 使用 Android Studio 运行

推荐方式：

1. 用 Android Studio 打开 `android-nfc-card-manager`
2. 等待 Gradle Sync 完成
3. 连接 Android 真机或启动模拟器
4. 点击 Run 运行 `app`

## 6. 使用命令行运行

### 6.1 查看任务

```bash
./gradlew tasks
```

### 6.2 构建 Debug 包

```bash
./gradlew assembleDebug
```

### 6.3 运行单元测试

```bash
./gradlew test
```

### 6.4 安装到已连接设备

```bash
./gradlew installDebug
```

如果使用 Windows：

```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
```

## 7. APK 产物位置

Debug APK 默认输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 8. 真机运行建议

为保证 NFC 功能正常，建议：

1. 使用带 NFC 功能的 Android 手机
2. 在系统设置中开启 NFC
3. 首次安装后确认应用已正常安装
4. 读卡/写卡/锁卡/解锁时，将标签贴近手机背部 NFC 感应区

## 9. 常见问题

### 9.1 `Unable to locate a Java Runtime`

说明未安装或未正确配置 JDK。

### 9.2 `SDK location not found`

说明 `local.properties` 未配置或 SDK 路径错误。

### 9.3 NFC 页面进入即报错

请检查：

- 手机是否支持 NFC
- 系统 NFC 是否开启
- 当前安装是否为最新 APK

### 9.4 模拟器无法验证 NFC

这是正常现象，建议改用真机测试。

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

## 应用功能使用说明

## 1. 登录

- 启动应用后先进入登录页
- 演示账号：
  - `operator`
  - `supervisor`
  - `admin`
  - `auditor`
- 演示密码统一为：`123456`

登录成功后会进入首页，并自动恢复上次登录态。

## 2. 首页与角色权限

首页提供以下主要入口：

- 读卡
- 写卡
- 锁卡
- 解锁
- 模板管理
- 日志审计
- 我的 / 设置

不同角色可见/可操作能力不同：

- 普通操作员：读卡、写卡、查看日志
- 主管/审核人：读卡、写卡、锁卡、解锁、查看日志
- 系统管理员：全部功能
- 审计员：查看日志、查看设置

## 3. 读卡

使用方式：

1. 首页点击“读卡”
2. 将 NFC 标签贴近手机背部
3. 应用自动识别卡片
4. 跳转到读卡结果页

当前支持：

- 基础卡型识别
- NDEF 内容读取
- 读卡结果展示
- 最近读卡结果缓存

## 4. 写卡

使用方式：

1. 首页点击“写卡”
2. 选择模板，或手工输入文本内容
3. 点击“开始写卡”
4. 将支持 NDEF 的标签贴近手机背部
5. 写入完成后自动执行回读校验

当前写卡能力：

- 支持 NDEF Text Record 写入
- 支持写后自动回读校验
- 支持模板写卡

## 5. 锁卡

使用方式：

1. 首页点击“锁卡”
2. 阅读高风险提示
3. 勾选风险确认
4. 输入确认词 `LOCK`
5. 点击“开始锁卡”并贴卡

当前锁卡能力：

- 仅支持 **NDEF 永久只读锁卡**
- 锁卡后通常不可逆
- 会自动校验标签是否已不可写

## 6. 解锁

使用方式：

1. 首页点击“解锁”
2. 输入解锁理由
3. 输入解锁凭据
4. 点击“开始解锁”并贴卡

当前解锁能力边界：

- 当前版本仅实现**解锁流程骨架**
- NDEF 永久只读锁定通常不可逆，不能通用解锁
- 对密码保护型卡片，当前主要用于流程演示
- 演示凭据：`123456`

## 7. 模板管理

模板管理页支持：

- 查看模板列表
- 新增模板
- 编辑模板
- 删除模板

模板已接入本地持久化：

- 应用重启后模板仍会保留
- 写卡页会实时使用最新模板列表

## 8. 日志审计

日志页支持：

- 查看本地审计日志
- 点击查看日志详情
- 按操作类型筛选
- 按结果状态筛选
- 关键词筛选

当前会记录：

- 读卡
- 写卡
- 锁卡
- 解锁

## 9. 我的 / 设置

设置页当前支持：

- 查看账号信息
- 查看当前角色
- 查看 NFC 状态
- 查看应用版本与说明
- 清理缓存
- 退出登录

当前“清理缓存”会清理：

- 本地审计日志
- 最近读卡缓存

不会清理：

- 登录态
- 模板数据

## 10. 当前版本说明

当前项目为可运行的演示版，已实现主要流程与基础功能，但仍有以下边界：

- 锁卡目前仅支持 NDEF 永久只读
- 解锁仍以流程骨架为主，未完成通用真实底层解锁命令接入
- 权限控制、登录态、模板、日志目前以本地实现为主
