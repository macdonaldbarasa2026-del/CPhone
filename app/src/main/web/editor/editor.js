import { closeBrackets, closeBracketsKeymap } from "@codemirror/autocomplete";
import {
  defaultKeymap,
  history,
  historyKeymap,
  indentWithTab,
} from "@codemirror/commands";
import { cpp } from "@codemirror/lang-cpp";
import { css } from "@codemirror/lang-css";
import { go } from "@codemirror/lang-go";
import { html } from "@codemirror/lang-html";
import { java } from "@codemirror/lang-java";
import { javascript } from "@codemirror/lang-javascript";
import { json } from "@codemirror/lang-json";
import { markdown } from "@codemirror/lang-markdown";
import { php } from "@codemirror/lang-php";
import { python } from "@codemirror/lang-python";
import { rust } from "@codemirror/lang-rust";
import { sql } from "@codemirror/lang-sql";
import { xml } from "@codemirror/lang-xml";
import { yaml } from "@codemirror/lang-yaml";
import { shell } from "@codemirror/legacy-modes/mode/shell";
import {
  HighlightStyle,
  bracketMatching,
  defaultHighlightStyle,
  foldGutter,
  foldKeymap,
  indentOnInput,
  indentUnit,
  StreamLanguage,
  syntaxHighlighting,
} from "@codemirror/language";
import { lintGutter, setDiagnostics } from "@codemirror/lint";
import { search, SearchQuery, searchKeymap, setSearchQuery } from "@codemirror/search";
import { Compartment, EditorState } from "@codemirror/state";
import {
  drawSelection,
  dropCursor,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers,
  rectangularSelection,
} from "@codemirror/view";
import { tags } from "@lezer/highlight";

const palettes = {
  one_dark: ["#1e1e2e", "#abb2bf", "#181825", "#282a3e", "#2b2d42", "#528bff", "#c678dd", "#e5c07b", "#61afef", "#98c379", "#d19a66", "#5c6370", "#e06c75"],
  dracula: ["#282a36", "#f8f8f2", "#21222c", "#343746", "#44475a", "#bd93f9", "#ff79c6", "#8be9fd", "#50fa7b", "#f1fa8c", "#bd93f9", "#6272a4", "#ffb86c"],
  monokai: ["#272822", "#f8f8f2", "#1e1f1c", "#3e3d32", "#414339", "#a6e22e", "#f92672", "#66d9ef", "#a6e22e", "#e6db74", "#ae81ff", "#75715e", "#fd971f"],
  tokyo_night: ["#1a1b26", "#c0caf5", "#16161e", "#24283b", "#292e42", "#7aa2f7", "#bb9af7", "#2ac3de", "#7dcfff", "#9ece6a", "#ff9e64", "#565f89", "#f7768e"],
  solarized_dark: ["#002b36", "#839496", "#073642", "#093946", "#164b5a", "#268bd2", "#859900", "#b58900", "#268bd2", "#2aa198", "#d33682", "#586e75", "#cb4b16"],
  github_light: ["#ffffff", "#1f2328", "#f6f8fa", "#f0f6ff", "#d0d7de", "#0969da", "#cf222e", "#8250df", "#0550ae", "#116329", "#953800", "#6e7781", "#cf222e"],
};

