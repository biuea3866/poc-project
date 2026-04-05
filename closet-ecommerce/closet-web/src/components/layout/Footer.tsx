import Link from 'next/link';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div>
            <h3 className="text-white text-lg font-bold mb-4">Closet</h3>
            <p className="text-sm leading-relaxed">
              프리미엄 패션 이커머스 플랫폼.
              <br />
              나만의 스타일을 발견하세요.
            </p>
          </div>

          {/* Shopping */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">쇼핑</h4>
            <ul className="space-y-2 text-sm">
              <li>
                <Link href="/products" className="hover:text-white transition-colors">
                  전체 상품
                </Link>
              </li>
              <li>
                <Link href="/products?sort=popularity,desc" className="hover:text-white transition-colors">
                  인기 상품
                </Link>
              </li>
              <li>
                <Link href="/products?sort=createdAt,desc" className="hover:text-white transition-colors">
                  신상품
                </Link>
              </li>
            </ul>
          </div>

          {/* Customer Service */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">고객센터</h4>
            <ul className="space-y-2 text-sm">
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  자주 묻는 질문
                </span>
              </li>
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  배송 및 반품
                </span>
              </li>
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  1:1 문의하기
                </span>
              </li>
            </ul>
          </div>

          {/* Company */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">회사</h4>
            <ul className="space-y-2 text-sm">
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  소개
                </span>
              </li>
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  이용약관
                </span>
              </li>
              <li>
                <span className="hover:text-white transition-colors cursor-pointer">
                  개인정보처리방침
                </span>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 text-sm text-center">
          &copy; {new Date().getFullYear()} Closet. All rights reserved. | 주식회사 클로젯
        </div>
      </div>
    </footer>
  );
}
