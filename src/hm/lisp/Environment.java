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
        if (isExpression(element))
            return executeExpression((List) element, memory);
        else if (memory.containsKey(element))
            return execute(memory.get(element), memory);
        else
            return element;
    }

    private Object debug(Object element) {
        System.err.println(element.toString()
                               .replaceAll("\\[", "(")
                               .replaceAll("\\]", ")")
                               .replaceAll(", ", " "));
        return element;
    }

    private Object executeExpression(List list, Map memory) {
        if (isLambdaCall(list))
            return execute(reduceLambda(list, memory), memory);
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
        else if (isFunctionCall(list, memory))
            return executeFunction(list, memory);
        else if (isPrimitive(list))
            return executePrimitive(reduceArguments(list, memory));
        else
            return execute(reduceOperator(list, memory), memory);
    }

    private List reduceOperator(List list, Map memory) {
        List reduced = new ArrayList();
        reduced.add(0, execute(list.get(0), memory));
        for (int i = 1; i < list.size(); i++)
            reduced.add(list.get(i));
        return reduced;
    }

    private Object executeFunction(List list, Map memory) {
        List lambda = (List) memory.get(list.remove(0));
        list.add(0, lambda);
        return execute(reduceLambda(list, memory), memory);
    }

    private boolean isFunctionCall(List list, Map memory) {
        Object operator = list.get(0);
        return !isExpression(operator) &&
               memory.containsKey(operator) &&
               isExpression(memory.get(operator)) &&
               isLambda((List) memory.get(operator));
    }

    private Object scopedExpression(List list, Map memory) {
        return execute(list.get(2), scopedMemory(list, memory));
    }

    private Map scopedMemory(List list, Map memory) {
        List<List> definitions = (List) list.get(1);
        Map localMemory = new HashMap(memory);
        for (List definition : definitions)
            registerInMemory(localMemory, definition.get(0), execute(definition.get(1), localMemory));
        return localMemory;
    }

    private boolean isLetExpression(List list) {
        return list.size() == 3 &&
               list.get(0).equals("let") &&
               isExpression(list.get(1));
    }

    private List reduceArguments(List list, Map memory) {
        List reverse = new ArrayList();
        reverse.add(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            Object argument = list.get(i);
            argument = execute(argument, memory);
            reverse.add(argument);
        }
        return reverse;
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
        Object operator = list.get(0);
        return isExpression(operator) && ((List) list.get(0)).get(0).equals("lambda");
    }

    private Object reduceLambda(List lambdaCall, Map memory) {
        Object operator = lambdaCall.get(0);
        List lambda = (List) operator;
        List arguments = (List) lambda.get(1);

        if (!isExpression(operator))
            throw new RuntimeException(format("Undefined operator %s", operator));

        Map valueMap = mapValues(lambdaCall, arguments, memory);
        Object reducedLambdaBody = reduceLambdaBody(lambda.get(2), valueMap);
        if (lambdaCall.size() == (arguments.size() + 1))
            return reducedLambdaBody;
        else
            return curry(lambdaCall, arguments, reducedLambdaBody);
    }

    private Object curry(List lambdaCall, List arguments, Object reducedLambdaBody) {
        List curry = new ArrayList();
        curry.add("lambda");
        List curryArguments = new ArrayList<>();
        curry.add(curryArguments);
        for (int i = lambdaCall.size() - 1; i < arguments.size(); i++)
            curryArguments.add(arguments.get(i));
        curry.add(reducedLambdaBody);
        return curry;
    }

    private Map mapValues(List lambdaCall, List arguments, Map memory) {
        Map<Object, Object> values = new HashMap<>();
        for (int i = 1; i < lambdaCall.size(); i++){
            Object value = lambdaCall.get(i);
            if(!isExpression(value) && memory.containsKey(value))
                value = memory.get(value);
            values.put(arguments.get(i - 1), value);
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
        String operator = (String) list.get(0);
        String name = operator.substring("<primitive>".length());
        switch (name) {
            case "print":
                return print(list);
            case "+":
                return plus(list);
            case "-":
                return minus(list);
            case "/":
                return divide(list);
            case "*":
                return multiply(list);
            case "=":
                return equal(list);
            case "not":
                return not(list);
            case "and":
                return and(list);
            case "or":
                return or(list);
            case ">":
                return greaterThan(list);
            case ">=":
                return greaterOrEqualThan(list);
            case "<":
                return lessThan(list);
            case "<=":
                return lessThanOrEqualTo(list);
            default:
                throw new RuntimeException(format("Primitive operator %s does not exist", name));
        }
    }

    private Object greaterOrEqualThan(List list) {
        return (double) list.get(1) >= (double) list.get(2);
    }

    private Object lessThanOrEqualTo(List list) {
        return (double) list.get(1) <= (double) list.get(2);
    }

    private Object lessThan(List list) {
        return (double) list.get(1) < (double) list.get(2);
    }

    private Object greaterThan(List list) {
        return (double) list.get(1) > (double) list.get(2);
    }

    private Object not(List list) {
        return !((boolean) list.get(1));
    }

    private Object and(List list) {
        return (boolean) list.get(1) && (boolean) list.get(2);
    }


    private Object or(List list) {
        return (boolean) list.get(1) || (boolean) list.get(2);
    }

    private Object divide(List list) {
        return (double) list.get(1) / (double) list.get(2);
    }

    private Object minus(List list) {
        return (double) list.get(1) - (double) list.get(2);
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

            Object key = functionDefinition.get(0);
            registerInMemory(memory, key, lambda);
        } else {
            Object value = execute(definition);
            registerInMemory(memory, identifier, value);
        }
        return null;
    }

    private Object registerInMemory(Map memory, Object identifier, Object value) {
        if (identifier.toString().contains("<primitive>"))
            throw new RuntimeException("Cannot define new primitives");
        return memory.put(identifier, value);
    }

    private boolean isPrimitive(List list) {
        Object operator = list.get(0);
        return operator.toString().startsWith("<primitive>");
    }
}