package se.tap2.j2nbus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.lang.model.element.Modifier;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions("outDir")
public class J2NAnnotationProcessor extends
		AbstractProcessor {


    public final static String OUT_DIR = "outDir";

	private final static String PRIM_TO_BYTES_FORMAT = "" +
			"__size += J2NProto::serialize(&%s, sizeof(%s), __out + __size);";

    private final static String PRIM_FROM_BYTES_FORMAT = "" +
			"__size += J2NProto::deserialize(&%s, sizeof(%s), __in + __size);";

    private final static String PRIMARRAY_TO_BYTES_FORMAT = "" +
            "__size += J2NProto::serialize(&%s, __out + __size);";

    private final static String PRIMARRAY_FROM_BYTES_FORMAT = "" +
            "__size += J2NProto::deserialize(&%s, __in + __size);";

    private final static String LIST_TO_BYTES_FORMAT = "" +
            "__size += J2NProto::serializeList(&%s, __out + __size);";

    private final static String LIST_FROM_BYTES_FORMAT = "" +
            "__size += J2NProto::deserializeList(&%s, __in + __size);";

    private final static String STRING_TO_BYTES_FORMAT = "" +
			"__size += J2NProto::serialize(%s, __out + __size);";
	
	private final static String STRING_FROM_BYTES_FORMAT =
            "__size += J2NProto::deserialize(&%s, __in + __size);";
	
	private Messager messager;
	private Set<String> supportedAnnotationNames;
	private Types typeUtils;
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		messager = processingEnv.getMessager();
		
		typeUtils = processingEnv.getTypeUtils();

		messager.printMessage(Diagnostic.Kind.NOTE, "Starting j2n annotation processing");

    }
	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {

		for (TypeElement typeElement : annotations) {
			messager.printMessage(Diagnostic.Kind.NOTE, "Processing j2n " + typeElement.toString());	
			Set<? extends Element> elementsAnnotatedWithData = roundEnv.getElementsAnnotatedWith(typeElement);
			
			for (Element element : elementsAnnotatedWithData) {
				
				if (element.getKind() == ElementKind.CLASS) {
					TypeElement classElement = (TypeElement) element;
					PackageElement packageElement =
							(PackageElement) classElement.getEnclosingElement();
					
					
					writeClassDeclarationsAndSerialization(classElement, roundEnv);
				}
			}
			
			
		}
		
		return true;
	}


    private String findOutDir() {
        String path = processingEnv.getOptions().get(OUT_DIR);
        if(path != null) {
            return path;
        }

        return "./jni/j2nbus-generated";
    }
	
	private void writeClassDeclarationsAndSerialization(TypeElement classElement, RoundEnvironment roundEnv) {


        File outDir = new File(findOutDir());
        outDir.mkdirs();


        ArrayList<String> declarations = new ArrayList<String>();
        ArrayList<String> includes = new ArrayList<String>();
		
		ArrayList<String> serializations = new ArrayList<String>();
		ArrayList<String> deserializations = new ArrayList<String>();
		
		List<Element> allMembers = new ArrayList<Element>(processingEnv.getElementUtils().getAllMembers(classElement));

        Collections.sort(allMembers, new Comparator<Element>() {
            @Override
            public int compare(Element e, Element e2) {
                return e.getSimpleName().toString().compareTo(e2.getSimpleName().toString());
            }
        });

		for (Element enclosedElement : allMembers) {

			if(enclosedElement.getKind() == ElementKind.FIELD) {
				VariableElement variableElement = (VariableElement) enclosedElement;

                if(variableElement.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

				addDeclaration(roundEnv, declarations, includes, serializations, deserializations, variableElement);

			}

		}

        String s = buildClassString(classElement.getQualifiedName(), classElement.getSimpleName(), includes, declarations, serializations, deserializations);

        //messager.printMessage(Diagnostic.Kind.NOTE, s);
        try {
            File file = new File(outDir, classElement.getSimpleName().toString() + ".h");
            file.createNewFile();
            PrintWriter printWriter = new PrintWriter(file);
            printWriter.append(s);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private String buildClassString(Name qualifiedName, Name simpleName, ArrayList<String> includes, ArrayList<String> declarations, ArrayList<String> serializations, ArrayList<String> deserializations) {

        String classString = Templates.CPP_CLASS;

        classString = classString.replace("__$simpleclassname", simpleName.toString());
        classString = classString.replace("__$fullclassname", qualifiedName.toString());

        StringBuilder declarationsStrigBuilder = stringForList(declarations);
        classString = classString.replace("__$declarations", declarationsStrigBuilder.toString());

        StringBuilder includesStrigBuilder = stringForList(includes);
        classString = classString.replace("__$includes", includesStrigBuilder.toString());

        StringBuilder serializationsStringBuilder = stringForList(serializations);
        classString = classString.replace("__$serializations", serializationsStringBuilder.toString());

        StringBuilder deserializationsStringBuilder = stringForList(deserializations);
        classString = classString.replace("__$deserializations", deserializationsStringBuilder.toString());

        return classString;
    }

    private StringBuilder stringForList(ArrayList<String> declarations) {
        StringBuilder declarationsStrigBuilder = new StringBuilder();

        declarationsStrigBuilder.append("\n");
        for (String declaration : declarations) {
            declarationsStrigBuilder.append(declaration);
            declarationsStrigBuilder.append("\n");

        }
        return declarationsStrigBuilder;
    }


    private void addDeclaration(RoundEnvironment roundEnv,
                                ArrayList<String> declarations, ArrayList<String> includes, ArrayList<String> serializations, ArrayList<String> deserializations, VariableElement variableElement) {
		String typeName = variableElement.asType().toString();
		
		if(typeName.equals(String.class.getName())) {
			
			String stringDeclaration = getStringDeclaration(variableElement);
			declarations.add(stringDeclaration);

            String stringSerialization = STRING_TO_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            serializations.add(stringSerialization);

            String stringDeserialization = STRING_FROM_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            deserializations.add(stringDeserialization);
			
		} else if (isPrimitiveType(typeName)) {
			
			String primitiveDeclaration = getPrimitiveDeclaration(variableElement);
			declarations.add(primitiveDeclaration);

            String stringSerialization = PRIM_TO_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            serializations.add(stringSerialization);

            String stringDeserialization = PRIM_FROM_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            deserializations.add(stringDeserialization);
			
		} else if (nestedHasDataAnnotation(typeName, roundEnv)) {
			
			String cppTypeName = javaClassNameToCppClassName(variableElement);
			String declaration = getDeclaration(cppTypeName, variableElement);
			declarations.add(declaration);

            String serialization = "__size += " + variableElement.getSimpleName().toString() + ".toBytes(__out + __size);";
            serializations.add(serialization);

            String deserialization = "__size += " + variableElement.getSimpleName().toString() + ".fromBytes(__in + __size);";
            deserializations.add(deserialization);

            includes.add("#include \"" + cppTypeName + ".h\"");
			
		} else if (isPrimitiveArrayType(typeName)) {

            String primitiveArrayDeclaration = getPrimitiveArrayDeclaration(variableElement);
            declarations.add(primitiveArrayDeclaration);

            String stringSerialization = PRIMARRAY_TO_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            serializations.add(stringSerialization);

            String stringDeserialization = PRIMARRAY_FROM_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            deserializations.add(stringDeserialization);

			messager.printMessage(Diagnostic.Kind.WARNING, "ARRAY ARRAY", variableElement);
			
		} else if (isListOfData(typeName, roundEnv)) {


            String elementTypeName = getElementTypeName(typeName);
            elementTypeName = elementTypeName.substring(elementTypeName.lastIndexOf(".") + 1); // strip namespace
            String cppTypeName = getCppArrayTypeName(elementTypeName);
            String declaration = getDeclaration(cppTypeName, variableElement);
            declarations.add(declaration);

            String stringSerialization = LIST_TO_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            serializations.add(stringSerialization);

            String stringDeserialization = LIST_FROM_BYTES_FORMAT.replaceAll("%s", variableElement.getSimpleName().toString());
            deserializations.add(stringDeserialization);

            includes.add("#include \"" + elementTypeName + ".h\"");

        } else {
            messager.printMessage(Diagnostic.Kind.WARNING, "All fields must be primitive or classes annotated with @Data .... was " + typeName, variableElement);
        }
	}

    private boolean isListOfData(String typeName, RoundEnvironment roundEnv) {
        if(!typeName.startsWith(List.class.getName())) {
            return false;
        }

        String elementTypeName = getElementTypeName(typeName);

        return nestedHasDataAnnotation(elementTypeName, roundEnv);
    }

    private String getElementTypeName(String typeName) {
        return typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">"));
    }

    private String getPrimitiveArrayDeclaration(VariableElement variableElement) {

        String cppTypeName = getCppArrayTypeName(variableElement.asType().toString());

        return getDeclaration(cppTypeName, variableElement);

    }


    private String javaClassNameToCppClassName(VariableElement variableElement) {
		String typeName = variableElement.asType().toString();
		if(typeName.contains(".")) {
			return typeName.substring(typeName.lastIndexOf('.') + 1);			
		} else {
			return typeName;
		}
	}
	private String getDeclaration(String cppTypeName, VariableElement variableElement) {
		
		String declaration = String.format("%s %s;", cppTypeName, variableElement.getSimpleName());
		//messager.printMessage(Diagnostic.Kind.NOTE, declaration, variableElement);
		return declaration;
		
	}
	private boolean nestedHasDataAnnotation(String typeName, RoundEnvironment roundEnv) {
		
		Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(Data.class);
		for (Element element : elementsAnnotatedWith) {
			if(element.asType().toString().equals(typeName)) {
				return true;
			} 
			
		}
		
		return false;
		
	}
	private String getPrimitiveDeclaration(VariableElement variableElement) {
		
		String cppTypeName = getCppTypeName(variableElement.asType().toString());
		
		return getDeclaration(cppTypeName, variableElement);

		
	}

    private String getCppArrayTypeName(String javaTypeName) {

        if(javaTypeName.equals("long[]")) {
            return "std::vector<int64>";
        } else if(javaTypeName.equals("short[]")) {
            return "std::vector<int16>";
        } else if(javaTypeName.equals("boolean[]")) {
            return "std::vector<bool>";
        } else if (javaTypeName.equals("byte[]")) {
            return "std::vector<char>";
        } else if (javaTypeName.equals("int[]")) {
            return "std::vector<int>";
        } else if (javaTypeName.equals("double[]")) {
            return "std::vector<double>";
        } else if (javaTypeName.equals("float[]")) {
            return "std::vector<float>";
        } else {
            return String.format("std::vector<%s>", javaTypeName);
        }

    }
	private String getCppTypeName(String javaTypeName) {
		
		if(javaTypeName.equals("long")) {
			return "int64";
		} else if(javaTypeName.equals("short")) {
			return "int16";
		} else if(javaTypeName.equals("boolean")) {
			return "bool";
		} else if (javaTypeName.equals("byte")) {
			return "char";
		} else {
			return javaTypeName;
		}
		
		
		
	}
	private boolean isPrimitiveType(String typeName) {
		return "int short byte long boolean double float".contains(typeName);
	}

    private boolean isPrimitiveArrayType(String typeName) {
        return "int[] short[] byte[] long[] boolean[] double[] float[]".contains(typeName);
    }
	private String getStringDeclaration(VariableElement variableElement) {
		return getDeclaration("std::string", variableElement);

		
	}

	
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		if (supportedAnnotationNames == null) {
			Class<?>[] annotationClassesArray = { //
					Data.class
			};

			Set<String> set = new HashSet<String>(annotationClassesArray.length);
			for (Class<?> annotationClass : annotationClassesArray) {
				set.add(annotationClass.getName());
			}

			supportedAnnotationNames = Collections.unmodifiableSet(set);
		}
		return supportedAnnotationNames;
	}

	

}
