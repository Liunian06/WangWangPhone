
/**
 * 桌面主屏幕逻辑 - 4x6网格吸附系统
 * 解决问题：
 * 1. 不再显示"长按拖拽图标以调整位置"提示
 * 2. 小组件支持拖拽换位
 * 3. 4x6网格吸附，图标悬停在指定位置
 * 4. 拖拽时浮层在最上层（z-index: 10000）
 */

import { getLocation } from './location.js';
import { getWeather } from './weather.js';
import { licenseManager } from './license.js';
import { layoutManager } from './layout.js';
import { ChatApp } from './chat.js';

let chatAppInstance = null;

const DEFAULT_APPS = [
    { id: 'phone', name: '电话', icon: '📞', useImage: false },
    { id: 'chat', name: '聊天', icon: '💬', useImage: false },
    { id: 'safari', name: 'Safari', icon: '🧭', useImage: false },
    { id: 'music', name: '音乐', icon: '🎵', useImage: false },
    { id: 'camera', name: '相机', icon: '📷', useImage: false },
    { id: 'calendar', name: '日历', icon: '📅', useImage: false },
    { id: 'settings', name: '设置', icon: 'settings', useImage: true },
    { id: 'wangwang', name: '汪汪', icon: '🐶', useImage: false }
];

const GRID_COLS = 4;
const GRID_ROWS = 6;
const TOTAL_CELLS = GRID_COLS * GRID_ROWS;

let gridPositions = {};   // { cellIndex: app }
let dockApps = [];
const MAX_DOCK_APPS = 4;
let isEditMode = false;
let widgetOrder = ['clock', 'weather'];
let longPressTimer = null;
const LONG_PRESS_DURATION = 500;

// 拖拽状态
let isDragging = false;
let draggedElement = null;
let draggedCellIndex = -1;
let draggedDockIndex = -1;
let dragStartX = 0;
let dragStartY = 0;
let dragSource = 'grid';
let dragOverlayEl = null;
let gridHighlightEl = null;

// ====== 工具函数 ======
function findNextEmptyCell(start) {
    for (let i = (start || 0); i < TOTAL_CELLS; i++) {
        if (!gridPositions[i]) return i;
    }
    return -1;
}

function getGridMetrics() {
    const grid = document.getElementById('app-grid');
    const r = grid.getBoundingClientRect();
    return { gridRect: r, cellWidth: r.width / GRID_COLS, cellHeight: r.height / GRID_ROWS };
}

function getCellPosition(cellIndex) {
    const { cellWidth, cellHeight } = getGridMetrics();
    const col = cellIndex % GRID_COLS;
    const row = Math.floor(cellIndex / GRID_COLS);
    return { left: col * cellWidth + (cellWidth - 60) / 2, top: row * cellHeight + 5 };
}

function getCellIndexFromPoint(clientX, clientY) {
    const { gridRect: r, cellWidth: cw, cellHeight: ch } = getGridMetrics();
    const rx = clientX - r.left, ry = clientY - r.top;
    if (rx < 0 || ry < 0 || rx > r.width || ry > r.height) return -1;
    const col = Math.max(0, Math.min(GRID_COLS - 1, Math.floor(rx / cw)));
    const row = Math.max(0, Math.min(GRID_ROWS - 1, Math.floor(ry / ch)));
    return row * GRID_COLS + col;
}

function isPointerInDock(cx, cy) {
    const dock = document.getElementById('dock-bar');
    const r = dock.getBoundingClientRect();
    return cy >= r.top && cy <= r.bottom && cx >= r.left && cx <= r.right;
}

function isPointerInGrid(cx, cy) {
    const { gridRect: r } = getGridMetrics();
    return cy >= r.top && cy <= r.bottom && cx >= r.left && cx <= r.right;
}

