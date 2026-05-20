# SnapSort 前端原型交付说明

本文档面向后续继续编码的模型或工程师，说明当前 Jetpack Compose 实现范围、代码入口、真实接入边界和后续任务。

## 当前状态

当前仓库已在本地 `main` 分支完成前端原型骨架，并已开始接入第一版真实数据链路：

1. Android Compose 项目骨架。
2. Material 3 主题基础配置（已优化为专业 slate/charcoal 克制配色，并修复了暗黑模式的纯白背景 bug）。
3. Compose Navigation 页面路由。
4. 首页空状态与任务工作台（已进行版式调整，提升字重和控件一致性，移除 Secret Toggle）。
5. 组内照片筛选页（已优化导航底栏节奏，重新设计非阻塞标记删除悬浮态）。
6. 扫描进度页。
7. 删除确认底部面板（已加入可视化文件统计面板与弱化清单背景）。
8. 设置页（已优化卡片和段落留白）。

2026-05-20 继续开发进展：

1. 首页、扫描、选片、设置、删除确认和删除结果流程已串联。
2. 核心中文文案已替换为设计文档中的低焦虑表达。
3. 组内选片页已加入下滑标记删除、上滑取消标记的阈值手势反馈。
4. 新增 `core` 纯 Kotlin 规则层，覆盖 JPG/JPEG 识别、RAW 匹配和照片分组。
5. 新增 JVM 单元测试覆盖 RAW 匹配和分组算法关键规则。
6. 已新增 `gradle.properties` 启用 AndroidX，并新增 `.gitignore` 忽略构建产物。
7. 尝试生成 Gradle Wrapper 时，Gradle 对 `https://services.gradle.org/distributions/gradle-8.12-bin.zip` 的 distribution URL 校验失败；当前仍使用本机已缓存 Gradle 8.12 可执行文件验证。
8. 新增 Room 持久化，保存最近任务、分组、照片、RAW 匹配结果和删除标记。
9. 新增 DataStore 设置存储，设置页已接入真实读写。
10. 新增 SAF 文件夹选择与当前层级扫描，扫描结果会写入 Room。
11. 新增 ExifInterface 拍摄时间读取，失败时回退文件修改时间。
12. 首页和选片页已从 Mock 状态切到 Room 数据；图片预览已接入 Coil。
13. 删除确认面板已读取真实待删除 JPG/RAW 文件清单。
14. 已接入 Android `MediaStore.createDeleteRequest` 系统删除授权入口；授权返回后会尝试打开原 URI 校准删除结果，成功删除的 JPG 从 Room 移除，失败项保留标记。
15. 2026-05-20 真机测试发现 SAF Document URI 传入 `MediaStore.createDeleteRequest` 时可能不会弹出系统确认框；已增加 `DocumentFile.fromSingleUri(...).delete()` 回退删除路径，并通过 `SnapSortDelete` tag 输出删除日志。
16. 2026-05-20 进行了全面的 UI 体验审查和改进。提升 Typography 对比度；主题去掉了默认 Material Purple，改用与摄影场景更协调的低饱和度 Slate 配色方案；优化删除确认 Sheet 排版；特别是选片界面的已删除标记由 58% 透明度全屏遮黑优化为了仅包含浅红色微底与右上方提示气泡，保证删除后仍能识别图片细节。代码完全编译通过且单元测试通过。
17. 2026-05-20 在 `MainActivity` 中启用了 Android 15 级别的 edge-to-edge (沉浸式状态栏/导航栏) 支持，并在各页面适配了 WindowInsets；同时在设置页中添加了 `ThemeMode`（跟随系统/浅色/深色）的手动主题切换功能。

核心产品和交互依据见：`docs/superpowers/specs/2026-05-19-snapsort-design.md`。

原始实现计划见：`docs/superpowers/plans/2026-05-19-snapsort-frontend-prototype-plan.md`。

## 代码入口

关键文件如下：

1. `app/src/main/java/com/snapsort/app/MainActivity.kt`
   App 入口，调用 `SnapSortTheme` 和 `SnapSortApp()`。
2. `app/src/main/java/com/snapsort/app/ui/navigation/SnapSortNavigation.kt`
   Compose Navigation 路由入口，当前包含 `home`、`scan_progress`、`group_selection/{groupId}`、`settings`。
