"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const index_1 = require("./index");
async function initDatabase() {
    try {
        await index_1.db.connect();
        await index_1.db.init();
        console.log('Database initialized successfully');
        await index_1.db.close();
        process.exit(0);
    }
    catch (error) {
        console.error('Failed to initialize database:', error);
        process.exit(1);
    }
}
initDatabase();
//# sourceMappingURL=init.js.map