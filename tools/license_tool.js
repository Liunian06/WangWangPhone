const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

// 生成 RSA 密钥对
function generateKeyPair() {
    console.log('正在生成 RSA 密钥对...');
    const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
        modulusLength: 2048,
        publicKeyEncoding: {
            type: 'spki',
            format: 'pem'
        },
        privateKeyEncoding: {
            type: 'pkcs8',
            format: 'pem'
        }
    });

    const keyDir = path.join(__dirname, 'keys');
    if (!fs.existsSync(keyDir)) {
        fs.mkdirSync(keyDir);
    }

    fs.writeFileSync(path.join(keyDir, 'private.pem'), privateKey);
    fs.writeFileSync(path.join(keyDir, 'public.pem'), publicKey);

    console.log('密钥对已生成：');
    console.log('- 私钥 (用于签发激活码): keys/private.pem');
    console.log('- 公钥 (集成到 App 中): keys/public.pem');
    
    // 同时也生成一个 C++ 风格的公钥数组，方便直接集成
    const publicKeyClean = publicKey.replace(/-----(BEGIN|END) PUBLIC KEY-----/g, '').replace(/\n/g, '');
    console.log('\nApp 集成建议 (C++ 公钥字符串):');
    console.log(publicKeyClean);
}

// 签发激活码工具
function signLicense(machineId, daysValid = 365) {
    const privateKey = fs.readFileSync(path.join(__dirname, 'keys/private.pem'));
    
    const exp = Math.floor(Date.now() / 1000) + (daysValid * 24 * 60 * 60);
    const payload = {
        mid: machineId,
        exp: exp,
        type: 'pro',
        salt: crypto.randomBytes(8).toString('hex')
    };

    const data = JSON.stringify(payload);
    const signer = crypto.createSign('SHA256');
    signer.update(data);
    signer.end();
    
    const signature = signer.sign(privateKey, 'base64');
    
    // 最终激活码格式: WANGWANG-[Payload-Base64].[Signature-Base64]
    const license = 'WANGWANG-' + Buffer.from(data).toString('base64') + '.' + signature;
    console.log('\n生成的激活码:');
    console.log(license);
}

// 命令行参数处理
const args = process.argv.slice(2);
if (args[0] === 'gen') {
    generateKeyPair();
} else if (args[0] === 'sign' && args[1]) {
    signLicense(args[1], parseInt(args[2]) || 365);
} else {
    console.log('汪汪机授权管理工具');
    console.log('用法:');
    console.log('  node license_tool.js gen              # 生成新的公私钥对');
    console.log('  node license_tool.js sign <机器码>     # 为指定设备签发激活码');
}
