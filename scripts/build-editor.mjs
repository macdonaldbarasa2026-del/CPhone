import { build } from "esbuild";

await build({
  entryPoints: ["app/src/main/web/editor/editor.js"],
  bundle: true,
  format: "iife",
  target: "es2020",
  outfile: "app/build/generated/editorAssets/editor/editor.js",
});
