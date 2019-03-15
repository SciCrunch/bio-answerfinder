package org.bio_answerfinder.util;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Assertion {
    public static void assertEquals(int value1, int value2) {
        if (value1 != value2)
            throw new RuntimeException("First Value " + value1
                    + " is not equal to second value " + value2);
    }

    public static void assertEquals(int value1, int value2, String msg) {
        if (value1 != value2)
            throw new RuntimeException("First Value " + value1
                    + " is not equal to second value " + value2 + "\n" + msg);
    }

    public static void assertNotNull(Object o) {
        if (o == null)
            throw new RuntimeException("The object was null");
    }

    public static void assertNotNull(Object o, String objName) {
        if (o == null)
            throw new RuntimeException("The object '" + objName + "' was null");
    }

    public static void assertExistingPath(Object o, String objName) {
        if (o == null)
            throw new RuntimeException("The path '" + objName + "' was null");
        if (o instanceof String) {
            if (!new File((String) o).exists())
                throw new RuntimeException("The path '" + objName
                        + "' does not exists!:" + o);
        } else if (o instanceof File) {
            if (!((File) o).exists())
                throw new RuntimeException("The path '" + objName
                        + "' does not exists!:" + o);
        } else {
            throw new RuntimeException("Not a valid path:" + o);
        }
    }

    public static void assertNotEmpty(Object o, String msg) {
        if (o == null)
            throw new RuntimeException("The object was null:" + msg);
        if (o instanceof String) {
            if (((String) o).trim().length() == 0)
                throw new RuntimeException("The string was empty:" + msg);
        } else if (o instanceof Collection<?>) {
            if (((Collection<?>) o).isEmpty())
                throw new RuntimeException("The collection was empty:" + msg);
        }
    }

    public static void assertTrue(boolean expr) {
        if (!expr) {
            throw new RuntimeException(
                    "The expression evaluates to false! Expected:true");
        }
    }

    public static void assertTrue(boolean expr, String msg) {
        if (!expr) {
            if (msg == null)
                throw new RuntimeException(
                        "The expression evaluates to false! Expected:true");
            else
                throw new RuntimeException(msg);
        }
    }

    public static <T> void assertAllUnique(List<T> list) {
        Set<T> uniqSet = new HashSet<T>(list);
        if (uniqSet.size() != list.size()) {
            throw new RuntimeException("The list has " + uniqSet.size()
                    + " unique items, but " + list.size() + " items in total!");
        }
    }
}
