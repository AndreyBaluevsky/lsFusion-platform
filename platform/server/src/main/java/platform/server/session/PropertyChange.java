package platform.server.session;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static platform.base.BaseUtils.hashEquals;
import static platform.base.BaseUtils.rightJoin;

public class PropertyChange<T extends PropertyInterface> extends AbstractInnerContext<PropertyChange<T>> {
    private final Map<T, DataObject> mapValues; // для оптимизации в общем-то, важно чтобы проходили через ветку execute

    private final Map<T,KeyExpr> mapKeys;
    public final Expr expr;
    public final Where where;

    public static <T extends PropertyInterface> Map<T, Expr> getMapExprs(Map<T, KeyExpr> mapKeys, Map<T, DataObject> mapValues) {
        return getMapExprs(mapKeys, mapValues, Where.TRUE);
    }

    public static <T extends PropertyInterface> Map<T, Expr> getMapExprs(Map<T, KeyExpr> mapKeys, Map<T, DataObject> mapValues, Where where) {
        Map<T, Expr> keyValues = new HashMap<T, Expr>(DataObject.getMapExprs(mapValues));

        Map<BaseExpr, BaseExpr> exprValues = where.getExprValues();
        for(Map.Entry<T, KeyExpr> mapKey : mapKeys.entrySet()) {
            BaseExpr exprValue = exprValues.get(mapKey.getValue());
            keyValues.put(mapKey.getKey(), exprValue!=null ? exprValue : mapKey.getValue());
        }
        return keyValues;

    }

    public static <T> Map<T, KeyExpr> getFullMapKeys(Map<T, KeyExpr> mapKeys, Map<T, DataObject> mapValues) {
        assert Collections.disjoint(mapKeys.keySet(), mapValues.keySet());
        return BaseUtils.merge(mapKeys, KeyExpr.getMapKeys(mapValues.keySet()));
    }

    public static <C> Map<C, ? extends Expr> simplifyExprs(Map<C, ? extends Expr> implementExprs, Where andWhere) {
        KeyEquals keyEquals = andWhere.getKeyEquals(); // оптимизация
        KeyEqual keyEqual;
        if(keyEquals.size == 1 && !(keyEqual=keyEquals.getKey(0)).isEmpty())
            implementExprs = keyEqual.getTranslator().translate(implementExprs);
        return implementExprs;
    }

    public Map<T, Expr> getMapExprs() {
        return getMapExprs(mapKeys, mapValues, where);
    }

    public Map<T, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public Map<T, DataObject> getMapValues() {
        return mapValues;
    }

    public PropertyChange(Expr expr, Map<T, DataObject> mapValues) {
        this(mapValues, new HashMap<T, KeyExpr>(), expr, Where.TRUE);
    }

    public PropertyChange(ObjectValue value) {
        this(value.getExpr(), new HashMap<T, DataObject>());
    }

    public PropertyChange(ObjectValue value, T propInterface, DataObject intValue) {
        this(value.getExpr(), Collections.singletonMap(propInterface, intValue));
    }

    public PropertyChange(Map<T, DataObject> mapValues, Map<T, KeyExpr> mapKeys, Expr expr, Where where) {
        this.mapValues = mapValues;
        this.mapKeys = mapKeys;
        this.expr = expr;
        this.where = where;
    }

    public PropertyChange(PropertySet<T> set, ObjectValue value) {
        this(set.mapValues, set.mapKeys, value.getExpr(), set.where);
    }

    public PropertyChange(PropertyChange<T> change, Expr expr) {
        this(change.mapValues, change.mapKeys, expr, change.where);
    }

    public PropertyChange(PropertyChange<T> change, Expr expr, Where where) {
        this(change.mapValues, change.mapKeys, expr, where);
    }

    public PropertyChange(Map<T, KeyExpr> mapKeys, Expr expr, Where where) {
        this(new HashMap<T, DataObject>(), mapKeys, expr, where);
    }

    public static <P extends PropertyInterface> PropertyChange<P> STATIC(boolean isTrue) {
        return new PropertyChange<P>(new HashMap<P, KeyExpr>(), ValueExpr.TRUE, isTrue? Where.TRUE : Where.FALSE);
    }
    public PropertyChange(Map<T, KeyExpr> mapKeys, Expr expr) {
        this(mapKeys, expr, expr.getWhere());
    }

    public PropertyChange(Map<T, KeyExpr> mapKeys, Where where) {
        this(mapKeys, Expr.NULL, where);
    }

    public QuickSet<KeyExpr> getKeys() {
        return new QuickSet<KeyExpr>(mapKeys.values());
    }

    public QuickSet<Value> getValues() {
        return expr.getOuterValues().merge(where.getOuterValues()).merge(AbstractOuterContext.getOuterValues(DataObject.getMapExprs(mapValues).values()));
    }

