package com.bislan.dynamicquery.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.bislan.dynamicquery.library.expression.ExpressionEntries;
import com.bislan.dynamicquery.library.expression.ExpressionFactory;
import com.bislan.dynamicquery.library.expression.ExpressionType;
import com.bislan.dynamicquery.library.expression.PredicatePath;
import com.bislan.dynamicquery.library.expression.operator.OperatorType;
import com.bislan.dynamicquery.library.expression.operator.RelationType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.util.StringUtils;

public class QuerydslPredicateBuilder<T> {

    private final Map<String, JoinsData> joins = new LinkedHashMap<>();
    private final Map<String, Path<?>> pathCache = new HashMap<>();
    private final List<BooleanOperation> predicates = new ArrayList<>();
    private final Class<? extends T> entityType;
    private final QueryParameters params;

    public QuerydslPredicateBuilder(Class<? extends T> entityType, QueryParameters params) {
        this.entityType = entityType;
        this.params = params;
    }

    public Predicate toPredicate() {
        ExpressionEntries exprMetadata = ExpressionFactory.createFromParams(entityType, params.getParameters());
        final String parentName = StringUtils.uncapitalize(entityType.getSimpleName());
        final SimplePath<T> parentPath = Expressions.path(entityType, parentName);
        pathCache.put(parentName, parentPath);
        exprMetadata.getPredicates()
                .forEach(predicatePath -> {
                    Expression<?>[] exprs;
                    if (predicatePath.getOp().getOpType() == OperatorType.LIST) {
                        exprs = new Expression<?>[2];
                        exprs[1] = Expressions.list(
                                Stream.of(predicatePath.getValues())
                                        .map(Expressions::constant)
                                        .distinct()
                                        .toArray(Expression<?>[]::new));
                    } else {
                        exprs = new Expression<?>[predicatePath.getValues().length + 1];
                        for (int i = 0; i < predicatePath.getValues().length; i++)
                            exprs[i + 1] = Expressions.constant(predicatePath.getValues()[i]);
                    }
                    Path<?> lastPath = parentPath;
                    for (PredicatePath path : predicatePath.getPath()) {
                        final String alias = path.getFullPath();
                        Path<?> prev = pathCache.get(path.getPath());
                        Path<?> next;
                        if (path.getRelationType() == RelationType.COLLECTION) {
                            next = pathCache.computeIfAbsent(alias, key -> Expressions.path(path.getType(), key));
                            joins.computeIfAbsent(alias,
                                    fp -> JoinsData.of(
                                            Expressions.collectionPath(path.getType(), Expressions.path(path.getType(), path.getProperty()).getClass(),
                                                    PathMetadataFactory.forProperty(prev, path.getProperty())),
                                            next, path.getRelationType()));
                        } else if (path.getRelationType() == RelationType.SINGLE) {
                            next = pathCache.computeIfAbsent(alias, key -> Expressions.path(path.getType(), key));
                            joins.computeIfAbsent(alias,
                                    fp -> JoinsData.of(new PathBuilder<Object>(path.getType(), PathMetadataFactory.forProperty(prev, path.getProperty())), next,
                                            path.getRelationType()));
                        } else {
                            next = pathCache.computeIfAbsent(alias,
                                    key -> Expressions.path(path.getType(), PathMetadataFactory.forProperty(prev, path.getProperty())));
                        }
                        lastPath = next;
                    }
                    exprs[0] = Expressions.path(predicatePath.getPropertyType(), lastPath, predicatePath.getProperty());

                    predicates.add(Expressions.predicate(predicatePath.getOp().getOperator(), exprs));
                });
        BooleanExpression[] booleanExprs = predicates.toArray(new BooleanOperation[0]);
        // allOf: AND all the predicates | anyOf: OR all the predicates
        if (exprMetadata.getType() == ExpressionType.ANYOF) {
            return Expressions.anyOf(booleanExprs);
        } else {
            return Expressions.allOf(booleanExprs);
        }
    }

    public Collection<JoinsData> getJoins() {
        return joins.values();
    }
}
