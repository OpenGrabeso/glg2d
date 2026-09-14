# Jaagl
Java API agnostic GL - wrapper around LWJGL / JOGL so that projects can easily switch between the two

The common interfaces and LWJGL implementations are in `glg2d`. The `jogl`
package is in the optional `glg2d-jogl` artifact, built with `mvn -Pjogl package`.
Applications provide the backend bindings and native libraries they use.

#### Design rationale

The API strives to provide an interface common for both LWJGL / JOGL. The interface is kept intentionaly simple
even if sometimes it means some optimization specific for one of the APIs will not be available.

While the general structure mostly follows JOGL design, some API are more like what users of LWJGL are used to see
(esp. when arrays are passed as parameters).

