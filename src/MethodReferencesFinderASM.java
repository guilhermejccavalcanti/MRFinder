

import java.awt.Label;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;

//VISIT http://stackoverflow.com/questions/930289/how-can-i-find-all-the-methods-that-call-a-given-method-in-java

@SuppressWarnings("unused")
public class MethodReferencesFinderASM {
	private String targetClass;
	private Method targetMethod;
	private AppClassVisitor classVisitor;
	private ArrayList<Caller> callers = new ArrayList<Caller>();
	
	/**
	 * Find the methods that calls another given method (ASM-based implementation)
	 * @param jarFile
	 * @param className
	 * @param methodName
	 * @param methodDescription
	 * @return number of method's callers
	 */
	public int findMethodReferences(String jarPath, String targetClass,	String targetMethodDeclaration){
		int referenceCounter = 0;
		try{
			this.targetClass = targetClass;
			this.targetMethod = Method.getMethod(targetMethodDeclaration);
			this.classVisitor = new AppClassVisitor();

			JarFile jarFile = new JarFile(jarPath);
			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class")) {
					InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry), 1024);
					ClassReader reader = new ClassReader(stream);
					reader.accept(classVisitor, 0);
					stream.close();
				}
			}
			jarFile.close();

			for (Caller c : this.callers) {
				System.out.println(c.source + ":" + c.line + " " + c.className + " " + c.methodName + " " + c.methodDesc);
			}

			referenceCounter = callers.size();
			System.out.println("method invokes: " + referenceCounter);

			return referenceCounter;
		}catch(Exception e){
			e.printStackTrace();
		}
		return referenceCounter;
	}

	private class Caller {
		String className;
		String methodName;
		String methodDesc;
		String source;
		int line;

		public Caller(String cName, String mName, String mDesc, String src,int ln) {
			className = cName;
			methodName = mName;
			methodDesc = mDesc;
			source = src;
			line = ln;
		}
	}

	private class AppMethodVisitor extends MethodAdapter {
		boolean callsTarget;
		int line;

		public AppMethodVisitor() {
			super(new EmptyVisitor());
		}

		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			if (owner.equals(targetClass)
					&& name.equals(targetMethod.getName())
					&& desc.equals(targetMethod.getDescriptor())) {
				callsTarget = true;
			}
		}

		public void visitCode() {
			callsTarget = false;
		}

		public void visitLineNumber(int line, Label start) {
			this.line = line;
		}

		public void visitEnd() {
			if (callsTarget)
				callers.add(new Caller(classVisitor.className, classVisitor.methodName,
						classVisitor.methodDesc, classVisitor.source, line));
		}
	}

	private class AppClassVisitor extends ClassAdapter {
		private AppMethodVisitor mv = new AppMethodVisitor();
		public String source;
		public String className;
		public String methodName;
		public String methodDesc;

		public AppClassVisitor() {
			super(new EmptyVisitor());
		}

		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {
			className = name;
		}

		public void visitSource(String source, String debug) {
			this.source = source;
		}

		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			methodName = name;
			methodDesc = desc;

			return mv;
		}
	}

	public static void main(String[] args) {
		//new MethodReferencesFinderASM().findMethodReferences("example/call_multiple2.jar", "ClassSoma","int soma(int,int)");
		//new MethodReferencesFinderASM().findMethodReferences("example/asm.jar", "java/io/PrintStream","void println(String)");
		//new MethodReferencesFinderASM().findMethodReferences("C:\\Users\\Guilherme\\Desktop\\mockito\\output.jar", "org/mockito/internal/InOrderImpl","void markVerified(org.mockito.invocation.Invocation)");
		new MethodReferencesFinderASM().findMethodReferences("example/callifelse.jar", "Member","int soma(int,int)");
	}
}