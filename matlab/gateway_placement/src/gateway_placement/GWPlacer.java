/*
 * MATLAB Compiler: 4.14 (R2010b)
 * Date: Sun Feb 05 22:17:29 2012
 * Arguments: "-B" "macro_default" "-W" "java:gateway_placement,GWPlacer" "-T" "link:lib" 
 * "-d" "D:\\pdv\\workspace\\realtime-tdma\\matlab\\gateway_placement\\src" "-w" 
 * "enable:specified_file_mismatch" "-w" "enable:repeated_file" "-w" 
 * "enable:switch_ignored" "-w" "enable:missing_lib_sentinel" "-w" "enable:demo_license" 
 * "-v" 
 * "class{GWPlacer:D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeGWsMinSumDist.m,D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeMinSumDistGW.m}" 
 */

package gateway_placement;

import com.mathworks.toolbox.javabuilder.*;
import com.mathworks.toolbox.javabuilder.internal.*;
import java.util.*;

/**
 * The <code>GWPlacer</code> class provides a Java interface to the M-functions
 * from the files:
 * <pre>
 *  D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeGWsMinSumDist.m
 *  D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeMinSumDistGW.m
 * </pre>
 * The {@link #dispose} method <b>must</b> be called on a <code>GWPlacer</code> instance 
 * when it is no longer needed to ensure that native resources allocated by this class 
 * are properly freed.
 * @version 0.0
 */
public class GWPlacer extends MWComponentInstance<GWPlacer>
{
    /**
     * Tracks all instances of this class to ensure their dispose method is
     * called on shutdown.
     */
    private static final Set<Disposable> sInstances = new HashSet<Disposable>();

    /**
     * Maintains information used in calling the <code>computeGWsMinSumDist</code> 
     *M-function.
     */
    private static final MWFunctionSignature sComputeGWsMinSumDistSignature =
        new MWFunctionSignature(/* max outputs = */ 1,
                                /* has varargout = */ false,
                                /* function name = */ "computeGWsMinSumDist",
                                /* max inputs = */ 4,
                                /* has varargin = */ false);
    /**
     * Maintains information used in calling the <code>computeMinSumDistGW</code> 
     *M-function.
     */
    private static final MWFunctionSignature sComputeMinSumDistGWSignature =
        new MWFunctionSignature(/* max outputs = */ 1,
                                /* has varargout = */ false,
                                /* function name = */ "computeMinSumDistGW",
                                /* max inputs = */ 3,
                                /* has varargin = */ false);

    /**
     * Shared initialization implementation - private
     */
    private GWPlacer (final MWMCR mcr) throws MWException
    {
        super(mcr);
        // add this to sInstances
        synchronized(GWPlacer.class) {
            sInstances.add(this);
        }
    }

    /**
     * Constructs a new instance of the <code>GWPlacer</code> class.
     */
    public GWPlacer() throws MWException
    {
        this(Gateway_placementMCRFactory.newInstance());
    }
    
    private static MWComponentOptions getPathToComponentOptions(String path)
    {
        MWComponentOptions options = new MWComponentOptions(new MWCtfExtractLocation(path),
                                                            new MWCtfDirectorySource(path));
        return options;
    }
    
    /**
     * @deprecated Please use the constructor {@link #GWPlacer(MWComponentOptions componentOptions)}.
     * The <code>com.mathworks.toolbox.javabuilder.MWComponentOptions</code> class provides API to set the
     * path to the component.
     * @param pathToComponent Path to component directory.
     */
    public GWPlacer(String pathToComponent) throws MWException
    {
        this(Gateway_placementMCRFactory.newInstance(getPathToComponentOptions(pathToComponent)));
    }
    
    /**
     * Constructs a new instance of the <code>GWPlacer</code> class. Use this constructor 
     * to specify the options required to instantiate this component.  The options will 
     * be specific to the instance of this component being created.
     * @param componentOptions Options specific to the component.
     */
    public GWPlacer(MWComponentOptions componentOptions) throws MWException
    {
        this(Gateway_placementMCRFactory.newInstance(componentOptions));
    }
    
    /** Frees native resources associated with this object */
    public void dispose()
    {
        try {
            super.dispose();
        } finally {
            synchronized(GWPlacer.class) {
                sInstances.remove(this);
            }
        }
    }
  
