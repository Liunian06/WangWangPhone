import Foundation
import SQLite3

struct ApiPreset: Identifiable, Equatable {
    var id: Int64 = 0
    var name: String
    var type: String // "chat", "image", "voice"
    var provider: String // "openai", "gemini", "minimax"
    var apiKey: String
    var baseUrl: String
    var model: String
    var extraParams: String = "{}"
    
    static func == (lhs: ApiPreset, rhs: ApiPreset) -> Bool {
        return lhs.id == rhs.id
    }
}

class ApiPresetManager {
    static let shared = ApiPresetManager()
    private var db: OpaquePointer?
    
    private init() {
        openDatabase()
        createTable()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("api_preset.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
    }
    
    private func createTable() {
        let createTableQuery = """
        CREATE TABLE IF NOT EXISTS api_presets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            type TEXT NOT NULL,
            provider TEXT NOT NULL,
            api_key TEXT NOT NULL,
            base_url TEXT NOT NULL,
            model TEXT NOT NULL,
            extra_params TEXT DEFAULT '{}'
        )
        """
        
        if sqlite3_exec(db, createTableQuery, nil, nil, nil) != SQLITE_OK {
            print("Error creating table")
        }
    }
    
    func savePreset(_ preset: ApiPreset) -> Int64 {
        if preset.id > 0 {
            let updateQuery = """
            UPDATE api_presets SET name = ?, type = ?, provider = ?, api_key = ?, 
            base_url = ?, model = ?, extra_params = ? WHERE id = ?
            """
            var statement: OpaquePointer?
            
            if sqlite3_prepare_v2(db, updateQuery, -1, &statement, nil) == SQLITE_OK {
                sqlite3_bind_text(statement, 1, (preset.name as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 2, (preset.type as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 3, (preset.provider as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 4, (preset.apiKey as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 5, (preset.baseUrl as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 6, (preset.model as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 7, (preset.extraParams as NSString).utf8String, -1, nil)
                sqlite3_bind_int64(statement, 8, preset.id)
                
                if sqlite3_step(statement) == SQLITE_DONE {
                    sqlite3_finalize(statement)
                    return preset.id
                }
            }
            sqlite3_finalize(statement)
            return preset.id
        } else {
            let insertQuery = """
            INSERT INTO api_presets (name, type, provider, api_key, base_url, model, extra_params)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
            var statement: OpaquePointer?
            
            if sqlite3_prepare_v2(db, insertQuery, -1, &statement, nil) == SQLITE_OK {
                sqlite3_bind_text(statement, 1, (preset.name as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 2, (preset.type as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 3, (preset.provider as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 4, (preset.apiKey as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 5, (preset.baseUrl as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 6, (preset.model as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 7, (preset.extraParams as NSString).utf8String, -1, nil)
                
                if sqlite3_step(statement) == SQLITE_DONE {
                    let id = sqlite3_last_insert_rowid(db)
                    sqlite3_finalize(statement)
                    return id
                }
            }
            sqlite3_finalize(statement)
            return 0
        }
    }
    
    func getPresetsByType(_ type: String) -> [ApiPreset] {
        var presets: [ApiPreset] = []
        let query = "SELECT * FROM api_presets WHERE type = ? ORDER BY id DESC"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_text(statement, 1, (type as NSString).utf8String, -1, nil)
            
            while sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let name = String(cString: sqlite3_column_text(statement, 1))
                let type = String(cString: sqlite3_column_text(statement, 2))
                let provider = String(cString: sqlite3_column_text(statement, 3))
                let apiKey = String(cString: sqlite3_column_text(statement, 4))
                let baseUrl = String(cString: sqlite3_column_text(statement, 5))
                let model = String(cString: sqlite3_column_text(statement, 6))
                let extraParams = String(cString: sqlite3_column_text(statement, 7))
                
                presets.append(ApiPreset(
                    id: id, name: name, type: type, provider: provider,
                    apiKey: apiKey, baseUrl: baseUrl, model: model, extraParams: extraParams
                ))
            }
        }
        sqlite3_finalize(statement)
        return presets
    }
    
    func getPresetById(_ id: Int64) -> ApiPreset? {
        let query = "SELECT * FROM api_presets WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, id)
            
            if sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let name = String(cString: sqlite3_column_text(statement, 1))
                let type = String(cString: sqlite3_column_text(statement, 2))
                let provider = String(cString: sqlite3_column_text(statement, 3))
                let apiKey = String(cString: sqlite3_column_text(statement, 4))
                let baseUrl = String(cString: sqlite3_column_text(statement, 5))
                let model = String(cString: sqlite3_column_text(statement, 6))
                let extraParams = String(cString: sqlite3_column_text(statement, 7))
                
                sqlite3_finalize(statement)
                return ApiPreset(
                    id: id, name: name, type: type, provider: provider,
                    apiKey: apiKey, baseUrl: baseUrl, model: model, extraParams: extraParams
                )
            }
        }
        sqlite3_finalize(statement)
        return nil
    }
    
    func deletePreset(_ id: Int64) -> Bool {
        let deleteQuery = "DELETE FROM api_presets WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, deleteQuery, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, id)
            
            if sqlite3_step(statement) == SQLITE_DONE {
                sqlite3_finalize(statement)
                return true
            }
        }
        sqlite3_finalize(statement)
        return false
    }
    
    func getAllPresets() -> [ApiPreset] {
        var presets: [ApiPreset] = []
        let query = "SELECT * FROM api_presets ORDER BY id DESC"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            while sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let name = String(cString: sqlite3_column_text(statement, 1))
                let type = String(cString: sqlite3_column_text(statement, 2))
                let provider = String(cString: sqlite3_column_text(statement, 3))
                let apiKey = String(cString: sqlite3_column_text(statement, 4))
                let baseUrl = String(cString: sqlite3_column_text(statement, 5))
                let model = String(cString: sqlite3_column_text(statement, 6))
                let extraParams = String(cString: sqlite3_column_text(statement, 7))
                
                presets.append(ApiPreset(
                    id: id, name: name, type: type, provider: provider,
                    apiKey: apiKey, baseUrl: baseUrl, model: model, extraParams: extraParams
                ))
            }
        }
        sqlite3_finalize(statement)
        return presets
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
