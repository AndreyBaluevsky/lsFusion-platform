package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.TranslateValues;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

public class QueryException extends RuntimeException implements ValuesContext {

    public int hashValues(HashValues hashValues) {
        throw new RuntimeException("not supported yet");
    }

    public QuickSet<Value> getContextValues() {
        return new QuickSet<Value>();
    }

    public BaseUtils.HashComponents<Value> getValueComponents() {
        throw new RuntimeException("not supported yet");
    }

    public TranslateValues translateValues(MapValuesTranslate translate) {
        return this;
    }

    public TranslateValues translateRemoveValues(MapValuesTranslate translate) {
        return this;
    }
}
