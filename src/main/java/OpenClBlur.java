import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class OpenClBlur implements Callable<Void> {
    private static int[] mSource;
    private static int[] mDestination;
    private static int sThreshold = 100000000;
    private int mLength;
    private int mStart;
    private static int current = 0;
    private OpenCL openCL;

    public OpenClBlur(int mStart, int mLength, OpenCL openCL) {
        this.mLength = mLength;
        this.mStart = mStart;
        this.openCL = openCL;
    }

    public static void setSrcDest(int[] src,int[] dest)
    {
        mSource = src;
        mDestination = dest;
    }

    private static boolean printed = false;

    @Override
    public Void call() throws Exception {
        //System.out.println(current);
        current++;
        if (mLength < sThreshold) {

/*            if (!printed){
                printed = true;
                for (int i=mStart;i<mStart + mLength;i++)
                    System.out.print(mDestination[i] + " ");
                System.out.println();
                openCL.execute(mSource,mDestination,new int[]{mLength,mStart,mSource.length});
                for (int i=mStart;i<mStart + mLength;i++)
                    System.out.print(mDestination[i] + " ");
                System.out.println();
            }
            else{*/
                openCL.execute(mSource,mDestination,new int[]{mLength,mStart,mSource.length});
            //}
            return null;
        }

        int split = mLength / 2;

        OpenClBlur threadBlur1 = new OpenClBlur(mStart, split, openCL);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);
        futureTask1.run();

        OpenClBlur threadBlur2 = new OpenClBlur(mStart + split, mLength - split, openCL);
        FutureTask<Void> futureTask2 = new FutureTask<>(threadBlur2);
        futureTask2.run();


        futureTask1.get();
        futureTask2.get();
        return null;
    }
}
