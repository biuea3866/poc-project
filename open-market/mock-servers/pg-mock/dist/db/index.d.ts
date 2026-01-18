declare class Database {
    private db;
    connect(): Promise<void>;
    init(): Promise<void>;
    run(sql: string, params?: any[]): Promise<{
        lastID: number;
        changes: number;
    }>;
    get<T>(sql: string, params?: any[]): Promise<T | undefined>;
    all<T>(sql: string, params?: any[]): Promise<T[]>;
    close(): Promise<void>;
}
export declare const db: Database;
export {};
//# sourceMappingURL=index.d.ts.map