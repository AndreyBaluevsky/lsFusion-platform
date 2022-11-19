package lsfusion.server.logics.classes.data.utils.image;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Iterator;

public class ResizeImageSizeAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface widthInterface;
    private final ClassPropertyInterface heightInterface;

    public ResizeImageSizeAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        widthInterface = i.next();
        heightInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            RawFileData inputFile = (RawFileData) context.getKeyValue(fileInterface).getValue();
            Integer width = (Integer) context.getKeyValue(widthInterface).getValue();
            Integer height = (Integer) context.getKeyValue(heightInterface).getValue();
            if(inputFile != null) {
                if(width != null || height != null) {

                    BufferedImage image = ImageIO.read(inputFile.getInputStream());
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    double scaleWidth = width != null ? ((double) width / imageWidth) : (double) height / imageHeight;
                    double scaleHeight = height != null ? (double) height / imageHeight : ((double) width / imageWidth);

                    File outputFile = null;
                    try {
                        outputFile = Files.createTempFile("resized", ".jpg").toFile();
                        if(scaleWidth != 0 && scaleHeight != 0) {
                            Thumbnails.of(inputFile.getInputStream()).scale(scaleWidth, scaleHeight).toFile(outputFile);
                            findProperty("resizedImage[]").change(new RawFileData(outputFile), context);
                        }
                    } finally {
                        if (outputFile != null && !outputFile.delete())
                            outputFile.deleteOnExit();
                    }
                } else {
                    throw new RuntimeException("No width nor height found");
                }
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
