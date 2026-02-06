using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.IO;
using System.Windows.Media.Imaging;

namespace WangWangPhone.Core
{
    public class WallpaperRecord
    {
        public string WallpaperType { get; set; }
        public string FileName { get; set; }
        public long UpdatedAt { get; set; }
    }

    public class WallpaperManager
    {
        private static readonly Lazy<WallpaperManager> _instance = new Lazy<WallpaperManager>(() => new WallpaperManager());
        public static WallpaperManager Instance => _instance.Value;

        private SQLiteConnection _connection;
        private bool _isInitialized;
        private readonly string _dbName = "wangwang_wallpaper.db";
        private readonly string _wallpaperDir = "wallpapers";

        private WallpaperManager() { Initialize(); }

        public void Initialize()
        {
            if (_isInitialized) return;
            try {
                string dbPath = GetDatabasePath();
                _connection = new SQLiteConnection($"Data Source={dbPath};Version=3;");
                _connection.Open();
                CreateTables();
                GetWallpaperDirectory();
                _isInitialized = true;
            } catch { }
        }

        public string CopyImageToStorage(string sourcePath)
        {
            try {
                string fileName = $"wp_{Guid.NewGuid()}.jpg";
                string destPath = Path.Combine(GetWallpaperDirectory(), fileName);
                File.Copy(sourcePath, destPath, true);
                return fileName;
            } catch { return null; }
        }

        public bool SaveWallpaper(string type, string fileName)
        {
            var old = GetWallpaper(type);
            if (old != null) {
                try { File.Delete(Path.Combine(GetWallpaperDirectory(), old.FileName)); } catch {}
            }
            string sql = "INSERT OR REPLACE INTO wallpaper (wallpaper_type, file_name, updated_at) VALUES (@type, @file, strftime('%s', 'now'));";
            using (var cmd = new SQLiteCommand(sql, _connection)) {
                cmd.Parameters.AddWithValue("@type", type);
                cmd.Parameters.AddWithValue("@file", fileName);
                return cmd.ExecuteNonQuery() > 0;
            }
        }

        public WallpaperRecord GetWallpaper(string type)
        {
            string sql = "SELECT wallpaper_type, file_name, updated_at FROM wallpaper WHERE wallpaper_type = @type LIMIT 1;";
            using (var cmd = new SQLiteCommand(sql, _connection)) {
                cmd.Parameters.AddWithValue("@type", type);
                using (var reader = cmd.ExecuteReader()) {
                    if (reader.Read()) {
                        return new WallpaperRecord {
                            WallpaperType = reader.GetString(0),
                            FileName = reader.GetString(1),
                            UpdatedAt = reader.GetInt64(2)
                        };
                    }
                }
            }
            return null;
        }

        public string GetWallpaperFilePath(string type)
        {
            var record = GetWallpaper(type);
            if (record == null) return null;
            string path = Path.Combine(GetWallpaperDirectory(), record.FileName);
            return File.Exists(path) ? path : null;
        }

        private string GetDatabasePath()
        {
            string path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "WangWangPhone");
            if (!Directory.Exists(path)) Directory.CreateDirectory(path);
            return Path.Combine(path, _dbName);
        }

        private string GetWallpaperDirectory()
        {
            string path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "WangWangPhone", _wallpaperDir);
            if (!Directory.Exists(path)) Directory.CreateDirectory(path);
            return path;
        }

        private void CreateTables()
        {
            string sql = "CREATE TABLE IF NOT EXISTS wallpaper (id INTEGER PRIMARY KEY AUTOINCREMENT, wallpaper_type TEXT NOT NULL UNIQUE, file_name TEXT NOT NULL, created_at INTEGER DEFAULT (strftime('%s', 'now')), updated_at INTEGER DEFAULT (strftime('%s', 'now')));";
            using (var cmd = new SQLiteCommand(sql, _connection)) cmd.ExecuteNonQuery();
        }
    }
}
