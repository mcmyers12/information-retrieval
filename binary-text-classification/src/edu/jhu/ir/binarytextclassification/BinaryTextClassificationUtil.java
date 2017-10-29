package edu.jhu.ir.binarytextclassification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods to support the use of open source machine learning toolkit SVMlight
 * Process tab separated value (TSV) files, which are split into train, dev, and test sets
 * 		and create feature vectors saved to a file in a format recognizable by SVMlight
 * Train set is for building the models, dev is used in experiments, predictions are on test
 *
 * Using the results from SVMlight, calculate precision, recall, and f score
 *
 * Conduct four separate runs using SVM light
 * 		1. Baseline: use features only from the title field
 * 		2. Experiment 1: Use features from the title, abstract, and keywords fields
 * 		3. Experiment 2: Use attributed fields, i.e. features are specific to the column they come from
 * 		4. Experiment 4: Use attributed fields in addition to all columns in the data set
 * 				Columns used: title, authors, journal, ISSN, year, language, abstract, keywords
 *
 * Output test set predictions to a file using the approach with the best results (Experiment 4)
 *
 * @author Miranda Myers
 *
 */
public class BinaryTextClassificationUtil {

	private Map<String, Integer> termIdMap = new HashMap<>(); //Map of term string to termID
	private int termIdCounter = 1;
	private boolean attributed;

	public BinaryTextClassificationUtil(boolean attributed) {
		this.attributed = attributed;
	}


	/**
	 * Builds a map of term to termID for all terms in all sets - train, dev, test
	 * Takes one or more column indices from which to build the term map
	 * @param columnIndices
	 * @throws IOException
	 */
	private void buildTermIdMap(int...columnIndices) throws IOException {
		buildTermIdMap("phase1.train.shuf.tsv", columnIndices);
		buildTermIdMap("phase1.dev.shuf.tsv", columnIndices);
		buildTermIdMap("phase1.test.shuf.tsv", columnIndices);
	}


	/**
	 * Helper method to build term ID map
	 * @param columnIndices
	 * @throws IOException
	 */
	public void buildTermIdMap(String inputFileName, int...columnIndices) throws IOException {
		BufferedReader tsvFile = new BufferedReader(new FileReader(inputFileName));
		String row = tsvFile.readLine(); // Read first line.

		while (row != null) {
			String[] columns = row.split("\t", -1);

			for (int index : columnIndices) {
				if (columns.length > index) {
					for (String token: IRUtil.tokenize(columns[index])) {
						if (attributed) {
							token = index + token;
						}

						if (!termIdMap.containsKey(token)) {
							termIdMap.put(token, termIdCounter);
							termIdCounter++;
						}
					}
				}
			}

			row = tsvFile.readLine(); // Read next line of data.
		}

		tsvFile.close();
	}


	/**
	 * Given an input file name and an output file name, output a file containing
	 * 		feature vectors, one document vector per line
	 * Output file follows the format expected by SVM light
	 * Binary weights are used for each term
	 * The term ID map is sorted so that term IDs are in increasing order
	 * @param inputFileName
	 * @param outputFileName
	 * @param columnIndices
	 * @throws IOException
	 */
	public void outputFeatureVectors(String inputFileName, String outputFileName, int... columnIndices) throws IOException {
		BufferedReader tsvFile = new BufferedReader(new FileReader(inputFileName));
		PrintWriter writer = new PrintWriter(outputFileName);
		String row = tsvFile.readLine(); // Read first line.

		while (row != null) {
			String[] columns = row.split("\t", -1);

			String classLabel = row.substring(0, 2).trim();
			if (classLabel.equals("1")) {
				classLabel = "+1";
			}

			writer.print(classLabel + " ");

			Map<String, Integer> termIds = new HashMap<>();
			for (int index : columnIndices) {
				List<String> tokens = IRUtil.tokenize(columns[index]);

				for (String token: tokens) {
					if (attributed) {
						token = index + token;
					}

					termIds.put(token, termIdMap.get(token));
				}
			}

			Map<String, Integer> sortedTermIds = IRUtil.sortMapByValue(termIds);

			for (int termId: sortedTermIds.values()) {
				writer.print(termId + ":1 ");
			}
			writer.println();
			row = tsvFile.readLine(); // Read next line of data.
		}
		tsvFile.close();
		writer.close();
	}


	/**
	 * Processes the data files and writes a file containing feature vectors,
	 * 		one document vector per line
	 * Takes one or more column indices that represent which features to use
	 * @throws IOException
	 */
	public void outputFeatureVectors(String filePrefix, int... columnIndices) throws IOException {
		outputFeatureVectors("phase1.train.shuf.tsv", filePrefix + "-svmlight-train.txt", columnIndices);
		outputFeatureVectors("phase1.dev.shuf.tsv", filePrefix + "-svmlight-dev.txt", columnIndices);
		outputFeatureVectors("phase1.test.shuf.tsv", filePrefix + "-svmlight-test.txt", columnIndices);
	}


