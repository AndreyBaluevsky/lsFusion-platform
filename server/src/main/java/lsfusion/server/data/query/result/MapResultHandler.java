package lsfusion.server.data.query.result;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.lambda.Provider;

import java.sql.SQLException;

public class MapResultHandler<MK, MV, K, V> implements ResultHandler<K, V>, Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> {

    private final ResultHandler<MK, MV> handler;
    private final ImRevMap<MK, K> mapKeys;
    private final ImRevMap<MV, V> mapValues;

    public MapResultHandler(ResultHandler<MK, MV> handler, ImRevMap<MK, K> mapKeys, ImRevMap<MV, V> mapValues) {
        this.handler = handler;
        this.mapKeys = mapKeys;
        this.mapValues = mapValues;
    }

    public void start() {
        handler.start();
    }

    public void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) throws SQLException {
        handler.proceed(mapKeys.join(rowKey), mapValues.join(rowValue));
    }

    public void finish() throws SQLException {
        handler.finish();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> get() {
        return handler.getPrevResults().get().mapOrderKeyValues(mapKeys::crossJoin, mapValues::crossJoin);
    }

    public Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> getPrevResults() {
        return this;
    }

    @Override
    public boolean hasQueryLimit() {
        return handler.hasQueryLimit();
    }
}
