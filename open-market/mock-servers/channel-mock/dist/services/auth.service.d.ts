import { Channel } from '../types';
export declare const authService: {
    createToken(channel: Channel, ttlSeconds?: number): Promise<{
        token: string;
        expiresIn: number;
    }>;
    validateToken(channel: Channel, token: string): Promise<boolean>;
};
//# sourceMappingURL=auth.service.d.ts.map