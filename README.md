# GLG2D

GLG2D is an effort to translate Graphics2D calls directly into OpenGL calls
and accelerate the Java2D drawing functionality. 

Find more information on http://opengrabeso.github.com/glg2d/

Use cases:
 * OpenGL HTML rendering using https://github.com/OpenGrabeso/flyingsaucer 
 * use as a drop-in replacement for a JPanel and all Swing children will be
    accelerated
 * draw Swing components in an GLCanvas in your existing application

GLG2D is licensed under Apache 2.0. The bundled CPU tessellator retains its
upstream licenses; see `lib/src/main/resources/META-INF/tessellator/` for the
license texts, source version and checksum. JOGL is licensed and distributed separately.

### Fork

Forked from http://brandonborkholder.github.com/glg2d/

This version adds following features:

- fix issues preventing use of the [Flying Saucer library](https://github.com/OpenGrabeso/flyingsaucer)
- use GL3 shader based text-renderer (allows running on a core profile)
- allow using JOGL or LWJGL 

### How to build

This project uses Maven and targets Java 8. Run `mvn package` to build the
core and the Swing/LWJGL examples. JARs are written to each module's `target/`
directory. This default build does not require JOGL or GlueGen.

Run `mvn -Pjogl package` to also build the optional JOGL adapter and examples.
The artifact versions and LWJGL/JOGL versions are defined in the parent POM.

### Running examples on Windows

The examples create their Swing hierarchy on the EDT and use the default Metal
look and feel. Geometry scenes explicitly paint a white background. LWJGL's
canvas renders nested Swing layouts and repaints animations; it is a rendering
demonstration, without full Swing keyboard/mouse routing. The historical
`GLFWBasic` entry point now shows a geometry scene using the AWT/LWJGL canvas.

`scripts/examples.ps1` finds Maven on PATH or in IntelliJ IDEA; `-MavenHome`
overrides discovery. It uses a project-local `target/m2` cache, builds all
modules, and creates fresh dependency classpaths. For example:

```powershell
./scripts/examples.ps1 -JavaHome C:/Users/Ondra/.jdks/temurin-17.0.17 -Example LWJGLlUIDemo
./scripts/examples.ps1 -JavaHome C:/Users/Ondra/.jdks/temurin-21.0.12.1 -Example JoglExampleCurve
```

The launcher supplies JOGL's required module export on JDK 9+. Use `-SkipBuild`
only after a successful build and classpath generation with the same sources.
`-Offline` prevents network access when dependencies are already cached.
`-JvmOptions '-Dsun.java2d.uiScale=2'` can be used for a 200% scale check.

To validate every entry point in the three example modules on a selected JDK:

```powershell
./scripts/examples.ps1 -JavaHome C:/Users/Ondra/.jdks/temurin-17.0.17 -Validate -OutputDirectory target/example-smoke/17
./scripts/examples.ps1 -JavaHome C:/Users/Ondra/.jdks/temurin-21.0.12.1 -Validate -OutputDirectory target/example-smoke/21
./scripts/examples.ps1 -JavaHome C:/Users/Ondra/.jdks/temurin-25.0.4.1 -Validate -OutputDirectory target/example-smoke/25
```

Each test JVM has a 30-second timeout, opens and disposes its own window, and
saves logs and initial/resized PNGs alongside Java2D references. UI demos also
check animation. GL images come from the framebuffer, not the desktop;
the pure Swing controls use screen capture and briefly stay on top. Comparison
allows a two-pixel edge displacement and color/antialiasing differences, while
requiring visible foreground content (including the small curve). `results.csv`
records every outcome. These tests require an interactive desktop and native
OpenGL. CI's JDK 8/17/21/25 matrix verifies builds and CPU geometry only.
LWJGL tests also remove and reattach the canvas to check context recreation.
Use `-Module examples-lwjgl` or `-Module examples-jogl` with a separate output
directory to repeat only one backend's checks.
Example animation timers start with the window and stop when it is disposed.

See [the recorded Windows validation](docs/example-validation.md) for the JDK
matrix, evidence locations and remaining rendering limitations.

### How to use

#### JAAGL

The library uses OpenGL via the [Jaagl abstraction layer](https://github.com/OpenGrabeso/jaagl), therefore the same
library can be used with both LWJGL and JOGL. The `glg2d` artifact contains the
rendering core and LWJGL integration. The separate `glg2d-jogl` artifact adds
the JOGL integration and depends on `glg2d`. Backend bindings remain `provided`;
applications supply their chosen backend and its native libraries.

If necessary, it should be easy to provide Jaggl implementation for other platform / API, e.g. LWJGL OpenGL ES bindings.

The current bindings target JOGL 2.3.2 and LWJGL 3.3.4 (see `pom.xml`).

#### POM.XML 

The project is published at GitHub packages, add following to your pom.xml:

```
<repository>
  <id>github</id>
  <name>GitHub OpenGrabeso Apache Maven Packages</name>
  <url>https://maven.pkg.github.com/OpenGrabeso/_</url>
</repository>

<dependency>
 <groupId>net.opengrabeso</groupId>
 <artifactId>glg2d</artifactId>
 <version>${glg2d.version}</version>
</dependency>
```

For JOGL applications, use `net.opengrabeso:glg2d-jogl` at the same version
instead; it brings in `glg2d` transitively. Add JOGL and GlueGen bindings and
natives explicitly, as shown in `examples-jogl/pom.xml`. LWJGL applications
continue to use `glg2d` and their existing LWJGL dependencies, as shown in
`examples-lwjgl/pom.xml`.

### Migration

`GLG2DCanvas`, `GLG2DPanel`, `GLG2DSimpleEventListener`,
`GLG2DHeadlessListener`, `GLAwareRepaintManager`, and the JAAGL `jogl` package
now live in `glg2d-jogl`. Their class names and signatures are unchanged.
JOGL users must add the new artifact when upgrading; do not mix old and new
GLG2D JARs. LWJGL's public API is unchanged.

`AbstractTesselatorVisitor` now uses protected tessellator/callback types from
`net.opengrabeso.glg2d.impl.tessellation`. Subclasses using those fields must
update their imports and use the tessellator's instance methods. Tessellation
errors now throw `IllegalStateException` instead of JOGL's `GLException`.
The tessellation algorithm, winding rules and intersection handling are retained.

### Validation

#### JOGL AWT examples on Windows / JDK 9+

JOGL 2.3.2's AWT integration needs access to the JDK's `sun.awt` package.
Add this **VM option** when launching a JOGL Swing example:

```text
--add-exports=java.desktop/sun.awt=ALL-UNNAMED
```

Without it, the Windows graphics-configuration lookup can fail with
`ArrayIndexOutOfBoundsException: Index -1` in
`WindowsAWTWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationImpl`.
This is a separate path from the offscreen renderer tested below.

Shared IntelliJ run configurations are in `.run/`, named `Jogl... (JDK 9+)`.
Enable the Maven `jogl` profile and select one of those configurations. Existing
temporary IDE configurations need the VM option added manually. JDK 8 must be
launched without this module option. No backend dependency upgrade is required.

#### CPU and native rendering checks

Run the CPU geometry tests without a display or native bindings:

```sh
mvn -pl lib -am -DskipTests=false -Dtest=TessellationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The existing interactive JOGL visual tests are in `jogl/src/test`. Shared text
scenarios remain in the core test JAR. Their launcher defaults to LWJGL; `-jogl`
selects the test service provider from `jogl/target/test-classes` when that
directory and the JOGL dependencies are on the classpath. Interactive tests
remain skipped by default.

Bounded native smoke checks render a hole, a self-intersection, complex clipping,
text and an image. They save PNGs, check interior pixels against Java2D, then
destroy the context and exit. For example, in PowerShell on Windows:

```powershell
mvn -Pjogl package dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
$lwjglClasspath = (Get-Content examples-lwjgl/target/classpath.txt -Raw).Trim()
java -ea -cp "lib/target/test-classes;$lwjglClasspath" net.opengrabeso.glg2d.LwjglRenderingSmoke target/render-smoke
$joglClasspath = (Get-Content jogl/target/classpath.txt -Raw).Trim()
java -ea -cp "jogl/target/test-classes;jogl/target/classes;$joglClasspath" net.opengrabeso.glg2d.JoglRenderingSmoke target/render-smoke
```

Check a real JOGL example's AWT window creation, GL rendering and disposal on
Windows with JDK 9+ (the test briefly shows the window and then exits):

```powershell
$exampleClasspath = (Get-Content examples-jogl/target/classpath.txt -Raw).Trim()
java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -cp "examples-jogl/target/test-classes;examples-jogl/target/classes;$exampleClasspath" net.opengrabeso.glg2d.examples.JoglAwtSmoke JoglRoundCorners
```

The LWJGL check rejects a classpath containing JOGL. Maven Enforcer also bans
transitive JogAmp and adapter dependencies in the core and LWJGL examples.
