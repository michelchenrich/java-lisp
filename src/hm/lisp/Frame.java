package hm.lisp;

import java.util.ArrayList;
import java.util.List;

class Frame {
    boolean readingList;
    List list = new ArrayList();

    boolean readingSymbol = false;
    String symbol = "";

    boolean readingNumber = false;
    String number = "";

    boolean readingString = false;
    String string = "";
}
