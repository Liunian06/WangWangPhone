using System;
using System.Data.SQLite;
using System.IO;
using System.Management;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace WangWangPhone.Core
{
    /// <summary>
    /// 授权记录
    /// </summary>
    public class LicenseRecord
    {
        public string LicenseKey { get; set; }
        public string MachineId { get; set; }
        public long ExpirationTime { get; set; }
        public string LicenseType { get; set; }
        public long ActivationTime { get; set; }
    }

    public class AppLayoutRecord
    {
        public string AppId { get; set; }
        public int Col { get; set; }
        public int Row { get; set; }
        public int SpanX { get; set; }
        public int SpanY { get; set; }
    }

    /// <summary>
    /// 授权载荷
    /// </summary>
    public class LicensePayload
    {
        public string MachineId { get; set; }
        public long ExpirationTime { get; set; }
        public string Type { get; set; }
        public string Salt { get; set; }
    }

    /// <summary>
    /// 授权操作结果
    /// </summary>
    public class LicenseResult
    {
        public bool IsSuccess { get; set; }
        public LicenseRecord Record { get; set; }
        public string ErrorMessage { get; set; }

        public static LicenseResult Success(LicenseRecord record) =>
            new LicenseResult { IsSuccess = true, Record = record };

        public static LicenseResult Error(string message) =>
            new LicenseResult { IsSuccess = false, ErrorMessage = message };
    }

    /// <summary>
    /// 授权管理器 - Windows 平台实现
    /// 
    /// 该类封装了与 C++ Core LicenseManager 的交互。
    /// 当前版本使用 SQLite 进行本地持久化。
    /// </summary>
    public class LicenseManager
    {
        private static readonly Lazy<LicenseManager> _instance =
            new Lazy<LicenseManager>(() => new LicenseManager());

        public static LicenseManager Instance => _instance.Value;

        private SQLiteConnection _connection;
        private LicenseRecord _cachedLicense;
        private bool _isInitialized;
        private readonly string _dbName = "wangwang_license.db";

        private LicenseManager() { }

        /// <summary>
        /// 初始化授权管理器
        /// </summary>
        public bool Initialize()
        {
            if (_isInitialized) return true;

            try
            {
                string dbPath = GetDatabasePath();
                string connectionString = $"Data Source={dbPath};Version=3;";
                
                _connection = new SQLiteConnection(connectionString);
                _connection.Open();

                if (!CreateTables())
                {
                    Console.WriteLine("LicenseManager: 创建表失败");
                    return false;
                }

                // 尝试从数据库恢复授权
                _cachedLicense = GetLicenseRecord();
                _isInitialized = true;

                Console.WriteLine("LicenseManager: 初始化成功");
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"LicenseManager: 初始化失败 - {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 获取设备机器码
        /// </summary>
        public string GetMachineId()
        {
            try
            {
                // 使用多种硬件信息组合生成唯一机器码
                StringBuilder sb = new StringBuilder();

                // CPU ID
                using (var searcher = new ManagementObjectSearcher("SELECT ProcessorId FROM Win32_Processor"))
                {
                    foreach (var item in searcher.Get())
                    {
                        sb.Append(item["ProcessorId"]?.ToString() ?? "");
                        break;
                    }
                }

                // 主板序列号
                using (var searcher = new ManagementObjectSearcher("SELECT SerialNumber FROM Win32_BaseBoard"))
                {
                    foreach (var item in searcher.Get())
                    {
                        sb.Append(item["SerialNumber"]?.ToString() ?? "");
                        break;
                    }
                }

                // 计算哈希
                using (var sha256 = SHA256.Create())
                {
                    byte[] hash = sha256.ComputeHash(Encoding.UTF8.GetBytes(sb.ToString()));
                    string hashString = BitConverter.ToString(hash).Replace("-", "").Substring(0, 16);
                    return $"WIN-{hashString}";
                }
            }
            catch
            {
                // 如果获取硬件信息失败，使用机器名
                return $"WIN-{Environment.MachineName}";
            }
        }

        /// <summary>
        /// 验证并激活授权码
        /// </summary>
        public async Task<LicenseResult> VerifyLicenseAsync(string licenseKey)
        {
            return await Task.Run(() =>
            {
                try
                {
                    // 检查格式
                    if (!licenseKey.StartsWith("WANGWANG-"))
                    {
                        return LicenseResult.Error("激活码格式无效");
                    }

                    // 解析激活码
                    var payload = ParseLicenseKey(licenseKey);
                    if (payload == null)
                    {
                        return LicenseResult.Error("激活码解析失败");
                    }

                    // 验证机器码
                    string currentMachineId = GetMachineId();
                    if (payload.MachineId != currentMachineId)
                    {
                        return LicenseResult.Error("机器码不匹配");
                    }

                    // 验证过期时间
                    long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                    if (payload.ExpirationTime < now)
                    {
                        return LicenseResult.Error("授权已过期");
                    }

                    // 保存到数据库
                    var record = new LicenseRecord
                    {
                        LicenseKey = licenseKey,
                        MachineId = payload.MachineId,
                        ExpirationTime = payload.ExpirationTime,
                        LicenseType = payload.Type,
                        ActivationTime = now
                    };

                    if (SaveLicenseRecord(record))
                    {
                        _cachedLicense = record;
                        return LicenseResult.Success(record);
                    }
                    else
                    {
                        return LicenseResult.Error("保存授权信息失败");
                    }
                }
                catch (Exception ex)
                {
                    return LicenseResult.Error($"激活失败: {ex.Message}");
                }
            });
        }

        /// <summary>
        /// 检查是否已激活
        /// </summary>
        public bool IsActivated()
        {
            var license = _cachedLicense ?? GetLicenseRecord();
            if (license == null) return false;

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            return license.ExpirationTime > now;
        }

        /// <summary>
        /// 检查授权是否过期
        /// </summary>
        public bool IsExpired()
        {
            if (_cachedLicense == null) return true;
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            return _cachedLicense.ExpirationTime <= now;
        }

        /// <summary>
        /// 获取剩余天数
        /// </summary>
        public int GetRemainingDays()
        {
            if (_cachedLicense == null) return 0;
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long remaining = _cachedLicense.ExpirationTime - now;
            return remaining > 0 ? (int)(remaining / (24 * 60 * 60)) : 0;
        }

        /// <summary>
        /// 获取过期日期字符串
        /// </summary>
        public string GetExpirationDateString()
        {
            if (_cachedLicense == null) return "未激活";
            var date = DateTimeOffset.FromUnixTimeSeconds(_cachedLicense.ExpirationTime).LocalDateTime;
            return date.ToString("yyyy-MM-dd");
        }

        /// <summary>
        /// 获取授权类型
        /// </summary>
        public string GetLicenseType()
        {
            return _cachedLicense?.LicenseType ?? "free";
        }

        /// <summary>
        /// 清除授权信息
        /// </summary>
        public bool ClearLicense()
        {
            _cachedLicense = null;
            return ClearLicenseRecord();
        }

        /// <summary>
        /// 保存布局信息
        /// </summary>
        public bool SaveAppLayout(string appId, int col, int row, int spanX = 1, int spanY = 1)
        {
            try
            {
                string replaceSQL = @"
                    INSERT OR REPLACE INTO app_layout (app_id, col, row, span_x, span_y, updated_at)
                    VALUES (@appId, @col, @row, @spanX, @spanY, strftime('%s', 'now'));
                ";

                using (var command = new SQLiteCommand(replaceSQL, _connection))
                {
                    command.Parameters.AddWithValue("@appId", appId);
                    command.Parameters.AddWithValue("@col", col);
                    command.Parameters.AddWithValue("@row", row);
                    command.Parameters.AddWithValue("@spanX", spanX);
                    command.Parameters.AddWithValue("@spanY", spanY);
                    command.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 获取所有布局信息
        /// </summary>
        public System.Collections.Generic.List<AppLayoutRecord> GetAppLayouts()
        {
            var layouts = new System.Collections.Generic.List<AppLayoutRecord>();
            try
            {
                string selectSQL = "SELECT app_id, col, row, span_x, span_y FROM app_layout;";
                using (var command = new SQLiteCommand(selectSQL, _connection))
                using (var reader = command.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        layouts.Add(new AppLayoutRecord
                        {
                            AppId = reader.GetString(0),
                            Col = reader.GetInt32(1),
                            Row = reader.GetInt32(2),
                            SpanX = reader.GetInt32(3),
                            SpanY = reader.GetInt32(4)
                        });
                    }
                }
            }
            catch { }
            return layouts;
        }

        #region 私有方法

        private string GetDatabasePath()
        {
            string appDataPath = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string appFolder = Path.Combine(appDataPath, "WangWangPhone");
            
            if (!Directory.Exists(appFolder))
            {
                Directory.CreateDirectory(appFolder);
            }

            return Path.Combine(appFolder, _dbName);
        }

        private bool CreateTables()
        {
            string createTableSQL = @"
                CREATE TABLE IF NOT EXISTS license (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    license_key TEXT NOT NULL,
                    machine_id TEXT NOT NULL,
                    expiration_time INTEGER NOT NULL,
                    license_type TEXT DEFAULT 'standard',
                    activation_time INTEGER NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
                );
            ";

            string createLayoutTableSQL = @"
                CREATE TABLE IF NOT EXISTS app_layout (
                    app_id TEXT PRIMARY KEY,
                    col INTEGER NOT NULL,
                    row INTEGER NOT NULL,
                    span_x INTEGER DEFAULT 1,
                    span_y INTEGER DEFAULT 1,
                    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
                );
            ";

            try
            {
                using (var command = new SQLiteCommand(createTableSQL, _connection))
                {
                    command.ExecuteNonQuery();
                }
                using (var command = new SQLiteCommand(createLayoutTableSQL, _connection))
                {
                    command.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        private bool SaveLicenseRecord(LicenseRecord record)
        {
            try
            {
                // 先清除旧记录
                ClearLicenseRecord();

                string insertSQL = @"
                    INSERT INTO license (license_key, machine_id, expiration_time, license_type, activation_time)
                    VALUES (@licenseKey, @machineId, @expirationTime, @licenseType, @activationTime);
                ";

                using (var command = new SQLiteCommand(insertSQL, _connection))
                {
                    command.Parameters.AddWithValue("@licenseKey", record.LicenseKey);
                    command.Parameters.AddWithValue("@machineId", record.MachineId);
                    command.Parameters.AddWithValue("@expirationTime", record.ExpirationTime);
                    command.Parameters.AddWithValue("@licenseType", record.LicenseType);
                    command.Parameters.AddWithValue("@activationTime", record.ActivationTime);
                    command.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        private LicenseRecord GetLicenseRecord()
        {
            string selectSQL = @"
                SELECT license_key, machine_id, expiration_time, license_type, activation_time
                FROM license
                ORDER BY id DESC
                LIMIT 1;
            ";

            try
            {
                using (var command = new SQLiteCommand(selectSQL, _connection))
                using (var reader = command.ExecuteReader())
                {
                    if (reader.Read())
                    {
                        return new LicenseRecord
                        {
                            LicenseKey = reader.GetString(0),
                            MachineId = reader.GetString(1),
                            ExpirationTime = reader.GetInt64(2),
                            LicenseType = reader.GetString(3),
                            ActivationTime = reader.GetInt64(4)
                        };
                    }
                }
            }
            catch
            {
                // 忽略错误
            }

            return null;
        }

        private bool ClearLicenseRecord()
        {
            try
            {
                using (var command = new SQLiteCommand("DELETE FROM license;", _connection))
                {
                    command.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 解析激活码 (模拟实现)
        /// TODO: 当 C++ Core 就绪后，通过 P/Invoke 调用真正的 RSA 验签
        /// </summary>
        private LicensePayload ParseLicenseKey(string licenseKey)
        {
            try
            {
                // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
                string rest = licenseKey.Substring("WANGWANG-".Length);
                string[] parts = rest.Split('.');

                if (parts.Length != 2) return null;

                // TODO: 验证签名
                // TODO: Base64 解码并解析 JSON

                // 模拟解析 - 使用当前时间 + 365天作为过期时间
                long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                long expiration = now + (365 * 24 * 60 * 60);

                return new LicensePayload
                {
                    MachineId = GetMachineId(),
                    ExpirationTime = expiration,
                    Type = "pro",
                    Salt = "generated"
                };
            }
            catch
            {
                return null;
            }
        }

        #endregion
    }
}