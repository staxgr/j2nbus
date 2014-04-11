#pragma once

#include <vector>
class J2NProto {
public:
	static int serialize(std::string s, char* out) {
		int size = 0;

		int s__size = s.size();
		memcpy(out + size, (char*)&s__size, sizeof(s__size));
		size +=sizeof(s__size);
		memcpy(out + size, (char*)s.c_str(), s.size());
		size += s.size();

		return size;
	}

	static int serialize(void* prim, int sizeOfPrim, char* out) {
		memcpy(out, (char*)prim, sizeOfPrim);
		return sizeOfPrim;
	}
    template <class element_type>
	static int serialize(std::vector<element_type>* array, char* out) {

        int arraySize = array->size();
	    out += serialize(&arraySize, sizeof(arraySize), out);
	    for(int i = 0 ; i < array->size(); i++) {
	        out += serialize((void*)&(array->at(i)), sizeof(element_type), out);
	    }

        return 4 + sizeof(element_type) * arraySize;
    }

    template <class j2n_quacker>
    static int serializeList(std::vector<j2n_quacker>* list, char* out) {
        int out_start = (int)out;
        int listSize = list->size();
        out += serialize(&listSize, sizeof(listSize), out);
        for(int i = 0 ; i < list->size(); i++) {
            out += list->at(i).toBytes(out);
        }

        return ((int)out) - out_start;
    }

	static int deserialize(std::string* s, char* in) {
		int size = 0;

		int s__size = 0;
		size += deserialize(&s__size, 4, in + size);
		*s = std::string(in + size, s__size );
		size += s__size;

		return size;
	}


	static int deserialize(void* prim, int sizeOfPrim, char* in) {
		memcpy(prim, in, sizeOfPrim);
		return sizeOfPrim;
	}

    template <class element_type>
	static int deserialize(std::vector<element_type>* array, char* in) {
	    int arraySize;
	    in += deserialize(&arraySize, sizeof(arraySize), in);

	    array->clear();

        for(int i = 0 ; i < arraySize; i++) {
            element_type e;
            in += deserialize(&e, sizeof(e), in);
            array->push_back(e);
        }

        return 4 + sizeof(element_type) * arraySize;
    }

    template <class j2n_quacker>
    static int deserializeList(std::vector<j2n_quacker>* list, char* in) {
        int in_start = (int)in;
        int listSize = list->size();
        in += deserialize(&listSize, sizeof(listSize), in);
        for(int i = 0 ; i < listSize; i++) {
            j2n_quacker e;
            in += e.fromBytes(in);
        }

        return ((int)in) - in_start;
    }
};
