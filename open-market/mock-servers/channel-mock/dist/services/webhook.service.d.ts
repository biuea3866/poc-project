import { Channel } from '../types';
interface WebhookRecord {
    id: number;
    channel: Channel;
    event_type: string;
    target_url: string;
    created_at: string;
}
interface WebhookEventRecord {
    id: number;
    channel: Channel;
    event_type: string;
    payload_json: string;
    received_at: string;
}
export declare const webhookService: {
    register(channel: Channel, eventTypes: string[], targetUrl: string): Promise<void>;
    list(channel: Channel, eventType?: string): Promise<WebhookRecord[]>;
    trigger(channel: Channel, eventType: string, payload: Record<string, unknown>): Promise<{
        delivered: number;
        failed: number;
        results: ({
            url: string;
            status: number;
            error?: undefined;
        } | {
            url: string;
            status: number;
            error: string;
        })[];
    }>;
    storeReceived(channel: Channel, eventType: string, payload: Record<string, unknown>): Promise<void>;
    listReceived(limit?: number): Promise<WebhookEventRecord[]>;
};
export {};
//# sourceMappingURL=webhook.service.d.ts.map