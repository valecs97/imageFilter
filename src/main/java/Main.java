import mpi.MPI;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Main {

    private static String fileName = "spongebob.png";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        ThreadColdStart();
        //MPIColdStart(args);
        OpenCLColdStart();
    }

    private static void OpenCLColdStart() throws ExecutionException, InterruptedException, IOException {
        String srcName = fileName;
        File srcFile = new File(srcName);
        BufferedImage image = ImageIO.read(srcFile);

        System.out.println("Source image: " + srcName);

        BufferedImage blurredImage = blueOpenCL(image);


        String dstName = "blurredOpenCL.jpg";
        File dstFile = new File(dstName);
        ImageIO.write(blurredImage, "jpg", dstFile);

        System.out.println("Output image: " + dstName);
    }

    private static void ThreadColdStart() throws ExecutionException, InterruptedException, IOException {
        String srcName = fileName;
        File srcFile = new File(srcName);
        BufferedImage image = ImageIO.read(srcFile);

        System.out.println("Source image: " + srcName);

        BufferedImage blurredImage = blurThread(image);


        String dstName = "blurredThread.jpg";
        File dstFile = new File(dstName);
        ImageIO.write(blurredImage, "jpg", dstFile);

        System.out.println("Output image: " + dstName);
    }

    private static void MPIColdStart(String[] args) throws IOException, ExecutionException, InterruptedException {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (me == 0) {
            String srcName = fileName;
            File srcFile = new File(srcName);
            BufferedImage image = ImageIO.read(srcFile);

            System.out.println("Source image: " + srcName);

            BufferedImage blurredImage = blueMPI(image);


            String dstName = "blurredMPI.jpg";
            File dstFile = new File(dstName);
            ImageIO.write(blurredImage, "jpg", dstFile);

            System.out.println("Output image: " + dstName);
        } else {
            MPIBlur.MPIWorker(me);
        }

        MPI.Finalize();
    }

    private static BufferedImage blueOpenCL(BufferedImage srcImage) throws ExecutionException, InterruptedException {
        int w = srcImage.getWidth();
        int h = srcImage.getHeight();
        int[] src = srcImage.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        System.out.println("Array size is " + src.length);

        OpenClBlur.setSrcDest(src,dst);
        OpenCL openCL = new OpenCL();
        OpenClBlur threadBlur1 = new OpenClBlur(0, src.length, openCL);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);

        long startTime = System.currentTimeMillis();

        futureTask1.run();
        futureTask1.get();
        openCL.releaseProgram();

        long endTime = System.currentTimeMillis();

        System.out.println("Image blur OpenCL took " + (endTime - startTime) +
                " milliseconds.");

        BufferedImage dstImage =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        return dstImage;
    }

    private static BufferedImage blueMPI(BufferedImage srcImage) throws ExecutionException, InterruptedException {
        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        int[] src = srcImage.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        System.out.println("Array size is " + src.length);

        MPIBlur.setSrcDest(src,dst);
        MPIBlur threadBlur1 = new MPIBlur(0, src.length,3);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);

        long startTime = System.currentTimeMillis();

        futureTask1.run();
        futureTask1.get();

        long endTime = System.currentTimeMillis();

        System.out.println("Image blur MPI took " + (endTime - startTime) +
                " milliseconds.");

        BufferedImage dstImage =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        return dstImage;
    }

    private static BufferedImage blurThread(BufferedImage srcImage) throws ExecutionException, InterruptedException {
        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        int[] src = srcImage.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        System.out.println("Array size is " + src.length);
        System.out.println("Threshold is " + ForkBlur.sThreshold);

        //int processors = Runtime.getRuntime().availableProcessors();
        //System.out.println(Integer.toString(processors) + " processor" + (processors != 1 ? "s are " : " is ") + "available");

        //ForkBlur fb = new ForkBlur(src, 0, src.length, dst);

        //ForkJoinPool pool = new ForkJoinPool();

        ThreadBlur threadBlur1 = new ThreadBlur(src, 0, src.length, dst);
        FutureTask<Void> futureTask1 = new FutureTask<>(threadBlur1);


        long startTime = System.currentTimeMillis();
        futureTask1.run();
        futureTask1.get();
        long endTime = System.currentTimeMillis();

        System.out.println("Image blur THREAD took " + (endTime - startTime) +
                " milliseconds.");

        BufferedImage dstImage =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        return dstImage;
    }
}

