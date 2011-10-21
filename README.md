Goja - Globus Online Java API
==========================

Goji is based on [JGO](http://confluence.globus.org/display/~neillm/JGOClient+Homepage) written by Neill Miller. All of the original code is copyrighted by the University of Chicago. It's written to support GO support for the [Grisu framework](https://github.com/grisu/grisu).

Most of the changes to the **JGO** code are refactorings to have a more object-oriented GO client library, which can be used in a threadsafe way within the Grisu framework.

Also, I converted it into a maven project since that is what Grisu uses for dependency management.

Purpose
-------

Goji has 2 goals:

1) make the GlobusOnline REST API available via an easy-to-use Java library

2) integrate VOMS and an Information sytem provider into this Java Library so that GlobusOnline can be used in conjunction with those technologies



