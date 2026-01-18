import { NextFunction, Request, Response } from 'express';
import { Channel } from '../types';
export declare function requireAuth(channel: Channel, options?: {
    allowMissing?: boolean;
}): (req: Request, res: Response, next: NextFunction) => Promise<void | Response<any, Record<string, any>>>;
//# sourceMappingURL=auth.d.ts.map