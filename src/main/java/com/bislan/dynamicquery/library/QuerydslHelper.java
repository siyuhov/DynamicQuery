package com.bislan.dynamicquery.library;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;

public class QuerydslHelper {

    private QuerydslHelper() {
    }

    static <T> JPAQuery<T> applyPagination(JPAQuery<T> query, Pageable pageable, EntityPath<T> path) {
        if (pageable.isUnpaged()) {
            return query;
        }
        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());
        return applySorting(query, pageable.getSort(), path);
    }

    public static <T> JPAQuery<T> applySorting(JPAQuery<T> query, Sort sort, EntityPath<T> path) {
        if (!sort.isSorted()) {
            return query;
        }
        sort.get()
                .forEach(order -> query.orderBy(toOrderSpecifier(order, path)));
        return query;
    }

    static <T> OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder, EntityPath<T> path) {
        final Order order = sortOrder.isAscending() ? Order.ASC : Order.DESC;
        return new OrderSpecifier(order, Expressions.path(path.getType(), path, sortOrder.getProperty()),
                OrderSpecifier.NullHandling.Default);
    }
}