3. `app/src/main/java/com/snapsort/app/ui/theme/`
   Material 3 主题、颜色和字体配置。
4. `app/src/main/java/com/snapsort/app/ui/home/`
   首页工作台和 Room 驱动的首页状态。
5. `app/src/main/java/com/snapsort/app/ui/selection/`
   组内选片页和 Room 驱动的选片状态。
6. `app/src/main/java/com/snapsort/app/ui/scan/ScanProgressScreen.kt`
   扫描进度 UI。
7. `app/src/main/java/com/snapsort/app/ui/components/DeleteConfirmationSheet.kt`
   删除确认底部面板。
8. `app/src/main/java/com/snapsort/app/ui/settings/SettingsScreen.kt`
   设置页 UI，已接入 DataStore。
9. `app/src/main/java/com/snapsort/app/core/`
   平台无关核心规则：文件类型识别、RAW 匹配、照片分组。
10. `app/src/test/java/com/snapsort/app/core/`
   JVM 单元测试，当前覆盖 RAW 匹配和分组算法。
11. `app/src/main/java/com/snapsort/app/data/db/`
   Room 数据库、实体和 DAO。
12. `app/src/main/java/com/snapsort/app/data/settings/UserSettingsRepository.kt`
   DataStore 用户设置。
13. `app/src/main/java/com/snapsort/app/data/scanner/FolderScanner.kt`
   SAF 当前层级扫描、EXIF 读取、RAW 匹配和分组。
14. `app/src/main/java/com/snapsort/app/data/repository/TaskRepository.kt`
   最近任务读写、删除标记和待删除候选。

## 已实现 UI 范围

### 首页

文件：`app/src/main/java/com/snapsort/app/ui/home/HomeScreen.kt`

已实现：

1. 首次空状态：说明文案和 `选择照片文件夹` 主按钮。
2. 活跃任务工作台：顶部文件夹、任务状态、`完成` 和设置入口。
3. 分组列表：封面预览、`连拍`/`散片` 标签、张数、时间范围、已标记删除数量。
4. 首页已由 `HomeViewModel` 观察 Room 最近任务，不再依赖 Mock Toggle。

当前状态文件：`HomeViewModel.kt`。

后续替换建议：继续完善空状态中的“继续上次筛选”和“重新扫描”行为；当前选择文件夹会直接进入系统文件夹选择器并扫描。

### 组内选片页

文件：`app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

已实现：

1. `HorizontalPager` 左右滑动照片。
2. 大面积 JPG 预览区域。
3. 顶部轻量信息：组类型、当前序号、照片时间、RAW 状态。
4. 底部固定操作区：上一张、下一张、标记删除/撤销、完成。
5. 已标记删除照片显示遮罩和删除提示。

当前状态文件：`PhotoSelectionViewModel.kt`。

当前 `PhotoSelectionViewModel.kt` 已读取 Room 照片数据，并写回真实删除标记。UI 层使用 `PhotoSelectionItem`、`PhotoSelectionGroup` 作为轻量展示模型。

已实现 Coil JPG 预览、按钮标记删除/取消标记、手势快捷开关接入 DataStore。

### 扫描进度页

文件：`app/src/main/java/com/snapsort/app/ui/scan/ScanProgressScreen.kt`

已实现：

1. 进度指示器。
2. 当前扫描阶段。
3. 已处理数量和总数。
4. 取消按钮。

当前已由 `ScanProgressViewModel` 驱动真实扫描 Flow。取消扫描会取消当前扫描 Job，不保存不完整任务。

### 删除确认底部面板

文件：`app/src/main/java/com/snapsort/app/ui/components/DeleteConfirmationSheet.kt`

已实现：

1. Material 3 `ModalBottomSheet`。
2. 删除汇总：JPG 数量、RAW 数量、总文件数。
3. 可展开文件清单。
4. `确认删除` 和 `取消` 操作。

当前文件清单已来自 `TaskRepository.getDeleteCandidates()`。长文件名在列表中截断显示。

### 核心规则层

目录：`app/src/main/java/com/snapsort/app/core/`

已实现：

1. `ScannedFile`、`ScannedPhoto`、`RawMatch`、`PhotoGroup` 等平台无关模型。
2. `isJpgFile()`：`.jpg/.jpeg` 大小写不敏感识别。
3. `isSupportedRawFile()`：支持 `.raw`、`.cr2`、`.cr3`、`.nef`、`.arw`、`.raf`、`.rw2`、`.dng`、`.orf`、`.pef`，大小写不敏感。
4. `matchRawFiles()`：按同一输入集合内相同基础文件名严格匹配 RAW，不做模糊匹配。
5. `groupPhotos()`：按拍摄时间、文件名、URI 稳定排序；按相邻间隔阈值生成连拍组；连续散片合并；排序方向支持从新到旧和从旧到新。

当前测试：

1. `RawMatcherTest`
2. `PhotoGrouperTest`

验证命令：

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle testDebugUnitTest :app:compileDebugKotlin
```

