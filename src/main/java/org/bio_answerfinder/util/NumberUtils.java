package org.bio_answerfinder.util;

import java.text.NumberFormat;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class NumberUtils {

    protected NumberUtils() {
    }

    public static double getDouble(String numStr) {
        return Double.parseDouble(numStr);
    }

    public static int getInt(String numStr) {
        return Integer.parseInt(numStr);
    }

    public static short getShort(String numStr) {
        return Short.parseShort(numStr);
    }

    public static int toInt(String value, int defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return defaultVal;
        }
    }

    public static float toFloat(String value, float defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException nfe) {
            return defaultVal;
        }
    }

    public static double toDouble(String value, double defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            return defaultVal;
        }
    }

    public static boolean toBoolean(String value, boolean defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")
                || value == "1") {
            return true;
        } else if (value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("no") || value == "0") {
            return false;
        } else {
            return defaultVal;
        }
    }

    public static boolean isAllNumbers(String s) {
        if (s == null)
            return false;
        if (!Character.isDigit(s.charAt(0))) {
            return false;
        }
        char[] carr = s.toCharArray();
        for (int i = 1; i < carr.length; i++) {
            if (!Character.isDigit(carr[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWithNumbers(String s) {
        if (s == null)
            return false;

        if (!Character.isDigit(s.charAt(0))) {
            return false;
        }
        return true;
    }

    public static boolean isPossibleNumber(String s) {
        char c = s.charAt(0);
        if (c == '-' || c == '+' || c == '.') {
            return s.length() > 1 && Character.isDigit(s.charAt(1));
        } else if (Character.isDigit(c)) {
            return true;
        }
        return false;
    }

    public static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static String formatDecimal(double num, int maxFracDigits) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(maxFracDigits);
        return nf.format(num);
    }

    public static boolean isInteger(double nval) {
        double diff = nval - (int) nval;
        return diff < 10e-6;
    }

    public static long getLong(String value) {
        return Long.parseLong(value);
    }

    public static boolean getBool(String value) {
        return "true".equalsIgnoreCase(value);
    }
}
