package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
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
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

import static net.preibisch.surface.Test.*;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    //@Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    //@Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/";

    //private String targetImage = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/heightSurf200-bot.tif";
    //private String targetImage = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/heightSurf200-top.tif";
    private String targetImage = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/heightSurf200-bot-downsampleRotLeft.tif";

    //@Parameter
    private long originalDimX = 9007;

    //@Parameter
    private long originalDimZ = 9599;

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
        System.out.println("Using as imp: " + imp);

        Img img = ImageJFunctions.wrap(imp);

        //FinalInterval botHalfInterval = Intervals.createMinMax(0, 0, 0, img.dimension(0)-1, img.dimension(1)/2-1, img.dimension(2)-1);
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

        FinalInterval topHalfInterval = Intervals.createMinMax(0, 0, img.dimension(2)/2-1, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)-1);
        Img<FloatType> topImg = ops.create().img(topHalfInterval, new FloatType());

        IterableInterval<UnsignedByteType> topView = Views.interval(img, topHalfInterval);
        Cursor<UnsignedByteType> topViewCur = topView.cursor();
        Cursor<FloatType> topCur = topImg.cursor();
        while(botCur.hasNext()) {
            botCur.fwd();
            botViewCur.fwd();
            botCur.get().set(botViewCur.get().getRealFloat());
        }

        img = botImg;

		final Img<IntType> surface = process2( img, 5, 40, 20 );

		final Img< FloatType > rendererSurface = img.factory().create( img );

		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );

        ImagePlus input = Util.getImagePlusInstance(img);
        input.setTitle("Target");
		input.show();

        ImagePlus surfaceImp = Util.getImagePlusInstance(surface);
		surfaceImp.show();

        ImagePlus rendererSurfaceImp = Util.getImagePlusInstance(rendererSurface);
        rendererSurfaceImp.setTitle("Generated surface");
		rendererSurfaceImp.show();

		IJ.run(input, "Reslice [/]...", "output=1.000 start=Top avoid");
		IJ.run(rendererSurfaceImp, "Reslice [/]...", "output=1.000 start=Top avoid");
		//IJ.run("Merge Channels...", "c2=[Reslice of Target] c6=[Reslice of Generated] create keep");
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        imagej.command().run(SurfaceFitCommand.class, true);
    }
}
