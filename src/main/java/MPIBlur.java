import mpi.MPI;
import mpi.Status;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class MPIBlur implements Callable<Void>{

    private static int[] mSource;
    private static int[] mDestination;
    private static int sThreshold = 10000000;
    private int mLength;
    private int mStart;
    private int threadDestination;
    private static int current = 0;

    public MPIBlur(int mStart, int mLength, int threadDestination) {
        this.mLength = mLength;
        this.mStart = mStart;
        this.threadDestination = threadDestination;
    }

    public static void setSrcDest(int[] src,int[] dest)
    {
        mSource = src;
        mDestination = dest;
    }

    @Override
    public Void call() throws Exception {
        System.out.println(current);
        current++;
        if (mLength < sThreshold) {
            MPI.COMM_WORLD.Send(new int[]{mStart, mLength,mSource.length},0,3,MPI.INT,threadDestination,threadDestination);
            MPI.COMM_WORLD.Send(mSource,0,mSource.length,MPI.INT,threadDestination,threadDestination);
            MPI.COMM_WORLD.Send(mDestination,0,mDestination.length,MPI.INT,threadDestination,threadDestination);
            int[] res = new int[mSource.length];
            MPI.COMM_WORLD.Recv(res,0,mSource.length,MPI.INT,threadDestination,threadDestination);
            for (int index = mStart; index < mStart + mLength; index++) {
                mDestination[index] = res[index];
            }
            return null;
        }

        int split = mLength / 2;

        MPIBlur threadBlur1 = new MPIBlur(mStart, split,1);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);
        futureTask1.run();

        MPIBlur threadBlur2 = new MPIBlur(mStart + split, mLength - split,2);
        FutureTask<Void> futureTask2 = new FutureTask<>(threadBlur2);
        futureTask2.run();


        futureTask1.get();
        futureTask2.get();
        return null;
    }

    public static void MPIWorker(int me){
        while (true) {
            //System.out.println("Preparing receiving !");
            int[] siz = new int[3];
            Status status = MPI.COMM_WORLD.Recv(siz, 0, 3, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            int mStart = siz[0];
            int mLength = siz[1];
            int arrayLength = siz[2];
            if (arrayLength == -5)
                break;
            int src[] = new int[arrayLength];
            int dest[] = new int[arrayLength];
            MPI.COMM_WORLD.Recv(src, 0, arrayLength, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            MPI.COMM_WORLD.Recv(dest, 0, arrayLength, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);

            //System.out.println("RECEIVED !  with tag " + status.tag + " with source " + status.source);

            computeDirectly(mStart,mLength,src,dest);

            //System.out.println("NOW SENDING BACK THE RESULT ! " + status.tag);
            MPI.COMM_WORLD.Ssend(dest, 0, arrayLength, MPI.INT, status.source, status.tag);
            //System.out.println("SEND ! " + status.tag);
        }
    }

    private static void computeDirectly(int mStart, int mLength, int[] mSource,int[] mDestination) {
        // Processing window size, should be odd.
        int mBlurWidth = 15;
        int sidePixels = (mBlurWidth - 1) / 2;
        ThreadBlur.computeDirectly(sidePixels, mStart, mLength, mSource, mBlurWidth, mDestination);
    }
}
