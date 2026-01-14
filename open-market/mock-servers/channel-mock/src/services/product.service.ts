import { db } from '../db';
import { Channel, ProductRecord } from '../types';

interface CreateProductInput {
  channel: Channel;
  name: string;
  price: number;
  stock: number;
  rawData: Record<string, unknown>;
}

const formatProductId = (channel: Channel, id: number) => {
  switch (channel) {
    case 'ST11':
      return `ST11-PRD-${String(id).padStart(5, '0')}`;
    case 'NAVER_STORE':
      return `${1000000000 + id}`;
    case 'KAKAO_STORE':
      return `KAKAO-PRD-${String(id).padStart(5, '0')}`;
    case 'TOSS_STORE':
      return `TOSS-PRD-${String(id).padStart(5, '0')}`;
    case 'COUPANG':
      return `${2000000000 + id}`;
    default:
      return `${id}`;
  }
};

export const productService = {
  async create(input: CreateProductInput): Promise<ProductRecord> {
    const createdAt = new Date().toISOString();

    const result = await db.run(
      'INSERT INTO products (channel, external_id, name, price, stock, raw_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [
        input.channel,
        '',
        input.name,
        input.price,
        input.stock,
        JSON.stringify(input.rawData),
        createdAt,
      ]
    );

    const externalId = formatProductId(input.channel, result.lastID);
    await db.run('UPDATE products SET external_id = ? WHERE id = ?', [externalId, result.lastID]);

    return {
      id: result.lastID,
      channel: input.channel,
      externalId,
      name: input.name,
      price: input.price,
      stock: input.stock,
      rawData: JSON.stringify(input.rawData),
      createdAt,
    };
  },

  async getByExternalId(channel: Channel, externalId: string): Promise<ProductRecord | undefined> {
    return db.get<ProductRecord>(
      'SELECT id, channel, external_id as externalId, name, price, stock, raw_json as rawData, created_at as createdAt FROM products WHERE channel = ? AND external_id = ? LIMIT 1',
      [channel, externalId]
    );
  },
};
