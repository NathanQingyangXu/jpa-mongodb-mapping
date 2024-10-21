package org.hibernate.omm.util;

import com.mongodb.assertions.Assertions;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public final class StringUtil {
    private StringUtil() {}

    /**
     * Return JSON field value of string type with delimiter escaped
     *
     * @param str string to be escaped (if necessary)
     * @return escaped json string field value with double quotes as delimiters
     * @apiNote copied intact from {@code StrictCharacterStreamJsonWriter}'s private method
     */
    public static String writeStringHelper(final String str) {
        Assertions.notNull("str", str);
        StringBuilder sb = new StringBuilder(str.length() + 2);
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    switch (Character.getType(c)) {
                        case Character.UPPERCASE_LETTER:
                        case Character.LOWERCASE_LETTER:
                        case Character.TITLECASE_LETTER:
                        case Character.OTHER_LETTER:
                        case Character.DECIMAL_DIGIT_NUMBER:
                        case Character.LETTER_NUMBER:
                        case Character.OTHER_NUMBER:
                        case Character.SPACE_SEPARATOR:
                        case Character.CONNECTOR_PUNCTUATION:
                        case Character.DASH_PUNCTUATION:
                        case Character.START_PUNCTUATION:
                        case Character.END_PUNCTUATION:
                        case Character.INITIAL_QUOTE_PUNCTUATION:
                        case Character.FINAL_QUOTE_PUNCTUATION:
                        case Character.OTHER_PUNCTUATION:
                        case Character.MATH_SYMBOL:
                        case Character.CURRENCY_SYMBOL:
                        case Character.MODIFIER_SYMBOL:
                        case Character.OTHER_SYMBOL:
                            sb.append(c);
                            break;
                        default:
                            sb.append("\\u");
                            sb.append(Integer.toHexString((c & 0xf000) >> 12));
                            sb.append(Integer.toHexString((c & 0x0f00) >> 8));
                            sb.append(Integer.toHexString((c & 0x00f0) >> 4));
                            sb.append(Integer.toHexString(c & 0x000f));
                            break;
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
