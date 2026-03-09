(() => {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    const signView = document.getElementById('sign');

    if (!sidebar || !mainContent || !signView) {
        return;
    }

    const style = document.createElement('style');
    style.textContent = `
        .batch-upload-panel {
            border: 1px dashed rgba(99, 102, 241, 0.35);
            border-radius: 20px;
            padding: 24px;
            background: rgba(99, 102, 241, 0.04);
        }
        [data-theme="dark"] .batch-upload-panel {
            background: rgba(99, 102, 241, 0.08);
            border-color: rgba(167, 139, 250, 0.35);
        }
        .batch-toolbar {
            display: flex;
            flex-wrap: wrap;
            gap: 14px;
            align-items: center;
            margin-top: 18px;
        }
        .batch-help {
            margin-top: 10px;
            font-size: 14px;
            line-height: 1.7;
            color: var(--text-secondary);
        }
        .batch-meta {
            margin-top: 14px;
            font-size: 14px;
            color: var(--text-secondary);
            word-break: break-all;
        }
        .batch-summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 16px;
            margin-bottom: 20px;
        }
        .batch-summary-item {
            padding: 18px;
            border-radius: 18px;
            border: 1px solid var(--glass-border);
            background: rgba(255, 255, 255, 0.35);
        }
        [data-theme="dark"] .batch-summary-item {
            background: rgba(255, 255, 255, 0.04);
        }
        .batch-summary-label {
            font-size: 12px;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .batch-summary-value {
            margin-top: 8px;
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
        }
        .batch-ghost-btn {
            background: rgba(128, 128, 128, 0.08);
            color: var(--text-main);
            border: 1px solid rgba(128, 128, 128, 0.18);
            box-shadow: none;
        }
        .batch-ghost-btn:hover:not(:disabled) {
            box-shadow: none;
        }
        .batch-table-wrap {
            margin-top: 12px;
        }
        .batch-table {
            table-layout: auto;
        }
        .batch-table td {
            vertical-align: top;
        }
        .batch-license-cell {
            width: 140px;
            white-space: nowrap;
        }
        .batch-license-trigger {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 92px;
            padding: 8px 12px;
            border-radius: 999px;
            background: rgba(99, 102, 241, 0.12);
            border: 1px solid rgba(99, 102, 241, 0.18);
            color: #4f46e5;
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0.2px;
            cursor: default;
        }
        [data-theme="dark"] .batch-license-trigger {
            color: #c4b5fd;
            background: rgba(167, 139, 250, 0.14);
            border-color: rgba(167, 139, 250, 0.22);
        }
        .batch-license-empty {
            color: var(--text-secondary);
            font-size: 13px;
        }
        .batch-status-ok {
            color: var(--success);
            font-weight: 700;
        }
        .batch-status-fail {
            color: var(--error);
            font-weight: 700;
        }
        .batch-status-pending {
            color: var(--warning);
            font-weight: 700;
        }
        .batch-inline-note {
            margin-top: 12px;
            font-size: 13px;
            color: var(--text-secondary);
        }
    `;
    document.head.appendChild(style);

    const signNav = sidebar.querySelector('.nav-item[data-view="sign"]');
    const batchNav = document.createElement('div');
    batchNav.className = 'nav-item';
    batchNav.dataset.view = 'batch';
    batchNav.innerHTML = '<span>📥</span> 批量激活';
    if (signNav) {
        signNav.insertAdjacentElement('afterend', batchNav);
    } else {
        sidebar.appendChild(batchNav);
    }

    const batchView = document.createElement('div');
    batchView.id = 'batch';
    batchView.className = 'view';
    batchView.innerHTML = `
        <h2><span class="floating-icon">📦</span> 批量导入激活</h2>
        <div class="card">
            <div class="batch-upload-panel">
                <div style="font-size: 18px; font-weight: 700; color: var(--text-main);">直接导入 Excel / CSV 表格</div>
                <div class="batch-help">支持 <code>.xlsx</code>、<code>.xls</code>、<code>.csv</code>。默认读取第一个工作表，并按以下列名处理：序号、设备码、小红书ID、QQID、激活时长、授权级别、激活码。</div>
                <div class="batch-toolbar">
                    <input type="file" id="batchFileInput" accept=".xlsx,.xls,.csv" style="display: none;">
                    <button id="batchImportBtn" class="btn">选择表格</button>
                    <button id="batchGenerateBtn" class="btn" disabled>批量生成激活码</button>
                    <button id="batchExportXlsxBtn" class="btn batch-ghost-btn" disabled>导出 Excel</button>
                    <button id="batchExportCsvBtn" class="btn batch-ghost-btn" disabled>导出 CSV</button>
                    <button id="batchResetBtn" class="btn batch-ghost-btn" disabled>清空结果</button>
                </div>
                <div id="batchFileMeta" class="batch-meta">尚未导入表格文件</div>
                <div id="batchError" class="error-msg"></div>
                <div id="batchResult" class="result-box" style="display: none;"></div>
                <div class="batch-inline-note">说明：浏览器安全限制下无法直接覆盖你电脑上的原文件，导出时会生成一个已回填激活码的新表格。</div>
            </div>
        </div>
        <div id="batchWorkspace" class="card" style="display: none;">
            <div class="batch-summary-grid">
                <div class="batch-summary-item">
                    <div class="batch-summary-label">导入行数</div>
                    <div id="batchTotalCount" class="batch-summary-value">0</div>
                </div>
                <div class="batch-summary-item">
                    <div class="batch-summary-label">成功行数</div>
                    <div id="batchSuccessCount" class="batch-summary-value">0</div>
                </div>
                <div class="batch-summary-item">
                    <div class="batch-summary-label">失败行数</div>
                    <div id="batchFailedCount" class="batch-summary-value">0</div>
                </div>
                <div class="batch-summary-item">
                    <div class="batch-summary-label">当前工作表</div>
                    <div id="batchSheetName" class="batch-summary-value" style="font-size: 18px; word-break: break-all;">-</div>
                </div>
            </div>
            <div class="table-container batch-table-wrap">
                <table class="batch-table">
                    <thead>
                        <tr>
                            <th style="width: 90px;">序号</th>
                            <th style="width: 180px;">设备码</th>
                            <th style="width: 120px;">小红书ID</th>
                            <th style="width: 120px;">QQID</th>
                            <th style="width: 120px;">激活时长</th>
                            <th style="width: 120px;">授权级别</th>
                            <th style="width: 140px;">激活码</th>
                            <th style="width: 180px;">处理结果</th>
                        </tr>
                    </thead>
                    <tbody id="batchTableBody"></tbody>
                </table>
            </div>
        </div>
    `;
    signView.insertAdjacentElement('afterend', batchView);

    const batchColumnAliases = {
        serial: ['序号', '编号', 'id'],
        machineId: ['设备码', '机器码', '设备编号', 'machineid', 'machine id', 'mid'],
        xhsId: ['小红书id', '小红书 id', 'xhsid', 'xhs id', 'xhs'],
        qqId: ['qqid', 'qq id', 'qq'],
        days: ['激活时长', '有效时长', '时长', '天数', 'days'],
        level: ['授权级别', '授权等级', '授权类型', '激活级别', 'license type', 'type'],
        license: ['激活码', '授权码', 'license', 'license key', 'licensekey']
    };

    const requiredBatchFields = ['serial', 'machineId', 'xhsId', 'qqId', 'days', 'level'];

    const state = {
        workbook: null,
        sheetName: '',
        fileName: '',
        headerRowIndex: 0,
        licenseHeaderIndex: -1,
        sheetRows: [],
        rows: [],
        hasProcessed: false
    };

    const fileInput = document.getElementById('batchFileInput');
    const importBtn = document.getElementById('batchImportBtn');
    const generateBtn = document.getElementById('batchGenerateBtn');
    const exportXlsxBtn = document.getElementById('batchExportXlsxBtn');
    const exportCsvBtn = document.getElementById('batchExportCsvBtn');
    const resetBtn = document.getElementById('batchResetBtn');
    const errorEl = document.getElementById('batchError');
    const resultEl = document.getElementById('batchResult');
    const workspaceEl = document.getElementById('batchWorkspace');
    const metaEl = document.getElementById('batchFileMeta');
    const tableBodyEl = document.getElementById('batchTableBody');

    function normalizeHeaderName(value) {
        return String(value || '')
            .trim()
            .toLowerCase()
            .replace(/[\s_\-]/g, '')
            .replace(/[()（）]/g, '');
    }

    function getAliasSet(field) {
        return new Set(batchColumnAliases[field].map(normalizeHeaderName));
    }

    function resolveHeaderMap(headerRow) {
        const aliasSets = Object.fromEntries(Object.keys(batchColumnAliases).map(key => [key, getAliasSet(key)]));
        const map = {};
        let maxIndex = -1;

        headerRow.forEach((value, index) => {
            const normalized = normalizeHeaderName(value);
            if (!normalized) {
                return;
            }
            Object.entries(aliasSets).forEach(([field, aliasSet]) => {
                if (map[field] == null && aliasSet.has(normalized)) {
                    map[field] = index;
                    maxIndex = Math.max(maxIndex, index);
                }
            });
        });

        const missing = requiredBatchFields.filter(field => map[field] == null);
        return { map, missing, maxIndex };
    }

    function findHeaderRow(rows) {
        let bestMatch = null;
        const scanCount = Math.min(rows.length, 10);
        for (let index = 0; index < scanCount; index += 1) {
            const current = resolveHeaderMap(rows[index] || []);
            if (!bestMatch || current.missing.length < bestMatch.missing.length) {
                bestMatch = { ...current, index };
            }
            if (current.missing.length === 0) {
                return { ...current, index };
            }
        }
        return bestMatch;
    }

    function cellToText(value) {
        return value == null ? '' : String(value).trim();
    }

    function isEmptyRow(row) {
        return !row || row.every(cell => cellToText(cell) === '');
    }

    function getCell(row, index) {
        if (!Array.isArray(row) || index == null || index < 0) {
            return '';
        }
        return cellToText(row[index]);
    }

    function ensureLicenseColumn() {
        const headerRow = state.sheetRows[state.headerRowIndex] || [];
        if (state.licenseHeaderIndex === -1) {
            state.licenseHeaderIndex = headerRow.length;
            headerRow.push('激活码');
            state.sheetRows[state.headerRowIndex] = headerRow;
        } else if (!headerRow[state.licenseHeaderIndex]) {
            headerRow[state.licenseHeaderIndex] = '激活码';
        }
    }

    function setError(message) {
        errorEl.style.display = message ? 'block' : 'none';
        errorEl.textContent = message || '';
    }

    function setResult(message) {
        resultEl.style.display = message ? 'block' : 'none';
        resultEl.textContent = message || '';
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
    function renderLicenseCell(license) {
        if (!license) {
            return '<span class="batch-license-empty">-</span>';
        }

        const safeLicense = escapeHtml(license);
        return '<span class="batch-license-trigger" title="' + safeLicense + '">悬浮查看</span>';
    }

    function syncButtons() {
        const hasRows = state.rows.length > 0;
        generateBtn.disabled = !hasRows;
        resetBtn.disabled = !hasRows;
        exportXlsxBtn.disabled = !hasRows || !state.hasProcessed;
        exportCsvBtn.disabled = !hasRows || !state.hasProcessed;
    }

    function updateSummary(successCount = 0, failedCount = 0) {
        document.getElementById('batchTotalCount').textContent = String(state.rows.length);
        document.getElementById('batchSuccessCount').textContent = String(successCount);
        document.getElementById('batchFailedCount').textContent = String(failedCount);
        document.getElementById('batchSheetName').textContent = state.sheetName || '-';
    }

    function renderRows() {
        tableBodyEl.innerHTML = '';
        state.rows.forEach(row => {
            const statusClass = row.status === '成功'
                ? 'batch-status-ok'
                : row.status === '失败'
                    ? 'batch-status-fail'
                    : 'batch-status-pending';
            const statusText = row.status === '失败' && row.error ? `${row.status}：${row.error}` : row.status;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(row.serial)}</td>
                <td style="font-family: monospace;">${escapeHtml(row.machineId)}</td>
                <td>${escapeHtml(row.xhsId || '-')}</td>
                <td>${escapeHtml(row.qqId || '-')}</td>
                <td>${escapeHtml(row.days)}</td>
                <td>${escapeHtml(row.level)}</td>
                <td class="batch-license-cell">${renderLicenseCell(row.license)}</td>
                <td class="${statusClass}">${escapeHtml(statusText)}</td>
            `;
            tableBodyEl.appendChild(tr);
        });
        workspaceEl.style.display = state.rows.length > 0 ? 'block' : 'none';
    }

    function resetState() {
        state.workbook = null;
        state.sheetName = '';
        state.fileName = '';
        state.headerRowIndex = 0;
        state.licenseHeaderIndex = -1;
        state.sheetRows = [];
        state.rows = [];
        state.hasProcessed = false;
        fileInput.value = '';
        metaEl.textContent = '尚未导入表格文件';
        setError('');
        setResult('');
        updateSummary(0, 0);
        renderRows();
        syncButtons();
    }

    function syncSheetWithRows() {
        ensureLicenseColumn();
        state.rows.forEach(row => {
            const sheetRow = state.sheetRows[row.sheetRowIndex] || [];
            while (sheetRow.length <= state.licenseHeaderIndex) {
                sheetRow.push('');
            }
            sheetRow[state.licenseHeaderIndex] = row.license || '';
            state.sheetRows[row.sheetRowIndex] = sheetRow;
        });
    }

    function exportWorkbook(bookType) {
        if (!state.rows.length || !state.hasProcessed) {
            return;
        }

        syncSheetWithRows();
        const safeBaseName = (state.fileName || '批量激活').replace(/\.[^.]+$/, '');
        if (bookType === 'xlsx') {
            state.workbook.Sheets[state.sheetName] = XLSX.utils.aoa_to_sheet(state.sheetRows);
            XLSX.writeFile(state.workbook, `${safeBaseName}-已回填激活码.xlsx`, { compression: true });
            return;
        }

        const csvWorkbook = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(csvWorkbook, XLSX.utils.aoa_to_sheet(state.sheetRows), state.sheetName || 'Sheet1');
        XLSX.writeFile(csvWorkbook, `${safeBaseName}-已回填激活码.csv`, { bookType: 'csv' });
    }

    async function handleFileImport(file) {
        if (!file) {
            return;
        }

        if (typeof XLSX === 'undefined') {
            setError('xlsx 解析脚本未加载，无法导入表格');
            return;
        }

        setError('');
        setResult('');
        metaEl.textContent = '正在读取表格，请稍候...';

        try {
            const buffer = await file.arrayBuffer();
            const workbook = XLSX.read(buffer, { type: 'array', raw: false, cellDates: false });
            if (!workbook.SheetNames || workbook.SheetNames.length === 0) {
                throw new Error('表格中没有可读取的工作表');
            }

            const sheetName = workbook.SheetNames[0];
            const sheet = workbook.Sheets[sheetName];
            const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false, blankrows: false });
            if (!rows.length) {
                throw new Error('表格内容为空');
            }

            const headerInfo = findHeaderRow(rows);
            if (!headerInfo || headerInfo.missing.length > 0) {
                const missingText = (headerInfo && headerInfo.missing.length > 0)
                    ? headerInfo.missing.map(field => batchColumnAliases[field][0]).join('、')
                    : '序号、设备码、小红书ID、QQID、激活时长、授权级别';
                throw new Error(`表头不完整，缺少：${missingText}`);
            }

            state.workbook = workbook;
            state.sheetName = sheetName;
            state.fileName = file.name;
            state.headerRowIndex = headerInfo.index;
            state.licenseHeaderIndex = headerInfo.map.license == null ? -1 : headerInfo.map.license;
            state.sheetRows = rows.map(row => Array.isArray(row) ? row.slice() : []);
            state.hasProcessed = false;

            ensureLicenseColumn();

            const parsedRows = [];
            for (let rowIndex = headerInfo.index + 1; rowIndex < state.sheetRows.length; rowIndex += 1) {
                const row = state.sheetRows[rowIndex];
                if (isEmptyRow(row)) {
                    continue;
                }

                while (row.length <= state.licenseHeaderIndex) {
                    row.push('');
                }

                parsedRows.push({
                    sheetRowIndex: rowIndex,
                    rowNumber: rowIndex + 1,
                    serial: getCell(row, headerInfo.map.serial) || String(parsedRows.length + 1),
                    machineId: getCell(row, headerInfo.map.machineId),
                    xhsId: getCell(row, headerInfo.map.xhsId),
                    qqId: getCell(row, headerInfo.map.qqId),
                    days: getCell(row, headerInfo.map.days),
                    level: getCell(row, headerInfo.map.level),
                    license: getCell(row, state.licenseHeaderIndex),
                    status: '待处理',
                    error: ''
                });
            }

            if (!parsedRows.length) {
                throw new Error('表格中没有可处理的数据行');
            }

            state.rows = parsedRows;
            metaEl.textContent = `已导入文件：${file.name}，工作表：${sheetName}，待处理 ${parsedRows.length} 行`; 
            updateSummary(0, 0);
            renderRows();
            syncButtons();
        } catch (error) {
            resetState();
            setError(error.message || '导入表格失败');
        }
    }

    async function handleBatchGenerate() {
        if (!state.rows.length) {
            setError('请先导入表格');
            return;
        }

        setError('');
        setResult('');
        generateBtn.disabled = true;
        generateBtn.textContent = '批量生成中...';

        try {
            const payloadRows = state.rows.map(row => ({
                serial: row.serial,
                rowNumber: row.rowNumber,
                mid: row.machineId,
                xhs: row.xhsId,
                qq: row.qqId,
                days: row.days,
                type: row.level
            }));

            const response = await fetch('/api/batch-sign', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rows: payloadRows })
            });
            const data = await response.json();
            if (!response.ok || data.success === false) {
                throw new Error(data.error || '批量签发失败');
            }

            state.rows = state.rows.map((row, index) => {
                const result = data.rows[index] || {};
                return {
                    ...row,
                    license: result.success ? (result.license || '') : (row.license || ''),
                    status: result.success ? '成功' : '失败',
                    error: result.error || ''
                };
            });
            state.hasProcessed = true;
            syncSheetWithRows();

            const failedRows = state.rows.filter(row => row.status === '失败');
            const failedPreview = failedRows.slice(0, 10).map(row => `第${row.rowNumber}行：${row.error}`).join('；');
            const resultText = failedRows.length > 0
                ? `批量处理完成：共 ${data.summary.total} 行，成功 ${data.summary.success} 行，失败 ${data.summary.failed} 行。失败明细：${failedPreview}${failedRows.length > 10 ? '；其余失败行请在表格预览中查看。' : ''}`
                : `批量处理完成：共 ${data.summary.total} 行，全部签发成功。现在可以直接导出已回填激活码的表格。`;

            setResult(resultText);
            updateSummary(data.summary.success, data.summary.failed);
            renderRows();
            syncButtons();
        } catch (error) {
            setError(error.message || '批量签发失败');
        } finally {
            generateBtn.disabled = false;
            generateBtn.textContent = '批量生成激活码';
        }
    }

    importBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', event => handleFileImport(event.target.files[0]));
    generateBtn.addEventListener('click', handleBatchGenerate);
    exportXlsxBtn.addEventListener('click', () => exportWorkbook('xlsx'));
    exportCsvBtn.addEventListener('click', () => exportWorkbook('csv'));
    resetBtn.addEventListener('click', resetState);

    syncButtons();
})();

