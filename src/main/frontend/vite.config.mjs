import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import springBoot, {
  BuildSystem,
} from "@wim.deblauwe/vite-plugin-spring-boot";

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)));

export default defineConfig({
  plugins: [
    springBoot({
      buildSystem: BuildSystem.Gradle,
      fullCopyFilePaths: {
        include: ["**/*.html", "**/*.svg"],
        exclude: ["build/**", "node_modules/**", ".vite/**", ".git/**"],
      },
    }),
    tailwindcss(),
  ],
  root: frontendRoot,
  base: "/",
  server: {
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    watch: {
      usePolling: true,
    },
  },
  build: {
    outDir: path.resolve(frontendRoot, "../../../build/vite"),
    emptyOutDir: true,
    manifest: true,
    rollupOptions: {
      input: path.resolve(frontendRoot, "src/main.js"),
    },
  },
});
