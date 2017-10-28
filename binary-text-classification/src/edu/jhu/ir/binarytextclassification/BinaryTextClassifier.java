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

public class BinaryTextClassifier {

	private Map<String, Integer> lexicon = new HashMap<>(); //Map of term string to termID
	private int termIdCounter = 1;

	private void buildLexicon(int...columnIndices) throws IOException {
		buildLexicon("phase1.train.shuf.tsv", columnIndices);
		buildLexicon("phase1.dev.shuf.tsv", columnIndices);
	}

	/**
	 * Builds a map of term to termID
	 * Takes one or more column indices from which to build the term map
	 * @param columnIndices
	 * @throws IOException
	 */
	public void buildLexicon(String inputFileName, int...columnIndices) throws IOException {
		BufferedReader tsvFile = new BufferedReader(new FileReader(inputFileName));
		String row = tsvFile.readLine(); // Read first line.

		while (row != null) {
			String[] columns = row.split("\t");

			if (columns.length == 10) {
				for (int index : columnIndices) {
					for (String token: IRUtil.tokenize(columns[index])) {
						if (!lexicon.containsKey(token)) {
							lexicon.put(token, termIdCounter);
							termIdCounter++;

							//System.out.println(token + ": " + termId);
						}
					}
				}
			}
			row = tsvFile.readLine(); // Read next line of data.
		}

		// Close the file once all data has been read.
		tsvFile.close();
	}

	public void outputFeatureVectors(String inputFileName, String outputFileName, int... columnIndices) throws IOException {
		BufferedReader tsvFile = new BufferedReader(new FileReader(inputFileName));
		PrintWriter writer = new PrintWriter(outputFileName);
		String row = tsvFile.readLine(); // Read first line.

		while (row != null) {
			String[] columns = row.split("\t");
			if (columns.length == 10) {
				String classLabel = row.substring(0, 2).trim();
				if (classLabel.equals("1")) {
					classLabel = "+1";
				}

				writer.print(classLabel + " ");

				Map<String, Integer> termIds = new HashMap<>();
				for (int index : columnIndices) {
					List<String> tokens = IRUtil.tokenize(columns[index]);
					for (String token: tokens) {
						termIds.put(token, lexicon.get(token));
					}
				}

				/*for (Map.Entry<String, Integer> entry: termIds.entrySet()) {
				System.out.println(entry.getKey() + ": " + entry.getValue());
				}*/

				Map<String, Integer> sortedTermIds = IRUtil.sortMapByValue(termIds);

				for (int termId: sortedTermIds.values()) {
					writer.print(termId + ":1 ");
				}
				writer.println();
			}
			row = tsvFile.readLine(); // Read next line of data.
		}
		// Close the file once all data has been read.
		tsvFile.close();
		writer.close();

		// End the printout with a blank line.
		System.out.println();

	}


	/**
	 * Processes the data files and writes a file containing feature vectors,
	 * 		one document vector per line
	 * Uses features only from the title sectino (column 3) of the data
	 * @throws IOException
	 */
	public void outputFeatureVectors(int... columnIndices) throws IOException {
		//outputFeatureVectors("phase1.train.shuf.tsv", "phase1.train.svmlight.txt", columnIndices);
		outputFeatureVectors("phase1.dev.shuf.tsv", "phase1.dev.svmlight.txt", columnIndices);
	}

	public static void main(String[] args) throws IOException {
		BinaryTextClassifier binaryTextClassifier = new BinaryTextClassifier();
		binaryTextClassifier.buildLexicon(2, 8, 9);
		binaryTextClassifier.outputFeatureVectors(2, 8, 9);
	}
}







