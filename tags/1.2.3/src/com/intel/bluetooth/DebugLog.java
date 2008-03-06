/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.util.Enumeration;
import java.util.Vector;

/**
 * The methods of this calls would be removed automaticaly because they are
 * empty if debugCompiledOut = true.
 * 
 * This class itself will disappear from bytecode after obfuscation by proguard.
 * 
 */
public class DebugLog {

	private static final boolean debugCompiledOut = false;

	public final static int DEBUG = 1;

	public final static int ERROR = 4;
	
	private static boolean debugEnabled = false;

	private static boolean initialized = false;
	
	private static final String FQCN = DebugLog.class.getName();
	
	//private static final Set fqcnSet = new HashSet();
	private static final Vector fqcnSet = new Vector(); 
	
	private static boolean java13 = false;

	private static Vector loggerAppenders = new Vector();
	
	public static interface LoggerAppender {
		public void appendLog(int level, String message, Throwable throwable);
	}

	static {
		fqcnSet.addElement(FQCN);
	}
	
	private static void initialize() {
		initialized = true;
		String d = System.getProperty("bluecove.debug");
		debugEnabled = ((d != null) && (d.equalsIgnoreCase("true") || d.equalsIgnoreCase("1")));
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		}
	}
	
	public static boolean isDebugEnabled() {
		if (!initialized) {
			initialize();
		}
		return debugEnabled;
	}
	
	public static void setDebugEnabled(boolean debugEnabled) {
		initialize();
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		}
		DebugLog.debugEnabled = debugEnabled;
	}
	
	public static void debug(String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message);
			printLocation();
			callAppenders(DEBUG, message, null);
		}
	}
	
	public static void debug(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
			printLocation();
			callAppenders(DEBUG, message + " " + v, null);
		}
	}

	public static void debug(String message, Object obj) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + obj.toString());
			printLocation();
			callAppenders(DEBUG, message + " " + obj.toString(), null);
		}
	}
	
	public static void debug(String message, String v, String v2) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v + " " + v2);
			printLocation();
			callAppenders(DEBUG, message + " " + v + " " + v2, null);
		}
	}
	
	public static void debug(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + String.valueOf(v));
			printLocation();
			callAppenders(DEBUG, message + " " + String.valueOf(v), null);
		}
	}

	public static void debug(String message, boolean v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
			printLocation();
			callAppenders(DEBUG, message + " " + v, null);
		}
	}
	
	public static void error(String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message);
			printLocation();
			callAppenders(ERROR, message, null);
		}
	}
	
	public static void error(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
			printLocation();
			callAppenders(ERROR, message + " " + v, null);
		}
	}
	
	public static void error(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
			printLocation();
			callAppenders(ERROR, message + " " + v, null);
		}
	}
	
	public static void error(String message, Throwable t) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + t);
			printLocation();
			t.printStackTrace(System.out);
			callAppenders(ERROR, message, t);
		}
	}

	public static void fatal(String message) {
		System.out.println("error " + message);
		printLocation();
		callAppenders(ERROR, message, null);
	}
	
	public static void fatal(String message, Throwable t) {
		System.out.println("error " + message + " " + t);
		printLocation();
		t.printStackTrace(System.out);
		callAppenders(ERROR, message, t);
	}
	
	private static void callAppenders(int level, String message, Throwable throwable) {
		for (Enumeration iter = loggerAppenders.elements(); iter.hasMoreElements();) {
			LoggerAppender a = (LoggerAppender) iter.nextElement();
			a.appendLog(level, message, throwable);
		};
	}
	
	public static void addAppender(LoggerAppender newAppender) {
		loggerAppenders.addElement(newAppender);
	}
	
	public static void removeAppender(LoggerAppender newAppender) {
		loggerAppenders.removeElement(newAppender);
	}
	
	private static void printLocation() {
		if (java13) {
			return;
		}
		try {
		 System.out.println("\t  "+ formatLocation(getLocation()));
		} catch (Throwable e) {
			java13 = true;
		}
	}
	
	private static String formatLocation(StackTraceElement ste) {
		if (ste == null) {
			return "";
		}
		// Make Line# clickable in eclipse
		return ste.getClassName() + "." + ste.getMethodName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
	}
	
    private static StackTraceElement getLocation() {
		if (java13) {
			return null;
		}
		try {
			StackTraceElement[] ste = new Throwable().getStackTrace();
			for (int i = 0; i < ste.length - 1; i++) {
				if (fqcnSet.contains(ste[i].getClassName())) {
					String nextClassName = ste[i + 1].getClassName();
					if (nextClassName.startsWith("java.") || nextClassName.startsWith("sun.")) {
						continue;
					}
					if (!fqcnSet.contains(nextClassName)) {
						return ste[i + 1];
					}
				}
			}
		} catch (Throwable e) {
			java13 = true;
		}
		return null;
	}

}