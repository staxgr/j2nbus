LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog -lOpenSLES
LOCAL_MODULE    := main
LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/j2nbus-gen $(LOCAL_PATH)/../../j2nbus-include
LOCAL_SRC_FILES := main.cpp
LOCAL_CPPFLAGS = -std=c++0x -D__STDC_INT64__
include $(BUILD_SHARED_LIBRARY)
