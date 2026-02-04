/**
 * Location Service Module
 * Handles city acquisition via IP or manual settings.
 */

/**
 * Gets the current location city.
 * Priority: Manual Setting > Local Cache > Online API
 * @returns {Promise<string>} City name
 */
export async function getLocation() {
    // 1. Manual Setting (Mocking database lookup from localStorage)
    const settings = JSON.parse(localStorage.getItem('ai_settings') || '{}');
    if (settings.manualLocation) {
        console.log('Location: Using manual setting', settings.manualLocation);
        return settings.manualLocation;
    }

    // 2. Local Cache (24h)
    const cache = JSON.parse(localStorage.getItem('locationData') || 'null');
    if (cache && (Date.now() - cache.timestamp < 24 * 60 * 60 * 1000)) {
        console.log('Location: Using cache', cache.city);
        return cache.city;
    }

    // 3. Online Positioning
    try {
        console.log('Location: Fetching online...');
        // Note: In a real browser environment, CORS might be an issue with this specific API directly.
        // For prototype, we assume it works or we might need a proxy. 
        // If it fails due to CORS, we fallback.
        const response = await fetch('https://myip.ipip.net/');
        const text = await response.text();
        // Format: "当前 IP：218.17.40.74  来自于：中国 广东 深圳  电信"
        console.log('Location API Response:', text);
        
        let city = 'Unknown';
        // Extract "来自于：" part
        const fromPart = text.split('来自于：')[1];
        if (fromPart) {
            // Remove operator info (电信, 移动, 联通 etc, usually at the end after spaces)
            // Example: "中国 广东 深圳  电信" -> ["中国", "广东", "深圳", "电信"]
            const parts = fromPart.trim().split(/\s+/);
            
            // Filter out "中国" and operators
            // Heuristic: The last geographical name. 
            // Often it is Country Province City Operator.
            // We want the most specific one before the operator.
            
            // Simple logic: iterate backwards, skip known operators/keywords.
            const ignoreList = ['电信', '移动', '联通', '铁通', '教育网', '鹏博士', '宽带'];
            
            for (let i = parts.length - 1; i >= 0; i--) {
                const part = parts[i];
                if (!ignoreList.includes(part) && part !== '中国') {
                    city = part;
                    break;
                }
            }
        }

        if (city !== 'Unknown') {
            localStorage.setItem('locationData', JSON.stringify({
                city: city,
                timestamp: Date.now()
            }));
            return city;
        }
    } catch (e) {
        console.error("Location fetch failed:", e);
    }
    
    // Default fallback
    return "北京"; 
}