// ====== 初始化 ======
export async function initHomeScreen() {
    await licenseManager.initialize();
    await layoutManager.initialize();

    const savedLayout = await layoutManager.getLayout();
    if (savedLayout.length > 0) {
        gridPositions = {};
        const gridItems = savedLayout.filter(i => i.area === 'grid');
        for (const li of gridItems) {
            const app = DEFAULT_APPS.find(a => a.id === li.appId);
            if (app && li.position >= 0 && li.position < TOTAL_CELLS) gridPositions[li.position] = app;
        }
        dockApps = [];
        const dockItems = savedLayout.filter(i => i.area === 'dock').sort((a, b) => a.position - b.position);
        for (const li of dockItems) {
            const app = DEFAULT_APPS.find(a => a.id === li.appId);
            if (app) dockApps.push(app);
        }
        const widgetItems = savedLayout.filter(i => i.area === 'widget').sort((a, b) => a.position - b.position);
        if (widgetItems.length > 0) widgetOrder = widgetItems.map(i => i.appId);
        const allSavedIds = [...gridItems, ...dockItems].map(i => i.appId);
        for (const app of DEFAULT_APPS) {
            if (!allSavedIds.includes(app.id)) {
                const c = findNextEmptyCell(0);
                if (c >= 0) gridPositions[c] = app;
            }
        }
    } else {
        DEFAULT_APPS.forEach((app, i) => { if (i < TOTAL_CELLS) gridPositions[i] = app; });
    }

    renderWidgets();
    renderAppGrid();
    renderDock();
    refreshActivationUI();

    document.getElementById('edit-done-btn').addEventListener('click', () => exitEditMode());
    document.getElementById('home-screen').addEventListener('click', (e) => {
        if (isEditMode && (e.target.id === 'home-screen' || e.target.id === 'app-grid')) exitEditMode();
    });
}

// ====== 小组件 ======
function renderWidgets() {
    const area = document.querySelector('.widgets-area');
    area.innerHTML = '';
    widgetOrder.forEach((wid, index) => {
        const el = wid === 'clock' ? createClockWidget() : createWeatherWidget();
        el.dataset.widgetId = wid;
        el.dataset.widgetIndex = index;
        if (isEditMode) el.classList.add('edit-mode');
        setupWidgetDrag(el, index);
        area.appendChild(el);
    });
    updateTime();
}

function createClockWidget() {
    const w = document.createElement('div');
    w.className = 'widget clock-widget';
    w.innerHTML = '<div class="widget-date" id="widget-date">--</div><div class="widget-time" id="widget-time">--:--</div><div class="widget-city">北京</div>';
    return w;
}

function createWeatherWidget() {
    const w = document.createElement('div');
    w.className = 'widget weather-widget';
    w.innerHTML = '<div><div style="font-weight:bold">北京</div><div class="widget-temp">24°</div></div><div style="display:flex;flex-direction:column;align-items:flex-end"><div style="display:flex;align-items:center;gap:5px"><div class="widget-icon">☀️</div><div class="widget-status">晴朗</div></div><div class="widget-range">最高 28° 最低 18°</div></div>';
    return w;
}

