package com.bislan.dynamicquery.library.expression;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;

import com.bislan.dynamicquery.library.expression.operator.PredicateOperator;
import com.bislan.dynamicquery.library.expression.operator.RelationType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExpressionFactory {

    public static ExpressionEntries createFromParams(Class<?> entity, Map<String, String> params) {
        if (params.isEmpty()) {
            return new ExpressionEntries();
        }
        Map<String, String> searchParams = new HashMap<>(params);
        final ExpressionType exprType = extractExpressionType(searchParams);
        List<PredicateEntry> predicates = searchParams.entrySet()
                .stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue()))
                .map(entry -> create(entity, entry.getKey(), entry.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return new ExpressionEntries(exprType, predicates);
    }

    private static Optional<PredicateEntry> create(Class<?> entity, String key, String value) {
        Iterator<String> keyParts = Arrays.stream(StringUtils.tokenizeToStringArray(key, "."))
                .filter(StringUtils::hasText)
                .iterator();

        if (!keyParts.hasNext()) {
            return Optional.empty();
        }

        BeanWrapper beanIntro = new BeanWrapperImpl(entity);
        final List<PredicatePath> paths = new ArrayList<>();
        String property = null;
        Class<?> propertyType = null;
        final StringBuilder pathBuilder =
                new StringBuilder(com.querydsl.core.util.StringUtils.uncapitalize(entity.getSimpleName()));
        while (keyParts.hasNext()) {
            String part = keyParts.next();
            TypeDescriptor prop = beanIntro.getPropertyTypeDescriptor(part);

            if (prop == null) {
                return Optional.empty();
            }

            propertyType = prop.getType();

            if (Iterable.class.isAssignableFrom(propertyType)) {
                Class<?> genericType = GenericTypeResolver.resolveTypeArgument(prop.getType(), Collection.class);

                if (genericType == null) {
                    throw new RuntimeException("Cannot get generic type of collection '" + prop.getName() + "'");
                }
                if (classHasAnnotation(genericType, Entity.class)) {
                    beanIntro = updatePathAndGetNextBeanIntro(paths, genericType, part, pathBuilder, RelationType.COLLECTION);
                }
            } else if (classHasAnnotation(propertyType, Entity.class)) {
                beanIntro = updatePathAndGetNextBeanIntro(paths, propertyType, part, pathBuilder, RelationType.SINGLE);
            } else if (classHasAnnotation(propertyType, Embeddable.class)) {
                beanIntro = updatePathAndGetNextBeanIntro(paths, propertyType, part, pathBuilder, RelationType.EMBEDDED);
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append(part);
                keyParts.forEachRemaining(str -> sb.append('.').append(str));
                property = sb.toString();
            }
        }
        if (property == null) {
            throw new RuntimeException("The final property must be a simple field: " + key);
        }
        final OperatorAndValues opAndVals = extractOpAndValues(propertyType, value);
        return Optional.of(new PredicateEntry(opAndVals.op, paths,
                com.querydsl.core.util.StringUtils.uncapitalize(entity.getSimpleName()),
                propertyType, property, opAndVals.values));
    }

    private static BeanWrapper updatePathAndGetNextBeanIntro(List<PredicatePath> paths, Class<?> propertyType,
            String propName, StringBuilder pathBuilder,
            RelationType relationType) {
        paths.add(PredicatePath.of(propertyType, propName, pathBuilder.toString(), relationType));
        pathBuilder.append('_').append(propName);
        return new BeanWrapperImpl(propertyType);
    }

    private static boolean classHasAnnotation(Class<?> propertyType, Class<? extends Annotation> annotation) {

        if (propertyType.isAnnotationPresent(annotation)) {
            return true;
        }

        Annotation inheritedAnnotation = AnnotationUtils.findAnnotation(propertyType, annotation);
        return inheritedAnnotation != null;
    }

    private static OperatorAndValues extractOpAndValues(Class<?> propertyType, String value) {
        int openParenthesesIdx = value.indexOf("(");
        if (openParenthesesIdx <= 0) {
            throw new RuntimeException("Invalid predicate or cannot find operator: " + value);
        }
        if (!value.endsWith(")")) {
            throw new RuntimeException("Invalid predicate or no closing parentheses at the end: " + value);
        }
        String opStr = value.substring(0, openParenthesesIdx).toUpperCase();
        PredicateOperator op;
        try {
            op = PredicateOperator.valueOf(opStr);
        } catch (Exception e) {
            throw new RuntimeException("Invalid operator '" + opStr + "' for predicate: " + value);
        }
        int lastIdx = value.length() - 1;
        String[] strValues;
        if (lastIdx - openParenthesesIdx > 1) {
            strValues = value.substring(openParenthesesIdx + 1, lastIdx).split(",");
        } else {
            strValues = new String[0];
        }
        return OperatorAndValues.of(op, toTypedValues(propertyType, strValues));
    }

    private static Object[] toTypedValues(final Class<?> type, final String[] values) {
        if (type == String.class) {
            return values;
        } else if (type == Long.class) {
            return convertValues(values, Long::valueOf);
        } else if (type == Integer.class) {
            return convertValues(values, Integer::valueOf);
        } else if (type == BigDecimal.class) {
            return convertValues(values, BigDecimal::new);
        } else if (type == Boolean.class) {
            return convertValues(values, Boolean::parseBoolean);
        } else if (type == Double.class) {
            return convertValues(values, Double::valueOf);
        } else if (type == LocalDate.class) {
            return convertValues(values, LocalDate::parse);
        } else if (type == Instant.class) {
            return convertValues(values, Instant::parse);
        } else if (type == Float.class) {
            return convertValues(values, Float::valueOf);
        } else if (type == Short.class) {
            return convertValues(values, Short::valueOf);
        } else if (type == Byte.class) {
            return convertValues(values, Byte::valueOf);
        } else if (type == Character.class) {
            return convertValues(values, str -> str.charAt(0));
        } else {
            return convertValues(values, type);
        }
    }

    private static Object[] convertValues(String[] values, Function<String, Object> mapper) {
        return Stream.of(values)
                .map(mapper)
                .distinct()
                .toArray(Object[]::new);
    }

    private static Object[] convertValues(String[] values, Class<?> type) {
        return Stream.of(values)
                .map(str -> new ObjectMapper().convertValue(str, type))
                .distinct()
                .toArray(Object[]::new);
    }

    private static ExpressionType extractExpressionType(Map<String, String> params) {
        ExpressionType exprType = ExpressionType.ALLOF; // default
        String type = params.remove(ExpressionType.TypeKey);
        if (StringUtils.hasText(type)) {
            exprType = ExpressionType.valueOf(type.toUpperCase());
        }
        return exprType;
    }

    private static class OperatorAndValues {
        private final PredicateOperator op;
        private final Object[] values;

        private OperatorAndValues(PredicateOperator op, Object[] values) {
            this.op = op;
            this.values = values;
        }

        public static OperatorAndValues of(PredicateOperator op, Object[] values) {
            return new OperatorAndValues(op, values);
        }
    }
}
