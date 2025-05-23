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

import com.mathworks.toolbox.javabuilder.pooling.Poolable;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The <code>GWPlacerRemote</code> class provides a Java RMI-compliant interface to the 
 * M-functions from the files:
 * <pre>
 *  D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeGWsMinSumDist.m
 *  D:\\pdv\\workspace\\realtime-tdma\\matlab\\computeMinSumDistGW.m
 * </pre>
 * The {@link #dispose} method <b>must</b> be called on a <code>GWPlacerRemote</code> 
 * instance when it is no longer needed to ensure that native resources allocated by this 
 * class are properly freed, and the server-side proxy is unexported.  (Failure to call 
 * dispose may result in server-side threads not being properly shut down, which often 
 * appears as a hang.)  
 *
 * This interface is designed to be used together with 
 * <code>com.mathworks.toolbox.javabuilder.remoting.RemoteProxy</code> to automatically 
 * generate RMI server proxy objects for instances of gateway_placement.GWPlacer.
 */
public interface GWPlacerRemote extends Poolable
{
    /**
     * Provides the standard interface for calling the <code>computeGWsMinSumDist</code> 
     * M-function with 4 input arguments.  
     *
     * Input arguments to standard interface methods may be passed as sub-classes of 
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of any 
     * supported Java type (i.e. scalars and multidimensional arrays of any numeric, 
     * boolean, or character type, or String). Arguments passed as Java types are 
     * converted to MATLAB arrays according to default conversion rules.
     *
     * All inputs to this method must implement either Serializable (pass-by-value) or 
     * Remote (pass-by-reference) as per the RMI specification.
     *
     * M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     *
     * @param nargout Number of outputs to return.
     * @param rhs The inputs to the M function.
     *
     * @return Array of length nargout containing the function outputs. Outputs are 
     * returned as sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>. 
     * Each output array should be freed by calling its <code>dispose()</code> method.
     *
     * @throws java.jmi.RemoteException An error has occurred during the function call or 
     * in communication with the server.
     */
    public Object[] computeGWsMinSumDist(int nargout, Object... rhs) throws RemoteException;
    /**
     * Provides the standard interface for calling the <code>computeMinSumDistGW</code> 
     * M-function with 3 input arguments.  
     *
     * Input arguments to standard interface methods may be passed as sub-classes of 
     * <code>com.mathworks.toolbox.javabuilder.MWArray</code>, or as arrays of any 
     * supported Java type (i.e. scalars and multidimensional arrays of any numeric, 
     * boolean, or character type, or String). Arguments passed as Java types are 
     * converted to MATLAB arrays according to default conversion rules.
     *
     * All inputs to this method must implement either Serializable (pass-by-value) or 
     * Remote (pass-by-reference) as per the RMI specification.
     *
     * M-documentation as provided by the author of the M function:
     * <pre>
     * %computeGWsMinSumDist place num of GWs that minize the sum distance between
     * % all source and destination sensor nodes to the nearest GWs.
     * %   sensors is a n*3 matrix. 
     * %   row i represents the sensor i, the values correspond to the
     * %   x-coordinate, y coordinate and the period of a sensor node.
     * %   maxx, the width of the space. maxy, the height of the space.
     * </pre>
     *
     * @param nargout Number of outputs to return.
     * @param rhs The inputs to the M function.
     *
     * @return Array of length nargout containing the function outputs. Outputs are 
     * returned as sub-classes of <code>com.mathworks.toolbox.javabuilder.MWArray</code>. 
     * Each output array should be freed by calling its <code>dispose()</code> method.
     *
     * @throws java.jmi.RemoteException An error has occurred during the function call or 
     * in communication with the server.
     */
    public Object[] computeMinSumDistGW(int nargout, Object... rhs) throws RemoteException;
  
    /** Frees native resources associated with the remote server object */
    void dispose() throws RemoteException;
}
