# latex-exhibition

## About

**latex-exhibition** is a Java library for embedding LaTeX math formulas into Apache POI PowerPoint (`.pptx`) presentations as **native Office Math (OMML) equations** — not images.

Formulas are rendered by PowerPoint/LibreOffice itself, so they scale perfectly, match the document theme, and remain fully editable after the file is opened.

### How it works

The conversion pipeline is:

```
LaTeX string  →  MathML DOM (SnuggleTeX)  →  OMML XML (MML2OMML.XSL / Saxon-HE)  →  PPTX shape (Apache POI)
```

Two insertion modes are supported:

- **Standalone shape** — formula is placed at an absolute position on the slide.
- **Inline paragraph** — formula is embedded inside a text paragraph alongside surrounding text, using `$...$` or `\[...\]` delimiters.

---

## Usage

### 1. Standalone formula on a slide

Place a formula at a specific position (x, y, width, height in points):

```java
XMLSlideShow pptx = new XMLSlideShow();
XSLFSlide slide = pptx.createSlide();

LatexFormulaInserter inserter = new LatexFormulaInserter();
inserter.insert(
    slide,
    "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
    x: 100, y: 200, width: 400, height: 80
);

try (FileOutputStream out = new FileOutputStream("output.pptx")) {
    pptx.write(out);
}
```

The formula string may optionally include display math delimiters:

```java
inserter.insert(slide, "\\[ E = mc^2 \\]", 50, 300, 300, 60);
inserter.insert(slide, "$\\nabla \\cdot \\mathbf{E} = \\frac{\\rho}{\\varepsilon_0}$", 50, 400, 400, 60);
```

---

### 2. Inline formula inside a text paragraph

Use `InlineParagraphAppender` to embed formulas within surrounding text. Formulas are delimited by `$...$`, `\(...\)`, or `\[...\]`:

```java
XSLFTextShape contentBox = slide.getPlaceholder(1);

InlineParagraphAppender appender = new InlineParagraphAppender();
appender.append(
    contentBox,
    "The energy-mass relation $E = mc^2$ changed physics forever."
);
```

Multiple formulas in one string are supported:

```java
appender.append(
    contentBox,
    "Maxwell's equations: $\\nabla \\cdot \\mathbf{E} = \\frac{\\rho}{\\varepsilon_0}$ " +
    "and $\\nabla \\times \\mathbf{B} = \\mu_0 \\mathbf{J} + \\mu_0 \\varepsilon_0 \\frac{\\partial \\mathbf{E}}{\\partial t}$."
);
```

The result is a single `<a:p>` paragraph containing interleaved `<a:r>` text runs and `<a14:m>` equation elements — exactly how PowerPoint stores inline equations natively.

---

### 3. Full example

```java
public static void main(String[] args) throws Exception {
    try (XMLSlideShow pptx = new XMLSlideShow()) {
        XSLFSlideMaster master = pptx.getSlideMasters().get(0);
        XSLFSlideLayout layout = master.getLayout(SlideLayout.TITLE_AND_CONTENT);
        XSLFSlide slide = pptx.createSlide(layout);

        slide.getPlaceholder(0).setText("Schrödinger Equation");

        XSLFTextShape content = slide.getPlaceholder(1);
        content.clearText();

        InlineParagraphAppender appender = new InlineParagraphAppender();
        appender.append(content,
            "The time-dependent Schrödinger equation: " +
            "$\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H} \\Psi$" +
            " — one of the most beautiful equations in physics.");

        try (FileOutputStream out = new FileOutputStream("output.pptx")) {
            pptx.write(out);
        }
    }
}
```

---

## Dependencies

| Library | Purpose |
|---|---|
| [Apache POI](https://poi.apache.org/) `5.3.0` | PPTX creation and XML manipulation |
| [SnuggleTeX](https://github.com/rototor/snuggletex) `1.3.0` | LaTeX → MathML DOM conversion |
| [Saxon-HE](https://www.saxonica.com/) `12.5` | XSLT 2.0 processor for `MML2OMML.XSL` |
| `MML2OMML.XSL` | Microsoft's official MathML → OMML stylesheet (included in resources) |
