const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// 初始化后端签名数据库
const sqlite3 = require('sqlite3').verbose();
const dbPath = path.join(__dirname, 'server_sign_logs.db');
const db = new sqlite3.Database(dbPath);

db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS sign_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        machine_id TEXT NOT NULL,
        license_key TEXT NOT NULL,
        expiration_time INTEGER NOT NULL,
        license_type TEXT NOT NULL,
        xhs_id INTEGER,
        qq_id INTEGER,
        client_ip TEXT,
        created_at INTEGER DEFAULT (strftime('%s', 'now'))
    )`);
});

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
        stmt.finalize();

        return { success: true, license: license };
    } catch (err) {
        return { success: false, error: err.message };
    }
}

const server = http.createServer((req, res) => {
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
    // 处理签名 API
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
    } else {
        res.writeHead(404);
        res.end('Not Found');
    }
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`汪汪机授权签名后台已启动: http://localhost:${PORT}`);
    console.log(`请在浏览器中打开该地址进行签名。`);
});