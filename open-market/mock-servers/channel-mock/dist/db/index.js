"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.db = void 0;
const sqlite3_1 = __importDefault(require("sqlite3"));
const path_1 = __importDefault(require("path"));
const fs_1 = __importDefault(require("fs"));
const DB_PATH = path_1.default.join(__dirname, '../../data/channel.db');
class Database {
    constructor() {
        this.db = null;
    }
    async connect() {
        return new Promise((resolve, reject) => {
            const dataDir = path_1.default.dirname(DB_PATH);
            if (!fs_1.default.existsSync(dataDir)) {
                fs_1.default.mkdirSync(dataDir, { recursive: true });
            }
            this.db = new sqlite3_1.default.Database(DB_PATH, (err) => {
                if (err) {
                    console.error('Failed to connect to database:', err);
                    reject(err);
                }
                else {
                    console.log('Connected to SQLite database');
                    resolve();
                }
            });
        });
    }
    async init() {
        if (!this.db) {
            throw new Error('Database not connected');
        }
        const schemaPath = path_1.default.join(__dirname, '../../src/db/schema.sql');
        const schema = fs_1.default.readFileSync(schemaPath, 'utf-8');
        return new Promise((resolve, reject) => {
            this.db.exec(schema, (err) => {
                if (err) {
                    console.error('Failed to initialize database:', err);
                    reject(err);
                }
                else {
                    console.log('Database initialized');
                    resolve();
                }
            });
        });
    }
    run(sql, params = []) {
        return new Promise((resolve, reject) => {
            if (!this.db) {
                reject(new Error('Database not connected'));
                return;
            }
            this.db.run(sql, params, function (err) {
                if (err) {
                    reject(err);
                }
                else {
                    resolve({ lastID: this.lastID, changes: this.changes });
                }
            });
        });
    }
    get(sql, params = []) {
        return new Promise((resolve, reject) => {
            if (!this.db) {
                reject(new Error('Database not connected'));
                return;
            }
            this.db.get(sql, params, (err, row) => {
                if (err) {
                    reject(err);
                }
                else {
                    resolve(row);
                }
            });
        });
    }
    all(sql, params = []) {
        return new Promise((resolve, reject) => {
            if (!this.db) {
                reject(new Error('Database not connected'));
                return;
            }
            this.db.all(sql, params, (err, rows) => {
                if (err) {
                    reject(err);
                }
                else {
                    resolve(rows);
                }
            });
        });
    }
    async close() {
        return new Promise((resolve, reject) => {
            if (!this.db) {
                resolve();
                return;
            }
            this.db.close((err) => {
                if (err) {
                    reject(err);
                }
                else {
                    console.log('Database connection closed');
                    this.db = null;
                    resolve();
                }
            });
        });
    }
}
exports.db = new Database();
//# sourceMappingURL=index.js.map