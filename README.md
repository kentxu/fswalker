# fswalker

A library to iterate through directory structure and map out all files found within for further data analysis. It also comes with basic command line support for some common use cases.



## Source & Build

```
git clone https://github.com/kentxu/fswalker.git && cd fswalker
mvn clean package
```

Generated JAR files are in the target sub directory. 

It generates fswalker-x.x.x.jar with all the dependencies in a single package. It is great for command line execution. 

It also generates original-fswalker-x.x.x.jar which only contains FSWalker code without dependencies.



## CLI

To print a list of all content in c:\temp.
> java -jar target/fswalker-x.x.x.jar c:\temp

To show a summary of c:\temp.
> java -jar target/fswalker-x.x.x.jar -c c:\temp

Or, only show a summary in a human friendly format.
> java -jar target/fswalker-x.x.x.jar -chq c:\temp

To show all files with detailed information.
> java -jar target/fswalker-x.x.x.jar -l c:\temp

Or, show the same list with content hash.
> java -jar target/fswalker-x.x.x.jar -l -hc size20k c:\temp

For a print list of all possible options, execute without any parameter.
> java -jar target/fswalker-x.x.x.jar 

## Troubleshoot

FSWalker log can be turned on using standard log4j2 configuration file. For example, the following loads log configuration in CLI.
> java -Dlog4j.configurationFile=log4j2.xml  -jar target/fswalker-x.x.x.jar ~/temp
