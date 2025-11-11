/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cyber: {
          blue: '#00D9FF',
          cyan: '#00FFC8',
          green: '#39FF14',
          purple: '#BF40BF',
          red: '#FF3366',
          yellow: '#FFCC00',
        },
        bg: {
          dark: '#0A0E14',
          surface: '#1A1F2E',
          elevated: '#2A2F3E',
        }
      },
      fontFamily: {
        mono: ['Monaco', 'Courier New', 'monospace'],
      }
    },
  },
  plugins: [],
}
