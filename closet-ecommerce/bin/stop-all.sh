#!/bin/bash
# Stop all Closet services
echo "Stopping all Closet services..."
pkill -f "closet-.*\.jar" 2>/dev/null || true
echo "Done."