### 持久化与扫描

已实现：

1. Room 数据库 `SnapSortDatabase`。
2. `TaskEntity`、`PhotoGroupEntity`、`PhotoEntity` 保存最近任务快照。
3. `UserSettingsRepository` 使用 DataStore 保存连拍阈值、排序方向、自动进入下一组、手势快捷。
4. `FolderScanner` 使用 SAF `DocumentFile.fromTreeUri()` 扫描所选文件夹当前层级。
5. 扫描器只过滤 JPG/JPEG，不递归子目录。
6. 使用 `ExifInterface.TAG_DATETIME_ORIGINAL` 读取拍摄时间，失败时回退文件修改时间。
7. 使用核心 `matchRawFiles()` 和 `groupPhotos()` 生成结果。

当前限制：

1. 重新扫描会替换最近任务分组，但会按仍存在照片的 JPG URI 保留删除标记；不存在的文件会从新快照中移除，新增 JPG 默认未标记。
2. 删除优先尝试 MediaStore 系统授权；如果创建请求失败，会回退到 SAF `DocumentFile.delete()`。删除校准通过“URI 是否仍可打开”判断成功/失败，仍需在更多设备和文件提供器上实测。
3. 删除失败原因当前为 App 校准生成的通用原因，Android 系统并不会为每个文件返回细粒度失败原因。

## 真实接入边界

以下内容已经接入真实数据或系统能力：

1. 首页任务、分组和标记数量来自 Room。
2. 选片页照片列表和删除标记来自 Room。
3. 设置页读写 DataStore。
4. 扫描页使用 SAF 文件夹 URI 扫描当前层级，并写入 Room。
5. 删除确认文件清单来自真实删除候选。
6. 删除授权使用 Android `MediaStore.createDeleteRequest`。

以下内容仍需继续完善：

1. 删除结果页已接入真实删除校准结果，但失败原因仍是通用文案。
2. 重新扫描已保留仍存在照片的删除标记，但还没有独立 UI 告知新增/移除数量。

## 后续模型优先任务

建议后续模型按以下顺序继续：

1. 继续尝试增加 Gradle Wrapper；当前阻塞为 Gradle distribution URL 校验失败。
2. 增加重新扫描结果提示，展示新增、移除和保留标记数量。
3. 增加更多设备上的删除授权实测，确认 SAF Document URI 与 MediaStore 删除授权的兼容性。
4. 添加 Compose Preview 和 UI 测试，覆盖首页、扫描进度、选片标记、删除确认。
5. 增加 Room 迁移策略和数据库测试。

## 已知限制

1. 当前仓库没有 `gradlew`；已尝试生成，但 Gradle distribution URL 校验失败。
2. 当前已包含真实文件扫描、EXIF 读取、RAW 匹配、Room/DataStore 和系统删除授权入口。
3. 当前 `HomeScreen` 已接收导航回调，并由 Room 数据驱动。
4. 当前 `PhotoSelectionScreen` 已实现上下滑删除手势，并已接入真实设置开关。
5. 当前设置页已接入 DataStore。
6. 当前删除结果页已接入真实校准结果，但失败原因粒度有限。

## 交接建议

后续模型接手时，应优先阅读以下文件，顺序如下：

1. `docs/superpowers/specs/2026-05-19-snapsort-design.md`
2. `docs/frontend-handoff.md`
3. `PRODUCT.md`
4. `app/src/main/java/com/snapsort/app/ui/navigation/SnapSortNavigation.kt`
5. `app/src/main/java/com/snapsort/app/ui/home/HomeScreen.kt`
6. `app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

如果后续模型继续接手，应尽量保留现有 Compose 文件结构和视觉层级，优先补齐数据校准、删除结果和测试，不要大规模重写 UI。
