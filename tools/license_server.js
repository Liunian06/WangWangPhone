const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// 初始化后端签名数据库
const sqlite3 = require('better-sqlite3');
const dbPath = path.join(__dirname, 'server_sign_logs.db');
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

// 导入签名逻辑 (模拟 license_tool.js 的功能)
function signLicense(machineId, daysValid = 365, type = 'pro', xhsID = 0, qqID = 0, ip = '127.0.0.1') {
    try {
        const privateKeyPath = path.join(__dirname, 'keys/private.pem');
        if (!fs.existsSync(privateKeyPath)) {
            return { success: false, error: '私钥文件不存在，请先运行 node license_tool.js gen' };
        }
        
        const privateKey = fs.readFileSync(privateKeyPath);
        
        const exp = Math.floor(Date.now() / 1000) + (daysValid * 24 * 60 * 60);
        const payload = {
            mid: machineId,
            exp: exp,
            type: type,
            xhs: xhsID,
            qq: qqID,
            salt: crypto.randomBytes(8).toString('hex')
        };

        const data = JSON.stringify(payload);
        const signer = crypto.createSign('SHA256');
        signer.update(data);
        signer.end();
        
        const signature = signer.sign(privateKey, 'base64');
        
        // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
        const license = 'WANGWANG-' + Buffer.from(data).toString('base64') + '.' + signature;
        
        // 记录到数据库
        const stmt = db.prepare("INSERT INTO sign_logs (machine_id, license_key, expiration_time, license_type, xhs_id, qq_id, client_ip) VALUES (?, ?, ?, ?, ?, ?, ?)");
        stmt.run(machineId, license, exp, type, xhsID, qqID, ip);

        return { success: true, license: license };
    } catch (err) {
        return { success: false, error: err.message };
    }
}

// 溯源：解密激活码
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

        // 验证签名
        const publicKeyPath = path.join(__dirname, 'keys/public.pem');
        if (fs.existsSync(publicKeyPath)) {
            const publicKey = fs.readFileSync(publicKeyPath);
            const verifier = crypto.createVerify('SHA256');
            verifier.update(payloadJson);
            const isValid = verifier.verify(publicKey, signatureBase64, 'base64');
            payload.is_authentic = isValid;
        } else {
            payload.is_authentic = 'unknown (public key missing)';
        }

        return { success: true, data: payload };
    } catch (err) {
        return { success: false, error: err.message };
    }
}

const server = http.createServer((req, res) => {
    // 允许跨域 (如果需要)
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // 处理静态 HTML 页面
    if (req.url === '/' || req.url === '/index.html') {
        fs.readFile(path.join(__dirname, 'license_gui.html'), (err, data) => {
            if (err) {
                res.writeHead(500);
                res.end('Error loading GUI');
                return;
            }
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(data);
        });
    } 
    // API: 签发
    else if (req.url === '/api/sign' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk.toString(); });
        req.on('end', () => {
            try {
                const params = JSON.parse(body);
                const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
                const result = signLicense(params.mid, params.days, params.type, params.xhs, params.qq, ip);
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(result));
            } catch (err) {
                res.writeHead(400);
                res.end(JSON.stringify({ success: false, error: 'Invalid request' }));
            }
        });
    }
    // API: 统计信息
    else if (req.url === '/api/stats' && req.method === 'GET') {
        try {
            const total = db.prepare("SELECT COUNT(*) as count FROM sign_logs").get().count;
            const recent = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE created_at > (strftime('%s', 'now') - 86400)").get().count;
            const proCount = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE license_type = 'pro'").get().count;
            const standardCount = db.prepare("SELECT COUNT(*) as count FROM sign_logs WHERE license_type = 'standard'").get().count;
            
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                success: true,
                stats: { total, recent_24h: recent, pro: proCount, standard: standardCount }
            }));
        } catch (err) {
            res.writeHead(500);
            res.end(JSON.stringify({ success: false, error: err.message }));
        }
    }
    // API: 溯源
    else if (req.url === '/api/trace' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk.toString(); });
        req.on('end', () => {
            try {
                const params = JSON.parse(body);
                const result = traceLicense(params.license);
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(result));
            } catch (err) {
                res.writeHead(400);
                res.end(JSON.stringify({ success: false, error: 'Invalid request' }));
            }
        });
    }
    // API: 查询
    else if (req.url.startsWith('/api/query') && req.method === 'GET') {
        try {
            const url = new URL(req.url, `http://${req.headers.host}`);
            const query = url.searchParams.get('q') || '';
            const keywords = query.split(/\s+/).filter(k => k.length > 0);
            
            let sql = "SELECT * FROM sign_logs";
            let params = [];
            
            if (keywords.length > 0) {
                sql += " WHERE " + keywords.map(() => 
                    "(machine_id LIKE ? OR license_key LIKE ? OR license_type LIKE ? OR xhs_id LIKE ? OR qq_id LIKE ? OR client_ip LIKE ?)"
                ).join(" AND ");
                
                keywords.forEach(kw => {
                    const pattern = `%${kw}%`;
                    params.push(pattern, pattern, pattern, pattern, pattern, pattern);
                });
            }
            
            sql += " ORDER BY created_at DESC LIMIT 100";
            const rows = db.prepare(sql).all(...params);
            
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, data: rows }));
        } catch (err) {
            res.writeHead(500);
            res.end(JSON.stringify({ success: false, error: err.message }));
        }
    }
    else {
        res.writeHead(404);
        res.end('Not Found');
    }
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`汪汪机管理系统已启动: http://localhost:${PORT}`);
});