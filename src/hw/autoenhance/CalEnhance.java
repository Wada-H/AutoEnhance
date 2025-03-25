package hw.autoenhance;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

//z軸に対してはz軸に対しての平均値をもともめ、それをかけることにする。

public class CalEnhance {
	ImagePlus imp; //duplicate(all image)
	ImagePlus imp_forCalc; //crop
	int nCh;
	int nZ;
	int nT;
	int nSize;
	int p_num;
	int selectedZ;
	int selectedFrame;

	Roi r;
	
	public CalEnhance(ImagePlus i) {
		r = i.getRoi();
		i.killRoi();
		imp = i.duplicate();
		i.setRoi(r);

		imp_forCalc = i.crop("stack");

		
		nCh = imp.getNChannels();
		nZ = imp.getNSlices();
		nT = imp.getNFrames();
		nSize = imp.getStackSize();
		selectedZ = i.getZ();
		selectedFrame = i.getT();
	}

	public ImagePlus autoEnhance(){
		long s_t = System.currentTimeMillis();

		if(imp.getBitDepth() == 24){
			autoEnhanceRGB();
		}else{
			autoEnhanceGray();
		}

		long e_t = System.currentTimeMillis();
		System.out.println("process time : " + (e_t - s_t));

		return imp;
	}

	public void autoEnhanceForZ(){//deconvolution後、zの輝度がブレるのを補正するため。
		double[] ave_array = new double[nCh];
		IntStream z_stream = IntStream.range(0, nZ);
		p_num = 0;
		System.out.println("selectedZ : " + selectedZ);
		for(int c = 0; c < nCh; c++){ //1枚目の輝度測定 -> selected frameに変更
			double[] z_ave_array = new double[nZ];
			for(int z = 0; z < nZ; z++){
				int index = imp_forCalc.getStackIndex((c+1), selectedZ, 1);
				z_ave_array[z] = getAveInt(imp_forCalc.getStack().getProcessor(index));

			}
			ave_array[c] = getAveInt(z_ave_array);
		}

		z_stream.parallel().forEach(z->{
			for(int c = 0; c < nCh; c++){
				double[] z_ave_array = new double[nZ];
				int index = imp_forCalc.getStackIndex((c+1), (z+1), (1));

				double ave_a = ave_array[c];
				double ave_b = getAveInt(imp_forCalc.getStack().getProcessor(index));

				double magnification =  ave_a / ave_b;
				imp.getStack().getProcessor(index).multiply(magnification);

			}
			p_num++;
			IJ.showProgress(p_num, nT);
		});

	}

	public void autoEnhanceForT(){
		double[][] ave_array = new double[nT][nCh];
		IntStream t_stream = IntStream.range(0, nT);
		p_num = 0;
		for(int c = 0; c < nCh; c++){ //1枚目の輝度測定 -> selected frameに変更
			double[] z_ave_array = new double[nZ];
			for(int z = 0; z < nZ; z++){
				int index = imp_forCalc.getStackIndex((c+1), (z+1), (selectedFrame));
				z_ave_array[z] = getAveInt(imp_forCalc.getStack().getProcessor(index));

			}
			ave_array[selectedFrame - 1][c] = getAveInt(z_ave_array);
			System.out.println("C-" + c + " : " + ave_array[selectedFrame - 1][c]);
		}


		t_stream.parallel().forEach(t->{
			for(int c = 0; c < nCh; c++){
				double[] z_ave_array = new double[nZ];
				for(int z = 0; z < nZ; z++){
					int index = imp_forCalc.getStackIndex((c+1), (z+1), (t+1));
					z_ave_array[z] = getAveInt(imp_forCalc.getStack().getProcessor(index));

				}
				ave_array[t][c] = getAveInt(z_ave_array);

				double ave_a = ave_array[selectedFrame-1][c];
				double ave_b = ave_array[t][c];

				double magnification =  ave_a / ave_b;
				for(int z = 0; z < nZ; z++){
					int index = imp.getStackIndex((c+1), (z+1), (t+1));
					imp.getStack().getProcessor(index).multiply(magnification);
				}

			}
			p_num++;
			IJ.showProgress(p_num, nT);
		});
	}

