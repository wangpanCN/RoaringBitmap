package me.lemire.roaringbitmap.experiments;

import it.uniroma3.mat.extendedset.intset.ConciseSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.annotation.Generated;

import me.lemire.roaringbitmap.*;
import me.lemire.roaringbitmap.experiments.LineCharts.LineChartDemo1;
import me.lemire.roaringbitmap.experiments.LineCharts.LineChartPoint;

import org.brettw.SparseBitSet;
import org.devbrat.util.WAHBitSet;
import sparsebitmap.SparseBitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import net.sourceforge.sizeof.*;

public class BenchSpeedyRBConcise {
	
	static ArrayList<Vector<LineChartPoint>> SizeGraphCoordinates;
	static ArrayList<Vector<LineChartPoint>> OrGraphCoordinates;
	static ArrayList<Vector<LineChartPoint>> XorGraphCoordinates;
	static ArrayList<Vector<LineChartPoint>> AndGraphCoordinates;
	static int nbTechnique = 9;
	static int FastAgregate = 1;
	static int ClassicAgregate = 0;
	private static UniformDistribution uniform;
	private static ZipfianDistribution zpf;
	private static ClusteredDataGenerator cdg;
	private static int distClustered = 2;
	private static int distUniform = 1;
	private static int distZipf = 0;
	private static int classic = 0;
	private static int Fast = 1;
	private static int inPlace = 2;
	private static int FastinPlace = 3;
	private static BufferedWriter bw = null;
	private static int SetSize = (int) Math.pow(10, 5);
	private static int BitmapsPerSet = 1;
	private static String CPU = "IntelCorei3_M330";
	//AmdBulldozer
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	        SizeOf.skipStaticField(true);
	        SizeOf.skipFinalField(true);
	        SizeOf.setMinSizeToLog(0);// disable warnings
		//test(10, 18, 10);
                if (args.length > 0) {                    
                	Tests(BitmapsPerSet, 10, args[0], distUniform);
                	Tests(BitmapsPerSet, 10, args[0], distZipf);
                	//Tests(nbBitmaps, 10, args[0], distClustered);
                }
                else {
                        Tests(BitmapsPerSet, 10, null, distUniform);// no plots needed
                        Tests(BitmapsPerSet, 10, null, distZipf);
                        Tests(BitmapsPerSet, 10, null, distClustered);
                	}
	}
	
	/*private static SpeedyRoaringBitmap fastOR(SpeedyRoaringBitmap[] tabRB) {
		return new FastAggregation.inplace_or(tabRB);
	}
	
	private static SpeedyRoaringBitmap fastXOR(SpeedyRoaringBitmap[] tabRB) {
		return //FastAggregation.inplace_xor(tabRB);
	}
	
	private static SpeedyRoaringBitmap fastAND(SpeedyRoaringBitmap[] tabRB) {
		return FastAggregation.inplace_and(tabRB);
	}*/
	
	/*private static RoaringBitmap simpleOR(RoaringBitmap[] tabRB) {
		RoaringBitmap rb = tabRB[0];
		for(int i=0; i<tabRB.length; i++) 
			rb = RoaringBitmap.or(rb, tabRB[1]);
		return rb;
	}*/
	
	/**
	 * Generating N sets of nbInts integers using Zipfian distribution.
	 * @param N number of generated sets of integers
	 * @param repeat number of repetitions
	 */
	public static void Tests(int N, int repeat, String path, int distribution) {				
		System.out.println("Distribution (0:zipf, 1:uniform, 2:clustered) :: "+distribution);
		zpf = new ZipfianDistribution();	
		uniform = new UniformDistribution();
		cdg = new ClusteredDataGenerator();	
		
		String distdir = null;
		
		
		//Creating the distribution folder
		if(path!=null)
		switch(distribution) {
		case 0 : distdir = path+File.separator+"ConciseTests_DynamicArray_"+CPU+File.separator+"Zipf"; break;
		case 1 : distdir = path+File.separator+"ConciseTests_DynamicArray_"+CPU+File.separator+"Uniform";break;
		case 2 : distdir = path+File.separator+"ConciseTests_DynamicArray_"+CPU+File.separator+"Clustered";break;
		default : System.out.println("Can you choose a distribution ?");
				  System.exit(0);
		}
		
		launchBenchmark(distribution, N, repeat, distdir, classic);
		//launchBenchmark(distribution, N, repeat, df, distdir, Fast);
		//launchBenchmark(distribution, N, repeat, df, distdir, inPlace);
		//launchBenchmark(distribution, N, repeat, df, distdir, FastinPlace);
	}
	
	private static int[] getRandomIntArray(int length) {
		int[] array = new int[length];
		Random rand = new Random();
		
		for(int i=0; i<length; i++) {
			double x = rand.nextDouble();
			array[i] = (int)(x*SetSize);
		}
		
		return array;
	}
	
	public static void launchBenchmark(int distribution, int N, int repeat, 
			String distdir, int optimisation) {
		
		String Chartsdir = null, Benchmarkdir = null, optdir = null;	
		DecimalFormat df = new DecimalFormat("0.###");
		
		//Creating the kind of optimization folder
		if(distdir!=null)
		switch(optimisation) {
		case 0 : optdir = distdir+File.separator+"RoaringBitmap_Classic"; break;
		case 1 : optdir = distdir+File.separator+"RoaringBitmap_FastAggregations";	break;
		case 2 : optdir = distdir+File.separator+"RoaringBitmap_inPlace"; break;
		case 3 : optdir = distdir+File.separator+"RoaringBitmap_FastAgg_inPlace"; break;
		default : System.out.println("Please, choose a distribution ?");
				  System.exit(0);
		}
		
		//Generating an array of random integers
		int[] randIntsArray = getRandomIntArray(10000);  
		
		//Creating the charts folder		
		if(optdir!=null)
		Chartsdir = optdir+File.separator+"Charts";
		
		//Creating the benchmark results folder
		if(optdir!=null)
		Benchmarkdir = optdir+File.separator+"Benchmark";
		
		try {
			boolean success = (new File(Chartsdir).mkdirs());
			boolean success2 = (new File(Benchmarkdir).mkdirs());
			if(success & success2) System.out.println("folders created with success");
		}	catch(Exception e) {e.getMessage();}
		
		try {			 
			File file = new File(Benchmarkdir+"/Benchmark.txt");
			
			// if file doesn't exists, then create it
			if (!file.exists()) 
				file.createNewFile();
						
			Date date = new Date();
		    SimpleDateFormat dateFormatComp;
		 
		    dateFormatComp = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a");
		    //System.out.println(dateFormatComp.format(date));
		    String[] op = {"Classic", "FastAggregations", "inPlace operations", "FastAggregations & inPlace operations"};
		    
		    System.out.println("# For each instance, we report the size, the construction time, ");
		    System.out.println("# the time required to recover the set bits,");
		    System.out
		    .println("# and the time required to compute logical ors (unions) between lots of bitmaps." +
		    		"\n\n" 
		    		+"# Number of bitmaps = "+N
		    		+"\n# Bitmaps cardinality = "+SetSize
		    		+"\n# Optimisation = "+op[optimisation]
		    		+"\n# "+dateFormatComp.format(date)
		    		+"\n# CPU = "+System.getProperty("os.arch")
		    		+"\n# "+System.getProperty("os.name")
		    		+"\n# Java "+System.getProperty("java.version"));		
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		bw=null;
		bw = new BufferedWriter(fw);
		bw.write("\n# For each instance, we report the size, the construction time, \n"
				+"# the time required to recover the set bits,"
				+"# and the time required to compute logical ors (unions) between lots of bitmaps."
				+"# and the time required to compute logical ors (unions) between lots of bitmaps." +
				"\n\n" +
				 "# Number of bitmaps = "+N
				+"\n# Bitmaps cardinality = "+SetSize 
				+"\n# Optimisation = "+op[optimisation]
				+"\n# "+dateFormatComp.format(date)
				+"\n# CPU = "+System.getenv("os.arch")
				+"\n# "+System.getProperty("os.name")
				+"\n# Java "+System.getProperty("java.version")
				);
	} catch (IOException e) {e.printStackTrace();}
		
		for(double k=0.001; k<1.0; k*=10) {
			SizeGraphCoordinates = new ArrayList<Vector<LineChartPoint>>();
			OrGraphCoordinates = new ArrayList<Vector<LineChartPoint>>();
			AndGraphCoordinates = new ArrayList<Vector<LineChartPoint>>();
			XorGraphCoordinates = new ArrayList<Vector<LineChartPoint>>();	
			
			for(int i =0; i< nbTechnique; i++) {
				SizeGraphCoordinates.add(new Vector<LineChartPoint>());
				OrGraphCoordinates.add(new Vector<LineChartPoint>());
				AndGraphCoordinates.add(new Vector<LineChartPoint>());
				XorGraphCoordinates.add(new Vector<LineChartPoint>());
			}
			for( double density = k; density<k*10.0; density+=density )
			{
				if(density>=0.7) 
					density=0.6;			
				
				int max = (int)(SetSize/density);
				int data[][] = new int[N][];
				int data2[][] = new int[N][];
				
				System.out.println("\n\ndensity = "+density);
				System.out.println("# generating random data...");
				try {
					bw.write("\n\n\ndensity = "+density+
							"\n# generating random data...");
				} catch (IOException e1) 
					{e1.printStackTrace();}
				
				for(int i =0; i< nbTechnique; i++) {
					SizeGraphCoordinates.get(i).add(new LineChartPoint(0.0, String.valueOf(density), null));
					OrGraphCoordinates.get(i).add(new LineChartPoint(0.0, String.valueOf(density), null));
					AndGraphCoordinates.get(i).add(new LineChartPoint(0.0, String.valueOf(density), null));
					XorGraphCoordinates.get(i).add(new LineChartPoint(0.0, String.valueOf(density), null));
				}
				
				// Generating the first set				
				for(int i=0; i<N; i++)
				{	
					switch (distribution) {
					case 0 : data[i] = zpf.GenartingInts(SetSize, max);
							 data2[i] = zpf.GenartingInts(SetSize, max);
							break;
					case 1 : data[i] = uniform.GenartingInts(SetSize, max);
							 data2[i] = uniform.GenartingInts(SetSize, max);
							 break;
					case 2 : int[] inter = cdg.generateClustered(1 << (18 / 2), max);
							 data[i] = IntUtil.unite(inter, cdg.generateClustered(1 << 18, max));
							 data2[i] = IntUtil.unite(inter, cdg.generateClustered(1 << 18, max));
							 break;
					default : System.out.println("Launching tests aborted");
							  System.exit(0);
					}				
					
					Arrays.sort(data[i]);
					Arrays.sort(data2[i]);				
					
					/*System.out.println("\n\n data1");
					int bigger = 0, aver = 0, val1 = data[i][0];
					for(int j=1; j<data[i].length; j++){
						bigger = bigger < Math.abs(data[i][j]-val1) ? (data[i][j]-val1) : bigger;
						aver += Math.abs(data[i][j] - val1);
						val1=data[i][j];
						//System.out.println(data[i][j]+" ");
					}
					System.out.println(bigger+" "+" average = "+(aver/data[i].length));*/
					/*System.out.println("\n data2");				
					for(int j=0; j<data2[i].length; j++)
						System.out.println(data2[i][j]+" ");*/				
				}
				
				// Start experiments with Zipfian data distribution
				System.out.println("# generating random data... ok.");
				System.out.println("#  density = "+ density+" nb setBits = "+SetSize+", bit sets per 32-bit word = "+(32*density));
				try {
					bw.write("\n# generating random data... ok.\n"
							+"#  density = "+ density+" nb setBits = "+SetSize);
				} catch (IOException e) 
					{e.printStackTrace();}
				
				// Launching benchmarks				
				testBitSet(data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
                testSpeedyRoaringBitmap(data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testRoaringBitmap(data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testWAH32(        data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testConciseSet(   data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testSparseBitmap( data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testSparseBitSet( data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testEWAH64(       data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				testEWAH32(       data.clone(), data2.clone(), repeat, df, optimisation, randIntsArray);
				System.out.println();		
			}		
                        if (Chartsdir != null) {
                                String p = Chartsdir + File.separator;
                                try {
                                        new LineChartDemo1(
                                                "Line_Chart_Compression_size_"
                                                        + k + "_" + (k * 10),
                                                "size (KB)",
                                                SizeGraphCoordinates, p);
                                        new LineChartDemo1(
                                                "Line_Chart_OR_times_" + k
                                                        + "_" + (k * 10),
                                                "Time (sec)",
                                                OrGraphCoordinates, p);
                                        new LineChartDemo1(
                                                "Line_Chart_AND_times_" + k
                                                        + "_" + (k * 10),
                                                "Time (sec)",
                                                AndGraphCoordinates, p);
                                        new LineChartDemo1(
                                                "Line_Chart_XOR_times_" + k
                                                        + "_" + (k * 10),
                                                "Time (sec)",
                                                XorGraphCoordinates, p);
                                } catch (Exception e) {
                                        System.out
                                                .println("Running in headless mode. Graphical visualization disabled.");
                                }

                        }
			}
		try {
    		bw.close();
    	} catch (IOException e) {e.printStackTrace();}
	}

	public static void testRoaringBitmap(int[][] data, int[][] data2,
			int repeat, DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# RoaringBitmap");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
						+ "time to compute unions (OR), intersections (AND) "
						+ "and exclusive unions (XOR) ");
		try {
			bw.write("\n"+"# RoaringBitmap\n"+"# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		
		// Calculating the construction time
		long bef, aft;
		String line = "";
		int bogus = 0;
		int N = data.length, size = 0;
		
		bef = System.currentTimeMillis();
		RoaringBitmap[] bitmap = new RoaringBitmap[N];		
		for (int r = 0; r < repeat; ++r) {
			size = 0;
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new RoaringBitmap();
				for (int x = 0; x < data[k].length; ++x) {
					bitmap[k].set(data[k][x]);
				}				
				//if(r==0) System.out.println(bitmap[k].toString());				
	                        bitmap[k].trim();
			}
		}
		aft = System.currentTimeMillis();
		
		for (RoaringBitmap rb : bitmap)
			rb.validate();
		
		// Building the second array of RoaringBitmaps
		RoaringBitmap[] bitmap2 = new RoaringBitmap[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new RoaringBitmap();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].set(data2[k][x]);
			bitmap2[k].trim();
		}
		for (RoaringBitmap rb : bitmap2)
			rb.validate();
		
		//System.out.println("Average nb of shorts per node in this bitmap = "+bitmap[bitmap.length-1].getAverageNbIntsPerNode());
		
		//Calculating the all RoaringBitmaps size 
		for(int k=0; k<N; k++) {
			size += bitmap[k].getSizeInBytes(); //first array (bitmap)
			size += bitmap2[k].getSizeInBytes(); //second array (bitmap2)
		}
		
		int cardinality = 0, BC = 0, nbIntAC = 0;
		long sizeOf = 0;
		//Size with verification
		for(int k=0; k<N; k++) {
			cardinality += bitmap[k].getCardinality();
                    cardinality += bitmap2[k].getCardinality();
		}		
		
		//Memory size in bytes
		sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2))); 
		
		line += "\t"+cardinality+"\t" + size +"\t"+ sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(7).lastElement().setGname("Roaring Bitmap");
		SizeGraphCoordinates.get(7).lastElement().setY(size/1024);
		
		for (RoaringBitmap rb : bitmap)
			rb.validate();
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = bitmap[k].getIntegers();
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);					

		// logical or + retrieval
		{
		RoaringBitmap bitmapor1 = null, bitmapor2;
		switch(optimisation)
		{			
		case 0 : bitmapor1 = bitmap[0];
				 bitmapor2 = bitmap2[0];	
				for (int k = 1; k < N; ++k) {
					bitmapor1 = RoaringBitmap.or(bitmapor1, bitmap[k]);
					bitmapor1.validate();
					bitmapor2 = RoaringBitmap.or(bitmapor2, bitmap2[k]);
					bitmapor2.validate();
				}
				bitmapor1 = RoaringBitmap.or(bitmapor1, bitmapor2);
				bitmapor1.validate();
				break;
		/*case 1 : bitmapor1 = null;
				 bitmapor2 = null;
				 bitmapor1 = FastAggregation.or(bitmap);
				 bitmapor1.validate();
				 bitmapor2 = FastAggregation.or(bitmap2);
				 bitmapor2.validate();
				 bitmapor1 = RoaringBitmap.or(bitmapor1, bitmapor2);
				 bitmapor1.validate();
				 break;*/
		case 2 : bitmapor1 = bitmap[0].clone();
				 bitmapor2 = bitmap2[0].clone();
				 for (int k = 1; k < N; ++k) {
					 bitmapor1.inPlaceOR(bitmap[k]);
					 bitmapor1.validate();
					 bitmapor2.inPlaceOR(bitmap2[k]);
					 bitmapor2.validate();
				 }
				 bitmapor1.inPlaceOR(bitmapor2);
				 bitmapor1.validate();
				 break;
		/*case 3 : bitmapor1 = null;
		 		bitmapor2 = null;
		 		bitmapor1 = FastAggregation.inplace_or(bitmap);
		 		bitmapor1.validate();
		 		bitmapor2 = FastAggregation.inplace_or(bitmap2);
		 		bitmapor2.validate();
		 		bitmapor1.inPlaceOR(bitmapor2);
		 		bitmapor1.validate();
		 		break;*/
			}
			int[] array = bitmapor1.getIntegers();
			bogus += array.length;
		}

		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			RoaringBitmap bitmapor1 = null, bitmapor2;
			switch(optimisation)
			{			
			case 0 : bitmapor1 = bitmap[0];
					 bitmapor2 = bitmap2[0];	
					for (int k = 1; k < N; ++k) {
						bitmapor1 = RoaringBitmap.or(bitmapor1, bitmap[k]);
						bitmapor2 = RoaringBitmap.or(bitmapor2, bitmap2[k]);
					}
					bitmapor1 = RoaringBitmap.or(bitmapor1, bitmapor2);
					break;
			/*case 1 : bitmapor1 = null;
					 bitmapor2 = null;
					 bitmapor1 = FastAggregation.or(bitmap);
					 bitmapor2 = FastAggregation.or(bitmap2);
					 bitmapor1 = RoaringBitmap.or(bitmapor1, bitmapor2);
					 break;*/
			case 2 : bitmapor1 = bitmap[0].clone();
					 bitmapor2 = bitmap2[0].clone();
					 for (int k = 1; k < N; ++k) {
						 bitmapor1.inPlaceOR(bitmap[k]);
						 bitmapor2.inPlaceOR(bitmap2[k]);
					 }
					 bitmapor1.inPlaceOR(bitmapor2);
					 break;
			/*case 3 : bitmapor1 = null;
			 		bitmapor2 = null;
			 		bitmapor1 = FastAggregation.inplace_or(bitmap);
			 		bitmapor2 = FastAggregation.inplace_or(bitmap2);
			 		bitmapor1.inPlaceOR(bitmapor2);
			 		break;*/
				}
			int[] array = bitmapor1.getIntegers();
			bogus += array.length;
		}

		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(7).lastElement().setGname("Roaring Bitmap");
		OrGraphCoordinates.get(7).lastElement().setY((aft - bef) / 1000.0);
		
		// logical and + retrieval
		{
			RoaringBitmap bitmapand1 = null, bitmapand2;
			switch(optimisation)
			{			
			case 0 : bitmapand1 = bitmap[0];
					 bitmapand2 = bitmap2[0];	
					for (int k = 1; k < N; ++k) {
						bitmapand1 = RoaringBitmap.or(bitmapand1, bitmap[k]);
						bitmapand1.validate();
						bitmapand2 = RoaringBitmap.or(bitmapand2, bitmap2[k]);
						bitmapand2.validate();
					}
					bitmapand1 = RoaringBitmap.or(bitmapand1, bitmapand2);
					bitmapand1.validate();
					break;
			/*case 1 : bitmapand1 = null;
					 bitmapand2 = null;
					 bitmapand1 = FastAggregation.and(bitmap);
					 bitmapand1.validate();
					 bitmapand2 = FastAggregation.and(bitmap2);
					 bitmapand2.validate();
					 bitmapand1 = RoaringBitmap.and(bitmapand1, bitmapand2);
					 bitmapand1.validate();
					 break;*/
			case 2 : bitmapand1 = bitmap[0].clone();
					 bitmapand2 = bitmap2[0].clone();
					 for (int k = 1; k < N; ++k) {
						 bitmapand1.inPlaceOR(bitmap[k]);
						 bitmapand1.validate();
						 bitmapand2.inPlaceOR(bitmap2[k]);
						 bitmapand2.validate();
					 }
					 bitmapand1.inPlaceOR(bitmapand2);
					 bitmapand1.validate();
					 break;
			/*case 3 : bitmapand1 = null;
			 		bitmapand2 = null;
			 		bitmapand1 = FastAggregation.inplace_or(bitmap);
			 		bitmapand1.validate();
			 		bitmapand2 = FastAggregation.inplace_or(bitmap2);
			 		bitmapand2.validate();
			 		bitmapand1.inPlaceOR(bitmapand2);
			 		bitmapand1.validate();
			 		break;*/
				}
				int[] array = bitmapand1.getIntegers();
				bogus += array.length;
			}

		
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			RoaringBitmap bitmapand1 = null, bitmapand2;
			switch(optimisation)
			{			
			case 0 : bitmapand1 = bitmap[0];
					 bitmapand2 = bitmap2[0];	
					for (int k = 1; k < N; ++k) {
						bitmapand1 = RoaringBitmap.and(bitmapand1, bitmap[k]);
						bitmapand2 = RoaringBitmap.and(bitmapand2, bitmap2[k]);
					}
					bitmapand1 = RoaringBitmap.and(bitmapand1, bitmapand2);
					break;
			/*case 1 : bitmapand1 = null;
					 bitmapand2 = null;
					 bitmapand1 = FastAggregation.and(bitmap);
					 bitmapand2 = FastAggregation.and(bitmap2);
					 bitmapand1 = RoaringBitmap.and(bitmapand1, bitmapand2);
					 break;*/
			case 2 : bitmapand1 = bitmap[0].clone();
					 bitmapand2 = bitmap2[0].clone();
					 for (int k = 1; k < N; ++k) {
						 bitmapand1.inPlaceAND(bitmap[k]);
						 bitmapand2.inPlaceAND(bitmap2[k]);
					 }
					 bitmapand1.inPlaceAND(bitmapand2);
					 break;
			/*case 3 : bitmapand1 = null;
			 		bitmapand2 = null;
			 		bitmapand1 = FastAggregation.inplace_and(bitmap);
			 		bitmapand2 = FastAggregation.inplace_and(bitmap2);
			 		bitmapand1.inPlaceAND(bitmapand2);
			 		break;*/
				}
				int[] array = bitmapand1.getIntegers();
				bogus += array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		AndGraphCoordinates.get(7).lastElement().setGname("Roaring Bitmap");
		AndGraphCoordinates.get(7).lastElement().setY((aft - bef) / 1000.0);

		// logical xor + retrieval
		{
			RoaringBitmap bitmapxor1 = null, bitmapxor2;
			switch(optimisation)
			{			
			case 0 : //classic
					 bitmapxor1 = bitmap[0];
					 bitmapxor2 = bitmap2[0];	
					for (int k = 1; k < N; ++k) {
						bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmap[k]);
						bitmapxor1.validate();
						bitmapxor2 = RoaringBitmap.xor(bitmapxor2, bitmap2[k]);
						bitmapxor2.validate();
					}
					bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmapxor2);
					bitmapxor1.validate();
					break;
			/*case 1 : //Using FastAggregations
					 bitmapxor1 = null;
					 bitmapxor2 = null;
					 bitmapxor1 = FastAggregation.xor(bitmap);
					 bitmapxor1.validate();
					 bitmapxor2 = FastAggregation.xor(bitmap2);
					 bitmapxor2.validate();
					 bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmapxor2);
					 bitmapxor1.validate();
					 break;*/
			case 2 : //Using inPlace operations
					 bitmapxor1 = bitmap[0].clone();
					 bitmapxor2 = bitmap2[0].clone();
					 for (int k = 1; k < N; ++k) {
						 bitmapxor1.inPlaceXOR(bitmap[k]);
						 bitmapxor1.validate();
						 bitmapxor2.inPlaceXOR(bitmap2[k]);
						 bitmapxor2.validate();
					 }
					 bitmapxor1.inPlaceXOR(bitmapxor2);
					 bitmapxor1.validate();
					 break;
			/*case 3 : //Using FastAggregations and inPlace operations 
					bitmapxor1 = null;
			 		bitmapxor2 = null;
			 		bitmapxor1 = FastAggregation.inplace_xor(bitmap);
			 		bitmapxor1.validate();
			 		bitmapxor2 = FastAggregation.inplace_xor(bitmap2);
			 		bitmapxor2.validate();
			 		bitmapxor1.inPlaceXOR(bitmapxor2);
			 		bitmapxor1.validate();
			 		break;*/
			}
			int[] array = bitmapxor1.getIntegers();
			bogus += array.length;
			}

		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			RoaringBitmap bitmapxor1 = null, bitmapxor2;
			switch(optimisation)
			{			
			case 0 : //classic
					 bitmapxor1 = bitmap[0];
					 bitmapxor2 = bitmap2[0];	
					for (int k = 1; k < N; ++k) {
						bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmap[k]);
						bitmapxor2 = RoaringBitmap.xor(bitmapxor2, bitmap2[k]);
					}
					bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmapxor2);
					break;
			/*case 1 : //Using FastAggregations
					 bitmapxor1 = null;
					 bitmapxor2 = null;
					 bitmapxor1 = FastAggregation.xor(bitmap);
					 bitmapxor2 = FastAggregation.xor(bitmap2);
					 bitmapxor1 = RoaringBitmap.xor(bitmapxor1, bitmapxor2);
					 break;*/
			case 2 : //Using inPlace operations
					 bitmapxor1 = bitmap[0].clone();
					 bitmapxor2 = bitmap2[0].clone();
					 for (int k = 1; k < N; ++k) {
						 bitmapxor1.inPlaceXOR(bitmap[k]);
						 bitmapxor2.inPlaceXOR(bitmap2[k]);
					 }
					 bitmapxor1.inPlaceXOR(bitmapxor2);
					 break;
			/*case 3 : //Using FastAggregations and inPlace operations 
					bitmapxor1 = null;
			 		bitmapxor2 = null;
			 		bitmapxor1 = FastAggregation.inplace_xor(bitmap);
			 		bitmapxor2 = FastAggregation.inplace_xor(bitmap2);
			 		bitmapxor1.inPlaceXOR(bitmapxor2);
			 		break;*/
			}
			int[] array = bitmapxor1.getIntegers();
			bogus += array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(7).lastElement().setGname("Roaring Bitmap");
		XorGraphCoordinates.get(7).lastElement().setY((aft - bef) / 1000.0);

		//Testing get time
		bef = System.currentTimeMillis();
		for (int i=0; i<N; i++)
			for(int k=0; k<randIntsArray.length; k++) {
				bitmap[i].contains(randIntsArray[k]);
				bitmap2[i].contains(randIntsArray[k]);
			}
		aft = System.currentTimeMillis();
		String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# Real size = "+size
				+" nbNodes = "+bitmap[0].getNbNodes()+" BC = "+BC+" nbIntsAC = "+nbIntAC
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
				bw.write("\n"+line
						+"\n# get time = "+getTime
						+"\n# Real size = "+size+" nbNodes = "+bitmap[0].getNbNodes()
						+" BC = "+BC+" nbIntsAC = "+nbIntAC
						+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
				bw.write("\n# ignore this " + bogus+"\n\n");
			} catch (IOException e) {e.printStackTrace();}
	}

    public static void testSpeedyRoaringBitmap(int[][] data, int[][] data2,
                int repeat, DecimalFormat df, int optimisation, int[] randIntsArray) {
        System.out.println("# SpeedyRoaringBitmap");
        System.out.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
                                        + "time to compute unions (OR), intersections (AND) "
                                        + "and exclusive unions (XOR) ");
        try {
                bw.write("\n"+"# SpeedyRoaringBitmap\n"+"# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
                                                + "time to compute unions (OR), intersections (AND) "
                                                + "and exclusive unions (XOR) ");
        } catch (IOException e1) {e1.printStackTrace();}
        
        // Calculating the construction time
        long bef, aft;
        String line = "";
        int bogus = 0;
        int N = data.length, size = 0;
        
        bef = System.currentTimeMillis();
        SpeedyRoaringBitmap[] bitmap = new SpeedyRoaringBitmap[N];              
        for (int r = 0; r < repeat; ++r) {
                size = 0;
                for (int k = 0; k < N; ++k) {
                        bitmap[k] = new SpeedyRoaringBitmap();
                        for (int x = 0; x < data[k].length; ++x) {
                                bitmap[k].set(data[k][x]);
                        }                               
                        //if(r==0) System.out.println(bitmap[k].toString());                            
                        bitmap[k].trim();
                }
        }
        aft = System.currentTimeMillis();
        
        for (SpeedyRoaringBitmap rb : bitmap)
                rb.validate();
        
        // Building the second array of RoaringBitmaps
        SpeedyRoaringBitmap[] bitmap2 = new SpeedyRoaringBitmap[N];
        for (int k = 0; k < N; ++k) {
                bitmap2[k] = new SpeedyRoaringBitmap();
                for (int x = 0; x < data2[k].length; ++x)
                        bitmap2[k].set(data2[k][x]);
                bitmap2[k].trim();
        }
        for (SpeedyRoaringBitmap rb : bitmap2)
                rb.validate();
        
        //System.out.println("Average nb of shorts per node in this bitmap = "+bitmap[bitmap.length-1].getAverageNbIntsPerNode());
        
        //Calculating the all RoaringBitmaps size 
        for(int k=0; k<N; k++) {
                size += bitmap[k].getSizeInBytes(); //first array (bitmap)
                size += bitmap2[k].getSizeInBytes(); //second array (bitmap2)
        }
        
        int cardinality = 0, BC = 0, nbIntAC = 0;
        long sizeOf = 0;
        for(int k=0; k<N; k++) {           
                cardinality += bitmap[k].getCardinality();
                cardinality += bitmap2[k].getCardinality();
        }               
        //Memory size in bytes
        sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2))); 
        line += "\t"+cardinality+"\t" + size +"\t"+ sizeOf;
        line += "\t" + df.format((aft - bef) / 1000.0);
        
        SizeGraphCoordinates.get(0).lastElement().setGname("Speedy Roaring");
        SizeGraphCoordinates.get(0).lastElement().setY(size/1024);
        
        for (SpeedyRoaringBitmap rb : bitmap)
                rb.validate();
        
        // uncompressing
        bef = System.currentTimeMillis();
        for (int r = 0; r < repeat; ++r)
                for (int k = 0; k < N; ++k) {
                        int[] array = bitmap[k].getIntegers();
                        bogus += array.length;
                }
        aft = System.currentTimeMillis();
        line += "\t" + df.format((aft - bef) / 1000.0);                                 

        // logical or + retrieval
        {
        SpeedyRoaringBitmap bitmapor1 = null, bitmapor2;
        switch(optimisation)
        {                       
        case 0 : bitmapor1 = bitmap[0];
                 bitmapor2 = bitmap2[0];        
                 for (int k = 1; k < N; ++k) {
                      bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmap[k]);
                      bitmapor1.validate();
                      bitmapor2 = SpeedyRoaringBitmap.or(bitmapor2, bitmap2[k]);
                      bitmapor2.validate();
                 }
                 bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmapor2);
                 bitmapor1.validate();
                 break;
        /*case 1 : bitmapor1 = null;
                         bitmapor2 = null;
                         bitmapor1 = FastAggregation.or(bitmap);
                         bitmapor1.validate();
                         bitmapor2 = FastAggregation.or(bitmap2);
                         bitmapor2.validate();
                         bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmapor2);
                         bitmapor1.validate();
                         break;*/
        /*case 2 : bitmapor1 = bitmap[0].clone();
                         bitmapor2 = bitmap2[0].clone();
                         for (int k = 1; k < N; ++k) {
                                 bitmapor1.inPlaceOR(bitmap[k]);
                                 bitmapor1.validate();
                                 bitmapor2.inPlaceOR(bitmap2[k]);
                                 bitmapor2.validate();
                         }
                         bitmapor1.inPlaceOR(bitmapor2);
                         bitmapor1.validate();
                         break;*/
        /*case 3 : bitmapor1 = null;
                        bitmapor2 = null;
                        bitmapor1 = FastAggregation.inplace_or(bitmap);
                        bitmapor1.validate();
                        bitmapor2 = FastAggregation.inplace_or(bitmap2);
                        bitmapor2.validate();
                        bitmapor1.inPlaceOR(bitmapor2);
                        bitmapor1.validate();
                        break;*/
                }
                int[] array = bitmapor1.getIntegers();
                bogus += array.length;
        }

        bef = System.currentTimeMillis();
        for (int r = 0; r < repeat; ++r) {
                SpeedyRoaringBitmap bitmapor1 = null, bitmapor2;
                switch(optimisation)
                {                       
                case 0 : bitmapor1 = bitmap[0];
                                 bitmapor2 = bitmap2[0];        
                                for (int k = 1; k < N; ++k) {
                                        bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmap[k]);
                                        bitmapor2 = SpeedyRoaringBitmap.or(bitmapor2, bitmap2[k]);
                                }
                                bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmapor2);
                                break;
                /*case 1 : bitmapor1 = null;
                                 bitmapor2 = null;
                                 bitmapor1 = FastAggregation.or(bitmap);
                                 bitmapor2 = FastAggregation.or(bitmap2);
                                 bitmapor1 = SpeedyRoaringBitmap.or(bitmapor1, bitmapor2);
                                 break;*/
                /*case 2 : bitmapor1 = bitmap[0].clone();
                                 bitmapor2 = bitmap2[0].clone();
                                 for (int k = 1; k < N; ++k) {
                                         bitmapor1.inPlaceOR(bitmap[k]);
                                         bitmapor2.inPlaceOR(bitmap2[k]);
                                 }
                                 bitmapor1.inPlaceOR(bitmapor2);
                                 break;*/
                /*case 3 : bitmapor1 = null;
                                bitmapor2 = null;
                                bitmapor1 = FastAggregation.inplace_or(bitmap);
                                bitmapor2 = FastAggregation.inplace_or(bitmap2);
                                bitmapor1.inPlaceOR(bitmapor2);
                                break;*/
                        }
                int[] array = bitmapor1.getIntegers();
                bogus += array.length;
        }

        aft = System.currentTimeMillis();
        line += "\t" + df.format((aft - bef) / 1000.0);
        
        OrGraphCoordinates.get(0).lastElement().setGname("Speedy Roaring");
        OrGraphCoordinates.get(0).lastElement().setY((aft - bef) / 1000.0);
        
        // logical and + retrieval
        {
                SpeedyRoaringBitmap bitmapand1 = null, bitmapand2;
                switch(optimisation)
                {                       
                case 0 : bitmapand1 = bitmap[0];
                                 bitmapand2 = bitmap2[0];       
                                for (int k = 1; k < N; ++k) {
                                        bitmapand1 = SpeedyRoaringBitmap.or(bitmapand1, bitmap[k]);
                                        bitmapand1.validate();
                                        bitmapand2 = SpeedyRoaringBitmap.or(bitmapand2, bitmap2[k]);
                                        bitmapand2.validate();
                                }
                                bitmapand1 = SpeedyRoaringBitmap.or(bitmapand1, bitmapand2);
                                bitmapand1.validate();
                                break;
                /*case 1 : bitmapand1 = null;
                                 bitmapand2 = null;
                                 bitmapand1 = FastAggregation.and(bitmap);
                                 bitmapand1.validate();
                                 bitmapand2 = FastAggregation.and(bitmap2);
                                 bitmapand2.validate();
                                 bitmapand1 = SpeedyRoaringBitmap.and(bitmapand1, bitmapand2);
                                 bitmapand1.validate();
                                 break;*/
                /*case 2 : bitmapand1 = bitmap[0].clone();
                                 bitmapand2 = bitmap2[0].clone();
                                 for (int k = 1; k < N; ++k) {
                                         bitmapand1.inPlaceOR(bitmap[k]);
                                         bitmapand1.validate();
                                         bitmapand2.inPlaceOR(bitmap2[k]);
                                         bitmapand2.validate();
                                 }
                                 bitmapand1.inPlaceOR(bitmapand2);
                                 bitmapand1.validate();
                                 break;*/
                /*case 3 : bitmapand1 = null;
                                bitmapand2 = null;
                                bitmapand1 = FastAggregation.inplace_or(bitmap);
                                bitmapand1.validate();
                                bitmapand2 = FastAggregation.inplace_or(bitmap2);
                                bitmapand2.validate();
                                bitmapand1.inPlaceOR(bitmapand2);
                                bitmapand1.validate();
                                break;*/
                        }
                        int[] array = bitmapand1.getIntegers();
                        bogus += array.length;
                }

        
        bef = System.currentTimeMillis();
        for (int r = 0; r < repeat; ++r) {
                SpeedyRoaringBitmap bitmapand1 = null, bitmapand2;
                switch(optimisation)
                {                       
                case 0 : bitmapand1 = bitmap[0];
                                 bitmapand2 = bitmap2[0];       
                                for (int k = 1; k < N; ++k) {
                                        bitmapand1 = SpeedyRoaringBitmap.and(bitmapand1, bitmap[k]);
                                        bitmapand2 = SpeedyRoaringBitmap.and(bitmapand2, bitmap2[k]);
                                }
                                bitmapand1 = SpeedyRoaringBitmap.and(bitmapand1, bitmapand2);
                                break;
                /*case 1 : bitmapand1 = null;
                                 bitmapand2 = null;
                                 bitmapand1 = FastAggregation.and(bitmap);
                                 bitmapand2 = FastAggregation.and(bitmap2);
                                 bitmapand1 = SpeedyRoaringBitmap.and(bitmapand1, bitmapand2);
                                 break;*/
                /*case 2 : bitmapand1 = bitmap[0].clone();
                                 bitmapand2 = bitmap2[0].clone();
                                 for (int k = 1; k < N; ++k) {
                                         bitmapand1.inPlaceAND(bitmap[k]);
                                         bitmapand2.inPlaceAND(bitmap2[k]);
                                 }
                                 bitmapand1.inPlaceAND(bitmapand2);
                                 break;*/
                /*case 3 : bitmapand1 = null;
                                bitmapand2 = null;
                                bitmapand1 = FastAggregation.inplace_and(bitmap);
                                bitmapand2 = FastAggregation.inplace_and(bitmap2);
                                bitmapand1.inPlaceAND(bitmapand2);
                                break;*/
                        }
                        int[] array = bitmapand1.getIntegers();
                        bogus += array.length;
        }
        aft = System.currentTimeMillis();
        line += "\t" + df.format((aft - bef) / 1000.0);
        
        AndGraphCoordinates.get(0).lastElement().setGname("Speedy Roaring");
        AndGraphCoordinates.get(0).lastElement().setY((aft - bef) / 1000.0);

        // logical xor + retrieval
        {
                SpeedyRoaringBitmap bitmapxor1 = null, bitmapxor2;
                switch(optimisation)
                {                       
                case 0 : //classic
                                 bitmapxor1 = bitmap[0];
                                 bitmapxor2 = bitmap2[0];       
                                for (int k = 1; k < N; ++k) {
                                        bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmap[k]);
                                        bitmapxor1.validate();
                                        bitmapxor2 = SpeedyRoaringBitmap.xor(bitmapxor2, bitmap2[k]);
                                        bitmapxor2.validate();
                                }
                                bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmapxor2);
                                bitmapxor1.validate();
                                break;
                /*case 1 : //Using FastAggregations
                                 bitmapxor1 = null;
                                 bitmapxor2 = null;
                                 bitmapxor1 = FastAggregation.xor(bitmap);
                                 bitmapxor1.validate();
                                 bitmapxor2 = FastAggregation.xor(bitmap2);
                                 bitmapxor2.validate();
                                 bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmapxor2);
                                 bitmapxor1.validate();
                                 break;*/
                /*case 2 : //Using inPlace operations
                                 bitmapxor1 = bitmap[0].clone();
                                 bitmapxor2 = bitmap2[0].clone();
                                 for (int k = 1; k < N; ++k) {
                                         bitmapxor1.inPlaceXOR(bitmap[k]);
                                         bitmapxor1.validate();
                                         bitmapxor2.inPlaceXOR(bitmap2[k]);
                                         bitmapxor2.validate();
                                 }
                                 bitmapxor1.inPlaceXOR(bitmapxor2);
                                 bitmapxor1.validate();
                                 break;*/
                /*case 3 : //Using FastAggregations and inPlace operations 
                                bitmapxor1 = null;
                                bitmapxor2 = null;
                                bitmapxor1 = FastAggregation.inplace_xor(bitmap);
                                bitmapxor1.validate();
                                bitmapxor2 = FastAggregation.inplace_xor(bitmap2);
                                bitmapxor2.validate();
                                bitmapxor1.inPlaceXOR(bitmapxor2);
                                bitmapxor1.validate();
                                break;*/
                }
                int[] array = bitmapxor1.getIntegers();
                bogus += array.length;
                }

        bef = System.currentTimeMillis();
        for (int r = 0; r < repeat; ++r) {
                SpeedyRoaringBitmap bitmapxor1 = null, bitmapxor2;
                switch(optimisation)
                {                       
                case 0 : //classic
                                 bitmapxor1 = bitmap[0];
                                 bitmapxor2 = bitmap2[0];       
                                for (int k = 1; k < N; ++k) {
                                        bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmap[k]);
                                        bitmapxor2 = SpeedyRoaringBitmap.xor(bitmapxor2, bitmap2[k]);
                                }
                                bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmapxor2);
                                break;
                /*case 1 : //Using FastAggregations
                                 bitmapxor1 = null;
                                 bitmapxor2 = null;
                                 bitmapxor1 = FastAggregation.xor(bitmap);
                                 bitmapxor2 = FastAggregation.xor(bitmap2);
                                 bitmapxor1 = SpeedyRoaringBitmap.xor(bitmapxor1, bitmapxor2);
                                 break;*/
                /*case 2 : //Using inPlace operations
                                 bitmapxor1 = bitmap[0].clone();
                                 bitmapxor2 = bitmap2[0].clone();
                                 for (int k = 1; k < N; ++k) {
                                         bitmapxor1.inPlaceXOR(bitmap[k]);
                                         bitmapxor2.inPlaceXOR(bitmap2[k]);
                                 }
                                 bitmapxor1.inPlaceXOR(bitmapxor2);
                                 break;*/
                /*case 3 : //Using FastAggregations and inPlace operations 
                                bitmapxor1 = null;
                                bitmapxor2 = null;
                                bitmapxor1 = FastAggregation.inplace_xor(bitmap);
                                bitmapxor2 = FastAggregation.inplace_xor(bitmap2);
                                bitmapxor1.inPlaceXOR(bitmapxor2);
                                break;*/
                }
                int[] array = bitmapxor1.getIntegers();
                bogus += array.length;
        }
        aft = System.currentTimeMillis();
        line += "\t" + df.format((aft - bef) / 1000.0);
        
        XorGraphCoordinates.get(0).lastElement().setGname("Speedy Roaring");
        XorGraphCoordinates.get(0).lastElement().setY((aft - bef) / 1000.0);

      //Testing get time
      		bef = System.currentTimeMillis();
      		for (int i=0; i<N; i++)
      			for(int k=0; k<randIntsArray.length; k++) {
      				bitmap[i].contains(randIntsArray[k]);
      				bitmap2[i].contains(randIntsArray[k]);
      			}
      		aft = System.currentTimeMillis();
      		String getTime = df.format((aft - bef) / 1000.0);
        
        System.out.println(line
        				+"\n# get time = "+getTime
           				+"\n# Real size = "+size
                        +" nbNodes = "+bitmap[0].getNbNodes()+" BC = "+BC+" nbIntsAC = "+nbIntAC
                        +"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
        System.out.println("# ignore this " + bogus);
        try {
                        bw.write("\n"+line
                        		+"\n# get time = "+getTime
                        		+"\n# Real size = "+size+" nbNodes = "+bitmap[0].getNbNodes()
                                +" BC = "+BC+" nbIntsAC = "+nbIntAC
                                +"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
                        bw.write("\n# ignore this " + bogus+"\n\n");
                } catch (IOException e) {e.printStackTrace();}
}
	
	public static void testBitSet(int[][] data, int[][] data2, int repeat,
		DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# BitSet");
		System.out.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
                + "time to compute unions (OR), intersections (AND) "
                + "and exclusive unions (XOR) ");
		try {
			bw.write("\n"+"# BitSet\n"+"# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
                    + "time to compute unions (OR), intersections (AND) "
                    + "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		String line = "";
		int N = data.length, size = 0, bogus = 0;
		
		bef = System.currentTimeMillis();
		BitSet[] bitmap = new BitSet[N];		
		for (int r = 0; r < repeat; ++r) {
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new BitSet();
				for (int x = 0; x < data[k].length; ++x) {
					bitmap[k].set(data[k][x]);
				}				
			}
		}
		aft = System.currentTimeMillis();
		

		// Creating and filling the 2nd bitset index
		BitSet[] bitmap2 = new BitSet[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new BitSet();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].set(data2[k][x]);
		}
		
		for(int k=0; k<N; k++) {
			size += bitmap[k].size() / 8;
			size += bitmap2[k].size() / 8;
		}
		
		long sizeOf = 0; 
		int cardinality = 0;
		//Size with verification
		for(int k=0; k<N; k++) {
			cardinality += bitmap[k].cardinality();
                    cardinality += bitmap2[k].cardinality();
		}		
		
		//Memory size in bytes
		sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2))); 
		
		line += "\t"+cardinality+"\t" + size +"\t"+ sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(8).lastElement().setGname("BitSet");
        SizeGraphCoordinates.get(8).lastElement().setY(size/1024);
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = new int[bitmap[k].cardinality()];
				int pos = 0;
				for (int i = bitmap[k].nextSetBit(0); i >= 0; i = bitmap[k]
						.nextSetBit(i + 1)) {
					array[pos++] = i;
				}
				bogus+=array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// logical or + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			BitSet bitmapor1 = (BitSet) bitmap[0].clone();
			
			bitmapor1.or(bitmap2[0]);
			
			int[] array = new int[bitmapor1.cardinality()];
			int pos = 0;
			for (int i = bitmapor1.nextSetBit(0); i >= 0; i = bitmapor1
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus+=array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(8).lastElement().setGname("BitSet");
        OrGraphCoordinates.get(8).lastElement().setY((aft - bef) / 1000.0);

		// logical and + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			BitSet bitmapand1 = (BitSet) bitmap[0].clone();
			
			bitmapand1.and(bitmap2[0]);
			int[] array = new int[bitmapand1.cardinality()];
			int pos = 0;
			for (int i = bitmapand1.nextSetBit(0); i >= 0; i = bitmapand1
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus+=array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		AndGraphCoordinates.get(8).lastElement().setGname("BitSet");
        AndGraphCoordinates.get(8).lastElement().setY((aft - bef) / 1000.0);

		// logical xor + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			BitSet bitmapxor1 = (BitSet) bitmap[0].clone();
			
			bitmapxor1.xor(bitmap2[0]);
			
			int[] array = new int[bitmapxor1.cardinality()];
			int pos = 0;
			for (int i = bitmapxor1.nextSetBit(0); i >= 0; i = bitmapxor1
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus+=array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		XorGraphCoordinates.get(8).lastElement().setGname("BitSet");
        XorGraphCoordinates.get(8).lastElement().setY((aft - bef) / 1000.0);
        
      //Testing get time
  		bef = System.currentTimeMillis();
  		for (int i=0; i<N; i++)
  			for(int k=0; k<randIntsArray.length; k++) {
  				bitmap[i].get(randIntsArray[k]);
  				bitmap2[i].get(randIntsArray[k]);
  			}
  		aft = System.currentTimeMillis();
  		String getTime = df.format((aft - bef) / 1000.0);
        
        System.out.println(line
				+"\n# get time = "+getTime
   				+"\n# Real size = "+size
                +"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
try {
                bw.write("\n"+line
                		+"\n# get time = "+getTime
                		+"\n# Real size = "+size                        
                        +"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
                bw.write("\n# ignore this " + bogus+"\n\n");
                bw.write("\n"+line+"\n\n");
                
        } catch (IOException e) {e.printStackTrace();}
		
		System.out.println(line);		
	}

	public static void testWAH32(int[][] data, int[][] data2, int repeat,
			DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# WAH 32 bit using the compressedbitset library");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, "
						+ "time to compute unions (OR), intersections (AND)");
		try {
			bw.write("\n"+"# WAH32bits\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		int bogus = 0;
		String line = "";
		int N = data.length, size = 0;
		
		//Calculating the construction time
		bef = System.currentTimeMillis();
		WAHBitSet[] bitmap = new WAHBitSet[N];
		
		for (int r = 0; r < repeat; ++r) {
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new WAHBitSet();
				for (int x = 0; x < data[k].length; ++x) {
					bitmap[k].set(data[k][x]);
				}			
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd WAH index
		WAHBitSet[] bitmap2 = new WAHBitSet[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new WAHBitSet();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].set(data2[k][x]);
		}
		
		for(int k=0; k<N; k++) {
			size += bitmap[k].memSize() * 4;
			size += bitmap2[k].memSize() * 4;
		}
		
		int cardinality = 0;
		//calculating the all cardinality
		for(int k=0; k<N; k++){
			cardinality += bitmap[k].cardinality();
			cardinality += bitmap2[k].cardinality();
		}
		
		//Memory size in bytes
		long sizeOf = 0;
		sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2)));
		
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(1).lastElement().setGname("WAH 32bit");
		SizeGraphCoordinates.get(1).lastElement().setY(size/1024);		
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = new int[bitmap[k].cardinality()];
				int c = 0;
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> i = bitmap[k].iterator(); i.hasNext(); array[c++] = i
						.next().intValue()) {
				}
				bogus += c;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
       // logical or + retrieval
       bef = System.currentTimeMillis();
       for (int r = 0; r < repeat; ++r) {
   			WAHBitSet bitmapor1 = null, bitmapor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapor1 = bitmap[0];
                		 bitmapor2 = bitmap2[0];	
                		 
                		 bitmapor1 = bitmapor1.or(bitmapor2);
               		 		
                		 break;
                case 1 : 
                case 3 : bitmapor1 = WAHBitSetUtil.fastOR(bitmap);
               			 bitmapor2 = WAHBitSetUtil.fastOR(bitmap2);
               			 bitmapor1 = bitmapor1.or(bitmapor2);
               			 break;                		  
                }
                int[] array = new int[bitmapor1.cardinality()];
                int c = 0;
                for (@SuppressWarnings("unchecked")
                Iterator<Integer> i = bitmapor1.iterator(); i.hasNext(); array[c++] = i
                                        .next().intValue()) {
                }
                bogus += c;
                }
                aft = System.currentTimeMillis();
                line += "\t" + df.format((aft - bef) / 1000.0);

		
		OrGraphCoordinates.get(1).lastElement().setGname("WAH 32bit");
		OrGraphCoordinates.get(1).lastElement().setY((aft - bef) / 1000.0);

		// logical and + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			WAHBitSet bitmapand1 = null, bitmapand2;
    		switch(optimisation)
    		{		
    		case 0 :
    		case 2 : bitmapand1 = bitmap[0];
    				 bitmapand2 = bitmap2[0];	
    				 
    				 bitmapand1 = bitmapand1.and(bitmapand2);
   		 		 	 break;
    		case 1 : 
    		case 3 : bitmapand1 = WAHBitSetUtil.fastAND(bitmap);
   		 			 bitmapand2 = WAHBitSetUtil.fastAND(bitmap2);
   		  			 bitmapand1 = bitmapand1.and(bitmapand2);
   		 			 break;                		  
    		}
    		int[] array = new int[bitmapand1.cardinality()];
        	int c = 0;
        	for (@SuppressWarnings("unchecked")
        	Iterator<Integer> i = bitmapand1.iterator(); i.hasNext(); array[c++] = i
                            .next().intValue()) {
        	}
        	bogus += c;
		}
        aft = System.currentTimeMillis();
        line += "\t" + df.format((aft - bef) / 1000.0);
		
		AndGraphCoordinates.get(1).lastElement().setGname("WAH 32bit");
		AndGraphCoordinates.get(1).lastElement().setY((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(1).lastElement().setGname("WAH 32bit");
		XorGraphCoordinates.get(1).lastElement().setY(0.0);

		//Testing get time
				bef = System.currentTimeMillis();
				for (int i=0; i<N; i++)
					for(int k=0; k<randIntsArray.length; k++) {
						bitmap[i].get(randIntsArray[k]);
						bitmap2[i].get(randIntsArray[k]);
					}
				aft = System.currentTimeMillis();
				String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line
					+"\n# get time = "+getTime
					+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
	}

	public static void testConciseSet(int[][] data, int[][] data2, int repeat,
			DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out
				.println("# ConciseSet 32 bit using the extendedset_2.2 library");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, time to compute unions  and intersections ");
		try {
			bw.write("\n"+"# ConciseSet\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		String line = "";
		int bogus = 0;

		int N = data.length, size = 0;
		bef = System.currentTimeMillis();
		ConciseSet[] bitmap = new ConciseSet[N];
		
		for (int r = 0; r < repeat; ++r) {			
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new ConciseSet();
				for (int x = 0; x < data[k].length; ++x) {
					try{bitmap[k].add(data[k][x]);
					
					}catch(IndexOutOfBoundsException e){e.printStackTrace(); 
														System.out.println("taille = "+data[k].length+
																" want to add "+data[k][x]);
														System.exit(0);
														}
				}
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd Concise index
		ConciseSet[] bitmap2 = new ConciseSet[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new ConciseSet();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].add(data2[k][x]);
		}
		
		for (int k=0; k<N; k++) {
		size += (int) (bitmap[k].size() * bitmap[k].collectionCompressionRatio()) * 4;
		size += (int) (bitmap2[k].size() * bitmap2[k].collectionCompressionRatio()) * 4;
		}
		
		int cardinality = 0;
		//calculating all bitmaps the cardinality
		for(int k=0; k<N; k++) {
			cardinality += bitmap[k].toArray().length;
			cardinality += bitmap2[k].toArray().length;
		}
		
		//Memory size in bytes
				long sizeOf = 0;
				sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2)));
				
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(2).lastElement().setGname("Concise");
		SizeGraphCoordinates.get(2).lastElement().setY(size/1024);
		
		
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = bitmap[k].toArray();
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// logical or + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
   			ConciseSet bitmapor1 = null;
			ConciseSet bitmapor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapor1 = bitmap[0].clone();	
                		 
                		 bitmapor1 = bitmapor1.union(bitmap2[0]);
               		 		
                		 break;
                case 1 : 
                case 3 : bitmapor1 = ConciseSetUtil.fastOR(bitmap);
               			 bitmapor2 = ConciseSetUtil.fastOR(bitmap2);
               			 bitmapor1 = bitmapor1.union(bitmapor2);
               			 break;                		  
            }
       		int[] array = bitmapor1.toArray();
            if(array!=null) bogus += array.length;
		}
            aft = System.currentTimeMillis();
                line += "\t" + df.format((aft - bef) / 1000.0);
		

		
		OrGraphCoordinates.get(2).lastElement().setGname("Concise");
		OrGraphCoordinates.get(2).lastElement().setY((aft - bef) / 1000.0);

		// logical and + retrieval
                bef = System.currentTimeMillis();
                for (int r = 0; r < repeat; ++r) {
           			ConciseSet bitmapand1 = null;
        			ConciseSet bitmapand2;
               		switch(optimisation) {		
                        case 0 :
                        case 2 : bitmapand1 = bitmap[0].clone();
                        		 bitmapand1 = bitmapand1.intersection(bitmap2[0]);
                       		 	break;
                        case 1 : 
                        case 3 : bitmapand1 = ConciseSetUtil.fastAND(bitmap);
                       			 bitmapand2 = ConciseSetUtil.fastAND(bitmap2);
                       			 bitmapand1 = bitmapand1.intersection(bitmapand2);
                       			 break;                		  
                    }
               		int[] array = bitmapand1.toArray();
                    if(array!=null) bogus += array.length;
                }
                aft = System.currentTimeMillis();
                line += "\t" + df.format((aft - bef) / 1000.0);

		
		AndGraphCoordinates.get(2).lastElement().setGname("Concise");
		AndGraphCoordinates.get(2).lastElement().setY((aft - bef) / 1000.0);
		
		// logical xor + retrieval
                bef = System.currentTimeMillis();
                for (int r = 0; r < repeat; ++r) {
                	ConciseSet bitmapxor1 = null;
        			ConciseSet bitmapxor2;
               		switch(optimisation) {		
                        case 0 :
                        case 2 : bitmapxor1 = bitmap[0].clone();	
                        		 bitmapxor1 = bitmapxor1.symmetricDifference(bitmap2[0]);
                       		 	 break;
                        case 1 : 
                        case 3 : bitmapxor1 = ConciseSetUtil.fastXOR(bitmap);
                       			 bitmapxor2 = ConciseSetUtil.fastXOR(bitmap2);
                       			 bitmapxor1 = bitmapxor1.symmetricDifference(bitmapxor2);
                       			 break;                		  
                    }
               		int[] array = bitmapxor1.toArray();
                    if(array!=null) bogus += array.length;
                }
                aft = System.currentTimeMillis();
                line += "\t" + df.format((aft - bef) / 1000.0);

		
		XorGraphCoordinates.get(2).lastElement().setGname("Concise");
		XorGraphCoordinates.get(2).lastElement().setY((aft - bef) / 1000.0);

		//Testing get time
		bef = System.currentTimeMillis();
		/*for (int i=0; i<N; i++)
			for(int k=0; k<randIntsArray.length; k++) {
				bitmap[i].get(randIntsArray[k]);
				bitmap2[i].get(randIntsArray[k]);
			}*/
		aft = System.currentTimeMillis();
		String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line
					+"\n# get time = "+getTime
					+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
	}

	public static void testSparseBitmap(int[][] data, int[][] data2,
			int repeat, DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# simple sparse bitmap implementation");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, time to compute unions (OR), intersections (AND) and exclusive unions (XOR) ");
		try {
			bw.write("\n"+"# simple sparse bitmap\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		int bogus = 0;
		String line = "";
		int N = data.length;
		bef = System.currentTimeMillis();
		SparseBitmap[] bitmap = new SparseBitmap[N];
		
		for (int r = 0; r < repeat; ++r) {			
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new SparseBitmap();
				for (int x = 0; x < data[k].length; ++x) {
					bitmap[k].set(data[k][x]);
				}
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd SparseBitmap index
		SparseBitmap[] bitmap2 = new SparseBitmap[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new SparseBitmap();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].set(data2[k][x]);
		}
		
		//Calculating the size
		int size = 0, cardinality = 0;
		for (int k=0; k<N; k++) {
			size += bitmap[k].sizeInBytes();
			size += bitmap2[k].sizeInBytes();
			cardinality += bitmap[k].cardinality;
			cardinality += bitmap2[k].cardinality;
		}		
		
		//Memory size in bytes
		long sizeOf = 0;
		sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2)));
				
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(3).lastElement().setGname("Sparse Bitmap");
		SizeGraphCoordinates.get(3).lastElement().setY(size/1024);
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = bitmap[k].toArray();
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// logical or + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			SparseBitmap bitmapor1 = new SparseBitmap();
			SparseBitmap bitmapor2 = new SparseBitmap();
			for (int k = 0; k < N; ++k) {
				bitmapor1.or(bitmap[k]);
				bitmapor2.or(bitmap2[k]);
			}
			bitmapor1.or(bitmapor2);
			int[] array = bitmapor1.toArray();
			bogus += array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(3).lastElement().setGname("Sparse Bitmap");
		OrGraphCoordinates.get(3).lastElement().setY((aft - bef) / 1000.0);

		// logical xor + retrieval
		bef = System.currentTimeMillis();
	try {
		for (int r = 0; r < repeat; ++r) {
			SparseBitmap bitmapxor1;
			
			bitmapxor1 = (SparseBitmap) bitmap[0].clone();
			bitmapxor1.xor(bitmap2[0]);
			
			int[] array = bitmapxor1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		
		aft = System.currentTimeMillis();
		String xorTime = "\t" + df.format((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(3).lastElement().setGname("Sparse Bitmap");
		XorGraphCoordinates.get(3).lastElement().setY((aft - bef) / 1000.0);

		// logical and + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			SparseBitmap bitmapand1;
			
			bitmapand1 = (SparseBitmap) bitmap[0].clone();
			bitmap[0].and(bitmap2[0]);
			
			int[] array = bitmap[0].toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0) + xorTime;
		
		AndGraphCoordinates.get(3).lastElement().setGname("Sparse Bitmap");
		AndGraphCoordinates.get(3).lastElement().setY((aft - bef) / 1000.0);
		
		System.out.println(line+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
	}

	public static void testSparseBitSet(int[][] data, int[][] data2,
		int repeat, DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# sparse BitSet implementation");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, time to compute unions (OR), intersections (AND) and exclusive unions (XOR) ");
		try {
			bw.write("\n"+"# sparse BitSet\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		int bogus = 0;
		String line = "";
		int N = data.length;
		bef = System.currentTimeMillis();
		SparseBitSet[] bitmap = new SparseBitSet[N];
		
		for (int r = 0; r < repeat; ++r) {			
			for (int k = 0; k < N; ++k) {
				bitmap[k] = new SparseBitSet();
				for (int x = 0; x < data[k].length; ++x) {
					bitmap[k].set(data[k][x]);
				}
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd SparseBitmap index
		SparseBitSet[] bitmap2 = new SparseBitSet[N];
		for (int k = 0; k < N; ++k) {
			bitmap2[k] = new SparseBitSet();
			for (int x = 0; x < data2[k].length; ++x)
				bitmap2[k].set(data2[k][x]);
		}
		
		//Calculating the size
		int size = 0, cardinality = 0;
		for (int k=0; k<N; k++) {
			size += bitmap[k].length()/8; ;
			size += bitmap2[k].length()/8;
			cardinality += bitmap[k].cardinality();
			cardinality += bitmap2[k].cardinality();
		}		
		
		//Memory size in bytes
		long sizeOf = 0;
		sizeOf = ((SizeOf.deepSizeOf(bitmap)+SizeOf.deepSizeOf(bitmap2)));
				
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(4).lastElement().setGname("Sparse BitSet");
		SizeGraphCoordinates.get(4).lastElement().setY(size/1024);
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = new int[bitmap[k].cardinality()];
				int pos = 0;
				for (int i = bitmap[k].nextSetBit(0); i >= 0; i = bitmap[k]
						.nextSetBit(i + 1)) {
					array[pos++] = i;
				}
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// logical or + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			SparseBitSet bitmapor1 = bitmap[0].clone();
			bitmapor1.or(bitmap2[0]);
			
			//retrieving set bits positions
			int[] array = new int[bitmapor1.cardinality()];
			int pos = 0;
			for (int i = bitmapor1.nextSetBit(0); i >= 0; i = bitmapor1
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus += array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(4).lastElement().setGname("Sparse BitSet");
		OrGraphCoordinates.get(4).lastElement().setY((aft - bef) / 1000.0);

		// logical xor + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			SparseBitSet bitmapxor1 = bitmap[0].clone();
			bitmapxor1.xor(bitmap2[0]);
			
			//retrieving set bits positions
			int[] array = new int[bitmapxor1.cardinality()];
			int pos = 0;
			for (int i = bitmapxor1.nextSetBit(0); i >= 0; i = bitmapxor1
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus += array.length;
		}
		aft = System.currentTimeMillis();
		String xorTime = "\t" + df.format((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(4).lastElement().setGname("Sparse BitSet");
		XorGraphCoordinates.get(4).lastElement().setY((aft - bef) / 1000.0);

		// logical and + retrieval
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r) {
			SparseBitSet bitmapand1 = bitmap[0].clone();
			bitmapand1.and(bitmap2[0]);
			
			//retrieving set bits positions
			int[] array = new int[bitmap[0].cardinality()];
			int pos = 0;
			for (int i = bitmap[0].nextSetBit(0); i >= 0; i = bitmap[0]
					.nextSetBit(i + 1)) {
				array[pos++] = i;
			}
			bogus += array.length;
		}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0) + xorTime;
		
		AndGraphCoordinates.get(4).lastElement().setGname("Sparse BitSet");
		AndGraphCoordinates.get(4).lastElement().setY((aft - bef) / 1000.0);

		//Testing get time
		bef = System.currentTimeMillis();
		for (int i=0; i<N; i++)
			for(int k=0; k<randIntsArray.length; k++) {
				bitmap[i].get(randIntsArray[k]);
				bitmap2[i].get(randIntsArray[k]);
			}
		aft = System.currentTimeMillis();
		String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line
					+"\n# get time = "+getTime
					+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
}

	public static void testEWAH64(int[][] data, int[][] data2, int repeat,
			DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# EWAH using the javaewah library");
		System.out
				.println("# cardinality, size (bytes), memory size (bytes), construction time, time to recover set bits, time to compute unions  and intersections ");
		try {
			bw.write("\n"+"# EWAH64bits\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		String line = "";
		int bogus = 0;
		int N = data.length;
		//Calculating the construction time and building the 1st array of ewah bitmaps 
		bef = System.currentTimeMillis();
		EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
		
		for (int r = 0; r < repeat; ++r) {			
			for (int k = 0; k < N; ++k) {
				ewah[k] = new EWAHCompressedBitmap();
				for (int x = 0; x < data[k].length; ++x) {
					ewah[k].set(data[k][x]);
				}
				ewah[k].trim();
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd array of ewah64 bitmaps
		EWAHCompressedBitmap[] ewah2 = new EWAHCompressedBitmap[N];
		for (int k = 0; k < N; ++k) {
			ewah2[k] = new EWAHCompressedBitmap();
			for (int x = 0; x < data2[k].length; ++x)
				ewah2[k].set(data2[k][x]);
                        ewah2[k].trim();
		}
		
		int size = 0;
		for (int k=0; k<N; k++) {
		        size += ewah[k].sizeInBytes();
			size += ewah2[k].sizeInBytes();
		}
		
		//Calculating the cardinality from the 1st array bitmaps
		int cardinality = 0;
		for(int k=0; k<N; k++) {
			cardinality += ewah[k].cardinality();
			cardinality += ewah2[k].cardinality();
		}
		
		//Memory size in bytes
		long sizeOf = 0;
		sizeOf = ((SizeOf.deepSizeOf(ewah)+SizeOf.deepSizeOf(ewah2)));
				
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(5).lastElement().setGname("Ewah 64bits");
		SizeGraphCoordinates.get(5).lastElement().setY(size/1024);
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = ewah[k].toArray();
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// fast logical or + retrieval
		bef = System.currentTimeMillis();
	try {
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap bitmapor1 = null, bitmapor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : 
							bitmapor1 = ewah[0].clone();	
                		 
                		 	bitmapor1 = bitmapor1.or(ewah2[0]);
               		 		break;
                case 1 : 
                case 3 : bitmapor1 = EWAHCompressedBitmap.or(Arrays
    					.copyOf(ewah, N));
               			 bitmapor2 = EWAHCompressedBitmap.or(Arrays
             					.copyOf(ewah2, N));
               			 bitmapor1 = bitmapor1.or(bitmapor2);
               			 break;                		  
            }       		
			int[] array = bitmapor1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(5).lastElement().setGname("Ewah 64bits");
		OrGraphCoordinates.get(5).lastElement().setY((aft - bef) / 1000.0);

		// fast logical and + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap bitmapand1 = null, bitmapand2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapand1 = ewah[0].clone();
                		 bitmapand1 = bitmapand1.and(ewah2[0]);
               		 	 break;
                case 1 : 
                case 3 : bitmapand1 = EWAHCompressedBitmap.and(Arrays
    					.copyOf(ewah, N));
               			 bitmapand2 = EWAHCompressedBitmap.and(Arrays
             					.copyOf(ewah2, N));
               			 bitmapand1 = bitmapand1.and(bitmapand2);
               			 break;                		  
            }       		
			int[] array = bitmapand1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		AndGraphCoordinates.get(5).lastElement().setGname("Ewah 64bits");
		AndGraphCoordinates.get(5).lastElement().setY((aft - bef) / 1000.0);

		// fast logical xor + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap bitmapxor1 = null, bitmapxor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapxor1 = ewah[0].clone();
                		 bitmapxor1 = bitmapxor1.xor(ewah2[0]);
               		 	 break;
                case 1 : 
                case 3 : bitmapxor1 = EWAHCompressedBitmap.xor(Arrays
    									.copyOf(ewah, N));
               			 bitmapxor2 = EWAHCompressedBitmap.xor(Arrays
               					 					.copyOf(ewah2, N));
               			 bitmapxor1 = bitmapxor1.xor(bitmapxor2);
               			 break;                		  
            }       		
			int[] array = bitmapxor1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(5).lastElement().setGname("Ewah 64bits");
		XorGraphCoordinates.get(5).lastElement().setY((aft - bef) / 1000.0);

		//Testing get time
		bef = System.currentTimeMillis();
		for (int i=0; i<N; i++)
			for(int k=0; k<randIntsArray.length; k++) {
				ewah[i].get(randIntsArray[k]);
				ewah2[i].get(randIntsArray[k]);
			}
		aft = System.currentTimeMillis();
		String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line
					+"\n# get time = "+getTime
					+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
	}

	public static void testEWAH32(int[][] data, int[][] data2, int repeat,
			DecimalFormat df, int optimisation, int[] randIntsArray) {
		System.out.println("# EWAH 32-bit using the javaewah library");
		System.out
				.println("# cardinality, size(bytes), memory size(bytes), construction time, time to recover set bits, time to compute unions  and intersections ");
		try {
			bw.write("\n"+"# EWAH32bits\n"+"# cardinality, size, construction time, time to recover set bits, "
							+ "time to compute unions (OR), intersections (AND) "
							+ "and exclusive unions (XOR) ");
		} catch (IOException e1) {e1.printStackTrace();}
		long bef, aft;
		String line = "";
		long bogus = 0;
		int N = data.length;
		
		//Calculating the construction time and building the 1st array of ewah32 bitmaps 
		bef = System.currentTimeMillis();
		EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
		
		for (int r = 0; r < repeat; ++r) {			
			for (int k = 0; k < N; ++k) {
				ewah[k] = new EWAHCompressedBitmap32();
				for (int x = 0; x < data[k].length; ++x) {
					ewah[k].set(data[k][x]);
				}
				ewah[k].trim();
			}
		}
		aft = System.currentTimeMillis();
		
		// Creating and filling the 2nd array of ewah32 bitmaps
		EWAHCompressedBitmap32[] ewah2 = new EWAHCompressedBitmap32[N];
		for (int k = 0; k < N; ++k) {
			ewah2[k] = new EWAHCompressedBitmap32();
			for (int x = 0; x < data2[k].length; ++x)
				ewah2[k].set(data2[k][x]);
                        ewah2[k].trim();
		}
		
		int size = 0;
		for (int k=0; k<N; k++) {
			size += ewah[k].sizeInBytes();
			size += ewah2[k].sizeInBytes();
		}
		
		int cardinality = 0;
		//calculating the cardinality per EwahBitmap
		for(int k=0; k<N; k++) {
			cardinality += ewah[k].cardinality();
			cardinality += ewah2[k].cardinality();
		}
		
		//Memory size in bytes
		long sizeOf = 0;
		sizeOf = ((SizeOf.deepSizeOf(ewah)+SizeOf.deepSizeOf(ewah2)));
				
		line += "\t"+cardinality+"\t" + size+"\t"+sizeOf;
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		SizeGraphCoordinates.get(6).lastElement().setGname("Ewah 32");
		SizeGraphCoordinates.get(6).lastElement().setY(size/1024);
		
		
		// uncompressing
		bef = System.currentTimeMillis();
		for (int r = 0; r < repeat; ++r)
			for (int k = 0; k < N; ++k) {
				int[] array = ewah[k].toArray();
				bogus += array.length;
			}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);

		// fast logical or + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap32 bitmapor1 = null, bitmapor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapor1 = ewah[0].clone();
                		 bitmapor1 = bitmapor1.or(ewah2[0]);
               		 	 break;
                case 1 : 
                case 3 : bitmapor1 = EWAHCompressedBitmap32.or(Arrays
    					.copyOf(ewah, N));
               			 bitmapor2 = EWAHCompressedBitmap32.or(Arrays
             					.copyOf(ewah2, N));
               			 bitmapor1 = bitmapor1.or(bitmapor2);
               			 break;                		  
            }       		
			int[] array = bitmapor1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		OrGraphCoordinates.get(6).lastElement().setGname("Ewah 32");
		OrGraphCoordinates.get(6).lastElement().setY((aft - bef) / 1000.0);

		// fast logical and + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap32 bitmapand1 = null, bitmapand2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapand1 = ewah[0].clone();
                		 bitmapand1 = bitmapand1.and(ewah2[0]);
               		 	 break;
                case 1 : 
                case 3 : bitmapand1 = EWAHCompressedBitmap32.and(Arrays
    					.copyOf(ewah, N));
               			 bitmapand2 = EWAHCompressedBitmap32.and(Arrays
             					.copyOf(ewah2, N));
               			 bitmapand1 = bitmapand1.and(bitmapand2);
               			 break;                		  
            }       		
			int[] array = bitmapand1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		AndGraphCoordinates.get(6).lastElement().setGname("Ewah 32");
		AndGraphCoordinates.get(6).lastElement().setY((aft - bef) / 1000.0);

		// fast logical xor + retrieval
		bef = System.currentTimeMillis();
	try{
		for (int r = 0; r < repeat; ++r) {
			EWAHCompressedBitmap32 bitmapxor1 = null, bitmapxor2;
       		switch(optimisation) {		
                case 0 :
                case 2 : bitmapxor1 = ewah[0].clone();
                		 bitmapxor1 = bitmapxor1.xor(ewah2[0]);
               		 	 break;
                case 1 : 
                case 3 : bitmapxor1 = EWAHCompressedBitmap32.xor(Arrays
    					.copyOf(ewah, N));
               			 bitmapxor2 = EWAHCompressedBitmap32.xor(Arrays
             					.copyOf(ewah2, N));
               			 bitmapxor1 = bitmapxor1.xor(bitmapxor2);
               			 break;                		  
            }       		
			int[] array = bitmapxor1.toArray();
			bogus += array.length;
		}
	} catch (CloneNotSupportedException e) {e.printStackTrace();}
		aft = System.currentTimeMillis();
		line += "\t" + df.format((aft - bef) / 1000.0);
		
		XorGraphCoordinates.get(6).lastElement().setGname("Ewah 32");
		XorGraphCoordinates.get(6).lastElement().setY((aft - bef) / 1000.0);

		//Testing get time
		bef = System.currentTimeMillis();
		for (int i=0; i<N; i++)
			for(int k=0; k<randIntsArray.length; k++) {
				ewah[i].get(randIntsArray[k]);
				ewah2[i].get(randIntsArray[k]);
			}
		aft = System.currentTimeMillis();
		String getTime = df.format((aft - bef) / 1000.0);
		
		System.out.println(line
				+"\n# get time = "+getTime
				+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
		System.out.println("# ignore this " + bogus);
		try {
			bw.write("\n"+line
					+"\n# get time = "+getTime
					+"\n# bits/int = "+df.format(((float)size*8/(float)cardinality)));
			bw.write("\n# ignore this " + bogus+"\n\n");
		} catch (IOException e) {e.printStackTrace();}
	}		
}
