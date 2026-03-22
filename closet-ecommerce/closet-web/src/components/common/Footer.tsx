export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="text-white text-lg font-bold mb-4">Closet</h3>
            <p className="text-sm">프리미엄 패션 이커머스</p>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">고객센터</h4>
            <ul className="space-y-2 text-sm">
              <li>자주 묻는 질문</li>
              <li>배송 및 반품</li>
              <li>문의하기</li>
            </ul>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">회사</h4>
            <ul className="space-y-2 text-sm">
              <li>소개</li>
              <li>이용약관</li>
              <li>개인정보처리방침</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-8 pt-8 text-sm text-center">
          &copy; 2026 Closet. All rights reserved. | 주식회사 클로젯
        </div>
      </div>
    </footer>
  );
}
