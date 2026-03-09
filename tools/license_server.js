const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const sqlite3 = require('better-sqlite3');

const dbPath = path.join(__dirname, 'server_sign_logs.db');
const guiPath = path.join(__dirname, 'license_gui.html');
const xlsxBrowserDistPath = path.join(__dirname, '..', 'node_modules', 'xlsx', 'dist', 'xlsx.full.min.js');
const db = new sqlite3(dbPath);

db.prepare(`CREATE TABLE IF NOT EXISTS sign_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        machine_id TEXT NOT NULL,
        license_key TEXT NOT NULL,
        expiration_time INTEGER NOT NULL,
        license_type TEXT NOT NULL,
        xhs_id INTEGER,
        qq_id INTEGER,
        client_ip TEXT,
        created_at INTEGER DEFAULT (strftime('%s', 'now'))
    )`).run();

function sendJson(res, statusCode, payload) {
    res.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(payload));
}

function sendText(res, statusCode, contentType, content) {
    res.writeHead(statusCode, { 'Content-Type': `${contentType}; charset=utf-8` });
    res.end(content);
}

function sendFile(res, filePath, contentType) {
    fs.readFile(filePath, (err, data) => {
        if (err) {
            sendJson(res, 500, { success: false, error: '文件读取失败' });
            return;
        }

        res.writeHead(200, { 'Content-Type': contentType });
        res.end(data);
    });
}

function readRequestBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';

        req.on('data', chunk => {
            body += chunk.toString();
            if (body.length > 10 * 1024 * 1024) {
                reject(new Error('请求体过大'));
                req.destroy();
            }
        });

        req.on('end', () => resolve(body));
        req.on('error', reject);
    });
}

function normalizeString(value) {
    return value == null ? '' : String(value).trim();
}

function getClientIp(req) {
    const forwarded = normalizeString(req.headers['x-forwarded-for']);
    if (forwarded) {
        return forwarded.split(',')[0].trim();
    }
    return req.socket.remoteAddress || '127.0.0.1';
}

function normalizeLicenseType(value) {
    const normalized = normalizeString(value).toLowerCase().replace(/\s+/g, '');

    if (!normalized) {
        return '';
    }

    if (['pro', '专业', '专业版', 'pro版'].includes(normalized)) {
        return 'pro';
    }

    if (['standard', 'std', '标准', '标准版', 'standard版'].includes(normalized)) {
        return 'standard';
    }

    return '';
}

function parseIntegerField(value, fieldName, options = {}) {
    const { allowEmpty = true, min = Number.MIN_SAFE_INTEGER, max = Number.MAX_SAFE_INTEGER } = options;
    const text = normalizeString(value);

    if (!text) {
        return allowEmpty
            ? { ok: true, value: 0, empty: true }
            : { ok: false, error: `${fieldName}不能为空` };
    }

    if (!/^-?\d+$/.test(text)) {
        return { ok: false, error: `${fieldName}必须是整数` };
    }

    const parsed = Number.parseInt(text, 10);
    if (parsed < min || parsed > max) {
        return { ok: false, error: `${fieldName}必须在 ${min} ~ ${max} 之间` };
    }

    return { ok: true, value: parsed };
}

function parseStringField(value, options = {}) {
    const { allowEmpty = true } = options;
    const text = normalizeString(value);

    if (!text && !allowEmpty) {
        return { ok: false, error: '字段不能为空' };
    }

    return { ok: true, value: text };
}

function validateBatchRow(row, index) {
    const machineId = normalizeString(row.mid || row.machineId || row.deviceCode || row['设备码']);
    if (!machineId) {
        return { success: false, error: '设备码不能为空' };
    }

    const daysResult = parseIntegerField(row.days || row.duration || row['激活时长'], '激活时长', {
        allowEmpty: false,
        min: 1,
        max: 36500
    });
    if (!daysResult.ok) {
        return { success: false, error: daysResult.error };
    }

    const type = normalizeLicenseType(row.type || row.level || row.licenseType || row['授权级别']);
    if (!type) {
        return { success: false, error: '授权级别仅支持 standard / pro' };
    }

    const xhsResult = parseStringField(row.xhs || row.xhsId || row['小红书ID'], {
        allowEmpty: true
    });
    if (!xhsResult.ok) {
        return { success: false, error: '小红书ID格式不正确' };
    }

    const qqResult = parseIntegerField(row.qq || row.qqId || row['QQID'], 'QQID', {
        allowEmpty: true,
        min: 0
    });
    if (!qqResult.ok) {
        return { success: false, error: qqResult.error };
    }

    return {
        success: true,
        row: {
            serial: normalizeString(row.serial || row.sequence || row['序号']) || String(index + 1),
            rowNumber: Number.isInteger(row.rowNumber) ? row.rowNumber : index + 2,
            mid: machineId,
            days: daysResult.value,
            type,
            xhs: xhsResult.value,
            qq: qqResult.value
        }
    };
}

