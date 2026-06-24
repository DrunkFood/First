package com.jy.eleaitender.ai.processor.prompt;

/**
 * Escapes literal braces before passing business text to Spring AI PromptTemplate.
 */
public final class PromptTemplateEscaper {

    private PromptTemplateEscaper() {
    }

    public static String escapeBraces(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("{", "\\{").replace("}", "\\}");
    }
}
