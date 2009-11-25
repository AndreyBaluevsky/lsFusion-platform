package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.Lazy;
import platform.server.caches.MapContext;
import platform.server.caches.MapHashIterable;
import platform.server.caches.MapParamsIterable;
import platform.server.data.expr.AndExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.MapExpr;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

@Immutable
public class GroupJoin implements MapContext, InnerJoin {
    final Where where;
    final Map<AndExpr, AndExpr> group;
    final Collection<KeyExpr> keys;

    public DataWhereSet getJoinFollows() {
        return MapExpr.getExprFollows(group);
    }

    public GroupJoin(Where where, Map<AndExpr, AndExpr> group, Collection<KeyExpr> keys) {
        this.where = where;
        this.group = group;
        this.keys = keys;
    }

    public int hash(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<AndExpr,AndExpr> expr : group.entrySet())
            hash += expr.getKey().hashContext(hashContext) ^ expr.getValue().hashCode();
        hash = hash*31;
        for(KeyExpr key : keys)
            hash += key.hashContext(hashContext);
        return where.hashContext(hashContext) + hash*31;
    }

    @Lazy
    public Context getContext() {
        Context context = new Context();
        where.fillContext(context);
        context.fill(group.keySet());
        context.keys.addAll(keys);
        return context;
    }

    KeyTranslator merge(GroupJoin groupJoin) {
        if(hashCode()!=groupJoin.hashCode())
            return null;

        for(KeyTranslator translator : new MapHashIterable(this,groupJoin,false))
            if(where.translateDirect(translator).equals(groupJoin.where) && translator.translateDirect(BaseUtils.reverse(group)).equals(BaseUtils.reverse(groupJoin.group)))
                    return translator;
        return null;
    }


    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GroupJoin && merge((GroupJoin) o)!=null;
    }

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = MapParamsIterable.hash(this,false);
            hashCoded = true;
        }
        return hashCode;
    }
}
