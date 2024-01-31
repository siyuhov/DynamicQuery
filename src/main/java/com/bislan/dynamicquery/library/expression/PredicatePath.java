package com.bislan.dynamicquery.library.expression;

import com.bislan.dynamicquery.library.expression.operator.RelationType;

public class PredicatePath {

    private final Class<?> type;
    private final String property;
    private final String prevPath;
    private final RelationType relationType;

    public PredicatePath(Class<?> type, String property, String prevPath, RelationType relationType) {
        this.type = type;
        this.property = property;
        this.prevPath = prevPath;
        this.relationType = relationType;
    }

    public static PredicatePath of(Class<?> type, String property, String prevPath, RelationType relationType) {
        return new PredicatePath(type, property, prevPath, relationType);
    }

    public Class<?> getType() {
        return type;
    }

    public String getProperty() {
        return property;
    }

    public String getPath() {
        return prevPath;
    }

    public String getFullPath() {
        return prevPath + "_" + property;
    }

    public RelationType getRelationType() {
        return relationType;
    }
}
