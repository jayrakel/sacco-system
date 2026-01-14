/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'sans-serif'], // Ensure you use a modern font like Inter or DM Sans
      },
      colors: {
        brand: {
          primary: 'rgb(var(--color-primary) / <alpha-value>)',
          secondary: 'rgb(var(--color-secondary) / <alpha-value>)',
          accent: '#6366f1', // Indigo-500
        }
      },
      boxShadow: {
        // Soft, multi-layered shadows for realistic depth
        'glass': '0 4px 30px rgba(0, 0, 0, 0.1)',
        'glass-hover': '0 20px 40px rgba(0, 0, 0, 0.15)',
        '3d': '0 10px 40px -10px rgba(0,0,0,0.15), 0 0 20px -5px rgba(0,0,0,0.1)',
        '3d-hover': '0 25px 50px -12px rgba(0,0,0,0.25), 0 0 30px -5px rgba(0,0,0,0.15)',
        'inner-light': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.6)', // Top highlight for 3D edge
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'glass-shine': 'linear-gradient(135deg, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 60%)', // The glossy reflection
      },
      animation: {
        blob: "blob 10s infinite",
        'float': "float 6s ease-in-out infinite",
      },
      keyframes: {
        blob: {
          "0%": { transform: "translate(0px, 0px) scale(1)" },
          "33%": { transform: "translate(30px, -50px) scale(1.1)" },
          "66%": { transform: "translate(-20px, 20px) scale(0.9)" },
          "100%": { transform: "translate(0px, 0px) scale(1)" },
        },
        float: {
            "0%, 100%": { transform: "translateY(0)" },
            "50%": { transform: "translateY(-10px)" },
        }
      },
    },
  },
  plugins: [
    require('tailwind-scrollbar-hide') // Ensure this is installed or remove if not using
  ],
}