package com.labimo.fs.fswalker;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FSWalkerFactory {

	private static FSWalkerFactory fsf=null;
	
	private FSWalkerFactory() {
		
		
	}
	

	public static FSWalkerFactory getInstance() {
		if (fsf==null) {
			fsf=new FSWalkerFactory();
		}
		return fsf;
	}
	
	public FSWalker getFSWalker(Path path,BasicFileAttributes attrs,FSVisitor visitor) {
		FSWalker walker=null;
		//default impl
		if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			walker= new DefaultFSWalker(path);
		}
		if (walker!=null) walker.setFSVisitor(visitor);
		return walker;
	}
	
}
