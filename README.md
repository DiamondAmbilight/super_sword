# 超强之剑 (Super Sword)

![封面](cover.png)

一个基于 **Fabric** 的 **Minecraft 1.20.1** 模组，添加了一把攻击力极高的自定义剑。

## 功能

- 自定义剑 **超强之剑**(`super_sword:custom_sword`)，攻击力远超原版。
- 独立的**创造模式物品栏**标签。
- 攻击特效：让目标**减速**，并**召唤一道闪电**。
- 可合成：2 个**铁锭** + 1 个**金锭**（竖排）。

## 环境要求

- Minecraft **1.20.1**
- Fabric Loader **>= 0.14.23**
- Fabric API（1.20.1 版本）
- Java **17+**

## 构建

```powershell
.\gradlew build
```

生成的 mod jar 位于 `build/libs/super_sword-1.1.0.jar`。

## 游戏内使用

- 获取物品：`/give @s super_sword:custom_sword`
- 合成配方：2 个铁锭 + 1 个金锭（竖排）

## 安装

从 [Releases](https://github.com/DiamondAmbilight/super_sword/releases) 下载对应的 jar，放入游戏版本的 `mods` 文件夹即可。

## 许可证

MIT