    /**
     * Invokes the first m-function specified by MCC, with any arguments given on
     * the command line, and prints the result.
     */
    public static void main (String[] args)
    {
        try {
            MWMCR mcr = Gateway_placementMCRFactory.newInstance();
            mcr.runMain( sComputeGWsMinSumDistSignature, args);
            mcr.dispose();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Calls dispose method for each outstanding instance of this class.
     */
    public static void disposeAllInstances()
    {
        synchronized(GWPlacer.class) {
            for (Disposable i : sInstances) i.dispose();
            sInstances.clear();
        }
    }

    /**
     * Provides the interface for calling the <code>computeGWsMinSumDist</code> M-function 
     * where the first input, an instance of List, receives the output of the M-function and
     * the second input, also an instance of List, provides the input to the M-function.
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param lhs List in which to return outputs. Number of outputs (nargout) is
     * determined by allocated size of this List. Outputs are returned as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>.
     * Each output array should be freed by calling its <code>dispose()</code>
     * method.
     *
     * @param rhs List containing inputs. Number of inputs (nargin) is determined
     * by the allocated size of this List. Input arguments may be passed as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or
     * as arrays of any supported Java type. Arguments passed as Java types are
     * converted to MATLAB arrays according to default conversion rules.
     * @throws MWException An error has occurred during the function call.
     */
    public void computeGWsMinSumDist(List lhs, List rhs) throws MWException
    {
        fMCR.invoke(lhs, rhs, sComputeGWsMinSumDistSignature);
    }

    /**
     * Provides the interface for calling the <code>computeGWsMinSumDist</code> M-function 
     * where the first input, an Object array, receives the output of the M-function and
     * the second input, also an Object array, provides the input to the M-function.
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param lhs array in which to return outputs. Number of outputs (nargout)
     * is determined by allocated size of this array. Outputs are returned as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>.
     * Each output array should be freed by calling its <code>dispose()</code>
     * method.
     *
     * @param rhs array containing inputs. Number of inputs (nargin) is
     * determined by the allocated size of this array. Input arguments may be
     * passed as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of
     * any supported Java type. Arguments passed as Java types are converted to
     * MATLAB arrays according to default conversion rules.
     * @throws MWException An error has occurred during the function call.
     */
    public void computeGWsMinSumDist(Object[] lhs, Object[] rhs) throws MWException
    {
        fMCR.invoke(Arrays.asList(lhs), Arrays.asList(rhs), sComputeGWsMinSumDistSignature);
    }

    /**
     * Provides the standard interface for calling the <code>computeGWsMinSumDist</code>
     * M-function with 4 input arguments.
     * Input arguments may be passed as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of
     * any supported Java type. Arguments passed as Java types are converted to
     * MATLAB arrays according to default conversion rules.
     *
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param nargout Number of outputs to return.
     * @param rhs The inputs to the M function.
     * @return Array of length nargout containing the function outputs. Outputs
     * are returned as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>. Each output array
     * should be freed by calling its <code>dispose()</code> method.
     * @throws MWException An error has occurred during the function call.
     */
    public Object[] computeGWsMinSumDist(int nargout, Object... rhs) throws MWException
    {
        Object[] lhs = new Object[nargout];
        fMCR.invoke(Arrays.asList(lhs), 
                    MWMCR.getRhsCompat(rhs, sComputeGWsMinSumDistSignature), 
                    sComputeGWsMinSumDistSignature);
        return lhs;
    }
    /**
     * Provides the interface for calling the <code>computeMinSumDistGW</code> M-function 
     * where the first input, an instance of List, receives the output of the M-function and
     * the second input, also an instance of List, provides the input to the M-function.
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param lhs List in which to return outputs. Number of outputs (nargout) is
     * determined by allocated size of this List. Outputs are returned as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>.
     * Each output array should be freed by calling its <code>dispose()</code>
     * method.
     *
     * @param rhs List containing inputs. Number of inputs (nargin) is determined
     * by the allocated size of this List. Input arguments may be passed as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or
     * as arrays of any supported Java type. Arguments passed as Java types are
     * converted to MATLAB arrays according to default conversion rules.
     * @throws MWException An error has occurred during the function call.
     */
    public void computeMinSumDistGW(List lhs, List rhs) throws MWException
    {
        fMCR.invoke(lhs, rhs, sComputeMinSumDistGWSignature);
    }

    /**
     * Provides the interface for calling the <code>computeMinSumDistGW</code> M-function 
     * where the first input, an Object array, receives the output of the M-function and
     * the second input, also an Object array, provides the input to the M-function.
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param lhs array in which to return outputs. Number of outputs (nargout)
     * is determined by allocated size of this array. Outputs are returned as
     * sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>.
     * Each output array should be freed by calling its <code>dispose()</code>
     * method.
     *
     * @param rhs array containing inputs. Number of inputs (nargin) is
     * determined by the allocated size of this array. Input arguments may be
     * passed as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of
     * any supported Java type. Arguments passed as Java types are converted to
     * MATLAB arrays according to default conversion rules.
     * @throws MWException An error has occurred during the function call.
     */
    public void computeMinSumDistGW(Object[] lhs, Object[] rhs) throws MWException
    {
        fMCR.invoke(Arrays.asList(lhs), Arrays.asList(rhs), sComputeMinSumDistGWSignature);
    }

    /**
     * Provides the standard interface for calling the <code>computeMinSumDistGW</code>
     * M-function with 3 input arguments.
     * Input arguments may be passed as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of
     * any supported Java type. Arguments passed as Java types are converted to
     * MATLAB arrays according to default conversion rules.
     *
     * <p>M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     * </p>
     * @param nargout Number of outputs to return.
     * @param rhs The inputs to the M function.
     * @return Array of length nargout containing the function outputs. Outputs
     * are returned as sub-classes of
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>. Each output array
     * should be freed by calling its <code>dispose()</code> method.
     * @throws MWException An error has occurred during the function call.
     */
    public Object[] computeMinSumDistGW(int nargout, Object... rhs) throws MWException
    {
        Object[] lhs = new Object[nargout];
        fMCR.invoke(Arrays.asList(lhs), 
                    MWMCR.getRhsCompat(rhs, sComputeMinSumDistGWSignature), 
                    sComputeMinSumDistGWSignature);
        return lhs;
    }
}
