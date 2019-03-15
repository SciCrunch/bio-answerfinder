package org.bio_answerfinder.util;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by bozyurt on 2/25/19.
 */
public class GenUtils {
    /**
     * Given a long string and a maximum number of characters per line, format
     * the given string into multiple lines having at most
     * <code>maxLineLen</code> of size.
     *
     * @param str
     * @param maxLineLen
     * @return line wrapped format of the passed string
     */
    public static String formatText(String str, int maxLineLen) {
        if (str.length() <= maxLineLen) {
            return str;
        }
        StringBuilder buf = new StringBuilder(str.length() + 10);
        StringTokenizer stok = new StringTokenizer(str);
        int count = 0;
        boolean first = true;
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken();
            int tokLen = tok.length();
            if (count + tokLen + 1 > maxLineLen) {
                buf.append("\n");
                buf.append(tok);
                count = tokLen;
            } else {
                if (first) {
                    buf.append(tok);
                    first = false;
                    count += tokLen;
                } else {
                    buf.append(' ').append(tok);
                    count += tokLen + 1;
                }
            }
        }
        return buf.toString();
    }

    public static String join(List<?> list, String delimiter) {
        StringBuilder buf = new StringBuilder(200);
        for (Iterator<?> iter = list.iterator(); iter.hasNext(); ) {
            Object obj = (Object) iter.next();
            buf.append(obj.toString());
            if (iter.hasNext())
                buf.append(delimiter);
        }
        return buf.toString();
    }

    public static String join(String[] arr, String delimiter) {
        StringBuilder buf = new StringBuilder(200);
        for (int i = 0; i < arr.length; i++) {
            buf.append(arr[i]);
            if ((i + 1) < arr.length) {
                buf.append(delimiter);
            }
        }
        return buf.toString();
    }

    public static boolean isEmpty(String text) {
        return text == null || text.trim().length() == 0;
    }

    public static boolean isEmpy(String text) {
        return text == null || text.length() == 0;
    }

    public static void serialize(Object o, String filename) throws IOException {
        Assertion.assertNotNull(o);
        filename = FileUtils.adjustPath(filename);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(filename), 4096));
            out.writeObject(o);
        } finally {
            FileUtils.close(out);
        }
    }

    /**
     * Resurrects a serialized object.
     *
     * @param filename the filename from which the serialized object is read
     * @return the deserialized object.
     * @throws IOException
     * @throws java.lang.ClassNotFoundException
     */
    public static Object deserialize(String filename) throws IOException,
            ClassNotFoundException {
        assert (filename != null);
        filename = FileUtils.adjustPath(filename);
        assert (new File(filename).isFile());
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(filename), 4096));
            Object o = in.readObject();
            return o;
        } finally {
            FileUtils.close(in);
        }
    }

    public static boolean askYesNoQuestion(String questStr) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(
                System.in));
        System.out.print(GenUtils.formatText(questStr, 80));
        System.out.print(" >>");
        String ans = console.readLine();
        return (ans.trim().equals("y") || ans.trim().equals("yes"));
    }

    public static String askQuestion(String questStr) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(
                System.in));
        System.out.print(GenUtils.formatText(questStr, 80));
        System.out.print(" >>");
        String ans = console.readLine().trim();
        return ans;
    }

    public static void getPapers(File journalDir, List<File> paperList) {
        File[] files = journalDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                getPapers(f, paperList);
            } else {
                if (f.getName().endsWith(".nxml")) {
                    paperList.add(f);
                }
            }
        }
    }
}
