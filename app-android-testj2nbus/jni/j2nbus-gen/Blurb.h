/*
 This file is auto generated by j2nbus' annotation processor, that's why it looks a little crazy. 
 DO NOT EDIT! 
*/
#pragma once
#include "j2nbus_protocol.h"
#include <string>             
#include <vector>             

                   
                              
class Blurb {    
    public:                   
   
int id;
std::string text;
            
    static std::string getJavaClassName() {                  
        return getJavaClassNameS();                          
    }                                                        
    static std::string getJavaClassNameS() {                 
        return std::string("se.tap2.testj2nbus.Blurb");    
    }                         
    int toBytes(char* __out) { 
        int __size = 0;        
        
__size += J2NProto::serialize(&id, sizeof(id), __out + __size);
__size += J2NProto::serialize(text, __out + __size);
      
        return __size;         
    }                          
    int fromBytes(char* __in) {
        int __size = 0;        
        
__size += J2NProto::deserialize(&id, sizeof(id), __in + __size);
__size += J2NProto::deserialize(&text, __in + __size);
    
        return __size;         
    }                          
};                              