function setupWidgetDrag(element, index) {
    let wDrag = false, wTimer = null, wStartX = 0, wStartY = 0, wOverlay = null;

    function mkOverlay() {
        rmOverlay();
        wOverlay = element.cloneNode(true);
        Object.assign(wOverlay.style, {
            position: 'fixed', zIndex: '10000', pointerEvents: 'none',
            opacity: '0.85', transform: 'scale(1.05)',
            width: element.offsetWidth + 'px', height: element.offsetHeight + 'px'
        });
        wOverlay.classList.remove('edit-mode', 'widget-dragging');
        const r = element.getBoundingClientRect();
        wOverlay.style.left = r.left + 'px';
        wOverlay.style.top = r.top + 'px';
        document.body.appendChild(wOverlay);
    }
    function rmOverlay() { if (wOverlay && wOverlay.parentNode) wOverlay.parentNode.removeChild(wOverlay); wOverlay = null; }

    function onDown(e) {
        wStartX = e.clientX; wStartY = e.clientY;
        clearTimeout(wTimer);
        wTimer = setTimeout(() => {
            if (!isEditMode) enterEditMode();
            wDrag = true;
            element.classList.add('widget-dragging');
            mkOverlay();
            if (navigator.vibrate) navigator.vibrate(50);
        }, isEditMode ? 150 : LONG_PRESS_DURATION);
    }
    function onMove(e) {
        if (!wDrag && (Math.abs(e.clientX - wStartX) > 8 || Math.abs(e.clientY - wStartY) > 8)) clearTimeout(wTimer);
        if (wDrag && wOverlay) {
            wOverlay.style.left = (e.clientX - wOverlay.offsetWidth / 2) + 'px';
            wOverlay.style.top = (e.clientY - wOverlay.offsetHeight / 2) + 'px';
            document.querySelectorAll('.widgets-area .widget').forEach((w, i) => {
                if (w !== element) {
                    const r = w.getBoundingClientRect();
                    const cx = r.left + r.width / 2;
                    w.classList.toggle('drop-target', Math.abs(e.clientX - cx) < r.width / 3);
                }
            });
        }
    }
    function onUp() {
        clearTimeout(wTimer);
        if (wDrag) {
            let swap = -1;
            document.querySelectorAll('.widgets-area .widget').forEach((w, i) => {
                if (w.classList.contains('drop-target')) swap = i;
                w.classList.remove('drop-target');
            });
            if (swap >= 0 && swap !== index) {
                const t = widgetOrder[index]; widgetOrder[index] = widgetOrder[swap]; widgetOrder[swap] = t;
            }
            element.classList.remove('widget-dragging');
            rmOverlay();
            wDrag = false;
            renderWidgets(); saveLayout(); initWeatherWidget();
        }
    }
    function onCancel() {
        clearTimeout(wTimer);
        if (wDrag) { element.classList.remove('widget-dragging'); rmOverlay(); wDrag = false; renderWidgets(); }
    }

    // 绑定document级别move/up以防止鼠标移出元素后丢失事件
    element.addEventListener('mousedown', (e) => {
        onDown(e);
        function docMove(ev) { onMove(ev); }
        function docUp(ev) { onUp(); document.removeEventListener('mousemove', docMove); document.removeEventListener('mouseup', docUp); }
        document.addEventListener('mousemove', docMove);
        document.addEventListener('mouseup', docUp);
    });
    element.addEventListener('touchstart', (e) => {
        const t = e.touches[0]; onDown({ clientX: t.clientX, clientY: t.clientY });
    }, { passive: false });
    element.addEventListener('touchmove', (e) => {
        e.preventDefault(); const t = e.touches[0]; onMove({ clientX: t.clientX, clientY: t.clientY });
    }, { passive: false });
    element.addEventListener('touchend', () => onUp());
    element.addEventListener('touchcancel', () => onCancel());
}

// ====== 应用网格渲染（4x6 绝对定位） ======
function renderAppGrid() {
    const grid = document.getElementById('app-grid');
    grid.innerHTML = '';
    removeGridHighlight();

    for (let cellIndex = 0; cellIndex < TOTAL_CELLS; cellIndex++) {
        const app = gridPositions[cellIndex];
        if (!app) continue;

        const wrapper = document.createElement('div');
        wrapper.className = 'app-icon-wrapper' + (isEditMode ? ' edit-mode' : '');
        wrapper.dataset.cellIndex = cellIndex;
        wrapper.dataset.appId = app.id;

        const pos = getCellPosition(cellIndex);
        wrapper.style.left = pos.left + 'px';
        wrapper.style.top = pos.top + 'px';

        const iconDiv = document.createElement('div');
        iconDiv.className = 'app-icon';
        if (app.useImage) {
            const picture = document.createElement('picture');
            const source = document.createElement('source');
            source.srcset = 'Setting_Dark.png'; source.media = '(prefers-color-scheme: dark)';
            const img = document.createElement('img');
            img.src = 'Setting_Light.png'; img.alt = app.name; img.className = 'settings-icon-img';
            picture.appendChild(source); picture.appendChild(img); iconDiv.appendChild(picture);
        } else {
            iconDiv.textContent = app.icon;
        }

        const nameDiv = document.createElement('div');
        nameDiv.className = 'app-name';
        nameDiv.textContent = app.name;

        wrapper.appendChild(iconDiv);
        wrapper.appendChild(nameDiv);
        setupGridIconDrag(wrapper, cellIndex);
        grid.appendChild(wrapper);
    }
}

