/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#667eea',
          50: '#f5f7ff',
          100: '#eef0ff',
          200: '#ddd9ff',
          300: '#ccc0ff',
          400: '#bba8ff',
          500: '#667eea',
          600: '#4c5fd9',
          700: '#3a4fc7',
          800: '#283fb5',
          900: '#162f93',
        },
        secondary: {
          DEFAULT: '#764ba2',
          50: '#f5f0ff',
          100: '#ebe1ff',
          200: '#e1d2ff',
          300: '#d7c3ff',
          400: '#cdb4ff',
          500: '#a855f7',
          600: '#9333ea',
          700: '#7e22ce',
          800: '#6b21a8',
          900: '#581c87',
        },
        accent: {
          DEFAULT: '#4facfe',
          light: '#00f2fe',
        },
        success: '#51cf66',
        danger: '#ff6b6b',
        warning: '#ffd43b',
        bg: {
          light: '#f8f9ff',
          white: '#ffffff',
        },
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'gradient-secondary': 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'gradient-accent': 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'gradient-hero': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      },
      animation: {
        'pulse-slow': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        pulse: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.3' },
        },
      },
    },
  },
  plugins: [],
}
