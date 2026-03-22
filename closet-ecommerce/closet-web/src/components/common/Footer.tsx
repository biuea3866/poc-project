export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="text-white text-lg font-bold mb-4">Closet</h3>
            <p className="text-sm">Your premium fashion destination.</p>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">Customer Service</h4>
            <ul className="space-y-2 text-sm">
              <li>FAQ</li>
              <li>Shipping & Returns</li>
              <li>Contact Us</li>
            </ul>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold mb-4">Company</h4>
            <ul className="space-y-2 text-sm">
              <li>About Us</li>
              <li>Terms of Service</li>
              <li>Privacy Policy</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-8 pt-8 text-sm text-center">
          &copy; 2026 Closet. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
