package ru.toshchev.latex.exhibition.formula;

public class LatexConversionException extends Exception {

    public LatexConversionException(String message) {
        super(message);
    }

    public LatexConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
