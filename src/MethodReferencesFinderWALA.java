

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.strings.Atom;

//VISIT https://ghostinshelldotnet.wordpress.com/2011/06/09/wala-call-graph-data-structure/

public class MethodReferencesFinderWALA {

	/**
	 * Find the methods that calls another given method (WALA-based implementation)
	 * @param jarFile
	 * @param className
	 * @param methodName
	 * @param methodDescription
	 * @return number of method's callers
	 */
	public int findMethodReferences(String jarFile, String className, String methodName, String methodDescription){
		int referenceCounter = 0;
		try{
			// represents code to be analyzed
			AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarFile, null);

			// a class hierarchy for name resolution, etc.
			IClassHierarchy classHierarchy = ClassHierarchy.make(scope);

			// the class of interest
			TypeReference typeReference = TypeReference.findOrCreate(ClassLoaderReference.Application, className);
			IClass iClass = classHierarchy.lookupClass(typeReference);

			// the method of interest
			Atom mN = Atom.findOrCreateUnicodeAtom(methodName);
			Descriptor mD = Descriptor.findOrCreateUTF8(methodDescription);
			IMethod iMethod = iClass.getMethod(new Selector(mN, mD));

			// call graph entrypoints
			//Iterable<Entrypoint> e = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, classHierarchy);
			Iterable<Entrypoint> e = new AllApplicationEntrypoints(scope, classHierarchy);

			// encapsulates various analysis options
			AnalysisOptions options = new AnalysisOptions(scope, e);

			// builds call graph via pointer analysis
			CallGraphBuilder builder = Util.makeZeroCFABuilder(options,	new AnalysisCache(), classHierarchy, scope);
			CallGraph callGraph = builder.makeCallGraph(options, null);

			// verify if the  method of interest is reached
			Set<CGNode> methodNodes = callGraph.getNodes(iMethod.getReference());
			if(!methodNodes.isEmpty()){
				CGNode methodNode = methodNodes.iterator().next();
				Iterator<CGNode> it = callGraph.getPredNodes(methodNode);
				while(it.hasNext()){
					CGNode n = it.next();
					//if(isApplicationNode(n)){
						System.out.println(it.next());
						referenceCounter++;
					//}
				}
				System.out.println("method invokes: " + (referenceCounter-1));

				return referenceCounter-1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return referenceCounter;
	}

	private boolean isApplicationNode(CGNode n) {
		return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
	}

	public static void main(String[] args) {
		//new MethodReferencesFinderWALA().findMethodReferences("example/call_twocalls.jar", "LClassSoma", "soma", "(II)I");
		//new MethodReferencesFinderWALA().findMethodReferences("example/asm.jar", "Ljava/io/PrintStream", "println", "(Ljava/lang/String;)V");
		//new MethodReferencesFinderWALA().findMethodReferences("example/call_onecall.jar", "Ljava/io/PrintStream", "println", "(Ljava/lang/String;)V");
		new MethodReferencesFinderWALA().findMethodReferences("example/callifelse.jar", "LMember", "soma2", "(II)I");
	}
}
