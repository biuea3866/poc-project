import { Channel } from '../types';
interface WebhookRouterOptions {
    requireAuth?: (req: any, res: any, next: any) => void;
}
export declare function createWebhookRouter(channel: Channel, options?: WebhookRouterOptions): import("express-serve-static-core").Router;
export {};
//# sourceMappingURL=webhook-management.d.ts.map