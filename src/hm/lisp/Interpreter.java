package hm.lisp;

import java.util.*;

import static java.lang.Character.isDigit;

public class Interpreter {
    private Map<Object, Object> globalMemory = new HashMap<>();

    private static class Level {
        private boolean readingList;
        private List<Object> list = new ArrayList();

        private boolean readingSymbol = false;
        private String symbol = "";

        private boolean readingNumber = false;
        private String number = "";

        private boolean readingString = false;
        private String string = "";
    }

    private Stack<Level> levels = new Stack<>();

    private Level currentLevel = new Level();

    public void evaluate(String code) {
        for (int index = 0; index < code.length(); index++) {
            char c = code.charAt(index);
            if (c == '\n') {
            } else if (c == '(') {
                if (currentLevel.readingList) {
                    levels.push(currentLevel);
                    currentLevel = new Level();
                    currentLevel.readingList = true;
                } else {
                    currentLevel.readingList = true;
                }
            } else if (c == ')') {
                if (currentLevel.readingNumber) {
                    currentLevel.readingNumber = false;
                    currentLevel.list.add(Double.parseDouble(currentLevel.number));
                }
                if (currentLevel.readingSymbol) {
                    currentLevel.readingSymbol = false;
                    currentLevel.list.add(currentLevel.symbol.trim());
                }
                currentLevel.readingList = false;

                if (!levels.empty()) {
                    Level previousFrame = levels.pop();
                    previousFrame.list.add(currentLevel.list);
                    currentLevel = previousFrame;
                } else {
                    execute(new ArrayList<>(), currentLevel.list, globalMemory);
                    currentLevel.list.clear();
                }
            } else if (c == '"') {
                if (currentLevel.readingString) {
                    currentLevel.list.add(currentLevel.string);
                    currentLevel.readingString = false;
                } else {
                    currentLevel.readingString = true;
                }
            } else if (currentLevel.readingList) {
                if (currentLevel.readingString) {
                    currentLevel.string += c;
                } else {
                    if (currentLevel.readingSymbol) {
                        if (c == ' ') {
                            currentLevel.readingSymbol = false;
                            currentLevel.list.add(currentLevel.symbol.trim());
                            currentLevel.symbol = "";
                        } else {
                            currentLevel.symbol += c;
                        }
                    } else {
                        if (currentLevel.readingNumber) {
                            if (c == ' ') {
                                currentLevel.readingNumber = false;
                                currentLevel.list.add(Double.parseDouble(currentLevel.number));
                                currentLevel.number = "";
                            } else {
                                currentLevel.number += c;
                            }
                        } else {
                            if (isDigit(c)) {
                                currentLevel.readingNumber = true;
                                currentLevel.number += c;
                            } else if (c != ' ') {
                                currentLevel.readingSymbol = true;
                                currentLevel.symbol += c;
                            }
                        }
                    }
                }
            } else {
                currentLevel.symbol += c;
                if (index + 1 == code.length())
                    System.out.print(globalMemory.get(currentLevel.symbol));
            }
        }
    }

    private Object execute(List<Object> callerList, List<Object> list, Map<Object, Object> memory) {
        if (list.isEmpty()) return null;

        if (memory.containsKey(list.get(0))) {
            Object element = list.get(0);
            list.remove(0);
            list.add(0, memory.get(element));
        }
        if (list.get(0).equals("define")) {
            memory.put(list.get(1), list.get(2));
            return null;
        } else if (list.get(0).equals("print")) {
            Object element = list.get(1);
            if (memory.containsKey(element)) {
                element = memory.get(element);
            } else if (element instanceof List) {
                element = execute(list, (List<Object>) element, memory);
            }
            System.out.print(element);
            return element;
        } else if (list.get(0).equals("+")) {
            double addend;
            double augend;
            if (memory.containsKey(list.get(1)))
                addend = (double) memory.get(list.get(1));
            else
                addend = (double) list.get(1);
            if (memory.containsKey(list.get(2)))
                augend = (double) memory.get(list.get(2));
            else
                augend = (double) list.get(2);
            return addend + augend;
        } else if (list.get(0).equals("if")) {
            Object conditionArgument = list.get(1);
            boolean condition;
            if (conditionArgument instanceof List)
                condition = (boolean) execute(callerList, (List<Object>) conditionArgument, memory);
            else
                condition = (boolean) conditionArgument;
            if (condition) {
                Object trueArgument = list.get(2);
                if (trueArgument instanceof List)
                    return execute(callerList, (List<Object>) trueArgument, memory);
                else
                    return trueArgument;
            } else {
                Object falseArgument = list.get(3);
                if (falseArgument instanceof List)
                    return execute(callerList, (List<Object>) falseArgument, memory);
                else
                    return falseArgument;
            }
        } else if (list.get(0).equals("=")) {
            Object lhs = list.get(1);
            if (memory.containsKey(lhs))
                lhs = memory.get(lhs);
            if (lhs instanceof List)
                lhs = execute(callerList, (List<Object>) lhs, memory);
            Object rhs = list.get(2);
            if (memory.containsKey(rhs))
                rhs = memory.get(rhs);
            if (rhs instanceof List)
                rhs = execute(callerList, (List<Object>) rhs, memory);
            return lhs.equals(rhs);
        } else if (list.get(0).equals("do")) {
            for (int i = 1; i < list.size(); i++) {
                execute(callerList, (List<Object>) list.get(i), memory);
            }
            return null;
        } else if (list.get(0).equals("increment")) {
            return ((double) list.get(1)) + 1;
        } else if (list.get(0).equals("decrement")) {
            return ((double) list.get(1)) - 1;
        } else if (list.get(0).equals("lambda")) {
            Map<Object, Object> localMemory = new HashMap<>(memory);
            List<Object> argumentList = (List<Object>) list.get(1);
            for (int i = 0; i < argumentList.size(); i++) {
                localMemory.put(argumentList.get(i), callerList.get(i + 1));
            }
            Object body = list.get(2);
            if (body instanceof List) {
                return substitute((List<Object>) body, localMemory);
            } else {
                if (localMemory.containsKey(body))
                    return localMemory.get(body);
                else
                    return body;
            }
        } else if (list.get(0) instanceof List) {
            List<Object> function = (List<Object>) list.get(0);
            Object result = execute(list, function, memory);
            if (result instanceof List)
                return execute(callerList, (List<Object>) result, memory);
            else
                return result;

        } else {
            return null;
        }
    }

    private Object substitute(List<Object> body, Map<Object, Object> memory) {
        List<Object> literalBody = new ArrayList<>();
        for (Object element : body) {
            if (element instanceof List) {
                literalBody.add(substitute((List<Object>) element, memory));
            } else if (memory.containsKey(element)) {
                literalBody.add(memory.get(element));
            } else {
                literalBody.add(element);
            }
        }
        return literalBody;
    }
}
