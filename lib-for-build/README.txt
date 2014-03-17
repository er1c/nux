
This directory contains

- junit.jar (3.8.1):

	Needed for building and testing Nux but not for running it.
	See http://www.junit.org
	
- jaxmeapi.jar: 

	Needed for building Nux but not for running it. 
	See http://ws.apache.org/JaxMe

- stax-api-1.0.jar: 

	Apache licensed StAX interfaces from woodstox-2.0.4. 
	Needed for building Nux benchmarks but not for running it. 
	See http://woodstox.codehaus.org/

- cvs-saxon8.jar: 

	An experimental version of saxon8.jar (v8.4) that includes Mike Kay's latest 
	and greatest source fixes as of Apr 9, 2005. Not recommended for production.
	See the Saxon Sourceforge Bug Tracker at 
	http://sourceforge.net/tracker/?group_id=29872&atid=397617 for what's 
	been fixed. If you encounter unexpected Saxon bugs you can try replacing 
	saxon8.jar with this file (or have cvs-saxon8.jar before saxon8.jar in the classpath) 
	and see if this fixes the problem. You can build this experimental version by manually 
	applying Mike's fixes and running 'cd nux; ant jar-cvs-saxon'. Good luck.
