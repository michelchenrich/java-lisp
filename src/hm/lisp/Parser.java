package hm.lisp;

import java.util.Stack;

import static java.lang.Character.isDigit;

class Parser {
    private Environment environment;
    private Stack<Frame> frames = new Stack<>();
    private Frame frame = new Frame();

    Parser(Environment environment) {
        this.environment = environment;
    }

    void parse(String code) {
        for (int index = 0; index < code.length(); index++) {
            char c = code.charAt(index);
            if (c == '\n') {
                continue;
            } else if (c == '(') {
                if (frame.readingList) {
                    frames.push(frame);
                    frame = new Frame();
                    frame.readingList = true;
                } else {
                    frame.readingList = true;
                }
            } else if (c == ')') {
                if (frame.readingNumber) {
                    frame.readingNumber = false;
                    frame.list.add(Double.parseDouble(frame.number));
                }
                if (frame.readingSymbol) {
                    frame.readingSymbol = false;
                    frame.list.add(frame.symbol.trim());
                }
                frame.readingList = false;

                if (!frames.empty()) {
                    Frame previousFrame = frames.pop();
                    previousFrame.list.add(frame.list);
                    frame = previousFrame;
                } else {
                    environment.execute(frame.list);
                    frame.list.clear();
                }
            } else if (c == '"') {
                if (frame.readingString) {
                    frame.list.add(frame.string);
                    frame.readingString = false;
                } else {
                    frame.readingString = true;
                }
            } else if (frame.readingList) {
                if (frame.readingString) {
                    frame.string += c;
                } else {
                    if (frame.readingSymbol) {
                        if (c == ' ') {
                            frame.readingSymbol = false;
                            frame.list.add(frame.symbol.trim());
                            frame.symbol = "";
                        } else {
                            frame.symbol += c;
                        }
                    } else {
                        if (frame.readingNumber) {
                            if (c == ' ') {
                                frame.readingNumber = false;
                                frame.list.add(Double.parseDouble(frame.number));
                                frame.number = "";
                            } else {
                                frame.number += c;
                            }
                        } else {
                            if (isDigit(c)) {
                                frame.readingNumber = true;
                                frame.number += c;
                            } else if (c != ' ') {
                                frame.readingSymbol = true;
                                frame.symbol += c;
                            }
                        }
                    }
                }
            } else {
                frame.symbol += c;
                if (index + 1 == code.length())
                    System.out.print(environment.execute(frame.symbol));
            }
        }
    }
}
