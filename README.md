eclipse-icon-enlarger
=====================

Scales Eclipse icons (PNG, GIF and JPG) to increase their size for HiDPI displays.

#### How To Build

1. Clone this repository locally
2. Run 'mvn clean package'
3. Your eclipse-icon-enlarger.jar with all dependencies is ready!

#### How To Run

    java -jar eclipse-icon-enlarger.jar -b c:\eclipse -o c:\Temp\eclipse_qhd
     -b,--baseDir <arg>         This is the base directory where we'll parse jars/zips
     -o,--outputDir <arg>       This is the base directory where we'll place output
     -z,--resizeFactor <arg>    This is the resize factor. Default is 2.
     -p,--parallelThreads <arg> Number of parallel threads. Default is available CPU cores.
     -h,--help <arg>            Show help

#### Attention

We recommend:

1. Install all you favorite plugins prior to icons conversion
2. Keep original eclipse directory for plugins installations and etc.

The main reason if that is following: first execution will enlarge 2x, but second execution on already converted files will enlarge 2x one more time
