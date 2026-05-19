# SnapSort 前端原型交付说明

本文档面向后续继续编码的模型或工程师，说明当前 Jetpack Compose 前端原型的完成范围、代码入口、Mock 边界和后续接入点。

## 当前状态

当前仓库已在本地 `main` 分支完成前端原型骨架，覆盖 SnapSort 第一版核心 UI 流程：

1. Android Compose 项目骨架。
2. Material 3 主题基础配置。
3. Compose Navigation 页面路由。
4. 首页空状态与任务工作台。
5. 组内照片筛选页。
6. 扫描进度页。
7. 删除确认底部面板。
8. 设置页占位。

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
   首页工作台和 Mock 首页状态。
5. `app/src/main/java/com/snapsort/app/ui/selection/`
   组内选片页和 Mock 选片状态。
6. `app/src/main/java/com/snapsort/app/ui/scan/ScanProgressScreen.kt`
   扫描进度 UI。
7. `app/src/main/java/com/snapsort/app/ui/components/DeleteConfirmationSheet.kt`
   删除确认底部面板。
8. `app/src/main/java/com/snapsort/app/ui/settings/SettingsScreen.kt`
   设置页占位，后续需要补完整设置项。

## 已实现 UI 范围

### 首页

文件：`app/src/main/java/com/snapsort/app/ui/home/HomeScreen.kt`

已实现：

1. 首次空状态：说明文案和 `Select Photo Folder` 主按钮。
2. 活跃任务工作台：顶部文件夹、任务状态、`Finish` 和设置入口。
3. 分组列表：封面占位、`Burst`/`Single` 标签、张数、时间范围、已标记删除数量。
4. Prototype 用 `Mock Toggle` 切换空状态和活跃任务状态。

当前 Mock 状态文件：`HomeViewModel.kt`。

后续替换建议：保留 `HomeUiState` 的方向，但将数据来源改为真实 Repository/ViewModel。后续模型可以把英文文案替换为设计文档中的中文文案，例如 `选择照片文件夹`、`继续上次筛选`、`重新扫描`、`连拍`、`散片`。

### 组内选片页

文件：`app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

已实现：

1. `HorizontalPager` 左右滑动照片。
2. 大面积照片占位色块，优先突出照片区域。
3. 顶部轻量信息：组类型、当前序号、照片时间、RAW 状态。
4. 底部固定操作区：上一张、下一张、标记删除/撤销、完成。
5. 已标记删除照片显示遮罩和删除提示。

当前 Mock 状态文件：`PhotoSelectionViewModel.kt`。

后续替换建议：将 `PhotoMock`、`PhotoGroupMock` 替换为真实 Photo/Group 模型；将色块占位替换为 Coil 图片加载；将按钮操作连接到真实删除标记状态。设计文档要求下滑标记删除、上滑取消标记，当前 prototype 未实现手势触发，只保留按钮和视觉结构。

### 扫描进度页

文件：`app/src/main/java/com/snapsort/app/ui/scan/ScanProgressScreen.kt`

已实现：

1. 进度指示器。
2. 当前扫描阶段。
3. 已处理数量和总数。
4. 取消按钮。

当前参数有默认 Mock 值，因此可以被导航无参调用。后续应由扫描 Flow/ViewModel 驱动 `currentStage`、`currentProgress`、`totalItems` 和 `onCancel`。

### 删除确认底部面板

文件：`app/src/main/java/com/snapsort/app/ui/components/DeleteConfirmationSheet.kt`

已实现：

1. Material 3 `ModalBottomSheet`。
2. 删除汇总：JPG 数量、RAW 数量、总文件数。
3. 可展开文件清单。
4. `Confirm Delete` 和 `Cancel` 操作。

当前文件清单为 Mock 生成。后续应传入真实待删除文件列表，并支持长文件名/路径截断与查看完整内容。

## Mock 边界

以下内容仍是前端演示数据，不应被当作业务实现：

1. `HomeViewModel` 中的 `PhotoGroup` 和 `mockGroups`。
2. `PhotoSelectionViewModel` 中的 `PhotoMock`、`PhotoGroupMock` 和初始照片列表。
3. `DeleteConfirmationSheet` 内部生成的 `mockFiles`。
4. `ScanProgressScreen` 的默认参数。
5. 首页按钮、设置按钮、完成按钮目前没有接入真实导航或业务动作。

## 后续模型优先任务

建议后续模型按以下顺序继续：

1. 增加 Gradle Wrapper，并先确认项目可以本地构建。
2. 补齐 Settings 页面 UI：连拍阈值、排序方向、自动进入下一组、手势快捷、主题跟随系统说明。
3. 接入真实导航回调：选择文件夹、扫描页、分组详情、设置页、删除确认面板。
4. 设计并实现真实领域模型：Task、Photo、Group、DeleteCandidate。
5. 用真实 ViewModel 替换 Mock ViewModel。
6. 接入 SAF 文件夹选择、扫描进度 Flow、Room/DataStore、RAW 匹配和删除授权流程。
7. 将所有英文 UI 文案统一替换为设计文档确认的中文低焦虑文案。
8. 添加 Compose Preview 和 UI 测试，覆盖首页、扫描进度、选片标记、删除确认。

## 已知限制

1. 当前仓库没有 `gradlew`，因此尚未执行真实 Gradle 构建。
2. 当前 UI 仍是 prototype，不包含真实文件扫描、EXIF 读取、RAW 匹配、Room/DataStore 或系统删除授权。
3. 当前 `HomeScreen` 没有接收导航回调，后续需要把 `onSelectFolder`、`onOpenGroup`、`onOpenSettings`、`onFinish` 等事件作为参数传入。
4. 当前 `PhotoSelectionScreen` 的上下滑删除手势尚未实现。
5. 当前设置页仍是占位。
6. 当前无长期删除结果页实现，只有删除确认底部面板。

## 交接建议

后续模型接手时，应优先阅读以下文件，顺序如下：

1. `docs/superpowers/specs/2026-05-19-snapsort-design.md`
2. `docs/frontend-handoff.md`
3. `PRODUCT.md`
4. `app/src/main/java/com/snapsort/app/ui/navigation/SnapSortNavigation.kt`
5. `app/src/main/java/com/snapsort/app/ui/home/HomeScreen.kt`
6. `app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

如果后续模型不擅长前端，应尽量保留现有 Compose 文件结构和视觉层级，只替换 Mock 数据来源，不要大规模重写 UI。
