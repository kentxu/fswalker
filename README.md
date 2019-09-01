# FSWalker

A library to iterate through directory structure to perform data collection for further data analysis. One use case is to list all the files and directories. 

It also comes with basic command line support for some common use cases.



## Source & Build

```
git clone https://github.com/kentxu/fswalker.git && cd fswalker
mvn clean package
```

Generated JAR files are in the "target" directory. 

fswalker-x.x.x.jar comes with all the dependencies in a single package. It is great for command line execution. 

original-fswalker-x.x.x.jar is the pure fswalker lib without dependencies.



## CLI Usage

To show all content in c:\temp.
> java -jar target/fswalker-x.x.x.jar c:\temp

To show a summary of c:\temp .
> java -jar target/fswalker-x.x.x.jar -c c:\temp

To see all possible CLI options.
> java -jar target/fswalker-x.x.x.jar 

Or, only show a summary in a human friendly format. This also turns on the speedy concurrent scanning mode by default.
> java -jar target/fswalker-x.x.x.jar -chq c:\temp

To shown detailed information for all content.
> java -jar target/fswalker-x.x.x.jar -l c:\temp


## More Advanced CLI Usage

Show the list with content hash.
> java -jar target/fswalker-x.x.x.jar -l -hc size20k c:\temp

Or, the speedy way to do the same.
> java -jar target/fswalker-x.x.x.jar -l -hc size20k -co 1 c:\temp

## Sample API USage

The snippet below scans directory "/Library".

```
FSVisitor visitor = new DefaultFSVisitor(null);
walker.setFSVisitor(visitor);
walker.walk("/Library", Integer.MAX_VALUE);
writer.close();
//do something with visitor, e.g. visitor.getFileCount();
```

The snippet below scans directory "/Library" and writes output to test.csv file.

```
FSWalker walker = new DefaultFSWalker();
FSWriter writer = new DefaultFSWriter(new File("test.csv"));
FSVisitor visitor = new DefaultFSVisitor(writer);
walker.setFSVisitor(visitor);
walker.walk("/Library", Integer.MAX_VALUE);
writer.close();
//do something with visitor, e.g. visitor.getFileCount();
```

Concurrent Mode

```
FSWriter writer=null;
ParallelFSVisitorContext ctx=new ParallelFSVisitorContext(writer);
ParallelFSVisitor visitor = new ParallelFSVisitor(ctx);
ParallelFSWalker walker=new ParallelFSWalker(ctx);
walker.setFSVisitor(visitor);
walker.walk("/Library", Integer.MAX_VALUE);
```

## FAQ
### What is concurrent mode?
Concurrent mode is the multi-threaded implementation of the walker. It does speed up things a lot as long as the disk IO is fast. On a SSD drive, using concurrent mode grants a significant speed boost. This is also helpful when content hash option is used.

One major trade off is the sequence of traversal is not guaranteed in this mode. 

This mode is turned on by default console output is not required, e.g. -q or database output. You can force concurrent mode wiht "-co 1". 

## Troubleshoot

FSWalker log can be turned on using standard log4j2 configuration file. For example, the following loads log configuration in CLI.
> java -Dlog4j.configurationFile=log4j2.xml  -jar target/fswalker-x.x.x.jar ~/temp
