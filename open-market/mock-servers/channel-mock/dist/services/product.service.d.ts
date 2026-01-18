import { Channel, ProductRecord } from '../types';
interface CreateProductInput {
    channel: Channel;
    name: string;
    price: number;
    stock: number;
    rawData: Record<string, unknown>;
}
export declare const productService: {
    create(input: CreateProductInput): Promise<ProductRecord>;
    getByExternalId(channel: Channel, externalId: string): Promise<ProductRecord | undefined>;
};
export {};
//# sourceMappingURL=product.service.d.ts.map