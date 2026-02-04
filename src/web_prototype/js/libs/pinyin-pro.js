/**
 * Mock implementation of pinyin-pro for prototype purposes.
 * Supports common Chinese city names.
 */
export function pinyin(text, options) {
    const cityMap = {
        '北京': 'beijing', '上海': 'shanghai', '广州': 'guangzhou', '深圳': 'shenzhen',
        '杭州': 'hangzhou', '南京': 'nanjing', '成都': 'chengdu', '武汉': 'wuhan',
        '天津': 'tianjin', '重庆': 'chongqing', '西安': 'xian', '苏州': 'suzhou',
        '郑州': 'zhengzhou', '长沙': 'changsha', '东莞': 'dongguan', '沈阳': 'shenyang',
        '青岛': 'qingdao', '合肥': 'hefei', '佛山': 'foshan', '宁波': 'ningbo',
        '昆明': 'kunming', '福州': 'fuzhou', '无锡': 'wuxi', '厦门': 'xiamen',
        '哈尔滨': 'haerbin', '长春': 'changchun', '济南': 'jinan', '大连': 'dalian',
        '贵阳': 'guiyang', '南宁': 'nanning', '太原': 'taiyuan', '石家庄': 'shijiazhuang'
    };

    let result = cityMap[text];
    
    // Simple fallback for unknown chars (just keep them or basic latin)
    if (!result) {
        result = text; 
    }

    if (options && options.type === 'array') {
        return [result];
    }
    return result;
}