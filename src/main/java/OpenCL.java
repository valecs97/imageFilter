import org.jocl.*;

import java.util.List;

import static org.jocl.CL.*;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;

public class OpenCL {
    private static String programSource =
            "__kernel void "+
                    "blueImage(__global const int *src,"+
                    "             __global int *dest,"+
                    "             __global const int *vars)" +
                    "{"+
                    "   int gid = get_global_id(0);"+
                    "   int mBlurWidth = 15;" +
                    "   int mStart = vars[1];" +
                    "   int index = mStart + gid;" +
                    "   int sidePixels = (mBlurWidth - 1) /2;" +
                    "   float rt=0,gt=0,bt=0;"+
                    "   for (int mi = -sidePixels; mi <= sidePixels; mi++)" +
                    "   {" +
                    "       int comp1 = max(mi+index,0);" +
                    "       int comp2 = vars[2] -1;" +
                    "       int mindex = min(comp1,comp2);" +
                    "       int pixel = src[mindex];" +
                    "       rt += (float) ((pixel & 0x00ff0000) >> 16) / mBlurWidth;" +
                    "       gt += (float) ((pixel & 0x0000ff00) >> 8) / mBlurWidth;" +
                    "       bt += (float) ((pixel & 0x000000ff) >> 0) / mBlurWidth;" +
                    "   }"+
                    "   int dpixel = (0xff000000) | (((int) rt) << 16) | (((int) gt) << 8) | (((int) bt) << 0);" +
                    "   dest[index] = dpixel;" +
                    "}";

    private cl_context context;
    private cl_kernel kernel;
    private cl_command_queue commandQueue;
    private cl_program program;

    public OpenCL(){
        build();
    }

    private void build()
    {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Create the program from the source code
        program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "blueImage", null);
    }

    public void execute(int src[],int dest[],int vars[]){
        Pointer pointerSrc = Pointer.to(src);
        Pointer pointerDest = Pointer.to(dest);
        Pointer pointerVars = Pointer.to(vars);

        cl_mem memObjects[] = new cl_mem[3];
        memObjects[0] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * src.length, pointerSrc, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * dest.length, pointerDest, null);
        memObjects[2] = clCreateBuffer(context,
                CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * vars.length, pointerVars, null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2,
                Sizeof.cl_mem, Pointer.to(memObjects[2]));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{vars[0]};
        long local_work_size[] = new long[]{1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);


        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                dest.length * Sizeof.cl_int, pointerDest, 0, null, null);

        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
    }

    public void releaseProgram(){

        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
