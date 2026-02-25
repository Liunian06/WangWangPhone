# 地理位置与天气服务开发文档

本项目的地理位置获取和天气信息查询功能主要由 `js/location.js` 和 `js/weather.js` 两个核心模块提供支持。

---

## 1. 地理位置获取模块 ([js/location.js](../js/location.js))

### 1.1 功能概述
该模块用于获取用户当前所在的城市名称。为了优化性能并尊重用户选择，它采用了多级获取策略。

### 1.2 获取策略 (优先级从高到低)
1.  **手动设置**：检查数据库 `ai_settings` 中是否存在 `manualLocation`。如果存在，则直接使用。
2.  **本地缓存**：检查是否存在 24 小时以内的定位缓存 (`locationData`)。
3.  **在线定位**：通过请求外部 API 进行实时 IP 定位。

### 1.3 技术细节
- **定位接口**：`https://myip.ipip.net/`
- **数据清洗**：
    - 接口返回格式示例：`当前 IP：218.17.40.74  来自于：中国 广东 深圳  电信`
    - 处理逻辑：剔除“中国”和运营商关键词（电信、移动、联通等），提取最后的地域名词作为城市。

### 1.4 示例输入/输出
- **API 响应**: `当前 IP：113.116.141.149  来自于：中国 广东 广州  电信`
- **函数返回**: `"广州"`

---

## 2. 天气信息查询模块 ([js/weather.js](../js/weather.js))

### 2.1 功能概述
该模块根据指定的城市名称，查询该城市的实时天气、风力以及未来三天的天气预报。

### 2.2 实现流程
1.  **参数转换**：使用 `pinyin-pro` 库将中文城市名转换为拼音（例如：`广州` -> `Guangzhou`），因为天气 API 仅支持拼音请求。
2.  **缓存校验**：检查数据库中是否存在该城市且在 24 小时内的天气缓存。
3.  **API 请求**：请求 `goweather` 接口获取数据。
4.  **数据持久化**：将获取到的结果存入 IndexedDB。

### 2.3 技术细节
- **查询接口**：`https://goweather.herokuapp.com/weather/{cityPinyin}`
- **依赖库**：[`js/libs/pinyin-pro.js`](../js/libs/pinyin-pro.js)

### 2.4 示例输入/输出
- **输入**: `getWeather("北京")`
- **内部处理**: 转换为 `Beijing`
- **API 返回示例 (JSON)**:
  ```json
  {
    "temperature": "+5 °C",
    "wind": "12 km/h",
    "description": "Sunny",
    "forecast": [
      { "day": "1", "temperature": "6 °C", "wind": "10 km/h" },
      { "day": "2", "temperature": "4 °C", "wind": "15 km/h" },
      { "day": "3", "temperature": "7 °C", "wind": "8 km/h" }
    ]
  }
  ```
- **函数最终返回**: 包含上述数据及 `timestamp` 的对象。

---

## 3. 开发者调用示例

```javascript
import { getLocation } from './location.js';
import { getWeather } from './weather.js';

async function initEnvironment() {
    try {
        // 1. 获取城市
        const city = await getLocation();
        console.log('当前定位城市:', city);

        // 2. 获取天气
        if (city) {
            const weather = await getWeather(city);
            console.log(`天气：${weather.description}, 温度：${weather.temperature}`);
        }
    } catch (error) {
        console.error('获取环境信息失败:', error);
    }
}
```
