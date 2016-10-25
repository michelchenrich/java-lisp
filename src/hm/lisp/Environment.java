package hm.lisp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

class Environment {
    private Map globals = new HashMap();

    Object execute(Object element) {
        return execute(element, globals);
    }

    private Object execute(Object element, Map memory) {
        element = reduceElement(element, memory);
        if (isExpression(element))
            return executeExpression((List) element, memory);
        else
            return element;
    }

    private void debug(Object element) {
        System.err.println(element.toString()
                               .replaceAll("\\[", "(")
                               .replaceAll("\\]", ")")
                               .replaceAll(", ", " "));
    }

    private Object executeExpression(List list, Map memory) {
        if (isLambdaCall(list))
            return execute(reduceLambda(list), memory);
        else if (isLambda(list))
            return list;
        else if (isDefinition(list))
            return define(list, memory);
        else if (isLetExpression(list))
            return scopedExpression(list, memory);
        else if (isConditional(list))
            return condition(list, memory);
        else if (isSequential(list))
            return runInSequence(list, memory);
        else
            return function(list, memory);
    }

    private Object scopedExpression(List list, Map memory) {
        return execute(list.get(2), scopedMemory(list, memory));
    }

    private Map scopedMemory(List list, Map memory) {
        List<List> definitions = (List) list.get(1);
        Map localMemory = new HashMap(memory);
        for (List definition : definitions)
            localMemory.put(definition.get(0), execute(definition.get(1), localMemory));
        return localMemory;
    }

    private boolean isLetExpression(List list) {
        return list.size() == 3 &&
               list.get(0).equals("let") &&
               isExpression(list.get(1));
    }

    private Object function(List list, Map memory) {
        reduceArguments(list, memory);
        if (isPrimitive(list)) {
            return executePrimitive(list);
        } else {
            return execute(reduceLambda(list), memory);
        }
    }

    private void reduceArguments(List list, Map memory) {
        for (int i = list.size() - 1; i > -1; i--) {
            Object argument = list.get(i);
            list.remove(i);
            list.add(i, execute(argument, memory));
        }
    }

    private Object reduceElement(Object element, Map memory) {
        if (!isExpression(element) && memory.containsKey(element))
            return memory.get(element);
        else
            return element;
    }

    private boolean isExpression(Object element) {
        return element instanceof List;
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
            return isExpression(operator) && ((List) list.get(0)).get(0).equals("lambda");
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object reduceLambda(List list) {
        Object operator = list.get(0);
        if (!isExpression(operator))
            throw new RuntimeException(format("Undefined operator %s", operator));
        List lambda = (List) operator;
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
        if (isExpression(body)) {
            return reduceExpression((List) body, values);
        } else
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
        if (isExpression(body))
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
        if (isLetExpression(expression))
            return reduceLetExpression(expression, values);
        else {
            List reduced = new ArrayList();
            for (Object element : expression) {
                if (values.containsKey(element)) {
                    element = values.get(element);
                } else if (isExpression(element)) {
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
    }

    private Object reduceLetExpression(List letExpression, Map values) {
        Map scopedValues = new HashMap(values);

        List expression = new ArrayList();
        expression.add(letExpression.get(0));

        List definitions = new ArrayList();
        expression.add(definitions);

        for (List definition : (List<List>) letExpression.get(1)) {
            Object symbol = definition.get(0);
            Object body = definition.get(1);
            if (isExpression(body))
                body = reduceExpression((List) body, scopedValues);
            else if (scopedValues.containsKey(body))
                body = scopedValues.get(body);

            List reducedDefinition = new ArrayList();
            reducedDefinition.add(symbol);
            reducedDefinition.add(body);
            definitions.add(reducedDefinition);

            if (scopedValues.containsKey(symbol))
                scopedValues.remove(symbol);
        }

        expression.add(reduceExpression((List) letExpression.get(2), scopedValues));
        return expression;
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
            case "*":
                return multiply(list);
            case "=":
                return equal(list);
            default:
                return null;
        }
    }

    private Object equal(List list) {
        return list.get(1).equals(list.get(2));
    }

    private Object runInSequence(List list, Map memory) {
        for (int i = 1; i < list.size(); i++)
            execute(list.get(i), memory);
        return null;
    }

    private Object condition(List list, Map memory) {
        return ((boolean) execute(list.get(1), memory) ?
            execute(list.get(2), memory) :
            execute(list.get(3), memory));
    }

    private Object multiply(List list) {
        return (double) list.get(1) * (double) list.get(2);
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
        Object argument = list.get(1);
        if (isExpression(argument))
            System.out.print(argument.toString()
                                 .replaceAll("\\[", "(")
                                 .replaceAll("\\]", ")")
                                 .replaceAll(", ", " "));
        else
            System.out.print(argument);
        return list.get(1);
    }

    private Object define(List list, Map memory) {
        Object identifier = list.get(1);
        Object definition = list.get(2);
        if ((isExpression(identifier))) {
            List functionDefinition = (List) identifier;
            List arguments = new ArrayList();
            for (int i = 1; i < functionDefinition.size(); i++)
                arguments.add(functionDefinition.get(i));

            List lambda = new ArrayList();
            lambda.add("lambda");
            lambda.add(arguments);
            lambda.add(definition);

            memory.put(functionDefinition.get(0), lambda);
        } else {
            memory.put(identifier, execute(definition));
        }
        return null;
    }

    private boolean isPrimitive(List list) {
        Object operator = list.get(0);
        return operator.equals("print") ||
               operator.equals("increment") ||
               operator.equals("decrement") ||
               operator.equals("+") ||
               operator.equals("*") ||
               operator.equals("=");
    }
}