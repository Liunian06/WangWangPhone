import Foundation
import SQLite3

struct PersonaRecord {
    var name: String = ""
    var gender: String = ""
    var age: String = ""
    var personality: String = ""
    var background: String = ""
    var appearance: String = ""
    var occupation: String = ""
    var hobbies: String = ""
    var relationships: String = ""
    var goals: String = ""
    var speechStyle: String = ""
    var specialTraits: String = ""
}

class PersonaDbHelper {
    private var db: OpaquePointer?
    
    init() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("persona.db")
        
        if sqlite3_open(fileURL.path, &db) == SQLITE_OK {
            createTable()
        }
    }
    
    deinit {
        sqlite3_close(db)
    }
    
    private func createTable() {
        let createTableSQL = """
        CREATE TABLE IF NOT EXISTS persona (
            id INTEGER PRIMARY KEY,
            name TEXT,
            gender TEXT,
            age TEXT,
            personality TEXT,
            background TEXT,
            appearance TEXT,
            occupation TEXT,
            hobbies TEXT,
            relationships TEXT,
            goals TEXT,
            speechStyle TEXT,
            specialTraits TEXT
        )
        """
        
        var createTableStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, createTableSQL, -1, &createTableStatement, nil) == SQLITE_OK {
            if sqlite3_step(createTableStatement) == SQLITE_DONE {
                print("Persona table created.")
            }
        }
        sqlite3_finalize(createTableStatement)
    }
    
    func savePersona(_ persona: PersonaRecord) {
        let deleteSQL = "DELETE FROM persona"
        var deleteStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, deleteSQL, -1, &deleteStatement, nil) == SQLITE_OK {
            sqlite3_step(deleteStatement)
        }
        sqlite3_finalize(deleteStatement)
        
        let insertSQL = """
        INSERT INTO persona (name, gender, age, personality, background, appearance, occupation, hobbies, relationships, goals, speechStyle, specialTraits)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        
        var insertStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, insertSQL, -1, &insertStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(insertStatement, 1, (persona.name as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 2, (persona.gender as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 3, (persona.age as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 4, (persona.personality as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 5, (persona.background as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 6, (persona.appearance as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 7, (persona.occupation as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 8, (persona.hobbies as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 9, (persona.relationships as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 10, (persona.goals as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 11, (persona.speechStyle as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 12, (persona.specialTraits as NSString).utf8String, -1, nil)
            
            if sqlite3_step(insertStatement) == SQLITE_DONE {
                print("Persona saved successfully.")
            }
        }
        sqlite3_finalize(insertStatement)
    }
    
    func getPersona() -> PersonaRecord? {
        let querySQL = "SELECT * FROM persona LIMIT 1"
        var queryStatement: OpaquePointer?
        var persona: PersonaRecord?
        
        if sqlite3_prepare_v2(db, querySQL, -1, &queryStatement, nil) == SQLITE_OK {
            if sqlite3_step(queryStatement) == SQLITE_ROW {
                persona = PersonaRecord(
                    name: String(cString: sqlite3_column_text(queryStatement, 1)),
                    gender: String(cString: sqlite3_column_text(queryStatement, 2)),
                    age: String(cString: sqlite3_column_text(queryStatement, 3)),
                    personality: String(cString: sqlite3_column_text(queryStatement, 4)),
                    background: String(cString: sqlite3_column_text(queryStatement, 5)),
                    appearance: String(cString: sqlite3_column_text(queryStatement, 6)),
                    occupation: String(cString: sqlite3_column_text(queryStatement, 7)),
                    hobbies: String(cString: sqlite3_column_text(queryStatement, 8)),
                    relationships: String(cString: sqlite3_column_text(queryStatement, 9)),
                    goals: String(cString: sqlite3_column_text(queryStatement, 10)),
                    speechStyle: String(cString: sqlite3_column_text(queryStatement, 11)),
                    specialTraits: String(cString: sqlite3_column_text(queryStatement, 12))
                )
            }
        }
        sqlite3_finalize(queryStatement)
        return persona
    }
}