function signLicense(machineId, daysValid = 365, type = 'pro', xhsID = '', qqID = 0, ip = '127.0.0.1') {
    try {
        const privateKeyPath = path.join(__dirname, 'keys/private.pem');
        if (!fs.existsSync(privateKeyPath)) {
            return { success: false, error: '私钥文件不存在，请先运行 node license_tool.js gen' };
        }

        const privateKey = fs.readFileSync(privateKeyPath);
        const exp = Math.floor(Date.now() / 1000) + (daysValid * 24 * 60 * 60);
        const normalizedXhsId = normalizeString(xhsID);
        const payload = {
            mid: machineId,
            exp,
            type,
            xhs: normalizedXhsId,
            qq: qqID,
            salt: crypto.randomBytes(8).toString('hex')
        };

        const data = JSON.stringify(payload);
        const signer = crypto.createSign('SHA256');
        signer.update(data);
        signer.end();

        const signature = signer.sign(privateKey, 'base64');
        const license = `WANGWANG-${Buffer.from(data).toString('base64')}.${signature}`;

        const stmt = db.prepare('INSERT INTO sign_logs (machine_id, license_key, expiration_time, license_type, xhs_id, qq_id, client_ip) VALUES (?, ?, ?, ?, ?, ?, ?)');
        stmt.run(machineId, license, exp, type, normalizedXhsId, qqID, ip);

        return { success: true, license };
    } catch (err) {
        return { success: false, error: err.message };
    }
}

function batchSignLicenses(rows, ip = '127.0.0.1') {
    if (!Array.isArray(rows) || rows.length === 0) {
        return { success: false, error: '未提供批量数据' };
    }

    if (rows.length > 5000) {
        return { success: false, error: '单次最多处理 5000 行数据' };
    }

    const resultRows = [];
    let successCount = 0;

    rows.forEach((rawRow, index) => {
        const validated = validateBatchRow(rawRow, index);
        if (!validated.success) {
            resultRows.push({
                success: false,
                serial: normalizeString(rawRow.serial || rawRow.sequence || rawRow['序号']) || String(index + 1),
                rowNumber: Number.isInteger(rawRow.rowNumber) ? rawRow.rowNumber : index + 2,
                license: '',
                error: validated.error
            });
            return;
        }

        const row = validated.row;
        const signed = signLicense(row.mid, row.days, row.type, row.xhs, row.qq, ip);
        if (!signed.success) {
            resultRows.push({
                success: false,
                serial: row.serial,
                rowNumber: row.rowNumber,
                license: '',
                error: signed.error
            });
            return;
        }

        successCount += 1;
        resultRows.push({
            success: true,
            serial: row.serial,
            rowNumber: row.rowNumber,
            mid: row.mid,
            xhs: row.xhs,
            qq: row.qq,
            days: row.days,
            type: row.type,
            license: signed.license,
            error: ''
        });
    });

    return {
        success: true,
        rows: resultRows,
        summary: {
            total: rows.length,
            success: successCount,
            failed: rows.length - successCount
        }
    };
}

function traceLicense(licenseKey) {
    try {
        if (!licenseKey.startsWith('WANGWANG-')) {
            throw new Error('无效的激活码格式');
        }

        const rest = licenseKey.substring('WANGWANG-'.length);
        const parts = rest.split('.');
        if (parts.length !== 2) {
            throw new Error('激活码数据不完整');
        }

        const payloadBase64 = parts[0];
        const signatureBase64 = parts[1];
        const payloadJson = Buffer.from(payloadBase64, 'base64').toString('utf8');
        const payload = JSON.parse(payloadJson);

        const publicKeyPath = path.join(__dirname, 'keys/public.pem');
        if (fs.existsSync(publicKeyPath)) {
            const publicKey = fs.readFileSync(publicKeyPath);
            const verifier = crypto.createVerify('SHA256');
            verifier.update(payloadJson);
            verifier.end();
            payload.is_authentic = verifier.verify(publicKey, signatureBase64, 'base64');
        } else {
            payload.is_authentic = 'unknown (public key missing)';
        }

        return { success: true, data: payload };
    } catch (err) {
        return { success: false, error: err.message };
    }
}

