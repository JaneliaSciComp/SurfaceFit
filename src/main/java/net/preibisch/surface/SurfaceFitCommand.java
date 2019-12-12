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
        //IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");

        Img img = ImageJFunctions.wrap(imp);

        FinalInterval botHalfInterval = Intervals.createMinMax(0, 0, 0, img.dimension(0)-1, img.dimension(1)/2-1, img.dimension(2)-1);
        Img<FloatType> botImg = ops.create().img(botHalfInterval, new FloatType());

        IterableInterval<UnsignedByteType> botView = Views.interval(img, botHalfInterval);
        Cursor<UnsignedByteType> botViewCur = botView.cursor();
        Cursor<FloatType> botCur = botImg.cursor();
        while(botCur.hasNext()) {
            botCur.fwd();
            botViewCur.fwd();
            botCur.get().set(botViewCur.get().getRealFloat());
        }

        FinalInterval topHalfInterval = Intervals.createMinMax(0, img.dimension(1)/2-1, 0, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)-1);
        Img<FloatType> topImg = ops.create().img(topHalfInterval, new FloatType());

        img = botImg;

		final Img<IntType> surface = process2( img, 5, 40, 20 );

		final Img<FloatType> rendererSurface = img.factory().create( img );

		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );

        ImagePlus source = Util.getImagePlusInstance(img);
        source.setTitle("Input");
		source.show();

		float factor = originalDimX / surface.dimension(0) / 10;

		Cursor<IntType> cur = surface.cursor();
		while( cur.hasNext() ){
		    cur.fwd();
		    cur.get().mul(factor);
        }

        ImagePlus impSurface = Util.getImagePlusInstance(surface);
        impSurface.setTitle("Generated surface");

//        IJ.run(impSurface, "32-bit", "");
//        IJ.run(impSurface, "Rotate 90 Degrees Left", "");
//        IJ.run(impSurface, "Flip Horizontally", "");
//        IJ.run(impSurface, "Scale...", "x=- y=- width=9007 height=9599 interpolation=Bicubic average create");

		impSurface.show();

        ImagePlus impRenderSurface = Util.getImagePlusInstance(rendererSurface);
        impRenderSurface.setTitle("RendererSurface");
        impRenderSurface.show();

        //IJ.run(impRenderSurface, "Reslice [/]...", "output=1.000 start=Top avoid");

        IJ.run("Merge Channels...", "c2=Input c6=RendererSurface create keep");

//        Dataset dagmar;
//        try {
//            dagmar = dataset.open(targetImage);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//        ImgPlus<FloatType> dagmarImg = dagmar.typedImg(new FloatType());
//
//        Cursor<FloatType> dagmarCur = dagmarImg.cursor();
//        while( dagmarCur.hasNext() ) {
//            dagmarCur.fwd();
//            dagmarCur.get().mul(0.1f);
//        }
//
//		final Img<FloatType> dagmarRendererSurface = img.factory().create( img );
//		renderDepthMap2( dagmarRendererSurface, dagmarImg );
//		ImagePlus impDagmarSurface = Util.getImagePlusInstance(dagmarRendererSurface);
//        impDagmarSurface.setTitle("DagmarSurface");
//        impDagmarSurface.show();
//
//        //IJ.run("Merge Channels...", "c2=Input c6=RendererSurface create keep");
//        IJ.run("Merge Channels...", "c1=Input c2=RendererSurface c3=DagmarSurface create keep");

        //String outFilename = outputDirectory + "test.h5";

        //HDF5ImageJ.hdf5write( impRenderSurface, outFilename, "volume");
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        imagej.command().run(SurfaceFitCommand.class, true);
    }
}
