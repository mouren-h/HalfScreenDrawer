# HalfScreenDrawer

[![](https://jitpack.io/v/mouren-h/HalfScreenDrawer.svg)](https://jitpack.io/#mouren-h/HalfScreenDrawer)

纯 Java 实现的 Android 中半屏抽屉弹窗（Bottom Sheet Dialog）。外壳（面板、拖拽横条、动画、手势）全部由 Java 代码创建，内容区加载你传入的任意 XML 布局。**零第三方依赖、零资源文件依赖**，通过 JitPack 一行依赖即可接入。

## 特性

- 默认 50% 屏高，按住顶部横条上拉可扩展至 60% 屏高
- 下拉超过阈值自动关闭，未超过则回弹吸附到 50% / 60%
- 缓慢平滑的动画：入场 450ms 减速滑入，关闭 400ms 加速滑出
- 所有关闭入口（点击外部、下拉手势、条目点击、收起按钮、手动调用）统一走缓慢下滑动画
- 内容布局完全自定义：传入任意 `R.layout.xxx`，通过回调拿到布局实例设置子控件
- 不依赖任何 strings / colors / dimens / drawable / styles 资源文件
- 纯 Java，无第三方库，最低 API 21

## 安装

**1. 在 `settings.gradle` 中添加 JitPack 仓库**

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**2. 在模块 `build.gradle` 中添加依赖**

```groovy
dependencies {
    implementation 'com.github.mouren-h:HalfScreenDrawer:1.0'
}
```

## 快速开始

**1. 创建实例并显示**

```java
import mr.x.halfscreendrawer.HalfScreenDrawer;

HalfScreenDrawer drawer = new HalfScreenDrawer(this, R.layout.home);
// 等价写法：
// HalfScreenDrawer drawer = HalfScreenDrawer.show(this, R.layout.home);
```

**2. 通过回调设置内容布局的子控件**

```java
drawer.setOnBindViewListener(new HalfScreenDrawer.OnBindViewListener() {
    @Override
    public void onBindView(View contentView) {
        // contentView 就是 R.layout.home 加载出来的实例
        TextView title = (TextView) contentView.findViewById(R.id.tvTitle);
        title.setText("我的抽屉");
    }
});
```

**3. 手动关闭（带缓慢下滑动画）**

```java
drawer.closeWithAnimation();
```

## API

| 方法 | 说明 |
| --- | --- |
| `new HalfScreenDrawer(activity, layoutResId)` | 创建并立即显示抽屉，内容区加载指定布局 |
| `HalfScreenDrawer.show(activity, layoutResId)` | 静态创建方式，与构造方法等价 |
| `setOnBindViewListener(listener)` | 回调返回加载好的内容布局实例，用于设置子控件 |
| `getContentView()` | 获取内容布局实例 |
| `gb()` | 手动关闭，带缓慢下滑动画 |
| `closeWithAnimation()` | 同 `gb()`，带缓慢下滑动画 |
| `dismiss()` | 直接关闭（无动画） |

## 自定义动画速度

动画时长集中在类顶部两个常量，改数字即可全局生效：

```java
private static final long ANIM_SHOW = 450; // 显示动画时长（毫秒）
private static final long ANIM_HIDE = 400; // 关闭动画时长（毫秒）
```

## 示例内容布局

组件自带一套示例控件绑定：布局中若存在 `tvItemA` ~ `tvItemD`（列表条目）和 `btnClose`（收起按钮）这些 id，会自动获得圆角水波纹背景和点击事件；不存在则跳过，不影响你自己的布局。

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="中半屏抽屉"
        android:textSize="18sp"
        android:textColor="#FF1F1F1F"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tvTip"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:text="默认 50% 高，按住横条上拉可到 60%，下拉关闭"
        android:textSize="12sp"
        android:textColor="#FF8A8F99" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:scrollbars="none">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvItemA"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="8dp"
                android:gravity="center_vertical"
                android:paddingLeft="16dp"
                android:paddingRight="16dp"
                android:text="半屏内容 A"
                android:textSize="14sp"
                android:textColor="#FF3A3F47" />

            <!-- tvItemB / tvItemC / tvItemD 同上 -->
        </LinearLayout>
    </ScrollView>

    <TextView
        android:id="@+id/btnClose"
        android:layout_width="match_parent"
        android:layout_height="46dp"
        android:layout_marginTop="12dp"
        android:gravity="center"
        android:text="收起半屏抽屉"
        android:textSize="15sp"
        android:textColor="#FFFFFFFF" />
</LinearLayout>
```

## 环境要求

- Android API 21+（条目水波纹背景需要）
- 纯 Java，无任何第三方依赖
- 基于 `Dialog` 实现，无需在 AndroidManifest 注册
