package platform.server.data.where.classes;

import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.expr.BaseExpr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassWhere<K> extends AbstractClassWhere<K, ClassWhere<K>> {

    private boolean compatible(ClassWhere<K> where) {
        return where.isFalse() || isFalse() || wheres[0].compatible(where.wheres[0]);
    }
    // в некоторых случаях как при проверке интерфейсов, классы могут быть не compatible и тогда нарушится инвариант с OrClassSet -
    // что это либо OrObjectClassSet или DataClass поэтому пока так
    public ClassWhere<K> andCompatible(ClassWhere<K> where) {
        if(compatible(where))
            return and(where);
        else
            return STATIC(false);
    }

    public boolean meansCompatible(ClassWhere<K> where) {
        return compatible(where) && means(where);
    }

    public ClassWhere() {
        this(false);
    }

    public ClassWhere(boolean isTrue) {
        super(isTrue);
    }

    public ClassWhere(And<K> where) {
        super(where);
    }

    public static <K> ClassWhere<K> TRUE() {
        return STATIC(true);
    }
    public static <K> ClassWhere<K> STATIC(boolean isTrue) {
        return new ClassWhere<K>(isTrue);
    }

    private ClassWhere(And<K>[] iWheres) {
        super(iWheres);
    }
    protected ClassWhere<K> createThis(And<K>[] wheres) {
        return new ClassWhere<K>(wheres);
    }

    public ClassWhere(K key, AndClassSet classes) {
        super(key, classes);
    }

    private static <K> Map<K,AndClassSet> initUpClassSets(Map<K, ValueClass> map) {
        Map<K, AndClassSet> result = new HashMap<K,AndClassSet>();
        for(Map.Entry<K, ValueClass> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().getUpSet());
        return result;
    }
    public ClassWhere(Map<K, ValueClass> mapClasses,boolean up) {
        super(initUpClassSets(mapClasses));
        assert up;
    }



    public ClassWhere(Map<K, ? extends AndClassSet> mapSets) {
        super(mapSets);
    }

    public Map<K, ValueClass> getCommonParent(Collection<K> keys) {

        assert !isFalse();

        Map<K, ValueClass> result = new HashMap<K, ValueClass>();
        for(K key : keys) {
            OrClassSet orSet = wheres[0].get(key).getOr();
            for(int i=1;i<wheres.length;i++)
                orSet = orSet.or(wheres[i].get(key).getOr());
            result.put(key,orSet.getCommonClass());
        }
        return result;
    }

    public ClassExprWhere map(Map<K, BaseExpr> map) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<K> andWhere : wheres) {
            ClassExprWhere joinWhere = ClassExprWhere.TRUE;
            for(Map.Entry<K, BaseExpr> joinExpr : map.entrySet())
                joinWhere = joinWhere.and(joinExpr.getValue().getClassWhere(andWhere.get(joinExpr.getKey())));
            result = result.or(joinWhere);
        }
        return result;
    }

    public <V> ClassWhere(ClassWhere<V> classes, Map<V, K> map) {
        super(classes, map);
    }
}

