# to compile everything needed
compile:
	@java -jar JarJar/jtb132di.jar -te minijava.jj
	@java -jar JarJar/javacc5.jar minijava-jtb.jj
	@javac Basket_Full_Of_Stuff.java
	@javac Main.java

MYDIR = ./Files

# to semantically check and traslate the files of the directory Files
all:
	@for f in $(shell ls ${MYDIR}); do echo "File:  $${f}"; java Main ${MYDIR}/$${f}; done

# to delete everything (aside from the .ll files)
clean:
	@rm -f *.class JavaCharStream.java;
	@rm -f MiniJavaParser* Token* ParseException.java;
	@rm -rf visitor syntaxtree;
	@rm -f minijava-jtb.jj;