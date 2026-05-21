# SnapSort

SnapSort 是一款面向相机照片整理场景的 Android 选片应用。它适合在拍摄后把相机照片导入手机文件夹，再快速浏览 JPG/JPEG、按时间分组筛选、标记废片，并在删除前确认关联的 RAW 文件。

## 主要功能

- 选择本地照片文件夹并扫描 JPG/JPEG 文件
- 按拍摄时间将照片分组，便于处理连拍和同一批次照片
- 在选片界面快速标记或取消标记待删除照片
- 删除前展示 JPG、RAW 和总文件数量，降低误删风险
- 支持跟随系统的浅色、深色和动态色主题



## 构建

当前仓库未包含 `gradlew`，可使用本机 Gradle 执行构建：

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:assembleDebug
```

编译 release APK：

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:assembleRelease
```

## Release 签名

release 签名信息从根目录的 `keystore.properties` 读取。该文件和 `.jks` 证书不会提交到 Git。

```bash
cp keystore.properties.example keystore.properties
```

然后按本地证书信息填写：

```properties
storeFile=release-keystore.jks
storePassword=your-store-password
keyAlias=snapsort
keyPassword=your-key-password
```

release APK 输出位置：

```text
app/build/outputs/apk/release/
```
