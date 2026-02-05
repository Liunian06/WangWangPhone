/**
 * 授权管理器 - Web 平台实现
 * 
 * 使用 IndexedDB 进行本地持久化存储
 * 确保应用更新后无需重新激活
 */

const DB_NAME = 'wangwang_license';
const DB_VERSION = 1;
const STORE_NAME = 'license';

/**
 * 授权管理器类
 */
class LicenseManager {
    constructor() {
        this.db = null;
        this.cachedLicense = null;
        this.isInitialized = false;
    }

    /**
     * 初始化数据库
     */
    async initialize() {
        if (this.isInitialized) return true;

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(DB_NAME, DB_VERSION);

            request.onerror = (event) => {
                console.error('LicenseManager: 无法打开数据库', event);
                reject(false);
            };

            request.onsuccess = (event) => {
                this.db = event.target.result;
                this.isInitialized = true;
                console.log('LicenseManager: 初始化成功');
                
                // 尝试从数据库恢复授权
                this.getLicenseRecord().then(record => {
                    this.cachedLicense = record;
                });
                
                resolve(true);
            };

            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // 创建授权信息存储
                if (!db.objectStoreNames.contains(STORE_NAME)) {
                    const store = db.createObjectStore(STORE_NAME, { 
                        keyPath: 'id',
                        autoIncrement: true 
                    });
                    store.createIndex('machineId', 'machineId', { unique: false });
                }
            };
        });
    }

    /**
     * 获取设备机器码
     * 使用 canvas 指纹 + navigator 信息生成
     */
    getMachineId() {
        try {
            // 使用 canvas 指纹
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            ctx.textBaseline = 'top';
            ctx.font = "14px 'Arial'";
            ctx.fillStyle = '#f60';
            ctx.fillRect(125, 1, 62, 20);
            ctx.fillStyle = '#069';
            ctx.fillText('WWJ-WEB-FINGERPRINT', 2, 15);
            ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
            ctx.fillText('WWJ-WEB-FINGERPRINT', 4, 17);
            
            const dataUrl = canvas.toDataURL();
            
            // 简单哈希
            let hash = 0;
            for (let i = 0; i < dataUrl.length; i++) {
                const char = dataUrl.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash = hash & hash;
            }
            
            // 结合 navigator 信息
            const navInfo = navigator.userAgent + navigator.language + screen.width + screen.height;
            for (let i = 0; i < navInfo.length; i++) {
                const char = navInfo.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash = hash & hash;
            }
            
            // 转换为16进制字符串
            const hashHex = Math.abs(hash).toString(16).toUpperCase().padStart(12, '0').substring(0, 12);
            return `WEB-${hashHex}`;
        } catch (e) {
            // 如果 canvas 不可用，使用随机 ID 并存储
            let storedId = localStorage.getItem('wwj_machine_id');
            if (!storedId) {
                storedId = `WEB-${Date.now().toString(16).toUpperCase()}`;
                localStorage.setItem('wwj_machine_id', storedId);
            }
            return storedId;
        }
    }

    /**
     * 验证并激活授权码
     */
    async verifyLicense(licenseKey) {
        try {
            // 检查格式
            if (!licenseKey.startsWith('WANGWANG-')) {
                return { success: false, error: '激活码格式无效' };
            }

            // 解析激活码
            const payload = this.parseLicenseKey(licenseKey);
            if (!payload) {
                return { success: false, error: '激活码解析失败' };
            }

            // 验证机器码
            const currentMachineId = this.getMachineId();
            if (payload.machineId !== currentMachineId) {
                return { success: false, error: '机器码不匹配' };
            }

            // 验证过期时间
            const now = Math.floor(Date.now() / 1000);
            if (payload.expirationTime < now) {
                return { success: false, error: '授权已过期' };
            }

            // 保存到数据库
            const record = {
                licenseKey: licenseKey,
                machineId: payload.machineId,
                expirationTime: payload.expirationTime,
                licenseType: payload.type,
                activationTime: now
            };

            const saved = await this.saveLicenseRecord(record);
            if (saved) {
                this.cachedLicense = record;
                return { success: true, record: record };
            } else {
                return { success: false, error: '保存授权信息失败' };
            }
        } catch (e) {
            console.error('激活失败:', e);
            return { success: false, error: `激活失败: ${e.message}` };
        }
    }

    /**
     * 检查是否已激活
     */
    isActivated() {
        const license = this.cachedLicense;
        if (!license) return false;
        
        const now = Math.floor(Date.now() / 1000);
        return license.expirationTime > now;
    }

    /**
     * 检查授权是否过期
     */
    isExpired() {
        if (!this.cachedLicense) return true;
        const now = Math.floor(Date.now() / 1000);
        return this.cachedLicense.expirationTime <= now;
    }

    /**
     * 获取剩余天数
     */
    getRemainingDays() {
        if (!this.cachedLicense) return 0;
        const now = Math.floor(Date.now() / 1000);
        const remaining = this.cachedLicense.expirationTime - now;
        return remaining > 0 ? Math.floor(remaining / (24 * 60 * 60)) : 0;
    }

    /**
     * 获取过期日期字符串
     */
    getExpirationDateString() {
        if (!this.cachedLicense) return '未激活';
        const date = new Date(this.cachedLicense.expirationTime * 1000);
        return date.toISOString().split('T')[0];
    }

    /**
     * 获取授权类型
     */
    getLicenseType() {
        return this.cachedLicense?.licenseType ?? 'free';
    }

    /**
     * 清除授权信息
     */
    async clearLicense() {
        this.cachedLicense = null;
        return this.clearLicenseRecord();
    }

    /**
     * 保存授权记录到 IndexedDB
     */
    async saveLicenseRecord(record) {
        if (!this.db) return false;

        return new Promise((resolve) => {
            try {
                // 先清除旧记录
                const clearTx = this.db.transaction([STORE_NAME], 'readwrite');
                const clearStore = clearTx.objectStore(STORE_NAME);
                clearStore.clear();

                clearTx.oncomplete = () => {
                    // 插入新记录
                    const tx = this.db.transaction([STORE_NAME], 'readwrite');
                    const store = tx.objectStore(STORE_NAME);
                    store.add(record);

                    tx.oncomplete = () => resolve(true);
                    tx.onerror = () => resolve(false);
                };

                clearTx.onerror = () => resolve(false);
            } catch (e) {
                console.error('保存授权记录失败:', e);
                resolve(false);
            }
        });
    }

    /**
     * 从 IndexedDB 获取授权记录
     */
    async getLicenseRecord() {
        if (!this.db) return null;

        return new Promise((resolve) => {
            try {
                const tx = this.db.transaction([STORE_NAME], 'readonly');
                const store = tx.objectStore(STORE_NAME);
                const request = store.openCursor(null, 'prev'); // 获取最新记录

                request.onsuccess = (event) => {
                    const cursor = event.target.result;
                    if (cursor) {
                        resolve(cursor.value);
                    } else {
                        resolve(null);
                    }
                };

                request.onerror = () => resolve(null);
            } catch (e) {
                console.error('获取授权记录失败:', e);
                resolve(null);
            }
        });
    }

    /**
     * 清除 IndexedDB 中的授权记录
     */
    async clearLicenseRecord() {
        if (!this.db) return false;

        return new Promise((resolve) => {
            try {
                const tx = this.db.transaction([STORE_NAME], 'readwrite');
                const store = tx.objectStore(STORE_NAME);
                store.clear();

                tx.oncomplete = () => resolve(true);
                tx.onerror = () => resolve(false);
            } catch (e) {
                console.error('清除授权记录失败:', e);
                resolve(false);
            }
        });
    }

    /**
     * 解析激活码 (模拟实现)
     * TODO: 实现真正的 RSA 验签 (使用 Web Crypto API)
     */
    parseLicenseKey(licenseKey) {
        try {
            // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
            const rest = licenseKey.substring('WANGWANG-'.length);
            const parts = rest.split('.');

            if (parts.length !== 2) return null;

            // TODO: 验证签名 (使用 Web Crypto API)
            // TODO: Base64 解码并解析 JSON

            // 模拟解析 - 使用当前时间 + 365天作为过期时间
            const now = Math.floor(Date.now() / 1000);
            const expiration = now + (365 * 24 * 60 * 60);

            return {
                machineId: this.getMachineId(),
                expirationTime: expiration,
                type: 'pro',
                salt: 'generated'
            };
        } catch (e) {
            console.error('解析激活码失败:', e);
            return null;
        }
    }
}

// 导出单例
export const licenseManager = new LicenseManager();
export default licenseManager;