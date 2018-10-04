import java.util.Arrays;
import java.io.*;

class Oblig4 {
	public static void main(String[] args) throws FileNotFoundException {
		if(args.length != 3) {
			System.out.println("commandline arguments -> java Oblig4 [n] [seed] [# of threads]");
			return;
		}

		int n = Integer.parseInt(args[0]);
		int seed = Integer.parseInt(args[1]);
		int threads = Integer.parseInt(args[2]);

		speedup(n, seed, threads);
	}

	public static void speedup(int n, int seed, int threads) {	
		double[] medianSekvensiell = new double[7];
		double[] medianParallel = new double[7];

		for(int i = 0; i < 7; i++) {
			int[] arr1 = Oblig4Precode.generateArray(n, seed);
			SekvensiellRadix sr = new SekvensiellRadix();
			int[] arr2 = Oblig4Precode.generateArray(n, seed);
			ParallelRadix pr = new ParallelRadix(threads, arr2);

			long t1 = System.nanoTime();
			sr.radixMulti(arr1);
			medianSekvensiell[i] = (System.nanoTime() - t1)/1000000.0;
			sr.testSort(arr1);


			long t2 = System.nanoTime();
			pr.utfor();
			medianParallel[i] = (System.nanoTime() - t2)/1000000.0;
			pr.testSort(arr2);

			System.out.format("Sequential: %.2fms Parallel: %.2fms Speedup: %.1f%n", medianSekvensiell[i], medianParallel[i], medianSekvensiell[i]/medianParallel[i]);
		}

		Arrays.sort(medianSekvensiell);
		Arrays.sort(medianParallel);

		double msekv = medianSekvensiell[3];
		double mpara = medianParallel[3];

		System.out.format("Median -> Sequential: %.2fms Parallel: %.2fms Speedup: %.1f%n", msekv, mpara, msekv/mpara);
    }
}