function renderDock() {
    const dock = document.getElementById('dock-bar');
    dock.innerHTML = '';
    dockApps.forEach((app, index) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'app-icon-wrapper' + (isEditMode ? ' edit-mode' : '');
        wrapper.dataset.dockIndex = index;
        wrapper.dataset.appId = app.id;
        wrapper.style.position = 'relative';
        wrapper.style.cursor = 'pointer';

        const iconDiv = document.createElement('div');
        iconDiv.className = 'app-icon';
        if (app.useImage) {
            const picture = document.createElement('picture');
            const source = document.createElement('source');
            source.srcset = 'Setting_Dark.png'; source.media = '(prefers-color-scheme: dark)';
            const img = document.createElement('img');
            img.src = 'Setting_Light.png'; img.alt = app.name; img.className = 'settings-icon-img-dock';
            picture.appendChild(source); picture.appendChild(img); iconDiv.appendChild(picture);
        } else {
            iconDiv.textContent = app.icon;
        }
        wrapper.appendChild(iconDiv);
        setupDockIconDrag(wrapper, index);
        dock.appendChild(wrapper);
    });

    if (isEditMode && dockApps.length < MAX_DOCK_APPS) {
        for (let i = dockApps.length; i < MAX_DOCK_APPS; i++) {
            const ph = document.createElement('div');
            ph.className = 'app-icon';
            Object.assign(ph.style, { opacity: '0.3', border: '2px dashed rgba(255,255,255,0.4)', borderRadius: '15px', width: '55px', height: '55px', display: 'flex', justifyContent: 'center', alignItems: 'center', fontSize: '20px', color: 'rgba(255,255,255,0.5)' });
            ph.textContent = '+';
            dock.appendChild(ph);
        }
    }
}

// ====== 网格图标拖拽 ======
function setupGridIconDrag(element, cellIndex) {
    element.addEventListener('mousedown', (e) => {
        onGridDown(e, element, cellIndex);
        function docMove(ev) { onGridMove(ev); }
        function docUp(ev) { onGridUp(ev); document.removeEventListener('mousemove', docMove); document.removeEventListener('mouseup', docUp); }
        document.addEventListener('mousemove', docMove);
        document.addEventListener('mouseup', docUp);
    });
    element.addEventListener('touchstart', (e) => {
        const t = e.touches[0];
        onGridDown({ clientX: t.clientX, clientY: t.clientY }, element, cellIndex);
    }, { passive: false });
    element.addEventListener('touchmove', (e) => {
        e.preventDefault(); const t = e.touches[0]; onGridMove({ clientX: t.clientX, clientY: t.clientY });
    }, { passive: false });
    element.addEventListener('touchend', (e) => {
        const t = e.changedTouches && e.changedTouches[0];
        onGridUp(t ? { clientX: t.clientX, clientY: t.clientY } : { clientX: dragStartX, clientY: dragStartY });
    });
    element.addEventListener('touchcancel', () => finishDrag());
}

function onGridDown(e, element, cellIndex) {
    dragStartX = e.clientX; dragStartY = e.clientY;
    draggedCellIndex = cellIndex; dragSource = 'grid';
    clearTimeout(longPressTimer);
    longPressTimer = setTimeout(() => {
        if (!isEditMode) enterEditMode();
        startDrag(element, cellIndex, 'grid');
    }, isEditMode ? 150 : LONG_PRESS_DURATION);
}

