# Remote Web Manager

一个轻量 Android 远程 Web 设备管理器。

## 功能
- 多设备列表
- Room 持久化（杀进程/重启仍保留）
- 扫码添加远程 URL
- 粘贴 URL 添加/编辑
- 长按设备：打开 / 编辑 / 删除
- 内置 WebView，支持 JS、Cookie、localStorage、文件上传
- 切后台不主动 reload；Activity 状态保存时尝试恢复 WebView
- 记住最近打开设备 ID

## 构建
使用 Android Studio 打开项目并构建，或推送到 GitHub 后使用 `.github/workflows/android.yml` 自动产出 debug APK。

## 自动升级
应用内置自升级（基于 GitHub Releases，仓库需保持公开）：

- 每次打开应用静默检查更新（24 小时节流），也可在首页右上角「检查更新」手动触发。
- 发新版本：修改 `app/build.gradle.kts` 里的 `versionCode`/`versionName`，提交后打 tag 推送：
  ```bash
  git tag v0.2.1 && git push origin main --tags
  ```
  CI 构建完会自动创建同 tag 的 Release 并附上 APK，已安装的旧版本即可收到更新提示。

> 远程 URL 可能包含 sid/hash/token 等凭据。当前 v0.1 使用 Room 明文保存 URL，仅建议个人设备使用。正式版本建议改为 Android Keystore + 加密字段。
