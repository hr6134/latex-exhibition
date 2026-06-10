# latex-exhibition

## About

**latex-exhibition** is a Java library for generating Apache POI PowerPoint (`.pptx`) presentations from per-slide PPTX templates, with support for **Markdown content** and **LaTeX math formulas** rendered as native Office Math (OMML) equations — not images.

Formulas are rendered by PowerPoint/Keynote/LibreOffice itself, so they scale perfectly, match the document theme, and remain fully editable after the file is opened.

### How it works

The overall pipeline is:

```
Single-slide .pptx templates  +  Map<token, Markdown+LaTeX>
        ↓
  PresentationBuilder (fills placeholders, merges slides at OPC level)
        ↓
  Multi-slide .pptx output
```

The LaTeX conversion pipeline inside each slide is:

```
LaTeX string  →  MathML DOM (SnuggleTeX)  →  OMML XML (MML2OMML.XSL / Saxon-HE)  →  PPTX shape (Apache POI)
```

---

## Usage

### 1. Slide-by-slide builder (primary API)

Create a template `.pptx` in PowerPoint or Keynote for each slide type. Inside each template, add text boxes whose **entire text** is a placeholder token, e.g. `{title}`, `{leftColumn}`, `{rightColumn}`.

Then build the presentation slide by slide:

```java
Map<String, String> slide1 = Map.of(
    "{title}", "Quantum Mechanics",
    "{leftColumn}", """
        ## Key equations

        The **Schrödinger equation**:
        $\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H}\\Psi$

        - **Energy-mass**: $E = mc^2$
        """,
    "{rightColumn}", """
        | Constant | Symbol | Value |
        |---|---|---|
        | Speed of light | c | 3×10⁸ m/s |
        | Planck constant | h | 6.626×10⁻³⁴ J·s |
        """
);

Map<String, String> slide2 = Map.of(
    "{title}", "Code Example",
    "{body}", """
        ```python
        E = m * c**2
        ```
        """
);

try (FileInputStream t1  = new FileInputStream("slide1-template.pptx");
     FileInputStream t2  = new FileInputStream("slide2-template.pptx");
     FileOutputStream out = new FileOutputStream("output.pptx")) {

    LatexExhibition.newPresentation()
        .addSlide(t1, slide1)
        .addSlide(t2, slide2)
        .writeTo(out);
}
```

- Each template must contain **exactly one slide**.
- Placeholder tokens must be the **sole text** of their text box — no surrounding characters.
- Each `addSlide` call accepts a separate template, so each slide can have a completely different layout or theme.
- Slides are merged at the OPC package level, preserving each slide's original master and layout — the output is a valid `.pptx` that opens correctly in PowerPoint, Keynote, and Google Slides.

### Supported Markdown content

Each placeholder value is rendered as Markdown+LaTeX:

| Syntax | Result |
|---|---|
| `**bold**`, `*italic*`, `~~strike~~` | Formatted text runs |
| `# Heading` … `###### Heading` | Headings with scaled font size |
| `- item` / `1. item` | Bullet and numbered lists |
| `` `inline code` `` | Monospaced run |
| ` ```lang … ``` ` | Code block (Courier New) |
| `\| col \| … \|` | Table shape |
| `$...$` or `\[...\]` | Inline/display LaTeX formula → native OMML |

---

## Dependencies

| Library | Purpose |
|---|---|
| [Apache POI](https://poi.apache.org/) `5.3.0` | PPTX creation and XML manipulation |
| [SnuggleTeX](https://github.com/rototor/snuggletex) `1.3.0` | LaTeX → MathML DOM conversion |
| [Saxon-HE](https://www.saxonica.com/) `12.5` | XSLT 2.0 processor for `MML2OMML.XSL` |
| `MML2OMML.XSL` | Microsoft's official MathML → OMML stylesheet (included in resources) |
