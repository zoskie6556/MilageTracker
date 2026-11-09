/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/main/resources/templates/**/*.html", "./src/main/resources/static/js/**/*.js"],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#087f73",
          dark: "#06645b",
          light: "#e5f5f2"
        },
        ink: "#132e35",
        accent: "#f4b740"
      },
      boxShadow: {
        card: "0 12px 35px -22px rgb(19 46 53 / 28%)",
        float: "0 18px 50px -20px rgb(19 46 53 / 35%)"
      }
    }
  },
  plugins: []
};
