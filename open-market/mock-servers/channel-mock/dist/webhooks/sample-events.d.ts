import { Channel } from '../types';
export declare const channelEventSamples: Record<Channel, Record<string, Record<string, unknown>>>;
export declare const getEventTypes: (channel: Channel) => string[];
export declare const getSamplePayload: (channel: Channel, eventType: string) => Record<string, unknown>;
//# sourceMappingURL=sample-events.d.ts.map