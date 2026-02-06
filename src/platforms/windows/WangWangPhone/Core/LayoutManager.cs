using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.IO;

namespace WangWangPhone.Core
{
    /// <summary>
    /// 布局项
    /// </summary>
    public class LayoutItem
    {
        public string AppId { get; set; }
        public int Position { get; set; }
        public string Area { get; set; } = "grid";
    }

    /// <summary>
    /// 布局管理器 - Windows 平台实现
    /// 使用 SQLite 持久化存储用户自定义的主屏幕布局
    /// </summary>
    public class LayoutManager
    {
        private static readonly Lazy<LayoutManager> _instance =
            new Lazy<LayoutManager>(() => new LayoutManager());

        public static LayoutManager Instance => _instance.Value;

        private SQLiteConnection _connection;
        private bool _isInitialized;
        private readonly string _dbName = "wangwang_layout.db";

        private LayoutManager() { }

        /// <summary>
        /// 初始化布局管理器
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
                    Console.WriteLine("LayoutManager: 创建表失败");
                    return false;
                }

                _isInitialized = true;
                Console.WriteLine("LayoutManager: 初始化成功");
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"LayoutManager: 初始化失败 - {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 保存整个布局（事务操作）
        /// </summary>
        public bool SaveLayout(List<LayoutItem> items)
        {
            if (_connection == null) return false;

            using (var transaction = _connection.BeginTransaction())
            {
                try
                {
                    // 先清除旧布局
                    using (var clearCmd = new SQLiteCommand("DELETE FROM app_layout;", _connection, transaction))
                    {
                        clearCmd.ExecuteNonQuery();
                    }

                    // 插入新布局
                    string insertSQL = @"
                        INSERT INTO app_layout (app_id, position, area)
                        VALUES (@appId, @position, @area);
                    ";

                    foreach (var item in items)
                    {
                        using (var cmd = new SQLiteCommand(insertSQL, _connection, transaction))
                        {
                            cmd.Parameters.AddWithValue("@appId", item.AppId);
                            cmd.Parameters.AddWithValue("@position", item.Position);
                            cmd.Parameters.AddWithValue("@area", item.Area);
                            cmd.ExecuteNonQuery();
                        }
                    }

                    transaction.Commit();
                    Console.WriteLine($"LayoutManager: 布局已保存 ({items.Count} 项)");
                    return true;
                }
                catch (Exception ex)
                {
                    transaction.Rollback();
                    Console.WriteLine($"LayoutManager: 保存布局失败 - {ex.Message}");
                    return false;
                }
            }
        }

        /// <summary>
        /// 获取保存的布局
        /// </summary>
        public List<LayoutItem> GetLayout()
        {
            var items = new List<LayoutItem>();
            if (_connection == null) return items;

            string selectSQL = @"
                SELECT app_id, position, area
                FROM app_layout
                ORDER BY area ASC, position ASC;
            ";

            try
            {
                using (var cmd = new SQLiteCommand(selectSQL, _connection))
                using (var reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        items.Add(new LayoutItem
                        {
                            AppId = reader.GetString(0),
                            Position = reader.GetInt32(1),
                            Area = reader.GetString(2)
                        });
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"LayoutManager: 获取布局失败 - {ex.Message}");
            }

            return items;
        }

        /// <summary>
        /// 清除所有布局
        /// </summary>
        public bool ClearLayout()
        {
            try
            {
                using (var cmd = new SQLiteCommand("DELETE FROM app_layout;", _connection))
                {
                    cmd.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 检查是否有已保存的布局
        /// </summary>
        public bool HasLayout()
        {
            return GetLayout().Count > 0;
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
                CREATE TABLE IF NOT EXISTS app_layout (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app_id TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    area TEXT NOT NULL DEFAULT 'grid',
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
                );
            ";

            string createIndexSQL = @"
                CREATE UNIQUE INDEX IF NOT EXISTS idx_layout_app ON app_layout(app_id, area);
            ";

            try
            {
                using (var cmd = new SQLiteCommand(createTableSQL, _connection))
                {
                    cmd.ExecuteNonQuery();
                }
                using (var cmd = new SQLiteCommand(createIndexSQL, _connection))
                {
                    cmd.ExecuteNonQuery();
                }
                return true;
            }
            catch
            {
                return false;
            }
        }

        #endregion
    }
}