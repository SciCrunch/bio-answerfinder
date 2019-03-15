package org.bio_answerfinder.nlp.sentence;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 11/8/16.
 */
public class LexerUtils {

    public static List<char[]> getAbbreviationList() {
        List<char[]> abbrList = new ArrayList<char[]>();
        abbrList.add(new char[]{'v', 's', '.'});
        abbrList.add(new char[]{'i', '.'});
        abbrList.add(new char[]{'i', '.', 'e', '.'});
        abbrList.add(new char[]{'i', '.', 'p', '.'});
        abbrList.add(new char[]{'e', '.'});
        abbrList.add(new char[]{'e', '.', 'g', '.'});
        abbrList.add(new char[]{'U', '.'});
        abbrList.add(new char[]{'U', '.', 'S', '.'});
        abbrList.add(new char[]{'S', '.'});
        abbrList.add(new char[]{'S', '.', 'D', '.'});
        abbrList.add(new char[]{'M', '.'});
        abbrList.add(new char[]{'M', '.', 'E', '.'});
        abbrList.add(new char[]{'d', '.'});
        abbrList.add(new char[]{'d', '.', 'f', '.'});
        abbrList.add(new char[]{'s', '.', 'c', '.'});
        abbrList.add(new char[]{'e', 't', 'c', '.'});
        abbrList.add(new char[]{'e', 't', '.'});
        abbrList.add(new char[]{'c', 'a','t', '.'});
        abbrList.add(new char[]{'C', 'a','t', '.'});
        abbrList.add(new char[]{'n', 'o', '.'});
        abbrList.add(new char[]{'N', 'o', '.'});
        abbrList.add(new char[]{'a','l','.'});
        abbrList.add(new char[]{'D','r','.'});

        return abbrList;
    }

}
