package lsfusion.gwt.client.base.view.grid;

public class Range {
    public final int start;
    public final int end; //excluded

    public Range(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int length() {
        return end - start;
    }
}
