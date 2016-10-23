package hm.lisp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Environment {
    private Map globalMemory = new HashMap();

    Object execute(Object element) {
        if (globalMemory.containsKey(element)) {
            element = globalMemory.get(element);
        }
        if (element instanceof List) {
            List list = (List) element;
            if (isLambdaCall(list)) {
                Object element1 = reduceLambda(list);
                return execute(element1);
            } else if (isLambda(list)) {
                return list;
            } else if (isDefinition(list)) {
                return define(list);
            } else if (isConditional(list)) {
                return condition(list);
            } else if (isSequential(list)) {
                return runInSequence(list);
            } else {
                for (int i = list.size() - 1; i > -1; i--) {
                    Object argument = list.get(i);
                    list.remove(i);
                    list.add(i, execute(argument));
                }
                if (isPrimitive(list)) {
                    return executePrimitive(list);
                } else {
                    return execute(reduceLambda(list));
                }
            }
        } else {
            return element;
        }
    }

    private boolean isSequential(List list) {
        return list.get(0).equals("do") && list.size() > 2;
    }

    private boolean isConditional(List list) {
        return list.get(0).equals("if") && list.size() == 4;
    }

    private boolean isLambda(List list) {
        return list.get(0).equals("lambda") && list.size() == 3;
    }

    private boolean isDefinition(List list) {
        return list.get(0).equals("define") && list.size() == 3;
    }

    private boolean isLambdaCall(List list) {
        try {
            Object operator = list.get(0);
            return operator instanceof List && ((List) list.get(0)).get(0).equals("lambda");
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object reduceLambda(List list) {
        List lambda = (List) list.get(0);
        Map valueMap = mapValues(list, lambda);
        return reduceLambdaBody(lambda.get(2), valueMap);
    }

    private Map mapValues(List list, List lambda) {
        List arguments = (List) lambda.get(1);
        Map<Object, Object> values = new HashMap<>();
        for (int i = 0; i < arguments.size(); i++) {
            values.put(arguments.get(i), list.get(i + 1));
        }
        return values;
    }

    private Object reduceLambdaBody(Object body, Map values) {
        if (body instanceof List)
            return reduceExpression((List) body, values);
        else
            return values.containsKey(body) ? values.get(body) : body;
    }

    private Object reduceScopedLambda(List lambda, Map values) {
        List arguments = (List) lambda.get(1);
        Object body = lambda.get(2);

        List scopedLambda = new ArrayList();
        scopedLambda.add(lambda.get(0));
        scopedLambda.add(arguments);
        scopedLambda.add(scopeLambdaBody(values, arguments, body));
        return scopedLambda;
    }

    private Object scopeLambdaBody(Map values, List arguments, Object body) {
        if (body instanceof List)
            return reduceExpression((List) body, scopeValues(values, arguments));
        else
            return body;
    }

    private Map scopeValues(Map values, List arguments) {
        Map scopedValues = new HashMap(values);
        for (Object argument : arguments)
            scopedValues.remove(argument);
        return scopedValues;
    }

    private Object reduceExpression(List expression, Map values) {
        List reduced = new ArrayList();
        for (int j = 0; j < expression.size(); j++) {
            Object element = expression.get(j);
            if (values.containsKey(element)) {
                element = values.get(element);
            } else if (element instanceof List) {
                List subexpression = (List) element;
                if (isLambda(subexpression)) {
                    element = reduceScopedLambda(subexpression, values);
                } else {
                    element = reduceExpression(subexpression, values);
                }
            }
            reduced.add(element);
        }
        return reduced;
    }

    private Object executePrimitive(List list) {
        switch ((String) list.get(0)) {
            case "print":
                return print(list);
            case "increment":
                return increment(list);
            case "decrement":
                return decrement(list);
            case "+":
                return plus(list);
            case "=":
                return equal(list);
            default:
                return null;
        }
    }

    private Object equal(List list) {
        return list.get(1).equals(list.get(2));
    }

    private Object runInSequence(List list) {
        for (int i = 1; i < list.size(); i++)
            execute(list.get(i));
        return null;
    }

    private Object condition(List list) {
        return ((boolean) execute(list.get(1)) ? execute(list.get(2)) : execute(list.get(3)));
    }

    private Object plus(List list) {
        return (double) list.get(1) + (double) list.get(2);
    }

    private Object decrement(List list) {
        return ((double) list.get(1)) - 1;
    }

    private Object increment(List list) {
        return ((double) list.get(1)) + 1;
    }

    private Object print(List list) {
        System.out.print(list.get(1));
        return list.get(1);
    }

    private Object define(List list) {
        globalMemory.put(list.get(1), list.get(2));
        return null;
    }

    private boolean isPrimitive(List list) {
        Object operator = list.get(0);
        return operator.equals("print") ||
               operator.equals("increment") ||
               operator.equals("decrement") ||
               operator.equals("+") ||
               operator.equals("=");
    }
}
