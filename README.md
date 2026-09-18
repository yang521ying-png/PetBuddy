# 悬浮小布偶（PetBuddy）

一个安卓悬浮窗桌面宠物应用：一只毛绒小布偶常驻在其他应用上层，陪你上班、刷剧、学习。

## 功能

| 功能 | 说明 |
|---|---|
| 🪟 悬浮窗 | 布偶显示在所有应用上层，后台服务常驻 |
| ✋ 拖动 | 长按拖动布偶，松手自动吸附屏幕左右边缘 |
| 💕 单击摸摸 | 摸摸互动，心情 +15，布偶点头、冒气泡、切换开心表情 |
| 🍎 双击菜单 | 打开操作面板：投喂（饱腹 +25）/ 关闭布偶 |
| 🎭 表情系统 | 正常 / 开心 / 饥饿 / 睡觉 四种表情随状态切换 |
| 📉 状态衰减 | 心情、饱腹度随时间自然下降（1 分钟 -1），需要你照顾 |
| 🔔 前台通知 | 常驻通知显示实时状态，带"投喂 / 关闭"快捷按钮 |
| ✨ 待机动画 | 布偶待机时上下浮动呼吸，互动时点头 / 跳跃 / 摇摆 |

## 构建步骤（在你的电脑上）

1. 安装 **Android Studio**（Koala 2023.1 或更新版本），并确保 SDK Platform 34 已安装。
2. 打开 Android Studio：**File → Open**，选择 `PetBuddy` 目录，等待 Gradle Sync 完成。
   - 如果提示 "Gradle wrapper not found / 选择 Gradle 版本"，直接选择使用 Android Studio 自带的 Gradle 即可（或点击提示中的修复按钮自动生成 wrapper）。
3. 手机开启 **开发者选项 → USB 调试**，用数据线连接电脑。
4. 点击工具栏绿色 ▶ Run 运行到手机；或 **Build → Build App Bundle(s) / APK(s) → Build APK(s)** 生成安装包（产物在 `app/build/outputs/apk/debug/`）。
5. 安装后首次使用：
   - 打开 App → 点击「开启悬浮布偶」→ 系统会跳转到悬浮窗权限页，授予权限后重新点开启。
   - Android 13+ 首次会弹通知权限请求，允许后即可收到常驻通知。

## 没有电脑？用手机 + GitHub 云端自动编译（免费）

工程里已包含 GitHub Actions 配置（`.github/workflows/android-build.yml`），上传到 GitHub 后云端自动打包 APK，全程手机操作：

1. 手机浏览器打开 **github.com** → 注册/登录 → 右上角 **+ → New repository** 新建一个仓库（如 `PetBuddy`，Public / Private 均可）。
2. 进入新仓库 → **Add file → Upload files**，把解压后的 `PetBuddy` 文件夹里的**所有文件**上传（手机先解压 zip，多选上传；注意保留 `app`、`.github` 等目录结构）。
3. 上传完成点 **Commit changes** → 仓库页顶部 Actions 开始运行，等 3~5 分钟变绿。
4. 打开仓库 **Actions** 标签 → 点最新一条运行记录 → 底部 **Artifacts** 区域点 **petbuddy-debug-apk** 下载 zip，解压得到 `app-debug.apk`。
5. 手机安装 APK（需允许"安装未知来源应用"），打开授权悬浮窗即可使用。

> 说明：该方式产出的是 debug 版 APK（已自动签名，可直接安装）。构建日志在 Actions 页面可见，若构建失败把红色报错信息发给我即可排查。

## 使用说明

- **单击**布偶：摸摸它（心情上升）
- **双击**布偶：打开操作面板（投喂 / 关闭）
- **长按拖动**：移动位置，松手自动吸附到屏幕边缘
- 布偶饿到一定程度会晃动身体并冒气泡"我饿了…"，记得喂它

## 自定义布偶形象

替换 `app/src/main/res/drawable/` 下的四张图即可换形象（保持同名 PNG，建议透明背景）：

- `pet_normal.png` 正常表情
- `pet_happy.png` 开心表情
- `pet_hungry.png` 饥饿表情
- `pet_sleeping.png` 睡觉表情

原始素材在 `raw_assets/`（白底 jpeg），可用 `process_images.py` 重新去底处理：

```bash
python3 process_images.py
```

## 技术说明

- 语言 / 框架：Kotlin，无第三方 UI 依赖（仅 androidx core/appcompat）
- minSdk 24（Android 7.0），targetSdk 34（Android 14）
- 悬浮窗：`WindowManager` + `TYPE_APPLICATION_OVERLAY`（Android 8+）
- 前台服务：`foregroundServiceType="specialUse"`（适配 Android 14 要求）
- 状态持久化：`SharedPreferences`，按时间戳结算衰减

## 注意事项

- 悬浮窗权限是系统安全敏感权限，部分国产 ROM（MIUI / EMUI / ColorOS 等）除授权外还需在「后台管理」中允许自启动 / 后台运行，否则杀后台后布偶会消失。
- 若长按通知栏「关闭布偶」，服务停止后重新打开 App 点「开启」即可恢复。
- 在 Android 15+ 上运行时，如系统要求说明悬浮窗用途，请在权限页简要描述"桌面宠物悬浮窗"。
