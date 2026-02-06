/**
 * 布局管理器 - Web 平台实现
 * 
 * 使用 IndexedDB 进行布局持久化存储
 * 确保用户自定义的主屏幕布局在刷新后保留
 */

const LAYOUT_DB_NAME = 'wangwang_layout';
const LAYOUT_DB_VERSION = 1;
const LAYOUT_STORE_NAME = 'app_layout';

class LayoutManager {
    constructor() {
        this.db = null;
        this.isInitialized = false;
    }

    /**
     * 初始化数据库
     */
    async initialize() {
        if (this.isInitialized) return true;

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(LAYOUT_DB_NAME, LAYOUT_DB_VERSION);

            request.onerror = (event) => {
                console.error('LayoutManager: 无法打开数据库', event);
                reject(false);
            };

            request.onsuccess = (event) => {
                this.db = event.target.result;
                this.isInitialized = true;
                console.log('LayoutManager: 初始化成功');
                resolve(true);
            };

            request.onupgradeneeded = (event) => {
                const db = event.target.result;

                if (!db.objectStoreNames.contains(LAYOUT_STORE_NAME)) {
                    const store = db.createObjectStore(LAYOUT_STORE_NAME, {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('appId', 'appId', { unique: false });
                    store.createIndex('area', 'area', { unique: false });
                }
            };
        });
    }

    /**
     * 保存整个布局
     * @param {Array<{appId: string, position: number, area: string}>} items
     */
    async saveLayout(items) {
        if (!this.db) return false;

        return new Promise((resolve) => {
            try {
                // 先清除旧记录
                const clearTx = this.db.transaction([LAYOUT_STORE_NAME], 'readwrite');
                const clearStore = clearTx.objectStore(LAYOUT_STORE_NAME);
                clearStore.clear();

                clearTx.oncomplete = () => {
                    // 插入新记录
                    const tx = this.db.transaction([LAYOUT_STORE_NAME], 'readwrite');
                    const store = tx.objectStore(LAYOUT_STORE_NAME);

                    for (const item of items) {
                        store.add({
                            appId: item.appId,
                            position: item.position,
                            area: item.area || 'grid'
                        });
                    }

                    tx.oncomplete = () => {
                        console.log(`LayoutManager: 布局已保存 (${items.length} 项)`);
                        resolve(true);
                    };
                    tx.onerror = () => resolve(false);
                };

                clearTx.onerror = () => resolve(false);
            } catch (e) {
                console.error('LayoutManager: 保存布局失败:', e);
                resolve(false);
            }
        });
    }

    /**
     * 获取保存的布局
     * @returns {Promise<Array<{appId: string, position: number, area: string}>>}
     */
    async getLayout() {
        if (!this.db) return [];

        return new Promise((resolve) => {
            try {
                const tx = this.db.transaction([LAYOUT_STORE_NAME], 'readonly');
                const store = tx.objectStore(LAYOUT_STORE_NAME);
                const request = store.getAll();

                request.onsuccess = (event) => {
                    const items = event.target.result || [];
                    // 按区域和位置排序
                    items.sort((a, b) => {
                        if (a.area !== b.area) return a.area.localeCompare(b.area);
                        return a.position - b.position;
                    });
                    resolve(items);
                };

                request.onerror = () => resolve([]);
            } catch (e) {
                console.error('LayoutManager: 获取布局失败:', e);
                resolve([]);
            }
        });
    }

    /**
     * 清除所有布局
     */
    async clearLayout() {
        if (!this.db) return false;

        return new Promise((resolve) => {
            try {
                const tx = this.db.transaction([LAYOUT_STORE_NAME], 'readwrite');
                const store = tx.objectStore(LAYOUT_STORE_NAME);
                store.clear();

                tx.oncomplete = () => resolve(true);
                tx.onerror = () => resolve(false);
            } catch (e) {
                resolve(false);
            }
        });
    }

    /**
     * 检查是否有已保存的布局
     */
    async hasLayout() {
        const items = await this.getLayout();
        return items.length > 0;
    }
}

// 导出单例
export const layoutManager = new LayoutManager();
export default layoutManager;