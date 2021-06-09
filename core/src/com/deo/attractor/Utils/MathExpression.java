package com.deo.attractor.Utils;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Constant;
import org.mariuszgromada.math.mxparser.Expression;

public class MathExpression {
    
    Expression expression;
    Argument[] args;
    Constant[] consts;
    
    public MathExpression(String expression, String[] arguments, String[] constants) {
        this.expression = new Expression(expression);
        args = new Argument[arguments.length];
        consts = new Constant[constants.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = new Argument(arguments[i]);
        }
        for (int i = 0; i < constants.length; i++) {
            consts[i] = new Constant(constants[i]);
        }
        this.expression.addArguments(args);
        this.expression.addConstants(consts);
    }
    
    public double evaluateExp(float... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            args[i].setArgumentValue(arguments[i]);
        }
        return expression.calculate();
    }
    
    public void changeConstant(int index, float value) {
        consts[index].setConstantValue(value);
    }
    
}
