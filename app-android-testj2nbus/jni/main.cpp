#include <android/log.h>

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, ASN_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, ASN_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, ASN_TAG, __VA_ARGS__)

#define ASN_TAG "BLURB"


#include "j2nbus.h"

// Include your generated class
#include "Blurb.h"

// Create a method to receive blurbs
void onBlurb(Blurb blurb) {

    LOGV("onBlurb");
	LOGV("blurb: %s", blurb.text.c_str());

}


/**
* This will be called automatically when JNI has been initiated
*/
void initBus(J2NBus* bus) {
	LOGV("initBus");

    // Register your method with the bus
	bus->subscribe(&onBlurb);

}