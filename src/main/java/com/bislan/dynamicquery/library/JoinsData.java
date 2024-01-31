package com.bislan.dynamicquery.library;

import com.bislan.dynamicquery.library.expression.operator.RelationType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;

public class JoinsData {
    private final Expression<?> expr;
    private final Path<?> alias;
    private final RelationType relationType;

    private JoinsData(Expression<?> expr, Path<?> alias, RelationType relationType) {
        this.expr = expr;
        this.alias = alias;
        this.relationType = relationType;
    }

    static JoinsData of(Expression<?> expr, Path<?> alias, RelationType relationType) {
        return new JoinsData(expr, alias, relationType);
    }

    public Expression<?> getExpr() {
        return expr;
    }

    public Path<?> getAlias() {
        return alias;
    }

    public RelationType getRelationType() {
        return relationType;
    }
}
