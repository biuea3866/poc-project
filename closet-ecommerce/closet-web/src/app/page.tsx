import Link from 'next/link';

export default function HomePage() {
  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 text-center">
          <h1 className="text-5xl font-bold text-gray-900 mb-4">Closet</h1>
          <p className="text-xl text-gray-600 mb-8">
            Discover your style. Premium fashion curated for you.
          </p>
          <Link
            href="/products"
            className="inline-block bg-black text-white px-8 py-3 rounded-lg font-medium hover:bg-gray-800 transition-colors"
          >
            Shop Now
          </Link>
        </div>
      </section>

      {/* Featured Products Placeholder */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h2 className="text-2xl font-bold text-gray-900 mb-8">Featured Products</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="animate-pulse">
              <div className="aspect-[3/4] bg-gray-200 rounded-lg mb-3" />
              <div className="h-3 bg-gray-200 rounded w-1/3 mb-2" />
              <div className="h-4 bg-gray-200 rounded w-2/3 mb-2" />
              <div className="h-4 bg-gray-200 rounded w-1/4" />
            </div>
          ))}
        </div>
      </section>

      {/* Categories Placeholder */}
      <section className="bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <h2 className="text-2xl font-bold text-gray-900 mb-8">Shop by Category</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {['Tops', 'Bottoms', 'Outerwear', 'Accessories'].map((category) => (
              <div
                key={category}
                className="aspect-square bg-gray-200 rounded-lg flex items-center justify-center"
              >
                <span className="text-lg font-medium text-gray-600">{category}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
