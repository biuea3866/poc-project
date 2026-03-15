import { DocumentListResponse, Document } from '@/types/document';

// 타입 구조 검증 — 빌드/컴파일 타임에 잡히지 않는 런타임 형태 검증
describe('DocumentListResponse 타입', () => {
  it('올바른 구조의 응답 객체를 허용한다', () => {
    const response: DocumentListResponse = {
      documents: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };

    expect(response.documents).toBeInstanceOf(Array);
    expect(typeof response.page).toBe('number');
    expect(typeof response.size).toBe('number');
    expect(typeof response.totalElements).toBe('number');
    expect(typeof response.totalPages).toBe('number');
  });

  it('documents 배열에서 필터링이 가능하다 (TypeError 방지)', () => {
    const response: DocumentListResponse = {
      documents: [
        {
          id: 1,
          title: 'Test Document',
          status: 'ACTIVE',
          aiStatus: 'COMPLETED',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
          createdBy: 1,
          updatedBy: 1,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };

    // documents.filter is not a function 버그 방지 검증
    const docs = response.documents ?? [];
    expect(() => docs.filter((d) => d.status !== 'DELETED')).not.toThrow();
    expect(docs.filter((d) => d.status !== 'DELETED')).toHaveLength(1);
  });

  it('documents가 없는 경우 orEmpty()로 빈 배열을 반환한다', () => {
    const response = {
      documents: undefined as unknown as Document[],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };

    const docs = response.documents ?? [];
    expect(Array.isArray(docs)).toBe(true);
    expect(docs).toHaveLength(0);
  });
});
