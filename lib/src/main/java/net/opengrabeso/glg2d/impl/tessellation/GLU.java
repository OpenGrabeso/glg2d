/*
 * License Applicability. Except to the extent portions of this file are
 * made subject to an alternative license as permitted in the SGI Free
 * Software License B, Version 2.0 (the "License"), the contents of this
 * file are subject only to the provisions of the License. You may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
 * Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
 *
 * http://oss.sgi.com/projects/FreeB
 *
 * Note that, as provided in the License, the Software is distributed on an
 * "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
 * DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
 * CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
 * PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 *
 * NOTE:  The Original Code (as defined below) has been licensed to Sun
 * Microsystems, Inc. ("Sun") under the SGI Free Software License B
 * (Version 1.1), shown above ("SGI License").   Pursuant to Section
 * 3.2(3) of the SGI License, Sun is distributing the Covered Code to
 * you under an alternative license ("Alternative License").  This
 * Alternative License includes all of the provisions of the SGI License
 * except that Section 2.2 and 11 are omitted.  Any differences between
 * the Alternative License and the SGI License are offered solely by Sun
 * and not by SGI.
 *
 * Original Code. The Original Code is: OpenGL Sample Implementation,
 * Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
 * Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
 * Copyright in any portions created by third parties is as indicated
 * elsewhere herein. All Rights Reserved.
 *
 * Additional Notice Provisions: The application programming interfaces
 * established by SGI in conjunction with the Original Code are The
 * OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
 * April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
 * 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
 * Window System(R) (Version 1.3), released October 19, 1998. This software
 * was created using the OpenGL(R) version 1.2.1 Sample Implementation
 * published by SGI, but has not been independently verified as being
 * compliant with the OpenGL(R) version 1.2.1 Specification.
 */

package net.opengrabeso.glg2d.impl.tessellation;

/** Constants and diagnostics for the CPU-only GLU tessellator. No GL context is used. */
public final class GLU {
    private GLU() {}

    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GLU_INVALID_ENUM = 100900;
    public static final int GLU_INVALID_VALUE = 100901;
    public static final int GLU_OUT_OF_MEMORY = 100902;
    public static final int GLU_TESS_AVOID_DEGENERATE_TRIANGLES = 100149;
    public static final int GLU_TESS_BEGIN = 100100;
    public static final int GLU_TESS_BEGIN_DATA = 100106;
    public static final int GLU_TESS_BOUNDARY_ONLY = 100141;
    public static final int GLU_TESS_COMBINE = 100105;
    public static final int GLU_TESS_COMBINE_DATA = 100111;
    public static final int GLU_TESS_COORD_TOO_LARGE = 100155;
    public static final int GLU_TESS_EDGE_FLAG = 100104;
    public static final int GLU_TESS_EDGE_FLAG_DATA = 100110;
    public static final int GLU_TESS_END = 100102;
    public static final int GLU_TESS_END_DATA = 100108;
    public static final int GLU_TESS_ERROR = 100103;
    public static final int GLU_TESS_ERROR_DATA = 100109;
    public static final double GLU_TESS_MAX_COORD = 1.0e150;
    public static final int GLU_TESS_MISSING_BEGIN_CONTOUR = 100152;
    public static final int GLU_TESS_MISSING_BEGIN_POLYGON = 100151;
    public static final int GLU_TESS_MISSING_END_CONTOUR = 100154;
    public static final int GLU_TESS_MISSING_END_POLYGON = 100153;
    public static final int GLU_TESS_NEED_COMBINE_CALLBACK = 100156;
    public static final int GLU_TESS_TOLERANCE = 100142;
    public static final int GLU_TESS_VERTEX = 100101;
    public static final int GLU_TESS_VERTEX_DATA = 100107;
    public static final int GLU_TESS_WINDING_ABS_GEQ_TWO = 100134;
    public static final int GLU_TESS_WINDING_NEGATIVE = 100133;
    public static final int GLU_TESS_WINDING_NONZERO = 100131;
    public static final int GLU_TESS_WINDING_ODD = 100130;
    public static final int GLU_TESS_WINDING_POSITIVE = 100132;
    public static final int GLU_TESS_WINDING_RULE = 100140;
    /** Returns the diagnostic for a tessellator callback error. */
    public static String errorString(int error) {
        switch (error) {
            case GLU_INVALID_ENUM: return "invalid enumerant";
            case GLU_INVALID_VALUE: return "invalid value";
            case GLU_OUT_OF_MEMORY: return "out of memory";
            case GLU_TESS_MISSING_BEGIN_POLYGON: return "gluTessBeginPolygon() must precede a gluTessEndPolygon";
            case GLU_TESS_MISSING_BEGIN_CONTOUR: return "gluTessBeginContour() must precede a gluTessEndContour()";
            case GLU_TESS_MISSING_END_POLYGON: return "gluTessEndPolygon() must follow a gluTessBeginPolygon()";
            case GLU_TESS_MISSING_END_CONTOUR: return "gluTessEndContour() must follow a gluTessBeginContour()";
            case GLU_TESS_COORD_TOO_LARGE: return "a coordinate is too large";
            case GLU_TESS_NEED_COMBINE_CALLBACK: return "need combine callback";
            default: return "error (" + error + ")";
        }
    }
}