	/**
	 * Given output from an SVMLight classifier and the name of a dev set file,
	 * 		calculate and print recall, precision, and f score
	 * Print the numerator and denominator for precision and recall
	 * @param classifierOutputFileName
	 * @param devFileName
	 * @throws IOException
	 */
	public void calculateMetrics(String classifierOutputFileName, String devFileName) throws IOException {
		BufferedReader classifierOutputFile = new BufferedReader(new FileReader(classifierOutputFileName));
		BufferedReader devFile = new BufferedReader(new FileReader(devFileName));
		String classifierOutputRow = classifierOutputFile.readLine();
		String devFileRow = devFile.readLine();

		int precisionNumerator = 0;
		int precisionDenominator = 0;
		int recallNumerator = 0;
		int recallDenominator = 0;

		while (classifierOutputRow != null) {
			double score = Double.parseDouble(classifierOutputRow);
			String actualLabel = devFileRow.substring(0, 2).trim();

			String predictedLabel;
			if (score > 0) {
				predictedLabel = "1";
			}
			else {
				predictedLabel = "-1";
			}

			if (predictedLabel.equalsIgnoreCase("1")) {
				precisionDenominator++;
			}

			if (predictedLabel.equals(actualLabel) && predictedLabel.equals("1")) {
				precisionNumerator++;
				recallNumerator++;
			}

			if (actualLabel.equals("1")) {
				recallDenominator++;
			}


			classifierOutputRow = classifierOutputFile.readLine();
			devFileRow = devFile.readLine();
		}

		double precision = 100 * (precisionNumerator / (double) precisionDenominator);
		precision = Math.round(precision * 100.0) / 100.0;

		double recall = 100 * (recallNumerator / (double) recallDenominator);
		recall = Math.round(recall * 100.0) / 100.0;

		double fScore = 2 * precision * recall / (precision + recall);
		fScore = Math.round(fScore * 100.0) / 100.0;

		System.out.println("precisionNumerator: " + precisionNumerator);
		System.out.println("precisionDenominator: " + precisionDenominator);
		System.out.println("precision: " + precision + "%");
		System.out.println("\nrecallNumerator: " + recallNumerator);
		System.out.println("recallDenominator: " + recallDenominator);
		System.out.println("recall: " + recall + "%");

		System.out.println("\nf score: " + fScore);
		devFile.close();
		classifierOutputFile.close();
	}


	/**
	 * Given the output from experiment 3 SVMLight classifier run on the test set,
	 * 		output test set predictions to a file
	 * Use format docid [tab] prediction
	 * @throws IOException
	 */
	public void outputTestSetPredictions() throws IOException {
		PrintWriter writer = new PrintWriter("myers-prog4.txt");
		BufferedReader classifierOutputFile = new BufferedReader(new FileReader("experiment3-classifier-output-test.txt"));
		BufferedReader testFile = new BufferedReader(new FileReader("phase1.test.shuf.tsv"));

		String classifierOutputRow = classifierOutputFile.readLine();
		String testFileRow = testFile.readLine();
		while (classifierOutputRow != null) {
			String[] columns = testFileRow.split("\t", -1);
			String docId = columns[1];

			double score = Double.parseDouble(classifierOutputRow);

			String predictedLabel;
			if (score > 0) {
				predictedLabel = "1";
			}
			else {
				predictedLabel = "-1";
			}

			writer.println(docId + "\t" + predictedLabel);

			classifierOutputRow = classifierOutputFile.readLine();
			testFileRow = testFile.readLine();
		}

		writer.close();
		classifierOutputFile.close();
		testFile.close();
	}


	/**
	 * Main execution program - sections in this main program are run separately in the following order:
	 * 		1.  Methods to output feature vectors to files in SVMlight format
	 *		2.  Methods to calculate metrics based on SVMlight output
	 *		3.  Method to output test set predictions
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		//////////////////////////Methods to output feature vectors to files in SVMlight format/////////////////
		BinaryTextClassificationUtil binaryTextClassifier = new BinaryTextClassificationUtil(false);

		// Baseline and Scoring
		binaryTextClassifier.buildTermIdMap(2);
		binaryTextClassifier.outputFeatureVectors("baseline", 2);

		// Experiment 1: Use features from title, abstract, and keywords
		binaryTextClassifier.buildTermIdMap(2, 8, 9);
		binaryTextClassifier.outputFeatureVectors("experiment1", 2, 8, 9);

		// Experiment 2:
		BinaryTextClassificationUtil binaryTextClassifierAttributed = new BinaryTextClassificationUtil(true);
		binaryTextClassifierAttributed.buildTermIdMap(2, 8, 9);
		binaryTextClassifierAttributed.outputFeatureVectors("experiment2", 2, 8, 9);

		// Experiment 3:
		BinaryTextClassificationUtil binaryTextClassifierAttributedAllFeatures = new BinaryTextClassificationUtil(true);
		binaryTextClassifierAttributedAllFeatures.buildTermIdMap(2, 3, 4, 5, 6, 7, 8, 9);
		binaryTextClassifierAttributedAllFeatures.outputFeatureVectors("experiment3", 2, 3, 4, 5, 6, 7, 8, 9);


		///////////////////////////////Methods to calculate metrics based on SVMlight output/////////////////////
		// Metrics calculations
		binaryTextClassifier.calculateMetrics("baseline-classifier-output.txt", "phase1.dev.shuf.tsv");
		binaryTextClassifier.calculateMetrics("experiment1-classifier-output.txt", "phase1.dev.shuf.tsv");
		binaryTextClassifier.calculateMetrics("experiment2-classifier-output.txt", "phase1.dev.shuf.tsv");
		binaryTextClassifier.calculateMetrics("experiment3-classifier-output.txt", "phase1.dev.shuf.tsv");


		//////////////////////////////Method to output test set predictions//////////////////////////////////////
		//Test set predictions:
		binaryTextClassifierAttributedAllFeatures.outputTestSetPredictions();
	}
}







