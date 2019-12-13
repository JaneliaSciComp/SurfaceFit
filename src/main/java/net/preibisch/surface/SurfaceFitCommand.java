package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.FolderOpener;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.SciJava;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.preibisch.surface.Test.*;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    @Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    @Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/";

    @Parameter
    private long originalDimX = 9007;

    @Parameter
    private long originalDimZ = 9599;

    @Parameter
    private String outputBasename = "heightSurf200";

    @Parameter
    private DatasetIOService dataset;

    @Parameter
    private OpService ops;

    @Override
    public void run() {
        // Open image sequence from input directory
        ImagePlus imp = FolderOpener.open(inputDirectory, " file=tif");
        imp.setTitle("Target");
        // Reslice image
        IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");
        imp = IJ.getImage();
        Img img = ImageJFunctions.wrap(imp);

        ImagePlus botSurfaceMap = getScaledSurfaceMap(getBotImg(img));
        ImagePlus topSurfaceMap = getScaledSurfaceMap(getTopImg(img));

        IJ.save(botSurfaceMap, outputDirectory + outputBasename + "-bot.tif");
        IJ.save(topSurfaceMap, outputDirectory + outputBasename + "-top.tif");
    }

    private ImagePlus getScaledSurfaceMap(Img img) {
        final Img<IntType> surface = process2( img, 5, 40, 20 );
		final Img< FloatType > rendererSurface = img.factory().create( img );
		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );
        ImagePlus input = Util.getImagePlusInstance(img);
        input.setTitle("Target");
		input.show();

        ImagePlus surfaceImp = Util.getImagePlusInstance(surface);
        surfaceImp.setTitle("Surface");
		surfaceImp.show();

        ImagePlus rendererSurfaceImp = Util.getImagePlusInstance(rendererSurface);
        rendererSurfaceImp.setTitle("Generated surface");
		//rendererSurfaceImp.show();

		IJ.run(input, "Reslice [/]...", "output=1.000 start=Top avoid");
		IJ.run(rendererSurfaceImp, "Reslice [/]...", "output=1.000 start=Top avoid");

		// TODO smoothing before upsampling

		IJ.run(surfaceImp, "Scale...", "x=- y=- width=" + originalDimX + " height=" + originalDimZ + " interpolation=Bicubic average create title=ScaleSurface");

        ImagePlus scaledSurfaceImp = WindowManager.getImage("ScaleSurface");
        scaledSurfaceImp.setTitle("ScaleSurfaceFetched");
        return scaledSurfaceImp;
    }

    public Img getBotImg(Img img) {
        FinalInterval botHalfInterval = Intervals.createMinMax(0, 0, 0, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)/2-1);
        Img<FloatType> botImg = ops.create().img(botHalfInterval, new FloatType());

        IterableInterval<UnsignedByteType> botView = Views.interval(img, botHalfInterval);
        Cursor<UnsignedByteType> botViewCur = botView.cursor();
        Cursor<FloatType> botCur = botImg.cursor();
        while(botCur.hasNext()) {
            botCur.fwd();
            botViewCur.fwd();
            botCur.get().set(botViewCur.get().getRealFloat());
        }
        return botImg;
    }

    public Img getTopImg(Img img) {
        FinalInterval topHalfInterval = Intervals.createMinMax(0, 0, img.dimension(2)/2, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)-1);
        FinalInterval topIntervalSize = Intervals.createMinMax(0, 0, 0, topHalfInterval.dimension(0)-1, topHalfInterval.dimension(1)-1, topHalfInterval.dimension(2)-1);
        Img<FloatType> topImg = ops.create().img(topIntervalSize, new FloatType());

        IterableInterval<UnsignedByteType> topView = Views.interval(img, topHalfInterval);
        Cursor<UnsignedByteType> topViewCur = topView.cursor();
        Cursor<FloatType> topCur = topImg.cursor();
        while(topCur.hasNext()) {
            topCur.fwd();
            topViewCur.fwd();
            topCur.get().set(topViewCur.get().getRealFloat());
        }
        return topImg;
    }

//    public static void main(String[] args) {
//        ImageJ imagej = new ImageJ();
//        imagej.ui().showUI();
//
//        Map<String, Object> argmap = new HashMap<>();
////        argmap.put("inputDirectory", "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/");
////        argmap.put("outputDirectory", "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/");
////        argmap.put("originalDimX", 9007);
////        argmap.put("originalDimZ", 9599);
////        argmap.put("outputBasename", "heightSurf200");
//
//        argmap.put("inputDirectory", args[1]);
//        argmap.put("outputDirectory", args[2]);
//        argmap.put("originalDimX", args[3]);
//        argmap.put("originalDimZ", args[4]);
//        argmap.put("outputBasename", args[5]);
//
//        imagej.command().run(SurfaceFitCommand.class, true, argmap);
//    }

  public static void main(String... args) {
    SciJava sj = new SciJava();
    sj.launch(args);
    sj.context().dispose();
  }
}
