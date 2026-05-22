/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: 'var(--pf-primary)',
        'primary-hover': 'var(--pf-primary-hover)',
        'primary-soft': 'var(--pf-primary-soft)',
        secondary: 'var(--pf-primary-hover)',
        accent: 'var(--pf-primary)',
        danger: '#EF4444',
        success: 'var(--pf-primary)',
        warning: '#F59E0B',
        info: '#6B7280',
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
      },
    },
  },
  plugins: [],
}
