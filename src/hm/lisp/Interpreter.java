package hm.lisp;

import java.util.*;

import static java.lang.Character.isDigit;

public class Interpreter {
    private Map<Object, Object> globalMemory = new HashMap<>();

    private static class StackFrame {
        private boolean readingList;
        private List<Object> list = new ArrayList();

        private boolean readingSymbol = false;
        private String symbol = "";

        private boolean readingNumber = false;
        private String number = "";

        private boolean readingString = false;
        private String string = "";
    }

    private Stack<StackFrame> stack = new Stack<>();


    private StackFrame currentFrame = new StackFrame();

    public void evaluate(String code) {
        for (int index = 0; index < code.length(); index++) {
            char c = code.charAt(index);
            if (c == '\n') {
            } else if (c == '(') {
                if (currentFrame.readingList) {
                    stack.push(currentFrame);
                    currentFrame = new StackFrame();
                    currentFrame.readingList = true;
                } else {
                    currentFrame.readingList = true;
                }
            } else if (c == ')') {
                if (currentFrame.readingNumber) {
                    currentFrame.readingNumber = false;
                    currentFrame.list.add(Double.parseDouble(currentFrame.number));
                }
                if (currentFrame.readingSymbol) {
                    currentFrame.readingSymbol = false;
                    currentFrame.list.add(currentFrame.symbol.trim());
                }
                currentFrame.readingList = false;

                if (!stack.empty()) {
                    StackFrame previousFrame = stack.pop();
                    previousFrame.list.add(currentFrame.list);
                    currentFrame = previousFrame;
                } else {
                    execute(currentFrame.list, globalMemory);
                    currentFrame.list.clear();
                }
            } else if (c == '"') {
                if (currentFrame.readingString) {
                    currentFrame.list.add(currentFrame.string);
                    currentFrame.readingString = false;
                } else {
                    currentFrame.readingString = true;
                }
            } else if (currentFrame.readingList) {
                if (currentFrame.readingString) {
                    currentFrame.string += c;
                } else {
                    if (currentFrame.readingSymbol) {
                        if (c == ' ') {
                            currentFrame.readingSymbol = false;
                            currentFrame.list.add(currentFrame.symbol.trim());
                            currentFrame.symbol = "";
                        } else {
                            currentFrame.symbol += c;
                        }
                    } else {
                        if (currentFrame.readingNumber) {
                            if (c == ' ') {
                                currentFrame.readingNumber = false;
                                currentFrame.list.add(Double.parseDouble(currentFrame.number));
                                currentFrame.number = "";
                            } else {
                                currentFrame.number += c;
                            }
                        } else {
                            if (isDigit(c)) {
                                currentFrame.readingNumber = true;
                                currentFrame.number += c;
                            } else if (c != ' ') {
                                currentFrame.readingSymbol = true;
                                currentFrame.symbol += c;
                            }
                        }
                    }
                }
            } else {
                currentFrame.symbol += c;
                if (index + 1 == code.length())
                    System.out.print(globalMemory.get(currentFrame.symbol));
            }
        }
    }

    private Object execute(List<Object> list, Map<Object, Object> memory) {
        if (list.isEmpty()) return null;

        if (list.get(0).equals("define")) {
            memory.put(list.get(1), list.get(2));
            return null;
        } else if (list.get(0).equals("print")) {
            Object element = list.get(1);
            if (memory.containsKey(element)) {
                element = memory.get(element);
            } else if (element instanceof List) {
                element = execute((List<Object>) element, memory);
            }
            System.out.print(element);
            return element;
        } else if (list.get(0).equals("+")) {
            double addend = (double) memory.get(list.get(1));
            double augend = (double) memory.get(list.get(2));
            return addend + augend;
        } else if (list.get(0).equals("lambda")) {
            List<Object> body = (List<Object>) list.get(2);
            List<Object> substitutedBody = new ArrayList<>();
            for (Object element : body) {
                if (memory.containsKey(element))
                    substitutedBody.add(memory.get(element));
                else
                    substitutedBody.add(element);
            }
            return execute(substitutedBody, memory);
        } else if (list.get(0) instanceof List) {
            List<Object> function = (List<Object>) list.get(0);
            Map<Object, Object> localMemory = new HashMap<>(memory);
            List<Object> argumentList = (List<Object>) function.get(1);
            for (int i = 0; i < argumentList.size(); i++) {
                localMemory.put(argumentList.get(i), list.get(i + 1));
            }
            return execute(function, localMemory);
        } else {
            return null;
        }
    }
}
