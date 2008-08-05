package org.softevo.mutation.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import junit.framework.TestResult;

import org.hibernate.annotations.IndexColumn;
import org.softevo.mutation.runtime.MutationTestListener;

@Entity
public class SingleTestResult {

	@Id
	@GeneratedValue
	private Long id;

	private int runs;

	/**
	 * True if the mutation was touched by at least one TestCase;
	 */
	boolean touched;

	@OneToMany(cascade = CascadeType.ALL)
	// , fetch = FetchType.EAGER)
	@OrderBy("testCaseName")
	@IndexColumn(name = "failure_list_id")
	private List<TestMessage> failures = new ArrayList<TestMessage>();

	@OneToMany(cascade = CascadeType.ALL)
	// , fetch = FetchType.EAGER)
	@JoinTable(name = "SingleTestResult_Errors", joinColumns = { @JoinColumn(name = "singleTestResult_id") }, inverseJoinColumns = @JoinColumn(name = "testMessage_id"))
	@IndexColumn(name = "error_id")
	private List<TestMessage> errors = new ArrayList<TestMessage>();

	@OneToMany(cascade = CascadeType.ALL)
	// , fetch = FetchType.EAGER)
	@JoinTable(name = "SingleTestResult_Passing", joinColumns = { @JoinColumn(name = "singleTestResult_id") }, inverseJoinColumns = @JoinColumn(name = "testMessage_id"))
	@IndexColumn(name = "passing_id")
	private List<TestMessage> passing = new ArrayList<TestMessage>();

	// Temporal(TemporalType.TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date date;

//	 @Column(name="vInvariants", nullable=true)
//	 @IndexColumn(name = "violated_id")

	private int[] violatedInvariants;

	private int differentViolatedInvariants;

	private int totalViolations;

	/**
	 * @return the violatedInvariants
	 */
	public int[] getViolatedInvariants() {
		return violatedInvariants;
	}

	/**
	 * @return the totalViolations
	 */
	public int getTotalViolations() {
		return totalViolations;
	}

	/**
	 * @param violatedInvariants
	 *            the violatedInvariants to set
	 */
	public void setViolatedInvariants(int[] violatedInvariants) {
		if (violatedInvariants.length > 30) {
			System.out.println("SingleTestResult.setViolatedInvariants(): truncating violated invariants");
			this.violatedInvariants = new int[30];
			System.arraycopy(violatedInvariants, 0, this.violatedInvariants, 0,
					30);
		} else {
			this.violatedInvariants = violatedInvariants;
		}
	}

	/**
	 * @param totalViolations
	 *            the totalViolations to set
	 */
	public void setTotalViolations(int totalViolations) {
		this.totalViolations = totalViolations;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	@SuppressWarnings("unused")
	// Needed by hibernate
	private SingleTestResult() {
	}

	public SingleTestResult(TestResult mutationTestResult,
			MutationTestListener mutationTestListener,
			Set<String> touchingTestCases) {
		this.runs = mutationTestResult.runCount();
		this.failures = mutationTestListener.getFailureMessages();
		this.errors = mutationTestListener.getErrorMessages();
		this.passing = mutationTestListener.getPassingMessages();
		this.date = new Date();
		if (touchingTestCases != null && touchingTestCases.size() > 0) {
			updateTouched(touchingTestCases, failures);
			updateTouched(touchingTestCases, errors);
			updateTouched(touchingTestCases, passing);
			// updateTimes(mutationTestListener.getDurations());
			touched = true;
		}
	}

	private static void updateTouched(Set<String> touchingTestCases,
			List<TestMessage> testMessages) {
		for (TestMessage tm : testMessages) {
			if (touchingTestCases.contains(tm.getTestCaseName())) {
				tm.setHasTouched(true);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format(
				"Runs: %d  Failures: %d  Errors: %d LineTouched: %s", runs,
				failures.size(), errors.size(), touched ? "yes" : "no"));
		sb.append(" date: " + date + "\n");
		if (failures.size() > 0) {
			sb.append("Failures:\n");
			for (TestMessage tm : failures) {
				sb.append(tm);
				sb.append('\n');
			}
		}
		if (errors.size() > 0) {
			sb.append("Errors:\n");
			for (TestMessage tm : errors) {
				sb.append(tm);
				sb.append('\n');
			}
		}
		if (passing.size() > 0) {
			sb.append("Passing:\n");
			for (TestMessage tm : passing) {
				sb.append(tm);
				sb.append('\n');
			}
		}
		return sb.toString();

	}

	/**
	 * @return the runs
	 */
	public int getRuns() {
		return runs;
	}

	/**
	 * @param runs
	 *            The runs to set
	 */
	public void setRuns(int runs) {
		this.runs = runs;
	}

	/**
	 * @return the errors
	 */

	public int getNumberOfErrors() {
		return errors.size();
	}

	/**
	 * @return the failures
	 */
	public int getNumberOfFailures() {
		return failures.size();
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the errors
	 */
	public Collection<TestMessage> getErrors() {
		return errors;
	}

	/**
	 * @return the failures
	 */
	public List<TestMessage> getFailures() {
		return failures;
	}

	/**
	 * @param errors
	 *            the errors to set
	 */
	public void setErrors(List<TestMessage> errors) {
		this.errors = errors;
	}

	/**
	 * @param failures
	 *            the failures to set
	 */
	public void setFailures(List<TestMessage> failures) {
		this.failures = failures;
	}

	/**
	 * @return the passing
	 */
	public List<TestMessage> getPassing() {
		return passing;
	}

	/**
	 * @param passing
	 *            the passing to set
	 */
	public void setPassing(List<TestMessage> passing) {
		this.passing = passing;
	}

	/**
	 * @return the touched
	 */
	public boolean isTouched() {
		return touched;
	}

	/**
	 * @param touched
	 *            the touched to set
	 */
	public void setTouched(boolean touched) {
		this.touched = touched;
	}

	/**
	 * @return the differentViolatedInvariants
	 */
	public int getDifferentViolatedInvariants() {
		return differentViolatedInvariants;
	}

	/**
	 * @param differentViolatedInvariants
	 *            the differentViolatedInvariants to set
	 */
	public void setDifferentViolatedInvariants(int differentViolatedInvariants) {
		this.differentViolatedInvariants = differentViolatedInvariants;
	}

}