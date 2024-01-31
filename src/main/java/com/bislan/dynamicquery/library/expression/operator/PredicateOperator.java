package com.bislan.dynamicquery.library.expression.operator;

import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;

public enum PredicateOperator implements Operator {
    EQ(Ops.EQ), NE(Ops.NE), IS_NULL(Ops.IS_NULL), IS_NOT_NULL(Ops.IS_NOT_NULL), BETWEEN(Ops.BETWEEN), GOE(Ops.GOE), GT(Ops.GT), LOE(Ops.LOE), LT(
            Ops.LT), MATCHES(Ops.MATCHES), //regex
    MATCHES_IC(Ops.MATCHES_IC), //regex
    STRING_IS_EMPTY(Ops.STRING_IS_EMPTY), STARTS_WITH(Ops.STARTS_WITH), STARTS_WITH_IC(Ops.STARTS_WITH_IC), EQ_IGNORE_CASE(Ops.EQ_IGNORE_CASE), ENDS_WITH(
            Ops.ENDS_WITH), ENDS_WITH_IC(Ops.ENDS_WITH_IC), STRING_CONTAINS(Ops.STRING_CONTAINS), STRING_CONTAINS_IC(
                    Ops.STRING_CONTAINS_IC), LIKE(Ops.LIKE), LIKE_IC(Ops.LIKE_IC), LIKE_ESCAPE(Ops.LIKE_ESCAPE), LIKE_ESCAPE_IC(Ops.LIKE_ESCAPE_IC),

    IN(Ops.IN, OperatorType.LIST), NOT_IN(Ops.NOT_IN, OperatorType.LIST);

    private final Operator operator;
    private final OperatorType operatorType;

    PredicateOperator(Operator operator) {
        this.operator = operator;
        this.operatorType = OperatorType.SINGLE;
    }

    PredicateOperator(Operator operator, OperatorType operatorType) {
        this.operator = operator;
        this.operatorType = operatorType;
    }

    public Operator getOperator() {
        return operator;
    }

    public OperatorType getOpType() {
        return operatorType;
    }

    @Override
    public Class<?> getType() {
        return operator.getType();
    }
}
