#!/bin/bash
# 从 tools/keys/public.pem 读取公钥并生成 Swift 常量文件
# 此脚本应在 Xcode Build Phase 的 "Run Script" 中执行

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PEM_FILE="$PROJECT_ROOT/tools/keys/public.pem"
OUTPUT_FILE="$SCRIPT_DIR/WangWangPhone/Core/GeneratedPublicKey.swift"

if [ ! -f "$PEM_FILE" ]; then
    echo "ERROR: public.pem not found at $PEM_FILE"
    exit 1
fi

# 提取 Base64 内容（去掉 PEM 头尾和换行）
PUBLIC_KEY_B64=$(sed '1d;$d' "$PEM_FILE" | tr -d '\n\r ')

echo "// 此文件由 generate_pubkey.sh 自动生成，请勿手动编辑" > "$OUTPUT_FILE"
echo "// 公钥来源: tools/keys/public.pem" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "import Foundation" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "enum LicenseKeys {" >> "$OUTPUT_FILE"
echo "    static let rsaPublicKeyBase64 = \"$PUBLIC_KEY_B64\"" >> "$OUTPUT_FILE"
echo "}" >> "$OUTPUT_FILE"

echo "Generated public key constant at $OUTPUT_FILE (${#PUBLIC_KEY_B64} chars)"
