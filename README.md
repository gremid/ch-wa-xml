# XML Conversion of Chadwyck-Healey's „Weimarer Ausgabe”

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.15591243.svg)](https://doi.org/10.5281/zenodo.15591243)

_Conversion of Goethe's Collected Works from EBT DynaText to XML_


The CD-ROM edition of Goethe's collected works by Chadwyck Healey, published in
1995 and based on the „Weimarer Ausgabe” – while not the most recent edition –
still is the most comprehensive digital publication of Goethe's literary works
as well as his scientific writings, letters, diaries and other texts.
Unfortunately it is not easily accessible anymore. The CD-ROM version suffers
from incompatibility with current hardware setups and lacking support of the
accompanying DynaText software by current operating systems. Online versions of
the edition – made available via web gateways and licensed exclusively to
libraries – are scarce and allow only for the browsing of individual texts in
the edition.

This conversion routine reads the edition's data in EBT's DynaText format, as
provided on CD-ROM, and converts it to XML, thereby facilitating the usage of
the edition as a whole in a machine-readable form.

## Prerequisites

The conversion routines are written in
[Clojure](https://clojure.org/), a JVM-based Lisp
dialect. Accordingly, a Clojure installation is required.

## Retrieve an ISO image of the CD-ROM edition

Retrieve an ISO image of the CD-ROM edition and copy the directory
`\EBTBOOKS\BOOKS\GOETHE\` to the project's `data/` directory with paths
converted to lower case, i. e.

```
$ sudo mount -o loop,ro $CH_WA_ISO_FILE /mnt
$ cp -R /mnt/ebtbooks/books/goethe data
```

## Convert to XML

Once the EBT DynaText dataset is available at `data/goethe/`, run the
conversion:

```
$ clojure -M:convert ~/data/ksw/ch-wa-xml
2023-02-25T21:28:40.188Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 1", "type" "literary", "n" "001"}
2023-02-25T21:28:44.958Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 2", "type" "literary", "n" "002"}
2023-02-25T21:28:47.569Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 3", "type" "literary", "n" "003"}
2023-02-25T21:28:49.967Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 4", "type" "literary", "n" "004"}
2023-02-25T21:28:51.497Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 5i", "type" "literary", "n" "005"}
2023-02-25T21:28:52.596Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 5ii", "type" "literary", "n" "006"}
2023-02-25T21:28:58.025Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 6", "type" "literary", "n" "007"}
2023-02-25T21:29:01.747Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 7", "type" "literary", "n" "008"}
2023-02-25T21:29:03.901Z textmaschine INFO [ch-wa-xml:156] - {"weiref" "I‚ 8", "type" "literary", "n" "009"}
[…]
$ ls -lh data/ch-wa.xml
-rw-rw-r-- 1 joe users 130M Feb 25 22:36 data/ch-wa.xml
```

The resulting XML document can be found at `data/ch-wa.xml`.

## License

Chadwyck-Healey's edition of Goethe's works is subject to
copyright. The conversion of the edition's data has been conducted
**solely for the purpose of text and data mining in the context of a
non-commercial research project** and is as such legal under the
provision of German [Urheberrechtsgesetz §
60d](https://www.gesetze-im-internet.de/urhg/__60d.html).

The conversion routines are licensed under the GNU General Public
License v3.0.

## Bibliography

1. Goethe, J. W., & Chadwyck-Healey Ltd. (1995). Goethes Werke auf CD-ROM.
   Cambridge: Chadwyck-Healey Ltd.
   [WorldCat](http://www.worldcat.org/oclc/1176029950).
1. DynaText [Wikipedia](https://en.wikipedia.org/wiki/Dynatext).
1. DeRose, Steven, Vogel, Jeffrey (1991). Data processing system and method for
   generating a representation for and random access rendering of electronic
   documents. [Google
   Patents](https://patents.google.com/patent/US6101512A/en?oq=6%2c101%2c512)
