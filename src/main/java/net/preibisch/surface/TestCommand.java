package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.File;

import static net.preibisch.surface.Test.process2;
import static net.preibisch.surface.Test.renderDepthMap;

@Plugin(type = Command.class)
public class TestCommand implements Command {

    @Override
    public void run() {
        new ImageJ();

		final Img<FloatType> img = Util.openAs32Bit( new File( "resampled_orth_upper.tif" ) );

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
		IJ.run("Merge Channels...", "c2=[Reslice of Target] c6=[Reslice of Generated] create keep");
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        imagej.command().run(TestCommand.class, true);
    }
}
