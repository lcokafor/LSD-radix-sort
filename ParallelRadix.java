import java.util.concurrent.*;
import java.util.Random;

class ParallelRadix {
	final static int NUM_BIT = 7;
	int threads;
	int globalMax;
	int[] bit;
	int[] arr;
	int[] arrb;
	int[] t; 
	boolean done;
	CyclicBarrier cb;

	int[][] allCount;
	int[] sumCount;
	int mask;
	int shift;
	CyclicBarrier synk;

	ParallelRadix(int threads, int[] arr) {
		this.threads = threads;
		this.arr = arr;
		this.t = arr;
		this.arrb = new int[arr.length];
		this.allCount = new int[threads][];
		this.done = false;
		cb = new CyclicBarrier(threads + 1);
		synk = new CyclicBarrier(threads);
	}

    synchronized void global(int test) {
		if(test > globalMax) {
		    globalMax = test;
		}
    }

	int[] utfor() {
		int increment = arr.length/threads;
		int start = 0;
		int end = increment;

		Thread[] traader = new Thread[threads];

		for(int i = 0; i < threads; i++) {
		    traader[i] = new Thread(new RadixTraad(i, start, end));
		    traader[i].start();

			start += increment;
			end += increment;
			if(i == threads - 2) end = (arr.length); 
		}

		try {
			cb.await();
		}
		catch(InterruptedException e) {
		    System.out.println("InterruptedException in utfor1.");
		}	
		catch(BrokenBarrierException e) {
		    System.out.println("BrokenBarrierException in utfor1.");
		}

		int numBit = 2;
		int numDigits;

		while(globalMax >= (1L << numBit)) numBit++;

		numDigits = Math.max(1, numBit/NUM_BIT);
		bit = new int[numDigits];
		int rest = numBit%NUM_BIT;
		int sum = 0;

		for (int i = 0; i < bit.length; i++){
			bit[i] = numBit/numDigits;
		    if(rest-- > 0 && bit.length > 1) bit[i]++;
		}

		for(int i = 0; i < bit.length; i++) {
			this.mask = (1 << bit[i]) - 1;
			this.shift = sum;
			sumCount = new int[mask + 1];

			try {
				//wait for summation of count array and moving from arr<->arrb
				cb.await();
				cb.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in utfor2.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in utfor2.");
			}

			sum += bit[i];

			//swap arrays (pointers only)
			t = arr;
			arr = arrb;
			arrb = t;
		}

		done = true;

		try {
			cb.await();
			cb.await();
		}
		catch(InterruptedException e) {
		    System.out.println("InterruptedException in utfor2.");
		}	
		catch(BrokenBarrierException e) {
		    System.out.println("BrokenBarrierException in utfor2.");
		}

		// et odde antall sifre, kopier innhold tilbake til original a[] (naa b)
		if ((bit.length & 1) != 0 ) System.arraycopy (arr, 0, arrb, 0, arr.length);

		return arr;
	}

	void testSort(int [] a){
		for (int i = 0; i < a.length - 1; i++) {
			if (a[i] > a[i+1]) {
		    	System.out.println("SorteringsFEIL paa plass: " +
                					i + " a["+i+"]:" + a[i] + " > a["+(i+1)+"]:" + a[i+1]);
		    	return;
			}	
		}
	}

	class RadixTraad implements Runnable {
		int threadNum;
		int arrStart;
		int arrEnd;
		int count[];
		int localCount[];

		public RadixTraad(int threadNum, int arrStart, int arrEnd) {
			this.threadNum = threadNum;
			this.arrStart = arrStart;
			this.arrEnd = arrEnd;
		}

		public void run() {
			finnMax();

			try {
				cb.await();
				cb.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in run1.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in run1.");
			}

			while(!done) {
				kjorCount();
				summerCount();
				flytt();

				try {
					cb.await();
					cb.await();
				}
				catch(InterruptedException e) {
				    System.out.println("InterruptedException in run2.");
				}	
				catch(BrokenBarrierException e) {
				    System.out.println("BrokenBarrierException in run2.");
				}
			}

			try {
				cb.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in run2.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in run2.");
			}

		}

		public void finnMax() {
			int localMax = 0;

			for(int i = arrStart; i < arrEnd && i < arr.length; i++) {
			    if(arr[i] > localMax) {
					localMax = arr[i];
			    }
			}

			global(localMax);

			try {
				synk.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in finnMax.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in finnMax.");
			}
		}

		public void kjorCount() {
			this.count = new int[mask + 1];

			for (int i = arrStart; i < arrEnd; i++) {
				count[(arr[i] >>> shift) & mask]++;
			}

			allCount[threadNum] = count;

			try {
				synk.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in kjorCount.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in kjorCount.");
			}
		}

		public void summerCount() {
			int increment = sumCount.length/threads;
			int sumStart = increment*threadNum;
			int sumEnd = sumStart + increment;
			if(threadNum == threads - 1) sumEnd = sumCount.length;

			int tally = 0;
			for(int i = 0; i < sumStart; i++) {
				for(int j = 0; j < threads; j++) {
					tally += allCount[j][i];
				}
			}

			for(int i = sumStart; i < sumEnd; i++) {
				sumCount[i] = tally;

				for(int j = 0; j < threads; j++) {
					tally += allCount[j][i];
				}
			}

			try {
				synk.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in kjorCount.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in kjorCount.");
			}

			this.localCount = new int[sumCount.length];

			int summation = 0;
			for(int i = 0; i < sumCount.length; i++) {
				summation = sumCount[i];
				for(int j = 0; j < threadNum; j++) {
					summation += allCount[j][i];
				}

				localCount[i] = summation;
			}

			try {
				synk.await();
			}
			catch(InterruptedException e) {
			    System.out.println("InterruptedException in summerCount.");
			}	
			catch(BrokenBarrierException e) {
			    System.out.println("BrokenBarrierException in summerCount.");
			}
		}

		public void flytt() {
			for (int i = arrStart; i < arrEnd; i++) {
				arrb[localCount[(arr[i] >>> shift) & mask]++] = arr[i];
			}
		}
	}
}