package org.softevo.mutation.run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.softevo.mutation.io.ClassFileSource;
import org.softevo.mutation.io.Io;
import org.softevo.mutation.properties.MutationProperties;


public class SystemExitFinder {

	public static class SystemExitFinderClassAdapter extends ClassAdapter {

		private boolean hasSystemExit;

		private String className;

		public SystemExitFinderClassAdapter(ClassVisitor cv) {
			super(cv);
		}

		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {
			super
					.visit(version, access, name, signature, superName,
							interfaces);
			className = name;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {

			MethodVisitor mv = super.visitMethod(access, name, desc, signature,
					exceptions);
			return new SystemExitFinderMethodAdapter(mv, this);
		}

		/**
		 * @return the hasSystemExit
		 */
		public boolean hasSystemExit() {
			return hasSystemExit;
		}

		/**
		 * @return the className
		 */
		public String getClassName() {
			return className;
		}

		/**
		 * @param hasSystemExit
		 *            the hasSystemExit to set
		 */
		public void setHasSystemExit(boolean hasSystemExit) {
			this.hasSystemExit = hasSystemExit;
		}

	}

	public static class SystemExitFinderMethodAdapter extends MethodAdapter {

		private final SystemExitFinderClassAdapter systemExitFinderClassAdapter;

		public SystemExitFinderMethodAdapter(MethodVisitor mv,
				SystemExitFinderClassAdapter systemExitFinderClassAdapter) {
			super(mv);
			this.systemExitFinderClassAdapter = systemExitFinderClassAdapter;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			super.visitMethodInsn(opcode, owner, name, desc);
			if (name.equals("exit") && owner.equals("java/lang/System")) {
				System.out.println(owner);
				systemExitFinderClassAdapter.setHasSystemExit(true);
			}
		}

	}

	public static void main(String[] args) {
		ClassFileSource cfs = new ClassFileSource();
		List<String> classesWithSystemExit = new ArrayList<String>();
		try {
			List classList = cfs.getClasses(new File(
					MutationProperties.ASPECTJ_DIR));
			for (Object object : classList) {
				// System.out.println("Processing: "+ object);
				if (object instanceof File) {
					File classFile = (File) object;
					byte[] classBytes = Io.getBytesFromFile(classFile);
					ClassReader cr = new ClassReader(classBytes);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					SystemExitFinderClassAdapter systemExitFinderClassAdapter = new SystemExitFinderClassAdapter(
							cw);
					cr.accept(systemExitFinderClassAdapter, 0);
					if (systemExitFinderClassAdapter.hasSystemExit()) {
						classesWithSystemExit.add(systemExitFinderClassAdapter
								.getClassName());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Classes With calls to system exit:");
		for (String string : classesWithSystemExit) {
			System.out.println(string);
		}

	}
}