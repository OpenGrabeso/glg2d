package net.opengrabeso.opengl;

/** Optional backend for the shared interactive rendering scenarios. */
public interface JaaglTestBackend {
    String name();
    void run(Jaagl2EventListener listener);
}