const server = http.createServer(async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    if (req.url === '/' || req.url === '/index.html') {
        sendFile(res, guiPath, 'text/html; charset=utf-8');
        return;
    }

    if (req.url === '/vendor/xlsx.full.min.js') {
        if (!fs.existsSync(xlsxBrowserDistPath)) {
            sendJson(res, 500, { success: false, error: 'xlsx 浏览器资源不存在' });
            return;
        }

        sendFile(res, xlsxBrowserDistPath, 'application/javascript; charset=utf-8');
        return;
    }

    if (req.url === '/license_batch.js') {
        sendFile(res, path.join(__dirname, 'license_batch.js'), 'application/javascript; charset=utf-8');
        return;
    }

    if (req.url === '/api/sign' && req.method === 'POST') {
        try {
            const params = JSON.parse(await readRequestBody(req));
            const result = signLicense(params.mid, params.days, params.type, params.xhs, params.qq, getClientIp(req));
            sendJson(res, 200, result);
        } catch (err) {
            sendJson(res, 400, { success: false, error: '请求格式不正确' });
        }
        return;
    }

    if (req.url === '/api/batch-sign' && req.method === 'POST') {
        try {
            const params = JSON.parse(await readRequestBody(req));
            const result = batchSignLicenses(params.rows, getClientIp(req));
            sendJson(res, result.success ? 200 : 400, result);
        } catch (err) {
            sendJson(res, 400, { success: false, error: '请求格式不正确' });
        }
        return;
    }

    if (req.url === '/api/stats' && req.method === 'GET') {
        try {
            const total = db.prepare('SELECT COUNT(*) as count FROM sign_logs').get().count;
            const recent = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE created_at > (strftime('%s', 'now') - 86400)").get().count;
            const proCount = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE license_type = 'pro'").get().count;
            const standardCount = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE license_type = 'standard'").get().count;

            sendJson(res, 200, {
                success: true,
                stats: { total, recent_24h: recent, pro: proCount, standard: standardCount }
            });
        } catch (err) {
            sendJson(res, 500, { success: false, error: err.message });
        }
        return;
    }

    if (req.url === '/api/chart-data' && req.method === 'GET') {
        try {
            const dailyData = db.prepare(`
                SELECT date(created_at, 'unixepoch') as day, COUNT(*) as count
                FROM sign_logs
                WHERE created_at > (strftime('%s', 'now') - 86400 * 30)
                GROUP BY day
                ORDER BY day ASC
            `).all();

            const dailyByType = db.prepare(`
                SELECT date(created_at, 'unixepoch') as day, license_type, COUNT(*) as count
                FROM sign_logs
                WHERE created_at > (strftime('%s', 'now') - 86400 * 30)
                GROUP BY day, license_type
                ORDER BY day ASC
            `).all();

            const typeDistribution = db.prepare(`
                SELECT license_type, COUNT(*) as count
                FROM sign_logs
                GROUP BY license_type
            `).all();

            const recent7 = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE created_at > (strftime('%s', 'now') - 86400 * 7)").get().count;
            const prev7 = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE created_at > (strftime('%s', 'now') - 86400 * 14) AND created_at <= (strftime('%s', 'now') - 86400 * 7)").get().count;

            sendJson(res, 200, {
                success: true,
                daily: dailyData,
                dailyByType,
                typeDistribution,
                weekComparison: { recent7, prev7 }
            });
        } catch (err) {
            sendJson(res, 500, { success: false, error: err.message });
        }
        return;
    }

    if (req.url === '/api/trace' && req.method === 'POST') {
        try {
            const params = JSON.parse(await readRequestBody(req));
            const result = traceLicense(params.license);
            sendJson(res, 200, result);
        } catch (err) {
            sendJson(res, 400, { success: false, error: '请求格式不正确' });
        }
        return;
    }

    if (req.url.startsWith('/api/query') && req.method === 'GET') {
        try {
            const url = new URL(req.url, `http://${req.headers.host}`);
            const q = url.searchParams.get('q') || '';
            const type = url.searchParams.get('type') || '';
            const start = url.searchParams.get('start') || '';
            const end = url.searchParams.get('end') || '';
            const page = Number.parseInt(url.searchParams.get('page'), 10) || 1;
            const limit = Number.parseInt(url.searchParams.get('limit'), 10) || 30;
            const offset = (page - 1) * limit;

            const keywords = q.split(/\s+/).filter(k => k.length > 0);
            const whereClauses = [];
            const params = [];

            if (keywords.length > 0) {
                const kwClause = `(${keywords.map(() => '(machine_id LIKE ? OR license_key LIKE ? OR license_type LIKE ? OR CAST(xhs_id AS TEXT) LIKE ? OR CAST(qq_id AS TEXT) LIKE ? OR client_ip LIKE ?)').join(' AND ')})`;
                whereClauses.push(kwClause);
                keywords.forEach(kw => {
                    const pattern = `%${kw}%`;
                    params.push(pattern, pattern, pattern, pattern, pattern, pattern);
                });
            }

            if (type && type !== 'all') {
                whereClauses.push('license_type = ?');
                params.push(type);
            }

            if (start) {
                whereClauses.push('created_at >= ?');
                params.push(Math.floor(new Date(start).getTime() / 1000));
            }

            if (end) {
                whereClauses.push('created_at <= ?');
                params.push(Math.floor(new Date(end).getTime() / 1000) + 86399);
            }

            const whereSql = whereClauses.length > 0 ? ` WHERE ${whereClauses.join(' AND ')}` : '';
            const countRow = db.prepare(`SELECT COUNT(*) as count FROM sign_logs${whereSql}`).get(...params);
            const rows = db.prepare(`SELECT * FROM sign_logs${whereSql} ORDER BY created_at DESC LIMIT ? OFFSET ?`).all(...params, limit, offset);

            sendJson(res, 200, {
                success: true,
                data: rows,
                pagination: {
                    total: countRow.count,
                    page,
                    limit,
                    totalPages: Math.ceil(countRow.count / limit)
                }
            });
        } catch (err) {
            sendJson(res, 500, { success: false, error: err.message });
        }
        return;
    }

    sendText(res, 404, 'text/plain', 'Not Found');
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`汪汪机管理系统已启动: http://localhost:${PORT}`);
});

