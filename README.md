j2nbus
======

A bus alternative to JNI. For communication between Java and C++ in the same process. Supports Android.

j2nbus works by automatically translating simple Java data classes to C++ and making them availabe to subscribe to and publish from both Java and C++.

j2nbus takes care of all JNI boilerplating for you. 

This is a minimal example that publishes some data from java and prints it in C++:

First, define a simple data class in java and annotate it with ```@Data```

```java
import se.tap2.j2nbus.Data;

@Data
public class Blurb {
    public int id;
    public String text;
}
```

Now when you build your java project, j2nbus will automatically create the same class in C++ for you.

Add some code to publish the data from java:

```java
System.loadLibrary("<your cpp library>");

J2NBus bus = J2NBus.getBus();
Blurb blurb = new Blurb();
blurb.text = "hello from java";
bus.post(blurb);
```

All you need to do now in C++ to receive the data is:

```Cpp
// Include j2nbus and your generated class
#include "j2nbus.h"
#include "Blurb.h"

// Create a method to receive blurbs
void onBlurb(Blurb blurb) {
	pfrintf("blurb: %s", blurb.text.c_str());
	
}

// This will be called automatically when JNI has been initiated
void initBus(J2NBus* bus) {

    // Register your method with the bus
	bus->subscribe(&onBlurb);

}

```

No more C++ code is needed. jnbus will init JNI for you and export all JNI interfaces neccessary.