	public void autoEnhanceGray(){
		if((imp.getNFrames() == 1)&&(imp.getNSlices() > 1)){
			autoEnhanceForZ();
		}else{
			autoEnhanceForT();
		}
	}


	public void autoEnhanceRGB(){ //HSBに変換後Bを処理、もう一度RGBに戻す

		double[] ave_array = new double[nT];
		IntStream t_stream = IntStream.range(1, nT);
		p_num = 0;

		double[] z_ave_array = new double[nZ];
		for(int z = 0; z < nZ; z++){
			int index = imp_forCalc.getStackIndex((1), (z+1), (0+1));
			z_ave_array[z] = getBrightnessAveInt((ColorProcessor) imp_forCalc.getStack().getProcessor(index));

		}
		ave_array[0] = getAveInt(z_ave_array);


		t_stream.parallel().forEach(t->{//2枚目以降は並列処理
			double[] zAve_array = new double[nZ];
			for(int z = 0; z < nZ; z++){
				int index = imp_forCalc.getStackIndex((1), (z+1), (t+1));
				zAve_array[z] = getBrightnessAveInt((ColorProcessor) imp_forCalc.getStack().getProcessor(index));

			}
			ave_array[t] = getAveInt(zAve_array);

			double ave_a = ave_array[0];
			double ave_b = ave_array[t];

			double magnification =  ave_a / ave_b;
			System.out.println("mag : " + magnification);
			for(int z = 0; z < nZ; z++){
				int index = imp.getStackIndex((1), (z+1), (t+1));
				ColorProcessor buff = (ColorProcessor) imp.getStack().getProcessor(index).duplicate();
				imp.getStack().setProcessor(getProcessedRGB(buff, magnification), index);
			}

			p_num++;
			IJ.showProgress(p_num, nT);
		});

	}

	public void convertAveIntensity(ImageProcessor teacherImage, ImageProcessor img){
		double t_ave = getAveInt(teacherImage);
		double img_ave = getAveInt(img);
		double magnification = t_ave / img_ave;
		img.multiply(magnification);
	}

	public ImageProcessor getProcessedRGB(ColorProcessor img, double magnification){
		ImageStack hsbStack = img.getHSBStack();
		hsbStack.getProcessor(3).multiply(magnification);
		ImagePlus buff = new ImagePlus();
		buff.setStack(hsbStack);
		ImageConverter imageConverter = new ImageConverter(buff);
		imageConverter.convertHSBToRGB();
		return buff.getProcessor();
	}

	public double getAveInt(double[] d_array){
		DoubleStream d_stream = DoubleStream.of(d_array);
		double result = d_stream.parallel().average().getAsDouble();
		return result;
	}
	
	public double getAveInt(ImageProcessor img){
		double result = 0.0;
		int[][] pixels = img.getIntArray();
		double[] pixels_d = intArrayConvertDoubleArray(toFlatten(pixels));
		return getAveInt(pixels_d);
	}

	public double getBrightnessAveInt(ColorProcessor cimp){
		double result = 0.0;
		ImageProcessor img = cimp.getHSBStack().getProcessor(3);//pick up Brightness
		int[][] pixels = img.getIntArray();
		double[] pixels_d = intArrayConvertDoubleArray(toFlatten(pixels));
		DoubleStream d_stream = DoubleStream.of(pixels_d);
		result = d_stream.parallel().average().getAsDouble();

		return result;
	}


	public int[] toFlatten(int[][] array2D){
		int[] a = Arrays.stream(array2D).flatMapToInt(Arrays::stream).toArray();
		return a;
	}
	
	public double[] intArrayConvertDoubleArray(int[] ia){
		double[] da = new double[ia.length];
		for(int i = 0; i < ia.length; i++){
			da[i] = (double)ia[i];
		}
		return da;
	}
	
}