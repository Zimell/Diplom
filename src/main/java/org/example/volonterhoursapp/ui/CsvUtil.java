package org.example.volonterhoursapp.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Единый формат CSV для экспорта и импорта.
 * <p>Используется разделитель «;» и в начало файла пишется BOM — так файл
 * корректно открывается в русском Excel (кириллица не превращается в «кракозябры»,
 * строки разбиваются по столбцам). Импорт при этом понимает и «;», и «,».
 */
public final class CsvUtil {

    /** UTF-8 BOM. Пишется первым символом файла, чтобы Excel распознал кодировку. */
    public static final String BOM = "﻿";

    /** Разделитель полей при экспорте. */
    public static final char SEPARATOR = ';';

    private CsvUtil() {}

    /** Экранирует одно поле: оборачивает в кавычки, если внутри есть разделитель, кавычки или перенос строки. */
    public static String field(String s) {
        if (s == null) return "";
        boolean needQuotes = s.indexOf(SEPARATOR) >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!needQuotes) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /** Собирает строку CSV из значений с текущим разделителем. */
    public static String line(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(SEPARATOR);
            sb.append(field(values[i] == null ? "" : values[i].toString()));
        }
        return sb.append('\n').toString();
    }

    /** Определяет разделитель по строке заголовка (поддерживаются «;» и «,»). */
    public static char detectSeparator(String headerLine) {
        if (headerLine == null) return SEPARATOR;
        int semicolons = count(headerLine, ';');
        int commas = count(headerLine, ',');
        return commas > semicolons ? ',' : ';';
    }

    /**
     * Разбирает одну строку CSV с учётом кавычек.
     * @param line   исходная строка (без завершающего перевода строки)
     * @param sep    разделитель полей
     */
    public static List<String> parseLine(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == sep) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** Убирает BOM из начала первой строки файла, если он есть. */
    public static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '﻿') return s.substring(1);
        return s;
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
