/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#047857',
          hover: '#065f46',
          soft: '#CCFBF1',
          50: '#F0FDFA',
          100: '#CCFBF1',
          200: '#99F6E4',
          300: '#5EEAD4',
          400: '#2DD4BF',
          500: '#14B8A6',
          600: '#0D9488',
          700: '#0F766E',
          800: '#115E59',
          900: '#134E4A',
        },
        accent: {
          DEFAULT: '#0D9488',
          soft: '#CCFBF1',
        },
      },
      boxShadow: {
        card: '0 8px 40px rgba(0, 0, 0, 0.12)',
        'card-hover': '0 12px 48px rgba(4, 120, 87, 0.15)',
        'btn-primary': '0 10px 32px rgba(2, 44, 34, 0.5)',
      },
      fontFamily: {
        sans: ['Inter', 'PingFang SC', 'Microsoft YaHei', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