function onGridMove(e) {
    if (!isDragging && longPressTimer && (Math.abs(e.clientX - dragStartX) > 8 || Math.abs(e.clientY - dragStartY) > 8)) {
        clearTimeout(longPressTimer); longPressTimer = null;
    }
    if (isDragging && dragOverlayEl) {
        dragOverlayEl.style.left = (e.clientX - 30) + 'px';
        dragOverlayEl.style.top = (e.clientY - 30) + 'px';

        const dock = document.getElementById('dock-bar');
        const inDock = isPointerInDock(e.clientX, e.clientY);
        const inGrid = isPointerInGrid(e.clientX, e.clientY);

        if (inDock && dragSource === 'grid') {
            dock.style.outline = '2px solid rgba(0,122,255,0.6)';
            dock.style.outlineOffset = '-2px';
            removeGridHighlight();
        } else {
            dock.style.outline = ''; dock.style.outlineOffset = '';
            if (inGrid && dragSource === 'grid') {
                const targetCell = getCellIndexFromPoint(e.clientX, e.clientY);
                if (targetCell >= 0) showGridHighlight(targetCell);
            }
        }
    }
}

function onGridUp(e) {
    clearTimeout(longPressTimer); longPressTimer = null;
    if (isDragging && dragSource === 'grid') {
        const cx = e.clientX, cy = e.clientY;
        const inDock = isPointerInDock(cx, cy);
        const inGrid = isPointerInGrid(cx, cy);

        if (inDock && dockApps.length < MAX_DOCK_APPS) {
            // 移到Dock
            const app = gridPositions[draggedCellIndex];
            if (app) {
                delete gridPositions[draggedCellIndex];
                dockApps.push(app);
            }
        } else if (inGrid) {
            // 吸附到目标网格位置
            const targetCell = getCellIndexFromPoint(cx, cy);
            if (targetCell >= 0 && targetCell !== draggedCellIndex) {
                const app = gridPositions[draggedCellIndex];
                const targetApp = gridPositions[targetCell];
                delete gridPositions[draggedCellIndex];
                if (targetApp) {
                    // 交换
                    gridPositions[draggedCellIndex] = targetApp;
                }
                gridPositions[targetCell] = app;
            }
        }
        finishDrag();
    } else if (!isDragging && !isEditMode) {
        // 普通点击
        const app = gridPositions[draggedCellIndex];
        if (app && app.id === 'settings') showSettings();
        else if (app && app.id === 'chat') openChatApp();
    }
}

// ====== Dock图标拖拽 ======
function setupDockIconDrag(element, dockIndex) {
    element.addEventListener('mousedown', (e) => {
        onDockDown(e, element, dockIndex);
        function docMove(ev) { onDockMove(ev); }
        function docUp(ev) { onDockUp(ev); document.removeEventListener('mousemove', docMove); document.removeEventListener('mouseup', docUp); }
        document.addEventListener('mousemove', docMove);
        document.addEventListener('mouseup', docUp);
    });
    element.addEventListener('touchstart', (e) => {
        const t = e.touches[0]; onDockDown({ clientX: t.clientX, clientY: t.clientY }, element, dockIndex);
    }, { passive: false });
    element.addEventListener('touchmove', (e) => {
        e.preventDefault(); const t = e.touches[0]; onDockMove({ clientX: t.clientX, clientY: t.clientY });
    }, { passive: false });
    element.addEventListener('touchend', (e) => {
        const t = e.changedTouches && e.changedTouches[0];
        onDockUp(t ? { clientX: t.clientX, clientY: t.clientY } : { clientX: dragStartX, clientY: dragStartY });
    });
    element.addEventListener('touchcancel', () => finishDrag());
}

function onDockDown(e, element, dockIndex) {
    dragStartX = e.clientX; dragStartY = e.clientY;
    draggedDockIndex = dockIndex; dragSource = 'dock';
    clearTimeout(longPressTimer);
    longPressTimer = setTimeout(() => {
        if (!isEditMode) enterEditMode();
        startDrag(element, dockIndex, 'dock');
    }, isEditMode ? 150 : LONG_PRESS_DURATION);
}

