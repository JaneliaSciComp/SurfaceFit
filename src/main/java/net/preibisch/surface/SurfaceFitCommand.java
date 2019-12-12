package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.hdf5.HDF5ImageJ;
import sc.fiji.hdf5.HDF5_Writer_Vibez;

import static net.preibisch.surface.Test.process2;
import static net.preibisch.surface.Test.renderDepthMap;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    //@Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    //@Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/";

    //private String targetImage = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/heightSurf200-bot.tif";
    private String targetImage = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/heightSurf200-top.tif";

    //@Parameter
    private long originalDimX = 9007;

    //@Parameter
    private long originalDimZ = 9599;

    @Override
    public void run() {
        // Open image sequence from input directory
        ImagePlus imp = FolderOpener.open(inputDirectory, " file=tif");
        // Reslice image
        IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");

        Img img = ImageJFunctions.wrap(imp);
        
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
        impRenderSurface.show();

        //IJ.run(impRenderSurface, "Reslice [/]...", "output=1.000 start=Top avoid");

        ImagePlus target = IJ.openImage(targetImage);
        target.setTitle("Target");
        target.show();

        //IJ.run("Merge Channels...", "c2=Target c6=[Generated surface-1] create keep");

        //String outFilename = outputDirectory + "test.h5";

        //HDF5ImageJ.hdf5write( impRenderSurface, outFilename, "volume");
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        imagej.command().run(SurfaceFitCommand.class, true);
    }
}
