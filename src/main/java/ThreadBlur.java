import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


public class ThreadBlur implements Callable<Void> {
    private int[] mSource;
    private int mStart;
    private int mLength;
    private int[] mDestination;
    private int mBlurWidth = 185; // Processing window size, should be odd.
    protected static int sThreshold = 100000000;
    private static int current = 0;

    public ThreadBlur(int[] src, int start, int length, int[] dst) {
        mSource = src;
        mStart = start;
        mLength = length;
        mDestination = dst;
    }

    protected void computeDirectly() {
        int sidePixels = (mBlurWidth - 1) / 2;
        computeDirectly(sidePixels, mStart, mLength, mSource, mBlurWidth, mDestination);
    }

    static void computeDirectly(int sidePixels, int mStart, int mLength, int[] mSource, int mBlurWidth, int[] mDestination) {
        for (int index = mStart; index < mStart + mLength; index++) {
            // Calculate average.
            float rt = 0, gt = 0, bt = 0;
            for (int mi = -sidePixels; mi <= sidePixels; mi++) {
                int mindex = Math.min(Math.max(mi + index, 0), mSource.length - 1);
                int pixel = mSource[mindex];
                rt += (float) ((pixel & 0x00ff0000) >> 16) / mBlurWidth;
                gt += (float) ((pixel & 0x0000ff00) >> 8) / mBlurWidth;
                bt += (float) ((pixel & 0x000000ff) >> 0) / mBlurWidth;
            }

            // Re-assemble destination pixel.
            int dpixel = (0xff000000)
                    | (((int) rt) << 16)
                    | (((int) gt) << 8)
                    | (((int) bt) << 0);
            mDestination[index] = dpixel;
        }
    }

    private static boolean printed = false;

    @Override
    public Void call() throws Exception {
        //System.out.println(current);
        current++;
        if (mLength < sThreshold) {
            computeDirectly();
            /*if (!printed){
                printed = true;
                for (int i=mStart;i<mStart + mLength;i++)
                    System.out.print(mDestination[i] + " ");
                System.out.println();
            }*/
            return null;
        }

        int split = mLength / 2;

        ThreadBlur threadBlur1 = new ThreadBlur(mSource, mStart, split, mDestination);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);
        futureTask1.run();

        ThreadBlur threadBlur2 = new ThreadBlur(mSource, mStart + split, mLength - split, mDestination);
        FutureTask<Void> futureTask2 = new FutureTask<>(threadBlur2);
        futureTask2.run();

        futureTask1.get();
        futureTask2.get();
        return null;
    }
}
