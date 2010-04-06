package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.type.Type;
import platform.server.data.type.ConcatenateType;
import platform.base.BaseUtils;

public class ConcatenateClassSet implements ConcreteClass  {

    private AndClassSet[] classes;

    public ConcatenateClassSet(AndClassSet[] classes) {
        this.classes = classes;
        assert classes.length > 1;
    }

    public AndClassSet get(int i) {
        return classes[i];
    }

    public AndClassSet and(AndClassSet node) {
        ConcatenateClassSet and = (ConcatenateClassSet) node;
        assert and.classes.length == classes.length;

        AndClassSet[] andClasses = new AndClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            andClasses[i] = classes[i].and(and.classes[i]);

        return new ConcatenateClassSet(andClasses);
    }

    public boolean isEmpty() {
        for(AndClassSet classSet : classes)
            if(classSet.isEmpty())
                return true;
        return false;
    }

    public boolean containsAll(AndClassSet node) {
        if(!(node instanceof ConcatenateClassSet)) return false;

        ConcatenateClassSet concatenate = (ConcatenateClassSet) node;
        assert concatenate.classes.length == classes.length;

        for(int i=0;i<classes.length;i++)
            if(!classes[i].containsAll(concatenate.classes[i]))
                return false;
        return true; 
    }

    public OrClassSet getOr() {
        return new OrConcatenateClass(BaseUtils.toMap(classes));
    }

    public Type getType() {
        Type[] types = new Type[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].getType();
        return new ConcatenateType(types);
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }
}