    public PropertyChange<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertyChange<T>(mapValues, mapKeys, expr, where.and(andWhere));
    }

    public <P extends PropertyInterface> PropertyChange<P> map(Map<P,T> mapping) {
        return new PropertyChange<P>(rightJoin(mapping, mapValues), rightJoin(mapping, mapKeys),expr,where);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }

    public PropertyChange<T> add(PropertyChange<T> change) {
        if(isEmpty())
            return change;
        if(change.isEmpty())
            return this;
        if(equals(change))
            return this;

        if(mapValues.isEmpty()) {
            // assert что addJoin.getWhere() не пересекается с where, в общем случае что по пересекаемым они совпадают
            Join<String> addJoin = change.join(mapKeys);
            return new PropertyChange<T>(mapKeys, expr.ifElse(where, addJoin.getExpr("value")), where.or(addJoin.getWhere()));
        } else {
            Map<T, KeyExpr> addKeys = getFullMapKeys(mapKeys, mapValues); // тут по хорошему надо искать общие и потом частично join'ить
            
            Join<String> thisJoin = join(addKeys);
            Join<String> addJoin = change.join(addKeys);

            Where thisWhere = thisJoin.getWhere();
            return new PropertyChange<T>(addKeys, thisJoin.getExpr("value").ifElse(thisWhere, addJoin.getExpr("value")), thisWhere.or(addJoin.getWhere()));
        }
    }

    public Where getWhere(Map<T, ? extends Expr> joinImplement) {
        return join(joinImplement).getWhere();
    }

    public Join<String> join(Map<T, ? extends Expr> joinImplement) {
        return getQuery().join(joinImplement);
    }

    public Pair<Map<T, DataObject>, ObjectValue> getSimple() {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            return new Pair<Map<T, DataObject>, ObjectValue>(mapValues, exprValue);
        return null;
    }

    public OrderedMap<Map<T, DataObject>, Map<String, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            return new OrderedMap<Map<T, DataObject>, Map<String, ObjectValue>>(mapValues, Collections.singletonMap("value", exprValue));

        return getQuery().executeClasses(env);
    }

    public void addRows(SinglePropertyTableUsage<T> table, SQLSession session, BaseClass baseClass, boolean update, QueryEnvironment queryEnv) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            table.insertRecord(session, mapValues, exprValue, update);
        else
            table.addRows(session, getQuery(), baseClass, update, queryEnv);
    }

    public void writeRows(SinglePropertyTableUsage<T> table, SQLSession session, BaseClass baseClass, QueryEnvironment queryEnv) throws SQLException {
        ObjectValue exprValue;
        if(mapKeys.isEmpty() && where.isTrue() && (exprValue = expr.getObjectValue())!=null)
            table.writeRows(session, Collections.singletonMap(mapValues, Collections.singletonMap("value", exprValue)));
        else
            table.writeRows(session, getQuery(), baseClass, queryEnv);
    }

    @IdentityLazy
    public Query<T,String> getQuery() {
        Query<T,String> query = new Query<T, String>(getFullMapKeys(mapKeys, mapValues), where, mapValues); // через query для кэша
        query.properties.put("value",expr);
        return query;
   }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return 31 * (where.hashOuter(hashContext)*31*31 + expr.hashOuter(hashContext)*31 + AbstractOuterContext.hashOuter(mapKeys, hashContext)) +
                AbstractOuterContext.hashOuter(DataObject.getMapExprs(mapValues), hashContext);
    }

    public boolean equalsInner(PropertyChange<T> object) {
        return hashEquals(where, object.where) && hashEquals(expr, object.expr) && hashEquals(mapKeys, object.mapKeys) && hashEquals(mapValues, object.mapValues);
    }

    protected PropertyChange<T> translate(MapTranslate translator) {
        return new PropertyChange<T>(translator.translateDataObjects(mapValues), translator.translateKey(mapKeys),expr.translateOuter(translator),where.translateOuter(translator));
    }

    protected long calculateComplexity(boolean outer) {
        return where.getComplexity(outer) + expr.getComplexity(outer);
    }
    @Override
    public PropertyChange<T> calculatePack() {
        Where packWhere = where.pack();
        return new PropertyChange<T>(mapValues, mapKeys, expr.followFalse(packWhere.not(), true), packWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, WhereBuilder where) {
        Join<String> join = join(joinImplement);
        if(where !=null) where.add(join.getWhere());
        return join.getExpr("value");
    }

    public static <P extends PropertyInterface> PropertyChange<P> addNull(PropertyChange<P> change1, PropertyChange<P> change2) {
        if(change1==null)
            return change2;
        if(change2==null)
            return change1;
        return change1.add(change2);
    }

/*    public StatKeys<T> getStatKeys() {
        return where.getStatKeys(getInnerKeys()).mapBack(mapKeys).and(new StatKeys<T>(mapValues.keySet(), Stat.ONE));
    }*/
}
