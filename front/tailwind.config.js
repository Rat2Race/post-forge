/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#fff8f0',
          100: '#ffe8d6',
          200: '#ffd1ad',
          300: '#ffb380',
          400: '#ff8f4d',
          500: '#ff6f0f',
          600: '#f55d00',
          700: '#cc4900',
          800: '#a33a00',
          900: '#7a2d00',
        },
        carrot: {
          light: '#ffe8d6',
          DEFAULT: '#ff6f0f',
          dark: '#cc4900',
        }
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
