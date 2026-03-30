# External Integrations

**Analysis Date:** 2026-03-30

## APIs & External Services

**Hardware / OS APIs:**
- Android NFC Framework - 用于读卡、写卡、格式化、锁卡与扫描会话
  - SDK/Client: Android 平台 API，直接在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`TagParser.kt`、`NdefWriter.kt`、`NdefFormatter.kt`、`NdefLocker.kt` 中调用 `NfcAdapter`、`Ndef`、`NdefFormatable`、`IsoDep`、`MifareUltralight`
  - Auth: 不适用；依赖设备 NFC 能力与 `app/src/main/AndroidManifest.xml` 中的 `android.permission.NFC`

**Application services:**
- Android SharedPreferences - 用于持久化登录态
  - SDK/Client: Android 平台 API，实现在 `app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`
  - Auth: 不适用
- Android SQLite - 用于本地审计日志与模板持久化
  - SDK/Client: Android 平台 API，实现在 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogDbHelper.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt`
  - Auth: 不适用

**Remote APIs / SaaS:**
- 未发现远程 HTTP API、GraphQL、WebSocket、MQTT 或第三方 SaaS 接入
  - 证据 1：`app/src/main/AndroidManifest.xml` 未声明 `android.permission.INTERNET`
  - 证据 2：`app/build.gradle.kts` 未声明 Retrofit、OkHttp、Ktor、Firebase、AWS、Supabase、Stripe 等网络或云服务 SDK
  - 证据 3：源码搜索仅命中 Android NFC、本地存储与本地权限逻辑，未发现网络客户端实现文件

## Data Storage

**Databases:**
- SQLite（应用本地文件）
  - 连接: 无环境变量；通过 `SQLiteOpenHelper` 直接打开应用私有数据库
  - Client: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogDbHelper.kt` 管理 `audit_logs.db`
  - Client: `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt` 管理 `templates.db`

**File Storage:**
- 本地文件系统仅用于 Android/Gradle 构建配置与 APK 产物
  - 构建配置位于 `local.properties`、`gradle/wrapper/gradle-wrapper.properties`
  - 构建产物输出位于 `app/build/outputs/apk/debug/app-debug.apk`
- 未发现对象存储、云盘或外部文件上传 SDK；证据见 `app/build.gradle.kts` 依赖列表

**Caching:**
- 内存缓存：最近一次读卡结果保存在 `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultStore.kt`
- 会话缓存/持久化：登录态通过 `app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt` 写入 SharedPreferences
- 未发现 Redis、Memcached 或磁盘缓存框架；证据见依赖与源码搜索结果

## Authentication & Identity

**Auth Provider:**
- 自定义本地演示鉴权
  - Implementation: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 内置账号 `operator`、`supervisor`、`admin`、`auditor`，并将会话写入 `SessionStore.kt`
  - 权限控制由 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` 在导航层按角色拦截页面访问
- 未发现 OAuth、OIDC、JWT、Firebase Auth、企业 SSO、LDAP 或后端认证服务；证据是仓库中不存在相关 SDK 依赖与网络权限

## Monitoring & Observability

**Error Tracking:**
- 未发现 Sentry、Firebase Crashlytics、Bugsnag 等错误追踪服务
  - 证据：`app/build.gradle.kts` 无对应依赖，仓库无 `google-services.json`、Crashlytics/Firebase 配置文件

**Logs:**
- 运行期日志：`app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 使用 `android.util.Log.w`
- 业务审计日志：`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt` 写入本地 SQLite，界面展示在 `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogScreen.kt` 与 `AuditLogDetailScreen.kt`

## CI/CD & Deployment

**Hosting:**
- GitHub Releases - APK 发布由 `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml` 上传到 GitHub Release
- 未发现独立后端、CDN、对象存储或移动应用托管平台配置

**CI Pipeline:**
- GitHub Actions
  - `.github/workflows/android-build.yml`：在 push/PR/master/manual 触发时执行 `./gradlew test`、`./gradlew assembleDebug`，并上传 APK Artifact
  - `.github/workflows/android-release.yml`：在 tag `v*` 或手动触发时构建并创建 GitHub Release

## Environment Configuration

**Required env vars:**
- `ANDROID_SDK_ROOT` - CI 中由 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml` 用于生成 `local.properties`
- `JAVA_HOME` - `README.md` 作为本地开发建议记录
- `ANDROID_HOME` - `README.md` 作为本地 SDK 环境说明记录
- 未发现业务 API Key、第三方服务 Token、数据库连接串或云凭据变量；证据是仓库中未发现 `.env` 文件，且构建脚本未读取此类变量

**Secrets location:**
- 未发现应用业务密钥文件或凭据配置
- `local.properties` 存放本地 Android SDK 路径，但不属于业务密钥；`README.md` 提供 `local.properties.example` 作为模板
- GitHub Actions workflow 文件未直接引用仓库 secrets 键名；当前 CI 主要依赖标准环境中的 Android SDK 路径
- 发现硬编码演示凭据逻辑，位置在 `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/UnlockExecutor.kt`；仅记录存在位置，不抄写具体值

## Hardware, Permissions & Device Capabilities

**NFC / hardware:**
- `app/src/main/AndroidManifest.xml` 声明 `android.hardware.nfc`，且 `android:required="false"`
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt` 使用 Reader Mode 扫描 NFC-A / NFC-B / NFC-F / NFC-V
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 解析 `NfcA`、`Ndef`、`IsoDep`、`MifareUltralight` 技术栈

**Permissions:**
- 已发现：`android.permission.NFC`，定义在 `app/src/main/AndroidManifest.xml`
- 未发现：网络、蓝牙、相机、定位、存储、通知等权限；证据是 `app/src/main/AndroidManifest.xml` 未声明这些权限

## Webhooks & Callbacks

**Incoming:**
- 未发现 Webhook、Deep Link、App Link、自定义 URL Scheme 或 Broadcast Receiver 回调
  - 证据：`app/src/main/AndroidManifest.xml` 仅声明 Launcher Activity `com.opencode.nfccardmanager.MainActivity`

**Outgoing:**
- 未发现向外部服务发送回调、Webhook、消息推送或远程审计上报
  - 证据：仓库无网络权限、无 HTTP 客户端依赖、无服务端 URL 配置

---

*Integration audit: 2026-03-30*
