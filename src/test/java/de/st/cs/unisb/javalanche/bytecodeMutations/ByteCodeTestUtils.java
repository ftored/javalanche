package de.unisb.cs.st.javalanche.mutation.bytecodeMutations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import de.unisb.cs.st.javalanche.mutation.javaagent.MutationForRun;
import de.unisb.cs.st.javalanche.mutation.mutationPossibilities.MutationPossibilityCollector;
import de.unisb.cs.st.javalanche.mutation.results.Mutation;
import de.unisb.cs.st.javalanche.mutation.results.MutationCoverage;
import de.unisb.cs.st.javalanche.mutation.results.MutationTestResult;
import de.unisb.cs.st.javalanche.mutation.results.TestName;
import de.unisb.cs.st.javalanche.mutation.results.Mutation.MutationType;
import de.unisb.cs.st.javalanche.mutation.results.persistence.HibernateUtil;
import de.unisb.cs.st.javalanche.mutation.results.persistence.QueryManager;

import de.unisb.cs.st.ds.util.io.Io;
import de.unisb.st.bytecodetransformer.processFiles.FileTransformer;

/**
 *
 * Class contains several helper methods for UnitTests that test the different
 * mutations.
 *
 * @author David Schuler
 *
 */
// Because of hibernate
@SuppressWarnings("unchecked")
public class ByteCodeTestUtils {

	private static final String DEFAULT_OUTPUT_FILE = "redefine-ids.txt";

	private static Logger logger = Logger.getLogger(ByteCodeTestUtils.class);

	private ByteCodeTestUtils() {
	}

	public static void deleteCoverageData(String className) {

		List<Mutation> mutationsForClass = QueryManager.getMutationsForClass(className);
		List<Long> ids = new ArrayList<Long>();
		for (Mutation mutation : mutationsForClass) {
			ids.add(mutation.getId());
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		Query query = session
				.createQuery("from MutationCoverage WHERE mutationId IN (:mutation_ids) ");
		query.setParameterList("mutation_ids", ids);
		List l = query.list();
		for (Object o : l) {
			session.delete(o);
		}
		tx.commit();
		session.close();
	}

	public static void generateTestDataInDB(String classFileName,
			CollectorByteCodeTransformer collectorTransformer) {
		File classFile = new File(classFileName);
		FileTransformer ft = new FileTransformer(classFile);
		MutationPossibilityCollector mpc = new MutationPossibilityCollector();
		collectorTransformer.setMpc(mpc);
		ft.process(collectorTransformer);
		mpc.toDB();
	}

	@SuppressWarnings("unchecked")
	public static void deleteTestMutationResult(String className) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		String queryString = String
				.format("from Mutation where classname=:clname");
		Query q = session.createQuery(queryString);
		q.setString("clname", className);
		List<Mutation> mutations = q.list();
		for (Mutation m : mutations) {
			MutationTestResult singleTestResult = m.getMutationResult();
			if (singleTestResult != null) {
				logger.info("Trying to delete + " + singleTestResult);
				m.setMutationResult(null);
				session.delete(singleTestResult);
			}
		}
		tx.commit();
		session.close();
	}

	public static void deleteTestMutationResultOLD(String className) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		String queryString = String
				.format("from Mutation where classname=:clname");
		Query q = session.createQuery(queryString);
		q.setString("clname", className);
		List mutations = q.list();
		for (Object m : mutations) {
			((Mutation) m).setMutationResult(null);
		}
		tx.commit();
		session.close();
	}

	public static void deleteMutations(String className) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		String queryString = String
				.format("delete from Mutation where classname=:clname");
		Query q = session.createQuery(queryString);
		q.setString("clname", className);
		int rowsAffected = q.executeUpdate();
		logger.info("Deleted " + rowsAffected + " rows");
		tx.commit();
		session.close();
	}

	public static void generateCoverageData(String className,
			String[] testCaseNames, int[] linenumbers) {
		List<Mutation> mutations = QueryManager.getMutationsForClass(className);
		List<TestName> testNames = new ArrayList<TestName>();
		for (String testCaseName : testCaseNames) {
			TestName tm = QueryManager.getTestName(testCaseName);
			if (tm == null) {
				tm = new TestName(testCaseName);
				QueryManager.save(tm);
			}
			testNames.add(tm);
		}
		for (Mutation m : mutations) {
			MutationCoverage mutationCoverage = new MutationCoverage(m.getId(),
					testNames);
			QueryManager.save(mutationCoverage);
		}
	}

	public static String[] generateTestCaseNames(String testCaseClassName,
			int numberOfMethods) {
		String[] testCaseNames = new String[numberOfMethods];
		for (int i = 0; i < numberOfMethods; i++) {
			testCaseNames[i] = testCaseClassName + ".testMethod" + (i + 1);
		}
		return testCaseNames;
	}

	@SuppressWarnings("unchecked")
	public static String getFileNameForClass(Class clazz) {
		String result = null;
		try {
			String className = clazz.getSimpleName() + ".class";
			result = clazz.getResource(className).getFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Tests if exactly one testMethod failed because of the mutation.
	 *
	 * @param testClassName
	 *            The class that test the mutated class.
	 */
	@SuppressWarnings("unchecked")
	public static void testResults(String testClassName) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		Query query = session
				.createQuery("from Mutation as m where m.className=:clname");
		query.setString("clname", testClassName);
		List<Mutation> mList = query.list();
		int nonNulls = 0;
		for (Mutation m : mList) {
			System.out.println(m);
			if (m.getMutationType() != MutationType.NO_MUTATION) {
				MutationTestResult singleTestResult = m.getMutationResult();
				if (singleTestResult != null) {
					nonNulls++;
					Assert.assertEquals("Mutation: " + m, 1, singleTestResult
							.getNumberOfErrors()
							+ singleTestResult.getNumberOfFailures());
				}
			}
		}

		tx.commit();
		session.close();
		Assert.assertTrue("Expected failing tests because of mutations",
				nonNulls >= mList.size() / 2);
	}

	@SuppressWarnings("unchecked")
	public static void redefineMutations(String testClassName) {
		List<Long> ids = new ArrayList<Long>();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		Query query = session
				.createQuery("from Mutation as m where m.className=:clname");
		query.setString("clname", testClassName);
		List<Mutation> mList = query.list();
		for (Mutation m : mList) {
			ids.add(m.getId());
		}
		tx.commit();
		session.close();
		StringBuilder sb = new StringBuilder();
		for (Long l : ids) {
			sb.append(l + "\n");
		}
		File file = new File(DEFAULT_OUTPUT_FILE);
		Io.writeFile(sb.toString(), file);
		System.setProperty("mutation.file", file.getAbsolutePath());
		MutationForRun.getInstance().reinit();
	}

	public static void addMutations(String filename) {
		FileTransformer ft = new FileTransformer(new File(filename));
		MutationPossibilityCollector mpc = new MutationPossibilityCollector();
		ft.process(new MutationScannerTransformer(mpc));
		mpc.toDB();
	}

	public static void doSetup(String classname,
			CollectorByteCodeTransformer collector) {
		deleteMutations(classname);
		generateTestDataInDB(System.getProperty("user.dir")
				+ "/target/classes/" + classname.replace('.', '/') + ".class",
				collector);
		System.setProperty("mutation.run.mode", "mutation-no-invariant");
		System.setProperty("invariant.mode", "off");
		redefineMutations(classname);
	}
}
