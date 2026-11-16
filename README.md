# AAM RWR (Client)

纯客户端 **RWR 雷达 HUD** 喵：不需要服务端安装，单人 / 多人服务器都能用喵。

> Client-side only RWR radar HUD for **Fabric 1.21.11**. No server-side installation required.

## 功能 / Features

### 雷达 / Radar
- 左下角三环雷达盘（`H` 开关喵）
- 玩家红/蓝点（敌/友，按队伍判定喵）
- **所有弹射物**弧段告警（箭、雪球、潜影弹、三叉戟、导弹…喵）
- 弹射物朝你飞来 → 红框「敌导弹」+ 急促提示音喵
- 视野内玩家头顶红/蓝小点喵

### 全实体模式 / All-entity mode（`Z` 切换喵）
- 敌对生物（含**猪灵**）→ 🔴 红色带边框方块喵
- 中立 / 友好生物 → 🟡 黄色圆点喵
- 掉落物 → 🟣 紫色圆点喵
- 玩家 / 弹射物保持原配色喵

### 锁定 / Lock-on
- `X` 手动锁定 / 取消锁定喵
- 准星锥体内保持 **3 秒**自动锁定喵
- 锁定框：名字 + 血条 + 护甲条 + 距离 + 速度喵
- 目标出屏 / 身后 → **屏幕边缘箭头** + 距离 + 名字喵
- 目标离开视野 8 秒自动脱锁喵

### 航标 / Waypoints
```mcfunction
/rwrset 家 120 64 -300      # 添加航标（绿色点 + 坐标 + 距离喵）
/rwrremove 家               # 删除航标
```
- 目标 30 格内**自动删除**并提示喵
- 数据保存在 `config/aam-rwr-waypoints.json` 喵

### 血量 / Health
- 客户端读取真实血量与护甲喵
- **服务端隐藏血量时**（血量报 0 / 大于最大值 / 最大血量被改喵）自动切换本地模型：
  - 按原版最大血量显示（带 `~` 前缀喵）
  - 每次受伤按**手上武器伤害 + 附魔 + 目标护甲/保护**估算扣血喵
- 速度显示：服务端不下发速度时，用**最近 3 秒位移平均**估算（带 `~` 喵）

## 安装 / Installation

1. 装 **Fabric Loader 0.18.4+**
2. 把 **Fabric API 0.141.0+** 和 `aam-rwr-client-1.0.0.jar` 放进 `.minecraft/mods/`
3. 客户端启动即可，**不需要服务端装**喵

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | >= 0.18.4 |
| Fabric API | >= 0.141.0 |

## 按键 / Keybinds

| 默认键 | 功能 |
|---|---|
| `H` | 开关 RWR 雷达 |
| `X` | 手动锁定 / 取消锁定 |
| `Z` | 切换全实体检测模式 |

## 配置文件 / Config

`config/aam-rwr-client.properties`

```properties
range=128.0                 # 雷达探测半径（受客户端实体追踪距离限制喵）
require_helmet=false        # 只在戴 RWR 头盔时显示
sound=true                  # 告警音
threat_range=0.0            # 0 = 朝我飞来就告警；填 64 = 只有 64 格内告警
enabled=true                # HUD 总开关
all_entities=false          # 全实体模式（Z 键切换后自动保存）
hidden_hit_damage=5.0       # 完全算不出伤害时的兜底值
player_protection=0.64      # 旧字段（保留兼容）
default_player_armor=20.0   # 读不到护甲时：玩家
default_mob_armor=1.0       # 读不到护甲时：怪物
default_player_protection=0.64   # 读不到附魔时：玩家（保护 IV）
default_mob_protection=0.0       # 读不到附魔时：怪物
```

## 纯客户端的限制 / Limitations

- 只能看到**客户端已追踪**的实体（远处实体不存在喵）
- 无法得知「你是否被服务端锁定」，告警基于「弹射物朝我飞来」的几何判定喵
- 锁定为纯视觉指示，不给任何武器提供制导喵
- 血量隐藏时的伤害为**估算值**（按武器/附魔/护甲计算，可能存在小误差喵）

> ⚠️ 本模组会显示服务端隐藏的信息（隐身玩家、穿墙目标等喵）。在公开服务器使用前请确认服务器规则喵。

## 构建 / Build

```bash
./gradlew build
# 产物: build/libs/aam-rwr-client-1.0.0.jar
```

## 许可 / License

MIT · 作者 Yuike
