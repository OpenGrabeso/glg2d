# GLU tessellator provenance

Source: org.jogamp.jogl:jogl-all:2.3.2:sources (jogl-all-2.3.2-sources.jar)
SHA-256: 733e3194fe81550cd5bac00919434c79718ec79c26d5216bce369298968923b5
Upstream: https://jogamp.org/ and https://github.com/sgothel/jogl

Imported: jogamp/opengl/glu/tessellator/*.java (19 files), and
com/jogamp/opengl/glu/GLUtessellator{,Callback,CallbackAdapter}.java (3 files).
GLU.java contains only the required constants and tessellation diagnostics,
adapted from com/jogamp/opengl/glu/GLU.java and jogamp/opengl/glu/error/Error.java.

Changes: relocated to net.opengrabeso.glg2d.impl.tessellation; removed JogAmp
imports; substituted local primitive constants for GL constants. The tessellation
algorithm is unchanged. No JOGL context, native loader, or general GLU API is included.
Original per-file copyright and license headers are preserved. See the accompanying
license texts; the imported files are not relicensed under GLG2D's Apache license.
The marker interface now declares the existing instance methods used by GLG2D.
Javadoc links were adjusted to local types. Error text also comes from
jogamp/opengl/glu/Glue.java. GLU.java preserves the upstream error implementation
license header. The SGI-B-2.0.txt text is from:
https://github.com/spdx/license-list-data/blob/main/text/SGI-B-2.0.txt