function onDockMove(e) {
    if (!isDragging && longPressTimer && (Math.abs(e.clientX - dragStartX) > 8 || Math.abs(e.clientY - dragStartY) > 8)) {
        clearTimeout(longPressTimer); longPressTimer = null;
    }
    if (isDragging && dragOverlayEl && dragSource === 'dock') {
        dragOverlayEl.style.left = (e.clientX - 30) + 'px';
        dragOverlayEl.style.top = (e.clientY - 30) + 'px';

        const inGrid = isPointerInGrid(e.clientX, e.clientY);
        if (inGrid) {
            const targetCell = getCellIndexFromPoint(e.clientX, e.clientY);
            if (targetCell >= 0) showGridHighlight(targetCell);
        } else {
            removeGridHighlight();
        }
    }
}

function onDockUp(e) {
    clearTimeout(longPressTimer); longPressTimer = null;
    if (isDragging && dragSource === 'dock') {
        const cx = e.clientX, cy = e.clientY;
        const inGrid = isPointerInGrid(cx, cy);

        if (inGrid) {
            // 从Dock移到网格
            const targetCell = getCellIndexFromPoint(cx, cy);
            if (targetCell >= 0 && draggedDockIndex >= 0 && draggedDockIndex < dockApps.length) {
                const app = dockApps[draggedDockIndex];
                dockApps.splice(draggedDockIndex, 1);
                if (gridPositions[targetCell]) {
                    // 如果目标格子有应用，放到空位
                    const displaced = gridPositions[targetCell];
                    gridPositions[targetCell] = app;
                    const emptyCell = findNextEmptyCell(0);
                    if (emptyCell >= 0) gridPositions[emptyCell] = displaced;
                    else dockApps.push(displaced); // 网格满了放回Dock
                } else {
                    gridPositions[targetCell] = app;
                }
            }
        }
        finishDrag();
    } else if (!isDragging && !isEditMode) {
        const app = dockApps[draggedDockIndex];
        if (app && app.id === 'settings') showSettings();
        else if (app && app.id === 'chat') openChatApp();
    }
}

// ====== 通用拖拽 ======
function startDrag(element, index, source) {
    isDragging = true;
    draggedElement = element;
    dragSource = source;
    if (source === 'grid') draggedCellIndex = index;
    else draggedDockIndex = index;

    element.classList.add('dragging');
    createDragOverlay(element, source);
    if (navigator.vibrate) navigator.vibrate(50);
}

function createDragOverlay(element, source) {
    removeDragOverlay();
    dragOverlayEl = element.cloneNode(true);
    dragOverlayEl.className = 'drag-overlay';
    Object.assign(dragOverlayEl.style, {
        position: 'fixed', zIndex: '10000', pointerEvents: 'none',
        opacity: '0.85', transform: 'scale(1.15)'
    });
    const r = element.getBoundingClientRect();
    dragOverlayEl.style.left = r.left + 'px';
    dragOverlayEl.style.top = r.top + 'px';
    dragOverlayEl.style.width = r.width + 'px';
    document.body.appendChild(dragOverlayEl);
}

function removeDragOverlay() {
    if (dragOverlayEl && dragOverlayEl.parentNode) dragOverlayEl.parentNode.removeChild(dragOverlayEl);
    dragOverlayEl = null;
}

function showGridHighlight(cellIndex) {
    const grid = document.getElementById('app-grid');
    if (!gridHighlightEl) {
        gridHighlightEl = document.createElement('div');
        gridHighlightEl.className = 'grid-cell-highlight';
        grid.appendChild(gridHighlightEl);
    }
    const { cellWidth, cellHeight } = getGridMetrics();
    const col = cellIndex % GRID_COLS;
    const row = Math.floor(cellIndex / GRID_COLS);
    Object.assign(gridHighlightEl.style, {
        left: (col * cellWidth + 5) + 'px',
        top: (row * cellHeight + 2) + 'px',
        width: (cellWidth - 10) + 'px',
        height: (cellHeight - 4) + 'px',
        opacity: '1'
    });
}

