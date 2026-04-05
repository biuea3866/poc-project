'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

interface NavItem {
  label: string;
  href: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: '전체', href: '/products' },
  { label: '상의', href: '/products?categoryId=1' },
  { label: '하의', href: '/products?categoryId=2' },
  { label: '아우터', href: '/products?categoryId=3' },
  { label: '신발', href: '/products?categoryId=4' },
  { label: '액세서리', href: '/products?categoryId=5' },
];

export default function Nav() {
  const pathname = usePathname();

  return (
    <nav className="bg-white border-b border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex gap-8 overflow-x-auto scrollbar-hide">
          {NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href || pathname + '?' === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'py-3 text-sm whitespace-nowrap border-b-2 transition-colors',
                  isActive
                    ? 'border-black text-black font-semibold'
                    : 'border-transparent text-gray-500 hover:text-gray-900 hover:border-gray-300',
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
