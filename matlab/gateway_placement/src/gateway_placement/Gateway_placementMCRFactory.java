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

/**
 * <i>INTERNAL USE ONLY</i>
 */
public class Gateway_placementMCRFactory
{
   
    
    /** Component's uuid */
    private static final String sComponentId = "gateway_plac_2BAE6F7393B053F838ACB5477906D630";
    
    /** Component name */
    private static final String sComponentName = "gateway_placement";
    
   
    /** Pointer to default component options */
    private static final MWComponentOptions sDefaultComponentOptions = 
        new MWComponentOptions(
            MWCtfExtractLocation.EXTRACT_TO_CACHE, 
            new MWCtfClassLoaderSource(Gateway_placementMCRFactory.class)
        );
    
    
    private Gateway_placementMCRFactory()
    {
        // Never called.
    }
    
    public static MWMCR newInstance(MWComponentOptions componentOptions) throws MWException
    {
        if (null == componentOptions.getCtfSource()) {
            componentOptions = new MWComponentOptions(componentOptions);
            componentOptions.setCtfSource(sDefaultComponentOptions.getCtfSource());
        }
        return MWMCR.newInstance(
            componentOptions, 
            Gateway_placementMCRFactory.class, 
            sComponentName, 
            sComponentId,
            new int[]{7,14,0}
        );
    }
    
    public static MWMCR newInstance() throws MWException
    {
        return newInstance(sDefaultComponentOptions);
    }
}