function removeGridHighlight() {
    if (gridHighlightEl && gridHighlightEl.parentNode) {
        gridHighlightEl.parentNode.removeChild(gridHighlightEl);
        gridHighlightEl = null;
    }
}

function finishDrag() {
    isDragging = false;
    removeDragOverlay();
    removeGridHighlight();
    if (draggedElement) {
        draggedElement.classList.remove('dragging');
        draggedElement = null;
    }
    const dock = document.getElementById('dock-bar');
    dock.style.outline = ''; dock.style.outlineOffset = '';
    draggedCellIndex = -1; draggedDockIndex = -1; dragSource = 'grid';
    renderAppGrid(); renderDock(); saveLayout();
}

// ====== 编辑模式 ======
function enterEditMode() {
    isEditMode = true;
    document.getElementById('edit-banner').classList.add('active');
    renderWidgets(); renderAppGrid(); renderDock();
}

function exitEditMode() {
    isEditMode = false;
    document.getElementById('edit-banner').classList.remove('active');
    renderWidgets(); renderAppGrid(); renderDock(); saveLayout();
}

// ====== 布局持久化 ======
async function saveLayout() {
    const items = [];
    for (let i = 0; i < TOTAL_CELLS; i++) {
        if (gridPositions[i]) items.push({ appId: gridPositions[i].id, position: i, area: 'grid' });
    }
    dockApps.forEach((app, i) => items.push({ appId: app.id, position: i, area: 'dock' }));
    widgetOrder.forEach((wid, i) => items.push({ appId: wid, position: i, area: 'widget' }));
    await layoutManager.saveLayout(items);
}

// ====== 授权相关 ======
function refreshActivationUI() {
    if (licenseManager.isActivated()) {
        document.querySelectorAll('.status-val').forEach(el => el.textContent = "已查看 >");
        const expiryEl = document.getElementById('expiry-date-web');
        if (expiryEl) { expiryEl.textContent = '有效期至: ' + licenseManager.getExpirationDateString(); expiryEl.style.display = 'block'; }
    }
}

// ====== 时钟 ======
function updateTime() {
    const now = new Date();
    const h = String(now.getHours()).padStart(2, '0');
    const m = String(now.getMinutes()).padStart(2, '0');
    const timeStr = h + ':' + m;
    document.querySelectorAll('.time, .lock-time').forEach(el => el.textContent = timeStr);
    const widgetTime = document.getElementById('widget-time');
    if (widgetTime) widgetTime.textContent = timeStr;
    const widgetDate = document.getElementById('widget-date');
    if (widgetDate) {
        const month = now.getMonth() + 1;
        const date = now.getDate();
        const days = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六'];
        widgetDate.textContent = month + '月' + date + '日 ' + days[now.getDay()];
    }
}
setInterval(updateTime, 1000);

// ====== 天气 ======
async function initWeatherWidget() {
    try {
        const city = await getLocation();
        document.querySelectorAll('.widget-city').forEach(el => el.textContent = city);
        const weatherWidget = document.querySelector('.weather-widget');
        if (weatherWidget) {
            const cityEl = weatherWidget.querySelector('div > div:first-child');
            if (cityEl) cityEl.textContent = city;
        }
        if (city) {
            const weather = await getWeather(city);
            if (weatherWidget) {
                const tempEl = weatherWidget.querySelector('.widget-temp');
                const statusEl = weatherWidget.querySelector('.widget-status');
                const rangeEl = weatherWidget.querySelector('.widget-range');
                if (tempEl) tempEl.textContent = weather.temperature.replace(' ', '');
                if (statusEl) statusEl.textContent = weather.description;
                if (weather.forecast && weather.forecast.length > 0) {
                    rangeEl.textContent = 'Wind: ' + weather.wind;
                }
            }
        }
    } catch (error) {
        console.error('获取环境信息失败:', error);
    }
}
initWeatherWidget();