const kotlinLanguage = StreamLanguage.define({
  token(stream, state) {
    if (stream.eatSpace()) return null;
    if (stream.match(/\/\/.*/)) return "lineComment";
    if (state.inComment) {
      if (stream.match(/.*?\*\//)) state.inComment = false;
      else stream.skipToEnd();
      return "blockComment";
    }
    if (stream.match(/\/\*.*?\*\//)) return "blockComment";
    if (stream.match(/\/\*/)) {
      state.inComment = true;
      stream.skipToEnd();
      return "blockComment";
    }
    if (stream.match(/"(?:\\.|[^"\\])*"/)) return "string";
    if (stream.match(/'(?:\\.|[^'\\])'/)) return "string";
    if (stream.match(/\b(?:abstract|as|break|by|class|companion|const|constructor|continue|data|do|else|enum|false|final|for|fun|if|import|in|interface|is|lateinit|null|object|open|override|package|private|protected|public|return|sealed|super|suspend|this|throw|true|try|typealias|val|var|when|while)\b/)) return "keyword";
    if (stream.match(/\b(?:Boolean|Byte|Char|Double|Float|Int|Long|Short|String|Unit|Nothing|Any)\b/)) return "typeName";
    if (stream.match(/\b\d+(?:\.\d+)?\b/)) return "number";
    if (stream.match(/[A-Za-z_$][\w$]*(?=\s*\()/)) return "function";
    if (stream.match(/[A-Za-z_$][\w$]*/)) return "variableName";
    stream.next();
    return null;
  },
  startState() {
    return { inComment: false };
  },
});
const shellLanguage = StreamLanguage.define(shell);

function languageFor(id) {
  switch (id) {
    case "PYTHON": return python();
    case "JAVASCRIPT": return javascript({ jsx: true });
    case "TYPESCRIPT": return javascript({ jsx: true, typescript: true });
    case "HTML": return html();
    case "CSS": return css();
    case "JSON": return json();
    case "MARKDOWN": return markdown();
    case "KOTLIN": return kotlinLanguage;
    case "JAVA": return java();
    case "CPP": return cpp();
    case "PHP": return php();
    case "XML": return xml();
    case "YAML": return yaml();
    case "SQL": return sql();
    case "RUST": return rust();
    case "GO": return go();
    case "SHELL": return shellLanguage;
    default: return [];
  }
}

function themeExtensions(config) {
  const index = config.dark ? (config.theme === "github_light" ? "one_dark" : config.theme) : "github_light";
  const [background, foreground, gutter, activeLine, border, cursor, keyword, type, fn, string, number, comment, variable] =
    palettes[index] || palettes.one_dark;
  const cssVars = {
    "--cm-background": background,
    "--cm-foreground": foreground,
    "--cm-gutter": gutter,
    "--cm-active-line": activeLine,
    "--cm-border": border,
    "--cm-cursor": cursor,
    "--cm-selection": config.dark ? "#36588088" : "#0969da33",
    "--cm-font-size": `${config.fontSize || 13.5}px`,
    "--cm-line-height": `${(config.fontSize || 13.5) * (config.lineSpacing || 1.48)}px`,
  };
  const theme = EditorView.theme({
    "&": { color: foreground, backgroundColor: background },
    ".cm-content": { fontFamily: "monospace", fontSize: "var(--cm-font-size)", lineHeight: "var(--cm-line-height)", caretColor: cursor },
    ".cm-gutters": { color: comment, backgroundColor: gutter, borderColor: border },
    ".cm-activeLineGutter": { backgroundColor: activeLine },
    ".cm-cursor, .cm-dropCursor": { borderLeftColor: cursor },
    ".cm-tooltip": { color: foreground, backgroundColor: gutter, borderColor: border },
    ".cm-panels": { color: foreground, backgroundColor: gutter },
  }, { dark: index !== "github_light" });
  const syntax = syntaxHighlighting(makeHighlightStyle(keyword, type, fn, string, number, comment, variable));
  document.documentElement.style.cssText = Object.entries(cssVars).map(([key, value]) => `${key}:${value}`).join(";");
  document.body.style.background = background;
  return [theme, syntax];
}

function makeHighlightStyle(keyword, type, fn, string, number, comment, variable) {
  return HighlightStyle.define([
    { tag: tags.keyword, color: keyword },
    { tag: [tags.typeName, tags.className], color: type },
    { tag: [tags.function(tags.variableName), tags.function(tags.propertyName)], color: fn },
    { tag: [tags.string, tags.special(tags.string)], color: string },
    { tag: tags.number, color: number },
    { tag: tags.comment, color: comment, fontStyle: "italic" },
    { tag: [tags.variableName, tags.propertyName], color: variable },
    { tag: tags.operator, color: fn },
    { tag: [tags.bool, tags.null], color: keyword },
  ]);
}

const lineNumbersCompartment = new Compartment();
const bracketsCompartment = new Compartment();
const activeLineCompartment = new Compartment();
const languageCompartment = new Compartment();
const themeCompartment = new Compartment();
const optionsCompartment = new Compartment();

let view;
let currentDocumentId = "";
let configuredLanguage = "";
let configuredLineNumbers = null;
let configuredBrackets = null;
let configuredActiveLine = null;
let configuredTheme = "";
let configuredOptions = "";
let configuredSearch = "";
let configuredDiagnostics = "";
let latestConfig = {};
let updateTimer;
let pinchStartDistance = 0;
let pinchStartFontSize = 13.5;
let lastFontNotification = 0;

function diagnosticsFor(state, values) {
  return (values || []).flatMap((item) => {
    if (!state.doc.lines) return [];
    const line = state.doc.line(Math.min(Math.max(1, item.line), state.doc.lines));
    const from = Math.min(line.to, line.from + Math.max(0, item.column - 1));
    return [{
      from,
      to: Math.min(line.to, from + 1),
      severity: item.severity === "ERROR" ? "error" : item.severity === "WARNING" ? "warning" : "info",
      message: item.message,
    }];
  });
}

function configure(next) {
  latestConfig = { ...latestConfig, ...next };
  if (!view) return;
  const config = latestConfig;
  const effects = [];
  const languageId = config.language || "PLAINTEXT";
  if (languageId !== configuredLanguage) {
    effects.push(languageCompartment.reconfigure(languageFor(languageId)));
    configuredLanguage = languageId;
  }
  if (config.showLineNumbers !== configuredLineNumbers) {
    effects.push(lineNumbersCompartment.reconfigure(config.showLineNumbers ? lineNumbers() : []));
    configuredLineNumbers = config.showLineNumbers;
  }
  if (config.bracketAutoClose !== configuredBrackets) {
    effects.push(bracketsCompartment.reconfigure(config.bracketAutoClose ? closeBrackets() : []));
    configuredBrackets = config.bracketAutoClose;
  }
  if (config.showActiveLineHighlight !== configuredActiveLine) {
    effects.push(activeLineCompartment.reconfigure(
      config.showActiveLineHighlight ? [highlightActiveLine(), highlightActiveLineGutter()] : [],
    ));
    configuredActiveLine = config.showActiveLineHighlight;
  }
  const themeKey = JSON.stringify([
    config.theme,
    config.dark,
    config.fontSize,
    config.lineSpacing,
  ]);
  if (themeKey !== configuredTheme) {
    effects.push(themeCompartment.reconfigure(themeExtensions(config)));
    configuredTheme = themeKey;
  }
  const options = [
    indentUnit.of(" ".repeat(Math.max(1, Math.min(8, config.tabSize || 4)))),
    ...(config.wordWrap ? [EditorView.lineWrapping] : []),
  ];
  const optionsKey = JSON.stringify([config.tabSize, config.wordWrap]);
  if (optionsKey !== configuredOptions) {
    effects.push(optionsCompartment.reconfigure(options));
    configuredOptions = optionsKey;
  }
  if (effects.length) view.dispatch({ effects });

  if (config.search !== configuredSearch) {
    configuredSearch = config.search || "";
    setSearchQuery(view, new SearchQuery({ search: configuredSearch, caseSensitive: false }));
  }
  const diagnosticsKey = JSON.stringify(config.diagnostics || []);
  if (diagnosticsKey !== configuredDiagnostics) {
    configuredDiagnostics = diagnosticsKey;
    setDiagnostics(view, diagnosticsFor(view.state, config.diagnostics));
  }
}

function createEditor() {
  const extensions = [
    history(),
    bracketMatching(),
    foldGutter(),
    indentOnInput(),
    lintGutter(),
    drawSelection(),
    dropCursor(),
    rectangularSelection(),
    EditorState.allowMultipleSelections.of(true),
    bracketsCompartment.of([]),
    activeLineCompartment.of([]),
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    search(),
    lineNumbersCompartment.of([]),
    languageCompartment.of([]),
    themeCompartment.of([]),
    optionsCompartment.of([]),
    keymap.of([
      indentWithTab,
      ...closeBracketsKeymap,
      ...defaultKeymap,
      ...historyKeymap,
      ...foldKeymap,
      ...searchKeymap,
    ]),
    EditorView.updateListener.of((update) => {
      if (!update.docChanged && !update.selectionSet) return;
      clearTimeout(updateTimer);
      updateTimer = setTimeout(() => {
        const selection = view.state.selection.main;
        window.AndroidEditor?.onEditorUpdate(
          view.state.doc.toString(),
          selection.anchor,
          selection.head,
        );
      }, 80);
    }),
  ];
  view = new EditorView({
    state: EditorState.create({ extensions }),
    parent: document.getElementById("editor"),
  });
  configure(latestConfig);
  window.AndroidEditor?.onEditorReady();
}

window.cphoneConfigure = configure;
window.cphoneSetDocument = (documentId, text, anchor, head) => {
  if (!view) return;
  const newDocument = documentId !== currentDocumentId;
  currentDocumentId = documentId;
  const current = view.state.doc.toString();
  const selection = view.state.selection.main;
  const from = Math.max(0, Math.min(text.length, anchor));
  const to = Math.max(0, Math.min(text.length, head));
  const changes = current === text ? undefined : { from: 0, to: view.state.doc.length, insert: text };
  const selectionChanged = newDocument || selection.anchor !== from || selection.head !== to;
  if (!changes && !selectionChanged) return;
  view.dispatch({
    changes,
    selection: selectionChanged ? { anchor: from, head: to } : undefined,
    effects: selectionChanged ? EditorView.scrollIntoView(to, { y: "center" }) : undefined,
  });
};

document.addEventListener("touchstart", (event) => {
  if (!latestConfig.pinchToZoom || event.touches.length !== 2) return;
  const dx = event.touches[0].clientX - event.touches[1].clientX;
  const dy = event.touches[0].clientY - event.touches[1].clientY;
  pinchStartDistance = Math.hypot(dx, dy);
  pinchStartFontSize = latestConfig.fontSize || 13.5;
}, { passive: true });

document.addEventListener("touchmove", (event) => {
  if (!latestConfig.pinchToZoom || event.touches.length !== 2 || !pinchStartDistance) return;
  const dx = event.touches[0].clientX - event.touches[1].clientX;
  const dy = event.touches[0].clientY - event.touches[1].clientY;
  const distance = Math.hypot(dx, dy);
  const next = Math.max(9, Math.min(26, pinchStartFontSize * distance / pinchStartDistance));
  if (Math.abs(next - (latestConfig.fontSize || 13.5)) < 0.35) return;
  latestConfig.fontSize = next;
  configure({});
  const now = Date.now();
  if (now - lastFontNotification > 120) {
    lastFontNotification = now;
    window.AndroidEditor?.onFontSizeChanged(next);
  }
}, { passive: true });

createEditor();
