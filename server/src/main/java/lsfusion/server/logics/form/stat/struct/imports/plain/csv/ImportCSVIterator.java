package lsfusion.server.logics.form.stat.struct.imports.plain.csv;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportMatrixIterator;
import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.io.InputStreamReader;

public class ImportCSVIterator extends ImportMatrixIterator {
    private CSVReader csvReader;
    
    public ImportCSVIterator(final ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, boolean noHeader, boolean noEscape, String separator) throws IOException {
        super(fieldTypes, noHeader);

        InputStreamReader isReader = charset != null ? new InputStreamReader(new BOMInputStream(file.getInputStream()), charset) : new InputStreamReader(new BOMInputStream(file.getInputStream()));
        char escapeChar = noEscape ? '\0' : ICSVParser.DEFAULT_ESCAPE_CHARACTER;
        char quoteChar = noEscape ? '\0' : ICSVParser.DEFAULT_QUOTE_CHARACTER;
        this.csvReader = new CSVReaderBuilder(isReader).withCSVParser(new CSVParserBuilder().withSeparator(separator.charAt(0)).withQuoteChar(quoteChar).withEscapeChar(escapeChar).build()).build();
        
        finalizeInit();
    }

    private ImList<String> line;

    @Override
    protected boolean nextRow() throws IOException {
        line = readLine();
        return line != null;
    }

    private ImList<String> readLine() throws IOException {
        String[] line = csvReader.readNext();
        return line != null ? ListFact.toList(line) : null;
    }

    @Override
    public void release() throws IOException {
        if (csvReader != null)
            csvReader.close();
    }

    @Override
    protected Object getPropValue(Integer fieldIndex, Type type) throws ParseException {
        if (fieldIndex >= line.size())
            throw new ParseException("Column with index " + fieldIndex + " not found");
        return type.parseCSV(line.get(fieldIndex));
    }

    @Override
    protected boolean isLastValue(Integer fieldIndex) {
        return fieldIndex >= line.size();
    }
}