// ====== 全局函数（供HTML onclick使用） ======
function showSettings() {
    document.getElementById('settings-overlay').style.display = 'flex';
}

function openChatApp() {
    const container = document.getElementById('chat-app-container');
    container.style.display = 'block';
    chatAppInstance = new ChatApp(container, () => {
        container.style.display = 'none';
        chatAppInstance = null;
    });
}

window.unlock = function() {
    document.getElementById('lock-screen').classList.add('unlocked');
};
window.showSettings = showSettings;
window.hideSettings = function() {
    document.getElementById('settings-overlay').style.display = 'none';
};
window.showDisplaySettings = function() {
    document.getElementById('display-settings-overlay').style.display = 'flex';
    updateWallpaperPreviews();
};
window.hideDisplaySettings = function() {
    document.getElementById('display-settings-overlay').style.display = 'none';
};

let pendingWallpaperType = 'lock';
window.selectWallpaper = function(type) {
    pendingWallpaperType = type;
    document.getElementById('wallpaper-input').click();
};
window.handleWallpaperUpload = function(input) {
    const file = input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function(e) {
        localStorage.setItem('wallpaper_' + pendingWallpaperType, e.target.result);
        updateWallpaperPreviews();
        applyWallpapers();
    };
    reader.readAsDataURL(file);
};

function updateWallpaperPreviews() {
    ['lock', 'home'].forEach(type => {
        const data = localStorage.getItem('wallpaper_' + type);
        const preview = document.getElementById(type + '-wp-preview');
        const status = document.getElementById(type + '-wp-status');
        if (data) {
            preview.innerHTML = '<img src="' + data + '" style="width:100%;height:100%;object-fit:cover">';
            status.textContent = '已设置，点击更换';
        }
    });
}

function applyWallpapers() {
    const lockWp = localStorage.getItem('wallpaper_lock');
    const homeWp = localStorage.getItem('wallpaper_home');
    if (lockWp) {
        const ls = document.getElementById('lock-screen');
        ls.style.background = 'url(' + lockWp + ') no-repeat center center';
        ls.style.backgroundSize = 'cover';
    }
    if (homeWp) {
        const pc = document.getElementById('phone-container');
        pc.style.background = 'url(' + homeWp + ') no-repeat center center';
        pc.style.backgroundSize = 'cover';
    }
}
applyWallpapers();

window.showActivation = function() {
    document.getElementById('activation-overlay').style.zIndex = 300;
    document.getElementById('activation-overlay').style.display = 'flex';
    document.getElementById('web-machine-id').textContent = licenseManager.getMachineId();
};
window.copyMachineId = function() {
    const text = document.getElementById('web-machine-id').textContent;
    navigator.clipboard.writeText(text).then(() => alert('机器码已复制'));
};
window.pasteLicenseKey = function() {
    navigator.clipboard.readText().then(text => {
        document.getElementById('license-key-input').value = text;
    });
};
window.hideActivation = function() {
    document.getElementById('activation-overlay').style.display = 'none';
};

window.addEventListener('popstate', function() {
    if (document.getElementById('activation-overlay').style.display === 'flex') {
        window.hideActivation(); history.pushState(null, null, null);
    } else if (document.getElementById('settings-overlay').style.display === 'flex') {
        window.hideSettings(); history.pushState(null, null, null);
    }
});
history.pushState(null, null, null);

window.submitActivation = async function() {
    const license = document.getElementById('license-key-input').value?.trim();
    if (!license) { alert('请输入激活码。'); return; }
    const result = await licenseManager.verifyLicense(license);
    if (result.success) {
        alert('软件激活成功！');
        refreshActivationUI();
        window.hideActivation();
    } else {
        alert('激活失败: ' + result.error);
    }
};

window.openChatApp = openChatApp;