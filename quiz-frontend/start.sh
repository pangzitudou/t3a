#!/bin/bash
# Windows-compatible startup script for frontend

cd /mnt/e/projects/demos/test-again-and-again/quiz-frontend

# Try to use Windows npm if available
if hash npm.cmd 2>/dev/null; then
    # npm.cmd exists in PATH
    echo "Using Windows npm..."
    npm.cmd run dev
else
    # Use npx to run vite directly
    echo "Using npx vite..."
    npx vite --host > /tmp/frontend.log 2>&1 &
fi

echo "Frontend starting on port 5173..."
echo "Please access: http://localhost:5173"
