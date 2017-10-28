package edu.jhu.ir.binarytextclassification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a binary classifier
 * Uses training data to build the classifier and then uses the trained model to
 * 		make predictions on the test data.
 * Processes tab separated value (TSV) files (split into train, dev, and test sets)
 * 		and creates feature vectors
 * 		Train builds the models, dev is used in experiments, predictions are on test
 * Uses SVMs for training using open source mahchine learning toolkit SVMlight
 *
 * @author Miranda Myers
 *
 */

public class BinaryTextClassificationUtil {

	private Map<String, Integer> termIdMap = new HashMap<>(); //Map of term string to termID
	private int termIdCounter = 1;

	/**
	 * Builds a map of term to termID for all terms in all sets
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
	 *
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
	 * Uses features only from the title sectino (column 3) of the data
	 * @throws IOException
	 */
	public void outputFeatureVectors(int... columnIndices) throws IOException {
		outputFeatureVectors("phase1.train.shuf.tsv", "experiment2-svmlight-train.txt", columnIndices);
		outputFeatureVectors("phase1.dev.shuf.tsv", "experiment2-svmlight-dev.txt", columnIndices);
	}


	public void calculateMetrics(String classifierOutputFileName, String devFileName) throws IOException {
		BufferedReader classifierOutputFile = new BufferedReader(new FileReader(classifierOutputFileName));
		BufferedReader devFile = new BufferedReader(new FileReader(devFileName));
		String classifierOutputRow = classifierOutputFile.readLine();
		String devFileRow = devFile.readLine();

		int precisionNumerator = 0;
		int precisionDenominator = 0;
		int recallNumerator = 0;
		int recallDenominator = 0;

		while (classifierOutputRow != null) { // && devFileRow != null) {
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
				precisionDenominator++; //Increment every time to keep track of total
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

	public static void main(String[] args) throws IOException {
		BinaryTextClassificationUtil binaryTextClassifier = new BinaryTextClassificationUtil();
		//binaryTextClassifier.buildTermIdMap(2, 8, 9);
		//binaryTextClassifier.outputFeatureVectors(2, 8, 9);

		binaryTextClassifier.calculateMetrics("experiment2-classifier-output.txt", "phase1.dev.shuf.tsv");
	}
}







