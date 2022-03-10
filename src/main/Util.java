package main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Util {
	
	@FunctionalInterface
	public interface Function<X, Y> { 
		Y apply(X x);
	}
	
	public static void print(String s) {
		System.out.println(s);
	}
	
	public static void print(Object o) {
		if (o == null) {
			print(null+"");
			return;
		}
		print(o.toString());
	}
	
	public static void print(Object...os) {
		print(Arrays.toString(os));
	}
	
	public static void println(Collection<Object> os) {
		for (Object o: os)
			print(o);
		print("");
	}
	
	public static String multiply(String s, int n) {
		return new String(new char[n]).replace("\0", s);
	}
	
}

